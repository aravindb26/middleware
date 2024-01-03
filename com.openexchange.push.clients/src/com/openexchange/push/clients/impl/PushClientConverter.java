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
package com.openexchange.push.clients.impl;

import java.util.Optional;
import com.openexchange.push.clients.PushClientProvider;

/**
 * {@link PushClientConverter} - is a {@link PushClientProvider} which casts a generic object into a convenient type
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class PushClientConverter<T> implements PushClientProvider<T> {

    private final PushClientRegistry registry;
    private final Class<? extends T> clazz;

    /**
     * Initializes a new {@link PushClientConverter}.
     *
     * @param clazz The class of the client
     * @param registry The underlying client registry
     */
    public PushClientConverter(Class<? extends T> clazz, PushClientRegistry registry) {
        super();
        this.clazz = clazz;
        this.registry = registry;
    }

    @Override
    public Optional<T> optClient(String id) {
        // @formatter:off
        return registry.getClient(id)
                     .filter(client -> client != null && clazz.isAssignableFrom(client.getClass()))
                     .map(client -> clazz.cast(client));
        // @formatter:on
    }

}
