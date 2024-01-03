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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Arrays;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.ajax.mail.actions.SendRequest;
import com.openexchange.ajax.mail.actions.SendResponse;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.exception.OXException;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link SendTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a> - tests with manager
 */
public final class SendTest extends AbstractMailTest {

    private MailTestManager manager;

    /**
     * Default constructor.
     *
     * @param name Name of this test.
     */
    public SendTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager = new MailTestManager(getClient(), false);
    }

    /**
     * Tests the <code>action=new</code> request on INBOX folder
     *
     * @throws Throwable
     */
    @Test
    public void testSend() throws Throwable {
        /*
         * Create JSON mail object
         */
        final String mailObject_25kb = createSelfAddressed25KBMailObject().toString();
        /*
         * Perform send request
         */
        final SendResponse response = Executor.execute(getSession(), new SendRequest(mailObject_25kb));
        assertNull(response.getErrorMessage(), response.getErrorMessage());
        assertNotNull(response.getFolderAndID(), "Response should contain a folder and a id but only contained: " + response.getData());
        assertTrue(response.getFolderAndID().length > 0, "No mail in the sent folder");
    }

    @Test
    public void testSendWithManager() throws OXException, IOException, JSONException {
        UserValues values = getClient().getValues();

        TestMail mail = new TestMail();
        mail.setSubject("Test sending with manager");
        mail.setFrom(values.getSendAddress());
        mail.setTo(Arrays.asList(new String[] { values.getSendAddress() }));
        mail.setContentType(MailContentType.PLAIN.toString());
        mail.setBody("This is the message body.");
        mail.sanitize();

        TestMail inSentBox = manager.send(mail);
        assertFalse(manager.getLastResponse().hasError(), "Sending resulted in error");
        assertEquals(values.getSentFolder(), inSentBox.getFolder(), "Mail went into inbox");
    }
}
