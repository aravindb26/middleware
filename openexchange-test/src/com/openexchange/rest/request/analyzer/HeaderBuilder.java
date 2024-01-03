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
package com.openexchange.rest.request.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.testing.restclient.models.Header;

/**
 * {@link HeaderBuilder} is a util class which helps to build a list of headers
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class HeaderBuilder {

    private final List<Header> headerList = new ArrayList<>();

    /**
     * Creates a new builder
     *
     * @return A new builder
     */
    public static HeaderBuilder builder() {
        return new HeaderBuilder();
    }

    /**
     * Adds a new header to the header list
     *
     * @param name
     * @param value
     * @return this builder
     */
    public HeaderBuilder add(String name, String value) {
        Header header = new Header();
        header.setName(name);
        header.setValue(value);
        headerList.add(header);
        return this;
    }

    /**
     * Creates a unmodifiable header list
     *
     * @return The list
     */
    public List<Header> build() {
        return Collections.unmodifiableList(headerList);
    }

}
