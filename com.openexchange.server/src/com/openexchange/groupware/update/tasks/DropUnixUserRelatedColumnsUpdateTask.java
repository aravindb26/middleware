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

import static com.openexchange.java.Strings.getEmptyStrings;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateConcurrency;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.groupware.update.WorkingLevel;
import com.openexchange.tools.update.Column;
import com.openexchange.tools.update.Tools;

/**
 * {@link DropUnixUserRelatedColumnsUpdateTask} - drops unix related columns from user and group tables
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class DropUnixUserRelatedColumnsUpdateTask implements UpdateTaskV2 {

    /**
     * Initializes a new {@link DropUnixUserRelatedColumnsUpdateTask}.
     */
    public DropUnixUserRelatedColumnsUpdateTask() {
        super();
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        try {
            List<Column> userColumns = List.of(new Column("gidNumber", null),
                                                new Column("uidNumber", null),
                                                new Column("homeDirectory", null),
                                                new Column("loginShell", null));

            List<Column> groupsColumns = List.of(new Column("gidNumber", null));

            // drops columns from groups tables
            checkAndDropColumns(con, "groups", groupsColumns);
            // drops columns from groups delete table
            checkAndDropColumns(con, "del_groups", groupsColumns);
            // drops columns from user table
            checkAndDropColumns(con, "user", userColumns);
            // drops columns from user delete table
            checkAndDropColumns(con, "del_user", userColumns);

            // Drop unused sequence tables (if existent)
            if (Tools.tableExists(con, "sequence_uid_number")) {
                Tools.dropTable(con, "sequence_uid_number");
            }
            if (Tools.tableExists(con, "sequence_gid_number")) {
                Tools.dropTable(con, "sequence_gid_number");
            }
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        }
    }

    /**
     * Drops all existing columns from specified table.
     *
     * @param con The connection to use
     * @param table The corresponding table name
     * @param columns The column to default value mapping
     * @throws SQLException in case of errors
     */
    private static void checkAndDropColumns(Connection con, String table, List<Column> columns) throws SQLException {
        if (Tools.tableExists(con, table)) {
            Tools.checkAndDropColumns(con, table, columns.toArray(new Column[columns.size()]));
        }
    }

    @Override
    public String[] getDependencies() {
        return getEmptyStrings();
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(UpdateConcurrency.BLOCKING, WorkingLevel.SCHEMA);
    }

}
