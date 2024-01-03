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

package com.openexchange.database;

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.migration.DBMigrationExecutorService;
import com.openexchange.database.migration.resource.accessor.BundleResourceAccessor;
import com.openexchange.database.preupgrade.ConfigDBPreUpgradeTask;
import com.openexchange.osgi.Tools;
import com.openexchange.preupgrade.PreUpgradeTask;
import com.openexchange.preupgrade.properties.PreUpgradeProperty;
import com.openexchange.server.ServiceLookup;
import liquibase.resource.ResourceAccessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ConfigDBMigrationServiceTracker} - Tracks the {@link DBMigrationExecutorService} and schedules migrations
 * from a specific change-set file.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ConfigDBMigrationServiceTracker implements ServiceTrackerCustomizer<DBMigrationExecutorService, DBMigrationExecutorService> {

    private final String changeSetLocation;
    private final BundleContext context;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link ConfigDBMigrationServiceTracker}.
     */
    public ConfigDBMigrationServiceTracker(ServiceLookup services, BundleContext context, String changeSetLocation) {
        super();
        this.context = context;
        this.services = services;
        this.changeSetLocation = changeSetLocation;
    }

    @Override
    public DBMigrationExecutorService addingService(ServiceReference<DBMigrationExecutorService> serviceReference) {
        DBMigrationExecutorService service = context.getService(serviceReference);
        ResourceAccessor resourceAccessor = new BundleResourceAccessor(context.getBundle());
        try {
            ConfigDBMigrationService configDBMigrationService = services.getServiceSafe(ConfigDBMigrationService.class);
            LeanConfigurationService leanConfigService = services.getServiceSafe(LeanConfigurationService.class);
            boolean upgradePreparation = leanConfigService.getBooleanProperty(PreUpgradeProperty.ENABLED);
            if (upgradePreparation) {
                context.registerService(PreUpgradeTask.class, new ConfigDBPreUpgradeTask(configDBMigrationService, changeSetLocation, resourceAccessor), Tools.withRanking(Integer.MAX_VALUE - 1));
            } else {
                configDBMigrationService.scheduleMigrations(changeSetLocation, resourceAccessor);
            }
            return service;
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(ConfigDBMigrationServiceTracker.class);
            logger.error("Failed to apply change-set: {}", changeSetLocation, e);
        }

        context.ungetService(serviceReference);
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<DBMigrationExecutorService> serviceReference, DBMigrationExecutorService dbMigrationExecutorService) {
        //no-op
    }

    @Override
    public void removedService(ServiceReference<DBMigrationExecutorService> serviceReference, DBMigrationExecutorService dbMigrationExecutorService) {
        context.ungetService(serviceReference);
    }
}
