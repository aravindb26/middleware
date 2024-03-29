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

package com.openexchange.http.grizzly;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;


/**
 * {@link GrizzlyExceptionCode}
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public enum GrizzlyExceptionCode implements DisplayableOXExceptionCode {

    /** The grizzly server could not be started. */
    GRIZZLY_SERVER_NOT_STARTED("The grizzly server could not be started", CATEGORY_ERROR, 1, null),
    /** The following needed service is missing: "%1$s" */
    NEEDED_SERVICE_MISSING("The following needed service is missing: \"%1$s\"", CATEGORY_SERVICE_DOWN, 2, null),
    /** The maximum number of HTTP sessions (%1$n) is exceeded */
    MAX_NUMBER_OF_SESSIONS_REACHED("The maximum number of HTTP sessions (%1$n) is exceeded.", CATEGORY_ERROR, 3, null),
    /** The maximum number of HTTP sessions (%1$n) is exceeded */
    GRIZZLY_FEATURE_MISSING("The following needed feature could not be enabled: \"%1$s\"", CATEGORY_SERVICE_DOWN, 4, null),
    /** File "%1$s" could not be found */
    FILE_NOT_FOUND("File \"%1$s\" could not be found.", Category.CATEGORY_ERROR, 5, null),
    /** An I/O error occurred: %1$s */
    IO_ERROR("An I/O error occurred: %1$s", Category.CATEGORY_ERROR, 6, null),
    ;

    private final String message;
    private final int number;
    private final Category category;
    private String displayMessage;

    private GrizzlyExceptionCode(final String message, final Category category, final int detailNumber, String displayMessage) {
        this.message = message;
        number = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
    }

    @Override
    public boolean equals(OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getPrefix() {
        return "GRIZZLY";
    }

    @Override
    public String getMessage() {
        return message;
    }
    
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
