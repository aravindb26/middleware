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
package com.openexchange.request.analyzer;

import java.net.URI;
import java.net.URL;
import java.util.Optional;
import org.apache.http.client.utils.URIBuilder;
import com.openexchange.exception.OXException;

/**
 * {@link RequestURL} wraps a {@link URL} and an {@link URIBuilder} and
 * provides utility methods.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public interface RequestURL {

    /**
     * Gets the request' URL represented by an instance of {@link URI}.
     *
     * @return The URL
     */
    URI getURL();

    /**
     * Whether the URL contains given parameter or not.
     *
     * @param param The name of the parameter to check
     * @return <code>true</code> if the URL contains the given parameter, <code>false</code> otherwise
     * @throws OXException In case of an invalid URL
     */
    boolean hasParameter(String param) throws OXException;

    /**
     * Gets optional value of a given parameter.
     *
     * @param param The parameter to get
     * @return The parameter value
     * @throws OXException In case of an invalid URL
     */
    Optional<String> optParameter(String param) throws OXException;

    /**
     * Gets the optional path of this URL.
     *
     * @return The path
     * @throws OXException In case of an invalid URL
     */
    Optional<String> getPath() throws OXException;

}
