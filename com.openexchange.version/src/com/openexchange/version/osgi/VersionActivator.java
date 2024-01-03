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

package com.openexchange.version.osgi;

import com.openexchange.log.LogProperties;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.version.VersionService;
import com.openexchange.version.internal.VersionServiceImpl;

/**
 * Reads version and build number from the bundle manifest.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a> Refactoring
 */
public class VersionActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VersionActivator.class);

    /**
     * Initializes a new {@link VersionActivator}.
     */
    public VersionActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        // Expect VersionService to be the first invoked activator. Hence, put `LogProperties.Name.DROP_MDC` log property here.
        LogProperties.putProperty(LogProperties.Name.DROP_MDC, "true");
        LOG.info("Starting bundle {}", context.getBundle().getSymbolicName());
        try {
            VersionService versionService = new VersionServiceImpl();
            registerService(VersionService.class, versionService);
            LOG.info("{} {}", VersionServiceImpl.NAME, versionService.getVersion());
            LOG.info("(c) OX Software GmbH , Open-Xchange GmbH");
        } catch (Exception e) {
            LOG.error("Error starting bundle {}", context.getBundle().getSymbolicName(), e);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        try {
            LOG.info("Stopping bundle {}", context.getBundle().getSymbolicName());
            super.stopBundle();
        } catch (Exception e) {
            LOG.error("Error stopping bundle {}", context.getBundle().getSymbolicName(), e);
        }
    }

}

