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

package com.openexchange.ajax.appointment;

import com.openexchange.ajax.participant.ParticipantTools;
import com.openexchange.groupware.container.Appointment;

/**
 * {@link AppointmentTools}. Utility class that contains all help methods for comparing
 * appointment objects
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class AppointmentTools extends ParticipantTools {

    /**
     * Compares the specified objects
     *
     * @param appointmentObj1 The expected {@link Appointment}
     * @param appointmentObj2 The actual {@link Appointment}
     */
    public static void compareObject(final Appointment appointmentObj1, final Appointment appointmentObj2) {
        assertEquals(appointmentObj1.getObjectID(), appointmentObj2.getObjectID(), "id is not equals");
        assertEqualsAndNotNull(appointmentObj1.getTitle(), appointmentObj2.getTitle(), "title is not equals");
        assertEqualsAndNotNull(appointmentObj1.getStartDate(), appointmentObj2.getStartDate(), "start is not equals");
        assertEqualsAndNotNull(appointmentObj1.getEndDate(), appointmentObj2.getEndDate(), "end is not equals");
        assertEqualsAndNotNull(appointmentObj1.getLocation(), appointmentObj2.getLocation(), "location is not equals");
        assertEquals(appointmentObj1.getShownAs(), appointmentObj2.getShownAs(), "shown_as is not equals");
        assertEquals(appointmentObj1.getParentFolderID(), appointmentObj2.getParentFolderID(),"folder id is not equals");
        assertTrue(appointmentObj1.getPrivateFlag() == appointmentObj2.getPrivateFlag(), "private flag is not equals");
        assertTrue(appointmentObj1.getFullTime() == appointmentObj2.getFullTime(), "full time is not equals");
        assertEquals(appointmentObj1.getLabel(), appointmentObj2.getLabel(), "label is not equals");
        assertEquals(appointmentObj1.getAlarm(), appointmentObj2.getAlarm(), "alarm is not equals");
        assertTrue(appointmentObj1.getAlarmFlag() == appointmentObj2.getAlarmFlag(), "alarm flag is not equals");
        assertEquals(appointmentObj1.getRecurrenceType(), appointmentObj2.getRecurrenceType(), "recurrence_type");
        assertEquals(appointmentObj1.getInterval(), appointmentObj2.getInterval(), "interval");
        assertEquals(appointmentObj1.getDays(), appointmentObj2.getDays(), "days");
        assertEquals(appointmentObj1.getMonth(), appointmentObj2.getMonth(), "month");
        assertEquals(appointmentObj1.getDayInMonth(), appointmentObj2.getDayInMonth(), "day_in_month");
        assertEquals(appointmentObj1.getUntil(), appointmentObj2.getUntil(), "until");
        assertEqualsAndNotNull(appointmentObj1.getNote(), appointmentObj2.getNote(), "note is not equals");
        assertEqualsAndNotNull(appointmentObj1.getCategories(), appointmentObj2.getCategories(), "categories is not equals");
        assertEqualsAndNotNull(appointmentObj1.getDeleteException(), appointmentObj2.getDeleteException(), "delete exception is not equals");

        assertEqualsAndNotNull(participants2String(appointmentObj1.getParticipants()), participants2String(appointmentObj2.getParticipants()), "participants are not equals");
        assertEqualsAndNotNull(users2String(appointmentObj1.getUsers()), users2String(appointmentObj2.getUsers()), "users are not equals");
    }
}
