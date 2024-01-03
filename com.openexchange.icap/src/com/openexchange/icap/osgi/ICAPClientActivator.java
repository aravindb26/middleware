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

package com.openexchange.icap.osgi;

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.icap.ICAPClientFactoryService;
import com.openexchange.icap.impl.ICAPClientFactoryServiceImpl;
import com.openexchange.monitoring.osgi.SocketLoggerBlackListServiceTracker;
import com.openexchange.monitoring.sockets.SocketLoggerRegistryService;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link ICAPClientActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public class ICAPClientActivator extends HousekeepingActivator {

    /**
     * Initialises a new {@link ICAPClientActivator}.
     */
    public ICAPClientActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        registerService(ICAPClientFactoryService.class, new ICAPClientFactoryServiceImpl(this));
        track(SocketLoggerRegistryService.class, new SocketLoggerBlackListServiceTracker("com.openexchange.icap", context));
        openTrackers();
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
    }
}
