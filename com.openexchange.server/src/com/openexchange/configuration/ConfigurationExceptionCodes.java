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

package com.openexchange.configuration;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Error codes for the configuration exception.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public enum ConfigurationExceptionCodes implements DisplayableOXExceptionCode {
    /** File name for property file is not defined. */
    NO_FILENAME("File name for property file is not defined.", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 1),
    /** File "%1$s" does not exist. */
    FILE_NOT_FOUND("File \"%1$s\" does not exist.", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 2),
    /** File "%1$s" is not readable. */
    NOT_READABLE("File \"%1$s\" is not readable.", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 3),
    /** Cannot read file "%1$s". */
    READ_ERROR("Cannot read file \"%1$s\".", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 4),
    /** Property "%1$s" is not defined. */
    PROPERTY_MISSING("Property \"%1$s\" is not defined.", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 5),
    /** Cannot load class "%1$s". */
    CLASS_NOT_FOUND("Cannot load class \"%1$s\".", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 6),
    /** Invalid configuration: %1$s */
    INVALID_CONFIGURATION("Invalid configuration: %1$s", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 7),
    /** Property %1$s is not an integer */
    PROPERTY_NOT_AN_INTEGER("Property %1$s is not an integer", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 8),
    /** An I/O error occurred: %1$s */
    IO_ERROR("An I/O error occurred: %1$s", ConfigurationExceptionCodes.CONFIG_ERROR_DISPLAY, Category.CATEGORY_CONFIGURATION, 9);

    // Error in server configuration.
    private final static String CONFIG_ERROR_DISPLAY = "Error in server configuration.";

    private final String message;

    private final String displayMessage;

    private final Category category;

    private final int detailNumber;

    private ConfigurationExceptionCodes(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    @Override
    public String getPrefix() {
        return "CFG";
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
