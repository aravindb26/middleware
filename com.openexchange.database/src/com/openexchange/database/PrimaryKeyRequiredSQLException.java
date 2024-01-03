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

import java.sql.SQLException;

/**
 * {@link PrimaryKeyRequiredSQLException} - The special SQL exception signaling an attempt to pass an incorrect string to database.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PrimaryKeyRequiredSQLException extends SQLException {

    private static final long serialVersionUID = 3213082500383087281L;

    /** The (vendor) error code <code>1173</code> that signals a table type requires a primary key */
    public static final int ERROR_CODE = com.mysql.cj.exceptions.MysqlErrorNumbers.ER_REQUIRES_PRIMARY_KEY;

    public static final char UNKNOWN = '\ufffd';

    /**
     * Attempts to yield an appropriate {@code IncorrectStringSQLException} instance for specified SQL exception.
     *
     * @param e The SQL exception
     * @return The appropriate {@code IncorrectStringSQLException} instance or <code>null</code>
     */
    public static PrimaryKeyRequiredSQLException instanceFor(SQLException e) {
        if (null == e) {
            return null;
        }
        if (ERROR_CODE != e.getErrorCode()) {
            return null;
        }

        // E.g. "This table type requires a primary key"
        if (!"This table type requires a primary key".equals(e.getMessage())) {
            return null;
        }

        return new PrimaryKeyRequiredSQLException(e);
    }

    // ---------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link PrimaryKeyRequiredSQLException}.
     *
     * @param cause The associated SQL exception
     */
    public PrimaryKeyRequiredSQLException(SQLException cause) {
        super(cause);
    }

}
