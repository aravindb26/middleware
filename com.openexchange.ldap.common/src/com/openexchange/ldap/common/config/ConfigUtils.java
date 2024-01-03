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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.openexchange.config.YamlUtils;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.exception.OXException;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link ConfigUtils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ConfigUtils extends YamlUtils {

    /**
     * The YAML file holding the client configurations.
     */
    public static final String CONFIG_FILENAME = "ldap-client-config.yml";

    /**
     * Maps the given object to a list
     *
     * @param yaml The object to map
     * @return The list or null in case no object is given
     * @throws OXException in case the given object is not a list
     */
    static List<Object> asList(Object yaml) throws OXException {
        if (null == yaml) {
            return null;
        }
        if (false == (yaml instanceof List)) {
            throw ConfigurationExceptionCodes.NOT_READABLE.create(CONFIG_FILENAME);
        }
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) yaml;
        return list;
    }

    /**
     * Gets the {@link SearchScope} value from the map with the given key
     *
     * @param map The map containing the key
     * @param key The key
     * @return The {@link SearchScope}
     * @throws OXException in case the value is missing or invalid
     */
    public static SearchScope getSearchScope(Map<String, Object> map, String key) throws OXException {
        String value = opt(map, key);
        if (null == value) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(key);
        }
        switch (value.toUpperCase(Locale.US)) {
            case "SUB":
                return SearchScope.SUB;
            case "ONE":
                return SearchScope.ONE;
            case "BASE":
                return SearchScope.BASE;
            case "SUBORDINATE_SUBTREE":
                return SearchScope.SUBORDINATE_SUBTREE;
            default:
                throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(key);
        }
    }

}
