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

package com.openexchange.ajax.redirect;

import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.AJAXUtility;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.java.LongReference;
import com.openexchange.tools.servlet.http.Tools;

/**
 * {@link RedirectServlet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedirectServlet extends HttpServlet {

    private static final long serialVersionUID = 6111473866164506367L;

    private static final Property PROPERTY_MAX_PARAMETER_LENGTH = DefaultProperty.valueOf("com.openexchange.http.deferrer.maxParameterLength", Long.valueOf(1024*10l)); // 10 KB

    private final long maxParameterLength;

    /**
     * Initializes a new {@link RedirectServlet}.
     */
    public RedirectServlet(LeanConfigurationService lean) {
        super();
        this.maxParameterLength = null != lean ? lean.getLongProperty(PROPERTY_MAX_PARAMETER_LENGTH) : -1;
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        // create a new HttpSession if it's missing
        req.getSession(true);

        String location = req.getParameter("location");
        if (location == null) {
            Tools.sendErrorPage(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing \"location\" parameter");
            return;
        }

        if (maxParameterLength > 0) {
            LongReference size = new LongReference();
            String defaultCharEnc = ServerConfig.getProperty(ServerConfig.Property.DefaultEncoding);
            for (Enumeration<?> parameterNames = req.getParameterNames(); parameterNames.hasMoreElements();) {
                String name = (String) parameterNames.nextElement();
                String parameter = req.getParameter(name);
                // Add both the parameter's name size and the parameter's content size
                size.incrementValue((name != null ? name.getBytes(defaultCharEnc).length : 0) + (parameter != null ? parameter.getBytes(defaultCharEnc).length : 0));
                if (size.getValue() > maxParameterLength) {
                    Tools.sendErrorPage(resp, HttpServletResponse.SC_REQUEST_URI_TOO_LONG, "Request parameters contain too much data");
                    return;
                }
            }
        }

        if (!isRelative(location)) {
            Tools.sendErrorPage(resp, HttpServletResponse.SC_BAD_REQUEST, "Specified location must not be absolute.");
            return;
        }

        if (isServerRelative(location)) {
            resp.sendRedirect(AJAXUtility.encodeUrl(location, true));
            return;
        }

        String referer = purgeHost(req.getHeader("referer"));
        if (referer == null) {
            Tools.sendErrorPage(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing \"referer\" header");
            return;
        }

        location = assumeRelative(referer, location);
        try {
            resp.sendRedirect(AJAXUtility.encodeUrl(location, true, true));
        } catch (IllegalArgumentException e) {
            // Apparently location is invalid
            Tools.sendErrorPage(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid \"location\" parameter");
        }
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

}
