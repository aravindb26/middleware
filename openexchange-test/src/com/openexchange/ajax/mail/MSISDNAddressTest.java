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

package com.openexchange.ajax.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.ajax.mail.actions.SendRequest;
import com.openexchange.ajax.mail.actions.SendResponse;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.ajax.user.actions.GetRequest;
import com.openexchange.ajax.user.actions.GetResponse;
import com.openexchange.ajax.user.actions.UpdateRequest;
import com.openexchange.ajax.user.actions.UpdateResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.mail.MailJSONField;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MSISDNFromTest} MSISDN is a number uniquely identifying a subscription in a GSM or a UMTS mobile network. Simply put, it is the
 * telephone number to the SIM card in a mobile/cellular phone. This abbreviation has several interpretations, the most common one being
 * "Mobile Subscriber Integrated Services Digital Network-Number" Some ISPs allow MSISDNS as email sender addresses. This test verifies that
 * our sender address handling in the backend is able to use MSISDNS specified in the users contact object within the phone number fields.
 * MSISDN numbers are allowed to consist of up to 15 digits and are formed by three pieces Country Code + National Destination Code +
 * Subscriber Number
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class MSISDNAddressTest extends AbstractMailTest {

    private UserValues userValues = null;

    private Contact contactData;

    private final String validTestCellPhoneNumber = "491401234567890";

    private final String invalidTestCellPhoneNumber = "491501234567890";

    public MSISDNAddressTest() {
        super();
    }

    //for local testing make sure com.openexchange.mail.supportMsisdnAddresses=true
    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        userValues = getClient().getValues();
        // get the current contactData and
        GetResponse response = getClient().execute(new GetRequest(userValues.getUserId(), userValues.getTimeZone()));
        contactData = response.getContact();
        setCellularNumberOfContact();
    }

    private void setCellularNumberOfContact() throws OXException, IOException, JSONException {
        Contact changedContactData = new Contact();
        changedContactData.setObjectID(contactData.getObjectID());
        changedContactData.setInternalUserId(contactData.getInternalUserId());
        changedContactData.setCellularTelephone1(validTestCellPhoneNumber);
        changedContactData.setLastModified(new Date());
        UpdateRequest updateRequest = new UpdateRequest(changedContactData, null);
        UpdateResponse updateResponse = getClient().execute(updateRequest);
        // successful update returns only a timestamp
        Date timestamp = updateResponse.getTimestamp();
        assertNotNull(timestamp);
    }

    /*
     * Send an e-mail with the msisdn we just set in the contact
     */
    @Test
    public void testValidFromAddress() throws OXException, IOException, JSONException {
        JSONObject createEMail = createEMail(getSendAddress(getClient()), "MSISDNSubject", MailContentType.PLAIN.name(), "Testing MSISDN as sender address");
        createEMail.put(MailJSONField.FROM.getKey(), validTestCellPhoneNumber);
        SendRequest request = new SendRequest(createEMail.toString());
        SendResponse response = getClient().execute(request);
        assertTrue(response.getFolderAndID() != null && response.getFolderAndID().length > 0, "Send request failed");
    }

    @Test
    public void testInvalidFromAddress() throws OXException, IOException, JSONException {
        System.out.println("Testing invalid");
        JSONObject createEMail = createEMail(getSendAddress(getClient()), "MSISDNSubject", MailContentType.PLAIN.name(), "Testing MSISDN as sender address");
        createEMail.put(MailJSONField.FROM.getKey(), invalidTestCellPhoneNumber);
        SendRequest request = new SendRequest(createEMail.toString(), false);
        SendResponse response = getClient().execute(request);
        assertTrue(response.getException() != null);
        assertEquals(OXException.CATEGORY_USER_INPUT, response.getException().getCategory());
        assertEquals("MSG-0056", response.getException().getErrorCode(), response.getErrorMessage());
    }
}
