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

package com.openexchange.groupware.preupgrade.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.preupgrade.impl.GroupwareDBPreUpdateTask;
import com.openexchange.groupware.update.ExtendedUpdateTaskService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.Tools;
import com.openexchange.preupgrade.PreUpgradeTask;

/**
 * {@link GroupwareDBPreUpgradeActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class GroupwareDBPreUpgradeActivator extends HousekeepingActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupwareDBPreUpgradeActivator.class);

    /**
     * Initializes a new {@link GroupwareDBPreUpgradeActivator}.
     */
    public GroupwareDBPreUpgradeActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ExtendedUpdateTaskService.class, LeanConfigurationService.class, DatabaseService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            registerService(PreUpgradeTask.class, new GroupwareDBPreUpdateTask(getService(ExtendedUpdateTaskService.class), getService(LeanConfigurationService.class), getService(DatabaseService.class)), Tools.withRanking(Integer.MAX_VALUE - 100));//Give headroom for other configdb and globaldb pre-upgrade tasks
        } catch (Exception e) {
            LOGGER.error("Failed to start '{}'", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }
}
