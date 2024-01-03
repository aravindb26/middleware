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

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import okhttp3.Response;

/**
* {@link OIDCLoginTest} - Tests for OIDC login and logout functionality
*
* @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
*/
public class OIDCLoginTest extends AbstractOIDCTest {

    @Test
    public void testSuccessfullSSOLoginLogoutFlow() throws Exception {
        // login
        Response loginPage = initLoginFlow(client);
        String rpRedirectURIAuth = keycloakLogin(client, testUser.getLogin(), testUser.getPassword(), loginPage);
        OIDCSession session = middlewareLogin(client, rpRedirectURIAuth);

        // logout
        String opLogoutEndpoint = initLogoutFlow(client, session.session(), session.cookie());
        String rpRedirectURIPostSSOLogout = keycloakLogout(client, opLogoutEndpoint);
        middlewareLogout(client, rpRedirectURIPostSSOLogout, session.cookie());
    }

    @Test
    public void testFailedKeycloakLogin() throws Exception {
        // user does not exist at keycloak
        Response loginPage = initLoginFlow(client);
        assertThrows(AssertionError.class, () -> {
            keycloakLogin(client, "idontexist", "secret", loginPage);
        });
    }

    @Test
    public void testFailedOXLogin() throws Exception {
        // user exists at keycloak, but not in the middleware
        Response loginPage = initLoginFlow(client);
        String rpRedirectURIAuth = keycloakLogin(client, "anton@context2147483647.ox.test", "secret", loginPage);
        assertThrows(AssertionError.class, () -> {
            middlewareLogin(client, rpRedirectURIAuth);
        });
    }

}
