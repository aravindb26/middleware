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

import com.openexchange.exception.OXException;

/**
 * {@link RedisVoidOperation} - Represents an operation performed against Redis end-point that executes one or more commands.
 * <p>
 * Moreover, an operation may specify whether circuit break shall be omitted and the concrete command timeout.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
@FunctionalInterface
public interface RedisVoidOperation extends RedisOperation<Void> {

    /**
     * Executes the operation.
     *
     * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
     * @throws OXException If an error is occurred during the execution
     */
    void executeWithoutResult(RedisCommandsProvider commandsProvider) throws OXException;

    @Override
    default Void execute(RedisCommandsProvider commandsProvider) throws OXException {
        executeWithoutResult(commandsProvider);
        return null;
    }

}
