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

package com.openexchange.imap.util;

/**
 * {@link IMAPRuntimeException} - The runtime exception for IMAP module wrapping a checked exception.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public final class IMAPRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -8718044474021219370L;

    /**
     * Yields an appropriate unchecked runtime exception for given instance of <code>java.lang.Exception</code>.
     *
     * @param e The exception instance to wrap (if adequate)
     * @return The runtime exception
     */
    public static RuntimeException runtimeExceptionFor(Exception e) {
        return e instanceof RuntimeException rte ? rte : (e == null ? null : new IMAPRuntimeException(e));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link IMAPRuntimeException} with the specified cause and a detail message of:
     * <pre>
     *   (cause==null ? null : cause.toString())
     * </pre>
     *
     * @param cause The checked exception to wrap
     * @throws IllegalArgumentException If cause is <code>null</code> or an unchecked exception
     */
    public IMAPRuntimeException(Throwable cause) {
        super(cause);
        if (cause == null) {
            throw new IllegalArgumentException("Cause must not be null");
        }
        if (cause instanceof RuntimeException) {
            throw new IllegalArgumentException("Cause must not be an unchecked exception");
        }
    }

}
