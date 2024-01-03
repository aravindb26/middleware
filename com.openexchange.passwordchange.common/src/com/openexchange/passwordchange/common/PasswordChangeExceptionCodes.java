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

package com.openexchange.passwordchange.common;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link PasswordChangeExceptionCodes} - Enumeration of all {@link OXException}s known in password-change module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum PasswordChangeExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR(PasswordChangeExceptionCodes.UNEXPECTED_ERROR_MSG, CATEGORY_ERROR, 1),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR(PasswordChangeExceptionCodes.IO_ERROR_MSG, CATEGORY_ERROR, 2),
    
    /**
     * <li>No service found to perform a password change
     * <li>There is no service available that can perform the password change
     */
    MISSING_SERVICE(PasswordChangeExceptionCodes.MISSING_SERVICE_MSG, CATEGORY_ERROR, 3)
    
    ;

    /**
     * The error code prefix for password-change module.
     */
    public static final String PREFIX = "PWCHANGE";

    private static final String UNEXPECTED_ERROR_MSG = "An error occurred: %1$s";

    private static final String IO_ERROR_MSG = "An I/O error occurred: %1$s";
    
    private static final String MISSING_SERVICE_MSG = "There is no service available that can perform the password change";

    private final Category category;

    private final int detailNumber;

    private final String message;

    /**
     * Message displayed to the user
     */
    private String displayMessage;

    /**
     * Initializes a new {@link PasswordChangeExceptionCodes}.
     * 
     * @param message
     * @param category
     * @param detailNumber
     */
    private PasswordChangeExceptionCodes(final String message, final Category category, final int detailNumber) {
        this(message, category, detailNumber, null);
    }

    /**
     * Initializes a new {@link PasswordChangeExceptionCodes}.
     * 
     * @param message
     * @param category
     * @param detailNumber
     * @param displayMessage
     */
    private PasswordChangeExceptionCodes(final String message, final Category category, final int detailNumber, final String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
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
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayMessage() {
        return this.displayMessage;
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
