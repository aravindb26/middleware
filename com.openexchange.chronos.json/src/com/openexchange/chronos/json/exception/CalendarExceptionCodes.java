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

package com.openexchange.chronos.json.exception;

import static com.openexchange.exception.OXExceptionStrings.MESSAGE;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;

/**
 * {@link CalendarExceptionCodes}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public enum CalendarExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * The appointment %s couldn't be deleted: %s
     */
    ERROR_DELETING_EVENT("The appointment %s couldn't be deleted: %s", CalendarExceptionMessages.ERROR_DELETING_EVENT_MSG, Category.CATEGORY_ERROR, 1),

    /**
     * Multiple appointments couldn't be deleted.
     */
    ERROR_DELETING_EVENTS("Multiple appointments couldn't be deleted.", CalendarExceptionMessages.ERROR_DELETING_EVENTS_MSG, Category.CATEGORY_ERROR, 2),

    /**
     * Unable to add reminders: %s
     */
    UNABLE_TO_ADD_ALARMS("Unable to add reminders: %s", CalendarExceptionMessages.UNABLE_TO_ADD_ALARMS_MSG, Category.CATEGORY_ERROR, 3),
    /**
     * The content-id '%1$s' refers to a non-existing attachment in the body part.
     */
    MISSING_BODY_PART_ATTACHMENT_REFERENCE("The content-id '%1$s' refers to a non-existing attachment in the body part.", CalendarExceptionMessages.MISSING_BODY_PART_ATTACHMENT_REFERENCE_MSG, Category.CATEGORY_ERROR, 4),
    /**
     * <li>The metadata of the attachment '%1$s' does not have a content-id.</li>
     * <li>The attachment's metadata does not have a content-id.</li>
     */
    MISSING_METADATA_ATTACHMENT_REFERENCE("The attachment's metadata does not have a content-id.", CalendarExceptionMessages.MISSING_METADATA_ATTACHMENT_REFERENCE_MSG, Category.CATEGORY_ERROR, 4),
    /**
     * An unexpected error occurred: %1$s
     */
    UNEXPECTED_ERROR("An unexpected error occurred: %1$s", null, CATEGORY_ERROR, 5),
    /**
     * Unable to add drive attachment with id '%s'
     */
    UNABLE_TO_ADD_DRIVE_ATTACHMENT("Unable to add drive attachment with id '%s'", CalendarExceptionMessages.UNABLE_TO_ADD_DRIVE_ATTACHMENT_MSG, Category.CATEGORY_ERROR, 6),
    ;

    public static final String PREFIX = "CAL_JSON".intern();

    private final String message;
    private final String displayMessage;
    private final Category category;
    private final int number;

    private CalendarExceptionCodes(String message, String displayMessage, Category category, int number) {
        this.message = message;
        this.displayMessage = null != displayMessage ? displayMessage : MESSAGE;
        this.category = category;
        this.number = number;
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
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public boolean equals(OXException e) {
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
