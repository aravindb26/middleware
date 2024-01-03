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
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.BindRequestFactory;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;

/**
 * {@link CustomConfig}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class CustomConfig {

    /**
     * Initializes a new {@link CustomConfig} from the supplied .yaml-based provider configuration section.
     * 
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     * @throws OXException In case value is missing
     */
    public static CustomConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new CustomConfig.Builder() // @formatter:off
                               .id(get(configEntry, "id"))
                               .initPoolAuthType(getEnum(configEntry, "initPoolAuthType", AuthType.class))
                               .build(); // @formatter:on
    }

    private static class Builder {

        String id;
        AuthType initPoolAuthType;

        Builder() {
            super();
        }

        Builder id(String value) {
            this.id = value;
            return this;
        }

        Builder initPoolAuthType(AuthType value) {
            this.initPoolAuthType = value;
            return this;
        }

        CustomConfig build() {
            return new CustomConfig(this);
        }
    }

    private final String id;
    private final AuthType initPoolAuthType;

    /**
     * Initializes a new {@link CustomConfig}.
     * 
     * @param builder The builder to use for initialization
     */
    CustomConfig(CustomConfig.Builder builder) {
        super();
        this.id = builder.id;
        this.initPoolAuthType = builder.initPoolAuthType;
    }

    /**
     * Gets the id of the custom {@link BindRequestFactory} implementation.
     *
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the auth type for connection pool initilization
     *
     * @return The auth type
     * @throws OXException In case the auth type is not ADMINDN or ANONYMOUS
     */
    public AuthType getInitPoolAuthType() throws OXException {
        if (initPoolAuthType.equals(AuthType.ADMINDN) || initPoolAuthType.equals(AuthType.ANONYMOUS)) {
            return initPoolAuthType;
        }
        throw LDAPCommonErrorCodes.INIT_POOL_AUTH_ERROR.create(initPoolAuthType);
    }

}
