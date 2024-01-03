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

package com.openexchange.group;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Error codes for the group module.
 */
public enum GroupExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * A database connection Cannot be obtained.
     */
    NO_CONNECTION("Cannot get database connection.", Category.CATEGORY_SERVICE_DOWN, 1),
    /**
     * SQL Problem: "%1$s".
     */
    SQL_ERROR("SQL Problem: \"%1$s\"", OXExceptionStrings.SQL_ERROR_MSG, Category.CATEGORY_ERROR, 2),
    /**
     * No group given.
     */
    NULL("No group given.", GroupExceptionMessage.NULL_MSG, Category.CATEGORY_ERROR, 3),
    /**
     * The mandatory field %1$s is not defined.
     */
    MANDATORY_MISSING("The mandatory field %1$s is not defined.", GroupExceptionMessage.MANDATORY_MISSING_MSG, Category.CATEGORY_USER_INPUT, 4),
    /**
     * The simple name contains invalid characters: "%1$s".
     */
    NOT_ALLOWED_SIMPLE_NAME("The simple name contains invalid characters: \"%1$s\".", GroupExceptionMessage.NOT_ALLOWED_SIMPLE_NAME_MSG, Category.CATEGORY_USER_INPUT, 5),
    /**
     * Another group with the same identifier name exists: %1$d.
     */
    DUPLICATE("Another group with the same identifier name exists: %1$d.", GroupExceptionMessage.DUPLICATE_MSG, Category.CATEGORY_USER_INPUT, 6),
    /**
     * Group contains a not existing member %1$d.
     */
    NOT_EXISTING_MEMBER("Group contains a not existing member %1$d.", GroupExceptionMessage.NOT_EXISTING_MEMBER_MSG, Category.CATEGORY_USER_INPUT, 7),
    /**
     * Group contains invalid data: "%1$s".
     */
    INVALID_DATA("Group contains invalid data: \"%1$s\".", GroupExceptionMessage.INVALID_DATA_MSG, Category.CATEGORY_USER_INPUT, 8),
    /**
     * You are not allowed to create groups.
     */
    NO_CREATE_PERMISSION("You are not allowed to create groups.", GroupExceptionMessage.NO_CREATE_PERMISSION_MSG, Category.CATEGORY_PERMISSION_DENIED, 9),
    /**
     * Edit Conflict. Your change cannot be completed because somebody else has made a conflicting change to the same item. Please
     * refresh or synchronize and try again.
     */
    MODIFIED("Edit Conflict. Your change cannot be completed because somebody else has made a conflicting change to the same item. Please refresh or synchronize and try again.", GroupExceptionMessage.MODIFIED_MSG, Category.CATEGORY_CONFLICT, 10),
    /**
     * You are not allowed to change groups.
     */
    NO_MODIFY_PERMISSION("You are not allowed to change groups.", GroupExceptionMessage.NO_MODIFY_PERMISSION_MSG, Category.CATEGORY_PERMISSION_DENIED, 11),
    /**
     * You are not allowed to delete groups.
     */
    NO_DELETE_PERMISSION("You are not allowed to delete groups.", GroupExceptionMessage.NO_DELETE_PERMISSION_MSG, Category.CATEGORY_PERMISSION_DENIED, 12),
    /**
     * Group "%1$s" can not be deleted.
     */
    NO_GROUP_DELETE("Group \"%1$s\" can not be deleted.", GroupExceptionMessage.NO_GROUP_DELETE_MSG, Category.CATEGORY_USER_INPUT, 13),
    /**
     * Group "%1$s" can not be changed.
     */
    NO_GROUP_UPDATE("Group \"%1$s\" can not be changed.", GroupExceptionMessage.NO_GROUP_UPDATE_MSG, Category.CATEGORY_USER_INPUT, 14),
    /**
     * The display name "%1$s" is reserved. Please choose another one.
     */
    RESERVED_DISPLAY_NAME("The display name \"%1$s\" is reserved. Please choose another one.", GroupExceptionMessage.RESERVED_DISPLAY_NAME_MSG, Category.CATEGORY_USER_INPUT, 15),

    NO_GUEST_USER_IN_GROUP("Group contains a guest user %1$d.", GroupExceptionMessage.NO_GUEST_USER_IN_GROUP_MSG, Category.CATEGORY_USER_INPUT, 16),

    ;

    private final String message;
    private final String displayMessage;
    private final Category category;
    private final int detailNumber;

    private GroupExceptionCodes(String message, Category category, int detailNumber) {
        this(message, null, category, detailNumber);
    }

    private GroupExceptionCodes(String message, String displayMessage, Category category, int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    @Override
    public String getPrefix() {
        return "GRP";
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
    public OXException create(Object... args) {
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
