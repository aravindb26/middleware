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

import static com.openexchange.java.Autoboxing.I;
import java.util.Map;
import com.openexchange.config.YamlUtils;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.Property;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link ConfigUtils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ConfigUtils extends YamlUtils {
    
    /** The yaml file holding the provider configs */
    public static final String CONFIG_FILENAME = "contacts-provider-ldap.yml";

    /** The yaml file holding the configured contact mappings */
    public static final String MAPPING_FILENAME = "contacts-provider-ldap-mappings.yml";

    /** The static prefix for the identifier of LDAP contact providers */
    public static final String PROVIDER_ID_PREFIX = "ldap.";

    /** The property to configure which contact provider accounts are available for a user */
    public static final Property PROPERTY_ACCOUNTS = DefaultProperty.valueOf("com.openexchange.contacts.ldap.accounts", "");

    /** The property to configure which contact fields are considered for the 'auto-complete' search */
    public static final Property PROPERTY_AUTOCOMPLETE_FIELDS = DefaultProperty.valueOf("com.openexchange.contact.autocomplete.fields", "GIVEN_NAME, SUR_NAME, DISPLAY_NAME, EMAIL1, EMAIL2, EMAIL3");

    /** The property to configure the number of characters a search pattern must contain */
    public static final Property PROPERTY_MINIMUM_SEARCH_CHARACTERS = DefaultProperty.valueOf("com.openexchange.MinimumSearchCharacters", I(0));

    /** The contact fields to use the mapped attributes from when resolving referenced distribution list members */
    public static final ContactField[] DISTLISTMEMBER_FIELDS = { 
        ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3, ContactField.FOLDER_ID, ContactField.OBJECT_ID, ContactField.DISPLAY_NAME, ContactField.SUR_NAME, ContactField.GIVEN_NAME
    };
    
    /** The contact fields to use the mapped attributes from when resolving referenced manager/assistant names */
    public static final ContactField[] DISPLAYNAME_FIELDS = { 
        ContactField.EMAIL1, ContactField.EMAIL2, ContactField.EMAIL3, ContactField.DISPLAY_NAME, ContactField.SUR_NAME, ContactField.GIVEN_NAME, ContactField.COMPANY            
    };        

    static <T> ProtectableValue<T> getProtectableValue(Map<String, Object> map, String key, Class<T> clazz) throws OXException {
        Map<String, Object> configEntry = asMap(map.get(key));
        if (null == configEntry) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(key);
        }
        boolean isProtected = getBoolean(configEntry, "protected");
        T defaultValue = opt(configEntry, "defaultValue", clazz, null);
        if (null == defaultValue) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create("defaultValue");
        }
        return new ProtectableValue<T>(defaultValue, isProtected);
    }

    static Filter getFilter(Map<String, Object> map, String key) throws OXException {
        String value = opt(map, key, String.class, null);
        if (null == value) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(key);
        }
        try {
            return Filter.create(value);
        } catch (LDAPException e) {
            throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(e, key);
        }
    }

    static SearchScope getSearchScope(Map<String, Object> map, String key) throws OXException {
        String value = opt(map, key, String.class, null);
        if (null == value) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(key);
        }
        for (SearchScope searchScope : SearchScope.values()) {
            if (searchScope.getName().equalsIgnoreCase(value)) {
                return searchScope;
            }
        }
        throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(key);
    }

    static SearchScope optSearchScope(Map<String, Object> map, String key, SearchScope defaultValue) throws OXException {
        String value = opt(map, key, String.class, null);
        if (null == value) {
            return defaultValue;
        }
        for (SearchScope searchScope : SearchScope.values()) {
            if (searchScope.getName().equalsIgnoreCase(value)) {
                return searchScope;
            }
        }
        throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(key);
    }

}
