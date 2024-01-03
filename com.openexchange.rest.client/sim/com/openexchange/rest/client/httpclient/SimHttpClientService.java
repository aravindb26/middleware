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

package com.openexchange.rest.client.httpclient;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import com.openexchange.annotation.NonNull;

/**
 * {@link SimHttpClientService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class SimHttpClientService implements HttpClientService {

    /**
     * Initializes a new {@link SimHttpClientService}.
     */
    public SimHttpClientService() {
        super();
    }

    @Override
    public @NonNull ManagedHttpClient getHttpClient(String httpClientId) {
        return new SimManagedHttpclient(HttpClientBuilder.create().build());
    }

    @Override
    public void destroyHttpClient(String httpClientId) {
        // Nothing
    }

    private static class SimManagedHttpclient implements ManagedHttpClient {

        private final HttpClient httpClient;

        SimManagedHttpclient(HttpClient httpClient) {
            super();
            this.httpClient = httpClient;
        }

        @Override
        public HttpParams getParams() {
            return httpClient.getParams();
        }

        @Override
        public ClientConnectionManager getConnectionManager() {
            return httpClient.getConnectionManager();
        }

        @Override
        public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
            return httpClient.execute(request);
        }

        @Override
        public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
            return httpClient.execute(request, context);
        }

        @Override
        public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
            return httpClient.execute(target, request);
        }

        @Override
        public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
            return httpClient.execute(target, request, context);
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
            return httpClient.execute(request, responseHandler);
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
            return httpClient.execute(request, responseHandler, context);
        }

        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
            return httpClient.execute(target, request, responseHandler);
        }

        @Override
        public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
            return httpClient.execute(target, request, responseHandler, context);
        }
    }

}
