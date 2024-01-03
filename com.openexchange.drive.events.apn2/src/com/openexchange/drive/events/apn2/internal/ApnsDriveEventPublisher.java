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

package com.openexchange.drive.events.apn2.internal;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import com.openexchange.apn.common.APNClient;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.drive.events.DriveEvent;
import com.openexchange.drive.events.DriveEventPublisher;
import com.openexchange.drive.events.subscribe.DriveSubscriptionStore;
import com.openexchange.drive.events.subscribe.Subscription;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.push.clients.PushClientProvider;
import com.openexchange.push.clients.PushClientProviderFactory;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;

/**
 * {@link ApnsDriveEventPublisher} is a {@link DriveEventPublisher} which publishes drive events via apples notification service
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 *
 * @since v7.10.1
 */
public abstract class ApnsDriveEventPublisher implements DriveEventPublisher {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ApnsDriveEventPublisher.class);

    private final ServiceLookup services;
    private final String serviceId;
    private final PushClientProvider<APNClient> provider;
    private final Property propertyEnabled;
    private final Property propertyClientId;

    /**
     * Initializes a new {@link ApnsDriveEventPublisher}.
     *
     * @param services The tracked OSGi services
     * @param serviceId The service id
     * @param propertyEnabled The property defining whether publishing is enabled or not
     * @param propertyClientId The property defining the push client configuration id
     * @throws OXException
     */
    protected ApnsDriveEventPublisher(ServiceLookup services, String serviceId, Property propertyEnabled, Property propertyClientId) throws OXException {
        super();
        this.services = services;
        this.serviceId = serviceId;
        this.propertyEnabled = propertyEnabled;
        this.propertyClientId = propertyClientId;
        provider = services.getServiceSafe(PushClientProviderFactory.class).createProvider(APNClient.class);
    }

    /**
     * Creates the task to deliver a drive event notification for a certain subscription.
     * 
     * @param subscription The subscription to create the delivery task for
     * @param event The drive event to create the delivery task for
     * @param client The underlying APN client to use
     * @return The subscription delivery task
     */
    protected abstract Task<Void> createSubscriptionDeliveryTask(Subscription subscription, DriveEvent event, APNClient client);

    @Override
    public boolean isLocalOnly() {
        return true;
    }

    @Override
    public void publish(DriveEvent event) {
        // Query available subscriptions
        List<Subscription> subscriptions = null;
        try {
            DriveSubscriptionStore subscriptionStore = services.getService(DriveSubscriptionStore.class);
            subscriptions = subscriptionStore.getSubscriptions(event.getContextID(), new String[] { this.serviceId }, event.getUserIDs());
        } catch (Exception e) {
            LOG.error("unable to get subscriptions for service {}", this.serviceId, e);
        }

        if (null == subscriptions) {
            // Nothing to do
            return;
        }

        int numOfSubscriptions = subscriptions.size();
        if (numOfSubscriptions == 0) {
            // Nothing to do
            return;
        }

        // Send push notification & handle response
        ThreadPoolService threadPool = services.getService(ThreadPoolService.class);
        if (null == threadPool || numOfSubscriptions == 1) {
            publishToSubscribers(subscriptions, event, (subscription, task) -> {
                try {
                    ThreadPools.execute(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("Interrupted while sending push notification for drive event for device token {}", subscription.getToken(), e);
                    return Boolean.FALSE;
                } catch (Exception e) {
                    LOG.warn("Failed sending push notification for drive event to device with token {}", subscription.getToken(), e);
                }
                return Boolean.TRUE;
            });
        } else {
            publishToSubscribers(subscriptions, event, (subscription, task) -> {
                threadPool.submit(task, CallerRunsBehavior.getInstance());
                return Boolean.TRUE;
            });
        }
    }

    /**
     * Publishes the given drive events to the given subscribers
     *
     * @param subscriptions The subscriptions
     * @param event The event to publish
     * @param executor The executer for the delivery task
     */
    private void publishToSubscribers(List<Subscription> subscriptions, DriveEvent event, BiFunction<Subscription, Task<Void>, Boolean> executor) {
        LeanConfigurationService configService = services.getService(LeanConfigurationService.class);
        if (null == configService) {
            LOG.error("Can't publish drive event to subscribers due to missing configuration service.",
                ServiceExceptionCode.absentService(LeanConfigurationService.class));
            return;
        }
        for (Subscription subscription : subscriptions) {
            /*
             * check if push is applicable and enabled for target user
             */
            if (null != event.getPushTokenReference() && subscription.matches(event.getPushTokenReference())) {
                LOG.trace("Skipping notification due to matching push token for subscription: {}", subscription);
                continue;
            }
            int userId = subscription.getUserID();
            int contextId = subscription.getContextID();
            if (false == event.getFolderIDs(userId).contains(subscription.getRootFolderID())) {
                LOG.trace("Skipping notification due to mismatching folder for subscription: {}", subscription);
                continue;
            }
            if (false == configService.getBooleanProperty(userId, contextId, propertyEnabled)) {
                LOG.trace("Push via {} is disabled for user {} in context {}.", this.serviceId, I(userId), I(contextId));
                continue;
            }
            /*
             * get associated push client id
             */
            String clientId = configService.getProperty(userId, contextId, propertyClientId);
            if (Strings.isEmpty(clientId)) {
                LOG.error("No push client id configured for user {} in context {}, unable to publish drive event.", I(userId), I(contextId));
                continue;
            }
            Optional<APNClient> optClient = provider.optClient(clientId);
            if (optClient.isEmpty()) {
                LOG.error("Unable to get push client with id {}, unable to publish drive event.", clientId);
                continue;
            }
            /*
             * create and execute subscription delivery task
             */
            if (false == b(executor.apply(subscription, createSubscriptionDeliveryTask(subscription, event, optClient.get())))) {
                // Unable to continue e.g. because of an InterruptedException
                return;
            }
        }
    }

}
