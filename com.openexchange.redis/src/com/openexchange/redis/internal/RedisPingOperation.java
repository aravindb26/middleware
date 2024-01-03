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


package com.openexchange.redis.internal;

import java.time.Duration;
import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.redis.RedisOperation;

/**
 * {@link RedisPingOperation} - The implementation for <code>PING</code> command.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisPingOperation implements RedisOperation<Boolean> {

    private static final RedisPingOperation INSTANCE = new RedisPingOperation();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static RedisPingOperation getInstance() {
        return INSTANCE;
    }

    // --------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link RedisPingOperation}.
     */
    private RedisPingOperation() {
        super();
    }

    @Override
    public Boolean execute(RedisCommandsProvider commandsProvider) throws OXException {
        return Boolean.valueOf("PONG".equals(commandsProvider.getBaseCommands().ping()));
    }

    @Override
    public boolean omitCircuitBreaker() {
        return true;
    }

    @Override
    public Optional<Duration> getCommandTimeout() {
        return Optional.of(Duration.ofSeconds(1));
    }

}
