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

package com.openexchange.mail;

import static com.openexchange.java.Autoboxing.I;
import java.util.Optional;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link OAuthMailErrorCodes}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.5
 */
public enum OAuthMailErrorCodes implements DisplayableOXExceptionCode {

    /**
     * No account access permitted
     */
    NO_ACCOUNT_ACCESS("No account access permitted", CATEGORY_PERMISSION_DENIED, 1, 403, OAuthMailErrorMessages.NO_ACCOUNT_ACCESS_MSG);

    private static final String HTTP_STATUS = "HTTP_STATUS";
    private static final String PREFIX = "OAUTHMAIL";

    /**
     * The prefix for this error codes.
     */
    public static String prefix() {
        return PREFIX;
    }

    private final Category category;
    private final int detailNumber;
    private final String message;
    private final String displayMessage;
    private final int statuscode;

    /**
     * Initializes a new {@link OAuthMailErrorCodes}.
     *
     * @param message
     * @param category
     * @param detailNumber
     * @param errorCode
     */
    private OAuthMailErrorCodes(String message, Category category, int detailNumber, int errorCode) {
        this(message, category, detailNumber, errorCode, null);
    }

    /**
     * Initializes a new {@link OAuthMailErrorCodes}.
     *
     * @param message
     * @param category
     * @param detailNumber
     * @param statuscode
     * @param displayMessage
     */
    private OAuthMailErrorCodes(String message, Category category, int detailNumber, int statuscode, String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
        this.statuscode = statuscode;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDisplayMessage() {
        return this.displayMessage;
    }

    /**
     * Gets the errorCode
     *
     * @return The errorCode
     */
    private int getHttpStatusCode() {
        return statuscode;
    }

    @Override
    public boolean equals(OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create() {
        return addStatusCode(OXExceptionFactory.getInstance().create(this, new Object[0]));
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(Object... args) {
        return addStatusCode(OXExceptionFactory.getInstance().create(this, (Throwable) null, args));
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(Throwable cause, Object... args) {
        return addStatusCode(OXExceptionFactory.getInstance().create(this, cause, args));
    }

    private OXException addStatusCode(OXException e) {
        e.setArgument(HTTP_STATUS, I(getHttpStatusCode()));
        return e;
    }

    /**
     * Gets the http status code of the given exception in case it is a {@link OAuthMailErrorCodes} error.
     *
     * @param e The exception
     * @return The optional status code
     */
    public static Optional<Integer> getHttpStatus(OXException e) {
        Object result = e.getArgument(HTTP_STATUS);
        return result instanceof Integer ? Optional.of((Integer) result) : Optional.empty();
    }

}
