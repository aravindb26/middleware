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

package com.openexchange.pns.transport.apns_http2.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.pns.EnabledKey;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotification;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.SuffixAwarePushClientProvider;
import com.openexchange.apn.common.APNClient;
import com.openexchange.pns.transport.apns_http2.util.ApnsHttp2PushPerformer;
import com.openexchange.push.clients.PushClientProvider;
import com.openexchange.push.clients.PushClientProviderFactory;

/**
 * {@link ApnsHttp2PushNotificationTransport}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class ApnsHttp2PushNotificationTransport implements PushNotificationTransport {

    private static final String ID = KnownTransport.APNS_HTTP2.getTransportId();

    // ---------------------------------------------------------------------------------------------------------------

    private final ConfigViewFactory configViewFactory;
    private final PushSubscriptionRegistry subscriptionRegistry;
    private final PushMessageGeneratorRegistry generatorRegistry;
    private final PushClientProvider<APNClient> clientProvider;
    private final Cache<EnabledKey, Boolean> cache_availability = CacheBuilder.newBuilder().maximumSize(65536).expireAfterWrite(30, TimeUnit.MINUTES).build();

    /**
     * Initializes a new {@link ApnsHttp2PushNotificationTransport}.
     *
     * @param subscriptionRegistry
     * @param generatorRegistry
     * @param configViewFactory
     * @param providerFactory
     */
    public ApnsHttp2PushNotificationTransport(PushSubscriptionRegistry subscriptionRegistry, PushMessageGeneratorRegistry generatorRegistry, ConfigViewFactory configViewFactory, PushClientProviderFactory providerFactory) {
        this.configViewFactory = configViewFactory;
        this.generatorRegistry = generatorRegistry;
        this.subscriptionRegistry = subscriptionRegistry;
        clientProvider = new SuffixAwarePushClientProvider<APNClient>(providerFactory.createProvider(APNClient.class), "-ios");
    }

    @Override
    public boolean servesClient(String client) throws OXException {
        try {
            return clientProvider.optClient(client).isPresent();
        } catch (RuntimeException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Invalidates the <i>enabled cache</i>.
     */
    public void invalidateEnabledCache() {
        cache_availability.invalidateAll();
    }

    @Override
    public boolean isEnabled(String topic, String client, int userId, int contextId) throws OXException {
        EnabledKey key = new EnabledKey(topic, client, userId, contextId);
        Boolean result = cache_availability.getIfPresent(key);
        if (null == result) {
            result = Boolean.valueOf(doCheckEnabled(topic, client, userId, contextId));
            cache_availability.put(key, result);
        }
        return result.booleanValue();
    }

    private boolean doCheckEnabled(String topic, String client, int userId, int contextId) throws OXException {
        ConfigView view = configViewFactory.getView(userId, contextId);

        String basePropertyName = "com.openexchange.pns.transport.apns_http2.ios.enabled";

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
        new ApnsHttp2PushPerformer(ID, subscriptionRegistry, generatorRegistry, clientProvider).sendPush(notifications);
    }

    @Override
    public void transport(PushNotification notification, Collection<PushMatch> matches) throws OXException {
        if (null != notification && null != matches) {
            List<PushMatch> pushMatches = new ArrayList<PushMatch>(matches);
            transport(Collections.singletonMap(notification, pushMatches));
        }
    }

}
