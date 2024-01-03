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

package com.openexchange.rest.client.httpclient.internal.control;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpUriRequest;
import com.openexchange.java.AbstractOperationsWatcher;
import com.openexchange.rest.client.httpclient.HttpClients;

/**
 * {@link HttpClientInfo} - Wraps the <code>HttpClient</code> instance this is supposed to be connected.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HttpClientInfo extends AbstractOperationsWatcher.Operation {

    /** The constant dummy instance */
    public static final HttpClientInfo POISON = new HttpClientInfo() {

        @Override
        public int compareTo(Delayed o) {
            return -1;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public void interrupt() {
            // Nothing
        }
    };

    // -------------------------------------------------------------------------------------------------------------------------------

    private final HttpRequest request;

    /**
     * Initializes a new {@link HttpClientInfo}.
     */
    private HttpClientInfo() {
        super();
        this.request = null;
    }

    /**
     * Initializes a new {@link HttpClientInfo}.
     *
     * @param httpClientThread The thread connecting the <code>HttpClient</code> instance
     * @param timeout The timeout in milliseconds
     * @param request The associated HTTP request
     */
    public HttpClientInfo(Thread httpClientThread, int timeout, HttpRequest request) {
        super(httpClientThread, timeout);
        this.request = request;
    }

    /**
     * Interrupts the thread performing initial connect against HTTP end-point and aborts associated HTTP request.
     */
    public void interrupt() {
        getProcessingThread().interrupt();
        if (request instanceof HttpUriRequest uriRequest) {
            HttpClients.abort(uriRequest);
        }
    }
}
