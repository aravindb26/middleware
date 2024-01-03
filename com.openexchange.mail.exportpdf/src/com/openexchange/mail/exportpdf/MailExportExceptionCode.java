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

package com.openexchange.mail.exportpdf;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link MailExportExceptionCode}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public enum MailExportExceptionCode implements DisplayableOXExceptionCode {

    /**
     * Unexpected error: %1$s
     */
    UNEXPECTED_ERROR("Unexpected error: %1$s", CATEGORY_ERROR),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", CATEGORY_ERROR),
    /**
     * <li>The server is busy at the moment. Try again later.</li>
     * <li>Too many concurrent exports '%1$s'. Try increasing the value of the property '%2$s'</li>
     */
    TOO_MANY_CONCURRENT_EXPORTS("Too many concurrent exports '%1$s'. Try increasing the value of the property '%2$s'", MailExportExceptionMessages.TOO_MANY_CONCURRENT_EXPORTS, CATEGORY_ERROR),
    /**
     * Unable to load font %1$s: '%2$s'
     */
    FONT_LOADING_ERROR("Unable to load font %1$s: '%2$s'", CATEGORY_ERROR),
    /**
     * The operation could not be completed due to missing capabilities.
     */
    MISSING_CAPABILITIES("The operation could not be completed due to missing capabilities.", CATEGORY_PERMISSION_DENIED)
    ;

    private static final String PREFIX = "MAIL-EXPORT";

    private final String message;
    private final Category category;
    private final String displayMessage;

    /**
     * Initializes a new {@link MailExportExceptionCode}.
     *
     * @param message The message
     * @param category The error category
     */
    MailExportExceptionCode(String message, Category category) {
        this(message, null, category);
    }

    /**
     * Initializes a new {@link MailExportExceptionCode}.
     *
     * @param message The message
     * @param displayMessage The display name
     * @param category The error category
     */
    MailExportExceptionCode(final String message, String displayMessage, Category category) {
        this.message = message;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
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
        return ordinal() + 1;
    }

    @Override
    public String getMessage() {
        return message;
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
        return displayMessage;
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
        return OXExceptionFactory.getInstance().create(this, null, args);
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
