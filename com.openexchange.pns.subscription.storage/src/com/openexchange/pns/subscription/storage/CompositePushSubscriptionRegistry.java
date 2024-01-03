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

package com.openexchange.pns.subscription.storage;

import static com.openexchange.java.Autoboxing.I;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.pns.DefaultPushSubscription;
import com.openexchange.pns.Hits;
import com.openexchange.pns.IteratorBackedHits;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushSubscription;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionListener;
import com.openexchange.pns.PushSubscriptionProvider;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.pns.PushSubscriptionResult;
import com.openexchange.pns.subscription.storage.rdb.RdbPushSubscriptionRegistry;


/**
 * {@link CompositePushSubscriptionRegistry}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class CompositePushSubscriptionRegistry implements PushSubscriptionRegistry {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CompositePushSubscriptionRegistry.class);

    private final RdbPushSubscriptionRegistry persistentRegistry;
    private final ServiceListing<PushSubscriptionProvider> providers;
    private final ServiceListing<PushSubscriptionListener> listeners;

    /**
     * Initializes a new {@link CompositePushSubscriptionRegistry}.
     */
    public CompositePushSubscriptionRegistry(RdbPushSubscriptionRegistry persistentRegistry, ServiceListing<PushSubscriptionProvider> providers, ServiceListing<PushSubscriptionListener> listeners) {
        super();
        this.persistentRegistry = persistentRegistry;
        this.providers = providers;
        this.listeners = listeners;
    }

    @Override
    public boolean hasInterestedSubscriptions(int userId, int contextId, String topic) throws OXException {
        return hasInterestedSubscriptions(null, userId, contextId, topic);
    }

    @Override
    public boolean hasInterestedSubscriptions(String clientId, int userId, int contextId, String topic) throws OXException {
        {
            boolean hasAny = null == clientId ? persistentRegistry.hasInterestedSubscriptions(userId, contextId, topic) : persistentRegistry.hasInterestedSubscriptions(clientId, userId, contextId, topic);
            if (hasAny) {
                return true;
            }
        }

        for (PushSubscriptionProvider provider : providers) {
            boolean hasAny = null == clientId ? provider.hasInterestedSubscriptions(userId, contextId, topic) : provider.hasInterestedSubscriptions(clientId, userId, contextId, topic);
            if (hasAny) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Hits getInterestedSubscriptions(int[] userIds, int contextId, String topic) throws OXException {
        return getInterestedSubscriptions(null, userIds, contextId, topic);
    }

    @Override
    public Hits getInterestedSubscriptions(String clientId, int[] userIds, int contextId, String topic) throws OXException {
        Map<ClientAndTransport, List<PushMatch>> map = null;

        {
            Hits currentHits;
            if (null == clientId) {
                currentHits = persistentRegistry.getInterestedSubscriptions(userIds, contextId, topic);
            } else {
                currentHits = persistentRegistry.getInterestedSubscriptions(clientId, userIds, contextId, topic);
            }
            if (false == currentHits.isEmpty()) {
                map = ((MapBackedHits) currentHits).getMap();
            }
        }

        // Build hits from queried registries
        MapBackedHits hits = null == map ? null : new MapBackedHits(map);

        // Check for more hits from providers
        LinkedList<Hits> moreHits = null;
        for (PushSubscriptionProvider provider : providers) {
            Hits currentHits;
            if (null == clientId) {
                currentHits = provider.getInterestedSubscriptions(userIds, contextId, topic);
            } else {
                currentHits = provider.getInterestedSubscriptions(clientId, userIds, contextId, topic);
            }
            if (false == currentHits.isEmpty()) {
                if (null == moreHits) {
                    moreHits = new LinkedList<>();
                }
                moreHits.add(currentHits);
            }
        }

        if (null == moreHits) {
            return null == hits ? Hits.EMPTY_HITS : hits;
        }

        if (null != hits) {
            moreHits.addFirst(hits);
        }

        return new IteratorBackedHits(moreHits);
    }

    @Override
    public PushSubscriptionResult registerSubscription(PushSubscriptionDescription desc, PushSubscriptionRestrictions restrictions) throws OXException {
        List<PushSubscriptionListener> listeners = this.listeners.getServiceList();
        for (PushSubscriptionListener listener : listeners) {
            try {
                if (!listener.addingSubscription(desc, restrictions)) {
                    LOG.info("Listener {} denied registration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), desc.getTopics(), I(desc.getUserId()), I(desc.getContextId()));
                    OXException error = PushExceptionCodes.PUSH_SUBSCRIPTION_DENIED.create(desc.toString());
                    return PushSubscriptionResult.builder().withError(error).build();
                }
            } catch (Exception e) {
                LOG.error("Listener {} failed handling registration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), desc.getTopics(), I(desc.getUserId()), I(desc.getContextId()), e);
            }
        }

        PushSubscriptionResult result = persistentRegistry.registerSubscription(desc, restrictions);

        if (PushSubscriptionResult.Status.OK == result.getStatus() && !listeners.isEmpty()) {
            DefaultPushSubscription subscription = DefaultPushSubscription.instanceFor(desc);
            for (PushSubscriptionListener listener : listeners) {
                try {
                    listener.addedSubscription(subscription);
                } catch (Exception e) {
                    LOG.error("Listener {} failed handling performed registration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), desc.getTopics(), I(desc.getUserId()), I(desc.getContextId()), e);
                }
            }
        }

        return result;
    }

    @Override
    public boolean unregisterSubscription(PushSubscriptionDescription desc, PushSubscriptionRestrictions restrictions) throws OXException {
        List<PushSubscriptionListener> listeners = this.listeners.getServiceList();
        for (PushSubscriptionListener listener : listeners) {
            try {
                listener.removingSubscription(desc, restrictions);
            } catch (Exception e) {
                LOG.error("Listener {} failed handling performed unregistration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), desc.getTopics(), I(desc.getUserId()), I(desc.getContextId()), e);
            }
        }

        PushSubscription removedSubscription = persistentRegistry.removeSubscription(DefaultPushSubscription.instanceFor(desc), restrictions);
        if (null == removedSubscription) {
            // Nothing removed
            return false;
        }

        for (PushSubscriptionListener listener : listeners) {
            try {
                listener.removedSubscription(removedSubscription);
            } catch (Exception e) {
                LOG.error("Listener {} failed handling performed unregistration of subscription with topics '{}' for user {} in context {}", listener.getClass().getSimpleName(), removedSubscription.getTopics(), I(removedSubscription.getUserId()), I(removedSubscription.getContextId()), e);
            }
        }
        return true;
    }

    @Override
    public int unregisterSubscription(String token, String transportId) throws OXException {
        return persistentRegistry.unregisterSubscription(token, transportId);
    }

    @Override
    public boolean updateToken(PushSubscriptionDescription subscription, String newToken) throws OXException {
        return persistentRegistry.updateToken(subscription, newToken);
    }

}
