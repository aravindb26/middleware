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

package com.openexchange.ajax.helper;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 *
 * The error code enumeration for missing or invalid request parameters.
 */
public enum ParamContainerExceptionCode implements DisplayableOXExceptionCode {

    /**
     * Bad value %1$s in parameter %2$s
     */
    BAD_PARAM_VALUE(CATEGORY_USER_INPUT, 1, ParamContainerExceptionCode.BAD_PARAM_VALUE_MSG, ParamContainerExceptionMessage.BAD_PARAM_VALUE_MSG_DISPLAY),
    /**
     * Missing parameter %1$s
     */
    MISSING_PARAMETER(CATEGORY_ERROR, 2, ParamContainerExceptionCode.MISSING_PARAMETER_MSG, ParamContainerExceptionMessage.MISSING_PARAMETER_MSG_DISPLAY);

    /**
     * Bad value %1$s in parameter %2$s
     */
    private final static String BAD_PARAM_VALUE_MSG = "Bad value %1$s in parameter %2$s";

    /**
     * Missing parameter %1$s
     */
    private final static String MISSING_PARAMETER_MSG = "Missing parameter %1$s";

    /**
     * Message of the exception.
     */
    private final String message;

    /**
     * Category of the exception.
     */
    private final Category category;

    /**
     * Detail number of the exception.
     */
    private final int detailNumber;

    /**
     * Message displayed to the user
     */
    private String displayMessage;

    /**
     * Default constructor.
     * 
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     * @param displayMessage the message to display the user.
     */
    private ParamContainerExceptionCode(final Category category, final int detailNumber, final String message, String displayMessage) {
        this.category = category;
        this.detailNumber = detailNumber;
        this.message = message;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    /**
     * Default constructor.
     * 
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private ParamContainerExceptionCode(final Category category, final int detailNumber, final String message) {
        this(category, detailNumber, message, null);
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
    public Category getCategory() {
        return category;
    }

    @Override
    public String getPrefix() {
        return "REQ_PARAM";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayMessage() {
        return this.displayMessage;
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
