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

package com.openexchange.ajax.share.actions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.Header;

/**
 * {@link ResolveShareRequest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ResolveShareRequest implements AJAXRequest<ResolveShareResponse> {

    private final String url;
    private final boolean failOnNonRedirect;
    private final String client;

    /**
     * Initializes a new {@link ResolveShareRequest}.
     *
     * @param url The share url to resolve
     * @param failOnNonRedirect <code>true</code> to fail if request is not redirected, <code>false</code>, otherwise
     */
    public ResolveShareRequest(String url, boolean failOnNonRedirect) {
        this(url, failOnNonRedirect, null);
    }

    /**
     * Initializes a new {@link ResolveShareRequest}.
     *
     * @param url The share url to resolve
     * @param failOnNonRedirect <code>true</code> to fail if request is not redirected, <code>false</code>, otherwise
     * @param client The client identifier to send, or null to omit it
     */
    public ResolveShareRequest(String url, boolean failOnNonRedirect, String client) {
        super();
        this.url = url;
        this.failOnNonRedirect = failOnNonRedirect;
        this.client = client;
    }

    @Override
    public ResolveShareParser getParser() {
        return new ResolveShareParser(failOnNonRedirect);
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return Method.GET;
    }

    @Override
    public String getServletPath() {
        try {
            return new URL(url).getPath();
        } catch (MalformedURLException e) {
            Assertions.fail("Malformed share URL: " + url);
            return null;
        }
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() throws IOException, JSONException {
        return client != null ? new Parameter[] { new Parameter("client", client) } : new Parameter[0];
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        return null;
    }

    @Override
    public Header[] getHeaders() {
        return new Header[0];
    }

}
