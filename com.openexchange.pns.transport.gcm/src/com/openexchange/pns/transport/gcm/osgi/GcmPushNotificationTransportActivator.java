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

package com.openexchange.pns.transport.gcm.osgi;

import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.transport.gcm.internal.GcmPushNotificationTransport;
import com.openexchange.push.clients.PushClientProviderFactory;


/**
 * {@link GcmPushNotificationTransportActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class GcmPushNotificationTransportActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link ApnPushNotificationTransportActivator}.
     */
    public GcmPushNotificationTransportActivator() {
        super();
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected Class<?>[] getNeededServices() {
        // @formatter:off
        return new Class<?>[] { ConfigurationService.class,
                                PushSubscriptionRegistry.class,
                                PushMessageGeneratorRegistry.class,
                                ConfigViewFactory.class,
                                PushClientProviderFactory.class };
        // @formatter:on
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        // @formatter:off
        GcmPushNotificationTransport transport = new GcmPushNotificationTransport(getServiceSafe(PushSubscriptionRegistry.class),
                                                                                  getServiceSafe(PushMessageGeneratorRegistry.class),
                                                                                  getServiceSafe(ConfigViewFactory.class),
                                                                                  getServiceSafe(PushClientProviderFactory.class));
        // @formatter:on
        registerService(PushNotificationTransport.class, transport);
        registerService(ForcedReloadable.class, (ConfigurationService configService) -> transport.invalidateEnabledCache());
    }

}
