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

package com.openexchange.database.internal.change.custom;

import static com.openexchange.database.Databases.columnExists;
import static com.openexchange.database.Databases.isNullable;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.openexchange.database.Databases;
import com.openexchange.java.util.UUIDs;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * {@link ServerAddUuidColumnCustomTaskChange}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ServerAddUuidColumnCustomTaskChange implements CustomTaskChange {

    /**
     * Initializes a new {@link ServerAddUuidColumnCustomTaskChange}.
     */
    public ServerAddUuidColumnCustomTaskChange() {
        super();
    }

    @Override
    public String getConfirmationMessage() {
        return "Column \"uuid\" successfully added for table \"server\"";
    }

    @Override
    public void setUp() throws SetupException {
        // Nothing
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // Ignore
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        DatabaseConnection databaseConnection = database.getConnection();
        if (!(databaseConnection instanceof JdbcConnection)) {
            throw new CustomChangeException("Cannot get underlying connection because database connection is not of type " + JdbcConnection.class.getName() + ", but of type: " + databaseConnection.getClass().getName());
        }

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerAddUuidColumnCustomTaskChange.class);
        Connection configDbCon = ((JdbcConnection) databaseConnection).getUnderlyingConnection();
        int rollback = 0;
        try {
            /*
             * check if column exists & is already declared as NOT NULL
             */
            boolean columnExists = columnExists(configDbCon, "server", "uuid");
            boolean isNullable = columnExists && isNullable(configDbCon, "server", "uuid");
            if (columnExists && false == isNullable) {
                logger.info("Column \"uuid\" already exists, nothing to do.");
                return;
            }
            Databases.startTransaction(configDbCon);
            rollback = 1;
            /*
             * add uuid column if missing, defaulting to NULL
             */
            if (false == columnExists) {
                addOrModifyUuidColumn(configDbCon, "BINARY(16) DEFAULT NULL", false);
            }
            /*
             * populate existing / empty rows with a value for the new column
             */
            insertRandomUUIDs(configDbCon, getServerIdsWithoutUUID(configDbCon));
            /*
             * modify uuid column, making it NOT NULL
             */
            addOrModifyUuidColumn(configDbCon, "BINARY(16) NOT NULL", true);
            /*
             * commit
             */
            configDbCon.commit();
            rollback = 2;
            logger.info("Successfully added column \"uuid\" to table \"server\"");
        } catch (SQLException e) {
            logger.error("Failed to add column \"uuid\" to table \"server\"", e);
            throw new CustomChangeException("SQL error", e);
        } catch (RuntimeException e) {
            logger.error("Failed to add column \"uuid\" to table \"server\"", e);
            throw new CustomChangeException("Runtime error", e);
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(configDbCon);
                }
                Databases.autocommit(configDbCon);
            }
        }
    }

    private void insertRandomUUIDs(Connection connection, List<Integer> serverIds) throws SQLException {
        if (null == serverIds || serverIds.isEmpty()) {
            return;
        }
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE `server` SET `uuid`=? WHERE `server_id`=?;")) {
            for (Integer serverId : serverIds) {
                stmt.setBytes(1, UUIDs.toByteArray(UUID.randomUUID()));
                stmt.setInt(2, i(serverId));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private static List<Integer> getServerIdsWithoutUUID(Connection connection) throws SQLException {
        List<Integer> serverIds = new ArrayList<Integer>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT `server_id` FROM `server` WHERE `uuid` IS NULL;"); 
            ResultSet resultSet = stmt.executeQuery()) {
            while (resultSet.next()) {
                serverIds.add(I(resultSet.getInt(1)));
            }
        }
        return serverIds;
    }

    private static void addOrModifyUuidColumn(Connection connection, String columnDefinition, boolean modify) throws SQLException {
        String sql = new StringBuilder().append("ALTER TABLE `server` ").append(modify ? "MODIFY" : "ADD").append(" `uuid` ").append(columnDefinition).toString();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
}
