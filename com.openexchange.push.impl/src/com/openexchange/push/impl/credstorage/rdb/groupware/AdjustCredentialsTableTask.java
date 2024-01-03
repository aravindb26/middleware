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
package com.openexchange.push.impl.credstorage.rdb.groupware;

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
import com.openexchange.tools.update.Tools;


/**
 * {@link AdjustCredentialsTableTask}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class AdjustCredentialsTableTask extends UpdateTaskAdapter {

    private static final String SQL_GET_CREDENTIALS = "SELECT `cid`, `user`, `password` FROM `credentials` FOR UPDATE";
    private static final String SQL_UPDATE_CREDENTIALS = "UPDATE `credentials` SET `password` = ? WHERE `cid` = ? AND `user` = ?";

    private final ConfigurationService configService;
    private final CryptoService cryptoService;

    public AdjustCredentialsTableTask(ConfigurationService configService, CryptoService cryptoService) {
        super();
        this.configService = configService;
        this.cryptoService = cryptoService;
    }

    @Override
    public void perform(PerformParameters params) throws OXException {
        String cryptKey = configService.getProperty("com.openexchange.push.credstorage.passcrypt");

        Connection con = params.getConnection();
        PreparedStatement getStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;
        int rollback = 0;
        try {
            Databases.startTransaction(con);

            rollback = 1;
            if (512 > Tools.getVarcharColumnSize("password", "credentials", con)) {
                Tools.exec(con, "ALTER TABLE `credentials` MODIFY COLUMN `password` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL");
            }

            getStmt = con.prepareStatement(SQL_GET_CREDENTIALS);
            updateStmt = con.prepareStatement(SQL_UPDATE_CREDENTIALS);
            rs = getStmt.executeQuery();
            while (rs.next()) {
                int contextId = rs.getInt(1);
                int userId = rs.getInt(2);
                String encPassword = rs.getString(3);
                if (needsRecrypt(encPassword)) {
                    @SuppressWarnings("deprecation") String decryptedPassword = cryptoService.decrypt(new EncryptedData(encPassword, null), cryptKey, false);
                    String recrypted = cryptoService.encrypt(decryptedPassword, cryptKey);
                    updateStmt.setString(1, recrypted);
                    updateStmt.setInt(2, contextId);
                    updateStmt.setInt(3, userId);
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
        return new String[] { new CredConvertUtf8ToUtf8mb4Task().getClass().getName() };
    }

    private boolean needsRecrypt(String encryptedGuestPassword) {
        if (Strings.isNotEmpty(encryptedGuestPassword)) {
            return 1 == encryptedGuestPassword.split("\\$").length;
        }
        return false;
    }

}
