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

package com.openexchange.drive.events.apn2.internal.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.openexchange.apn.common.APNClient;
import com.openexchange.drive.events.DriveContentChange;
import com.openexchange.drive.events.DriveEvent;
import com.openexchange.drive.events.apn2.internal.ApnsDriveEventPublisher;
import com.openexchange.drive.events.apn2.util.ApnsHttp2Notification;
import com.openexchange.drive.events.apn2.util.SubscriptionDeliveryTask;
import com.openexchange.drive.events.subscribe.Subscription;
import com.openexchange.drive.events.subscribe.SubscriptionMode;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link IOSApns2SubscriptionDeliveryTask} is a {@link SubscriptionDeliveryTask} for apns2 based {@link ApnsDriveEventPublisher} for iOS
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.4
 */
public class IOSApns2SubscriptionDeliveryTask extends SubscriptionDeliveryTask {

    /**
     * Initializes a new {@link IOSApns2SubscriptionDeliveryTask}.
     *
     * @param subscription The subscription
     * @param event The drive event
     * @param apnClient The apn client
     * @param services Service lookup
     */
    public IOSApns2SubscriptionDeliveryTask(Subscription subscription, DriveEvent event, APNClient apnClient, ServiceLookup services) {
        super(subscription, event, apnClient, services);
    }

    @Override
    protected SimpleApnsPushNotification getNotification(DriveEvent event, Subscription subscription, APNClient apnClient) {
        String pushTokenReference = event.getPushTokenReference();
        if (null != pushTokenReference && subscription.matches(pushTokenReference)) {
            return null;
        }
        if (null == apnClient) {
            return null;
        }
        ApnsHttp2Notification.Builder builder = new ApnsHttp2Notification.Builder(subscription.getToken(), apnClient.optTopic().orElse(null))
            .withCustomField("container-identifier", "NSFileProviderRootContainerItemIdentifier")
            .withExpiration(TimeUnit.DAYS.toMillis(1L))
        ;
        String domain = subscription.getDomain();
        if (Strings.isNotEmpty(domain)) {
            builder.withCustomField("domain", domain).withCollapseId(domain);
        }
        if (event.isContentChangesOnly() && SubscriptionMode.SEPARATE.equals(subscription.getMode())) {
            List<String> folderIds = new ArrayList<String>();
            for (DriveContentChange contentChange : event.getContentChanges()) {
                if (contentChange.isSubfolderOf(subscription.getUserID(), subscription.getRootFolderID())) {
                    folderIds.add(contentChange.getFolderId());
                }
            }
            if (false == folderIds.isEmpty()) {
                builder.withCustomField("folderIds", folderIds);
            }
        }
        builder.withPriority(DeliveryPriority.CONSERVE_POWER);
        builder.withPushType(PushType.FILEPROVIDER);
        return builder.build();
    }

}
