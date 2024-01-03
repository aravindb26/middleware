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

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link RampUpDeprecated} - Decoy of the ramp-up action which throws an exception
 * to inform the clients
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class RampUpDeprecated implements LoginRequestHandler {

    /**
     * Initializes a new {@link RampUpDeprecated}.
     */
    public RampUpDeprecated() {
        super();
    }

    @Override
    public void handleRequest(HttpServletRequest req, HttpServletResponse resp, LoginRequestContext requestContext) throws IOException {
        LoginServlet.logAndSendException(resp, AjaxExceptionCodes.DEPRECATED_ACION.create(AJAXServlet.ACTION_RAMPUP, LoginServlet.SERVLET_PATH_APPENDIX));
    }
}
