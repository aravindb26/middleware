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

package com.openexchange.pgp.core.exceptions;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link PGPCoreExceptionCodes}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.8.4
 */
public enum PGPCoreExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * No encrypted items found.
     */
    NO_PGP_DATA_FOUND("No encrypted items found.", PGPCoreExceptionMessages.NO_PGP_DATA_FOUND, CATEGORY_ERROR, 1),
    /**
     * The private key for the identity '%1$s' could not be found.
     */
    PRIVATE_KEY_NOT_FOUND("The private key for the identity '%1$s' could not be found.", PGPCoreExceptionMessages.PRIVATE_KEY_NOT_FOUND, CATEGORY_ERROR, 2),
    /**
     * Bad password.
     */
    BAD_PASSWORD("Bad password.", PGPCoreExceptionMessages.BAD_PASSWORD, CATEGORY_USER_INPUT, 3),
    /**
     * No signature items found
     */
    NO_PGP_SIGNATURE_FOUND("No signature items found", PGPCoreExceptionMessages.NO_PGP_SIGNATURE_FOUND, CATEGORY_ERROR, 4),
    /**
     * An I/O error occurred: '%1$s
     */
    IO_EXCEPTION("An I/O error occurred: '%1$s'", PGPCoreExceptionMessages.IO_EXCEPTION, CATEGORY_ERROR, 5),
    /**
     * A PGP error occurred: '%1$s'
     */
    PGP_EXCEPTION("A PGP error occurred: '%1$s'", PGPCoreExceptionMessages.PGP_EXCEPTION, CATEGORY_ERROR, 6),
    ;

    /** The error code prefix for PGP-related errors */
    public static final String PREFIX = "PGP-CORE";

    private final String message;
    private final String displayMessage;
    private final Category category;
    private final int detailNumber;

    /**
     * Initializes a new {@link PGPCoreExceptionCodes}.
     *
     * @param message The error message
     * @param category The category
     * @param number The error number
     *
     */
    private PGPCoreExceptionCodes(String message, Category category, int number) {
        this(message, null, category, number);
    }

    /**
     * Initializes a new {@link PGPCoreExceptionCodes}.
     *
     * @param message The error message
     * @param displayMessage The displayed error message or <code>null</code>
     * @param category The category
     * @param number The error number
     */
    private PGPCoreExceptionCodes(String message, String displayMessage, Category category, int number) {
        this.message = message;
        this.displayMessage = null == displayMessage ? OXExceptionStrings.MESSAGE : displayMessage;
        this.category = category;
        this.detailNumber = number;
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

    @Override
    public boolean equals(OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }
}
