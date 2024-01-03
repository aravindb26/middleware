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

package com.openexchange.redis.internal.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.java.Strings;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.redis.RedisConnectorProvider;
import com.openexchange.redis.RedisConnectorService;
import com.openexchange.redis.internal.AbstractRedisConnector;
import com.openexchange.redis.internal.BaseRedisPropertyProvider;
import com.openexchange.redis.internal.RedisConfiguration;
import com.openexchange.redis.internal.RedisConnectorInitializer;
import com.openexchange.redis.internal.RedisConnectorResult;
import com.openexchange.redis.internal.RedisProperty;
import com.openexchange.redis.internal.RemoteRedisPropertyProvider;
import com.openexchange.version.VersionService;

/**
 * {@link RedisConnectorActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisConnectorActivator extends HousekeepingActivator {

    private List<AbstractRedisConnector<?, ?>> redisConnectors;

    /**
     * Initializes a new {@link RedisConnectorActivator}.
     */
    public RedisConnectorActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class, VersionService.class, SSLSocketFactoryProvider.class,
            SSLConfigurationService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisConnectorActivator.class);
        logger.info("Starting bundle {}", context.getBundle().getSymbolicName());

        List<AbstractRedisConnector<?, ?>> redisConnectors = new ArrayList<>();
        try {

            LeanConfigurationService configService = getService(LeanConfigurationService.class);
            if (!configService.getBooleanProperty(RedisProperty.ENABLED)) {
                logger.info("Redis connector disabled via configuration. Aborting initialization of Redis connector...");
                return;
            }

            RedisConfiguration localConfiguration = new RedisConfiguration(BaseRedisPropertyProvider.getInstance(), configService);
            RedisConnectorInitializer localInitializer = new RedisConnectorInitializer(localConfiguration, this);
            RedisConnectorResult localRedisConnectorResult = localInitializer.init();
            redisConnectors.add(localRedisConnectorResult.getRedisConnector());
            RedisConnectorProvider localConnectorProvider = localRedisConnectorResult.createConnectorProvider();

            // Initialize remote ones
            List<RedisConnectorProvider> remoteRedisConnectors = initRemoteRedisConnectors(redisConnectors, configService);

            // Register services
            registerService(MWHealthCheck.class, localRedisConnectorResult.createHealthCheck());
            registerService(RedisConnectorService.class, new RedisConnectorService() {

                @Override
                public List<RedisConnectorProvider> getRemoteConnectorProviders() throws OXException {
                    return remoteRedisConnectors;
                }

                @Override
                public RedisConnectorProvider getConnectorProvider() throws OXException {
                    return localConnectorProvider;
                }
            });

            logger.info("Bundle {} started successfully.", context.getBundle().getSymbolicName());
            this.redisConnectors = redisConnectors;
            redisConnectors = null;
        } catch (OXException e) {
            logger.error("Failed to initialize Redis connector", e);
        } catch (Exception e) {
            logger.error("Error starting bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        } finally {
            if (redisConnectors != null) {
                // Exception path...
                cleanUp();
                for (AbstractRedisConnector<?,?> redisConnector : redisConnectors) {
                    redisConnector.shutdown();
                }
            }
        }
    }

    private List<RedisConnectorProvider> initRemoteRedisConnectors(List<AbstractRedisConnector<?, ?>> redisConnectors, LeanConfigurationService configService) throws OXException {
        boolean sitesEnabled = configService.getBooleanProperty(RedisProperty.SITES_ENABLED);
        if (!sitesEnabled) {
            // Remote site NOT enabled per configuration
            return Collections.emptyList();
        }

        String sSitesIds = configService.getProperty(RedisProperty.SITES);
        if (Strings.isEmpty(sSitesIds)) {
            // No remote site available in configuration
            return Collections.emptyList();
        }

        String[] sitesIds = Strings.splitByComma(sSitesIds);
        List<RedisConnectorProvider> remoteRedisConnectors = new ArrayList<>(sitesIds.length);
        for (String siteId : sitesIds) {
            RedisConfiguration remoteConfiguration = new RedisConfiguration(siteId, RemoteRedisPropertyProvider.getInstance(), configService);
            RedisConnectorInitializer remoteInitializer = new RedisConnectorInitializer(remoteConfiguration, this);
            RedisConnectorResult remoteRedisConnectorResult = remoteInitializer.init();
            redisConnectors.add(remoteRedisConnectorResult.getRedisConnector());
            // Add connector provider to list
            remoteRedisConnectors.add(remoteRedisConnectorResult.createConnectorProvider());
        }
        return List.copyOf(remoteRedisConnectors);
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisConnectorActivator.class);
        try {
            logger.info("Stopping bundle {}", context.getBundle().getSymbolicName());
            super.stopBundle();
            List<AbstractRedisConnector<?, ?>> redisConnectors = this.redisConnectors;
            if (redisConnectors != null) {
                this.redisConnectors = null;
                for (AbstractRedisConnector<?,?> redisConnector : redisConnectors) {
                    redisConnector.shutdown();
                }
            }
            logger.info("Bundle {} stopped successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error stopping bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

}
