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

package com.openexchange.pns.transport.wns.internal;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.Functions;
import com.openexchange.java.SortableConcurrentList;
import com.openexchange.osgi.util.RankedService;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.EnabledKey;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.Message;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushMessageGenerator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotification;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.pns.transport.wns.WnsOptions;
import com.openexchange.pns.transport.wns.WnsOptionsProvider;
import ar.com.fernandospr.wns.WnsService;
import ar.com.fernandospr.wns.exceptions.WnsException;
import ar.com.fernandospr.wns.model.WnsNotificationResponse;
import ar.com.fernandospr.wns.model.WnsText;
import ar.com.fernandospr.wns.model.WnsTile;
import ar.com.fernandospr.wns.model.builders.WnsTileBuilder;


/**
 * {@link WnsPushNotificationTransport}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class WnsPushNotificationTransport extends ServiceTracker<WnsOptionsProvider, WnsOptionsProvider> implements PushNotificationTransport {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(WnsPushNotificationTransport.class);

    private static final String ID = KnownTransport.WNS.getTransportId();

    private static final int MAX_TOTAL_BYTE_SIZE = 5000;

    // ---------------------------------------------------------------------------------------------------------------

    private final ConfigViewFactory configViewFactory;
    private final PushSubscriptionRegistry subscriptionRegistry;
    private final PushMessageGeneratorRegistry generatorRegistry;
    private final SortableConcurrentList<RankedService<WnsOptionsProvider>> trackedProviders;
    private ServiceRegistration<PushNotificationTransport> registration; // non-volatile, protected by synchronized blocks

    /**
     * Initializes a new {@link ApnPushNotificationTransport}.
     */
    public WnsPushNotificationTransport(PushSubscriptionRegistry subscriptionRegistry, PushMessageGeneratorRegistry generatorRegistry, ConfigViewFactory configViewFactory, BundleContext context) {
        super(context, WnsOptionsProvider.class, null);
        this.configViewFactory = configViewFactory;
        this.trackedProviders = new SortableConcurrentList<RankedService<WnsOptionsProvider>>();
        this.subscriptionRegistry = subscriptionRegistry;
        this.generatorRegistry = generatorRegistry;
    }

    // ---------------------------------------------------------------------------------------------------------

    @Override
    public synchronized WnsOptionsProvider addingService(ServiceReference<WnsOptionsProvider> reference) {
        int ranking = RankedService.getRanking(reference);
        WnsOptionsProvider provider = context.getService(reference);

        trackedProviders.addAndSort(new RankedService<WnsOptionsProvider>(provider, ranking));

        if (null == registration) {
            registration = context.registerService(PushNotificationTransport.class, this, null);
        }

        return provider;
    }

    @Override
    public void modifiedService(ServiceReference<WnsOptionsProvider> reference, WnsOptionsProvider provider) {
        // Nothing
    }

    @Override
    public synchronized void removedService(ServiceReference<WnsOptionsProvider> reference, WnsOptionsProvider provider) {
        trackedProviders.remove(new RankedService<WnsOptionsProvider>(provider, RankedService.getRanking(reference)));

        if (trackedProviders.isEmpty() && null != registration) {
            registration.unregister();
            registration = null;
        }

        context.ungetService(reference);
    }

    // ---------------------------------------------------------------------------------------------------------

    private WnsOptions getHighestRankedWnsOptionsFor(String client) throws OXException {
        List<RankedService<WnsOptionsProvider>> list = trackedProviders.getSnapshot();
        for (RankedService<WnsOptionsProvider> rankedService : list) {
            WnsOptions options = rankedService.service.getOptions(client);
            if (null != options) {
                return options;
            }
        }
        throw PushExceptionCodes.UNEXPECTED_ERROR.create("No options found for client: " + client);
    }

    @Override
    public boolean servesClient(String client) throws OXException {
        try {
            return null != getHighestRankedWnsOptionsFor(client);
        } catch (OXException x) {
            return false;
        } catch (RuntimeException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    private static final Cache<EnabledKey, Boolean> CACHE_AVAILABILITY = CacheBuilder.newBuilder().maximumSize(65536).expireAfterWrite(30, TimeUnit.MINUTES).build();

    /**
     * Invalidates the <i>enabled cache</i>.
     */
    public static void invalidateEnabledCache() {
        CACHE_AVAILABILITY.invalidateAll();
    }

    @Override
    public boolean isEnabled(String topic, String client, int userId, int contextId) throws OXException {
        EnabledKey key = new EnabledKey(topic, client, userId, contextId);
        Boolean result = CACHE_AVAILABILITY.getIfPresent(key);
        if (null == result) {
            result = Boolean.valueOf(doCheckEnabled(topic, client, userId, contextId));
            CACHE_AVAILABILITY.put(key, result);
        }
        return result.booleanValue();
    }

    private boolean doCheckEnabled(String topic, String client, int userId, int contextId) throws OXException {
        ConfigView view = configViewFactory.getView(userId, contextId);

        String basePropertyName = "com.openexchange.pns.transport.wns.enabled";

        ComposedConfigProperty<Boolean> property;
        property = null == topic || null == client ? null : view.property(basePropertyName + "." + client + "." + topic, boolean.class);
        if (null != property) {
            Boolean value = property.get();
            if (value != null) {
                return value.booleanValue();
            }
        }

        property = null == client ? null : view.property(basePropertyName + "." + client, boolean.class);
        if (null != property) {
            Boolean value = property.get();
            if (value != null) {
                return value.booleanValue();
            }
        }

        property = view.property(basePropertyName, boolean.class);
        if (null != property) {
            Boolean value = property.get();
            if (value != null) {
                return value.booleanValue();
            }
        }

        return false;
    }

    @Override
    public void transport(Map<PushNotification, List<PushMatch>> notifications) throws OXException {
        if (null != notifications && 0 < notifications.size()) {
            for (Map.Entry<PushNotification, List<PushMatch>> entry : notifications.entrySet()) {
                transport(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void transport(PushNotification notification, Collection<PushMatch> matches) throws OXException {
        if (null != notification && null != matches) {
            Map<String, List<PushMatch>> clientMatches = categorizeByClient(matches);
            for (Map.Entry<String, List<com.openexchange.pns.PushMatch>> entry : clientMatches.entrySet()) {
                transport(entry.getKey(), notification, entry.getValue());
            }
        }
    }

    private void transport(String client, PushNotification notification, List<PushMatch> matches) throws OXException {
        if (null == notification || null == matches) {
            return;
        }

        LOG.debug("Going to send notification \"{}\" via transport '{}' for user {} in context {}", notification.getTopic(), ID, I(notification.getUserId()), I(notification.getContextId()));
        int size = matches.size();
        if (size <= 0) {
            return;
        }

        WnsService service = optWnsService(client);
        if (null == service) {
            return;
        }

        for (PushMatch match : matches) {
            String channelUri = match.getToken().getValue();
            WnsTile tile = getTile(client, notification);

            try {
                boolean stop = false;
                for (int retryCount = 0; stop && retryCount < 6; retryCount++) {
                    stop = true;
                    WnsNotificationResponse response = service.pushTile(channelUri, tile);

                    int code = response.code;
                    if (code == 200) {
                        // The notification was accepted by WNS. Check status as well
                        String status = response.notificationStatus;
                        if (ar.com.fernandospr.wns.model.types.WnsNotificationStatusType.RECEIVED.equals(status)) {
                            // All fine
                            LOG.info("Sent notification \"{}\" via transport '{}' to channel URI \"{}\" for user {} in context {}", notification.getTopic(), ID, channelUri, I(notification.getUserId()), I(notification.getContextId()));
                            retryCount = 6;
                        } else if (ar.com.fernandospr.wns.model.types.WnsNotificationStatusType.DROPPED.equals(status)) {
                            // The notification was explicitly dropped because of an error or because the client has explicitly rejected these notifications.
                            LOG.warn("WNS service dropped push notification to channel URI {}", channelUri);
                            retryCount = 6;
                        } else if (ar.com.fernandospr.wns.model.types.WnsNotificationStatusType.CHANNELTHROTTLED.equals(status)) {
                            // The notification was dropped because the app server exceeded the rate limit for this specific channel.
                            // Handle rate limit using exponential backoff
                            int retry = retryCount + 1;
                            if (retry > 5) {
                                // Repeatedly exceeded rate limit
                                throw new WnsException("Repeatedly exceeded rate limit. Surrender publishing push notification to channel URI: " + channelUri);
                            }
                            long nanosToWait = TimeUnit.NANOSECONDS.convert((retry * 1000) + ((long) (Math.random() * 1000)), TimeUnit.MILLISECONDS);
                            LockSupport.parkNanos(nanosToWait);
                            stop = false;
                        }
                    } else if (code == 403) {
                        // The cloud service is not authorized to send a notification to this URI even though they are authenticated.
                        // The access token provided in the request does not match the credentials of the app that requested the channel URI.
                        // Ensure that your package name in your app's manifest matches the cloud service credentials given to your app in the Dashboard.
                        LOG.warn("WNS service denied push notification to channel URI: {}", channelUri);
                    } else if (code == 404) {
                        // The channel URI is not valid or is not recognized by WNS.
                        // Do not send further notifications to this channel; notifications to this address will fail.
                        LOG.warn("Channel URI invalid: {}", channelUri);
                        removeChannelUri(notification, channelUri);
                    } else if (code == 406) {
                        // Handle rate limit using exponential backoff
                        int retry = retryCount + 1;
                        if (retry > 5) {
                            // Repeatedly exceeded rate limit
                            throw new WnsException("Repeatedly exceeded rate limit. Surrender publishing push notification to channel URI: " + channelUri);
                        }
                        long nanosToWait = TimeUnit.NANOSECONDS.convert((retry * 1000) + ((long) (Math.random() * 1000)), TimeUnit.MILLISECONDS);
                        LockSupport.parkNanos(nanosToWait);
                        stop = false;
                    } else if (code == 413) {
                        // The notification payload exceeds the 5000 byte size limit.
                        throw PushExceptionCodes.MESSAGE_TOO_BIG.create(I(MAX_TOTAL_BYTE_SIZE), I(-1));
                    } else {
                        // All other...
                        String errorDescription = response.errorDescription;
                        if (null == errorDescription) {
                            errorDescription = "<unknown>";
                        }
                        String deviceConnectionStatus = response.deviceConnectionStatus;
                        if (null == deviceConnectionStatus) {
                            deviceConnectionStatus = "";
                        } else {
                            deviceConnectionStatus = deviceConnectionStatus + " ";
                        }
                        LOG.warn("Failed publishing push notification to {}device using channel URI {} with status code {}: {}", deviceConnectionStatus, channelUri, I(code), errorDescription);
                    }
                }

            } catch (WnsException e) {
                LOG.warn("Error publishing push notification to channel URI", channelUri, e);
            }
        }
    }

    private Map<String, List<PushMatch>> categorizeByClient(Collection<PushMatch> matches) throws OXException {
        Map<String, List<PushMatch>> clientMatches = new LinkedHashMap<>(matches.size());
        for (PushMatch match : matches) {
            String client = match.getClient();

            // Check generator availability
            {
                PushMessageGenerator generator = generatorRegistry.getGenerator(match);
                if (null == generator) {
                    throw PushExceptionCodes.NO_SUCH_GENERATOR.create(client, ID);
                }
            }

            clientMatches.computeIfAbsent(client, Functions.getNewLinkedListFuntion()).add(match);
        }
        return clientMatches;
    }

    private WnsTile getTile(String client, PushNotification notification) throws OXException {
        PushMessageGenerator generator = generatorRegistry.getGenerator(client, ID);
        if (null == generator) {
            throw PushExceptionCodes.NO_SUCH_GENERATOR.create(client, ID);
        }

        Message<?> message = generator.generateMessageFor(ID, notification);
        Object object = message.getMessage();
        if (object instanceof WnsTile) {
            return checkWnsTile((WnsTile) object);
        } else if (object instanceof Map) {
            return checkWnsTile(toWnsTile((Map<String, Object>) object));
        } else {
            throw PushExceptionCodes.UNSUPPORTED_MESSAGE_CLASS.create(null == object ? "null" : object.getClass().getName());
        }
    }

    private static WnsTile checkWnsTile(WnsTile tile) throws OXException {
        List<WnsText> texts = tile.visual.binding.texts;
        long length = 0;
        Charset cs = Charsets.UTF_8;
        for (WnsText wnsText : texts) {
            String value = wnsText.value;
            if (null != value) {
                length += value.getBytes(cs).length;
            }
        }
        if (length > MAX_TOTAL_BYTE_SIZE) {
            throw PushExceptionCodes.MESSAGE_TOO_BIG.create(I(MAX_TOTAL_BYTE_SIZE), L(length));
        }

        return tile;
    }

    private static WnsTile toWnsTile(Map<String, Object> message) {
        WnsTileBuilder tileBuilder = new WnsTileBuilder();

        Map<String, Object> source = new HashMap<>(message);

        {
            String text = (String) source.remove("text");
            if (null != text) {
                tileBuilder.bindingTemplateTileWideText03(text);
            }
        }

        return tileBuilder.build();
    }

    /**
     * (Optionally) Gets a WNS service based on the configured API key for given client.
     *
     * @param client The client
     * @return The WNS service or <code>null</code>
     */
    private WnsService optWnsService(String client) {
        try {
            return getWnsService(client);
        } catch (Exception e) {
            LOG.error("Error getting WNS service", e);
        }
        return null;
    }

    /**
     * Gets a WNS service based on the configured API key.
     *
     * @param client The client
     * @return The WNS service
     * @throws OXException If WNS service cannot be returned
     */
    private WnsService getWnsService(String client) throws OXException {
        WnsOptions wnsOptions = getHighestRankedWnsOptionsFor(client);
        return new WnsService(wnsOptions.getSid(), wnsOptions.getSecret(), false);
    }

    private boolean removeChannelUri(PushNotification notification, String channelUri) {
        try {
            PushSubscriptionDescription desc = new PushSubscriptionDescription()
                .setContextId(notification.getContextId())
                .setToken(DefaultToken.tokenFor(channelUri))
                .setTransportId(ID)
                .setUserId(notification.getUserId());

            boolean success = subscriptionRegistry.unregisterSubscription(desc, new PushSubscriptionRestrictions());
            if (success) {
                LOG.info("Successfully removed channel URI {}.", channelUri);
                return true;
            }
        } catch (OXException e) {
            LOG.error("Error removing subscription", e);
        }
        LOG.warn("Channel URI {} not removed.", channelUri);
        return false;
    }

}
