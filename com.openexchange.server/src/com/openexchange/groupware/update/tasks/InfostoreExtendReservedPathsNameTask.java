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

import static com.openexchange.groupware.update.WorkingLevel.SCHEMA;
import static com.openexchange.tools.update.Tools.checkAndModifyColumns;
import java.sql.Connection;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.Attributes;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.TaskAttributes;
import com.openexchange.groupware.update.UpdateConcurrency;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.java.Strings;
import com.openexchange.tools.update.Column;

/**
 * {@link InfostoreExtendReservedPathsNameTask}
 *
 * Extends the size of the 'name' column in the 'infostoreReservedPaths' table.
 *
 * @author <a href="mailto:tobias.Friedrich@open-xchange.com">Tobias Friedruch</a>
 */
public final class InfostoreExtendReservedPathsNameTask extends UpdateTaskAdapter {

    /**
     * Default constructor.
     */
    public InfostoreExtendReservedPathsNameTask() {
        super();
    }

    @Override
    public String[] getDependencies() {
        return Strings.getEmptyStrings();
    }

    @Override
    public TaskAttributes getAttributes() {
        return new Attributes(UpdateConcurrency.BLOCKING, SCHEMA);
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InfostoreExtendReservedPathsNameTask.class);
        log.info("Performing update task {}", InfostoreExtendReservedPathsNameTask.class.getSimpleName());

        Connection connection = params.getConnection();
        int rollback = 0;
        try {
            connection.setAutoCommit(false);
            rollback = 1;
            checkAndModifyColumns(connection, "infostoreReservedPaths", new Column("name", "varchar(767)"));
            connection.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (Exception e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(connection);
                }
                Databases.autocommit(connection);
            }
        }
        log.info("{} successfully performed.", InfostoreExtendReservedPathsNameTask.class.getSimpleName());
    }

}
