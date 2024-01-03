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

package net.fortuna.ical4j.httpclient;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.apache.http.protocol.HttpContext;
import com.openexchange.net.HostList;
import com.openexchange.rest.client.httpclient.util.LocationCheckingRedirectStrategy;
import com.openexchange.rest.client.httpclient.util.UriDeniedProtocolException;

/**
 *
 * {@link TimeZoneRegistryRedirectStrategy}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class TimeZoneRegistryRedirectStrategy extends LocationCheckingRedirectStrategy {

    private static final TimeZoneRegistryRedirectStrategy MYINSTANCE = new TimeZoneRegistryRedirectStrategy();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static TimeZoneRegistryRedirectStrategy getInstance() {
        return MYINSTANCE;
    }

    private static final String LOCAL_HOST_NAME;
    private static final String LOCAL_HOST_ADDRESS;

    static {
        // Host name initialization
        String localHostName;
        String localHostAddress;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            localHostName = localHost.getCanonicalHostName();
            localHostAddress = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            localHostName = "localhost";
            localHostAddress = "127.0.0.1";
        }
        LOCAL_HOST_NAME = localHostName;
        LOCAL_HOST_ADDRESS = localHostAddress;
    }

    /** A set containing denied host names and IP addresses */
    public static final HostList DENIED_HOSTS = HostList.of("localhost", "127.0.0.1", LOCAL_HOST_ADDRESS, LOCAL_HOST_NAME);

    // ------------------------------------------------------------------------------------------------------------------------------------

    private TimeZoneRegistryRedirectStrategy() {
        super();
    }

    @Override
    protected boolean isInternalHostAllowed(HttpContext context) {
        return false;
    }

    @Override
    protected void checkLocationUri(URI locationURI, HttpContext context) throws UriDeniedProtocolException {
        if (DENIED_HOSTS.contains(locationURI.getHost())) {
            // Deny redirecting to a local address
            throw new UriDeniedProtocolException("Invalid redirect URI: " + locationURI + ". The URI is local.", locationURI);
        }
    }

}
