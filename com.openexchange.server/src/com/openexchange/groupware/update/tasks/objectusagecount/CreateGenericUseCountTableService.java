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
package com.openexchange.groupware.update.tasks.objectusagecount;

import com.openexchange.database.AbstractCreateTableImpl;


/**
 * {@link CreateGenericUseCountTableService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
public class CreateGenericUseCountTableService extends AbstractCreateTableImpl {

    public CreateGenericUseCountTableService() {
        super();
    }

    @Override
    public String[] requiredTables() {
        return new String[] { "object_use_count" };
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { "generic_use_count" };
    }

    @Override
    protected String[] getCreateStatements() {
        return new String [] {
            "CREATE TABLE `generic_use_count` ("
                + "  `cid` int(10) unsigned NOT NULL,"
                + "  `uuid` binary(16) NOT NULL,"
                + "  `user` int(10) unsigned NOT NULL,"
                + "  `module` int(4) unsigned NOT NULL,"
                + "  `account` int(4) unsigned NOT NULL,"
                + "  `folder` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,"
                + "  `object` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,"
                + "  `value` int(10) unsigned NOT NULL,"
                + "  `lastModified` bigint(20) NOT NULL,"
                + "  PRIMARY KEY (`cid`,`uuid`),"
                + "  UNIQUE KEY `usercount` (`cid`,`user`,`module`,`account`,`folder`,`object`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=UTF8MB4_UNICODE_CI;"
        };
    }

}
