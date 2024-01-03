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

package com.openexchange.ajax.framework;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.httpclient.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import com.openexchange.test.common.configuration.AJAXConfig;

/**
 * This class stores the HTTP client instance and the session identifier for an AJAX session. Additionally the fallback web conversation is
 * stored here.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class AJAXSession implements AutoCloseable {

    /**
     * User Agent displayed to server - needs to be consistent during a test run for security purposes
     */
    public static final String USER_AGENT = "HTTP API Testing Agent";

    private final CloseableHttpClient httpClient;

    private String id;
    private String createdBy;

    /**
     * 
     * Initializes a new {@link AJAXSession}.
     *
     * @param createdBy For whom the client is created
     */
    public AJAXSession(String createdBy) {
        this(newHttpClient(createdBy), null);
        this.createdBy = createdBy;
    }

    /**
     * 
     * Initializes a new {@link AJAXSession}.
     *
     * @param createdBy For whom the client is created
     * @param client The client to use
     */
    public AJAXSession(String createdBy, CloseableHttpClient client) {
        this(client, null);
        this.createdBy = createdBy;
    }

    /**
     * 
     * Initializes a new {@link AJAXSession}.
     *
     * @param httpClient The client to use
     * @param id The session identifier
     */
    public AJAXSession(CloseableHttpClient httpClient, String id) {
        super();
        this.httpClient = httpClient;
        this.id = id;
    }

    /**
     * Get the {@link CloseableHttpClient}
     *
     * @return The HTTP client
     */
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void close() throws Exception {
        if (null != httpClient) {
            httpClient.close();
        }
    }

    /**
     * From which instance this client was created
     *
     * @return The instance
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * The session identifier
     *
     * @return the session ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the session identifier
     *
     * @param id The session ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Creates a new HTTP client
     *
     * @param createdBy The test user that created the client
     * @return A {@link CloseableHttpClient}
     */
    public static CloseableHttpClient newHttpClient(String createdBy) {
        return newHttpClientBuilder(createdBy).build();
    }

    /**
     * Creates a new HTTP client builder
     *
     * @param createdBy The test user that created the client
     * @return A {@link CloseableHttpClient}
     */
    public static HttpClientBuilder newHttpClientBuilder(String createdBy) {
        return newHttpClientBuilder(createdBy, null);
    }

    /**
     * Creates a new HTTP client builder
     *
     * @param createdBy The test user that created the client
     * @param c Consumer to adjust the {@link RequestConfig}
     * @return A {@link CloseableHttpClient}
     */
    public static HttpClientBuilder newHttpClientBuilder(String createdBy, Consumer<RequestConfig.Builder> c) {
        HttpClientBuilder builder = HttpClientBuilder.create();

        builder.setConnectionTimeToLive(1, TimeUnit.MINUTES);
        builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(1 * 60 * 1000 * 1000).build());
        /*
         * OX cookies work with all browsers, meaning they are a mix of the Netscape draft and the RFC
         */
        Builder requestConfigBuilder = RequestConfig.custom();
        if (null != c) {
            c.accept(requestConfigBuilder);
        }
        builder.setDefaultRequestConfig(requestConfigBuilder.setCookieSpec(CookieSpecs.DEFAULT).setSocketTimeout(1 * 60 * 1000 * 1000).build());
        builder.setUserAgent(USER_AGENT);
        HttpRequestInterceptor requestInterceptor = new HttpRequestInterceptor() {

            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                request.addHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, createdBy == null ? AJAXSession.class.getCanonicalName() : createdBy);

            }
        };
        builder.addInterceptorFirst(requestInterceptor);
        builder.setConnectionManager(getConnectionManager());
        return builder;
    }

    private static PoolingHttpClientConnectionManager getConnectionManager() {
        PoolingHttpClientConnectionManager connManager;
        if (AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL).equalsIgnoreCase("https")) {
            try {
                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                                                                                         .register("https",
                                                                                                   new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build(), NoopHostnameVerifier.INSTANCE))
                                                                                         .register("http", new PlainConnectionSocketFactory())
                                                                                         .build();

                connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                connManager = new PoolingHttpClientConnectionManager();
            }
        } else {
            connManager = new PoolingHttpClientConnectionManager();
        }
        connManager.setDefaultMaxPerRoute(5);
        connManager.setMaxTotal(10);
        return connManager;
    }

}
