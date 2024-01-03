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

package com.openexchange.pop3.storage.mailaccount.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.mailaccount.MailAccountDeleteListener;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.pop3.POP3Access;
import com.openexchange.pop3.POP3Provider;
import com.openexchange.pop3.services.POP3ServiceRegistry;
import com.openexchange.pop3.storage.POP3Storage;
import com.openexchange.pop3.storage.mailaccount.RdbPOP3StorageProperties;
import com.openexchange.pop3.storage.mailaccount.RdbPOP3StorageTrashContainer;
import com.openexchange.pop3.storage.mailaccount.RdbPOP3StorageUIDLMap;
import com.openexchange.pop3.storage.mailaccount.SessionParameterNames;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.tools.sql.DBUtils;

/**
 * {@link StorageDeleteListener} - Delete listener for mail account POP3 storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class StorageDeleteListener implements MailAccountDeleteListener {

    /**
     * Initializes a new {@link StorageDeleteListener}.
     */
    public StorageDeleteListener() {
        super();
    }

    @Override
    public void onAfterMailAccountDeletion(final int id, final Map<String, Object> eventProps, final int user, final int cid, final Connection con) throws OXException {
        // Nothing to do
    }

    @Override
    public void onBeforeMailAccountDeletion(final int id, final Map<String, Object> eventProps, final int user, final int cid, final Connection con) throws OXException {
        /*
         * Check if account denotes a POP3 account
         */
        final String url;
        {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = con.prepareStatement("SELECT url FROM user_mail_account WHERE cid = ? AND id = ? AND user = ?");
                stmt.setLong(1, cid);
                stmt.setLong(2, id);
                stmt.setLong(3, user);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    /*
                     * Strange...
                     */
                    return;
                }
                url = rs.getString(1).toLowerCase(Locale.ENGLISH);
            } catch (SQLException e) {
                throw MailAccountExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } finally {
                Databases.closeSQLStuff(rs, stmt);
            }
        }
        if (!url.startsWith(POP3Provider.PROTOCOL_POP3.getName())) {
            /*
             * Not a POP3 account...
             */
            return;
        }
        /*
         * Delete storage content for user if a valid session can be found
         */
        SessiondService sessiondService = POP3ServiceRegistry.getServiceRegistry().getService(SessiondService.class);
        if (null != sessiondService) {
            boolean dropped = false;
            for (Session session : sessiondService.getSessions(user, cid)) {
                if (!dropped){
                    final POP3Access pop3Access = POP3Access.newInstance(session, id);
                    final POP3Storage pop3Storage = pop3Access.getPOP3Storage();
                    pop3Storage.drop();
                    dropped = true;
                }

                String key = SessionParameterNames.getStorageProperties(id);
                session.setParameter(key, null);
                key = SessionParameterNames.getTrashContainer(id);
                session.setParameter(key, null);
                key = SessionParameterNames.getUIDLMap(cid);
                session.setParameter(key, null);
            }
        }
        /*
         * Drop database entries
         */
        final boolean restoreConstraints = disableForeignKeyChecks(con);
        try {
            RdbPOP3StorageProperties.dropProperties(id, user, cid, con);
            RdbPOP3StorageTrashContainer.dropTrash(id, user, cid, con);
            RdbPOP3StorageUIDLMap.dropIDs(id, user, cid, con);
        } finally {
            if (restoreConstraints) {
                try {
                    enableForeignKeyChecks(con);
                } catch (SQLException e) {
                    org.slf4j.LoggerFactory.getLogger(StorageDeleteListener.class).error("", e);
                }
            }
        }
    }

    private static boolean disableForeignKeyChecks(final Connection con) {
        if (null == con) {
            return false;
        }
        try {
            DBUtils.disableMysqlForeignKeyChecks(con);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void enableForeignKeyChecks(final Connection con) throws SQLException {
        if (null == con) {
            return;
        }
        DBUtils.enableMysqlForeignKeyChecks(con);
    }

}