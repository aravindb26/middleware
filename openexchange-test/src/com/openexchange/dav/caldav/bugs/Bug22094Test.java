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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link NewTest} - appointments can not be moved between calendars via iCal
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */

public class Bug22094Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMoveAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create target folder for move on server
         */
        FolderObject subfolder = super.createFolder("move test" + System.currentTimeMillis());
        String subfolderID = Integer.toString(subfolder.getObjectID());
        /*
         * create appointment in default folder on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next thursday at 7:15");
        Date end = TimeTools.D("next thursday at 11:30");
        String title = "move test";
        String iCal = "BEGIN:VCALENDAR" + "\r\n" + "VERSION:2.0" + "\r\n" + "PRODID:-//Apple Inc.//iCal 5.0.2//EN" + "\r\n" + "CALSCALE:GREGORIAN" + "\r\n" + "BEGIN:VTIMEZONE" + "\r\n" + "TZID:Europe/Berlin" + "\r\n" + "BEGIN:DAYLIGHT" + "\r\n" + "TZOFFSETFROM:+0100" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + "DTSTART:19810329T020000" + "\r\n" + "TZNAME:CEST" + "\r\n" + "TZOFFSETTO:+0200" + "\r\n" + "END:DAYLIGHT" + "\r\n" + "BEGIN:STANDARD" + "\r\n" + "TZOFFSETFROM:+0200" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + "DTSTART:19961027T030000" + "\r\n" + "TZNAME:CET" + "\r\n" + "TZOFFSETTO:+0100" + "\r\n" + "END:STANDARD" + "\r\n" + "END:VTIMEZONE" + "\r\n" + "BEGIN:VEVENT" + "\r\n" + "CREATED:" + formatAsUTC(new Date()) + "\r\n" + "UID:" + uid + "\r\n" + "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" + "TRANSP:OPAQUE" + "\r\n" + "CLASS:PUBLIC" + "\r\n" + "SUMMARY:" + title + "\r\n" + "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" + "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" + "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" + "SEQUENCE:1" + "\r\n" + "END:VEVENT" + "\r\n" + "END:VCALENDAR";
        assertEquals(StatusCodes.SC_CREATED, super.putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        super.rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        /*
         * move appointment on client
         */
        assertEquals(StatusCodes.SC_CREATED, super.move(iCalResource, subfolderID), "response code wrong");
        /*
         * update etag from moved appointment
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        PropFindMethod propFind = new PropFindMethod(webDAVClient.getBaseURI() + iCalResource.getHref(), DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse[] responses = webDAVClient.doPropFind(propFind);
        assertNotNull(responses, "got no response");
        assertTrue(1 == responses.length, "got no responses");
        String eTag = this.extractTextContent(PropertyNames.GETETAG, responses[0]);
        assertNotNull(eTag, "got no ETag from response");
        iCalResource.setEtag(eTag);
        /*
         * update resource on target location again
         */
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(subfolderID, uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        /*
         * verify moved appointment on server
         */
        assertNull(super.getAppointment(uid), "appointment still in source folder on server");
        appointment = super.getAppointment(subfolderID, uid);
        super.rememberForCleanUp(appointment);
        assertNotNull(appointment, "appointment not found in target folder on server");
        assertEquals(subfolder.getObjectID(), appointment.getParentFolderID(), "folder ID wrong");
    }

}
