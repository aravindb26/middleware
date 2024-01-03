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

package com.openexchange.ajax.framework.session.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.java.Lists;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * {@link TestSessionReservationUtil} is a util class which helps to create session reservations
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public final class TestSessionReservationUtil {

    private TestSessionReservationUtil() {}

    /**
     * Reserves a session for the given user
     *
     * @param host The host to reserve the session on
     * @param user The user to reserver the session for
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     * @throws JSONException
     */
    public static String reserveSession(String host, TestUser user) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, JSONException {
        assertTrue(user.getContextId() > 0);
        assertTrue(user.getUserId() > 0);
        String scheme = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
        boolean useSSL = scheme.equals("https");
        URI uri = new URIBuilder().setScheme(scheme)
                                  .setHost(host)
                                  .setPath("ajax/reserveSessionForTest")
                                  .setPort(useSSL ? 443 : Integer.parseInt(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT)))
                                  .addParameter("userId", String.valueOf(user.getUserId()))
                                  .addParameter("contextId", String.valueOf(user.getContextId()))
                                  .addParameter("timeout", "10000")
                                  .build();

        HttpGet get = new HttpGet(uri);
        HttpResponse response = createClient(user, useSSL).execute(get);

        // Check and parse response
        assertEquals(200, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        HttpEntity entity = response.getEntity();
        assertNotNull(entity);
        String body = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(body);
        String result = json.getString("data");
        return result;
    }

    /**
     * Creates a new http client for the given user
     *
     * @param user The user to create the client for
     * @param useSSL Whether to use ssl encryption or not
     * @return The {@link HttpClient}
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    private static HttpClient createClient(TestUser user, boolean useSSL) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        Header header = new BasicHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, user.getCreatedBy());
        List<Header> headers = Lists.toList(header);
        if (useSSL) {
            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(SSLContexts.custom()
                                                                                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                                                                        .build(),
                                                                             NoopHostnameVerifier.INSTANCE);
            return HttpClients.custom()
                              .setDefaultHeaders(headers)
                              .setSSLSocketFactory(scsf)
                              .build();
        }
        return HttpClients.custom()
                          .setDefaultHeaders(headers)
                          .build();
    }

}
