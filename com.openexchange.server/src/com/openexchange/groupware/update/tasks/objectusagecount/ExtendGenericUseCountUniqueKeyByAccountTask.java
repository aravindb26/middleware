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

package com.openexchange.groupware.update.tasks.objectusagecount;

import java.sql.Connection;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.tools.update.Tools;

/**
 * {@link ExtendGenericUseCountUniqueKeyByAccountTask}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ExtendGenericUseCountUniqueKeyByAccountTask extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link ExtendGenericUseCountUniqueKeyByAccountTask}.
     */
    public ExtendGenericUseCountUniqueKeyByAccountTask() {
        super();
    }

    @Override
    public String[] getDependencies() {
        return new String[] { GenericUseCountShrinkFolderAndObjectTask.class.getName() };
    }

    @Override
    public void perform(PerformParameters params) throws OXException {

        Connection con = params.getConnection();
        int rollback = 0;
        try {
            String table = "generic_use_count";
            if (Tools.tableExists(con, table) == false) {
                // No such table
                return;
            }

            String[] oldColumns = new String[] { "cid", "user", "module", "folder", "object" };
            String[] newColumns = new String[] { "cid", "user", "module", "account", "folder", "object" };

            // Check existence
            String oldIndex = Tools.existsIndex(con, table, oldColumns);
            String newIndex = Tools.existsIndex(con, table, newColumns);

            if (null == oldIndex && null != newIndex) {
                // Old unique key does no more exist and new unique key does exist: nothing to do
                return;
            }

            Databases.startTransaction(con);
            rollback = 1;

            // Drop & re-create unique key (if necessary)
            if (oldIndex != null) {
                Tools.dropIndex(con, table, oldIndex);
            }
            if (newIndex == null) {
                Tools.createIndex(con, table, "usercount", newColumns, true);
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

}
