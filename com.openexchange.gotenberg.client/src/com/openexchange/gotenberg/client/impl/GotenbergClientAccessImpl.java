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

package com.openexchange.gotenberg.client.impl;

import java.net.URI;
import java.net.URISyntaxException;
import com.openexchange.exception.OXException;
import com.openexchange.gotenberg.client.GotenbergClient;
import com.openexchange.gotenberg.client.GotenbergClientAccess;
import com.openexchange.gotenberg.client.exception.GotenbergExceptionCodes;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.server.ServiceLookup;

/**
 * {@link GotenbergClientAccessImpl} -default implementation of the {@link GotenbergClientAccess}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class GotenbergClientAccessImpl implements GotenbergClientAccess {

    private static final String CLIENT_ID = "gotenberg";

    private final ServiceLookup services;

    /**
     * Initializes a new {@link GotenbergClientAccessImpl}.
     *
     * @param services - The {@link ServiceLookup}
     */
    public GotenbergClientAccessImpl(ServiceLookup services) {
        this.services = services;
    }

    /**
     * Internal method to get the {@link HttpClientService}
     *
     * @return The {@link HttpClientService}
     * @throws OXException if the {@link HttpClientService} is absent
     */
    private HttpClientService getHttpClientService() throws OXException {
        return this.services.getServiceSafe(HttpClientService.class);
    }

    /**
     * Destroys the underlying client managed by the {@link HttpClientService}
     *
     * @throws OXException if an error is occurred
     */
    public void destroyClient() throws OXException {
        getHttpClientService().destroyHttpClient(CLIENT_ID);
    }

    @Override
    public GotenbergClient getClient(String baseUri) throws OXException {
        try {
            return new GotenbergClient(new URI(baseUri), getHttpClientService().getHttpClient(CLIENT_ID));
        } catch (URISyntaxException e) {
            throw GotenbergExceptionCodes.INVALID_URL.create(e, baseUri, e.getMessage());
        }
    }
}
