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

package com.openexchange.contact.provider.composition;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.common.AccountAwareContactsFolder;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.common.GroupwareFolderType;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.search.SearchTerm;
import com.openexchange.session.Session;
import com.openexchange.tx.TransactionAware;

/**
 * {@link IDBasedContactsAccess}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public interface IDBasedContactsAccess extends TransactionAware, ContactsParameters {

    /**
     * Gets the session associated with this contacts access instance.
     *
     * @return The session the access was initialized for
     */
    Session getSession();

    /**
     * Gets a list of warnings that occurred during processing.
     *
     * @return A list if warnings, or an empty list if there were none
     */
    List<OXException> getWarnings();

    /**
     * Gets a reference to the access for internal users.
     * <p/>
     * The parameters of this access are taken over implicitly.
     *
     * @return The user access reference
     */
    IDBasedUserAccess getUserAccess();

    /**
     * Creates a new contact
     *
     * @param folderId The fully qualified identifier of the parent folder to create the contact in
     * @param contact The contact to create
     * @throws OXException if an error is occurred
     */
    void createContact(String folderId, Contact contact) throws OXException;

    /**
     * Updates an existing contact.
     *
     * @param contactId The contact identifier
     * @param contact The contact data to update
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @throws OXException if an error is occurred
     */
    void updateContact(ContactID contactId, Contact contact, long clientTimestamp) throws OXException;

    /**
     * Deletes an existing contact.
     *
     * @param contactId The contact identifier
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @throws OXException if an error is occurred
     */
    void deleteContact(ContactID contactId, long clientTimestamp) throws OXException;

    /**
     * Deletes multiple contacts.
     *
     * @param contactIds The contact identifiers
     * @param clientTimestamp The last know timestamp by the client
     * @throws OXException if an error is occurred
     */
    void deleteContacts(List<ContactID> contactsIds, long clientTimestamp) throws OXException;

    /**
     * Gets a specific contact
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param contactId The identifier of the contact to get
     *
     * @return The contact
     * @throws OXException if an error is occurred
     */
    Contact getContact(ContactID contactId) throws OXException;

    /**
     * Gets a list of contacts with the specified identifiers.
     *
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param contactIDs A list of the identifiers of the contacts to get
     * @return The contacts
     * @throws OXException if an error is occurred
     */
    List<Contact> getContacts(List<ContactID> contactIDs) throws OXException;

    /**
     * Gets all contacts in a specific contacts folder.
     *
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to get the contacts from
     * @return The contacts
     * @throws OXException if an error is occurred
     */
    List<Contact> getContacts(String folderId) throws OXException;

    /**
     * Gets a list of modified contacts in the specified folder
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     *
     * @param folderId the folder identifier
     * @param from Specifies the lower inclusive limit of the queried range, i.e. only
     *            contacts modified on or after this date should be returned.
     * @return The list of modified contacts
     * @throws OXException if an error is occurred
     */
    List<Contact> getModifiedContacts(String folderId, Date from) throws OXException;

    /**
     * Gets a list of deleted contacts in the specified folder.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     *
     * @param folderId the folder identifier
     * @param from Specifies the lower inclusive limit of the queried range, i.e. only
     *            contacts deleted on or after this date should be returned.
     * @return The list of deleted contacts
     * @throws OXException if an error is occurred
     */
    List<Contact> getDeletedContacts(String folderId, Date from) throws OXException;
    
    /**
     * Gets lists of new and updated as well as deleted contacts since a specific timestamp in certain folders.
     * <p/>
     * <b>Note:</b> Only available for {@link SyncAware} contacts providers.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_IGNORE} ("changed", "deleted", "count")</li>
     * </ul>
     * 
     * @param folderIds The identifiers of the folder to get the updates from
     * @param since The timestamp since when the updates should be returned
     * @return The updates results, mapped to the corresponding folder ids
     */
    Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String> folderIds, Date since) throws OXException;

    /**
     * Gets the sequence numbers of certain contacts folders, which is the highest timestamp of all contained items. Distinct object access
     * permissions (e.g. <i>read own</i>) are not considered. Additionally, the actual item count in each of the folders is returned,
     * aiding proper detection of removed items during incremental synchronizations.
     * <p/>
     * <b>Note:</b> Only available for {@link SyncAware} contacts providers.
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_IGNORE} ("count")</li>
     * </ul>
     * 
     * @param folderIds The identifiers of the folders to get the sequence number for
     * @return The sequence number results, mapped to the corresponding folder ids
     */
    Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException;

    /**
     * Retrieves the CTag (Collection Entity Tag) for a folder.
     * <p/>
     * The CTag is like a resource ETag and changes each time something within the folder has changed. This allows clients to quickly
     * determine whether it needs to synchronize any changed contents or not.
     * <p/>
     * <b>Note:</b> Only available for {@link CTagAware} contacts providers.
     *
     * @param folderID The fully qualified identifier of the folder
     * @return the CTag for this folder
     */
    String getCTag(String folderId) throws OXException;

    ///////////////////////////////////// FOLDERS ////////////////////////////////////

    /**
     * Gets a list of all visible contacts folders.
     *
     * @param type The type to get the visible folders for
     * @return A list of all visible contacts folders of the type
     * @throws OXException if an error is occurred
     */
    List<AccountAwareContactsFolder> getVisibleFolders(GroupwareFolderType type) throws OXException;

    /**
     * Gets multiple contacts folders.
     *
     * @param folderIds The fully qualified identifiers of the folders to get
     * @return The contacts folders (including information about the underlying account)
     * @throws OXException if an error is occurred
     */
    List<AccountAwareContactsFolder> getFolders(List<String> folderIds) throws OXException;

    /**
     * Gets a specific contacts folder.
     *
     * @param folderId The fully qualified identifier of the folder to get
     * @return The contacts folder (including information about the underlying account)
     * @throws OXException if an error is occurred
     */
    AccountAwareContactsFolder getFolder(String folderId) throws OXException;

    /**
     * Gets the user's default contacts folder.
     *
     * @return The default contacts folder
     * @throws OXException if an error is occurred
     */
    ContactsFolder getDefaultFolder() throws OXException;

    /**
     * Create a new contacts folder.
     * <p/>
     * Depending on the capabilities of the targeted contacts provider, either a new subfolder is created within an existing contacts
     * account (of a {@link ContactsFolderProvider}), or a new contacts account representing a contacts subscription (of a
     * {@link BasicContactsProvider}) is created implicitly, resulting in a new virtual folder.
     *
     * @param providerId The fully qualified identifier of the parent folder, or <code>null</code> if not needed
     * @param folder contacts folder data to take over for the new contacts account
     * @param userConfig Arbitrary user configuration data for the new contacts account, or <code>null</code> if not needed
     * @return The fully qualified identifier of the newly created folder
     * @throws OXException if an error is occurred
     */
    String createFolder(String providerId, ContactsFolder folder, JSONObject userConfig) throws OXException;

    /**
     * Updates a contacts folder.
     * <p/>
     * Depending on the capabilities of the underlying contacts provider, also arbitrary account properties can be updated.
     *
     * @param folderId The fully qualified identifier of the folder to update
     * @param folder The updated contacts folder data
     * @param userConfig Arbitrary user configuration data for the contacts account, or <code>null</code> if not needed
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The (possibly changed) fully qualified identifier of the updated folder
     * @throws OXException if an error is occurred
     */
    String updateFolder(String folderId, ContactsFolder folder, JSONObject userConfig, long clientTimestamp) throws OXException;

    /**
     * Deletes a contacts folder.
     *
     * @param folderId The fully qualified identifier of the folder to delete
     * @throws OXException if an error is occurred
     */
    void deleteFolder(String folderId, long clientTimestamp) throws OXException;

    /**
     * Returns if the provided {@link ContactField}s are supported by the storage. To 'support' the given field the storage
     * should be able to set new values for it. If at least one of the provided fields is not supported <code>false</code> will be
     * returned.
     *
     * @param folderId The ID of the folder to check
     * @param fields the contact fields that should be checked for support
     * @return <code>true</code> if all fields are supported; <code>false</code> if at least one is not supported
     * @throws OXException if an error is occurred
     */
    boolean supports(String folderId, ContactField... fields) throws OXException;

    ////////////////////////////////////// SEARCH ///////////////////////////////////////

    /**
     * Searches for contacts.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * <li>{@link ContactsParameters#PARAMETER_INCLUDE_UNSUBSCRIBED_FOLDERS}</li>
     * <li>{@link ContactsParameters#PARAMETER_PICKER_FOLDERS_ONLY}</li>
     * </ul>
     *
     * @param folderIds The identifiers of the folders to perform the search in, or <code>null</code> to search in all folders
     * @param term The search term, or <code>null</code> to lookup all contacts contained in the specified folders
     * @return The contacts found with the search
     * @throws OXException if an error is occurred
     */
    <O> List<Contact> searchContacts(List<String> folderIds, SearchTerm<O> term) throws OXException;

    /**
     * Searches for contacts.
     * <p/>
     * <b>Note:</b> The search is only performed in the default <i>internal</i> groupware account.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * </ul>
     *
     * @param contactSearch the contact search object
     * @return the contacts found with the search
     * @throws OXException if an error is occurred
     */
    List<Contact> searchContacts(ContactsSearchObject contactSearch) throws OXException;

    /**
     * Performs an "auto-complete" lookup for contacts.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * <li>{@link ContactsParameters#PARAMETER_RIGHT_HAND_LIMIT}</li>
     * <li>{@link ContactsParameters#PARAMETER_REQUIRE_EMAIL}</li>
     * <li>{@link ContactsParameters#PARAMETER_IGNORE_DISTRIBUTION_LISTS}</li>
     * </ul>
     *
     * @param folderIds The identifiers of the folders to perform the search in, or <code>null</code> to search in all folders
     * @param query The search query as supplied by the client
     * @return The resulting contacts
     */
    List<Contact> autocompleteContacts(List<String> folderIds, String query) throws OXException;

    /**
     * Searches for contacts whose birthday falls into the specified period.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * </ul>
     *
     * @param folderIds the IDs of the parent folders
     * @param from Specifies the lower inclusive limit of the queried range, i.e. only
     *            contacts whose birthdays start on or after this date should be returned.
     * @param until Specifies the upper exclusive limit of the queried range, i.e. only
     *            contacts whose birthdays end before this date should be returned.
     * @return the contacts found with the search
     * @throws OXException if an error is occurred
     */
    List<Contact> searchContactsWithBirthday(List<String> folderIds, Date from, Date until) throws OXException;

    /**
     * Searches for contacts whose anniversary falls into the specified period.
     * <p/>
     * The following contacts parameters are evaluated:
     * <ul>
     * <li>{@link ContactsParameters#PARAMETER_FIELDS}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER}</li>
     * <li>{@link ContactsParameters#PARAMETER_ORDER_BY}</li>
     * </ul>
     *
     * @param folderIds the IDs of the parent folders
     * @param from Specifies the lower inclusive limit of the queried range, i.e. only
     *            contacts whose anniversaries start on or after this date should be returned.
     * @param until Specifies the upper exclusive limit of the queried range, i.e. only
     *            contacts whose anniversaries end before this date should be returned.
     * @return the contacts found with the search
     * @throws OXException if an error is occurred
     */
    List<Contact> searchContactsWithAnniversary(List<String> folderIds, Date from, Date until) throws OXException;

}
