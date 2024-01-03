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

package com.openexchange.contact.provider.ldap;

import java.util.Objects;
import org.json.JSONObject;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.provider.CommonContactsConfigurationFields;
import com.openexchange.contact.provider.ldap.config.ProtectableValue;
import com.openexchange.contact.provider.ldap.config.ProviderConfig;
import com.openexchange.exception.OXException;

/**
 * {@link SettingsHelper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class SettingsHelper {

    /**
     * Prevent initialization.
     */
    private SettingsHelper() {
        super();
    }

    static boolean setFolderProperty(JSONObject internalConfig, String folderId, String propertyName, ProtectableValue<?> propertyConfig, Object newValue) throws OXException {
        Object oldValue = putFolderProperty(internalConfig, folderId, propertyName, newValue);
        if (false == Objects.equals(oldValue, newValue)) {
            if (propertyConfig.isProtected() && false == Objects.equals(propertyConfig.getDefaultValue(), newValue)) {
                throw LdapContactsExceptionCodes.CANT_CHANGE_PROTECTED_FOLDER_PROPERTY.create(folderId, propertyName);
            }
            return true;
        }
        return false;
    }

    static JSONObject optFolderSettings(ContactsAccount account, String folderId) {
        JSONObject configObject = account.getInternalConfiguration();
        if (null != configObject) {
            JSONObject foldersObject = configObject.optJSONObject("folders");
            if (null != foldersObject) {
                return foldersObject.optJSONObject(folderId);
            }
        }
        return null;
    }

    static Object putFolderProperty(JSONObject internalConfig, String folderId, String propertyName, Object value) {
        JSONObject foldersObject = internalConfig.optJSONObject("folders");
        if (null == foldersObject) {
            foldersObject = new JSONObject();
            internalConfig.putSafe("folders", foldersObject);
        }
        JSONObject folderObject = foldersObject.optJSONObject(folderId);
        if (null == folderObject) {
            folderObject = new JSONObject();
            foldersObject.putSafe(folderId, folderObject);
        }
        Object oldValue = folderObject.optIfNotNull(propertyName);
        folderObject.putSafe(propertyName, value);
        return oldValue;
    }

    static JSONObject initInternalConfig(ProviderConfig providerConfig) {
        JSONObject internalConfig = new JSONObject();
        internalConfig.putSafe(CommonContactsConfigurationFields.NAME, providerConfig.getName());
        return internalConfig;
    }

}
