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

package com.openexchange.pns.transport.websocket.internal;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Functions;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.EnabledKey;
import com.openexchange.pns.Hits;
import com.openexchange.pns.IteratorBackedHits;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.Message;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushMessageGenerator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotification;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionProvider;
import com.openexchange.pns.Token;
import com.openexchange.pns.transport.websocket.WebSocketClient;
import com.openexchange.pns.transport.websocket.WebSocketToClientResolver;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.UserAndContext;
import com.openexchange.websockets.WebSocketService;

/**
 * {@link WebSocketPushNotificationTransport}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class WebSocketPushNotificationTransport implements PushNotificationTransport, PushSubscriptionProvider {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(WebSocketPushNotificationTransport.class);

    /** The identifier of the Web Socket transport */
    static final String ID = KnownTransport.WEB_SOCKET.getTransportId();

    private static final String TOKEN_PREFIX = "ws::";

    // ---------------------------------------------------------------------------------------------------------------

    private final ConfigViewFactory configViewFactory;
    private final WebSocketToClientResolverRegistry resolvers;
    private final WebSocketService webSocketService;
    private final PushMessageGeneratorRegistry generatorRegistry;

    /**
     * Initializes a new {@link WebSocketPushNotificationTransport}.
     */
    public WebSocketPushNotificationTransport(WebSocketToClientResolverRegistry resolvers, ServiceLookup services) {
        super();
        this.resolvers = resolvers;
        this.webSocketService = services.getService(WebSocketService.class);
        this.generatorRegistry = services.getService(PushMessageGeneratorRegistry.class);
        this.configViewFactory = services.getService(ConfigViewFactory.class);
    }

    /**
     * Stops this Web Socket transport.
     */
    public void stop() {
        // Nothing to do (so far)
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * Creates the artificial Web Socket subscription token for specified arguments; e.g.
     * <pre>"ws::17-1337::open-xchange-appsuite"</pre>
     *
     * @param client The client identifier
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The appropriate token to use
     */
    static Token createTokenFor(String client, int userId, int contextId) {
        return DefaultToken.tokenFor(new StringBuilder(24).append(TOKEN_PREFIX).append(userId).append('-').append(contextId).append("::").append(client).toString());
    }

    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean hasInterestedSubscriptions(int userId, int contextId, String topic) throws OXException {
        // Remember checked clients
        Map<WebSocketClient, Boolean> checkedOnes = new LinkedHashMap<>();

        // Check resolvers
        for (WebSocketToClientResolver resolver : resolvers) {
            Map<String, WebSocketClient> clients = resolver.getSupportedClients();
            for (WebSocketClient client : clients.values()) {
                Boolean exists = checkedOnes.get(client);
                if (null == exists) {
                    String pathFilter = client.getPathFilter();
                    try {
                        boolean found = webSocketService.exists(pathFilter, userId, contextId);
                        if (found) {
                            LOG.debug("Found open Web Socket for client '{}' ({}) satisfying topic '{}' for user {} in context {}", client.getClient(), client.getPathFilter(), topic, I(userId), I(contextId));
                        } else {
                            LOG.debug("No such open Web Socket for client '{}' ({}) satisfying topic '{}' for user {} in context {}", client.getClient(), client.getPathFilter(), topic, I(userId), I(contextId));
                        }
                        exists = Boolean.valueOf(found);
                    } catch (OXException e) {
                        LOG.error("Failed to check for any open filter-satisfying Web Socket using filter \"{}\" for user {} in context {}. Assuming there is any...", null == pathFilter ? "<none>" : pathFilter, I(userId), I(contextId), e);
                        exists = Boolean.TRUE;
                    }
                    checkedOnes.put(client, exists);
                }
                if (exists.booleanValue()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean hasInterestedSubscriptions(String clientId, int userId, int contextId, String topic) throws OXException {
        // Check resolvers
        for (WebSocketToClientResolver resolver : resolvers) {
            Map<String, WebSocketClient> clients = resolver.getSupportedClients();
            WebSocketClient wsClient = clients.get(clientId);
            if (null != wsClient) {
                String pathFilter = wsClient.getPathFilter();
                Boolean exists;
                try {
                    boolean found = webSocketService.exists(pathFilter, userId, contextId);
                    if (found) {
                        LOG.debug("Found open Web Socket for client '{}' ({}) satisfying topic '{}' for user {} in context {}", wsClient.getClient(), wsClient.getPathFilter(), topic, I(userId), I(contextId));
                    } else {
                        LOG.debug("No such open Web Socket for client '{}' ({}) satisfying topic '{}' for user {} in context {}", wsClient.getClient(), wsClient.getPathFilter(), topic, I(userId), I(contextId));
                    }
                    exists = Boolean.valueOf(found);
                } catch (OXException e) {
                    LOG.error("Failed to check for any open filter-satisfying Web Socket using filter \"{}\" for user {} in context {}. Assuming there is any...", null == pathFilter ? "<none>" : pathFilter, I(userId), I(contextId), e);
                    exists = Boolean.TRUE;
                }
                if (exists.booleanValue()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Hits getInterestedSubscriptions(final int[] userIds, final int contextId, String topic) throws OXException {
        List<Hits> hitsList = null;
        for (int userId : userIds) {
            Optional<Hits> optionalHits = getInterestedSubscriptions(userId, contextId, topic);
            if (optionalHits.isPresent()) {
                if (hitsList == null) {
                    hitsList = new ArrayList<Hits>();
                }
                hitsList.add(optionalHits.get());
            }
        }
        return hitsList == null ? Hits.EMPTY_HITS : new IteratorBackedHits(hitsList);
    }

    private Optional<Hits> getInterestedSubscriptions(final int userId, final int contextId, String topic) {
        LOG.debug("Checking for interested Web Socket subscriptions for topic '{}' for user {} in context {}", topic, I(userId), I(contextId));

        // Remember checked clients
        Map<WebSocketClient, Boolean> checkedOnes = new LinkedHashMap<>();
        Set<WebSocketClient> interestedAndHasOpenWebSocket = new LinkedHashSet<>();

        // Check resolvers
        for (WebSocketToClientResolver resolver : resolvers) {
            LOG.debug("Using '{}' to determine interested Web Socket subscriptions for topic '{}' for user {} in context {}", resolver.getClass().getSimpleName(), topic, I(userId), I(contextId));
            for (WebSocketClient client : resolver.getSupportedClients().values()) {
                Boolean exists = checkedOnes.computeIfAbsent(client, c -> Boolean.valueOf(isWebSocketClientInterestedAndHasOpenWebSocket(client, userId, contextId, topic)));
                if (exists.booleanValue()) {
                    interestedAndHasOpenWebSocket.add(client);
                }
            }
        }

        if (interestedAndHasOpenWebSocket.isEmpty()) {
            // No client found, which is interested in topic and has an open Web Socket
            return Optional.empty();
        }

        // Advertise subscription for each client that has an open Web Socket
        return Optional.of(new WebSocketHits(interestedAndHasOpenWebSocket, userId, contextId, topic));
    }

    private boolean isWebSocketClientInterestedAndHasOpenWebSocket(WebSocketClient client, final int userId, final int contextId, String topic) {
        LOG.debug("Checking client '{}' ({}) for open Web Sockets subscriptions for topic '{}' for user {} in context {}", client.getClient(), client.getPathFilter(), topic, I(userId), I(contextId));
        try {
            boolean found = client.isInterestedIn(topic) && webSocketService.exists(client.getPathFilter(), userId, contextId);
            if (found) {
                LOG.debug("Found open Web Socket for client '{}' ({}) satisfying topic '{}' for user {} in context {}", client.getClient(), client.getPathFilter(), topic, I(userId), I(contextId));
            } else {
                LOG.debug("No such open Web Socket for client '{}' ({}) satisfying topic '{}' for user {} in context {}", client.getClient(), client.getPathFilter(), topic, I(userId), I(contextId));
            }
            return found;
        } catch (OXException e) {
            LOG.error("Failed to check for any open filter-satisfying Web Socket using filter \"{}\" for user {} in context {}. Assuming there is any...", null == client.getPathFilter() ? "<none>" : client.getPathFilter(), I(userId), I(contextId), e);
            return true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Hits getInterestedSubscriptions(String clientId, int[] userIds, int contextId, String topic) throws OXException {
        List<Hits> hitsList = new ArrayList<Hits>();
        for (int userId : userIds) {
            Hits hits = null != clientId ? getInterestedSubscriptions(clientId, userId, contextId, topic) : getInterestedSubscriptions(userId, contextId, topic).orElse(Hits.EMPTY_HITS);
            if (null != hits && !hits.isEmpty()) {
                hitsList.add(hits);
            }
        }
        return new IteratorBackedHits(hitsList);
    }

    private Hits getInterestedSubscriptions(String clientId, int userId, int contextId, String topic) {
        // Check resolvers
        for (WebSocketToClientResolver resolver : resolvers) {
            LOG.debug("Using '{}' to determine interested Web Socket subscriptions for client {} and topic '{}' for user {} in context {}", resolver.getClass().getSimpleName(), clientId, topic, I(userId), I(contextId));
            Map<String, WebSocketClient> clients = resolver.getSupportedClients();
            WebSocketClient client = clients.get(clientId);
            if (null != client && isWebSocketClientInterestedAndHasOpenWebSocket(client, userId, contextId, topic)) {
                return new WebSocketHits(Collections.singleton(client), userId, contextId, topic);
            }
        }

        return Hits.EMPTY_HITS;
    }

    // ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean servesClient(String client) throws OXException {
        return resolvers.getAllSupportedClients().containsKey(client);
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

        String basePropertyName = "com.openexchange.pns.transport.websocket.enabled";

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

        // Default is "true"
        return true;
    }

    @Override
    public void transport(PushNotification notification, Collection<PushMatch> matches) throws OXException {
        if (null != notification && null != matches && !matches.isEmpty()) {
            // Verify
            for (PushMatch match : matches) {
                if (notification.getContextId() != match.getContextId()) {
                    throw OXException.general("Passed wrong push match for notification with topic \"" + notification.getTopic() + "\" for user " + notification.getUserId() + " in context " + notification.getContextId() + ": Wrong context identifier in " + match);
                }
                if (notification.getUserId() != match.getUserId()) {
                    throw OXException.general("Passed wrong push match for notification with topic \"" + notification.getTopic() + "\" for user " + notification.getUserId() + " in context " + notification.getContextId() + ": Wrong user identifier in " + match);
                }
            }

            // Only care about clients for Web Sockets
            Set<String> clients = extractClientsFrom(matches);

            // Determine associated client and path filter
            Map<String, ClientAndPathFilter> clientToFilter = getResolveResultsFor(clients);
            if (null == clientToFilter) {
                // No appropriate client/path-filter associated with specified matches
                return;
            }

            // Deliver per client
            for (String client : clients) {
                ClientAndPathFilter clientAndPathFilter = clientToFilter.get(client);
                if (null != clientAndPathFilter) {
                    // There is a known path association for current client
                    transport(notification, clientAndPathFilter);
                }
            }
        }
    }

    private void transport(PushNotification notification, ClientAndPathFilter clientAndPathFilter) throws OXException {
        String client = clientAndPathFilter.getClient();
        PushMessageGenerator generator = generatorRegistry.getGenerator(client, ID);
        if (null == generator) {
            // Assume generic one
            generator = generatorRegistry.getGenerator(PushMessageGenerator.ID_GENERIC_SOCKET_IO, ID);
            if (null == generator) {
                throw PushExceptionCodes.NO_SUCH_GENERATOR.create(client, ID);
            }
        }

        Message<?> message = generator.generateMessageFor(ID, notification);

        // Get & send message's textual representation
        String textMessage = message.getMessage().toString();
        int userId = notification.getUserId();
        int contextId = notification.getContextId();
        LOG.debug("Going to send notification \"{}\" via transport '{}' for user {} in context {}", notification.getTopic(), ID, I(userId), I(contextId));
        webSocketService.sendMessage(textMessage, notification.getSourceToken(), clientAndPathFilter.getPathFilter(), userId, contextId);
    }

    private Map<String, ClientAndPathFilter> getResolveResultsFor(Set<String> clients) throws OXException {
        Map<String, ClientAndPathFilter> applicable = null;
        for (String client : clients) {
            for (WebSocketToClientResolver resolver : resolvers) {
                String pathFilter = resolver.getPathFilterFor(client);
                if (pathFilter == null) {
                    // Client unknown
                    LOG.warn("Client \"{}\" is unknown, hence cannot resolve it to an appropriate path filter expression.", client);
                } else {
                    if (null == applicable) {
                        applicable = new HashMap<>(clients.size());
                    }
                    applicable.put(client, new ClientAndPathFilter(client, pathFilter));
                }
            }
        }
        return applicable;
    }

    private static Set<String> extractClientsFrom(Collection<PushMatch> userMatches) {
        int numberOfUserMatches = userMatches.size();
        if (1 == numberOfUserMatches) {
            return Collections.singleton(userMatches.iterator().next().getClient());
        }

        return userMatches.stream().map(m -> m.getClient()).collect(Collectors.toSet());
    }

    private static Map<UserAndContext, List<PushMatch>> groupByUser(Collection<PushMatch> matches) {
        if (1 == matches.size()) {
            PushMatch match = matches.iterator().next();
            return Collections.singletonMap(UserAndContext.newInstance(match.getUserId(), match.getContextId()), Collections.singletonList(match));
        }

        Map<UserAndContext, List<PushMatch>> byUser = new LinkedHashMap<>();

        // Only one match is needed as there is no difference per match
        for (PushMatch match : matches) {
            byUser.computeIfAbsent(UserAndContext.newInstance(match.getUserId(), match.getContextId()), Functions.getNewLinkedListFuntion()).add(match);
        }
        return byUser;
    }

}
