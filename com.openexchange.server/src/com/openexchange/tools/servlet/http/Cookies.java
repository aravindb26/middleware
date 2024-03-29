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

package com.openexchange.tools.servlet.http;

import static com.openexchange.net.IPAddressUtil.textToNumericFormatV4;
import static com.openexchange.net.IPAddressUtil.textToNumericFormatV6;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import com.google.common.net.InternetDomainName;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.config.ConfigurationService;
import com.openexchange.java.Strings;
import com.openexchange.net.HostList;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link Cookies} - Utility class for {@link Cookie}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Cookies {

    /**
     * Initializes a new {@link Cookies}.
     */
    private Cookies() {
        super();
    }

    private static final HostList LOCALS = HostList.of("localhost", "127.0.0.1", "::1");

    /**
     * Checks if specified request's server name is considered as part of local LAN.
     *
     * @param request The request
     * @return <code>true</code> if considered as part of local LAN; otherwise <code>false</code>
     */
    public static boolean isLocalLan(final HttpServletRequest request) {
        return isLocalLan(request.getServerName());
    }

    /**
     * Checks if specified server name is considered as part of local LAN.
     *
     * @param serverName The server name
     * @return <code>true</code> if considered as part of local LAN; otherwise <code>false</code>
     */
    public static boolean isLocalLan(final String serverName) {
        if (com.openexchange.java.Strings.isEmpty(serverName)) {
            return false;
        }
        return LOCALS.contains(serverName.toLowerCase(Locale.US));
    }

    private static volatile Boolean domainEnabled;

    /**
     * Checks whether domain parameter is enabled
     *
     * @return <code>true</code> if enabled; otherwise <code>false</code>
     */
    public static boolean domainEnabled() {
        Boolean tmp = domainEnabled;
        if (null == tmp) {
            synchronized (LoginServlet.class) {
                tmp = domainEnabled;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    tmp = Boolean.valueOf(null != service && service.getBoolProperty("com.openexchange.cookie.domain.enabled", false));
                    domainEnabled = tmp;
                }
            }
        }
        return tmp.booleanValue();
    }

    private static volatile Boolean prefixWithDot;

    /**
     * Checks whether domain parameter should start with a dot (<code>'.'</code>) character
     *
     * @return <code>true</code> for starting dot; otherwise <code>false</code>
     */
    public static boolean prefixWithDot() {
        Boolean tmp = prefixWithDot;
        if (null == tmp) {
            synchronized (LoginServlet.class) {
                tmp = prefixWithDot;
                if (null == tmp) {
                    final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    tmp = Boolean.valueOf(null == service || service.getBoolProperty("com.openexchange.cookie.domain.prefixWithDot", true));
                    prefixWithDot = tmp;
                }
            }
        }
        return tmp.booleanValue();
    }

    private static volatile String configuredDomain;

    /**
     * Gets the configured domain or <code>null</code>
     *
     * @return The configured domain or <code>null</code>
     */
    public static String configuredDomain() {
        String tmp = configuredDomain;
        if (null == tmp) {
            synchronized (LoginServlet.class) {
                tmp = configuredDomain;
                if (null == tmp) {
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return null;
                    }

                    tmp = service.getProperty("com.openexchange.cookie.domain", "null").trim();
                    configuredDomain = tmp;
                }
            }
        }
        return "null".equalsIgnoreCase(tmp) ? null : tmp;
    }

    /**
     * Gets the (possible) domain argument to use when creating a cookie.
     *
     * @param serverName The server name
     * @return The domain parameter or <code>null</code>
     * @see #getDomainValueFromConfiguration()
     */
    public static String getDomainValue(final String serverName) {
        String domain = getDomainValueFromConfiguration(serverName);
        if (null != domain) {
            return domain;
        }
        return null;
    }

    /**
     * Gets the domain parameter for specified server name with configured default behavior whether to prefix domain with a dot (
     * <code>'.'</code>) character.
     *
     * @param serverName The server name
     * @return The domain parameter or <code>null</code>
     * @see #prefixWithDot()
     * @see #configuredDomain()
     */
    public static String getDomainValueFromConfiguration(final String serverName) {
        if (!domainEnabled()) {
            return null;
        }
        final String configuredDomain = configuredDomain();
        if (null != configuredDomain) {
            return configuredDomain;
        }
        return getDomainValue(serverName, prefixWithDot(), null, true);
    }

    /**
     * Gets the domain parameter for specified server name.
     *
     * @param serverName The server name
     * @param prefixWithDot Whether to prefix domain with a dot (<code>'.'</code>) character
     * @param configuredDomain The pre-configured domain name for this host
     * @param domainEnabled Whether to write a domain parameter at all (<code>null</code> is immediately returned)
     * @return The domain parameter or <code>null</code>
     */
    public static String getDomainValue(final String serverName, final boolean prefixWithDot, final String configuredDomain, final boolean domainEnabled) {
        if (!domainEnabled) {
            return null;
        }
        if (null != configuredDomain) {
            return configuredDomain;
        }
        // Try by best-guessed attempt
        if (null == serverName) {
            return null;
        }
        if (prefixWithDot) { // Follow RFC 2109 syntax for domain name
            if (serverName.startsWith("www.")) {
                return serverName.substring(3);
            } else if ("localhost".equalsIgnoreCase(serverName)) {
                return null;
            } else {
                if (null == textToNumericFormatV4(serverName) && (null == textToNumericFormatV6(serverName))) {
                    // Not an IP address
                    final int fpos = serverName.indexOf('.');
                    if (fpos < 0) {
                        return null; // Equal to server name
                    }
                    final int pos = serverName.indexOf('.', fpos + 1);
                    if (pos < 0) {
                        return null; // Equal to server name
                    }
                    final String domain = serverName.substring(fpos);
                    final InternetDomainName tmp = InternetDomainName.from(domain);
                    if (tmp.isPublicSuffix()) {
                        return null; // Equal to server name
                    }
                    return domain;
                }
            }
        } else {
            if (isValidDomainValue(serverName)) {
                return serverName.toLowerCase(Locale.US).startsWith("www.") ? serverName.substring(4) : serverName;
            }
        }
        return null;
    }

    /**
     * Checks if specified server/host name is valid for being used as domain value.
     * <ul>
     * <li>Not <code>"localhost"</code></li>
     * <li>Not an IPv4 identifier</li>
     * <li>Not an IPv6 identifier</li>
     * </ul>
     *
     * @param serverName The server name
     * @return <code>true</code> if server/host name is valid for being used as domain value; otherwise <code>false</code>
     */
    public static boolean isValidDomainValue(String serverName) {
        return (null != serverName && !"localhost".equalsIgnoreCase(serverName) && (null == textToNumericFormatV4(serverName)) && (null == textToNumericFormatV6(serverName)));
    }

    /**
     * Pretty-prints given cookies.
     *
     * @param cookies The cookies
     * @return The string representation
     */
    public static String prettyPrint(final Cookie[] cookies) {
        if (null == cookies) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(cookies.length << 4);
        final String sep = Strings.getLineSeparator();
        for (int i = 0; i < cookies.length; i++) {
            final Cookie cookie = cookies[i];
            sb.append(i + 1).append(": ").append(cookie.getName());
            sb.append('=').append(cookie.getValue());
            sb.append("; version=").append(cookie.getVersion());
            final int maxAge = cookie.getMaxAge();
            if (maxAge >= 0) {
                sb.append("; max-age=").append(maxAge);
            }
            final String path = cookie.getPath();
            if (null != path) {
                sb.append("; path=").append(path);
            }
            final String domain = cookie.getDomain();
            if (null != domain) {
                sb.append("; domain=").append(domain);
            }
            final boolean secure = cookie.getSecure();
            if (secure) {
                sb.append("; secure");
            }
            sb.append(sep);
        }
        return sb.toString();
    }

    /**
     * Creates a cookie map for given HTTP request.
     *
     * @param req The HTTP request
     * @return The cookie map or {@link java.util.Collections#emptyMap()}
     */
    public static Map<String, Cookie> cookieMapFor(final HttpServletRequest req) {
        if (null == req) {
            return Collections.emptyMap();
        }
        @SuppressWarnings("unchecked") Map<String, Cookie> m = (Map<String, Cookie>) req.getAttribute("__cookie.map");
        if (null != m) {
            return m;
        }
        final Cookie[] cookies = req.getCookies();
        if (null == cookies) {
            return Collections.emptyMap();
        }
        final int length = cookies.length;
        m = new LinkedHashMap<String, Cookie>(length);
        for (final Cookie cookie : cookies) {
            m.put(cookie.getName(), cookie);
        }
        req.setAttribute("__cookie.map", m);
        return m;
    }
}
