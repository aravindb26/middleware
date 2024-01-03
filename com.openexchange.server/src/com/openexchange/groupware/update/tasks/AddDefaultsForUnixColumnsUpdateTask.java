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

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Strings.getEmptyStrings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateConcurrency;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskV2;
import com.openexchange.groupware.update.WorkingLevel;
import com.openexchange.tools.update.Tools;

/**
 * {@link AddDefaultsForUnixColumnsUpdateTask} - adds defaults to all unix related columns so that they can be removed safely in a future release
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class AddDefaultsForUnixColumnsUpdateTask implements UpdateTaskV2 {

    private static final String USER_TABLE = "user";
    private static final String GROUPS_TABLE = "groups";
    private static final String USER_DELETE_TABLE = "del_user";
    private static final String GROUPS_DELETE_TABLE = "del_groups";

    private static final Map<String, Object> USER_COLUMNS = new HashMap<String, Object>(4);
    private static final Map<String, Object> USER_DEL_COLUMNS = new HashMap<String, Object>(2);
    private static final Map<String, Object> GROUPS_COLUMNS = new HashMap<String, Object>(1);
    private static final Map<String, Object> GROUPS_DEL_COLUMNS = new HashMap<String, Object>(1);

    static {
        // User table
        USER_COLUMNS.put("gidNumber", I(0));
        USER_COLUMNS.put("uidNumber", I(0));
        USER_COLUMNS.put("homeDirectory", "''");
        USER_COLUMNS.put("loginShell", "''");

        // User delete table
        USER_DEL_COLUMNS.put("gidNumber", I(0));
        USER_DEL_COLUMNS.put("uidNumber", I(0));

        // Groups table
        GROUPS_COLUMNS.put("gidNumber", I(0));

        // Groups delete table
        GROUPS_DEL_COLUMNS.put("gidNumber", I(0));
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        try {
            // change groups tables
            checkAndAdjustColumn(con, GROUPS_TABLE, GROUPS_COLUMNS);
            // change groups delete table
            checkAndAdjustColumn(con, GROUPS_DELETE_TABLE, GROUPS_DEL_COLUMNS);
            // change user table
            checkAndAdjustColumn(con, USER_TABLE, USER_COLUMNS);
            // change user delete table
            checkAndAdjustColumn(con, USER_DELETE_TABLE, USER_DEL_COLUMNS);
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        }
    }

    /**
     * Adjusts the defaults for all existing columns
     *
     * @param con The connection to use
     * @param table The corresponding table name
     * @param columns The column to default value mapping
     * @throws SQLException in case of errors
     */
    private void checkAndAdjustColumn(Connection con, String table, Map<String, Object> columns) throws SQLException {
        List<Map.Entry<String, Object>> columns2adjust = new ArrayList<>(columns.size());
        for (Map.Entry<String, Object> col : columns.entrySet()) {
            if (null != Tools.getColumnTypeName(con, table, col.getKey())) {
                columns2adjust.add(col);
            }
        }
        adjustColumn(con, table, columns2adjust);
    }

    /**
     * Adjusts the default value for all given columns
     *
     * @param con The connection to use
     * @param table The corresponding table name
     * @param cols The columns to adjust
     * @throws SQLException in case of errors
     */
    private void adjustColumn(Connection con, String table, List<Map.Entry<String, Object>> cols) throws SQLException {
        if (null == cols || cols.size() == 0) {
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(table);
        boolean first = true;
        for (Map.Entry<String, Object> col : cols) {
            if (first) {
                sql.append(" alter ").append(col.getKey()).append(" set default ").append(col.getValue());
                first = false;
                continue;
            }
            sql.append(", alter ").append(col.getKey()).append(" set default ").append(col.getValue());
        }

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(sql.toString());
        } finally {
            closeSQLStuff(stmt);
        }
    }

    @Override
    public String[] getDependencies() {
        return getEmptyStrings();
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(UpdateConcurrency.BACKGROUND, WorkingLevel.SCHEMA);
    }

}
