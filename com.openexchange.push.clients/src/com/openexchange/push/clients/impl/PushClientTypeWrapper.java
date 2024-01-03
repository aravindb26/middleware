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


/**
 * {@link PushClientTypeWrapper} is a wrapper for push clients and their respective type
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class PushClientTypeWrapper {

    private final Object client;
    private final String type;

    /**
     * Initializes a new {@link PushClientTypeWrapper}.
     *
     * @param client The client
     * @param type The type of the client
     */
    public PushClientTypeWrapper(Object client, String type) {
        super();
        this.client = client;
        this.type = type;
    }

    /**
     * Gets the client
     *
     * @return
     */
    public Object getClient() {
        return client;
    }

    /**
     * Gets the type of the client
     *
     * @return
     */
    public String getType() {
        return type;
    }

}
