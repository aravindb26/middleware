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

import org.slf4j.Logger;
import com.openexchange.health.DefaultMWHealthCheckResponse;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.health.MWHealthCheckResponse;
import com.openexchange.redis.internal.RedisPingOperation;


/**
 * {@link RedisSentinelHealthCheck}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisSentinelHealthCheck implements MWHealthCheck {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisSentinelHealthCheck.class);
    }

    private final RedisSentinelConnector redisSentinelConnector;

    /**
     * Initializes a new {@link RedisSentinelHealthCheck}.
     *
     * @param redisSentinelConnector The stand-alone connector
     */
    public RedisSentinelHealthCheck(RedisSentinelConnector redisSentinelConnector) {
        super();
        this.redisSentinelConnector = redisSentinelConnector;
    }

    @Override
    public String getName() {
        return "redis";
    }

    @Override
    public MWHealthCheckResponse call() {
        try {
            Boolean alive = redisSentinelConnector.executeOperation(RedisPingOperation.getInstance());
            return new DefaultMWHealthCheckResponse(getName(), null, alive.booleanValue());
        } catch (Exception e) {
            LoggerHolder.LOG.warn("Failed PING command to check Redis still alive", e);
            return new DefaultMWHealthCheckResponse(getName(), null, false);
        }
    }

}
