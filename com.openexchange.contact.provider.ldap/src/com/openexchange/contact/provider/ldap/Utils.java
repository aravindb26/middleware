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

import static com.openexchange.contact.common.ContactsParameters.PARAMETER_LEFT_HAND_LIMIT;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_ORDER;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_ORDER_BY;
import static com.openexchange.contact.common.ContactsParameters.PARAMETER_RIGHT_HAND_LIMIT;
import static com.openexchange.contact.provider.ldap.mapping.LdapMapPropertyMapping.PROP_LOGIN_CONTEXT_INFO;
import static com.openexchange.contact.provider.ldap.mapping.LdapMapPropertyMapping.PROP_LOGIN_USER_INFO;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.io.BaseEncoding;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.SortOptions;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.ldap.mapping.LdapMapPropertyMapping;
import com.openexchange.contact.provider.ldap.mapping.LdapMapper;
import com.openexchange.contact.provider.ldap.mapping.LdapMapping;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.ContactUtil;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.search.Order;
import com.openexchange.java.Charsets;
import com.openexchange.java.SimpleTokenizer;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.server.ServiceLookup;
import com.openexchange.user.UserService;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.controls.SortKey;

/**
 * {@link Utils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Prevent initialization.
     */
    private Utils() {
        super();
    }

    /**
     * Resolves the internal context- and user identifiers within the supplied contact based on the information found either in the
     * numerical id properties, or in the special extended properties {@link LdapMapPropertyMapping#PROP_LOGIN_USER_INFO} and
     * {@link LdapMapPropertyMapping#PROP_LOGIN_CONTEXT_INFO} as used my the LDAP mapper.
     * 
     * @param services A service lookup reference
     * @param contact The contact to resolve the context- and user ids in
     * @return The passed contact reference, with resolved or not set context- and user identifiers
     */
    static Contact resolveUserAndContextId(ServiceLookup services, Contact contact) {
        int contextId = contact.getContextId();
        int userId = contact.getInternalUserId();
        String loginContextInfo = contact.removeProperty(PROP_LOGIN_CONTEXT_INFO);
        String loginUserInfo = contact.removeProperty(PROP_LOGIN_USER_INFO);
        try {
            if (0 >= contextId && Strings.isNotEmpty(loginContextInfo)) {
                contextId = services.getServiceSafe(ContextService.class).getContextId(loginContextInfo);
                if (0 >= contextId) {
                    LOG.warn("Unable to resolve context login info \"{}\" to a context.", loginContextInfo);
                } else {
                    LOG.trace("Successfully resolved context login info \"{}\" to context {}.", loginContextInfo, I(contextId));
                }
            }
            if (0 < contextId && 0 >= userId && Strings.isNotEmpty(loginUserInfo)) {
                Context context = services.getServiceSafe(ContextService.class).getContext(contextId);
                userId = services.getServiceSafe(UserService.class).getUserId(loginUserInfo, context);
                LOG.trace("Successfully resolved user login info \"{}\" to user {} in context {}.", loginUserInfo, I(userId), I(contextId));
            }
        } catch (OXException e) {
            LOG.warn("Error resolving context and user id for {}, treating as external contact.", contact, e);
        }
        if (0 < userId && 0 < contextId) {
            contact.setInternalUserId(userId);
            contact.setContextId(contextId);
        } else {
            contact.removeInternalUserId();
            contact.removeContextID();
        }
        return contact;
    }

    /**
     * Generates a display name for the supplied contact, consisting of it's constructed display name and email address.
     * 
     * @param contact The contact to generate the display name for
     * @return The display name
     */
    static String generateDisplayName(Contact contact) {
        String email = contact.getEmail1();
        if (Strings.isEmpty(email)) {
            email = contact.getEmail2();
            if (Strings.isEmpty(email))
                email = contact.getEmail3();
        }
        ContactUtil.generateDisplayName(contact);
        String displayName = contact.getDisplayName();
        if (Strings.isEmpty(email)) {
            return displayName;
        }
        if (Strings.isEmpty(displayName)) {
            return email;
        }
        return String.format("%1$s <%2$s>", displayName, email);
    }

    static void collectAttributes(Filter filter, Collection<String> attributes) {
        String attributeName = filter.getAttributeName();
        if (null != attributeName) {
            attributes.add(attributeName);
        }
        Filter notComponent = filter.getNOTComponent();
        if (null != notComponent) {
            collectAttributes(notComponent, attributes);
        }
        Filter[] components = filter.getComponents();
        if (null != components) {
            for (Filter component : components) {
                collectAttributes(component, attributes);
            }
        }
    }

    static List<String> selectFolderIds(Entry entry, Map<String, Filter> filtersPerFolderId) {
        List<String> folderIds = new ArrayList<String>();
        for (Map.Entry<String, Filter> filterForFolderId : filtersPerFolderId.entrySet()) {
            try {
                if (filterForFolderId.getValue().matchesEntry(entry)) {
                    folderIds.add(filterForFolderId.getKey());
                }
            } catch (LDAPException e) {
                LOG.warn("Unexpected error checking if filter {} matches entry {}", filterForFolderId.getValue(), entry, e);
            }
        }
        return folderIds;
    }

    static Filter addRequireEMailFilter(Filter filter, LdapMapper mapper) throws OXException {
        List<Filter> hasEmailFilters = new ArrayList<Filter>();
        for (ContactField field : new ContactField[] { ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3 }) {
            LdapMapping<? extends Object> mapping = mapper.opt(field);
            if (null != mapping) {
                for (String attributeName : mapping.getAttributeNames()) {
                    hasEmailFilters.add(Filter.createPresenceFilter(attributeName));
                }
            }
        }
        LdapMapping<? extends Object> mapping = mapper.opt(ContactField.MARK_AS_DISTRIBUTIONLIST);
        if (null != mapping) {
            SingleSearchTerm isDistributionlistTerm = new SingleSearchTerm(SingleOperation.EQUALS)
                .addOperand(new ContactFieldOperand(ContactField.MARK_AS_DISTRIBUTIONLIST))
                .addOperand(new ConstantOperand<Boolean>(Boolean.TRUE));
            hasEmailFilters.add(mapper.getFilter(isDistributionlistTerm));
        }
        if (hasEmailFilters.isEmpty()) {
            throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create("No email mapping defined");
        }
        return Filter.createANDFilter(filter, Filter.createORFilter(hasEmailFilters));
    }

    static SearchRequest applySortOptions(SearchRequest searchRequest, ContactsParameters parameters, LdapMapper mapper) throws OXException {
        SortOptions sortOptions = getSortOptions(parameters);
        int sizeLimit = getSizeLimit(sortOptions);
        if (0 < sizeLimit) {
            searchRequest.setSizeLimit(sizeLimit);
        }
        SortKey[] sortKeys = mapper.getSortKeys(sortOptions);
        if (null != sortKeys && 0 < sortKeys.length) {
            searchRequest.setControls(new ServerSideSortRequestControl(sortKeys));
        }
        return searchRequest;
    }

    private static Filter createSubInitialFilters(String[] attributes, String subInitial) {
        if (1 == attributes.length) {
            return Filter.createSubInitialFilter(attributes[0], subInitial);
        }
        List<Filter> filters = new ArrayList<Filter>(attributes.length);
        for (String attribute : attributes) {
            filters.add(Filter.createSubInitialFilter(attribute, subInitial));
        }
        return Filter.createORFilter(filters);
    }

    static List<String> extractPatterns(String query, Collection<OXException> warnings, int minimumPatternLength) {
        /*
         * extract patterns & remove too short patterns
         */
        List<String> patterns = SimpleTokenizer.tokenize(query);
        for (Iterator<String> iterator = patterns.iterator(); iterator.hasNext();) {
            String pattern = iterator.next();
            if (minimumPatternLength > pattern.length()) {
                iterator.remove();
                if (null != warnings) {
                    warnings.add(ContactExceptionCodes.IGNORED_PATTERN.create(ContactExceptionCodes.TOO_FEW_SEARCH_CHARS.create(I(minimumPatternLength)), pattern));
                }
            }
        }
        return patterns;
    }

    static Filter getAutocompleteFilter(String query, String[] autocompleteAttributes, Collection<OXException> warnings, int minimumPatternLength) throws OXException {
        /*
         * extract & check patterns from query
         */
        List<String> patterns = extractPatterns(query, warnings, minimumPatternLength);
        if (patterns.isEmpty()) {
            throw ContactExceptionCodes.TOO_FEW_SEARCH_CHARS.create(new Exception("No attribute filters for query: " + query), I(minimumPatternLength));
        }
        if (1 == patterns.size()) {
            return createSubInitialFilters(autocompleteAttributes, patterns.get(0));
        }
        /*
         * add alternative attribute filter for each token
         */
        List<Filter> filters = new ArrayList<Filter>(autocompleteAttributes.length * patterns.size());
        for (String pattern : patterns) {
            filters.add(createSubInitialFilters(autocompleteAttributes, pattern));
        }
        /*
         * combine attribute filters for each token with OR filter for whole query
         */
        Filter queryFilter = createSubInitialFilters(autocompleteAttributes, Strings.trimEnd(query, '*'));
        return Filter.createORFilter(Filter.createANDFilter(filters), queryFilter);
    }

    /**
     * Converts an auto-complete query as supplied by the client into a search term targeting the configured contact fields.
     * 
     * @param query The incoming auto-complete query
     * @param autocompleteFields The fields considered for the auto-complete operation
     * @return The corresponding search term
     */
    static SearchTerm<?> getAutocompleteTerm(String query, List<ContactField> autocompleteFields, Collection<OXException> warnings, int minimumPatternLength) throws OXException {
        /*
         * extract & check patterns from query
         */
        List<String> patterns = extractPatterns(query, warnings, minimumPatternLength);
        if (patterns.isEmpty()) {
            throw ContactExceptionCodes.TOO_FEW_SEARCH_CHARS.create(new Exception("No attribute filters for query: " + query), I(minimumPatternLength));
        }
        if (1 == patterns.size()) {
            return createStartsWithTerm(autocompleteFields, patterns.get(0));
        }
        /*
         * add alternative search term for each token
         */
        CompositeSearchTerm andTerm = new CompositeSearchTerm(CompositeOperation.AND);
        for (String pattern : patterns) {
            andTerm.addSearchTerm(createStartsWithTerm(autocompleteFields, pattern));
        }
        /*
         * combine search term for each token with OR term for whole query
         */
        return new CompositeSearchTerm(CompositeOperation.OR).addSearchTerm(andTerm).addSearchTerm(createStartsWithTerm(autocompleteFields, query));
    }

    private static SearchTerm<?> createStartsWithTerm(List<ContactField> fields, String query) {
        String pattern = query;
        if (Strings.isNotEmpty(pattern) && false == "*".equals(pattern) && '*' != pattern.charAt(pattern.length() - 1)) {
            pattern = pattern + "*";
        }
        ConstantOperand<String> constantOperand = new ConstantOperand<String>(pattern);
        CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
        for (ContactField field : fields) {
            orTerm.addSearchTerm(new SingleSearchTerm(SingleOperation.EQUALS).addOperand(new ContactFieldOperand(field)).addOperand(constantOperand));
        }
        return orTerm;
    }

    static <V> SingleSearchTerm getContactFieldTerm(SingleOperation operation, ContactField field, V value) {
        return new SingleSearchTerm(operation).addOperand(new ContactFieldOperand(field)).addOperand(new ConstantOperand<V>(value));
    }

    static Map<String, List<String>> getObjectIdsPerFolderId(List<ContactID> contactIDs) {
        if (null == contactIDs || contactIDs.isEmpty()) {
            return Collections.emptyMap();
        }
        if (1 == contactIDs.size()) {
            ContactID contactID = contactIDs.get(0);
            return Collections.singletonMap(contactID.getFolderID(), Collections.singletonList(contactID.getObjectID()));
        }
        Map<String, List<String>> objectsIdsPerFolderId = new HashMap<String, List<String>>();
        for (ContactID contactID : contactIDs) {
            com.openexchange.tools.arrays.Collections.put(objectsIdsPerFolderId, contactID.getFolderID(), contactID.getObjectID());
        }
        return objectsIdsPerFolderId;
    }

    static List<Contact> applyFolderExclusions(List<Contact> contacts, Set<String> excludedFolders) {
        if (null != contacts && 0 < contacts.size() && null != excludedFolders && false == excludedFolders.isEmpty()) {
            for (Iterator<Contact> iterator = contacts.iterator(); iterator.hasNext();) {
                Contact contact = iterator.next();
                if (excludedFolders.contains(contact.getFolderId())) {
                    iterator.remove();
                }
            }
        }
        return contacts;
    }

    static Filter getOrFilter(LdapMapper mapper, ContactField field, List<String> possibleValues) throws OXException {
        if (null == possibleValues || possibleValues.isEmpty()) {
            return null;
        }
        List<Filter> filters = new ArrayList<Filter>(possibleValues.size());
        for (String value : possibleValues) {
            SingleSearchTerm term = new SingleSearchTerm(SingleOperation.EQUALS)
                .addOperand(new ContactFieldOperand(field)).addOperand(new ConstantOperand<String>(value));
            filters.add(mapper.getFilter(term));
        }
        return 1 == filters.size() ? filters.get(0) : Filter.createORFilter(filters);
    }

    static String getContactId(String baseDn, String dn) throws LDAPException {
        return null == dn ? null : BaseEncoding.base64().omitPadding().encode(stripBaseDN(dn, baseDn).getBytes(Charsets.UTF_8));
    }

    static String getDN(String baseDn, String contactId) {
        return null == contactId ? null : appendBaseDN(new String(BaseEncoding.base64().omitPadding().decode(contactId), Charsets.UTF_8), baseDn);
    }

    static SortOptions getSortOptions(ContactsParameters parameters) throws OXException {
        ContactField orderBy = parameters.get(PARAMETER_ORDER_BY, ContactField.class);
        if (null == orderBy) {
            return SortOptions.EMPTY;
        }
        Order order = parameters.get(PARAMETER_ORDER, Order.class, Order.ASCENDING);
        SortOptions options = new SortOptions(SortOptions.Order(orderBy, order));

        int leftHandLimit = i(parameters.get(PARAMETER_LEFT_HAND_LIMIT, Integer.class, I(0)));
        if (0 < leftHandLimit) {
            options.setRangeStart(leftHandLimit);
        }
        int rightHandLimit = i(parameters.get(PARAMETER_RIGHT_HAND_LIMIT, Integer.class, I(-1)));
        if (-1 != rightHandLimit) {
            if (rightHandLimit < leftHandLimit) {
                throw ContactsProviderExceptionCodes.INVALID_RANGE_LIMITS.create();
            }
            options.setLimit(rightHandLimit - leftHandLimit);
        }
        return options;
    }

    static String stripBaseDN(String dn, String baseDN) throws LDAPException {
        if (dn.endsWith(baseDN)) {
            String strippedDN = dn.substring(0, dn.length() - baseDN.length()).trim();
            return Strings.trimEnd(strippedDN, ',');
        }
        return stripBaseDN(new DN(dn), new DN(baseDN)).toString();
    }

    static String appendBaseDN(String dn, String baseDN) {
        return dn + ',' + baseDN;
    }

    static DN stripBaseDN(DN dn, DN baseDN) {
        LinkedList<RDN> rdns = new LinkedList<RDN>(Arrays.asList(dn.getRDNs()));
        RDN[] baseRdns = baseDN.getRDNs();
        for (int i = baseRdns.length; i-- > 0;) {
            if (baseRdns[i].equals(rdns.peekLast())) {
                rdns.removeLast();
            } else {
                break;
            }
        }
        return new DN(rdns);
    }

    static DN appendBaseDN(DN dn, DN baseDN) {
        LinkedList<RDN> rdns = new LinkedList<RDN>(Arrays.asList(dn.getRDNs()));
        for (RDN baseRdn : baseDN.getRDNs()) {
            rdns.add(baseRdn);
        }
        return new DN(rdns);
    }

    static int getSizeLimit(SortOptions sortOptions) {
        if (null != sortOptions && false == SortOptions.EMPTY.equals(sortOptions) && 0 < sortOptions.getLimit()) {
            if (0 < sortOptions.getRangeStart()) {
                return sortOptions.getRangeStart() + sortOptions.getLimit();
            }
            return sortOptions.getLimit();
        }
        return -1;
    }

    static <T> List<T> applyRangeAndLimit(SortOptions sortOptions, List<T> results) {
        if (null == results || results.isEmpty() || null == sortOptions || SortOptions.EMPTY.equals(sortOptions) ||
            (0 >= sortOptions.getLimit() && 0 >= sortOptions.getRangeStart())) {
            return results;
        }
        int fromIndex = 0 < sortOptions.getRangeStart() ? sortOptions.getRangeStart() : 0;
        int toIndex = 0 < sortOptions.getLimit() ? fromIndex + sortOptions.getLimit() : results.size();
        return fromIndex > results.size() ? Collections.emptyList() : results.subList(fromIndex, Math.min(toIndex, results.size()));
    }

    public static SearchRequest applyPagedResultsControl(SearchRequest searchRequest, int pageSize, ASN1OctetString cookie) {
        if (0 < pageSize) {
            /*
             * add new paged results control & copy over other controls from request
             */
            List<Control> controls = new LinkedList<Control>();
            controls.add(new SimplePagedResultsControl(pageSize, cookie));
            List<Control> existingControls = searchRequest.getControlList();
            if (null != existingControls) {
                for (Control existingControl : existingControls) {
                    if (false == SimplePagedResultsControl.PAGED_RESULTS_OID.equals(existingControl.getOID())) {
                        controls.add(existingControl);
                    }
                }
            }
            searchRequest.setControls(controls);
        }
        return searchRequest;
    }

    /**
     *
     * @param value
     * @return
     */
    public static String escapeForFilter(String value) {
        // According to RFC2254 section 4 we escape the following chars so that no LDAP injection can be made:
        // Character       ASCII value
        // ---------------------------
        // *               0x2a
        // (               0x28
        // )               0x29
        // \               0x5c
        // NUL             0x00
        if (null == value) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char curChar = value.charAt(i);
            switch (curChar) {
                case '\\':
                    stringBuilder.append("\\5c");
                    break;
                //We always treat "*" as wildcard
                //               case '*':
                //                   sb.append("\\2a");
                //                   break;
                case '(':
                    stringBuilder.append("\\28");
                    break;
                case ')':
                    stringBuilder.append("\\29");
                    break;
                case '\u0000':
                    stringBuilder.append("\\00");
                    break;
                case '?':
                    stringBuilder.append('*');
                    break;
                default:
                    stringBuilder.append(curChar);
                    break;
            }
        }
        return stringBuilder.toString();
    }

}
