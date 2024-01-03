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

package com.openexchange.contact.provider.composition.impl;

import static com.openexchange.contact.common.ContactsAccount.DEFAULT_ACCOUNT;
import static com.openexchange.osgi.Tools.requireService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.common.AccountAwareContactsFolder;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.common.GroupwareContactsFolder;
import com.openexchange.contact.common.GroupwareFolderType;
import com.openexchange.contact.provider.ContactsAccountService;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.ContactsProviderRegistry;
import com.openexchange.contact.provider.composition.IDBasedContactsAccess;
import com.openexchange.contact.provider.composition.IDBasedUserAccess;
import com.openexchange.contact.provider.composition.impl.idmangling.IDManglingContactsAccountAwareGroupwareFolder;
import com.openexchange.contact.provider.folder.AnnualDateFolderSearchAware;
import com.openexchange.contact.provider.folder.FolderSearchAware;
import com.openexchange.contact.provider.groupware.InternalContactsAccess;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.java.Strings;
import com.openexchange.search.SearchTerm;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link InternalIDBasedContactsAccess}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class InternalIDBasedContactsAccess extends AbstractCompositingIDBasedContactsAccess implements IDBasedContactsAccess, IDBasedUserAccess {

    /**
     * Initializes a new {@link InternalIDBasedContactsAccess}.
     *
     * @param session The session
     * @param providerRegistry The provider registry to use
     * @param services A service lookup reference
     */
    public InternalIDBasedContactsAccess(Session session, ContactsProviderRegistry providerRegistry, ServiceLookup services) {
        super(session, providerRegistry, services);
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public List<OXException> getWarnings() {
        return warnings;
    }

    @Override
    public IDBasedUserAccess getUserAccess() {
        return this;
    }

    @Override
    public void createContact(String folderId, Contact contact) throws OXException {
        getAccess().createContact(checkFolderId(folderId), contact);
    }

    @Override
    public void updateContact(ContactID contactId, Contact contact, long clientTimestamp) throws OXException {
        getAccess().updateContact(checkContactId(contactId), contact, clientTimestamp);
    }

    @Override
    public void deleteContact(ContactID contactId, long clientTimestamp) throws OXException {
        getAccess().deleteContact(checkContactId(contactId), clientTimestamp);
    }

    @Override
    public void deleteContacts(List<ContactID> contactsIds, long clientTimestamp) throws OXException {
        getAccess().deleteContacts(checkContactIds(contactsIds), clientTimestamp);
    }

    @Override
    public Contact getContact(ContactID contactId) throws OXException {
        return getAccess().getContact(checkFolderId(contactId.getFolderID()), contactId.getObjectID());
    }

    @Override
    public List<Contact> getContacts(List<ContactID> contactIDs) throws OXException {
        return getAccess().getContacts(checkContactIds(contactIDs));
    }

    @Override
    public List<Contact> getContacts(String folderId) throws OXException {
        return getAccess().getContacts(checkFolderId(folderId));
    }

    @Override
    public List<Contact> getDeletedContacts(String folderId, Date from) throws OXException {
        return getAccess().getDeletedContacts(checkFolderId(folderId), from);
    }

    @Override
    public List<Contact> getModifiedContacts(String folderId, Date from) throws OXException {
        return getAccess().getModifiedContacts(checkFolderId(folderId), from);
    }

    @Override
    public Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String> folderIds, Date since) throws OXException {
        return getAccess().getUpdatedContacts(checkFolderIds(folderIds), since);
    }

    @Override
    public Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException {
        return getAccess().getSequenceNumbers(checkFolderIds(folderIds));
    }

    @Override
    public String getCTag(String folderId) throws OXException {
        throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(DEFAULT_ACCOUNT.getProviderId());
    }

    @Override
    public List<AccountAwareContactsFolder> getVisibleFolders(GroupwareFolderType type) throws OXException {
        return getAccountAwareFolders(getAccess().getVisibleFolders(type), getAccount());
    }

    @Override
    public List<AccountAwareContactsFolder> getFolders(List<String> folderIds) throws OXException {
        if (null == folderIds || folderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccountAwareContactsFolder> folders = new ArrayList<AccountAwareContactsFolder>(folderIds.size());
        for (String folderId : folderIds) {
            folders.add(getFolder(folderId));
        }
        return folders;
    }

    @Override
    public AccountAwareContactsFolder getFolder(String folderId) throws OXException {
        return getAccountAwareFolder(((GroupwareContactsFolder) getAccess().getFolder(checkFolderId(folderId))), getAccount());
    }

    @Override
    public AccountAwareContactsFolder getDefaultFolder() throws OXException {
        return getAccountAwareFolder(getAccess().getDefaultFolder(), getAccount());
    }

    @Override
    public String createFolder(String providerId, ContactsFolder folder, JSONObject userConfig) throws OXException {
        if (null != providerId && false == DEFAULT_ACCOUNT.getProviderId().equals(providerId)) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(DEFAULT_ACCOUNT.getProviderId());
        }
        return getAccess().createFolder(folder);
    }

    @Override
    public String updateFolder(String folderId, ContactsFolder folder, JSONObject userConfig, long clientTimestamp) throws OXException {
        String updatedId = getAccess().updateFolder(checkFolderId(folderId), folder, clientTimestamp);
        if (null != userConfig) {
            requireService(ContactsAccountService.class, services).updateAccount(session, DEFAULT_ACCOUNT.getAccountId(), userConfig, clientTimestamp, this);
        }
        return updatedId;
    }

    @Override
    public void deleteFolder(String folderId, long clientTimestamp) throws OXException {
        getAccess().deleteFolder(checkFolderId(folderId), clientTimestamp);
    }

    @Override
    public boolean supports(String folderId, ContactField... fields) throws OXException {
        return getAccess().supports(checkFolderId(folderId), fields);
    }

    @Override
    public List<Contact> searchContacts(ContactsSearchObject contactSearch) throws OXException {
        return getAccess().searchContacts(contactSearch);
    }

    @Override
    public <O> List<Contact> searchContacts(List<String> folderIds, SearchTerm<O> term) throws OXException {
        return getAccess(DEFAULT_ACCOUNT.getAccountId(), FolderSearchAware.class).searchContacts(checkFolderIds(folderIds), term);
    }

    @Override
    public List<Contact> autocompleteContacts(List<String> folderIds, String query) throws OXException {
        return getAccess(DEFAULT_ACCOUNT.getAccountId(), FolderSearchAware.class).autocompleteContacts(checkFolderIds(folderIds), query);
    }

    @Override
    public List<Contact> searchContactsWithBirthday(List<String> folderIds, Date from, Date until) throws OXException {
        return getAccess(DEFAULT_ACCOUNT.getAccountId(), AnnualDateFolderSearchAware.class).searchContactsWithBirthday(checkFolderIds(folderIds), from, until);
    }

    @Override
    public List<Contact> searchContactsWithAnniversary(List<String> folderIds, Date from, Date until) throws OXException {
        return getAccess(DEFAULT_ACCOUNT.getAccountId(), AnnualDateFolderSearchAware.class).searchContactsWithAnniversary(checkFolderIds(folderIds), from, until);
    }

    @Override
    public List<Contact> getUserContacts(int[] userIds) throws OXException {
        return getAccess().getUserContacts(userIds);
    }

    @Override
    public List<Contact> getUserContacts() throws OXException {
        return getAccess().getUserContacts();
    }

    @Override
    public List<Contact> searchUserContacts(ContactsSearchObject contactSearch) throws OXException {
        return getAccess().searchUserContacts(contactSearch);
    }

    @Override
    public List<Contact> searchUserContacts(SearchTerm<?> searchTerm) throws OXException {
        return getAccess().searchUserContacts(searchTerm);
    }

    @Override
    public Contact getGuestContact(int userId) throws OXException {
        return getAccess().getGuestContact(userId);
    }

    private InternalContactsAccess getAccess() throws OXException {
        return getAccess(DEFAULT_ACCOUNT.getAccountId(), InternalContactsAccess.class);
    }

    private ContactsAccount getAccount() throws OXException {
        return getAccount(DEFAULT_ACCOUNT.getAccountId());
    }

    private static String checkFolderId(String folderId) throws OXException {
        if (1 > Strings.parsePositiveInt(folderId)) {
            throw ContactsProviderExceptionCodes.FOLDER_NOT_FOUND.create(folderId);
        }
        return folderId;
    }

    private static List<String> checkFolderIds(List<String> folderIds) throws OXException {
        if (null != folderIds) {
            for (String folderId : folderIds) {
                checkFolderId(folderId);
            }
        }
        return folderIds;
    }

    private static ContactID checkContactId(ContactID contactID) throws OXException {
        if (null != contactID && 1 > Strings.parsePositiveInt(contactID.getFolderID())) {
            throw ContactsProviderExceptionCodes.FOLDER_NOT_FOUND.create(contactID.getFolderID());
        }
        return contactID;
    }

    private static List<ContactID> checkContactIds(List<ContactID> contactIDs) throws OXException {
        if (null != contactIDs && 0 < contactIDs.size()) {
            for (ContactID contactID : contactIDs) {
                checkContactId(contactID);
            }
        }
        return contactIDs;
    }

    private static List<AccountAwareContactsFolder> getAccountAwareFolders(List<GroupwareContactsFolder> folders, ContactsAccount account) {
        if (null == folders) {
            return null;
        }
        List<AccountAwareContactsFolder> accountAwareFolders = new ArrayList<AccountAwareContactsFolder>(folders.size());
        for (GroupwareContactsFolder folder : folders) {
            accountAwareFolders.add(getAccountAwareFolder(folder, account));
        }
        return accountAwareFolders;
    }

    private static AccountAwareContactsFolder getAccountAwareFolder(GroupwareContactsFolder folder, ContactsAccount account) {
        if (null == folder) {
            return null;
        }
        return new IDManglingContactsAccountAwareGroupwareFolder(folder, account, folder.getId(), folder.getParentId());
    }

}
