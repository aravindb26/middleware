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
package com.openexchange.mailmapping.ldap;

import java.util.Locale;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mailmapping.ldap.impl.LDAPMailMappingErrorCodes;
import com.openexchange.mailmapping.ldap.impl.LDAPMailMappingService;
import com.openexchange.server.ServiceLookup;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link LDAPMailMappingConfig}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LDAPMailMappingConfig {

    /**
     * Enables the LDAP bases mail mapping service {@link LDAPMailMappingService}
     */
    public static final String PROPERTY_ENABLED = "com.openexchange.mailmapping.ldap.enabled";
    /**
     * LDAP client config identifier
     */
    public static final String PROPERTY_CLIENTID = "com.openexchange.mailmapping.ldap.clientId";
    /**
     * LDAP search filter expression
     */
    public static final String PROPERTY_SEARCH_FILTER = "com.openexchange.mailmapping.ldap.searchFilter";
    /**
     * Placeholder which is replaced by the mail address
     */
    public static final String PROPERTY_SEARCH_FILTER_VALUE = "[value]";
    /**
     * LDAP search scope {@link SearchScope}
     */
    public static final String PROPERTY_SEARCH_SCOPE= "com.openexchange.mailmapping.ldap.searchScope";
    /**
     * LDAP attribute to fetch the userId from
     */
    public static final String PROPERTY_USERID_ATTRIBUTE = "com.openexchange.mailmapping.ldap.userIdAttribute";
    /**
     * LDAP attribute to fetch the user name from
     */
    public static final String PROPERTY_USERNAME_ATTRIBUTE = "com.openexchange.mailmapping.ldap.userNameAttribute";
    /**
     * LDAP attribute to fetch the contextId from
     */
    public static final String PROPERTY_CONTEXTID_ATTRIBUTE = "com.openexchange.mailmapping.ldap.contextIdAttribute";
    /**
     * LDAP attribute to fetch the context name from
     */
    public static final String PROPERTY_CONTEXTNAME_ATTRIBUTE = "com.openexchange.mailmapping.ldap.contextNameAttribute";
    /**
     * Cache expire time in seconds
     */
    public static final String PROPERTY_CACHE_EXPIRE = "com.openexchange.mailmapping.ldap.cacheExpire";

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Default value for "com.openexchange.mailmapping.ldap.enabled"
     */
    private static final boolean DEFAULT_ENABLED = false;
    /**
     * Default value for "com.openexchange.mailmapping.ldap.clientId"
     */
    private static final String DEFAULT_CLIENTID = null;
    /**
     * Default value for "com.openexchange.mailmapping.ldap.searchFilter"
     */
    private static final String DEFAULT_SEARCH_FILTER = "(mail=[value])";
    /**
     * Default value for "com.openexchange.mailmapping.ldap.searchScope"
     */
    private static final String DEFAULT_SEARCH_SCOPE = SearchScope.SUB.getName();
    /**
     * Default value for "com.openexchange.mailmapping.ldap.userIdAttribute"
     */
    private static final String DEFAULT_USERID_ATTRIBUTE = null;
    /**
     * Default value for "com.openexchange.mailmapping.ldap.userNameAttribute"
     */
    private static final String DEFAULT_USERNAME_ATTRIBUTE = null;
    /**
     * Default value for "com.openexchange.mailmapping.ldap.contextIdAttribute"
     */
    private static final String DEFAULT_CONTEXTID_ATTRIBUTE = null;
    /**
     * Default value for "com.openexchange.mailmapping.ldap.contextNameAttribute"
     */
    private static final String DEFAULT_CONTEXTNAME_ATTRIBUTE = null;
    /**
     * Default value for "com.openexchange.mailmapping.ldap.cacheExpire"
     */
    private static final int DEFAULT_CACHE_EXPIRE = 600;

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final boolean enabled;
    private final String clientId;
    private final String searchFilter;
    private final String searchScope;
    private final String userIdAttribute;
    private final String userNameAttribute;
    private final String contextIdAttribute;
    private final String contextNameAttribute;
    private final int cacheExpire;

    /**
     * Initializes a new {@link LDAPMailMappingConfig}.
     *
     * @param services The service look-up
     */
    public LDAPMailMappingConfig(ServiceLookup services) {
        ConfigurationService config = services.getOptionalService(ConfigurationService.class);
        if (config == null) {
            enabled = DEFAULT_ENABLED;
            clientId = DEFAULT_CLIENTID;
            searchFilter = DEFAULT_SEARCH_FILTER;
            searchScope = DEFAULT_SEARCH_SCOPE;
            userIdAttribute = DEFAULT_USERID_ATTRIBUTE;
            userNameAttribute = DEFAULT_USERNAME_ATTRIBUTE;
            contextIdAttribute = DEFAULT_CONTEXTID_ATTRIBUTE;
            contextNameAttribute = DEFAULT_CONTEXTNAME_ATTRIBUTE;
            cacheExpire = DEFAULT_CACHE_EXPIRE;
        } else {
            enabled = config.getBoolProperty(PROPERTY_ENABLED, DEFAULT_ENABLED);
            clientId = config.getProperty(PROPERTY_CLIENTID, DEFAULT_CLIENTID);
            searchFilter = config.getProperty(PROPERTY_SEARCH_FILTER, DEFAULT_SEARCH_FILTER);
            searchScope = config.getProperty(PROPERTY_SEARCH_SCOPE, DEFAULT_SEARCH_SCOPE);
            userIdAttribute = config.getProperty(PROPERTY_USERID_ATTRIBUTE, DEFAULT_USERID_ATTRIBUTE);
            userNameAttribute = config.getProperty(PROPERTY_USERNAME_ATTRIBUTE, DEFAULT_USERNAME_ATTRIBUTE);
            contextIdAttribute = config.getProperty(PROPERTY_CONTEXTID_ATTRIBUTE, DEFAULT_CONTEXTID_ATTRIBUTE);
            contextNameAttribute = config.getProperty(PROPERTY_CONTEXTNAME_ATTRIBUTE, DEFAULT_CONTEXTNAME_ATTRIBUTE);
            cacheExpire = config.getIntProperty(PROPERTY_CACHE_EXPIRE, DEFAULT_CACHE_EXPIRE);
        }
    }

    /**
     * Checks if the LDAP-based mail mapping service is enabled.
     *
     * @return <code>true</code> if enabled; otherwise <code>false</code>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the LDAP client identifier.
     *
     * @return The LDAP client identifier
     */
    public String getClientId(){
        return clientId;
    }

    /**
     * Gets the LDAP search filter expression, which already contains the mail address.
     *
     * @param The mail address
     * @return The LDAP search filter expression
     * @throws OXException If the filter can not be created
     */
    public Filter getSearchFilter(String mail) throws OXException {
        String validMail;
        try {
            validMail = new InternetAddress(mail).getAddress();
        } catch (AddressException e) {
            throw LDAPMailMappingErrorCodes.INVALID_MAIL_ADDRESS.create(e, mail);
        }
        try {
            return Filter.create(searchFilter.replace(PROPERTY_SEARCH_FILTER_VALUE, encodeForLDAP(validMail)));
        } catch (LDAPException e) {
            throw LDAPMailMappingErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the LDAP search scope.
     *
     * @param The mail address
     * @return The LDAP search scope
     * @throws OXException If the search scope is unknown
     */
    public SearchScope getSearchScope() throws OXException {
        switch (searchScope.toUpperCase(Locale.US)) {
            case "SUB":
                return SearchScope.SUB;
            case "ONE":
                return SearchScope.ONE;
            default:
                throw LDAPMailMappingErrorCodes.UNKNOWN_SCOPE.create(searchScope);
        }
    }

    /**
     * Gets the LDAP attribute to fetch the userId from
     *
     * @return The LDAP attribute to fetch the userId from
     */
    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    /**
     * Gets the LDAP attribute to fetch the user name from
     *
     * @return The LDAP attribute to fetch the user name from
     */
    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    /**
     * Gets the LDAP attribute to fetch the contextId from
     *
     * @return The LDAP attribute to fetch the contextId from
     */
    public String getContextIdAttribute(){
        return contextIdAttribute;
    }

    /**
     * Gets the LDAP attribute to fetch the context name from
     *
     * @return The LDAP attribute to fetch the context name from
     */
    public String getContextNameAttribute(){
        return contextNameAttribute;
    }

    /**
     * Gets the cache expire time in seconds
     *
     * @return The cache expire time in seconds
     */
    public int getCacheExpire() {
        return cacheExpire;
    }

    /**
     * Encode data for use in LDAP queries.
     *
     * @param input the text to encode for LDAP
     *
     * @return input encoded for use in LDAP
     */
    private static String encodeForLDAP(String input) {
        if( input == null ) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
            case '\\':
                sb.append("\\5c");
                break;
            case '*':
                sb.append("\\2a");
                break;
            case '(':
                sb.append("\\28");
                break;
            case ')':
                sb.append("\\29");
                break;
            case '\0':
                sb.append("\\00");
                break;
            default:
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
