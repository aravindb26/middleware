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

import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.createAnalyzeResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.requesthandler.Dispatcher;
import com.openexchange.authentication.Cookie;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.tools.functions.ErrorAwareSupplier;

/**
 * {@link CookieRequestAnalyzer} is a {@link RequestAnalyzer} which uses public session cookies to determine the marker
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class CookieRequestAnalyzer extends AbstractCookieBasedRequestAnalyzer {

    private final ErrorAwareSupplier<Dispatcher> dispatcher;

    /**
     * Initializes a new {@link CookieRequestAnalyzer}.
     *
     * @param services The {@link ServiceLookup}
     * @throws OXException in case a required service is missing
     */
    public CookieRequestAnalyzer(ServiceLookup services) throws OXException {
        super(services, LoginServlet.PUBLIC_SESSION_PREFIX);
        this.dispatcher = () -> services.getServiceSafe(Dispatcher.class);
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        if (false == hasValidPrefix(data)) {
            return Optional.empty();
        }
        Optional<ModuleAndAction> optModuleAndAction = optModuleAndAction(prefix, data);
        if (optModuleAndAction.isEmpty()) {
            return Optional.empty();
        }

        ModuleAndAction moduleAndAction = optModuleAndAction.get();
        if (false == dispatcher.get().mayUseFallbackSession(moduleAndAction.module(), moduleAndAction.action())) {
            return Optional.empty();
        }

        Map<String, Cookie> cookies = getCookies(data);
        if (cookies.isEmpty()) {
            return Optional.empty();
        }

        // Try expected cookie name if optional additional parameters are available ("getSessionObjectByAlternativeId")
        Optional<String[]> optAdditionals = optAdditionals(data);
        SessiondService service = sessiondService.get();
        if (optAdditionals.isPresent()) {
            String name = getCookieName(data, optAdditionals.get());
            return Optional.ofNullable(cookies.get(name))
                           .map(cookie -> service.getSessionByAlternativeId(cookie.getValue()))
                           .map(session -> createAnalyzeResult(session));
        }

        // Try all public session cookies, otherwise ("getSessionObjectByAnyAlternativeId")
        for (Cookie cookie : filterCookies(cookies)) {
            Session session = service.getSessionByAlternativeId(cookie.getValue());
            if (isValidCookie(cookie, data, session)) {
                return Optional.of(createAnalyzeResult(session));
            }
        }
        return Optional.empty();
    }

    @Override
    protected String getUserAgent(RequestData data) throws OXException {
        String result = getUserAgentHeader(data);
        return result == null ? "" : result;
    }

    @Override
    protected String getClient(RequestData data) throws OXException {
        // Client is null
        return null;
    }

    // ------------------------ private methods ---------------------

    /**
     * Checks if the cookie is valid
     *
     * @param cookie The cookie to check
     * @param data The request data
     * @param session The session associated with the cookie
     * @return <code>true</code> if the cookie is valid, <code>false</code> otherwise
     * @throws OXException
     */
    private boolean isValidCookie(Cookie cookie, RequestData data, Session session) throws OXException {
        String[] additionals = new String[] { String.valueOf(session.getContextId()), String.valueOf(session.getUserId()) };
        String cookieName = getCookieName(data, additionals);
        return cookie.getName().equals(cookieName);
    }

    /**
     * Gets the optional additionals for the cookie hash
     *
     * @param data The {@link RequestData}
     * @return The optional additionals
     * @throws OXException In case the url is malformed
     */
    private static Optional<String[]> optAdditionals(RequestData data) throws OXException {
        String user = data.getParsedURL().optParameter("user").orElse(null);
        String context = data.getParsedURL().optParameter("context").orElse(null);
        if (user == null || context == null) {
            return Optional.empty();
        }
        return Optional.of(new String[] { context, user });
    }

    /**
     * Gets all cookies with the public session prefix
     *
     * @param cookies The cookie map to filter
     * @return The filtered cookie list
     */
    private List<Cookie> filterCookies(Map<String, Cookie> cookies) {
        return cookies.entrySet()
                      .stream()
                      .filter(entry -> entry.getKey()
                                            .trim()
                                            .startsWith(cookiePrefix))
                      .map(entry -> entry.getValue())
                      .toList();
    }

}
