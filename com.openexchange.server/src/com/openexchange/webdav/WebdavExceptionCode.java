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

package com.openexchange.webdav;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;
import com.openexchange.groupware.EnumComponent;

public enum WebdavExceptionCode implements DisplayableOXExceptionCode {

    /**
     * Invalid value in element &quot;%1$s&quot;: %2$s.
     */
    INVALID_VALUE("Invalid value in element \"%1$s\": %2$s.", CATEGORY_ERROR, 1, null),
    /**
     * An I/O error occurred.
     */
    IO_ERROR("An I/O error occurred.", CATEGORY_ERROR, 2, null),
    /**
     * Missing field %1$s.
     */
    MISSING_FIELD("Missing field %1$s.", CATEGORY_ERROR, 3, null),
    /**
     * Missing header field %1$s.
     */
    MISSING_HEADER_FIELD("Missing header field %1$s.", CATEGORY_ERROR, 4, null),
    /**
     * Invalid action %1$s.
     */
    INVALID_ACTION("Invalid action %1$s.", CATEGORY_ERROR, 5, null),
    /**
     * %1$s is not a number.
     */
    NOT_A_NUMBER("%1$s is not a number.", CATEGORY_USER_INPUT, 6, WebdavExceptionMessages.NOT_A_NUMBER_MSG),
    /**
     * No principal found: %1$s.
     */
    NO_PRINCIPAL("No principal found: %1$s.", CATEGORY_ERROR, 7, null),
    /**
     * Empty passwords are not allowed.
     */
    EMPTY_PASSWORD("Empty passwords are not allowed.", CATEGORY_USER_INPUT, 8, WebdavExceptionMessages.EMPTY_PASSWORD_MSG),
    /**
     * Unsupported authorization mechanism in "Authorization" header: %1$s.
     */
    UNSUPPORTED_AUTH_MECH("Unsupported authorization mechanism in \"Authorization\" header: %1$s.", CATEGORY_ERROR, 9, null),
    /**
     * Resolving user name "%1$s" failed.
     */
    RESOLVING_USER_NAME_FAILED("Resolving user name \"%1$s\" failed.", CATEGORY_ERROR, 10, null),
    /**
     * Authentication failed for user name: %1$s
     */
    AUTH_FAILED("Authentication failed for user name: %1$s", CATEGORY_ERROR, 11, WebdavExceptionMessages.AUTH_FAILED_MSG),
    /**
     * Unexpected error: %1$s
     */
    UNEXPECTED_ERROR("Unexpected error: %1$s", CATEGORY_ERROR, 11, null);

    private final String message;

    private final int detailNumber;

    private final Category category;
    
    private String displayMessage;

    private WebdavExceptionCode(final String message, final Category category, final int detailNumber, String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    @Override
    public String getPrefix() {
        return EnumComponent.WEBDAV.getAbbreviation();
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
        return displayMessage;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create() {
        return OXExceptionFactory.getInstance().create(this);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Object... args) {
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, args);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }
}
