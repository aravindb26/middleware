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

package com.openexchange.icap.impl.cache;

/**
 * {@link GenericICAPCacheKey}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public class GenericICAPCacheKey {

    private final String server;
    private final int port;
    private final String service;
    private final int hashCode;

    /**
     * Initialises a new {@link GenericICAPCacheKey}.
     */
    public GenericICAPCacheKey(String server, int port, String service) {
        super();
        this.server = server;
        this.port = port;
        this.service = service;
        final int prime = 31;
        int result = 1;
        result = prime * result + port;
        result = prime * result + ((server == null) ? 0 : server.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        this.hashCode = result;
    }

    /**
     * Gets the server
     *
     * @return The server
     */
    public String getServer() {
        return server;
    }

    /**
     * Gets the port
     *
     * @return The port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the service
     *
     * @return The service
     */
    public String getService() {
        return service;
    }

    @Override
    public int hashCode() {
        return hashCode;
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
        GenericICAPCacheKey other = (GenericICAPCacheKey) obj;
        if (port != other.port) {
            return false;
        }
        if (server == null) {
            if (other.server != null) {
                return false;
            }
        } else if (!server.equals(other.server)) {
            return false;
        }
        if (service == null) {
            return other.service == null;
        } else {
            return service.equals(other.service);
        }
    }
}
