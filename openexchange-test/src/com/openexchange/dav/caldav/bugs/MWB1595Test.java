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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB1595Test}
 * 
 * Appointment deleted via CalDAV that was previously declined
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1595Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager catm2;

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.IOS_15_0_1;
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        catm2 = new CalendarTestManager(client2);
        catm2.setFailOnError(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeclinedThenMovedException(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user b, create appointment series on server & invite user a
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("last week at noon", TimeZone.getTimeZone("Europe/Berlin")));
        Date seriesStart = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date seriesEnd = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        calendar.add(Calendar.DATE, 2);
        Date exceptionStart = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date exceptionEnd = calendar.getTime();
        Appointment appointment2 = new Appointment();
        appointment2.setShownAs(Appointment.RESERVED);
        appointment2.setTimezone("Europe/Berlin");
        appointment2.setUid(uid);
        appointment2.setTitle("MWB1595Test");
        appointment2.setIgnoreConflicts(true);
        appointment2.setRecurrenceType(Appointment.DAILY);
        appointment2.setInterval(1);
        appointment2.setStartDate(seriesStart);
        appointment2.setEndDate(seriesEnd);
        appointment2.addParticipant(new UserParticipant(client2.getValues().getUserId()));
        appointment2.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment2.setParentFolderID(catm2.getPrivateFolder());
        appointment2 = catm2.insert(appointment2);
        /*
         * as user a, 'accept' the whole series, but also 'decline' one in the past
         */
        Appointment appointment = catm.get(catm.getPrivateFolder(), appointment2.getObjectID());
        catm.confirm(catm.getPrivateFolder(), appointment.getObjectID(), appointment.getLastModified(), Appointment.ACCEPT, "ok");
        appointment = catm.get(catm.getPrivateFolder(), appointment.getObjectID());
        catm.confirm(catm.getPrivateFolder(), appointment.getObjectID(), appointment.getLastModified(), Appointment.DECLINE, "conflict", 3);
        /*
         * as user a, get & verify series via caldav
         */
        ICalResource iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs");
        Property attendeePropertySeries = iCalResource.getVEvent().getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeePropertySeries, "Attendee not found");
        assertEquals("ACCEPTED", attendeePropertySeries.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        Component exceptionComponent = iCalResource.getVEvents().get(1);
        Property attendeePropertyException = exceptionComponent.getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeePropertyException, "Attendee not found");
        assertEquals("DECLINED", attendeePropertyException.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        /*
         * as user b, re-schedule the occurrence that was previously declined by user a
         */
        Appointment exception2 = catm2.get(catm2.getPrivateFolder(), appointment.getObjectID(), 3);
        Appointment exception2Update = new Appointment();
        exception2Update.setObjectID(exception2.getObjectID());
        exception2Update.setParentFolderID(exception2.getParentFolderID());
        exception2Update.setLastModified(exception2.getLastModified());
        exception2Update.setStartDate(CalendarUtils.add(exceptionStart, Calendar.HOUR, 2));
        exception2Update.setEndDate(CalendarUtils.add(exceptionEnd, Calendar.HOUR, 2));
        catm2.update(exception2Update);
        /*
         * as user a, w/o sync'ing again, do the "iOS housekeeping" and transform previously declined overridden instance to EXDATE
         */
        Property recurrenceIdProperty = exceptionComponent.getProperty("RECURRENCE-ID");
        iCalResource.getVEvent().setProperty("EXDATE", recurrenceIdProperty.getValue(), recurrenceIdProperty.getAttributes());
        iCalResource.getVCalendar().getComponents().remove(exceptionComponent);
        String scheduleTag = iCalResource.getScheduleTag();
        assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, putICalUpdate(getDefaultFolderID(), uid, iCalResource.toString(), null, scheduleTag), "response code wrong");
        /*
         * verify participation status in exception as user b
         */
        exception2 = catm2.get(catm2.getPrivateFolder(), appointment.getObjectID(), 3);
        assertNotNull(exception2);
        assertNotNull(exception2.getUsers());
        for (UserParticipant participant : exception2.getUsers()) {
            if (catm.getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.NONE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * get & check appointment via caldav as user a
         */
        iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs");
        attendeePropertySeries = iCalResource.getVEvent().getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeePropertySeries, "Attendee not found");
        assertEquals("ACCEPTED", attendeePropertySeries.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        attendeePropertyException = iCalResource.getVEvents().get(1).getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeePropertyException, "Attendee not found");
        assertEquals("NEEDS-ACTION", attendeePropertyException.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        assertNotEquals(scheduleTag, iCalResource.getScheduleTag());
    }

}
