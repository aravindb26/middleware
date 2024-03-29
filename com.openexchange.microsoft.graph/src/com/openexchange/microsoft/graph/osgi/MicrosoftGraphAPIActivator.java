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

package com.openexchange.microsoft.graph.osgi;

import static com.openexchange.microsoft.graph.api.client.MicrosoftGraphRESTClient.CLIENT_NAME;
import com.openexchange.microsoft.graph.api.MicrosoftGraphAPI;
import com.openexchange.microsoft.graph.contacts.MicrosoftGraphContactsService;
import com.openexchange.microsoft.graph.contacts.impl.MicrosoftGraphContactsServiceImpl;
import com.openexchange.microsoft.graph.onedrive.MicrosoftGraphDriveService;
import com.openexchange.microsoft.graph.onedrive.impl.MicrosoftGraphDriveServiceImpl;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.SpecificHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MicrosoftGraphAPIActivator}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public class MicrosoftGraphAPIActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(MicrosoftGraphAPIActivator.class);

    /**
     * Initialises a new {@link MicrosoftGraphAPIActivator}.
     */
    public MicrosoftGraphAPIActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[]{HttpClientService.class};
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            MicrosoftGraphAPI microsoftGraphAPI = new MicrosoftGraphAPI(this);
            registerService(MicrosoftGraphContactsService.class, new MicrosoftGraphContactsServiceImpl(microsoftGraphAPI.contacts()));
            registerService(MicrosoftGraphDriveService.class, new MicrosoftGraphDriveServiceImpl(microsoftGraphAPI.drive()));
            registerService(SpecificHttpClientConfigProvider.class, new DefaultHttpClientConfigProvider(CLIENT_NAME, "Open-Xchange Microsoft Graph Client"));
        } catch (Exception e) {
            LOG.error("Error starting {}", context.getBundle(), e);
            throw e;
        }
    }
}
