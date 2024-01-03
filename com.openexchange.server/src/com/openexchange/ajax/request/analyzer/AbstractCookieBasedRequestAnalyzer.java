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

package com.openexchange.ajax.request.analyzer;

import static com.openexchange.ajax.fields.Header.USER_AGENT;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.openexchange.ajax.fields.LoginFields;
import com.openexchange.ajax.login.HashCalculator;
import com.openexchange.authentication.Cookie;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.http.AuthCookie;

/**
 * {@link AbstractCookieBasedRequestAnalyzer} is an abstract {@link AbstractPrefixAwareRequestAnalyzer} which provides
 * some additional methods for cookie handling.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public abstract class AbstractCookieBasedRequestAnalyzer extends AbstractPrefixAwareRequestAnalyzer {

    protected final String cookiePrefix;

    /**
     * Initializes a new {@link AbstractCookieBasedRequestAnalyzer}.
     *
     * @param services The {@link ServiceLookup}
     * @param cookiePrefix The cookie prefix of the cookie which should be analyzed
     * @throws OXException in case the {@link DispatcherPrefixService} is missing
     */
    AbstractCookieBasedRequestAnalyzer(ServiceLookup services, String cookiePrefix) throws OXException {
        super(services);
        this.cookiePrefix = cookiePrefix;
    }

    /**
     * Gets the client information
     *
     * @param data The request data
     * @return The client or null
     * @throws OXException In case of errors while determining the client
     */
    protected abstract String getClient(RequestData data) throws OXException;

    /**
     * Gets the user agent
     *
     * @param data The request data
     * @return The user agent or null
     * @throws OXException In case the request url is malformed
     */
    protected String getUserAgent(RequestData data) throws OXException {
        return data.getParsedURL()
                   .optParameter(LoginFields.USER_AGENT)
                   .orElseGet(() -> getUserAgentHeader(data));
    }

    /**
     * Gets the user agent header
     *
     * @param data the request data
     * @return The user agent header or null
     */
    protected final String getUserAgentHeader(RequestData data) {
        return data.getHeaders()
                   .getFirstHeaderValue(USER_AGENT);
    }

    /**
     * Gets the cookie name
     *
     * @param data The request data
     * @param additionals The optional additionals
     * @return The cookie name
     * @throws OXException in case of errors
     */
    protected final String getCookieName(RequestData data, String[] additionals) throws OXException {
        String client = getClient(data);
        String agent = getUserAgent(data);
        String hash = HashCalculator.getInstance().getHash(data.getHeaders(),
                                          agent == null ? "" : agent,
                                          client,
                                          additionals);
        return cookiePrefix + hash;
    }

    // -------------------- static methods --------------------

    /**
     * Gets all cookies of the request
     *
     * @param data The request data
     * @return The cookie map
     */
    protected static Map<String, Cookie> getCookies(RequestData data) {
        String cookieHeader = data.getHeaders().getHeaderValue("Cookie", ";");

        if (cookieHeader == null) {
            return Collections.emptyMap();
        }

        return Stream.of(cookieHeader.split(";"))
                     .map(AbstractCookieBasedRequestAnalyzer::toCookie)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toMap(Cookie::getName, c -> c));
    }

    /**
     * Creates a cookie from the given cookie string
     *
     * @param cookieString The cookie string
     * @return the cookie
     */
    private static Cookie toCookie(String cookieString) {
        String[] split = cookieString.split("=");
        if (split.length != 2) {
            return null;
        }
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(split[0].trim(), split[1].trim());
        return new AuthCookie(cookie);
    }

}
