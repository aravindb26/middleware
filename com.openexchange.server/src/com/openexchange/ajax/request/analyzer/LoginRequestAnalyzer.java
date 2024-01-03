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

import static com.openexchange.ajax.AJAXServlet.ACTION_LOGIN;
import static com.openexchange.ajax.AJAXServlet.PARAMETER_ACTION;
import static com.openexchange.ajax.LoginServlet.ACTION_FORMLOGIN;
import static com.openexchange.ajax.LoginServlet.SERVLET_PATH_APPENDIX;
import static com.openexchange.ajax.fields.LoginFields.AUTHID_PARAM;
import static com.openexchange.ajax.fields.LoginFields.CLIENT_IP_PARAM;
import static com.openexchange.ajax.fields.LoginFields.CLIENT_PARAM;
import static com.openexchange.ajax.fields.LoginFields.LOGIN_PARAM;
import static com.openexchange.ajax.fields.LoginFields.NAME_PARAM;
import static com.openexchange.ajax.fields.LoginFields.PASSWORD_PARAM;
import static com.openexchange.ajax.fields.LoginFields.SHARE_TOKEN;
import static com.openexchange.ajax.fields.LoginFields.STAY_SIGNED_IN;
import static com.openexchange.ajax.fields.LoginFields.TRANSIENT;
import static com.openexchange.ajax.fields.LoginFields.USER_AGENT;
import static com.openexchange.ajax.fields.LoginFields.VERSION_PARAM;
import static com.openexchange.login.Interface.HTTP_JSON;
import static com.openexchange.request.analyzer.utils.RequestAnalyzerUtils.analyzeViaLogin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.login.HashCalculator;
import com.openexchange.ajax.login.LoginConfiguration;
import com.openexchange.ajax.login.LoginRequestImpl;
import com.openexchange.ajax.login.LoginTools;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.authentication.Cookie;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.login.LoginRequest;
import com.openexchange.login.listener.internal.LoginListenerRegistry;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.BodyData;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestURL;
import com.openexchange.request.analyzer.utils.RequestAnalyzerUtils;
import com.openexchange.server.ServiceLookup;

/**
 * {@link LoginRequestAnalyzer} is a {@link RequestAnalyzer} which uses the users credentials
 * to determine the marker for the request
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 */
public class LoginRequestAnalyzer extends AbstractPrefixAwareRequestAnalyzer {

    private final LoginListenerRegistry loginListenerRegistry;

    /**
     * Initializes a new {@link LoginRequestAnalyzer}.
     *
     * @param services The service lookup
     * @param loginListenerRegistry The {@link LoginListenerRegistry} to use
     * @throws OXException
     */
    public LoginRequestAnalyzer(ServiceLookup services, LoginListenerRegistry loginListenerRegistry) throws OXException {
        super(services);
        this.loginListenerRegistry = loginListenerRegistry;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        RequestURL url = data.getParsedURL();

        Optional<String> optPath = url.getPath();
        if (optPath.isEmpty()) {
            return Optional.empty();
        }

        String module = AJAXRequestDataTools.getModuleFromPath(prefix, optPath.get());
        String action = url.optParameter(PARAMETER_ACTION).orElse(null);
        boolean isFormLogin = ACTION_FORMLOGIN.equals(action);
        boolean isLogin = ACTION_LOGIN.equals(action);

        if (module == null || !module.equals(SERVLET_PATH_APPENDIX)) {
            // Wrong module
            return Optional.empty();
        }

        if (!(isLogin || isFormLogin)) {
            // Action is neither login nor formlogin
            return Optional.empty();
        }

        Optional<BodyData> body = data.optBody();
        if (body.isEmpty()) {
            // Body is missing
            return Optional.of(AnalyzeResult.MISSING_BODY);
        }

        Optional<LoginRequest> loginRequest = buildLoginRequest(data, isFormLogin ? LOGIN_PARAM : NAME_PARAM);
        return Optional.of(loginRequest.isEmpty() ? AnalyzeResult.UNKNOWN : analyzeViaLogin(loginRequest.get(), loginListenerRegistry));
    }

