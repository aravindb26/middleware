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

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Strings.getEmptyStrings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateConcurrency;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.tools.update.Tools;

/**
 * {@link EnsureExistenceOfGenericUseCountTask} - Shrinks length of "folder" and "object" VARCHAR columns from 255 to 191 to be used in
 * UNIQUE KEY.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class EnsureExistenceOfGenericUseCountTask extends UpdateTaskAdapter {

    /**
     * Initializes a new {@link EnsureExistenceOfGenericUseCountTask}.
     */
    public EnsureExistenceOfGenericUseCountTask() {
        super();
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(UpdateConcurrency.BLOCKING);
    }

    @Override
    public String[] getDependencies() {
        return getEmptyStrings();
    }

    @Override
    public void perform(final PerformParameters params) throws OXException {
        Connection con = params.getConnection();

        PreparedStatement stmt = null;
        try {
            if (Tools.tableExists(con, "generic_use_count")) {
                // Table does already exist
                return;
            }

            stmt = con.prepareStatement(new CreateGenericUseCountTableService().getCreateStatements()[0]);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            closeSQLStuff(stmt);
        }
    }

}
