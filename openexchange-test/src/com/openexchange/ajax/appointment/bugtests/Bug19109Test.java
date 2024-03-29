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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
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
 * {@link Bug19109Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug19109Test extends AbstractAJAXSession {

    private Appointment appointment;
    private Appointment updateAppointment;

    public Bug19109Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        appointment = new Appointment();
        appointment.setStartDate(D("15.08.2011 08:00"));
        appointment.setEndDate(D("15.08.2011 09:00"));
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setOccurrence(4);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setTitle("Bug 19109 Test");
        appointment.setIgnoreConflicts(true);

        InsertRequest insertRequest = new InsertRequest(appointment, getClient().getValues().getTimeZone());
        AppointmentInsertResponse insertResponse = getClient().execute(insertRequest);
        insertResponse.fillAppointment(appointment);

        updateAppointment = new Appointment();
        updateAppointment.setObjectID(appointment.getObjectID());
        updateAppointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        updateAppointment.setStartDate(D("15.08.2011 06:00"));
        updateAppointment.setEndDate(D("15.08.2011 07:00"));
        updateAppointment.setRecurrenceType(Appointment.DAILY);
        updateAppointment.setInterval(1);
        updateAppointment.setOccurrence(4);
        updateAppointment.setIgnoreConflicts(true);
        updateAppointment.setLastModified(new Date(Long.MAX_VALUE));
    }

    @Test
    public void testBug19109() throws Exception {
        UpdateRequest updateRequest = new UpdateRequest(updateAppointment, getClient().getValues().getTimeZone());
        UpdateResponse updateResponse = getClient().execute(updateRequest);
        updateResponse.fillObject(appointment);

        AllRequest allRequest = new AllRequest(getClient().getValues().getPrivateAppointmentFolder(), new int[] { Appointment.OBJECT_ID }, new Date(1313452800000L), new Date(1313539200000L), getClient().getValues().getTimeZone());
        CommonAllResponse allResponse = getClient().execute(allRequest);
        assertTrue(allResponse.getArray().length > 0, "Expected a result.");

        boolean found = false;
        Object[][] objects = allResponse.getArray();
        for (Object[] object : objects) {
            if (((Integer) object[0]).intValue() == appointment.getObjectID()) {
                found = true;
            }
        }

        assertTrue(found, "Expected Appointment Occurrence");
    }
}
