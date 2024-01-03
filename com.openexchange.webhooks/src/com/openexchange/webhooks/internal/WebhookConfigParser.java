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

package com.openexchange.webhooks.internal;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.webhooks.BasicAuthCredentials;
import com.openexchange.webhooks.DefaultWebhookInfo;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookUriValidationMode;

/**
 * {@link WebhookConfigParser} - The parser for Webhook configuration expected in YAML file <code>"webhooks.yml"</code>.
 * <p>
 * Example:
 * <pre>
 * # Configuration for Webhook
 * webhook-test:
 *    enabled: true
 *    uri: http://localhost:8009/preliminary/webhook-test/event
 *    webhookSecret:
 *    login: th0rb3n
 *    password: secret
 *    signatureSecret: da39a3ee5e6b4b
 * </pre>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class WebhookConfigParser {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebhookConfigParser.class);

    private static final String CONFIGFILE_WEBHOOK = "webhooks.yml";

    /**
     * Gets the file name for the Webhook configuration YAML file.
     *
     * @return The file name
     */
    public static String getConfigfileWebhookYamlFile() {
        return CONFIGFILE_WEBHOOK;
    }

    /**
     * Initializes a new {@link WebhookConfigParser}.
     */
    private WebhookConfigParser() {
        super();
    }

    /**
     * Parses the Webhook configuration.
     * <p>
     * Configuration is expected in YAML file <code>"webhook.yml"</code>:
     * <pre>
     * # Configuration for Webhook
     * webhook-test:
     *    enabled: true
     *    uri: http://localhost:8009/preliminary/webhook-test/event
     *    webhookSecret:
     *    login: admin
     *    password: secret
     *    webhookSecret: da39a3ee5e6b4b
     *    version: 1
     *
     * webhook-anothertest:
     *    enabled: true
     *    uri: http://localhost:8009/preliminary/webhook-anothertest/event
     *    authorization:
     *    login: anotheradmin
     *    password: anothersecret
     *    webhookSecret: 1356a3ee5e6b4b
     *    version: 1
     *
     * ...
     * </pre>
     *
     * @param configService The configuration service to use
     * @return The parsed Webhook configuration in an immutable map
     * @throws OXException If parsing Webhook configuration fails
     */
    public static Map<String, WebhookInfo> parseWebhookConfig(ConfigurationService configService) throws OXException {
        Map<String, WebhookInfo> webhooks = null;

        Object yaml = configService.getYaml(CONFIGFILE_WEBHOOK);
        if (yaml instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) yaml;
            if (!map.isEmpty()) {
                try {
                    webhooks = parseWebhookConfig(map);
                } catch (ClassCastException e) {
                    LOG.warn("Invalid Webhook configuration specified in \"{}\".", CONFIGFILE_WEBHOOK, e);
                    return Collections.emptyMap();
                }
            }
        }

        if (null == webhooks || webhooks.isEmpty()) {
            LOG.warn("No Webhooks configured in \"{}\".", CONFIGFILE_WEBHOOK);
            return Collections.emptyMap();
        }
        return webhooks;
    }

    private static Map<String, WebhookInfo> parseWebhookConfig(Map<String, Object> yaml) throws OXException {
        Map<String, WebhookInfo> webhooks = null;
        for (Map.Entry<String, Object> entry : yaml.entrySet()) {
            String webhookId = entry.getKey();
            if (webhookId.indexOf("--") >= 0) {
                throw OXException.general("Invalid Webhook identifier: " + webhookId);
            }

            // Check for duplicate
            if (webhooks != null && webhooks.containsKey(webhookId)) {
                throw OXException.general("Duplicate Webhook configuration: " + webhookId);
            }

            // Check values map
            if (!(entry.getValue() instanceof Map)) {
                throw OXException.general("Invalid Webhook configuration for " + webhookId);
            }

            // Parse values map
            Optional<WebhookInfo> optWebhookInfo = parseWebhookValues(webhookId, (Map<String, Object>) entry.getValue());
            if (optWebhookInfo.isPresent()) {
                if (webhooks == null) {
                    webhooks = new LinkedHashMap<String, WebhookInfo>(yaml.size());
                }
                webhooks.put(webhookId, optWebhookInfo.get());
            }
        }
        return webhooks == null ? null : Map.copyOf(webhooks);
    }

    /**
     * Parses a Webhook section.
     * <p>
     * <pre>
     * webhook-test:
     *    enabled: true
     *    uri: http://localhost:8009/preliminary/webhook-test/event
     *    webhookSecret:
     *    login: admin
     *    password: secret
     *    webhookSecret: da39a3ee5e6b4b
     *    version: 1
     * </pre>
     *
     * @param webhookId The Webhook identifier
     * @param values The values as a map
     * @return The parsed Webhook information or empty
     */
    private static Optional<WebhookInfo> parseWebhookValues(String webhookId, Map<String, Object> values) {
        try {
            String sUriValidationMode = (String) values.get("uriValidationMode");
            WebhookUriValidationMode uriValidationMode = WebhookUriValidationMode.modeFor(sUriValidationMode, WebhookUriValidationMode.PREFIX);

            String uri = (String) values.get("uri");
            if (Strings.isEmpty(uri) && uriValidationMode != WebhookUriValidationMode.NONE) {
                // URI is required, but not present
                LOG.warn("Invalid Webhook configuration for \"{}\". URI is not specified, but required due to \"uriValidationMode={}\".", webhookId, uriValidationMode, new Throwable("Missing URI"));
                return Optional.empty();
            }

            String authorization = (String) values.get("webhookSecret");
            String login = (String) values.get("login");
            String password = (String) values.get("password");
            String signatureSecret = (String) values.get("signatureSecret");
            Integer version = (Integer) values.get("version");
            String signatureHeaderName = (String) values.get("signatureHeaderName");
            Number maxTimeToLiveMillis = (Number) values.get("maxTimeToLiveMillis");
            Number maxNumberOfSubscriptionsPerUser = (Number) values.get("maxNumberOfSubscriptionsPerUser");
            Boolean allowSharedUri = (Boolean) values.get("allowSharedUri");

            WebhookInfo webhookInfo = DefaultWebhookInfo.builderWithDefaults()
                .withWebhookId(webhookId)
                .withUri(uri)
                .withAuthorization(authorization)
                .withBasicAuthCredentials(BasicAuthCredentials.getBasicAuthCredentialsFor(login, password))
                .withSignatureSecret(Strings.isEmpty(signatureSecret) ? null : signatureSecret.trim())
                .withVersion(version == null ? WebhookInfo.DEFAULT_VERSION : version.intValue())
                .withSignatureHeaderName(Strings.isEmpty(signatureHeaderName) ? WebhookInfo.DEFAULT_SIGNATURE_HEADER_NAME : signatureHeaderName.trim())
                .withMaxTimeToLiveMillis(maxTimeToLiveMillis == null ? 0L : maxTimeToLiveMillis.longValue())
                .withMaxNumberOfSubscriptionsPerUser(maxNumberOfSubscriptionsPerUser == null ? 0 : maxNumberOfSubscriptionsPerUser.intValue())
                .withUriValidationMode(uriValidationMode)
                .withAllowSharedUri(allowSharedUri == null ? true : allowSharedUri.booleanValue())
                .build();
            return Optional.of(webhookInfo);
        } catch (URISyntaxException e) {
            LOG.warn("Invalid Webhook configuration for \"{}\". URI is invalid.", webhookId, e);
            return Optional.empty();
        }
    }

}
