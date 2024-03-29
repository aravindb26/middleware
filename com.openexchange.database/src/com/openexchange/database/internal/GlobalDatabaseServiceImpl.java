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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.database.GlobalDatabaseService;
import com.openexchange.database.internal.wrapping.JDBC4ConnectionReturner;
import com.openexchange.database.migration.DBMigration;
import com.openexchange.database.migration.DBMigrationCallback;
import com.openexchange.database.migration.DBMigrationConnectionProvider;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.database.migration.DBMigrationState;
import com.openexchange.database.migration.resource.accessor.BundleResourceAccessor;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import liquibase.changelog.ChangeSet;

/**
 * {@link GlobalDatabaseServiceImpl}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class GlobalDatabaseServiceImpl implements GlobalDatabaseService {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GlobalDatabaseServiceImpl.class);
    private static final String GLOBALDB_CHANGE_LOG = "/liquibase/globaldbChangeLog.xml";

    final Pools pools;
    final ReplicationMonitor monitor;
    private Map<String, GlobalDbConfig> globalDbConfigs;
    private final ConfigViewFactory configViewFactory;
    private final ConfigDatabaseServiceImpl configDatabaseService;

    /**
     * Initializes a new {@link GlobalDatabaseServiceImpl}.
     *
     * @param pools A reference to the connection pool
     * @param monitor The replication monitor
     * @param configDatabaseService
     * @param configurationService
     * @param globalDbConfigs The known global database configurations
     * @param configViewFactory The config view factory
     * @throws OXException
     */
    public GlobalDatabaseServiceImpl(Pools pools, ReplicationMonitor monitor, ConfigurationService configurationService, ConfigDatabaseServiceImpl configDatabaseService, ConfigViewFactory configViewFactory) throws OXException {
        super();
        this.pools = pools;
        this.monitor = monitor;
        this.configViewFactory = configViewFactory;
        this.configDatabaseService = configDatabaseService;

        Map<String, GlobalDbConfig> loadGlobalDbConfigs = this.loadGlobalDbConfigs(configurationService);
        this.setGlobalDbConfigs(loadGlobalDbConfigs);
    }

    /**
     * Returns the up to date configuration for global databases from the globaldb.yml file. To apply the returned configuration you have to call com.openexchange.database.internal.GlobalDatabaseServiceImpl.setGlobalDbConfigs(Map<String,
     * GlobalDbConfig>)
     *
     * @param configurationService
     * @throws OXException
     */
    public Map<String, GlobalDbConfig> loadGlobalDbConfigs(ConfigurationService configurationService) throws OXException {
        Map<String, GlobalDbConfig> lGlobalDbConfigs = new ConcurrentHashMap<String, GlobalDbConfig>();

        if ((this.globalDbConfigs != null) && (!this.globalDbConfigs.isEmpty())) {
            lGlobalDbConfigs.putAll(this.globalDbConfigs);
        }

        Map<String, GlobalDbConfig> newGlobalDbConfigs = GlobalDbInit.init(configurationService, configDatabaseService, pools, monitor);
        lGlobalDbConfigs.putAll(newGlobalDbConfigs);

        for (String filename : new ArrayList<String>(lGlobalDbConfigs.keySet())) {
            if (!newGlobalDbConfigs.containsKey(filename)) {
                lGlobalDbConfigs.remove(filename);
            }
        }

        return lGlobalDbConfigs;
    }

    /**
     * Sets the globalDbConfigs
     *
     * @param globalDbConfigs The globalDbConfigs to set
     */
    public void setGlobalDbConfigs(Map<String, GlobalDbConfig> globalDbConfigs) {
        this.globalDbConfigs = globalDbConfigs;
    }

    /**
     * Schedules pending migrations for all known global databases.
     *
     * @param migrationService The database migration service
     * @return The scheduled migrations
     */
    public List<DBMigrationState> scheduleMigrations(DBMigrationExecutorService migrationService) throws OXException {
        return this.scheduleMigrations(migrationService, globalDbConfigs);
    }

    /**
     * Schedules pending migrations for all global databases provided within the {@link Map<String, GlobalDbConfig>} parameter.
     *
     * @param migrationService The database migration service
     * @param newGlobalDbConfigs The configuration to schedule migrations for
     *
     * @return The scheduled migrations
     */
    public List<DBMigrationState> scheduleMigrations(DBMigrationExecutorService migrationService, Map<String, GlobalDbConfig> newGlobalDbConfigs) throws OXException {
        if (null == newGlobalDbConfigs || 0 == newGlobalDbConfigs.size()) {
            return Collections.emptyList();
        }
        /*
         * use appropriate connection provider per global database & a local resource accessor for the changeset file
         */
        Bundle bundle = FrameworkUtil.getBundle(GlobalDbInit.class);
        if (bundle == null) {
            throw new OXException().setLogMessage("Class '" + GlobalDbInit.class.getName() + "' was loaded outside from OSGi!");
        }
        Set<GlobalDbConfig> dbConfigs = new HashSet<GlobalDbConfig>(newGlobalDbConfigs.values());
        List<DBMigrationState> migrationStates = new ArrayList<DBMigrationState>(dbConfigs.size());
        for (GlobalDbConfig dbConfig : dbConfigs) {
            /*
             * use a special assignment override that pretends a connection to the config database to prevent accessing a not yet existing
             * replication monitor
             */
            final AssignmentImpl assignment = dbConfig.getAssignment();
            /*
             * use a migration callback that fetches & returns a writable connection to trigger the replication monitor once after
             * migrations were executed
             */
            DBMigrationCallback migrationCallback = (executed, rolledBack) -> {
                if (null != executed && 0 < executed.size() || null != rolledBack && 0 < rolledBack.size()) {
                    Connection connection = null;
                    try {
                        connection = monitor.checkActualAndFallback(pools, assignment, true, true);
                    } catch (OXException e) {
                        LOG.warn("Unexpected error during migration callback", e);
                    } finally {
                        ConnectionState connectionState = new ConnectionState(false);
                        connectionState.setUsedForUpdate(true);
                        monitor.backAndIncrementTransaction(pools, assignment, connection, true, true, connectionState);
                    }
                }
            };
            /*
             * register utility MBean and schedule migration
             */
            DBMigration migration = getMigration(dbConfig);
            migrationService.register(migration);
            migrationStates.add(migrationService.scheduleDBMigration(migration, migrationCallback));
        }
        return migrationStates;
    }

    /**
     * Returns a map of the currently not executed change sets of the given changelog for the database per schema.
     *
     * @param migrationExecutorService The database migration service
     * @return Map with the currently not executed liquibase change sets
     * @throws OXException
     */
    public Map<String, List<ChangeSet>> listUnrunDBChangeSetsPerSchema(DBMigrationExecutorService migrationExecutorService) throws OXException {
        Map<String, List<ChangeSet>> changeSets = new HashMap<>();
        for (GlobalDbConfig globalDbConfig : globalDbConfigs.values()) {
            changeSets.put(globalDbConfig.getSchema(), listUnrunDBChangeSets(migrationExecutorService, globalDbConfig));
        }
        return changeSets;
    }

    /**
     * Returns a list of the currently not executed change sets of the given changelog for the database.
     *
     * @param migrationExecutorService The database migration service
     * @return List with the currently not executed liquibase change sets
     * @throws OXException
     */
    public List<ChangeSet> listUnrunDBChangeSets(DBMigrationExecutorService migrationExecutorService, GlobalDbConfig globalDbConfig) throws OXException {
        return migrationExecutorService.listUnrunDBChangeSets(getMigration(globalDbConfig));
    }

    /**
     * Returns the {@link DBMigration} for the specified {@link GlobalDbConfig}
     * 
     * @param dbConfig the {@link GlobalDbConfig}
     *
     * @return the {@link DBMigration}
     */
    private DBMigration getMigration(GlobalDbConfig dbConfig) throws OXException {
        /*
         * use appropriate connection provider per global database & a local resource accessor for the changeset file
         */
        Bundle bundle = FrameworkUtil.getBundle(GlobalDbInit.class);
        if (bundle == null) {
            throw new OXException().setLogMessage("Class '" + GlobalDbInit.class.getName() + "' was loaded outside from OSGi!");
        }
        BundleResourceAccessor localResourceAccessor = new BundleResourceAccessor(bundle);
        /*
         * use a special assignment override that pretends a connection to the config database to prevent accessing a not yet existing
         * replication monitor
         */
        final AssignmentImpl assignment = dbConfig.getAssignment();
        final AssignmentImpl firstAssignment = new AssignmentImpl(assignment);
        DBMigrationConnectionProvider connectionProvider = new DBMigrationConnectionProvider() {

            @Override
            public Connection get() throws OXException {
                return GlobalDatabaseServiceImpl.this.get(firstAssignment, true, true);
            }

            @Override
            public void back(Connection connection) {
                GlobalDatabaseServiceImpl.this.back(connection, false);
            }

            @Override
            public void backAfterReading(Connection connection) {
                GlobalDatabaseServiceImpl.this.back(connection, true);
            }
        };

        return new DBMigration(connectionProvider, GLOBALDB_CHANGE_LOG, localResourceAccessor, assignment.getSchema());
    }

    @Override
    public boolean isGlobalDatabaseAvailable() {
        return !globalDbConfigs.isEmpty();
    }

    @Override
    public boolean isGlobalDatabaseAvailable(String group) {
        String name = Strings.isEmpty(group) ? GlobalDbConfig.DEFAULT_GROUP : group;
        return globalDbConfigs.containsKey(name);
    }

    @Override
    public boolean isGlobalDatabaseAvailable(int contextId) throws OXException {
        String group = configViewFactory.getView(-1, contextId).opt("com.openexchange.context.group", String.class, null);
        return isGlobalDatabaseAvailable(group);
    }

    @Override
    public Set<String> getDistinctGroupsPerSchema() {
        Set<String> addedSchemas = new HashSet<String>();
        Set<String> distinctGroupsPerSchema = new HashSet<String>();
        for (Entry<String, GlobalDbConfig> entry : globalDbConfigs.entrySet()) {
            if (addedSchemas.add(entry.getValue().getSchema())) {
                distinctGroupsPerSchema.add(entry.getKey());
            }
        }
        return distinctGroupsPerSchema;
    }

    @Override
    public Connection getReadOnlyForGlobal(String group) throws OXException {
        return get(getAssignment(group), false, false);
    }

    @Override
    public Connection getReadOnlyForGlobal(int contextId) throws OXException {
        return get(getAssignment(contextId), false, false);
    }

    @Override
    public void backReadOnlyForGlobal(String group, Connection connection) {
        back(connection, true);
    }

    @Override
    public void backReadOnlyForGlobal(int contextId, Connection connection) {
        back(connection, true);
    }

    @Override
    public Connection getWritableForGlobal(String group) throws OXException {
        return get(getAssignment(group), true, false);
    }

    @Override
    public Connection getWritableForGlobal(int contextId) throws OXException {
        return get(getAssignment(contextId), true, false);
    }

    @Override
    public void backWritableForGlobal(String group, Connection connection) {
        back(connection, false);
    }

    @Override
    public void backWritableForGlobal(int contextId, Connection connection) {
        back(connection, false);
    }

    @Override
    public void backWritableForGlobalAfterReading(String group, Connection connection) {
        back(connection, true);
    }

    @Override
    public void backWritableForGlobalAfterReading(int contextId, Connection connection) {
        back(connection, true);
    }

    AssignmentImpl getAssignment(String group) throws OXException {
        String name = Strings.isEmpty(group) ? GlobalDbConfig.DEFAULT_GROUP : group;
        GlobalDbConfig dbConfig = globalDbConfigs.get(name);
        if (null == dbConfig) {
            // TODO: fall back to "default" also in that case?
            throw DBPoolingExceptionCodes.NO_GLOBALDB_CONFIG_FOR_GROUP.create(group);
        }
        return dbConfig.getAssignment();
    }

    private AssignmentImpl getAssignment(int contextId) throws OXException {
        String group = configViewFactory.getView(-1, contextId).opt("com.openexchange.context.group", String.class, null);
        return getAssignment(group);
    }

    Connection get(AssignmentImpl assignment, boolean write, boolean noTimeout) throws OXException {
        return monitor.checkActualAndFallback(pools, assignment, noTimeout, write);
    }

    void back(Connection connection, boolean usedAsRead) {
        if (null == connection) {
            LOG.error("", DBPoolingExceptionCodes.NULL_CONNECTION.create());
            return;
        }
        try {
            if (usedAsRead && (connection instanceof JDBC4ConnectionReturner)) {
                // Not the nice way to tell the replication monitor not to increment the counter.
                ((JDBC4ConnectionReturner) connection).setUsedAsRead(true);
            }
            connection.close();
        } catch (SQLException e) {
            LOG.error("", DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage()));
        }
    }

}
