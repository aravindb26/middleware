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

package com.openexchange.push;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link PushExceptionCodes} - Enumeration about all {@link OXException}s.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum PushExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR("An error occurred: %1$s", Category.CATEGORY_ERROR, 1, null),
    /**
     * Missing property: %1$s
     */
    MISSING_PROPERTY("Missing property: %1$s", Category.CATEGORY_ERROR, 2, null),
    /**
     * Invalid property value in property "%1$s": %2$s
     */
    INVALID_PROPERTY("Invalid property value in property \"%1$s\": %2$s", Category.CATEGORY_ERROR, 3, null),
    /**
     * An SQL error occurred: %1$s
     */
    SQL_ERROR("An SQL error occurred: %1$s", Category.CATEGORY_ERROR, 4, null),
    /**
     * Missing master password for mail access.
     */
    MISSING_MASTER_PASSWORD("Missing master password for mail access.", Category.CATEGORY_CONFIGURATION, 5, null),
    /**
     * Missing password for mail access.
     */
    MISSING_PASSWORD("Missing password for mail access.", Category.CATEGORY_CONFIGURATION, 6, null),
    /**
     * Missing login string for mail access.
     */
    MISSING_LOGIN_STRING("Missing login string for mail access.", Category.CATEGORY_CONFIGURATION, 7, null),
    /**
     * Push user %1$s in context %2$s has no push registration.
     */
    NO_PUSH_REGISTRATION("Push user %1$s in context %2$s has no push registration.", Category.CATEGORY_PERMISSION_DENIED, 8, null),
    /**
     * Starting a permanent push listener is not supported.
     */
    PERMANENT_NOT_SUPPORTED("Starting a permanent push listener is not supported.", Category.CATEGORY_CONFIGURATION, 9, null),
    /**
     * Authentication error.
     */
    AUTHENTICATION_ERROR("Authentication error.", Category.CATEGORY_PERMISSION_DENIED, 10, null)
    ;

    /** The exception code prefix */
    public static final String PREFIX = "PUSH";

    private final Category category;
    private final int detailNumber;
    private final String message;
    private final String displayMessage;

    private PushExceptionCodes(final String message, final Category category, final int detailNumber, String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
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
    public String getMessage() {
        return message;
    }

    @Override
    public int getNumber() {
        return detailNumber;
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
