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

package com.openexchange.demo.osgi;

import static com.openexchange.demo.DemoProperty.ENABLED;
import org.slf4j.Logger;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.demo.DemoSystemHealthCheck;
import com.openexchange.demo.InitializationPerformer;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link DemoActivator}
 *
 * @author <a href="mailto:nikolaos.tsapanidis@open-xchange.com">Nikolaos Tsapanidis</a>
 * @since v8.0.0
 */
public final class DemoActivator extends HousekeepingActivator {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DemoActivator.class);

    private DelayedInitializer delayedInitializer;

    /**
     * Initializes a new {@link DemoActivator}.
     */
    public DemoActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { LeanConfigurationService.class, ThreadPoolService.class };
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        LOG.info("Starting bundle {}", context.getBundle().getSymbolicName());
        LeanConfigurationService configService = this.getServiceSafe(LeanConfigurationService.class);

        if (configService.getBooleanProperty(ENABLED) == false) {
            LOG.info("Canceled start-up of bundle {} since it's disabled by com.openexchange.demo.enabled", context.getBundle().getSymbolicName());
            return;
        }

        InitializationPerformer initializationPerformer = new InitializationPerformer(configService);

        registerService(MWHealthCheck.class, new DemoSystemHealthCheck(initializationPerformer));

        DelayedInitializer delayedInitializer = new DelayedInitializer(initializationPerformer, getServiceSafe(ThreadPoolService.class), context);
        this.delayedInitializer = delayedInitializer;
        delayedInitializer.start();
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        LOG.info("Stopping bundle {}", context.getBundle().getSymbolicName());

        DelayedInitializer delayedInitializer = this.delayedInitializer;
        if (delayedInitializer != null) {
            this.delayedInitializer = null;
            delayedInitializer.stop();
        }

        super.stopBundle();
    }

}
