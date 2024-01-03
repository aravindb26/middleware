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


package com.openexchange.pns.transport.webhooks.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import com.openexchange.java.Strings;
import com.openexchange.pns.Meta;
import com.openexchange.webhooks.BasicAuthCredentials;
import com.openexchange.webhooks.DefaultWebhookInfo;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookUriValidationMode;

/**
 * {@link WebhookInfoUtils} - Utility class for Webhook information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhookInfoUtils {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebhookInfoUtils.class);

    /**
     * Initializes a new {@link WebhookInfoUtils}.
     */
    private WebhookInfoUtils() {
        super();
    }

    /**
     * Gets the client-specified Webhook info for given token.
     *
     * @param webhookId The Webhook identifier
     * @param meta The meta information
     * @return The client-specified Webhook info
     * @throws IllegalArgumentException If Webhook identifier is <code>null</code> or empty
     */
    public static ClientSpecifiedWebhookInfo webhookInfoFrom(String webhookId, Meta meta) {
        if (Strings.isEmpty(webhookId)) {
            throw new IllegalArgumentException("Webhook identifier must not be null or empty");
        }

        if (meta == null) {
            // Only Webhook identifier available
            return ClientSpecifiedWebhookInfo.builder().withWebhookId(webhookId).build();
        }

        // Authorization
        String authorization = meta.getString("authorization", "").trim();
        BasicAuthCredentials basicAuthCredentials;
        {
            String login = meta.getString("login", "").trim();
            if (Strings.isEmpty(login)) {
                login = meta.getString("username", "").trim();
            }
            String password = meta.getString("password", "").trim();
            if (Strings.isNotEmpty(login) && Strings.isNotEmpty(password)) {
                basicAuthCredentials = new BasicAuthCredentials(login, password);
            } else {
                basicAuthCredentials = null;
            }
        }

        return ClientSpecifiedWebhookInfo.builder()
            .withWebhookId(webhookId)
            .withAuthorization(authorization)
            .withBasicAuthCredentials(basicAuthCredentials)
            .build();
    }

    /**
     * Validates URI from configured and (optional) client-specified Webhook information.
     *
     * @param webhookId The Webhook identifier
     * @param configuredWebhook The configured Webhook information
     * @param clientSuppliedUri The optional URI passed by the client during subscribe
     * @param forTransport Whether to check URI for transport
     * @return The validated URI or empty
     */
    public static Optional<UriValidationResult> validateUri(String webhookId, WebhookInfo configuredWebhook, Optional<String> clientSuppliedUri, boolean forTransport) {
        // Check URI validity
        WebhookUriValidationMode validationMode = configuredWebhook.getUriValidationMode();

        URI clientSpecifiedUri = null;
        if (clientSuppliedUri.isPresent()) {
            try {
                clientSpecifiedUri = new URI(clientSuppliedUri.get());
            } catch (URISyntaxException e) {
                LOG.warn("Denied Webhook {}: Client-specified URI ({}) for Webhook {} is invalid", getInvocationType(forTransport), clientSuppliedUri.get(), webhookId, e);
                return Optional.empty();
            }
        }

        URI configuredUri = configuredWebhook.getUri();
        if (WebhookUriValidationMode.EXACT == validationMode) {
            if (configuredUri == null) {
                // URI missing
                LOG.warn("Denied Webhook {}: No configured URI for Webhook {}", getInvocationType(forTransport), webhookId); // NOSONARLINT
                return Optional.empty();
            }
            if (clientSpecifiedUri != null && !clientSpecifiedUri.equals(configuredUri)) {
                // URIs mismatch
                LOG.warn("Denied Webhook {}: Client-specified URI ({}) for Webhook {} does not match the configured one ({})", getInvocationType(forTransport), clientSpecifiedUri, webhookId, configuredUri); // NOSONARLINT
                return Optional.empty();
            }
        } else if (WebhookUriValidationMode.PREFIX == validationMode) {
            if (configuredUri == null) {
                // URI missing
                LOG.warn("Denied Webhook {}: No configured URI (prefix) for Webhook {}", getInvocationType(forTransport), webhookId); // NOSONARLINT
                return Optional.empty();
            }
            if (clientSpecifiedUri != null) {
                // There is client-specific URI. Validate it against configured one.
                if (!configuredUri.getScheme().equals(clientSpecifiedUri.getScheme())) {
                    // Protocols mismatch
                    LOG.warn("Denied Webhook {}: Client-specified URI's protocol ({}) for Webhook {} does not match the configured one ({})", getInvocationType(forTransport), clientSpecifiedUri, webhookId, configuredUri); // NOSONARLINT
                    return Optional.empty();
                }

                if (!configuredUri.getHost().equals(clientSpecifiedUri.getHost())) {
                    // Hosts mismatch
                    LOG.warn("Denied Webhook {}: Client-specified URI's host ({}) for Webhook {} does not match the configured one ({})", getInvocationType(forTransport), clientSpecifiedUri, webhookId, configuredUri); // NOSONARLINT
                    return Optional.empty();
                }

                int configuredPort = configuredUri.getPort();
                if (configuredPort >= 0 && (clientSpecifiedUri.getPort() < 0 || configuredPort != clientSpecifiedUri.getPort())) {
                    // Ports mismatch
                    LOG.warn("Denied Webhook {}: Client-specified URI's port ({}) for Webhook {} does not match the configured one ({})", getInvocationType(forTransport), clientSpecifiedUri, webhookId, configuredUri); // NOSONARLINT
                    return Optional.empty();
                }

                String configuredPath = configuredUri.getPath();
                if (Strings.isNotEmpty(configuredPath) && (Strings.isEmpty(clientSpecifiedUri.getPath()) || !clientSpecifiedUri.getPath().startsWith(configuredPath))) {
                    // Paths mismatch
                    LOG.warn("Denied Webhook {}: Client-specified URI's path ({}) for Webhook {} does not match the configured one ({})", getInvocationType(forTransport), clientSpecifiedUri, webhookId, configuredUri); // NOSONARLINT
                    return Optional.empty();
                }
            }
        } else {
            // "None"...
            if (configuredUri == null && clientSpecifiedUri == null) {
                LOG.warn("Denied Webhook {}: Neither configured nor client-specified URI for Webhook {}", getInvocationType(forTransport), webhookId); // NOSONARLINT
                return Optional.empty();
            }
        }

        // All fine
        return Optional.of(clientSpecifiedUri != null ? new UriValidationResult(clientSpecifiedUri, true) : new UriValidationResult(configuredUri, false));
    }

    private static String getInvocationType(boolean forTransport) {
        return forTransport ? "request" : "subscription";
    }

    /**
     * Merges client-specified URI into configured Webhook information.
     *
     * @param webhookUri The Webhook URI
     * @param configuredWebhookInfo The configured Webhook information
     * @return The effective Webhook information
     * @throws NoValidUriAvailableException If there is no valid URI available
     */
    public static WebhookInfo merge(String webhookUri, WebhookInfo configuredWebhookInfo) throws NoValidUriAvailableException {
        String webhookId = configuredWebhookInfo.getWebhookId();
        DefaultWebhookInfo.Builder mergedWebhookInfo = null;

        Optional<UriValidationResult> optValidationResult = validateUri(webhookId, configuredWebhookInfo, Optional.of(webhookUri), true);
        if (optValidationResult.isEmpty()) {
            throw new NoValidUriAvailableException("No valid URI available in current configuration for Webhook " + webhookId);
        }

        UriValidationResult validationResult = optValidationResult.get();
        if (validationResult.isFromClient()) {
            mergedWebhookInfo = DefaultWebhookInfo.builder(configuredWebhookInfo);
            mergedWebhookInfo.withUri(validationResult.getUri());
        }

        return mergedWebhookInfo == null ? configuredWebhookInfo : mergedWebhookInfo.build();
    }

    /**
     * Merges client-specified Webhook information into configured one.
     *
     * @param clientSpecifiedWebhookInfo The client-specified Webhook information
     * @param webhookUri The Webhook URI
     * @param configuredWebhookInfo The configured Webhook information
     * @return The effective Webhook information
     * @throws NoValidUriAvailableException If there is no valid URI available
     * @throws IllegalArgumentException If arguments are invalid
     */
    public static WebhookInfo merge(ClientSpecifiedWebhookInfo clientSpecifiedWebhookInfo, String webhookUri, WebhookInfo configuredWebhookInfo) throws NoValidUriAvailableException {
        String webhookId = clientSpecifiedWebhookInfo.getWebhookId();
        if (Strings.isEmpty(webhookId)) {
            throw new IllegalArgumentException("Webhook identifier must not be null or empty in client-specified WebhookInfo");
        }
        if (!webhookId.equals(configuredWebhookInfo.getWebhookId())) {
            throw new IllegalArgumentException("WebhookInfo cannot be merged due to Webhook identifier mismatch");
        }

        DefaultWebhookInfo.Builder mergedWebhookInfo = null;

        if (Strings.isNotEmpty(clientSpecifiedWebhookInfo.getAuthorization())) {
            mergedWebhookInfo = DefaultWebhookInfo.builder(configuredWebhookInfo);
            mergedWebhookInfo.withAuthorization(clientSpecifiedWebhookInfo.getAuthorization());
        }

        {
            Optional<UriValidationResult> optValidationResult = validateUri(webhookId, configuredWebhookInfo, Optional.of(webhookUri), true);
            if (optValidationResult.isEmpty()) {
                throw new NoValidUriAvailableException("No valid URI available in current configuration for Webhook " + webhookId);
            }

            UriValidationResult validationResult = optValidationResult.get();
            if (validationResult.isFromClient()) {
                if (mergedWebhookInfo == null) {
                    mergedWebhookInfo = DefaultWebhookInfo.builder(configuredWebhookInfo);
                }
                mergedWebhookInfo.withUri(validationResult.getUri());
            }
        }

        if (clientSpecifiedWebhookInfo.getBasicAuthCredentials() != null) {
            if (mergedWebhookInfo == null) {
                mergedWebhookInfo = DefaultWebhookInfo.builder(configuredWebhookInfo);
            }
            mergedWebhookInfo.withBasicAuthCredentials(clientSpecifiedWebhookInfo.getBasicAuthCredentials());
        }

        return mergedWebhookInfo == null ? configuredWebhookInfo : mergedWebhookInfo.build();
    }

}
