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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.java.Strings;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug67329Test}
 *
 * Caldav sync after time limit in com.openexchange.caldav.interval.end from client to OX possible
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class Bug67329Test extends CalDAVTest {

    private Date configuredMinDateTime;
    private Date configuredMaxDateTime;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * discover min- and max-date-time
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.MIN_DATE_TIME);
        props.add(PropertyNames.MAX_DATE_TIME);
        String uri = webDAVClient.getBaseURI() + Config.getPathPrefix() + "/caldav/" + encodeFolderID(getDefaultFolderID());
        PropFindMethod propFind = new PropFindMethod(uri, DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse[] responses = webDAVClient.doPropFind(propFind);
        assertNotNull(responses, "got no response");
        MultiStatusResponse response = assertSingleResponse(responses);
        String minDateTime = (String) response.getProperties(StatusCodes.SC_OK).get(PropertyNames.MIN_DATE_TIME).getValue();
        if (Strings.isNotEmpty(minDateTime)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            configuredMinDateTime = dateFormat.parse(minDateTime);
        }
        String maxDateTime = (String) response.getProperties(StatusCodes.SC_OK).get(PropertyNames.MAX_DATE_TIME).getValue();
        if (Strings.isNotEmpty(maxDateTime)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            configuredMaxDateTime = dateFormat.parse(maxDateTime);
        }
        /*
         * ensure timerange checks are in "strict" mode
         */
        setUpConfiguration();
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("com.openexchange.caldav.interval.strict", "true");
        return config;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateInDistantFuture(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * check if max-date-time is configured
         */
        Calendar calendar = Calendar.getInstance();
        if (null != configuredMaxDateTime) {
            calendar.setTime(configuredMaxDateTime);
            calendar.add(Calendar.DATE, 20);
        } else {
            calendar.setTime(TimeTools.D("in five years"));
        }
        /*
         * prepare event
         */
        String uid = randomUID();
        Date start = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date end = calendar.getTime();
        String iCal = generateICal(start, end, uid, "Bug67329Test", null);
        if (null != configuredMaxDateTime) {
            /*
             * attempt to create event, expecting to fail
             */
            int statusCode = putICal(uid, iCal);
            assertEquals(StatusCodes.SC_FORBIDDEN, statusCode, "response code wrong");
        } else {
            /*
             * attempt to create event
             */
            assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
            /*
             * verify appointment on server & client
             */
            Appointment appointment = super.getAppointment(uid);
            assertNotNull(appointment, "appointment not found on server");
            rememberForCleanUp(appointment);
            ICalResource iCalResource = get(uid);
            assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
            assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        }
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateInDistantPast(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * check if min-date-time is configured
         */
        Calendar calendar = Calendar.getInstance();
        if (null != configuredMinDateTime) {
            calendar.setTime(configuredMinDateTime);
            calendar.add(Calendar.DATE, -20);
        } else {
            calendar.setTime(TimeTools.D("five years ago"));
        }
        /*
         * prepare event
         */
        String uid = randomUID();
        Date start = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date end = calendar.getTime();
        String iCal = generateICal(start, end, uid, "Bug67329Test", null);
        if (null != configuredMinDateTime) {
            /*
             * attempt to create event, expecting to fail
             */
            int statusCode = putICal(uid, iCal);
            assertEquals(StatusCodes.SC_FORBIDDEN, statusCode, "response code wrong");
        } else {
            /*
             * attempt to create event
             */
            assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
            /*
             * verify appointment on server & client
             */
            Appointment appointment = super.getAppointment(uid);
            assertNotNull(appointment, "appointment not found on server");
            rememberForCleanUp(appointment);
            ICalResource iCalResource = get(uid);
            assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
            assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        }
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateEndlessSeriesInDistantPast(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * check if min-date-time is configured
         */
        Calendar calendar = Calendar.getInstance();
        if (null != configuredMinDateTime) {
            calendar.setTime(configuredMinDateTime);
            calendar.add(Calendar.DATE, -20);
        } else {
            calendar.setTime(TimeTools.D("five years ago"));
        }
        /*
         * prepare event
         */
        String uid = randomUID();
        Date start = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date end = calendar.getTime();
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n" +
            "DTSTART:19810329T020000\r\n" +
            "TZNAME:MESZ\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n" +
            "DTSTART:19961027T030000\r\n" +
            "TZNAME:MEZ\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "RRULE:FREQ=YEARLY\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:Bug67329Test\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        /*
         * attempt to create event
         */
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server & client
         */
        Appointment appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
    }

}
