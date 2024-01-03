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
package com.openexchange.mail.login.resolver.ldap;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.Property;
import com.openexchange.mail.login.resolver.ldap.impl.LDAPResolver;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link LDAPResolverProperties}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public enum LDAPResolverProperties implements Property {

    /**
     * Enables the {@link LDAPResolver} to resolve mail logins and ACL names
     */
    ENABLED("enabled", B(false)),
    /**
     * LDAP client identifier
     */
    CLIENTID("clientId", null),
    /**
     * LDAP search filter expression to find userId, contextId and displayName for a given mail login
     */
    MAIL_LOGIN_SEARCH_FILTER("mailLoginSearchFilter", "(oxLocalMailRecipient=[mailLogin])"),
    /**
     * LDAP search scope {@link SearchScope} for mail login search
     */
    MAIL_LOGIN_SEARCH_SCOPE("mailLoginSearchScope", SearchScope.SUB.getName()),
    /**
     * LDAP attribute to fetch the userId from
     */
    ENTITY_USERID_ATTRIBUTE("userIdAttribute", null),
    /**
     * LDAP attribute to fetch the user name from
     */
    ENTITY_USERNAME_ATTRIBUTE("userNameAttribute", null),
    /**
     * LDAP attribute to fetch the contextId from
     */
    ENTITY_CONTEXTID_ATTRIBUTE("contextIdAttribute", null),
    /**
     * LDAP attribute to fetch the context name from
     */
    ENTITY_CONTEXTNAME_ATTRIBUTE("contextNameAttribute", null),
    /**
     * LDAP search filter expression to find a mail login for given contextId and userId
     */
    ENTITY_SEARCH_FILTER("entitySearchFilter", "(&(oxContextId=[cid])(oxUserId=[uid]))"),
    /**
     * LDAP search scope {@link SearchScope} for entities
     */
    ENTITY_SEARCH_SCOPE("entitySearchScope", SearchScope.SUB.getName()),
    /**
     * LDAP attribute to fetch the mail login from
     */
    MAIL_LOGIN_ATTRIBUTE("mailLoginAttribute", "oxLocalMailRecipient"),
    /**
     * Cache expire time in seconds
     */
    CACHE_EXPIRE("cacheExpire", I(600)),
    ;

    private final String fqn;
    private final Object defaultValue;

    private static final String PREFIX = "com.openexchange.mail.login.resolver.ldap.";

    /**
     * Placeholder which is replaced by the mail login
     */
    public static final String MAIL_LOGIN_SEARCH_FILTER_VALUE = "[mailLogin]";
    /**
     * Placeholder which is replaced by the userId
     */
    public static final String ENTITY_UID_SEARCH_FILTER_VALUE = "[uid]";
    /**
     * Placeholder which is replaced by the user name
     */
    public static final String ENTITY_UNAME_SEARCH_FILTER_VALUE = "[uname]";
    /**
     * Placeholder which is replaced by the contextId
     */
    public static final String ENTITY_CID_SEARCH_FILTER_VALUE = "[cid]";
    /**
     * Placeholder which is replaced by the context name
     */
    public static final String ENTITY_CNAME_SEARCH_FILTER_VALUE = "[cname]";

    /**
     * Initialises a new {@link LDAPResolverProperties}.
     */
    private LDAPResolverProperties(String suffix, Object defaultValue) {
        this.fqn = PREFIX + suffix;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}