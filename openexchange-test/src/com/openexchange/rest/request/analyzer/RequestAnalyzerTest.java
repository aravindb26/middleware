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
package com.openexchange.rest.request.analyzer;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Application;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.ajax.framework.session.util.TestSessionReservationUtil;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.java.util.HttpStatusCode;
import com.openexchange.request.analyzer.rest.RequestAnalyzerServlet;
import com.openexchange.rest.AbstractRestTest;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.AcquireTokenResponse;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.models.ShareLinkResponse;
import com.openexchange.testing.httpclient.models.ShareTargetData;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.ShareManagementApi;
import com.openexchange.testing.httpclient.modules.TokenApi;
import com.openexchange.testing.restclient.invoker.ApiException;
import com.openexchange.testing.restclient.invoker.ApiResponse;
import com.openexchange.testing.restclient.models.AnalysisResult;
import com.openexchange.testing.restclient.models.AnalysisResultHeaders;
import com.openexchange.testing.restclient.models.Header;
import com.openexchange.testing.restclient.models.RequestData;
import com.openexchange.testing.restclient.modules.RequestAnalysisApi;
import okhttp3.Cookie;
import okhttp3.HttpUrl;


/**
 * {@link RequestAnalyzerTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 */
public class RequestAnalyzerTest extends AbstractRestTest {

