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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.EofSensorWatcher;
import org.apache.http.util.EntityUtils;
import com.openexchange.java.Streams;

/**
 * {@link HttpClients} - Utility class for HTTP client.
 * <p>
 * See <a href="http://svn.apache.org/repos/asf/httpcomponents/httpclient/branches/4.0.x/httpclient/src/examples/org/apache/http/examples/client/">here</a> for several examples.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.1
 */
public final class HttpClients {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HttpClients.class);

    /**
     * Initializes a new {@link HttpClients}.
     */
    private HttpClients() {
        super();
    }

    /** The default timeout for client connections. */
    private static final int DEFAULT_TIMEOUT_MILLIS = 30000;

    /**
     * Applies the default timeout of 30sec to given HTTP request.
     *
     * @param request The HTTP request
     */
    public static void setDefaultRequestTimeout(HttpRequestBase request) {
        if (null == request) {
            return;
        }
        request.setConfig(RequestConfig.custom().setConnectTimeout(DEFAULT_TIMEOUT_MILLIS).setSocketTimeout(DEFAULT_TIMEOUT_MILLIS).build());
    }

    /**
     * Applies the specified timeout to given HTTP request.
     *
     * @param timeoutMillis The timeout in milliseconds to apply
     * @param request The HTTP request
     */
    public static void setRequestTimeout(int timeoutMillis, HttpRequestBase request) {
        if (null == request || timeoutMillis <= 0) {
            return;
        }
        request.setConfig(RequestConfig.custom().setConnectTimeout(timeoutMillis).setSocketTimeout(timeoutMillis).build());
    }

    /**
     * Closes the supplied HTTP request / response resources silently.
     * <p>
     * <ul>
     * <li>Resets internal state of the HTTP request making it reusable.</li>
     * <li>Ensures that the response's content is fully consumed and the content stream, if exists, is closed</li>
     * <li>Checks if HTTP response is an instance of {@link CloseableHttpResponse}. If so <code>close()</code> is invoked</li>
     * </ul>
     *
     * @param request The HTTP request to reset
     * @param response The HTTP response to consume and close
     */
    public static void close(HttpRequestBase request, HttpResponse response) {
        close(response, true);
        if (null != request) {
            try {
                request.reset();
            } catch (Exception e) {
                LOG.trace("Failed to reset request for making it reusable.", e);
            }
        }
    }

    /**
     * Closes the supplied HTTP response resource silently.
     * <p>
     * <ul>
     * <li>Ensures that the response's content is fully consumed and the content stream, if exists, is closed (provided that <code>consumeEntity</code> is set to <code>true</code>)</li>
     * <li>Checks if HTTP response is an instance of {@link CloseableHttpResponse}. If so <code>close()</code> is invoked</li>
     * </ul>
     *
     * @param response The HTTP response to consume and close
     * @param consumeEntity Whether to consume the message entity of given HTTP response
     */
    public static void close(HttpResponse response, boolean consumeEntity) {
        if (null != response) {
            if (consumeEntity) {
                HttpEntity entity = response.getEntity();
                if (null != entity) {
                    try {
                        EntityUtils.consumeQuietly(entity);
                    } catch (Exception e) {
                        LOG.trace("Failed to ensure that the entity content is fully consumed and the content stream, if exists, is closed.", e);
                    }
                }
            }
            if (response instanceof CloseableHttpResponse) {
                try {
                    ((CloseableHttpResponse) response).close();
                } catch (Exception e) {
                    LOG.debug("Error closing HTTP response", e);
                }
            }
        }
    }

    /**
     * Aborts the supplied HTTP response silently.
     *
     * @param response The HTTP response to abort
     */
    public static void abort(HttpResponse response) {
        if (null != response) {
            HttpEntity entity = response.getEntity();
            if (entity.isStreaming()) {
                try {
                    InputStream inStream = entity.getContent();
                    if (inStream != null && (entity instanceof EofSensorWatcher)) {
                        EofSensorWatcher eofSensorWatcher = (EofSensorWatcher) entity;
                        eofSensorWatcher.eofDetected(inStream);
                    }
                } catch (Exception e) {
                    LOG.debug("Failed to abort HTTP response's input stream", e);
                }
            }
            if (response instanceof CloseableHttpResponse) {
                try {
                    ((CloseableHttpResponse) response).close();
                } catch (Exception e) {
                    LOG.debug("Error closing HTTP response", e);
                }
            }
        }
    }

    /**
     * Aborts the supplied HTTP request silently.
     * <p>
     * Any active execution of this method should return immediately. If the request has not started, it will abort after the next execution.
     * Aborting this request will cause all subsequent executions with this request to fail.
     *
     * @param request The HTTP request to abort
     */
    public static void abort(HttpUriRequest request) {
        if (null != request) {
            try {
                request.abort();
            } catch (Exception e) {
                LOG.trace("Failed to abort request.", e);
            }
        }
    }

    // ----------------------------------------------- Response entity stream --------------------------------------------------------------

    /**
     * Initializes a new {@link HttpResponseStream} for given HTTP response.
     *
     * @param response The HTTP response to create the stream for
     * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
     * @return The newly created response stream
     * @throws IOException If initialization fails
     */
    public static HttpResponseStream createContentLengthIgnoringHttpResponseStreamFor(HttpResponse response, Optional<HttpRequestBase> optionalRequest) throws IOException {
        return createContentLengthIgnoringHttpResponseStreamFor(response, optionalRequest, true);
    }

    /**
     * Initializes a new {@link HttpResponseStream} for given HTTP response.
     *
     * @param response The HTTP response to create the stream for
     * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
     * @return The newly created response stream
     * @throws IOException If initialization fails
     */
    public static HttpResponseStream createContentLengthIgnoringHttpResponseStreamFor(HttpResponse response, Optional<HttpRequestBase> optionalRequest, boolean consumeEntity) throws IOException {
        if (response == null) {
            return null;
        }

        HttpEntity entity = response.getEntity();
        if (null == entity) {
            throw new IOException("No response entity");
        }

        return createContentLengthIgnoringHttpResponseStreamFor(entity, response, optionalRequest, consumeEntity);
    }

    /**
     * Initializes a new {@link HttpResponseStream} for given HTTP response.
     *
     * @param entity The HTTP entity providing the content
     * @param response The HTTP response to create the stream for
     * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
     * @param consumeEntity Whether entity is supposed to be fully consumed on <code>close()</code> or not
     * @return The newly created response stream
     * @throws IOException If initialization fails
     */
    public static HttpResponseStream createContentLengthIgnoringHttpResponseStreamFor(HttpEntity entity, HttpResponse response, Optional<HttpRequestBase> optionalRequest, boolean consumeEntity) throws IOException {
        if (entity == null || response == null) {
            return null;
        }

        return new HttpResponseStream(entity.getContentLength(), entity.getContent(), response, optionalRequest, consumeEntity);
    }

    /**
     * Initializes a new {@link HttpResponseStream} for given HTTP response.
     *
     * @param response The HTTP response to create the stream for
     * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
     * @return The newly created response stream
     * @throws IOException If initialization fails
     */
    public static HttpResponseStream createHttpResponseStreamFor(HttpResponse response, Optional<HttpRequestBase> optionalRequest) throws IOException {
        return createHttpResponseStreamFor(response, optionalRequest, true);
    }

    /**
     * Initializes a new {@link HttpResponseStream} for given HTTP response.
     *
     * @param response The HTTP response to create the stream for
     * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
     * @param consumeEntity Whether entity is supposed to be fully consumed on <code>close()</code> or not
     * @return The newly created response stream
     * @throws IOException If initialization fails
     */
    public static HttpResponseStream createHttpResponseStreamFor(HttpResponse response, Optional<HttpRequestBase> optionalRequest, boolean consumeEntity) throws IOException {
        if (response == null) {
            return null;
        }

        HttpEntity entity = response.getEntity();
        if (null == entity) {
            throw new IOException("No response entity");
        }

        return createHttpResponseStreamFor(entity, response, optionalRequest, consumeEntity);
    }

    /**
     * Initializes a new {@link HttpResponseStream} for given HTTP response.
     *
     * @param entity The HTTP entity providing the content
     * @param response The HTTP response to create the stream for
     * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
     * @param consumeEntity Whether entity is supposed to be fully consumed on <code>close()</code> or not
     * @return The newly created response stream
     * @throws IOException If initialization fails
     */
    public static HttpResponseStream createHttpResponseStreamFor(HttpEntity entity, HttpResponse response, Optional<HttpRequestBase> optionalRequest, boolean consumeEntity) throws IOException {
        if (entity == null || response == null) {
            return null;
        }

        long contentLength = entity.getContentLength();
        if (contentLength < 0) {
            // No content length advertised
            return new HttpResponseStream(-1, entity.getContent(), response, optionalRequest, consumeEntity);
        }

        /*-
         * Content length advertised. Hence, an instance of `org.apache.http.impl.io.ContentLengthInputStream` represents actual entity's
         * content stream. Closing such an instance results in unnecessarily reading of all remaining data from stream:
         *
         * public void close() throws IOException {
         *  ...
         *  if (pos < contentLength) {
         *      final byte buffer[] = new byte[BUFFER_SIZE];
         *      while (read(buffer) >= 0) { <------------------ Read data until EOF
         *        // do nothing.
         *      }
         *  }
         *  ...
         * }
         */
        return new ContentLengthAwareHttpResponseStream(contentLength, entity.getContent(), response, optionalRequest, consumeEntity);
    }

    /**
     * An input stream providing the content of a response's entity.
     */
    public static class HttpResponseStream extends FilterInputStream {

        /** The HTTP response whose entity's content is read from */
        protected final HttpResponse response;

        /** The optional HTTP request; if present request will be reset when returned stream gets closed */
        protected final Optional<HttpRequestBase> optionalRequest;

        /** The length of the content on bytes */
        protected final long contentLength;

        /** Whether to consume HTTP entity on <code>close()</code> invocation */
        protected final boolean consumeEntity;

        /**
         * Initializes a new {@link HttpResponseStream}.
         *
         * @param contentLength The number of bytes of the content, or a negative number if unknown
         * @param content The response entity's input stream
         * @param response The HTTP response whose entity stream shall be read from
         * @param optionalRequest The optional HTTP request; if present request will be reset when returned stream gets closed
         * @param consumeEntity Whether to consume HTTP entity on <code>close()</code> invocation
         */
        HttpResponseStream(long contentLength, InputStream content, HttpResponse response, Optional<HttpRequestBase> optionalRequest, boolean consumeEntity) {
            super(content);
            this.contentLength = contentLength;
            this.response = response;
            this.optionalRequest = optionalRequest;
            this.consumeEntity = consumeEntity;
        }

        /**
         * Gets the length of the content, if known.
         *
         * @return The number of bytes of the content, or a negative number if unknown
         */
        public long getContentLength() {
            return contentLength;
        }

        @Override
        public void close() throws IOException {
            try {
                doClose();
            } catch (Exception e) {
                // Ignore
            }
        }

        private void doClose() throws IOException {
            try {
                HttpClients.close(response, consumeEntity);
                if (optionalRequest.isPresent()) {
                    HttpClients.close(optionalRequest.get(), null);
                }
            } finally {
                super.close();
            }
        }
    } // End of class HttpResponseStream

    private static class ContentLengthAwareHttpResponseStream extends HttpResponseStream {

        /** The current position */
        private long pos = 0;

        /** The marked position */
        private long mark = -1;

        /**
         * Initializes a new {@link HttpResponseStream}.
         *
         * @param contentLength The length of the content, which is the number of bytes of the content, or a negative number if unknown
         * @param entityStream The response entity's input stream
         * @param response The HTTP response whose entity stream shall be read from
         * @param consumeEntity Whether to consume HTTP entity on <code>close()</code> invocation
         */
        ContentLengthAwareHttpResponseStream(long contentLength, InputStream entityStream, HttpResponse response, Optional<HttpRequestBase> optionalRequest, boolean consumeEntity) {
            super(contentLength, entityStream, response, optionalRequest, consumeEntity);
        }

        @Override
        public void close() throws IOException {
            try {
                if (0 < contentLength && contentLength > pos) {
                    // Invoke with consumeEntity=false since stream is closed in finally block
                    HttpClients.abort(response);
                } else {
                    HttpClients.close(response, consumeEntity);
                }
                if (optionalRequest.isPresent()) {
                    HttpClients.close(optionalRequest.get(), null);
                }
            } finally {
                Streams.close(in);
            }
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read >= 0) {
                pos++;
            }
            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int readLen = super.read(b, off, len);
            if (readLen > 0) {
                pos += readLen;
            }
            return readLen;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipLen = super.skip(n);
            if (skipLen > 0) {
                pos += skipLen;
            }
            return skipLen;
        }

        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
            mark = pos;
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
            pos = mark;
        }
    } // End of class ContentLengthAwareHttpResponseStream

}
