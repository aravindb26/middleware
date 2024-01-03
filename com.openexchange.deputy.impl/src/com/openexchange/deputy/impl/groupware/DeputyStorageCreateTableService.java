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

package com.openexchange.deputy.impl.groupware;

import static com.openexchange.java.Strings.getEmptyStrings;
import com.openexchange.database.AbstractCreateTableImpl;

/**
 * {@link DeputyStorageCreateTableService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyStorageCreateTableService extends AbstractCreateTableImpl {

    private static final DeputyStorageCreateTableService INSTANCE = new DeputyStorageCreateTableService();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static DeputyStorageCreateTableService getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private DeputyStorageCreateTableService() {
        super();
    }

    private static final String DEPUTY_STORAGE_TABLE = "deputy";

    // @formatter:off
    private static final String CREATE_DEPUTY_STORAGE_TABLE = "CREATE TABLE " + DEPUTY_STORAGE_TABLE + "("
        + "uuid BINARY(16) NOT NULL,"
        + "cid INT4 unsigned NOT NULL,"
        + "user INT4 unsigned NOT NULL,"
        + "entity INT4 unsigned NOT NULL,"
        + "groupFlag TINYINT(1) NOT NULL DEFAULT 0,"
        + "sendOnBehalfOf TINYINT(1) NOT NULL DEFAULT 0,"
        + "moduleIds TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,"
        + "PRIMARY KEY (uuid),"
        + "KEY id (cid, user, uuid),"
        + "KEY entityId (cid, entity)"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    // @formatter:on

    @Override
    public String[] requiredTables() {
        return getEmptyStrings();
    }

    @Override
    public String[] tablesToCreate() {
        return new String[] { DEPUTY_STORAGE_TABLE };
    }

    @Override
    public String[] getCreateStatements() {
        return new String[] { CREATE_DEPUTY_STORAGE_TABLE };
    }

}
