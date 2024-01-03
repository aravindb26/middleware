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
package com.openexchange.folderstorage;

import com.openexchange.config.lean.Property;

/**
 * {@link GlobalAddressBookProperties}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public enum GlobalAddressBookProperties implements Property {
    
    /**
     * "com.openexchange.contacts.gabFolderName"
     */
    GAB_FOLDER_NAME_IDENTIFIER("gabFolderName", "all_users"),
    /**
     * "com.openexchange.contacts.customGabFolderName"
     */
    CUSTOM_GAB_FOLDER_NAME("customGabFolderName", null),
    /**
     * "com.openexchange.contacts.customGabFolderName.[locale]"
     */
    CUSTOM_LOCALIZED_GAB_FOLDER_NAME("customGabFolderName.[locale]", null);
    
    /**
     * 'Global address book' identifier
     */
    public static final String GLOBAL_ADDRESS_BOOK_ID = "global_address_book";
    /**
     * 'Internal users' identifier
     */
    public static final String INTERNAL_USERS_ID = "internal_users";
    /**
     * 'All users' identifier
     */
    public static final String ALL_USERS_ID = "all_users";
    /**
     * Custom identifier. Name has to be configured using {@link CUSTOM_GAB_FOLDER_NAME}
     */
    public static final String CUSTOM = "custom";
    
    private final String fqn;
    private final String defaultValue;
    
    /**
     * Initializes a new {@link GlobalAddressBookProperties}.
     *
     * @param suffix The property name suffix to take over
     * @param defaultValue The property's default value
     */
    private GlobalAddressBookProperties(String suffix, String defaultValue) {
        this.defaultValue = defaultValue;
        this.fqn = "com.openexchange.contacts." + suffix;
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
