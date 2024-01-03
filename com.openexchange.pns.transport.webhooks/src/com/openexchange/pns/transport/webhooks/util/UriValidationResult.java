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
import java.util.Objects;

/**
 * {@link UriValidationResult} - The result when calling {@link WebhookInfoUtils#validateUri(String, com.openexchange.webhooks.WebhookInfo, com.openexchange.webhooks.WebhookInfo validateUri())}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class UriValidationResult {

    private final URI uri;
    private final boolean fromClient;

    /**
     * Initializes a new {@link UriValidationResult}.
     *
     * @param uri The effective URI to use in request
     * @param fromClient Whether URI originates from client-specified Webhook information or not
     */
    public UriValidationResult(URI uri, boolean fromClient) {
        super();
        this.uri = uri;
        this.fromClient = fromClient;
    }

    /**
     * Gets the effective URI to use in request.
     *
     * @return The URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Checks if URI originates from client-specified Webhook information or not.
     *
     * @return <code>true</code> if client-specified; otherwise <code>false</code>
     */
    public boolean isFromClient() {
        return fromClient;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Boolean.valueOf(fromClient), uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UriValidationResult)) {
            return false;
        }
        UriValidationResult other = (UriValidationResult) obj;
        return fromClient == other.fromClient && Objects.equals(uri, other.uri);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (uri != null) {
            builder.append("uri=").append(uri).append(", ");
        }
        builder.append("fromClient=").append(fromClient).append("]");
        return builder.toString();
    }

}
