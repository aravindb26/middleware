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

package com.openexchange.webhooks;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import com.openexchange.java.Strings;

/**
 * {@link WebhookInfos} - Utility class for Webhook information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhookInfos {

    /**
     * Initializes a new {@link WebhookInfos}.
     */
    private WebhookInfos() {
        super();
    }

    /**
     * Gets the optional value for the <code>"Authorization"</code> HTTP header from specified Webhook information.
     * <p>
     * The value is either the return value from {@link #getAuthorization()} if a non-<code>null</code> and non-empty string is returned; or
     * HTTP Basic Authentication provided that {@link #getBasicAuthCredentials()} gives a non-<code>null</code> return value. If neither nor
     * is given, empty is returned.
     *
     * @param webhookInfo The Webhook information
     * @return The value for the <code>"Authorization"</code> HTTP header or empty
     */
    public static Optional<String> getAuthorizationHeaderValue(WebhookInfo webhookInfo) {
        if (Strings.isNotEmpty(webhookInfo.getAuthorization())) {
            return Optional.of(webhookInfo.getAuthorization());
        }

        if (webhookInfo.getBasicAuthCredentials() != null) {
            // Compile: auth := <login> + ":" + <password>
            StringBuilder sb = new StringBuilder(32);
            BasicAuthCredentials basicAuthCredentials = webhookInfo.getBasicAuthCredentials();
            String auth = sb.append(basicAuthCredentials.getLogin()).append(':').append(basicAuthCredentials.getPassword()).toString();

            // Compile: "Basic " + <base64(auth)>
            sb.setLength(0);
            return Optional.of(sb.append("Basic ").append(Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1))).toString());
        }

        return Optional.empty();
    }

}
