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

package com.openexchange.session.oauth;

/**
 * {@link RefreshResult} - Represents the result for a token refresh attempt.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class RefreshResult {

    /** The success reason for a token refresh attempt */
    public static enum SuccessReason {
        /**
         * Access token is not expired yet
         */
        NON_EXPIRED,
        /**
         * Access token was successfully refreshed
         */
        REFRESHED,
        /**
         * Access token was refreshed by another thread in the meantime
         */
        CONCURRENT_REFRESH;
    }

    /** The fail reason for a token refresh attempt */
    public static enum FailReason {
        /**
         * Session contains an invalid refresh token
         */
        INVALID_REFRESH_TOKEN,
        /**
         * Lock timeout was exceeded
         */
        LOCK_TIMEOUT,
        /**
         * A temporary error occurred, retry later
         */
        TEMPORARY_ERROR,
        /**
         * A permanent error occurred, retry will not help to resolve this
         */
        PERMANENT_ERROR;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a success refresh result with given success reason.
     *
     * @param reason The success reason
     * @return The result
     */
    public static RefreshResult success(SuccessReason reason) {
        return new RefreshResult(reason);
    }

    /**
     * Creates a fail refresh result with given fail reason.
     *
     * @param reason The fail reason
     * @param description The optional failure description
     * @return The reason
     */
    public static RefreshResult fail(FailReason reason, String description) {
        return fail(reason, description, null);
    }

    /**
     * Creates a fail refresh result with given fail reason.
     *
     * @param reason The fail reason
     * @param description The optional failure description
     * @param t The option exception causing the failure
     * @return The reason
     */
    public static RefreshResult fail(FailReason reason, String description, Throwable t) {
        return new RefreshResult(reason, description == null ? "Unknown" : description, t);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final SuccessReason successReason;
    private final FailReason failReason;
    private final String errorDesc;
    private final Throwable exception;

    private RefreshResult(SuccessReason successReason) {
        super();
        this.successReason = successReason;
        this.failReason = null;
        this.errorDesc = null;
        this.exception = null;
    }

    private RefreshResult(FailReason failReason, String errorDesc, Throwable exception) {
        super();
        this.successReason = null;
        this.failReason = failReason;
        this.errorDesc = errorDesc;
        this.exception = exception;
    }

    public boolean isSuccess() {
        return successReason != null;
    }

    public boolean isFail() {
        return failReason != null;
    }

    public SuccessReason getSuccessReason() {
        return successReason;
    }

    public FailReason getFailReason() {
        return failReason;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public boolean hasException() {
        return exception != null;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        builder.append('[');
        if (successReason != null) {
            builder.append("successReason=").append(successReason).append(", ");
        }
        if (failReason != null) {
            builder.append("failReason=").append(failReason).append(", ");
        }
        if (errorDesc != null) {
            builder.append("errorDesc=").append(errorDesc).append(", ");
        }
        if (exception != null) {
            builder.append("exception=").append(exception);
        }
        builder.append(']');
        return builder.toString();
    }

}
