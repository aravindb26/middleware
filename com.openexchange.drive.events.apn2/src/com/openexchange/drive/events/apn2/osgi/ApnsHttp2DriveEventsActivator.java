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

package com.openexchange.drive.events.apn2.osgi;

import static com.openexchange.drive.events.apn2.internal.DriveEventsAPNProperty.IOS_FILEPROVIDER_CLIENTID;
import static com.openexchange.drive.events.apn2.internal.DriveEventsAPNProperty.IOS_FILEPROVIDER_ENABLED;
import static com.openexchange.drive.events.apn2.internal.DriveEventsAPNProperty.IOS_LEGACY_CLIENTID;
import static com.openexchange.drive.events.apn2.internal.DriveEventsAPNProperty.IOS_LEGACY_ENABLED;
import static com.openexchange.drive.events.apn2.internal.DriveEventsAPNProperty.MACOS_FILEPROVIDER_CLIENTID;
import static com.openexchange.drive.events.apn2.internal.DriveEventsAPNProperty.MACOS_FILEPROVIDER_ENABLED;
import com.openexchange.apn.common.APNClient;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.drive.events.DriveEvent;
import com.openexchange.drive.events.DriveEventService;
import com.openexchange.drive.events.apn2.internal.ApnsDriveEventPublisher;
import com.openexchange.drive.events.apn2.internal.tasks.IOSApns2SubscriptionDeliveryTask;
import com.openexchange.drive.events.apn2.internal.tasks.IOSApnsSubscriptionDeliveryTask;
import com.openexchange.drive.events.apn2.internal.tasks.MacOSApns2SubscriptionDeliveryTask;
import com.openexchange.drive.events.subscribe.DriveSubscriptionStore;
import com.openexchange.drive.events.subscribe.Subscription;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.push.clients.PushClientProviderFactory;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.TimerService;

/**
 * {@link ApnsHttp2DriveEventsActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.1
 */
public class ApnsHttp2DriveEventsActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ApnsHttp2DriveEventsActivator.class);

    private static final String IOS_FILEPROVIDER_SERVICE_ID = "apn2";
    private static final String IOS_LEGACY_SERVICE_ID = "apn";
    private static final String MACOS_FILEPROVIDER_SERVICE_ID = "apn2.macos";

    /**
     * Initializes a new {@link ApnsHttp2DriveEventsActivator}.
     */
    public ApnsHttp2DriveEventsActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        // @formatter:off
        return new Class<?>[] { DriveEventService.class,
                                DriveSubscriptionStore.class,
                                LeanConfigurationService.class,
                                TimerService.class,
                                ThreadPoolService.class,
                                PushClientProviderFactory.class };
        // @formatter:on
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("starting bundle: {}", context.getBundle().getSymbolicName());
        DriveEventService driveEventService = getServiceSafe(DriveEventService.class);
        /*
         * register publishers for fileprovider- and legacy iOS client, using client-specific notification payload generator tasks
         */
        driveEventService.registerPublisher(new ApnsDriveEventPublisher(this, IOS_FILEPROVIDER_SERVICE_ID, IOS_FILEPROVIDER_ENABLED, IOS_FILEPROVIDER_CLIENTID) {

            @Override
            protected Task<Void> createSubscriptionDeliveryTask(Subscription subscription, DriveEvent event, APNClient client) {
                return new IOSApns2SubscriptionDeliveryTask(subscription, event, client, ApnsHttp2DriveEventsActivator.this);
            }
        });
        driveEventService.registerPublisher(new ApnsDriveEventPublisher(this, IOS_LEGACY_SERVICE_ID, IOS_LEGACY_ENABLED, IOS_LEGACY_CLIENTID) {

            @Override
            protected Task<Void> createSubscriptionDeliveryTask(Subscription subscription, DriveEvent event, APNClient client) {
                return new IOSApnsSubscriptionDeliveryTask(subscription, event, client, ApnsHttp2DriveEventsActivator.this);
            }
        });

        /*
         * register publisher for fileprovider macOS client, using client-specific notification payload generator tasks
         */
        driveEventService.registerPublisher(new ApnsDriveEventPublisher(this, MACOS_FILEPROVIDER_SERVICE_ID, MACOS_FILEPROVIDER_ENABLED, MACOS_FILEPROVIDER_CLIENTID) {

            @Override
            protected Task<Void> createSubscriptionDeliveryTask(Subscription subscription, DriveEvent event, APNClient client) {
                return new MacOSApns2SubscriptionDeliveryTask(subscription, event, client, ApnsHttp2DriveEventsActivator.this);
            }
        });
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("stopping bundle: {}", context.getBundle().getSymbolicName());
        super.stopBundle();
    }

}
