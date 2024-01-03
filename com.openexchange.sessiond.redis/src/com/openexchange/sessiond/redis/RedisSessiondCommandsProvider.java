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

package com.openexchange.sessiond.redis;

import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.redis.commands.StringRedisStringCommands;
import com.openexchange.session.Session;
import com.openexchange.sessiond.redis.commands.SessionRedisStringCommands;
import com.openexchange.sessiond.redis.commands.SessionRedisStringCommands.VersionMismatchHandler;
import io.lettuce.core.api.sync.RedisStringCommands;

/**
 * {@link RedisSessiondCommandsProvider} - Provides Redis SessionD commands for a given connection.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface RedisSessiondCommandsProvider extends RedisCommandsProvider {

    /**
     * Gets the session commands from given Redis connection.
     *
     * @param obfuscator The obfuscator needed for passwords
     * @param versionMismatchHandler The handle for possible version mismatch
     * @return The session commands
     */
    default RedisStringCommands<String, Session> getSessionCommands(Obfuscator obfuscator, VersionMismatchHandler versionMismatchHandler) {
        return new SessionRedisStringCommands(getRawStringCommands(), obfuscator, versionMismatchHandler);
    }

    /**
     * Gets the string commands from given Redis connection.
     *
     * @return The string commands
     */
    @Override
    default RedisStringCommands<String, String> getStringCommands() {
        return new StringRedisStringCommands(getRawStringCommands());
    }

}
