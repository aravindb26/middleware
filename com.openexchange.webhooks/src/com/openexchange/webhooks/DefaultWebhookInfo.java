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

import java.net.URI;
import java.net.URISyntaxException;
import com.openexchange.java.Strings;

/**
 * {@link DefaultWebhookInfo} - Default implementation for information for a Webhook like its identifier, URI end-point, authentication data, etc.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DefaultWebhookInfo implements WebhookInfo {

    /**
     * Creates a new builder pre-initialized with default values for Webhook information.
     *
     * @param defaultVersion The default version to assume
     * @return The newly created builder
     */
    public static Builder builderWithDefaults() {
        return new Builder(true);
    }

    /**
     * Creates a new empty builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder(false);
    }

    /**
     * Creates a new builder pre-initialized from given Webhook information.
     *
     * @param webhookInfo The Webhook information to copy from
     * @return The newly created builder
     */
    public static Builder builder(WebhookInfo webhookInfo) {
        return new Builder(webhookInfo);
    }

    /** A builder for an instance of <code>DefaultWebhookInfo</code> */
    public static class Builder {

        private String webhookId;
        private URI uri;
        private String authorization;
        private BasicAuthCredentials basicAuthCredentials;
        private String signatureSecret;
        private int version;
        private String signatureHeaderName;
        private long maxTimeToLiveMillis;
        private int maxNumberOfSubscriptionsPerUser;
        private boolean allowSharedUri;
        private WebhookUriValidationMode uriValidationMode;

        /**
         * Initializes a new {@link Builder}.
         *
         * @param withDefaults <code>true</code> to pre-initialize with default values; otherwise <code>false</code>
         */
        private Builder(boolean withDefaults) {
            super();
            if (withDefaults) {
                version = WebhookInfo.DEFAULT_VERSION;
                signatureHeaderName = WebhookInfo.DEFAULT_SIGNATURE_HEADER_NAME;
                uriValidationMode = WebhookInfo.DEFAULT_URI_VALIDATION_MODE;
            }
            maxTimeToLiveMillis = 0;
            maxNumberOfSubscriptionsPerUser = 0;
            allowSharedUri = true;
        }

        /**
         * Initializes a new {@link Builder}.
         *
         * @param webhookInfo The Webhook info to copy from
         */
        private Builder(WebhookInfo webhookInfo) {
            super();
            webhookId = webhookInfo.getWebhookId();
            uri = webhookInfo.getUri();
            authorization = webhookInfo.getAuthorization();
            basicAuthCredentials = webhookInfo.getBasicAuthCredentials();
            signatureSecret = webhookInfo.getSignatureSecret();
            version = webhookInfo.getVersion();
            signatureHeaderName = webhookInfo.getSignatureHeaderName();
            maxTimeToLiveMillis = webhookInfo.getMaxTimeToLiveMillis();
            maxNumberOfSubscriptionsPerUser = webhookInfo.getMaxNumberOfSubscriptionsPerUser();
            allowSharedUri = webhookInfo.isAllowSharedUri();
            uriValidationMode = webhookInfo.getUriValidationMode();
        }

        /**
         * Sets how the possible client-specified URI for a Webhook end-point is supposed to be validated against the URI from configured Webhook end-point.
         *
         * @param uriValidationMode The validation mode
         * @return This builder
         */
        public Builder withUriValidationMode(WebhookUriValidationMode uriValidationMode) {
            this.uriValidationMode = uriValidationMode;
            return this;
        }

        /**
         * Sets the max. number of subscriptions for this Webhook allowed for a single user.
         *
         * @param maxNumberOfSubscriptionsPerUser The max. number of subscriptions
         * @return This builder
         */
        public Builder withMaxNumberOfSubscriptionsPerUser(int maxNumberOfSubscriptionsPerUser) {
            this.maxNumberOfSubscriptionsPerUser = maxNumberOfSubscriptionsPerUser;
            return this;
        }

        /**
         * Sets whether the same URI can be used by multiple different users or not.
         *
         * @param allowSharedUri <code>true</code> if shared tokens are allowed, <code>false</code> otherwise
         * @return This builder
         */
        public Builder withAllowSharedUri(boolean allowSharedUri) {
            this.allowSharedUri = allowSharedUri;
            return this;
        }

        /**
         * Sets the max. time to live in milliseconds for the Webhook before considered as expired.
         *
         * @param maxTimeToLiveMillis The max. time to live in milliseconds
         * @return This builder
         */
        public Builder withMaxTimeToLiveMillis(long maxTimeToLiveMillis) {
            this.maxTimeToLiveMillis = maxTimeToLiveMillis;
            return this;
        }

        /**
         * Sets the version number.
         *
         * @param version The version number
         * @return This builder
         */
        public Builder withVersion(int version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the signature secret.
         *
         * @param signatureSecret The signature secret
         * @return This builder
         */
        public Builder withSignatureSecret(String signatureSecret) {
            this.signatureSecret = signatureSecret;
            return this;
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
         * Sets the Webhook's URI.
         *
         * @param uri The URI to set
         * @return This builder
         */
        public Builder withUri(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Sets the Webhook's URI.
         *
         * @param uri The URI string to set
         * @return This builder
         * @throws URISyntaxException If given URI string is invalid
         */
        public Builder withUri(String uri) throws URISyntaxException {
            this.uri = uri == null ? null : new URI(uri);
            return this;
        }

        /**
         * Sets the Webhook's name of the signature header.
         *
         * @param uri The name of the signature header to set
         * @return This builder
         */
        public Builder withSignatureHeaderName(String signatureHeaderName) {
            this.signatureHeaderName = signatureHeaderName;
            return this;
        }

        /**
         * Sets the <code>"Authorization"</code> header value.
         *
         * @param authorization The <code>"Authorization"</code> header value to set
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
         * Builds the instance of <code>DefaultWebhookInfo</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultWebhookInfo</code>
         */
        public DefaultWebhookInfo build() {
            if (Strings.isEmpty(webhookId)) {
                throw new IllegalArgumentException("Webhook identifier must not be null or empty");
            }
            if (uri == null) {
                throw new IllegalArgumentException("Webhook URI must not be null");
            }
            if (Strings.isNotEmpty(signatureSecret) && Strings.isEmpty(signatureHeaderName)) {
                throw new IllegalArgumentException("The name of the signature header must not be null or empty if a shared secret is given");
            }
            return new DefaultWebhookInfo(webhookId, uri, authorization, basicAuthCredentials, signatureSecret, version, signatureHeaderName, maxTimeToLiveMillis, maxNumberOfSubscriptionsPerUser, allowSharedUri, uriValidationMode);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    private final String webhookId;
    private final URI uri;
    private final String authorization;
    private final BasicAuthCredentials basicAuthCredentials;
    private final String signatureSecret;
    private final int version;
    private final String signatureHeaderName;
    private final long maxTimeToLiveMillis;
    private int hash;
    private final WebhookUriValidationMode uriValidationMode;
    private final boolean allowSharedUri;
    private final int maxNumberOfSubscriptionsPerUser;

    /**
     * Initializes a new {@link DefaultWebhookInfo}.
     *
     * @param webhookId The Webhook identifier
     * @param uri The Webhook's URI
     * @param authorization The <code>"Authorization"</code> header value or <code>null</code>
     * @param basicAuthCredentials The HTTP Basic Authentication credentials or <code>null</code>
     * @param signatureSecret The signature secret shared between client and end-point host
     * @param version The version number
     * @param signatureHeaderName The name of the signature header
     * @param maxTimeToLiveMillis The max. time to live in milliseconds
     * @param maxNumberOfSubscriptionsPerUser The max. number of subscriptions for this Webhook allowed for a single user
     * @param allowSharedUri <code>true</code> if shared URIs are allowed, <code>false</code> otherwise
     * @param uriValidationMode The URI validation mode
     */
    private DefaultWebhookInfo(String webhookId, URI uri, String authorization, BasicAuthCredentials basicAuthCredentials, String signatureSecret, int version, String signatureHeaderName, long maxTimeToLiveMillis, int maxNumberOfSubscriptionsPerUser, boolean allowSharedUri, WebhookUriValidationMode uriValidationMode) {
        super();
        this.webhookId = webhookId;
        this.uri = uri;
        this.authorization = authorization;
        this.basicAuthCredentials = basicAuthCredentials;
        this.signatureSecret = signatureSecret;
        this.version = version;
        this.signatureHeaderName = signatureHeaderName;
        this.maxTimeToLiveMillis = maxTimeToLiveMillis;
        this.maxNumberOfSubscriptionsPerUser = maxNumberOfSubscriptionsPerUser;
        this.allowSharedUri = allowSharedUri;
        this.uriValidationMode = uriValidationMode;
        hash = 0;
    }

    @Override
    public WebhookUriValidationMode getUriValidationMode() {
        return uriValidationMode;
    }

    @Override
    public int getMaxNumberOfSubscriptionsPerUser() {
        return maxNumberOfSubscriptionsPerUser;
    }

    @Override
    public boolean isAllowSharedUri() {
        return allowSharedUri;
    }

    @Override
    public long getMaxTimeToLiveMillis() {
        return maxTimeToLiveMillis;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getSignatureSecret() {
        return signatureSecret;
    }

    @Override
    public String getWebhookId() {
        return webhookId;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public String getAuthorization() {
        return authorization;
    }

    @Override
    public BasicAuthCredentials getBasicAuthCredentials() {
        return basicAuthCredentials;
    }

    @Override
    public String getSignatureHeaderName() {
        return signatureHeaderName;
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            int prime = 31;
            result = prime * 1 + ((webhookId == null) ? 0 : webhookId.hashCode());
            result = prime * result + ((uri == null) ? 0 : uri.hashCode());
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultWebhookInfo other = (DefaultWebhookInfo) obj;
        if (webhookId == null) {
            if (other.webhookId != null) {
                return false;
            }
        } else if (!webhookId.equals(other.webhookId)) {
            return false;
        }
        if (uri == null) {
            if (other.uri != null) {
                return false;
            }
        } else if (!uri.equals(other.uri)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('[');
        if (webhookId != null) {
            sb.append("webhookId=").append(webhookId).append(", ");
        }
        if (uri != null) {
            sb.append("uri=").append(uri).append(", ");
        }
        if (authorization != null) {
            sb.append("authorization=").append(authorization).append(", ");
        }
        if (basicAuthCredentials != null) {
            sb.append("basicAuthCredentials=").append(basicAuthCredentials).append(", ");
        }
        if (signatureSecret != null) {
            sb.append("signatureSecret=").append(signatureSecret).append(", ");
        }
        sb.append("version=").append(version).append(", ");
        if (signatureHeaderName != null) {
            sb.append("signatureHeaderName=").append(signatureHeaderName).append(", ");
        }
        sb.append("maxTimeToLiveMillis=").append(maxTimeToLiveMillis).append(", ");
        sb.append("allowSharedUri=").append(allowSharedUri);
        sb.append(']');
        return sb.toString();
    }

}
