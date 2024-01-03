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

package com.openexchange.pubsub.redis.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.pubsub.PubSubService;
import com.openexchange.pubsub.redis.RedisPubSubService;
import com.openexchange.pubsub.redis.impl.RedisPubSubConnectionProviderImpl;
import com.openexchange.redis.RedisConnectorProvider;
import com.openexchange.redis.RedisConnectorService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;

/**
 * {@link RedisPubSubServiceActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisPubSubServiceActivator extends HousekeepingActivator {

    private RedisPubSubService redisPubSubService;

    /**
     * Initializes a new {@link RedisPubSubServiceActivator}.
     */
    public RedisPubSubServiceActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { RedisConnectorService.class, LeanConfigurationService.class, TimerService.class, ThreadPoolService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisPubSubServiceActivator.class);
        try {
            trackService(LockService.class);
            openTrackers();
            logger.info("Starting bundle {}", context.getBundle().getSymbolicName());
            RedisPubSubService redisPubSubService = initRedisPubSubService(getService(RedisConnectorService.class));
            registerService(PubSubService.class, redisPubSubService);
            this.redisPubSubService = redisPubSubService;
            logger.info("Bundle {} started successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error starting bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

    private RedisPubSubService initRedisPubSubService(RedisConnectorService connectorService) throws OXException {
        RedisConnectorProvider connectorProvider = connectorService.getConnectorProvider();
        return new RedisPubSubService(new RedisPubSubConnectionProviderImpl(connectorProvider.getConnector()), this);
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisPubSubServiceActivator.class);
        try {
            logger.info("Stopping bundle {}", context.getBundle().getSymbolicName());
            RedisPubSubService redisPubSubService = this.redisPubSubService;
            if (redisPubSubService != null) {
                this.redisPubSubService = null;
                redisPubSubService.shutdown();
            }
            super.stopBundle();
            logger.info("Bundle {} stopped successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error stopping bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }
}
