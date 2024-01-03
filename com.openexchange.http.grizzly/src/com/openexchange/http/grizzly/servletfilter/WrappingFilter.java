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

package com.openexchange.http.grizzly.servletfilter;

import static com.openexchange.http.grizzly.http.servlet.HttpServletRequestWrapper.HTTPS_SCHEME;
import static com.openexchange.http.grizzly.http.servlet.HttpServletRequestWrapper.HTTP_SCHEME;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.http.grizzly.GrizzlyConfig;
import com.openexchange.http.grizzly.http.servlet.HttpServletRequestWrapper;
import com.openexchange.http.grizzly.http.servlet.HttpServletResponseWrapper;
import com.openexchange.http.grizzly.osgi.Services;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.net.ClientIPUtil;
import com.openexchange.version.VersionService;

/**
 * {@link WrappingFilter} - Wrap the Request in {@link HttpServletResponseWrapper} and the Response in {@link HttpServletResponseWrapper}
 * and creates a new HttpSession.
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class WrappingFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(WrappingFilter.class);

    /** The attribute name for Grizzly's request number */
    private static final String ATTRIBUTE_REQUEST_NUMBER = "grizzly.reqnum";

    private static final Pattern PATTERN_CRLF = Pattern.compile("\r?\n|\r|(?:%0[aA])?%0[dD]|%0[aA]");

    private static final int MAX_LOGGED_HEADER_LENGTH = 1024;

    private static String dropLinebreaks(String s) {
        // Strip possible "\r?\n" and/or "%0A?%0D"
        return PATTERN_CRLF.matcher(s).replaceAll("");
    }

    private static final String LOCAL_HOST;
    static {
        String fbHost;
        try {
            fbHost = InetAddress.getLocalHost().getHostAddress();
        } catch (@SuppressWarnings("unused") UnknownHostException e) {
            fbHost = "127.0.0.1";
        }
        LOCAL_HOST = fbHost;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    private final String version;
    private final AtomicLong counter;
    private final int serverId;
    private final String protocolHeader;
    private final boolean isConsiderXForwards;
    private final String echoHeaderName;
    private final boolean considerEchoHeader;
    private final String contentSecurityPolicy;
    private final boolean considerContentSecurityPolicy;
    private final boolean checkTrackingIdInRequestParameters;
    private final String robotsMetaTag;
    private final boolean considerRobotsMetaTag;
    private final List<String> additionalHeaders;

    private final ClientIPUtil ipUtils;

    /**
     * Initializes a new {@link WrappingFilter}.
     *
     * @param config The grizzly config
     * @param ipUtils The ip util
     */
    public WrappingFilter(GrizzlyConfig config, ClientIPUtil ipUtils) {
        super();
        this.ipUtils = ipUtils;
        version = Services.getService(VersionService.class).getVersion().toString();
        serverId = Math.abs(OXException.getServerId());
        counter = new AtomicLong(0); // new AtomicLong(serverId >> 1);
        this.protocolHeader = config.getProtocolHeader();
        this.isConsiderXForwards = config.isConsiderXForwards();
        this.echoHeaderName = config.getEchoHeader();
        this.considerEchoHeader = Strings.isNotEmpty(echoHeaderName);
        this.robotsMetaTag = config.getRobotsMetaTag();
        this.considerRobotsMetaTag = Strings.isNotEmpty(robotsMetaTag);
        this.contentSecurityPolicy = config.getContentSecurityPolicy();
        this.considerContentSecurityPolicy = Strings.isNotEmpty(contentSecurityPolicy);

        this.checkTrackingIdInRequestParameters = config.isCheckTrackingIdInRequestParameters();
        this.additionalHeaders = config.getAdditionalHttpHeaders().isEmpty() ? null : config.getAdditionalHttpHeaders();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Inspect echoHeader and when present copy it to Response
        String echoHeaderValue = considerEchoHeader ? httpRequest.getHeader(echoHeaderName) : null;
        if (echoHeaderValue != null) {
            echoHeaderValue = dropLinebreaks(echoHeaderValue);
            httpResponse.setHeader(echoHeaderName, echoHeaderValue);
        }

        // Set Content-Security-Policy header
        if (considerContentSecurityPolicy) {
            String contentSecurityPolicy = this.contentSecurityPolicy;
            httpResponse.setHeader("Content-Security-Policy", contentSecurityPolicy);
            httpResponse.setHeader("X-WebKit-CSP", contentSecurityPolicy);
            httpResponse.setHeader("X-Content-Security-Policy", contentSecurityPolicy);
        }

        if (considerRobotsMetaTag) {
            httpResponse.setHeader("X-Robots-Tag", robotsMetaTag);
        }

        // Inspect X-Forwarded headers and create HttpServletRequestWrapper accordingly
        httpRequest = buildHttpServletRequestWrapper(httpRequest);

        // Build the HttpServletResponseWrapper instance
        httpResponse = new HttpServletResponseWrapper(httpResponse, echoHeaderName, echoHeaderValue);

        // Set LogProperties
        {

            // Servlet related properties
            LogProperties.put(LogProperties.Name.GRIZZLY_REQUEST_URI, httpRequest.getRequestURI());
            LogProperties.put(LogProperties.Name.GRIZZLY_SERVLET_PATH, httpRequest.getServletPath());
            LogProperties.put(LogProperties.Name.GRIZZLY_PATH_INFO, httpRequest.getPathInfo());
            {
                String queryString = httpRequest.getQueryString();
                LogProperties.put(LogProperties.Name.GRIZZLY_QUERY_STRING, null == queryString ? "<none>" : LogProperties.getSanitizedQueryString(queryString));
            }
            LogProperties.put(LogProperties.Name.GRIZZLY_METHOD, httpRequest.getMethod());

            // Remote infos
            LogProperties.put(LogProperties.Name.GRIZZLY_REMOTE_PORT, Integer.toString(httpRequest.getRemotePort()));
            LogProperties.put(LogProperties.Name.GRIZZLY_REMOTE_ADDRESS, httpRequest.getRemoteAddr());

            // Names, addresses
            Thread currentThread = Thread.currentThread();
            LogProperties.put(LogProperties.Name.GRIZZLY_THREAD_NAME, currentThread.getName());
            LogProperties.put(LogProperties.Name.THREAD_ID, Long.toString(currentThread.getId()));
            LogProperties.put(LogProperties.Name.LOCALHOST_IP_ADDRESS, LOCAL_HOST);
            LogProperties.put(LogProperties.Name.LOCALHOST_VERSION, version);
            LogProperties.put(LogProperties.Name.GRIZZLY_SERVER_NAME, httpRequest.getServerName());
            {
                String userAgent = httpRequest.getHeader("User-Agent");
                LogProperties.put(LogProperties.Name.GRIZZLY_USER_AGENT, null == userAgent ? "<unknown>" : userAgent);
            }

            // Tracking identifier
            long requestNumber = getRequestNumber();
            String trackingId;
            if (checkTrackingIdInRequestParameters) {
                trackingId = request.getParameter("trackingId");
                if (trackingId == null) {
                    trackingId = generateTrackingId(requestNumber);
                }
            } else {
                trackingId = generateTrackingId(requestNumber);
            }

            // Some more log properties for the request
            LogProperties.putProperty(LogProperties.Name.REQUEST_TRACKING_ID, trackingId);
            request.setAttribute(ATTRIBUTE_REQUEST_NUMBER, Long.valueOf(requestNumber));
            if (additionalHeaders != null) {
                for (String headerName : additionalHeaders) {
                    String headerValue = httpRequest.getHeader(headerName);
                    if (Strings.isNotEmpty(headerValue)) {
                        headerValue = Strings.sanitizeString(headerValue.length() > MAX_LOGGED_HEADER_LENGTH ? Strings.abbreviate(headerValue, MAX_LOGGED_HEADER_LENGTH) : headerValue);
                        LogProperties.putProperty("com.openexchange.request." + headerName.toLowerCase(), headerValue);
                    }
                }
            }
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private long getRequestNumber() {
        long count = counter.incrementAndGet();
        while (count < 0) {
            long newNext = 1;
            count = counter.compareAndSet(count, newNext) ? newNext : counter.incrementAndGet();
        }
        return count;
    }

    private String generateTrackingId(long requestNumber) {
        return new StringBuilder(16).append(serverId).append('-').append(requestNumber).toString();
    }

    private HttpServletRequestWrapper buildHttpServletRequestWrapper(HttpServletRequest httpRequest) {
        if (!isConsiderXForwards) {
            return new HttpServletRequestWrapper(httpRequest);
        }

        // Determine remote IP address
        String remoteAddress = ipUtils.getIP(httpRequest);

        // Determine protocol/scheme of the incoming request
        String protocol = httpRequest.getHeader(protocolHeader);
        if (!isValidProtocol(protocol)) {
            LOG.debug("Could not detect a valid protocol header value in {}, falling back to default", protocol);
            protocol = httpRequest.getScheme();
        }

        return new HttpServletRequestWrapper(protocol, remoteAddress, httpRequest.getServerPort(), httpRequest);
    }

    private static boolean isValidProtocol(String protocolHeaderValue) {
        return HTTP_SCHEME.equals(protocolHeaderValue) || HTTPS_SCHEME.equals(protocolHeaderValue);
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }

}
