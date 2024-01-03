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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.LoginServlet;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.session.actions.FormLoginRequest;
import com.openexchange.ajax.session.actions.FormLoginResponse;

/**
 * Session count steadily grows with usage of form login
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug32695Test extends AbstractAJAXSession {

    private AJAXClient client;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = new AJAXClient(new AJAXSession(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName()), true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        client.logout();
    }

    @Test
    public void testAutoFormLoginWithOtherUser() throws Exception {
        /*
         * perform initial form login
         */
        firstFormLogin();
        /*
         * perform second form login
         */
        FormLoginRequest secondLoginRequest = new FormLoginRequest(testUser2.getLogin(), testUser2.getPassword());
        secondLoginRequest.setCookiesNeeded(false);
        FormLoginResponse secondLoginResponse = this.client.execute(secondLoginRequest);
        String secondSessionID = secondLoginResponse.getSessionId();
        assertFalse(client.getSession().getId().equals(secondSessionID), "Same session ID");
        this.client.getSession().setId(secondSessionID);
    }

    @Test
    public void testAutoFormLoginWithWrongCredentials() throws Exception {
        /*
         * perform initial form login
         */
        firstFormLogin();
        /*
         * perform second form login with wrong credentials
         */
        FormLoginRequest secondLoginRequest = new FormLoginRequest(testUser2.getLogin(), "wrongpassword");
        secondLoginRequest.setCookiesNeeded(false);
        AssertionError expectedError = null;
        try {
            this.client.execute(secondLoginRequest);
        } catch (AssertionError e) {
            expectedError = e;
        }
        assertNotNull(expectedError, "No errors performing second login with wrong password");
    }

    @Test
    public void testAutoFormLoginWithWrongSecretCookie() throws Exception {
        /*
         * perform initial form login
         */
        FormLoginResponse firstFormLogin = firstFormLogin();
        /*
         * perform second form login with wrong secret cookie
         */
        findCookie(firstFormLogin, LoginServlet.SECRET_PREFIX).setValue("wrongsecret");
        FormLoginRequest secondLoginRequest = new FormLoginRequest(testUser2.getLogin(), testUser2.getPassword());
        secondLoginRequest.setCookiesNeeded(false);
        FormLoginResponse secondLoginResponse = this.client.execute(secondLoginRequest);
        String secondSessionID = secondLoginResponse.getSessionId();
        assertFalse(client.getSession().getId().equals(secondSessionID), "Same session ID");
        this.client.getSession().setId(secondSessionID);
    }

    private FormLoginResponse firstFormLogin() throws Exception {
        FormLoginResponse loginResponse = this.client.execute(new FormLoginRequest(testUser.getLogin(), testUser.getPassword()));
        String sessionID = loginResponse.getSessionId();
        assertNotNull(sessionID, "No session ID");
        client.getSession().setId(sessionID);
        return loginResponse;
    }

    private BasicClientCookie findCookie(AbstractAJAXResponse response, String prefix) {
        List<Cookie> cookies = response.getCookies();
        for (int i = 0; i < cookies.size(); i++) {
            if (cookies.get(i).getName().startsWith(prefix)) {
                return (BasicClientCookie) cookies.get(i);
            }
        }
        fail("No cookie with prefix \"" + prefix + "\" found");
        return null;
    }

}
