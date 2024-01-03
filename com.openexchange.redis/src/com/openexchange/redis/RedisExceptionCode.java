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

package com.openexchange.redis;

import static com.openexchange.exception.OXExceptionStrings.MESSAGE;
import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;

/**
 * {@link RedisExceptionCode} - An enumeration of error codes for Redis.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum RedisExceptionCode implements DisplayableOXExceptionCode {

    /**
     * Unexpected error: %1$s
     */
    UNEXPECTED_ERROR("Unexpected error: %1$s", MESSAGE, CATEGORY_ERROR, 1),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", MESSAGE, CATEGORY_ERROR, 2),
    /**
     * A JSON error occurred: %1$s
     */
    JSON_ERROR("A JSON error occurred: %1$s", MESSAGE, CATEGORY_ERROR, 3),
    /**
     * A connection pool error occurred: %1$s
     */
    CONNECTION_POOL_ERROR("A connection pool error occurred: %1$s", MESSAGE, Category.CATEGORY_ERROR, 4),
    /**
     * Unable to connect to Redis end-point: %1$s
     */
    CONNECT_FAILURE("Unable to connect to Redis end-point: %1$s", MESSAGE, Category.CATEGORY_CONFIGURATION, 5),
    /**
     * Ping to Redis end-point failed: %1$s. The service at the other end of the connected socket does NOT behave like a Redis server.
     */
    PING_FAILURE("Ping to Redis end-point failed: %1$s. The service at the other end of the connected socket does NOT behave like a Redis server.", MESSAGE, Category.CATEGORY_CONFIGURATION, 5),
    /**
     * A Redis error occurred: %1$s
     */
    REDIS_ERROR("A Redis error occurred: %1$s", MESSAGE, Category.CATEGORY_ERROR, 6),
    /**
     * Redis command exceeded timeout: %1$s
     */
    REDIS_COMMAND_TIMEOUT("Redis command exceeded timeout: %1$s", MESSAGE, Category.CATEGORY_ERROR, 7),
    /**
     * Redis command exceeded timeout %1$s (%2$s).
     */
    REDIS_COMMAND_TIMEOUT_WITH_MILLIS("Redis command exceeded timeout %1$s (%2$s).", MESSAGE, Category.CATEGORY_ERROR, 7), // Yes, same code
    /**
     * Redis command failed: %1$s
     */
    REDIS_COMMAND_ERROR("Redis command failed: %1$s", MESSAGE, Category.CATEGORY_ERROR, 8),
    /**
     * Redis command has been invoked with invalid arguments
     */
    REDIS_COMMAND_INVALID_ARGUMENTS("Redis command has been invoked with invalid arguments", MESSAGE, Category.CATEGORY_ERROR, 9),
    /**
     * Connection to Redis end-point "%1$s" was closed
     */
    CONNECTION_CLOSED("Connection to Redis end-point \"%1$s\" was closed", MESSAGE, Category.CATEGORY_ERROR, 10),
   /**
    * Failed to put object into or retrieve object from Redis storage.
    */
   CONVERSION_ERROR("Failed to put object into or retrieve object from Redis storage.", MESSAGE, Category.CATEGORY_ERROR, 11),
    ;

    /** The Redis exception prefix */
    private static final String PREFIX = "REDIS";

    /**
     * Gets the prefix for Redis exceptions.
     *
     * @return The <code>"REDIS"</code> prefix
     */
    public static String prefix() {
        return PREFIX;
    }

    /**
     * Checks if specified exception hints to a connectivity problem with Redis end-point.
     *
     * @param e The exception to check
     * @return <code>true</code> for connectivity problem; otherwise <code>false</code>
     */
    public static boolean isConnectivityError(OXException e) {
        return e != null && (RedisExceptionCode.CONNECT_FAILURE.equals(e) || RedisExceptionCode.REDIS_COMMAND_TIMEOUT.equals(e) || RedisExceptionCode.CONNECTION_CLOSED.equals(e));
    }

    private final String message;
    private final String displayMessage;
    private final int detailNumber;
    private final Category category;

    private RedisExceptionCode(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.detailNumber = detailNumber;
        this.category = category;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public int getNumber() {
        return detailNumber;
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
