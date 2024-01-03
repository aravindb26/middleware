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

package com.openexchange.jsieve.export.utils;

import java.io.IOException;
import com.openexchange.mailfilter.internal.CircuitBreakerInfo;
import net.jodah.failsafe.CircuitBreakerOpenException;
import net.jodah.failsafe.FailsafeException;

/**
 * {@link ExceptionHandler}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
final class ExceptionHandler {

    private static final String DENIED_WRITE_MESSAGE = "Denied SIEVE write access since circuit breaker is open.";
    private static final String DENIED_READ_MESSAGE = "Denied SIEVE read access since circuit breaker is open.";

    /**
     * Handles the specified fail-safe exception
     * 
     * @param exception The exception to handle
     * @throws IOException if the cause is an {@link IOException} or an {@link Error}; otherwise
     *             encapsulates the cause into an {@link IOException}
     */
    static IOException handleFailsafeException(FailsafeException exception) throws IOException {
        Throwable failure = exception.getCause();
        if (failure instanceof IOException ioe) {
            throw ioe;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IOException(failure);
    }

    /**
     * Handles the specified circuit breaker open exception.
     * Is called when the circuit breaker denied an access attempt,
     * because it is currently open and not allowing executions to occur.
     * 
     * @param exception The thrown exception when an execution is attempted while a configured CircuitBreaker is open
     * @param info the circuit breaker info
     * @throws IOException re-throws the exception as an {@link IOException}
     */
    static void handleCircuitBreakerOpenWriteException(CircuitBreakerOpenException exception, CircuitBreakerInfo info) throws IOException {
        info.incrementDenials();
        throw new IOException(DENIED_WRITE_MESSAGE, exception);
    }

    /**
     * Handles the specified circuit breaker open exception.
     * Is called when the circuit breaker denied an access attempt,
     * because it is currently open and not allowing executions to occur.
     * 
     * @param exception The thrown exception when an execution is attempted while a configured CircuitBreaker is open
     * @param info the circuit breaker info
     * @return an {@link IOException} for further processing
     */
    static IOException handleCircuitBreakerOpenReadException(CircuitBreakerOpenException exception, CircuitBreakerInfo info) {
        info.incrementDenials();
        return new IOException(DENIED_READ_MESSAGE, exception);
    }
}
