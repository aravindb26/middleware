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

package com.openexchange.ajax.requesthandler.converters.preview.cache.groupware;

import com.openexchange.database.AbstractCreateTableImpl;

/**
 * {@link PreviewCacheCreateDataTableService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PreviewCacheCreateDataTableService extends AbstractCreateTableImpl {

    /**
     * Gets the table names.
     *
     * @return The table names.
     */
    public static String[] getTablesToCreate() {
        return new String[] { "previewData" };
    }

    /**
     * Gets the CREATE-TABLE statements.
     *
     * @return The CREATE statements
     */
    public static String[] getCreateStmts() {
        return new String[] { "CREATE TABLE previewData (" +
            " cid INT4 unsigned NOT NULL," +
            " user INT4 unsigned NOT NULL," +
            " id VARCHAR(128) CHARACTER SET latin1 NOT NULL," +
            " data LONGBLOB," +
            " PRIMARY KEY (cid, user, id)," +
            " INDEX `globaldocument` (cid, id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" };
    }

    /**
     * Initializes a new {@link PreviewCacheCreateDataTableService}.
     */
    public PreviewCacheCreateDataTableService() {
        super();
    }

    @Override
    public String[] requiredTables() {
        return new String[] { "preview" };
    }

    @Override
    public String[] tablesToCreate() {
        return getTablesToCreate();
    }

    @Override
    protected String[] getCreateStatements() {
        return getCreateStmts();
    }
}
