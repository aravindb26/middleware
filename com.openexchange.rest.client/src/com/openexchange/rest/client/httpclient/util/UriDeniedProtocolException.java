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


package com.openexchange.rest.client.httpclient.util;

import java.net.URI;
import org.apache.http.ProtocolException;

/**
 * {@link UriDeniedProtocolException} - Special protocol exception signaling that a certain URI is denied being accessed.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class UriDeniedProtocolException extends ProtocolException {

    private static final long serialVersionUID = -3243571074341228994L;

    private final URI uri;

    /**
     * Creates a new UriDeniedProtocolException with default detail message: <code>"Access to URI denied"</code>.
     */
    public UriDeniedProtocolException() {
        this(null);
    }

    /**
     * Creates a new UriDeniedProtocolException with default detail message: <code>"Access to URI denied: " + $URI</code>.
     *
     * @param uri The URI to which access has been denied or <code>null</code>
     */
    public UriDeniedProtocolException(URI uri) {
        super(uri == null ? "Access to URI denied" : "Access to URI denied: " + uri);
        this.uri = uri;
    }

    /**
     * Creates a new UriDeniedProtocolException with the specified detail message.
     *
     * @param message The exception detail message
     * @param uri The URI to which access has been denied or <code>null</code>
     */
    public UriDeniedProtocolException(String message, URI uri) {
        super(message == null ? "Access to URI denied" : message);
        this.uri = uri;
    }

    /**
     * Creates a new UriDeniedProtocolException with the specified detail message and cause.
     *
     * @param message the exception detail message
     * @param uri The URI to which access has been denied or <code>null</code>
     * @param cause the {@code Throwable} that caused this exception, or {@code null} if the cause is unavailable, unknown, or not a {@code Throwable}
     */
    public UriDeniedProtocolException(String message, URI uri, Throwable cause) {
        super(message == null ? "Access to URI denied" : message, cause);
        this.uri = uri;
    }

    /**
     * Gets the URI to which access has been denied.
     *
     * @return The URI or <code>null</code> if not set
     */
    public URI getUri() {
        return uri;
    }
}
