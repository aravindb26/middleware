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

package com.openexchange.oauth.provider.impl.groupware;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.oauth.provider.exceptions.OAuthProviderExceptionCodes;

/**
 * {@link AuthCodeCreateTableTask}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AuthCodeCreateTableTask extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link AuthCodeCreateTableTask}.
     */
    public AuthCodeCreateTableTask() {
        super();
    }

    @Override
    public void perform(final PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        int rollback = 0;
        try {
            con.setAutoCommit(false);
            rollback = 1;

            perform(con);

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback==1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
        }
    }

    private void perform(Connection con) throws OXException {
        PreparedStatement stmt = null;
        try {
            String[] tableNames = AuthCodeCreateTableService.getTablesToCreate();
            String[] createStmts = AuthCodeCreateTableService.getCreateStmts();
            for (int i = 0; i < tableNames.length; i++) {
                if (tableExists(con, tableNames[i])) {
                    continue;
                }
                stmt = con.prepareStatement(createStmts[i]);
                stmt.executeUpdate();
                stmt.close();
                stmt = null;
            }
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw OAuthProviderExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public String[] getDependencies() {
        return new String[] {};
    }

    private static final boolean tableExists(final Connection con, final String table) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        ResultSet rs = null;
        try {
            rs = metaData.getTables(con.getCatalog(), null, table, new String[] { "TABLE" });
            return (rs.next() && rs.getString("TABLE_NAME").equalsIgnoreCase(table));
        } finally {
            Databases.closeSQLStuff(rs);
        }
    }

}
