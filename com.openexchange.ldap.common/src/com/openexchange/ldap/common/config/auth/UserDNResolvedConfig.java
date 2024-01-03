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

package com.openexchange.ldap.common.config.auth;

import static com.openexchange.ldap.common.config.ConfigUtils.get;
import static com.openexchange.ldap.common.config.ConfigUtils.getEnum;
import static com.openexchange.ldap.common.config.ConfigUtils.opt;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.config.ConfigUtils;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link UserDNResolvedConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class UserDNResolvedConfig {

    /**
     * Initializes a new {@link UserDNResolvedConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static UserDNResolvedConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new UserDNResolvedConfig.Builder() // @formatter:off
            .nameSource(getEnum(configEntry, "nameSource", UserNameSource.class))
            .searchFilterTemplate(get(configEntry, "searchFilterTemplate"))
            .searchScope(ConfigUtils.getSearchScope(configEntry, "searchScope"))
            .searchBaseDN(opt(configEntry, "searchBaseDN"))
            .searchAuthType(getEnum(configEntry, "searchAuthType", AuthType.class))
        .build(); // @formatter:on
    }

    private static class Builder {

        UserNameSource nameSource;
        String searchFilterTemplate;
        SearchScope searchScope;
        String searchBaseDN;
        AuthType searchAuthType;

        Builder() {
            super();
        }

        Builder nameSource(UserNameSource value) {
            this.nameSource = value;
            return this;
        }

        Builder searchFilterTemplate(String value) {
            this.searchFilterTemplate = value;
            return this;
        }

        Builder searchScope(SearchScope value) {
            this.searchScope = value;
            return this;
        }

        Builder searchBaseDN(String value) {
            this.searchBaseDN = value;
            return this;
        }

        Builder searchAuthType(AuthType value) {
            this.searchAuthType = value;
            return this;
        }

        UserDNResolvedConfig build() {
            return new UserDNResolvedConfig(this);
        }
    }

    private final UserNameSource nameSource;
    private final String searchFilterTemplate;
    private final SearchScope searchScope;
    private final String searchBaseDN;
    private final AuthType searchAuthType;

    /**
     * Initializes a new {@link UserDNResolvedConfig}.
     *
     * @param builder The builder to use for initialization
     */
    UserDNResolvedConfig(UserDNResolvedConfig.Builder builder) {
        super();
        this.nameSource = builder.nameSource;
        this.searchFilterTemplate = builder.searchFilterTemplate;
        this.searchScope = builder.searchScope;
        this.searchBaseDN = builder.searchBaseDN;
        this.searchAuthType = builder.searchAuthType;
    }

    /**
     * @return the nameSource
     */
    public UserNameSource getNameSource() {
        return nameSource;
    }

    /**
     * @return the searchFilterTemplate
     */
    public String getSearchFilterTemplate() {
        return searchFilterTemplate;
    }

    /**
     * @return the searchScope
     */
    public SearchScope getSearchScope() {
        return searchScope;
    }

    /**
     * @return the searchBaseDN
     */
    public String getSearchBaseDN() {
        return searchBaseDN;
    }

    /**
     * @return the searchAuthType
     */
    public AuthType getSearchAuthType() {
        return searchAuthType;
    }

}
