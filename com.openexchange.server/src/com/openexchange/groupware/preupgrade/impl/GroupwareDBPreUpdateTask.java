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

package com.openexchange.groupware.preupgrade.impl;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.database.SchemaInfo;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.ExtendedUpdateTaskService;
import com.openexchange.groupware.update.TaskFailure;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.java.Strings;
import com.openexchange.preupgrade.AbstractPreUpgradeTask;
import com.openexchange.preupgrade.PreUpgradeExceptionCodes;
import com.openexchange.preupgrade.PreUpgradeTask;
import com.openexchange.preupgrade.properties.DatabasePreUpgradeProperty;
import com.openexchange.tools.arrays.Arrays;
import com.openexchange.tools.arrays.Collections;

/**
 * {@link GroupwareDBPreUpdateTask} - Prepares and executes the update tasks for the groupware DB.
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class GroupwareDBPreUpdateTask extends AbstractPreUpgradeTask implements PreUpgradeTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupwareDBPreUpdateTask.class);

    private static final String GLOBAL_DB_POSTFIX = "_global";
    private static final String SELECT_POOL_ID_SCHEMA_NAME = "SELECT db_pool_id, schemaname FROM contexts_per_dbschema";
    private static final String SELECT_SCHEMATA = "SELECT db_pool_id FROM contexts_per_dbschema WHERE schemaname=?";

    private final ExtendedUpdateTaskService updateTaskService;
    private final LeanConfigurationService leanConfigurationService;
    private final DatabaseService databaseService;

    private Map<Integer, List<String>> schemasPerPool;
    private Map<Integer, Map<String, Integer>> schemasPerPoolTaskCount;

    /**
     * Initializes a new {@link GroupwareDBPreUpdateTask}.
     *
     * @param updateTaskService The update task service
     * @param leanConfigurationService The lean configuration service
     * @param databaseService The database service
     */
    public GroupwareDBPreUpdateTask(ExtendedUpdateTaskService updateTaskService, LeanConfigurationService leanConfigurationService, DatabaseService databaseService) {
        super();
        this.updateTaskService = updateTaskService;
        this.leanConfigurationService = leanConfigurationService;
        this.databaseService = databaseService;
    }

    @Override
    
    public int prepareUpgrade() throws OXException {
        schemasPerPool = getSchemata();
        if (!schemasPerPool.isEmpty()) {
            // Will only upgrade specific schemata set by configuration
            int tasks = getTotalProgressForTasks(updateTaskService);
            setRequired(tasks > 0);
            return tasks;
        }
        // Collect information for all schemata
        for (SchemaInfo schema : getAllSchemata()) {
            Collections.put(schemasPerPool, I(schema.getPoolId()), schema.getSchema());
        }
        int tasks = getTotalProgressForTasks(updateTaskService);
        setRequired(tasks > 0);
        return tasks;
    }

    @Override
    public void executeUpgrade() {
        updateSchemataPerDBPool();
        done();
    }

    /**
     * Counts the pending update tasks on all schemata and sets the amount to the total progress of the monitor
     *
     * @param updateTaskService The update task service
     * @throws RuntimeException if getting the update tasks list fails
     */
    private int getTotalProgressForTasks(ExtendedUpdateTaskService updateTaskService) {
        AtomicInteger totalSize = new AtomicInteger();
        schemasPerPoolTaskCount = new ConcurrentHashMap<>();
        schemasPerPool.entrySet().parallelStream().forEach((entry) -> {
            Map<String, Integer> tasksPerSchema = new ConcurrentHashMap<>();
            entry.getValue().parallelStream().forEach((schema) -> {
                try {
                    List<Map<String, Object>> tasksList = filterByState(updateTaskService.getPendingTasksList(schema, true, false, false), "pending");
                    logSummary(schema, tasksList);
                    int relativeSize = tasksList.size();
                    tasksPerSchema.put(schema, I(relativeSize));
                    totalSize.addAndGet(relativeSize);
                } catch (RemoteException e) {
                    throw new RuntimeException(PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create("An error occurred while fetching update tasks' information for schema '" + schema + "'", e));
                }
            });
            schemasPerPoolTaskCount.put(entry.getKey(), tasksPerSchema);
        });
        return totalSize.get();
    }

    /**
     * Logs a summary of what this update task is about to perform
     *
     * @param schema The schema name
     * @param tasksList the update tasks that will be executed
     */
    private void logSummary(String schema, List<Map<String, Object>> tasksList) {
        if (tasksList.isEmpty()) {
            LOGGER.info("No unrun update tasks in schema '{}' detected.", schema);
            return;
        }
        StringBuilder builder = new StringBuilder("The following update task(s) will be executed in schema '");
        builder.append(schema).append("': \n");
        tasksList.forEach(task -> builder.append("  ").append(task.get("taskName")).append("\n"));
        LOGGER.info(builder.toString());
    }

    /**
     * Update configured schemas per DB pool
     *
     * @throws RuntimeException if any schema update fails
     */
    private void updateSchemataPerDBPool() {
        schemasPerPool.entrySet().parallelStream().forEach((entry) -> entry.getValue().forEach((schema) -> {
            try {
                int pendingTasksCount = i(schemasPerPoolTaskCount.get(entry.getKey()).get(schema));
                if (pendingTasksCount == 0) {
                    LOGGER.info("There are 0 pending tasks for schema {}. Nothing to do.", schema);
                    return;
                }
                LOGGER.info("There are {} pending task(s) for schema {}. Executing now...", I(pendingTasksCount), schema);
                List<TaskFailure> failures = updateTaskService.runUpdateFor(schema);
                if (failures.isEmpty()) {
                    LOGGER.info("Finished updating schema {}}", schema);
                    progress(pendingTasksCount);
                    return;
                }
                StringBuilder failed = new StringBuilder("The following task(s) failed for schema '");
                failed.append(schema).append(" ':\n");
                failures.forEach(m -> failed.append("  ").append(m.getTaskName()).append("\n"));
                throw new RuntimeException(PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create(failed.toString()));
            } catch (RemoteException e) {
                throw new RuntimeException(PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create(e.getMessage(), e));
            }
        }));
    }

    /**
     * Get configured schemas per DB pool ID
     *
     * @return The schemas or an empty map
     * @throws OXException if the configured schemata names cannot be fetched
     */
    private Map<Integer, List<String>> getSchemata() throws OXException {
        String property = leanConfigurationService.getProperty(DatabasePreUpgradeProperty.SCHEMATA);
        if (Strings.isEmpty(property)) {
            return new HashMap<>();
        }
        Map<Integer, List<String>> schemasPerPool = new HashMap<>();
        for (String schema : Strings.splitByComma(property)) {
            if (schema.endsWith(GLOBAL_DB_POSTFIX)) {
                // Skip '_global' schemata from groupware updates
                continue;
            }
            Optional<Integer> poolId = optPoolId(schema);
            poolId.ifPresent(id -> Collections.put(schemasPerPool, id, schema));
        }
        return schemasPerPool;
    }

    /**
     * Gets the pool ID of the schema
     *
     * @param schema The schema name
     * @return The optional pool ID
     * @throws OXException if no read-only connection can be established to the database
     */
    private Optional<Integer> optPoolId(String schema) throws OXException {
        Connection con = databaseService.getReadOnly();
        try {
            return optPoolId(schema, con);
        } finally {
            databaseService.backReadOnly(con);
        }
    }

    /**
     * Gets the pool ID of the schema using given connection
     *
     * @param schema The schema name
     * @param con The connection to use
     * @return The pool ID
     * @throws OXException if an SQL or an unexpected error is occurred
     */
    private Optional<Integer> optPoolId(String schema, Connection con) throws OXException {
        // Determine all DB schemata that are currently in use
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Grab database schemata
            stmt = con.prepareStatement(SELECT_SCHEMATA);
            stmt.setString(1, schema);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                // No such database schema in use
                return Optional.empty();
            }
            return Optional.of(I(rs.getInt(1)));
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    /**
     * Gets all available schemas.
     *
     * @return A list containing schemas.
     * @throws OXException If an error occurs
     */
    private Collection<SchemaInfo> getAllSchemata() throws OXException {
        // Determine all DB schemas that are currently in use
        Connection con = databaseService.getReadOnly();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement(SELECT_POOL_ID_SCHEMA_NAME);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                // No database schema in use
                return ImmutableList.of();
            }

            Set<SchemaInfo> set = new LinkedHashSet<>();
            do {
                String schemaName = rs.getString(2);
                // Skip '_global' schemata from groupware updates
                if (!schemaName.endsWith(GLOBAL_DB_POSTFIX)) {
                    set.add(SchemaInfo.valueOf(rs.getInt(1), schemaName));
                }
            } while (rs.next());
            return set;
        } catch (SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw UpdateExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
            Database.back(false, con);
        }
    }

    private static List<Map<String, Object>> filterByState(List<Map<String, Object>> tasksList, String... states) {
        if (null == states || 0 == states.length || null == tasksList || tasksList.isEmpty()) {
            return tasksList;
        }
        List<Map<String, Object>> filteredList = new ArrayList<>(tasksList.size());
        for (Map<String, Object> task : tasksList) {
            if (Arrays.contains(states, (String) task.get("state"))) {
                filteredList.add(task);
            }
        }
        return filteredList;
    }

}
