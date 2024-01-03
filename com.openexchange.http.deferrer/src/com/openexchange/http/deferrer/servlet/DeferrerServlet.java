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

package com.openexchange.http.deferrer.servlet;

import static com.openexchange.ajax.AJAXUtility.encodeUrl;
import static com.openexchange.java.Strings.isEmpty;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.http.deferrer.CustomRedirectURLDetermination;
import com.openexchange.http.deferrer.impl.AbstractDeferringURLService;
import com.openexchange.java.LongReference;
import com.openexchange.java.Strings;
import com.openexchange.tools.servlet.http.Tools;

/**
 * {@link DeferrerServlet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DeferrerServlet extends HttpServlet {

    private static final long serialVersionUID = 1358634554782437089L;

    private static final Property PROPERTY_MAX_PARAMETER_LENGTH = DefaultProperty.valueOf("com.openexchange.http.deferrer.maxParameterLength", Long.valueOf(1024*10l)); // 10 KB

    /**
     * The listing for custom handlers.
     */
    public static final List<CustomRedirectURLDetermination> CUSTOM_HANDLERS = new CopyOnWriteArrayList<CustomRedirectURLDetermination>();

    private final long maxParameterLength;

    /**
     * Initializes a new {@link DeferrerServlet}.
     */
    public DeferrerServlet(LeanConfigurationService lean) {
        super();
        this.maxParameterLength = null != lean ? lean.getLongProperty(PROPERTY_MAX_PARAMETER_LENGTH) : -1;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Create a new HttpSession if it's missing
            req.getSession(true);

            String defaultCharEnc = ServerConfig.getProperty(ServerConfig.Property.DefaultEncoding);
            LongReference size = new LongReference();

            // Determine the URL
            RedirectUri redirectUri = determineRedirectURL(req, size, defaultCharEnc);
            if (redirectUri == null) {
                Tools.sendErrorPage(resp, HttpServletResponse.SC_NOT_FOUND, "No such redirect URI.");
                return;
            }

            // Get the URL to defer/to redirect
            String redirectURL = redirectUri.redirectUri;

            // Check URI in case grabbed from parameter
            if (RedirectUri.Type.PARAMETER == redirectUri.type) {
                if (!isRelative(redirectURL)) {
                    Tools.sendErrorPage(resp, HttpServletResponse.SC_BAD_REQUEST, "Specified location must not be absolute.");
                    return;
                }

                if (!isServerRelative(redirectURL)) {
                    String referer = purgeHost(req.getHeader("referer"));
                    if (referer == null) {
                        Tools.sendErrorPage(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing \"referer\" header");
                        return;
                    }

                    redirectURL = assumeRelative(referer, redirectURL);
                }
            }

            char concat = '?';
            if (redirectURL.indexOf('?') >= 0) {
                concat = '&';
            }

            Map<String, String> params = parseQueryStringFromUrl(redirectURL, defaultCharEnc);
            StringBuilder builder = new StringBuilder(encodeUrl(redirectURL, true, false));
            for (Enumeration<?> parameterNames = req.getParameterNames(); parameterNames.hasMoreElements();) {
                String name = (String) parameterNames.nextElement();
                if ("redirect".equals(name)) {
                    continue;
                }

                String parameter = req.getParameter(name);
                if (maxParameterLength > 0) {
                    // Add both the parameter's name size and the parameter's content size
                    size.incrementValue((name != null ? name.getBytes(defaultCharEnc).length : 0) + (parameter != null ? parameter.getBytes(defaultCharEnc).length : 0));
                    if (size.getValue() > maxParameterLength) {
                        throw new RequestParametersTooBigException();
                    }
                }

                if (params.containsKey(name)) {
                    continue;
                }

                builder.append(concat);
                concat = '&';
                builder.append(name).append('=').append(encodeUrl(parameter, true, false));
            }
            resp.sendRedirect(builder.toString());
        } catch (RequestParametersTooBigException e) {
            Tools.sendErrorPage(resp, HttpServletResponse.SC_REQUEST_URI_TOO_LONG, "Request parameters contain too much data");
            return;
        }
    }

    private RedirectUri determineRedirectURL(HttpServletRequest req, LongReference size, String defaultCharEnc) throws UnsupportedEncodingException, RequestParametersTooBigException {
        for (CustomRedirectURLDetermination determination : CUSTOM_HANDLERS) {
            String url = determination.getURL(req);
            if (url != null) {
                return new RedirectUri(prepareCustomRedirectURL(url, defaultCharEnc), RedirectUri.Type.REGISTERED);
            }
        }

        // Get redirect URI from "redirect" request parameter
        String name = "redirect";
        String redirectUri = req.getParameter(name);
        if (redirectUri == null) {
            return null;
        }

        if (maxParameterLength > 0) {
            // Add both the parameter's name size and the parameter's content size
            size.incrementValue(name.getBytes(defaultCharEnc).length + redirectUri.getBytes(defaultCharEnc).length);
            if (size.getValue() > maxParameterLength) {
                throw new RequestParametersTooBigException();
            }
        }
        return new RedirectUri(redirectUri, RedirectUri.Type.PARAMETER);
    }

    /**
     * Checks if custom redirect URL starts with deferrer path.
     * <p>
     * E.g. /ajax/defer?redirect=http:%2F%2Fmy.host.com%2Fpath...
     * <p>
     * This avoids duplicate redirect as redirect URL would again redirect to <code>DeferrerServlet</code>.
     *
     * @param url The redirect URL to check
     * @param defaultCharEnc The default charset to use
     * @return The checked redirect URL
     */
    private String prepareCustomRedirectURL(String url, String defaultCharEnc) {
        try {
            final URL jUrl = new URL(url);
            final String path = jUrl.getPath();
            if (null != path && Strings.toLowerCase(path).startsWith(getDeferrerPath())) {
                final String query = jUrl.getQuery();
                if (null != query) {
                    final Map<String, String> params = parseQueryString(query, defaultCharEnc);
                    if (1 == params.size() && params.containsKey("redirect")) {
                        final String redirect = params.get("redirect");
                        return isEmpty(redirect) ? url : redirect;
                    }
                }
            }
        } catch (Exception e) {
            final Logger logger = org.slf4j.LoggerFactory.getLogger(DeferrerServlet.class);
            logger.debug("", e);
        }
        return url;
    }

    private String getDeferrerPath() {
        return new StringBuilder(AbstractDeferringURLService.PREFIX.get().getPrefix()).append("defer").toString();
    }

    /**
     * Parses a query string from given URL.
     *
     * @param url The URL string to be parsed
     * @param defaultCharEnc The default charset to use
     * @return The parsed parameters
     */
    private Map<String, String> parseQueryStringFromUrl(String url, String defaultCharEnc) {
        try {
            final URL jUrl = new URL(url);
            final String query = jUrl.getQuery();
            if (null != query) {
                return parseQueryString(query, defaultCharEnc);
            }
        } catch (Exception e) {
            final Logger logger = org.slf4j.LoggerFactory.getLogger(DeferrerServlet.class);
            logger.debug("", e);
        }
        return Collections.emptyMap();
    }

    private static final java.util.regex.Pattern PATTERN_SPLIT = java.util.regex.Pattern.compile("&");

    /**
     * Parses given query string.
     *
     * @param queryStr The query string to be parsed
     * @param defaultCharEnc The default charset to use
     * @return The parsed parameters
     */
    private Map<String, String> parseQueryString(final String queryStr, String defaultCharEnc)  {
        final String[] paramsNVPs = PATTERN_SPLIT.split(queryStr, 0);
        final Map<String, String> map = new LinkedHashMap<String, String>(4);
        for (String paramsNVP : paramsNVPs) {
            paramsNVP = paramsNVP.trim();
            if (paramsNVP.length() > 0) {
                // Look-up character '='
                final int pos = paramsNVP.indexOf('=');
                if (pos >= 0) {
                    map.put(paramsNVP.substring(0, pos), Utility.decodeUrl(paramsNVP.substring(pos + 1), defaultCharEnc));
                } else {
                    map.put(paramsNVP, "");
                }
            }
        }
        return map;
    }

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(\\w*:)?//");

    private boolean isRelative(final String location) {
        final Matcher matcher = PROTOCOL_PATTERN.matcher(location);
        return !matcher.find();
    }

    private static final Pattern HOST_PATTERN = Pattern.compile("^(\\w*:)?//\\w*/");

    private String purgeHost(final String location) {
        if (location == null) {
            return null;
        }
        return HOST_PATTERN.matcher(location).replaceAll("");
    }

    private boolean isServerRelative(final String location) {
        return location.length() > 0 && location.charAt(0) == '/';
    }

    private String assumeRelative(String referer, String location) {
        int index = referer.lastIndexOf('/');
        if (index >= 0) {
            return "/" + referer.substring(0, index) + "/" + location;
        }

        return "/" + referer + "/" + location;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class RequestParametersTooBigException extends Exception {

        private static final long serialVersionUID = -4416685697137347354L;

        /**
         * Initializes a new {@link RequestParametersTooBigException}.
         */
        RequestParametersTooBigException() {
            super();
        }
    }

}