    String protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
    String host = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
    String port = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT);
    String basePath = AJAXConfig.getProperty(AJAXConfig.Property.BASE_PATH);

    private RequestAnalysisApi reqApi;

    @BeforeEach
    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        reqApi = new RequestAnalysisApi(getRestClient());
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(RequestAnalyzerServlet.class);
    }

    /**
     * Tests that a session based request is properly mapped
     *
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testSessionRequest() throws OXException, IOException, JSONException {
        final String urlFormat = "%s://%s:%s%slogin?action=login&session=%s";
        final RequestData data = new RequestData();
        data.setMethod("GET");
        final List<Header> headerList = HeaderBuilder.builder()
                                               .add("someheader", "someValue")
                                               .build();
        data.setHeaders(headerList);
        data.setUrl(urlFormat.formatted(protocol, host, port, basePath, "nope"));
        data.setRemoteIP("127.0.0.1");
        try {
            reqApi.analyzeRequest(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(404, e.getCode());
        }

        data.setUrl(urlFormat.formatted(protocol, host, port, basePath, getAjaxClient().getSession().getId()));
        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (final ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Tests that a dav based request is properly mapped
     *
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testDavRequest() throws OXException, IOException, JSONException {
        // Try invalid data first
        final RequestData data = new RequestData();
        data.setMethod("GET");
        data.setUrl("http://localhost/servlet/dav");
        List<Header> headerList = HeaderBuilder.builder()
                                               .add("Authorization", "basic YW50b25AMTpzZWNyZXQxMg==")
                                               .build();
        data.setHeaders(headerList);
        data.setRemoteIP("127.0.0.1");
        try {
            reqApi.analyzeRequest(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(404, e.getCode());
        }

        // Try correct data
        final String base64Credentials = Base64.getEncoder()
                                         .encodeToString("%s:%s".formatted(testUser.getLogin(),
                                                                           testUser.getPassword())
                                                                .getBytes());
        headerList = HeaderBuilder.builder()
                                  .add("Authorization", "basic %s".formatted(base64Credentials))
                                  .build();
        data.setHeaders(headerList);
        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (final ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Tests that a token login based request is properly mapped
     */
    @Test
    public void testTokenLoginRequest() throws Exception {
        /*
         * acquire a valid token from a logged in client
         */
        AcquireTokenResponse acquireTokenResponse = new TokenApi(testUser.getApiClient()).acquireTokenBuilder().execute();
        assertNull(acquireTokenResponse.getError());
        assertNotNull(acquireTokenResponse.getData());
        String token = acquireTokenResponse.getData().getToken();
        assertNotNull(token);
        /*
         * build request URI and corresponding request data to analyze, using the previously retrieved token
         */
        String body = "token=" + token + "&client=myclient&secret=1234";
        RequestData requestData = prepareRequestData(
            "POST",
            "login",
            "redeemToken",
            Base64.getEncoder().encodeToString(body.getBytes(Charsets.UTF_8)),
            new Header[] { new Header().name(HttpHeaders.CONTENT_TYPE).value("application/x-www-form-urlencoded") },
            (NameValuePair[]) null)
        ;
        /*
         * attempt to analyze it & verify response
         */
        checkSuccessfulResponse(assertAnalyzeResponse(reqApi, requestData, HttpStatusCode.OK));
    }

    @Test
    public void testTokenLoginRequestInvalidToken() throws Exception {
        /*
         * build request URI and corresponding request data to analyze, with an invalid/unknown token value specified
         */
        String body = "token=gibtsesnicht&client=myclient&secret=1234";
        RequestData requestData = prepareRequestData(
            "POST",
            "login",
            "redeemToken",
            Base64.getEncoder().encodeToString(body.getBytes(Charsets.UTF_8)),
            new Header[] { new Header().name(HttpHeaders.CONTENT_TYPE).value("application/x-www-form-urlencoded") },
            (NameValuePair[]) null)
        ;
        /*
         * attempt to analyze it, expecting HTTP 404
         */
        assertAnalyzeResponse(reqApi, requestData, HttpStatusCode.NOT_FOUND);
    }

    @Test
    public void testTokenLoginRequestGarbledBody() throws Exception {
        /*
         * build request URI and corresponding request data to analyze, with an invalid/unknown token value specified
         */
        String body = "asclkjvclksdvkjnevwekncv";
        RequestData requestData = prepareRequestData(
            "POST",
            "login",
            "redeemToken",
            Base64.getEncoder().encodeToString(body.getBytes(Charsets.UTF_8)),
            new Header[] { new Header().name(HttpHeaders.CONTENT_TYPE).value("text/plain") },
            (NameValuePair[]) null)
        ;
        /*
         * attempt to analyze it, expecting HTTP 404
         */
        assertAnalyzeResponse(reqApi, requestData, HttpStatusCode.NOT_FOUND);
    }

    @Test
    public void testTokenLoginRequestNoBody() throws Exception {
        /*
         * build request URI and corresponding request data to analyze, w/o body data
         */
        RequestData requestData = prepareRequestData(
            "POST",
            "login",
            "redeemToken",
            null,
            new Header[] { new Header().name(HttpHeaders.CONTENT_TYPE).value("application/x-www-form-urlencoded") },
            (NameValuePair[]) null)
        ;
        /*
         * attempt to analyze it, expecting HTTP 422
         */
        assertAnalyzeResponse(reqApi, requestData, HttpStatusCode.UNPROCESSABLE_ENTITY);
    }

    @Test
    public void testRegularLoginRequestNoBody() {
        RequestData data = new RequestData();
        data.setHeaders(HeaderBuilder.builder().add("someheader", "someValue").build());
        data.setRemoteIP("127.0.0.1");
        data.setUrl("%s://%s:%s%slogin?action=login".formatted(protocol, host, port, basePath));
        data.setMethod("POST");

        try {
            reqApi.analyzeRequest(data);
            fail("Expected ApiException with code 422, but no exception was thrown");
        } catch (ApiException e) {
            assertEquals(422, e.getCode());
        }
    }

    @Test
    public void testRegularLoginRequestInvalidCredentials() {
        RequestData data = new RequestData();
        data.setHeaders(HeaderBuilder.builder().add("someheader", "someValue").build());
        data.setRemoteIP("127.0.0.1");
        data.setUrl("%s://%s:%s%slogin?action=login".formatted(protocol, host, port, basePath));
        data.setMethod("POST");
        String encodedBody = Base64.getEncoder().encodeToString(("action=login&name=nouser&password=nosecret&locale=de_DE&client=open-xchange-appsuite&version=main&timeout=10000&rampup=false&staySignedIn=true").getBytes());
        data.setBody(encodedBody);

        try {
            reqApi.analyzeRequest(data);
            fail("Expected ApiException with code 404, but no exception was thrown");
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testRegularLoginRequestValidCredentials() throws OXException, IOException, JSONException  {
        RequestData data = new RequestData();
        data.setHeaders(HeaderBuilder.builder().add("someheader", "someValue").build());
        data.setRemoteIP("127.0.0.1");
        data.setUrl("%s://%s:%s%slogin?action=login".formatted(protocol, host, port, basePath));
        data.setMethod("POST");
        String encodedBody = Base64.getEncoder().encodeToString(("action=login&name=%s&password=secret&locale=de_DE&client=open-xchange-appsuite&version=main&timeout=10000&rampup=false&staySignedIn=true".formatted(testUser.getLogin())).getBytes());
        data.setBody(encodedBody);

        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    @Test
    public void testFormLoginRequestNoBody() {
        RequestData data = new RequestData();
        data.setHeaders(HeaderBuilder.builder().add("someheader", "someValue").build());
        data.setRemoteIP("127.0.0.1");
        data.setUrl("%s://%s:%s%slogin?action=formlogin&authId=123456789".formatted(protocol, host, port, basePath));
        data.setMethod("POST");

        try {
            reqApi.analyzeRequest(data);
            fail("Expected ApiException with code 422, but no exception was thrown");
        } catch (ApiException e) {
            assertEquals(422, e.getCode());
        }
    }

    @Test
    public void testFormLoginRequestInvalidCredentials() {
        RequestData data = new RequestData();
        data.setHeaders(HeaderBuilder.builder().add("someheader", "someValue").build());
        data.setRemoteIP("127.0.0.1");
        data.setUrl("%s://%s:%s%slogin?action=formlogin&authId=123456789".formatted(protocol, host, port, basePath));
        data.setMethod("POST");
        String encodedBody = Base64.getEncoder().encodeToString(("login=falselogin&password=falsepassword&client=open-xchange-appsuite&version=8.0.0&autologin=true").getBytes());
        data.setBody(encodedBody);

        try {
            reqApi.analyzeRequest(data);
            fail("Expected ApiException with code 404, but no exception was thrown");
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testFormLoginRequestValidCredentials() throws JSONException, OXException, IOException {
        RequestData data = new RequestData();
        data.setHeaders(HeaderBuilder.builder().add("someheader", "someValue").build());
        data.setRemoteIP("127.0.0.1");
        data.setUrl("%s://%s:%s%slogin?action=formlogin&authId=123456789".formatted(protocol, host, port, basePath));
        data.setMethod("POST");
        String encodedBody = Base64.getEncoder().encodeToString(("login=%s&password=%s&client=open-xchange-appsuite&version=8.0.0&autologin=true".formatted(testUser.getLogin(), testUser.getPassword())).getBytes());
        data.setBody(encodedBody);

        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Tests that an autologin request is properly mapped
     *
     *
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     * @throws com.openexchange.testing.httpclient.invoker.ApiException
     */
    @Test
    public void testAutoLoginRequest() throws OXException, JSONException, IOException, com.openexchange.testing.httpclient.invoker.ApiException {
        final RequestData data = new RequestData();

        String url = "%s://%s:%s%slogin?action=login".formatted(protocol, host, port, basePath);

        final List<Cookie> cookies = testUser.getApiClient()
                                             .getHttpClient()
                                             .cookieJar()
                                             .loadForRequest(HttpUrl.get(url));

        final String cookieHeader = cookies.stream()
                                           .map(cookie -> "%s=%s".formatted(cookie.name(), cookie.value()))
                                           .collect(Collectors.joining(";"));

        url = "%s://%s:%s%slogin?action=autologin".formatted(protocol, host, port, basePath);

        final List<Header> headerList = HeaderBuilder.builder()
                                                     .add("Cookie", cookieHeader)
                                                     .add("User-Agent", testUser.getApiClient().getUserAgent())
                                                     .build();

        data.setHeaders(headerList);
        data.setRemoteIP("127.0.0.1");
        data.setUrl(url);
        data.setMethod("GET");

        try {
            ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Tests that a public session cookie based request is properly mapped
     *
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     * @throws com.openexchange.testing.httpclient.invoker.ApiException
     */
    @Test
    public void testPublicSessionCookieRequest() throws OXException, IOException, JSONException, com.openexchange.testing.httpclient.invoker.ApiException {
        final RequestData data = new RequestData();
        data.setMethod("GET");

        String url = "%s://%s:%s%scontacts/picture?action=get".formatted(protocol, host, port, basePath);
        final List<Cookie> cookies = testUser.getApiClient().getHttpClient().cookieJar().loadForRequest(HttpUrl.get(url));
        final String cookieHeader = cookies.stream()
                                     .map(cookie -> "%s=%s".formatted(cookie.name(), cookie.value()))
                                     .collect(Collectors.joining(";"));

        // Apply cookies
        final List<Header> headerList = HeaderBuilder.builder()
                                               .add("Cookie", cookieHeader)
                                               .add("User-Agent", testUser.getApiClient().getUserAgent())
                                               .build();
        data.setHeaders(headerList);
        data.setUrl(url);
        data.setRemoteIP("127.0.0.1");

        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            checkSuccessfulResponse(response);
        } catch (final ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }

        // Check wrong url
        url = "%s://%s:%s%scontacts/?action=get".formatted(protocol, host, port, basePath);
        data.setUrl(url);
        try {
            reqApi.analyzeRequest(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    /**
     * Tests that a sharing link based request is properly mapped
     *
     * @throws Exception
     */
    @Test
    public void testSharingLinkRequest() throws Exception {

        // Create folder
        final FoldersApi foldersApi = new FoldersApi(testUser.getApiClient());
        final ShareManagementApi shareApi = new ShareManagementApi(testUser.getApiClient());

        final int parent = getAjaxClient().getValues().getPrivateInfostoreFolder();
        final FolderData folder = insertFolder(foldersApi, EnumAPI.OX_NEW, String.valueOf(parent));

        // Share folder
        final ShareTargetData shareTarget = new ShareTargetData();
        shareTarget.folder(folder.getFolderId());
        shareTarget.setModule(String.valueOf(FolderObject.INFOSTORE));
        final ShareLinkResponse resp = shareApi.getShareLinkBuilder()
                                         .withShareTargetData(shareTarget)
                                         .execute();

        // Analyze request with share url
        assertNull(resp.getError());
        assertNotNull(resp.getData());
        final String url = resp.getData().getUrl();

        final RequestData data = new RequestData();
        data.setMethod("GET");
        final List<Header> headerList = HeaderBuilder.builder()
                                               .add("someheader", "someValue")
                                               .build();
        data.setHeaders(headerList);
        data.setUrl(url);
        data.setRemoteIP("127.0.0.1");
        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);

            // Check response
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getData());
            final AnalysisResultHeaders headers = response.getData().getHeaders();
            assertNotNull(headers);
            final UserValues userValues = getAjaxClient().getValues();
            assertEquals(userValues.getContextId(), headers.getxOxContextId());
            assertNotNull(headers.getxOxUserId());
            assertNotEquals(userValues.getUserId(), headers.getxOxUserId());
        } catch (final ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Tests that a saml login request is properly mapped
     *
     * @throws Exception
     */
    @Test
    public void testSAMLRequest() throws Exception {
        // Reserve a session
        String token = TestSessionReservationUtil.reserveSession(host, testUser);
        String url = "%s://%s:%s%slogin?action=samlLogin&token=%s".formatted(protocol, host, port, basePath, token);

        RequestData data = new RequestData();
        data.setMethod("GET");
        List<Header> headerList = HeaderBuilder.builder()
                                               .add("someheader", "someValue")
                                               .build();
        data.setHeaders(headerList);
        data.setUrl(url);
        data.setRemoteIP("127.0.0.1");
        try {
            ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);

            // Check response
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getData());
            AnalysisResultHeaders headers = response.getData().getHeaders();
            assertNotNull(headers);
            UserValues userValues = getAjaxClient().getValues();
            assertEquals(userValues.getContextId(), headers.getxOxContextId());
            assertEquals(userValues.getUserId(), headers.getxOxUserId());
        } catch (ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Tests that a free busy request is properly mapped
     *
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testFreeBusyRequest() {
        final RequestData data = new RequestData();

        final String url = "%s://%s:%s/servlet/webdav.freebusy?contextId=%d&userName=%s&server=%s".formatted(protocol, host, port, Integer.valueOf(testUser.getContextId()), testUser.getUser(), testUser.getContext());
        final ArrayList<Header> headerList = new ArrayList<>();
        final Header header = new Header();

        header.setName("headername");
        header.setValue("headerValue");
        headerList.add(header);

        data.setHeaders(headerList);
        data.setRemoteIP("127.0.0.1");
        data.setUrl(url);
        data.setMethod("POST");

        try {
            final ApiResponse<AnalysisResult> response = reqApi.analyzeRequestWithHttpInfo(data);
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getData());
            final String marker = response.getData().getMarker();
            assertNotNull(marker);
        } catch (final ApiException e) {
            fail("Unable to get marker: %s".formatted(e.getMessage()));
        }

        //check a string as cid
        data.setUrl("%s://%s:%s/servlet/webdav.freebusy?contextId=noRealContextID&userName=%s&server=%s".formatted(protocol, host, port, testUser.getUser(), testUser.getContext()));
        testInvalidRequest(data, 404);

        //check a non existing context as cid
        data.setUrl("%s://%s:%s/servlet/webdav.freebusy?contextId=2147483647&userName=%s&server=%s".formatted(protocol, host, port, testUser.getUser(), testUser.getContext()));
        testInvalidRequest(data, 404);
    }

    /**
     * Tests that the servlet responds with a 400er error in case at least one required field is missing.
     */
    @Test
    public void testMissingFields() {
        final RequestData data = new RequestData();
        try {
            reqApi.analyzeRequestWithHttpInfo(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(400, e.getCode());
        }

        data.setUrl("someurl");
        try {
            reqApi.analyzeRequestWithHttpInfo(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(400, e.getCode());
        }

        data.setMethod("method");
        try {
            reqApi.analyzeRequestWithHttpInfo(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(400, e.getCode());
        }

        data.setRemoteIP("127.0.0.1");
        try {
            reqApi.analyzeRequestWithHttpInfo(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    // ----------------- private methods ---------------------

    private static final Integer BITS_ADMIN = Integer.valueOf(403710016);

    /**
     * Inserts a new folder
     *
     * @param foldersApi The {@link FoldersApi} to use
     * @param api The folder api to use
     * @param parent The parent folder
     * @return The {@link FolderData} of the new folder
     * @throws com.openexchange.testing.httpclient.invoker.ApiException
     */
    private FolderData insertFolder(FoldersApi foldersApi, EnumAPI api, String parent) throws com.openexchange.testing.httpclient.invoker.ApiException {
        final NewFolderBody body = new NewFolderBody();
        final NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setTitle(UUID.randomUUID().toString());
        folder.setFolderId(parent);
        folder.setModule(Module.INFOSTORE.getName());
        folder.setType(I(FolderObject.PRIVATE));
        folder.setPermissions(createPermissions(testUser));
        body.setFolder(folder);

        final String tree = String.valueOf(api.getTreeId());

        final FolderUpdateResponse resp = foldersApi.createFolderBuilder()
                                              .withTree(tree)
                                              .withFolderId(parent)
                                              .withNewFolderBody(body)
                                              .execute();
        assertNull(resp.getError(), resp.getErrorDesc());
        assertNotNull(resp.getData());
        final FolderResponse folderResponse = foldersApi.getFolderBuilder()
                                                  .withId(resp.getData())
                                                  .withTree(tree)
                                                  .execute();
        assertNull(folderResponse.getError(), folderResponse.getErrorDesc());
        assertNotNull(folderResponse.getData());
        return folderResponse.getData();
    }

    /**
     * Creates a list of permissions for a new folder
     *
     * @param user The user which creates the folder
     * @return The list of permissions
     */
    private List<FolderPermission> createPermissions(TestUser user) {
        final List<FolderPermission> result = new ArrayList<>();
        final FolderPermission adminPerm = createPermissionFor(I(user.getUserId()), BITS_ADMIN, Boolean.FALSE);
        result.add(adminPerm);
        return result;
    }

    /**
     * Creates a {@link FolderPermission} for a given entity
     *
     * @param entity The entity id
     * @param bits The permission bits
     * @param isGroup Whether the entity is a group or not
     * @return The {@link FolderPermission}
     */
    private FolderPermission createPermissionFor(Integer entity, Integer bits, Boolean isGroup) {
        final FolderPermission p = new FolderPermission();
        p.setEntity(entity);
        p.setGroup(isGroup);
        p.setBits(bits);
        return p;
    }

    /**
     * Checks that the response is successful and contains the correct data for user 1
     *
     * @param response The response to check
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    private void checkSuccessfulResponse(ApiResponse<AnalysisResult> response) throws OXException, IOException, JSONException {
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getData());
        final AnalysisResultHeaders headers = response.getData().getHeaders();
        assertNotNull(headers);
        final UserValues userValues = getAjaxClient().getValues();
        assertEquals(userValues.getUserId(), headers.getxOxUserId());
        assertEquals(userValues.getContextId(), headers.getxOxContextId());
    }

    /**
     *
     * Checks that an invalid request returns the right exception code
     *
     * @param data The request data
     * @param exceptionCode The expected exception code
     */
    private void testInvalidRequest(RequestData data, int exceptionCode) {
        try {
            reqApi.analyzeRequest(data);
            fail();
        } catch (final ApiException e) {
            assertEquals(exceptionCode, e.getCode());
        }
    }

    /**
     * Performs an <code>analyze</code> request to the given {@link RequestAnalysisApi} and checks the HTTP status code of the received response.
     * <p/>
     * For {@link HttpStatusCode#OK} responses, the analysis result is returned, otherwise an {@link ApiException} is expected whose
     * status code must match the expected one.
     *
     * @param analysisApi The request analysis API to use
     * @param requestData The request data to hand over to the analyzer
     * @param expectedStatusCode The expected status code
     * @return The analysis result for {@link HttpStatusCode#OK} responses, <code>null</code>, otherwise
     */
    private static ApiResponse<AnalysisResult> assertAnalyzeResponse(RequestAnalysisApi analysisApi, RequestData requestData, HttpStatusCode expectedStatusCode) {
        try {
            ApiResponse<AnalysisResult> analysisResult = analysisApi.analyzeRequestWithHttpInfo(requestData);
            if (HttpStatusCode.OK.equals(expectedStatusCode) && HttpStatusCode.OK.getCode() == analysisResult.getStatusCode()) {
                return analysisResult;
            }
            fail("Expected response with HTTP status " + expectedStatusCode);
        } catch (ApiException e) {
            assertEquals(expectedStatusCode.getCode(), e.getCode());
        }
        return null;

    }

    /**
     * Initializes and prepares a {@link RequestData} object for a typical Ajax request to the HTTP API.
     * <p/>
     * Typical header values (like <code>User-Agent</code>, <code>Accept</code> and <code>Host</code> are included implicitly for a
     * exemplary, browser-based client).
     *
     * @param httpMethod The HTTP Method to indicate, e.g. <code>PUT</code>
     * @param ajaxModule The Ajax module to append as path segment to the request URI
     * @param ajaxAction The Ajax action to append as <code>action</code> query parameter
     * @param body The request body to indicate (as encoded string), or <code>null</code> to omit
     * @param headers Additional headers to set in the request data, or <code>null</code> if not applicable
     * @param uriParameters Additional parameters to include in the request URI, or <code>null</code> if not applicable
     * @return The request data
     */
    private RequestData prepareRequestData(String httpMethod, String ajaxModule, String ajaxAction, String body, Header[] headers, NameValuePair... uriParameters) throws NumberFormatException, URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder()
            .setScheme(protocol)
            .setPort(Integer.parseInt(port))
            .setHost(host)
            .setPathSegments(Strings.trimStart(Strings.trimEnd(basePath, '/'), '/'), Strings.trimStart(Strings.trimEnd(ajaxModule, '/'), '/'))
        ;
        if (null != ajaxAction) {
            uriBuilder.setParameter("action", "redeemToken");
        }
        if (null != uriParameters) {
            uriBuilder.addParameters(Arrays.asList(uriParameters));
        }
        RequestData requestData = new RequestData();
        requestData.setMethod(httpMethod);
        requestData.setRemoteIP("198.51.100.77");
        requestData.setUrl(uriBuilder.build().toASCIIString());
        requestData.setBody(body);
        requestData.addHeadersItem(new Header().name(HttpHeaders.USER_AGENT).value("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"));
        requestData.addHeadersItem(new Header().name(HttpHeaders.ACCEPT).value("application/json, text/javascript, */*; q=0.01"));
        requestData.addHeadersItem(new Header().name(HttpHeaders.HOST).value(host));
        if (null != headers && 0 < headers.length) {
            for (Header header : headers) {
                requestData.addHeadersItem(header);
            }
        }
        return requestData;
    }

}
