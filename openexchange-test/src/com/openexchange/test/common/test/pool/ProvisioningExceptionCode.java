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

package com.openexchange.test.common.test.pool;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * {@link ProvisioningExceptionCode} - defines exception codes for {@link ProvisioningExceptionCode}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public enum ProvisioningExceptionCode implements OXExceptionCode {

    /**
     * Service configuration failed because of internal errors: '%1$s'
     */
    SERVICE_CONFIGURATION_FAILED("Provisioning service configuration failed because of internal errors: '%1$s'", Category.CATEGORY_ERROR, 1),

    /**
     * Unable to create user: '%1$s'
     */
    UNABLE_TO_CREATE_USER("Unable to create user: '%1$s'", Category.CATEGORY_ERROR, 2),

    /**
     * Unable to update user: '%1$s'
     */
    UNABLE_TO_UPDATE_USER("Unable to update user: '%1$s'", Category.CATEGORY_ERROR, 3),

    /**
     * Unable to delete user: '%1$s'
     */
    UNABLE_TO_DELETE_USER("Unable to delete user: '%1$s'", Category.CATEGORY_ERROR, 4),

    /**
     * Unable to create group: '%1$s'
     */
    UNABLE_TO_CREATE_GROUP("Unable to create group: '%1$s'", Category.CATEGORY_ERROR, 5),

    /**
     * Unable to update group: '%1$s'
     */
    UNABLE_TO_UPDATE_GROUP("Unable to update group: '%1$s'", Category.CATEGORY_ERROR, 6),

    /**
     * Unable to delete group: '%1$s'
     */
    UNABLE_TO_DELETE_GROUP("Unable to delete group: '%1$s'", Category.CATEGORY_ERROR, 7),

    /**
     * Unable to create resource: '%1$s'
     */
    UNABLE_TO_CREATE_RESOURCE("Unable to create resource: '%1$s'", Category.CATEGORY_ERROR, 8),

    /**
     * Unable to update resource: '%1$s'
     */
    UNABLE_TO_UPDATE_RESOURCE("Unable to update resource: '%1$s'", Category.CATEGORY_ERROR, 9),

    /**
     * Unable to delete resource: '%1$s'
     */
    UNABLE_TO_DELETE_RESOURCE("Unable to delete resource: '%1$s'", Category.CATEGORY_ERROR, 10),

    /**
     * Unable to create context: '%1$s'
     */
    UNABLE_TO_CREATE_CONTEXT("Unable to create context.", Category.CATEGORY_ERROR, 11),

    /**
     * Unable to update context: '%1$s'
     */
    UNABLE_TO_UPDATE_CONTEXT("Unable to update context: '%1$s'", Category.CATEGORY_ERROR, 12),

    /**
     * Unable to delete context: '%1$s'
     */
    UNABLE_TO_DELETE_CONTEXT("Unable to delete context '%1$s': '%2$s'", Category.CATEGORY_ERROR, 13),

    /**
     * Unable to parse email address for the given user: '%1$s'
     */
    UNABLE_TO_PARSE_ADDRESS("Unable to parse email address for the given user: '%1$s'", Category.CATEGORY_ERROR, 14),
    
    /**
     * Unable to create schema: '%1$s'
     */
    UNABLE_TO_CREATE_SCHEMA("Unable to create schema", Category.CATEGORY_ERROR, 15),

    /**
     * Unable to create user: '%1$s'
     */
    UNABLE_TO_GET_USER("Unable to get user: '%1$s'", Category.CATEGORY_ERROR, 16),

    ;

    private final String message;
    private final String displayMessage;
    private final int detailNumber;
    private final Category category;

    /**
     * Initializes a new {@link ProvisioningExceptionCode}.
     *
     * @param message
     * @param category
     * @param detailNumber
     */
    ProvisioningExceptionCode(final String message, final Category category, final int detailNumber) {
        this(message, null, category, detailNumber);
    }

    /**
     * Initializes a new {@link ProvisioningExceptionCode}.
     *
     * @param message
     * @param displayMessage
     * @param category
     * @param detailNumber
     */
    ProvisioningExceptionCode(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    @Override
    public boolean equals(OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public int getNumber() {
        return this.detailNumber;
    }

    @Override
    public Category getCategory() {
        return this.category;
    }

    @Override
    public String getPrefix() {
        return "PROVISIONING";
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public String getDisplaymessage() {
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