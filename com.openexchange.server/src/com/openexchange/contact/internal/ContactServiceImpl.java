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

package com.openexchange.contact.internal;

import static com.openexchange.contact.internal.Tools.parse;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import com.openexchange.contact.AutocompleteParameters;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.internal.mapping.ContactMapper;
import com.openexchange.contact.storage.ContactStorage;
import com.openexchange.event.impl.EventClient;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.ContactMergerator;
import com.openexchange.groupware.contact.Search;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.results.DefaultSequenceResult;
import com.openexchange.groupware.results.DefaultUpdatesResult;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.user.UserService;
import com.openexchange.user.interceptor.UserServiceInterceptor;
import com.openexchange.user.interceptor.UserServiceInterceptorRegistry;

/**
 * {@link ContactServiceImpl}
 *
 * Default {@link ContactService} implementation.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactServiceImpl extends DefaultContactService {

    private final UserServiceInterceptorRegistry interceptorRegistry;

    /**
     * Initializes a new {@link ContactServiceImpl}.
     */
    public ContactServiceImpl(UserServiceInterceptorRegistry interceptorRegistry) {
        super();
        this.interceptorRegistry = interceptorRegistry;
    }

    @Override
    protected void doCreateContact(final Session session, final String folderID, final Contact contact) throws OXException {
        final int userID = session.getUserId();
        final int contextID = session.getContextId();
        final ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * check supplied contact
         */
        Check.validateProperties(contact);
        /*
         * check general permissions
         */
        final EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
        Check.canCreateObjects(permission, session, folderID);
        /*
         * check folder
         */
        final FolderObject folder = Tools.getFolder(contextID, folderID);
        Check.isContactFolder(folder, session);
        Check.noPrivateInPublic(folder, contact, session);
        Check.canWriteInGAB(storage, session, folderID, contact, null);

        /*
         * prepare create
         */
        final Date now = new Date();
        contact.setParentFolderID(parse(folderID));
        contact.setContextId(contextID);
        contact.setLastModified(now);
        contact.setCreationDate(now);
        contact.setCreatedBy(userID);
        contact.setModifiedBy(userID);
        contact.removeObjectID(); // set by storage during create
        contact.setNumberOfAttachments(0);
        if (contact.containsImage1()) {
            contact.setImageLastModified(now);
            if (null != contact.getImage1()) {
                contact.setNumberOfImages(1);
            } else {
                contact.setNumberOfImages(0);
                contact.setImageContentType(null);
            }
        }
        if (contact.containsDistributionLists()) {
            Tools.fillDistributionListMembers(session, contact.getDistributionList());
        }
        if (false == contact.containsUid() || Tools.isEmpty(contact.getUid())) {
            contact.setUid(UUID.randomUUID().toString());
        }
        if (false == contact.containsFileAs() && contact.containsDisplayName()) {
            contact.setFileAs(contact.getDisplayName());
        }
        /*
         * pass through to storage
         */
        storage.create(session, folderID, contact);
        /*
         * broadcast event
         */
        new EventClient(session).create(contact, folder);
    }

    @Override
    protected void doUpdateAndMoveContact(final Session session, final String sourceFolderId, final String targetFolderId, final String objectID,
            final Contact contact, final Date lastRead) throws OXException {
        final int userID = session.getUserId();
        final int contextID = session.getContextId();
        /*
         * check supplied contact
         */
        ContactMapper.getInstance().validateAll(contact);
        if (contact.containsObjectID() && contact.getObjectID() > 0 && false == Integer.toString(contact.getObjectID()).equals(objectID)) {
            throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextID));
        }
        /*
         * check source folder
         */
        final FolderObject sourceFolder = Tools.getFolder(contextID, sourceFolderId);
        Check.isContactFolder(sourceFolder, session);
        final EffectivePermission sourceFolderPermission = Tools.getPermission(contextID, sourceFolderId, userID);
        Check.canDeleteOwn(sourceFolderPermission, session, sourceFolderId);
        Check.canReadOwn(sourceFolderPermission, session, sourceFolderId);
        /*
         * check destination folder
         */
        final FolderObject targetFolder = Tools.getFolder(contextID, targetFolderId);
        Check.isContactFolder(targetFolder, session);
        Check.noPrivateInPublic(targetFolder, contact, session);
        final EffectivePermission targetFolderPermission = Tools.getPermission(contextID, targetFolderId, userID);
        Check.canCreateObjects(targetFolderPermission, session, targetFolderId);
        /*
         * check currently stored contact
         */
        final ContactStorage sourceStorage = Tools.getStorage(session, sourceFolderId);
        final Contact storedContact = sourceStorage.get(session, sourceFolderId, objectID, ContactField.values());
        Check.contactNotNull(storedContact, contextID, Tools.parse(objectID));
        if (storedContact.getCreatedBy() != userID) {
            Check.canDeleteAll(sourceFolderPermission, session, sourceFolderId);
            Check.canReadAll(sourceFolderPermission, session, sourceFolderId);
        }
        Check.lastModifiedBefore(storedContact, lastRead);
        Check.folderEquals(storedContact, sourceFolderId, contextID);
        /*
         * check for not allowed changes
         */
        Contact delta = ContactMapper.getInstance().getDifferences(storedContact, contact);
        Check.readOnlyFields(userID, storedContact, delta);
        /*
         * prepare update
         */
        final Date now = new Date();
        delta.setLastModified(now);
        delta.setModifiedBy(userID);
        delta.setParentFolderID(parse(targetFolderId));
        if ((false == storedContact.containsUid() || Tools.isEmpty(storedContact.getUid())) && false == delta.containsUid()) {
            delta.setUid(UUID.randomUUID().toString());
        }
        if (delta.containsImage1()) {
            delta.setImageLastModified(now);
            if (null != delta.getImage1()) {
                delta.setNumberOfImages(1);
            } else {
                delta.setNumberOfImages(0);
                delta.setImageContentType(null);
            }
        }
        if (delta.containsDistributionLists()) {
            Tools.fillDistributionListMembers(session, delta.getDistributionList());
        }
        Tools.invalidateAddressesIfNeeded(delta);
        Tools.setFileAsIfNeeded(delta);
        Contact updatedContact = new Contact();
        ContactMapper.getInstance().mergeDifferences(updatedContact, storedContact);
        ContactMapper.getInstance().mergeDifferences(updatedContact, delta);
        delta.removeUseCount();
        /*
         * pass through to storage
         */
        final ContactStorage targetStorage = Tools.getStorage(session, targetFolderId);
        if (sourceStorage.equals(targetStorage)) {
            /*
             * same storage, send update as delta
             */
            sourceStorage.update(session, sourceFolderId, objectID, delta, lastRead);
            /*
             * merge back differences to supplied contact
             */
            ContactMapper.getInstance().mergeDifferences(contact, delta);
        } else {
            /*
             * different storage, perform delete & create of complete contact information
             */
            //TODO: move attachments
            targetStorage.create(session, targetFolderId, updatedContact);
            /*
             * merge back differences to supplied contact
             */
            ContactMapper.getInstance().mergeDifferences(contact, updatedContact);
            try {
                sourceStorage.delete(session, sourceFolderId, objectID, lastRead);
            } catch (OXException e) {
                LOG.warn("error deleting contact from source folder, rolling back move operation", e);
                // TODO: simple rollback for now
                targetStorage.delete(session, targetFolderId, Integer.toString(storedContact.getObjectID()),
                        storedContact.getLastModified());
                throw e;
            }
        }
        /*
         * broadcast event
         */
        for (final ContactStorage contactStorage : Tools.getStorages(session)) {
            contactStorage.updateReferences(session, storedContact, updatedContact);
        }
        new EventClient(session).modify(storedContact, updatedContact, targetFolder);
    }

    @Override
    protected void doUpdateContact(final Session session, final String folderID, final String objectID, final Contact contact, final Date lastRead) throws OXException {
        final int userID = session.getUserId();
        final int contextID = session.getContextId();
        final ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * check supplied contact
         */
        Check.validateProperties(contact);
        if (contact.containsObjectID() && contact.getObjectID() > 0 && false == Integer.toString(contact.getObjectID()).equals(objectID)) {
            throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextID));
        }
        /*
         * check general permissions with regard to global address book
         */
        final EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
        Check.canWriteOwn(permission, session);
        /*
         * check currently stored contact
         */
        final Contact storedContact = storage.get(session, folderID, objectID, ContactField.values());
        Check.contactNotNull(storedContact, contextID, Tools.parse(objectID));
        if (storedContact.getCreatedBy() != userID) {
            Check.canWriteAll(permission, session);
        }
        Check.lastModifiedBefore(storedContact, lastRead);
        Check.folderEquals(storedContact, folderID, contextID);
        /*
         * check folder
         */
        final FolderObject folder = Tools.getFolder(contextID, folderID);
        Check.isContactFolder(folder, session);
        Check.noPrivateInPublic(folder, contact, session);
        Check.canWriteInGAB(storage, session, folderID, contact, storedContact);
        /*
         * check for not allowed changes
         */
        Contact delta = ContactMapper.getInstance().getDifferences(storedContact, contact);
        Check.readOnlyFields(userID, storedContact, delta);
        if (delta.containsParentFolderID()) {
            throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextID));
        }
        /*
         * prepare update
         */
        final Date now = new Date();
        delta.setLastModified(now);
        delta.setModifiedBy(userID);
        if ((false == storedContact.containsUid() || Tools.isEmpty(storedContact.getUid())) && false == delta.containsUid()) {
            delta.setUid(UUID.randomUUID().toString());
        }
        if (delta.containsImage1()) {
            delta.setImageLastModified(now);
            if (null != delta.getImage1()) {
                delta.setNumberOfImages(1);
                if (null == delta.getImageContentType()) {
                    delta.setImageContentType(storedContact.getImageContentType());
                }
            } else {
                delta.setNumberOfImages(0);
                delta.setImageContentType(null);
            }
        }
        if (delta.containsDistributionLists()) {
            Tools.fillDistributionListMembers(session, delta.getDistributionList());
        }
        Tools.invalidateAddressesIfNeeded(delta);
        Tools.setFileAsIfNeeded(delta);
        Contact updatedContact = new Contact();
        ContactMapper.getInstance().mergeDifferences(updatedContact, storedContact);
        ContactMapper.getInstance().mergeDifferences(updatedContact, delta);
        delta.removeUseCount(); // use counts are handled by use count service
        /*
         * pass through to storage
         */
        boolean isUserContact = updatedContact.getInternalUserId() > 0;
        Context storageContext = isUserContact ? Tools.getContext(session) : null;
        if (isUserContact) {
            beforeUserUpdate(storageContext, updatedContact);
        }
        storage.update(session, folderID, objectID, delta, lastRead);
        /*
         * merge back differences to supplied contact
         */
        ContactMapper.getInstance().mergeDifferences(contact, delta);
        if (isUserContact) {
            afterUserUpdate(storageContext, updatedContact);
        }
        /*
         * broadcast event
         */
        for (final ContactStorage contactStorage : Tools.getStorages(session)) {
            contactStorage.updateReferences(session, storedContact, updatedContact);
        }
        new EventClient(session).modify(storedContact, updatedContact, folder);
    }

    @Override
    protected void doUpdateUser(final Session session, final String folderID, final String objectID, final Contact contact,
        final Date lastRead) throws OXException {
        int userID = session.getUserId();
        int contextId = session.getContextId();
        ContactStorage storage = Tools.getStorage(session, folderID);
        final Context storageContext = Tools.getContext(session);
        /*
         * check supplied contact
         */
        Check.validateProperties(contact);
        if (contact.containsObjectID() && contact.getObjectID() > 0 && false == Integer.toString(contact.getObjectID()).equals(objectID)) {
            throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextId));
        }
        /*
         * check folder
         */
        if (FolderObject.SYSTEM_LDAP_FOLDER_ID != parse(folderID) ||
            contact.containsParentFolderID() && 0 < contact.getParentFolderID() &&
            FolderObject.SYSTEM_LDAP_FOLDER_ID != contact.getParentFolderID()) {
            throw ContactExceptionCodes.NO_ACCESS_PERMISSION.create(I(FolderObject.SYSTEM_LDAP_FOLDER_ID), I(contextId), I(userID));
        }
        /*
         * check currently stored contact
         */
        Contact storedContact = storage.get(session, folderID, objectID, ContactField.values());
        Check.contactNotNull(storedContact, contextId, Tools.parse(objectID));
        if (storedContact.getCreatedBy() != userID) {
            if (storedContact.getInternalUserId() == userID && storedContact.getCreatedBy() == Tools.getContext(contextId).getMailadmin()) {
                /*
                 * take over bugfix for https://bugs.open-xchange.com/show_bug.cgi?id=19128#c9:
                 * Accepting context admin as a user's contact creator, too, and executing self-healing mechanism
                 */
                contact.setCreatedBy(storedContact.getInternalUserId());
            } else {
                throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextId));
            }
        }
        Check.lastModifiedBefore(storedContact, lastRead);
        Check.folderEquals(storedContact, folderID, contextId);
        /*
         * check special GAB permissions
         */
        Check.canWriteInGAB(storage, session, folderID, contact, storedContact);
        /*
         * check for not allowed changes
         */
        Contact delta = ContactMapper.getInstance().getDifferences(storedContact, contact);
        Check.readOnlyFields(userID, storedContact, delta);
        if (delta.containsParentFolderID()) {
            throw ContactExceptionCodes.NO_CHANGE_PERMISSION.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextId));
        }
        /*
         * prepare update
         */
        final Date now = new Date();
        delta.setLastModified(now);
        delta.setModifiedBy(userID);
        if ((false == storedContact.containsUid() || Tools.isEmpty(storedContact.getUid())) && false == delta.containsUid()) {
            delta.setUid(UUID.randomUUID().toString());
        }
        if (delta.containsImage1()) {
            delta.setImageLastModified(now);
            if (null != delta.getImage1()) {
                delta.setNumberOfImages(1);
            } else {
                delta.setNumberOfImages(0);
                delta.setImageContentType(null);
            }
        }
        if (delta.containsDistributionLists()) {
            Tools.fillDistributionListMembers(session, delta.getDistributionList());
        }
        Tools.invalidateAddressesIfNeeded(delta);
        Tools.setFileAsIfNeeded(delta);
        Contact updatedContact = new Contact();
        ContactMapper.getInstance().mergeDifferences(updatedContact, storedContact);
        ContactMapper.getInstance().mergeDifferences(updatedContact, delta);
        /*
         * pass through to storage
         */
        beforeUserUpdate(storageContext, storedContact);
        storage.update(session, folderID, objectID, delta, lastRead);
        afterUserUpdate(storageContext, updatedContact);
        /*
         * merge back differences to supplied contact
         */
        ContactMapper.getInstance().mergeDifferences(contact, delta);
        /*
         * broadcast event
         */
        for (final ContactStorage contactStorage : Tools.getStorages(session)) {
            contactStorage.updateReferences(session, storedContact, updatedContact);
        }
        new EventClient(session).modify(storedContact, updatedContact, Tools.getFolder(contextId, folderID));
        UserService us = ContactServiceLookup.getService(UserService.class);
        us.invalidateUser(storageContext, userID);
    }

    @Override
    protected void doDeleteContact(final Session session, final String folderID, final String objectID, final Date lastRead) throws OXException {
        final int userID = session.getUserId();
        final int contextID = session.getContextId();
        final ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * check folder
         */
        final FolderObject folder = Tools.getFolder(contextID, folderID);
        Check.isContactFolder(folder, session);
        /*
         * check general permissions
         */
        final EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
        Check.canDeleteOwn(permission, session, folderID);
        /*
         * check currently stored contact
         */
        final Contact storedContact = storage.get(session, folderID, objectID, new ContactField[] { ContactField.CREATED_BY,
            ContactField.LAST_MODIFIED, ContactField.VCARD_ID, ContactField.BIRTHDAY });
        Check.contactNotNull(storedContact, contextID, Tools.parse(objectID));
        if (storedContact.getCreatedBy() != userID) {
            Check.canDeleteAll(permission, session, folderID);
        }
        Check.lastModifiedBefore(storedContact, lastRead);
        /*
         * delete contact from storage
         */
        storage.delete(session, folderID, objectID, lastRead);
        /*
         * broadcast event
         */
        storedContact.setContextId(contextID);
        storedContact.setParentFolderID(parse(folderID));
        storedContact.setObjectID(parse(objectID));
        new EventClient(session).delete(storedContact, folder);
    }

    @Override
    protected void doDeleteContacts(Session session, String folderID, String[] objectIDs, Date lastRead) throws OXException {
        final int userID = session.getUserId();
        final int contextID = session.getContextId();
        final ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * check folder
         */
        FolderObject folder = Tools.getFolder(contextID, folderID);
        Check.isContactFolder(folder, session);
        /*
         * check general permissions
         */
        EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
        Check.canDeleteOwn(permission, session, folderID);
        /*
         * check currently stored contacts
         */
        SearchIterator<Contact> searchIterator = null;
        List<Contact> storedContacts = new ArrayList<Contact>();
        try {
            searchIterator = storage.list(session, folderID, objectIDs, new ContactField[] { ContactField.CREATED_BY,
                ContactField.LAST_MODIFIED, ContactField.OBJECT_ID, ContactField.VCARD_ID, ContactField.BIRTHDAY,
                ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3 });
            while (searchIterator.hasNext()) {
                Contact storedContact = searchIterator.next();
                if (storedContact.getCreatedBy() != userID) {
                    Check.canDeleteAll(permission, session, folderID);
                }
                Check.lastModifiedBefore(storedContact, lastRead);
                storedContacts.add(storedContact);
            }
        } finally {
            Tools.close(searchIterator);
        }
        /*
         * ensure all contacts were found
         */
        for (String objectID : objectIDs) {
            boolean found = false;
            for (Contact contact : storedContacts) {
                if (contact.getObjectID() == parse(objectID)) {
                    found = true;
                    break;
                }
            }
            if (false == found) {
                throw ContactExceptionCodes.CONTACT_NOT_FOUND.create(Integer.valueOf(parse(objectID)), Integer.valueOf(contextID));
            }
        }
        /*
         * delete contacts from storage
         */
        storage.delete(session, folderID, objectIDs, lastRead);
        /*
         * broadcast event
         */
        EventClient eventClient = new EventClient(session);
        for (Contact storedContact : storedContacts) {
            storedContact.setContextId(contextID);
            storedContact.setParentFolderID(parse(folderID));
            storedContact.setObjectID(storedContact.getObjectID());
            eventClient.delete(storedContact, folder);
        }
    }

    @Override
    protected void doDeleteContacts(final Session session, final String folderID) throws OXException {
        int userID = session.getUserId();
        int contextID = session.getContextId();
        ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * check folder
         */
        FolderObject folder = Tools.getFolder(contextID, folderID);
        Check.isContactFolder(folder, session);
        /*
         * check currently stored contacts
         */
        boolean containsForeignContacts = false;
        List<Contact> storedContacts = new ArrayList<Contact>();
        SearchIterator<Contact> searchIterator = null;
        try {
            searchIterator = storage.all(session, folderID, new ContactField[] { ContactField.CREATED_BY, ContactField.OBJECT_ID, ContactField.VCARD_ID, ContactField.BIRTHDAY });
            if (null != searchIterator) {
                while (searchIterator.hasNext()) {
                    Contact storedContact = searchIterator.next();
                    storedContacts.add(storedContact);
                    containsForeignContacts |= userID != storedContact.getCreatedBy();
                }
            }
        } finally {
            Tools.close(searchIterator);
        }
        /*
         * check delete permissions as required
         */
        if (0 < storedContacts.size()) {
            EffectivePermission permission = Tools.getPermission(contextID, folderID, userID);
            if (containsForeignContacts) {
                Check.canDeleteAll(permission, session, folderID);
            } else {
                Check.canDeleteOwn(permission, session, folderID);
            }
        }
        /*
         * delete contacts from storage
         */
        storage.delete(session, folderID);
        /*
         * broadcast events
         */
        final EventClient eventClient = new EventClient(session);
        final Date now = new Date();
        for (final Contact contact : storedContacts) {
            contact.setContextId(contextID);
            contact.setParentFolderID(parse(folderID));
            contact.setLastModified(now);
            contact.setModifiedBy(userID);
            eventClient.delete(contact, folder);
        }
    }

    @Override
    protected SearchIterator<Contact> doGetContacts(final boolean deleted, final Session session, final String folderID, final String[] ids,
        final ContactField[] fields, SortOptions sortOptions, final Date since) throws OXException {
        int contextID = session.getContextId();
        /*
         * check folder
         */
        FolderObject folder = Tools.getFolder(contextID, folderID);
        Check.isContactFolder(folder, session);
        /*
         * check general permissions
         */
        EffectivePermission permission = Tools.getPermission(session, folder);
        Check.canReadOwn(permission, session, folderID);
        /*
         * prepare fields and sort options
         */
        QueryFields queryFields = new QueryFields(fields);
        if (null == sortOptions) {
            sortOptions = SortOptions.EMPTY;
        }
        /*
         * get contacts from storage
         */
        ContactStorage storage = Tools.getStorage(session, folderID);
        SearchIterator<Contact> contacts = null;
        if (null != since) {
            contacts = deleted ? storage.deleted(session, folderID, since, queryFields.getFields(), sortOptions) :
                storage.modified(session, folderID, since, queryFields.getFields(), sortOptions);
        } else if (null != ids) {
            contacts = storage.list(session, folderID, ids, queryFields.getFields(), sortOptions);
        } else {
            contacts = storage.all(session, folderID, queryFields.getFields(), sortOptions);
        }
        if (null == contacts) {
            throw ContactExceptionCodes.UNEXPECTED_ERROR.create("got no results from storage");
        }
        /*
         * filter results respecting object permission restrictions, adding attachment info as needed
         */
        return new ResultIterator(contacts, queryFields.needsAttachmentInfo(), session, permission.canReadAllObjects());
    }

    @Override
    protected SearchIterator<Contact> doGetContacts(final Session session, List<String> folderIDs, ContactField[] fields, SortOptions sortOptions) throws OXException {
        int contextID = session.getContextId();
        int userID = session.getUserId();
        /*
         * check folders
         */
        if (null != folderIDs) {
            for (String folderID : folderIDs) {
                FolderObject folder = Tools.getFolder(contextID, folderID);
                Check.isContactFolder(folder, session);
                EffectivePermission permission = Tools.getPermission(session, folder);
                Check.canReadOwn(permission, session, folderID);
            }
        }
        /*
         * determine queried storages according to queried folders
         */
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, null == folderIDs ? Tools.getVisibleFolders(contextID, userID) : folderIDs);
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        final QueryFields queryFields = new QueryFields(fields);
        final SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            if (1 == queriedStorage.getValue().size()) {
                tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                    @Override
                    public SearchIterator<Contact> call() throws Exception {
                        return queriedStorage.getKey().all(session, queriedStorage.getValue().get(0), queryFields.getFields(), sOptions);
                    }
                });
            } else {
                final CompositeSearchTerm folderIDsTerm = new CompositeSearchTerm(CompositeOperation.OR);
                for (String folderID : queriedStorage.getValue()) {
                    folderIDsTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderID));
                }
                tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                    @Override
                    public SearchIterator<Contact> call() throws Exception {
                        return queriedStorage.getKey().search(session, folderIDsTerm, queryFields.getFields(), sOptions);
                    }
                });
            }
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected <O> SearchIterator<Contact> doSearchContacts(Session session, List<String> folderIds, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException {
        /*
         * check that no folder operands are defined in term
         */
        if (null != term && new SearchTermAnalyzer(term).hasFolderIDs()) {
            throw ContactExceptionCodes.UNEXPECTED_ERROR.create(new UnsupportedOperationException("no 'folder' operand in term allowed"));
        }
        /*
         * determine queried storages according to searched folders
         */
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, null != folderIds ? folderIds : Tools.getSearchFolders(session.getContextId(), session.getUserId(), false));
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        QueryFields queryFields = new QueryFields(fields);
        SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks, combining the term with extracted folder information for each storage
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            SearchTerm<?> searchTerm;
            if (null == term) {
                searchTerm = Tools.getFoldersTerm(queriedStorage.getValue());
            } else {
                searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(Tools.getFoldersTerm(queriedStorage.getValue())).addSearchTerm(term);
            }
            tasks.add(new AbstractTask<SearchIterator<Contact>>() {

                @Override
                public SearchIterator<Contact> call() throws Exception {
                    return queriedStorage.getKey().search(session, searchTerm, queryFields.getFields(), sOptions);
                }
            });
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected <O> SearchIterator<Contact> doSearchContacts(Session session, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException {
        int userID = session.getUserId();
        int contextID = session.getContextId();
        /*
         * analyze term
         */
        SearchTermAnalyzer termAnanlyzer = new SearchTermAnalyzer(term);
        /*
         * determine queried storages according to searched folders
         */
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, termAnanlyzer.hasFolderIDs() ? termAnanlyzer.getFolderIDs() : Tools.getSearchFolders(contextID, userID, false));
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        final QueryFields queryFields = new QueryFields(fields);
        final SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            final SearchTerm<?> searchTerm;
            if (termAnanlyzer.hasFolderIDs()) {
                /*
                 * leave term as is
                 */
                searchTerm = term;
            } else {
                /*
                 * combine term with extracted folder information for that storage
                 */
                CompositeSearchTerm combinedTerm = new CompositeSearchTerm(CompositeOperation.AND);
                combinedTerm.addSearchTerm(Tools.getFoldersTerm(queriedStorage.getValue()));
                combinedTerm.addSearchTerm(term);
                searchTerm = combinedTerm;
            }
            tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                @Override
                public SearchIterator<Contact> call() throws Exception {
                    return queriedStorage.getKey().search(session, searchTerm, queryFields.getFields(), sOptions);
                }
            });
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected SearchIterator<Contact> doSearchContacts(final Session session, final ContactSearchObject contactSearch, ContactField[] fields,
            SortOptions sortOptions) throws OXException {
        int userID = session.getUserId();
        int contextID = session.getContextId();
        /*
         * check supplied search
         */
        Check.validateSearch(contactSearch);

        /*
         * determine and filter search folders
         */
        List<String> folders = contactSearch.hasFolders() ? Tools.toStringList(contactSearch.getFolders()) : Tools.getSearchFolders(contextID, userID, contactSearch.isEmailAutoComplete());

        if (contactSearch.hasExcludeFolders()) {
            folders.removeAll(Tools.toStringList(contactSearch.getExcludeFolders()));
        }

        /*
         * determine queried storages according to searched folders
         */
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, folders);
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        final QueryFields queryFields = new QueryFields(fields);
        final SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            /*
             * use folders specific to this storage in each contact search
             */
            tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                @Override
                public SearchIterator<Contact> call() throws Exception {
                    return queriedStorage.getKey().search(
                        session, Tools.prepareContactSearch(contactSearch, queriedStorage.getValue()), queryFields.getFields(), sOptions);
                }
            });
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected String doGetOrganization(Session session) throws OXException {
        /*
         * prepare search term for context admin
         */
        int userID = Tools.getContext(session).getMailadmin();
        String folderID = Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID);
        ContactStorage storage = Tools.getStorage(session, folderID);
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
        searchTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderID));
        searchTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.INTERNAL_USERID, SingleOperation.EQUALS, Integer.valueOf(userID)));
        /*
         * search
         */
        Contact contact = null;
        SearchIterator<Contact> searchIterator = null;
        try {
            searchIterator = storage.search(session, searchTerm, new ContactField[] { ContactField.COMPANY });
            if (null != searchIterator && searchIterator.hasNext()) {
                contact = searchIterator.next();
            }
        } finally {
            Tools.close(searchIterator);
        }
        /*
         * extract organization
         */
        Check.contactNotNull(contact, session.getContextId(), userID);
        return contact.getCompany();
    }

    @Override
    protected <O> SearchIterator<Contact> doGetUsers(Session session, int[] userIDs, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException {
        return doGetUsers(session, Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID), userIDs, term, fields, sortOptions);
    }

    @Override
    protected <O> SearchIterator<Contact> doGetGuestUsers(Session session, int[] userIDs, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException {
        return doGetUsers(session, Integer.toString(FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID), userIDs, term, fields, sortOptions);
    }

    protected <O> SearchIterator<Contact> doGetUsers(Session session, String folderID, int[] userIDs, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException {
        int currentUserID = session.getUserId();
        int contextID = session.getContextId();
        ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * limit queried fields when necessary due to permissions
         */
        final EffectivePermission permission = Tools.getPermission(contextID, folderID, currentUserID);
        QueryFields queryFields;
        if (permission.canReadAllObjects() || null != userIDs && 1 == userIDs.length && currentUserID == userIDs[0]) {
            // no limitation
            queryFields = new QueryFields(fields);
        } else {
            // restrict queried fields
            if (Tools.getUserPermissionBits(session).hasWebMail()) {
                queryFields = new QueryFields(fields, LIMITED_USER_FIELDS);
            } else {
                queryFields = new QueryFields(fields, LIMITED_USER_FIELDS_NO_MAIL);
            }
        }
        if (null == sortOptions) {
            sortOptions = SortOptions.EMPTY;
        }
        /*
         * prepare search term for users
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
        searchTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderID));
        if (null == userIDs || 0 == userIDs.length) {
            searchTerm.addSearchTerm(Tools.createContactFieldTerm(
                ContactField.INTERNAL_USERID, SingleOperation.GREATER_THAN, Integer.valueOf(0)));
        } else if (1 == userIDs.length) {
            searchTerm.addSearchTerm(Tools.createContactFieldTerm(
                ContactField.INTERNAL_USERID, SingleOperation.EQUALS, Integer.valueOf(userIDs[0])));
        } else {
            CompositeSearchTerm userIDsTerm = new CompositeSearchTerm(CompositeOperation.OR);
            for (int userID : userIDs) {
                userIDsTerm.addSearchTerm(Tools.createContactFieldTerm(
                    ContactField.INTERNAL_USERID, SingleOperation.EQUALS, Integer.valueOf(userID)));
            }
            searchTerm.addSearchTerm(userIDsTerm);
        }
        if (null != term) {
            searchTerm.addSearchTerm(term);
        }
        /*
         * get user contacts from storage
         */
        return new ResultIterator(storage.search(session, searchTerm, queryFields.getFields(), sortOptions),
                queryFields.needsAttachmentInfo(), session, true);
    }

    @Override
    protected SearchIterator<Contact> doGetUsers(final Session session, final int[] userIDs, final ContactSearchObject contactSearch,
            final ContactField[] fields, SortOptions sortOptions) throws OXException {
        final int currentUserID = session.getUserId();
        final int contextID = session.getContextId();
        final String folderID = Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID);
        final ContactStorage storage = Tools.getStorage(session, folderID);
        /*
         * limit queried fields when necessary due to permissions
         */
        final EffectivePermission permission = Tools.getPermission(contextID, folderID, currentUserID);
        QueryFields queryFields;
        if (permission.canReadAllObjects() || null != userIDs && 1 == userIDs.length && currentUserID == userIDs[0]) {
            // no limitation
            queryFields = new QueryFields(fields);
        } else {
            // restrict queried fields
            if (Tools.getUserPermissionBits(session).hasWebMail()) {
                queryFields = new QueryFields(fields, LIMITED_USER_FIELDS);
            } else {
                queryFields = new QueryFields(fields, LIMITED_USER_FIELDS_NO_MAIL);
            }
        }
        if (null == sortOptions) {
            sortOptions = SortOptions.EMPTY;
        }
        /*
         * get user contacts from storage
         */
        return new ResultIterator(storage.search(session, contactSearch, queryFields.getFields(), sortOptions),
                queryFields.needsAttachmentInfo(), session, true);
    }

    @Override
    protected SearchIterator<Contact> doSearchContactsWithBirthday(final Session session, final Date from, final Date until, List<String> folderIDs, ContactField[] fields, SortOptions sortOptions) throws OXException {
        int userID = session.getUserId();
        int contextID = session.getContextId();
        /*
         * determine queried storages according to searched folders
         */
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, null == folderIDs ? Tools.getVisibleFolders(contextID, userID) : folderIDs);
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        final QueryFields queryFields = new QueryFields(fields);
        final SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            /*
             * use folders specific to this storage in each contact search
             */
            tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                @Override
                public SearchIterator<Contact> call() throws Exception {
                    return queriedStorage.getKey().searchByBirthday(
                        session, queriedStorage.getValue(), from, until, queryFields.getFields(), sOptions);
                }
            });
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected SearchIterator<Contact> doSearchContactsWithAnniversary(final Session session, final Date from, final Date until, List<String> folderIDs, ContactField[] fields, SortOptions sortOptions) throws OXException {
        int userID = session.getUserId();
        int contextID = session.getContextId();
        /*
         * determine queried storages according to searched folders
         */
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, null == folderIDs ? Tools.getVisibleFolders(contextID, userID) : folderIDs);
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        final QueryFields queryFields = new QueryFields(fields);
        final SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            /*
             * use folders specific to this storage in each contact search
             */
            tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                @Override
                public SearchIterator<Contact> call() throws Exception {
                    return queriedStorage.getKey().searchByAnniversary(
                        session, queriedStorage.getValue(), from, until, queryFields.getFields(), sOptions);
                }
            });
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected SearchIterator<Contact> doAutocompleteContacts(final Session session, List<String> folderIDs, final String query, final AutocompleteParameters parameters, ContactField[] fields, SortOptions sortOptions) throws OXException {
        int userID = session.getUserId();
        int contextID = session.getContextId();
        parameters.put(AutocompleteParameters.USER_ID, I(userID));
        /*
         * check supplied search
         */
        Search.checkPatternLength(query);
        /*
         * determine queried storages according to searched folders
         */
        List<String> searchFolders = null != folderIDs && 0 < folderIDs.size() ? folderIDs : Tools.getSearchFolders(contextID, userID, true);
        if (null == searchFolders || 0 == searchFolders.size()) {
            return SearchIteratorAdapter.emptyIterator();
        }
        Map<ContactStorage, List<String>> queriedStorages = Tools.getStorages(session, searchFolders);
        Check.hasStorages(queriedStorages);
        /*
         * prepare fields and sort options
         */
        final QueryFields queryFields = new QueryFields(fields);
        final SortOptions sOptions = null != sortOptions ? sortOptions : SortOptions.EMPTY;
        /*
         * create tasks
         */
        List<AbstractTask<SearchIterator<Contact>>> tasks = new ArrayList<AbstractTask<SearchIterator<Contact>>>(queriedStorages.size());
        for (final Entry<ContactStorage, List<String>> queriedStorage : queriedStorages.entrySet()) {
            /*
             * use folders specific to this storage in each contact search
             */
            tasks.add(new AbstractTask<SearchIterator<Contact>>() {
                @Override
                public SearchIterator<Contact> call() throws Exception {
                    return queriedStorage.getKey().autoComplete(
                        session, queriedStorage.getValue(), query, parameters, queryFields.getFields(), sOptions);
                }
            });
        }
        /*
         * get results, filtered respecting object permission restrictions, adding attachment info as needed
         */
        return perform(tasks, session, queryFields.needsAttachmentInfo(), sOptions);
    }

    @Override
    protected int doCountContacts(Session session, String folderId) throws OXException {
        int contextId = session.getContextId();
        /*
         * check folder
         */
        FolderObject folder = Tools.getFolder(contextId, folderId);
        Check.isContactFolder(folder, session);
        /*
         * check general permissions
         */
        EffectivePermission permission = Tools.getPermission(session, folder);
        if (!permission.canReadOwnObjects()) {
            return 0;
        }
        /*
         * get contacts from storage
         */
        ContactStorage storage = Tools.getStorage(session, folderId);
        return storage.count(session, folderId, permission.canReadAllObjects());
    }

    @Override
    protected boolean doCheckIfFolderIsEmpty(Session session, String folderID) throws OXException {
        /*
         * prepare search term for folder
         */
        ContactStorage storage = Tools.getStorage(session, folderID);
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
        searchTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderID));
        /*
         * search using a limit of 1
         */
        SearchIterator<Contact> searchIterator = null;
        try {
            searchIterator = storage.search(session, searchTerm, new ContactField[] { ContactField.OBJECT_ID }, new SortOptions(0, 1));
            return null == searchIterator || false == searchIterator.hasNext();
        } finally {
            Tools.close(searchIterator);
        }
    }

    @Override
    protected boolean doCheckIfFolderContainsForeignObjects(Session session, String folderID) throws OXException {
        /*
         * prepare search term for folder
         */
        ContactStorage storage = Tools.getStorage(session, folderID);
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND);
        searchTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.FOLDER_ID, SingleOperation.EQUALS, folderID));
        searchTerm.addSearchTerm(Tools.createContactFieldTerm(ContactField.CREATED_BY, SingleOperation.NOT_EQUALS, Integer.valueOf(session.getUserId())));
        /*
         * search using a limit of 1
         */
        SearchIterator<Contact> searchIterator = null;
        try {
            searchIterator = storage.search(session, searchTerm, new ContactField[] { ContactField.OBJECT_ID }, new SortOptions(0, 1));
            return null != searchIterator && searchIterator.hasNext();
        } finally {
            Tools.close(searchIterator);
        }
    }

    @Override
    protected Map<String, SequenceResult> doGetSequenceNumbers(Session session, List<String> folderIds, String[] ignore) throws OXException {
        SortOptions sortOptions = new SortOptions(ContactField.LAST_MODIFIED, Order.DESCENDING);
        sortOptions.setLimit(1);
        ContactField[] fields = { ContactField.OBJECT_ID, ContactField.LAST_MODIFIED };
        Map<String, SequenceResult> sequenceResults = new LinkedHashMap<String, SequenceResult>(folderIds.size());
        SearchIterator<Contact> iterator = null;
        for (String folderId : folderIds) {
            long timestamp = 0L;
            try {
                iterator = doGetContacts(false, session, folderId, null, fields, sortOptions, null);
                while (iterator.hasNext()) {
                    Date lastModified = iterator.next().getLastModified();
                    if (null != lastModified && lastModified.getTime() > timestamp) {
                        timestamp = lastModified.getTime();
                    }
                }
            } finally {
                SearchIterators.close(iterator);
            }
            try {
                iterator = doGetContacts(true, session, folderId, null, fields, sortOptions, 0L < timestamp ? new Date(timestamp) : null);
                while (iterator.hasNext()) {
                    Date lastModified = iterator.next().getLastModified();
                    if (null != lastModified && lastModified.getTime() > timestamp) {
                        timestamp = lastModified.getTime();
                    }
                }
            } finally {
                SearchIterators.close(iterator);
            }
            long totalCount = -1L;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "count")) {
                totalCount = doCountContacts(session, folderId);
            }
            sequenceResults.put(folderId, new DefaultSequenceResult(timestamp, totalCount));
        }
        return sequenceResults;
    }

    @Override
    protected Map<String, UpdatesResult<Contact>> doGetUpdatedContacts(Session session, List<String> folderIds, Date since, ContactField[] fields, String[] ignore) throws OXException {
        Function<Contact, Long> timestampFunction = c -> null != c.getLastModified() ? L(c.getLastModified().getTime()) : null;
        Map<String, UpdatesResult<Contact>> updatesResults = new LinkedHashMap<String, UpdatesResult<Contact>>(folderIds.size());
        for (String folderId : folderIds) {
            List<Contact> newAndModifiedContacts = null;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "changed")) {
                newAndModifiedContacts = SearchIterators.asList(getModifiedContacts(session, folderId, since, fields));
            }
            List<Contact> deletedContacts = null;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "deleted")) {
                deletedContacts = SearchIterators.asList(getDeletedContacts(session, folderId, since, fields));
            }
            long totalCount = -1L;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "count")) {
                totalCount = doCountContacts(session, folderId);
            }
            updatesResults.put(folderId, new DefaultUpdatesResult<Contact>(newAndModifiedContacts, deletedContacts, timestampFunction, totalCount));
        }
        return updatesResults;
    }

    private void beforeUserUpdate(Context context, Contact userContact) throws OXException {
        for (UserServiceInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            interceptor.beforeUpdate(context, null, userContact, UserServiceInterceptor.EMPTY_PROPS);
        }
    }

    private void afterUserUpdate(Context context, Contact userContact) throws OXException {
        for (UserServiceInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            interceptor.afterUpdate(context, null, userContact, UserServiceInterceptor.EMPTY_PROPS);
        }
    }

    /**
     * Performs a list of tasks returning a search iterator of contacts and merges the results into a single search iterator, respecting
     * the supplied sort options. Multiple tasks are executed concurrently, while a single task is invoked on the current thread.
     *
     * @param tasks The tasks to execute
     * @param session The session
     * @param needsAttachmentInfo <code>true</code>, whether the result iterator needs attachment information, <code>false</code>,
     *        otherwise
     * @param sortOptions The sort options or use, or <code>null</code> if not needed
     * @return The combined iterator
     * @throws OXException
     */
    private static SearchIterator<Contact> perform(List<AbstractTask<SearchIterator<Contact>>> tasks, Session session,
        boolean needsAttachmentInfo, SortOptions sortOptions) throws OXException {
        try {
            if (null == tasks || 0 == tasks.size()) {
                /*
                 * no tasks, no results
                 */
                List<Contact> emptyList = Collections.emptyList();
                return new SearchIteratorAdapter<Contact>(emptyList.iterator(), 0);
            } else if (1 == tasks.size()) {
                /*
                 * one task, execute locally
                 */
                return new ResultIterator(tasks.get(0).call(), needsAttachmentInfo, session);
            } else {
                /*
                 * multiple tasks, invoke in executor...
                 */
                List<SearchIterator<Contact>> searchIterators = new ArrayList<SearchIterator<Contact>>();
                ExecutorService executor = ContactServiceLookup.getService(ThreadPoolService.class).getExecutor();
                for (Future<SearchIterator<Contact>> future : executor.invokeAll(tasks)) {
                    searchIterators.add(new ResultIterator(future.get(), needsAttachmentInfo, session));
                }
                /*
                 * ... and merge results, respecting the sort options
                 */
                return new ContactMergerator(Tools.getComparator(sortOptions), searchIterators);
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            if (null != e.getCause() && (e.getCause() instanceof OXException)) {
                throw (OXException)e.getCause();
            }
            throw ContactExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Session session, String folderID, ContactField... fields) throws OXException {
        final ContactStorage storage = Tools.getStorage(session, folderID);

        return storage.supports(fields);
    }

}
