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

package com.openexchange.preupgrade.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.preupgrade.internal.PreUpgradeExecutor;
import com.openexchange.preupgrade.properties.PreUpgradeProperty;
import com.openexchange.startup.SignalStartedService;
import com.openexchange.startup.StaticSignalStartedService;

/**
 * {@link PreUpgradeActivator} - The pre-upgrade activator
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class PreUpgradeActivator extends HousekeepingActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreUpgradeActivator.class);

    /**
     * Initializes a new {@link PreUpgradeActivator}.
     */
    public PreUpgradeActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, LeanConfigurationService.class, SignalStartedService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        if (false == getService(LeanConfigurationService.class).getBooleanProperty(PreUpgradeProperty.ENABLED)) {
            LOGGER.info("Starting of bundle {} prohibited per configuration", context.getBundle().getSymbolicName());
            return;
        }

        LOGGER.info("Starting bundle {}", context.getBundle().getSymbolicName());

        SignalStartedService startedService = getService(SignalStartedService.class);
        if (false == StaticSignalStartedService.State.OK.equals(startedService.getState())) {
            LOGGER.warn("Unable to perform pre-upgrade. Server didn't start correctly");
            shutdown(true);
            return;
        }

        // Server is started completely, begin upgrade
        LOGGER.info("Preparing upgrade.");
        PreUpgradeExecutor preUpgrade = null;
        boolean error = true;
        try {
            preUpgrade = new PreUpgradeExecutor(context);
            preUpgrade.prepareUpgrade();
            preUpgrade.executeUpgrade();
            error = false;
            LOGGER.info("Pre-upgrade has finished.");
        } catch (Exception e) {
            LOGGER.error("Pre-upgrade was aborted due to errors: ", e);
        } finally {
            shutDownExecutorSafely(preUpgrade);
            shutdown(error);
        }
    }

    private void shutDownExecutorSafely(PreUpgradeExecutor preUpgrade) {
        if (preUpgrade != null) {
            try {
                preUpgrade.shutDown();
            } catch (Exception e) {
                LOGGER.warn("Shut-down of pre-upgrade executor failed", e);
            }
        }
    }

    /**
     * Shut down the server if configured
     * <p>
     * Will trigger <code>com.openexchange.control.ControlActivator.ControlShutdownHookThread</code>
     * and thus <code>com.openexchange.control.internal.GeneralControl.shutdown(BundleContext, boolean)</code> implicitly
     *
     * @param error Whether an error occurred during upgrade
     */
    private void shutdown(boolean error) {
        try {
            if (getService(LeanConfigurationService.class).getBooleanProperty(PreUpgradeProperty.SHUTDOWN)) {
                LOGGER.info("Shuting down server as per configuration");
                Runtime.getRuntime().exit(error ? 1 : 0);
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to shutdown: {}", e.getMessage(), e);
        }
    }
}
