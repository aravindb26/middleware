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

package com.openexchange.conference.webhook;

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.java.Strings;

/**
 * {@link ConferenceWebhookConfiguration}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class ConferenceWebhookConfiguration {

    private final String secret;
    private final String uri;

    /**
     * Initializes a new {@link ConferenceWebhookConfiguration}.
     * 
     * @param uri The URI to use
     * @param secret The secret to use
     */
    private ConferenceWebhookConfiguration(String uri, String secret) {
        super();
        this.secret = secret;
        this.uri = uri;
    }

    /**
     * Loads the configuration
     *
     * @param configService The LeanConfigurationService
     * @return A ConferenceWebhookConfiguration, or <code>null</code> if not configured
     */
    public static ConferenceWebhookConfiguration getConfig(LeanConfigurationService configService, int user, int context) {
        String baseUri = configService.getProperty(user, context, ConferenceWebhookProperties.baseUri);
        if (Strings.isEmpty(baseUri)) {
            return null;
        }
        String secret = configService.getProperty(user, context, ConferenceWebhookProperties.secret);
        String uri = baseUri + "v1/webhook";
        return new ConferenceWebhookConfiguration(uri, secret);
    }

    /**
     * Gets the webhookSecret
     *
     * @return The webhookSecret
     */
    public String getWebhookSecret() {
        return secret;
    }
    /**
     * Gets the uri
     *
     * @return The uri
     */
    public String getUri() {
        return uri;
    }

}
