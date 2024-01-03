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
package com.openexchange.log.audit.slf4j;

import com.openexchange.config.lean.Property;

/**
 * {@link Slf4jAuditLoggingDateProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public enum Slf4jAuditLoggingDateProperty implements Property {

    /**
     * Specifies the optional date pattern to use.
     * Accepts a pattern according to: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
     *
     * If empty, standard ISO-8601 formatting is used and accompanying properties "locale" and "timezone" are ignored.
     * If a pattern is specified the accompanying properties may optionally be used to also define the locale and time zone to use for date formatting.
     */
    pattern(null),

    /**
     * Specifies the optional locale
     */
    locale("en_US"),

    /**
     * Specifies the optional timezone
     */
    timezone("GMT"),

    ;

    private static final String PREFIX = "com.openexchange.log.audit.slf4j.date.";
    private final Object defaultValue;

    private Slf4jAuditLoggingDateProperty(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
