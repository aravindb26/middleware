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

package com.openexchange.database.internal;

import static com.openexchange.java.Autoboxing.L;
import java.sql.Connection;
import java.sql.SQLException;
import com.mysql.cj.NativeSession;
import com.mysql.cj.jdbc.ConnectionImpl;

/**
 * {@link MysqlUtils}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class MysqlUtils {

    /**
     * Initializes a new {@link MysqlUtils}.
     */
    private MysqlUtils() {
        super();
    }

    /**
     * Gets the idle time of the given pool object
     *
     * @param o A pool object
     * @return The idle time in milliseconds or null
     */
    public static Long getIdleTime(Object o) {
        if (o instanceof ConnectionImpl) {
            NativeSession session = ((ConnectionImpl) o).getSession();
            return session == null ? null : L(session.getIdleFor());
        }
        return null;
    }


    /** The closed state for a connection */
    public static enum ClosedState {
        /** Connection appears to be open */
        OPEN,
        /** Connection has been explicitly closed since {@link Connection#isClosed()} signaled <code>true</code> */
        EXPLICITLY_CLOSED,
        /** Connection seems to be internally closed; meaning necessary resources were closed rendering connection unusable */
        INTERNALLY_CLOSED;
    }

    /**
     * Checks whether specified connection appears to be closed. This is connection has been explicitly closed or lost its internal network resources.
     *
     * @param con The connection to check
     * @return The determined closed status for specified connection
     * @throws SQLException If closed status cannot be returned
     */
    public static ClosedState isClosed(Connection con, boolean closeOnInternallyClosed) throws SQLException {
        if (con.isClosed()) {
            return ClosedState.EXPLICITLY_CLOSED;
        }

        if (isInternallyClosed(con, closeOnInternallyClosed)) {
            return ClosedState.INTERNALLY_CLOSED;
        }

        return ClosedState.OPEN;
    }

    /**
     * Checks whether specified connection appears to be internally closed. This is connection lost its internal network resources.
     *
     * @param con The connection to check
     * @param closeOnInternallyClosed Whether to perform an explicit close in case considered as internally closed
     * @return The determined closed status for specified connection
     */
    public static boolean isInternallyClosed(Connection con, boolean closeOnInternallyClosed) {
        if (con instanceof com.mysql.cj.jdbc.ConnectionImpl) {
            com.mysql.cj.jdbc.ConnectionImpl mysqlConnectionImpl = (com.mysql.cj.jdbc.ConnectionImpl) con;
            if (seemsClosed(mysqlConnectionImpl)) {
                if (closeOnInternallyClosed) {
                    closeSafe(mysqlConnectionImpl);
                }
                return true;
            }
        }

        return false;
    }

    private static boolean seemsClosed(com.mysql.cj.jdbc.ConnectionImpl mysqlConnectionImpl) {
        return mysqlConnectionImpl.getSession() == null ? true : mysqlConnectionImpl.getSession().isClosed();
    }

    private static void closeSafe(com.mysql.cj.jdbc.ConnectionImpl mysqlConnection) {
        if (null != mysqlConnection) {
            try {
                mysqlConnection.realClose(false, false, false, null);
            } catch (Exception e) {
                // ignore, we're going away.
            }
        }
    }

    /**
     * Code is correct and will not leave a connection in CLOSED_WAIT state. See CloseWaitTest.java.
     */
    public static void close(Connection con) {
        if (null != con) {
            try {
                con.close();
            } catch (Exception e) {
                // ignore, we're going away.
            }
        }
    }

    /**
     * Gets the connection identifier.
     *
     * @param con The connection
     * @return The connection identifier or <code>0</code> (zero)
     */
    public static long getConnectionId(Connection con) {
        return (con instanceof com.mysql.cj.MysqlConnection) ? ((com.mysql.cj.MysqlConnection) con).getId() : 0L;
    }

}
