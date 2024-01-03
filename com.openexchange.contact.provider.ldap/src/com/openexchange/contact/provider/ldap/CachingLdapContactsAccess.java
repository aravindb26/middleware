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

import static com.openexchange.contact.provider.ldap.Utils.applyFolderExclusions;
import static com.openexchange.contact.provider.ldap.Utils.collectAttributes;
import static com.openexchange.contact.provider.ldap.Utils.getAutocompleteTerm;
import static com.openexchange.contact.provider.ldap.Utils.getContactFieldTerm;
import static com.openexchange.contact.provider.ldap.Utils.getOrFilter;
import static com.openexchange.contact.provider.ldap.Utils.getSortOptions;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.SortOrder;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.ldap.config.ProviderConfig;
import com.openexchange.contact.provider.ldap.mapping.LdapIntegerMapping;
import com.openexchange.contact.provider.ldap.mapping.LdapMapper;
import com.openexchange.contact.provider.ldap.mapping.LdapMapping;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.groupware.search.Order;
import com.openexchange.java.Enums;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.Operand;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.functions.ErrorAwareSupplier;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;

/**
 * {@link CachingLdapContactsAccess} is {@link LdapContactsAccess} which uses a regularly updated {@link Cache} to answer requests if able.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class CachingLdapContactsAccess extends LdapContactsAccess {

    private static final Logger LOG = LoggerFactory.getLogger(CachingLdapContactsAccess.class);

    /** The contact fields that are held in the cache by default */
    // @formatter:off
    private static final EnumSet<ContactField> DEFAULT_CACHED_FIELDS = EnumSet.of(
        ContactField.CONTEXTID, ContactField.FOLDER_ID, ContactField.OBJECT_ID, ContactField.INTERNAL_USERID, ContactField.UID, 
        ContactField.LAST_MODIFIED, ContactField.CREATION_DATE, ContactField.MODIFIED_BY, ContactField.CREATED_BY, 
        ContactField.USE_COUNT, ContactField.PRIVATE_FLAG, ContactField.MARK_AS_DISTRIBUTIONLIST, ContactField.DISTRIBUTIONLIST, 
        ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3, ContactField.DISPLAY_NAME, ContactField.FILE_AS, 
        ContactField.COMPANY, ContactField.YOMI_COMPANY, ContactField.SUR_NAME, ContactField.GIVEN_NAME, ContactField.TITLE, 
        ContactField.YOMI_LAST_NAME, ContactField.YOMI_FIRST_NAME, ContactField.DEPARTMENT)
    ;
    // @formatter:on

    /** The contact fields that are held in the cache by default */
    private static final ContactField[] DEFAULT_CACHED_FIELDS_ARRAY = DEFAULT_CACHED_FIELDS.toArray(new ContactField[DEFAULT_CACHED_FIELDS.size()]);

    /** Fields needed from both the cache and the storage to perform merge operations afterwards */
    private static final List<ContactField> FIELDS_FOR_MERGE = Arrays.asList(ContactField.OBJECT_ID, ContactField.MARK_AS_DISTRIBUTIONLIST, ContactField.DISTRIBUTIONLIST, ContactField.FOLDER_ID, ContactField.LAST_MODIFIED);

    private final Collection<ContactField> cachedFields;
    private final ContactField[] cachedFieldsArray;

    private final String providerId;
    private final LocalCacheRegistry cacheRegistry;
    private final ErrorAwareSupplier<List<Contact>> cacheLoader;

    /**
     * Initializes a new {@link CachingLdapContactsAccess}.
     *
     * @param services The {@link ServiceLookup}
     * @param config The {@link ProviderConfig}
     * @param session The users {@link Session}
     * @param account The {@link ContactsAccount}
     * @param parameters The {@link ContactsParameters}
     * @param cacheRegistry The {@link LocalCacheRegistry}
     * @param providerId The provider identifier
     * @param The {@link LocalCacheRegistry}
     */
    public CachingLdapContactsAccess(ServiceLookup services, ProviderConfig config, Session session, ContactsAccount account, ContactsParameters parameters, LocalCacheRegistry cacheRegistry, String providerId) {
        super(services, config, session, account, parameters);
        this.cacheRegistry = cacheRegistry;
        this.providerId = providerId;
        Optional<String> optCachedFields = config.optCacheConfig().get().optCachedFields();
        if (optCachedFields.isPresent()) {
            List<ContactField> contactFields = Enums.parseCsv(ContactField.class, optCachedFields.get());
            cachedFields = contactFields;
            cachedFieldsArray = contactFields.toArray(new ContactField[contactFields.size()]);
        } else {
            cachedFields = DEFAULT_CACHED_FIELDS;
            cachedFieldsArray = DEFAULT_CACHED_FIELDS_ARRAY;
        }
        cacheLoader = () -> getAll();
    }

    @Override
    public List<Contact> getContacts(List<ContactID> contactIDs) throws OXException {
        if (isCached(super.getFields())) {
            Optional<LocalCache> optCache = getCache();
            if (optCache.isPresent()) {
                List<Contact> result = new ArrayList<Contact>(contactIDs.size());
                for (ContactID contactId : contactIDs) {
                    Contact cachedContact = optCache.get().get(contactId.getFolderID(), contactId.getObjectID());
                    if (cachedContact != null) {
                        LOG.debug("Get contact {} from cache", contactId);
                        result.add(postProcess(cachedContact));
                    }
                }
                return result;
            }
        }
        return postProcess(mergeCacheData(super.getContacts(contactIDs)));
    }

    @Override
    public List<Contact> getContacts(String folderId) throws OXException {
        if (isCached(super.getFields())) {
            Optional<LocalCache> optCache = getCache();
            if (optCache.isPresent()) {
                return postProcess(sortAndSlice(new ArrayList<>(optCache.get().getByFolder(folderId))));
            }
        }
        return postProcess(mergeCacheData(super.getContacts(folderId)));
    }

    @Override
    public List<Contact> searchContacts(ContactsSearchObject contactSearch) throws OXException {
        SearchTerm<?> searchTerm = new ContactsSearchAdapter(config.getMapper(), warnings).getSearchTerm(contactSearch);
        if (isCached(super.getFields()) && isCached(searchTerm)) {
            Optional<LocalCache> optCache = getCache();
            if (optCache.isPresent()) {
                List<Contact> contacts = filter(getCachedContacts(optCache.get(), contactSearch.getFolders()), searchTerm, ServerSessionAdapter.valueOf(session).getUser().getLocale());
                return postProcess(applyFolderExclusions(contacts, contactSearch.getExcludeFolders()));
            }
        }
        return postProcess(mergeCacheData(super.searchContacts(contactSearch)));
    }

    @Override
    public <O> List<Contact> searchContacts(List<String> folderIds, SearchTerm<O> term) throws OXException {
        SearchTerm<?> termForCache = null == term ? null : prepareSearchTermForCache(term);
        if (isCached(super.getFields()) && isCached(termForCache)) {
            Optional<LocalCache> optCache = getCache();
            if (optCache.isPresent()) {
                List<Contact> contacts = filter(getCachedContacts(optCache.get(), folderIds), termForCache, ServerSessionAdapter.valueOf(session).getUser().getLocale());
                return postProcess(contacts);
            }
        }
        return postProcess(mergeCacheData(super.searchContacts(folderIds, term)));
    }

    @Override
    public List<Contact> autocompleteContacts(List<String> folderIds, String query) throws OXException {
        /*
         * check if search can be performed within local cache
         */
        List<ContactField> autocompleteFields = getAutocompleteFields(true);
        if (false == isCached(autocompleteFields)) {
            return super.autocompleteContacts(folderIds, query);
        }
        Optional<LocalCache> localCache = getCache();
        if (false == localCache.isPresent()) {
            return super.autocompleteContacts(folderIds, query);
        }
        /*
         * transform query to search term & perform search in cache
         */
        return searchContacts(folderIds, getAutocompleteTerm(query, autocompleteFields, getWarnings(), getMinimumSearchCharacters()));
    }

    @Override
    ContactField[] getFields(ContactField... additionalFields) {
        // only return none cached fields
        return getUnknownFields(super.getFields(additionalFields));
    }

    // -------------------------------------------------- Cache helper methods ----------------------------------------------------

    /**
     * Sorts and slices the given cache results
     *
     * @param contacts The contacts to sort and slice
     * @return The sorted and sliced contacts
     * @throws OXException in case the sorting arguments are invalid
     */
    private List<Contact> sortAndSlice(List<Contact> contacts) throws OXException {
        SortOptions sortOptions = getSortOptions(parameters);
        if (null != sortOptions && false == SortOptions.EMPTY.equals(sortOptions)) {
            int sizeLimit = sortOptions.getLimit();
            int rangeStart = sortOptions.getRangeStart();
            SortOrder[] order = sortOptions.getOrder();
            Locale locale = ServerSessionAdapter.valueOf(session).getUser().getLocale();
            Stream<Contact> stream = contacts.stream();
            if (order != null && order.length > 0) {
                stream = stream.sorted(getComparator(order, locale));
            }
            if (rangeStart > 0) {
                stream = stream.skip(rangeStart);
            }
            if (sizeLimit > 0) {
                stream = stream.limit(sizeLimit);
            }
            return stream.collect(Collectors.toList());
        }

        return contacts;
    }

    /**
     * Gets a contact comparator suitable for the supplied sort order(s).
     *
     * @param sortOrder The requested sort order
     * @param locale The locale, or <code>null</code> if not specified
     * @return A suitable contact comparator
     */
    private Comparator<Contact> getComparator(SortOrder[] sortOrder, Locale locale) {
        return new Comparator<Contact>() {

            @Override
            public int compare(Contact o1, Contact o2) {
                for (SortOrder order : sortOrder) {
                    int comparison = 0;

                    LdapMapping<? extends Object> mapping = config.getMapper().opt(order.getBy());
                    if (null != mapping) {
                        comparison = mapping.compare(o1, o2, locale);
                    }
                    if (0 != comparison) {
                        return Order.DESCENDING.equals(order.getOrder()) ? -1 * comparison : comparison;
                    }
                }
                return 0;
            }
        };
    }

    /**
     * Filters out the contacts which doesn't match the given search term
     *
     * @param <O> The search term type
     * @param contacts The contacts to filter
     * @param term The search term
     * @param locale The users locale
     * @return The filtered list of contacts
     */
    private <O> List<Contact> filter(List<Contact> contacts, SearchTerm<O> term, Locale locale) {
        return null != contacts && null != term ? new SearchFilter(term, locale, prepareMapperForCache(config.getMapper())).filter(contacts) : contacts;
    }

    /**
     * Gets the optional cache
     *
     * @return The optional cache
     * @throws OXException in case an error occurred while obtaining the cache
     */
    private Optional<LocalCache> getCache() throws OXException {
        return cacheRegistry.optCache(providerId, config, cacheLoader);
    }

    /**
     * Gets all contacts. Primarily used to pre-load the cache.
     *
     * @return All contacts of the LDAP server
     * @throws OXException If not all contacts could be retrieved
     */
    private List<Contact> getAll() throws OXException {
        Set<String> collectedAttributes = new HashSet<>(cachedFieldsArray.length);
        config.getMapper().collectAttributes(cachedFieldsArray, collectedAttributes);
        Map<String, Filter> filtersPerFolderId = getFiltersPerFolderId(null);
        filtersPerFolderId.values().forEach(f -> collectAttributes(f, collectedAttributes));
        String[] attributes = collectedAttributes.toArray(new String[collectedAttributes.size()]);

        ContactField oldOrderBy = parameters.get(ContactsParameters.PARAMETER_ORDER_BY, ContactField.class);
        Integer oldRightHandLimit = parameters.get(ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT, Integer.class);
        Integer oldLeftHandLimit = parameters.get(ContactsParameters.PARAMETER_LEFT_HAND_LIMIT, Integer.class);
        try {
            parameters.set(ContactsParameters.PARAMETER_ORDER_BY, null);
            parameters.set(ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT, null);
            parameters.set(ContactsParameters.PARAMETER_LEFT_HAND_LIMIT, null);
            return super.search((result) -> getContacts(result, filtersPerFolderId, cachedFieldsArray, true), getCommonContactSearchScope(), getCommonContactFilter(), attributes);
        } finally {
            parameters.set(ContactsParameters.PARAMETER_ORDER_BY, oldOrderBy);
            parameters.set(ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT, oldRightHandLimit);
            parameters.set(ContactsParameters.PARAMETER_LEFT_HAND_LIMIT, oldLeftHandLimit);
        }
    }

    /**
     * Gets a value indicating whether all of the supplied fields are present
     * in the cache or not.
     *
     * @param requestedFields the contact fields
     * @return <code>true</code>, if the fields are cached, <code>false</code> otherwise
     */
    private boolean isCached(ContactField[] requestedFields) {
        return null != requestedFields && isCached(Arrays.asList(requestedFields));
    }

    /**
     * Gets a value indicating whether all of the supplied fields are present
     * in the cache or not.
     *
     * @param requestedFields the contact fields
     * @return <code>true</code>, if the fields are cached, <code>false</code> otherwise
     */
    private boolean isCached(List<ContactField> requestedFields) {
        if (requestedFields == null) {
            return false;
        }
        ArrayList<ContactField> tmp = new ArrayList<ContactField>(requestedFields);
        tmp.remove(ContactField.USE_COUNT);
        return cachedFields.containsAll(tmp);
    }

    /**
     * Gets a value indicating whether all of the fields referred by the
     * supplied search term are present in the cache or not.
     *
     * @param term The term to check
     * @return <code>true</code>, if the fields are cached, <code>false</code> otherwise
     * @throws IllegalArgumentException If neither an 'SingleSearchTerm' nor 'CompositeSearchTerm'
     */
    private boolean isCached(SearchTerm<?> term) {
        if (null != term) {
            if ((term instanceof SingleSearchTerm)) {
                return isCached((SingleSearchTerm) term);
            } else if ((term instanceof CompositeSearchTerm)) {
                return isCached((CompositeSearchTerm) term);
            } else {
                throw new IllegalArgumentException("Need either an 'SingleSearchTerm' or 'CompositeSearchTerm'.");
            }
        }
        return true;
    }

    /**
     * Gets a value indicating whether all of the fields referred by the
     * supplied search term are present in the cache or not.
     *
     * @param term the term to check
     * @return <code>true</code>, if the fields are cached, <code>false</code> otherwise
     */
    private boolean isCached(SingleSearchTerm term) {
        if (null != term.getOperands()) {
            for (Operand<?> operand : term.getOperands()) {
                if (Operand.Type.COLUMN.equals(operand.getType())) {
                    if ((operand.getValue() instanceof ContactField)) {
                        if (false == cachedFields.contains(operand.getValue())) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Gets a value indicating whether all of the fields referred by the
     * supplied search term are present in the cache or not.
     *
     * @param term the term to check
     * @return <code>true</code>, if the fields are cached, <code>false</code> otherwise
     */
    private boolean isCached(CompositeSearchTerm term) {
        if (null != term.getOperands()) {
            for (SearchTerm<?> searchTerm : term.getOperands()) {
                if (false == isCached(searchTerm)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates an array of contact fields that are not covered by the cached data.
     *
     * @param requestedFields the fields to check
     * @return the unknown fields
     */
    public ContactField[] getUnknownFields(ContactField[] requestedFields) {
        if (null == requestedFields || 0 >= requestedFields.length) {
            return requestedFields;
        }

        // @formatter:off
        return Arrays.asList(requestedFields).stream()
                                             .filter(reqField -> FIELDS_FOR_MERGE.contains(reqField) || cachedFields.contains(reqField) == false)
                                             .toArray(ContactField[]::new);
        // @formatter:on
    }

    /**
     * Merges the given contacts with the data cached data
     *
     * @param contacts The contacts to merge
     * @return A list of completely filled contacts
     * @throws OXException
     */
    private List<Contact> mergeCacheData(List<Contact> contacts) throws OXException {
        if (null == contacts || contacts.isEmpty()) {
            return contacts;
        }
        List<Contact> result = new ArrayList<Contact>(contacts.size());
        Optional<LocalCache> cache = getCache();
        LdapMapper mapperForCache = prepareMapperForCache(config.getMapper());
        for (Contact loadedContact : contacts) {
            Contact cachedContact = null;
            if (cache.isPresent()) {
                cachedContact = cache.get().get(loadedContact.getFolderId(), loadedContact.getId());
            }
            if (null == cachedContact) {
                LOG.debug("Cache miss. Loading complete contact instead for contact with identifier {}", loadedContact.getId());
                // not cached, try to load completely as fallback
                Contact fallbackContact = null;
                try {
                    fallbackContact = getCompleteContact(loadedContact.getFolderId(), loadedContact.getId());
                } catch (OXException e) {
                    if (false == e.isNotFound()) {
                        throw e;
                    }
                }
                if (null != fallbackContact) {
                    result.add(fallbackContact);
                }
            } else {
                // merge information from cache
                mapperForCache.mergeDifferences(loadedContact, cachedContact);
                result.add(loadedContact);
                LOG.debug("Merged cached entry for contact with identifier {}", loadedContact.getId());
            }
        }
        return result;
    }

    /**
     * Gets a contact with all requested fields. This is primarily used to get contact data of cache misses
     *
     * @param folderId The folder identifier
     * @param contactId The contact identifier
     * @return The contact with all contact data
     * @throws OXException If the contact couldn't be found
     */
    public Contact getCompleteContact(String folderId, String contactId) throws OXException {
        LDAPConnectionProvider connectionProvider = getConnectionProvider();
        LDAPConnection connection = connectionProvider.getConnection(session);
        try {
            Filter filter = getOrFilter(config.getMapper(), ContactField.OBJECT_ID, Arrays.asList(contactId));
            List<Contact> results = searchContacts(folderId, filter, super.getFields(ContactField.OBJECT_ID));
            if (results.size() == 1) {
                return results.get(0);
            }
        } finally {
            connectionProvider.back(connection);
        }
        throw ContactsProviderExceptionCodes.CONTACT_NOT_FOUND_IN_FOLDER.create(folderId, contactId);
    }

    private static List<Contact> getCachedContacts(LocalCache cache, Collection<String> folderIds) {
        if (null != folderIds && false == folderIds.isEmpty()) {
            List<Contact> contacts = new ArrayList<Contact>();
            folderIds.forEach(fid -> contacts.addAll(cache.getByFolder(fid)));
        }
        return new ArrayList<Contact>(cache.getAll());
    }

    /**
     * Post-processes a contact obtained from the cache prior returning it to the caller.
     * <p/>
     * This involves creating a contact copy, then context- and user identifiers are stripped in case it denotes a context different
     * from the current session. The context id is always removed afterwards as it is no longer needed.
     * 
     * @param contact The contact to post-process
     * @return A copied contact with adjustments performed as needed
     */
    private Contact postProcess(Contact contact) {
        if (null == contact) {
            return contact;
        }
        Contact copiedContact;
        try {
            copiedContact = config.getMapper().copy(contact, null, (ContactField[]) null);
            copiedContact.setId(contact.getId());
            copiedContact.setFolderId(contact.getFolderId());
            copiedContact.setContextId(contact.getContextId());
            copiedContact.setInternalUserId(contact.getInternalUserId());
        } catch (OXException e) {
            LOG.warn("Unexpected error copying contact data, falling back to original contact", e);
            return contact;
        }
        removeForeignUserAndContextId(copiedContact);
        return copiedContact;
    }

    /**
     * Post-processes a list of contacts obtained from the cache prior returning them to the caller.
     * <p/>
     * This involves creating a contact copy, then context- and user identifiers are stripped in case it denotes a context different
     * from the current session. The context id is always removed afterwards as it is no longer needed.
     * 
     * @param contacts The contacts to post-process
     * @return A list of adjusted contact copies
     */
    private List<Contact> postProcess(List<Contact> contacts) {
        if (null == contacts || contacts.isEmpty()) {
            return contacts;
        }
        List<Contact> postProcessedContacts = new ArrayList<Contact>(contacts.size());
        for (Contact contact : contacts) {
            postProcessedContacts.add(postProcess(contact));
        }
        return postProcessedContacts;
    }

    /**
     * Prepares a search term prior using it against the in-memory contact cache.
     * 
     * @param term The search term to prepare
     * @return The prepared search term
     */
    private SearchTerm<?> prepareSearchTermForCache(SearchTerm<?> term) throws OXException {
        if ((term instanceof SingleSearchTerm)) {
            return prepareSearchTermForCache((SingleSearchTerm) term);
        }
        if ((term instanceof CompositeSearchTerm)) {
            return prepareSearchTermForCache((CompositeSearchTerm) term);
        }
        throw new IllegalArgumentException("Need either an 'SingleSearchTerm' or 'CompositeSearchTerm'.");
    }

    /**
     * Prepares a single search using it against the in-memory contact cache.
     * 
     * @param term The search term to prepare
     * @return The prepared search term
     */
    private SearchTerm<?> prepareSearchTermForCache(SingleSearchTerm term) throws OXException {
        ContactField mappedField = config.getMapper().getMappedField(term);
        if (ContactField.INTERNAL_USERID.equals(mappedField)) {
            /*
             * adjust user id term based on operation & qualify with corresponding context id term as needed
             */
            switch (term.getOperation()) {
                case NOT_EQUALS:
                    return new CompositeSearchTerm(CompositeOperation.OR)
                        .addSearchTerm(getContactFieldTerm(SingleOperation.NOT_EQUALS, ContactField.CONTEXTID, I(session.getContextId())))
                        .addSearchTerm(term);
                case ISNULL:
                    return term;
                default:
                    return new CompositeSearchTerm(CompositeOperation.AND)
                        .addSearchTerm(getContactFieldTerm(SingleOperation.EQUALS, ContactField.CONTEXTID, I(session.getContextId())))
                        .addSearchTerm(term);
            }
        }
        return term;
    }

    /**
     * Prepares a composite search term prior using it against the in-memory contact cache.
     * 
     * @param term The search term to prepare
     * @return The prepared search term
     */
    private SearchTerm<?> prepareSearchTermForCache(CompositeSearchTerm term) throws OXException {
        if (CompositeOperation.NOT.equals(term.getOperation()) && null != term.getOperands() && 
            1 == term.getOperands().length && SingleOperation.ISNULL.equals(term.getOperands()[0].getOperation()) && 
            ContactField.INTERNAL_USERID.equals(config.getMapper().getMappedField((SingleSearchTerm) term.getOperands()[0]))) {
            /*
             * adjust "user id is not null" term with alternative representation
             */
            return prepareSearchTermForCache(getContactFieldTerm(SingleOperation.GREATER_OR_EQUAL, ContactField.INTERNAL_USERID, I(1)));
        }
        /*
         * prepare subsequent terms recursively
         */
        CompositeSearchTerm preparedCompositeTerm = new CompositeSearchTerm(term.getOperation());
        for (SearchTerm<?> compositeTerm : term.getOperands()) {
            preparedCompositeTerm.addSearchTerm(prepareSearchTermForCache(compositeTerm));
        }
        return preparedCompositeTerm;
    }

    /**
     * Prepares an LDAP mapper to be used for contact objects held in the cache, which may be necessary if indirect mappings are used
     * for the context- and/or user identifier mappings.
     * 
     * @param mapper The underlying mapper
     * @return The prepared mapper
     */
    private static LdapMapper prepareMapperForCache(LdapMapper mapper) {
        /*
         * convert context- and user id mappings to straight integer mappings if needed
         */
        EnumMap<ContactField, LdapMapping<? extends Object>> adjustedMappings = new EnumMap<ContactField, LdapMapping<? extends Object>>(ContactField.class);
        EnumMap<ContactField, LdapMapping<? extends Object>> mappings = mapper.getLdapMappings();
        LdapMapping<? extends Object> mapping = mappings.get(ContactField.INTERNAL_USERID);
        if (null != mapping && false == (mapping instanceof LdapIntegerMapping)) {
            adjustedMappings.put(ContactField.INTERNAL_USERID, new LdapIntegerMapping(mapping.getAttributeNames()) {

                @Override
                public void set(Contact contact, Integer value) {
                    contact.setInternalUserId(null == value ? 0 : i(value));
                }

                @Override
                public boolean isSet(Contact contact) {
                    return contact.containsInternalUserId();
                }

                @Override
                public Integer get(Contact contact) {
                    return I(contact.getInternalUserId());
                }

                @Override
                public void remove(Contact contact) {
                    contact.removeInternalUserId();
                }
            });
        }
        mapping = mappings.get(ContactField.CONTEXTID);
        if (null != mapping && false == (mapping instanceof LdapIntegerMapping)) {
            adjustedMappings.put(ContactField.CONTEXTID, new LdapIntegerMapping(mapping.getAttributeNames()) {

                @Override
                public void set(Contact contact, Integer value) {
                    contact.setContextId(null == value ? 0 : i(value));
                }

                @Override
                public boolean isSet(Contact contact) {
                    return contact.containsContextId();
                }

                @Override
                public Integer get(Contact contact) {
                    return I(contact.getContextId());
                }

                @Override
                public void remove(Contact contact) {
                    contact.removeContextID();
                }
            });
        }
        if (false == adjustedMappings.isEmpty()) {
            EnumMap<ContactField, LdapMapping<? extends Object>> preparedMappings = new EnumMap<ContactField, LdapMapping<? extends Object>>(mapper.getLdapMappings());
            preparedMappings.putAll(adjustedMappings);
            return new LdapMapper(preparedMappings);
        }
        return mapper;
    }

}
