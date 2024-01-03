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

package com.openexchange.collabora.online.client.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.collabora.online.client.CollaboraClientAccess;
import com.openexchange.collabora.online.client.impl.CollaboraClientAccessImpl;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.HttpClientService;

/**
 * {@link CollaboraClientActivator}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraClientActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboraClientActivator.class);
    private CollaboraClientAccessImpl collaboraClientFactory;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { HttpClientService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle '{}'", context.getBundle().getSymbolicName());
            collaboraClientFactory = new CollaboraClientAccessImpl(this);
            registerService(CollaboraClientAccess.class, collaboraClientFactory);
        } catch (Exception e) {
            LOG.error("Failed to start bundle {}", context.getBundle().getSymbolicName(), e);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle '{}'", context.getBundle().getSymbolicName());
        try {
            if (collaboraClientFactory != null) {
                collaboraClientFactory.destroyClient();
            }
        } catch (OXException e) {
            LOG.error(e.getMessage(), e);
        }
        super.stopBundle();
    }
}
