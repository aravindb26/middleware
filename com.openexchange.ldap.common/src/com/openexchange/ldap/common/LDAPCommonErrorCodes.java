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

package com.openexchange.ldap.common;

import static com.openexchange.exception.OXExceptionStrings.MESSAGE;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link LDAPCommonErrorCodes} contains common LDAP client error codes.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public enum LDAPCommonErrorCodes implements DisplayableOXExceptionCode {

    /**
     * Unable to establish a connection to the LDAP server: %s
     */
    CONNECTION_ERROR("Unable to establish a connection to the LDAP server: %s", MESSAGE, Category.CATEGORY_ERROR, 1),
    /**
     * Unexpected LDAP error: %s
     */
    UNEXPECTED_ERROR("Unexpected LDAP error: %s", MESSAGE, Category.CATEGORY_ERROR, 2),
    /**
     * No LDAP server config with id \"%s\" found
     */
    NOT_FOUND("No LDAP server config with id \"%s\" found", MESSAGE, Category.CATEGORY_ERROR, 3),
    /**
     * Missing required property \"%s\" in the LDAP configuration.
     */
    PROPERTY_MISSING("Missing required property \"%s\" in the LDAP configuration.", MESSAGE, Category.CATEGORY_ERROR, 4),
    /**
     * Invalid LDAP configuration.
     */
    INVALID_CONFIG("Invalid LDAP configuration.", MESSAGE, Category.CATEGORY_ERROR, 5),
    /**
     * BindRequestFactory with id \"%s\" not found.
     */
    BIND_NOT_FOUND("BindRequestFactory with id \"%s\" not found.", MESSAGE, Category.CATEGORY_ERROR, 6),
    /**
     * Auth type \"%s\" is not allowed to initialize the connection pool.
     */
    INIT_POOL_AUTH_ERROR("Auth type \"%s\" is not allowed to initialize the connection pool.", MESSAGE, Category.CATEGORY_ERROR, 7),
    ;

    /** Error code prefix for common LDAP client errors */
    public static final String PREFIX = "LDAP_COMMON";

    /**
     * (Log) Message of the exception.
     */
    private final String message;

    /**
     * Display message of the exception.
     */
    private final String displayMessage;

    /**
     * Category of the exception.
     */
    private final Category category;

    /**
     * Detail number of the exception.
     */
    private final int number;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param displayMessage The (optional) display message
     * @param category category.
     * @param detailNumber detail number.
     */
    private LDAPCommonErrorCodes(final String message, String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage == null ? OXExceptionStrings.MESSAGE : displayMessage;
        this.category = category;
        number = detailNumber;
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
        return displayMessage;
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
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }

}
