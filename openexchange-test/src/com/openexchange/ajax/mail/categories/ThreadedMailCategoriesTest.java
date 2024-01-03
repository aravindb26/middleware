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

package com.openexchange.ajax.mail.categories;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;
import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.testing.httpclient.models.MailConversationsResponse;
import com.openexchange.testing.httpclient.models.MailDestinationResponse;

/**
 * {@link ThreadedMailCategoriesTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.3
 */
public class ThreadedMailCategoriesTest extends AbstractMailCategoriesTest {

    @Test
    public void conversationTest() throws Exception {
        AJAXClient user2Client = testUser2.getAjaxClient();
        String send2 = getSendAddress(user2Client);

        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 1;

        // @formatter:off
        final String eml =    "Message-Id: <4A002517.4650.0059.1@foobar.com>\n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n"
                            + "From: " + getSendAddress()
                            + "\n"
                            + "To: " + send2
                            + "\n"
                            + "Subject: Invitation for launch\n"
                            + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                            + "Content-Transfer-Encoding: 8bit\n" + "\n"
                            + "This is a MIME message. If you are reading this text, you may want to \n"
                            + "consider changing to a mail reader or gateway that understands how to \n"
                            + "properly handle MIME multipart messages.";

        final String eml2 =   "Message-Id: <4A002517.4650.0059.1@foobar.com>\n"
                            + "Date: Tue, 05 May 2009 11:37:58 -0500\n"
                            + "From: " + getSendAddress()
                            + "\n"
                            + "To: " + getSendAddress()
                            + "\n"
                            + "Subject: Invitation for launch\n"
                            + "Mime-Version: 1.0\n"
                            + "Content-Type: text/plain; charset=\"UTF-8\"\n"
                            + "Content-Transfer-Encoding: 8bit\n"
                            + "\n"
                            + "This is a MIME message. If you are reading this text, you may want to \n"
                            + "consider changing to a mail reader or gateway that understands how to \n"
                            + "properly handle MIME multipart messages.";
        // @formatter:on

        File tmp = File.createTempFile("eml1", null);
        tmp.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
        writer.write(eml);
        writer.close();

        File tmp2 = File.createTempFile("eml1", null);
        tmp2.deleteOnExit();
        BufferedWriter writer2 = new BufferedWriter(new FileWriter(tmp2));
        writer2.write(eml2);
        writer2.close();

        for (int i = 0; i < numOfMails; i++) {
            MailDestinationResponse resp = mailApi.sendOrSaveMail(tmp, getClient().getValues().getInboxFolder(), null);
            assertNull(resp.getError());
            resp = mailApi.sendOrSaveMail(tmp2, getClient().getValues().getInboxFolder(), null);
            assertNull(resp.getError());
        }

        String origin = values.getInboxFolder();

        // check general - should contain the thread
        MailConversationsResponse convResp = mailApi.getMailConversations(origin, COLUMNS, null, null, null, "610", "DESC", FALSE, I(0), I(10), null, CAT_GENERAL, null);
        assertNull(convResp.getError(), convResp.getErrorDesc());

        assertTrue(convResp.getData().size() == 1, "The number of messages is incorrect.");
        assertTrue(convResp.getData().get(0).getThread().size() > 1, "The message does not contain any child messages.");

        // check social - should be empty
        convResp = mailApi.getMailConversations(origin, COLUMNS, null, null, null, "610", "DESC", FALSE, I(0), I(10), null, CAT_1, null);
        assertNull(convResp.getError(), convResp.getErrorDesc());

        assertTrue(convResp.getData().size() == 0, "The number of messages is incorrect.");

        // train categories
        train(CAT_1, getSendAddress(), TRUE, FALSE);

        // check social again - should now contain the thread
        convResp = mailApi.getMailConversations(origin, COLUMNS, null, null, null, "610", "DESC", FALSE, I(0), I(10), null, CAT_1, null);
        assertNull(convResp.getError(), convResp.getErrorDesc());

        assertTrue(convResp.getData().size() == 1, "The number of messages is incorrect.");
        assertTrue(convResp.getData().get(0).getSize().longValue() != 0, "The message does not contain any child messages.");
    }

}
