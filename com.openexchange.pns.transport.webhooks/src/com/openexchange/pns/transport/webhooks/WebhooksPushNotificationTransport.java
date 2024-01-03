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

package com.openexchange.pns.transport.webhooks;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.pns.transport.webhooks.WebhooksPushNotificationMetrics.getRequestTimer;
import static com.openexchange.webhooks.WebhookInfos.getAuthorizationHeaderValue;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Functions;
import com.openexchange.java.Strings;
import com.openexchange.java.util.HttpStatusCode;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.EnabledKey;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.Meta;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushMessageGenerator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotification;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.pns.transport.webhooks.util.ClientSpecifiedWebhookInfo;
import com.openexchange.pns.transport.webhooks.util.NoValidUriAvailableException;
import com.openexchange.pns.transport.webhooks.util.WebhookInfoUtils;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.HttpClients;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.UserAndContext;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookProperty;
import com.openexchange.webhooks.WebhookRegistry;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link WebhooksPushNotificationTransport} - The push notification transport for Webhooks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhooksPushNotificationTransport implements PushNotificationTransport {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebhooksPushNotificationTransport.class);

    private static final String ID = KnownTransport.WEBHOOK.getTransportId();

    // ---------------------------------------------------------------------------------------------------------------

    private final HttpClientService httpClientService;
    private final ConfigViewFactory configViewFactory;
    private final PushSubscriptionRegistry subscriptionRegistry;
    private final PushMessageGeneratorRegistry generatorRegistry;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link WebhooksPushNotificationTransport}.
     *
     * @param httpClientService The HTTP client service
     * @param subscriptionRegistry The subscription registry
     * @param generatorRegistry The registry for known generators
     * @param configViewFactory The config-cascade service
     * @param services The service look-up for needed OSGi services
     */
    public WebhooksPushNotificationTransport(HttpClientService httpClientService, PushSubscriptionRegistry subscriptionRegistry, PushMessageGeneratorRegistry generatorRegistry, ConfigViewFactory configViewFactory, ServiceLookup services) {
        super();
        this.httpClientService = httpClientService;
        this.configViewFactory = configViewFactory;
        this.generatorRegistry = generatorRegistry;
        this.subscriptionRegistry = subscriptionRegistry;
        this.services = services;
    }

    @Override
    public boolean servesClient(String client) throws OXException {
        try {
            return generatorRegistry.getGenerator(client, ID) != null || generatorRegistry.getGenerator(PushMessageGenerator.ID_GENERIC_SOCKET_IO, ID) != null;
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

        String basePropertyName = WebhooksPushNotificationProperty.ENABLED.getFQPropertyName();

        ComposedConfigProperty<Boolean> property;
        property = null == topic || null == client ? null : view.property(basePropertyName + "." + client + "." + topic, boolean.class);
        if (null != property && property.isDefined()) {
            return property.get().booleanValue();
        }

        property = null == client ? null : view.property(basePropertyName + "." + client, boolean.class);
        if (null != property && property.isDefined()) {
            return property.get().booleanValue();
        }

        property = view.property(basePropertyName, boolean.class);
        if (null != property && property.isDefined()) {
            return property.get().booleanValue();
        }

        return WebhooksPushNotificationProperty.ENABLED.getDefaultValue(Boolean.class).booleanValue();
    }

    @Override
    public void transport(PushNotification notification, Collection<PushMatch> matches) throws OXException {
        if (null == notification || null == matches || matches.isEmpty()) {
            return;
        }

        // Verify
        for (PushMatch match : matches) {
            if (notification.getContextId() != match.getContextId()) {
                throw OXException.general("Passed wrong push match for notification with topic \"" + notification.getTopic() + "\" for user " + notification.getUserId() + " in context " + notification.getContextId() + ": Wrong context identifier in " + match);
            }
            if (notification.getUserId() != match.getUserId()) {
                throw OXException.general("Passed wrong push match for notification with topic \"" + notification.getTopic() + "\" for user " + notification.getUserId() + " in context " + notification.getContextId() + ": Wrong user identifier in " + match);
            }
        }

        // Check Webhook config
        UserWebhookConfig webhookConfig = getUserWebhookConfig(notification.getUserId(), notification.getContextId(), services);

        // Process by Webhook (client is the Webhook identifier)
        for (Map.Entry<String, List<PushMatch>> entry : groupByWebhook(matches).entrySet()) {
            transportForWebhook(entry.getKey(), notification, entry.getValue(), webhookConfig);
        }
    }

    /**
     * Transports give notification to user-associated matches of the same client.
     *
     * @param webhookId The Webhook identifier
     * @param notification The notification to transport
     * @param matches The matches
     * @param webhookConfig The use-sensitive configuration options for Webhooks
     * @throws OXException If transport of notification fails
     */
    private void transportForWebhook(String webhookId, PushNotification notification, List<PushMatch> matches, UserWebhookConfig webhookConfig) throws OXException {
        if (null == notification || null == matches) {
            return;
        }

        // Acquired needed Webhook registry
        WebhookRegistry webhookRegistry = services.getServiceSafe(WebhookRegistry.class);

        // Yield the message
        Object oMessage;
        {
            PushMessageGenerator generator = generatorRegistry.getGenerator(webhookId, ID);
            if (null == generator) {
                // Assume generic one
                generator = generatorRegistry.getGenerator(PushMessageGenerator.ID_GENERIC_SOCKET_IO, ID);
                if (null == generator) {
                    throw PushExceptionCodes.NO_SUCH_GENERATOR.create(webhookId, ID);
                }
            }
            oMessage = generator.generateMessageFor(ID, notification).getMessage();
        }

        // Try to inject time stamp
        long timestamp = System.currentTimeMillis();
        if (oMessage instanceof JSONObject jMessage) {
            jMessage.putSafe("timestamp", Long.valueOf(timestamp));
        }

        // Generate the text message to deliver
        String textMessage = oMessage.toString();

        // Extract Webhook infos from matches
        Set<WebhookExtract> absentWebhookInfos = new HashSet<>();
        Set<WebhookInfo> webhookInfos = extractWebhookInfosFrom(matches, absentWebhookInfos, webhookRegistry);
        deliverMessageToWebhooks(textMessage, timestamp, webhookInfos, notification, webhookConfig);

        // Drop non-existing ones
        for (WebhookExtract absentWebhook : absentWebhookInfos) {
            removeWebhookSubscription(notification, absentWebhook, "No such Webhook configured");
        }
    }

    private void deliverMessageToWebhooks(String textMessage, long timestamp, Set<WebhookInfo> webhookInfos, PushNotification notification, UserWebhookConfig webhookConfig) throws OXException {
        if (webhookInfos.isEmpty()) {
            return;
        }

        // Deliver for each Webhook
        for (WebhookInfo webhookInfo : webhookInfos) {
            if (!webhookConfig.enabledWebhookIds().contains(webhookInfo.getWebhookId())) {
                LOG.warn("Denied calling Webhook \"{}\" ({}) for topic {} for user {} in context {} since Webhook is not enabled. Check setting for property {}", webhookInfo.getWebhookId(), webhookInfo.getUri(), notification.getTopic(), I(notification.getUserId()), I(notification.getContextId()), WebhookProperty.ENABLED_IDS.getFQPropertyName());
            } else if (webhookConfig.httpsOnly() && !webhookInfo.isHttps()) {
                LOG.warn("Denied calling Webhook \"{}\" ({}) for topic {} for user {} in context {} since Webhook's end-point is not HTTPS. Check setting for property {}", webhookInfo.getWebhookId(), webhookInfo.getUri(), notification.getTopic(), I(notification.getUserId()), I(notification.getContextId()), WebhooksPushNotificationProperty.HTTPS_ONLY.getFQPropertyName());
            } else {
                LOG.debug("Calling Webhook \"{}\" ({}) for topic {} for user {} in context {} with message: {}", webhookInfo.getWebhookId(), webhookInfo.getUri(), notification.getTopic(), I(notification.getUserId()), I(notification.getContextId()), textMessage);
                transport(webhookInfo, notification, textMessage, timestamp);
            }
        }
    }

    private static final Cache<UserAndContext, UserWebhookConfig> CACHE_USER_CONFIG = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(7)).build();

    private static UserWebhookConfig getUserWebhookConfig(int userId, int contextId, ServiceLookup services) throws OXException {
        UserAndContext key = UserAndContext.newInstance(userId, contextId);

        UserWebhookConfig config = CACHE_USER_CONFIG.getIfPresent(key);
        if (config != null) {
            return config;
        }

        try {
            return CACHE_USER_CONFIG.get(key, () -> {
                LeanConfigurationService configurationService = services.getOptionalService(LeanConfigurationService.class);

                boolean httpsOnly;
                String sEnabledIds;
                if (configurationService == null) {
                    httpsOnly = WebhooksPushNotificationProperty.HTTPS_ONLY.getDefaultValue(Boolean.class).booleanValue();
                    sEnabledIds = WebhookProperty.ENABLED_IDS.getDefaultValue(String.class);
                } else {
                    httpsOnly = configurationService.getBooleanProperty(userId, contextId, WebhooksPushNotificationProperty.HTTPS_ONLY);
                    sEnabledIds = configurationService.getProperty(userId, contextId, WebhookProperty.ENABLED_IDS);
                }

                Set<String> enabledIds = Strings.isEmpty(sEnabledIds) ? Collections.emptySet() : new HashSet<>(Arrays.asList(Strings.splitBy(sEnabledIds, ',', true)));
                return new UserWebhookConfig(httpsOnly, enabledIds);
            });
        } catch (ExecutionException e) {
            throw OXException.general("Failed to determine enabled Webhooks", e);
        }
    }

    /**
     * A set of status codes implying that Webhook subscription should be removed
     * <ul>
     * <li> <code>403 Forbidden</code>
     * <li> <code>404 Not Found</code>
     * <li> <code>405 Method Not Allowed</code>
     * </ul>
     */
    private static final TIntSet REMOVE_SUBSCRIPTION_STATUS_CODES = new TIntHashSet(new int[] { HttpStatus.SC_NOT_FOUND , HttpStatus.SC_FORBIDDEN, HttpStatus.SC_METHOD_NOT_ALLOWED });

    /**
     * Transports specified text message via POST request to Webhook URI.
     *
     * @param webhookInfo The Webhook info (ID, URI, authentication infos, etc.)
     * @param notification The notification from which the text message was generated
     * @param textMessage The text message to send
     * @param timestamp The time stamp for the Webhook request
     * @throws OXException If sending text message fails
     */
    private void transport(WebhookInfo webhookInfo, PushNotification notification, String textMessage, long timestamp) throws OXException {
        URI uri = webhookInfo.getUri();
        HttpResponse response = null;
        HttpPost request = null;
        try {
            // Compile POST request to Webhook
            request = new HttpPost(uri);
            request.setEntity(new StringEntity(textMessage, ContentType.APPLICATION_JSON));

            // "Authorization" header value
            Optional<String> optAuthorizationHeaderValue = getAuthorizationHeaderValue(webhookInfo);
            if (optAuthorizationHeaderValue.isPresent()) {
                request.addHeader(HttpHeaders.AUTHORIZATION, optAuthorizationHeaderValue.get());
            }

            // Signing...
            if (Strings.isNotEmpty(webhookInfo.getSignatureSecret()) && Strings.isNotEmpty(webhookInfo.getSignatureHeaderName())) {
                // Generate signature through concatenating the version number, time stamp, and payload of the notification: v1:<time-stamp>:<request-payload>
                StringBuilder sb = new StringBuilder(128).append('v').append(webhookInfo.getVersion()).append(':').append(timestamp).append(':').append(textMessage);
                HashFunction hmacSha256 = Hashing.hmacSha256(webhookInfo.getSignatureSecret().getBytes(StandardCharsets.UTF_8));
                String signature = hmacSha256.hashBytes(sb.toString().getBytes(StandardCharsets.UTF_8)).toString();

                // Construct the final header value via: t=<time-stamp>,v1=<signature>
                sb.setLength(0);
                request.setHeader(webhookInfo.getSignatureHeaderName(), sb.append("t=").append(timestamp).append(", v").append(webhookInfo.getVersion()).append('=').append(signature).toString());
            }

            // Issue POST request & examine response
            HttpResponseAndStatusCode responseAndStatus = execute(request, webhookInfo.getWebhookId());
            response = responseAndStatus.response();
            int status = responseAndStatus.status();
            if (REMOVE_SUBSCRIPTION_STATUS_CODES.contains(status)) {
                removeWebhookSubscription(notification, webhookInfo.getWebhookId(), uri.toString(), "Webhook \"" + webhookInfo.getWebhookId() + "\" responded with HTTP status code " + status);
            }
            if (HttpStatusCode.isSucess(status)) {
                LOG.debug("HTTP request to Webhook \"{}\" ({}) yielded {} status code", webhookInfo.getWebhookId(), uri, I(status));
            } else {
                LOG.warn("HTTP request to Webhook \"{}\" ({}) yielded {} status code", webhookInfo.getWebhookId(), uri, I(status));
            }
        } catch (IOException e) {
            throw handleCommunicationError(webhookInfo, e);
        } finally {
            HttpClients.close(request, response);
        }
    }

    /**
     * Executes specified POST request against the Webhook associated with given Webhook identifier.
     *
     * @param request The POST request to execute
     * @param webhookId The Webhook identifier
     * @return The (measured) HTTP response and status code
     * @throws IOException If an I/O error occurs
     * @throws OXException If HttpClient cannot be obtained
     */
    private HttpResponseAndStatusCode execute(HttpPost request, String webhookId) throws IOException, OXException {
        // Get the HttpClient
        HttpClient httpClient = getHttpClient();

        // Execute request...
        String status = "UNKNOWN";
        int iStatus = -1;
        long dur = 0;
        long start = System.nanoTime();
        try {
            HttpResponse response = httpClient.execute(request);
            dur = System.nanoTime() - start;
            if (response == null) {
                status = "CLIENT_ERROR";
            } else {
                iStatus = response.getStatusLine().getStatusCode();
                HttpStatusCode statusCode = HttpStatusCode.httpStatusCodeFor(iStatus);
                if (statusCode != null) {
                    status = statusCode.name();
                }
            }
            return new HttpResponseAndStatusCode(response, iStatus);
        } catch (Exception e) {
            status = "ERROR";
            throw e;
        } finally {
            getRequestTimer(HttpStatusCode.isSucess(iStatus), webhookId, request.getRequestLine().getMethod(), status).record(dur, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Removes the Webhook subscription for specified Webhook.
     *
     * @param notification The notification that could not be delivered
     * @param webhookId The subscription's Webhook
     * @param uri The URI of the Webhook
     * @param reason The reason phrase why Webhook subscription shall be dropped
     * @return <code>true</code> if removed; otherwise <code>false</code> if removal attempt failed
     */
    private boolean removeWebhookSubscription(PushNotification notification, WebhookExtract webhook, String reason) {
        return removeWebhookSubscription(notification, webhook.webhookId, webhook.uri, reason);
    }

    /**
     * Removes the Webhook subscription for specified Webhook.
     *
     * @param notification The notification that could not be delivered
     * @param webhookId The subscription's Webhook
     * @param uri The URI of the Webhook
     * @param reason The reason phrase why Webhook subscription shall be dropped
     * @return <code>true</code> if removed; otherwise <code>false</code> if removal attempt failed
     */
    private boolean removeWebhookSubscription(PushNotification notification, String webhookId, String uri, String reason) {
        try {
            PushSubscriptionDescription desc = new PushSubscriptionDescription()
                .setClient(webhookId)
                .setContextId(notification.getContextId())
                .setToken(DefaultToken.tokenFor(uri))
                .setTransportId(ID)
                .setUserId(notification.getUserId());

            boolean success = subscriptionRegistry.unregisterSubscription(desc, new PushSubscriptionRestrictions());
            if (success) {
                LOG.info("Removed subscription for Webhook {} of user {} in context {}: {}", webhookId, I(notification.getUserId()), I(notification.getContextId()), reason);
                return true;
            }
            LOG.warn("Subscription for Webhook {} of user {} in context {} could not be removed.", webhookId, I(notification.getUserId()), I(notification.getContextId()));
        } catch (Exception e) {
            LOG.error("Error removing subscription for Webhook {} of user {} in context {}", webhookId, I(notification.getUserId()), I(notification.getContextId()), e);
        }
        return false;
    }

    /**
     * Handles communication errors. If the end-point is not available it is blacklisted.
     *
     * @param webhookInfo The Webhook info
     * @param e The exception
     * @return An OXException to re-throw
     */
    private static OXException handleCommunicationError(WebhookInfo webhookInfo, IOException e) {
        if (e instanceof org.apache.http.conn.ConnectionPoolTimeoutException) {
            // Waiting for an available connection in HttpClient pool expired.
            return PushExceptionCodes.IO_ERROR.create(e, e.getMessage());
        }

        LOG.warn("Failed communication with Webhook \"{}\" ({})", webhookInfo.getWebhookId(), webhookInfo.getUri(), e);
        return PushExceptionCodes.IO_ERROR.create(e, e.getMessage());
    }

    /**
     * Gets the HTTP client to use to communicate with Webhook end-point.
     *
     * @return The HTTP client
     */
    private HttpClient getHttpClient() {
        return httpClientService.getHttpClient(ID);
    }

    // ---------------------------------------------------------- Helpers ------------------------------------------------------------------

    private static Set<WebhookInfo> extractWebhookInfosFrom(Collection<PushMatch> userMatches, Set<WebhookExtract> absentWebhookInfos, WebhookRegistry webhookRegistry) {
        Map<String, WebhookInfo> webhookConfig = webhookRegistry.getConfiguredWebhooks();
        Set<WebhookInfo> webhookInfos = null;
        for (WebhookExtract webhookExtract : userMatches.stream().map(WebhookExtract::new).collect(Collectors.toSet())) {
            String webhookId = webhookExtract.webhookId;
            WebhookInfo configuredWebhookInfo = webhookConfig.get(webhookId);
            if (configuredWebhookInfo == null) {
                LOG.warn("Denied calling Webhook {} for user {} in context {} since there is no such Webhook in configuration", webhookId, I(webhookExtract.userId), I(webhookExtract.contextId));
                absentWebhookInfos.add(webhookExtract);
            } else {
                try {
                    // Check for client-specified WebhookInfo in meta data
                    WebhookInfo effectiveWebhookInfo;
                    Optional<Meta> optMeta = webhookExtract.optMeta;
                    if (optMeta.isPresent()) {
                        ClientSpecifiedWebhookInfo clientSpecifiedWebhookInfo = WebhookInfoUtils.webhookInfoFrom(webhookId, optMeta.get());
                        effectiveWebhookInfo = WebhookInfoUtils.merge(clientSpecifiedWebhookInfo, webhookExtract.uri, configuredWebhookInfo);
                    } else {
                        effectiveWebhookInfo = WebhookInfoUtils.merge(webhookExtract.uri, configuredWebhookInfo);
                    }

                    // Prefer client-specified WebhookInfo
                    if (webhookInfos == null) {
                        webhookInfos = new HashSet<>();
                    }
                    webhookInfos.add(effectiveWebhookInfo);
                } catch (NoValidUriAvailableException e) {
                    LOG.warn("Denied calling Webhook {} for user {} in context {} since there is no valid URI available", webhookId, I(webhookExtract.userId), I(webhookExtract.contextId), e);
                }
            }
        }

        return webhookInfos == null ? Collections.emptySet() : webhookInfos;
    }

    private Map<String, List<PushMatch>> groupByWebhook(Collection<PushMatch> matches) throws OXException {
        Map<String, List<PushMatch>> clientMatches = new LinkedHashMap<>(matches.size());
        for (PushMatch match : matches) {
            // Client contain the Webhook identifier
            String webhookId = match.getClient();

            // Check generator availability
            {
                PushMessageGenerator generator = generatorRegistry.getGenerator(webhookId, ID);
                if (null == generator) {
                    // Assume generic one
                    generator = generatorRegistry.getGenerator(PushMessageGenerator.ID_GENERIC_SOCKET_IO, ID);
                    if (null == generator) {
                        throw PushExceptionCodes.NO_SUCH_GENERATOR.create(webhookId, ID);
                    }
                }
            }

            clientMatches.computeIfAbsent(webhookId, Functions.getNewLinkedListFuntion()).add(match);
        }
        return clientMatches;
    }

    /**
     * The tuple for HTTP response and the status code associated with it.
     *
     * @param response The HTTP response
     * @param status The status code
     */
    private static record HttpResponseAndStatusCode(HttpResponse response, int status) {
        // Nothing
    }

    /**
     * Basic Webhook data for transport.
     */
    private static class WebhookExtract {

        /** The Webhook identifier */
        final String webhookId;

        /** The Webhook URI */
        final String uri;

        /** The optional meta information */
        final Optional<Meta> optMeta;

        /** The user identifier */
        final int userId;

        /** The context identifier */
        final int contextId;

        /**
         * Initializes basic Webhook data for transport.
         *
         * @param m The push match to initialize from
         */
        WebhookExtract(PushMatch m) {
            super();
            webhookId = m.getClient();
            uri = m.getToken().getValue();
            optMeta = m.getToken().getOptionalMeta();
            userId = m.getUserId();
            contextId = m.getContextId();
        }
    }

    /**
     * Webhook configuration for a user
     *
     * @param httpsOnly Whether only HTTPS is allowed
     * @param enabledWebhookIds The enabled Webhook identifiers
     */
    private static record UserWebhookConfig(boolean httpsOnly, Set<String> enabledWebhookIds) {
        // Nothing
    }
}
