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

/**
 * {@link UserDNTemplateConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class UserDNTemplateConfig {

    /**
     * Initializes a new {@link UserDNTemplateConfig} from the supplied .yaml-based provider configuration section.
     * 
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static UserDNTemplateConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new UserDNTemplateConfig.Builder() // @formatter:off
            .nameSource(getEnum(configEntry, "nameSource", UserNameSource.class))
            .dnTemplate(get(configEntry, "dnTemplate"))
        .build(); // @formatter:on
    }

    private static class Builder {

        UserNameSource nameSource;
        String dnTemplate;

        Builder() {
            super();
        }

        Builder nameSource(UserNameSource value) {
            this.nameSource = value;
            return this;
        }

        Builder dnTemplate(String value) {
            this.dnTemplate = value;
            return this;
        }

        UserDNTemplateConfig build() {
            return new UserDNTemplateConfig(this);
        }
    }

    private final UserNameSource nameSource;
    private final String dnTemplate;

    /**
     * Initializes a new {@link UserDNTemplateConfig}.
     * 
     * @param builder The builder to use for initialization
     */
    UserDNTemplateConfig(UserDNTemplateConfig.Builder builder) {
        super();
        this.nameSource = builder.nameSource;
        this.dnTemplate = builder.dnTemplate;
    }

    /**
     * @return the nameSource
     */
    public UserNameSource getNameSource() {
        return nameSource;
    }

    /**
     * @return the dn template
     */
    public String getDNTemplate() {
        return dnTemplate;
    }

}
