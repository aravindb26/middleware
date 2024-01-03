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
package com.openexchange.ajax.oidc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.scheduling.RESTUtilities;
import com.openexchange.java.Charsets;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.restclient.invoker.ApiClient;
import com.openexchange.testing.restclient.invoker.ApiException;
import com.openexchange.testing.restclient.invoker.ApiResponse;
import com.openexchange.testing.restclient.models.AnalysisResult;
import com.openexchange.testing.restclient.models.AnalysisResultHeaders;
import com.openexchange.testing.restclient.models.Header;
import com.openexchange.testing.restclient.models.RequestData;
import com.openexchange.testing.restclient.modules.RequestAnalysisApi;
import okhttp3.Response;

/**
 * {@link OIDCRequestAnalyzerTest} tests the oidc request analyzer
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class OIDCRequestAnalyzerTest extends AbstractOIDCTest {

    private RequestAnalysisApi reqApi;
    private String hostname;

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        // Create rest client
        ApiClient restClient = RESTUtilities.createRESTClient(testContext.getUsedBy());
        restClient.setVerifyingSsl(false);
        restClient.setBasePath(getBasePath());
        TestUser restUser = TestContextPool.getRestUser();
        restClient.setUsername(restUser.getUser());
        restClient.setPassword(restUser.getPassword());
        String authorizationHeaderValue = "Basic " + Base64.encodeBase64String((restUser.getUser() + ":" + restUser.getPassword()).getBytes(Charsets.UTF_8));
        restClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        reqApi = new RequestAnalysisApi(restClient);
    }

    @Test
    public void testOIDCAnalysis() throws Exception {
        // init login flow
        Response loginPage = initLoginFlow(client);
        String rpRedirectURIAuth = keycloakLogin(client, testUser.getLogin(), testUser.getPassword(), loginPage);
        Response resp = followRedirect(client, rpRedirectURIAuth);
        String url = getLocation(resp);
        assertTrue(url.contains("oidcLogin"), "Missing oidcLogin action");
        assertTrue(url.contains("sessionToken"), "Missing sessionToken parameter");

        // Prepare request data with url
        RequestData data = new RequestData();
        data.setMethod("GET");
        ArrayList<Header> headerList = new ArrayList<>();
        Header header = new Header();
        header.setName("headername");
        header.setValue("headerValue");
        headerList.add(header);
        data.setHeaders(headerList);
        data.setUrl(url);
        data.setRemoteIP("127.0.0.1");

        try {
            ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Checks that the response is successful and contains the correct data for user 1
     *
     * @param response The response to check
     */
    private void checkSuccessfulResponse(ApiResponse<AnalysisResult> response) {
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getData());
        AnalysisResultHeaders headers = response.getData().getHeaders();
        assertNotNull(headers);
        assertEquals(testUser.getUserId(), headers.getxOxUserId());
        assertEquals(testUser.getContextId(), headers.getxOxContextId());
    }

    /**
     * Gets the base path for the rest client
     *
     * @return The base path
     */
    protected String getBasePath() {
        if (hostname == null) {
            this.hostname = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
        }

        return this.protocol + "://" + this.hostname + (protocol.equals("https") ? ":443" : ":80");
    }

}
