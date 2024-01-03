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

package com.openexchange.exception;

/**
 * {@link OXRuntimeException} - A special runtime exception that wraps an {@code OXException} instance.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class OXRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -4927113641657577000L;

    private final OXException exception;

    /**
     * Initializes a new {@link OXRuntimeException}.
     *
     * @param cause The {@code OXException} to wrap
     */
    public OXRuntimeException(OXException cause) {
        super(cause);
        if (cause == null) {
            throw new IllegalArgumentException("Causing OXException must not be null");
        }
        this.exception = cause;
    }

    /**
     * Gets the wrapped exception.
     *
     * @return The exception
     */
    public OXException getException() {
        return exception;
    }

}
