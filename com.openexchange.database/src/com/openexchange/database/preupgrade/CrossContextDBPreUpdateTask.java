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

package com.openexchange.database.preupgrade;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.internal.GlobalDatabaseServiceImpl;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.exception.OXException;
import com.openexchange.preupgrade.AbstractPreUpgradeTask;
import com.openexchange.preupgrade.PreUpgradeExceptionCodes;
import com.openexchange.preupgrade.PreUpgradeTask;
import liquibase.changelog.ChangeSet;

/**
 * {@link CrossContextDBPreUpdateTask} - Prepares and executes the update tasks for the global DBs.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class CrossContextDBPreUpdateTask extends AbstractPreUpgradeTask implements PreUpgradeTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrossContextDBPreUpdateTask.class);

    private final DBMigrationExecutorService migrationExecutorService;
    private final GlobalDatabaseServiceImpl globalDatabaseService;

    /**
     * Initializes a new {@link CrossContextDBPreUpdateTask}.
     *
     * @param globalDatabaseService The global db service
     * @param migrationExecutorService the migration executor service
     */
    public CrossContextDBPreUpdateTask(GlobalDatabaseServiceImpl globalDatabaseService, DBMigrationExecutorService migrationExecutorService) {
        super();
        this.globalDatabaseService = globalDatabaseService;
        this.migrationExecutorService = migrationExecutorService;
    }

    @Override
    public int prepareUpgrade() throws OXException {
        try {
            int unrun = 0;
            Map<String, List<ChangeSet>> changeSets = globalDatabaseService.listUnrunDBChangeSetsPerSchema(migrationExecutorService);
            for (Entry<String, List<ChangeSet>> entry : changeSets.entrySet()) {
                logSummary(entry.getKey(), entry.getValue());
                unrun += entry.getValue().size();
            }
            setRequired(0 < unrun);
            return 0 < unrun ? changeSets.size() : 0; // Tracking multiple cross-context database change sets
        } catch (Exception e) {
            throw PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create(e, "Unable to prepare migration for cross context db");
        }
    }

    @Override
    public void executeUpgrade() throws OXException {
        try {
            globalDatabaseService.scheduleMigrations(migrationExecutorService).parallelStream().forEach(state -> {
                try {
                    state.awaitCompletion();
                    progress(1);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            done();
        } catch (Exception e) {
            throw PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create(e, "Unable to execute migration for cross context db");
        }
    }

    /**
     * Logs a summary of what this update task is about to perform
     *
     * @param schemaName the schema name
     * @param changeSets the changes that will be performed
     */
    private static void logSummary(String schemaName, List<ChangeSet> changeSets) {
        if (changeSets.isEmpty()) {
            LOGGER.info("No unrun change-sets in cross context db schema '{}' detected.", schemaName);
            return;
        }
        StringBuilder builder = new StringBuilder("The following change-set(s) will be executed in cross context db schema '");
        builder.append(schemaName).append("': \n");
        changeSets.forEach(set -> builder.append("  ").append(set.getId()).append("\n"));
        LOGGER.info(builder.toString());
    }
}
