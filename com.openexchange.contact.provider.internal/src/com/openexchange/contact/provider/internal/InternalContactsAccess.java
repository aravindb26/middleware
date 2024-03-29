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

package com.openexchange.contact.provider.internal;

import static com.openexchange.contact.ContactSessionParameterNames.getParamReadOnlyConnection;
import static com.openexchange.contact.ContactSessionParameterNames.getParamWritableConnection;
import static com.openexchange.contact.common.ContactsFolderProperty.USED_IN_PICKER;
import static com.openexchange.contact.common.ContactsFolderProperty.USED_IN_PICKER_LITERAL;
import static com.openexchange.contact.common.ContactsFolderProperty.isProtected;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_CONNECTION;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_INCLUDE_UNSUBSCRIBED_FOLDERS;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_PICKER_FOLDERS_ONLY;
import static com.openexchange.contact.provider.composition.IDMangling.getRelativeFolderId;
import static com.openexchange.contact.provider.internal.Constants.ACCOUNT_ID;
import static com.openexchange.contact.provider.internal.Constants.CONTENT_TYPE;
import static com.openexchange.contact.provider.internal.Constants.PRIVATE_FOLDER_ID;
import static com.openexchange.contact.provider.internal.Constants.PUBLIC_FOLDER_ID;
import static com.openexchange.contact.provider.internal.Constants.SHARED_FOLDER_ID;
import static com.openexchange.contact.provider.internal.Constants.TREE_ID;
import static com.openexchange.contact.provider.internal.Constants.USER_PROPERTY_PREFIX;
import static com.openexchange.folderstorage.ContactsFolderConverter.getStorageFolder;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.osgi.Tools.requireService;
import java.sql.Connection;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.contact.AutocompleteParameters;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.ContactService;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.SortOrder;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.common.ContactsFolderProperty;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.common.ContactsSession;
import com.openexchange.contact.common.DefaultGroupwareContactsFolder;
import com.openexchange.contact.common.ExtendedProperties;
import com.openexchange.contact.common.ExtendedProperty;
import com.openexchange.contact.common.GroupwareContactsFolder;
import com.openexchange.contact.common.GroupwareFolderType;
import com.openexchange.contact.common.UsedForSync;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.extensions.SubscribeAware;
import com.openexchange.contact.provider.extensions.UseCountAware;
import com.openexchange.contact.provider.folder.AnnualDateFolderSearchAware;
import com.openexchange.contact.provider.folder.FolderSearchAware;
import com.openexchange.contact.provider.folder.FolderSyncAware;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.ContactsFolderConverter;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.ParameterizedFolder;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.ContactSearchObject;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.java.Collators;
import com.openexchange.java.Strings;
import com.openexchange.java.util.TimeZones;
import com.openexchange.search.SearchTerm;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.property.FolderUserPropertyStorage;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link InternalContactsAccess}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class InternalContactsAccess implements com.openexchange.contact.provider.groupware.InternalContactsAccess, FolderSearchAware, AnnualDateFolderSearchAware, UseCountAware, FolderSyncAware, SubscribeAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalContactsAccess.class);

    private static final ContactField[] ALL_CONTACT_FIELDS = ContactField.values();

    /** The property representing the configured default for the <i>usedInPicker</i> setting for a specific folder type */
    private static final Property PROPERTY_USED_IN_PICKER = DefaultProperty.valueOf("com.openexchange.contacts.usedInPicker.[type]", Boolean.TRUE);

    /** The property representing whether the the <i>usedInPicker</i> setting is <i>protected</i> or not for a specific folder type */
    private static final Property PROPERTY_USED_IN_PICKER_PROTECTED = DefaultProperty.valueOf("com.openexchange.contacts.usedInPicker.[type].protected", Boolean.TRUE);

    private final ServiceLookup services;
    private final ContactsSession session;

    /**
     * Initializes a new {@link InternalContactsAccess}.
     *
     * @param session The session
     * @param services The services lookup instance
     */
    public InternalContactsAccess(ContactsSession session, ServiceLookup services) {
        super();
        this.session = session;
        this.services = services;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public int countContacts(String folderId) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return getContactService().countContacts(session.getSession(), folderId);
    }

    @Override
    public void createContact(String folderId, Contact contact) throws OXException {
        decorateSessionWithWritableConnection();
        getContactService().createContact(session.getSession(), folderId, contact);
        contact.setId(Integer.toString(contact.getObjectID()));
    }

    @Override
    public void updateContact(ContactID contactId, Contact contact, long clientTimestamp) throws OXException {
        decorateSessionWithWritableConnection();
        getContactService().updateContact(session.getSession(), contactId.getFolderID(), contactId.getObjectID(), contact, new Date(clientTimestamp));
        contact.setId(Integer.toString(contact.getObjectID()));
    }

    @Override
    public void deleteContact(ContactID contactId, long clientTimestamp) throws OXException {
        decorateSessionWithWritableConnection();
        getContactService().deleteContact(session.getSession(), contactId.getFolderID(), contactId.getObjectID(), new Date(clientTimestamp));
    }

    @Override
    public void deleteContacts(List<ContactID> contactsIds, long clientTimestamp) throws OXException {
        decorateSessionWithWritableConnection();
        for (Entry<String, List<String>> entry : separateContactIdsPerFolder(contactsIds).entrySet()) {
            getContactService().deleteContacts(session.getSession(), entry.getKey(), entry.getValue().toArray(Strings.getEmptyStrings()), new Date(clientTimestamp));
        }
    }

    @Override
    public void deleteContacts(String folderId) throws OXException {
        decorateSessionWithWritableConnection();
        getContactService().deleteContacts(session.getSession(), folderId);
    }

    ///////////////////////////// FOLDERS /////////////////////////////////

    @Override
    public String createFolder(ContactsFolder folder) throws OXException {
        String folderId;
        {
            DefaultGroupwareContactsFolder plainFolder = new DefaultGroupwareContactsFolder(folder);
            plainFolder.setExtendedProperties(null);
            ParameterizedFolder folderToCreate = getStorageFolder(TREE_ID, CONTENT_TYPE, plainFolder, null, ACCOUNT_ID, null);
            FolderResponse<String> response = getFolderService().createFolder(folderToCreate, session.getSession(), initDecorator());
            folderId = response.getResponse();
        }
        if (null != folder.getExtendedProperties()) {
            updateProperties(getFolder(folderId), folder.getExtendedProperties());
        }
        return folderId;
    }

    @Override
    public String updateFolder(String folderId, ContactsFolder folder, long clientTimestamp) throws OXException {
        GroupwareContactsFolder originalFolder = getFolder(folderId);
        if (null != folder.getExtendedProperties()) {
            updateProperties(originalFolder, folder.getExtendedProperties());
            DefaultGroupwareContactsFolder folderUpdate = new DefaultGroupwareContactsFolder(folder);
            folderUpdate.setExtendedProperties(null);
            // Update extended properties as needed; 'hide' the change in folder update afterwards
            folder = folderUpdate;
        }
        // Perform common folder update
        ParameterizedFolder storageFolder = getStorageFolder(TREE_ID, CONTENT_TYPE, folder, null, ACCOUNT_ID, null);
        getFolderService().updateFolder(storageFolder, new Date(clientTimestamp), session.getSession(), initDecorator());
        return storageFolder.getID();
    }

    @Override
    public void deleteFolder(String folderId, long clientTimestamp) throws OXException {
        getFolderService().deleteFolder(TREE_ID, folderId, new Date(clientTimestamp), session.getSession(), initDecorator());
    }

    @Override
    public GroupwareContactsFolder getDefaultFolder() throws OXException {
        UserizedFolder folder = getFolderService().getDefaultFolder(ServerSessionAdapter.valueOf(session.getSession()).getUser(), TREE_ID, CONTENT_TYPE, PrivateType.getInstance(), session.getSession(), initDecorator());
        return getContactsFolder(folder);
    }

    @Override
    public GroupwareContactsFolder getFolder(String folderId) throws OXException {
        return getContactsFolder(getFolderService().getFolder(TREE_ID, folderId, session.getSession(), initDecorator()));
    }

    @Override
    public List<GroupwareContactsFolder> getVisibleFolders(GroupwareFolderType type) throws OXException {
        switch (type) {
            case PRIVATE:
                return getContactFolders(getSubfoldersRecursively(getFolderService(), initDecorator(), PRIVATE_FOLDER_ID));
            case SHARED:
                return getContactFolders(getSubfoldersRecursively(getFolderService(), initDecorator(), SHARED_FOLDER_ID));
            case PUBLIC:
                return getContactFolders(getSubfoldersRecursively(getFolderService(), initDecorator(), PUBLIC_FOLDER_ID));
            case SYSTEM:
                return Collections.emptyList(); // global address book collected below PUBLIC_FOLDER_ID
            default:
                throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(Constants.PROVIDER_ID);
        }
    }

    @Override
    public Contact getContact(String folderId, String contactId) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return transferIds(getContactService().getContact(session.getSession(), folderId, contactId, getFields()));
    }

    @Override
    public List<Contact> getContacts(List<ContactID> contactIDs) throws OXException {
        decorateSessionWithReadOnlyConnection();
        Map<String, List<String>> ids = separateContactIdsPerFolder(contactIDs);
        List<Contact> contacts = new LinkedList<>();
        for (Entry<String, List<String>> entry : ids.entrySet()) {
            iterateContacts(getContactService().getContacts(session.getSession(), entry.getKey(), entry.getValue().toArray(Strings.getEmptyStrings()), getFields()), contacts);
        }
        return contacts;
    }

    @Override
    public List<Contact> getContacts(String folderId) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().getAllContacts(session.getSession(), folderId, getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> getDeletedContacts(String folderId, Date from) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().getDeletedContacts(session.getSession(), folderId, from, getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> getModifiedContacts(String folderId, Date from) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().getModifiedContacts(session.getSession(), folderId, from, getFields(), getSortOptions()));
    }

    @Override
    public boolean isFolderEmpty(String folderId) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return getContactService().isFolderEmpty(session.getSession(), folderId);
    }

    @Override
    public boolean containsForeignObjectInFolder(String folderId) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return getContactService().containsForeignObjectInFolder(session.getSession(), folderId);
    }

    @Override
    public List<Contact> getUserContacts(int[] userIds) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().getUsers(session.getSession(), userIds, getFields()));
    }

    @Override
    public List<Contact> getUserContacts() throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().getAllUsers(session.getSession(), getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> searchUserContacts(ContactsSearchObject contactSearch) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().searchUsers(session.getSession(), convert(contactSearch), getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> searchUserContacts(SearchTerm<?> searchTerm) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().searchUsers(session.getSession(), searchTerm, getFields(), getSortOptions()));
    }


    @Override
    public List<Contact> getGuestContacts(int[] userIds) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().getGuestUsers(session.getSession(), userIds, getFields()));
    }

    @Override
    public boolean supports(String folderId, ContactField... fields) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return getContactService().supports(session.getSession(), folderId, fields);
    }

    @Override
    public Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String> folderIds, Date since) throws OXException {
        decorateSessionWithReadOnlyConnection();
        String[] ignore = session.get(ContactsParameters.PARAMETER_IGNORE, String[].class);
        return getContactService().getUpdatedContacts(session.getSession(), folderIds, since, getFields(), ignore);
    }

    @Override
    public Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException {
        decorateSessionWithReadOnlyConnection();
        String[] ignore = session.get(ContactsParameters.PARAMETER_IGNORE, String[].class);
        return getContactService().getSequenceNumbers(session.getSession(), folderIds, ignore);
    }

    ////////////////////////////////// SEARCH ////////////////////////////////////

    @Override
    public List<Contact> searchContacts(ContactsSearchObject contactSearch) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().searchContacts(session.getSession(), convert(contactSearch), getFields(), getSortOptions()));
    }

    @Override
    public <O> List<Contact> searchContacts(List<String> folderIds, SearchTerm<O> term) throws OXException {
        decorateSessionWithReadOnlyConnection();
        List<String> searchFolderIds = getSearchFolderIds(folderIds);
        if (null != searchFolderIds && searchFolderIds.isEmpty()) {
            return Collections.emptyList(); // no suitable folders for search available
        }
        return iterateContacts(getContactService().searchContacts(session.getSession(), searchFolderIds, term, getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> autocompleteContacts(List<String> folderIds, String query) throws OXException {
        decorateSessionWithReadOnlyConnection();
        List<String> searchFolderIds = getSearchFolderIds(folderIds);
        if (null != searchFolderIds && searchFolderIds.isEmpty()) {
            return Collections.emptyList(); // no suitable folders for search available
        }
        AutocompleteParameters parameters = AutocompleteParameters.newInstance();
        parameters.put(AutocompleteParameters.REQUIRE_EMAIL, session.get(ContactsParameters.PARAMETER_REQUIRE_EMAIL, Boolean.class));
        parameters.put(AutocompleteParameters.IGNORE_DISTRIBUTION_LISTS, session.get(ContactsParameters.PARAMETER_IGNORE_DISTRIBUTION_LISTS, Boolean.class));
        return iterateContacts(getContactService().autocompleteContacts(session.getSession(), searchFolderIds, query, parameters, getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> searchContactsWithBirthday(List<String> folderIds, Date from, Date until) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().searchContactsWithBirthday(session.getSession(), folderIds, from, until, getFields(), getSortOptions()));
    }

    @Override
    public List<Contact> searchContactsWithAnniversary(List<String> folderIds, Date from, Date until) throws OXException {
        decorateSessionWithReadOnlyConnection();
        return iterateContacts(getContactService().searchContactsWithAnniversary(session.getSession(), folderIds, from, until, getFields(), getSortOptions()));
    }

    ///////////////////////////////// CONTACTS PARAMETERS /////////////////////////////

    /**
     * Gets the {@link ContactsParameters#PARAMETER_ORDER_BY} parameter.
     *
     * @return The order by {@link ContactField}
     */
    private ContactField getOrderBy() {
        return session.get(ContactsParameters.PARAMETER_ORDER_BY, ContactField.class);
    }

    /**
     * Gets the {@link ContactsParameters#PARAMETER_ORDER} parameter. If absent falls back to the {@link Order#ASCENDING}
     *
     * @return The {@link Order}
     */
    private Order getOrder() {
        Order order = session.get(ContactsParameters.PARAMETER_ORDER, Order.class);
        if (order == null) {
            order = Order.ASCENDING;
        }
        return order;
    }

    /**
     * Gets the {@link ContactsParameters#PARAMETER_FIELDS} parameter. If absent falls back to the {@link ALL_CONTACT_FIELDS}
     *
     * @return The {@link ContactField}s array
     */
    private ContactField[] getFields() {
        ContactField[] fields = session.get(ContactsParameters.PARAMETER_FIELDS, ContactField[].class);
        if (fields == null) {
            fields = ALL_CONTACT_FIELDS;
        }
        return fields;
    }

    /**
     * Gets the {@link SortOptions} based on the orderBy and order or
     * on the sortOptions parameters.
     *
     * @return The {@link SortOptions}
     * @throws OXException if the leftHandLimit is greater than the rightHandLimit
     */
    private SortOptions getSortOptions() throws OXException {
        SortOrder order = SortOptions.Order(getOrderBy(), getOrder());
        SortOptions options = order == null ? new SortOptions() : new SortOptions(order);
        String collation = getCollation();
        if (Strings.isNotEmpty(collation)) {
            options.setCollation(collation);
        }

        Integer leftHandLimit = getLeftHandLimit();
        if (leftHandLimit.intValue() >= 0) {
            options.setRangeStart(i(leftHandLimit));
        }

        Integer rightHandLimit = getRightHandLimit();
        if (rightHandLimit.intValue() >= 0) {
            if (leftHandLimit.intValue() > rightHandLimit.intValue()) {
                throw ContactsProviderExceptionCodes.INVALID_RANGE_LIMITS.create();
            }
            options.setLimit(rightHandLimit.intValue() - leftHandLimit.intValue());
        }

        return options;
    }

    /**
     * Gets the {@link ContactsParameters#PARAMETER_COLLATION}
     *
     * @return the integer value for the start parameter, -1 if not present
     */
    private String getCollation() {
        return session.get(ContactsParameters.PARAMETER_COLLATION, String.class);
    }

    /**
     * Gets the {@link ContactsParameters#PARAMETER_LEFT_HAND_LIMIT}
     *
     * @return the integer value for the left-hand-limit parameter, -1 if not present
     */
    private Integer getLeftHandLimit() {
        return session.get(ContactsParameters.PARAMETER_LEFT_HAND_LIMIT, Integer.class, I(0));
    }

    /**
     * Gets the {@link ContactsParameters#PARAMETER_RIGHT_HAND_LIMIT}
     *
     * @return the integer value for the right-hand-limit parameter, -1 if not present
     */
    private Integer getRightHandLimit() {
        return session.get(ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT, Integer.class, I(0));
    }

    /////////////////////////////// FOLDER STUFF //////////////////////////////////

    /**
     * Separates the specified contact ids per folder
     *
     * @param contactsIds The contact ids to separate
     * @return The separated contact ids per folder
     */
    private Map<String, List<String>> separateContactIdsPerFolder(List<ContactID> contactsIds) {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        for (ContactID id : contactsIds) {
            List<String> cids = new LinkedList<>();
            cids.add(id.getObjectID());
            List<String> absent = ids.putIfAbsent(id.getFolderID(), cids);
            if (absent != null) {
                absent.add(id.getObjectID());
                ids.put(id.getFolderID(), absent);
            }
        }
        return ids;
    }

    /**
     * Gets a list of groupware contacts folders representing the folders in the supplied userized folders.
     *
     * @param folders The folders from the folder service
     * @return The groupware contacts folders
     * @throws OXException if an error is occurred
     */
    private List<GroupwareContactsFolder> getContactFolders(List<UserizedFolder> folders) throws OXException {
        if (null == folders || 0 == folders.size()) {
            return Collections.emptyList();
        }
        List<GroupwareContactsFolder> contactFolders = new ArrayList<>(folders.size());
        for (UserizedFolder userizedFolder : folders) {
            contactFolders.add(getContactsFolder(userizedFolder));
        }
        return sort(contactFolders, ((ServerSession) session.getSession()).getUser().getLocale());
    }

    /**
     * Gets the groupware contacts folder representing the userized folders in the supplied folder response.
     *
     * @param folderResponse The response from the folder service
     * @return The groupware contacts folder
     * @throws OXException if an error is occurred
     */
    private GroupwareContactsFolder getContactsFolder(UserizedFolder userizedFolder) throws OXException {
        DefaultGroupwareContactsFolder contactsFolder = ContactsFolderConverter.getContactsFolder(userizedFolder);
        Map<String, String> userProperties = loadUserProperties(session.getContextId(), userizedFolder.getID(), session.getUserId());
        contactsFolder.setExtendedProperties(getExtendedProperties(userProperties, userizedFolder));
        contactsFolder.setSupportedCapabilities(ContactsAccessCapability.getCapabilityNames(InternalContactsAccess.class));
        /*
         * ensure default contacts folder is always 'used for sync'
         */
        if (userizedFolder.isDefault() && FolderObject.CONTACT == userizedFolder.getDefaultType() && PrivateType.getInstance().equals(userizedFolder.getType())) {
            contactsFolder.setUsedForSync(UsedForSync.FORCED_ACTIVE);
        }
        return contactsFolder;
    }

    /**
     * Collects all contacts subfolders from a parent folder recursively.
     *
     * @param folderService A reference to the folder service
     * @param decorator The optional folder service decorator to use
     * @param parentId The parent folder identifier to get the subfolders from
     * @return The collected subfolders, or an empty list if there are none
     * @throws OXException if an error is occurred
     */
    private List<UserizedFolder> getSubfoldersRecursively(FolderService folderService, FolderServiceDecorator decorator, String parentId) throws OXException {
        UserizedFolder[] subfolders = folderService.getSubfolders(TREE_ID, parentId, true, session.getSession(), decorator).getResponse();
        if (null == subfolders || 0 == subfolders.length) {
            return Collections.emptyList();
        }
        List<UserizedFolder> allFolders = new ArrayList<>();
        for (UserizedFolder subfolder : subfolders) {
            if (CONTENT_TYPE.equals(subfolder.getContentType())) {
                allFolders.add(subfolder);
            }
            if (subfolder.hasSubscribedSubfolders()) {
                allFolders.addAll(getSubfoldersRecursively(folderService, decorator, subfolder.getID()));
            }
        }
        allFolders.sort((folder1, folder2) -> {
            String id1 = folder1.getID();
            String id2 = folder1.getID();
            int intId1 = Strings.parsePositiveInt(folder1.getID());
            int intId2 = Strings.parsePositiveInt(folder2.getID());
            if (-1 != intId1 && -1 != intId2) {
                return Integer.compare(intId1, intId2);
            }
            return Strings.compare(id1, id2);
        });
        return allFolders;
    }

    /**
     * Creates and initializes a folder service decorator ready to use with calls to the underlying folder service.
     *
     * @return A new folder service decorator
     */
    private FolderServiceDecorator initDecorator() {
        FolderServiceDecorator decorator = new FolderServiceDecorator();
        Connection connection = optConnection();
        if (null != connection) {
            decorator.put(Connection.class.getName(), connection);
        }
        decorator.setLocale(((ServerSession) session.getSession()).getUser().getLocale());
        decorator.put("altNames", Boolean.TRUE.toString());
        decorator.setTimeZone(TimeZones.UTC);
        decorator.setAllowedContentTypes(Collections.<ContentType> singletonList(CONTENT_TYPE));
        return decorator;
    }

    /**
     * Sorts the specified list of contacts folders by name. The default folders will end up
     * in the first places.
     *
     * @param contactsFolders The contacts folders to sort
     * @param locale The locale
     * @return The sorted contacts folders
     */
    private List<GroupwareContactsFolder> sort(List<GroupwareContactsFolder> contactsFolders, Locale locale) {
        if (null == contactsFolders || 2 > contactsFolders.size()) {
            return contactsFolders;
        }
        Collator collator = Collators.getSecondaryInstance(locale);
        contactsFolders.sort((folder1, folder2) -> {
            if (folder1.isDefaultFolder() != folder2.isDefaultFolder()) {
                // Default folders first
                return folder1.isDefaultFolder() ? -1 : 1;
            }
            // Otherwise, compare folder names
            return collator.compare(folder1.getName(), folder2.getName());
        });
        return contactsFolders;
    }

    //////////////////////////////// EXTENDED PROPERTIES //////////////////////////////////////

    /**
     * Gets the extended contacts properties for a storage folder.
     * 
     * @param userProperties The stored user properties of the folder
     * @param folder The folder to get the extended contacts properties for
     * @return The extended properties
     */
    private ExtendedProperties getExtendedProperties(Map<String, String> userProperties, UserizedFolder folder) {
        ExtendedProperties properties = new ExtendedProperties();
        /*
         * usedInPicker
         */
        ExtendedProperty usedInPickerProperty = getDefaultUsedInPicker(ContactsFolderConverter.getFolderType(folder.getType()));
        if (false == isProtected(usedInPickerProperty) && null != userProperties && userProperties.containsKey(USER_PROPERTY_PREFIX + USED_IN_PICKER_LITERAL)) {
            String value = userProperties.get(USER_PROPERTY_PREFIX + USED_IN_PICKER_LITERAL);
            usedInPickerProperty = new ExtendedProperty(USED_IN_PICKER_LITERAL, value, usedInPickerProperty.getParameters());
        }
        properties.add(usedInPickerProperty);
        return properties;
    }

    /**
     * Updates extended contacts properties of a groupware contacts folder.
     *
     * @param originalFolder The original folder being updated
     * @param properties The properties as passed by the client
     * @throws OXException if an error is occurred
     */
    private void updateProperties(GroupwareContactsFolder originalFolder, ExtendedProperties properties) throws OXException {
        ExtendedProperties originalProperties = originalFolder.getExtendedProperties();
        List<ExtendedProperty> propertiesToStore = new ArrayList<>();
        for (ExtendedProperty property : properties) {
            ExtendedProperty originalProperty = originalProperties.get(property.getName());
            if (null == originalProperty) {
                throw OXException.noPermissionForFolder();
            }
            if (originalProperty.equals(property)) {
                continue;
            }
            if (ContactsFolderProperty.isProtected(originalProperty)) {
                throw OXException.noPermissionForFolder();
            }
            propertiesToStore.add(property);
        }
        if (0 == propertiesToStore.size()) {
            return;
        }
        Map<String, String> updatedProperties = new HashMap<>(propertiesToStore.size());
        Set<String> removedProperties = new HashSet<>();
        for (ExtendedProperty property : propertiesToStore) {
            String name = USER_PROPERTY_PREFIX + property.getName();
            if (null == property.getValue()) {
                removedProperties.add(name);
                continue;
            }
            if ((property.getValue() instanceof String) || (property.getValue() instanceof Boolean)) {
                updatedProperties.put(name, property.getValue().toString());
            } else {
                throw OXException.noPermissionForFolder();
            }
        }
        removeUserProperties(session.getContextId(), originalFolder.getId(), session.getUserId(), removedProperties);
        storeUserProperties(session.getContextId(), originalFolder.getId(), session.getUserId(), updatedProperties);
    }

    /**
     * Stores the user properties for the specified folder
     *
     * @param contextId The context identifier
     * @param folderId The folder identifier
     * @param userId The user identifier
     * @param properties The properties to store
     * @throws OXException if an error is occurred
     */
    private void storeUserProperties(int contextId, String folderId, int userId, Map<String, String> properties) throws OXException {
        if (null == properties || properties.isEmpty()) {
            return;
        }
        FolderUserPropertyStorage propertyStorage = requireService(FolderUserPropertyStorage.class, services);
        Connection connection = optConnection();
        if (null == connection) {
            propertyStorage.setFolderProperties(contextId, Integer.parseInt(getRelativeFolderId(folderId)), userId, properties);
        } else {
            propertyStorage.setFolderProperties(contextId, Integer.parseInt(getRelativeFolderId(folderId)), userId, properties, connection);
        }
    }

    /**
     * Removes the user properties for the specified folder
     *
     * @param contextId The context identifier
     * @param folderId The folder identifier
     * @param userId The user identifier
     * @param propertyNames The names of the properties that shall be removed
     * @throws OXException if an error is occurred
     */
    private void removeUserProperties(int contextId, String folderId, int userId, Set<String> propertyNames) throws OXException {
        if (null == propertyNames || propertyNames.isEmpty()) {
            return;
        }
        FolderUserPropertyStorage propertyStorage = requireService(FolderUserPropertyStorage.class, services);
        Connection connection = optConnection();
        if (null == connection) {
            propertyStorage.deleteFolderProperties(contextId, Integer.parseInt(getRelativeFolderId(folderId)), userId, propertyNames);
        } else {
            propertyStorage.deleteFolderProperties(contextId, Integer.parseInt(getRelativeFolderId(folderId)), userId, propertyNames, connection);
        }
    }

    /**
     * Loads the user properties for the specified folder
     *
     * @param contextId The context identifier
     * @param folderId The folder identifier
     * @param userId The user identifier
     * @return A {@link Map} with all user properties for the specified folder
     * @throws OXException if an error is occurred
     */
    private Map<String, String> loadUserProperties(int contextId, String folderId, int userId) throws OXException {
        FolderUserPropertyStorage propertyStorage = requireService(FolderUserPropertyStorage.class, services);
        Connection connection = optConnection();
        if (null == connection) {
            return propertyStorage.getFolderProperties(contextId, Integer.parseInt(getRelativeFolderId(folderId)), userId);
        }
        return propertyStorage.getFolderProperties(contextId, Integer.parseInt(getRelativeFolderId(folderId)), userId, connection);
    }

    /////////////////////////////// UTILITIES ////////////////////////////////

    /**
     * Iterates over the specified {@link SearchIterator} to compile
     * a {@link List} with {@link Contact}s
     *
     * @param iterator The {@link SearchIterator} to use
     * @return A {@link List} with the {@link Contact}s
     */
    private List<Contact> iterateContacts(SearchIterator<Contact> iterator) {
        return iterateContacts(iterator, new LinkedList<>());
    }

    /**
     * Iterates over the specified {@link SearchIterator} to compile
     * a {@link List} with {@link Contact}s
     *
     * @param iterator The {@link SearchIterator} to use
     * @param contacts The optional list to store the iterated contacts
     * @return A {@link List} with the {@link Contact}s
     */
    private List<Contact> iterateContacts(SearchIterator<Contact> iterator, List<Contact> contacts) {
        try {
            while (iterator.hasNext()) {
                contacts.add(transferIds(iterator.next()));
            }
        } catch (OXException e) {
            LOGGER.error("Could not retrieve contact from folder using a FolderIterator, exception was: ", e);
        } finally {
            SearchIterators.close(iterator);
        }
        return contacts;
    }

    /**
     * Decorates the {@link ServerSession} with an optional writeable database connection
     */
    private void decorateSessionWithWritableConnection() {
        session.getSession().setParameter(getParamWritableConnection(), optConnection());
    }

    /**
     * Decorates the {@link ServerSession} with an optional read-only database connection
     */
    private void decorateSessionWithReadOnlyConnection() {
        session.getSession().setParameter(getParamReadOnlyConnection(), optConnection());
    }

    /**
     * Optionally gets the {@link Connection} that was passed as a
     * {@link Session} parameter
     *
     * @return The {@link Connection} or <code>null</code>
     */
    private Connection optConnection() {
        return session.get(PARAMETER_CONNECTION(), Connection.class);
    }

    /**
     * Transfers the integer based identifiers of the supplied {@link Contact}
     * to their corresponding string fields
     *
     * @param contact The contact
     * @return the contact for chained calls
     */
    private Contact transferIds(Contact contact) {
        contact.setId(Integer.toString(contact.getObjectID()));
        contact.setFolderId(Integer.toString(contact.getParentFolderID()));
        return contact;
    }

    /**
     * Converts the new {@link ContactsSearchObject} to its legacy counter-part {@link ContactSearchObject}
     *
     * @param contactSearch the object to convert
     * @return The converted object
     */
    private ContactSearchObject convert(ContactsSearchObject contactSearch) {
        ContactSearchObject cso = new ContactSearchObject();
        if (null != contactSearch.getFolders()) {
            for (String folderId : contactSearch.getFolders()) {
                int numericalId = Strings.parsePositiveInt(folderId);
                if (-1 != numericalId) {
                    cso.addFolder(numericalId);
                } else {
                    LOGGER.warn("Ignoring malformed folder id {}", folderId);
                }
            }
        }
        if (null != contactSearch.getExcludeFolders()) {
            for (String folderId : contactSearch.getExcludeFolders()) {
                int numericalId = Strings.parsePositiveInt(folderId);
                if (-1 != numericalId) {
                    cso.addExcludeFolder(numericalId);
                } else {
                    LOGGER.warn("Ignoring malformed excluded folder id {}", folderId);
                }
            }
        }
        cso.setPattern(contactSearch.getPattern());
        cso.setStartLetter(contactSearch.isStartLetter());
        cso.setEmailAutoComplete(contactSearch.isEmailAutoComplete());
        cso.setOrSearch(contactSearch.isOrSearch());
        cso.setExactMatch(contactSearch.isExactMatch());
        cso.setHasImage(contactSearch.isHasImage());
        cso.setSurname(contactSearch.getSurname());
        cso.setDisplayName(contactSearch.getDisplayName());
        cso.setGivenName(contactSearch.getGivenName());
        cso.setCompany(contactSearch.getCompany());
        cso.setEmail1(contactSearch.getEmail1());
        cso.setEmail2(contactSearch.getEmail2());
        cso.setEmail3(contactSearch.getEmail3());
        cso.setCatgories(contactSearch.getCatgories());
        cso.setSubfolderSearch(contactSearch.isSubfolderSearch());
        return cso;
    }

    /**
     * Gets the identifiers of the folders that should be considered for a search request, depending on
     * {@link ContactsParameters#PARAMETER_PICKER_FOLDERS_ONLY} and {@link ContactsParameters#PARAMETER_INCLUDE_UNSUBSCRIBED_FOLDERS}.
     * 
     * @param requestedFolderIds The explicitly requested folder ids from the client
     * @return The identifiers of the folders to perform the search in, or an empty list if none are applicable
     */
    private List<String> getSearchFolderIds(List<String> requestedFolderIds) throws OXException {
        if (null != requestedFolderIds) {
            return requestedFolderIds; // as requested
        }
        boolean pickerFoldersOnly = b(session.get(PARAMETER_PICKER_FOLDERS_ONLY, Boolean.class, Boolean.FALSE));
        boolean includeUnsubscribedFolders = b(session.get(PARAMETER_INCLUDE_UNSUBSCRIBED_FOLDERS, Boolean.class, Boolean.FALSE));
        if (false == pickerFoldersOnly && false == includeUnsubscribedFolders) {
            return null; // any subscribed folder by default
        }
        List<String> searchFolderIds = new LinkedList<String>();
        for (GroupwareContactsFolder folder : getVisibleFolders()) {
            if (pickerFoldersOnly && false == isUsedInPicker(folder)) {
                continue; // skip non-picker folders
            }
            if (false == includeUnsubscribedFolders && false == isSubscribed(folder)) {
                continue; // skip unsububscribed folders 
            }
            searchFolderIds.add(folder.getId());
        }
        return searchFolderIds;
    }

    /**
     * Gets a value indicating whether a specific folder is <i>subscribed</i> or not.
     * 
     * @param folder The folder to check
     * @return <code>true</code> if the folder is subscribed, <code>false</code>, otherwise
     */
    private static boolean isSubscribed(ContactsFolder folder) {
        return null == folder.isSubscribed() || b(folder.isSubscribed());
    }

    /**
     * Gets a value indicating whether a specific folder is configured to be used in the address book picker dialog or not.
     * 
     * @param folder The folder to check
     * @return <code>true</code> if the folder is used in the picker, <code>false</code>, otherwise
     */
    private boolean isUsedInPicker(GroupwareContactsFolder folder) {
        ExtendedProperty defaultProperty = getDefaultUsedInPicker(folder.getType());
        if (isProtected(defaultProperty)) {
            return Boolean.parseBoolean((String) defaultProperty.getValue());
        }
        ExtendedProperties extendedProperties = folder.getExtendedProperties();
        if (null != extendedProperties) {
            ExtendedProperty usedInPickerProperty = extendedProperties.get(USED_IN_PICKER_LITERAL);
            if (null != usedInPickerProperty && Boolean.parseBoolean((String) usedInPickerProperty.getValue())) {
                return true;
            }
        }
        return Boolean.parseBoolean((String) defaultProperty.getValue());
    }

    /**
     * Gets an extended property representing the configured default for the <i>usedInPicker</i> setting for a specific folder type.
     * 
     * @param type The type of folder to get the configured default for the <i>usedInPicker</i> setting for
     * @return The configured default for the <i>usedInPicker</i> setting for this folder type
     */
    private ExtendedProperty getDefaultUsedInPicker(GroupwareFolderType type) {
        Map<String, String> optionals = Collections.singletonMap("type", type.toString().toLowerCase(LocaleTools.DEFAULT_LOCALE));
        try {
            LeanConfigurationService configService = services.getServiceSafe(LeanConfigurationService.class);
            boolean value = configService.getBooleanProperty(session.getUserId(), session.getContextId(), PROPERTY_USED_IN_PICKER, optionals);
            boolean protekted = configService.getBooleanProperty(session.getUserId(), session.getContextId(), PROPERTY_USED_IN_PICKER_PROTECTED, optionals);
            return USED_IN_PICKER(Boolean.toString(value), protekted);
        } catch (OXException e) {
            LOGGER.warn("Error getting default 'usedInPicker' configuration for folder type {}, falling back to defaults.", type, e);
            return USED_IN_PICKER(String.valueOf(PROPERTY_USED_IN_PICKER.getDefaultValue(Boolean.class)), b(PROPERTY_USED_IN_PICKER_PROTECTED.getDefaultValue(Boolean.class)));
        }
    }

    //////////////////////////////// SERVICES ///////////////////////////

    /**
     * Returns the {@link ContactService}
     *
     * @return the {@link ContactService}
     * @throws OXException if the service is absent
     */
    private ContactService getContactService() throws OXException {
        return services.getServiceSafe(ContactService.class);
    }

    /**
     * Returns the {@link FolderService}
     *
     * @return the {@link FolderService}
     * @throws OXException if the service is absent
     */
    private FolderService getFolderService() throws OXException {
        return services.getServiceSafe(FolderService.class);
    }

}
