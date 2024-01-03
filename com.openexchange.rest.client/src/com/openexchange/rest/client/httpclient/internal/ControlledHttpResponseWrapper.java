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

package com.openexchange.rest.client.httpclient.internal;

import java.util.Locale;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;

/**
 * A wrapper that returns a controlled HTTP entity on {@link #getEntity()} invocation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ControlledHttpResponseWrapper implements HttpResponse {

    private final HttpResponse httpResponse;
    private final int timeout;

    /**
     * Initializes a new {@link ControlledHttpResponseWrapper}.
     *
     * @param httpResponse The HTTP response to wrap
     * @param timeout The timeout
     */
    public ControlledHttpResponseWrapper(HttpResponse httpResponse, int timeout) {
        super();
        this.httpResponse = httpResponse;
        this.timeout = timeout;
    }

    /**
     * Gets the wrapped HTTP response instance.
     *
     * @return The wrapped HTTP response instance
     */
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    @Override
    public StatusLine getStatusLine() {
        return httpResponse.getStatusLine();
    }

    @Override
    public void setStatusLine(StatusLine statusline) {
        httpResponse.setStatusLine(statusline);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return httpResponse.getProtocolVersion();
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code) {
        httpResponse.setStatusLine(ver, code);
    }

    @Override
    public boolean containsHeader(String name) {
        return httpResponse.containsHeader(name);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code, String reason) {
        httpResponse.setStatusLine(ver, code, reason);
    }

    @Override
    public Header[] getHeaders(String name) {
        return httpResponse.getHeaders(name);
    }

    @Override
    public void setStatusCode(int code) throws IllegalStateException {
        httpResponse.setStatusCode(code);
    }

    @Override
    public Header getFirstHeader(String name) {
        return httpResponse.getFirstHeader(name);
    }

    @Override
    public void setReasonPhrase(String reason) throws IllegalStateException {
        httpResponse.setReasonPhrase(reason);
    }

    @Override
    public Header getLastHeader(String name) {
        return httpResponse.getLastHeader(name);
    }

    @Override
    public HttpEntity getEntity() {
        return new ControlledHttpEntityWrapper(httpResponse.getEntity(), timeout);
    }

    @Override
    public void setEntity(HttpEntity entity) {
        httpResponse.setEntity(entity);
    }

    @Override
    public Header[] getAllHeaders() {
        return httpResponse.getAllHeaders();
    }

    @Override
    public void addHeader(Header header) {
        httpResponse.addHeader(header);
    }

    @Override
    public void addHeader(String name, String value) {
        httpResponse.addHeader(name, value);
    }

    @Override
    public Locale getLocale() {
        return httpResponse.getLocale();
    }

    @Override
    public void setHeader(Header header) {
        httpResponse.setHeader(header);
    }

    @Override
    public void setLocale(Locale loc) {
        httpResponse.setLocale(loc);
    }

    @Override
    public void setHeader(String name, String value) {
        httpResponse.setHeader(name, value);
    }

    @Override
    public void setHeaders(Header[] headers) {
        httpResponse.setHeaders(headers);
    }

    @Override
    public void removeHeader(Header header) {
        httpResponse.removeHeader(header);
    }

    @Override
    public void removeHeaders(String name) {
        httpResponse.removeHeaders(name);
    }

    @Override
    public HeaderIterator headerIterator() {
        return httpResponse.headerIterator();
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return httpResponse.headerIterator(name);
    }

    @Override
    public HttpParams getParams() {
        return httpResponse.getParams();
    }

    @Override
    public void setParams(HttpParams params) {
        httpResponse.setParams(params);
    }

}
