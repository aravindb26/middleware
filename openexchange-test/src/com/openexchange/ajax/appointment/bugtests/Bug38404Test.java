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

package com.openexchange.ajax.appointment.bugtests;

import static com.openexchange.test.common.groupware.calendar.TimeTools.D;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.java.util.TimeZones;
import com.openexchange.test.CalendarTestManager;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug38404Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.8.0
 */
public class Bug38404Test extends AbstractAJAXSession {

    private CalendarTestManager ctm2;
    private Appointment appointment;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        ctm2 = new CalendarTestManager(testUser2.getAjaxClient());

        appointment = new Appointment();
        appointment.setTitle("Bug 38404");
        appointment.setStartDate(D("10.08.2015 08:00"));
        appointment.setEndDate(D("10.08.2015 09:00"));
        UserParticipant user1 = new UserParticipant(getClient().getValues().getUserId());
        UserParticipant user2 = new UserParticipant(testUser2.getAjaxClient().getValues().getUserId());
        appointment.setParticipants(new Participant[] { user1, user2 });
        appointment.setUsers(new UserParticipant[] { user1, user2 });
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setIgnoreConflicts(true);
    }

    @Test
    public void testBug38404() throws Exception {
        catm.insert(appointment);
        appointment.setRecurrenceType(Appointment.WEEKLY);
        appointment.setDays(Appointment.TUESDAY + Appointment.WEDNESDAY + Appointment.THURSDAY);
        appointment.setInterval(2);
        catm.update(appointment);
        appointment.setParentFolderID(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder());
        ctm2.confirm(appointment, Appointment.ACCEPT, "yes", 1);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        Appointment loaded = catm.get(appointment);
        assertNotNull(loaded.getChangeException(), "Missing change exception.");
        assertEquals(1, loaded.getChangeException().length, "Wrong amount of change exceptions.");
        assertEquals(2, loaded.getUsers().length, "Wrong amount of participants.");
        for (UserParticipant up : loaded.getUsers()) {
            if (up.getIdentifier() == getClient().getValues().getUserId()) {
                assertEquals(Appointment.ACCEPT, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            } else if (up.getIdentifier() == testUser2.getAjaxClient().getValues().getUserId()) {
                assertEquals(Appointment.NONE, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            }
        }

        Appointment[] all = ctm2.all(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder(), D("11.08.2015 00:00", TimeZones.UTC), D("12.08.2015 00:00", TimeZones.UTC));
        int exceptionId = 0;
        for (Appointment app : all) {
            if (app.getRecurrenceID() == appointment.getObjectID() && app.getRecurrenceID() != app.getObjectID()) {
                exceptionId = app.getObjectID();
                break;
            }
        }
        assertFalse(exceptionId == 0, "Unable to find exception.");
        Appointment loadedException = ctm2.get(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder(), exceptionId);
        assertEquals(2, loadedException.getUsers().length, "Wrong amount of participants.");
        for (UserParticipant up : loadedException.getUsers()) {
            if (up.getIdentifier() == getClient().getValues().getUserId()) {
                assertEquals(Appointment.ACCEPT, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            } else if (up.getIdentifier() == testUser2.getAjaxClient().getValues().getUserId()) {
                assertEquals(Appointment.ACCEPT, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            }
        }

        Appointment updateAlarm = new Appointment();
        updateAlarm.setObjectID(appointment.getObjectID());
        updateAlarm.setParentFolderID(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder());
        updateAlarm.setAlarm(15);
        updateAlarm.setLastModified(new Date(Long.MAX_VALUE));
        ctm2.update(updateAlarm);

        loaded = catm.get(appointment);
        assertNotNull(loaded.getChangeException(), "Missing change exception.");
        assertEquals(1, loaded.getChangeException().length, "Wrong amount of change exceptions.");
        assertEquals(2, loaded.getUsers().length, "Wrong amount of participants.");
        for (UserParticipant up : loaded.getUsers()) {
            if (up.getIdentifier() == getClient().getValues().getUserId()) {
                assertEquals(Appointment.ACCEPT, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            } else if (up.getIdentifier() == testUser2.getAjaxClient().getValues().getUserId()) {
                assertEquals(Appointment.NONE, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            }
        }

        loadedException = ctm2.get(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder(), exceptionId);
        assertEquals(2, loadedException.getUsers().length, "Wrong amount of participants.");
        for (UserParticipant up : loadedException.getUsers()) {
            if (up.getIdentifier() == getClient().getValues().getUserId()) {
                assertEquals(Appointment.ACCEPT, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            } else if (up.getIdentifier() == testUser2.getAjaxClient().getValues().getUserId()) {
                assertEquals(Appointment.ACCEPT, up.getConfirm(), "Wrong confirmation status for user: " + up.getIdentifier());
            }
        }
    }

    @Test
    public void testSomethingElse() throws Exception {
        appointment.setRecurrenceType(Appointment.WEEKLY);
        appointment.setDays(Appointment.TUESDAY + Appointment.WEDNESDAY + Appointment.THURSDAY);
        appointment.setInterval(2);
        catm.insert(appointment);
        Appointment loaded = catm.get(appointment);
        assertEquals(D("11.08.2015 08:00"), loaded.getStartDate(), "Wrong start.");
        assertEquals(D("11.08.2015 09:00"), loaded.getEndDate(), "Wrong end.");
    }
}
