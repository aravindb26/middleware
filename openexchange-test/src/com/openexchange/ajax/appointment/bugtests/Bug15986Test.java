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

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.AppointmentInsertResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug15986Test}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Bug15986Test extends AbstractAJAXSession {

    private Appointment appointment;
    private TimeZone timeZone;

    public Bug15986Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        timeZone = getClient().getValues().getTimeZone();
        appointment = new Appointment();
        appointment.setTitle("Appointment for bug 15986");
        appointment.setIgnoreConflicts(true);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        Calendar calendar = TimeTools.createCalendar(timeZone);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR, 1);
        appointment.setEndDate(calendar.getTime());
        InsertRequest request = new InsertRequest(appointment, timeZone);
        AppointmentInsertResponse response = getClient().execute(request);
        response.fillAppointment(appointment);
    }

    @Test
    public void testForNullPointerException() throws Throwable {
        Appointment changed = new Appointment();
        changed.setObjectID(appointment.getObjectID());
        changed.setParentFolderID(appointment.getParentFolderID());
        changed.setLastModified(appointment.getLastModified());
        changed.setNote("Beschreibung");
        changed.setShownAs(Appointment.TEMPORARY);
        UpdateRequest request = new UpdateRequest(changed, timeZone);
        UpdateResponse response = getClient().execute(request);
        assertFalse(response.hasError());
        response.fillObject(appointment);
    }
}
