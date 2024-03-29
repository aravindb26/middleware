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

package com.openexchange.contact;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.search.SearchTerm;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * {@link ContactService} - Provides access to the contact module.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
@SingletonService
public interface ContactService {

    /**
     * Contact fields that may be queried from contacts of the global address
     * list, even if the current session's user has no sufficient access
     * permissions for that folder.
     */
    static final ContactField[] LIMITED_USER_FIELDS = new ContactField[] { ContactField.DISPLAY_NAME, ContactField.GIVEN_NAME,
        ContactField.SUR_NAME, ContactField.MIDDLE_NAME, ContactField.SUFFIX, ContactField.LAST_MODIFIED,
        ContactField.INTERNAL_USERID, ContactField.OBJECT_ID, ContactField.FOLDER_ID, ContactField.UID, ContactField.EMAIL1
    };

    /**
     * Contact fields that may be queried from contacts of the global address
     * list, even if the current session's user has no sufficient access
     * permissions for that folder, and the user has no 'webmail' module access.
     */
    static final ContactField[] LIMITED_USER_FIELDS_NO_MAIL = com.openexchange.tools.arrays.Arrays.remove(LIMITED_USER_FIELDS, ContactField.EMAIL1);

    /**
     * Gets a contact with all fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param id the object ID
     * @return the contact
     * @throws OXException
     */
    Contact getContact(Session session, String folderId, String id) throws OXException;

    /**
     * Gets a contact with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param id the object ID
     * @param fields the contact fields that should be retrieved
     * @return the contact
     * @throws OXException
     */
    Contact getContact(Session session, String folderId, String id, ContactField[] fields) throws OXException;

    /**
     * Gets all contacts with all fields in a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllContacts(Session session, String folderId) throws OXException;

    /**
     * Gets all contacts with all fields in a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllContacts(Session session, String folderId, SortOptions sortOptions) throws OXException;

    /**
     * Gets all contacts with specified fields in a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param fields the contact fields that should be retrieved
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllContacts(Session session, String folderId, ContactField[] fields) throws OXException;

    /**
     * Gets all contacts with specified fields in a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllContacts(Session session, String folderId, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Gets all contacts from multiple folders.
     *
     * @param session the session
     * @param folderIDs the IDs of the parent folders
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllContacts(Session session, List<String> folderIDs, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Gets all contacts from all visible folders.
     *
     * @param session the session
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllContacts(Session session, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Counts all contacts within the given folder.
     *
     * @param session the session
     * @param folderId ID of the folder to count in
     * @return the number of contacts
     * @throws OXException
     */
    int countContacts(Session session, String folderId) throws OXException;

