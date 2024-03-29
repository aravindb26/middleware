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
package com.openexchange.mail;

import com.openexchange.config.lean.Property;

/**
 * {@link MailProperty}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public enum MailProperty implements Property {

    /**
     * Whether mail transport of external mail accounts is allowed or not.
     */
    SMTP_ALLOW_EXTERNAL("smtp.allowExternal", Boolean.TRUE)
    ;

    private static final String PREFIX = "com.openexchange.mail.";

    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link MailProperty}.
     *
     */
    private MailProperty(String name, Object defaultValue) {
        this.fqn = PREFIX + name;
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
