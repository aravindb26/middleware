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

package com.openexchange.oauth.impl.internal.groupware;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.oauth.KnownApi;

/**
 * {@link RemoveTwitterAccountsUpdateTask} - Removes twitter stuff from the
 * <code>oauthAccounts</code>, <code>genconf_attributes_*</code> and <code>messagingAccount</code> tables.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class RemoveTwitterAccountsUpdateTask extends AbstractOAuthUpdateTask {

    private static final String SERVICE_ID = "com.openexchange.messaging.twitter";
    private static final int MAX_BATCH = 50;

    /**
     * Initialises a new {@link RemoveTwitterAccountsUpdateTask}.
     */
    public RemoveTwitterAccountsUpdateTask() {
        super();
    }

    @Override
    void innerPerform(Connection connection, PerformParameters performParameters) throws OXException, SQLException {
        deleteFromOAuthAccounts(connection);
        deleteFromGenConf(connection);
        deleteFromMessagingAccount(connection);
    }

    /**
     * Deletes twitter data from the <code>oauthAccounts</code> table
     *
     * @param connection The connection to use
     * @throws SQLException if an SQL error is occurred
     */
    private void deleteFromOAuthAccounts(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM oauthAccounts WHERE displayName=? OR serviceId=?")) {
            stmt.setString(1, KnownApi.TWITTER.getDisplayName());
            stmt.setString(2, KnownApi.TWITTER.getServiceId());
            stmt.execute();
        }
    }

    /**
     * Deletes twitter data from the <code>genconf_attributes_strings</code> and <code>genconf_attributes_bools</code> table
     *
     * @param connection The connection to use
     * @throws SQLException if an SQL error is occurred
     */
    private void deleteFromGenConf(Connection connection) throws SQLException {
        boolean executed = false;
        try (PreparedStatement stmt = connection.prepareStatement("SELECT cid, confId FROM messagingAccount WHERE serviceId=?");  //
             PreparedStatement sStrings = connection.prepareStatement("DELETE FROM genconf_attributes_strings WHERE cid=? AND id=?"); //
             PreparedStatement sBools = connection.prepareStatement("DELETE FROM genconf_attributes_bools WHERE cid=? AND id=?");) {
            stmt.setString(1, SERVICE_ID);
            try (ResultSet rs = stmt.executeQuery()) {
                int i = 0;
                while (rs.next()) {
                    int cid = rs.getInt(1);
                    int confId = rs.getInt(2);

                    sStrings.setInt(1, cid);
                    sStrings.setInt(2, confId);
                    sStrings.addBatch();

                    sBools.setInt(1, cid);
                    sBools.setInt(2, confId);
                    sBools.addBatch();

                    executed = false;

                    if (++i >= MAX_BATCH) {
                        sStrings.executeBatch();
                        sBools.executeBatch();
                        executed = true;
                        i = 0;
                    }
                }
                if (!executed) {
                    sStrings.executeBatch();
                    sBools.executeBatch();
                }
            }
        }
    }

    /**
     * Deletes twitter data from the <code>messagingAccount</code> table
     *
     * @param connection The connection to use
     * @throws SQLException if an SQL error is occurred
     */
    private void deleteFromMessagingAccount(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM messagingAccount WHERE displayName=? OR serviceId=?")) {
            stmt.setString(1, SERVICE_ID);
            stmt.setString(2, SERVICE_ID);
            stmt.execute();
        }
    }

    @Override
    public String[] getDependencies() {
        return new String[] { OAuthCreateTableTask2.class.getName() };
    }
}
