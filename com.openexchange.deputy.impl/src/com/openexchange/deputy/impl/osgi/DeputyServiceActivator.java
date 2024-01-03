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

package com.openexchange.deputy.impl.osgi;

import static com.openexchange.java.Autoboxing.I;
import java.util.Dictionary;
import java.util.Hashtable;
import org.slf4j.Logger;
import com.openexchange.capabilities.CapabilityChecker;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.FailureAwareCapabilityChecker;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.context.ContextService;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.deputy.DeputyModuleProvider;
import com.openexchange.deputy.DeputyService;
import com.openexchange.deputy.impl.DeputyModuleProviderRegistry;
import com.openexchange.deputy.impl.DeputyServiceImpl;
import com.openexchange.deputy.impl.groupware.DeputyStorageCreateTableService;
import com.openexchange.deputy.impl.groupware.DeputyStorageCreateTableTask;
import com.openexchange.deputy.impl.groupware.DeputyStorageDeleteListener;
import com.openexchange.deputy.impl.storage.DeputyStorage;
import com.openexchange.deputy.impl.storage.rdb.RdbDeputyStorage;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.update.DefaultUpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link DeputyServiceActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyServiceActivator extends HousekeepingActivator {

    /** The logger constant */
    static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DeputyServiceActivator.class);

    /**
     * Initializes a new {@link DeputyServiceActivator}.
     */
    public DeputyServiceActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { DatabaseService.class, ConfigViewFactory.class, CapabilityService.class, UserService.class,
            MailAccountStorageService.class, ContextService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle {}", context.getBundle().getSymbolicName());

        DeputyModuleProviderRegistry providerRegistry = new DeputyModuleProviderRegistry(context);
        track(DeputyModuleProvider.class, providerRegistry);
        openTrackers();

        DeputyStorage deputyStorage = new RdbDeputyStorage(this);

        DeputyServiceImpl deputyService = new DeputyServiceImpl(providerRegistry, deputyStorage, this);
        registerService(DeputyService.class, deputyService);

        // Register Groupware stuff.
        registerService(CreateTableService.class, DeputyStorageCreateTableService.getInstance());
        registerService(UpdateTaskProviderService.class, new DefaultUpdateTaskProviderService(
            new DeputyStorageCreateTableTask()
        ));
        registerService(DeleteListener.class, new DeputyStorageDeleteListener());


        // Announce GDPR data export available
        {
            final ServiceLookup services = this;
            final String sCapability = DeputyService.CAPABILITY_DEPUTY;
            Dictionary<String, Object> properties = new Hashtable<String, Object>(1);
            properties.put(CapabilityChecker.PROPERTY_CAPABILITIES, sCapability);
            registerService(CapabilityChecker.class, new FailureAwareCapabilityChecker() {

                @Override
                public FailureAwareCapabilityChecker.Result checkEnabled(String capability, Session session) {
                    if (sCapability.equals(capability)) {
                        if (session == null || session.getContextId() <= 0 || session.getUserId() <= 0) {
                            return FailureAwareCapabilityChecker.Result.DISABLED;
                        }

                        try {
                            boolean enabled = deputyService.isAvailable(session);
                            if (!enabled) {
                                return FailureAwareCapabilityChecker.Result.DISABLED;
                            }

                            User user = getUserFor(session, services);
                            if (user.isAnonymousGuest() || user.isGuest()) {
                                return FailureAwareCapabilityChecker.Result.DISABLED;
                            }
                        } catch (Exception e) {
                            LOG.warn("Failed to check if deputy service is enabled for user {} in context {}", I(session.getUserId()), I(session.getContextId()), e);
                            return FailureAwareCapabilityChecker.Result.FAILURE;
                        }
                    }

                    return FailureAwareCapabilityChecker.Result.ENABLED;
                }

                private User getUserFor(Session session, ServiceLookup services) throws OXException {
                    return session instanceof ServerSession ? ((ServerSession) session).getUser() : services.getServiceSafe(UserService.class).getUser(session.getUserId(), session.getContextId());
                }

            }, properties);
            getService(CapabilityService.class).declareCapability(sCapability);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        LOG.info("Stopped bundle {}", context.getBundle().getSymbolicName());
    }

}
