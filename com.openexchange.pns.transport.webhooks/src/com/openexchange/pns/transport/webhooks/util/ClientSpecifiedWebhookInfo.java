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

import com.openexchange.java.Strings;
import com.openexchange.webhooks.BasicAuthCredentials;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookInfos;

/**
 * {@link ClientSpecifiedWebhookInfo} - Information for a Webhook specified by client during subscription.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ClientSpecifiedWebhookInfo {

    /**
     * Creates a new empty builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder for an instance of <code>ClientSpecifiedWebhookInfo</code> */
    public static class Builder {

        private String webhookId;
        private String authorization;
        private BasicAuthCredentials basicAuthCredentials;

        /**
         * Initializes a new {@link Builder}.
         */
        private Builder() {
            super();
        }

        /**
         * Sets the Webhook identifier.
         *
         * @param webhookId The Webhook identifier to set
         * @return This builder
         */
        public Builder withWebhookId(String webhookId) {
            this.webhookId = webhookId;
            return this;
        }

        /**
         * Sets the authorization header value.
         *
         * @param authorization The authorization to set
         * @return This builder
         */
        public Builder withAuthorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        /**
         * Sets the HTTP Basic Authentication credentials.
         *
         * @param basicAuthCredentials The Basic Authentication credentials to set
         * @return This builder
         */
        public Builder withBasicAuthCredentials(BasicAuthCredentials basicAuthCredentials) {
            this.basicAuthCredentials = basicAuthCredentials;
            return this;
        }

        /**
         * Builds the instance of <code>ClientSpecifiedWebhookInfo</code> from this builder's arguments.
         *
         * @return The instance of <code>ClientSpecifiedWebhookInfo</code>
         */
        public ClientSpecifiedWebhookInfo build() {
            if (Strings.isEmpty(webhookId)) {
                throw new IllegalArgumentException("Webhook identifier must not be null or empty");
            }
            return new ClientSpecifiedWebhookInfo(webhookId, authorization, basicAuthCredentials);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    private final String webhookId;
    private final String authorization;
    private final BasicAuthCredentials basicAuthCredentials;

    /**
     * Initializes a new {@link ClientSpecifiedWebhookInfo}.
     *
     * @param webhookId The Webhook identifier
     * @param authorization The authorization or <code>null</code>
     * @param basicAuthCredentials The HTTP Basic Authentication credentials or <code>null</code>
     */
    private ClientSpecifiedWebhookInfo(String webhookId, String authorization, BasicAuthCredentials basicAuthCredentials) {
        super();
        this.webhookId = webhookId;
        this.authorization = authorization;
        this.basicAuthCredentials = basicAuthCredentials;
    }

    /**
     * Gets the Webhook identifier.
     *
     * @return The Webhook identifier
     */
    public String getWebhookId() {
        return webhookId;
    }

    /**
     * Gets the direct value for the <code>"Authorization"</code> HTTP header.
     * <p>
     * An authorization header value has precedence over HTTP Basic Authentication as value for the <code>"Authorization"</code> HTTP header.<br>
     * If {@link #getAuthorization()} returns non-<code>null</code> value, {@link #getBasicAuthCredentials()} is ignored.
     *
     * @return The header value or <code>null</code>
     * @see #getBasicAuthCredentials()
     * @see WebhookInfos#getAuthorizationHeaderValue(WebhookInfo)
     */
    public String getAuthorization() {
        return authorization;
    }

    /**
     * Gets the HTTP Basic Authentication credentials.
     * <p>
     * An authorization header value has precedence over HTTP Basic Authentication as value for the <code>"Authorization"</code> HTTP header.<br>
     * If {@link #getAuthorization()} returns non-<code>null</code> value, {@link #getBasicAuthCredentials()} is ignored.
     *
     * @return The HTTP Basic Authentication credentials or <code>null</code>
     * @see #getAuthorization()
     * @see WebhookInfos#getAuthorizationHeaderValue(WebhookInfo)
     */
    public BasicAuthCredentials getBasicAuthCredentials() {
        return basicAuthCredentials;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('[').append("webhookId=").append(webhookId);
        if (authorization != null) {
            sb.append(", authorization=").append("***");
        }
        if (basicAuthCredentials != null) {
            sb.append(", basicAuthCredentials=").append("***");
        }
        sb.append(']');
        return sb.toString();
    }

}
