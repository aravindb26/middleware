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

package com.openexchange.webdav.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.webdav.client.jackrabbit.WebDAVClientImpl;

/**
 * {@link WebDAVClientFactory}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public interface WebDAVClientFactory {

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    WebDAVClient create(HttpClient client, URI baseUrl) throws OXException;

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    default WebDAVClient create(HttpClient client, String baseUrl) throws OXException {
        try {
            return create(client, new URI(baseUrl));
        } catch (URISyntaxException e) {
            throw WebDAVClientExceptionCodes.UNABLE_TO_PARSE_URI.create(e, baseUrl);
        }
    }

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param context The underlying HTTP context to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    WebDAVClient create(HttpClient client, HttpContext context, URI baseUrl) throws OXException;

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param client The underlying HTTP client to use
     * @param context The underlying HTTP context to use
     * @param baseUrl The URL of the WebDAV host to connect to
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    default WebDAVClient create(HttpClient client, HttpContext context, String baseUrl) throws OXException {
        try {
            return create(client, context, new URI(baseUrl));
        } catch (URISyntaxException e) {
            throw WebDAVClientExceptionCodes.UNABLE_TO_PARSE_URI.create(e, baseUrl);
        }
    }

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param session The users session
     * @param accountId The account id
     * @param baseUrl The URL of the WebDAV host to connect to
     * @param optClientId The optional http client id to use
     * @param context The {@link HttpClientContext} to use
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    WebDAVClient create(Session session, String accountId, URI baseUrl, Optional<String> optClientId, HttpContext context) throws OXException;

    /**
     * Initializes a new {@link WebDAVClientImpl}.
     *
     * @param session The users session
     * @param accountId The account id
     * @param baseUrl The URL of the WebDAV host to connect to
     * @param context The {@link HttpClientContext} to use
     * @return An initialized WebDAV client
     * @throws If WebDAV client cannot be created
     */
    default WebDAVClient create(Session session, String accountId, String baseUrl, HttpContext context) throws OXException {
        try {
            return create(session, accountId, new URI(baseUrl), Optional.empty(), context);
        } catch (URISyntaxException e) {
            throw WebDAVClientExceptionCodes.UNABLE_TO_PARSE_URI.create(e, baseUrl);
        }
    }

}

