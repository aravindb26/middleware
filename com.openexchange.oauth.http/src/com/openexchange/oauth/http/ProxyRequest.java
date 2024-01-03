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

package com.openexchange.oauth.http;

import java.util.Map;
import com.openexchange.oauth.OAuthAccount;

/**
 * {@link ProxyRequest} is a wrapper for proxy requests informations
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class ProxyRequest {

    private final HTTPMethod method;
    private final Map<String, String> parameters;
    private final Map<String, String> headers;
    private final String url;
    private final String body;
    private final OAuthAccount account;

    /**
     * Initializes a new {@link ProxyRequest}.
     *
     * @param account The {@link OAuthAccount}
     * @param method The method of the request
     * @param parameters The parameters of the request
     * @param headers The headers of the request
     * @param url The target url of the request
     * @param body The optional body of the request (in case of a PUT)
     */
    public ProxyRequest(OAuthAccount account, HTTPMethod method, Map<String, String> parameters, Map<String, String> headers, String url, String body) {
        super();
        this.account = account;
        this.method = method;
        this.parameters = parameters;
        this.headers = headers;
        this.url = url;
        this.body = body;
    }

    /**
     * Gets the method
     *
     * @return The method
     */
    public HTTPMethod getMethod() {
        return method;
    }

    /**
     * Gets the parameters
     *
     * @return The parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Gets the headers
     *
     * @return The headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Gets the url
     *
     * @return The url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the body
     *
     * @return The body
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the account
     *
     * @return The account
     */
    public OAuthAccount getAccount() {
        return account;
    }

}
