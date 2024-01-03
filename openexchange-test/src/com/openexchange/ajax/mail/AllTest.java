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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import javax.mail.internet.InternetAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.mail.actions.AllRequest;
import com.openexchange.ajax.mail.actions.AllResponse;
import com.openexchange.ajax.mail.actions.NewMailRequest;
import com.openexchange.mail.dataobjects.MailMessage;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AllTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="karsten.will@open-xchange.com">Karsten Will</a>
 */
public final class AllTest extends AbstractMailTest {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllTest.class);

    String mailObject_25kb;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * Clean everything
         */
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
        clearFolder(getTrashFolder());

        /*
         * Create JSON mail object
         */
        mailObject_25kb = createSelfAddressed25KBMailObject().toString();
    }

    /**
     * Tests the <code>action=all</code> request on INBOX folder
     *
     * @throws Throwable
     */
    @Test
    public void testAll() throws Throwable {
        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 25;
        LOG.info("Sending " + numOfMails + " mails to fill emptied INBOX");
        final String eml = "Message-Id: <4A002517.4650.0059.1@foobar.com>\n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Subject: Invitation for launch\n" + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"US-ASCII\"\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "Blah blah blah blah blah blah";
        for (int i = 0; i < numOfMails; i++) {
            getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true));
            LOG.info("Appended " + (i + 1) + ". mail of " + numOfMails);
        }
        /*
         * Perform all request
         */
        final AllResponse allR = Executor.execute(getSession(), new AllRequest(getInboxFolder(), COLUMNS_DEFAULT_LIST, 0, null, true));
        if (allR.hasError()) {
            fail(allR.getException().toString());
        }
        final Object[][] array = allR.getArray();
        assertNotNull(array, "Array of all request is null.");
        assertEquals(numOfMails, array.length, "All request shows different number of mails.");
        assertEquals(COLUMNS_DEFAULT_LIST.length, array[0].length, "Number of columns differs from request ones.");
    }

    /**
     * Tests the <code>action=all</code> request on INBOX folder
     *
     * @throws Throwable
     */
    @Test
    public void testAllLimit() throws Throwable {
        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 25;
        LOG.info("Sending " + numOfMails + " mails to fill emptied INBOX");
        final String eml = "Message-Id: <4A002517.4650.0059.1@foobar.com>\n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Subject: Invitation for launch\n" + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"US-ASCII\"\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "Blah blah blah blah blah blah";
        for (int i = 0; i < numOfMails; i++) {
            getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true));
            LOG.info("Appended " + (i + 1) + ". mail of " + numOfMails);
        }
        /*
         * Perform all request
         */
        final int left = 0;
        final int right = 10;
        final AllRequest allRequest = new AllRequest(getInboxFolder(), COLUMNS_DEFAULT_LIST, 0, null, true);
        allRequest.setLeftHandLimit(left);
        allRequest.setRightHandLimit(right);
        final AllResponse allR = Executor.execute(getSession(), allRequest);
        if (allR.hasError()) {
            fail(allR.getException().toString());
        }
        final Object[][] array = allR.getArray();
        assertNotNull(array, "Array of all request is null.");
        assertEquals((right - left), array.length, "All request shows different number of mails.");
        assertEquals(COLUMNS_DEFAULT_LIST.length, array[0].length, "Number of columns differs from request ones.");
    }

    @Test
    public void testAllResponseGetMailObjects() throws Exception {

        /*
         * Insert <numOfMails> mails through a send request
         */
        final int numOfMails = 5;
        LOG.info("Sending " + numOfMails + " mails to fill emptied INBOX");
        final String eml = "Message-Id: <4A002517.4650.0059.1@foobar.com>\n" + "Date: Tue, 05 May 2009 11:37:58 -0500\n" + "From: " + getSendAddress() + "\n" + "To: " + getSendAddress() + "\n" + "Subject: Invitation for launch\n" + "Mime-Version: 1.0\n" + "Content-Type: text/plain; charset=\"US-ASCII\"\n" + "Content-Transfer-Encoding: 7bit\n" + "\n" + "Blah blah blah blah blah blah";
        for (int i = 0; i < numOfMails; i++) {
            getClient().execute(new NewMailRequest(getClient().getValues().getInboxFolder(), eml, -1, true));
            LOG.info("Appended " + (i + 1) + ". mail of " + numOfMails);
        }

        final AllResponse allR = Executor.execute(getSession(), new AllRequest(getInboxFolder(), COLUMNS_DEFAULT_LIST, 0, null, true));
        if (allR.hasError()) {
            fail(allR.getException().toString());
        }
        final MailMessage[] mailMessages = allR.getMailMessages(COLUMNS_DEFAULT_LIST);
        for (final MailMessage mailMessage : mailMessages) {
            assertEquals(new InternetAddress(getSendAddress()), mailMessage.getFrom()[0], "From is not equal");
            assertEquals("Invitation for launch", mailMessage.getSubject(), "Subject is not equal");
            assertEquals(getInboxFolder(), mailMessage.getFolder(), "Folder is not equal");
            assertFalse(mailMessage.hasAttachment(), "hasAttachment is not equal");
            assertEquals(new InternetAddress(getSendAddress()), mailMessage.getTo()[0], "To is not equal");
        }
    }

}
