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

package com.openexchange.database;

import static org.slf4j.LoggerFactory.getLogger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * {@link DatabaseConnectionListeners}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class DatabaseConnectionListeners {

    /**
     * Tries to add a callback routine that'll be invoked after the supplied database connection has been committed.
     *
     * @param connection The connection to add the callback routine for, or <code>null</code> for a no-op
     * @param callback The callback routine to add
     * @return <code>true</code> if the callback routine could be added, <code>false</code>, otherwise
     */
    public static boolean addAfterCommitCallback(Connection connection, Consumer<Connection> callback) {
        try {
            if (null == connection || false == Databases.isInTransaction(connection)) {
                return false;
            }
        } catch (SQLException e) {
            getLogger(DatabaseConnectionListeners.class).warn("", e);
            return false;
        }
        DatabaseConnectionListenerAnnotatable listenerAnnotatable = optDatabaseConnectionListenerAnnotatable(connection);
        if (null == listenerAnnotatable) {
            return false;
        }
        listenerAnnotatable.addListener(new AfterCommitDatabaseConnectionListener(callback));
        return true;
    }

    /**
     * Obtains a reference to the connection listener implemented by the supplied database connection if possible.
     *
     * @param connection The connection to get the connection listener annotatable for, or <code>null</code> for a no-op
     * @return A reference to the connection listener implemented by the supplied database connection, or <code>null</code> if not available
     */
    public static DatabaseConnectionListenerAnnotatable optDatabaseConnectionListenerAnnotatable(Connection connection) {
        if (null != connection) {
            if ((connection instanceof DatabaseConnectionListenerAnnotatable)) {
                return (DatabaseConnectionListenerAnnotatable) connection;
            }
            try {
                if (connection.isWrapperFor(DatabaseConnectionListenerAnnotatable.class)) {
                    return connection.unwrap(DatabaseConnectionListenerAnnotatable.class);
                }
            } catch (SQLException e) {
                getLogger(DatabaseConnectionListeners.class).warn("", e);
            }
        }
        return null;
    }

    /**
     * Initializes a new {@link DatabaseConnectionListeners}.
     */
    private DatabaseConnectionListeners() {
        super();
    }

}
