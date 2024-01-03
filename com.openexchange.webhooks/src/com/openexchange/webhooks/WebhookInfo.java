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
import com.openexchange.java.Strings;

/**
 * {@link WebhookInfo} - Information for a Webhook like its identifier, URI end-point, authentication data, etc.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface WebhookInfo {

    /**
     * The default version number: <code>1</code>.
     */
    public static final int DEFAULT_VERSION = 1;

    /**
     * The default name for the signature header: <code>"X-OX-Signature"</code>
     */
    public static final String DEFAULT_SIGNATURE_HEADER_NAME = "X-OX-Signature";

    /**
     * The default URI validation mode: {@link WebhookUriValidationMode#PREFIX PREFIX}.
     */
    public static final WebhookUriValidationMode DEFAULT_URI_VALIDATION_MODE = WebhookUriValidationMode.PREFIX;

    /**
     * Checks if the URI end-point is HTTPS.
     *
     * @return <code>true</code> if the URI end-point is HTTPS; otherwise <code>false</code>
     */
    default boolean isHttps() {
        URI uri = getUri();
        return uri != null && "https".equals(Strings.asciiLowerCase(uri.getScheme()));
    }

    /**
     * Gets the version number for signing.
     *
     * @return The version number
     */
    int getVersion();

    /**
     * Gets the shared secret between client and end-point host for signing.
     *
     * @return The shared secret
     */
    String getSignatureSecret();

    /**
     * Gets the name of the signature header.
     *
     * @return The name of the signature header
     * @see #DEFAULT_SIGNATURE_HEADER_NAME
     */
    String getSignatureHeaderName();

    /**
     * Gets the Webhook identifier.
     *
     * @return The Webhook identifier
     */
    String getWebhookId();

    /**
     * Gets the Webhook's URI.
     *
     * @return The URI
     */
    URI getUri();

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
    String getAuthorization();

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
    BasicAuthCredentials getBasicAuthCredentials();

    /**
     * Gets the max. time to live in milliseconds for the Webhook before considered as expired.
     *
     * @return The max. time to live in milliseconds
     */
    long getMaxTimeToLiveMillis();

    /**
     * Gets the max. number of subscriptions for this Webhook allowed for a single user.
     *
     * @return The max. number of subscriptions
     */
    int getMaxNumberOfSubscriptionsPerUser();

    /**
     * Gets a value indicating whether the same URI can be used by multiple different users or not.
     * 
     * @return <code>true</code> if shared URIs are allowed, <code>false</code> otherwise
     */
    boolean isAllowSharedUri();

    /**
     * Gets the mode defining how the possible client-specified URI for a Webhook end-point is supposed
     * to be validated against the URI from configured Webhook end-point.
     *
     * @return The URI validation mode
     */
    WebhookUriValidationMode getUriValidationMode();

}
