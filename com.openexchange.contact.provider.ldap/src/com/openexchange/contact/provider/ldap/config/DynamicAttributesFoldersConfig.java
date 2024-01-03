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

package com.openexchange.contact.provider.ldap.config;

import static com.openexchange.config.YamlUtils.get;
import static com.openexchange.config.YamlUtils.optEnum;
import static com.openexchange.config.YamlUtils.optTimeSpan;
import static com.openexchange.contact.provider.ldap.Utils.applyPagedResultsControl;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optSearchScope;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.BaseEncoding;
import com.openexchange.contact.provider.ldap.LdapContactsExceptionCodes;
import com.openexchange.contact.provider.ldap.LdapFolderFilter;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.search.Order;
import com.openexchange.java.Charsets;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.session.Session;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;

/**
 * {@link DynamicAttributesFoldersConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class DynamicAttributesFoldersConfig extends FoldersConfig {

    /**
     * Initializes a new {@link DynamicAttributesFoldersConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @param parent The generic folder config builder to take over the common settings from
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static DynamicAttributesFoldersConfig init(Map<String, Object> configEntry, FoldersConfig.Builder parent) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new DynamicAttributesFoldersConfig.Builder(parent) // @formatter:off
            .attributeName(get(configEntry, "attributeName"))
            .contactFilterTemplate(get(configEntry, "contactFilterTemplate"))
            .contactSearchScope(optSearchScope(configEntry, "contactSearchScope", SearchScope.SUB))
            .refreshInterval(optTimeSpan(configEntry, "refreshInterval", TimeUnit.HOURS.toMillis(1L)))
            .sortOrder(optEnum(configEntry, "sortOrder", Order.class, Order.NO_ORDER))
        .build(); // @formatter:on
    }

    private static class Builder extends FoldersConfig.Builder {

        String attributeName;
        String contactFilterTemplate;
        SearchScope contactSearchScope;
        long refreshInterval;
        Order sortOrder;

        Builder(FoldersConfig.Builder parent) {
            super();
            this.usedForSync = parent.usedForSync;
            this.shownInTree = parent.shownInTree;
            this.usedInPicker = parent.usedInPicker;
        }

        Builder attributeName(String value) {
            this.attributeName = value;
            return this;
        }

        Builder contactFilterTemplate(String value) {
            this.contactFilterTemplate = value;
            return this;
        }

        Builder contactSearchScope(SearchScope value) {
            this.contactSearchScope = value;
            return this;
        }

        Builder refreshInterval(long value) {
            this.refreshInterval = value;
            return this;
        }

        Builder sortOrder(Order value) {
            this.sortOrder = value;
            return this;
        }

        DynamicAttributesFoldersConfig build() throws OXException {
            return new DynamicAttributesFoldersConfig(this);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DynamicAttributesFoldersConfig.class);

    private final String attributeName;
    private final String contactFilterTemplate;
    private final SearchScope contactSearchScope;
    private final long refreshInterval;
    private final Order sortOrder;
    private final Filter commonContactFilter;
    private final Cache<String, Map<String, LdapFolderFilter>> folderFiltersCache;

    /**
     * Initializes a new {@link DynamicAttributesFoldersConfig}.
     *
     * @param builder The builder to use for initialization
     */
    DynamicAttributesFoldersConfig(DynamicAttributesFoldersConfig.Builder builder) throws OXException {
        super(builder);
        this.attributeName = builder.attributeName;
        this.contactFilterTemplate = builder.contactFilterTemplate;
        this.contactSearchScope = builder.contactSearchScope;
        this.refreshInterval = builder.refreshInterval;
        this.sortOrder = builder.sortOrder;
        this.commonContactFilter = prepareFilter(contactFilterTemplate, "[value]", "*");
        this.folderFiltersCache = CacheBuilder.newBuilder().expireAfterWrite(refreshInterval, TimeUnit.MILLISECONDS).build();
    }

    /**
     * @return the attributeName
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * @return the contactFilter
     */
    public String getContactFilterTemplate() {
        return contactFilterTemplate;
    }

    /**
     * @return the contactSearchScope
     */
    public SearchScope getContactSearchScope() {
        return contactSearchScope;
    }

    public long getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public Filter getCommonContactFilter() {
        return commonContactFilter;
    }

    @Override
    public SearchScope getCommonContactSearchScope() {
        return getContactSearchScope();
    }

    @Override
    public Map<String, LdapFolderFilter> getFolderFilters(ProviderConfig config, LDAPConnectionProvider connectionProvider, Session session) throws OXException {
        try {
            return folderFiltersCache.get(getCacheKey(connectionProvider, session), () -> discoverFolderFilters(config, connectionProvider, session));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (null != cause && (e.getCause() instanceof OXException)) {
                throw (OXException) cause;
            }
            throw ContactExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private Map<String, LdapFolderFilter> discoverFolderFilters(ProviderConfig config, LDAPConnectionProvider connectionProvider, Session session) throws OXException {
        Set<String> distinctValues = Order.ASCENDING.equals(sortOrder) ? new TreeSet<String>() :
            Order.DESCENDING.equals(sortOrder) ? new TreeSet<String>(Collections.reverseOrder()) : new LinkedHashSet<String>();
        Filter filter = prepareFilter(getContactFilterTemplate(), "[value]", "*");
        SearchRequest searchRequest = new SearchRequest(connectionProvider.getBaseDN(), getContactSearchScope(), filter, getAttributeName());
        LOG.trace("Discovering distinct values for attribute '{}' with filter {}...", getAttributeName(), filter);
        long start = System.nanoTime();
        int totalEntries = 0;
        LDAPConnection connection = connectionProvider.getConnection(session);
        try {
            ASN1OctetString resumeCookie = null;
            do {
                SearchResult searchResult = connection.search(applyPagedResultsControl(searchRequest, config.getMaxPageSize(), resumeCookie));
                distinctValues.addAll(collectAttributeValues(searchResult, getAttributeName()));
                totalEntries += searchResult.getEntryCount();
                SimplePagedResultsControl pagedResponseControl = SimplePagedResultsControl.get(searchResult);
                resumeCookie = null != pagedResponseControl && pagedResponseControl.moreResultsToReturn() ? pagedResponseControl.getCookie() : null;
            } while (null != resumeCookie);
        } catch (LDAPException e) {
            throw LdapContactsExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            connectionProvider.back(connection);
        }
        LOG.info("Successfully discovered {} distinct values for attribute '{}', based on {} total found entries ({}ms elapsed).",
            I(distinctValues.size()), getAttributeName(), I(totalEntries), L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
        Map<String, LdapFolderFilter> folderFilters = new LinkedHashMap<String, LdapFolderFilter>(distinctValues.size());
        for (String value : distinctValues) {
            String folderId = BaseEncoding.base64().omitPadding().encode(value.getBytes(Charsets.UTF_8));
            Filter contactFilter = prepareFilter(getContactFilterTemplate(), "[value]", Filter.encodeValue(value));
            folderFilters.put(folderId, new LdapFolderFilter(value, contactFilter, getContactSearchScope()));
        }
        return folderFilters;
    }

    private static String getCacheKey(LDAPConnectionProvider connectionProvider, Session session) {
        if (connectionProvider.isIndividualBind()) {
            return new StringBuilder("filters.").append(session.getContextId()).append('.').append(session.getUserId()).toString();
        }
        return "filters";
    }

    private static Set<String> collectAttributeValues(SearchResult searchResult, String attributeName) {
        Set<String> distinctValues = new HashSet<String>();
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            String[] attributeValues = entry.getAttributeValues(attributeName);
            if (null != attributeValues) {
                for (String attributeValue : attributeValues) {
                    distinctValues.add(attributeValue);
                }
            }
        }
        return distinctValues;
    }

}
