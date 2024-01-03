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

package com.openexchange.passwordchange.common;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.Property;

/**
 * {@link PasswordChangeProperties}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.6
 *
 */
public enum PasswordChangeProperties implements Property {

    /** The minimum length of a password */
    MIN_LENGTH("minLength", I(4)),

    /** The maximum length of a password. <code>0</code> means no limit */
    MAX_LENGTH("maxLength", I(0)),

    /** A regex pattern new passwords must meet. Empty by default */
    ALLOWED_PATTERN("allowedPattern", ""),

    /** A displayable hint for the user how the pattern can be met. Empty by default */
    PATTERN_HINT("allowedPatternHint", ""),

    /** Whether or not the specific password change service identified by its <code>providerId</code> is enabled or not */
    ENABLED("[providerId].enabled", Boolean.FALSE),
    ;

    public static final String PREFIX = "com.openexchange.passwordchange.";
    private final String name;
    private final Object defaultValue;

    /**
     * Initializes a new {@link PasswordChangeProperties}.
     *
     */
    private PasswordChangeProperties(String name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;

    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
