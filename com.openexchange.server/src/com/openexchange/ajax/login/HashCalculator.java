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

package com.openexchange.ajax.login;

import static com.openexchange.java.Strings.isEmpty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import com.openexchange.ajax.fields.LoginFields;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.servlet.Header;
import com.openexchange.servlet.Headers;
import com.openexchange.tools.encoding.Base64;

/**
 * {@link HashCalculator} - Calculates the hash string.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class HashCalculator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HashCalculator.class);

    private static final String USER_AGENT = LoginFields.USER_AGENT;
    private static final String CLIENT_PARAM = LoginFields.CLIENT_PARAM;

    // -------------------------------    SINGLETON    ------------------------------------------------ //

    private static final HashCalculator SINGLETON = new HashCalculator();

    /**
     * Gets the singleton instance of {@link HashCalculator}.
     *
     * @return The instance
     */
    public static HashCalculator getInstance() {
        return SINGLETON;
    }

    // -------------------------------    MEMBER STUFF    -------------------------------------------- //

    private volatile String[] fields;
    private volatile byte[] salt;

    private HashCalculator() {
        super();
        fields = Strings.getEmptyStrings();
        salt = new byte[0];
    }

    /**
     * Configures this {@link HashCalculator} using specified configuration service.
     *
     * @param service The configuration service
     * @throws OXException If 'com.openexchange.cookie.hash.salt' is not set
     */
    public void configure(final ConfigurationService service) throws OXException {
        if (null != service) {
            final String fieldList = service.getProperty("com.openexchange.cookie.hash.fields", "");
            fields = Pattern.compile("\\s*,\\s*").split(fieldList, 0);
            String property = service.getProperty("com.openexchange.cookie.hash.salt");
            if (Strings.isEmpty(property)) {
                throw OXException.general("Property \"com.openexchange.cookie.hash.salt\" is not set");
            }
            salt = property.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets the calculated hash string for specified request and client identifier.
     *
     * @param req The HTTP Servlet request
     * @param client The optional client identifier
     * @return The calculated hash string
     */
    public String getHash(final HttpServletRequest req, final String client) {
        return getHash(req, getUserAgent(req), client);
    }

    /**
     * Gets the calculated hash string for specified arguments.
     *
     * @param req The HTTP Servlet request
     * @param userAgent The optional <code>User-Agent</code> identifier
     * @param client The optional client identifier
     * @return The calculated hash string
     */
    public String getHash(final HttpServletRequest req, final String userAgent, final String client) {
        return getHash(req, userAgent, client, (String[])null);
    }

    /**
     * Gets the calculated hash string for specified arguments.
     *
     * @param req The HTTP Servlet request
     * @param userAgent The optional <code>User-Agent</code> identifier
     * @param client The optional client identifier
     * @param additionals Additional values to include in the hash, or <code>null</code> if not needed
     * @return The calculated hash string
     */
    public String getHash(HttpServletRequest req, String userAgent, String client, String... additionals) {
        String effectiveUserAgent = null == userAgent ? parseClientUserAgent(req, "") : userAgent;
        return getHash(extractHeaders(req), effectiveUserAgent, client, additionals);
    }

    /**
     * Gets the calculated hash string for specified arguments.
     *
     * @param headers The request headers
     * @param userAgent The optional <code>User-Agent</code> identifier
     *            This is typically retrieved from the clientUserAgent parameter or the user-agent header
     * @param client The optional client identifier
     * @param additionals Additional values to include in the hash, or <code>null</code> if not needed
     * @return The calculated hash string
     */
    public String getHash(Headers headers, String userAgent, String client, String... additionals) {
        try {
            StringBuilder traceBuilder = LOG.isTraceEnabled() ? new StringBuilder("md5 (") : null;
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Update digest with User-Agent info
            {
                String effectiveUserAgent = null == userAgent ? "" : userAgent;
                md.update(effectiveUserAgent.getBytes(Charsets.UTF_8));
                if (null != traceBuilder) {
                    traceBuilder.append("effectiveUserAgent=").append(effectiveUserAgent);
                }
            }

            // Update digest with client info
            if (null != client) {
                md.update(client.getBytes(Charsets.UTF_8));
                if (null != traceBuilder) {
                    traceBuilder.append(", client=").append(client);
                }
            }

            // Update digest with additional info (if any)
            if (null != additionals) {
                for (String value : additionals) {
                    md.update(value.getBytes(Charsets.UTF_8));
                    if (null != traceBuilder) {
                        traceBuilder.append(", additional=").append(value);
                    }
                }
            }

            // Update digest with configured header info (if any)
            String[] fields = this.fields;
            if (null != fields) {
                for (final String field : fields) {
                    final String header = null == field || 0 == field.length() ? null : headers.getFirstHeaderValue(field);
                    if (null != header && Strings.isNotEmpty(header)) {
                        md.update(header.getBytes(Charsets.UTF_8));
                        if (null != traceBuilder) {
                            traceBuilder.append(", ").append(field).append('=').append(field);
                        }
                    }
                }
            }

            // Update digest with configured salt
            byte[] salt = this.salt;
            if (null != salt) {
                md.update(salt);
                if (null != traceBuilder) {
                    traceBuilder.append(", salt=***");
                }
            }

            // Calculate hash & create its string representation
            String hash = removeNonWordCharactersFrom(Base64.encode(md.digest()));
            if (null != traceBuilder) {
                traceBuilder.append(") -> ").append(hash);
                LOG.trace(traceBuilder.toString());
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            LOG.error("", e);
        }
        return "";
    }

    /**
     * Gets the calculated hash string for specified request.
     *
     * @param req The HTTP Servlet request
     * @return The calculated hash string
     */
    public String getHash(final HttpServletRequest req) {
        return getHash(req, getClient(req));
    }

    /**
     * Gets the <code>"client"</code> request parameter or <code>"default"</code> if absent.
     *
     * @param req The HTTP Servlet request
     * @return The client identifier or <code>"default"</code> if absent
     */
    public static String getClient(final HttpServletRequest req) {
        final String parameter = req.getParameter(CLIENT_PARAM);
        return isEmpty(parameter) ? "default" : parameter;
    }

    /**
     * Gets the calculated hash string for user-agent only.
     *
     * @param req The HTTP Servlet request
     * @return The calculated hash string
     */
    public String getUserAgentHash(final HttpServletRequest req) {
        return getUserAgentHash(req, null);
    }

    /**
     * Gets the calculated hash string for user-agent only.
     *
     * @param req The HTTP Servlet request
     * @param userAgent The optional <code>User-Agent</code> identifier
     * @return The calculated hash string
     */
    public String getUserAgentHash(final HttpServletRequest req, final String userAgent) {
        final String md5 = com.openexchange.tools.HashUtility.getMD5(null == userAgent ? getUserAgent(req) : userAgent, "hex");
        return null == md5 ? "" : md5;
    }

    /**
     * Gets the <code>"clientUserAgent"</code> request parameter or given <code>defaultValue</code> if absent.
     *
     * @param req The request
     * @param defaultValue The default value
     * @return The <code>"clientUserAgent"</code> request parameter or given <code>defaultValue</code> if absent
     */
    private static String parseClientUserAgent(final HttpServletRequest req, final String defaultValue) {
        final String parameter = req.getParameter(USER_AGENT);
        return isEmpty(parameter) ? defaultValue : parameter;
    }

    /**
     * Gets the <code>"User-Agent"</code> request header or an empty String if absent.
     *
     * @param req The request
     * @return The <code>"User-Agent"</code> request header or an empty String if absent
     */
    public static String getUserAgent(final HttpServletRequest req) {
        String header = req.getHeader(com.openexchange.ajax.fields.Header.USER_AGENT);
        return header == null ? "" : header;
    }

    private static String removeNonWordCharactersFrom(String str) {
        if (null == str) {
            return null;
        }

        int length = str.length();
        if (length == 0) {
            return str;
        }

        StringBuilder sb = null;
        for (int i = 0, k = length; k-- > 0; i++) {
            char ch = str.charAt(i);
            if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9') && ch != '_') {
                // A non-word character
                if (null == sb) {
                    sb = new StringBuilder(length);
                    if (i > 0) {
                        sb.append(str, 0, i);
                    }
                }
            } else {
                // A word character
                if (null != sb) {
                    sb.append(ch);
                }
            }
        }

        return null == sb ? str : sb.toString();
    }

    /**
     * Extracts the headers from the given {@link HttpServletRequest}
     *
     * @param req The {@link HttpServletRequest}
     * @return The {@link Headers}
     */
    private static Headers extractHeaders(HttpServletRequest req) {
        List<Header> result = new ArrayList<>();
        Iterator<String> iter = req.getHeaderNames().asIterator();
        while (iter.hasNext()) {
            String headerName = iter.next();
            if (headerName == null) {
                continue;
            }
            Enumeration<String> headers = req.getHeaders(headerName);
            applyHeaderValues(headers, headerName, result);
        }
        return new Headers(result);
    }

    /**
     * Maps a header value enumeration to a list of headers and adds them to a given list
     *
     * @param headerValues The enumeration of header values
     * @param name The header name
     * @param headers The header list to add the headers to
     */
    private static void applyHeaderValues(Enumeration<String> headerValues, String name, List<Header> headers) {
        Iterator<String> iterValues = headerValues.asIterator();
        while (iterValues.hasNext()) {
            String value = iterValues.next();
            if (value != null) {
                headers.add(new Header(name, value));
            }
        }
    }

}
