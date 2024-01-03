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
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.ajax.mail.actions.NewMailRequest;
import com.openexchange.ajax.mail.actions.NewMailResponse;
import com.openexchange.exception.OXException;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MoveMailTest}
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class MoveMailTest extends AbstractMailTest {

    private UserValues values;

    public MoveMailTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        values = getClient().getValues();
    }

    @Test
    public void testShouldMoveFromSentToDrafts() throws OXException, IOException, JSONException {
        final String eml = "Date: Mon, 19 Nov 2012 21:36:51 +0100 (CET)\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Message-ID: <1508703313.17483.1353357411049>\n" + "Subject: Move a mail\n" + "MIME-Version: 1.0\n" + "Content-Type: multipart/alternative; \n" + "    boundary=\"----=_Part_17482_1388684087.1353357411002\"\n" + "\n" + "------=_Part_17482_1388684087.1353357411002\n" + "MIME-Version: 1.0\n" + "Content-Type: text/plain; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "Move from sent to drafts\n" + "------=_Part_17482_1388684087.1353357411002\n" + "MIME-Version: 1.0\n" + "Content-Type: text/html; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">" + " <head>\n" + "    <meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"/>\n" + " </head><body style=\"font-family: verdana,geneva; font-size: 10pt; \">\n" + " \n" + "  <div>\n" + "   Move from sent to drafts\n" + "  </div>\n" + " \n" + "</body></html>\n" + "------=_Part_17482_1388684087.1353357411002--\n";

        getClient().execute(new NewMailRequest(getInboxFolder(), eml, -1, true));

        String origin = values.getInboxFolder();
        String destination = values.getDraftsFolder();

        TestMail myMail = new TestMail(getFirstMailInFolder(origin));
        String oldID = myMail.getId();

        TestMail movedMail = mtm.move(myMail, destination);
        String newID = movedMail.getId();

        mtm.get(destination, newID);
        assertTrue(!mtm.getLastResponse().hasError(), "Should produce no errors when getting moved e-mail");
        assertTrue(!mtm.getLastResponse().hasConflicts(), "Should produce no conflicts when getting moved e-mail");

        mtm.get(origin, oldID);
        assertTrue(mtm.getLastResponse().hasError(), "Should produce errors when trying to get moved e-mail from original place");
    }

    @Test
    public void testShouldNotMoveToNonExistentFolder() throws OXException, IOException, JSONException {
        final String eml = "Date: Mon, 19 Nov 2012 21:36:51 +0100 (CET)\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Message-ID: <1508703313.17483.1353357411049>\n" + "Subject: Move a mail\n" + "MIME-Version: 1.0\n" + "Content-Type: multipart/alternative; \n" + "    boundary=\"----=_Part_17482_1388684087.1353357411002\"\n" + "\n" + "------=_Part_17482_1388684087.1353357411002\n" + "MIME-Version: 1.0\n" + "Content-Type: text/plain; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "Move from sent to drafts\n" + "------=_Part_17482_1388684087.1353357411002\n" + "MIME-Version: 1.0\n" + "Content-Type: text/html; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">" + " <head>\n" + "    <meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"/>\n" + " </head><body style=\"font-family: verdana,geneva; font-size: 10pt; \">\n" + " \n" + "  <div>\n" + "   Move from sent to drafts\n" + "  </div>\n" + " \n" + "</body></html>\n" + "------=_Part_17482_1388684087.1353357411002--\n";

        getClient().execute(new NewMailRequest(getInboxFolder(), eml, -1, true));

        String origin = values.getInboxFolder();
        String destination = values.getDraftsFolder() + "doesn't exist";

        TestMail myMail = new TestMail(getFirstMailInFolder(origin));
        String oldID = myMail.getId();

        mtm.move(myMail, destination);
        assertTrue(mtm.getLastResponse().hasError(), "Should produce error message when trying to move to nonexistent folder");
        assertEquals("IMAP-1002", mtm.getLastResponse().getException().getErrorCode(), "Should produce proper error message ");

        mtm.get(origin, oldID);
        assertTrue(!mtm.getLastResponse().hasError(), "Should still have e-mail at original location");
        assertTrue(!mtm.getLastResponse().hasConflicts(), "Should produce no conflicts when getting e-mail from original location");
    }

    @Test
    public void testShouldNotTryToMoveToSameFolder() throws Exception {
        //Send mail to myself
        final String eml = ("Message-Id: <4A002517.4650.0059.1@foobar.com>\n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n" + "From: #ADDR#\n" + "To: #ADDR#\n" + "Subject: Invitation for launch\n" + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"UTF-8\"\n" + "Content-Transfer-Encoding: 8bit\n" + "\n" + "This is a MIME message. If you are reading this text, you may want to \n" + "consider changing to a mail reader or gateway that understands how to \n" + "properly handle MIME multipart messages.").replaceAll("#ADDR#", getSendAddress());
        NewMailResponse newMailResponse = getClient().execute(new NewMailRequest(getInboxFolder(), eml, -1, true));
        String folder = newMailResponse.getFolder();
        String id = newMailResponse.getId();
        // Check if mail exists in sent folder
        TestMail newMail = mtm.get(new String[] { folder, id });
        assertNotNull(newMail, "New mail may not be null and has to be found in sent-items folder");
        // Try to move to sent-items
        TestMail movedMail = mtm.move(newMail, folder);
        assertNotNull(movedMail, "Moved mail may not be null");
        // Check that mail remains in the original location
        assertEquals(folder, movedMail.getFolder(), "Mail should not be moved and remain in the original folder.");
        // Check that sent mail wasn't duplicated
        assertEquals(1, mtm.findSimilarMailsInSameFolder(newMail, getClient()).size(), "Mail shouldn't have been duplicated");
    }

}
