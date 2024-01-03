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

package com.openexchange.contact.provider.ldap;

import static com.openexchange.contact.common.ContactsParameters.PARAMETER_FIELDS;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_IGNORE;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_INCLUDE_UNSUBSCRIBED_FOLDERS;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_LEFT_HAND_LIMIT;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_ORDER;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_ORDER_BY;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_PICKER_FOLDERS_ONLY;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_REQUIRE_EMAIL;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT;
import static com.openexchange.contact.common.ContactsPermission.NO_PERMISSIONS;
import static com.openexchange.contact.common.ContactsPermission.READ_ALL_OBJECTS;
import static com.openexchange.contact.common.ContactsPermission.READ_FOLDER;
import static com.openexchange.contact.provider.ldap.SettingsHelper.initInternalConfig;
import static com.openexchange.contact.provider.ldap.SettingsHelper.optFolderSettings;
import static com.openexchange.contact.provider.ldap.SettingsHelper.setFolderProperty;
import static com.openexchange.contact.provider.ldap.Utils.addRequireEMailFilter;
import static com.openexchange.contact.provider.ldap.Utils.applyFolderExclusions;
import static com.openexchange.contact.provider.ldap.Utils.applyPagedResultsControl;
import static com.openexchange.contact.provider.ldap.Utils.applyRangeAndLimit;
import static com.openexchange.contact.provider.ldap.Utils.collectAttributes;
import static com.openexchange.contact.provider.ldap.Utils.generateDisplayName;
import static com.openexchange.contact.provider.ldap.Utils.getAutocompleteFilter;
import static com.openexchange.contact.provider.ldap.Utils.getContactFieldTerm;
import static com.openexchange.contact.provider.ldap.Utils.getObjectIdsPerFolderId;
import static com.openexchange.contact.provider.ldap.Utils.getOrFilter;
import static com.openexchange.contact.provider.ldap.Utils.getSizeLimit;
import static com.openexchange.contact.provider.ldap.Utils.getSortOptions;
import static com.openexchange.contact.provider.ldap.Utils.resolveUserAndContextId;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.DISPLAYNAME_FIELDS;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.DISTLISTMEMBER_FIELDS;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.PROPERTY_AUTOCOMPLETE_FIELDS;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.PROPERTY_MINIMUM_SEARCH_CHARACTERS;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.common.DefaultContactsPermission;
import com.openexchange.contact.common.DefaultGroupwareContactsFolder;
import com.openexchange.contact.common.ExtendedProperties;
import com.openexchange.contact.common.ExtendedProperty;
import com.openexchange.contact.common.ExtendedPropertyParameter;
import com.openexchange.contact.common.GroupwareContactsFolder;
import com.openexchange.contact.common.GroupwareFolderType;
import com.openexchange.contact.common.UsedForSync;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.ContactsAccountService;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.extensions.SubscribeAware;
import com.openexchange.contact.provider.extensions.WarningsAware;
import com.openexchange.contact.provider.folder.FolderCTagAware;
import com.openexchange.contact.provider.folder.FolderSearchAware;
import com.openexchange.contact.provider.folder.FolderSyncAware;
import com.openexchange.contact.provider.folder.UpdateableFolderContactsAccess;
import com.openexchange.contact.provider.groupware.GroupwareContactsAccess;
import com.openexchange.contact.provider.ldap.config.ConfigUtils;
import com.openexchange.contact.provider.ldap.config.FoldersConfig;
import com.openexchange.contact.provider.ldap.config.ProtectableValue;
import com.openexchange.contact.provider.ldap.config.ProviderConfig;
import com.openexchange.contact.provider.ldap.mapping.LdapIntegerMapping;
import com.openexchange.contact.provider.ldap.mapping.LdapMapping;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.results.DefaultSequenceResult;
import com.openexchange.groupware.results.DefaultUpdatesResult;
import com.openexchange.groupware.results.SequenceResult;
import com.openexchange.groupware.results.UpdatesResult;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.java.Enums;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.Operand;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.functions.ErrorAwareFunction;
import com.openexchange.user.UserService;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.controls.SortKey;

