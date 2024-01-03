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
package com.openexchange.mail.login.resolver.ldap.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.ENTITY_CONTEXTID_ATTRIBUTE;
import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.ENTITY_USERID_ATTRIBUTE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Strings;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.mail.login.resolver.MailLoginResolver;
import com.openexchange.mail.login.resolver.ResolverResult;
import com.openexchange.mail.login.resolver.ldap.LDAPResolverErrorCodes;
import com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties;
import com.openexchange.session.UserAndContext;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link LDAPResolver}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LDAPResolver implements MailLoginResolver {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LDAPResolver.class);

    private final UserService userService;
    private final ContextService contextService;
    private final LDAPService ldapService;
    private final LDAPResolverConfig resolverConfig;
    private final Cache<MailLoginCacheKey, ResolverResult> mailLoginCache;
    private final Cache<EntityCacheKey, ResolverResult> entityCache;

    /**
     * Initialises a new {@link LDAPResolver}.
     *
     * @param userService The {@link UserService} reference
     * @param contextService The {@link ContextService} reference
     * @param ldapService The {@link LDAPService} reference
     * @param resolverConfig The LDAP resolver configuration
     */
    public LDAPResolver(UserService userService, ContextService contextService, LDAPService ldapService, LDAPResolverConfig resolverConfig) {
        this.userService = userService;
        this.contextService = contextService;
        this.ldapService = ldapService;
        this.resolverConfig = resolverConfig;
        this.mailLoginCache = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(resolverConfig.getCacheExpire(), TimeUnit.SECONDS)
            .build();
        this.entityCache = CacheBuilder.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(resolverConfig.getCacheExpire(), TimeUnit.SECONDS)
            .build();
    }

    @Override
    public ResolverResult resolveMailLogin(int contextId, String mailLogin) throws OXException {
        if (false == resolverConfig.isEnabled(contextId)) {
            return ResolverResult.FAILURE();
        }
        try {
            return mailLoginCache.get(new MailLoginCacheKey(mailLogin, resolverConfig.getClientId(contextId)), new Callable<ResolverResult>() {

                @Override
                public ResolverResult call() throws Exception {
                    return resolveMailLoginInternal(contextId, mailLogin);
                }
            });
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (null != cause && cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public ResolverResult resolveEntity(int contextId, UserAndContext entity) throws OXException {
        if (false == resolverConfig.isEnabled(contextId)) {
            return ResolverResult.FAILURE();
        }
        try {
            return entityCache.get(new EntityCacheKey(entity, resolverConfig.getClientId(contextId)), new Callable<ResolverResult>() {

                @Override
                public ResolverResult call() throws Exception {
                    return resolveEntityInternal(contextId, entity);
                }
            });
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (null != cause && cause instanceof OXException) {
                throw (OXException) cause;
            }
            throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public List<ResolverResult> resolveMultipleMailLogins(int contextId, List<String> mailLogins) throws OXException {
        if (null == mailLogins || mailLogins.isEmpty()) {
            return Collections.emptyList();
        }
        if (false == resolverConfig.isEnabled(contextId)) {
            List<ResolverResult> results = new ArrayList<ResolverResult>(mailLogins.size());
            for (int i = 0; i < mailLogins.size(); i++) {
                results.add(ResolverResult.FAILURE());
            }
            return results;
        }
        ResolverResult[] results = new ResolverResult[mailLogins.size()];
        String clientId = resolverConfig.getClientId(contextId);
        List<MailLoginCacheKey> cacheKeys = new ArrayList<MailLoginCacheKey>(mailLogins.size());
        for (String mailLogin : mailLogins) {
            cacheKeys.add(new MailLoginCacheKey(mailLogin, clientId));
        }
        Map<MailLoginCacheKey, ResolverResult> cachedResults = mailLoginCache.getAllPresent(cacheKeys);
        for (int i = 0; i < mailLogins.size(); i++) {
            ResolverResult result = cachedResults.get(new MailLoginCacheKey(mailLogins.get(i), clientId));
            results[i] = null != result ? result : resolveMailLogin(contextId, mailLogins.get(i));
        }
        return Arrays.asList(results);
    }

    @Override
    public List<ResolverResult> resolveMultipleEntities(int contextId, List<UserAndContext> entities) throws OXException {
        if (null == entities || entities.isEmpty()) {
            return Collections.emptyList();
        }
        if (false == resolverConfig.isEnabled(contextId)) {
            List<ResolverResult> results = new ArrayList<ResolverResult>(entities.size());
            for (int i = 0; i < entities.size(); i++) {
                results.add(ResolverResult.FAILURE());
            }
            return results;
        }
        ResolverResult[] results = new ResolverResult[entities.size()];
        String clientId = resolverConfig.getClientId(contextId);
        List<EntityCacheKey> cacheKeys = new ArrayList<EntityCacheKey>(entities.size());
        for (UserAndContext entity : entities) {
            cacheKeys.add(new EntityCacheKey(entity, clientId));
        }
        Map<EntityCacheKey, ResolverResult> cachedResults = entityCache.getAllPresent(cacheKeys);
        for (int i = 0; i < entities.size(); i++) {
            ResolverResult result = cachedResults.get(new EntityCacheKey(entities.get(i), clientId));
            results[i] = null != result ? result : resolveEntity(contextId, entities.get(i));
        }
        return Arrays.asList(results);
    }

    /**
     * Parses the given attribute from a {@link SearchResultEntry}
     *
     * @param entry {@link SearchResultEntry}
     * @param attribute The attribute to get the value for
     * @return attributeValue
     */
    private static String getAttributeValue(SearchResultEntry entry, String attribute) {
        return entry.getAttributeValue(attribute);
    }

    /**
     * Gets a {@link LDAPConnectionProvider}
     *
     * @param contextId The session's contextId
     * @return LDAP Connection Provider
     * @throws OXException If the client id is missing or the client has an inappropriate auth configuration
     */
    private LDAPConnectionProvider getConnectionProvider(int contextId) throws OXException {
        String clientId = resolverConfig.getClientId(contextId);
        if (Strings.isEmpty(clientId)) {
            throw LDAPResolverErrorCodes.PROPERTY_MISSING.create(LDAPResolverProperties.CLIENTID.getFQPropertyName());
        }
        LDAPConnectionProvider connectionProvider = ldapService.getConnection(clientId);
        if (connectionProvider.isIndividualBind()) {
            throw LDAPResolverErrorCodes.CLIENT_AUTH_ERROR.create(clientId);
        }
        return connectionProvider;
    }

    /**
     * Resolves given mail login.
     *
     * @param contextId The session's contextId
     * @param mailLogin The mail login to resolve
     * @return The resolved mail login
     * @throws OXException If there was an error
     */
    protected ResolverResult resolveMailLoginInternal(int contextId, String mailLogin) throws OXException, LDAPSearchException {
        LDAPConnectionProvider connectionProvider = getConnectionProvider(contextId);
        LDAPConnection connection = connectionProvider.getConnection(null);
        try {
            return resolveMailLoginInternal(contextId, connection, connectionProvider.getBaseDN(), mailLogin);
        } finally {
            connectionProvider.back(connection);
        }
    }

    /**
     * Resolves given mail login.
     *
     * @param contextId The session's contextId
     * @param connection The LDAP connection
     * @param baseDN The LDAP baseDN
     * @param mailLogin The mail login to resolve
     * @return The resolved mail login
     * @throws OXException If there was an error
     */
    protected ResolverResult resolveMailLoginInternal(int ctxId, LDAPConnection connection, String baseDN, String mailLogin) throws OXException, LDAPSearchException {
        LOG.debug("Start resolving mail login {}.", mailLogin);
        Filter searchFilter = resolverConfig.getMailLoginSearchFilter(ctxId, mailLogin);
        SearchScope searchScope = resolverConfig.getMailLoginSearchScope(ctxId);
        String userIdAttribute = resolverConfig.getUserIdAttribute(ctxId);
        String userNameAttribute = resolverConfig.getUserNameAttribute(ctxId);
        String contextIdAttribute = resolverConfig.getContextIdAttribute(ctxId);
        String contextNameAttribute = resolverConfig.getContextNameAttribute(ctxId);
        if (userIdAttribute == null && userNameAttribute == null) {
            throw LDAPResolverErrorCodes.PROPERTY_MISSING.create(ENTITY_USERID_ATTRIBUTE.getFQPropertyName());
        }
        if (contextIdAttribute == null && contextNameAttribute == null) {
            throw LDAPResolverErrorCodes.PROPERTY_MISSING.create(ENTITY_CONTEXTID_ATTRIBUTE.getFQPropertyName());
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
            LOG.debug("Unable to resolve mail login {}. No entries found.", mailLogin, Long.valueOf(System.currentTimeMillis() - start));
            return ResolverResult.FAILURE();
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
                LOG.debug("Successfully resolved mail login {} to userId {} and contextId {}.", mailLogin, I(userId), I(contextId));
                return ResolverResult.SUCCESS(userId, contextId, mailLogin);
            }
        }
        LOG.debug("Unable to resolve mail login {}. No LDAP entries with valid user or context attributes {} found.", mailLogin, searchAttributes);
        return ResolverResult.FAILURE();
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
        if (contextIdAttributeValue == null) {
            contextId = contextService.getContextId(contextNameAttributeValue);
            if (contextId == -1) {
                throw LDAPResolverErrorCodes.NO_MAPPING.create(contextNameAttributeValue);
            }
        } else {
            contextId = Integer.parseInt(contextIdAttributeValue);
        }
        if (userIdAttributeValue == null) {
            Context context = contextService.getContext(contextId);
            userId = userService.getUserId(userNameAttributeValue, context);
        } else {
            userId = Integer.parseInt(userIdAttributeValue);
        }
        return UserAndContext.newInstance(userId, contextId);
    }

    /**
     * Resolves given entity (userId/contextId).
     *
     * @param contextId The session's contextId
     * @param entity The userId and contextId to resolve
     * @return The resolved entity
     * @throws OXException If there was an error
     */
    protected ResolverResult resolveEntityInternal(int contextId, UserAndContext entity) throws OXException, LDAPSearchException, NumberFormatException {
        LDAPConnectionProvider connectionProvider = getConnectionProvider(contextId);
        LDAPConnection connection = connectionProvider.getConnection(null);
        try {
            return resolveEntityInternal(contextId, connection, connectionProvider.getBaseDN(), entity);
        } finally {
            connectionProvider.back(connection);
        }
    }

    /**
     * Resolves given entity (userId/contextId).
     *
     * @param contextId The session's contextId
     * @param connection The LDAP connection
     * @param baseDN The LDAP baseDN
     * @param entity The userId and contextId to resolve
     * @return The resolved entity
     * @throws OXException If there was an error
     */
    protected ResolverResult resolveEntityInternal(int contextId, LDAPConnection connection, String baseDN, UserAndContext entity) throws OXException, LDAPSearchException, NumberFormatException {
        LOG.debug("Start resolving entity {}.", entity);
        User user = userService.getUser(entity.getUserId(), entity.getContextId());
        Context context = contextService.getContext(entity.getContextId());
        Filter searchFilter = resolverConfig.getEntitySearchFilter(contextId, entity, user.getLoginInfo(), context.getName());
        SearchScope searchScope = resolverConfig.getEntitySearchScope(contextId);
        String mailLoginAttribute = resolverConfig.getMailLoginAttribute(contextId);
        long start = System.currentTimeMillis();
        LOG.debug("Searching LDAP: [baseDN: {}; searchScope: {}; searchFilter: {}; attributes: [{}]", baseDN, searchScope, searchFilter.toString(), mailLoginAttribute);
        SearchResult searchResult = connection.search(baseDN, searchScope, searchFilter);
        int entryCount = searchResult.getEntryCount();
        LOG.debug("LDAP search took {}ms. Found {} entries.", Long.valueOf(System.currentTimeMillis() - start), I(entryCount));
        if (entryCount <= 0) {
            LOG.debug("Unable to resolve entity {}. No entries found.", entity, Long.valueOf(System.currentTimeMillis() - start));
            return ResolverResult.FAILURE();
        }
        String mailLoginAttributeValue = null;
        for (SearchResultEntry entry: searchResult.getSearchEntries()) {
            mailLoginAttributeValue = getAttributeValue(entry, mailLoginAttribute);
            if (mailLoginAttributeValue != null) {
                LOG.debug("Successfully resolved entity {} to mail login {}.", entity, mailLoginAttributeValue);
                return ResolverResult.SUCCESS(entity.getUserId(), entity.getContextId(), mailLoginAttributeValue);
            }
        }
        LOG.debug("Unable to resolve entity {}. LDAP entries is missing mail login attribute {}.", entity, mailLoginAttribute);
        return ResolverResult.FAILURE();
    }

}