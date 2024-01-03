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

package com.openexchange.dav.caldav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestUser;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 * {@link MWB713Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.5
 */
public class MWB713Test extends CalDAVTest {

    private TestUser organizer;
    private String uid;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        organizer = testContextList.get(1).acquireUser();
        uid = randomUID();
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateAsAttendee(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        Date start = TimeTools.D("next monday at 12:00");
        Date end = TimeTools.D("next monday at 13:00");
        Date start2 = TimeTools.D("next monday at 14:00");
        Date end2 = TimeTools.D("next monday at 15:00");

        String attendeeMail = getClient().getValues().getDefaultAddress();
        String organizerMail = organizer.getLogin();

        // @formatter:off
        String create =
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//Open-Xchange//7.10.5-Rev0//EN\n" +
            "METHOD:REQUEST\n" +
            "BEGIN:VEVENT\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\n" +
            "ATTENDEE;CN=A;PARTSTAT=NEEDS-ACTION;CUTYPE=INDIVIDUAL;EMAIL=" + organizerMail + ":mailto:" + organizerMail + "\n" +
            "ATTENDEE;CN=B;PARTSTAT=ACCEPTED;CUTYPE=INDIVIDUAL;EMAIL=" + attendeeMail + ":mailto:" + attendeeMail + "\n" +
            "CLASS:PUBLIC\n" +
            "CREATED:20201204T091730Z\n" +
            "LAST-MODIFIED:20201204T091730Z\n" +
            "ORGANIZER;CN=A:mailto:" + organizerMail + "\n" +
            "SEQUENCE:0\n" +
            "SUMMARY:MWB713Test\n" +
            "TRANSP:OPAQUE\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";

        String update =
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//Open-Xchange//7.10.5-Rev0//EN\n" +
            "METHOD:REQUEST\n" +
            "BEGIN:VEVENT\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start2, "Europe/Berlin") + "\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end2, "Europe/Berlin") + "\n" +
            "ATTENDEE;CN=A;PARTSTAT=NEEDS-ACTION;CUTYPE=INDIVIDUAL;EMAIL=" + organizerMail + ":mailto:" + organizerMail + "\n" +
            "ATTENDEE;CN=B;PARTSTAT=ACCEPTED;CUTYPE=INDIVIDUAL;EMAIL=" + attendeeMail + ":mailto:" + attendeeMail + "\n" +
            "CLASS:PUBLIC\n" +
            "CREATED:20201204T091730Z\n" +
            "LAST-MODIFIED:20201204T091730Z\n" +
            "ORGANIZER;CN=A:mailto:" + organizerMail + "\n" +
            "SEQUENCE:0\n" +
            "SUMMARY:MWB713Test\n" +
            "TRANSP:OPAQUE\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
        // @formatter:on

        assertEquals(StatusCodes.SC_CREATED, putICal(uid, create), "response code wrong");
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(start.getTime(), iCalResource.getVEvent().getDTStart().getTime());
        assertEquals(end.getTime(), iCalResource.getVEvent().getDTEnd().getTime());

        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, update, iCalResource.getETag()), "response code wrong");

        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(start2.getTime(), iCalResource.getVEvent().getDTStart().getTime());
        assertEquals(end2.getTime(), iCalResource.getVEvent().getDTEnd().getTime());
    }

}
