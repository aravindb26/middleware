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

package com.openexchange.groupware.tools.mappings.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import com.openexchange.java.util.UUIDs;

/**
 * {@link UUIDMapping} - Database mapping for <code>Types.BINARY</code>.
 *
 * @param <O> the type of the object
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class UUIDMapping<O> extends DefaultDbMapping<UUID, O> { // NOSONARLINT

    /**
     * Initializes a new {@link UUIDMapping}.
     *
     * @param columnName The name of the UUID binary column
     * @param readableName The readable name
     */
    protected UUIDMapping(final String columnName, final String readableName) {
        super(columnName, readableName, Types.BINARY);
    }

    @Override
    public UUID get(final ResultSet resultSet, String columnLabel) throws SQLException {
        byte[] bytes = resultSet.getBytes(columnLabel);
        return bytes == null ? null : UUIDs.toUUID(bytes);
    }

    @Override
    public int set(PreparedStatement statement, int parameterIndex, O object) throws SQLException {
        if (this.isSet(object)) {
            final UUID value = this.get(object);
            if (null != value) {
                statement.setBytes(parameterIndex, UUIDs.toByteArray(value));
            } else {
                statement.setNull(parameterIndex, this.getSqlType());
            }
        } else {
            statement.setNull(parameterIndex, this.getSqlType());
        }
        return 1;
    }

}
