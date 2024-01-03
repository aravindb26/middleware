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
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.tools.update.Tools;

/**
 * {@link DropForeignKeysFromSettingsTables} - Drops rather needless foreign keys from several settings tables.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DropForeignKeysFromSettingsTables extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link DropForeignKeysFromSettingsTables}.
     */
    public DropForeignKeysFromSettingsTables() {
        super();
    }

    @Override
    public String[] getDependencies() {
        return getEmptyStrings();
    }

    @Override
    public void perform(final PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        int rollback = 0;
        try {
            Databases.startTransaction(con);
            rollback = 1;

            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DropForeignKeysFromSettingsTables.class);

            // Drop foreign keys from...
            for (String table : Arrays.asList(
                                              "user_configuration",
                                              "user_setting_mail",
                                              "user_setting_mail_signature",
                                              "user_setting_spellcheck",
                                              "user_setting_admin",
                                              "user_setting_server",
                                              "user_attribute",
                                              "login2user",
                                              "groups_member",
                                              "oxfolder_tree",
                                              "oxfolder_permissions",
                                              "oxfolder_specialfolders",
                                              "oxfolder_userfolders_standardfolders",
                                              "del_oxfolder_tree",
                                              "del_oxfolder_permissions")) {
                dropForeignKeysFrom(table, con, logger);
            }

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
        }
    }

    private static void dropForeignKeysFrom(String table, Connection con, org.slf4j.Logger logger) throws OXException {
        if (tableExists(table, con)) {
            List<String> keyNames = allForeignKeysFrom(table, con);
            Statement stmt = null;
            for (String keyName : keyNames) {
                try {
                    stmt = con.createStatement();
                    stmt.execute("ALTER TABLE " + table + " DROP FOREIGN KEY " + keyName);
                    logger.info("Successfully dropped FOREIGN KEY `{}` from `{}` table", keyName, table);
                } catch (SQLException e) {
                    throw UpdateExceptionCodes.SQL_PROBLEM.create(e, "Failed to drop FOREIGN KEY `" + keyName + "` from table: " + table).markLightWeight();
                } finally {
                    Databases.closeSQLStuff(stmt);
                }
            }
        }
    }

    private static boolean tableExists(String table, Connection con) throws OXException {
        try {
            return Tools.tableExists(con, table);
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, "Failed to check existence of table: " + table).markLightWeight();
        }
    }

    private static List<String> allForeignKeysFrom(String table, Connection con) throws OXException {
        try {
            return Tools.allForeignKey(con, table);
        } catch (Exception e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, "Failed to obtain FOREIGN KEYs from table: " + table).markLightWeight();
        }
    }

}
