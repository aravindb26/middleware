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

package com.openexchange.deputy.impl.groupware;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedExceptionCode;
import com.openexchange.groupware.delete.DeleteListener;

/**
 * {@link DeputyStorageDeleteListener}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyStorageDeleteListener implements DeleteListener {

    /**
     * Initializes a new {@link DeputyStorageDeleteListener}.
     */
    public DeputyStorageDeleteListener() {
        super();
    }

    @Override
    public void deletePerformed(DeleteEvent event, Connection readCon, Connection writeCon) throws OXException {
        if (DeleteEvent.TYPE_USER == event.getType()) {
            deleteDeputyStorageForUser(event.getId(), event.getContext().getContextId(), writeCon);
        } else if (DeleteEvent.TYPE_CONTEXT == event.getType()) {
            deleteDeputyStorageForContext(event.getContext().getContextId(), writeCon);
        }
    }

    private void deleteDeputyStorageForContext(int contextId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("DELETE FROM deputy WHERE cid=?");
            stmt.setInt(1, contextId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;
        } catch (SQLException e) {
            throw DeleteFailedExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private void deleteDeputyStorageForUser(int userId, int contextId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("DELETE FROM deputy WHERE cid=? AND (user=? OR entity=?)");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;
        } catch (SQLException e) {
            throw DeleteFailedExceptionCode.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

}
