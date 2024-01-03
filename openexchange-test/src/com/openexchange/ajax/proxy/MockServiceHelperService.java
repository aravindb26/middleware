/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.ajax.proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.java.Strings;
import com.openexchange.test.common.configuration.AJAXConfig;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ConnectionPool;

/**
 * {@link MockServiceHelperService}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class MockServiceHelperService {

    private static final MockServiceHelperService INSTANCE = new MockServiceHelperService();
    private final OkHttpClient client;

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static MockServiceHelperService getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link MockServiceHelperService}.
     */
    private MockServiceHelperService() {
        super();
        client = new OkHttpClient.Builder().connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS)).build();
    }

    /**
     * Mocks the given request
     *
     * @param method The request method to mock
     * @param path The path of the request to mock
     * @param responseContent The content of the mock response
     * @param httpStatus the status of the mock response
     * @param contentType The response content type
     * @param responseHeaders The headers of the mock response
     * @param requestParameter The request parameter
     * @param delay The delay of the mock response
     */
    public void mock(MockRequestMethod method, String path, String responseContent, int httpStatus, String contentType, Map<String, String> responseHeaders, Map<String, String> requestParameter, int delay) {
        JSONObject bodyJSON = new JSONObject();
        try {
            if (responseContent != null) {
                bodyJSON.put("mockBody", responseContent);
            }
            if (responseHeaders != null && responseHeaders.isEmpty() == false) {
                bodyJSON.put("headers", responseHeaders);
            }
            if (requestParameter != null && requestParameter.isEmpty() == false) {
                bodyJSON.put("params", requestParameter);
            }
            // @formatter:off
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = bodyJSON.isEmpty() ? null : RequestBody.create(mediaType, bodyJSON.toString());
            String portStr = AJAXConfig.getProperty(AJAXConfig.Property.MOCK_PORT);
            HttpUrl.Builder builder = new HttpUrl.Builder()
                                     .scheme(AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL))
                                     .host(AJAXConfig.getProperty(AJAXConfig.Property.MOCK_HOSTNAME))
                                     .port(Integer.valueOf(portStr).intValue())
                                     .addPathSegments("mock/configure")
                                     .addPathSegments(path.startsWith("/") ? path.substring(1) : path)
                                     .addQueryParameter("status", String.valueOf(httpStatus))
                                     .addQueryParameter("method", method.name())
                                     .addQueryParameter("delay", String.valueOf(delay));

            if(Strings.isNotEmpty(contentType)) {
                builder.addQueryParameter("contentType", String.valueOf(contentType));
            }

            assert body != null;
            Request mockRequest = new Request.Builder()
                                             .put(body)
                                             .url(builder.build())
                                             .build();

            try (Response resp = client.newCall(mockRequest).execute()) {
                if (resp.isSuccessful() == false) {
                    fail("Unable to mock request: " + resp.body().string());
                }
                assertTrue(resp.body().string() != null);
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            fail("Unable to mock request: " + e.getMessage());
        }
    }

}
