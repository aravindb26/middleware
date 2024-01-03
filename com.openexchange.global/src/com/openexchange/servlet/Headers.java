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

package com.openexchange.servlet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.openexchange.java.Strings;

/**
 * {@link Headers} - Represents an immutable list of headers from an examined request.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class Headers implements Iterable<Header> {

    private final List<Header> list;

    /**
     * Initializes a new empty headers listing.
     */
    public Headers() {
        super();
        this.list = Collections.emptyList();
    }

    /**
     * Initializes a new headers listing from given list.
     *
     * @param headers The initial headers
     */
    public Headers(List<Header> headers) {
        super();
        if (headers != null && !headers.isEmpty()) {
            this.list = List.copyOf(headers);
        } else {
            this.list = Collections.emptyList();
        }
    }

    /**
     * Initializes a new headers listing from another headers object.
     *
     * @param headers The headers object to copy
     */
    public Headers(Headers headers) {
        super();
        if (headers != null && !headers.isEmpty()) {
            this.list = List.copyOf(headers.list);
        } else {
            this.list = Collections.emptyList();
        }
    }

    /**
     * Checks if this headers listing is empty.
     *
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Iterator<Header> iterator() {
        return list.iterator();
    }

    /**
     * Gets the first encountered header for given name.
     *
     * @param name The header name to look-up by
     * @return The first header or <code>null</code> if there is no such header
     */
    public Header getFirstHeader(String name) {
        if (Strings.isEmpty(name)) {
            return null;
        }
        return list.stream()
                   .filter(h -> name.equalsIgnoreCase(h.name()))
                   .findFirst()
                   .orElse(null);
    }

    /**
     * Gets the headers for given name.
     *
     * @param name The header name to look-up by
     * @return The headers or empty list if there is no such header
     */
    public List<Header> getHeader(String name) {
        if (Strings.isEmpty(name)) {
            return Collections.emptyList();
        }

        return list.stream()
                   .filter(h -> name.equalsIgnoreCase(h.name()))
                   .toList();
    }

    /**
     * Gets the value of the first encountered header for specified header name.
     * <p>
     * Returns <code>null</code> if no headers with the specified name exist.
     *
     * @param name The header name
     * @return The value or <code>null</code>
     */
    public String getFirstHeaderValue(String name) {
        return getHeaderValue(name, null);
    }

    /**
     * Gets all the headers for specified header name, returned as a single String, with headers separated by the delimiter.
     * <p>
     * If the delimiter is <code>null</code>, only the first header is returned.
     * <p>
     * Returns <code>null</code> if no headers with the specified name exist.
     *
     * @param name The header name
     * @param delimiter The delimiter
     * @return The value fields for all headers with given name, or <code>null</code> if none
     */
    public String getHeaderValue(String name, String delimiter) {
        List<Header> s = getHeader(name);

        int size = s.size();
        if (size <= 0) {
            return null;
        }

        if (delimiter == null || size == 1) {
            return s.get(0).value();
        }

        return s.stream()
                .map(h -> h.value())
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Returns a sequential stream of this headers
     *
     * @return The stream
     */
    public Stream<Header> stream() {
        return list.stream();
    }

}
