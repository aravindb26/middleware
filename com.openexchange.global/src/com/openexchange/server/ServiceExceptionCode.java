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

package com.openexchange.server;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * The error code enumeration for service errors.
 */
public enum ServiceExceptionCode implements DisplayableOXExceptionCode {

    /**
     * The required service %1$s is temporary not available. Please try again later.
     */
    SERVICE_UNAVAILABLE("The required service %1$s is temporary not available. Please try again later.",
        ServiceExceptionMessage.SERVICE_UNAVAILABLE_MSG, Category.CATEGORY_TRY_AGAIN, 1),
    /**
     * An I/O error occurred
     */
    IO_ERROR("An I/O error occurred", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 2),
    /**
     * Service initialization failed
     */
    SERVICE_INITIALIZATION_FAILED("Service initialization failed", OXExceptionStrings.MESSAGE, Category.CATEGORY_ERROR, 3);

    /**
     * Creates a new <code>"SRV-0001"</code> (SERVICE_UNAVAILABLE) exception for specified class
     *
     * @param clazz The class of missing service
     * @return The appropriate {@link OXException} instance
     */
    public static OXException serviceUnavailable(final Class<?> clazz) {
        if (null == clazz) {
            return null;
        }
        return SERVICE_UNAVAILABLE.create(clazz.getName());
    }

    private static final String PREFIX = "SRV";

    /**
     * Checks if specified {@code OXException}'s prefix is equal to this {@code OXExceptionCode} enumeration.
     *
     * @param e The {@code OXException} to check
     * @return <code>true</code> if prefix is equal; otherwise <code>false</code>
     */
    public static boolean hasPrefix(final OXException e) {
        if (null == e) {
            return false;
        }
        return PREFIX.equals(e.getPrefix());
    }

    private final String message;
    private final String displayMessage;
    private final int detailNumber;
    private final Category category;

    private ServiceExceptionCode(final String message, String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.detailNumber = detailNumber;
        this.category = category;
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
    public Category getCategory() {
        return category;
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

    /**
     * Generates an appropriate OXException for specified absent service.
     *
     * @param serviceClass The class of absent service
     * @return An appropriate OXException for specified absent service
     */
    public static <S> OXException absentService(final Class<S> serviceClass) {
        return SERVICE_UNAVAILABLE.create(serviceClass.getName());
    }

}
