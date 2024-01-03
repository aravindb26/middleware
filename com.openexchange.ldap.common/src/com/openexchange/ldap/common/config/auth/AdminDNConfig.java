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
import java.util.Map;
import com.openexchange.exception.OXException;

/**
 * {@link AdminDNConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class AdminDNConfig {

    /**
     * Initializes a new {@link AdminDNConfig} from the supplied .yaml-based provider configuration section.
     * 
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static AdminDNConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new AdminDNConfig.Builder() // @formatter:off
            .dn(get(configEntry, "dn"))
            .password(get(configEntry, "password"))
        .build(); // @formatter:on
    }

    private static class Builder {

        String dn;
        String password;

        Builder() {
            super();
        }

        Builder dn(String value) {
            this.dn = value;
            return this;
        }

        Builder password(String value) {
            this.password = value;
            return this;
        }

        AdminDNConfig build() {
            return new AdminDNConfig(this);
        }
    }

    private final String dn;
    private final String password;

    /**
     * Initializes a new {@link AdminDNConfig}.
     * 
     * @param builder The builder to use for initialization
     */
    AdminDNConfig(AdminDNConfig.Builder builder) {
        super();
        this.dn = builder.dn;
        this.password = builder.password;
    }

    /**
     * Gets the distinguished name used for administrative bind operations.
     *
     * @return The distinguished name
     */
    public String getDn() {
        return dn;
    }

    /**
     * Gets the password to use for administrative bind operations.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

}
