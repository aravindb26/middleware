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

package com.openexchange.redis.internal.connectors;

import com.openexchange.redis.OperationMode;
import com.openexchange.redis.RedisConnector;
import com.openexchange.redis.RedisConnectorProvider;

/**
 * {@link AbstractRedisConnectorProvider} - The abstract Redis connector provider.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractRedisConnectorProvider implements RedisConnectorProvider {

    /** The operation mode supported by this Redis connector service */
    protected final OperationMode operationMode;

    /** The Redis connector */
    protected final RedisConnector redisConnector;

    /**
     * Initializes a new {@link AbstractRedisConnectorProvider}.
     *
     * @param operationMode The operation mode supported by this Redis connector service
     * @param redisConnector The Redis connector
     */
    protected AbstractRedisConnectorProvider(OperationMode operationMode, RedisConnector redisConnector) {
        super();
        this.operationMode = operationMode;
        this.redisConnector = redisConnector;
    }

    @Override
    public OperationMode getOperationMode() {
        return operationMode;
    }

    @Override
    public RedisConnector getConnector() {
        return redisConnector;
    }

}
