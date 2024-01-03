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

package com.openexchange.redis.internal.sentinel;

import java.io.InputStream;
import java.util.Optional;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.redis.commands.StringRedisHashCommands;
import com.openexchange.redis.commands.StringRedisListCommands;
import com.openexchange.redis.commands.StringRedisSetCommands;
import com.openexchange.redis.commands.StringRedisSortedSetCommands;
import io.lettuce.core.api.sync.BaseRedisCommands;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisListCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.api.sync.RedisTransactionalCommands;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;


/**
 * {@link RedisSentinelCommandsProvider} - The commands provider for a Sentinal connection.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisSentinelCommandsProvider implements RedisCommandsProvider {

    private final StatefulRedisMasterReplicaConnection<String, InputStream> connection;

    /**
     * Initializes a new {@link RedisSentinelCommandsProvider}.
     *
     * @param connection The connection to use
     */
    public RedisSentinelCommandsProvider(StatefulRedisMasterReplicaConnection<String, InputStream> connection) {
        super();
        this.connection = connection;
    }

    @Override
    public Optional<RedisTransactionalCommands<String, InputStream>> optTransactionalCommands() {
        return Optional.of(connection.sync());
    }

    @Override
    public BaseRedisCommands<String, InputStream> getBaseCommands() {
        return connection.sync();
    }

    @Override
    public RedisStringCommands<String, InputStream> getRawStringCommands() {
        return connection.sync();
    }

    @Override
    public RedisKeyCommands<String, InputStream> getKeyCommands() {
        return connection.sync();
    }

    @Override
    public RedisSetCommands<String, String> getSetCommands() {
        return new StringRedisSetCommands(connection.sync());
    }

    @Override
    public RedisSortedSetCommands<String, String> getSortedSetCommands() {
        return new StringRedisSortedSetCommands(connection.sync());
    }

    @Override
    public RedisHashCommands<String, String> getHashCommands() {
        return new StringRedisHashCommands(connection.sync());
    }

    @Override
    public RedisListCommands<String, String> getListCommands() {
        return new StringRedisListCommands(connection.sync());
    }
}
