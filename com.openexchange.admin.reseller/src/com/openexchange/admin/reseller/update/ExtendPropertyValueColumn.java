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

package com.openexchange.admin.reseller.update;

import java.sql.Connection;
import java.sql.SQLException;
import com.openexchange.database.Databases;
import com.openexchange.tools.update.Column;
import com.openexchange.tools.update.Tools;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 *
 * {@link ExtendPropertyValueColumn}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ExtendPropertyValueColumn implements CustomTaskChange {

    /**
     * Initializes a new {@link ExtendPropertyValueColumn}.
     */
    public ExtendPropertyValueColumn() {
        super();
    }

    @Override
    public String getConfirmationMessage() {
        return "Column \"propertyValue\" of table \"subadmin_config_properties\" successfully extended to TEXT";
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

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExtendPropertyValueColumn.class);
        Connection configDbCon = ((JdbcConnection) databaseConnection).getUnderlyingConnection();
        int rollback = 0;
        try {
            if (!Tools.columnExists(configDbCon, "subadmin_config_properties", "propertyValue")) {
                return;
            }

            if ("TEXT".equalsIgnoreCase(Tools.getColumnTypeName(configDbCon, "subadmin_config_properties", "propertyValue"))) {
                // Already set to TEXT
                return;
            }

            configDbCon.setAutoCommit(false);
            rollback = 1;

            Tools.checkAndModifyColumns(configDbCon, "subadmin_config_properties", new Column("propertyValue", "TEXT CHARACTER SET latin1 NOT NULL DEFAULT ''"));

            configDbCon.commit();
            rollback = 2;
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

}
