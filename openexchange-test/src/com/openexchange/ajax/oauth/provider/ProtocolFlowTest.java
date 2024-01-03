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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.oauth.provider.actions.RevokeRequest;
import com.openexchange.ajax.oauth.provider.protocol.GETRequest;
import com.openexchange.ajax.oauth.provider.protocol.GETResponse;
import com.openexchange.ajax.oauth.provider.protocol.HttpTools;
import com.openexchange.ajax.oauth.provider.protocol.OAuthParams;
import com.openexchange.ajax.oauth.provider.protocol.POSTRequest;
import com.openexchange.ajax.oauth.provider.protocol.POSTResponse;
import com.openexchange.ajax.oauth.provider.protocol.Protocol;
import com.openexchange.oauth.provider.authorizationserver.client.ClientManagement;
import com.openexchange.oauth.provider.impl.grant.OAuthGrantStorage;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;

/**
 * {@link ProtocolFlowTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ProtocolFlowTest extends EndpointTest {

    @Test
    public void testFlow() throws Exception {
        GETRequest getLoginForm = new GETRequest().setScheme(SCHEME).setHostname(HOSTNAME).setPort(PORT).setClientId(getClientId()).setRedirectURI(getRedirectURI()).setState(csrfState);
        GETResponse loginFormResponse = getLoginForm.execute(client);
        POSTRequest loginRequest = loginFormResponse.preparePOSTRequest().setLogin(testUser.getLogin()).setPassword(testUser.getPassword());
        POSTResponse loginResponse = loginRequest.submit(client);
        GETResponse getAuthForm = loginResponse.followRedirect(client);
        POSTRequest authRequest = getAuthForm.preparePOSTRequest();
        POSTResponse authResponse = authRequest.submit(client);
        Map<String, String> redirectParams = HttpTools.extractQueryParams(authResponse.getRedirectLocation());
        assertNull(redirectParams.get("error"));
        assertNotNull(redirectParams.get("code"));
        assertEquals(csrfState, redirectParams.get("state"));
    }

    @Test
    public void testRedeemRefreshToken() throws Exception {
        OAuthClient oauthClient = new OAuthClient(testUser, getClientId(), getClientSecret(), getRedirectURI(), getScope());
        oauthClient.assertAccess();
        OAuthSession session = (OAuthSession) oauthClient.getSession();
        String accessToken = session.getAccessToken();
        String refreshToken = session.getRefreshToken();

        LinkedList<NameValuePair> redeemRefreshTokenParams = new LinkedList<>();
        redeemRefreshTokenParams.add(new BasicNameValuePair("client_id", getClientId()));
        redeemRefreshTokenParams.add(new BasicNameValuePair("client_secret", getClientSecret()));
        redeemRefreshTokenParams.add(new BasicNameValuePair("grant_type", "refresh_token"));
        redeemRefreshTokenParams.add(new BasicNameValuePair("redirect_uri", getRedirectURI()));
        redeemRefreshTokenParams.add(new BasicNameValuePair("refresh_token", refreshToken));

        HttpPost redeemRefreshToken = new HttpPost(new URIBuilder().setScheme(SCHEME).setHost(HOSTNAME).setPath(EndpointTest.TOKEN_ENDPOINT).build());
        redeemRefreshToken.setEntity(new UrlEncodedFormEntity(redeemRefreshTokenParams));

        HttpResponse accessTokenResponse = client.execute(redeemRefreshToken);
        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusLine().getStatusCode(), "Unexpected error: " + accessTokenResponse.getStatusLine().getReasonPhrase());
        JSONObject jAccessTokenResponse = JSONObject.parse(new InputStreamReader(accessTokenResponse.getEntity().getContent(), accessTokenResponse.getEntity().getContentEncoding() == null ? "UTF-8" : accessTokenResponse.getEntity().getContentEncoding().getValue())).toObject();
        assertTrue("bearer".equalsIgnoreCase(jAccessTokenResponse.getString("token_type")));
        assertNotNull(jAccessTokenResponse.get("access_token"));
        assertNotNull(jAccessTokenResponse.get("refresh_token"));
        assertNotNull(jAccessTokenResponse.get("scope"));
        assertNotNull(jAccessTokenResponse.get("expires_in"));

        // Expect both tokens to be different from the old ones
        assertFalse(jAccessTokenResponse.getString("access_token").equals(accessToken));
        assertFalse(jAccessTokenResponse.getString("refresh_token").equals(refreshToken));

        // access with old token must not work anymore
        boolean error = false;
        try {
            oauthClient.assertAccess();
        } catch (AssertionError e) {
            error = true;
        }
        assertTrue(error);
    }

    @Test
    public void testRedeemIsDeniedWhenRedirectURIChanges() throws Exception {
        OAuthParams params = new OAuthParams()
            .setScheme(SCHEME)
            .setHostname(HOSTNAME)
            .setPort(PORT)
            .setClientId(getClientId())
            .setClientSecret(getClientSecret())
            .setRedirectURI(getRedirectURI())
            .setScope(getScope().toString());
        String sessionId = Protocol.login(client, params, testUser.getLogin(), testUser.getPassword());
        String authCode = Protocol.authorize(client, params, sessionId);

        LinkedList<NameValuePair> redeemAuthCodeParams = new LinkedList<>();
        redeemAuthCodeParams.add(new BasicNameValuePair("client_id", getClientId()));
        redeemAuthCodeParams.add(new BasicNameValuePair("client_secret", getClientSecret()));
        redeemAuthCodeParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        redeemAuthCodeParams.add(new BasicNameValuePair("redirect_uri", getSecondRedirectURI())); // <- wrong redirect URI
        redeemAuthCodeParams.add(new BasicNameValuePair("code", authCode));

        HttpPost redeemAuthCode = new HttpPost(new URIBuilder().setScheme(SCHEME).setHost(params.getHostname()).setPath(EndpointTest.TOKEN_ENDPOINT).build());
        redeemAuthCode.setEntity(new UrlEncodedFormEntity(redeemAuthCodeParams));
        HttpResponse accessTokenResponse = client.execute(redeemAuthCode);
        assertEquals(HttpStatus.SC_BAD_REQUEST, accessTokenResponse.getStatusLine().getStatusCode());
        JSONObject jAccessTokenResponse = JSONObject.parse(new InputStreamReader(accessTokenResponse.getEntity().getContent(), accessTokenResponse.getEntity().getContentEncoding() == null ? "UTF-8" : accessTokenResponse.getEntity().getContentEncoding().getValue())).toObject();
        assertEquals("invalid_request", jAccessTokenResponse.get("error"));

        // Test replay with correct URI (auth code must have been invalidated anyway for security reasons)
        redeemAuthCodeParams.clear();
        redeemAuthCodeParams.add(new BasicNameValuePair("client_id", getClientId()));
        redeemAuthCodeParams.add(new BasicNameValuePair("client_secret", getClientSecret()));
        redeemAuthCodeParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        redeemAuthCodeParams.add(new BasicNameValuePair("redirect_uri", getRedirectURI())); // <- correct redirect URI
        redeemAuthCodeParams.add(new BasicNameValuePair("code", authCode));

        redeemAuthCode = new HttpPost(new URIBuilder().setScheme(SCHEME).setHost(params.getHostname()).setPath(EndpointTest.TOKEN_ENDPOINT).build());
        redeemAuthCode.setEntity(new UrlEncodedFormEntity(redeemAuthCodeParams));
        accessTokenResponse = client.execute(redeemAuthCode);
        assertEquals(HttpStatus.SC_BAD_REQUEST, accessTokenResponse.getStatusLine().getStatusCode());
        jAccessTokenResponse = JSONObject.parse(new InputStreamReader(accessTokenResponse.getEntity().getContent(), accessTokenResponse.getEntity().getContentEncoding() == null ? "UTF-8" : accessTokenResponse.getEntity().getContentEncoding().getValue())).toObject();
        assertEquals("invalid_request", jAccessTokenResponse.get("error"));
    }

    @Test
    public void testAuthCodeReplay() throws Exception {
        OAuthParams params = new OAuthParams()
            .setScheme(SCHEME)
            .setHostname(HOSTNAME)
            .setPort(PORT)
            .setClientId(getClientId())
            .setClientSecret(getClientSecret())
            .setRedirectURI(getRedirectURI())
            .setScope(getScope().toString());
        String sessionId = Protocol.login(client, params, testUser.getLogin(), testUser.getPassword());
        String authCode = Protocol.authorize(client, params, sessionId);
        Protocol.redeemAuthCode(client, params, authCode);

        /*
         * Try to obtain another token with the same auth code
         */
        LinkedList<NameValuePair> redeemAuthCodeParams = new LinkedList<>();
        redeemAuthCodeParams.add(new BasicNameValuePair("client_id", getClientId()));
        redeemAuthCodeParams.add(new BasicNameValuePair("client_secret", getClientSecret()));
        redeemAuthCodeParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        redeemAuthCodeParams.add(new BasicNameValuePair("redirect_uri", getRedirectURI()));
        redeemAuthCodeParams.add(new BasicNameValuePair("code", authCode));

        HttpPost redeemAuthCode = new HttpPost(new URIBuilder().setScheme(SCHEME).setHost(params.getHostname()).setPath(EndpointTest.TOKEN_ENDPOINT).build());
        redeemAuthCode.setEntity(new UrlEncodedFormEntity(redeemAuthCodeParams));
        HttpResponse replayResponse = client.execute(redeemAuthCode);
        assertEquals(HttpStatus.SC_BAD_REQUEST, replayResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testMaxNumberOfDistinctGrants() throws Exception {
        // A user must have at max. OAuthProviderService.MAX_CLIENTS_PER_USER grants for different clients
        List<Client> clients = new ArrayList<>(ClientManagement.MAX_CLIENTS_PER_USER);
        for (int i = 0; i < ClientManagement.MAX_CLIENTS_PER_USER; i++) {
            clients.add(ConfigAwareProvisioningService.getService().registerOAuthClient("testMaxNumberOfDistinctGrants " + i + " " + System.currentTimeMillis(), testContext.getUsedBy()));
        }

        try {
            // acquire one token per client
            for (Client client : clients) {
                OAuthClient c = new OAuthClient(testUser, client.getId(), client.getSecret(), client.getRedirectURIs().get(0), getScope());
                c.assertAccess();
            }

            // now the max + 1 try with the default client
            boolean error = false;
            try {
                OAuthClient c = new OAuthClient(testUser, getClientId(), getClientSecret(), getRedirectURI(), getScope());
                c.assertAccess();
            } catch (AssertionError e) {
                error = true;
            }
            assertTrue(error);

            // revoke access for one client and assure we can now grant access to another one
            Iterator<Client> it = clients.iterator();
            Client client2 = it.next();
            AJAXClient ajaxClient = testUser.getAjaxClient();
            ajaxClient.execute(new RevokeRequest(client2.getId()));

            OAuthClient c = new OAuthClient(testUser, getClientId(), getClientSecret(), getRedirectURI(), getScope());
            c.assertAccess();
        } finally {
            for (Client client : clients) {
                try {
                    ConfigAwareProvisioningService.getService().unregisterOAuthClient(client.getId(), testUser.getCreatedBy());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testGrantStorageQuota() throws Exception {
        // there must be at max. OAuthGrantStorage.MAX_GRANTS_PER_CLIENT grants per user per client. On further auth requests old grants are deleted (LRU).
        List<OAuthClient> clients = new ArrayList<>();
        for (int i = 0; i < OAuthGrantStorage.MAX_GRANTS_PER_CLIENT; i++) {
            // obtains a fresh access token
            clients.add(new OAuthClient(testUser, getClientId(), getClientSecret(), getRedirectURI(), getScope()));
            LockSupport.parkNanos(1000000);  // last modified is used to delete old entries and has milliseconds granularity
        }

        for (OAuthClient client : clients) {
            client.assertAccess();
        }

        // max + 1 should replace the oldest one
        clients.add(new OAuthClient(testUser, getClientId(), getClientSecret(), getRedirectURI(), getScope()));
        boolean error = false;
        try {
            clients.get(0).assertAccess();
        } catch (AssertionError e) {
            error = true;
        }

        assertTrue(error);
        clients.remove(0);

        for (OAuthClient client : clients) {
            client.assertAccess();
        }
    }

}
