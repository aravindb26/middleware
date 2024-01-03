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

package com.openexchange.ajax.session;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Stream;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.session.actions.TokenLoginJSONRequest;
import com.openexchange.ajax.session.actions.TokenLoginJSONResponse;
import com.openexchange.ajax.session.actions.TokenLoginRequest;
import com.openexchange.ajax.session.actions.TokenLoginResponse;
import com.openexchange.ajax.session.actions.TokensRequest;
import com.openexchange.ajax.session.actions.TokensResponse;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.sessiond.SessionExceptionCodes;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * Tests the action tokenLogin of the login servlet.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class TokenLoginTest extends AbstractAJAXSession {

    @Test
    public void testTokenLogin(TestInfo testInfo) throws Exception {
        final AJAXSession session = prepareSession(testInfo);
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginResponse response = myClient.execute(new TokenLoginRequest(testUser.getLogin(), testUser.getPassword()));
            assertNotNull(response.getPath(), "Path of redirect response is not found.");
            assertNotNull(response.getServerToken(), "Server side token not found as fragment.");
            // assertNotNull("Login string was not found as fragment.", response.getLogin());
            assertNotSame(I(-1), I(response.getUserId()), "");
            assertNotNull(response.getLanguage(), "Language string was not found as fragment.");
            // Activate session with tokens.
            TokensResponse response2 = myClient.execute(new TokensRequest(response.getHttpSessionId(), response.getClientToken(), response.getServerToken()));
            session.setId(response2.getSessionId());
            // Test if session really works
            int userId = myClient.execute(new GetRequest(Tree.Identifier)).getInteger();
            assertTrue(userId > 2, "Users identifier is somehow wrong. Check if session is correctly activated.");
        } finally {
            myClient.logout();
        }
    }

    @Test
    public void testSessionExpire(TestInfo testInfo) throws Exception {
        final AJAXSession session = new AJAXSession(getCreatedBy(testInfo), AJAXSession.newHttpClient(getCreatedBy(testInfo)));
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginResponse response = myClient.execute(new TokenLoginRequest(testUser.getLogin(), testUser.getPassword()));
            assertNotNull(response.getPath(), "Path of redirect response is not found.");
            assertNotNull(response.getServerToken(), "Server side token not found as fragment.");
            // assertNotNull("Login string was not found as fragment.", response.getLogin());
            assertNotSame(I(-1), I(response.getUserId()), "");
            assertNotNull(response.getLanguage(), "Language string was not found as fragment.");
            Thread.sleep(60000);
            // Tokened session should be timed out.
            TokensResponse response2 = myClient.execute(new TokensRequest(response.getHttpSessionId(), response.getClientToken(), response.getServerToken(), false));
            assertTrue(response2.hasError(), "Tokened session should be expired.");
            // Check for correct exception
            OXException expected = OXExceptionFactory.getInstance().create(SessionExceptionCodes.NO_SESSION_FOR_SERVER_TOKEN, "a", "b");
            OXException actual = response2.getException();
            assertTrue(actual.similarTo(expected), "Wrong exception");
        } finally {
            myClient.logout();
        }
    }

    @Test
    public void testTokenLogin_passwordInRequest_returnError(TestInfo testInfo) throws Exception {
        final AJAXSession session = prepareSession(testInfo);
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginJSONResponse response = myClient.execute(new TokenLoginJSONRequest(testUser.getLogin(), testUser.getPassword(), false, true));

            assertNotNull(response);
            assertTrue(response.hasError(), "Error expected.");
            assertEquals(AjaxExceptionCodes.NOT_ALLOWED_URI_PARAM.getNumber(), response.getException().getCode(), "Wrong error.");
            assertTrue(response.getErrorMessage().contains("password"), "Wrong param");
        } finally {
            myClient.logout();
        }
    }

    @Test
    public void testTokenLoginWithJSON_returnJsonResponse(TestInfo testInfo) throws Exception {
        final AJAXSession session = prepareSession(testInfo);
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginJSONResponse response = myClient.execute(new TokenLoginJSONRequest(testUser.getLogin(), testUser.getPassword(), true));
            JSONObject json = response.getResponse().getJSON();
            assertNotNull(json);
            assertNotNull(json.get("jsessionid"));
            assertNull(response.getData());
            assertNull(response.getErrorMessage());
            assertNull(response.getConflicts());
            assertNull(response.getException());
        } finally {
            myClient.logout();
        }
    }

    @Test
    public void testTokenLoginWithJSONResponse_passwordInRequest_returnError(TestInfo testInfo) throws Exception {
        final AJAXSession session = prepareSession(testInfo);
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginJSONResponse response = myClient.execute(new TokenLoginJSONRequest(testUser.getLogin(), testUser.getPassword(), true, true));

            JSONObject json = ResponseWriter.getJSON(response.getResponse());
            assertNotNull(json);
            assertTrue(response.hasError(), "Error expected.");
            assertEquals(AjaxExceptionCodes.NOT_ALLOWED_URI_PARAM.getNumber(), response.getException().getCode(), "Wrong error.");
            assertTrue(response.getErrorMessage().contains("password"), "Wrong param");
        } finally {
            myClient.logout();
        }
    }

    @Test
    public void testSessionCookieWithStaySignedIn(TestInfo testInfo) throws Exception {
        final AJAXSession session = prepareSession(testInfo);
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginResponse response = myClient.execute(new TokenLoginRequest(testUser.getLogin(), testUser.getPassword(), true));
            TokensResponse response2 = myClient.execute(new TokensRequest(response.getHttpSessionId(), response.getClientToken(), response.getServerToken()));

            Stream<Cookie> cookies = response2.getCookies().stream();
            Cookie sessionCookie = cookies.filter(c -> c.getName().startsWith(LoginServlet.SESSION_PREFIX)).findFirst().orElse(null);
            assertNotNull(sessionCookie, "Session cookie is missing in tokens response");
            assertNotNull(sessionCookie.getExpiryDate(), "Session cookie has no expiry date set");
            session.setId(response2.getSessionId());
        } finally {
            myClient.logout();
        }
    }

    @Test
    public void testSessionCookieWithoutStaySignedIn(TestInfo testInfo) throws Exception {
        final AJAXSession session = prepareSession(testInfo);
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            TokenLoginResponse response = myClient.execute(new TokenLoginRequest(testUser.getLogin(), testUser.getPassword()));
            TokensResponse response2 = myClient.execute(new TokensRequest(response.getHttpSessionId(), response.getClientToken(), response.getServerToken()));

            Stream<Cookie> cookies = response2.getCookies().stream();
            Cookie sessionCookie = cookies.filter(c -> c.getName().startsWith(LoginServlet.SESSION_PREFIX)).findFirst().orElse(null);
            assertNotNull(sessionCookie, "Session cookie is missing in tokens response");
            assertNull(sessionCookie.getExpiryDate(), "Session cookie has expiry date set but should not");
            session.setId(response2.getSessionId());
        } finally {
            myClient.logout();
        }
    }

    private AJAXSession prepareSession(TestInfo testInfo) {
        return new AJAXSession(getCreatedBy(testInfo), AJAXSession.newHttpClientBuilder(getCreatedBy(testInfo), c -> c.setRedirectsEnabled(false)).build());
    }

    private String getCreatedBy(TestInfo testInfo) {
        return this.getClass().getCanonicalName() + "." + testInfo.getDisplayName();
    }

}
