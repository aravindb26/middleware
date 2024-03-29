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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.Header;
import com.openexchange.ajax.session.actions.FormLoginRequest;
import com.openexchange.ajax.session.actions.FormLoginResponse;
import org.junit.jupiter.api.TestInfo;

/**
 * Login using "formlogin" doesn't work after changing IP address of client
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug36484Test extends AbstractAJAXSession {

    private AJAXClient client;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = new AJAXClient(new AJAXSession(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName()), true);
    }

    @AfterEach
    public void tearDown() throws Exception {
        client.logout();
    }

    @Test
    public void testAutoFormLoginWithChangedIP() throws Exception {
        /*
         * perform initial form login & store session cookie
         */
        FormLoginResponse loginResponse = this.client.execute(new FormLoginRequest(testUser.getLogin(), testUser.getPassword()));
        String firstSessionID = loginResponse.getSessionId();
        assertNotNull(firstSessionID, "No session ID");

        /*
         * perform second form login from other IP (faked via X-Forwarded-For header)
         */
        FormLoginRequest secondLoginRequest = new ChangedIPFormLoginRequest(testUser.getLogin(), testUser.getPassword(), "53.246.23.4");
        secondLoginRequest.setCookiesNeeded(false);
        FormLoginResponse secondLoginResponse = this.client.execute(secondLoginRequest);
        String secondSessionID = secondLoginResponse.getSessionId();
        assertFalse(firstSessionID.equals(secondSessionID), "Same session ID");
        this.client.getSession().setId(secondSessionID);
    }

    private static final class ChangedIPFormLoginRequest extends FormLoginRequest {

        private final String fakedRemoteIP;

        /**
         * Initializes a new {@link ChangedIPFormLoginRequest}.
         *
         * @param login The login name
         * @param password The password
         * @param fakedRemoteIP The remote address to use
         */
        public ChangedIPFormLoginRequest(String login, String password, String fakedRemoteIP) {
            super(login, password);
            this.fakedRemoteIP = fakedRemoteIP;
        }

        @Override
        public Header[] getHeaders() {
            return new Header[] { new com.openexchange.ajax.framework.Header.SimpleHeader("X-OX-Test-Fake-Remote-IP", fakedRemoteIP) };
        }

    }

}
