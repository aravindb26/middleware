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

package com.openexchange.rest.userfeedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import com.openexchange.ajax.mail.actions.AllRequest;
import com.openexchange.ajax.mail.actions.AllResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.search.Order;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.test.common.tools.RandomString;
import com.openexchange.testing.restclient.invoker.ApiException;
import com.openexchange.testing.restclient.invoker.ApiResponse;
import com.openexchange.userfeedback.rest.services.SendUserFeedbackService;

/**
 * {@link SendTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.4
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SendTest extends AbstractUserFeedbackTest {

    private SimplifiedFeedback requestBody = null;

    private String mailId = "";

    @Override
    protected Application configure() {
        return new ResourceConfig(SendUserFeedbackService.class);
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        requestBody = new SimplifiedFeedback(Collections.singletonList(new SimplifiedRecipient(testUser.getLogin(), testUser.getUser())), "body", "subject", true);
    }

    private void assertMailRetrieved(String subject, MailMessage[] mailMessages) {
        for (MailMessage mailMessage : mailMessages) {
            if (mailMessage.getSubject().equals(subject)) {
                mailId = mailMessage.getMailId();
            }
        }
        assertFalse(mailId.equals(""), "Unable to find feeback mail");
    }

    private static final int[] listAttributes = new int[] { MailListField.ID.getField(), MailListField.FROM.getField(), MailListField.TO.getField(), MailListField.SUBJECT.getField() };

    @Test
    public void testSend_everythingFine_returnMessageAndVerifyMail() throws OXException, IOException, JSONException, AddressException, ApiException {
        String subject = RandomString.generateChars(35);
        requestBody.subject = subject;
        ApiResponse<String> send = userfeedbackApi.sendWithHttpInfo("default", type, requestBody, Long.valueOf(0), Long.valueOf(0));
        assertEquals(200, send.getStatusCode());
        JSONObject resp = new JSONObject(send.getData());
        assertFalse(resp.hasAndNotNull("fail"));

        AllRequest all = new AllRequest(getAjaxClient().getValues().getInboxFolder(), listAttributes, 0, Order.DESCENDING, true);
        AllResponse response = getAjaxClient().execute(all);

        MailMessage[] mailMessages = response.getMailMessages(listAttributes);
        assertMailRetrieved(subject, mailMessages);
    }

    @Test
    public void testSend_subjectNull_sentWithDefaultSubject() throws ApiException, OXException, IOException, JSONException, AddressException {
        requestBody.subject = null;
        ApiResponse<String> resp = userfeedbackApi.sendWithHttpInfo("default", type, requestBody, Long.valueOf(0), Long.valueOf(0));
        assertEquals(200, resp.getStatusCode());

        AllRequest all = new AllRequest(getAjaxClient().getValues().getInboxFolder(), listAttributes, 0, Order.DESCENDING, true);
        AllResponse response = getAjaxClient().execute(all);

        MailMessage[] mailMessages = response.getMailMessages(listAttributes);
        assertMailRetrieved("User Feedback Report", mailMessages);
    }

    @Test
    public void testSend_bodyNull_sentWithEmptyBody() throws ApiException, OXException, IOException, JSONException, AddressException {
        String subject = RandomString.generateChars(35);
        requestBody.subject = subject;
        requestBody.body = null;
        ApiResponse<String> resp = userfeedbackApi.sendWithHttpInfo("default", type, requestBody, Long.valueOf(0), Long.valueOf(0));
        assertEquals(200, resp.getStatusCode());

        AllRequest all = new AllRequest(getAjaxClient().getValues().getInboxFolder(), listAttributes, 0, Order.DESCENDING, true);
        AllResponse response = getAjaxClient().execute(all);

        MailMessage[] mailMessages = response.getMailMessages(listAttributes);
        assertMailRetrieved(subject, mailMessages);
    }

    @Test
    public void testSend_noDisplayName_sentWithoutDisplayName() throws JSONException, ApiException, OXException, IOException, AddressException {
        String subject = RandomString.generateChars(35);
        SimplifiedFeedback body = new SimplifiedFeedback(Collections.singletonList(new SimplifiedRecipient(testUser.getLogin(), null)), null, subject, true);
        ApiResponse<String> resp = userfeedbackApi.sendWithHttpInfo("default", type, body, Long.valueOf(0), Long.valueOf(0));
        assertEquals(200, resp.getStatusCode());

        AllRequest all = new AllRequest(getAjaxClient().getValues().getInboxFolder(), listAttributes, 0, Order.DESCENDING, true);
        AllResponse response = getAjaxClient().execute(all);

        MailMessage[] mailMessages = response.getMailMessages(listAttributes);
        assertMailRetrieved(subject, mailMessages);
    }

    @Test
    public void testSend_badMailAddress_returnException() throws JSONException {
        List<SimplifiedRecipient> recipients = new ArrayList<>();
        recipients.add(new SimplifiedRecipient("badmailaddress", testUser.getUser()));
        SimplifiedFeedback body = new SimplifiedFeedback(recipients, null, null, false);

        try {
            userfeedbackApi.send("default", type, body, Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
            JSONObject exception = new JSONObject(e.getResponseBody());
            assertEquals("Provided addresses are invalid.", exception.get("error_desc"));
        }
    }

    @Test
    public void testSend_unknownContextGroup_return404() {
        try {
            userfeedbackApi.send("unknown", type, requestBody, Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testSend_unknownFeefdbackType_return404() {
        try {
            userfeedbackApi.send("default", "schalke-rating", requestBody, Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testSend_negativeStart_return404() {
        try {
            userfeedbackApi.send("default", type, requestBody, Long.valueOf(-11111), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testSend_negativeEnd_return404() {
        try {
            userfeedbackApi.send("default", type, requestBody, Long.valueOf(0), Long.valueOf(-11111));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testSend_endBeforeStart_return404() {
        try {
            userfeedbackApi.send("default", type, requestBody, Long.valueOf(222222222), Long.valueOf(11111));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    /**
     *
     * {@link SimplifiedFeedback}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.6
     */
    public static class SimplifiedFeedback {

        List<SimplifiedRecipient> recipients;
        String body;
        String subject;
        boolean compress;

        /**
         * Initializes a new {@link SimplifiedFeedback}.
         *
         * @param recipients
         */
        public SimplifiedFeedback(List<SimplifiedRecipient> recipients, String body, String subject, boolean compress) {
            super();
            this.recipients = recipients;
            this.body = body;
            this.compress = compress;
            this.subject = subject;
        }

    }

    /**
     * {@link SimplifiedRecipient}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.6
     */
    public static class SimplifiedRecipient {

        final String address;
        final String displayName;

        /**
         * Initializes a new {@link SimplifiedRecipient}.
         *
         * @param address
         * @param displayName
         */
        public SimplifiedRecipient(String address, String displayName) {
            super();
            this.address = address;
            this.displayName = displayName;
        }

    }
}
