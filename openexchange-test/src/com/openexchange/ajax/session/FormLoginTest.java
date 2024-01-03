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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.session.actions.FormLoginRequest;
import com.openexchange.ajax.session.actions.FormLoginResponse;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * Tests the action formLogin of the login servlet.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class FormLoginTest extends AbstractAJAXSession {

    private String login;

    private String password;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        TestUser testUser3 = testContext.acquireUser();
        login = testUser3.getLogin();
        password = testUser3.getPassword();
    }

    @Test
    public void testFormLogin(TestInfo testInfo) throws Exception {
        final AJAXSession session = new AJAXSession(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName());
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            FormLoginResponse response = myClient.execute(new FormLoginRequest(login, password));
            assertNotNull(response.getPath(), "Path of redirect response is not found.");
            assertNotNull(response.getSessionId(), "Session identifier not found as fragment.");
            // assertNotNull("Login string was not found as fragment.", response.getLogin());
            assertNotSame(I(-1), I(response.getUserId()), "");
            assertNotNull(response.getLanguage(), "Language string was not found as fragment.");
            session.setId(response.getSessionId());
        } finally {
            myClient.logout();
        }
    }

    /**
     * This test is disabled because it tests a hidden feature. Normally the formLogin request requires an authId. This test verifies that
     * the formLogin request works without authId. To disable the required authId put
     * <code>com.openexchange.login.formLoginWithoutAuthId=true</code> into the login.properties configuration file.
     */
    public void dontTestFormLoginWithoutAuthId(TestInfo testInfo) throws Exception {
        final AJAXSession session = new AJAXSession(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName());
        final AJAXClient myClient = new AJAXClient(session, false);
        try {
            FormLoginResponse response = myClient.execute(new FormLoginRequest(login, password, null));
            assertNotNull(response.getPath(), "Path of redirect response is not found.");
            assertNotNull(response.getSessionId(), "Session identifier not found as fragment.");
            //assertNotNull("Login string was not found as fragment.", response.getLogin());
            assertNotSame(I(-1), I(response.getUserId()), "");
            assertNotNull(response.getLanguage(), "Language string was not found as fragment.");
            session.setId(response.getSessionId());
        } finally {
            myClient.logout();
        }
    }
}