    /**
     * Method to build a LoginRequest
     *
     * @param data The request data
     * @param loginParameterName The expected name of the login parameter
     * @return The LoginRequest, or empty if no value login request could be parsed
     * @throws OXException If parsing share information fails
     */
    private static Optional<LoginRequest> buildLoginRequest(RequestData data, String loginParameterName) throws OXException {
        /*
         * extract body parameters & parse login name
         */
        Optional<BodyData> requestBody = data.optBody();
        if (requestBody.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String[]> parametersMap = new TreeMap<String, String[]>(String.CASE_INSENSITIVE_ORDER);
        addToMap(parseBodyParameters(requestBody.get()), parametersMap);
        String login = getFirstParameterValue(parametersMap, loginParameterName);
        if (null == login) {
            return Optional.empty(); // no login name in body, no useful login request
        }
        /*
         * parse further login request parameters from request URI/headers/body
         */
        RequestURL url = data.getParsedURL();
        addToMap(parseRequestParameters(url), parametersMap);
        LoginConfiguration loginConf = LoginServlet.getLoginConfiguration();
        String password = getFirstParameterValue(parametersMap, PASSWORD_PARAM);
        String authId = getFirstParameterValue(parametersMap, AUTHID_PARAM, UUIDs.getUnformattedString(UUID.randomUUID()));
        String client = getFirstParameterValue(parametersMap, CLIENT_PARAM, loginConf.getDefaultClient());
        String version = getFirstParameterValue(parametersMap, VERSION_PARAM);
        String userAgent = getFirstParameterValue(parametersMap, USER_AGENT, data.getHeaders().getFirstHeaderValue("User-Agent"));
        String clientIp = getFirstParameterValue(parametersMap, CLIENT_IP_PARAM, data.getClientIp());
        boolean staySignedIn = AJAXRequestDataTools.parseBoolParameter(getFirstParameterValue(parametersMap, STAY_SIGNED_IN));
        String tranzient = getFirstParameterValue(parametersMap, TRANSIENT, "false");
        String[] additionals = LoginTools.parseShareInformation(url.optParameter(SHARE_TOKEN).orElse(null));
        /*
         * extract and convert cookies
         */
        Map<String, Cookie> cookieMap = AbstractCookieBasedRequestAnalyzer.getCookies(data);
        Cookie[] cookies = cookieMap.values().toArray(new Cookie[cookieMap.size()]);
        /*
         * build & return login request
         */
        return Optional.of(new LoginRequestImpl.Builder()
                .login(login)
                .password(password)
                .clientIP(clientIp)
                .userAgent(userAgent)
                .authId(authId)
                .client(client)
                .version(version)
                .hash(HashCalculator.getInstance().getHash(data.getHeaders(), userAgent, clientIp, additionals))
                .headers(RequestAnalyzerUtils.convert2headerMap(data.getHeaders()))
                .requestParameter(parametersMap)
                .serverName(url.getURL().getHost())
                .serverPort(url.getURL().getPort())
                .staySignedIn(staySignedIn)
                .cookies(cookies)
                .iface(HTTP_JSON)
                .tranzient(Boolean.parseBoolean(tranzient))
                .build());
    }

    /**
     * Adds a collection of name-value-pairs into the supplied map holding all parameter values by their name.
     *
     * @param nameValuePairs The name-value-pairs to add
     * @param parametersMap The map to put the name-value-pairs into
     */
    private static void addToMap(List<NameValuePair> nameValuePairs, Map<String, String[]> parametersMap) {
        if (null == nameValuePairs || nameValuePairs.isEmpty()) {
            return;
        }
        for (NameValuePair nameValuePair : nameValuePairs) {
            String[] values = parametersMap.get(nameValuePair.getName());
            if (null == values) {
                parametersMap.put(nameValuePair.getName(), new String[] { nameValuePair.getValue() });
            } else {
                parametersMap.put(nameValuePair.getName(), com.openexchange.tools.arrays.Arrays.add(values, nameValuePair.getValue()));
            }
        }
    }

    /**
     * Parses the parameters from the given <code>application/x-www-form-urlencoded</code> request body data as name-value-pairs.
     *
     * @param bodyData The body data to parse
     * @return The parsed form data parameters
     */
    private static List<NameValuePair> parseBodyParameters(BodyData bodyData) {
        if (null != bodyData) {
            try {
                return URLEncodedUtils.parse(bodyData.getDataAsString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(LoginRequestAnalyzer.class).debug("Error parsing form parameters from body: {}", e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Parses the parameters from the given request URI as name-value-pairs.
     *
     * @param requestURL The request URL to parse
     * @return The parsed parameters
     */
    private static List<NameValuePair> parseRequestParameters(RequestURL requestURL) {
        if (null != requestURL && null != requestURL.getURL()) {
            String query = requestURL.getURL().getQuery();
            if (null != query) {
                return URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Gets the first parameter value matching the given name from the supplied parameter map.
     *
     * @param parametersMap The parameter map to get the first matching value from
     * @param parameterName The name of the parameter to match
     * @return The first matching value, or <code>null</code> if not found
     */
    private static String getFirstParameterValue(Map<String, String[]> parametersMap, String parameterName) {
        return getFirstParameterValue(parametersMap, parameterName, null);
    }

    /**
     * Gets the first parameter value matching the given name from the supplied parameter map, falling back to the supplied alternative.
     *
     * @param parametersMap The parameter map to get the first matching value from
     * @param parameterName The name of the parameter to match
     * @param fallback The value to return as fallback
     * @return The first matching value, or the given fallback if not found
     */
    private static String getFirstParameterValue(Map<String, String[]> parametersMap, String parameterName, String fallback) {
        if (null == parametersMap || parametersMap.isEmpty()) {
            return fallback;
        }
        String[] values = parametersMap.get(parameterName);
        if (null == values || 0 == values.length) {
            return fallback;
        }
        return values[0];
    }

}
