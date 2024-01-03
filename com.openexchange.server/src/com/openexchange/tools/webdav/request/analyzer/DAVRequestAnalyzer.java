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
package com.openexchange.tools.webdav.request.analyzer;

import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.analyzeViaLogin;
import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.checkCredentials;
import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.createAnalyzeResult;
import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.createLoginRequest;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import com.openexchange.ajax.Client;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.listener.internal.LoginListenerRegistry;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.http.Authorization;
import com.openexchange.tools.servlet.http.Authorization.Credentials;
import com.openexchange.tools.webdav.AuthorizationHeader;
import com.openexchange.tools.webdav.DAVServletPathProvider;
import com.openexchange.tools.webdav.WebDAVSessionService;

/**
 * {@link DAVRequestAnalyzer} is a {@link RequestAnalyzer} which uses the authorization header
 * to determine the marker for the request
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class DAVRequestAnalyzer extends RankingAwareNearRegistryServiceTracker<DAVServletPathProvider> implements RequestAnalyzer {

    private final WebDAVSessionService webDAVSessionService;
    private final LoginListenerRegistry loginListenerRegistry;

    /**
     * Initializes a new {@link DAVRequestAnalyzer}.
     *
     * @param context The bundle context
     * @param services The {@link ServiceLookup} to use
     * @param webDAVSessionService The {@link WebDAVSessionService} to use
     * @param loginListenerRegistry The {@link LoginListenerRegistry} to use
     */
    public DAVRequestAnalyzer(BundleContext context, ServiceLookup services, WebDAVSessionService webDAVSessionService, LoginListenerRegistry loginListenerRegistry) {
        super(context, DAVServletPathProvider.class);
        this.webDAVSessionService = webDAVSessionService;
        this.loginListenerRegistry = loginListenerRegistry;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        Optional<String> path = data.getParsedURL().getPath();
        if (path.isEmpty() || getServiceList().stream().noneMatch(pathProvider -> pathProvider.servesPath(path.get()))) {
            return Optional.empty();
        }
        String authHeader = data.getHeaders().getFirstHeaderValue("Authorization");
        if (Strings.isEmpty(authHeader)) {
            return Optional.of(AnalyzeResult.UNKNOWN); // unauthenticated
        }

        AuthorizationHeader authorizationHeader = AuthorizationHeader.parseSafe(authHeader);
        if (null == authorizationHeader) {
            return Optional.of(AnalyzeResult.UNKNOWN); // unauthenticated
        }

        if (false == "basic".equalsIgnoreCase(authorizationHeader.getScheme())) {
            return Optional.empty(); // probably OAuth; handled by com.openexchange.oauth.provider.impl.request.analyzer.OAuthRequestAnalyzer
        }

        Credentials creds = Authorization.decode(authHeader);
        if (false == checkCredentials(creds)) {
            return Optional.of(AnalyzeResult.UNKNOWN); // unauthenticated
        }

        String userAgent = data.getHeaders().getFirstHeaderValue("user-agent");
        String client = Client.WEBDAV_INFOSTORE.getClientId();

        LoginRequest loginRequest = createLoginRequest(creds.getLogin(),
                                                       creds.getPassword(),
                                                       client,
                                                       data.getClientIp(),
                                                       userAgent,
                                                       data.getHeaders());
        Optional<Session> optSession = webDAVSessionService.optSession(loginRequest);
        AnalyzeResult result = optSession.isPresent() ? createAnalyzeResult(optSession.get()) : analyzeViaLogin(loginRequest, loginListenerRegistry);
        return Optional.of(result);
    }

}
