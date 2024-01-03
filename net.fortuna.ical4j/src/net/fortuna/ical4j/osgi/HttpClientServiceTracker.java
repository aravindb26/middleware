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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.rest.client.httpclient.HttpClientService;
import net.fortuna.ical4j.httpclient.HttpClientServiceProvider;

/**
 * {@link HttpClientServiceTracker} - Tracker for HtppClient service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HttpClientServiceTracker implements ServiceTrackerCustomizer<HttpClientService, HttpClientService> {

    private final BundleContext context;

    /**
     * Initializes a new {@link HttpClientServiceTracker}.
     *
     * @param context The bundle context
     */
    public HttpClientServiceTracker(BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public HttpClientService addingService(ServiceReference<HttpClientService> reference) {
        HttpClientService service = context.getService(reference);
        HttpClientServiceProvider.setHttpClientService(service);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<HttpClientService> reference, HttpClientService service) {
        // Ignore
    }

    @Override
    public void removedService(ServiceReference<HttpClientService> reference, HttpClientService service) {
        HttpClientServiceProvider.setHttpClientService(null);
        context.ungetService(reference);
    }

}
