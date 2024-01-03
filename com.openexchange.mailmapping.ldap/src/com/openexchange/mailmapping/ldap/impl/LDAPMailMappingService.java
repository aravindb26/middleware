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
package com.openexchange.mailmapping.ldap.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mailmapping.ldap.LDAPMailMappingConfig.PROPERTY_CONTEXTID_ATTRIBUTE;
import static com.openexchange.mailmapping.ldap.LDAPMailMappingConfig.PROPERTY_USERID_ATTRIBUTE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Strings;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.mailmapping.MultipleMailResolver;
import com.openexchange.mailmapping.ResolvedMail;
import com.openexchange.mailmapping.ldap.LDAPMailMappingConfig;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.UserAndContext;
import com.openexchange.user.UserService;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link LDAPMailMappingService}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LDAPMailMappingService implements MultipleMailResolver {

    private final LoadingCache<String, ResolvedMail> resolvedMailCache;

    /**
     * Initializes a new {@link LDAPMailMappingService}.
     *
     * @param services The service look-up
     * @param config The configuration to use
     */
    public LDAPMailMappingService(ServiceLookup services, LDAPMailMappingConfig config) {
        super();
        this.resolvedMailCache = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(config.getCacheExpire(), TimeUnit.SECONDS)
            .build(new LDAPMailMappingCacheLoader(services, config));
    }

    @Override
    public ResolvedMail resolve(String mail) throws OXException {
        try {
            return resolvedMailCache.get(mail);
        } catch (ExecutionException | UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (null != cause && cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public ResolvedMail[] resolveMultiple(String... mails) throws OXException {
        try {
            Map<String, ResolvedMail> result = resolvedMailCache.getAll(Arrays.asList(mails));
            return result.values().toArray(new ResolvedMail[0]);
        } catch (ExecutionException | UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (null != cause && cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class LDAPMailMappingCacheLoader extends CacheLoader<String, ResolvedMail> {

        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LDAPMailMappingService.class);

        private final ServiceLookup services;
        private final LDAPMailMappingConfig config;

        /**
         * Initializes a new {@link LDAPMailMappingCacheLoader}.
         *
         * @param services The service look-up
         * @param config The configuration to use
         */
        LDAPMailMappingCacheLoader(ServiceLookup services, LDAPMailMappingConfig config) {
            this.services = services;
            this.config = config;
        }

        @Override
        public ResolvedMail load(String mail) throws OXException {
            LDAPConnectionProvider connectionProvider = getConnectionProvider(config.getClientId());
            LDAPConnection connection = connectionProvider.getConnection(null);
            try {
                return resolve(connection, connectionProvider.getBaseDN(), mail);
            } catch (LDAPSearchException e) {
                throw LDAPCommonErrorCodes.CONNECTION_ERROR.create(e, e.getMessage());
            } catch (RuntimeException e) {
                throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                connectionProvider.back(connection);
            }
        }

        @Override
        public Map<String, ResolvedMail> loadAll(Iterable<? extends String> mails) throws OXException {
            LDAPConnectionProvider connectionProvider = getConnectionProvider(config.getClientId());
            LDAPConnection connection = connectionProvider.getConnection(null);
            Map<String, ResolvedMail> results = new HashMap<>();
            try {
                for (String mail : mails) {
                    results.put(mail, resolve(connection, connectionProvider.getBaseDN(), mail));
                }
            } catch (LDAPSearchException e) {
                throw LDAPCommonErrorCodes.CONNECTION_ERROR.create(e, e.getMessage());
            } catch (RuntimeException e) {
                throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                connectionProvider.back(connection);
            }
            return results;
        }

        /**
         * Resolves given mail address.
         *
         * @param connection The LDAP connection
         * @param baseDN The LDAP baseDN
         * @param mail The mail address to resolve
         * @return The result
         * @throws OXException If there was an error
         */
        private ResolvedMail resolve(LDAPConnection connection, String baseDN, String mail) throws OXException, LDAPSearchException {
            LOG.debug("Start resolving mail address {}.", mail);
            Filter searchFilter = config.getSearchFilter(mail);
            SearchScope searchScope = config.getSearchScope();
            String userIdAttribute = config.getUserIdAttribute();
            String userNameAttribute = config.getUserNameAttribute();
            String contextIdAttribute = config.getContextIdAttribute();
            String contextNameAttribute = config.getContextNameAttribute();
            if (userIdAttribute == null && userNameAttribute == null) {
                throw LDAPMailMappingErrorCodes.PROPERTY_MISSING.create(PROPERTY_USERID_ATTRIBUTE);
            }
            if (contextIdAttribute == null && contextNameAttribute == null) {
                throw LDAPMailMappingErrorCodes.PROPERTY_MISSING.create(PROPERTY_CONTEXTID_ATTRIBUTE);
            }
            List<String> searchAttributes = new ArrayList<>();
            if (userIdAttribute != null) {
                searchAttributes.add(contextIdAttribute);
            }
            if (userNameAttribute != null) {
                searchAttributes.add(userNameAttribute);
            }
            if (contextIdAttribute != null) {
                searchAttributes.add(contextIdAttribute);
            }
            if (contextNameAttribute != null) {
                searchAttributes.add(contextNameAttribute);
            }
            long start = System.currentTimeMillis();
            LOG.debug("Searching LDAP: [baseDN: {}; searchScope: {}; searchFilter: {}; attributes: {}]", baseDN, searchScope, searchFilter.toString(), searchAttributes);
            SearchResult searchResult = connection.search(baseDN, searchScope, searchFilter, searchAttributes.toArray(new String[searchAttributes.size()]));
            int entryCount = searchResult.getEntryCount();
            LOG.debug("LDAP search took {}ms. Found {} entries.", Long.valueOf(System.currentTimeMillis() - start), I(entryCount));
            if (entryCount <= 0) {
                LOG.debug("Unable to resolve mail address {}. No entries found.", mail, Long.valueOf(System.currentTimeMillis() - start));
                return ResolvedMail.NEUTRAL();
            }
            for (SearchResultEntry entry: searchResult.getSearchEntries()) {
                UserAndContext user = null;
                try {
                    user = parseResultEntry(entry, contextIdAttribute, contextNameAttribute, userIdAttribute, userNameAttribute);
                } catch (OXException | NumberFormatException e) {
                    LOG.debug("Unable to parse search result entry {}", entry, e);
                }
                if (user != null) {
                    int userId = user.getUserId();
                    int contextId = user.getContextId();
                    LOG.debug("Successfully resolved mail address {} to userId {} and contextId {}.", mail, I(userId), I(contextId));
                    return ResolvedMail.ACCEPT(userId, contextId);
                }
            }
            LOG.debug("Unable to resolve mail address {}. No LDAP entries with valid user or context attributes {} found.", mail, searchAttributes);
            return ResolvedMail.NEUTRAL();
        }

        /**
         * Parses a single LDAP search result entry.
         *
         * @param entry The LDAP search result entry to parse
         * @param contextIdAttribute The LDAP baseDN
         * @param contextNameAttribute The mail address to resolve
         * @param contextIdAttribute The LDAP baseDN
         * @param contextNameAttribute The mail address to resolve
         * @return {@link UserAndContext} or <code>null</code> if the result entry does not contain a user or context attribute value
         * @throws OXException If user or context from search result entry does not exist
         * @throws NumberFormatException If user or context id attribute value is not a valid integer
         */
        private UserAndContext parseResultEntry(SearchResultEntry entry, String contextIdAttribute, String contextNameAttribute, String userIdAttribute, String userNameAttribute) throws OXException, NumberFormatException {
            String userIdAttributeValue = null;
            String userNameAttributeValue = null;
            String contextIdAttributeValue = null;
            String contextNameAttributeValue = null;
            if (userIdAttribute != null) {
                userIdAttributeValue = getAttributeValue(entry, userIdAttribute);
            }
            if (userNameAttribute != null) {
                userNameAttributeValue = getAttributeValue(entry, userNameAttribute);
            }
            if (contextIdAttribute != null) {
                contextIdAttributeValue = getAttributeValue(entry, contextIdAttribute);
            }
            if (contextNameAttribute != null) {
                contextNameAttributeValue = getAttributeValue(entry, contextNameAttribute);
            }
            if ((userIdAttributeValue == null && userNameAttributeValue == null) || (contextIdAttributeValue == null && contextNameAttributeValue == null)) {
                return null;
            }
            int contextId;
            int userId;
            ContextService contextService = services.getServiceSafe(ContextService.class);
            if (contextIdAttributeValue == null) {
                contextId = contextService.getContextId(contextNameAttributeValue);
                if (contextId == -1) {
                    throw LDAPMailMappingErrorCodes.NO_MAPPING.create(contextNameAttributeValue);
                }
            } else {
                contextId = Integer.parseInt(contextIdAttributeValue);
            }
            if (userIdAttributeValue == null) {
                UserService userService = services.getServiceSafe(UserService.class);
                Context context = contextService.getContext(contextId);
                userId = userService.getUserId(userNameAttributeValue, context);
            } else {
                userId = Integer.parseInt(userIdAttributeValue);
            }
            return UserAndContext.newInstance(userId, contextId);
        }

        /**
         * Gets a {@link LDAPConnectionProvider}
         *
         * @param clientId The LDAP client identifier
         * @return LDAP Connection Provider
         * @throws OXException If the client id is missing or the client has an inappropriate auth configuration
         */
        private LDAPConnectionProvider getConnectionProvider(String clientId) throws OXException {
            if (Strings.isEmpty(clientId)) {
                throw LDAPMailMappingErrorCodes.PROPERTY_MISSING.create(LDAPMailMappingConfig.PROPERTY_CLIENTID);
            }
            LDAPConnectionProvider connectionProvider = services.getServiceSafe(LDAPService.class).getConnection(clientId);
            if (connectionProvider.isIndividualBind()) {
                throw LDAPMailMappingErrorCodes.CLIENT_AUTH_ERROR.create(clientId);
            }
            return connectionProvider;
        }

        /**
         * Parses the given attribute from a {@link SearchResultEntry}
         *
         * @param entry {@link SearchResultEntry}
         * @param attribute The attribute to get the value for
         * @return attributeValue
         */
        private String getAttributeValue(SearchResultEntry entry, String attribute) {
            return entry.getAttributeValue(attribute);
        }
    }

}
