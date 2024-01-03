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

package com.openexchange.push.imapidle.osgi;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.exception.OXException;
import com.openexchange.hazelcast.configuration.HazelcastConfigurationService;
import com.openexchange.mail.MailProviderRegistration;
import com.openexchange.mail.Protocol;
import com.openexchange.osgi.Tools;
import com.openexchange.push.PushManagerService;
import com.openexchange.push.imapidle.ImapIdleConfiguration;
import com.openexchange.push.imapidle.ImapIdlePushManagerService;
import com.openexchange.push.imapidle.locking.HzImapIdleClusterLock;

/**
 * {@link ImapIdleRegisteringTracker}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class ImapIdleRegisteringTracker implements ServiceTrackerCustomizer<Object, Object> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ImapIdleRegisteringTracker.class);

    private final BundleContext context;
    private final ImapIdleActivator activator;
    private final ImapIdleConfiguration configuration;
    private final Lock lock = new ReentrantLock();
    private final boolean hazelcastRequired;
    private ImapIdlePushManagerService imapIdlePushManager;
    private ServiceRegistration<PushManagerService> reg;
    private HazelcastInstance hzInstance;
    private MailProviderRegistration imapRegistration;
    private HazelcastConfigurationService hzConfigService;

    /**
     * Initializes a new {@link ImapIdleRegisteringTracker}.
     */
    public ImapIdleRegisteringTracker(boolean hazelcastRequired, ImapIdleConfiguration configuration, ImapIdleActivator activator, BundleContext context) {
        super();
        this.context = context;
        this.configuration = configuration;
        this.activator = activator;
        this.hazelcastRequired = hazelcastRequired;
    }

    /**
     * Gets the associated filter expression
     *
     * @return The filter
     * @throws InvalidSyntaxException If filter cannot be generated
     */
    public Filter getFilter() throws InvalidSyntaxException {
        if (hazelcastRequired) {
            return Tools.generateServiceFilter(context, MailProviderRegistration.class, HazelcastInstance.class, HazelcastConfigurationService.class);
        }

        return Tools.generateServiceFilter(context, MailProviderRegistration.class);
    }

    private boolean allAvailable() {
        return hazelcastRequired ? (null != imapRegistration && null != hzInstance && null != hzConfigService) : (null != imapRegistration);
    }

    @Override
    public Object addingService(ServiceReference<Object> reference) {
        Object service = context.getService(reference);
        lock.lock();
        try {
            if ((service instanceof HazelcastInstance)) {
                if (false == hazelcastRequired) {
                    context.ungetService(reference);
                    return null;
                }
                this.hzInstance = (HazelcastInstance) service;
            } else if ((service instanceof MailProviderRegistration)) {
                MailProviderRegistration providerRegistration = (MailProviderRegistration) service;
                String protocol = providerRegistration.getRegisteredProvider();
                try {
                    Protocol p = Protocol.parseProtocol(protocol);
                    if (false == p.isSupported("imap")) {
                        context.ungetService(reference);
                        return null;
                    }

                    this.imapRegistration = providerRegistration;
                } catch (OXException e) {
                    LOG.error("Failed to handle registered MailProviderRegistration", e);
                }
            } else if ((service instanceof HazelcastConfigurationService)) {
                if (false == hazelcastRequired) {
                    context.ungetService(reference);
                    return null;
                }
                this.hzConfigService = (HazelcastConfigurationService) service;
            } else {
                // Huh...?
                context.ungetService(reference);
                return null;
            }

            if (allAvailable()) {
                init();
            }
        } finally {
            lock.unlock();
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference, Object service) {
        // Nothing
    }

    @Override
    public void removedService(ServiceReference<Object> reference, Object service) {
        boolean someServiceMissing = false;
        lock.lock();
        try {
            if ((service instanceof HazelcastInstance)) {
                if (this.hzInstance != null) {
                    this.hzInstance = null;
                    someServiceMissing = true;
                }
            } else if ((service instanceof MailProviderRegistration)) {
                if (this.imapRegistration != null) {
                    try {
                        MailProviderRegistration providerRegistration = (MailProviderRegistration) service;
                        String protocol = providerRegistration.getRegisteredProvider();
                        if (null != protocol && Protocol.parseProtocol(protocol).isSupported("imap")) {
                            this.imapRegistration = null;
                            someServiceMissing = true;
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to handle unregistered MailProviderRegistration", e);
                    }
                }
            } else if ((service instanceof HazelcastConfigurationService)) {
                if (this.hzConfigService != null) {
                    this.hzConfigService = null;
                    someServiceMissing = true;
                }
            }

            if (null != reg && someServiceMissing) {
                stop();
            }
        } finally {
            lock.unlock();
        }
        context.ungetService(reference);
    }

    private void init() {
        if (null != reg) {
            // Already registered
            return;
        }

        try {
            if (hazelcastRequired) {
                boolean hzEnabled = hzConfigService.isEnabled();
                if (false == hzEnabled) {
                    String msg = "IMAP-IDLE is configured to use Hazelcast-based locking, but Hazelcast is disabled as per configuration! Start of IMAP-IDLE aborted!";
                    LOG.error(msg, new Exception(msg));
                    return;
                }

                String mapName = discoverMapName(hzInstance.getConfig());
                ((HzImapIdleClusterLock) configuration.getClusterLock()).setMapName(mapName);
                activator.addService(HazelcastInstance.class, hzInstance);
            }

            imapIdlePushManager = ImapIdlePushManagerService.newInstance(configuration, activator);
            reg = context.registerService(PushManagerService.class, imapIdlePushManager, null);
        } catch (Exception e) {
            LOG.warn("Failed start-up for {}", context.getBundle().getSymbolicName(), e);
        }

    }

    private void stop() {
        if (null == reg) {
            // Already unregistered
            return;
        }

        imapIdlePushManager.shutDown();
        imapIdlePushManager = null;

        reg.unregister();
        reg = null;
    }

    /**
     * Discovers the map name from the supplied Hazelcast configuration.
     *
     * @param config The config object
     * @return The sessions map name
     * @throws IllegalStateException
     */
    private String discoverMapName(Config config) throws IllegalStateException {
        Map<String, MapConfig> mapConfigs = config.getMapConfigs();
        if (null != mapConfigs && 0 < mapConfigs.size()) {
            for (String mapName : mapConfigs.keySet()) {
                if (mapName.startsWith("imapidle-")) {
                    LOG.info("Using distributed IMAP-IDLE map '{}'.", mapName);
                    return mapName;
                }
            }
        }
        String msg = "No distributed IMAP-IDLE map found in hazelcast configuration";
        throw new IllegalStateException(msg, new BundleException(msg, BundleException.ACTIVATOR_ERROR));
    }

}
