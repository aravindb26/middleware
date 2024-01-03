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

package com.openexchange.ajax.onboarding.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.rmi.server.UID;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.testing.httpclient.models.UserAttribute;
import com.openexchange.testing.httpclient.models.UserAttributionResponse;
import com.openexchange.testing.httpclient.modules.ClientonboardingApi;
import com.openexchange.testing.httpclient.modules.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractPlistSMSTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.1
 */
public class AbstractPlistSMSTest extends AbstractConfigAwareAPIClientSession {

    protected static final String UID = new UID((short) 1).toString();

    protected static final String[] SCENARIOS = new String[] { "apple.iphone/mailsync", "apple.iphone/eassync", "apple.iphone/davsync" };

    protected ClientonboardingApi onboardingApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
        UserApi userApi = new UserApi(getApiClient());
        UserAttribute attribute = new UserAttribute();
        attribute.setName("user_sms_link_secret");
        attribute.setValue(UID);

        UserAttributionResponse response = userApi.setUserAttribute(String.valueOf(getApiClient().getUserId()), attribute, Boolean.FALSE);
        assertNull(response.getError(), response.getErrorDesc());

        onboardingApi = new ClientonboardingApi(getApiClient());
    }

    private void checkException(String code, String prefix, int number, String message) {
        assertNotNull(code, "Unexpected response from the server! Response does not contain an exception.");
        assertTrue(code.startsWith(prefix), "The error code should start with " + prefix + " but it is " + code + " instead.");
        int actualCodeNumber = Integer.parseInt(code.substring(code.indexOf('-')+1));
        assertEquals(number, actualCodeNumber, "Wrong exception number (error: " + message + ")!");
    }

    protected void checkException(String code, DisplayableOXExceptionCode exception) {
        checkException(code, exception.getPrefix(), exception.getNumber(), exception.getMessage());
    }

}
