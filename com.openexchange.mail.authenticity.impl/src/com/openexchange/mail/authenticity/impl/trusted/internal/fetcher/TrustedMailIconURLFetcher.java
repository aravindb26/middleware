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

package com.openexchange.mail.authenticity.impl.trusted.internal.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableSet;
import com.openexchange.exception.OXException;
import com.openexchange.java.InetAddresses;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.authenticity.impl.osgi.Services;
import com.openexchange.mail.authenticity.impl.trusted.internal.fetcher.httpclient.TrustedMailconRedirectStrategy;
import com.openexchange.mail.authenticity.impl.trusted.internal.fetcher.httpclient.TrustedMailIconHttpProperties;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;

/**
 * {@link TrustedMailIconURLFetcher}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class TrustedMailIconURLFetcher extends AbstractTrustedMailIconFetcher implements TrustedMailIconFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(TrustedMailIconURLFetcher.class);

    /**
     * Initialises a new {@link TrustedMailIconURLFetcher}.
     */
    public TrustedMailIconURLFetcher() {
        super();
    }

    private HttpClient getHttpClient() throws OXException {
        HttpClientService httpClientService = Services.getServiceLookup().getServiceSafe(HttpClientService.class);
        ManagedHttpClient httpClient = httpClientService.getHttpClient(TrustedMailIconHttpProperties.getHttpClientId());
        return httpClient;
    }

    private static final Set<String> PROTOCOLS_HTTP = ImmutableSet.<String> builderWithExpectedSize(2).add("http").add("https").build();

    @Override
    public boolean exists(String resourceUrl) {
        boolean isHttp;
        boolean originalAddressIsRemote;
        try {
            URL url = new URL(resourceUrl);
            InetAddress inetAddress = InetAddresses.forString(url.getHost());
            originalAddressIsRemote = false == InetAddresses.isInternalAddress(inetAddress);
            isHttp = PROTOCOLS_HTTP.contains(Strings.asciiLowerCase(url.getProtocol()));
        } catch (UnknownHostException e) {
            // IP address of that host could not be determined
            LOG.warn("Unknown host. Skipping retrieving image from {}", resourceUrl, e);
            return false;
        } catch (MalformedURLException e) {
            LOG.warn("Invalid URL. Skipping retrieving image from {}", resourceUrl, e);
            return false;
        }

        if (isHttp) {
            HttpContext httpContext = new BasicHttpContext();
            httpContext.setAttribute(TrustedMailconRedirectStrategy.ATTRIBUTE_ORIGINAL_ADDR_IS_REMOTE, Boolean.valueOf(originalAddressIsRemote));

            HttpHead request = null;
            HttpResponse httpResponse = null;
            try {
                request = new HttpHead(resourceUrl);
                httpResponse = getHttpClient().execute(request, httpContext);
                return httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
            } catch (Exception e) {
                LOG.error("An I/O error occurred while reading the resource URL '{}'", resourceUrl, e);
                return false;
            } finally {
                HttpClients.close(request, httpResponse);
            }
        }

        InputStream inputStream = null;
        try {
            URLConnection connection = prepareConnection(resourceUrl);
            connection.connect();

            inputStream = connection.getInputStream();
            return inputStream.read() > 0;
        } catch (IOException e) {
            LOG.error("An I/O error occurred while reading the resource URL '{}': {}", resourceUrl, e.getMessage(), e);
            return false;
        } finally {
            Streams.close(inputStream);
        }
    }

    @Override
    public byte[] fetch(String resourceUrl) {
        boolean isHttp;
        boolean originalAddressIsRemote;
        try {
            URL url = new URL(resourceUrl);
            InetAddress inetAddress = InetAddresses.forString(url.getHost());
            originalAddressIsRemote = false == InetAddresses.isInternalAddress(inetAddress);
            isHttp = PROTOCOLS_HTTP.contains(Strings.asciiLowerCase(url.getProtocol()));
        } catch (UnknownHostException e) {
            // IP address of that host could not be determined
            LOG.warn("Unknown host. Skipping retrieving image from {}", resourceUrl, e);
            return null;
        } catch (MalformedURLException e) {
            LOG.warn("Invalid URL. Skipping retrieving image from {}", resourceUrl, e);
            return null;
        }

        if (isHttp) {
            HttpContext httpContext = new BasicHttpContext();
            httpContext.setAttribute(TrustedMailconRedirectStrategy.ATTRIBUTE_ORIGINAL_ADDR_IS_REMOTE, Boolean.valueOf(originalAddressIsRemote));

            HttpGet request = null;
            HttpResponse httpResponse = null;
            try {
                request = new HttpGet(resourceUrl);
                httpResponse = getHttpClient().execute(request, httpContext);
                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    LOG.error("An HTTP error occurred while reading from resource URL '{}': {}", resourceUrl, statusLine);
                    return null;
                }
                HttpEntity entity = httpResponse.getEntity();
                if (entity == null) {
                    LOG.error("No content while reading from resource URL '{}'", resourceUrl);
                    return null;
                }
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    return process(ImageIO.read(inputStream));
                } finally {
                    Streams.close(inputStream);
                }
            } catch (Exception e) {
                LOG.error("An I/O error occurred while reading from resource URL '{}'", resourceUrl, e);
                return null;
            } finally {
                HttpClients.close(request, httpResponse);
            }
        }

        InputStream inputStream = null;
        try {
            URLConnection connection = prepareConnection(resourceUrl);
            connection.connect();

            inputStream = connection.getInputStream();
            return process(ImageIO.read(inputStream));
        } catch (IOException e) {
            LOG.error("An I/O error occurred while reading the resource URL '{}': {}", resourceUrl, e.getMessage(), e);
            return null;
        } finally {
            Streams.close(inputStream);
        }
    }

    /**
     * Prepares an {@link URLConnection} for the specified resource URL
     *
     * @param resourceUrl The resource URL
     * @return The prepared {@link URLConnection}
     * @throws IOException if an I/O error occurs
     */
    private static URLConnection prepareConnection(String resourceUrl) throws IOException {
        URLConnection connection = new URL(resourceUrl).openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        return connection;
    }

}
