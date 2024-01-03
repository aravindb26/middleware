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

package com.openexchange.pns.transport.webhooks.util;

/**
 * {@link NoValidUriAvailableException} - Thrown in case there is no valid URI available in neither configured nor client-specified Webhook information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class NoValidUriAvailableException extends Exception {

    private static final long serialVersionUID = -5289319477458542295L;

    /**
     * Initializes a new {@link NoValidUriAvailableException}.
     */
    public NoValidUriAvailableException() {
        super();
    }

    /**
     * Initializes a new {@link NoValidUriAvailableException}.
     *
     * @param message The message
     */
    public NoValidUriAvailableException(String message) {
        super(message);
    }

    /**
     * Initializes a new {@link NoValidUriAvailableException}.
     *
     * @param cause The cause
     */
    public NoValidUriAvailableException(Throwable cause) {
        super(cause);
    }

    /**
     * Initializes a new {@link NoValidUriAvailableException}.
     *
     * @param message The message
     * @param cause The cause
     */
    public NoValidUriAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
