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


package com.openexchange.groupware.ldap;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Error codes for the ldap exception.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public enum LdapExceptionCode implements DisplayableOXExceptionCode {

    /**
     * A problem with distinguished names occurred.
     */
    DN_PROBLEM("Cannot build distinguished name from %s.", null, Category.CATEGORY_ERROR, 2),

    /**
     * Class can not be found.
     */
    CLASS_NOT_FOUND("Class %s can not be loaded.", null, Category.CATEGORY_CONFIGURATION, 3),

    /**
     * An implementation can not be instantiated.
     */
    INSTANTIATION_PROBLEM("Cannot instantiate class %s.", null, Category.CATEGORY_CONFIGURATION, 4),

    /**
     * A database connection Cannot be obtained.
     */
    NO_CONNECTION("Cannot get database connection.", OXExceptionStrings.SQL_ERROR_MSG, Category.CATEGORY_SERVICE_DOWN, 5),

    /**
     * SQL Problem: "%s".
     */
    SQL_ERROR("SQL problem: \"%s\"", OXExceptionStrings.SQL_ERROR_MSG, Category.CATEGORY_ERROR, 6),

    /**
     * Problem putting an object into the cache.
     */
    CACHE_PROBLEM("Problem putting/removing an object into/from the cache.", LdapExceptionMessage.CACHE_PROBLEM_DISPLAY,
        Category.CATEGORY_ERROR, 7),

    /**
     * Hash algorithm %s isn't found.
     */
    HASH_ALGORITHM("Hash algorithm %s could not be found.", LdapExceptionMessage.HASH_ALGORITHM_DISPLAY, Category.CATEGORY_ERROR, 8),

    /**
     * Encoding %s cannot be used.
     */
    UNSUPPORTED_ENCODING("Encoding %s cannot be used.", LdapExceptionMessage.UNSUPPORTED_ENCODING_DISPLAY, Category.CATEGORY_ERROR, 9),

    /**
     * Cannot find resource group with identifier %d.
     */
    RESOURCEGROUP_NOT_FOUND("Cannot find resource group with identifier %d.", LdapExceptionMessage.RESOURCEGROUP_NOT_FOUND_DISPLAY,
        Category.CATEGORY_ERROR, 10),

    /**
     * Found resource groups with same identifier %d.
     */
    RESOURCEGROUP_CONFLICT("Found resource groups with same identifier %d.", LdapExceptionMessage.RESOURCEGROUP_CONFLICT_DISPLAY,
        Category.CATEGORY_ERROR, 11),

    /**
     * Cannot find resource with identifier %d.
     */
    RESOURCE_NOT_FOUND("Cannot find resource with identifier %d.", LdapExceptionMessage.RESOURCE_NOT_FOUND_DISPLAY,
        Category.CATEGORY_ERROR, 12),

    /**
     * Found resources with same identifier %d.
     */
    RESOURCE_CONFLICT("Found resources with same identifier %d.", LdapExceptionMessage.RESOURCE_CONFLICT_DISPLAY,
        Category.CATEGORY_ERROR, 13),

    /**
     * Cannot find user with email %s.
     */
    NO_USER_BY_MAIL("Cannot find user with E-Mail %s.", LdapExceptionMessage.NO_USER_BY_MAIL_DISPLAY, Category.CATEGORY_ERROR, 14),

    /**
     * Cannot find user with identifier %1$s in context %2$d.
     */
    USER_NOT_FOUND("Cannot find user with identifier %1$s in context %2$d.", LdapExceptionMessage.USER_NOT_FOUND_DISPLAY,
        Category.CATEGORY_ERROR, 15),

    /**
     * Cannot find group with identifier %1$s in context %2$d.
     */
    GROUP_NOT_FOUND("Cannot find group with identifier %1$s in context %2$d.", LdapExceptionMessage.GROUP_NOT_FOUND_DISPLAY,
        Category.CATEGORY_ERROR, 17),

    /**
     * Unexpected error: %1$s
     */
    UNEXPECTED_ERROR("Unexpected error: %1$s", null, Category.CATEGORY_ERROR, 18);

    /**
     * Message of the exception.
     */
    private final String message;

    /**
     * Message shown to the user
     */
    private final String displayMessage;

    /**
     * Category of the exception.
     */
    private final Category category;

    /**
     * Detail number of the exception.
     */
    private final int detailNumber;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private LdapExceptionCode(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    @Override
    public String getPrefix() {
        return "";
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
    public boolean equals(final OXException e) {
        return /* getPrefix().equals(e.getPrefix()) && */e.getCode() == getNumber();
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
