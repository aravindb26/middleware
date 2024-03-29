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

package com.openexchange.tools.servlet.ratelimit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link Key} - A key for whom a certain rate limit is associated/tracked.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Key {

    private static int hashCode(List<String> l) {
        if (l == null) {
            return 0;
        }

        int result = 1;
        for (String str : l) {
            result = 31 * result + (str == null ? 0 : str.hashCode());
        }
        return result;
    }

    private static final String USER_AGENT = "User-Agent";

    // ----------------------------------------------------------------------------------------------------------------------------- //

    private final int remotePort;
    private final String remoteAddr;
    private final String userAgent;
    private final List<String> parts;
    private final int hash;

    /**
     * Initializes a new {@link Key}.
     *
     * @param servletRequest The HTTP request to determine the key for
     * @param userAgent The User-Agent associated with the HTTP request
     */
    public Key(final HttpServletRequest servletRequest) {
        this(servletRequest, servletRequest.getHeader(USER_AGENT));
    }

    /**
     * Initializes a new {@link Key}.
     *
     * @param servletRequest The HTTP request to determine the key for
     * @param userAgent The User-Agent associated with the HTTP request
     */
    public Key(HttpServletRequest servletRequest, String userAgent) {
        this(RateLimiter.considerRemotePort() ? servletRequest.getRemotePort() : 0, servletRequest.getRemoteAddr(), userAgent, extractParts(servletRequest));
    }

    /**
     * Initializes a new {@link Key}.
     *
     * @param servletRequest The HTTP request to determine the key for
     * @param userAgent The User-Agent associated with the HTTP request
     * @param parts Optional key parts to consider
     */
    public Key(HttpServletRequest servletRequest, String userAgent, String... parts) {
        this(RateLimiter.considerRemotePort() ? servletRequest.getRemotePort() : 0, servletRequest.getRemoteAddr(), userAgent,
            null == parts || 0 == parts.length ? null : Arrays.asList(parts));
    }

    Key(int remotePort, String remoteAddr, String userAgent, List<String> parts) {
        super();
        this.remotePort = remotePort;
        this.remoteAddr = remoteAddr;
        this.userAgent = userAgent;
        this.parts = parts;

        int prime = 31;
        int result = 1;
        result = prime * result + ((remoteAddr == null) ? 0 : remoteAddr.hashCode());
        result = prime * result + remotePort;
        result = prime * result + ((userAgent == null) ? 0 : userAgent.hashCode());
        result = prime * result + ((parts == null) ? 0 : hashCode(parts));
        this.hash = result;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Key)) {
            return false;
        }
        final Key other = (Key) obj;
        if (remotePort != other.remotePort) {
            return false;
        }
        if (remoteAddr == null) {
            if (other.remoteAddr != null) {
                return false;
            }
        } else if (!remoteAddr.equals(other.remoteAddr)) {
            return false;
        }
        if (userAgent == null) {
            if (other.userAgent != null) {
                return false;
            }
        } else if (!userAgent.equals(other.userAgent)) {
            return false;
        }
        if (parts == null) {
            if (other.parts != null) {
                return false;
            }
        } else if (!parts.equals(other.parts)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("Key [");
        if (remotePort > 0) {
            builder.append("remotePort=").append(remotePort).append(", ");
        }
        if (remoteAddr != null) {
            builder.append("remoteAddr=").append(remoteAddr).append(", ");
        }
        if (userAgent != null) {
            builder.append("userAgent=").append(userAgent).append(", ");
        }
        if (parts != null) {
            builder.append("parts=").append(parts).append(", ");
        }
        builder.append("hash=").append(hash).append(']');
        return builder.toString();
    }

    private static List<String> extractParts(HttpServletRequest servletRequest) {
        List<KeyPartProvider> keyPartProviders = RateLimiter.keyPartProviders();
        if (null == keyPartProviders || keyPartProviders.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<String>(keyPartProviders.size());
        for (KeyPartProvider keyPartProvider : keyPartProviders) {
            parts.add(keyPartProvider.getValue(servletRequest));
        }
        return parts;
    }

}
