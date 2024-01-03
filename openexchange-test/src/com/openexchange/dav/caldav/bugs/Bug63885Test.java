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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug63885Test} - "New appointment" notification for declined appointment
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.2
 */
public class Bug63885Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager catm2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        catm2 = new CalendarTestManager(client2);
        catm2.setFailOnError(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAcknowledgeAlarmOfDeletedEvent(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on server with alarm
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("next week in the morning", TimeZone.getTimeZone("Europe/Zurich")));
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setAlarm(15);
        appointment.setTitle("Bug63885Test");
        appointment.setIgnoreConflicts(true);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(catm2.getClient().getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment.setParentFolderID(catm.getPrivateFolder());
        appointment = catm.insert(appointment);
        /*
         * decline appointment as user b
         */
        catm2.confirm(catm2.getPrivateFolder(), appointment.getObjectID(), catm.getLastModification(), Appointment.DECLINE, "no");
        /*
         * get appointment via caldav as user a
         */
        ICalResource iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "Event alarm not found");
        assertNotNull(iCalResource.getScheduleTag(), "No schedule tag for calendar resource");
        /*
         * delete appointment on server
         */
        catm.delete(appointment);
        /*
         * try to update event via caldav (acknowledge alarm)
         */
        iCalResource.getVEvent().getVAlarm().setProperty("ACKNOWLEDGED", formatAsUTC(new Date()));
        int responseCode;
        PutMethod put = null;
        try {
            put = new PutMethod(getBaseUri() + iCalResource.getHref());
            put.addRequestHeader("If-Schedule-Tag-Match", iCalResource.getScheduleTag());
            put.setRequestEntity(new StringRequestEntity(iCalResource.toString(), "text/calendar", null));
            responseCode = webDAVClient.executeMethod(put);
        } finally {
            release(put);
        }
        assertEquals(HttpServletResponse.SC_NOT_FOUND, responseCode);
    }

}
