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

package com.openexchange.redis.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.openexchange.java.Strings;

/**
 * {@link RedisHost} - Represents a Redis host consisting of host and port.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisHost {

    /** The default port for Redis end-point */
    public static final int PORT_DEFAULT = 6379;

    /** The default port for Redis Sentinel end-point */
    public static final int PORT_DEFAULT_SENTINEL = 26379;

    /**
     * Parses given host list to Redis hosts.
     *
     * @param hostList The host list to parse
     * @param defaultPort The default port to assume in case not specified for a host entry
     * @return The Redis hosts
     */
    public static List<RedisHost> parse(String hostList, int defaultPort) {
        if (Strings.isEmpty(hostList)) {
            return Collections.emptyList();
        }

        String[] entries = Strings.splitByComma(hostList);
        List<RedisHost> hosts = new ArrayList<>(entries.length);
        for (String entry : entries) {
            if (Strings.isNotEmpty(entry)) {
                entry = entry.trim();
                int pos = entry.indexOf(':');
                if (pos <= 0) {
                    hosts.add(new RedisHost(entry, defaultPort));
                } else {
                    int port = Strings.getUnsignedInt(entry.substring(pos + 1));
                    if (port < 0) {
                        port = defaultPort;
                    }
                    entry = entry.substring(0, pos);
                    hosts.add(new RedisHost(entry, port));
                }
            }
        }
        return hosts;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    private final String host;
    private final int port;
    private int hash;

    /**
     * Initializes a new {@link RedisHost}.
     *
     * @param host The host name or textual representation of the IP address
     * @param port The port
     */
    public RedisHost(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    /**
     * Gets the host
     *
     * @return The host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port
     *
     * @return The port
     */
    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            int result = 31 * 1 + (host == null ? 0 : host.hashCode());
            result = 31 * result + (port);
            h = result;
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RedisHost other = (RedisHost) obj;
        return port == other.port && Objects.equals(host, other.host);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (host != null) {
            builder.append("host=").append(host).append(", ");
        }
        builder.append("port=").append(port).append(']');
        return builder.toString();
    }

}
