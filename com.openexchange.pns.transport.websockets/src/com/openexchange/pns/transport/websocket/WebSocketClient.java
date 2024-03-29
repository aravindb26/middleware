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

package com.openexchange.pns.transport.websocket;

import java.util.Collection;
import com.openexchange.pns.Interest;

/**
 * {@link WebSocketClient} - Provides the client identifier and the associated path filter expression to associate a Web Socket with that client.
 * <p>
 * {@link #equals(Object) equals()} method only considers client identifier.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public final class WebSocketClient implements Comparable<WebSocketClient> {

    private final String client;
    private final String pathFilter;
    private final Collection<Interest> interests;
    private Integer hash;

    /**
     * Initializes a new {@link WebSocketClient}.
     *
     * @param client The identifier of the client; e.g. <code>"open-xchange-appsuite"</code>
     * @param pathFilter The path filter expression that applies to the client; e.g. <code>"/socket.io/*"</code>
     * @param interests The interests for this client
     */
    public WebSocketClient(String client, String pathFilter, Collection<Interest> interests) {
        super();
        this.client = client;
        this.pathFilter = pathFilter;
        this.interests = interests;
    }

    /**
     * Checks if associated client is interested in given topic.
     *
     * @param topic The topic identifier; e.g <code>"ox:mail:new"</code>
     * @return <code>true</code> if interested; otherwise <code>false</code>
     */
    public boolean isInterestedIn(String topic) {
        for (Interest interest : interests) {
            if (interest.isInterestedIn(topic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the client identifier; e.g. <code>"open-xchange-appsuite"</code>
     *
     * @return The client identifier
     */
    public String getClient() {
        return client;
    }

    /**
     * Gets the path filter expression that applies to the client; e.g. <code>"/socket.io/*"</code>
     *
     * @return The path filter expression
     */
    public String getPathFilter() {
        return pathFilter;
    }

    @Override
    public int hashCode() {
        Integer tmp = this.hash;
        if (null == tmp) {
            // No concurrency here. In worst case each thread computes its own hash code
            int prime = 31;
            int result = 1;
            result = prime * result + ((client == null) ? 0 : client.hashCode());
            tmp = Integer.valueOf(result);
            this.hash = tmp;
        }
        return tmp.intValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (WebSocketClient.class != obj.getClass()) {
            return false;
        }
        WebSocketClient other = (WebSocketClient) obj;
        if (client == null) {
            if (other.client != null) {
                return false;
            }
        } else if (!client.equals(other.client)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        builder.append('{');
        if (client != null) {
            builder.append("client=").append(client).append(", ");
        }
        if (pathFilter != null) {
            builder.append("pathFilter=").append(pathFilter).append(", ");
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public int compareTo(WebSocketClient o) {
        int c = client.compareTo(o.client);
        return c == 0 ? pathFilter.compareTo(o.pathFilter) : c;
    }

}
