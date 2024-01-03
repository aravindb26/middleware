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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractTestEnvironment;
import com.openexchange.ajax.session.actions.HttpAuthRequest;
import com.openexchange.ajax.session.actions.HttpAuthResponse;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.configuration.AJAXConfig.Property;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * Tests the HTTP authorization header on the login servlet.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class HttpAuthTest extends AbstractTestEnvironment {

    private String protocol;
    private String hostname;
    private String login;
    private String password;
    private TestContext testContext;
    private TestUser testUser;

    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        testContext = TestContextPool.acquireContext(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName());

        protocol = AJAXConfig.getProperty(Property.PROTOCOL);
        hostname = AJAXConfig.getProperty(Property.SERVER_HOSTNAME);
        testUser = testContext.acquireUser();
        login = testUser.getLogin();
        password = testUser.getPassword();
    }

    @AfterEach
    public void tearDown() throws Exception {
        TestContextPool.deleteContext(testContext);
    }

    @Test
    public void testAuthorizationRequired(TestInfo testInfo) throws Throwable {
        HttpUriRequest request = new HttpGet(protocol + "://" + hostname + HttpAuthRequest.HTTP_AUTH_URL);
        try (CloseableHttpClient client = AJAXSession.newHttpClient(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName())) {
            HttpResponse response = client.execute(request);
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode(), "HTTP auth URL does not respond with a required authorization.");
        }
    }

    @Test
    public void testRedirect(TestInfo testInfo) throws Throwable {
        AJAXClient myClient = null;
        try (AJAXSession session = new AJAXSession(testUser.getCreatedBy(), AJAXSession.newHttpClientBuilder(testUser.getCreatedBy(), b -> b.setRedirectsEnabled(false)).build())) {
            myClient = new AJAXClient(session, false);
            // Create session.
            HttpAuthResponse response = myClient.execute(new HttpAuthRequest(login, password));
            String location = response.getLocation();
            assertNotNull(location, "Location is missing in response.");
        } finally {
            if (null != myClient)
                myClient.logout();

        }
    }
}
