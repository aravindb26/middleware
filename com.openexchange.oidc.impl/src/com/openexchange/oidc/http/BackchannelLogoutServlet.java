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

package com.openexchange.oidc.http;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.oidc.OIDCExceptionHandler;
import com.openexchange.oidc.OIDCWebSSOProvider;

/**
 * {@link BackchannelLogoutServlet}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.4
 */
public class BackchannelLogoutServlet extends OIDCServlet {

    private static final Logger LOG = LoggerFactory.getLogger(BackchannelLogoutServlet.class);
    private static final long serialVersionUID = 6178674443883447429L;

    /**
     * Initializes a new {@link BackchannelLogoutServlet}.
     * 
     * @param backend The underlying OIDC SSO provider
     * @param exceptionHandler The exception handler to use
     */
    public BackchannelLogoutServlet(OIDCWebSSOProvider provider, OIDCExceptionHandler exceptionHandler) {
        super(provider, exceptionHandler);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.trace("doPost(HttpServletRequest request: {}, HttpServletResponse response)", request.getRequestURI());
        try {
            provider.backchannelLogout(request, response);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (OXException e) {
            LOG.warn("Unexepcted error performing backchannel logout", e);
            exceptionHandler.handleLogoutFailed(request, response, e);
        }
    }

}
