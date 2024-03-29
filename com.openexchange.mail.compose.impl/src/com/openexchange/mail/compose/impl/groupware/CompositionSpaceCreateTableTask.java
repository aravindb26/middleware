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

package com.openexchange.mail.compose.impl.groupware;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.java.Strings;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link CompositionSpaceCreateTableTask}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class CompositionSpaceCreateTableTask extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link CompositionSpaceCreateTableTask}.
     */
    public CompositionSpaceCreateTableTask() {
        super();
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        int rollback = 0;
        try {
            con.setAutoCommit(false);
            rollback = 1;

            CompositionSpaceCreateTableService createTableService = CompositionSpaceCreateTableService.getInstance();
            String[] tableNames = createTableService.tablesToCreate();
            String[] createTableStatements = null;
            for (int i = 0; i < tableNames.length; i++) {
                if (!DBUtils.tableExists(con, tableNames[i])) {
                    PreparedStatement stmt = null;
                    try {
                        if (null == createTableStatements) {
                            createTableStatements = createTableService.getCreateStatements();
                        }
                        stmt = con.prepareStatement(createTableStatements[i]);
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
                    } finally {
                        Databases.closeSQLStuff(stmt);
                    }
                }
            }

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
        }
    }

    @Override
    public String[] getDependencies() {
        return Strings.getEmptyStrings();
    }

}
