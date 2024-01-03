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

package com.openexchange.share.subscription;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link ShareSubscriptionExceptions}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.5
 */
public enum ShareSubscriptionExceptions implements DisplayableOXExceptionCode {

    /** Unexpected error [%1$s] */
    UNEXPECTED_ERROR("Unexpected error [%1$s]", Category.CATEGORY_ERROR, 1),

    /**
     * <code>The link is missing</code>
     * {@value OXExceptionStrings#BAD_REQUEST}
     */
    MISSING_LINK("The link is missing", Category.CATEGORY_ERROR, 2, OXExceptionStrings.BAD_REQUEST),

    /**
     * <code>The credentials are missing</code>
     * {@value OXExceptionStrings#BAD_REQUEST}
     */
    MISSING_CREDENTIALS("The credentials are missing", Category.CATEGORY_ERROR, 3, OXExceptionStrings.BAD_REQUEST),

    /**
     * <li><code>Unable to find a subscription for \"%1$s\"</code></li>
     * <li>{@value ShareSubscriptionExceptionMessages#MISSING_SUBSCRIPTION_MSG}</li>
     */
    MISSING_SUBSCRIPTION("Unable to find a subscription for \"%1$s\"", Category.CATEGORY_ERROR, 4, ShareSubscriptionExceptionMessages.MISSING_SUBSCRIPTION_MSG),

    /**
     * <li>Unable to interpret the link \"%1$s\".</li>
     * <li>{@value ShareSubscriptionExceptionMessages#NOT_USABLE_MSG}</li>
     */
    NOT_USABLE("Unable to interpret the link \"%1$s\".", Category.CATEGORY_USER_INPUT, 5, ShareSubscriptionExceptionMessages.NOT_USABLE_MSG),

    /**
     * <li>You don't have enough permissions to perform the operation.</li>
     * <li>{@value ShareSubscriptionExceptionMessages#MISSING_PERMISSIONS_MSG}</li>
     */
    MISSING_PERMISSIONS("You don't have enough permissions to perform the operation.", Category.CATEGORY_ERROR, 6, ShareSubscriptionExceptionMessages.MISSING_PERMISSIONS_MSG),

    /**
     * <li>The folder %1$s belongs to a folder tree that is unsubscribed.</li>
     * <li>{@value ShareSubscriptionExceptionMessages#UNSUBSCRIEBED_FOLDER_MSG}</li>
     */
    UNSUBSCRIEBED_FOLDER("The folder %1$s belongs to a folder tree that is unsubscribed.", Category.CATEGORY_WARNING, 7, ShareSubscriptionExceptionMessages.UNSUBSCRIEBED_FOLDER_MSG),

    /**
     * <li>After unsubscribing from \"%1$s\", all folders from the account \"%2$s\" will be removed.</li>
     * <li>{@value ShareSubscriptionExceptionMessages#ACCOUNT_WILL_BE_REMOVED_MSG}</li>
     */
    ACCOUNT_WILL_BE_REMOVED("After unsubscribing from \"%1$s\", the account \"%2$s\" will be removed.", CATEGORY_WARNING, 8, ShareSubscriptionExceptionMessages.ACCOUNT_WILL_BE_REMOVED_MSG),

    ;

    /**
     * The error code prefix for password-change module.
     */
    public static final String PREFIX = "SUSE";

    private final Category category;

    private final int detailNumber;

    private final String message;

    /**
     * Message displayed to the user
     */
    private String displayMessage;

    /**
     * Initializes a new {@link ApiClientExceptions}.
     *
     * @param message The message
     * @param category The category
     * @param detailNumber The exception number
     */
    private ShareSubscriptionExceptions(final String message, final Category category, final int detailNumber) {
        this(message, category, detailNumber, null);
    }

    /**
     * Initializes a new {@link ApiClientExceptions}.
     *
     * @param message The message
     * @param category The category
     * @param detailNumber The exception number
     * @param displayMessage The display message to send to the client
     */
    private ShareSubscriptionExceptions(final String message, final Category category, final int detailNumber, final String displayMessage) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
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
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public String getDisplayMessage() {
        return this.displayMessage;
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
