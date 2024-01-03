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

package com.openexchange.drive.events.gcm.internal;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import com.google.android.gcm.Constants;
import com.google.android.gcm.Message;
import com.google.android.gcm.Result;
import com.google.android.gcm.Sender;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.drive.events.DriveContentChange;
import com.openexchange.drive.events.DriveEvent;
import com.openexchange.drive.events.DriveEventPublisher;
import com.openexchange.drive.events.subscribe.DriveSubscriptionStore;
import com.openexchange.drive.events.subscribe.Subscription;
import com.openexchange.drive.events.subscribe.SubscriptionMode;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.push.clients.PushClientProvider;
import com.openexchange.push.clients.PushClientProviderFactory;
import com.openexchange.server.ServiceLookup;

/**
 * {@link GCMDriveEventPublisher}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class GCMDriveEventPublisher implements DriveEventPublisher {

    private static final String SERIVCE_ID = "gcm";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GCMDriveEventPublisher.class);

    private final ServiceLookup services;
    private final PushClientProvider<Sender> provider;

    /**
     * Initializes a new {@link GCMDriveEventPublisher}.
     *
     * @param services The service lookup reference
     * @throws OXException
     */
    public GCMDriveEventPublisher(ServiceLookup services) throws OXException {
        super();
        this.services = services;
        provider = services.getServiceSafe(PushClientProviderFactory.class).createProvider(Sender.class);
    }

    @Override
    public boolean isLocalOnly() {
        return true;
    }

    @Override
    public void publish(DriveEvent event) {
        /*
         * get subscriptions from storage
         */
        DriveSubscriptionStore subscriptionStore = null;
        List<Subscription> subscriptions = null;
        try {
            subscriptionStore = services.getServiceSafe(DriveSubscriptionStore.class);
            subscriptions = subscriptionStore.getSubscriptions(event.getContextID(), new String[] { SERIVCE_ID }, event.getUserIDs());
        } catch (OXException e) {
            LOG.error("unable to get subscriptions for service {}", SERIVCE_ID, e);
        }
        /*
         * get associated push client ids for relevant subscriptions and obtain corresponding clients
         */
        for (Entry<String, List<Subscription>> entry : getSubscriptionsPerClientId(subscriptions, event).entrySet()) {
            Optional<Sender> optSender = provider.optClient(entry.getKey());
            if (optSender.isEmpty()) {
                LOG.debug("No push client configured for push via FCM with client id {}, skipping notifications.", entry.getKey());
                continue;
            }
            for (Subscription subscription : entry.getValue()) {
                /*
                 * generate & send individual push message for each subscription
                 */
                Message message = getMessage(event, subscription);
                Result result;
                try {
                    result = optSender.get().sendNoRetry(message, subscription.getToken());
                } catch (IOException e) {
                    LOG.warn("error publishing drive event", e);
                    continue;
                }
                /*
                 * process result
                 */
                LOG.debug("{}", result != null ? result : "");
                processResult(subscriptionStore, event.getContextID(), subscription.getToken(), result);
            }
        }
    }

    /*
     * http://developer.android.com/google/gcm/http.html#success
     */
    private void processResult(DriveSubscriptionStore subscriptionStore, int contextID, String registrationID, Result result) {
        if (null == result) {
            return;
        }
        if (null != result.getMessageId()) {
            /*
             * If message_id is set, check for registration_id:
             */
            if (null != result.getCanonicalRegistrationId()) {
                /*
                 * If registration_id is set, replace the original ID with the new value (canonical ID) in your server database.
                 * Note that the original ID is not part of the result, so you need to obtain it from the list of
                 * code>registration_ids passed in the request (using the same index).
                 */
                updateRegistrationIDs(subscriptionStore, contextID, registrationID, result.getCanonicalRegistrationId());
            }
        } else {
            /*
             * Otherwise, get the value of error:
             */
            String error = result.getErrorCodeName();
            if (Constants.ERROR_UNAVAILABLE.equals(error)) {
                /*
                 * If it is Unavailable, you could retry to send it in another request.
                 */
                LOG.warn("Push message could not be sent because the GCM servers were not available.");
            } else if (Constants.ERROR_NOT_REGISTERED.equals(error)) {
                /*
                 * If it is NotRegistered, you should remove the registration ID from your server database because the application
                 * was uninstalled from the device or it does not have a broadcast receiver configured to receive
                 * com.google.android.c2dm.intent.RECEIVE intents.
                 */
                removeRegistrations(subscriptionStore, contextID, registrationID);
            } else {
                /*
                 * Otherwise, there is something wrong in the registration ID passed in the request; it is probably a non-
                 * recoverable error that will also require removing the registration from the server database. See Interpreting
                 * an error response for all possible error values.
                 */
                LOG.warn("Received error {} when sending push message to {}, removing registration ID.", error, registrationID);
                removeRegistrations(subscriptionStore, contextID, registrationID);
            }
        }
    }

    private static void updateRegistrationIDs(DriveSubscriptionStore subscriptionStore, int contextID, String oldRegistrationID, String newRegistrationID) {
        try {
            if (subscriptionStore.updateToken(contextID, SERIVCE_ID, oldRegistrationID, newRegistrationID)) {
                 LOG.info("Successfully updated registration ID from {} to {}", oldRegistrationID, newRegistrationID);
            } else {
                LOG.warn("Registration ID {} not updated.", oldRegistrationID);
            }
        } catch (OXException e) {
            if ("DRV-0037".equals(e.getErrorCode())) {
                // Token is already registered, so delete obsolete registration instead
                LOG.warn("Registration ID {} already exists, removing obsolete registration ID {} instead.", newRegistrationID, oldRegistrationID);
                removeRegistrations(subscriptionStore, contextID, oldRegistrationID);
            } else {
                LOG.error("Error updating registration IDs", e);
            }
        }
    }

    private static void removeRegistrations(DriveSubscriptionStore subscriptionStore, int contextID, String registrationID) {
        try {
            if (0 < subscriptionStore.removeSubscriptions(contextID, SERIVCE_ID, registrationID)) {
                LOG.info("Successfully removed registration ID {}.", registrationID);
            } else {
                LOG.warn("Registration ID {} not removed.", registrationID);
            }
        } catch (OXException e) {
            LOG.error("Error removing registrations", e);
        }
    }

    private static Message getMessage(DriveEvent event, Subscription subscription) {
        Message.Builder builder = new Message.Builder()
            .collapseKey("TRIGGER_SYNC")
            .addData("action", "sync")
            .addData("root", subscription.getRootFolderID())
        ;
        if (event.isContentChangesOnly() && SubscriptionMode.SEPARATE.equals(subscription.getMode())) {
            int index = 0;
            for (DriveContentChange contentChange : event.getContentChanges()) {
                if (contentChange.isSubfolderOf(subscription.getUserID(), subscription.getRootFolderID())) {
                    builder.addData("path_" + index, contentChange.getPath(subscription.getUserID(), subscription.getRootFolderID()));
                    index++;
                }
            }
        }
        return builder.build();
    }

    private Map<String, List<Subscription>> getSubscriptionsPerClientId(List<Subscription> subscriptions, DriveEvent event) {
        if (null == subscriptions || subscriptions.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<Subscription>> subscriptionsPerClientId = new HashMap<String, List<Subscription>>();
        LeanConfigurationService configService = services.getService(LeanConfigurationService.class);
        if (null == configService) {
            LOG.warn("Unable to get configuration service to determine push client configuration.");
            return Collections.emptyMap();
        }
        for (Subscription subscription : subscriptions) {
            /*
             * check if applicable & enabled for user
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
            if (false == configService.getBooleanProperty(userId, contextId, DriveEventsGCMProperty.ENABLED)) {
                LOG.trace("Push via FCM is disabled for user {} in context {}, skipping notification.", I(userId), I(contextId));
                continue;
            }
            /*
             * get configured push client id
             */
            String clientId = configService.getProperty(userId, contextId, DriveEventsGCMProperty.CLIENT_ID);
            if (Strings.isEmpty(clientId)) {
                LOG.debug("No push client configured for push via FCM for subscription {}, skipping notification.", subscription);
                continue;
            }
            LOG.trace("Using configured client id {} for push via FCM for user {} in context {}.", clientId, I(userId), I(contextId));
            com.openexchange.tools.arrays.Collections.put(subscriptionsPerClientId, clientId, subscription);
        }
        return subscriptionsPerClientId;
    }

}
