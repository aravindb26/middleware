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

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.List;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.mail.actions.DeleteRequest;
import com.openexchange.ajax.mail.actions.ImportMailRequest;
import com.openexchange.ajax.mail.actions.ImportMailResponse;
import com.openexchange.java.Streams;

/**
 * {@link ReplyTest}
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class ReplyTest extends AbstractReplyTest {

    public ReplyTest() {
        super();
    }

    @Test
    public void testShouldReplyToSenderOnly() throws Exception {
        String[][] fmid = null;
        AJAXClient client2 = null;
        try {
            client2 = testUser2.getAjaxClient();
            String mail2 = client2.getValues().getSendAddress();

            String eml =
                "From: " + mail2 + "\n" +
                "To: " + getClient().getValues().getSendAddress() + "\n" +
                "Subject: Test\n" +
                "Mime-Version: 1.0\n" +
                "Content-Type: text/plain; charset=\"UTF-8\"\n" +
                "Content-Transfer-Encoding: 8bit\n" +
                "\n" +
                "Some plain text...";

            InputStream inputStream = Streams.newByteArrayInputStream(eml.getBytes(com.openexchange.java.Charsets.UTF_8));
            final ImportMailRequest importMailRequest = new ImportMailRequest(getClient().getValues().getInboxFolder(), MailFlag.SEEN.getValue(), inputStream);
            final ImportMailResponse importResp = getClient().execute(importMailRequest);
            JSONArray json = (JSONArray) importResp.getData();
            fmid = importResp.getIds();

            String mailID = json.getJSONObject(0).getString("id");
            String folderID = json.getJSONObject(0).getString("folder_id");
            TestMail mail = getMail(folderID, mailID);

            TestMail myReplyMail = new TestMail(getReplyEMail(mail));

            assertTrue(myReplyMail.getSubject().startsWith("Re:"), "Should contain indicator that this is a reply in the subject line");

            List<String> to = myReplyMail.getTo();
            assertTrue(contains(to, mail2), "Sender of original message should become recipient in reply");

            String from = myReplyMail.getFrom();
            assertTrue(from == null || from.isEmpty() || from.equals("[]"), "New sender field should be empty, because GUI offers selection there");
        } finally {
            // Delete the mail in this session
            if (null != fmid) {
                getClient().execute(new DeleteRequest(fmid, true));
            }

            if (null != client2) {
                client2.logout();
            }
        }
    }
}
