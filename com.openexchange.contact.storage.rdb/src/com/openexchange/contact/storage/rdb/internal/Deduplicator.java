/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.contact.storage.rdb.internal;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.openexchange.contact.storage.rdb.fields.DistListMemberField;
import com.openexchange.contact.storage.rdb.fields.Fields;
import com.openexchange.contact.storage.rdb.mapping.Mappers;
import com.openexchange.contact.storage.rdb.sql.Executor;
import com.openexchange.contact.storage.rdb.sql.Table;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.groupware.tools.mappings.Mapping;

/**
 * {@link Deduplicator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Deduplicator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Deduplicator.class);
    private static final int DELETE_CHUNK_SIZE = 500;

    /**
     * De-duplicates contacts in a folder.
     *
     * @param contextID The context ID
     * @param folderID The folder ID
     * @param limit The maximum number of contacts to process, or <code>0</code> for no limits
     * @param dryRun <code>true</code> to analyze the folder for duplicates only, without actually performing the deduplication,
     *               <code>false</code>, otherwise
     * @return The identifiers of the contacts identified (and deleted in case <code>dryRun</code> is <code>false</code>) as duplicates
     * @throws OXException
     */
    public static List<Integer> deduplicateContacts(int contextID, int folderID, long limit, boolean dryRun) throws OXException {
        /*
         * acquire database connection
         */
        DatabaseService databaseService = RdbServiceLookup.getService(DatabaseService.class, true);
        Connection connection = null;
        int rollback = 0;
        try {
            if (dryRun) {
                connection = databaseService.getReadOnly(contextID);
            } else {
                connection = databaseService.getWritable(contextID);
                connection.setAutoCommit(false);
                rollback = 1;
            }
            /*
             * get contacts per hash code, split data into lists of ids for different tables in case there are two or more found
             */
            List<Integer> contactDataToDelete = new ArrayList<Integer>();
            List<Integer> imageDataToDelete = new ArrayList<Integer>();
            List<Integer> distListDataToDelete = new ArrayList<Integer>();
            {
                Map<Integer, List<Contact>> contactsPerHash = getContactsPerHash(connection, contextID, folderID, limit);
                for (List<Contact> contacts : contactsPerHash.values()) {
                    for (int i = 1; i < contacts.size(); i++) {
                        Contact contact = contacts.get(i);
                        Integer objectID = Integer.valueOf(contact.getObjectID());
                        contactDataToDelete.add(objectID);
                        if (0 < contact.getNumberOfImages()) {
                            imageDataToDelete.add(objectID);
                        }
                        if (0 < contact.getNumberOfDistributionLists()) {
                            distListDataToDelete.add(objectID);
                        }
                    }
                }
            }
            /*
             * delete contact-, image- and distribution list data
             */
            if (dryRun) {
                LOG.info("Would delete {} duplicate contacts.", I(contactDataToDelete.size()));
            } else {
                LOG.info("Going to delete {} duplicate contacts.", I(contactDataToDelete.size()));
                int contactDataDeleted = deleteContactData(connection, Table.CONTACTS, contextID, contactDataToDelete, DELETE_CHUNK_SIZE);
                int imageDataDeleted = deleteContactData(connection, Table.IMAGES, contextID, imageDataToDelete, DELETE_CHUNK_SIZE);
                int distListDataDeleted = deleteContactData(connection, Table.DISTLIST, contextID, distListDataToDelete, DELETE_CHUNK_SIZE);
                LOG.info("Deleted {} records in table {}.", I(contactDataDeleted), Table.CONTACTS);
                LOG.info("Deleted {} records in table {}.", I(imageDataDeleted), Table.IMAGES);
                LOG.info("Deleted {} records in table {}.", I(distListDataDeleted), Table.DISTLIST);
            }
            if (rollback > 0) {
                connection.commit();
                rollback = 2;
            }
            return contactDataToDelete;
        } catch (SQLException e) {
            throw ContactExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback==1) {
                    Databases.rollback(connection);
                }
                Databases.autocommit(connection);
            }
            if (dryRun) {
                databaseService.backReadOnly(contextID, connection);
            } else {
                databaseService.backWritable(contextID, connection);
            }
        }
    }

    /**
     * Gets all contacts in a folder grouped by a hash key over content-related properties. Contacts with attachments are skipped.
     *
     * @param connection The database connection
     * @param contextID The context ID
     * @param folderID The folder ID
     * @param limit The maximum number of contacts to process, or <code>0</code> for no limits
     * @return The contacts per hash
     * @throws OXException
     */
    private static Map<Integer, List<Contact>> getContactsPerHash(Connection connection, int contextID, int folderID, long limit) throws OXException {
        Map<Integer, List<Contact>> contactsPerHash = new HashMap<Integer, List<Contact>>();
        /*
         * prepare statement
         */
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT ").append(Mappers.CONTACT.getColumns(Fields.CONTACT_DATABASE_ARRAY)).append(" FROM ").append(Table.CONTACTS)
            .append(" WHERE ").append(Mappers.CONTACT.get(ContactField.CONTEXTID).getColumnLabel()).append("=?")
            .append(" AND ").append(Mappers.CONTACT.get(ContactField.FOLDER_ID).getColumnLabel()).append("=?")
            .append(" ORDER BY ").append(Mappers.CONTACT.get(ContactField.OBJECT_ID).getColumnLabel()).append(" ASC");
        if (0 < limit) {
            stringBuilder.append(" LIMIT ").append(limit);
        }
        stringBuilder.append(';');
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            stmt.setInt(1, contextID);
            stmt.setInt(2, folderID);
            /*
             * execute and read out results
             */
            resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                /*
                 * create contact from result
                 */
                Contact contact = Mappers.CONTACT.fromResultSet(resultSet, Fields.CONTACT_DATABASE_ARRAY);
                if (0 < contact.getNumberOfAttachments()) {
                    LOG.info("Unable to de-deduplicate contacts with attachments, skipping contact {}.", I(contact.getObjectID()));
                    continue;
                }
                if (0 < contact.getNumberOfImages()) {
                    contact = mergeImageData(connection, contextID, contact);
                }
                if (0 < contact.getNumberOfDistributionLists()) {
                    mergeDistListData(connection, contextID, contact);
                }
                /*
                 * calculate hash, add to remembered object IDs
                 */
                Integer hash = Integer.valueOf(calculateHash(contact, getContentFields(), getDistListContentFields()));
                List<Contact> contacts = contactsPerHash.get(hash);
                if (null == contacts) {
                    /*
                     * remember as new unique contact
                     */
                    LOG.debug("Remembering new unique contact: {}", contact);
                    contacts = new LinkedList<Contact>();
                    contacts.add(stripContact(contact));
                    contactsPerHash.put(hash, contacts);
                } else {
                    /*
                     * found duplicate contact
                     */
                    LOG.debug("Found duplicate contact: {}", contact);
                    contacts.add(stripContact(contact));
                }
            }
        } catch (SQLException e) {
            throw ContactExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(resultSet, stmt);
        }
        return contactsPerHash;
    }

    /**
     * Merges distribution list image data into the supplied contact.
     *
     * @param connection The connection to use
     * @param contextID The context ID
     * @param contact The contact to merge the data into
     * @return The contact
     */
    private static Contact mergeDistListData(Connection connection, int contextID, Contact contact) throws SQLException, OXException {
        DistListMember[] members = new Executor().select(connection, Table.DISTLIST, contextID, contact.getObjectID(), Fields.DISTLIST_DATABASE_ARRAY);
        contact.setDistributionList(members);
        return contact;
    }

    /**
     * Merges data image data into the supplied contact.
     *
     * @param connection The connection to use
     * @param contextID The context ID
     * @param contact The contact to merge the data into
     * @return The contact
     */
    private static Contact mergeImageData(Connection connection, int contextID, Contact contact) throws SQLException, OXException {
        Contact imageData = new Executor().selectSingle(connection, Table.IMAGES, contextID, contact.getObjectID(), Fields.IMAGE_DATABASE_ARRAY);
        Mappers.CONTACT.mergeDifferences(contact, imageData);
        return contact;
    }

    /**
     * Deletes multiple records of contact data from a database table.
     *
     * @param connection The connection to use
     * @param table The database table to target
     * @param contextID The context ID
     * @param objectIDs The identifiers of the contact data to delete, matching against the <code>intfield01</code> column
     * @param chunkSize The maximum chunk size to use when deleting
     * @return The number of affected rows
     */
    private static int deleteContactData(Connection connection, Table table, int contextID, List<Integer> objectIDs, int chunkSize) throws SQLException, OXException {
        int affectedRows = 0;
        for (int i = 0; i < objectIDs.size(); i += chunkSize) {
            int length = Math.min(objectIDs.size(), i + chunkSize) - i;
            affectedRows += deleteContactData(connection, table, contextID, objectIDs.subList(i, i + length));
        }
        return affectedRows;
    }

    /**
     * Deletes multiple records of contact data from a database table.
     *
     * @param connection The connection to use
     * @param table The database table to target
     * @param contextID The context ID
     * @param objectIDs The identifiers of the contact data to delete, matching against the <code>intfield01</code> column
     * @return The number of affected rows
     */
    private static int deleteContactData(Connection connection, Table table, int contextID, List<Integer> objectIDs) throws SQLException, OXException {
        if (null == objectIDs || 0 == objectIDs.size()) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder("DELETE FROM ").append(table)
            .append(" WHERE ").append(Mappers.CONTACT.get(ContactField.CONTEXTID).getColumnLabel()).append("=?")
            .append(" AND ").append(Mappers.CONTACT.get(ContactField.OBJECT_ID).getColumnLabel()).append(" IN (?")
        ;
        for (int i = 1; i < objectIDs.size(); i++) {
            stringBuilder.append(",?");
        }
        stringBuilder.append(");");
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(stringBuilder.toString());
            stmt.setInt(1, contextID);
            for (int i = 0; i < objectIDs.size(); i++) {
                stmt.setInt(2 + i, objectIDs.get(i).intValue());
            }
            return stmt.executeUpdate();
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    /**
     * Gets an array of contact fields counting as actual "content" of a contact, i.e. data that was entered by a user or client.
     *
     * @return The content fields
     */
    static ContactField[] getContentFields() {
        ContactField[] syntheticFields =  {
            ContactField.CREATION_DATE, ContactField.CREATED_BY, ContactField.LAST_MODIFIED, ContactField.LAST_MODIFIED_UTC,
            ContactField.MODIFIED_BY, ContactField.FOLDER_ID, ContactField.CONTEXTID, //ContactField.INTERNAL_USERID,
            ContactField.OBJECT_ID, ContactField.UID, ContactField.FILENAME, ContactField.VCARD_ID, ContactField.USE_COUNT, 
            ContactField.LAST_MODIFIED_OF_NEWEST_ATTACHMENT, ContactField.IMAGE_LAST_MODIFIED, ContactField.IMAGE1_URL
        };
        EnumSet<ContactField> contentFields = EnumSet.allOf(ContactField.class);
        contentFields.removeAll(Arrays.asList(syntheticFields));
        return contentFields.toArray(new ContactField[contentFields.size()]);
    }

    /**
     * Gets an array of contact fields counting as actual "content" of a distribution list member, i.e. data that was entered by a user
     * or client.
     *
     * @return The content fields
     */
    static DistListMemberField[] getDistListContentFields() {
        EnumSet<DistListMemberField> contentFields = EnumSet.allOf(DistListMemberField.class);
        contentFields.removeAll(Arrays.asList(DistListMemberField.UUID, DistListMemberField.PARENT_CONTACT_ID, DistListMemberField.CONTEXT_ID));
        return contentFields.toArray(new DistListMemberField[contentFields.size()]);
    }

    /**
     * Creates a "stripped" version of a contact, taking over only the supplied fields.
     *
     * @param contact The contact to strip
     * @param fields The fields to take over
     * @return The stripped contact
     * @throws OXException
     */
    private static Contact stripContact(Contact contact, ContactField...fields) throws OXException {
        Contact strippedContact = new Contact();
        for (ContactField field : fields) {
            Mappers.CONTACT.get(field).copy(contact, strippedContact);
        }
        return strippedContact;
    }

    private static Contact stripContact(Contact contact) throws OXException {
        return stripContact(contact, ContactField.OBJECT_ID, ContactField.NUMBER_OF_IMAGES, ContactField.NUMBER_OF_DISTRIBUTIONLIST);
    }

    /**
     * Calculates a hashcode of a contact considering the supplied fields.
     *
     * @param contact The contact to generate the hash for
     * @param contentFields The fields to consider
     * @param memberContentFields The distribution list member fields to consider
     * @return The hash code
     */
    static int calculateHash(Contact contact, ContactField[] contentFields, DistListMemberField[] memberContentFields) {
        int prime = 31;
        int hash = 1;
        for (ContactField field : contentFields) {
            Mapping<? extends Object, Contact> mapping = Mappers.CONTACT.opt(field);
            if (null == mapping || false == mapping.isSet(contact)) {
                hash = hash * prime;
                continue;
            }
            Object value = mapping.get(contact);
            if (null == value) {
                hash = hash * prime;
            } else if ((value instanceof byte[])) {
                hash = hash * prime + Arrays.hashCode((byte[])value);
            } else if ((value instanceof DistListMember[])) {
                hash = hash * prime + calculateHash((DistListMember[])value, memberContentFields);
            } else {
                hash = hash * prime + value.hashCode();
            }
        }
        return hash;
    }

    /**
     * Calculates a hashcode of an array of distribution list members considering the supplied fields. The order of the members is not
     * relevant.
     *
     * @param members The members to generate the hash for
     * @param memberContentFields The distribution list member fields to consider
     * @return The hash code
     */
    static int calculateHash(DistListMember[] members, DistListMemberField[] memberContentFields) {
        if (null == members) {
            return 0;
        }
        int[] memberHashes = new int[members.length];
        for (int i = 0; i < members.length; i++) {
            memberHashes[i] = calculateHash(members[i], memberContentFields);
        }
        Arrays.sort(memberHashes);
        return Arrays.hashCode(memberHashes);
    }

    /**
     * Calculates a hashcode of a distribution list member considering the supplied fields.
     *
     * @param member The member to generate the hash for
     * @param memberContentFields The distribution list member fields to consider
     * @return The hash code
     */
    static int calculateHash(DistListMember member, DistListMemberField[] memberContentFields) {
        if (null == member) {
            return 0;
        }
        int prime = 31;
        int hash = 1;
        for (DistListMemberField field : memberContentFields) {
            Mapping<? extends Object, DistListMember> mapping = Mappers.DISTLIST.opt(field);
            if (null == mapping || false == mapping.isSet(member)) {
                continue;
            }
            Object value = mapping.get(member);
            if (null == value) {
                hash = hash * prime;
            } else {
                hash = hash * prime + value.hashCode();
            }
        }
        return hash;
    }

    static void duplicateContacts(int contextID, int folderID, int count, int forUser) throws OXException {
        Executor executor = new Executor();
        DatabaseService databaseService = RdbServiceLookup.getService(DatabaseService.class, true);
        Connection connection = null;
        int rollback = 0;
        try {
            connection = databaseService.getWritable(contextID);
            connection.setAutoCommit(false);
            rollback = 1;
            /*
             * get contacts to duplicate
             */
            List<Contact> contacts = executor.select(
                connection, Table.CONTACTS, contextID, folderID, null, Integer.MIN_VALUE, Fields.CONTACT_DATABASE_ARRAY, null, null, forUser);
            for (Contact contact : contacts) {
                if (0 < contact.getNumberOfAttachments()) {
                    continue;
                }
                if (0 < contact.getNumberOfImages()) {
                    mergeImageData(connection, contextID, contact);
                }
                if (0 < contact.getNumberOfDistributionLists()) {
                    mergeDistListData(connection, contextID, contact);
                }
            }
            for (int i = 0; i < count; i++) {
                for (Contact contact : contacts) {
                    /*
                     * prepare insert
                     */
                    contact.setObjectID(IDGenerator.getId(contextID, com.openexchange.groupware.Types.CONTACT, connection));
                    Date now = new Date();
                    contact.setLastModified(now);
                    contact.setCreationDate(now);
                    contact.setParentFolderID(folderID);
                    contact.setContextId(contextID);
                    /*
                     * insert image data if needed
                     */
                    if (contact.containsImage1() && null != contact.getImage1()) {
                        contact.setImageLastModified(now);
                        executor.insert(connection, Table.IMAGES, contact, Fields.IMAGE_DATABASE_ARRAY);
                    }
                    /*
                     * insert contact
                     */
                    executor.insert(connection, Table.CONTACTS, contact, Fields.CONTACT_DATABASE_ARRAY);
                    /*
                     * insert distribution list data if needed
                     */
                    if (contact.containsDistributionLists()) {
                        DistListMember[] members = DistListMember.create(contact.getDistributionList(), contextID, contact.getObjectID());
                        executor.insert(connection, Table.DISTLIST, members, Fields.DISTLIST_DATABASE_ARRAY);
                    }
                }
            }
            connection.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw ContactExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback==1) {
                    Databases.rollback(connection);
                }
                Databases.autocommit(connection);
            }
            databaseService.backWritable(contextID, connection);
        }
    }

}
