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

package com.openexchange.ldap.common.config;

import static com.openexchange.ldap.common.config.ConfigUtils.asMap;
import static com.openexchange.ldap.common.config.ConfigUtils.opt;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.config.auth.AuthConfig;

/**
 * {@link LDAPConfig}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LDAPConfig {

    /**
     * Initializes a new {@link LDAPConfig} from the supplied .yaml-based configuration.
     *
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static LDAPConfig init(String name, Map<String, Object> configEntry) throws OXException {
        return new LDAPConfig.Builder() // @formatter:off
            .name(name)
            .baseDN(opt(configEntry, "baseDN"))
            .authConfig(AuthConfig.init(asMap(configEntry.get("auth"))))
            .pool(LDAPConnectionPoolConfig.init(asMap(configEntry.get("pool"))))
            .build(); // @formatter:on
    }

    /**
     * {@link Builder} is a builder for the {@link LDAPConfig}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.6
     */
    private static class Builder {

        String name;
        String baseDN;
        AuthConfig authConfig;
        LDAPConnectionPoolConfig pool = null;

        Builder() {
            super();
        }

        Builder name(String value) {
            this.name = value;
            return this;
        }

        Builder baseDN(String value) {
            this.baseDN = value;
            return this;
        }

        Builder authConfig(AuthConfig value) {
            this.authConfig = value;
            return this;
        }

        Builder pool(LDAPConnectionPoolConfig servers) {
            this.pool = servers;
            return this;
        }

        LDAPConfig build() {
            return new LDAPConfig(this);
        }
    }

    private final String name;
    private final String baseDN;
    private final AuthConfig authConfig;
    private final LDAPConnectionPoolConfig pool;

    /**
     * Initializes a new {@link LDAPConfig}.
     *
     * @param builder The builder to use for initialization
     */
    LDAPConfig(LDAPConfig.Builder builder) {
        super();
        this.name = builder.name;
        this.baseDN = builder.baseDN;
        this.authConfig = builder.authConfig;
        this.pool = builder.pool;
    }

    /**
     * Gets the display name for the contacts provider.
     *
     * @return The display name for the contacts provider
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the configured base LDAP path. If defined, all Distinguished Names supplied to
     * and received from LDAP operations will be relative to the LDAP path
     * supplied. If not defined, the default naming context of the RootDSE is used as baseDN.
     *
     * @return The base DN
     */
    public String getBaseDN() {
        return baseDN;
    }

    /**
     * Gets the {@link AuthConfig}
     *
     * @return the {@link AuthConfig}
     */
    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    /**
     * Gets the {@link LDAPConnectionPoolConfig} configuration
     *
     * @return The {@link LDAPConnectionPoolConfig}
     */
    public LDAPConnectionPoolConfig getPool() {
        return pool;
    }

}
