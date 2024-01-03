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
 * {@link AbstractHttpBasicConfig}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public abstract class AbstractHttpBasicConfig implements HttpBasicConfig {

    protected int socketReadTimeout;
    protected int connectTimeout;
    protected int connectionRequestTimeout;
    protected int maxTotalConnections;
    protected int maxConnectionsPerRoute;
    protected int keepAliveDuration;
    protected int keepAliveMonitorInterval;
    protected int socketBufferSize;
    protected int maxLineLength;
    protected int maxHeaderCount;
    protected int hardConnectTimeout;
    protected int hardReadTimeout;
    protected SSLConnectionSocketFactory customSslConnectionSocketFactory;

    /**
     * Initializes a new {@link AbstractHttpBasicConfig}.
     */
    protected AbstractHttpBasicConfig() {
        super();
    }

    @Override
    public int getSocketReadTimeout() {
        return socketReadTimeout;
    }

    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    @Override
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    @Override
    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    @Override
    public int getKeepAliveDuration() {
        return keepAliveDuration;
    }

    @Override
    public int getKeepAliveMonitorInterval() {
        return keepAliveMonitorInterval;
    }

    @Override
    public int getSocketBufferSize() {
        return socketBufferSize;
    }

    @Override
    public int getMaxHeaderCount() {
        return maxHeaderCount;
    }

    @Override
    public int getMaxLineLength() {
        return maxLineLength;
    }

    @Override
    public int getHardConnectTimeout() {
        return hardConnectTimeout;
    }

    @Override
    public int getHardReadTimeout() {
        return hardReadTimeout;
    }

    @Override
    public SSLConnectionSocketFactory getCustomSSLConnectionSocketFactory() {
        return customSslConnectionSocketFactory;
    }

    @Override
    public int hashCode() {
        final int prime = 131;
        int result = 1;
        result = prime * result + connectionRequestTimeout;
        result = prime * result + connectTimeout;
        result = prime * result + keepAliveDuration;
        result = prime * result + keepAliveMonitorInterval;
        result = prime * result + maxConnectionsPerRoute;
        result = prime * result + maxTotalConnections;
        result = prime * result + socketBufferSize;
        result = prime * result + socketReadTimeout;
        result = prime * result + maxHeaderCount;
        result = prime * result + maxLineLength;
        result = prime * result + hardConnectTimeout;
        result = prime * result + hardReadTimeout;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HttpBasicConfig)) {
            return false;
        }
        HttpBasicConfig other = (HttpBasicConfig) obj;
        if (connectionRequestTimeout != other.getConnectionRequestTimeout()) {
            return false;
        }
        if (connectTimeout != other.getConnectTimeout()) {
            return false;
        }
        if (keepAliveDuration != other.getKeepAliveDuration()) {
            return false;
        }
        if (keepAliveMonitorInterval != other.getKeepAliveMonitorInterval()) {
            return false;
        }
        if (maxConnectionsPerRoute != other.getMaxConnectionsPerRoute()) {
            return false;
        }
        if (maxTotalConnections != other.getMaxTotalConnections()) {
            return false;
        }
        if (socketBufferSize != other.getSocketBufferSize()) {
            return false;
        }
        if (socketReadTimeout != other.getSocketReadTimeout()) {
            return false;
        }
        if (maxHeaderCount != other.getMaxHeaderCount()) {
            return false;
        }
        if (maxLineLength != other.getMaxLineLength()) {
            return false;
        }
        if (hardConnectTimeout != other.getHardConnectTimeout()) {
            return false;
        }
        if (hardReadTimeout != other.getHardReadTimeout()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        //@formatter:off
        return "UnmodifiableHttpBasicConfig [socketReadTimeout=" + socketReadTimeout + ", connectTimeout=" + connectTimeout + ", connectionRequestTimeout=" + connectionRequestTimeout
            + ", maxTotalConnections=" + maxTotalConnections + ", maxConnectionsPerRoute=" + maxConnectionsPerRoute + ", keepAliveDuration=" + keepAliveDuration
            + ", keepAliveMonitorInterval=" + keepAliveMonitorInterval + ", socketBufferSize=" + socketBufferSize + ", maxHeaderCount=" + maxHeaderCount
            + ", maxLineLength=" + maxLineLength + ", hardConnectTimeout=" + hardConnectTimeout +", hardReadTimeout=" + hardReadTimeout + "]";
        //@formatter:on
    }

}
