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

package com.openexchange.authentication.application.storage.rdb.passwords;

import com.openexchange.database.AbstractCreateTableImpl;

/**
 * {@link CreateHistoryTable}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.4
 */
public class CreateHistoryTable extends AbstractCreateTableImpl {

    private static final String TABLE_APP_HISTORY = "app_passwords_history";

    //@formatter:off
    private static final String CREATE_APP_HISTORY_TABLE =
        "CREATE TABLE " + TABLE_APP_HISTORY + " ( "
            + "`uuid` binary(16) NOT NULL, "
            + "`cid` int(10) unsigned NOT NULL, "
            + "`user` int(10) unsigned NOT NULL, "
            + "`timestamp` bigint(20) DEFAULT NULL, "
            + "`client` varchar(128) DEFAULT NULL, "
            + "`userAgent` varchar(256) DEFAULT NULL, "
            + "`ip` varchar(64) DEFAULT NULL, "
//            + "`lastLogin` bigint(20) DEFAULT NULL, "
//            + "`lastDevice` varchar(128) DEFAULT NULL, "
//            + "`lastIp` varchar(64) DEFAULT NULL, "
            + "PRIMARY KEY (`uuid`), "
            + "KEY `id` (`cid`,`user`,`uuid`)) "
            + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    //@formatter:on

    @Override
    public String[] requiredTables() {
        return NO_TABLES;
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { TABLE_APP_HISTORY };
    }

    @Override
    protected String[] getCreateStatements() {
        return new String[] { CREATE_APP_HISTORY_TABLE };
    }
}
