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
import static com.openexchange.java.Autoboxing.L;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.PushSubscription;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionListener;
import com.openexchange.pns.PushSubscriptionProvider;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.pns.transport.webhooks.util.UriValidationResult;
import com.openexchange.pns.transport.webhooks.util.WebhookInfoUtils;
import com.openexchange.server.ServiceLookup;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookProperty;
import com.openexchange.webhooks.WebhookRegistry;

/**
 * {@link WebhookPushSubscriptionListener} - Checks if Webhook subscriptions are legit.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhookPushSubscriptionListener implements PushSubscriptionListener {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebhookPushSubscriptionListener.class);

    private static final String ID = KnownTransport.WEBHOOK.getTransportId();

    private final ServiceLookup services;

    /**
     * Initializes a new {@link WebhookPushSubscriptionListener}.
     *
     * @param services The service look-up
     */
    public WebhookPushSubscriptionListener(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public boolean addingSubscription(PushSubscriptionDescription subscription, PushSubscriptionRestrictions restrictions) throws OXException {
        if (!ID.equals(subscription.getTransportId())) {
            // A non-webhook subscription. Don't care...
            return true;
        }

        // Examine Webhook subscription
        String webhookId = subscription.getClient();
        if (Strings.isEmpty(webhookId)) {
            // Missing Webhook configuration id
            LOG.warn("Denied Webhook subscription: Missing configuration identifier");
            return false;
        }

        // Examine Webhook subscription
        Optional<WebhookInfo> optConfiguredWebhook = services.getServiceSafe(WebhookRegistry.class).getConfiguredWebhook(webhookId);
        if (optConfiguredWebhook.isEmpty()) {
            // No such Webhook configuration available
            LOG.warn("Denied Webhook subscription: No such Webhook for identifier {} in configuration", webhookId);
            return false;
        }

        WebhookInfo configuredWebhook = optConfiguredWebhook.get();
        Optional<String> clientSuppliedUri = Strings.isEmpty(subscription.getToken().getValue()) ? Optional.empty() : Optional.of(subscription.getToken().getValue());

        boolean webhookAllowed = isWebhookAllowed(webhookId, configuredWebhook, clientSuppliedUri, getConfigViewFor(subscription), subscription, restrictions);
        if (webhookAllowed) {
            // Take over default URI as token in subscription unless overridden by client
            if (clientSuppliedUri.isEmpty()) {
                subscription.setToken(DefaultToken.builder().withValue(configuredWebhook.getUri().toString()).withMeta(subscription.getToken().getOptionalMeta().orElse(null)).build());
            }

            // Check expiration time
            long maxTimeToLive = configuredWebhook.getMaxTimeToLiveMillis();
            if (maxTimeToLive > 0) {
                long now = System.currentTimeMillis();
                long maxExpiration = now + maxTimeToLive;
                Date expirationDate = subscription.getExpires();
                if (expirationDate == null) {
                    // no client-defined expiration, let subscription live as long as possible
                    subscription.setExpires(new Date(maxExpiration));
                } else {
                    if (now > expirationDate.getTime()) {
                        // already expired, refuse subscription
                        LOG.warn("Denied Webhook subscription: Given expiration time {} already past due", L(expirationDate.getTime()));
                        return false;
                    }
                    if (maxExpiration < expirationDate.getTime()) {
                        // client-defined expiration beyond max. time to live, refuse subscription
                        LOG.warn("Denied Webhook subscription: Given expiration time {} exceeds max. allowed time-to-live.", L(expirationDate.getTime()));
                        return false;
                    }
                }
            }
        }
        return webhookAllowed;
    }

    private ConfigView getConfigViewFor(PushSubscriptionDescription subscription) throws OXException {
        ConfigViewFactory viewFactory = services.getServiceSafe(ConfigViewFactory.class);
        return viewFactory.getView(subscription.getUserId(), subscription.getContextId());
    }

    private static boolean isWebhookAllowed(String webhookId, WebhookInfo configuredWebhook, Optional<String> clientSuppliedUri, ConfigView view, PushSubscriptionDescription desc, PushSubscriptionRestrictions restrictions) throws OXException {
        // Check if enabled for subscription-associated user
        String sEnabledIds = ConfigViews.getDefinedStringPropertyFrom(WebhookProperty.ENABLED_IDS.getFQPropertyName(), WebhookProperty.ENABLED_IDS.getDefaultValue().toString(), view).trim();
        if (Strings.isEmpty(sEnabledIds) || !new HashSet<>(Arrays.asList(Strings.splitBy(sEnabledIds, ',', true))).contains(webhookId)) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Denied Webhook subscription: Webhook {} not contained in enabled Webhooks for user {} in context {}: {}", webhookId, I(desc.getUserId()), I(desc.getContextId()), sEnabledIds);
            } else {
                LOG.warn("Denied Webhook subscription: Webhook {} not contained in enabled Webhooks for user {} in context {}", webhookId, I(desc.getUserId()), I(desc.getContextId()));
            }
            return false;
        }

        // Check URI validity
        Optional<UriValidationResult> optValidateUri = WebhookInfoUtils.validateUri(webhookId, configuredWebhook, clientSuppliedUri, false);
        if (optValidateUri.isEmpty()) {
            return false;
        }
        if (!optValidateUri.get().isFromClient()) {
            // Validated URI wasn't derived from client-specified URI. Hence, need to set in PushSubscriptionDescription instance
            desc.setToken(DefaultToken.builder().withValue(optValidateUri.get().getUri().toString()).withMeta(desc.getToken().getOptionalMeta().orElse(null)).build());
        }

        // Something else to check? Number of subscriptions? Using a shared URI?
        int maxNumberOfSubscriptionsPerUser = configuredWebhook.getMaxNumberOfSubscriptionsPerUser();
        if (maxNumberOfSubscriptionsPerUser > 0) {
            restrictions.setMaxNumberOfSuchSubscription(maxNumberOfSubscriptionsPerUser);
        }
        restrictions.setAllowSharedToken(configuredWebhook.isAllowSharedUri());
        return true;
    }

    @Override
    public void removingSubscription(PushSubscriptionDescription subscription, PushSubscriptionRestrictions restrictions) throws OXException {
        if (!ID.equals(subscription.getTransportId())) {
            // A non-webhook subscription. Don't care...
            return;
        }

        // Examine Webhook subscription
        String webhookId = subscription.getClient();
        if (Strings.isEmpty(webhookId)) {
            // Missing Webhook configuration id
            LOG.warn("Unable to apply restrictions when removing Webhook subscription: Missing configuration identifier");
            return;
        }

        // Examine Webhook subscription
        Optional<WebhookInfo> optConfiguredWebhook = services.getServiceSafe(WebhookRegistry.class).getConfiguredWebhook(webhookId);
        if (optConfiguredWebhook.isEmpty()) {
            // No such Webhook configuration available
            LOG.warn("Unable to apply restrictions when removing Webhook subscription: No such Webhook for identifier {} in configuration", webhookId);
            return;
        }

        WebhookInfo configuredWebhook = optConfiguredWebhook.get();
        Optional<String> clientSuppliedUri = Strings.isEmpty(subscription.getToken().getValue()) ? Optional.empty() : Optional.of(subscription.getToken().getValue());

        // Take over default URI as token in subscription unless overridden by client
        if (clientSuppliedUri.isEmpty()) {
            subscription.setToken(DefaultToken.builder().withValue(configuredWebhook.getUri().toString()).withMeta(subscription.getToken().getOptionalMeta().orElse(null)).build());
        }

        // Using a shared URI?
        restrictions.setAllowSharedToken(configuredWebhook.isAllowSharedUri());
    }

    @Override
    public void addedSubscription(PushSubscription subscription) throws OXException {
        // Ignore
    }

    @Override
    public void removedSubscription(PushSubscription subscription) throws OXException {
        // Ignore
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean addingProvider(PushSubscriptionProvider provider) throws OXException {
        return true;
    }

    @Override
    public void addedProvider(PushSubscriptionProvider provider) throws OXException {
        // Ignore
    }

    @Override
    public void removedProvider(PushSubscriptionProvider provider) throws OXException {
        // Ignore
    }

}