/**
 * {@link LdapContactsAccess}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapContactsAccess implements UpdateableFolderContactsAccess, GroupwareContactsAccess, FolderSearchAware, WarningsAware, SubscribeAware, FolderSyncAware, FolderCTagAware {

    private static final Logger LOG = LoggerFactory.getLogger(LdapContactsAccess.class);

    /** A fallback date that is applied if no last modified/creation date attribute is available for a converted contact */
    private static final Date FALLBACK_DATE = new Date(1000);

    /** Used with an LDAP operation to include tombstones and deleted-objects in the results, as per [MS-ADTS] 3.1.1.3.4.1.14 */
    protected static final Control AD_SHOW_DELETED_CONTROL = new Control("1.2.840.113556.1.4.417");

    protected final ContactsAccount account;
    protected final ProviderConfig config;
    protected final ServiceLookup services;
    protected final Session session;
    protected final ContactsParameters parameters;
    protected final List<OXException> warnings;

    /**
     * Initializes a new {@link LdapContactsAccess}.
     *
     * @param services A service lookup reference
     * @param config The underlying provider config
     * @param session The current user's session
     * @param account The underlying contacts account
     * @param parameters The contacts parameters
     */
    public LdapContactsAccess(ServiceLookup services, ProviderConfig config, Session session, ContactsAccount account, ContactsParameters parameters) {
        super();
        this.account = account;
        this.services = services;
        this.config = config;
        this.session = session;
        this.parameters = parameters;
        this.warnings = new ArrayList<OXException>();
        LOG.debug("LDAP contacts access for {} initialized.", account);
    }

    @Override
    public GroupwareContactsFolder getFolder(String folderId) throws OXException {
        return initFolder(folderId, getFolderFilter(folderId).getName(), optFolderSettings(account, folderId));
    }

    @Override
    public List<GroupwareContactsFolder> getVisibleFolders(GroupwareFolderType type) throws OXException {
        if (false == GroupwareFolderType.PUBLIC.equals(type)) {
            return Collections.emptyList();
        }
        List<GroupwareContactsFolder> folders = new LinkedList<GroupwareContactsFolder>();
        for (Map.Entry<String, LdapFolderFilter> entry : getFolderFilters().entrySet()) {
            folders.add(initFolder(entry.getKey(), entry.getValue().getName(), optFolderSettings(account, entry.getKey())));
        }
        return folders;
    }

    @Override
    public String updateFolder(String folderId, ContactsFolder folder, long clientTimestamp) throws OXException {
        /*
         * update folder's configuration in underlying account's internal config
         */
        JSONObject internalConfig = null != account.getInternalConfiguration() ? new JSONObject(account.getInternalConfiguration()) : initInternalConfig(config);
        boolean wasUpdated = false;
        if (null != folder.isSubscribed() && setFolderProperty(
            internalConfig, folderId, "subscribed", config.getFoldersConfig().isShownInTree(), folder.isSubscribed())) {
            wasUpdated = true;
        }
        if (null != folder.getUsedForSync() && setFolderProperty(
            internalConfig, folderId, "usedForSync", config.getFoldersConfig().isUsedForSync(), B(folder.getUsedForSync().isUsedForSync()))) {
            wasUpdated = true;
        }
        if (null != folder.getExtendedProperties()) {
            ExtendedProperty property = folder.getExtendedProperties().get("usedInPicker");
            Boolean value;
            try {
                value = null == property ? config.getFoldersConfig().isUsedInPicker().getDefaultValue() : 
                    (property.getValue() instanceof Boolean) ? (Boolean) property.getValue() : Boolean.valueOf(String.valueOf(property.getValue())); 
            } catch (Exception e) {
                throw FolderExceptionErrorMessage.JSON_ERROR.create(e, "Unparsable value in 'usedInPicker'");
            }
            if (setFolderProperty(internalConfig, folderId, "usedInPicker", config.getFoldersConfig().isUsedInPicker(), value)) {
                wasUpdated = true;
            }
        }
        if (wasUpdated) {
            JSONObject userConfig = null != account.getUserConfiguration() ? account.getUserConfiguration() : new JSONObject();
            userConfig.putSafe("internalConfig", internalConfig);
            services.getService(ContactsAccountService.class).updateAccount(session, account.getAccountId(), userConfig, clientTimestamp, parameters);
        }
        return folderId;
    }

    @Override
    public List<Contact> getContacts(List<ContactID> contactIDs) throws OXException {
        if (null == contactIDs || contactIDs.isEmpty()) {
            return Collections.emptyList();
        }
        /*
         * search contacts per folder identifier
         */
        Map<String, List<String>> objectIdsPerFolderId = getObjectIdsPerFolderId(contactIDs);
        Map<ContactID, Contact> contactsById = new HashMap<ContactID, Contact>(objectIdsPerFolderId.size());
        for (Map.Entry<String, List<String>> entry : objectIdsPerFolderId.entrySet()) {
            Filter filter = getOrFilter(config.getMapper(), ContactField.OBJECT_ID, entry.getValue());
            for (Contact contact : searchContacts(entry.getKey(), filter, getFields(ContactField.OBJECT_ID))) {
                contactsById.put(new ContactID(entry.getKey(), contact.getId()), contact);
            }
        }
        /*
         * put contacts back into requested order & check that each contact was found
         */
        List<Contact> contacts = new ArrayList<Contact>(contactIDs.size());
        for (ContactID contactID : contactIDs) {
            Contact contact = contactsById.get(contactID);
            if (null == contact) {
                throw ContactsProviderExceptionCodes.CONTACT_NOT_FOUND_IN_FOLDER.create(contactID.getFolderID(), contactID.getObjectID());
            }
            contacts.add(contact);
        }
        return contacts;
    }

    @Override
    public List<Contact> getContacts(String folderId) throws OXException {
        return searchContacts(folderId, null);
    }

    @Override
    public List<Contact> autocompleteContacts(List<String> folderIds, String query) throws OXException {
        /*
         * build auto-complete filter from query
         */
        List<ContactField> contactFields = getAutocompleteFields(false);
        String[] attributes = config.getMapper().getAttributes(contactFields.toArray(new ContactField[contactFields.size()]));
        Filter autocompleteFilter = getAutocompleteFilter(query, attributes, getWarnings(), getMinimumSearchCharacters());
        /*
         * perform search & return results
         */
        return searchContacts(folderIds, autocompleteFilter);
    }

    /**
     * Gets the configured contact fields that should be considered for an 'auto-complete' search.
     *
     * @return The auto-complete fields
     */
    protected List<ContactField> getAutocompleteFields(boolean mappedOnly) {
        List<ContactField> parsedFields;
        try {
            LeanConfigurationService configService = services.getServiceSafe(LeanConfigurationService.class);
            String autocompleteFields = configService.getProperty(session.getUserId(), session.getContextId(), PROPERTY_AUTOCOMPLETE_FIELDS);
            parsedFields = Enums.parseCsv(ContactField.class, autocompleteFields);
        } catch (OXException | IllegalArgumentException e) {
            LOG.warn("Error getting {}, falling back to defaults.", PROPERTY_AUTOCOMPLETE_FIELDS.getFQPropertyName(), e);
            parsedFields = Enums.parseCsv(ContactField.class, PROPERTY_AUTOCOMPLETE_FIELDS.getDefaultValue(String.class));
        }
        if (mappedOnly) {
            parsedFields.removeIf(c -> null == config.getMapper().opt(c));
        }
        return parsedFields;
    }

    /**
     * Gets the configured minimum search characters.
     *
     * @return The minimum search characters
     */
    protected int getMinimumSearchCharacters() {
        try {
            LeanConfigurationService configService = services.getServiceSafe(LeanConfigurationService.class);
            return configService.getIntProperty(session.getUserId(), session.getContextId(), PROPERTY_MINIMUM_SEARCH_CHARACTERS);
        } catch (OXException | IllegalArgumentException e) {
            LOG.warn("Error getting {}, falling back to defaults.", PROPERTY_MINIMUM_SEARCH_CHARACTERS.getFQPropertyName(), e);
            return i(PROPERTY_MINIMUM_SEARCH_CHARACTERS.getDefaultValue(Integer.class));
        }
    }

    @Override
    public <O> List<Contact> searchContacts(List<String> folderIds, SearchTerm<O> term) throws OXException {
        /*
         * build search filter from term, perform search & return results
         */
        Filter searchFilter = null != term ? new SearchTermAdapter(config.getMapper(), warnings).getFilter(prepareSearchTerm(term)) : null;
        return searchContacts(folderIds, searchFilter);
    }
    
    @Override
    public List<Contact> searchContacts(ContactsSearchObject contactSearch) throws OXException {
        /*
         * build search filter from search object & perform search
         */
        Filter searchFilter = new ContactsSearchAdapter(config.getMapper(), warnings).getFilter(contactSearch);
        Set<String> folders = contactSearch.getFolders();
        List<String> folderIds = null != folders && 0 < folders.size() ? new ArrayList<String>(folders) : null;
        List<Contact> contacts = searchContacts(folderIds, searchFilter);
        /*
         * strip contacts from excluded folders if required
         */
        return applyFolderExclusions(contacts, folders);
    }

    @Override
    public List<Contact> getModifiedContacts(String folderId, Date from) throws OXException {
        LdapMapping<? extends Object> mapping = config.getMapper().opt(ContactField.LAST_MODIFIED);
        if (null == mapping) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No mapping for field " + ContactField.LAST_MODIFIED + ", unable to search for modified contacts."), account.getProviderId()));
            return Collections.emptyList();
        }
        return searchContacts(Collections.singletonList(folderId), getContactFieldTerm(SingleOperation.GREATER_OR_EQUAL, ContactField.LAST_MODIFIED, new Date(from.getTime() + 1)));
    }

    @Override
    public List<Contact> getDeletedContacts(String folderId, Date from) throws OXException {
        LdapMapping<? extends Object> mapping = config.getMapper().opt(ContactField.LAST_MODIFIED);
        if (null == mapping) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No mapping for field " + ContactField.LAST_MODIFIED + ", unable to search for deleted contacts."), account.getProviderId()));
            return Collections.emptyList();
        }
        if (false == config.isDeletedSupport()) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No support for 'isDeleted' control, unable to search for deleted contacts."), account.getProviderId()));
            return Collections.emptyList();
        }
        Filter searchFilter = new SearchTermAdapter(config.getMapper(), warnings).getFilter(
            getContactFieldTerm(SingleOperation.GREATER_OR_EQUAL, ContactField.LAST_MODIFIED, new Date(from.getTime() + 1)));
        Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("isDeleted", "TRUE"), searchFilter);
        return searchContacts(folderId, filter, AD_SHOW_DELETED_CONTROL);
    }
    
    @Override
    public Map<String, UpdatesResult<Contact>> getUpdatedContacts(List<String> folderIds, Date since) throws OXException {
        LdapMapping<? extends Object> mapping = config.getMapper().opt(ContactField.LAST_MODIFIED);
        if (null == mapping) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No mapping for field " + ContactField.LAST_MODIFIED + ", unable to search for modified contacts."), account.getProviderId()));
            return Collections.emptyMap();
        }
        if (false == config.isDeletedSupport()) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No support for 'isDeleted' control, unable to search for deleted contacts."), account.getProviderId()));
            return Collections.emptyMap();
        }
        Function<Contact, Long> timestampFunction = c -> null != c.getLastModified() ? L(c.getLastModified().getTime()) : null;
        Map<String, UpdatesResult<Contact>> updatesResults = new LinkedHashMap<String, UpdatesResult<Contact>>(folderIds.size());
        String[] ignore = parameters.get(PARAMETER_IGNORE, String[].class);
        for (String folderId : folderIds) {
            List<Contact> newAndModifiedContacts = null;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "changed")) {
                newAndModifiedContacts = getModifiedContacts(folderId, since);
            }
            List<Contact> deletedContacts = null;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "deleted")) {
                deletedContacts = getDeletedContacts(folderId, since);
            }
            long totalCount = -1L;
            if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "count")) {
                totalCount = countContacts(folderId);
            }
            updatesResults.put(folderId, new DefaultUpdatesResult<Contact>(newAndModifiedContacts, deletedContacts, timestampFunction, totalCount));
        }
        return updatesResults;
    }

    @Override
    public Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException {
        LdapMapping<? extends Object> mapping = config.getMapper().opt(ContactField.LAST_MODIFIED);
        if (null == mapping) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No mapping for field " + ContactField.LAST_MODIFIED + ", unable to get sequence numbers."), account.getProviderId()));
            return Collections.emptyMap();
        }
        if (false == config.isDeletedSupport()) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No support for 'isDeleted' control, unable to get sequence numbers."), account.getProviderId()));
            return Collections.emptyMap();
        }        
        String[] ignore = parameters.get(PARAMETER_IGNORE, String[].class);
        Filter isDeletedFilter = Filter.createEqualityFilter("isDeleted", "TRUE");
        Map<String, SequenceResult> sequenceResults = new HashMap<String, SequenceResult> (folderIds.size());
        ContactField oldOrderBy = parameters.get(PARAMETER_ORDER_BY, ContactField.class);
        Order oldOrder = parameters.get(PARAMETER_ORDER, Order.class);
        Integer oldRightHandLimit = parameters.get(PARAMETER_RIGHT_HAND_LIMIT, Integer.class);
        Integer oldLeftHandLimit = parameters.get(PARAMETER_LEFT_HAND_LIMIT, Integer.class);
        try {
            for (String folderId : folderIds) {
                parameters.set(PARAMETER_ORDER_BY, ContactField.LAST_MODIFIED);
                parameters.set(PARAMETER_ORDER, Order.DESCENDING);            
                parameters.set(PARAMETER_RIGHT_HAND_LIMIT, I(1));
                parameters.set(PARAMETER_LEFT_HAND_LIMIT, null);
                long timestamp = 0L;
                for (Contact contact : searchContacts(folderId, null, new ContactField[] { ContactField.LAST_MODIFIED }, (Control[]) null)) {
                    if (null != contact.getLastModified()) {
                        timestamp = Math.max(timestamp, contact.getLastModified().getTime());
                    }                    
                }
                for (Contact contact : searchContacts(folderId, isDeletedFilter, new ContactField[] { ContactField.LAST_MODIFIED }, AD_SHOW_DELETED_CONTROL)) {
                    if (null != contact.getLastModified()) {
                        timestamp = Math.max(timestamp, contact.getLastModified().getTime());
                    }
                }
                long totalCount = -1L;
                if (false == com.openexchange.tools.arrays.Arrays.contains(ignore, "count")) {
                    parameters.set(PARAMETER_ORDER_BY, null);
                    parameters.set(PARAMETER_ORDER, null);            
                    parameters.set(PARAMETER_RIGHT_HAND_LIMIT, null);
                    totalCount = countContacts(folderId);
                }
                sequenceResults.put(folderId, new DefaultSequenceResult(timestamp, totalCount));
            }
        } finally {
            parameters.set(PARAMETER_ORDER_BY, oldOrderBy);
            parameters.set(PARAMETER_ORDER, oldOrder);
            parameters.set(PARAMETER_RIGHT_HAND_LIMIT, oldRightHandLimit);
            parameters.set(PARAMETER_LEFT_HAND_LIMIT, oldLeftHandLimit);
        }
        return sequenceResults;
    }

    @Override
    public String getCTag(String folderId) throws OXException {
        /*
         * check if applicable
         */
        if (null == config.getMapper().opt(ContactField.LAST_MODIFIED)) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No mapping for field " + ContactField.LAST_MODIFIED + ", unable to calculate CTag."), account.getProviderId()));
            return null;
        }
        if (null == config.getMapper().opt(ContactField.UID)) {
            warnings.add(ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(
                new Exception("No mapping for field " + ContactField.UID + ", unable to calculate CTag."), account.getProviderId()));
            return null;
        }
        /*
         * calculate hash of all contact's UID and last-modified properties contained in the folder
         */
        Hasher hasher = Hashing.murmur3_128().newHasher();
        Integer oldRightHandLimit = parameters.get(PARAMETER_RIGHT_HAND_LIMIT, Integer.class);
        Integer oldLeftHandLimit = parameters.get(PARAMETER_LEFT_HAND_LIMIT, Integer.class);
        try {
            parameters.set(PARAMETER_RIGHT_HAND_LIMIT, null);
            parameters.set(PARAMETER_LEFT_HAND_LIMIT, null);
            for (Contact contact : searchContacts(folderId, null, new ContactField[] { ContactField.LAST_MODIFIED, ContactField.UID }, (Control[]) null)) {
                if (null != contact.getLastModified()) {
                    hasher.putLong(contact.getLastModified().getTime());
                } else {
                    hasher.putLong(FALLBACK_DATE.getTime());
                }
                if (null != contact.getUid()) {
                    hasher.putUnencodedChars(contact.getUid());
                }
            }
        } finally {
            parameters.set(PARAMETER_RIGHT_HAND_LIMIT, oldRightHandLimit);
            parameters.set(PARAMETER_LEFT_HAND_LIMIT, oldLeftHandLimit);
        }
        return hasher.hash().toString();
    }

    @Override
    public void close() {
        LOG.debug("Closing LDAP contacts access for {}.", account);
    }

    @Override
    public List<OXException> getWarnings() {
        return warnings;
    }

    @Override
    public String toString() {
        return "LdapContactsAccess [account=" + account + "]";
    }

    /**
     * Initializes a groupware contacts folder with a specific identifier, name and individual user configuration settings.
     *
     * @param id The identifier to assign to the contacts folder
     * @param folderFilter The name to take over for the contacts folder
     * @param folderSettings The individual user configurations settings to set
     * @return The initialized groupware contacts folder
     */
    private DefaultGroupwareContactsFolder initFolder(String id, String name, JSONObject folderSettings) {
        /*
         * init defaults
         */
        FoldersConfig foldersConfig = config.getFoldersConfig();
        DefaultGroupwareContactsFolder folder = new DefaultGroupwareContactsFolder();
        folder.setParentId(String.valueOf(FolderObject.SYSTEM_PUBLIC_FOLDER_ID));
        folder.setFolderType(GroupwareFolderType.PUBLIC);
        folder.setPermissions(Collections.singletonList(new DefaultContactsPermission(
            session.getUserId(), READ_FOLDER, READ_ALL_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS, false, false, 0)));
        folder.setUsedForSync(new UsedForSync(b(foldersConfig.isUsedForSync().getDefaultValue()), foldersConfig.isUsedForSync().isProtected()));
        folder.setSubscribed(foldersConfig.isShownInTree().getDefaultValue());
        ExtendedProperty usedInPickerProperty = new ExtendedProperty("usedInPicker", String.valueOf(foldersConfig.isUsedInPicker().getDefaultValue()),
            Collections.singletonList(new ExtendedPropertyParameter("protected", String.valueOf(foldersConfig.isUsedInPicker().isProtected()))));
        /*
         * apply folder-specific properties
         */
        folder.setId(id);
        folder.setName(name);
        if (null != folderSettings) {
            if (false == foldersConfig.isShownInTree().isProtected() && folderSettings.hasAndNotNull("subscribed")) {
                folder.setSubscribed(B(folderSettings.optBoolean("subscribed")));
            }
            if (false == foldersConfig.isUsedForSync().isProtected() && folderSettings.hasAndNotNull("usedForSync")) {
                folder.setUsedForSync(new UsedForSync(folderSettings.optBoolean("usedForSync"), false));
            }
            if (false == foldersConfig.isUsedInPicker().isProtected() && folderSettings.hasAndNotNull("usedInPicker")) {
                usedInPickerProperty = new ExtendedProperty("usedInPicker", String.valueOf(folderSettings.optBoolean("usedInPicker")),
                    Collections.singletonList(new ExtendedPropertyParameter("protected", "false")));
            }
        }
        /*
         * assign capabilities & extended properties & return prepared folder
         */
        folder.setExtendedProperties(new ExtendedProperties(Collections.singleton(usedInPickerProperty)));
        Set<String> capabilityNames = ContactsAccessCapability.getCapabilityNames(LdapContactsAccess.class);
        if (false == isSyncAware()) {
            capabilityNames.remove(ContactsAccessCapability.SYNC.getName());
        }
        if (false == isCTagAware()) {
            capabilityNames.remove(ContactsAccessCapability.CTAG.getName());
        }
        folder.setSupportedCapabilities(capabilityNames);
        return folder;
    }

    /**
     * Gets the LDAP search filters associated with a list of folders.
     *
     * @param requestedFolderIds The folder identifiers to get the LDAP filters for, or <code>null</code> to get filters for all configured folders
     * @return The LDAP search filters, mapped to the associated folder identifier
     */
    protected Map<String, Filter> getFiltersPerFolderId(List<String> requestedFolderIds) throws OXException {
        Map<String, Filter> filtersPerFolderId = new HashMap<String, Filter>();
        if (null == requestedFolderIds) {
            for (java.util.Map.Entry<String, LdapFolderFilter> entry : getFolderFilters().entrySet()) {
                filtersPerFolderId.put(entry.getKey(), entry.getValue().getContactFilter());
            }
        } else {
            for (String folderId : requestedFolderIds) {
                filtersPerFolderId.put(folderId, getFolderFilter(folderId).getContactFilter());
            }
        }
        return filtersPerFolderId;
    }

    /**
     * Gets the LDAP search filters associated with all defined of folders.
     *
     * @param pickerFoldersOnly <code>true</code> to only consider folders marked as <code>usedInPicker</code>, <code>false</code>, otherwise
     * @param subscribedFoldersOnly <code>true</code> to only consider folders marked as <code>subscribed</code>, <code>false</code>, otherwise
     * @return The LDAP search filters, mapped to the associated folder id, or an empty list if all were excluded
     */
    protected Map<String, Filter> getFiltersPerFolderId(boolean pickerFoldersOnly, boolean subscribedFoldersOnly) throws OXException {
        Map<String, LdapFolderFilter> folderFilters = getFolderFilters();
        Map<String, Filter> filtersPerFolderId = new HashMap<String, Filter>(folderFilters.size());
        for (java.util.Map.Entry<String, LdapFolderFilter> entry : folderFilters.entrySet()) {
            String folderId = entry.getKey();
            if (pickerFoldersOnly && false == isUsedInPicker(folderId) || subscribedFoldersOnly && false == isSubscribed(folderId)) {
                continue;
            }            
            filtersPerFolderId.put(folderId, entry.getValue().getContactFilter());
        }
        return filtersPerFolderId;
    }

    /**
     * Initializes a contact from an LDAP search result entry, for each folder whose LDAP filter matches the entry.
     *
     * @param searchResult The LDAP search result to create the contact or distribution list from
     * @param filtersPerFolderId A map providing the possible filters per folder identifier the search was executed against
     * @param fields The contact fields to consider, or <code>null</code> to apply all fields
     * @return The contact(s) (one for each matching folder), or an empty list if no matching parent folder for the entry is available
     */
    protected Collection<Contact> getContacts(LdapSearchResult searchResult, Map<String, Filter> filtersPerFolderId, ContactField[] fields) throws OXException {
        return getContacts(searchResult, filtersPerFolderId, fields, false);
    }

    /**
     * Initializes a contact from an LDAP search result entry, for each folder whose LDAP filter matches the entry.
     *
     * @param searchResult The LDAP search result to create the contact or distribution list from
     * @param filtersPerFolderId A map providing the possible filters per folder identifier the search was executed against
     * @param fields The contact fields to consider, or <code>null</code> to apply all fields
     * @param preserveContextInfo <code>true</code> to preserve previously parsed context and user ids from foreign contexts, <code>false</code> to strip it off
     * @return The contact(s) (one for each matching folder), or an empty list if no matching parent folder for the entry is available
     */
    protected Collection<Contact> getContacts(LdapSearchResult searchResult, Map<String, Filter> filtersPerFolderId, ContactField[] fields, boolean preserveContextInfo) throws OXException {
        List<Contact> contacts = new LinkedList<Contact>();
        for (Map.Entry<String, Filter> filterForFolderId : filtersPerFolderId.entrySet()) {
            try {
                if (filterForFolderId.getValue().matchesEntry(searchResult.getSearchEntry())) {
                    contacts.add(getContact(searchResult, filterForFolderId.getKey(), fields, preserveContextInfo));
                }
            } catch (LDAPException e) {
                throw LdapContactsExceptionCodes.LDAP_ERROR.create(e, e.getMessage());
            }
        }
        return contacts;
    }

    /**
     * Initializes a contact from an LDAP search result entry.
     *
     * @param searchResult The LDAP search result to create the contact or distribution list from
     * @param folderId The parent folder identifier to assign to the contact
     * @param fields The contact fields to consider, or <code>null</code> to apply all fields
     * @return The contact
     */
    private Contact getContact(LdapSearchResult searchResult, String folderId, ContactField[] fields) throws OXException {
        return getContact(searchResult, folderId, fields, false);
    }

    /**
     * Initializes a contact from an LDAP search result entry.
     *
     * @param searchResult The LDAP search result to create the contact or distribution list from
     * @param folderId The parent folder identifier to assign to the contact
     * @param fields The contact fields to consider, or <code>null</code> to apply all fields
     * @param preserveContextInfo <code>true</code> to preserve previously parsed context and user ids from foreign contexts, <code>false</code> to strip it off
     * @return The contact
     */
    private Contact getContact(LdapSearchResult searchResult, String folderId, ContactField[] fields, boolean preserveContextInfo) throws OXException {
        /*
         * create contact
         */
        Contact contact = config.getMapper().fromEntry(searchResult.getSearchEntry(), fields);
        /*
         * assign identifiers & apply further defaults
         */
        if (null == fields || com.openexchange.tools.arrays.Arrays.contains(fields, ContactField.FOLDER_ID)) {
            contact.setFolderId(folderId);
        }
        contact = resolveUserAndContextId(services, contact);
        if (false == preserveContextInfo) {
            removeForeignUserAndContextId(contact);
        }
        /*
         * derive internal user id if applicable in context of session user
         */
        if (null == contact.getLastModified() && (null == fields || com.openexchange.tools.arrays.Arrays.contains(fields, ContactField.LAST_MODIFIED))) {
            contact.setLastModified(FALLBACK_DATE);
        }
        if (null == contact.getCreationDate() && (null == fields || com.openexchange.tools.arrays.Arrays.contains(fields, ContactField.CREATION_DATE))) {
            contact.setCreationDate(FALLBACK_DATE);
        }
        /*
         * resolve distribution list members as needed
         */
        if (contact.getMarkAsDistribtuionlist()) {
            if (null == fields || com.openexchange.tools.arrays.Arrays.contains(fields, ContactField.DISTRIBUTIONLIST)) {
                contact.setDistributionList(resolveMembers(searchResult.getConnectionProvider(), contact.getDistributionList()));
            } else {
                contact.removeDistributionLists();
            }
        }
        /*
         * resolve manager / assistant as needed
         */
        if (Strings.isNotEmpty(contact.getManagerName())) {
            contact.setManagerName(resolveToDisplayName(searchResult.getConnectionProvider(), contact.getManagerName()));
        }
        if (Strings.isNotEmpty(contact.getAssistantName())) {
            contact.setAssistantName(resolveToDisplayName(searchResult.getConnectionProvider(), contact.getAssistantName()));
        }
        return contact;
    }

    private String resolveToDisplayName(LDAPConnectionProvider connectionProvider, String dn) {
        if (null == dn || false == DN.isValidDN(dn)) {
            return dn;
        }
        Set<String> collectedAttributes = new HashSet<String>(DISPLAYNAME_FIELDS.length);
        config.getMapper().collectAttributes(DISPLAYNAME_FIELDS, collectedAttributes);
        String[] attributes = collectedAttributes.toArray(new String[collectedAttributes.size()]);
        LDAPConnection connection = null;
        try {
            connection = connectionProvider.getConnection(session);
            /*
             * get referenced entry and convert to representative contact
             */
            SearchResultEntry resultEntry = connection.getEntry(dn, attributes);
            if (null == resultEntry) {
                throw OXException.notFound(dn);
            }
            return generateDisplayName(getContact(new LdapSearchResult(connectionProvider, resultEntry), dn, DISPLAYNAME_FIELDS, false));
        } catch (LDAPException | OXException e) {
            warnings.add(LdapContactsExceptionCodes.CANT_RESOLVE_ENTRY.create(e, dn));
        } finally {
            connectionProvider.back(connection);
        }
        return dn;
    }

    private DistributionListEntryObject[] resolveMembers(LDAPConnectionProvider connectionProvider, DistributionListEntryObject[] memberDNs) throws OXException {
        if (null == memberDNs || 0 == memberDNs.length) {
            return memberDNs;
        }
        Map<String, Filter> filtersPerFolderId = getFiltersPerFolderId(null);
        Set<String> collectedAttributes = new HashSet<String>();
        filtersPerFolderId.values().forEach(f -> collectAttributes(f, collectedAttributes));
        config.getMapper().collectAttributes(DISTLISTMEMBER_FIELDS, collectedAttributes);
        String[] attributes = collectedAttributes.toArray(new String[collectedAttributes.size()]);
        List<DistributionListEntryObject> resolvedMembers = new ArrayList<DistributionListEntryObject>(memberDNs.length);
        LDAPConnection connection = null;
        try {
            connection = connectionProvider.getConnection(session);
            for (DistributionListEntryObject memberDN : memberDNs) {
                try {
                    if (DN.isValidDN(memberDN.getDisplayname())) {
                        /*
                         * get referenced entry and convert to representative contact
                         */
                        SearchResultEntry memberEntry = connection.getEntry(memberDN.getDisplayname(), attributes);
                        if (null == memberEntry) {
                            throw OXException.notFound(memberDN.getDisplayname());
                        }
                        LdapSearchResult memberResult = new LdapSearchResult(connectionProvider, memberEntry);
                        Collection<Contact> contacts = getContacts(memberResult, filtersPerFolderId, DISTLISTMEMBER_FIELDS);
                        Contact referencedContact;
                        if (contacts.isEmpty()) {
                            LOG.debug("Cannot resolve {} to other contact, continuing with one-off member", memberDN.getDisplayname());
                            referencedContact = config.getMapper().fromEntry(memberEntry, DISTLISTMEMBER_FIELDS);
                        } else {
                            referencedContact = contacts.iterator().next();
                        }
                        /*
                         * transform contact to distribution list entry object & add to list
                         */
                        resolvedMembers.add(asMember(referencedContact));
                    } else {
                        /*
                         * take over value as email address for independent/one-off member
                         */
                        resolvedMembers.add(asMember(memberDN.getDisplayname()));
                    }
                } catch (OXException | LDAPException e) {
                    warnings.add(LdapContactsExceptionCodes.CANT_RESOLVE_ENTRY.create(e, memberDN.getDisplayname()));
                }
            }
        } finally {
            connectionProvider.back(connection);
        }
        return resolvedMembers.toArray(new DistributionListEntryObject[resolvedMembers.size()]);
    }

    private static DistributionListEntryObject asMember(String email) throws OXException {
        DistributionListEntryObject member = new DistributionListEntryObject();
        member.setEmailaddress(email);
        member.setEmailfield(DistributionListEntryObject.INDEPENDENT);
        return member;
    }

    private static DistributionListEntryObject asMember(Contact referencedContact) throws OXException {
        DistributionListEntryObject member = new DistributionListEntryObject();
        if (referencedContact.containsDisplayName()) {
            member.setDisplayname(referencedContact.getDisplayName());
        }
        if (referencedContact.containsSurName()) {
            member.setLastname(referencedContact.getSurName());
        }
        if (referencedContact.containsGivenName()) {
            member.setFirstname(referencedContact.getGivenName());
        }
        if (referencedContact.containsEmail1()) {
            member.setEmailaddress(referencedContact.getEmail1(), false);
            member.setEmailfield(DistributionListEntryObject.EMAILFIELD1);
        } else if (referencedContact.containsEmail2()) {
            member.setEmailaddress(referencedContact.getEmail2(), false);
            member.setEmailfield(DistributionListEntryObject.EMAILFIELD2);
        } else if (referencedContact.containsEmail3()) {
            member.setEmailaddress(referencedContact.getEmail3(), false);
            member.setEmailfield(DistributionListEntryObject.EMAILFIELD3);
        }
        if (referencedContact.containsFolderId() && referencedContact.containsId()) {
            /*
             * insert references to other contact
             */
            member.setEntryID(referencedContact.getId());
            member.setFolderID(referencedContact.getFolderId());
        } else {
            /*
             * treat as independent "one-off" entry
             */
            member.setEmailfield(DistributionListEntryObject.INDEPENDENT);
        }
        return member;
    }

    /**
     * Gets the fields to retrieve for contacts as requested by the client.
     *
     * @param additionalFields Additional fields to always include
     * @return The fields, or <code>null</code> if all data is requested
     */
    ContactField[] getFields(ContactField... additionalFields) {
        ContactField[] fields = parameters.get(PARAMETER_FIELDS, ContactField[].class);
        if (null == fields) {
            return fields;
        }
        Set<ContactField> mergedFields = new HashSet<ContactField>(Arrays.asList(fields));
        mergedFields.addAll(Arrays.asList(additionalFields));
        if (mergedFields.contains(ContactField.INTERNAL_USERID)) {
            mergedFields.add(ContactField.CONTEXTID);
        }
        return mergedFields.toArray(new ContactField[mergedFields.size()]);
    }

    private boolean isRequireEmail() {
        return Boolean.TRUE.equals(parameters.get(PARAMETER_REQUIRE_EMAIL, Boolean.class));
    }

    private List<Contact> searchContacts(List<String> folderIds, Filter optFilter, Control... additionalControls) throws OXException {
        /*
         * gather searched folders, perform search in single folder if possible
         */
        if (null != folderIds && 1 == folderIds.size()) {
            return searchContacts(folderIds.get(0), optFilter, additionalControls);
        }
        Map<String, Filter> filtersPerFolderId;
        if (null == folderIds) {
            boolean pickerFoldersOnly = b(parameters.get(PARAMETER_PICKER_FOLDERS_ONLY, Boolean.class, Boolean.FALSE));
            boolean includeUnsubscribedFolders = b(parameters.get(PARAMETER_INCLUDE_UNSUBSCRIBED_FOLDERS, Boolean.class, Boolean.FALSE));
            filtersPerFolderId = getFiltersPerFolderId(pickerFoldersOnly, false == includeUnsubscribedFolders);
        } else {
            filtersPerFolderId = getFiltersPerFolderId(folderIds);
        }
        if (filtersPerFolderId.isEmpty()) {
            return Collections.emptyList();
        }
        if (1 == filtersPerFolderId.size()) {
            return searchContacts(filtersPerFolderId.keySet().iterator().next(), optFilter, additionalControls);
        }
        /*
         * use common contacts filter, considering extended attributes for re-association of contacts to requested folders, otherwise
         */
        Filter filter = null == optFilter ? getCommonContactFilter() : Filter.createANDFilter(getCommonContactFilter(), optFilter);
        if (isRequireEmail()) {
            filter = addRequireEMailFilter(filter, config.getMapper());
        }
        ContactField[] contactFields = getFields();
        Set<String> collectedAttributes = new HashSet<String>();
        filtersPerFolderId.values().forEach(f -> collectAttributes(f, collectedAttributes));
        config.getMapper().collectAttributes(contactFields, collectedAttributes);
        String[] attributes = collectedAttributes.toArray(new String[collectedAttributes.size()]);
        SearchScope searchScope = getCommonContactSearchScope();
        /*
         * perform search and convert resulting entries to contacts, assigning suitable parent folder identifiers during conversion
         */
        return search((result) -> getContacts(result, filtersPerFolderId, contactFields), searchScope, filter, attributes, additionalControls);
    }

    protected List<Contact> searchContacts(String folderId, Filter optFilter, Control... additionalControls) throws OXException {
        return searchContacts(folderId, optFilter, getFields(), additionalControls);
    }

    protected List<Contact> searchContacts(String folderId, Filter optFilter, ContactField[] contactFields, Control... additionalControls) throws OXException {
        /*
         * prepare search request for all contacts in this folder
         */
        LdapFolderFilter folderFilter = getFolderFilter(folderId);
        Filter filter = null == optFilter ? folderFilter.getContactFilter() : Filter.createANDFilter(folderFilter.getContactFilter(), optFilter);
        if (isRequireEmail()) {
            filter = addRequireEMailFilter(filter, config.getMapper());
        }
        String[] attributes = config.getMapper().getAttributes(contactFields);
        SearchScope searchScope = folderFilter.getContactSearchScope();
        /*
         * perform search and convert resulting entries to contacts, assigning a static parent folder identifier during conversion
         */
        return search((result) -> Collections.singleton(getContact(result, folderId, contactFields)), searchScope, filter, attributes, additionalControls);
    }

    /**
     * Applies controls to the supplied search request.
     * <p/>
     * This always includes a control for the configured maximum page size, as well as an control for the requested sort order.
     * Also, the size limit is applied based on the actual {@link ContactsParameters}.
     *
     * @param searchRequest The search request to add the controls to
     * @param additionalControls Additional controls to include
     * @return The passed search request, with the added controls
     */
    private SearchRequest applyControls(SearchRequest searchRequest, Control... additionalControls) throws OXException {
        List<Control> controls = new LinkedList<Control>();
        if (null != additionalControls && 0 < additionalControls.length) {
            controls.addAll(Arrays.asList(additionalControls));
        }
        if (0 < config.getMaxPageSize()) {
            controls.add(new SimplePagedResultsControl(config.getMaxPageSize()));
        }
        SortOptions sortOptions = getSortOptions(parameters);
        if (null != sortOptions && false == SortOptions.EMPTY.equals(sortOptions)) {
            int sizeLimit = getSizeLimit(sortOptions);
            if (0 < sizeLimit) {
                searchRequest.setSizeLimit(sizeLimit);
            }
            SortKey[] sortKeys = config.getMapper().getSortKeys(sortOptions);
            if (null != sortKeys && 0 < sortKeys.length) {
                controls.add(new ServerSideSortRequestControl(sortKeys));
            }
        }
        searchRequest.setControls(controls);
        return searchRequest;
    }

    protected <T> List<T> search(ErrorAwareFunction<LdapSearchResult, Collection<T>> entryConverter, SearchScope searchScope, Filter filter, String[] attributes, Control... additionalControls) throws OXException {
        LdapSearchResultListener<T> resultListener = null;
        LDAPConnectionProvider connectionProvider = getConnectionProvider();
        LDAPConnection connection = connectionProvider.getConnection(session);
        try {
            resultListener = new LdapSearchResultListener<T>(connectionProvider, entryConverter);
            SearchRequest searchRequest = new SearchRequest(resultListener, connectionProvider.getBaseDN(), searchScope, filter, attributes);
            searchRequest = applyControls(searchRequest, additionalControls);
            performSearch(connection, searchRequest, resultListener);
        } catch (LDAPException e) {
            if (ResultCode.SIZE_LIMIT_EXCEEDED.equals(e.getResultCode())) {
                int clientLimit = getSizeLimit(getSortOptions(parameters));
                if (0 >= clientLimit || clientLimit > resultListener.getEntriesRead()) {
                    LOG.warn("Server request size limit exceeded ({}), abort reading.", e.getMessage(), e);
                } else {
                    LOG.debug("Client request size limit exceeded ({}), stop reading.", e.getMessage());
                }
            } else {
                throw LdapContactsExceptionCodes.LDAP_ERROR.create(e, e.getMessage());
            }
        } finally {
            connectionProvider.back(connection);
        }
        return applyRangeAndLimit(getSortOptions(parameters), resultListener.getResults());
    }

    private <T> List<T> performSearch(LDAPConnection connection, SearchRequest searchRequest, LdapSearchResultListener<T> resultListener) throws LDAPException {
        LOG.trace("Searching entries with filter {}...", searchRequest.getFilter());
        long start = System.nanoTime();
        int numPages = 0;
        ASN1OctetString resumeCookie = null;
        do {
            SearchResult searchResult = connection.search(applyPagedResultsControl(searchRequest, config.getMaxPageSize(), resumeCookie));
            numPages++;
            SimplePagedResultsControl pagedResponseControl = SimplePagedResultsControl.get(searchResult);
            resumeCookie = null != pagedResponseControl && pagedResponseControl.moreResultsToReturn() ? pagedResponseControl.getCookie() : null;
        } while (null != resumeCookie);
        LOG.trace("Search finished in {}ms [{} entries read from {} page(s), {} references followed, {} exceptions encountered]",
            L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)), I(resultListener.getEntriesRead()), I(numPages), I(resultListener.getReferencesRead()), I(resultListener.getExceptionsCaught()));
        return resultListener.getResults();
    }

    protected Map<String, LdapFolderFilter> getFolderFilters() throws OXException {
        return config.getFoldersConfig().getFolderFilters(config, getConnectionProvider(), session);
    }

    protected LdapFolderFilter getFolderFilter(String folderId) throws OXException {
        LdapFolderFilter folderFilter = getFolderFilters().get(folderId);
        if (null == folderFilter) {
            throw ContactsProviderExceptionCodes.FOLDER_NOT_FOUND.create(folderId);
        }
        return folderFilter;
    }

    protected Filter getCommonContactFilter() {
        return config.getFoldersConfig().getCommonContactFilter();
    }

    protected SearchScope getCommonContactSearchScope() {
        return config.getFoldersConfig().getCommonContactSearchScope();
    }

    protected LDAPConnectionProvider getConnectionProvider() throws OXException {
        return services.getServiceSafe(LDAPService.class).getConnection(config.getLdapClientId());
    }

    /**
     * Gets a value indicating whether a specific folder is <i>subscribed</i> or not.
     * 
     * @param folderId The identifier of the folder to check
     * @return <code>true</code> if the folder is subscribed, <code>false</code>, otherwise
     */
    protected boolean isSubscribed(String folderId) {
        ProtectableValue<Boolean> defaultValue = config.getFoldersConfig().isShownInTree();
        if (defaultValue.isProtected()) {
            return b(defaultValue.getDefaultValue());
        }
        JSONObject folderSettings = optFolderSettings(account, folderId);
        if (null != folderSettings && folderSettings.hasAndNotNull("subscribed")) {
            return folderSettings.optBoolean("subscribed");
        }
        return b(defaultValue.getDefaultValue());
    }

    /**
     * Gets a value indicating whether a specific folder is configured to be used in the address book picker dialog or not.
     * 
     * @param folderId The identifier of the folder to check
     * @return <code>true</code> if the folder is used in the picker, <code>false</code>, otherwise
     */
    protected boolean isUsedInPicker(String folderId) {
        ProtectableValue<Boolean> defaultValue = config.getFoldersConfig().isUsedInPicker();
        if (defaultValue.isProtected()) {
            return b(defaultValue.getDefaultValue());
        }
        JSONObject folderSettings = optFolderSettings(account, folderId);
        if (null != folderSettings && folderSettings.hasAndNotNull("usedInPicker")) {
            return folderSettings.optBoolean("usedInPicker");
        }
        return b(defaultValue.getDefaultValue());
    }
    
    /**
     * Gets a value indicating whether the provider configuration allows incremental synchronization of folder contents or not.
     * 
     * @return <code>true</code> if incremental synchronization is possible, <code>false</code>, otherwise
     */
    protected boolean isSyncAware() {
        return isCTagAware() && config.isDeletedSupport();
    }
    
    protected boolean isCTagAware() {
        LdapMapping<? extends Object> mapping = config.getMapper().opt(ContactField.LAST_MODIFIED);
        if (null == mapping) {
            return false;
        }
        mapping = config.getMapper().opt(ContactField.UID);
        if (null == mapping) {
            return false;
        }
        return true;
    }

    
    /**
     * Strips context- and user identifiers within the supplied contact in case it denotes a context different from the current session.
     * <p/>
     * The context id is always removed afterwards as it is no longer needed.
     * 
     * @param contact The contact to remove the context- and user identifiers from if needed
     */
    protected void removeForeignUserAndContextId(Contact contact) {
        if (null != contact && contact.containsContextId() && 0 < contact.getContextId()) {
            if (session.getContextId() != contact.getContextId()) {
                contact.removeInternalUserId(); // user from other context
            }
            contact.removeContextID();
        }
    }

    /**
     * Prepares a search term prior handing it down to the {@link SearchTermAdapter} and performing the search.
     * 
     * @param term The search term to prepare
     * @return The prepared search term
     */
    private SearchTerm<?> prepareSearchTerm(SearchTerm<?> term) throws OXException {
        if ((term instanceof SingleSearchTerm)) {
            return prepareSearchTerm((SingleSearchTerm) term);
        }
        if ((term instanceof CompositeSearchTerm)) {
            return prepareSearchTerm((CompositeSearchTerm) term);
        }
        throw new IllegalArgumentException("Need either an 'SingleSearchTerm' or 'CompositeSearchTerm'.");
    }

    /**
     * Prepares a single search term prior handing it down to the {@link SearchTermAdapter} and performing the search.
     * 
     * @param term The search term to prepare
     * @return The prepared search term
     */
    private SearchTerm<?> prepareSearchTerm(SingleSearchTerm term) throws OXException {
        ContactField mappedField = config.getMapper().getMappedField(term);
        LdapMapping<? extends Object> mapping = config.getMapper().opt(mappedField);
        if (null == mapping) {
            /*
             * not mapped (i.e., resulting contact value is 'null'), evaluate to TRUE or FALSE based on operation statically
             * using a synthetic term against mapped object id attribute (which is mandatory)
             */
            if (ContactField.OBJECT_ID.equals(mappedField)) {
                throw LdapContactsExceptionCodes.WRONG_OR_MISSING_CONFIG_VALUE.create(OXException.mandatoryField(ContactField.OBJECT_ID.toString()), ConfigUtils.MAPPING_FILENAME);
            }
            switch (term.getOperation()) {
                case NOT_EQUALS:
                case ISNULL:
                    /*
                     * use synthetic term that evaluates to TRUE
                     */
                    return new CompositeSearchTerm(CompositeOperation.NOT)
                        .addSearchTerm(getContactFieldTerm(SingleOperation.EQUALS, ContactField.OBJECT_ID, UUIDs.getUnformattedStringFromRandom()));                    
                default:
                    /*
                     * use synthetic term that evaluates to FALSE
                     */
                    return getContactFieldTerm(SingleOperation.EQUALS, ContactField.OBJECT_ID, UUIDs.getUnformattedStringFromRandom());
            }
        }        
        if (ContactField.CONTEXTID.equals(mappedField)) {
            /*
             * replace numerical context identifier if indicated by mapping
             */
            return replaceConstantOperand(term, config.getMapper().get(mappedField), (id) -> getContextLoginInfo(i(id)));
        }
        if (ContactField.INTERNAL_USERID.equals(mappedField)) {
            /*
             * adjust user id term based on operation
             */
            return prepareUserIdTerm(term);
        }
        return term;
    }

    /**
     * Prepares a composite search term prior handing it down to the {@link SearchTermAdapter} and performing the search.
     * 
     * @param term The search term to prepare
     * @return The prepared search term
     */
    private SearchTerm<?> prepareSearchTerm(CompositeSearchTerm term) throws OXException {
        if (CompositeOperation.NOT.equals(term.getOperation()) && null != term.getOperands() && 
            1 == term.getOperands().length && SingleOperation.ISNULL.equals(term.getOperands()[0].getOperation()) &&
            ContactField.INTERNAL_USERID.equals(config.getMapper().getMappedField((SingleSearchTerm) term.getOperands()[0]))) {
            /*
             * adjust "user id is not null" term with alternative representation 
             */
            return prepareSearchTerm(getContactFieldTerm(SingleOperation.GREATER_OR_EQUAL, ContactField.INTERNAL_USERID, I(1)));
        }
        /*
         * prepare subsequent terms recursively
         */
        CompositeSearchTerm preparedCompositeTerm = new CompositeSearchTerm(term.getOperation());
        for (SearchTerm<?> compositeTerm : term.getOperands()) {
            preparedCompositeTerm.addSearchTerm(prepareSearchTerm(compositeTerm));
        }
        return preparedCompositeTerm;
    }
    
    private SearchTerm<?> prepareUserIdTerm(SingleSearchTerm userIdTerm) throws OXException {
        /*
         * take over user id term as-is for integer attribute & qualify with corresponding context id term as needed
         */
        LdapMapping<? extends Object> userIdMapping = config.getMapper().get(ContactField.INTERNAL_USERID);
        if ((userIdMapping instanceof LdapIntegerMapping)) {
            switch (userIdTerm.getOperation()) {
                case NOT_EQUALS:
                    return new CompositeSearchTerm(CompositeOperation.OR)
                        .addSearchTerm(getPreparedContextIdTerm(SingleOperation.NOT_EQUALS))
                        .addSearchTerm(userIdTerm);
                case ISNULL:
                    return userIdTerm;
                default:
                    return new CompositeSearchTerm(CompositeOperation.AND)
                        .addSearchTerm(getPreparedContextIdTerm(SingleOperation.EQUALS))
                        .addSearchTerm(userIdTerm);
            }        
        }
        /*
         * use adjusted version of term otherwise & qualify with corresponding context id term as needed
         */
        switch (userIdTerm.getOperation()) {
            case EQUALS:
                /*
                 * replace numerical user identifier if indicated by mapping & qualify with corresponding context id term
                 */
                return new CompositeSearchTerm(CompositeOperation.AND)
                    .addSearchTerm(getPreparedContextIdTerm(SingleOperation.EQUALS))
                    .addSearchTerm(replaceConstantOperand(userIdTerm, userIdMapping, (id) -> getUserLoginInfo(i(id))));
            case GREATER_THAN:
            case GREATER_OR_EQUAL:
                Operand<?> constantOperand = optConstantOperand(userIdTerm);
                if (null != constantOperand && (constantOperand.getValue() instanceof Integer) && 1 >= i((Integer) constantOperand.getValue())) {
                    /*
                     * adjust to "is not null" variant & qualify with corresponding context id term
                     */
                    return new CompositeSearchTerm(CompositeOperation.AND)
                        .addSearchTerm(getPreparedContextIdTerm(SingleOperation.EQUALS))
                        .addSearchTerm(getContactFieldTerm(SingleOperation.EQUALS, ContactField.INTERNAL_USERID, "*"));
                }
                throw new UnsupportedOperationException();
            case ISNULL:
                return userIdTerm;
            case NOT_EQUALS:
                /*
                 * replace numerical user identifier if indicated by mapping & qualify with corresponding context id term
                 */                
                return new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(getPreparedContextIdTerm(SingleOperation.NOT_EQUALS))
                    .addSearchTerm(replaceConstantOperand(userIdTerm, userIdMapping, (id) -> getUserLoginInfo(i(id))));
            default:
                throw new UnsupportedOperationException();
        }
    }

    private SingleSearchTerm getPreparedContextIdTerm(SingleOperation operation) throws OXException {
        SingleSearchTerm contextIdTerm = getContactFieldTerm(operation, ContactField.CONTEXTID, I(session.getContextId()));
        LdapMapping<? extends Object> mapping = config.getMapper().get(ContactField.CONTEXTID);
        return replaceConstantOperand(contextIdTerm, mapping, (id) -> getContextLoginInfo(i(id)));
    }

    protected String getUserLoginInfo(int userId) throws OXException {
        return services.getServiceSafe(UserService.class).getUser(userId, session.getContextId()).getLoginInfo();
    }

    protected String getContextLoginInfo(int contextId) throws OXException {
        return services.getServiceSafe(ContextService.class).getContext(contextId).getName();
    }

    private long countContacts(String folderId) throws OXException {
        return searchContacts(folderId, null, new ContactField[] { ContactField.OBJECT_ID }, (Control[]) null).size();
    }

    private static Operand<?> optConstantOperand(SingleSearchTerm term) {
        for (Operand<?> operand : term.getOperands()) {
            if (Operand.Type.CONSTANT.equals(operand.getType())) {
                return operand;
            }
        }
        return null;
    }

    private static SingleSearchTerm replaceConstantOperand(SingleSearchTerm term, LdapMapping<? extends Object> mapping, ErrorAwareFunction<Integer, String> replacementFunction) throws OXException {
        /*
         * check underlying mapping
         */
        if ((mapping instanceof LdapIntegerMapping)) {
            return term; // use as-is
        }
        Operand<?> constantOperand = optConstantOperand(term);
        if (null == constantOperand || false == (constantOperand.getValue() instanceof Integer)) {
            return term; // use as-is
        }
        switch (term.getOperation()) {
            case LESS_OR_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
            case GREATER_OR_EQUAL:
                throw new UnsupportedOperationException();
            default:
                SingleSearchTerm adjustedTerm = new SingleSearchTerm(term.getOperation());
                for (Operand<?> operand : term.getOperands()) {
                    adjustedTerm.addOperand(Operand.Type.CONSTANT.equals(operand.getType()) ?
                        new ConstantOperand<String>(replacementFunction.apply((Integer) constantOperand.getValue())) : operand);
                }
                return adjustedTerm;
        }
    }

}
