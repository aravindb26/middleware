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
package com.openexchange.filestore.s3.internal.client;

import org.apache.http.HttpHeaders;
import com.amazonaws.Request;
import com.amazonaws.handlers.RequestHandler2;

/**
 * {@link HeaderCorrectionHandler} is a request handler which corrects invalid headers before sending them to the s3 endpoint.
 *
 * E.g. removing content length header in case it is lower case
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public class HeaderCorrectionHandler extends RequestHandler2 {

    private static final HeaderCorrectionHandler INSTANCE = new HeaderCorrectionHandler();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static HeaderCorrectionHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link HeaderCorrectionHandler}.
     */
    private HeaderCorrectionHandler() {
        super();
    }

    @Override
    public void beforeRequest(Request<?> request) {
        String clHeader = request.getHeaders().remove(HttpHeaders.CONTENT_LENGTH.toLowerCase());
        if (clHeader != null) {
            request.getHeaders().put(HttpHeaders.CONTENT_LENGTH, clHeader);
        }
        request.getHeaders().remove("x-rgw-object-type"); // remove special xion header which causes signature errors
    }

}
