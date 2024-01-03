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

package com.openexchange.contact.provider.ldap.config;

import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getBoolean;
import java.util.Map;
import java.util.Objects;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.exception.OXException;

/**
 * {@link ProtectableValue}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ProtectableValue<T> {

    /**
     * Initializes a new {@link ProtectableValue} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static <T> ProtectableValue<T> init(Map<String, Object> configEntry, Class<T> clazz) throws OXException {
        if (null == configEntry) {
            return null;
        }
        boolean isProtected = getBoolean(configEntry, "isProtected");
        T defaultValue = ConfigUtils.opt(configEntry, "defaultValue", clazz, null);
        if (null == defaultValue) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create("defaultValue");
        }
        return new ProtectableValue<T>(defaultValue, isProtected);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final T defaultValue;
    private final boolean isProtected;

    /**
     * Initializes a new {@link ProtectableValue}.
     *
     * @param defaultValue The default value
     * @param isProtected <code>true</code> if the value is <i>protected</i>, <code>false</code>, otherwise
     */
    public ProtectableValue(T defaultValue, boolean isProtected) {
        super();
        this.defaultValue = defaultValue;
        this.isProtected = isProtected;
    }

    /**
     * Gets the default value.
     *
     * @return The default value
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Gets a value indicating whether the value is <i>protected</i> or not.
     *
     * @return <code>true</code> if the value is <i>protected</i>, <code>false</code>, otherwise
     */
    public boolean isProtected() {
        return isProtected;
    }

    /**
     * Gets a value indicating whether the value is <i>protected</i>, and defaults to the given value.
     *
     * @return <code>true</code> if the value is <i>protected</i> and defaults to the given value, <code>false</code>, otherwise
     */
    public boolean isProtectedAndDefaultsTo(T defaultValue) {
        return isProtected && Objects.equals(getDefaultValue(), defaultValue);
    }

    @Override
    public String toString() {
        return "ProtectableValue [defaultValue=" + defaultValue + ", isProtected=" + isProtected + "]";
    }

}
