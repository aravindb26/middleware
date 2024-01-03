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

package com.openexchange.dav.osgi;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.dav.DAVUserAgentParser;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link Services}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class Services {

    /**
     * Initializes a new {@link Services}.
     */
    private Services() {
        super();
    }

    private static final AtomicReference<ServiceLookup> REF = new AtomicReference<ServiceLookup>();

    /**
     * Sets the service lookup.
     *
     * @param serviceLookup The service lookup or <code>null</code>
     */
    public static void setServiceLookup(final ServiceLookup serviceLookup) {
        REF.set(serviceLookup);
    }

    /**
     * Gets the service lookup.
     *
     * @return The service lookup or <code>null</code>
     */
    public static ServiceLookup getServiceLookup() {
        return REF.get();
    }

    /**
     * Get the {@link DAVUserAgentParser}
     *
     * @return The {@link DAVUserAgentParser}
     */
    public static DAVUserAgentParser getDAVUserAgentParser() {
        return REF.get().getService(DAVUserAgentParser.class);
    }

}
