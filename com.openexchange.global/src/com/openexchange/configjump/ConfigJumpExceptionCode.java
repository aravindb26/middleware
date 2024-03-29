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

package com.openexchange.configjump;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Error codes for login exceptions.
 */
public enum ConfigJumpExceptionCode implements DisplayableOXExceptionCode {
    /**
     * Unknown problem: "%s".
     */
    UNKNOWN("Unknown problem: \"%s\".", CATEGORY_ERROR, 1, null),
    /**
     * Too few (%d) login attributes.
     */
    MISSING_ATTRIBUTES("Too few (%d) login attributes.", CATEGORY_USER_INPUT, 2, ConfigJumpExceptionMessage.MISSING_ATTRIBUTES_MSG),
    /**
     * Problem while communicating with external authorization.
     */
    COMMUNICATION("Problem while communicating with external " + "authorization.", CATEGORY_SERVICE_DOWN, 3, null),
    /**
     * Instantiating the class failed.
     */
    INSTANTIATION_FAILED("Instantiating the class failed.", CATEGORY_ERROR, 4, null),
    /**
     * Class %1$s can not be found.
     */
    CLASS_NOT_FOUND("Class %1$s can not be found.", CATEGORY_CONFIGURATION, 5, null),
    /**
     * Missing property %1$s in system.properties.
     */
    MISSING_SETTING("Missing property %1$s in system.properties.", CATEGORY_CONFIGURATION, 6, null),
    /**
     * URL "%s" is malformed.
     */
    MALFORMED_URL("URL \"%s\" is malformed.", CATEGORY_ERROR, 7, null),
    /**
     * Link is not implemented.
     */
    NOT_IMPLEMENTED("Extras link is not implemented.", CATEGORY_CONFIGURATION, 8, null);

    /**
     * Message of the exception.
     */
    final String message;

    /**
     * Category of the exception.
     */
    final Category category;

    /**
     * Detail number of the exception.
     */
    final int number;
    
    private String displayMessage;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private ConfigJumpExceptionCode(final String message, final Category category, final int detailNumber, String displayMessage) {
        this.message = message;
        this.category = category;
        number = detailNumber;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    @Override
    public String getPrefix() {
        return "LGI";
    }

    /**
     * @return the category.
     */
    @Override
    public Category getCategory() {
        return category;
    }

    /**
     * @return the message.
     */
    @Override
    public String getMessage() {
        return message;
    }
    
    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }

    /**
     * @return the number.
     */
    @Override
    public int getNumber() {
        return number;
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
