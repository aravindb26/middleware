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

package com.openexchange.ajax.chronos.scheduling;

import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.testing.restclient.invoker.ApiClient;
import com.openexchange.testing.restclient.models.PushMail;
import com.openexchange.testing.restclient.modules.PushApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Assertions;

/**
 * 
 * {@link SchedulingRESTApiTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class SchedulingRESTApiTest extends AbstractConfigAwareAPIClientSession {

    private PushApi restApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        /*
         * Initialize REST API client
         */
        ApiClient restClient = RESTUtilities.createRESTClient(testContext.getUsedBy());
        restApi = new PushApi(restClient);
    }

    @Test
    public void testEvent_missingUser() throws Exception {
        String iMip = mockMail();
        /*
         * Prepare push mail
         */
        PushMail mail = new PushMail();
        mail.setFolder("INBOX");
        mail.setEvent(PushMail.EventEnum.MESSAGENEW);
        mail.setBody(iMip);

        /*
         * Syntactical still valid input
         */
        restApi.pushmail(mail);
    }

    @Test
    public void testEvent_missingEvent() throws Exception {
        String iMip = mockMail();
        /*
         * Prepare push mail
         */
        PushMail mail = new PushMail();
        mail.setUser(testUser.getLogin());
        mail.setFolder("INBOX");
        mail.setBody(iMip);

        /*
         * Syntactical still valid input
         */
        restApi.pushmail(mail);
    }

    @Test
    public void testEvent_missingFolder() {
        Assertions.assertThrows(com.openexchange.testing.restclient.invoker.ApiException.class, () -> {
            String iMip = mockMail();
            /*
             * Prepare push mail
             */
            PushMail mail = new PushMail();
            mail.setUser(testUser.getLogin());
            mail.setEvent(PushMail.EventEnum.MESSAGENEW);
            mail.setBody(iMip);

            restApi.pushmail(mail);
            Assertions.fail("Should have thrown ApiException");
        });
    }

    public void testEvent_wrongFolder() throws Exception {
        String iMip = mockMail();
        /*
         * Prepare push mail
         */
        PushMail mail = new PushMail();
        mail.setUser(testUser.getLogin());
        mail.setEvent(PushMail.EventEnum.MESSAGENEW);
        mail.setFolder("nonExistingFolder");
        mail.setBody(iMip);

        /*
         * Currently no use of the folder parameter over the push end point
         */
        restApi.pushmail(mail);
    }

    @Test
    public void testEvent_missingMail() {
        Assertions.assertThrows(com.openexchange.testing.restclient.invoker.ApiException.class, () -> {
            /*
             * Prepare push mail
             */
            PushMail mail = new PushMail();
            mail.setUser(testUser.getLogin());
            mail.setEvent(PushMail.EventEnum.MESSAGENEW);
            mail.setFolder("INBOX");

            restApi.pushmail(mail);
            Assertions.fail("Should have thrown ApiException");
        });

    }

    private String mockMail() {
        // From https://datatracker.ietf.org/doc/html/rfc6047#section-4.1
        return "   From: sman@netscape.example.com\n"//@formatter:off
            + "   To: stevesil@microsoft.example.com\n"
            + "   Subject: Phone Conference\n"
            + "   Mime-Version: 1.0\n"
            + "   Content-Type: text/calendar; method=REQUEST; charset=US-ASCII\n"
            + "   Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "   BEGIN:VCALENDAR\n"
            + "   PRODID:-//Example/ExampleCalendarClient//EN\n"
            + "   METHOD:REQUEST\n"
            + "   VERSION:2.0\n"
            + "   BEGIN:VEVENT\n"
            + "   ORGANIZER:mailto:man@netscape.example.com\n"
            + "   ATTENDEE;ROLE=CHAIR;PARTSTAT=ACCEPTED:mailto:man@netscape.example.com\n"
            + "   ATTENDEE;RSVP=YES:mailto:stevesil@microsoft.example.com\n"
            + "   DTSTAMP:19970611T190000Z\n"
            + "   DTSTART:19970701T210000Z\n"
            + "   DTEND:19970701T230000Z\n"
            + "   SUMMARY:Phone Conference\n"
            + "   DESCRIPTION:Please review the attached document.\n"
            + "   UID:calsvr.example.com-873970198738777\n"
            + "   ATTACH:ftp://ftp.bar.example.com/pub/docs/foo.doc\n"
            + "   STATUS:CONFIRMED\n"
            + "   END:VEVENT\n"
            + "   END:VCALENDAR";//@formatter:on
    }
}
