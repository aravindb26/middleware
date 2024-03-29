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

package com.openexchange.share.notification;

import static com.openexchange.share.notification.ShareNotifyExceptionMessages.INSUFFICIENT_PERMISSIONS_MSG;
import static com.openexchange.share.notification.ShareNotifyExceptionMessages.INVALID_MAIL_ADDRESS_MSG;
import static com.openexchange.share.notification.ShareNotifyExceptionMessages.MISSING_MAIL_ADDRESS_MSG;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;


/**
 * {@link ShareNotifyExceptionCodes}
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 * @since v7.8.0
 */
public enum ShareNotifyExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * An unexpected error occurred: %1$s
     * Parameters:
     * <ol>
     *  <li>root cause (message)</li>
     *  <li>recipients name/email address</li>
     * </ol>
     */
    UNEXPECTED_ERROR_FOR_RECIPIENT("An unexpected error occurred: %1$s", ShareNotifyExceptionMessages.UNEXPECTED_ERROR_FOR_RECIPIENT_MSG, Category.CATEGORY_ERROR, 1),

    /**
     * An unexpected error occurred: %1$s
     * Parameters:
     * <ol>
     *  <li>root cause (message)</li>
     * </ol>
     */
    UNEXPECTED_ERROR("An unexpected error occurred: %1$s", ShareNotifyExceptionMessages.UNEXPECTED_ERROR_MSG, Category.CATEGORY_ERROR, 2),

    /**
     * Missing email address for user %1$s (%2$s) in context %3$s
     * Parameters:
     * <ol>
     *  <li>user name</li>
     *  <li>user id</li>
     *  <li>context id</li>
     * </ol>
     */
    MISSING_MAIL_ADDRESS("Missing email address for user \"%1$s\" (%2$s) in context \"%3$s\"", MISSING_MAIL_ADDRESS_MSG, Category.CATEGORY_ERROR, 3),

    /**
     * \"%1$s\" is not a valid email address.
     */
    INVALID_MAIL_ADDRESS("\"%1$s\" is not a valid email address.", INVALID_MAIL_ADDRESS_MSG, Category.CATEGORY_USER_INPUT, 4),

    /**
     * Unknown notification transport: %1$s.
     */
    UNKNOWN_NOTIFICATION_TRANSPORT("Unknown notification transport: %1$s.", null, Category.CATEGORY_ERROR, 5),

    /**
     * <li>You don't have sufficient permissions to send notifications for this share.</li>
     * <li>Insufficient notification permissions for %1$s</li>
     */
    INSUFFICIENT_PERMISSIONS("Insufficient notification permissions for %1$s", INSUFFICIENT_PERMISSIONS_MSG, Category.CATEGORY_PERMISSION_DENIED, 6),

    ;

    public static final String PREFIX = "SHR_NOT";

    private final Category category;
    private final int number;
    private final String message;
    private final String displayMessage;

    private ShareNotifyExceptionCodes(String message, String displayMessage, Category category, int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
        this.number = detailNumber;
        this.category = category;
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
    public String getMessage() {
        return message;
    }

    @Override
    public String getDisplayMessage() {
        return null != displayMessage ? displayMessage : OXExceptionStrings.MESSAGE;
    }

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
    public OXException create(Throwable cause, Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }

}
