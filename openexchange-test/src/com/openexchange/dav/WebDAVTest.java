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

package com.openexchange.dav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.VisibleFoldersRequest;
import com.openexchange.ajax.folder.actions.VisibleFoldersResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.ajax.oauth.provider.AbstractOAuthTest;
import com.openexchange.ajax.oauth.provider.OAuthSession;
import com.openexchange.ajax.oauth.provider.protocol.Grant;
import com.openexchange.ajax.oauth.provider.protocol.OAuthParams;
import com.openexchange.ajax.oauth.provider.protocol.Protocol;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.dav.reports.SyncCollectionReportInfo;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.ContactTestManager;
import com.openexchange.test.FolderTestManager;
import com.openexchange.test.ResourceTestManager;
import com.openexchange.test.TaskTestManager;
import com.openexchange.test.TestManager;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.configuration.AJAXConfig.Property;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * {@link WebDAVTest} - Common base class for WebDAV tests
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public abstract class WebDAVTest extends AbstractConfigAwareAPIClientSession {

    private static final boolean AUTODISCOVER_AUTH = true;

    protected static final int TIMEOUT = 10000;
    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebDAVTest.class);

    protected WebDAVClient webDAVClient;

    @BeforeAll
    public static void prepareFramework() throws OXException {
        AJAXConfig.init();
    }

    // --- BEGIN: Optional OAuth Configuration ------------------------------------------------------------------------------

    protected static final String AUTH_METHOD_BASIC = "Basic Auth";

    protected static final String AUTH_METHOD_OAUTH = "OAuth";

    protected Client oAuthClientApp;

    protected Grant oAuthGrant;

    private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    }
    };
    private static final SSLContext trustAllSslContext;
    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();

    @SuppressWarnings("unused")
    protected static Iterable<Object[]> availableAuthMethods() {
        if (!AUTODISCOVER_AUTH) {
            List<Object[]> authMethods = new ArrayList<>(2);
            authMethods.add(new Object[] { AUTH_METHOD_OAUTH });
            authMethods.add(new Object[] { AUTH_METHOD_BASIC });

            return authMethods;
        }
        List<Object[]> authMethods = new ArrayList<Object[]>(2);
        try {
            AJAXConfig.init();
            DavPropertyNameSet props = new DavPropertyNameSet();
            props.add(PropertyNames.CURRENT_USER_PRINCIPAL);

            // @formatter:off
            OkHttpClient client = new OkHttpClient.Builder()
                .hostnameVerifier((hostname, session) -> true)
                .sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        Request request = chain.request();
                        Request newRequest = request.newBuilder()
                                .addHeader(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, WebDAVTest.class.getCanonicalName() + ".availableAuthMethods")
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS))
                .build();
            // @formatter:on
            PropFindMethod req = new PropFindMethod(Config.getBaseUri() + Config.getPathPrefix() + "/caldav/", DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            req.getRequestEntity().writeRequest(bos);
            // @formatter:off
            Request propFind = new Request.Builder()
                                          .url(req.getURI().toString())
                                          .method("PROPFIND", RequestBody.create(MediaType.parse("text/xml; charset=UTF-8"), bos.toByteArray()))
                                          .header("User-Agent", UserAgents.IOS_12_0)
                                          .build();
            // @formatter:on
            Response resp = client.newCall(propFind).execute();
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.code(), "Unexpected response status");
            for (String header : resp.headers("WWW-Authenticate")) {
                if (header.startsWith("Bearer")) {
                    authMethods.add(new Object[] { AUTH_METHOD_OAUTH });
                } else if (header.startsWith("Basic")) {
                    authMethods.add(new Object[] { AUTH_METHOD_BASIC });
                }
            }
            if(!resp.isSuccessful()) {
                resp.body().close();
            }
            assertFalse(authMethods.isEmpty(), "No available authentication mode detected");
        } catch (OXException | IOException e) {
            fail(e.getMessage());
        }
        return authMethods;
    }

    public void prepareOAuthClient() throws Exception {
        /*
         * Lazy initialization - static (BeforeClass) is not possible because the testOAuth()
         * depends on the configuration of the concrete subclass (via parameterized testing).
         *
         */
        if (oAuthClientApp == null && oAuthGrant == null) {
            oAuthClientApp = AbstractOAuthTest.registerTestClient(testUser.getCreatedBy());
            try (CloseableHttpClient client = OAuthSession.newOAuthHttpClient(testUser.getCreatedBy())) {
                String state = UUIDs.getUnformattedStringFromRandom();
                OAuthParams params = getOAuthParams(oAuthClientApp, state);
                oAuthGrant = Protocol.obtainAccess(client, params, testUser.getLogin(), testUser.getPassword());
            }
        }
    }

    /**
     * Prepares the underlying OAuth client access if the given authentication method indicates OAuth.
     * <p/>
     * Should be invoked at the very beginning of each test to mimic a lazy initialization of the access.
     *
     * @param authMethod The authentication method
     */
    protected void prepareOAuthClientIfNeeded(String authMethod) throws Exception {
        if ("OAuth".equalsIgnoreCase(authMethod)) {
            prepareOAuthClient();
        }
    }

    @AfterEach
    public void unregisterOAuthClient() {
        if (oAuthClientApp != null) {
            try {
                AbstractOAuthTest.unregisterTestClient(oAuthClientApp);
            } catch (Exception e) {
                e.printStackTrace();
            }
            oAuthClientApp = null;
            oAuthGrant = null;
        }
    }

    // --- END: Optional OAuth Configuration --------------------------------------------------------------------------------

    private final Set<String> folderIdsToDelete = new HashSet<String>();
    private final List<TestManager> testManagers = new ArrayList<TestManager>();

    protected CalendarTestManager catm;
    protected ContactTestManager cotm;
    protected FolderTestManager ftm;
    protected TaskTestManager ttm;
    protected ResourceTestManager resTm;

    protected UserApi testUserApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        super.setUpConfiguration();
        webDAVClient = new WebDAVClient(testUser, getDefaultUserAgent(), oAuthGrant);
        prepareOAuthClient();
        AJAXClient client = getClient();
        client.setHostname(getHostname());
        client.setProtocol(getProtocol());

        catm = new CalendarTestManager(client);
        testManagers.add(catm);
        cotm = new ContactTestManager(client);
        testManagers.add(cotm);
        ftm = new FolderTestManager(client);
        testManagers.add(ftm);
        ttm = new TaskTestManager(client);
        testManagers.add(ttm);
        resTm = new ResourceTestManager(client);
        testManagers.add(resTm);

        testUserApi = new UserApi(getApiClient(), testUser);
    }

    protected UserApi getUserApi() {
        return testUserApi;
    }

    protected boolean rememberFolderIdForCleanup(String folderId) {
        return folderIdsToDelete.add(folderId);
    }

    protected abstract String getDefaultUserAgent();

    /**
     * Gets a folder by its name.
     *
     * @param folderName
     * @return
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    protected FolderObject getFolder(String folderName) throws OXException, IOException, JSONException {
        VisibleFoldersResponse response = getClient().execute(new VisibleFoldersRequest(EnumAPI.OX_NEW, "contacts", new int[] { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME }));
        FolderObject folder = findByName(response.getPrivateFolders(), folderName);
        if (null == folder) {
            folder = findByName(response.getPublicFolders(), folderName);
            if (null == folder) {
                folder = findByName(response.getSharedFolders(), folderName);
            }
        }
        if (null != folder) {
            folder = ftm.getFolderFromServer(folder.getObjectID());
        }
        return folder;
    }

    /**
     * Gets a folder by its name.
     *
     * @param folderName
     * @return
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    protected FolderObject getCalendarFolder(String folderName) throws OXException, IOException, JSONException {
        VisibleFoldersResponse response = getClient().execute(new VisibleFoldersRequest(EnumAPI.OX_NEW, "calendar", new int[] { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME }));
        FolderObject folder = findByName(response.getPrivateFolders(), folderName);
        if (null == folder) {
            folder = findByName(response.getPublicFolders(), folderName);
            if (null == folder) {
                folder = findByName(response.getSharedFolders(), folderName);
            }
        }
        if (null != folder) {
            folder = ftm.getFolderFromServer(folder.getObjectID());
        }
        return folder;
    }

    protected FolderObject getTaskFolder(String folderName) throws OXException, IOException, JSONException {
        VisibleFoldersResponse response = getClient().execute(new VisibleFoldersRequest(EnumAPI.OX_NEW, "tasks", new int[] { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME }));
        FolderObject folder = findByName(response.getPrivateFolders(), folderName);
        if (null == folder) {
            folder = findByName(response.getPublicFolders(), folderName);
            if (null == folder) {
                folder = findByName(response.getSharedFolders(), folderName);
            }
        }
        if (null != folder) {
            folder = ftm.getFolderFromServer(folder.getObjectID());
        }
        return folder;
    }

    private static FolderObject findByName(Iterator<FolderObject> iter, String folderName) {
        while (iter.hasNext()) {
            FolderObject folder = iter.next();
            if (folderName.equals(folder.getFolderName())) {
                return folder;
            }
        }
        return null;
    }

    protected boolean removeFromETags(Map<String, String> eTags, String uid) {
        String href = this.getHrefFromETags(eTags, uid);
        if (null != href) {
            eTags.remove(href);
            return true;
        }
        return false;
    }

    protected String getHrefFromETags(Map<String, String> eTags, String uid) {
        for (String href : eTags.keySet()) {
            if (href.contains(uid)) {
                return href;
            }
        }
        return null;
    }

    protected List<String> getChangedHrefs(Map<String, String> previousETags, Map<String, String> newETags) {
        List<String> hrefs = new ArrayList<String>();
        for (String href : newETags.keySet()) {
            if (false == previousETags.containsKey(href) || false == newETags.get(href).equals(newETags.get(href))) {
                hrefs.add(href);
            }
        }
        return hrefs;
    }

    @SuppressWarnings("unused")
    protected FolderObject updateFolder(FolderObject folder) throws OXException, IOException, JSONException {
        return ftm.updateFolderOnServer(folder);
    }

    protected FolderObject getFolder(int folderID) {
        return ftm.getFolderFromServer(folderID);
    }

    protected void deleteFolder(FolderObject folder) throws OXException, IOException, JSONException {
        ftm.deleteFolderOnServer(folder);
    }

    protected FolderObject createFolder(FolderObject parent, String folderName) {
        FolderObject folder = new FolderObject();
        folder.setFolderName(folderName);
        folder.setParentFolderID(parent.getObjectID());
        folder.setModule(parent.getModule());
        folder.setType(parent.getType());
        folder.setPermissions(parent.getPermissions());
        return ftm.insertFolderOnServer(folder);
    }

    public static String getBaseUri() throws OXException {
        return getProtocol() + "://" + getDavHostname();
    }

    public static String getDavHostname() throws OXException {
        final String hostname = AJAXConfig.getProperty(Property.DAV_HOST);
        if (null == hostname) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(Property.DAV_HOST.getPropertyName());
        }
        return hostname;
    }

    public static String getHostname() throws OXException {
        final String hostname = AJAXConfig.getProperty(Property.SERVER_HOSTNAME);
        if (null == hostname) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(Property.SERVER_HOSTNAME.getPropertyName());
        }
        return hostname;
    }

    public static String getProtocol() throws OXException {
        final String hostname = AJAXConfig.getProperty(Property.PROTOCOL);
        if (null == hostname) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create(Property.PROTOCOL.getPropertyName());
        }
        return hostname;
    }

    protected static void release(HttpMethodBase method) {
        if (null != method) {
            method.releaseConnection();
        }
    }

    protected static String randomUID() {
        return UUID.randomUUID().toString();
    }

    protected AJAXClient getAJAXClient() {
        return getClient();
    }

    protected FolderData createSubfolder(String parentId, String title) throws Exception {
        return createSubfolder(getFolderData(parentId), title);
    }

    protected FolderData getFolderData(String id) throws Exception {
        FolderResponse response = getUserApi().getFoldersApi().getFolder(id, null, null, null, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    protected FolderData createSubfolder(FolderData parentFolder, String title) throws Exception {
        NewFolderBodyFolder newFolder = new NewFolderBodyFolder().module(parentFolder.getModule()).title(title).permissions(null);
        NewFolderBody newFolderBody = new NewFolderBody();
        newFolderBody.setFolder(newFolder);
        FolderUpdateResponse response = getUserApi().getFoldersApi().createFolder(parentFolder.getId(), newFolderBody, null, null, null, null);
        String newId = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        rememberFolderIdForCleanup(newId);
        return getFolderData(newId);
    }

    protected FolderData updateFolder(String id, FolderData folderUpdate) throws Exception {
        FolderBody folderBody = new FolderBody();
        folderBody.setFolder(folderUpdate);
        FolderUpdateResponse response = getUserApi().getFoldersApi().updateFolder(id, folderBody, null, null, null, null, null, null, null, null);
        String newId = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        rememberFolderIdForCleanup(newId);
        return getFolderData(newId);
    }

    protected String fetchSyncToken(String relativeUrl) throws Exception {
        PropFindMethod propFind = null;
        try {
            DavPropertyNameSet props = new DavPropertyNameSet();
            props.add(PropertyNames.SYNC_TOKEN);
            propFind = new PropFindMethod(getBaseUri() + relativeUrl, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
            MultiStatusResponse response = assertSingleResponse(webDAVClient.doPropFind(propFind, StatusCodes.SC_MULTISTATUS));
            return this.extractTextContent(PropertyNames.SYNC_TOKEN, response);
        } finally {
            release(propFind);
        }
    }

    /**
     * Performs a REPORT method at the specified URL with a Depth of 1,
     * requesting the ETag property of all resources that were changed since
     * the supplied sync token.
     *
     * @param syncToken
     * @return
     * @throws IOException
     * @throws ConfigurationException
     * @throws DavException
     */
    protected Map<String, String> syncCollection(String syncToken, String relativeUrl) throws Exception {
        Map<String, String> eTags = new HashMap<String, String>();
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        ReportInfo reportInfo = new SyncCollectionReportInfo(syncToken, props);
        MultiStatusResponse[] responses = webDAVClient.doReport(reportInfo, getBaseUri() + Config.getPathPrefix() + relativeUrl);
        for (final MultiStatusResponse response : responses) {
            if (response.getProperties(StatusCodes.SC_OK).contains(PropertyNames.GETETAG)) {
                String href = response.getHref();
                assertNotNull(href, "got no href from response");
                String eTag = this.extractTextContent(PropertyNames.GETETAG, response);
                assertNotNull(eTag, "got no ETag from response");
                eTags.put(href, eTag);
            }
        }
        return eTags;
    }

    protected SyncCollectionResponse syncCollection(SyncToken syncToken, String relativeUrl) throws Exception {
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        SyncCollectionReportInfo reportInfo = new SyncCollectionReportInfo(syncToken.getToken(), props);
        SyncCollectionResponse syncCollectionResponse = webDAVClient.doReport(reportInfo, getBaseUri() + Config.getPathPrefix() + relativeUrl);
        syncToken.setToken(syncCollectionResponse.getSyncToken());
        return syncCollectionResponse;
    }

    protected String extractHref(DavPropertyName propertyName, MultiStatusResponse response) {
        Node node = this.extractNodeValue(propertyName, response);
        assertMatches(PropertyNames.HREF, node);
        String content = node.getTextContent();
        assertNotNull(content, "no text content in " + PropertyNames.HREF + " child for " + propertyName);
        return content;
    }

    protected Node extractNodeValue(final DavPropertyName propertyName, final MultiStatusResponse response) {
        assertNotEmpty(propertyName, response);
        Object value = response.getProperties(StatusCodes.SC_OK).get(propertyName).getValue();
        if (value instanceof List<?>) {
            List<Node> nodeList = removeWhitspaceNodes((List<Node>) value);
            if (0 < nodeList.size()) {
                value = nodeList.get(0);
            }
        }
        assertTrue(value instanceof Node, "value is not a node in " + propertyName);
        return (Node) value;
    }

    protected List<Node> extractNodeListValue(DavPropertyName propertyName, MultiStatusResponse response) {
        assertNotEmpty(propertyName, response);
        final Object value = response.getProperties(StatusCodes.SC_OK).get(propertyName).getValue();
        assertTrue(value instanceof List<?>, "value is not a node list in " + propertyName);
        return removeWhitspaceNodes((List<Node>) value);
    }

    protected DavProperty<?> extractProperty(DavPropertyName propertyName, MultiStatusResponse response) {
        assertNotEmpty(propertyName, response);
        DavProperty<?> property = response.getProperties(StatusCodes.SC_OK).get(propertyName);
        assertNotNull(property, "property " + propertyName + " not found");
        return property;
    }

    protected String extractTextContent(final DavPropertyName propertyName, final MultiStatusResponse response) {
        assertNotEmpty(propertyName, response);
        final Object value = response.getProperties(StatusCodes.SC_OK).get(propertyName).getValue();
        assertTrue(value instanceof String, "value is not a string in " + propertyName);
        return (String) value;
    }

    public static String extractChildTextContent(DavPropertyName propertyName, Element element) {
        NodeList nodes = element.getElementsByTagNameNS(propertyName.getNamespace().getURI(), propertyName.getName());
        assertNotNull(nodes, "no child elements found by property name");
        assertEquals(1, nodes.getLength(), "0 or more than one child nodes found for property");
        Node node = nodes.item(0);
        assertNotNull(node, "no child element found by property name");
        return node.getTextContent();
    }

    protected static String formatAsUTC(final Date date) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    protected static String formatAsDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    /*
     * Additional assertXXX methods
     */

    public static void assertMatches(final DavPropertyName propertyName, final Node node) {
        assertEquals(propertyName.getName(), node.getLocalName(), "wrong element name");
        assertEquals(propertyName.getNamespace().getURI(), node.getNamespaceURI(), "wrong element namespace");
    }

    public static void assertContains(DavPropertyName propertyName, List<Node> nodeList) {
        for (Node node : nodeList) {
            if (propertyName.getName().equals(node.getLocalName()) && propertyName.getNamespace().getURI().equals(node.getNamespaceURI())) {
                return;
            }
        }
        fail("property " + propertyName + " not found in list");
    }

    public static void assertIsPresent(DavPropertyName propertyName, MultiStatusResponse response) {
        final DavProperty<?> property = response.getProperties(StatusCodes.SC_OK).get(propertyName);
        assertNotNull(property, "property " + propertyName + " not found");
    }

    public static void assertNotEmpty(DavPropertyName propertyName, MultiStatusResponse response) {
        assertIsPresent(propertyName, response);
        final Object value = response.getProperties(StatusCodes.SC_OK).get(propertyName).getValue();
        assertNotNull(value, "no value for " + propertyName);
    }

    public static MultiStatusResponse assertSingleResponse(MultiStatusResponse[] responses) {
        assertNotNull(responses, "got no multistatus responses");
        assertTrue(0 < responses.length, "got zero multistatus responses");
        assertTrue(1 == responses.length, "got more than one multistatus responses");
        final MultiStatusResponse response = responses[0];
        assertNotNull(response, "no multistatus response");
        return response;
    }

    public static void assertResponseHeaders(String[] expected, String headerName, HttpMethod method) {
        for (String expectedHeader : expected) {
            boolean found = false;
            Header[] actualHeaders = method.getResponseHeaders(headerName);
            assertTrue(null != actualHeaders && 0 < actualHeaders.length, "header '" + headerName + "' not found");
            for (Header actualHeader : actualHeaders) {
                HeaderElement[] actualHeaderElements = actualHeader.getElements();
                assertTrue(null != actualHeaderElements && 0 < actualHeaderElements.length, "no elements found in header '" + headerName + "'");
                for (HeaderElement actualHeaderElement : actualHeaderElements) {
                    if (expectedHeader.equals(actualHeaderElement.getName())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            assertTrue(found, "header element '" + expectedHeader + "'not found in header '" + headerName + "'");
        }
    }

    protected static List<Node> removeWhitspaceNodes(List<Node> nodes) {
        if (null == nodes || nodes.isEmpty()) {
            return nodes;
        }
        for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
            Node node = iterator.next();
            if (Node.TEXT_NODE == node.getNodeType() && com.openexchange.java.Strings.isEmpty(node.getTextContent())) {
                iterator.remove();
            }
        }
        return nodes;
    }

    protected static NodeList removeWhitspaceNodes(NodeList nodes) {
        if (null == nodes || 0 == nodes.getLength()) {
            return nodes;
        }
        List<Node> newNodes = new ArrayList<Node>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (Node.TEXT_NODE == node.getNodeType() && com.openexchange.java.Strings.isEmpty(node.getTextContent())) {
                continue;
            }
            newNodes.add(node);
        }

        return new NodeList() {

            @Override
            public Node item(int index) {
                return newNodes.get(index);
            }

            @Override
            public int getLength() {
                return newNodes.size();
            }
        };
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("com.openexchange.hostname", AJAXConfig.getProperty(Property.DAV_HOST));
        return configuration;
    }

}
