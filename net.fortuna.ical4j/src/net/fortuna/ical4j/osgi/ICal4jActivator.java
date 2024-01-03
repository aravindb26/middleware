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

package net.fortuna.ical4j.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.rest.client.httpclient.HttpClientService;

/**
 * {@link ICal4jActivator} - The activator for <code>"net.fortuna.ical4j"</code> bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ICal4jActivator implements BundleActivator {

    private ServiceTracker<HttpClientService, HttpClientService> httpClientServiceTracker;

    /**
     * Initializes a new {@link ICal4jActivator}.
     */
    public ICal4jActivator() {
        super();
    }

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        ServiceTracker<HttpClientService, HttpClientService> httpClientServiceTracker = new ServiceTracker<>(context, HttpClientService.class, new HttpClientServiceTracker(context));
        this.httpClientServiceTracker = httpClientServiceTracker;
        httpClientServiceTracker.open();
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        ServiceTracker<HttpClientService, HttpClientService> httpClientServiceTracker = this.httpClientServiceTracker;
        if (httpClientServiceTracker != null) {
            this.httpClientServiceTracker = null;
            httpClientServiceTracker.close();
        }
    }

}
