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

package com.openexchange.ajax.framework.config.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.json.JSONObject;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.java.Lists;
import com.openexchange.java.Strings;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestUser;

/**
 *
 * {@link TestConfigurationChangeUtil}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.4
 */
public final class TestConfigurationChangeUtil {

    private TestConfigurationChangeUtil() {}

    /**
     * {@link Scopes} - The scopes that can be set for a configuration change
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * @since v8.4
     */
    public enum Scopes {
        /** Changes will effect only one specific user */
        USER,
        /** Changes will effect the complete context */
        CONTEXT,
        /** The properties are set server-wide */
        SERVER,
        ;
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @param config The new config
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     */
    public static void changeConfigWithOwnClient(TestUser user, Map<String, String> config) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        changeConfigWithOwnClient(user, config, Scopes.USER, null);
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @param config The new config
     * @param scope The scope the change effects
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     */
    public static void changeConfigWithOwnClient(TestUser user, Map<String, String> config, String scope) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        changeConfigWithOwnClient(user, config, getScope(scope), null);
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @param config The new config
     * @param scope The scope the change effects
     * @param reloadables The properties to relaod, separated by a comma
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     */
    public static void changeConfigWithOwnClient(TestUser user, Map<String, String> config, String scope, String reloadables) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        changeConfigWithOwnClient(user, config, getScope(scope), null == reloadables ? Collections.emptyList() : Arrays.asList(reloadables.split(",")));
    }

    private static Scopes getScope(String scope) {
        if (Strings.isNotEmpty(scope)) {
            for (Scopes s : Scopes.values()) {
                if (s.name().equalsIgnoreCase(scope)) {
                    return s;
                }
            }
        }
        return Scopes.USER;
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @param config The new config
     * @param scope The scope the change effects
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     */
    public static void changeConfigWithOwnClient(TestUser user, Map<String, String> config, Scopes scope) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        changeConfigWithOwnClient(user, config, scope, null);
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @param config The new config
     * @param scope The scope the change effects
     * @param reloadables The properties to relaod, separated by a comma
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     */
    public static void changeConfigWithOwnClient(TestUser user, Map<String, String> config, Scopes scope, List<String> reloadables) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        changeConfigWithOwnClient(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME), user, config, scope, reloadables);
    }

    /**
     * Changes the configuration of the given user
     *
     * @param host The host to change the configuration on
     * @param user The user
     * @param config The new config
     * @param scope The scope the change effects
     * @param reloadables The properties to relaod, separated by a comma
     * @throws ClientProtocolException In case the config can't be changes
     * @throws IOException In case the config can't be changes
     * @throws URISyntaxException In case the config can't be changes
     * @throws KeyStoreException In case the config can't be changes
     * @throws NoSuchAlgorithmException In case the config can't be changes
     * @throws KeyManagementException In case the config can't be changes
     */
    public static void changeConfigWithOwnClient(String host, TestUser user, Map<String, String> config, Scopes scope, List<String> reloadables) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        assertTrue(user.getContextId() > 0);
        assertTrue(user.getUserId() > 0);
        assertThat("No configuration to change", config, is(not(nullValue())));
        assertFalse("No configuration to change", config.isEmpty());
        String scheme = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
        boolean useSSL = scheme.equals("https");
        Header header = new BasicHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, user.getCreatedBy());
        List<Header> headers = Lists.toList(header);
        HttpClient httpclient;
        if (useSSL) {
            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(SSLContexts.custom()
                                                                                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                                                                        .build(),
                                                                             NoopHostnameVerifier.INSTANCE);
            httpclient = HttpClients.custom()
                                    .setDefaultHeaders(headers)
                                    .setSSLSocketFactory(scsf)
                                    .build();
        } else {
            httpclient = HttpClients.custom()
                                    .setDefaultHeaders(headers)
                                    .build();
        }

        URI uri = new URIBuilder().setScheme(scheme)
                                  .setHost(host)
                                  .setPath("ajax/changeConfigForTest")
                                  .setPort(useSSL ? 443 : Integer.parseInt(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT)))
                                  .addParameter("userId", String.valueOf(user.getUserId()))
                                  .addParameter("contextId", String.valueOf(user.getContextId()))
                                  .addParameter("scope", null == scope ? Scopes.USER.name().toLowerCase() : scope.name().toLowerCase())
                                  .addParameter("reload", null == reloadables ? "" : reloadables.stream().collect(Collectors.joining(",")))
                                  .build();

        HttpPut httpPut = new HttpPut(uri);
        StringEntity entity = new StringEntity(new JSONObject(config).toString(), ContentType.APPLICATION_JSON);
        httpPut.setEntity(entity);

        HttpResponse response = httpclient.execute(httpPut);
        assertEquals(200, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
    }

}
