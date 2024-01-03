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

import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.request.analyzer.impl.RequestURLImpl;
import com.openexchange.servlet.Header;
import com.openexchange.servlet.Headers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link RequestData} contains all data needed to analyze the request. E.g. headers, body etc.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class RequestData {

    private final String method;
    private final String url;
    private final Headers headers;
    private final Optional<BodyData> body;
    private final String clientIp;
    private RequestURL parsedUrl;

    /**
     * Initializes a new {@link RequestData}.
     *
     * @param method The request method
     * @param url The request URL
     * @param headers The request headers
     * @param clientIp The client IP address
     * @param body The optional request body
     */
    private RequestData(String method, String url, Headers headers, String clientIp, BodyData body) {
        super();
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.clientIp = clientIp;
        this.body = Optional.ofNullable(body);
    }

    /**
     * Gets the method.
     *
     * @return The method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the URL.
     *
     * @return The URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the headers.
     *
     * @return The headers
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * Gets the optional body.
     *
     * @return The optional body
     */
    public Optional<BodyData> optBody() {
        return body;
    }

    /**
     * Gets the client IP address.
     *
     * @return The client IP address
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Gets the URL as a parsed {@link RequestURL}
     *
     * @return The {@link RequestURL}
     * @throws OXException In case the URL is malformed
     */
    public synchronized RequestURL getParsedURL() throws OXException {
        RequestURL parsedRequestUrl = this.parsedUrl;
        if (parsedRequestUrl == null) {
            parsedRequestUrl = new RequestURLImpl(url);
            this.parsedUrl = parsedRequestUrl;
        }
        return parsedRequestUrl;
    }

    // -------------------------------------- Builder stuff -----------------------------------------

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an instance of {@link RequestData}.
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     */
    public static class Builder {

        private String method = null;
        private String url = null;
        private List<Header> headerList = null;
        private Headers headers = null;
        private BodyData body = null;
        private String clientIp;

        /**
         * Initializes a new {@link RequestData.Builder}.
         */
        private Builder() {
            super();
        }

        /**
         * Adds the method to this builder
         *
         * @param method The method
         * @return This builder
         */
        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        /**
         * Adds the URL to this builder
         *
         * @param url The URL
         * @return This builder
         */
        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Adds a headers object to this builder
         *
         * @param headers The headers
         * @return This builder
         */
        public Builder withHeaders(Headers headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Adds headers to this builder
         *
         * @param headers The headers
         * @return This builder
         */
        public Builder withHeaders(List<Header> headers) {
            if (headers != null && !headers.isEmpty()) {
                if (this.headerList == null) {
                    this.headerList = new ArrayList<>(headers);
                } else {
                    this.headerList.addAll(headers);
                }
            }
            return this;
        }

        /**
         * Adds a header to this builder
         *
         * @param header The headers
         * @return This builder
         */
        public Builder withHeader(Header header) {
            if (header != null) {
                if (this.headerList == null) {
                    this.headerList = new ArrayList<>();
                }
                this.headerList.add(header);
            }
            return this;
        }

        /**
         * Adds the client ip to this builder
         *
         * @param clientIp The client ip to add
         * @return This builder
         */
        public Builder withClientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        /**
         * Adds the body to this builder
         *
         * @param body The body to add
         * @return This builder
         */
        public Builder withBody(BodyData body) {
            this.body = body;
            return this;
        }

        /**
         * Builds the instance of {@link RequestData} from this builder's arguments.
         *
         * @return The {@link RequestData} instance
         * @throws OXException If a required field is missing
         */
        public RequestData build() throws OXException {
            checkNotEmpty(method, "Missing method");
            checkNotEmpty(url, "Missing url");
            if (headerList == null && headers == null) {
                throw RequestAnalyzerExceptionCodes.INVALID_BODY.create("Missing headers");
            }
            checkNotEmpty(clientIp, "Missing client ip");
            return new RequestData(method, url, headers != null ? headers : new Headers(headerList), clientIp, body);
        }

        /**
         * Ensures that the given string is not null or empty
         *
         * @param toTest The string to test
         * @param msg The msg to add to the exception
         * @throws OXException
         */
        private static void checkNotEmpty(String toTest, String msg) throws OXException {
            if (Strings.isEmpty(toTest)) {
                throw RequestAnalyzerExceptionCodes.INVALID_BODY.create(msg);
            }
        }
    }

}
