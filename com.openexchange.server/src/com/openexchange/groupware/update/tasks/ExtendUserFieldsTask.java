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
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.tools.update.Tools;


/**
 * {@link ExtendUserFieldsTask} - Aligns certain user fields spread over tables to have at least a common max. size of 191 characters.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ExtendUserFieldsTask extends UpdateTaskAdapter {

    /** The constant for the size of a <code>VARCHAR</code> column if it's part of a UNIQUE or PRIMARY KEY */
    private static final int UNIQUE_VARCHAR_SIZE = 191;

    /**
     * Initializes a new {@link ExtendUserFieldsTask}.
     */
    public ExtendUserFieldsTask() {
        super();
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        Connection con = params.getConnection();
        int rollback = 0;
        try {
            con.setAutoCommit(false);
            rollback = 1;

            doPerform(con);

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

    private void doPerform(Connection con) throws OXException {
        for (String tableName : new String[] {"user", "del_user"}) {
            int columnSize = Tools.getVarcharColumnSize("imapLogin", tableName, con);
            if (columnSize != 256) {
                Tools.changeVarcharColumnSize("imapLogin", 256, tableName, con);
            }
        }

        {
            String tableName = "login2user";
            int columnSize = Tools.getVarcharColumnSize("uid", tableName, con);
            if (columnSize != UNIQUE_VARCHAR_SIZE) {
                Tools.changeVarcharColumnSize("uid", UNIQUE_VARCHAR_SIZE, tableName, con);
            }
        }
    }

    @Override
    public String[] getDependencies() {
        return new String[] { com.openexchange.groupware.update.tasks.AddUserSaltColumnTask.class.getName() };
    }

}
