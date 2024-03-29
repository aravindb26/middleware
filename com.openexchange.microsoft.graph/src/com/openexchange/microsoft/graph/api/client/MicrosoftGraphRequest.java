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

package com.openexchange.microsoft.graph.api.client;

import com.openexchange.rest.client.v2.AbstractRESTRequest;
import com.openexchange.rest.client.v2.RESTMethod;

/**
 * {@link MicrosoftGraphRequest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public class MicrosoftGraphRequest extends AbstractRESTRequest {

    private String accessToken;

    /**
     * Initialises a new {@link MicrosoftGraphRequest}.
     */
    public MicrosoftGraphRequest(MicrosoftGraphRESTEndPoint endPoint) {
        super(endPoint.getAbsolutePath());
    }

    /**
     * Initialises a new {@link MicrosoftGraphRequest}.
     * 
     * @param method The REST Method
     * @param path The path
     */
    public MicrosoftGraphRequest(RESTMethod method, String path) {
        super(method, path);
    }

    /**
     * Gets the accessToken
     *
     * @return The accessToken
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the accessToken
     *
     * @param accessToken The accessToken to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
