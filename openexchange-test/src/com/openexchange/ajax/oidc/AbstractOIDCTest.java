package com.openexchange.ajax.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AbstractTestEnvironment;
import com.openexchange.java.Strings;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;
import kotlin.Pair;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class AbstractOIDCTest extends AbstractTestEnvironment {

    private final String port = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT);
    protected final String protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);

    private final String oidcHostname = AJAXConfig.getProperty(AJAXConfig.Property.OIDC_HOSTNAME);
    private final String oidcPath = AJAXConfig.getProperty(AJAXConfig.Property.OIDC_PATH);
    protected final String basePath = AJAXConfig.getProperty(AJAXConfig.Property.BASE_PATH);

    private final String oidcBaseUrl = protocol + "://" + oidcHostname + ":" + port + basePath + oidcPath;

    protected TestUser testUser;

    protected OkHttpClient client;
    protected TestContext testContext;

    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        TestClassConfig testConfig = TestClassConfig.builder().build();
        String testName = this.getClass().getCanonicalName() + "." + testInfo.getTestMethod().get().getName();
        List<TestContext> testContextList = TestContextPool.acquireOrCreateContexts(testName, Optional.ofNullable(testConfig), testConfig.getNumberOfContexts());
        testContext = testContextList.get(0);
        testUser = testContext.acquireUser();
        client = new OkHttpClient.Builder().followRedirects(false).build();
    }

    /**
     * Starts the OIDC login flow, verifies the response is a redirect (to keycloak) and follows the redirect.<br>
     *
     * The redirect location is controlled by <code>com.openexchange.oidc.opAuthorizationEndpoint</code>
     *
     * @param client The OK HTTP client
     * @return The redirect response
     * @throws IOException if an unexpected error occurs
     */
    protected Response initLoginFlow(OkHttpClient client) throws IOException {
        Request request = new Request.Builder().url(oidcBaseUrl + "/init?flow=login&redirect=true").build();
        Response response = client.newCall(request).execute();
        response.close();
        String location = getLocation(response);
        return followRedirect(client, location);
    }

    /**
     * Login at keycloak and verifies the response is a redirect with an auth code to the middleware.<br>
     *
     * The redirect location is controlled by <code>com.openexchange.oidc.rpRedirectURIAuth</code>
     *
     * @param client The OK HTTP client
     * @param user The user
     * @param user The password
     * @param response The response containing the login page and cookies
     * @return The redirect location (com.openexchange.oidc.rpRedirectURIAuth)
     * @throws IOException if an unexpected error occurs
     */
    protected String keycloakLogin(OkHttpClient client, String user, String password, Response response) throws IOException, AssertionError {
        String responseBody = response.body().string();
        String postURL = Jsoup.parse(responseBody).getElementById("kc-form-login").attr("action");
        RequestBody postData = new FormBody.Builder().add("username", user).add("password", password).build();

        String keycloakCookie = getCookie(response);
        Request request = new Request.Builder().url(postURL).post(postData).header("Cookie", keycloakCookie).build();
        Response rsp = client.newCall(request).execute();
        rsp.close();

        String location = getLocation(rsp);
        assertTrue(location.contains("state"), "Missing auth code");
        return location;
    }

    /**
     * Exchanges auth code for tokens and verify the response is a redirect with session
     *
     * @param client The OK HTTP client
     * @param rpRedirectURIAuth The authentication endpoint of the middleware
     * @return The OIDCSession
     * @throws IOException if an unexpected error occurs
     */
    protected OIDCSession middlewareLogin(OkHttpClient client, String rpRedirectURIAuth) throws IOException {
        // exchange auth code
        Response response = followRedirect(client, rpRedirectURIAuth);
        String location = getLocation(response);
        assertTrue(location.contains("oidcLogin"), "Missing oidcLogin action");
        assertTrue(location.contains("sessionToken"), "Missing sessionToken parameter");
        response = followRedirect(client, location);
        response.close();

        // verify response
        location = getLocation(response);
        String session = StringUtils.substringAfter(location, "/#session=");
        assertNotNull("Missing session parameter", session);
        String cookie = getCookie(response);
        assertFalse(Strings.isEmpty(cookie));
        return new OIDCSession(session, cookie);
    }

    /**
     * Starts the OIDC logout flow and verifies the response is a redirect (to keycloak).<br>
     *
     * The redirect location is controlled by <code>com.openexchange.oidc.opLogoutEndpoint</code>
     *
     * @param client The OK HTTP client
     * @param session The user session
     * @param cookie The user session cookie
     * @return The redirect location (com.openexchange.oidc.opLogoutEndpoint)
     * @throws IOException if an unexpected error occurs
     */
    protected String initLogoutFlow(OkHttpClient client, String session, String cookie) throws IOException {
        Request request = new Request.Builder().url(oidcBaseUrl + "/init?flow=logout&redirect=true&session=" + session).header("Cookie", cookie).build();
        Response response = client.newCall(request).execute();
        response.close();
        return getLocation(response);
    }

    /**
     * Performs the keycloak logout, verifies the response is a redirect (to the middleware) and follows the redirect.<br>
     *
     * The redirect location is controlled by <code>com.openexchange.oidc.rpRedirectURIPostSSOLogout</code>
     *
     * @param client The OK HTTP client
     * @param opLogoutEndpoint The logout endpoint of the middleware
     * @return The redirect response
     * @throws IOException if an unexpected error occurs
     */
    protected String keycloakLogout(OkHttpClient client, String opLogoutEndpoint) throws IOException {
        Response response = followRedirect(client, opLogoutEndpoint);
        response.close();
        String location = getLocation(response);
        Request request = new Request.Builder().url(location).build();
        response = client.newCall(request).execute();
        response.close();
        location = getLocation(response);
        assertTrue(location.contains("action=oidcLogout"), "Missing oidcLogout action");
        assertTrue(location.contains("session"), "Missing session parameter");
        return location;
    }

    /**
     * Logout at the middleware to terminate the session and verify the response is a redirect.<br>
     *
     * The redirect location is controlled by <code>com.openexchange.oidc.rpRedirectURILogout</code>
     *
     * @param client The OK HTTP client
     * @param rpRedirectURIPostSSOLogout The logout endpoint of the middleware
     * @param cookie The session cookie
     * @return The redirect response
     * @throws IOException if an unexpected error occurs
     */
    protected void middlewareLogout(OkHttpClient client, String rpRedirectURIPostSSOLogout, String cookie) throws IOException {
        // follow the redirect
        Request request = new Request.Builder().url(rpRedirectURIPostSSOLogout).header("Cookie", cookie).build();
        Response response = client.newCall(request).execute();
        response.close();

        // verify the response is a redirect to the logout page
        String location = getLocation(response);
        assertEquals("https://www.open-xchange.com", location, "Wrong logout page");
    }

    /**
     * Record that handles a session with it's session cookie
     */
    protected record OIDCSession(String session, String cookie) {};

    // --------------------------------- Helper ---------------------------------

    /**
     * Reads 'Set-Cookie' response headers and joins them to a cookie header value
     *
     * @param response The response containing 'Set-Cookie' headers
     * @return Cookie header value
     */
    private String getCookie(Response response) {
        List<String> cookies = new ArrayList<>();
        for (Pair<?, ?> header : response.headers()) {
            if (header.getFirst().toString().equalsIgnoreCase("Set-Cookie")) {
                cookies.add(header.getSecond().toString().split(";")[0]);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String cookie : cookies) {
            sb.append(cookie).append("; ");
        }
        return sb.toString();
    }

    /**
     * Verifies if a response is a redirect and returns the location header value
     *
     * @param response The response
     * @return The location header value
     */
    protected String getLocation(Response response) {
        assertTrue(response.isRedirect(), "Response is not a redirect");
        assertTrue(response.code() == 302, "Response code is not 302");

        String location = response.header("Location");
        assertNotNull("Missing location header", location);
        return location;
    }

    /**
     * Follows a given redirect location
     *
     * @param client The OK HTTP client
     * @param location The redirect location
     * @return The redirect response
     * @throws IOException if an unexpected error occurs
     */
    protected Response followRedirect(OkHttpClient client, String location) throws IOException {
        Request request = new Request.Builder().url(location).build();
        return client.newCall(request).execute();
    }

}
