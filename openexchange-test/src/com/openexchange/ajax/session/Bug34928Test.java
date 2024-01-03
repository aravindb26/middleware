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
import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.session.actions.EmptyHttpAuthRequest;
import com.openexchange.ajax.session.actions.HttpAuthRequest;
import com.openexchange.ajax.session.actions.HttpAuthResponse;

/**
 * no autologin with httpauth
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug34928Test extends AbstractAJAXSession {

    private AJAXClient client;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        String name = this.getClass().getCanonicalName() + "." + testInfo.getDisplayName();
        client = new AJAXClient(new AJAXSession(name, AJAXSession.newHttpClientBuilder(name, c -> c.setRedirectsEnabled(false)).build()), true);
    }

    @Test
    public void testAutoHttpAuthLogin() throws Exception {
        /*
         * perform initial HTTP Auth login
         */
        firstHttpAuthLogin();
        String firstSessionID = client.getSession().getId();
        /*
         * perform second HTTP Auth login (without providing authorization headers)
         */
        client.getSession().setId(null);
        HttpAuthResponse httpAuthResponse = client.execute(new EmptyHttpAuthRequest(false, false, false));
        MatcherAssert.assertThat("Second authentication with cookies failed. Session " + firstSessionID, I(httpAuthResponse.getStatusCode()), equalTo(I(SC_FOUND)));
        String secondSessionID = extractSessionID(httpAuthResponse);
        assertNotNull(secondSessionID, "No session ID");
        assertEquals(firstSessionID, secondSessionID, "Different session IDs");
        /*
         * re-enable first session for logout in tearDown
         */
        client.getSession().setId(firstSessionID);
    }

    @Test
    public void testAutoHttpLoginWithWrongSecretCookie() throws Exception {
        /*
         * perform initial HTTP Auth login
         */
        HttpAuthResponse authResponse = firstHttpAuthLogin();
        String firstSessionID = client.getSession().getId();
        /*
         * perform second HTTP Auth login with wrong secret cookie
         */
        client.getSession().setId(null);
        BasicClientCookie cookie = findCookie(authResponse, LoginServlet.SECRET_PREFIX);
        String correctSecret = cookie.getValue();
        cookie.setValue("wrongsecret");
        HttpAuthResponse httpAuthResponse = client.execute(new EmptyHttpAuthRequest(false, false, false));
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, httpAuthResponse.getStatusCode(), "Wrong response code");
        /*
         * re-enable first session for logout in tearDown
         */
        client.getSession().setId(firstSessionID);
        cookie.setValue(correctSecret);
    }

    @Test
    public void testAutoHttpLoginWithWrongSessionCookie() throws Exception {
        /*
         * perform initial HTTP Auth login
         */
        HttpAuthResponse authResponse = firstHttpAuthLogin();
        String firstSessionID = client.getSession().getId();
        /*
         * perform second HTTP Auth login with wrong secret cookie
         */
        client.getSession().setId(null);
        BasicClientCookie cookie = findCookie(authResponse, LoginServlet.SESSION_PREFIX);
        String correctSession = cookie.getValue();
        cookie.setValue("wrongsecret");
        HttpAuthResponse httpAuthResponse = client.execute(new EmptyHttpAuthRequest(false, false, false));
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, httpAuthResponse.getStatusCode(), "Wrong response code");
        /*
         * re-enable first session for logout in tearDown
         */
        client.getSession().setId(firstSessionID);
        cookie.setValue(correctSession);
    }

    private HttpAuthResponse firstHttpAuthLogin() throws Exception {
        HttpAuthResponse httpAuthResponse = client.execute(new HttpAuthRequest(testUser.getLogin(), testUser.getPassword()));
        String sessionID = extractSessionID(httpAuthResponse);
        client.getSession().setId(sessionID);
        return httpAuthResponse;
    }

    private BasicClientCookie findCookie(HttpAuthResponse response, String prefix) {
        List<Cookie> cookies = response.getCookies();
        for (int i = 0; i < cookies.size(); i++) {
            if (cookies.get(i).getName().startsWith(prefix)) {
                return (BasicClientCookie) cookies.get(i);
            }
        }
        fail("No cookie with prefix \"" + prefix + "\" found");
        return null;
    }

    private static String extractSessionID(HttpAuthResponse httpAuthResponse) {
        String location = httpAuthResponse.getLocation();
        assertNotNull(location, "Location is missing in response");
        int sessionStart = location.indexOf("session=");
        assertTrue(0 <= sessionStart, "No session ID");
        return location.substring(sessionStart + 8);
    }
}
