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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.mail.actions.AttachmentRequest;
import com.openexchange.ajax.mail.actions.AttachmentResponse;
import com.openexchange.ajax.mail.actions.GetRequest;
import com.openexchange.ajax.mail.actions.GetResponse;
import com.openexchange.ajax.mail.actions.ImportMailRequest;
import com.openexchange.ajax.mail.actions.ImportMailResponse;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailListField;

/**
 * {@link Bug15901Test}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class Bug15901Test extends AbstractMailTest {

    private String folder;

    private String address;

    private String[][] ids;

    public Bug15901Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folder = getClient().getValues().getInboxFolder();
        address = getClient().getValues().getSendAddress();
        final String testmail = TestMails.replaceAddresses(TestMails.DDDTDL_MAIL, address);
        final byte[] buf = testmail.getBytes();
        final ByteArrayInputStream mail = new ByteArrayInputStream(buf);
        final ImportMailRequest request = new ImportMailRequest(folder, 32, mail);
        final ImportMailResponse response = getClient().execute(request);
        ids = response.getIds();
    }

    @Test
    public void testBug15901() throws Throwable {
        final GetRequest request = new GetRequest(folder, ids[0][1], false);
        final GetResponse response = getClient().execute(request);

        final JSONArray attachmentArray = response.getAttachments();

        assertNotNull(attachmentArray, "Attachments not present in JSON mail object.");

        final int len = attachmentArray.length();
        assertTrue(len > 0, "Unexpected number of attachments: ");

        String sequenceId = null;
        for (int i = 0; i < len && sequenceId == null; i++) {
            final JSONObject attachmentObject = attachmentArray.getJSONObject(i);
            final String contentType = attachmentObject.getString(MailJSONField.CONTENT_TYPE.getKey());
            if (contentType.regionMatches(true, 0, "text/htm", 0, 8)) {
                sequenceId = attachmentObject.getString(MailListField.ID.getKey());
            }
        }
        assertTrue(sequenceId != null, "No HTML part found");

        final AttachmentRequest attachmentRequest = new AttachmentRequest(folder, ids[0][1], sequenceId);
        attachmentRequest.setSaveToDisk(false);
        attachmentRequest.setFilter(true);
        AttachmentResponse resp = getClient().execute(attachmentRequest);
        assertNull(resp.getException());
        final String mailSourceCode = resp.getStringBody();
        assertTrue(mailSourceCode.contains("<dl>") && mailSourceCode.contains("<dt>") && mailSourceCode.contains("<dd>"), "Could not detect expected tags.");
    }
}
