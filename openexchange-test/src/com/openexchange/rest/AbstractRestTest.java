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

package com.openexchange.rest;

import java.util.Optional;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.scheduling.RESTUtilities;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.ProvisioningSetup;
import com.openexchange.java.Charsets;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.restclient.invoker.ApiClient;

public abstract class AbstractRestTest extends JerseyTest {

    private AJAXClient ajaxClient1;
    private AJAXClient ajaxClient2;
    protected TestContext testContext;
    protected TestUser admin;
    protected TestUser testUser;
    protected TestUser testUser2;

    private ApiClient restClient;
    private TestUser restUser;

    private Object protocol;

    private Object hostname;

    protected final ApiClient getRestClient() {
        return restClient;
    }

    protected final AJAXClient getAjaxClient() {
        return ajaxClient1;
    }

    protected final AJAXClient getAjaxClient2() {
        return ajaxClient2;
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        ProvisioningSetup.init();
    }
    
    @Override
    public void setUp() throws Exception {
        throw new IllegalAccessError("Don't use this method. Use \"setup(TestInfo testinfo)\" instead.");
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        testContext = TestContextPool.acquireContext(testInfo, Optional.ofNullable(getTestConfig()));
        testContext.configure(getTestConfig());
        Assertions.assertNotNull(testContext, "Unable to retrieve a context!");
        testUser = testContext.acquireUser();
        testUser2 = testContext.acquireUser();
        ajaxClient1 = testUser.getAjaxClient();
        ajaxClient2 = testUser2.getAjaxClient();
        admin = testContext.getAdmin();

        restClient = RESTUtilities.createRESTClient(testContext.getUsedBy());
        restClient.setVerifyingSsl(false);
        restClient.setBasePath(getBasePath());
        restUser = TestContextPool.getRestUser();
        restClient.setUsername(restUser.getUser());
        restClient.setPassword(restUser.getPassword());
        String authorizationHeaderValue = "Basic " + Base64.encodeBase64String((restUser.getUser() + ":" + restUser.getPassword()).getBytes(Charsets.UTF_8));
        restClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        TestContextPool.deleteContext(testContext);
    }

    /**
     * Gets the test config
     *
     * @return The {@link TestClassConfig}
     */
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().build();
    }

    protected String getBasePath() {
        if (hostname == null) {
            this.hostname = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
        }

        if (protocol == null) {
            this.protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
            if (this.protocol == null) {
                this.protocol = "http";
            }
        }
        return this.protocol + "://" + this.hostname + (protocol.equals("https") ? ":443" : ":80");
    }
}
