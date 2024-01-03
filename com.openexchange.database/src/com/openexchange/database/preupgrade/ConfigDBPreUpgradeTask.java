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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.ConfigDBMigrationService;
import com.openexchange.exception.OXException;
import com.openexchange.preupgrade.AbstractPreUpgradeTask;
import com.openexchange.preupgrade.PreUpgradeExceptionCodes;
import com.openexchange.preupgrade.PreUpgradeTask;
import liquibase.changelog.ChangeSet;
import liquibase.resource.ResourceAccessor;

/**
 * {@link ConfigDBPreUpgradeTask}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ConfigDBPreUpgradeTask extends AbstractPreUpgradeTask implements PreUpgradeTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigDBPreUpgradeTask.class);

    private final ConfigDBMigrationService configDBMigrationService;
    private final String changeSetLocation;
    private final ResourceAccessor resourceAccessor;

    /**
     * Initialises a new {@link ConfigDBPreUpgradeTask}.
     *
     * @param configDBMigrationService The config db migration service
     * @param changeSetLocation The change set location
     * @param resourceAccessor The resource accessor
     */
    public ConfigDBPreUpgradeTask(ConfigDBMigrationService configDBMigrationService, String changeSetLocation, ResourceAccessor resourceAccessor) {
        super();
        this.configDBMigrationService = configDBMigrationService;
        this.changeSetLocation = changeSetLocation;
        this.resourceAccessor = resourceAccessor;
    }

    @Override
    public String getName() {
        return super.getName() + " [" + changeSetLocation + ']';
    }

    @Override
    public int prepareUpgrade() throws OXException {
        try {
            List<ChangeSet> changeSets = configDBMigrationService.listUnrunDBChangeSets(changeSetLocation, resourceAccessor);
            logSummary(changeSets);
            setRequired(changeSets.size() > 0);
            return changeSets.isEmpty() ? 0 : 1; // ConfigDB steps are either 0 or 1; no possibility to track the progress of each individual change-set rule
        } catch (Exception e) {
            throw PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create(e, "Unable to prepare migration for config db");
        }
    }

    @Override
    public void executeUpgrade() throws OXException {
        try {
            configDBMigrationService.scheduleMigrations(changeSetLocation, resourceAccessor).awaitCompletion();
            done();
        } catch (Exception e) {
            throw PreUpgradeExceptionCodes.UNEXPECTED_ERROR.create(e, "Unable to run migration for config db");
        }
    }

    /**
     * Logs a summary of what this update task is about to perform
     *
     * @param changeSets the changes that will be performed
     */
    protected void logSummary(List<ChangeSet> changeSets) {
        if (changeSets.isEmpty()) {
            LOGGER.info("No unrun change-sets from '{}' in config db detected.", changeSetLocation);
            return;
        }
        StringBuilder builder = new StringBuilder("The following change-set(s) from '").append(changeSetLocation).append("' will be executed in the config db: \n");
        changeSets.forEach(set -> builder.append("  ").append(set.getId()).append("\n"));
        LOGGER.info(builder.toString());
    }
}
