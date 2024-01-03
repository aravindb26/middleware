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

package com.openexchange.datatypes.genericonf.storage.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link TX}
 *
 * @@author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @param <R> The result type
 */
public abstract class TX<R> {

    private final List<PreparedStatement> statements;
    private Connection connection;

    /**
     * Initializes a new {@link TX}.
     */
    protected TX() {
        super();
        statements = new LinkedList<PreparedStatement>();
    }

    /**
     * Performs this read-write operation.
     *
     * @return The result
     * @throws SQLException If an SQL error occurs
     */
    public abstract R perform() throws SQLException;

    /**
     * Closes this instance; e.g. closes all spawned instances of <code>PreparedStatement</code>.
     */
    public void close() {
        for (PreparedStatement stmt : statements) {
            try {
                stmt.close();
            } catch (SQLException x) {
                // IGNORE
            }
        }
        statements.clear();
    }

    /**
     * Sets the connection instance that should be used during read-write operation.
     *
     * @param connection The connection instance
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Gets the connection instance that should be used during read-write operation.
     *
     * @return The connection instance
     */
    protected Connection getConnection() {
        return connection;
    }

    /**
     * Yields a new instance of <code>PreparedStatement</code> for specified SQL statement.
     *
     * @param sql The SQL statement
     * @return The newly created instance of <code>PreparedStatement</code>
     * @throws SQLException If creation of <code>PreparedStatement</code> instance fails
     */
    protected PreparedStatement prepare(String sql) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        statements.add(stmt);
        return stmt;
    }
}
