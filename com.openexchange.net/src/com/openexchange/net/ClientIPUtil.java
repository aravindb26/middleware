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
package com.openexchange.net;

import javax.servlet.http.HttpServletRequest;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.servlet.Headers;

/**
 * {@link ClientIPUtil} is a utility service which helps to determine the IP address of a client.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
@SingletonService
public interface ClientIPUtil {

    /**
     * Determines the client IP address for a given request
     *
     * @param request The request
     * @return The client IP address
     */
    String getIP(HttpServletRequest request);

    /**
     * Determines the client IP address from a given remote IP address and header list
     *
     * @param remoteIp The remote IP address
     * @param headers The headers
     * @return The client IP address
     */
    String getIP(String remoteIp, Headers headers);

}
