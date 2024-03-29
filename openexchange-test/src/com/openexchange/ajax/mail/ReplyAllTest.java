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
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactSearchObject;

/**
 * {@link ReplyAllTest}
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class ReplyAllTest extends AbstractReplyTest {

    public List<Contact> extract(final int amount, final Contact[] source, final List<String> excludedEmail) {
        final List<Contact> returnees = new LinkedList<Contact>();
        int used = 0;
        for (final Contact elem : source) {
            if (!(excludedEmail.contains(elem.getEmail1()) || excludedEmail.contains(elem.getEmail2()) || excludedEmail.contains(elem.getEmail3())) && used < amount) {
                returnees.add(elem);
                used++;
            }
        }
        return returnees;
    }

    @Test
    public void testDummy() throws Exception {
        // Disabled test below because sending mails do not cover an estimateable time frame
        final String mail1 = getClient().getValues().getSendAddress();
        assertTrue(mail1.length() > 0);
    }

    public void no_testShouldReplyToSenderAndAllRecipients() throws OXException, IOException, JSONException {
        final AJAXClient client1 = getClient();
        final AJAXClient client2 = testUser2.getAjaxClient();
        {
            String folder = client2.getValues().getInboxFolder();
            Executor.execute(client2.getSession(), new com.openexchange.ajax.mail.actions.ClearRequest(folder).setHardDelete(true));
            folder = client2.getValues().getSentFolder();
            Executor.execute(client2.getSession(), new com.openexchange.ajax.mail.actions.ClearRequest(folder).setHardDelete(true));
        }

        final String mail1 = client1.getValues().getSendAddress(); // note: doesn't work the other way around on the dev system, because only the
        final String mail2 = client2.getValues().getSendAddress(); // first account is set up correctly.

        List<Contact> otherContacts;
        {
            ContactSearchObject searchObject = new ContactSearchObject();
            searchObject.setEmail1("*" + testContext.getName() + "*");
            searchObject.addFolder(6);
            otherContacts = extract(2, contactManager.searchAction(searchObject), Arrays.asList(mail1, mail2));
            if (otherContacts.isEmpty()) {
                searchObject = new ContactSearchObject();
                searchObject.setEmail1("*");
                searchObject.addFolder(6);
                otherContacts = extract(2, contactManager.searchAction(searchObject), Arrays.asList(mail1, mail2));
            }
        }
        assertTrue(otherContacts.size() > 1, "Precondition: This test needs at least two other contacts in the global address book to work, but has " + otherContacts.size());

        final String anotherMail = otherContacts.get(0).getEmail1();
        final String yetAnotherMail = otherContacts.get(1).getEmail1();

        final JSONObject mySentMail = createEMail(client2, adresses(mail1, anotherMail, yetAnotherMail), "ReplyAll test", MailContentType.ALTERNATIVE.toString(), MAIL_TEXT_BODY);
        sendMail(client2, mySentMail.toString());

        final JSONObject myReceivedMail = getFirstMailInFolder(getInboxFolder());
        final TestMail myReplyMail = new TestMail(getReplyAllEMail(new TestMail(myReceivedMail)));

        assertTrue(myReplyMail.getSubject().startsWith("Re:"), "Should contain indicator that this is a reply in the subject line");

        final List<String> toAndCC = myReplyMail.getTo();
        toAndCC.addAll(myReplyMail.getCc()); //need to do both because depending on user settings, it might be one of these

        assertTrue(contains(toAndCC, mail2), "Sender of original message should become recipient in reply");
        assertTrue(contains(toAndCC, anotherMail), "1st recipient (" + anotherMail + ") of original message should still be recipient in reply, but TO/CC field only has these: " + toAndCC);
        assertTrue(contains(toAndCC, yetAnotherMail), "2nd recipient (" + yetAnotherMail + ") of original message should still be recipient in reply, but TO/CC field only has these: " + toAndCC);
    }

    protected String adresses(final String... mails) {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (final String mail : mails) {
            builder.append("[null,");
            builder.append(mail);
            builder.append("],");
        }
        builder.setLength(builder.length() - 1);
        builder.append(']');
        return builder.toString();
    }

}
