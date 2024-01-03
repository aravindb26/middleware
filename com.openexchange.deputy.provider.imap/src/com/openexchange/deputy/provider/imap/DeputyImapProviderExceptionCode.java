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


package com.openexchange.deputy.provider.imap;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * The IMAP provider error codes for deputy permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public enum DeputyImapProviderExceptionCode implements DisplayableOXExceptionCode {

    /**
     * Unexpected error: %1$s
     */
    UNEXPECTED_ERROR("Unexpected error: %1$s", CATEGORY_ERROR),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", CATEGORY_ERROR),
    /**
     * A JSON error occurred: %1$s
     */
    JSON_ERROR("A JSON error occurred: %1$s", CATEGORY_ERROR),
    /**
     * An SQL error occurred: %1$s
     */
    SQL_ERROR("An SQL error occurred: %1$s", CATEGORY_ERROR),
    /**
     * Mail account %1$s is not supported
     */
    MAIL_ACCOUNT_NOT_SUPPORTED("Mail account %1$s is not supported", CATEGORY_PERMISSION_DENIED),
    /**
     * Cannot select folder: %1$s
     */
    CANNOT_SELECT_FOLDER("Cannot select IMAP folder: %1$s", CATEGORY_PERMISSION_DENIED),
    /**
     * Cannot get ACLs from folder: %1$s
     */
    CANNOT_GET_ACL("Cannot get ACLs from IMAP folder: %1$s", CATEGORY_PERMISSION_DENIED),
    /**
     * Cannot set ACLs from folder: %1$s
     */
    CANNOT_SET_ACL("Cannot set ACLs from IMAP folder: %1$s", CATEGORY_PERMISSION_DENIED),
    /**
     * There is no such deputy permission for folder: %1$s
     */
    MISSING_DEPUTY_PERMISSION("There is no such deputy permission for folder: %1$s", CATEGORY_ERROR),
    /**
     * There is already such a deputy permission for folder: %1$s
     */
    DUPLICATE_DEPUTY_PERMISSION("There is already such a deputy permission for folder: %1$s", CATEGORY_ERROR),
    /**
     * No such deputy provider for module "%1$s".
     */
    NO_SUCH_PROVIDER("No such deputy provider for module \"%1$s\".", CATEGORY_ERROR),
    /**
     * Deputy provider disabled for module "%1$s".
     */
    PROVIDER_DISABLED("Deputy provider disabled for module \"%1$s\".", CATEGORY_PERMISSION_DENIED),
    /**
     * No such deputy permission for identifier "%1$s".
     */
    NO_SUCH_DEPUTY("No such deputy permission for identifier \"%1$s\".", CATEGORY_ERROR),
    ;

    private static final String PREFIX = "IMAP_DEPUTY";

    private final String message;
    private final Category category;
    private final String displayMessage;

    /**
     * Initializes a new {@link DeputyImapProviderExceptionCode}.
     *
     * @param message
     * @param category
     */
    private DeputyImapProviderExceptionCode(final String message, final Category category) {
        this(message, category, null);
    }

    /**
     * Initializes a new {@link DeputyImapProviderExceptionCode}.
     *
     * @param message
     * @param category
     * @param displayMessage
     */
    private DeputyImapProviderExceptionCode(final String message, final Category category, final String displayMessage) {
        this.message = message;
        this.category = category;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
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
    public int getNumber() {
        return ordinal() + 1;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * {@inheritDoc}
     */
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
