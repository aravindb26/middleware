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

/**
 * {@link FolderConfigMode} - An enumeration of folder configuration modes.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public enum FolderConfigMode {

    /**
     * With mode "fixed attributes", all entries matching a filter and having an attribute set to one of the defined values do form a
     * folder.
     */
    FIXEDATTRIBUTES,

    /**
     * With mode "dynamic attributes", all possible values for one attribute are fetched periodically and serve as folders.
     */
    DYNAMICATTRIBUTES,

    /**
     * In "static" folder mode, a fixed list of folder definitions is used, each one with its own contact filter and name.
     */
    STATIC,
    ;

}
