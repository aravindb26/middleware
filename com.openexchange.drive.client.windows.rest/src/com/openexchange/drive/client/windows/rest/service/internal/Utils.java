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

package com.openexchange.drive.client.windows.rest.service.internal;

import static com.openexchange.tools.servlet.http.Tools.createHostData;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.drive.client.windows.rest.osgi.Services;
import com.openexchange.exception.OXException;
import com.openexchange.framework.request.DefaultRequestContext;
import com.openexchange.framework.request.RequestContext;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link Utils}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 */
public class Utils {

    public static boolean hasCapability(Session session, List<String> necessaryCap) throws OXException {
        CapabilityService capabilityService = Services.getServiceSafe(CapabilityService.class);
        for (String cap : necessaryCap ) {
            if ( !capabilityService.getCapabilities(session, true).contains(cap) ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Check if given string is a valid URL by using {@link URL}
     *
     * @param url The string to check
     * @return <code>true</code> if given string is a valid URL otherwise <code>false</code>
     */
    @SuppressWarnings("unused")
    public static boolean isUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException mue) {
            return false;
        }
    }

    /**
     * Builds a request context object for the given underlying request.
     * 
     * @param session The server session
     * @param request The underlying request
     * @return The request context
     */
    public static RequestContext buildRequestContext(ServerSession session, HttpServletRequest request) {
        DefaultRequestContext requestContext = new DefaultRequestContext();
        requestContext.setSession(session);
        requestContext.setUserAgent(request.getHeader("User-Agent"));
        requestContext.setHostData(createHostData(request, session.getContextId(), session.getUserId(), session.getUser().isGuest()));
        return requestContext;
    }

}
