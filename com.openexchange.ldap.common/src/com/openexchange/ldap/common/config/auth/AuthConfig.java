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

import static com.openexchange.ldap.common.config.ConfigUtils.asMap;
import static com.openexchange.ldap.common.config.ConfigUtils.getEnum;
import java.util.Map;
import java.util.Optional;
import com.openexchange.exception.OXException;

/**
 * {@link AuthConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class AuthConfig {

    /**
     * Initializes a new {@link AuthConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static AuthConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new AuthConfig.Builder() // @formatter:off
            .type(getEnum(configEntry, "type", AuthType.class))
            .adminDNConfig(AdminDNConfig.init(asMap(configEntry.get("adminDN"))))
            .userDNTemplateConfig(UserDNTemplateConfig.init(asMap(configEntry.get("userDNTemplate"))))
            .userDNResolvedConfig(UserDNResolvedConfig.init(asMap(configEntry.get("userDNResolved"))))
            .customConfig(CustomConfig.init(asMap(configEntry.get("custom"))))
        .build(); // @formatter:on
    }

    private static class Builder {

        AuthType type;
        AdminDNConfig adminDNConfig;
        UserDNTemplateConfig userDNTemplateConfig;
        UserDNResolvedConfig userDNResolvedConfig;
        CustomConfig customConfig;

        Builder() {
            super();
        }

        Builder type(AuthType value) {
            this.type = value;
            return this;
        }

        Builder adminDNConfig(AdminDNConfig value) {
            this.adminDNConfig = value;
            return this;
        }

        Builder userDNTemplateConfig(UserDNTemplateConfig value) {
            this.userDNTemplateConfig = value;
            return this;
        }

        Builder userDNResolvedConfig(UserDNResolvedConfig value) {
            this.userDNResolvedConfig = value;
            return this;
        }
        
        Builder customConfig(CustomConfig value) {
            this.customConfig = value;
            return this;
        }

        AuthConfig build() {
            return new AuthConfig(this);
        }
    }

    private final AuthType type;
    private final Optional<AdminDNConfig> adminDNConfig;
    private final Optional<UserDNTemplateConfig> userDNTemplateConfig;
    private final Optional<UserDNResolvedConfig> userDNResolvedConfig;
    private final Optional<CustomConfig> customConfig;

    /**
     * Initializes a new {@link AuthConfig}.
     *
     * @param builder The builder to use for initialization
     */
    AuthConfig(AuthConfig.Builder builder) {
        super();
        this.type = builder.type;
        this.adminDNConfig = Optional.ofNullable(builder.adminDNConfig);
        this.userDNTemplateConfig = Optional.ofNullable(builder.userDNTemplateConfig);
        this.userDNResolvedConfig = Optional.ofNullable(builder.userDNResolvedConfig);
        this.customConfig = Optional.ofNullable(builder.customConfig);
    }

    /**
     * @return the type
     */
    public AuthType getType() {
        return type;
    }

    /**
     * @return the adminDNConfig
     */
    public Optional<AdminDNConfig> optAdminDNConfig() {
        return adminDNConfig;
    }

    /**
     * @return the userDNTemplateConfig
     */
    public Optional<UserDNTemplateConfig> optUserDNTemplateConfig() {
        return userDNTemplateConfig;
    }

    /**
     * @return the userDNResolvedConfig
     */
    public Optional<UserDNResolvedConfig> optUserDNResolvedConfig() {
        return userDNResolvedConfig;
    }
    
    /**
     * @return the customConfig
     */
    public Optional<CustomConfig> optCustomConfig() {
        return customConfig;
    }

}
