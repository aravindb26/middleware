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

package com.openexchange.authentication.common;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.b;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.Cookie;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Pair;


/**
 * {@link AbstractConfigurationAwareAuthenticationService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public abstract class AbstractConfigurationAwareAuthenticationService implements AuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigurationAwareAuthenticationService.class);

    private static final String REFERER_HEADER = "Referer";

    protected final String identifier;
    protected final Map<String, String> optionals;

    /**
     * Initializes a new {@link AbstractConfigurationAwareAuthenticationService}.
     *
     * @param identifier The identifier
     */
    protected AbstractConfigurationAwareAuthenticationService(String identifier) {
        super();
        this.identifier = identifier;
        optionals = Collections.singletonMap(AuthenticationServiceProperty.OPTIONAL_FIELD, identifier);
    }

    @Override
    public AuthenticationResult doLogin(AuthenticationRequest authenticationRequest, boolean autologin) throws OXException {
        if (false == isResponsibleFor(authenticationRequest)) {
            return AuthenticationResult.failed();
        }
        return autologin ? handleAutoLoginRequest(authenticationRequest) : handleLoginRequest(authenticationRequest);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int getRanking() {
        try {
            return getConfigurationService().getIntProperty(AuthenticationServiceProperty.ranking, optionals);
        } catch (Exception e) {
            LOG.warn("(lean) configuration service cannot be obtained", e);
            return ((Integer) AuthenticationServiceProperty.ranking.getDefaultValue()).intValue();
        }
    }

    /**
     * Gets the (lean) configuration service.
     *
     * @return The (lean) configuration service
     * @throws OXException If the (lean) configuration service cannot be returned
     */
    protected abstract LeanConfigurationService getConfigurationService() throws OXException;

    /**
     * This method maps the login information from the login screen to the both parts needed to resolve the context and the user of that
     * context.
     *
     * @param authenticationRequest An {@link AuthenticationRequest} object containing the information needed for login
     * @return an {@link AuthenticationResult} containing context information to resolve the context and user information to resolve the user.
     * This return type can be enhanced with {@link SessionEnhancement} and/or {@link ResponseEnhancement}.
     * @throws OXException If something with the login info is wrong.
     */
    public abstract AuthenticationResult handleLoginRequest(AuthenticationRequest request) throws OXException;

    /**
     * This method authenticates a user using a global web services session which is useful in single sign on scenarios. If no such global
     * web services session exists either throw a {@link LoginException} or redirect the browser to some global login site with
     * {@link ResultCode#REDIRECT}. This method should never return <code>null</code>.
     *
     * If the implementing authentication bundle does not support some global web services single sign on this method has to throw
     * {@link LoginExceptionCodes#NOT_SUPPORTED}.
     *
     * @param authenticationRequest An {@link AuthenticationRequest} object containing the information needed for login
     * @return an {@link AuthenticationResult} containing context information to resolve the context and user information to resolve the user.
     * This return type can be enhanced with {@link SessionEnhancement} and/or {@link ResponseEnhancement}.
     * @throws OXException if something with the login info is wrong and no {@link AuthenticationResult} can be returned.
     */
    public abstract AuthenticationResult handleAutoLoginRequest(AuthenticationRequest request) throws OXException;

    /**
     * Checks if authentication service is responsible for an {@link AuthenticationRequest}
     *
     * @param authenticationRequest An {@link AuthenticationRequest} object containing the information needed for login
     * @return <code>true</code> if service is responsible for given authentication request, <code>false</code> otherwise
     * @throws OXException If operation fails
     */
    protected boolean isResponsibleFor(AuthenticationRequest authenticationRequest) throws OXException {
        LeanConfigurationService configService = getConfigurationService();
        {
            // Check that the host name matches
            String value = configService.getProperty(AuthenticationServiceProperty.hostnames, optionals);
            Optional<String> referer = getRefererDomain(authenticationRequest);
            if (Strings.isNotEmpty(value) && referer.isPresent()) {
                Pattern pattern = Pattern.compile(value);
                if (false == pattern.matcher(referer.get()).matches()) {
                    return false;
                }
            }
        }

        {
            // Check that the header and header values match
            String value = configService.getProperty(AuthenticationServiceProperty.customHeaders, optionals);
            if (Strings.isNotEmpty(value)) {
                String[] headers = Strings.splitByComma(value);
                try {
                    for (String header : headers) {
                        Pair<String, Optional<String>> keyValue = getKeyAndValue(header);
                        if (keyValue.getSecond().isPresent()) {
                            if (false == checkHeader(authenticationRequest, keyValue.getFirst(), keyValue.getSecond().get())) {
                                return false;
                            }
                        } else {
                            if (false == checkHeader(authenticationRequest, keyValue.getFirst())) {
                                return false;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    LOG.error("Invalid configuration", e);
                    return false;
                }
            }
        }

        {
            // Check that the cookies match
            String value = configService.getProperty(AuthenticationServiceProperty.customCookies, optionals);
            if (Strings.isNotEmpty(value)) {
                String[] cookies = Strings.splitByComma(value);
                try {
                    for (String cookie : cookies) {
                        Pair<String, Optional<String>> keyValue = getKeyAndValue(cookie);
                        if (Strings.isNotEmpty(keyValue.getFirst()) && keyValue.getSecond().isPresent()) {
                            if (false == checkCookie(authenticationRequest, keyValue.getFirst(), keyValue.getSecond().get())) {
                                return false;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    LOG.error("Invalid configuration", e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Converts a key-value string in format key=value into a Pair with key as first element and (optional) value as second element
     *
     * @param s The key-value string
     * @return The pair
     */
    protected Pair<String, Optional<String>> getKeyAndValue(String s) {
        String[] splitted = Strings.splitBy(s, '=', true);
        switch (splitted.length) {
            case 0:
                throw new RuntimeException("Invalid configuration, header/cookie is empty.");
            case 1:
                return new Pair<String, Optional<String>>(splitted[0], Optional.empty());
            case 2:
                return new Pair<String, Optional<String>>(splitted[0], Optional.of(splitted[1]));
            default:
                throw new RuntimeException("Invalid configuration, headers and cookies must be configured like 'name=value'.");
        }
    }

    /**
     * Gets the HTTP headers from given {@link AuthenticationRequest}
     *
     * @param authenticationRequest The request
     * @return The HTTP headers mapped by header name
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getRequestHeaders(AuthenticationRequest authenticationRequest) {
        Map<String, Object> props = authenticationRequest.getProperties();
        if (null != props && false == props.isEmpty()) {
            if (props.containsKey("headers")) {
                return (Map<String,Object>) props.get("headers");
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Gets the cookies from given {@link AuthenticationRequest}
     *
     * @param authenticationRequest The request
     * @return An array containing the cookies
     */
    protected Cookie[] getRequestCookies(AuthenticationRequest authenticationRequest) {
        Map<String, Object> props = authenticationRequest.getProperties();
        if (null != props && false == props.isEmpty()) {
            if (props.containsKey("cookies")) {
                return (Cookie[]) props.get("cookies");
            }
        }
        return new Cookie[0];
    }

    /**
     * Gets the referer of the {@link AuthenticationRequest}, if available
     *
     * @param authenticationRequest The request
     * @return The optional referer header's value
     */
    protected Optional<String> getRefererDomain(AuthenticationRequest authenticationRequest) {
        Map<String, Object> headers = getRequestHeaders(authenticationRequest);
        if (headers.containsKey(REFERER_HEADER)) {
            Object refererObject = headers.get(REFERER_HEADER);
            if (refererObject instanceof List) {
                @SuppressWarnings("unchecked") List<String> referer = (List<String>) refererObject;
                if (false == referer.isEmpty()) {
                    try {
                        URI uri = new URI(referer.get(0));
                        return Optional.ofNullable(uri.getHost());
                    } catch (URISyntaxException e) {
                        LOG.debug("{} contains an invalid uri.", REFERER_HEADER, e);
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if {@link AuthenticationRequest} contains header with given name
     *
     * @param request The request
     * @param headerName The expected header's name
     * @return <code>true</code> if header with given name was found, <code>false</code> otherwise
     */
    protected boolean checkHeader(AuthenticationRequest request, String headerName) {
        return getRequestHeaders(request).containsKey(headerName);
    }

    /**
     * Checks if {@link AuthenticationRequest} contains header with given name and given value
     *
     * @param request The request
     * @param headerName The expected header's name
     * @param headerValue The expected header's values
     * @return <code>true</code> if header with given name and value was found, <code>false</code> otherwise
     */
    protected boolean checkHeader(AuthenticationRequest request, String headerName, String headerValue) {
        Map<String, Object> headers = getRequestHeaders(request);
        if (headers.containsKey(headerName)) {
            Pattern regex = Pattern.compile(headerValue);
            @SuppressWarnings("unchecked") List<String> headerValues = (List<String>) headers.get(headerName);
            return headerValues.stream().allMatch(value -> regex.matcher(value).matches());
        }
        return false;
    }

    /**
     * Checks if {@link AuthenticationRequest} contains a cookie with given name and given value
     *
     * @param request The request
     * @param cookieName The expected cookie's name
     * @param cookieValue The expected cookie's value
     * @return <code>true</code> if expected cookie was found, <code>false</code> otherwise
     */
    protected boolean checkCookie(AuthenticationRequest request, String cookieName, String cookieValue) {
        List<Cookie> cookies = Arrays.asList(getRequestCookies(request));
        Pattern regex = Pattern.compile(cookieValue);
        // @formatter:off
        return b(cookies.stream()
                        .filter(c -> c.getName().equals(cookieName))
                        .findAny()
                        .map((c) -> B(regex.matcher(c.getValue()).matches()))
                        .orElse(Boolean.FALSE));
        // @formatter:on
    }

}
