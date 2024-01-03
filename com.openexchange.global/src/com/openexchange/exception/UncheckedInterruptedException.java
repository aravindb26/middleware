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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Objects;

/**
 * {@link UncheckedInterruptedException} - Wraps an {@link InterruptedException} with an unchecked exception.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class UncheckedInterruptedException extends RuntimeException {

    /**
     * Restores current thread's <i>interrupted status</i> and re-throws as unchecked <code>UncheckedInterruptedException</code> instance.
     *
     * @param interruptedException The causing <code>InterruptedException</code>
     * @throws UncheckedInterruptedException The thrown <code>UncheckedInterruptedException</code> wrapping specified <code>InterruptedException</code>
     */
    public static UncheckedInterruptedException restoreInterruptedStatusAndRethrow(InterruptedException interruptedException) {
        if (interruptedException == null) {
            // Nothing to do
            return null;
        }


        // Keep interrupted state...
        Thread.currentThread().interrupt();
        // ... and re-throw as unchecked exception
        return new UncheckedInterruptedException(interruptedException);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final long serialVersionUID = -4076573691304284416L;

    /**
     * Initializes a new {@link UncheckedInterruptedException}.
     *
     * @param message The detail message, can be null
     * @param cause The causing <code>InterruptedException</code>
     *
     * @throws NullPointerException If the cause is {@code null}
     */
    public UncheckedInterruptedException(String message, InterruptedException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    /**
     * Initializes a new {@link UncheckedInterruptedException} with default detail message: <code>"Thread has been interrupted"</code>.
     *
     * @param cause The causing <code>InterruptedException</code>
     * @throws NullPointerException If the cause is <code>null</code>
     */
    public UncheckedInterruptedException(InterruptedException cause) {
        super("Thread has been interrupted", Objects.requireNonNull(cause));
    }

    /**
     * Gets the cause of this exception.
     *
     * @return The <code>InterruptedException</code> which is the cause of this exception.
     */
    @Override
    public InterruptedException getCause() {
        return (InterruptedException) super.getCause();
    }

    /**
     * Called to read the object from a stream.
     *
     * @param s The <code>ObjectInputStream</code> from which data is read
     * @throws IOException If an I/O error occurs
     * @throws ClassNotFoundException If a serialized class cannot be loaded
     * @throws InvalidObjectException If the object is invalid or has a cause that is not an <code>InterruptedException</code>
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        Throwable cause = super.getCause();
        if (!(cause instanceof InterruptedException)) {
            throw new InvalidObjectException("Cause must be an InterruptedException");
        }
    }

}
