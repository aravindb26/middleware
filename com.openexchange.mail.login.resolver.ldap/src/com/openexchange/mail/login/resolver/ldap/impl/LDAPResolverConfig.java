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

import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.ENTITY_CID_SEARCH_FILTER_VALUE;
import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.ENTITY_CNAME_SEARCH_FILTER_VALUE;
import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.ENTITY_UID_SEARCH_FILTER_VALUE;
import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.ENTITY_UNAME_SEARCH_FILTER_VALUE;
import static com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties.MAIL_LOGIN_SEARCH_FILTER_VALUE;
import java.util.Locale;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.login.resolver.ldap.LDAPResolverErrorCodes;
import com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties;
import com.openexchange.session.UserAndContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link LDAPResolverConfig}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LDAPResolverConfig {

    private final LeanConfigurationService configService;

    /**
     * Initialises a new {@link LDAPResolverConfig}.
     *
     * @param configService The {@link LeanConfigurationService} reference
     */
    public LDAPResolverConfig(LeanConfigurationService configService) {
        super();
        this.configService = configService;
    }

    /**
     * Checks if the LDAP-based mail login resolver is enabled for given session.
     *
     * @param contextId The session's contextId
     * @return <code>true</code> if enabled; otherwise <code>false</code>
     */
    public boolean isEnabled(int contextId) {
        return configService.getBooleanProperty(-1, contextId, LDAPResolverProperties.ENABLED);
    }

    /**
     * Gets the LDAP client identifier.
     *
     * @param contextId The session's contextId
     * @return The LDAP client identifier
     */
    public String getClientId(int contextId) {
        return configService.getProperty(-1, contextId, LDAPResolverProperties.CLIENTID);
    }

    /**
     * Gets the LDAP search filter expression to find userId and contextId for a given mail login
     *
     * @param contextId The session's contextId
     * @param mailLogin The mail login
     * @return The LDAP search filter expression
     */
    public Filter getMailLoginSearchFilter(int contextId, String mailLogin) throws OXException {
        String mailLoginSearchFilter = configService.getProperty(-1, contextId, LDAPResolverProperties.MAIL_LOGIN_SEARCH_FILTER);
        try {
            return Filter.create(Strings.replaceSequenceWith(mailLoginSearchFilter, MAIL_LOGIN_SEARCH_FILTER_VALUE, encodeForLDAP(mailLogin)));
        } catch (LDAPException e) {
            throw LDAPResolverErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the LDAP search filter expression to find a mail login for given contextId and userId
     *
     * @param contextId The session's contextId
     * @param The entity containing userId and contextId
     * @return The LDAP search filter expression
     * @throws OXException If the filter can not be created
     */
    public Filter getEntitySearchFilter(int contextId, UserAndContext entity, String userName, String contextName) throws OXException {
        String entitySearchFilter = configService.getProperty(-1, contextId, LDAPResolverProperties.ENTITY_SEARCH_FILTER);
        //@formatter:off
        String searchFilter = Strings.replaceSequenceWith(
            Strings.replaceSequenceWith(
                Strings.replaceSequenceWith(
                    Strings.replaceSequenceWith(
                        entitySearchFilter,
                        ENTITY_CID_SEARCH_FILTER_VALUE,
                        String.valueOf(entity.getContextId())),
                    ENTITY_UID_SEARCH_FILTER_VALUE,
                    String.valueOf(entity.getUserId())),
                ENTITY_CNAME_SEARCH_FILTER_VALUE,
                encodeForLDAP(contextName)),
            ENTITY_UNAME_SEARCH_FILTER_VALUE,
            encodeForLDAP(userName)
        );
        //@formatter:on
        try {
            return Filter.create(searchFilter);
        } catch (LDAPException e) {
            throw LDAPResolverErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the LDAP search scope for a mail login search.
     *
     * @param contextId The session's contextId
     * @return The LDAP search scope
     * @throws OXException If the search scope is unknown
     */
    public SearchScope getMailLoginSearchScope(int contextId) throws OXException {
        String property = configService.getProperty(-1, contextId, LDAPResolverProperties.MAIL_LOGIN_SEARCH_SCOPE);
        switch (property.toUpperCase(Locale.US)) {
            case "SUB":
                return SearchScope.SUB;
            case "ONE":
                return SearchScope.ONE;
            default:
                throw LDAPResolverErrorCodes.UNKNOWN_SCOPE.create(property);
        }
    }

    /**
     * Gets the LDAP search scope for an entity search.
     *
     * @param contextId The session's contextId
     * @return The LDAP search scope
     * @throws OXException If the search scope is unknown
     */
    public SearchScope getEntitySearchScope(int contextId) throws OXException {
        String property = configService.getProperty(-1, contextId, LDAPResolverProperties.MAIL_LOGIN_SEARCH_SCOPE);
        switch (property.toUpperCase(Locale.US)) {
            case "SUB":
                return SearchScope.SUB;
            case "ONE":
                return SearchScope.ONE;
            default:
                throw LDAPResolverErrorCodes.UNKNOWN_SCOPE.create(property);
        }
    }

    /**
     * Gets the LDAP attribute to fetch the userId from
     *
     * @param contextId The session's contextId
     * @return The LDAP attribute to fetch the userId from
     */
    public String getUserIdAttribute(int contextId) {
        return configService.getProperty(-1, contextId, LDAPResolverProperties.ENTITY_USERID_ATTRIBUTE);
    }

    /**
     * Gets the LDAP attribute to fetch the user name from
     *
     * @param contextId The session's contextId
     * @return The LDAP attribute to fetch the user name from
     */
    public String getUserNameAttribute(int contextId) {
        return configService.getProperty(-1, contextId, LDAPResolverProperties.ENTITY_USERNAME_ATTRIBUTE);
    }

    /**
     * Gets the LDAP attribute to fetch the contextId from
     *
     * @param contextId The session's contextId
     * @return The LDAP attribute to fetch the contextId from
     */
    public String getContextIdAttribute(int contextId) {
        return configService.getProperty(-1, contextId, LDAPResolverProperties.ENTITY_CONTEXTID_ATTRIBUTE);
    }

    /**
     * Gets the LDAP attribute to fetch the context name from
     *
     * @param contextId The session's contextId
     * @return The LDAP attribute to fetch the contextId from
     */
    public String getContextNameAttribute(int contextId) {
        return configService.getProperty(-1, contextId, LDAPResolverProperties.ENTITY_CONTEXTNAME_ATTRIBUTE);
    }

    /**
     * Gets the LDAP attribute to fetch the mail login from
     *
     * @param contextId The session's contextId
     * @return The LDAP attribute to fetch the mail login from
     */
    public String getMailLoginAttribute(int contextId) {
        return configService.getProperty(-1, contextId, LDAPResolverProperties.MAIL_LOGIN_ATTRIBUTE);
    }

    /**
     * Gets the cache expire time in seconds
     *
     * @return The cache expire time in seconds
     */
    public int getCacheExpire() {
        return configService.getIntProperty(LDAPResolverProperties.CACHE_EXPIRE);
    }

    /**
     * Encode data for use in LDAP queries.
     *
     * @param input The text to encode for LDAP
     * @return The encoded text for use in LDAP
     */
    private static String encodeForLDAP(String input) {
        if( input == null ) {
            return null;
        }
        StringBuilder sb = null;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
            case '\\':
                sb = initIfNecessary(sb, input, i);
                sb.append("\\5c");
                break;
            case '*':
                sb = initIfNecessary(sb, input, i);
                sb.append("\\2a");
                break;
            case '(':
                sb = initIfNecessary(sb, input, i);
                sb.append("\\28");
                break;
            case ')':
                sb = initIfNecessary(sb, input, i);
                sb.append("\\29");
                break;
            case '\0':
                sb = initIfNecessary(sb, input, i);
                sb.append("\\00");
                break;
            default:
                if (sb != null) {
                    sb.append(c);
                }
            }
        }
        return sb == null ? input : sb.toString();
    }

    private static StringBuilder initIfNecessary(StringBuilder builder, String input, int i) {
        if (builder != null) {
            return builder;
        }

        StringBuilder sb = new StringBuilder();
        if (i > 0) {
            sb.append(input, 0, i);
        }
        return sb;
    }

}