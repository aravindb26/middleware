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

package com.openexchange.ajax.requesthandler.annotation.restricted;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link RestrictedActionExceptionCodes}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public enum RestrictedActionExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>An unexpected error occurred: %1$s</li>
     */
    UNEXPECTED_ERROR("An unexpected error occurred: %1$s", CATEGORY_ERROR, 1),
    /**
     * <li>This account isn't authorized to perform this action due to insufficient scopes.</li>
     * <li>Missing scope: %1$s</li>
     */
    INSUFFICIENT_SCOPES("Missing scope: %1$s", RestrictedActionExceptionMessages.INSUFFICIENT_SCOPES, Category.CATEGORY_PERMISSION_DENIED, 2),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>Unknown restricted session type</li>
     */
    UNKNOWN_SESSION_TYPE("Unknown restricted session type", Category.CATEGORY_ERROR, 3),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>OAuth Access is missing from the session</li>
     */
    OAUTH_ACCESS_MISSING("OAuth Access is missing from the session", Category.CATEGORY_ERROR, 4),
    /**
     * <li>This account isn't authorized to perform this action.</li>
     * <li>Access denied: %1$s</li>
     */
    ACCESS_DENIED("Access denied: %1$s", RestrictedActionExceptionMessages.ACCESS_DENIED, Category.CATEGORY_PERMISSION_DENIED, 5),
    /**
     * <li>An error occurred inside the server which prevented it from fulfilling the request.</li>
     * <li>No scopes are defined for action: %1$s</li>
     */
    NO_SCOPES("No scopes are defined for action: %1$s", CATEGORY_ERROR, 6),
    ;

    private static final String PREFIX = "RA";
    private final String message;
    private final String displayMessage;
    private final Category category;
    private final int number;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param category category.fffff
     * @param number detail number.
     */
    private RestrictedActionExceptionCodes(String message, Category category, int number) {
        this(message, null, category, number);
    }

    private RestrictedActionExceptionCodes(String message, String displayMessage, Category category, int number) {
        this.message = message;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
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