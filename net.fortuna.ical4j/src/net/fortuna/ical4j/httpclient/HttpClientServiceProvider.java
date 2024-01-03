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

package net.fortuna.ical4j.httpclient;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.rest.client.httpclient.HttpClientService;

/**
 * {@link HttpClientServiceProvider}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HttpClientServiceProvider {

    /**
     * Initializes a new {@link HttpClientServiceProvider}.
     */
    private HttpClientServiceProvider() {
        super();
    }

    private static final AtomicReference<HttpClientService> SERVICE_REF = new AtomicReference<HttpClientService>(null);

    /**
     * Sets the service.
     *
     * @param service The service instance to set
     */
    public static void setHttpClientService(HttpClientService service) {
        SERVICE_REF.set(service);
    }

    /**
     * Gets the service.
     *
     * @return The service or <code>null</code>
     */
    public static HttpClientService getHttpClientService() {
        return SERVICE_REF.get();
    }

}
