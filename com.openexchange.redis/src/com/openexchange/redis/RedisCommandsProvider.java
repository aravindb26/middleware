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

import java.io.InputStream;
import java.util.Optional;
import com.openexchange.redis.commands.StringRedisStringCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisListCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.api.sync.RedisTransactionalCommands;

/**
 * {@link RedisCommandsProvider} - Provides Redis commands to communicate with Redis end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface RedisCommandsProvider {

    // ---------------------------------------------------- InputStream values ------------------------------------------------------------

    /**
     * Gets the base commands.
     *
     * @return The base commands
     */
    BaseRedisCommands<String, InputStream> getBaseCommands();

    /**
     * Gets the key commands.
     *
     * @return The key commands
     */
    RedisKeyCommands<String, InputStream> getKeyCommands();

    /**
     * Gets the string commands.
     *
     * @return The string commands
     */
    RedisStringCommands<String, InputStream> getRawStringCommands();

    /**
     * Gets the optional transactional commands.
     *
     * @return The transactional commands or empty
     */
    Optional<RedisTransactionalCommands<String, InputStream>> optTransactionalCommands();

    // ---------------------------------------------------- String values -----------------------------------------------------------------

    /**
     * Gets the string commands.
     *
     * @return The string commands
     */
    default RedisStringCommands<String, String> getStringCommands() {
        return new StringRedisStringCommands(getRawStringCommands());
    }

    /**
     * Gets the set commands.
     *
     * @return The set commands
     */
    RedisSetCommands<String, String> getSetCommands();

    /**
     * Gets the sorted set commands.
     *
     * @return The sorted set commands
     */
    RedisSortedSetCommands<String, String> getSortedSetCommands();

    /**
     * Gets the set commands.
     *
     * @return The set commands
     */
    RedisHashCommands<String, String> getHashCommands();

    /**
     * Gets the list commands.
     *
     * @return The list commands
     */
    RedisListCommands<String, String> getListCommands();
}
