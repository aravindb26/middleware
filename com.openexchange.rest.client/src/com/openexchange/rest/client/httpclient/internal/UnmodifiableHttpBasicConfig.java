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

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;

/**
 * {@link UnmodifiableHttpBasicConfig} - Unmodifiable version of the {@link HttpBasicConfig}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class UnmodifiableHttpBasicConfig extends AbstractHttpBasicConfig {

    /**
     * Initializes a new {@link UnmodifiableHttpBasicConfig}.
     *
     * @param config The configuration to clone
     */
    public UnmodifiableHttpBasicConfig(HttpBasicConfig config) {
        super();
        this.socketReadTimeout = config.getSocketReadTimeout();
        this.connectTimeout = config.getConnectTimeout();
        this.connectionRequestTimeout = config.getConnectionRequestTimeout();
        this.maxTotalConnections = config.getMaxTotalConnections();
        this.maxConnectionsPerRoute = config.getMaxConnectionsPerRoute();
        this.keepAliveDuration = config.getKeepAliveDuration();
        this.keepAliveMonitorInterval = config.getKeepAliveMonitorInterval();
        this.socketBufferSize = config.getSocketBufferSize();
        this.maxHeaderCount = config.getMaxHeaderCount();
        this.maxLineLength = config.getMaxLineLength();
        this.hardConnectTimeout = config.getHardConnectTimeout();
        this.hardReadTimeout = config.getHardReadTimeout();
    }

    @Override
    public HttpBasicConfig setSocketReadTimeout(int socketReadTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setConnectTimeout(int connectTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setConnectionRequestTimeout(int connectionRequestTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setMaxTotalConnections(int maxTotalConnections) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setKeepAliveDuration(int keepAliveDuration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setKeepAliveMonitorInterval(int keepAliveMonitorInterval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setSocketBufferSize(int socketBufferSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setMaxHeaderCount(int maxHeaderCount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setMaxLineLength(int maxLineLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setHardConnectTimeout(int hardConnectTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setHardReadTimeout(int hardReadTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpBasicConfig setCustomSSLConnectionSocketFactory(SSLConnectionSocketFactory sslConnectionSocketFactory) {
        throw new UnsupportedOperationException();
    }
}
