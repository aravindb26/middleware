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
package com.openexchange.mail.login.resolver;

import static com.openexchange.java.Autoboxing.B;
import com.openexchange.config.lean.Property;

/**
 * {@link LDAPResolver}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public enum MailLoginResolverProperties implements Property {

    /**
     * Enables the {@link MailLoginResolverService}
     * Default: false
     */
    ENABLED("enabled", B(false))
    ;

    private final String fqn;
    private final Boolean defaultValue;
    private static final String PREFIX = "com.openexchange.mail.login.resolver.";

    /**
     * Initialises a new {@link MailLoginResolverProperties}.
     */
    private MailLoginResolverProperties(String suffix, Boolean defaultValue) {
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
