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

package com.openexchange.request.analyzer.utils;

import static com.openexchange.authentication.AuthenticationResult.Status.SUCCESS;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.login.LoginRequestImpl;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.authentication.ResolvedAuthenticated;
import com.openexchange.context.ContextService;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Strings;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.listener.LoginListener;
import com.openexchange.login.listener.internal.LoginListenerRegistry;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzerExceptionCodes;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.Type;
import com.openexchange.request.analyzer.UserInfo;
import com.openexchange.segment.SegmentMarker;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.servlet.Header;
import com.openexchange.servlet.Headers;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.http.Authorization;
import com.openexchange.tools.servlet.http.Authorization.Credentials;
import com.openexchange.user.UserService;

/**
 * {@link RequestAnalyzerUtils} - A collection of utility methods for the request analyzer implementations
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 */
public class RequestAnalyzerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAnalyzerUtils.class);

    /**
     * Prevent instantiation.
     */
    private RequestAnalyzerUtils() {
        super();
    }

    // -------------------- public methods -----------------

    /**
     * Tries to perform a login to get the analyze result. Necessary service references are directly obtained from the
     * {@link ServerServiceRegistry} instance.
     * <p/>
     * The result is either {@link Type#UNKNOWN} if the authentication failed or {@link Type#SUCCESS}
     *
     * @param loginRequest The login request to analyze
     * @param loginListenerRegistry The login listener registry to use
     * @return The {@link AnalyzeResult}
     */
    public static AnalyzeResult analyzeViaLogin(LoginRequest loginRequest, LoginListenerRegistry loginListenerRegistry) {
        try {
            AuthenticationServiceRegistry authenticationServiceRegistry = ServerServiceRegistry.getServize(AuthenticationServiceRegistry.class, true);
            Optional<Authenticated> optAuth = login(authenticationServiceRegistry, loginRequest, loginListenerRegistry.getLoginListeners());
            if (optAuth.isEmpty()) {
                return AnalyzeResult.UNKNOWN;
            }
            Authenticated auth = optAuth.get();
            ContextService contextService = ServerServiceRegistry.getServize(ContextService.class, true);
            UserService userService = ServerServiceRegistry.getServize(UserService.class, true);
            DatabaseService databaseService = ServerServiceRegistry.getServize(DatabaseService.class, true);
            return convert2AnalyzeResult(contextService, userService, databaseService, auth, loginRequest.getLogin());
        } catch (OXException e) {
            LOG.debug("Generating an AnalyzeResult by performing a login failed. Returning UNKNOWN result.", e);
            return AnalyzeResult.UNKNOWN;
        }
    }

    /**
     * Tries to perform a login to get the analyze result.
     * <p>
     * The result is either {@link Type#UNKNOWN} if the authentication failed or {@link Type#SUCCESS}
     *
     * @param contextService The context service
     * @param userService The user service
     * @param databaseService A reference to the database service to derive the schema name associated with the context
     * @param authenticationServiceRegistry The authentication service registry to use
     * @param loginRequest The login request
     * @param loginListener The login listener
     * @return The {@link AnalyzeResult}
     */
    public static AnalyzeResult analyzeViaLogin(ContextService contextService, // @formatter:off
                                                UserService userService,
                                                ConfigDatabaseService databaseService,
                                                AuthenticationServiceRegistry authenticationServiceRegistry,
                                                LoginRequest loginRequest,
                                                List<LoginListener> loginListener) { // @formatter:on
        try {
            Optional<Authenticated> optAuth = login(authenticationServiceRegistry, loginRequest, loginListener);
            if (optAuth.isEmpty()) {
                return AnalyzeResult.UNKNOWN;
            }
            Authenticated auth = optAuth.get();
            return convert2AnalyzeResult(contextService, userService, databaseService, auth, loginRequest.getLogin());
        } catch (OXException e) {
            LOG.debug("Generating an AnalyzeResult by performing a login failed. Returning UNKNOWN result.", e);
            return AnalyzeResult.UNKNOWN;
        }
    }

    /**
     * Creates an {@link AnalyzeResult} from a given session
     *
     * @param session The session, or <code>null</code> to produce {@link AnalyzeResult#UNKNOWN}
     * @return The {@link AnalyzeResult}
     */
    public static AnalyzeResult createAnalyzeResult(Session session) {
        if (session == null) {
            return AnalyzeResult.UNKNOWN;
        }
        Object schema = session.getParameter(Session.PARAM_USER_SCHEMA);
        if (schema == null) {
            return AnalyzeResult.UNKNOWN;
        }

        // Create result
        int userId = session.getUserId();
        int contextId = session.getContextId();
        String login = session.getLogin();
        String schemaString = schema instanceof String s ? s : schema.toString();
        return new AnalyzeResult(SegmentMarker.of(schemaString),
                                 UserInfo.builder(contextId)
                                         .withUserId(userId)
                                         .withLogin(login)
                                         .build());
    }

    /**
     * Converts the given headers to a map
     *
     * @param headers The headers to convert
     * @return The map
     */
    public static Map<String, List<String>> convert2headerMap(Headers headers) {
        BinaryOperator<List<String>> combiner = (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        };
        return headers.stream()
                      .collect(Collectors.groupingBy(Header::name,
                                                     Collector.of(ArrayList::new,
                                                                  (l, h) -> l.add(h.value()),
                                                                  combiner)));
    }

    /**
     * Creates a login request which can be used for {@link #analyzeViaLogin(ContextService, UserService, AuthenticationServiceRegistry, LoginRequest, List)}
     *
     * @param login The login
     * @param password The password
     * @param client The client id
     * @param clientIp The client ip
     * @param userAgent The user agent
     * @param headers The request headers
     * @return The login request
     */
    public static LoginRequest createLoginRequest(String login, // @formatter:off
                                            String password,
                                            String client,
                                            String clientIp,
                                            String userAgent,
                                            Headers headers) { // @formatter:on
        Map<String, List<String>> headersMap = convert2headerMap(headers);
        return new LoginRequestImpl.Builder().login(login)
                                             .password(password)
                                             .client(client)
                                             .clientIP(clientIp)
                                             .userAgent(userAgent)
                                             .headers(headersMap)
                                             .build();
    }

    /**
     * Checks if the password contains illegal values or the login is empty
     *
     * @param creds The credentials to validate
     * @return <code>true</code> if the credentials are valid, <code>false</code> otherwise
     */
    public static boolean checkCredentials(Credentials creds) {
        return Authorization.checkLogin(creds.getPassword()) && Strings.isNotEmpty(creds.getLogin());
    }

    /**
     * Parses the url string from the given request data to an {@link URL}
     *
     * @param data The request data
     * @return The {@link URL}
     * @throws OXException in case the url is malformed
     */
    public static URL parseUrl(RequestData data) throws OXException {
        try {
            return new URL(data.getUrl());
        } catch (MalformedURLException e) {
            throw RequestAnalyzerExceptionCodes.INVALID_URL.create(e);
        }
    }

    /**
     * Gets the parameter with the given name from the list of name-value pairs
     *
     * @param queryParams A list of query parameters
     * @param name The name of the parameter
     * @return The optional parameter value
     */
    public static Optional<String> optParam(List<NameValuePair> queryParams, String name) {
        return queryParams.stream()
                          .filter(q -> name.equals(q.getName()))
                          .findFirst()
                          .map(NameValuePair::getValue);
    }

    // -------------------- private methods -----------------

    /**
     * Converts the authenticated result into an {@link AnalyzeResult}.
     *
     * @param contextService The context service
     * @param userService The user service
     * @param databaseService A reference to the database service to derive the schema name associated with the context
     * @param auth The authenticated result
     * @param login The users login
     * @return The {@link AnalyzeResult}
     * @throws OXException in case an error occurred while determine the marker
     */
    private static AnalyzeResult convert2AnalyzeResult(ContextService contextService, UserService userService, ConfigDatabaseService databaseService, Authenticated auth, String login) throws OXException {
        int contextId;
        int userId;
        if (auth instanceof ResolvedAuthenticated resolvedAuth) {
            contextId = resolvedAuth.getContextID();
            userId = resolvedAuth.getUserID();
        } else {
            contextId = contextService.getContextId(auth.getContextInfo());
            Context context = contextService.getContext(contextId);
            userId = userService.getUserId(auth.getUserInfo(), context);
        }
        String schemaName = databaseService.getSchemaName(contextId);
        UserInfo userInfo = UserInfo.builder(contextId)
                                    .withUserId(userId)
                                    .withLogin(login)
                                    .build();
        return new AnalyzeResult(SegmentMarker.of(schemaName), userInfo);
    }

    /**
     * Performs a login and returns the {@link Authenticated} if the authentication was successful. Otherwise an empty {@link Optional} is returned.
     *
     * @param authenticationServiceRegistry The authentication service registry
     * @param loginReq The login request
     * @param loginListeners The login listeners
     * @return The optional authenticated
     * @throws OXException if the login attempt is rendered as invalid or something goes wrong while authenticating
     */
    private static Optional<Authenticated> login(AuthenticationServiceRegistry authenticationServiceRegistry, LoginRequest loginReq, List<LoginListener> loginListeners) throws OXException {
        Map<String, Object> props = triggerLoginListeners(loginReq, loginListeners);
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                                                                 .withLogin(loginReq.getLogin())
                                                                 .withPassword(loginReq.getPassword())
                                                                 .withClient(loginReq.getClient())
                                                                 .withClientIP(loginReq.getClientIP())
                                                                 .withUserAgent(loginReq.getUserAgent())
                                                                 .withProperties(props)
                                                                 .build();

        AuthenticationResult result = authenticationServiceRegistry.doLogin(authRequest, false);
        return SUCCESS.equals(result.getStatus()) ? result.optAuthenticated() : Optional.empty();
    }

    /**
     * Triggers the {@link LoginListener}s and returns a properties map of properties set by these listeners
     *
     * @param loginRequest The login request
     * @param loginListener The login listener
     * @return The properties map
     * @throws OXException if the login attempt is rendered as invalid
     */
    private static Map<String, Object> triggerLoginListeners(LoginRequest loginRequest, List<LoginListener> loginListener) throws OXException {
        HashMap<String, Object> result = new HashMap<>(1);
        for (LoginListener listener : loginListener) {
            listener.onBeforeAuthentication(loginRequest, result);
        }
        return result;
    }
}
