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

package com.openexchange.utils.osgi;


/**
 * A specific class for holding a service class name which is used for filtering in OSGi and the real class you are interested in
 *
 * @author <a href="mailto:dennis.sieben@open-xchange.com">Dennis Sieben</a>
 * @param <T>
 */
public class ServiceClassEntry<T> {

    private final Class<T> clazz;

    private final Class<?> service;

    /**
     * Convenience method if service filter class and service implementation class are the same
     * @param clazz
     */
    public ServiceClassEntry(final Class<T> clazz) {
        super();
        this.clazz = clazz;
        this.service = clazz;
    }

    /**
     * Initializes a new {@link ServiceClassEntry}.
     * @param clazz the service implementation class
     * @param service the service filter class
     */
    public ServiceClassEntry(final Class<T> clazz, final Class<?> service) {
        super();
        this.clazz = clazz;
        this.service = service;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public Class<?> getService() {
        return service;
    }

}
