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

package com.openexchange.pns.transport.gcm;


/**
 * {@link GcmOptionsPerClient} - A pair of client identifier and associated GCM options.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class GcmOptionsPerClient {

    private final String client;
    private final GcmOptions options;

    /**
     * Initializes a new {@link GcmOptionsPerClient}.
     *
     * @param client The client
     * @param options The associated GCM options
     */
    public GcmOptionsPerClient(String client, GcmOptions options) {
        super();
        this.client = client;
        this.options = options;
    }

    /**
     * Gets the client
     *
     * @return The client
     */
    public String getClient() {
        return client;
    }

    /**
     * Gets the options
     *
     * @return The options
     */
    public GcmOptions getOptions() {
        return options;
    }

}