    /**
     * Gets a list of contacts with all fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param ids the object IDs
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getContacts(Session session, String folderId, String[] ids) throws OXException;

    /**
     * Gets a list of contacts with all fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param ids the object IDs
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getContacts(Session session, String folderId, String[] ids, SortOptions sortOptions) throws OXException;

    /**
     * Gets a list of contacts with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param ids the object IDs
     * @param fields the contact fields that should be retrieved
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getContacts(Session session, String folderId, String[] ids, ContactField[] fields) throws OXException;

    /**
     * Gets a list of contacts with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param ids the object IDs
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getContacts(Session session, String folderId, String[] ids, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Gets a list of deleted contacts in a folder with all fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param since the exclusive minimum deletion time to consider
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getDeletedContacts(Session session, String folderId, Date since) throws OXException;

    /**
     * Gets a list of deleted contacts in a folder with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param since the exclusive minimum deletion time to consider
     * @param fields the contact fields that should be retrieved
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getDeletedContacts(Session session, String folderId, Date since, ContactField[] fields) throws OXException;

    /**
     * Gets a list of deleted contacts in a folder with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param since the exclusive minimum deletion time to consider
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getDeletedContacts(Session session, String folderId, Date since, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Gets a list of modified contacts in a folder with all fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param since the exclusive minimum modification time to consider
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getModifiedContacts(Session session, String folderId, Date since) throws OXException;

    /**
     * Gets a list of modified contacts in a folder with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param since the exclusive minimum modification time to consider
     * @param fields the contact fields that should be retrieved
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getModifiedContacts(Session session, String folderId, Date since, ContactField[] fields) throws OXException;

    /**
     * Gets a list of modified contacts in a folder with specified fields.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param since the exclusive minimum modification time to consider
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getModifiedContacts(Session session, String folderId, Date since, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param term the search term
     * @return the contacts found with the search
     * @throws OXException
     * @deprecated Use {@link #searchContacts(Session, List, SearchTerm, ContactField[], SortOptions)} and explicitly specify the searched folders.
     */
    @Deprecated
    <O> SearchIterator<Contact> searchContacts(Session session, SearchTerm<O> term) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param term the search term
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     * @deprecated Use {@link #searchContacts(Session, List, SearchTerm, ContactField[], SortOptions)} and explicitly specify the searched folders.
     */
    @Deprecated
    <O> SearchIterator<Contact> searchContacts(Session session, SearchTerm<O> term, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param term the search term
     * @param fields the contact fields that should be retrieved
     * @return the contacts found with the search
     * @throws OXException
     * @deprecated Use {@link #searchContacts(Session, List, SearchTerm, ContactField[], SortOptions)} and explicitly specify the searched folders.
     */
    @Deprecated
    <O> SearchIterator<Contact> searchContacts(Session session, SearchTerm<O> term, ContactField[] fields) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param term the search term
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     * @deprecated Use {@link #searchContacts(Session, List, SearchTerm, ContactField[], SortOptions)} and explicitly specify the searched folders.
     */
    @Deprecated
    <O> SearchIterator<Contact> searchContacts(Session session, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param folderIds The identifiers of the folders to perform the search in, or <code>null</code> to search in all folders
     * @param session the session
     * @param term The search term, or <code>null</code> to lookup all contacts contained in the specified folders
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    <O> SearchIterator<Contact> searchContacts(Session session, List<String> folderIds, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param contactSearch the contact search object
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContacts(Session session, ContactSearchObject contactSearch) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param contactSearch the contact search object
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContacts(Session session, ContactSearchObject contactSearch, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param contactSearch the contact search object
     * @param fields the contact fields that should be retrieved
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContacts(Session session, ContactSearchObject contactSearch, ContactField[] fields) throws OXException;

    /**
     * Searches for contacts.
     *
     * @param session the session
     * @param contactSearch the contact search object
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContacts(Session session, ContactSearchObject contactSearch, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts whose birthday falls into the specified period.
     *
     * @param session the session
     * @param from The lower (inclusive) limit of the requested time-range
     * @param until The upper (exclusive) limit of the requested time-range
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContactsWithBirthday(Session session, Date from, Date until, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts whose birthday falls into the specified period.
     *
     * @param session the session
     * @param folderIDs the IDs of the parent folders
     * @param from The lower (inclusive) limit of the requested time-range
     * @param until The upper (exclusive) limit of the requested time-range
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContactsWithBirthday(Session session, List<String> folderIDs, Date from, Date until, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts whose anniversary falls into the specified period.
     *
     * @param session the session
     * @param from The lower (inclusive) limit of the requested time-range
     * @param until The upper (exclusive) limit of the requested time-range
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContactsWithAnniversary(Session session, Date from, Date until, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for contacts whose anniversary falls into the specified period.
     *
     * @param session the session
     * @param folderIDs the IDs of the parent folders
     * @param from The lower (inclusive) limit of the requested time-range
     * @param until The upper (exclusive) limit of the requested time-range
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchContactsWithAnniversary(Session session, List<String> folderIDs, Date from, Date until, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Performs an "auto-complete" lookup for contacts.
     *
     * @param session The session
     * @param folderIDs A list of folder IDs to restrict the search to
     * @param query The search query as supplied by the client
     * @param parameters The additional parameters to refine the auto-complete search. Don't pass <code>null</code> here,
     *            but use an empty instance to use the default parameter values.
     * @param fields The contact fields that should be retrieved
     * @param sortOptions The options to sort the results
     * @return The contacts found with the search
     * @throws OXException
     *
     * @see {@link AutocompleteParameters#newInstance()}
     */
    SearchIterator<Contact> autocompleteContacts(Session session, List<String> folderIDs, String query, AutocompleteParameters parameters, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Performs an "auto-complete" lookup for contacts. Depending <code>com.openexchange.contacts.allFoldersForAutoComplete</code>, either
     * all folders visible to the user, or a reduced set of specific folders is used for the search.
     *
     * @param session The session
     * @param query The search query as supplied by the client
     * @param parameters The additional parameters to refine the auto-complete search. Don't pass <code>null</code> here,
     *            but use an empty instance to use the default parameter values.
     * @param fields The contact fields that should be retrieved
     * @param sortOptions The options to sort the results
     * @return The contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> autocompleteContacts(Session session, String query, AutocompleteParameters parameters, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Creates a new contact in a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param contact the contact to create
     * @throws OXException
     */
    void createContact(Session session, String folderId, Contact contact) throws OXException;

    /**
     * Updates a contact.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param id the object ID
     * @param contact the contact to update
     * @param lastRead the time the object was last read from the storage
     * @throws OXException
     */
    void updateContact(Session session, String folderId, String id, Contact contact, Date lastRead) throws OXException;

    /**
     * Updates a user's contact data, ignoring the folder permissions of the
     * global address book folder. Required to update user data in environments
     * where access to the global address book is restricted.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param id the object ID
     * @param contact the contact to update
     * @param lastRead the time the object was last read from the storage
     * @throws OXException
     */
    void updateUser(Session session, String folderId, String id, Contact contact, Date lastRead) throws OXException;

    /**
     * Deletes a contact.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param id the object ID
     * @param lastRead the time the object was last read from the storage
     * @throws OXException
     */
    void deleteContact(Session session, String folderId, String id, Date lastRead) throws OXException;

    /**
     * Deletes all contacts in a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @throws OXException
     */
    void deleteContacts(Session session, String folderId) throws OXException;

    /**
     * Deletes multiple contacts from a folder.
     *
     * @param session the session
     * @param folderId the ID of the parent folder
     * @param ids the object IDs
     * @param lastRead the time the objects were last read from the storage
     * @throws OXException
     */
    void deleteContacts(Session session, String folderId, String[] ids, Date lastRead) throws OXException;

    /**
     * Gets a user's contact with all fields.<p>
     *
     * If the current user has no adequate permissions, no exception is thrown,
     * but the queried contact fields are limited to fields defined by
     * <code>ContactService.LIMITED_USER_FIELDS</code>.
     *
     * @param session the session
     * @param userID the user's ID
     * @return the contact
     * @throws OXException
     */
    Contact getUser(Session session, int userID) throws OXException;

    /**
     * Gets a user's contact with specified fields.<p>
     *
     * If the current user has no adequate permissions, no exception is thrown,
     * but the queried contact fields are limited to the fields defined by
     * <code>ContactService.LIMITED_USER_FIELDS</code>.
     *
     * @param session the session
     * @param userID the user's ID
     * @param fields the contact fields that should be retrieved
     * @return the contact
     * @throws OXException
     */
    Contact getUser(Session session, int userID, ContactField[] fields) throws OXException;

    /**
     * Gets user contacts with all fields.<p>
     *
     * If the current user has no adequate permissions, no exception is thrown,
     * but the queried contact fields are limited to the fields defined by
     * <code>ContactService.LIMITED_USER_FIELDS</code>.
     *
     * @param session the session
     * @param userIDs the user IDs
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getUsers(Session session, int[] userIDs) throws OXException;

    /**
     * Gets user contacts with specified fields.<p>
     *
     * If the current user has no adequate permissions, no exception is thrown,
     * but the queried contact fields are limited to the fields defined by
     * <code>ContactService.LIMITED_USER_FIELDS</code>.
     *
     * @param session the session
     * @param userIDs the user IDs
     * @param fields the contact fields that should be retrieved
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getUsers(Session session, int[] userIDs, ContactField[] fields) throws OXException;

    /**
     * Gets guest user contacts with specified fields.<p>
     *
     * If the current user has no adequate permissions, no exception is thrown,
     * but the queried contact fields are limited to the fields defined by
     * <code>ContactService.LIMITED_USER_FIELDS</code>.
     *
     * @param session the session
     * @param userIDs the guest user IDs
     * @param fields the contact fields that should be retrieved
     * @return the guest contacts
     * @throws OXException
     */
    SearchIterator<Contact> getGuestUsers(Session session, int[] userIDs, ContactField[] fields) throws OXException;

    /**
     * Gets all user contacts with specified fields.<p>
     *
     * If the current user has no adequate permissions, no exception is thrown,
     * but the queried contact fields are limited to the fields defined by
     * <code>ContactService.LIMITED_USER_FIELDS</code>.
     *
     * @param session the session
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the contacts
     * @throws OXException
     */
    SearchIterator<Contact> getAllUsers(Session session, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Gets the value of the <code>ContactField.COMPANY</code> field from the
     * contact representing the current context's mail admin.
     *
     * @param session the session
     * @return the organization
     * @throws OXException
     */
    String getOrganization(Session session) throws OXException;

    /**
     * Searches for users.
     *
     * @param session the session
     * @param term the search term
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the user contacts found with the search
     * @throws OXException
     */
    <O> SearchIterator<Contact> searchUsers(Session session, SearchTerm<O> term, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Searches for users.
     *
     * @param session the session
     * @param contactSearch the contact search object
     * @param fields the contact fields that should be retrieved
     * @param sortOptions the options to sort the results
     * @return the user contacts found with the search
     * @throws OXException
     */
    SearchIterator<Contact> searchUsers(Session session, ContactSearchObject contactSearch, ContactField[] fields, SortOptions sortOptions) throws OXException;

    /**
     * Gets a value indicating if the folder with the supplied identifier is empty.
     *
     * @param session The session
     * @param folderID The ID of the folder to check
     * @return <code>true</code> if the folder is empty, <code>false</code>, otherwise.
     * @throws OXException
     */
    boolean isFolderEmpty(Session session, String folderID) throws OXException;

    /**
     * Gets a value indicating if the folder with the supplied identifier contains foreign objects, i.e. contacts that were not created
     * by the current session's user.
     *
     * @param session The session
     * @param folderID The ID of the folder to check
     * @return <code>true</code> if the folder contains foreign objects, <code>false</code>, otherwise.
     * @throws OXException
     */
    boolean containsForeignObjectInFolder(Session session, String folderID) throws OXException;

    /**
     * Returns if the provided {@link ContactField}s are supported by the storage. To 'support' the given field the storage should be able to set new values for it. If at least one of the provided fields is not supported <code>false</code> will be
     * returned.
     *
     * @param session The session
     * @param folderID The ID of the folder to check
     * @param fields the contact fields that should be checked for support
     * @return <code>true</code> if all fields are supported; <code>false</code> if at least one is not supported
     * @throws OXException
     */
    boolean supports(Session session, String folderID, ContactField... fields) throws OXException;

    /**
     * Gets lists of new and updated as well as deleted contacts since a specific timestamp in certain folders.
     * 
     * @param session The session
     * @param folderIds The identifiers of the folder to get the updates from
     * @param since The timestamp since when the updates should be returned
     * @param fields The contact fields to retrieve
     * @param ignore An optional array containing "changed", "deleted" and/or "count" to skip certain aspects in the result
     * @return The updates results, mapped to the corresponding folder ids
     */
    Map<String, UpdatesResult<Contact>> getUpdatedContacts(Session session, List<String> folderIds, Date since, ContactField[] fields, String[] ignore) throws OXException;

    /**
     * Gets the sequence numbers of certain contacts folders, which is the highest timestamp of all contained items. Distinct object access
     * permissions (e.g. <i>read own</i>) are not considered. Additionally, the actual item count in each of the folders is returned,
     * aiding proper detection of removed items during incremental synchronizations.
     * 
     * @param session The session
     * @param folderIds The identifiers of the folders to get the sequence number for
     * @param ignore An optional array containing "count" to skip certain aspects in the results
     * @return The sequence number results, mapped to the corresponding folder ids
     */
    Map<String, SequenceResult> getSequenceNumbers(Session session, List<String> folderIds, String[] ignore) throws OXException;

}
