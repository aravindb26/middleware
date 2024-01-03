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

package com.openexchange.ajax.mailaccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.config.util.ChangePropertiesRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountAllRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountAllResponse;
import com.openexchange.ajax.mailaccount.actions.MailAccountDeleteRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountDeleteResponse;
import com.openexchange.ajax.mailaccount.actions.MailAccountGetRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountGetResponse;
import com.openexchange.ajax.mailaccount.actions.MailAccountInsertRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountInsertResponse;
import com.openexchange.mail.MailProperty;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;

/**
 * {@link MailAccountAllowExternalTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailAccountAllowExternalTest extends AbstractMailAccountTest {

    /**
     * Default constructor
     */
    public MailAccountAllowExternalTest() {
        super();
    }

    //////////////////////////// CREATE //////////////////////////

    @Test
    public void testAllowCreateExternalAccount() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);
    }

    @Test
    public void testForbidCreateExternalAccount() throws Exception {
        setProperty(false);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc, false);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertTrue("Missing error message", resp.hasError());
        assertTrue("Wront error message: " + resp.getErrorMessage(), MailAccountExceptionCodes.EXTERNAL_ACCOUNTS_DISABLED.equals(resp.getException()));
    }

    ///////////////////////// GET /////////////////////////////

    @Test
    public void testAllowGetExternalAccount() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);

        MailAccountGetRequest mailAccountGetRequest = new MailAccountGetRequest(acc.getId());
        MailAccountGetResponse getResponse = getClient().execute(mailAccountGetRequest);
        assertFalse(getResponse.getErrorMessage(), getResponse.hasError());
        assertEquals(getResponse.getAsDescription().getId(), acc.getId());
    }

    @Test
    public void testForbidGetExternalAccount() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc, false);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);

        setProperty(false);

        MailAccountGetRequest mailAccountGetRequest = new MailAccountGetRequest(acc.getId(), false);
        MailAccountGetResponse getResponse = getClient().execute(mailAccountGetRequest);
        assertEquals(null, getResponse.getAsDescription().getTransportLogin());
    }

    /////////////////////////// GET ALL //////////////////////////

    @Test
    public void testAllowGetAllExternalAccounts() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);

        MailAccountAllRequest allRequest = new MailAccountAllRequest(1001);
        MailAccountAllResponse response = getClient().execute(allRequest);
        assertFalse(response.getErrorMessage(), response.hasError());
        assertNotNull(response.getDescriptions());
        assertFalse(response.getDescriptions().isEmpty());
        assertTrue(response.getDescriptions().size() == 2);
        assertEquals(response.getDescriptions().get(1).getId(), acc.getId());

    }

    @Test
    public void testForbidGetAllExternalAccounts() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);

        setProperty(false);

        MailAccountAllRequest allRequest = new MailAccountAllRequest(1001);
        MailAccountAllResponse response = getClient().execute(allRequest);
        assertFalse(response.getErrorMessage(), response.hasError());
        assertNotNull(response.getDescriptions());
        assertFalse(response.getDescriptions().isEmpty());
        assertTrue(response.getDescriptions().size() == 2);
        assertNotEquals(response.getDescriptions().get(0).getId(), acc.getId());
        assertNull(response.getDescriptions().get(1).getTransportLogin());
    }

    ///////////////////////////// DELETE ////////////////////////////////

    @Test
    public void testAllowDeleteExternalAccountsWithSettingOn() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);

        MailAccountDeleteRequest deleteRequest = new MailAccountDeleteRequest(acc.getId());
        MailAccountDeleteResponse deleteResponse = getClient().execute(deleteRequest);
        assertFalse(deleteResponse.getErrorMessage(), deleteResponse.hasError());

        MailAccountAllRequest allRequest = new MailAccountAllRequest(1001);
        MailAccountAllResponse response = getClient().execute(allRequest);
        assertFalse(response.getErrorMessage(), response.hasError());
        assertNotNull(response.getDescriptions());
        assertFalse(response.getDescriptions().isEmpty());
        assertTrue(response.getDescriptions().size() == 1);
        assertEquals(response.getDescriptions().get(0).getId(), Account.DEFAULT_ID);
    }

    @Test
    public void testAllowDeleteExternalAccountsWithSettingOff() throws Exception {
        setProperty(true);

        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.getErrorMessage(), resp.hasError());
        resp.fillObject(acc);

        setProperty(false);

        MailAccountDeleteRequest deleteRequest = new MailAccountDeleteRequest(acc.getId());
        MailAccountDeleteResponse deleteResponse = getClient().execute(deleteRequest);
        assertFalse(deleteResponse.getErrorMessage(), deleteResponse.hasError());

        MailAccountAllRequest allRequest = new MailAccountAllRequest(1001);
        MailAccountAllResponse response = getClient().execute(allRequest);
        assertFalse(response.getErrorMessage(), response.hasError());
        assertNotNull(response.getDescriptions());
        assertFalse(response.getDescriptions().isEmpty());
        assertTrue(response.getDescriptions().size() == 1);
        assertEquals(response.getDescriptions().get(0).getId(), Account.DEFAULT_ID);
    }


    /**
     * Sets the {@link MailProperty#SMTP_ALLOW_EXTERNAL} property to the given value
     *
     * @param flag The flag to set
     * @throws Exception if an error is occurred
     */
    private void setProperty(boolean flag) throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(MailProperty.SMTP_ALLOW_EXTERNAL.getFQPropertyName(), Boolean.toString(flag));
        ChangePropertiesRequest changePropertiesRequest = new ChangePropertiesRequest(properties, "context", null);
        client1.execute(changePropertiesRequest);
    }
}
