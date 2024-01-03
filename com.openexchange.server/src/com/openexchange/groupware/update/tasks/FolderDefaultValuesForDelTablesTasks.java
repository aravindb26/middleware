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

package com.openexchange.groupware.update.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.java.Strings;

/**
 * {@link FolderDefaultValuesForDelTablesTasks} - Adds default values to the 'del_oxfolder_tree', and 'virtualBackupTree' tables.
 * <p>
 * <ul>
 * <li>del_oxfolder_tree<ul><li>fname</ul>
 * <li>virtualBackupTree<ul><li>name</ul>
 * </ul>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderDefaultValuesForDelTablesTasks extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link FolderDefaultValuesForDelTablesTasks}.
     */
    public FolderDefaultValuesForDelTablesTasks() {
        super();
    }

    @Override
    public String[] getDependencies() {
        return Strings.getEmptyStrings();
    }

    @Override
    public void perform(final PerformParameters params) throws OXException {
        Connection connection = params.getConnection();
        int rollback = 0;
        try {
            connection.setAutoCommit(false);
            rollback = 1;

            if (!com.openexchange.tools.update.Tools.hasDefaultValue(connection, "del_oxfolder_tree", "fname")) {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("ALTER TABLE del_oxfolder_tree ALTER COLUMN fname SET DEFAULT ''");
                    stmt.executeUpdate();
                } finally {
                    Databases.closeSQLStuff(stmt);
                }
            }

            if (!com.openexchange.tools.update.Tools.hasDefaultValue(connection, "virtualBackupTree", "name")) {
                PreparedStatement stmt = null;
                try {
                    stmt = connection.prepareStatement("ALTER TABLE virtualBackupTree ALTER COLUMN name SET DEFAULT ''");
                    stmt.executeUpdate();
                } finally {
                    Databases.closeSQLStuff(stmt);
                }
            }

            connection.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(connection);
                }
                Databases.autocommit(connection);
            }
        }
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(com.openexchange.groupware.update.UpdateConcurrency.BLOCKING);
    }

}
