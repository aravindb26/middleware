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

package com.openexchange.groupware.update.internal;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Exception codes for the {@link OXException}.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public enum SchemaExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * Update conflict detected. Another process is currently updating schema %1$s.
     */
    ALREADY_LOCKED("Update conflict detected. Another process is currently updating schema %1$s.",
        SchemaExceptionMessages.DATABASE_ERROR_DISPLAY,Category.CATEGORY_PERMISSION_DENIED, 3),

    /**
     * Locking schema %1$s failed. Lock information could not be written to database.
     */
    LOCK_FAILED("Table update failed. Schema %1$s could not be locked.", SchemaExceptionMessages.DATABASE_ERROR_DISPLAY,
        Category.CATEGORY_ERROR, 4),

    /**
     * Update conflict detected. Schema %1$s is not marked as locked.
     */
    UPDATE_CONFLICT("Update conflict detected. Schema %1$s is not marked as locked.", SchemaExceptionMessages.DATABASE_ERROR_DISPLAY,
        Category.CATEGORY_ERROR, 5),

    /**
     * Schema %1$s could not be unlocked. Lock information could no be removed from database.
     */
    UNLOCK_FAILED("Schema %1$s could not be unlocked. Lock information could no be removed from database.",
        SchemaExceptionMessages.DATABASE_ERROR_DISPLAY, Category.CATEGORY_ERROR, 6),

    /**
     * A SQL problem occurred: %1$s.
     */
    SQL_PROBLEM("A SQL problem occurred: %1$s.", SchemaExceptionMessages.DATABASE_ERROR_DISPLAY, Category.CATEGORY_ERROR, 7),

    /**
     * Cannot get database connection.
     */
    DATABASE_DOWN("Cannot get database connection.", SchemaExceptionMessages.DATABASE_DOWN_DISPLAY, Category.CATEGORY_SERVICE_DOWN, 8),

    /**
     * Processed a wrong number of rows in database. Expected %1$d rows but worked on %2$d rows.
     */
    WRONG_ROW_COUNT("Processed a wrong number of rows in database. Expected %1$d rows but worked on %2$d rows.",
        SchemaExceptionMessages.DATABASE_ERROR_DISPLAY, Category.CATEGORY_ERROR, 9),

    /**
     * Failed executing updating task "%1$s" on schema %2$s.
     */
    TASK_FAILED("Failed executing updating task \"%1$s\" on schema %2$s.",
        SchemaExceptionMessages.DATABASE_ERROR_DISPLAY,Category.CATEGORY_PERMISSION_DENIED, 10),

    ;

    /**
     * Message of the exception.
     */
    final String message;

    final String displayMessage;

    /**
     * Category of the exception.
     */
    final Category category;

    /**
     * Detail number of the exception.
     */
    final int number;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param category category.
     * @param number detail number.
     */
    private SchemaExceptionCodes(final String message, final String displayMessage, final Category category, final int number) {
        this.message = message;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
        this.category = category;
        this.number = number;
    }

    @Override
    public String getPrefix() {
        return "UPD";
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public int getNumber() {
        return number;
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
