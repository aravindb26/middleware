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

package com.openexchange.database.internal;

import com.openexchange.database.ConfigDBMigrationService;
import com.openexchange.database.DatabaseExceptionCodes;
import com.openexchange.database.Databases;
import com.openexchange.database.migration.DBMigration;
import com.openexchange.database.migration.DBMigrationConnectionProvider;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.database.migration.DBMigrationState;
import com.openexchange.exception.OXException;
import liquibase.changelog.ChangeSet;
import liquibase.resource.ResourceAccessor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * {@link ConfigDBMigrationServiceImpl}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class ConfigDBMigrationServiceImpl implements ConfigDBMigrationService {

    private final ConfigDatabaseServiceImpl configDatabaseService;
    private final DBMigrationExecutorService migrationExecutorService;

    /**
     * Initializes a new {@link ConfigDBMigrationServiceImpl}.
     *
     * @param configDatabaseService The config database service
     * @param migrationExecutorService The migration executor service
     */
    public ConfigDBMigrationServiceImpl(ConfigDatabaseServiceImpl configDatabaseService, DBMigrationExecutorService migrationExecutorService) {
        super();
        this.configDatabaseService = configDatabaseService;
        this.migrationExecutorService = migrationExecutorService;
    }

    @Override
    public DBMigrationState scheduleMigrations(String changeSetLocation, ResourceAccessor resourceAccessor) throws OXException {
        return migrationExecutorService.scheduleDBMigration(getMigration(changeSetLocation, resourceAccessor));
    }

    @Override
    public List<ChangeSet> listUnrunDBChangeSets(String changeSetLocation, ResourceAccessor resourceAccessor) throws OXException {
        return migrationExecutorService.listUnrunDBChangeSets(getMigration(changeSetLocation, resourceAccessor));
    }

    /**
     * Returns the {@link DBMigration}
     *
     * @return the {@link DBMigration}
     * @throws OXException if getting the migration fails
     */
    private DBMigration getMigration(String changeSetLocation, ResourceAccessor resourceAccessor) throws OXException {
        // Get the connection/service to use
        String configDbSchemaName;
        {
            Connection con = configDatabaseService.getWritable();
            try {
                configDbSchemaName = con.getCatalog();
            } catch (SQLException e) {
                throw DatabaseExceptionCodes.SQL_ERROR.create(e);
            } finally {
                configDatabaseService.backWritableAfterReading(con);
            }
        }

        // Initialize connection provider
        DBMigrationConnectionProvider connectionProvider = new DBMigrationConnectionProvider() {

            @Override
            public Connection get() throws OXException {
                return configDatabaseService.getWritable();
            }

            @Override
            public void backAfterReading(Connection connection) {
                Databases.autocommit(connection);
                configDatabaseService.backWritableAfterReading(connection);
            }

            @Override
            public void back(Connection connection) {
                Databases.autocommit(connection);
                configDatabaseService.backWritable(connection);
            }
        };
        return new DBMigration(connectionProvider, changeSetLocation, resourceAccessor, configDbSchemaName);
    }
}
