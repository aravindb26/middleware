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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.HttpCookie;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.LoginServlet;

/**
 * {@link AutologinTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */

@Disabled
public class AutologinTest extends AbstractLoginTest {

    public AutologinTest() {
        super();
    }

    @Test
    public void testGeeIForgotMySessionIDCanYouGiveItBack() throws Exception {
        asUser(testUser);
        inModule("login");
        call("store");

        String sessionID = currentClient.getSessionID();

        forgetSession();

        raw("autologin");

        assertTrue(rawResponse.has("session"), "Missing session key");

        assertEquals(sessionID, rawResponse.getString("session"));

        inModule("quota");
        call("filestore", "session", sessionID); // Send some request.

        assertNoError();
    }

    @Test
    public void testRetrieveSessionIDForCertainClient() throws Exception {
        createClient();
        inModule("login");

        raw("login", "name", testUser.getLogin(), "password", testUser.getPassword(), "client", "testclient1");
        String sessionID = rawResponse.getString("session");

        call("store", "session", sessionID);

        forgetSession();
        raw("autologin", "client", "testclient1");

        assertEquals(sessionID, rawResponse.get("session"));

    }

    // Error Cases
    @Test
    public void testUnknownSession() throws Exception {
        asUser(testUser);
        inModule("login");
        call("store");

        Optional<HttpCookie> cookie = currentClient.getCookieStore().getCookies().stream().filter(c -> c.getName().startsWith(LoginServlet.SESSION_PREFIX)).findAny();
        assertTrue(cookie.isPresent());
        cookie.get().setValue("1234567");
        forgetSession();
        call("autologin");
        assertError();
        assertNoOXCookies();
    }

    @Test
    public void testSessionAndSecretMismatch() throws Exception {
        asUser(testUser);
        String sessionID = currentClient.getSessionID();

        asUser(testUser2);
        inModule("login");
        call("store");

        boolean replaced = false;
        for (HttpCookie cookie : currentClient.getCookieStore().getCookies()) {
            if (cookie.getName().startsWith(LoginServlet.SESSION_PREFIX)) {
                cookie.setValue(sessionID); // The session of user 1
                replaced = true;
                break;
            }
        }
        assertTrue(replaced, "Could not find session cookie");

        forgetSession();
        call("autologin");

        assertError();
        assertNoOXCookies();
    }

    private void forgetSession() {
        currentClient.setSessionID(null);
    }

}
