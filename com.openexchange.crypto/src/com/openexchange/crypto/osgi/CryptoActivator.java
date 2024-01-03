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

package com.openexchange.crypto.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.crypto.internal.CryptoServiceImpl;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link CryptoActivator} - The crypto service activator
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CryptoActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle {}", context.getBundle());
            registerService(CryptoService.class, new CryptoServiceImpl(getService(LeanConfigurationService.class)));
        } catch (Exception e) {
            LOG.error("Error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    public void stopBundle() throws Exception {
        try {
            LOG.info("Stopping bundle {}", context.getBundle());
            unregisterServices();
            super.stopBundle();
        } catch (Exception e) {
            LOG.error("Error stopping {}", context.getBundle(), e);
            throw e;
        }
    }
}
