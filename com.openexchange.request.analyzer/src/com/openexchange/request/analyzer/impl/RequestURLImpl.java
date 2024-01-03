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
package com.openexchange.request.analyzer.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.request.analyzer.RequestAnalyzerExceptionCodes;
import com.openexchange.request.analyzer.RequestURL;

/**
 * {@link RequestURL}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class RequestURLImpl implements RequestURL {

    private final URI url;
    private URIBuilder uriBuilder; // Guarded by synchronized

    /**
     * Initializes a new {@link RequestURLImpl}.
     *
     * @param url The URL string
     * @throws OXException In case of an invalid URL
     */
    public RequestURLImpl(String url) throws OXException {
        super();
        try {
            URI uri = new URI(url);
            if (Strings.isEmpty(uri.getScheme()) || !isValidProtocol(uri.getScheme())) {
                throw new MalformedURLException("Missing or invalid protocol: " + url);
            }
            if (!uri.isAbsolute()) {
                throw new MalformedURLException("URL is not absolute: " + url);
            }
            this.url = uri;
        } catch (URISyntaxException | MalformedURLException e) {
            throw RequestAnalyzerExceptionCodes.INVALID_URL.create(e);
        }
    }

    @Override
    public URI getURL() {
        return url;
    }

    @Override
    public boolean hasParameter(String parameter) throws OXException {
        if (parameter == null) {
            return false;
        }
        for (NameValuePair param : getUriBuilder().getQueryParams()) {
            if (param.getName().equals(parameter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<String> optParameter(String parameter) throws OXException {
        if (parameter == null) {
            return Optional.empty();
        }
        for (NameValuePair param : getUriBuilder().getQueryParams()) {
            if (param.getName().equals(parameter)) {
                return Optional.ofNullable(param.getValue());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getPath() throws OXException {
        return Optional.ofNullable(getUriBuilder().getPath());
    }

    @Override
    public String toString() {
        return "RequestURLImpl [url=" + url + "]";
    }

    // --------------- private methods ------------

    private synchronized URIBuilder getUriBuilder() {
        URIBuilder uriBuilder = this.uriBuilder;
        if (uriBuilder == null) {
            uriBuilder = new URIBuilder(url, StandardCharsets.UTF_8);
            this.uriBuilder = uriBuilder;
        }
        return uriBuilder;
    }

    // ---------------- helper methods---------------

    private static boolean isValidProtocol(String protocol) {
        int len = protocol.length();
        if (len < 1) {
            return false;
        }
        char c = protocol.charAt(0);
        if (!Character.isLetter(c)) {
            return false;
        }
        for (int i = 1; i < len; i++) {
            c = protocol.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '+' &&
                c != '-') {
                return false;
            }
        }
        return true;
    }
}
