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

import static com.openexchange.ajax.AJAXServlet.ACTION_AUTOLOGIN;
import static com.openexchange.ajax.fields.LoginFields.CLIENT_PARAM;
import static com.openexchange.ajax.fields.LoginFields.SHARE_TOKEN;
import java.util.Map;
import java.util.Optional;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.login.LoginTools;
import com.openexchange.authentication.Cookie;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.login.ConfigurationProperty;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestURL;
import com.openexchange.request.analyzer.utils.RequestAnalyzerUtils;
import com.openexchange.server.ServiceLookup;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link AutoLoginRequestAnalyzer} is a {@link RequestAnalyzer} which uses the session cookie
 * to determine the marker for the request
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 */
public class AutoLoginRequestAnalyzer extends AbstractCookieBasedRequestAnalyzer {

    private final String defaultClient;

    /**
     * Initializes a new {@link AutoLoginRequestAnalyzer}.
     *
     * @param services The {@link ServiceLookup}
     * @throws OXException in case a required service is missing from the service lookup
     */
    public AutoLoginRequestAnalyzer(ServiceLookup services) throws OXException {
        super(services, LoginServlet.SESSION_PREFIX);
        defaultClient = getDefaultClient(services);
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        if (!hasValidPrefix(data)) {
            // Prefix does not match
            return Optional.empty();
        }
        Optional<ModuleAndAction> optModuleAndAction = optModuleAndAction(prefix, data);
        if (optModuleAndAction.isEmpty()) {
            // Module or action is missing
            return Optional.empty();
        }
        String action = optModuleAndAction.get().action();
        if (!ACTION_AUTOLOGIN.equals(action) || false == LoginServlet.SERVLET_PATH_APPENDIX.equals(optModuleAndAction.get().module())) {
            // It's not an autologin action
            return Optional.empty();
        }

        Map<String, Cookie> cookies = getCookies(data);
        if (cookies.isEmpty()) {
            // Cookies are missing
            return Optional.of(AnalyzeResult.UNKNOWN);
        }

        String[] additionals = LoginTools.parseShareInformation(getShareToken(data.getParsedURL()));
        String name = getCookieName(data, additionals);
        Cookie matchingCookie = cookies.get(name);
        if (null == matchingCookie || Strings.isEmpty(matchingCookie.getValue())) {
            // No matching cookie
            return Optional.of(AnalyzeResult.UNKNOWN);
        }
        SessiondService service = sessiondService.get();
        return Optional.ofNullable(matchingCookie)
                       .map(cookie -> service.peekSession(cookie.getValue()))
                       .map(RequestAnalyzerUtils::createAnalyzeResult);
    }

    @Override
    protected String getClient(RequestData data) throws OXException {
        return data.getParsedURL()
                   .optParameter(CLIENT_PARAM)
                   .orElse(defaultClient);
    }

    // --------------------- private methods -----------------

    /**
     * Gets the share token from the given {@link RequestURL}
     *
     * @param url The url
     * @return The share token or null
     * @throws OXException in case the url is malformed
     */
    private String getShareToken(RequestURL url) throws OXException {
        return url.optParameter(SHARE_TOKEN).orElse(null);
    }

    /**
     * Gets the default client
     *
     * @param services The {@link ServiceLookup} containing the {@link ConfigurationService}
     * @return The default client
     * @throws OXException In case the {@link ConfigurationService} is missing
     */
    private String getDefaultClient(ServiceLookup services) throws OXException {
        ConfigurationService configService = services.getServiceSafe(ConfigurationService.class);
        return configService.getProperty(ConfigurationProperty.HTTP_AUTH_CLIENT.getPropertyName(),
                                         ConfigurationProperty.HTTP_AUTH_CLIENT.getDefaultValue());
    }
}
