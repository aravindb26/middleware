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

package com.openexchange.authentication;

import java.util.Optional;
import com.openexchange.exception.OXException;

/**
 * {@link AuthenticationResult} - Represents a result of an {@link AuthenticationRequest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class AuthenticationResult {

    /** An enumeration of possible authentication statuses */
    public static enum Status {
        /** Authentication attempt succeeded */
        SUCCESS,
        /** Authentication attempt failed */
        FAILED,
        ;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a successful authentication result.
     * <p>
     * <b>Note</b> that result can still end in failure, if {@link Authenticated} object is <code>null</code>
     *
     * @param authenticated The authentication information for the authenticated user
     * @return Successful authentication result
     */
    public static AuthenticationResult success(Authenticated authenticated) {
        return new AuthenticationResult(authenticated);
    }

    /**
     * Creates a "failed" authentication result
     *
     * @param e Exception occured during authentication attempt
     * @return Failed authentication result
     */
    public static AuthenticationResult failed(OXException e) {
        return new AuthenticationResult(e);
    }

    /**
     * Creates a "failed" authentication result without error
     *
     * @return Failed authentication result
     */
    public static AuthenticationResult failed() {
        return new AuthenticationResult((OXException) null);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Optional<Authenticated> authenticated;
    private final Optional<OXException> exception;
    private final Status status;

    private AuthenticationResult(Authenticated authenticated) {
        super();
        this.authenticated = Optional.ofNullable(authenticated);
        this.status = null != authenticated ? Status.SUCCESS : Status.FAILED;
        this.exception = Optional.empty();
    }

    private AuthenticationResult(OXException exception) {
        super();
        this.exception = Optional.ofNullable(exception);
        this.authenticated = Optional.empty();
        this.status = Status.FAILED;
    }

    /**
     * Gets the {@link Authenticated} object from successful authentication results
     *
     * @return The optional {@link Authenticated} object containing information about the authenticated user
     * @deprecated Use {@link #optAuthenticated()} instead
     */
    @Deprecated
    public Optional<Authenticated> getAuthenticated() {
        return authenticated;
    }

    /**
     * Gets the {@link Authenticated} object from successful authentication results
     *
     * @return The optional {@link Authenticated} object containing information about the authenticated user
     */
    public Optional<Authenticated> optAuthenticated() {
        return authenticated;
    }

    /**
     * Gets the {@link OXException} object from failed authentication results
     *
     * @return The optional {@link OXException} object containing information about the failed authentication attempt
     * @deprecated Use {@link #optException()} instead
     */
    @Deprecated
    public Optional<OXException> getException() {
        return exception;
    }

    /**
     * Gets the {@link OXException} object from failed authentication results
     *
     * @return The optional {@link OXException} object containing information about the failed authentication attempt
     */
    public Optional<OXException> optException() {
        return exception;
    }

    /**
     * Gets the authentication result's status
     *
     * @return The status
     */
    public Status getStatus() {
        return status;
    }

}
