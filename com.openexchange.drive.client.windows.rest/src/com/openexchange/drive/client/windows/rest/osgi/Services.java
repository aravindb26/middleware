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

package com.openexchange.drive.client.windows.rest.osgi;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;


/**
 * {@link Services}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since 8.0.0
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
     * Gets the service of specified type
     *
     * @param clazz The service's class
     * @return The service
     * @throws IllegalStateException If an error occurs while returning the demanded service
     */
    public static <S extends Object> S getService(final Class<? extends S> clazz) {
        final com.openexchange.server.ServiceLookup serviceLookup = REF.get();
        if (null == serviceLookup) {
            throw new IllegalStateException("Missing ServiceLookup instance. Bundle \"com.openexchange.drive.client.windows.rest\" not started?");
        }
        return serviceLookup.getService(clazz);
    }

    /**
     * (Optionally) Gets the service of specified type
     *
     * @param clazz The service's class
     * @return The service or <code>null</code> if absent
     */
    public static <S extends Object> S optService(final Class<? extends S> clazz) {
        ServiceLookup serviceLookup = REF.get();
        return null == serviceLookup ? null : serviceLookup.getOptionalService(clazz);
    }
    
    /**
     * Looks up a required service
     *
     * @param clazz The class of the service
     * @return The service
     * @throws OXException if the service is absent
     */
    public static <S extends Object> S getServiceSafe(Class<? extends S> clazz) throws OXException {
        S service = getService(clazz);
        if (null == service) {
            throw ServiceExceptionCode.absentService(clazz);
        }
        return service;
    }

}
