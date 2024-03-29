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

package com.openexchange.cluster.map.redis.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.cluster.map.ClusterMapService;
import com.openexchange.cluster.map.redis.RedisClusterMapService;
import com.openexchange.exception.OXException;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.redis.RedisConnectorProvider;
import com.openexchange.redis.RedisConnectorService;

/**
 * {@link RedisClusterMapServiceActivator} - The activator for Redis-backed cluster map service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisClusterMapServiceActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link RedisClusterMapServiceActivator}.
     */
    public RedisClusterMapServiceActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { RedisConnectorService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisClusterMapServiceActivator.class);
        try {
            trackService(LockService.class);
            openTrackers();
            logger.info("Starting bundle {}", context.getBundle().getSymbolicName());
            RedisClusterMapService service = initRedisClusterMapService(getServiceSafe(RedisConnectorService.class));
            registerService(ClusterMapService.class, service);
            logger.info("Bundle {} started successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error starting bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

    private static RedisClusterMapService initRedisClusterMapService(RedisConnectorService connectorService) throws OXException {
        boolean enableCachingForClusterMaps = true;
        RedisConnectorProvider connectorProvider = connectorService.getConnectorProvider();
        return new RedisClusterMapService(connectorProvider.getConnector(), enableCachingForClusterMaps);
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisClusterMapServiceActivator.class);
        try {
            logger.info("Stopping bundle {}", context.getBundle().getSymbolicName());
            super.stopBundle();
            logger.info("Bundle {} stopped successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error stopping bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }
}
