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
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.AllRequest;
import com.openexchange.ajax.appointment.action.AppointmentInsertResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.groupware.container.Appointment;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug16579Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug16579Test extends AbstractAJAXSession {

    private Appointment appointment;

    private Appointment updateAppointment;

    /**
     * Initializes a new {@link Bug16579Test}.
     *
     * @param name
     */
    public Bug16579Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        appointment = new Appointment();
        appointment.setTitle("Bug 16579 Test");
        appointment.setStartDate(D("02.08.2010 08:00"));
        appointment.setEndDate(D("02.08.2010 09:00"));
        appointment.setRecurrenceType(Appointment.WEEKLY);
        appointment.setDays(Appointment.MONDAY);
        appointment.setInterval(1);
        appointment.setUntil(D("23.08.2010 00:00"));
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setIgnoreConflicts(true);
        InsertRequest request = new InsertRequest(appointment, getClient().getValues().getTimeZone());
        AppointmentInsertResponse insertResponse = getClient().execute(request);
        insertResponse.fillObject(appointment);

        updateAppointment = new Appointment();
        insertResponse.fillObject(updateAppointment);
        updateAppointment.setParentFolderID(appointment.getParentFolderID());
        updateAppointment.setRecurrenceType(Appointment.WEEKLY);
        updateAppointment.setDays(Appointment.MONDAY);
        updateAppointment.setInterval(1);
        //updateAppointment.setOccurrence(8);
        updateAppointment.setUntil(D("20.09.2010 00:00"));
        updateAppointment.setIgnoreConflicts(true);
    }

    @Test
    public void testBug16579() throws Exception {
        UpdateResponse updateResponse = getClient().execute(new UpdateRequest(updateAppointment, getClient().getValues().getTimeZone()));
        appointment.setLastModified(updateResponse.getTimestamp());

        int[] columns = new int[] { Appointment.OBJECT_ID, Appointment.START_DATE, Appointment.END_DATE };
        AllRequest allRequest = new AllRequest(getClient().getValues().getPrivateAppointmentFolder(), columns, D("01.09.2010 00:00"), D("01.10.2010 00:00"), getClient().getValues().getTimeZone(), false);
        CommonAllResponse allResponse = getClient().execute(allRequest);
        JSONArray json = (JSONArray) allResponse.getData();
        int count = 0;
        for (int i = 0; i < json.length(); i++) {
            if (json.getJSONArray(i).getInt(0) == appointment.getObjectID()) {
                count++;
            }
        }

        assertEquals(3, count, "Wrong amount of occurrences");
    }

}
