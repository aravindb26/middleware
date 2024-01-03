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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.crypto.EncryptedData;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTaskAdapter;
import com.openexchange.java.Strings;
import com.openexchange.password.mechanism.PasswordDetails;
import com.openexchange.password.mechanism.PasswordMechRegistry;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.update.Tools;


/**
 * {@link RecryptGuestUserPasswords}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class RecryptGuestUserPasswords extends UpdateTaskAdapter {

    private static final String SQL_GET_ANONYMOUS_GUEST_USERS = "SELECT `cid`, `id`, `userPassword`  FROM user WHERE `guestCreatedBy` > 0 AND `mail` = '' FOR UPDATE";
    private static final String SQL_UPDATE_PASSWORDS = "UPDATE user SET `userPassword` = ?, `passwordMech` = ?, `salt` = ? WHERE `cid` = ? AND `id` = ?";

    // Copied from com.openexchange.share.core.ShareConstants.PASSWORD_MECH_ID to avoid dependency hell
    private static final String PASSWORD_MECH_ID = "{CRYPTO_SERVICE}";

    public RecryptGuestUserPasswords() {
        super();
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        ConfigurationService configService = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        if (null == configService) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(new Throwable(), "ConfigurationService unavailable, could not re-crpyt anonymous guest passwords.");
        }
        CryptoService cryptoService = ServerServiceRegistry.getInstance().getService(CryptoService.class);
        if (null == cryptoService) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(new Throwable(), "CryptoService unavailable, could not re-crpyt anonymous guest passwords.");
        }
        PasswordMechRegistry passwordMechRegistry = ServerServiceRegistry.getInstance().getService(PasswordMechRegistry.class);
        if (null == passwordMechRegistry) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(new Throwable(), "PasswordMechRegistry unavailable, could not re-crpyt anonymous guest passwords.");
        }
        String cryptKey = configService.getProperty("com.openexchange.share.cryptKey");

        int rollback = 0;
        Connection con = params.getConnection();
        PreparedStatement getStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;
        try {
            Databases.startTransaction(con);

            // Extend 'userPassword' column in user table
            rollback = 1;
            if (512 > Tools.getVarcharColumnSize("userPassword", "user", con)) {
                Tools.changeVarcharColumnSize("userPassword", 512, "user", con);
            }

            // Re-crypt passwords
            getStmt = con.prepareStatement(SQL_GET_ANONYMOUS_GUEST_USERS);
            updateStmt = con.prepareStatement(SQL_UPDATE_PASSWORDS);
            rs = getStmt.executeQuery();
            while (rs.next()) {
                int contextId = rs.getInt(1);
                int userId = rs.getInt(2);
                String encPassword = rs.getString(3);
                if (needsRecrypt(encPassword)) {
                    @SuppressWarnings("deprecation") String decryptedPassword = cryptoService.decrypt(new EncryptedData(encPassword, null), cryptKey, false);
                    PasswordDetails passwordDetails = passwordMechRegistry.get(PASSWORD_MECH_ID).encode(decryptedPassword);
                    updateStmt.setString(1, passwordDetails.getEncodedPassword());
                    updateStmt.setString(2, PASSWORD_MECH_ID);
                    updateStmt.setBytes(3, passwordDetails.getSalt());
                    updateStmt.setInt(4, contextId);
                    updateStmt.setInt(5, userId);
                    updateStmt.addBatch();
                }
            }
            rollback = 2;
            updateStmt.executeBatch();
            con.commit();
            rollback = 3;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.OTHER_PROBLEM.create(e, e.getMessage());
        } finally {
            if (0 < rollback) {
                if (3 > rollback) {
                    Databases.rollback(con);
                }
            }
            Databases.autocommit(con);
            Databases.closeSQLStuff(rs, getStmt, updateStmt);
        }

    }

    @Override
    public String[] getDependencies() {
        return new String[] { com.openexchange.groupware.update.tasks.ExtendUserFieldsTask.class.getName() };
    }

    private boolean needsRecrypt(String encryptedGuestPassword) {
        if (Strings.isNotEmpty(encryptedGuestPassword)) {
            return 1 == encryptedGuestPassword.split("\\$").length;
        }
        return false;
    }

}
