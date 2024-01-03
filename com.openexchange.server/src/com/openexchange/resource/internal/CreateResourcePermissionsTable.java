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

package com.openexchange.resource.internal;

import com.openexchange.database.AbstractCreateTableImpl;

/**
 * {@link CreateResourcePermissionsTable}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class CreateResourcePermissionsTable extends AbstractCreateTableImpl {

    public final static String TABLE_NAME = "resource_permissions";

    public CreateResourcePermissionsTable() {
        super();
    }

    @Override
    public String[] requiredTables() {
        return NO_TABLES;
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { TABLE_NAME };
    }

    @Override
    protected String[] getCreateStatements() {
        // @formatter:off
        String createStmt = "CREATE TABLE `resource_permissions` ("
            + "`cid` INT4 UNSIGNED NOT NULL,"
            + "`resource` INT4 UNSIGNED NOT NULL,"
            + "`entity` INT4 UNSIGNED NOT NULL,"
            + "`group` boolean NOT NULL,"
            + "`privilege` VARCHAR(64) NOT NULL,"
            + "PRIMARY KEY (`cid`,`resource`,`entity`)"
          + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
        // @formatter:on
        return new String[] { createStmt };
    }

}
