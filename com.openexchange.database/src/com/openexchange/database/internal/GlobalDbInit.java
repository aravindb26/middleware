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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.base.CharMatcher;
import com.openexchange.config.ConfigurationService;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DBPoolingExceptionCodes;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;

/**
 * {@link GlobalDbInit}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class GlobalDbInit {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GlobalDbInit.class);

    public static final String CONFIGFILE = "globaldb.yml";

    /**
     * Parses configuration settings for the global database from the configuration file <code>globaldb.yml</code> and performs initial
     * initialization steps, preparing the global database schemas and initial table layouts on demand.
     *
     * @param configService A reference to the configuration service
     * @param configDatabaseService The config database service
     * @param pools A reference to the connection pool
     * @param monitor The replication monitor
     * @return The global db configurations, mapped by their assigned group names, or an empty map if none are defined
     */
    public static Map<String, GlobalDbConfig> init(ConfigurationService configService, ConfigDatabaseService configDatabaseService, Pools pools, ReplicationMonitor monitor) throws OXException {
        /*
         * parse configs & read out associated connection settings
         */
        Map<String, GlobalDbConfig> configs = null;
        Object yaml = configService.getYaml(CONFIGFILE);

        if (null != yaml && (yaml instanceof Map)) {
            Map<String, Object> map = (Map<String, Object>) yaml;
            if (0 < map.size()) {
                configs = parse(map, configDatabaseService, configService);
            }
        }
        if (null == configs || 0 == configs.size()) {
            LOG.warn("No global database settings configured at \"{}\", global database features are not available.", CONFIGFILE);
            return Collections.emptyMap();
        }
        if (false == configs.containsKey(GlobalDbConfig.DEFAULT_GROUP)) {
            LOG.warn("No global database settings for group \"{}\" configured at \"{}\", no global fallback database available.", GlobalDbConfig.DEFAULT_GROUP, CONFIGFILE);
        }
        /*
         * ensure target schemas for global databases exist before returning the configs
         */
        prepare(pools, monitor, new HashSet<GlobalDbConfig>(configs.values()));
        return configs;
    }

    /**
     * Gets the set of globaldb pool identifiers
     *
     * @param configurationService The configuration service
     * @return A set of all gloabldb pool identifiers
     */
    static Set<Integer> getGlobalDBPoolIds(ConfigurationService configurationService) {
        Object yaml = configurationService.getYaml(CONFIGFILE);

        if (null != yaml && (yaml instanceof Map)) {
            Map<String, Object> map = (Map<String, Object>) yaml;
            if (!map.isEmpty()) {
                try {
                    return getGroupsByPool(map, configurationService).keySet();
                } catch (OXException e) {
                    LOG.error("Unable to get global db pool identifiers", e);
                }
            }
        }
        return Collections.emptySet();
    }

    /**
     * Prepares one or more global database by ensuring that the database schemas and the table layouts exist, creating the missing parts
     * dynamically as needed.
     *
     * @param pools A reference to the connection pool
     * @param monitor The replication monitor
     * @param globalDbConfigs The global database configurations to initialize
     */
    private static void prepare(Pools pools, ReplicationMonitor monitor, Collection<GlobalDbConfig> dbConfigs) throws OXException {
        if (null == dbConfigs || 0 == dbConfigs.size()) {
            return;
        }
        for (GlobalDbConfig dbConfig : dbConfigs) {
            /*
             * use a special assignment override that pretends a connection to the config database to prevent accessing a not yet existing
             * replication monitor
             */
            AssignmentImpl firstAssignment = new AssignmentImpl(dbConfig.getAssignment());
            Connection connection = null;
            boolean modified = false;

            int rollback = 0;
            try {
                connection = monitor.checkFallback(pools, firstAssignment, true, true, true);
                connection.setAutoCommit(false); // BEGIN
                rollback = 1;
                modified = prepare(connection, dbConfig.getSchema());
                connection.commit(); // COMMIT
                rollback = 2;
            } catch (SQLException e) {
                throw DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } finally {
                if (rollback > 0) {
                    if (rollback == 1) {
                        Databases.rollback(connection);
                    }
                    Databases.autocommit(connection);
                }

                if (null != connection) {
                    ConnectionState connectionState = new ConnectionState(!modified);
                    connectionState.setUsedForUpdate(modified);
                    monitor.backAndIncrementTransaction(pools, firstAssignment, connection, true, true, connectionState);
                }
            }
        }
    }

    /**
     * Initializes a global database by ensuring that the database schema exists, creating it as needed.
     *
     * @param connection The connection to use
     * @param schema The name of the database schema to prepare
     * @return <code>true</code> if the schema was newly created, <code>false</code> if it already existed
     */
    private static boolean prepare(Connection connection, String schema) throws SQLException {
        if (false == schemaExists(connection, schema) && 0 < createSchema(connection, schema)) {
            LOG.info("Created schema for global database: {}", schema);
            return true;
        }
        LOG.debug("Global database schema {} already exists.", schema);
        return false;
    }

    /**
     * Parses configuration settings for the global database from the supplied YAML map and reads out further connection settings from the
     * configuration database.
     *
     * @param yaml The global database configurations in a YAML map
     * @param configDatabaseService A reference to the configuration database service
     * @param configService A reference to the {@link ConfigurationService}
     * @return The global db configurations, mapped by their assigned group names, or an empty map if none are defined
     */
    private static Map<String, GlobalDbConfig> parse(Map<String, Object> yaml, ConfigDatabaseService configDatabaseService, ConfigurationService configService) throws OXException {
        /*
         * get context groups mapped to their database pool identifier
         */
        Map<Integer, Set<String>> groupsByPool = getGroupsByPool(yaml, configService);
        if (null == groupsByPool || 0 == groupsByPool.size()) {
            return Collections.emptyMap();
        }
        /*
         * read out database connection settings for each pool
         */
        Map<Integer, GlobalDbConfig> configsByPool = getConfigsByPool(configDatabaseService, groupsByPool.keySet());
        /*
         * check & map global database configs per group name
         */
        Map<String, GlobalDbConfig> dbConfigs = new HashMap<String, GlobalDbConfig>();
        for (Entry<Integer, Set<String>> entry : groupsByPool.entrySet()) {
            Integer poolID = entry.getKey();
            GlobalDbConfig dbConfig = configsByPool.get(poolID);
            if (null == dbConfig) {
                throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create("No database pool with identifier " + poolID + " found.");
            }
            for (String group : entry.getValue()) {
                if (null != dbConfigs.put(group, dbConfig)) {
                    throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create("Group " + group + " is assigned to more than one pool identifier");
                }
            }
        }
        return dbConfigs;
    }

    /**
     * Parses the pool identifiers and their associated context group names from the supplied YAML map and.
     *
     * @param yaml The global database configurations in a YAML map
     * @param configService A reference to the {@link ConfigurationService}
     * @return The configured database pool identifiers, mapped to their associated context group names, or an empty map if none are defined
     */
    private static Map<Integer, Set<String>> getGroupsByPool(Map<String, Object> yaml, ConfigurationService configService) throws OXException {
        Map<Integer, Set<String>> groupsByPool = new HashMap<Integer, Set<String>>();
        for (Map.Entry<String, Object> entry : yaml.entrySet()) {
            if (null == entry.getValue() || false == (entry.getValue() instanceof Map)) {
                throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create("Malformed configuration at section \"" + entry.getKey() + '"');
            }
            Map<String, Object> values = (Map<String, Object>) entry.getValue();
            Integer poolID;
            Object poolIDValue = values.get("id");
            if (null != poolIDValue) {
                /*
                 * parse pool identifier
                 */
                try {
                    poolID = Integer.valueOf(String.valueOf(values.get("id")));
                } catch (NumberFormatException e) {
                    throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create(e, "Database pool identifier can't be parsed for section \"" + entry.getKey() + '"');
                }
                Object groupsValue = values.get("groups");
                if (null == groupsValue || false == (groupsValue instanceof List)) {
                    throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create("Groups missing for section \"" + entry.getKey() + '"');
                }
                /*
                 * add mappings
                 */
                Set<String> groups = groupsByPool.get(poolID);
                if (null == groups) {
                    groups = new HashSet<String>();
                    groupsByPool.put(poolID, groups);
                }
                List<String> groupNames = (List<String>) groupsValue;
                int maxGroupNameLength = configService.getIntProperty("com.openexchange.database.contextgroup.nameLength", 32);
                checkGroupNameContraints(groupNames, maxGroupNameLength);

                groups.addAll(groupNames);
            } else {
                LOG.info("No pool identifier defined at section \"{}\", ignoring global database section", entry.getKey());
            }
        }
        return groupsByPool;
    }

    /**
     * Checks if the configured context group names do not exceed the configured size and only contain ASCII characters
     *
     * @param groupNames - List<String> with the used group names
     * @param maxGroupNameLength - int with the max allowed characters for one group
     * @throws OXException
     */
    private static void checkGroupNameContraints(List<String> groupNames, int maxGroupNameLength) throws OXException {
        for (String groupName : groupNames) {
            if (groupName.length() > maxGroupNameLength) {
                throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create("Group name \'" + groupName + "\' has more character than the allowed " + maxGroupNameLength + " one.");
            }
            if (!CharMatcher.ascii().matchesAllOf(groupName)) {
                throw DBPoolingExceptionCodes.INVALID_GLOBALDB_CONFIGURATION.create("Group name \'" + groupName + "\' does contain non ascii characters.");
            }
        }
    }

    /**
     * Reads out global database configuration parameters for one or more pool identifiers from the configuration database.
     *
     * @param configDatabaseService A reference to the config database service
     * @param poolIDs The pool identifiers to read out the database configuration parameters for
     * @return The database configuration parameters, mapped to their identifying write pool identifier
     */
    private static Map<Integer, GlobalDbConfig> getConfigsByPool(ConfigDatabaseService configDatabaseService, Collection<Integer> poolIDs) throws OXException {
        if (null == poolIDs || 0 == poolIDs.size()) {
            return Collections.emptyMap();
        }
        Connection connection = null;
        try {
            connection = configDatabaseService.getReadOnly();
            return selectConfigsByPool(connection, poolIDs);
        } catch (SQLException e) {
            throw DBPoolingExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.close(connection);
        }
    }

    /**
     * Reads out global database configuration parameters for one or more pool identifiers from the configuration database.
     *
     * @param connection A connection to the config database
     * @param poolIDs The pool identifiers to read out the database configuration parameters for
     * @return The database configuration parameters, mapped to their identifying write pool identifier
     */
    private static Map<Integer, GlobalDbConfig> selectConfigsByPool(Connection connection, Collection<Integer> poolIDs) throws SQLException {
        /*
         * build statement
         */
        StringBuilder stringBuilder = new StringBuilder()
            .append("SELECT db_pool.name, db_cluster.write_db_pool_id, db_cluster.read_db_pool_id ")
            .append("FROM db_pool LEFT JOIN db_cluster ON db_pool.db_pool_id=db_cluster.write_db_pool_id ")
            .append("WHERE db_pool.db_pool_id");
        if (1 == poolIDs.size()) {
            stringBuilder.append("=?;");
        } else {
            stringBuilder.append(" IN (?");
            for (int i = 1; i < poolIDs.size(); i++) {
                stringBuilder.append(",?");
            }
            stringBuilder.append(");");
        }
        /*
         * execute query & read out configs
         */
        Map<Integer, GlobalDbConfig> dbConfigs = new ConcurrentHashMap<Integer, GlobalDbConfig>(poolIDs.size(), 0.9f, 1);
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(stringBuilder.toString());
            int parameterIndex = 0;
            for (Integer poolID : poolIDs) {
                statement.setInt(++parameterIndex, poolID.intValue());
            }
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString(1);
                int writePoolId = resultSet.getInt(2);
                int readPoolId = resultSet.getInt(3);
                GlobalDbConfig config = new GlobalDbConfig(getSchemaName(name), 0 == readPoolId ? writePoolId : readPoolId, writePoolId);
                dbConfigs.put(Integer.valueOf(writePoolId), config);
            }
        } finally {
            Databases.closeSQLStuff(resultSet, statement);
        }
        return dbConfigs;
    }

    /**
     * Checks if a database schema exists.
     *
     * @param connection The connection to use
     * @param name The database schema name
     * @return <code>true</code> if the database schema exists, <code>false</code>, otherwise
     */
    private static boolean schemaExists(Connection connection, String name) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("SHOW DATABASES LIKE ?;");
            statement.setString(1, name);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } finally {
            Databases.closeSQLStuff(resultSet, statement);
        }
    }

    /**
     * Creates a new database schema.
     *
     * @param connection The connection to use
     * @param name The database schema name
     * @throws SQLException
     */
    private static int createSchema(Connection connection, String name) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("CREATE DATABASE IF NOT EXISTS `" + name + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            return statement.executeUpdate();
        } finally {
            Databases.closeSQLStuff(resultSet, statement);
        }
    }

    /**
     * Constructs the schema name for the global database based on the supplied database pool name.
     *
     * @param dbPoolName The database pool name to get the schema name for
     * @return The schema name
     */
    private static String getSchemaName(String dbPoolName) {
        return (dbPoolName + "_global").replace('`', '_');
    }

    /**
     * Initializes a new {@link GlobalDbInit}.
     */
    private GlobalDbInit() {
        super();
    }
}
