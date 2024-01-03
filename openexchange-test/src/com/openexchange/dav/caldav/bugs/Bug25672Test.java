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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug25672Test}
 *
 * USER_INPUT "Unable to calculate given position. Seems to be a delete exception or outside range"
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug25672Test extends CalDAVTest {


    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateWithDeleteExceptions(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment series on server as user b
         */
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("last month in the morning", TimeZone.getTimeZone("Europe/Berlin")));
        Appointment appointment = new Appointment();
        appointment.setUid(randomUID());
        appointment.setTitle("Bug25672Test");
        appointment.setIgnoreConflicts(true);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(super.getAJAXClient().getValues().getUserId()));
        appointment.setParentFolderID(catm.getPrivateFolder());
        catm.insert(appointment);
        Date clientLastModified = catm.getLastModification();
        /*
         * create delete exception on server as user b
         */
        Appointment exception = new Appointment();
        exception.setTitle("Bug23167Test_edit");
        exception.setObjectID(appointment.getObjectID());
        exception.setRecurrencePosition(2);
        exception.setLastModified(clientLastModified);
        exception.setParentFolderID(appointment.getParentFolderID());
        catm.delete(exception);
        clientLastModified = catm.getLastModification();
        /*
         * verify appointment series on client as user a
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(appointment.getUid(), calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(appointment.getTitle(), iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvent().getExDates(), "No EXDATE in iCal found");
        assertEquals(1, iCalResource.getVEvent().getExDates().size(), "EXDATE wrong");
        /*
         * update appointment on client as user a
         */
        String editedTitle = appointment.getTitle() + "_edit";
        iCalResource.getVEvent().setSummary(editedTitle);
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server as user a
         */
        appointment = super.getAppointment(appointment.getUid());
        assertNotNull(appointment, "appointment not found on server");
        assertEquals(editedTitle, appointment.getTitle(), "title wrong");
    }

}
