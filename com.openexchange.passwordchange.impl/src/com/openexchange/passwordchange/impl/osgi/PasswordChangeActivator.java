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

package com.openexchange.passwordchange.impl.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.caching.CacheService;
import com.openexchange.capabilities.CapabilityChecker;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.groupware.userconfiguration.service.PermissionAvailabilityService;
import com.openexchange.guest.GuestService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.password.mechanism.PasswordMechRegistry;
import com.openexchange.passwordchange.PasswordChangeRegistry;
import com.openexchange.passwordchange.PasswordChangeService;
import com.openexchange.passwordchange.impl.EditPasswordCapabilityChecker;
import com.openexchange.passwordchange.impl.PasswordChangeRegistryImpl;
import com.openexchange.passwordchange.impl.database.DatabasePasswordChangeService;
import com.openexchange.passwordchange.impl.script.ScriptPasswordChangeService;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link PasswordChangeActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PasswordChangeActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link PasswordChangeActivator}
     */
    public PasswordChangeActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { UserPermissionService.class, UserService.class, GuestService.class, PasswordMechRegistry.class, AuthenticationServiceRegistry.class, //
            SessiondService.class, EventAdmin.class, CacheService.class, LeanConfigurationService.class, CapabilityService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        /*
         * Register service tracker and register registry
         */
        PasswordChangeRegistryImpl registry = new PasswordChangeRegistryImpl(context);
        rememberTracker(registry);
        rememberTracker(new ServiceTracker<>(context, CapabilityService.class, new ServiceTrackerCustomizer<CapabilityService, CapabilityService>() {

            @SuppressWarnings("synthetic-access")
            @Override
            public CapabilityService addingService(ServiceReference<CapabilityService> reference) {
                /*
                 * Indirectly announce availability of the password change services to the capability service
                 */
                registerService(PermissionAvailabilityService.class, new PermissionAvailabilityService(Permission.EDIT_PASSWORD));
                return context.getService(reference);
            }

            @Override
            public void modifiedService(ServiceReference<CapabilityService> reference, CapabilityService service) {

            }

            @SuppressWarnings("synthetic-access")
            @Override
            public void removedService(ServiceReference<CapabilityService> reference, CapabilityService service) {
                unregisterService(PermissionAvailabilityService.class);

            }
        }));
        openTrackers();
        registerService(PasswordChangeRegistry.class, registry);
        /*
         * Register core implementations
         */
        registerService(PasswordChangeService.class, new DatabasePasswordChangeService(this));
        registerService(PasswordChangeService.class, new ScriptPasswordChangeService(this));
        /*
         * Register capability check
         */
        {
            Dictionary<String, Object> properties = new Hashtable<String, Object>(1);
            properties.put(CapabilityChecker.PROPERTY_CAPABILITIES, EditPasswordCapabilityChecker.EDIT_PASSWORD_CAP);
            registerService(CapabilityChecker.class, new EditPasswordCapabilityChecker(this, registry), properties);

            getServiceSafe(CapabilityService.class).declareCapability(EditPasswordCapabilityChecker.EDIT_PASSWORD_CAP);
        }
    }

}
