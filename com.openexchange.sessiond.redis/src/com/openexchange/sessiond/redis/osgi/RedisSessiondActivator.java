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

package com.openexchange.sessiond.redis.osgi;

import java.rmi.Remote;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.auth.Authenticator;
import com.openexchange.cluster.map.ClusterMapService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.crypto.CryptoService;
import com.openexchange.event.CommonEvent;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.osgi.Tools;
import com.openexchange.pubsub.PubSubService;
import com.openexchange.redis.RedisConnectorService;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.SessionSerializationInterceptor;
import com.openexchange.session.SessionVersionService;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.redis.Obfuscator;
import com.openexchange.sessiond.redis.RedisSessionStorageService;
import com.openexchange.sessiond.redis.RedisSessionVersionService;
import com.openexchange.sessiond.redis.RedisSessiondService;
import com.openexchange.sessiond.redis.RedisSessiondState;
import com.openexchange.sessiond.redis.config.RedisSessiondConfigProperty;
import com.openexchange.sessiond.redis.metrics.RedisSessiondMetricsProvider;
import com.openexchange.sessiond.redis.rmi.SessiondRMIServiceImpl;
import com.openexchange.sessiond.rmi.SessiondRMIService;
import com.openexchange.sessionstorage.SessionStorageService;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;

/**
 * {@link RedisSessiondActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSessiondActivator extends HousekeepingActivator implements Reloadable {

    private Obfuscator obfuscator;
    private RedisSessiondService service;

    /**
     * Initializes a new {@link RedisSessiondActivator}.
     */
    public RedisSessiondActivator() {
        super();
    }

    @Override
    public Interests getInterests() {
        RedisSessiondConfigProperty[] properties = RedisSessiondConfigProperty.values();
        String[] sProps = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            sProps[i] = properties[i].getFQPropertyName();
        }
        return Reloadables.interestsForProperties(sProps);
    }

    @Override
    public synchronized void reloadConfiguration(ConfigurationService configService) {
        LeanConfigurationService configurationService = getService(LeanConfigurationService.class);
        if (configurationService != null) {
            RedisSessiondState newState = RedisSessiondState.builder().initFrom(configurationService).build();

            RedisSessiondService service = this.service;
            if (service != null) {
                service.setState(newState);
            }

            Obfuscator obfuscator = this.obfuscator;
            if (obfuscator != null) {
                Obfuscator newObfuscator = newState.getObfuscator();

                this.obfuscator = null;
                unregisterService(ObfuscatorService.class);

                registerService(ObfuscatorService.class, newObfuscator);
                this.obfuscator = newObfuscator;

                obfuscator.destroy();
            }
        }
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { RedisConnectorService.class, LeanConfigurationService.class, CryptoService.class, ThreadPoolService.class, TimerService.class,
            EventAdmin.class, PubSubService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class[] { Authenticator.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisSessiondActivator.class);
        try {
            logger.info("Starting bundle {}", context.getBundle().getSymbolicName());

            Services.setServiceLookup(this);

            LeanConfigurationService configurationService = getServiceSafe(LeanConfigurationService.class);
            if (configurationService.getBooleanProperty(RedisSessiondConfigProperty.REDIS_ENABLED) == false) {
                logger.info("Redis sessiond service disabled via configuration. Aborting start-up of Redis sessiond service...");
                return;
            }

            RankingAwareNearRegistryServiceTracker<SessionSerializationInterceptor> serializationInterceptorTracker = new RankingAwareNearRegistryServiceTracker<>(context, SessionSerializationInterceptor.class);
            rememberTracker(serializationInterceptorTracker);

            // Check if distributed token-sessions are enabled
            if (configurationService.getBooleanProperty(RedisSessiondConfigProperty.USE_DISTRIBUTED_TOKEN_SESSIONS)) {
                // Track ClusterMapService
                track(ClusterMapService.class, new ClusterMapServiceTracker(this, context));
            }

            // Additionally tracked services
            trackService(LockService.class);

            // Open trackers
            openTrackers();

            RedisSessiondState initialState = RedisSessiondState.builder().initFrom(configurationService).build();
            Obfuscator obfuscator = initialState.getObfuscator();
            this.obfuscator = obfuscator;
            registerService(ObfuscatorService.class, obfuscator);

            registerService(SessionVersionService.class, RedisSessionVersionService.getInstance());

            RedisSessiondService service = new RedisSessiondService(getService(RedisConnectorService.class).getConnectorProvider().getConnector(), initialState, serializationInterceptorTracker, this);
            this.service = service;
            SessiondService.SERVICE_REFERENCE.set(service);
            RedisSessiondMetricsProvider.initInstance(service);
            com.openexchange.sessiond.redis.metrics.SessionMetricHandler.init();
            registerService(SessiondService.class, service, Tools.withRanking(100));
            registerService(SessionStorageService.class, new RedisSessionStorageService(service), Tools.withRanking(100));

            {
                Dictionary<String, Object> serviceProperties = new Hashtable<>(1);
                serviceProperties.put("RMI_NAME", SessiondRMIService.RMI_NAME);
                registerService(Remote.class, new SessiondRMIServiceImpl(service), serviceProperties);
            }

            // Clear other sessions for a user on (remote) password change event
            {
                Dictionary<String, Object> serviceProperties = new Hashtable<>(1);
                serviceProperties.put(EventConstants.EVENT_TOPIC, "com/openexchange/passwordchange");
                EventHandler passwordChangeEventHandler = new EventHandler() {

                    @Override
                    public void handleEvent(Event event) {
                        if (event.containsProperty(CommonEvent.REMOTE_MARKER)) {
                            // Received from remote node
                            int contextId = ((Integer) event.getProperty("com.openexchange.passwordchange.contextId")).intValue();
                            int userId = ((Integer) event.getProperty("com.openexchange.passwordchange.userId")).intValue();
                            service.removeLocalUserSessions(userId, contextId);
                        }
                    }
                };
                registerService(EventHandler.class, passwordChangeEventHandler, serviceProperties);
            }

            registerService(Reloadable.class, this);

            logger.info("Bundle {} started successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error starting bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        Logger logger = LoggerFactory.getLogger(RedisSessiondActivator.class);
        try {
            logger.info("Stopping bundle {}", context.getBundle().getSymbolicName());
            super.stopBundle();
            this.removeService(Obfuscator.class);
            Obfuscator obfuscator = this.obfuscator;
            if (obfuscator != null) {
                this.obfuscator = null;
                obfuscator.destroy();
            }
            com.openexchange.sessiond.redis.metrics.SessionMetricHandler.stop();
            RedisSessiondMetricsProvider.dropInstance();
            SessiondService.SERVICE_REFERENCE.set(null);
            RedisSessiondService service = this.service;
            if (service != null) {
                this.service = null;
                service.shutDown();
            }
            Services.setServiceLookup(null);
            logger.info("Bundle {} stopped successfully.", context.getBundle().getSymbolicName());
        } catch (Exception e) {
            logger.error("Error stopping bundle {}", context.getBundle().getSymbolicName(), e);
            throw e;
        }
    }

    @Override
    public <S> boolean addService(Class<S> clazz, S service) {
        return super.addService(clazz, service);
    }

    @Override
    public <S> boolean removeService(Class<? extends S> clazz) {
        return super.removeService(clazz);
    }

}
