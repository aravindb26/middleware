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

package com.openexchange.pns.subscription.storage;

/**
 * {@link ClientAndTransport}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public final class ClientAndTransport {

    /** The client identifier */
    public final String client;

    /** The transport identifier */
    public final String transportId;

    private final int hash;

    /**
     * Initializes a new {@link ClientAndTransport}.
     *
     * @param client The client identifier
     * @param transportId The transport identifier
     */
    public ClientAndTransport(String client, String transportId) {
        super();
        this.client = client;
        this.transportId = transportId;
        int prime = 31;
        int result = prime * 1 + ((client == null) ? 0 : client.hashCode());
        result = prime * result + ((transportId == null) ? 0 : transportId.hashCode());
        hash = result;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClientAndTransport)) {
            return false;
        }
        ClientAndTransport other = (ClientAndTransport) obj;
        if (client == null) {
            if (other.client != null) {
                return false;
            }
        } else if (!client.equals(other.client)) {
            return false;
        }
        if (transportId == null) {
            if (other.transportId != null) {
                return false;
            }
        } else if (!transportId.equals(other.transportId)) {
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
        if (transportId != null) {
            builder.append("transportId=").append(transportId);
        }
        builder.append('}');
        return builder.toString();
    }

}