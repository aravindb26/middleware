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

package com.openexchange.userfeedback.nps.exception;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;

/**
 * 
 * {@link NPSExceptionCodes}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.4
 */
public enum NPSExceptionCodes implements DisplayableOXExceptionCode {

    /** User provided an invalid value for field '%1$s': %2$s */
    INVALID_VALUE("User provided an invalid value for field '%1$s': %2$d", NPSExceptionMessages.INVALID_VALUE_MSG, CATEGORY_USER_INPUT, 1),

    /** User provided an invalid type for feedback '%1$s. */
    INVALID_TYPE("User provided an invalid type for feedback '%1$s'.", NPSExceptionMessages.INVALID_TYPE_MSG, CATEGORY_USER_INPUT, 2),

    /** User provided feedback with missing key: %1$s */
    PARAMETER_MISSING("User provided feedback with missing key: %1$s", NPSExceptionMessages.KEY_MISSING_MSG, CATEGORY_USER_INPUT, 3),

    /** User provided feedback with missing key: %1$s */
    BAD_PARAMETER("User provided feedback with bad formatting: %1$s", NPSExceptionMessages.BAD_VALUE_MSG, CATEGORY_USER_INPUT, 3),

    ;

    /**
     * The error code prefix for capability module.
     */
    public static final String PREFIX = "NPS";

    private final Category category;

    private final int detailNumber;

    private final String displayMessage;

    private final String message;

    private NPSExceptionCodes(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.displayMessage = displayMessage;
        this.category = category;
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
