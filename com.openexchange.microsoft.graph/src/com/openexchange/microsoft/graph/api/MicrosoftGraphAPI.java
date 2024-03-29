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

package com.openexchange.microsoft.graph.api;

import com.openexchange.microsoft.graph.api.client.MicrosoftGraphRESTClient;
import com.openexchange.server.ServiceLookup;

/**
 * {@link MicrosoftGraphAPI}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public class MicrosoftGraphAPI {

    private final MicrosoftGraphContactsAPI contactsAPI;
    private final MicrosoftGraphOneDriveAPI oneDriveAPI;

    /**
     * Initialises a new {@link MicrosoftGraphAPI}.
     * 
     * @param serviceLookup The service lookup
     */
    public MicrosoftGraphAPI(ServiceLookup serviceLookup) {
        super();
        MicrosoftGraphRESTClient client = new MicrosoftGraphRESTClient(serviceLookup);
        contactsAPI = new MicrosoftGraphContactsAPI(client);
        oneDriveAPI = new MicrosoftGraphOneDriveAPI(client);
    }

    /**
     * Returns the contacts API
     * 
     * @return the contacts API
     */
    public MicrosoftGraphContactsAPI contacts() {
        return contactsAPI;
    }

    /**
     * Gets the oneDriveAPI
     *
     * @return The oneDriveAPI
     */
    public MicrosoftGraphOneDriveAPI drive() {
        return oneDriveAPI;
    }
}
