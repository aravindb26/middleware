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
* but WITHOUT ANY WARRANTY, without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
*
* Any use of the work other than as authorized under this license or copyright law is prohibited.
*
*/

package com.openexchange.request.analyzer.rest.data;

import java.util.List;
import com.openexchange.java.Strings;
import com.openexchange.servlet.Header;

/**
 * {@link RequestDataPOJO} is a pojo object which contains the request data
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @param method The request method
 * @param url The request url
 * @param headers A list of request headers
 * @param body The request body or null
 * @param The remote ip of the client
 */
public record RequestDataPOJO(String method, String url, List<Header> headers, String body, String remoteIP) {

    /**
     * Whether this request contains a body or not
     *
     * @return <code>true</code> if it contains a body, <code>false</code> otherwise
     */
    public boolean containsBody() {
        return Strings.isNotEmpty(body);
    }

}
