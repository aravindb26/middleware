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

import io.lettuce.core.RedisException;


/**
 * {@link RedisConversionException} - Thrown when conversion fails to put objects into or retrieve objects from Redis storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisConversionException extends RedisException {

    private static final long serialVersionUID = 610661562857958540L;

    /**
     * Initializes a new {@link RedisConversionException}.
     *
     * @param msg The detail message
     */
    public RedisConversionException(String msg) {
        super(msg);
    }

    /**
     * Initializes a new {@link RedisConversionException}.
     *
     * @param cause The cause
     */
    public RedisConversionException(Throwable cause) {
        super(cause);
    }

    /**
     * Initializes a new {@link RedisConversionException}.
     *
     * @param msg The detail message
     * @param cause The cause
     */
    public RedisConversionException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
