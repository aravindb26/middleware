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

package com.openexchange.rest.client.httpclient.internal;

import static com.openexchange.java.Autoboxing.b;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNull;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;
import com.openexchange.rest.client.httpclient.internal.control.HttpClientControl;
import com.openexchange.rest.client.httpclient.internal.control.HttpClientInfo;

/**
 * {@link ManagedHttpClientImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
@SuppressWarnings("deprecation")
public class ManagedHttpClientImpl implements ManagedHttpClient {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ManagedHttpClientImpl.class);
    }

    private final String clientId;
    private final AtomicReference<CloseableHttpClient> httpClientReference;
    private final AtomicReference<ClientConnectionManager> ccm;
    private final Supplier<Boolean> reloadCallback;

    private volatile int configHashCode;
    private final int hardReadTimeout;
    private final int hardConnectTimeout;

    /**
     *
     * Initializes a new {@link ManagedHttpClientImpl}.
     *
     * @param clientId The client identifier
     * @param configHashCode The hash code of the client configuration
     * @param httpClient The actual HTTP client
     * @param ccm The connection manager of the HTTP client
     * @param reloadCallback A callback that initializes a new HTTP client and replaces it in this managed instance.
     *            <p>
     *            Apache framework silently closes the connection pool, when execution of a request fails. This
     *            callback ensures that a working HTTP client is returned to the caller or at least an error can be
     *            handled.
     *            <p>
     *            If the creation of the new HTTP client fails the managed instance will be removed from the cache and
     *            the underlying HTTP client closed. This will persist the error in cases where the managed client is a
     *            member within the calling class.
     *            When called, will return <code>true</code> in case the client has been successfully reloaded,
     *            <code>false</code> in case the client is unusable.
     */
    public ManagedHttpClientImpl(String clientId, int configHashCode, CloseableHttpClient httpClient, ClientConnectionManager ccm, Supplier<Boolean> reloadCallback, Supplier<HttpBasicConfig> configSupplier) {
        super();
        this.clientId = clientId;
        this.configHashCode = configHashCode;
        this.httpClientReference = new AtomicReference<>(httpClient);
        this.ccm = new AtomicReference<>(ccm);
        this.reloadCallback = reloadCallback;
        this.hardReadTimeout = configSupplier.get().getHardReadTimeout();
        this.hardConnectTimeout = configSupplier.get().getHardConnectTimeout();
    }

    private @NonNull HttpClient getHttpClient() throws IllegalStateException {
        CloseableHttpClient httpClient = httpClientReference.get();
        if (null == httpClient) {
            throw new IllegalStateException("HttpClient is null.");
        }
        if (ccm.get().isShutdown()) {
            /*
             * Connection manager is unusable. Client has to be re-created.
             */
            if (b(reloadCallback.get())) {
                httpClient = httpClientReference.get();
                if (null == httpClient) {
                    throw new IllegalStateException("HttpClient is null.");
                }
                return httpClient;
            }
            throw new IllegalStateException("HttpClient is not useable.");
        }
        return httpClient;
    }

    /**
     * Removes the HTTP client reference from this managed instance
     *
     * @return The HTTP client
     */
    public CloseableHttpClient unset() {
        ccm.set(null);
        return httpClientReference.getAndSet(null);
    }

    /**
     * Replaces the HTTP client in this managed instance
     *
     * @param newHttpClient The new HTTP client
     * @param ccm The ClientConnectionManager the HTTP client uses
     * @param configHashCode The hash code of the configuration used to create the HTTP client
     * @return The (old) HTTP client used by this managed instance
     */
    public CloseableHttpClient reload(CloseableHttpClient newHttpClient, ClientConnectionManager ccm, int configHashCode) {
        this.configHashCode = configHashCode;
        this.ccm.set(ccm);
        return httpClientReference.getAndSet(newHttpClient);
    }

    /**
     * Checks if neither connect nor read timeout is set.
     *
     * @return <code>true</code> if neither timeout is set; otherwise <code>false</code> if any timeout is set
     */
    private boolean isNoTimeoutSet() {
        return hardReadTimeout <= 0 && hardConnectTimeout <= 0;
    }

    /**
     * Gets the clientId
     *
     * @return The clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the configHash
     *
     * @return The configHash
     */
    public int getConfigHash() {
        return configHashCode;
    }

    @Override
    public String toString() {
        return "managedHttpClient[clientId=" + clientId + ", configHashCode=" + String.valueOf(configHashCode) + "]";
    }

    // ------------------------------------------------ HTTP client methods --------------------------------------------------------------

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(request);
        }

        return execute(request, (HttpContext) null);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(request, context);
        }

        return doExecute(determineTarget(request), request, context, getHttpClient());
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(target, request);
        }

        return doExecute(target, request, null, getHttpClient());
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(target, request, context);
        }

        return doExecute(target, request, context, getHttpClient());
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(request, responseHandler);
        }

        return handleResponse(this.execute(request), responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(request, responseHandler, context);
        }

        return handleResponse(this.execute(request, context), responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(target, request, responseHandler);
        }

        return handleResponse(this.execute(target, request), responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        if (isNoTimeoutSet()) {
            return getHttpClient().execute(target, request, responseHandler, context);
        }

        return handleResponse(this.execute(target, request, context), responseHandler);
    }

    @Override
    public HttpParams getParams() {
        return getHttpClient().getParams();
    }

    @Override
    public org.apache.http.conn.ClientConnectionManager getConnectionManager() {
        return getHttpClient().getConnectionManager();
    }

    // ------------------------------------------------- Watcher stuff ---------------------------------------------------------------------

    private HttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context, HttpClient httpClient) throws ClientProtocolException, IllegalStateException, IOException {
        if (hardConnectTimeout <= 0) {
            // No connect timeout set
            return executeReadTimeoutAware(target, request, context, httpClient);
        }

        // Watch connect
        HttpClientInfo taskWrapper = new HttpClientInfo(Thread.currentThread(), hardConnectTimeout, request);
        if (HttpClientControl.getInstance().add(taskWrapper)) {
            try {
                return executeReadTimeoutAware(target, request, context, httpClient);
            } finally {
                HttpClientControl.getInstance().remove(taskWrapper);
            }
        }

        // Unable to add to HttpClientControl
        return executeReadTimeoutAware(target, request, context, httpClient);
    }

    private HttpResponse executeReadTimeoutAware(HttpHost target, HttpRequest request, HttpContext context, HttpClient httpClient) throws IOException, ClientProtocolException {
        if (hardReadTimeout <= 0) {
            // No read timeout set
            return httpClient.execute(target, request, context);
        }

        HttpResponse httpResponse = httpClient.execute(target, request, context);
        HttpEntity entity = httpResponse.getEntity();
        if (null == entity) {
            // No content available from HTTP response. Thus nothing to control...
            return httpResponse;
        }

        entity = new AvoidConsumingEntityWrapper(entity, httpResponse);
        httpResponse.setEntity(entity);
        return new ControlledHttpResponseWrapper(httpResponse, hardReadTimeout);
    }

    // ------------------------------------------------- Utility stuff ---------------------------------------------------------------------

    private static <T> T handleResponse(HttpResponse response, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        try {
            final T result = responseHandler.handleResponse(response);
            final HttpEntity entity = response.getEntity();
            EntityUtils.consume(entity);
            return result;
        } catch (final ClientProtocolException t) {
            // Try to salvage the underlying connection in case of a protocol exception
            final HttpEntity entity = response.getEntity();
            try {
                EntityUtils.consume(entity);
            } catch (final Exception t2) {
                // Log this exception. The original exception is more
                // important and will be thrown to the caller.
                LoggerHolder.LOG.warn("Error consuming content after an exception.", t2);
            }
            throw t;
        } finally {
            CloseableHttpResponse closeableResponse = optCloseableHttpResponseFrom(response);
            if (closeableResponse != null) {
                closeableResponse.close();
            }
        }
    }

    private static CloseableHttpResponse optCloseableHttpResponseFrom(HttpResponse response) {
        HttpResponse resp = response;
        if (resp instanceof ControlledHttpResponseWrapper wrapper) {
            resp = wrapper.getHttpResponse();
        }
        if (resp instanceof CloseableHttpResponse closeableResponse) {
            return closeableResponse;
        }
        return null;
    }

    private static HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
        // A null target may be acceptable if there is a default target.
        // Otherwise, the null target is detected in the director.
        HttpHost target = null;

        final URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            target = URIUtils.extractHost(requestURI);
            if (target == null) {
                throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI);
            }
        }
        return target;
    }

    private static class AvoidConsumingEntityWrapper implements HttpEntity {

        private final HttpEntity wrapped;
        private final HttpResponse httpResponse;

        AvoidConsumingEntityWrapper(HttpEntity wrapped, HttpResponse httpResponse) {
            super();
            this.wrapped = wrapped;
            this.httpResponse = httpResponse;
        }

        @Override
        public boolean isRepeatable() {
            return wrapped.isRepeatable();
        }

        @Override
        public boolean isChunked() {
            return wrapped.isChunked();
        }

        @Override
        public long getContentLength() {
            return wrapped.getContentLength();
        }

        @Override
        public Header getContentType() {
            return wrapped.getContentType();
        }

        @Override
        public Header getContentEncoding() {
            return wrapped.getContentEncoding();
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return HttpClients.createHttpResponseStreamFor(wrapped, httpResponse, Optional.empty(), false);
        }

        @Override
        public void writeTo(OutputStream outStream) throws IOException {
            InputStream inStream = getContent();
            try {
                final byte[] tmp = new byte[8192];
                for (int l; (l = inStream.read(tmp)) != -1;) {
                    outStream.write(tmp, 0, l);
                }
            } finally {
                inStream.close();
            }
        }

        @Override
        public boolean isStreaming() {
            return wrapped.isStreaming();
        }

        @Override
        public void consumeContent() throws IOException {
            wrapped.consumeContent();
        }
    }

}
