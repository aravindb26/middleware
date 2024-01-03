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

package com.openexchange.ajax.oauth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractTestEnvironment;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.java.Lists;
import com.openexchange.java.util.UUIDs;
import com.openexchange.oauth.provider.impl.OAuthProviderConstants;
import com.openexchange.session.restricted.Scope;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * {@link EndpointTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public abstract class EndpointTest extends AbstractTestEnvironment {

    private static final String PREFIX = AJAXConfig.getProperty(AJAXConfig.Property.BASE_PATH);

    public static final String AUTHORIZATION_ENDPOINT = PREFIX + OAuthProviderConstants.AUTHORIZATION_SERVLET_ALIAS;

    public static final String TOKEN_ENDPOINT = PREFIX + OAuthProviderConstants.ACCESS_TOKEN_SERVLET_ALIAS;

    public static final String REVOKE_ENDPOINT = PREFIX + OAuthProviderConstants.REVOKE_SERVLET_ALIAS;

    protected CloseableHttpClient client;

    protected Client oauthClient;

    protected String csrfState;

    protected TestUser testUser;

    protected final static String SCHEME = AJAXConfig.getProperty(AJAXConfig.Property.OAUTH_PROTOCOL);

    protected final static String HOSTNAME = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);

    protected final static int PORT = SCHEME.equals("https") ? 443 : 80;

    protected TestContext testContext;

    protected AJAXClient noReplyClient;

    protected TestUser noReplyUser;

    @BeforeEach
    public void before(TestInfo testInfo) throws Exception {
        testContext = TestContextPool.acquireContext(this.getClass().getCanonicalName() + "." + testInfo.getTestMethod().get().getName());
        testUser = testContext.acquireUser();
        //noReplyUser = testContext.getNoReplyUser();
        //noReplyClient = new AJAXClient(noReplyUser);
        //noReplyClient.execute(new ClearMailsRequest());
        // prepare http client
        // prepare new httpClient
        SSLContext sslcontext = new SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build();
        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build();

        int minute = 1 * 60 * 1000;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(minute).setConnectionRequestTimeout(minute).setSocketTimeout(minute).build();

        Header header = new BasicHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, testContext.getUsedBy());
        List<Header> headers = Lists.toList(header);
        client = HttpClients.custom().disableRedirectHandling().setDefaultHeaders(headers).setDefaultRequestConfig(config).setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry)).build();

        // register client application
        oauthClient = ConfigAwareProvisioningService.getService().registerOAuthClient("Test App " + UUID.randomUUID().toString(), testContext.getUsedBy());
        csrfState = UUIDs.getUnformattedStringFromRandom();
    }

    @AfterEach
    public final void after() throws Exception {
        if (client != null && client.getConnectionManager() != null) {
            client.close();
        }
        if (oauthClient != null) {
            ConfigAwareProvisioningService.getService().unregisterOAuthClient(oauthClient.getId(), testContext.getUsedBy());
        }
        TestContextPool.deleteContext(testContext);
    }

    protected void expectSecureRedirect(HttpUriRequest request, HttpResponse response) {
        assertEquals(HttpStatus.SC_MOVED_PERMANENTLY, response.getStatusLine().getStatusCode());
        Header location = response.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        assertEquals("https://" + request.getURI().toString().substring(7), location.getValue());
    }

    protected HttpResponse executeAndConsume(HttpRequestBase request) throws ClientProtocolException, IOException {
        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                EntityUtils.consumeQuietly(entity);
            }
            return response;
        }
    }

    protected Scope getScope() {
        return Scope.parseScope(oauthClient.getDefaultScope());
    }

    protected String getClientId() {
        return oauthClient.getId();
    }

    protected String getClientSecret() {
        return oauthClient.getSecret();
    }

    protected String getRedirectURI() {
        return oauthClient.getRedirectURIs().get(0);
    }

    protected String getSecondRedirectURI() {
        return oauthClient.getRedirectURIs().get(1);
    }

    protected static void assertNoAccess(OAuthClient client) throws Exception {
        boolean error = false;
        try {
            client.assertAccess();
        } catch (AssertionError e) {
            error = true;
        }

        assertTrue(error, "API access was possible although it should not");
    }

}
