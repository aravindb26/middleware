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

package com.openexchange.ajax.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.test.Host;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.ConfigBody;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.modules.ConfigApi;

/**
 * {@link Bug27469Test} Test setting of the default sender address to one of the available aliases
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class Bug27469Test extends AbstractAJAXSession {

	ConfigApi configApi;

	/**
     * Initializes a new {@link Bug27469Test}.
     *
     * @param name
     */
    public Bug27469Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    	testContext.configure(getTestConfig());
    	configApi = new ConfigApi(testUser.getApiClient(Host.SINGLENODE));
    }

    @Override
    public TestClassConfig getTestConfig() {
    	List<String> aliasesList = Arrays.asList("newalias@test");
        return TestClassConfig.builder()
            .withContexts(1)
            .withUserConfig(TestUserConfig.builder().withAliases(aliasesList).build()).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetAliases() throws Exception{
        ConfigResponse response = configApi.getConfigNode("/modules/mail/addresses");
        ArrayList<String> allAddresses = (ArrayList<String>) response.getData();
        int numberOfAddresses = allAddresses.size();
        assertTrue(allAddresses.size() > 1);
        for (int i = 0; i < numberOfAddresses; i++) {
            String newAddress = allAddresses.get(i);
            setSendAddress(newAddress);
            assertEquals(newAddress, getSendAddress(), "The sendAddress wasn't updated");
        }
    }

    private void setSendAddress(String newAddress) throws Exception {
    	CommonResponse setResponse = configApi.putConfigNode("/modules/mail/sendaddress", new ConfigBody().data(newAddress));
        assertNull(setResponse.getError());
    }

    private String getSendAddress() throws Exception {
    	ConfigResponse response = configApi.getConfigNode("/modules/mail/sendaddress");
		String sendAddress = (String) response.getData();
        return sendAddress;
    }
}
