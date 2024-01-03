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
package com.openexchange.objectusecount.cleanup;

import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.Databases;
import com.openexchange.database.cleanup.CleanUpExecution;
import com.openexchange.database.cleanup.CleanUpExecutionConnectionProvider;
import com.openexchange.exception.OXException;

/**
 * {@link GenericUseCountCleanUpExecution}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
public class GenericUseCountCleanUpExecution implements CleanUpExecution {

    private static final Logger LOG = LoggerFactory.getLogger(GenericUseCountCleanUpExecution.class);

    private final static String SQL_DECREMENT = "UPDATE `generic_use_count` SET `lastModified` = ?, `value` = `value` - 1 WHERE `lastModified` < ? AND `value` > 0";
    private final static String SQL_PURGE = "DELETE FROM `generic_use_count` WHERE `value` = 0";

    private final long timespan;

    public GenericUseCountCleanUpExecution(long timespan) {
        super();
        this.timespan = timespan;
    }

    @Override
    public boolean isApplicableFor(String schema, int representativeContextId, int databasePoolId, Map<String, Object> state, CleanUpExecutionConnectionProvider connectionProvider) throws OXException {
        // check if `generic_use_count` table exists
        try {
            return Databases.tableExists(connectionProvider.getConnection(), "generic_use_count");
        } catch (SQLException e) {
            LOG.warn("Could not check for table generic_use_count.", e);
        }
        return false;
    }

    @Override
    public void executeFor(String schema, int representativeContextId, int databasePoolId, Map<String, Object> state, CleanUpExecutionConnectionProvider connectionProvider) throws OXException {
        long now = System.currentTimeMillis();
        try {
            int updatedRows = Databases.executeUpdate(connectionProvider.getConnection(), SQL_DECREMENT, s -> s.setLong(1, now), s -> s.setLong(2, now - timespan));
            if (0 < updatedRows) {
                Databases.executeUpdate(connectionProvider.getConnection(), SQL_PURGE);
            }
        } catch (SQLException e) {
            LOG.warn("Error cleaning up generic_use_count table", e);
        }
    }

}
