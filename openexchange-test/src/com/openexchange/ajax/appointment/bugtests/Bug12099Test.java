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

import static com.openexchange.ajax.folder.Create.ocl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.DeleteRequest;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.folder.Create;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.ajax.participant.ParticipantTools;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.common.groupware.calendar.TimeTools;

/**
 * Checks if series gets changed_from set to 0.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Bug12099Test extends AbstractAJAXSession {

    /**
     * Default constructor.
     * 
     * @param name test name.
     */
    public Bug12099Test() {
        super();
    }
    
    /**
     * Creates a series appointment. Deletes one occurrence and checks if series
     * then has the changed_from set to 0.
     */
    @Test
    public void testSeriesChangedFromIsZero() throws Throwable {
        final AJAXClient myClient = getClient();
        final int folderId = myClient.getValues().getPrivateAppointmentFolder();
        final TimeZone tz = myClient.getValues().getTimeZone();
        final Appointment series = new Appointment();
        {
            series.setTitle("Bug 12099 test");
            series.setParentFolderID(folderId);
            series.setIgnoreConflicts(true);
            // Start and end date.
            final Calendar calendar = new GregorianCalendar(tz);
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            series.setStartDate(calendar.getTime());
            calendar.add(Calendar.HOUR, 1);
            series.setEndDate(calendar.getTime());
            // Configure daily series with 2 occurences
            series.setRecurrenceType(Appointment.DAILY);
            series.setInterval(1);
            series.setOccurrence(2);
        }
        {
            final InsertRequest request = new InsertRequest(series, tz);
            final CommonInsertResponse response = myClient.execute(request);
            series.setObjectID(response.getId());
            series.setLastModified(response.getTimestamp());
        }
        try {
            {
                final DeleteRequest request = new DeleteRequest(series.getObjectID(), folderId, 1, series.getLastModified());
                final CommonDeleteResponse response = myClient.execute(request);
                series.setLastModified(response.getTimestamp());
            }
            {
                final GetRequest request = new GetRequest(folderId, series.getObjectID());
                final GetResponse response = myClient.execute(request);
                series.setLastModified(response.getTimestamp());
                final Appointment test = response.getAppointment(tz);
                assertEquals(myClient.getValues().getUserId(), test.getModifiedBy(), "Editor of appointment series must not be 0.");
            }
        } finally {
            myClient.execute(new DeleteRequest(series.getObjectID(), folderId, series.getLastModified()));
        }
    }

    /**
     * A shares his calendar to B with create rights. B creates a series
     * appointment there with C as participant. C deletes an occurrence of that
     * series appointment. A verifies that changed_from of the series is not
     * zero.
     */
    @Test
    public void testSeriesChangedFromIsZero2() throws Throwable {
        final AJAXClient clientA = getClient();
        final int userIdA = clientA.getValues().getUserId();
        final AJAXClient clientB = testUser2.getAjaxClient();
        final FolderObject folder = Create.folder(FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "Folder to test bug 12099 - " + UUID.randomUUID().toString(), FolderObject.CALENDAR, FolderObject.PRIVATE, ocl(userIdA, false, true, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION), ocl(clientB.getValues().getUserId(), false, false, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION));
        {
            final CommonInsertResponse response = clientA.execute(new com.openexchange.ajax.folder.actions.InsertRequest(EnumAPI.OX_OLD, folder));
            response.fillObject(folder);
        }
        final Appointment appointment = new Appointment();
        try {
            final AJAXClient clientC = testContext.acquireUser().getAjaxClient();
            final int userIdC = clientC.getValues().getUserId();
            final TimeZone tzB = clientB.getValues().getTimeZone();
            {
                appointment.setTitle("Test for bug 12099");
                appointment.setParentFolderID(folder.getObjectID());
                appointment.setStartDate(new Date(TimeTools.getHour(0, tzB)));
                appointment.setEndDate(new Date(TimeTools.getHour(1, tzB)));
                appointment.setRecurrenceType(CalendarObject.DAILY);
                appointment.setInterval(1);
                appointment.setOccurrence(3);
                appointment.setParticipants(ParticipantTools.createParticipants(userIdA, userIdC));
                appointment.setIgnoreConflicts(true);
                final InsertRequest request = new InsertRequest(appointment, tzB);
                final CommonInsertResponse response = clientB.execute(request);
                response.fillObject(appointment);
            }
            {
                final int calendarFolderC = clientC.getValues().getPrivateAppointmentFolder();
                final DeleteRequest request = new DeleteRequest(appointment.getObjectID(), calendarFolderC, 2, appointment.getLastModified());
                final CommonDeleteResponse response = clientC.execute(request);
                appointment.setLastModified(response.getTimestamp());
            }
            {
                final GetRequest request = new GetRequest(folder.getObjectID(), appointment.getObjectID());
                final GetResponse response = clientB.execute(request);
                final Appointment test = response.getAppointment(tzB);
                assertEquals(userIdC, test.getModifiedBy(), "Appointment modified badly updated.");
            }
        } finally {
            //            if (null != appointment.getLastModified()) {
            //                clientB.execute(new DeleteRequest(appointment.getObjectID(),
            //                    folder.getObjectID(), appointment.getLastModified()));
            //            }
            clientA.execute(new com.openexchange.ajax.folder.actions.DeleteRequest(EnumAPI.OX_OLD, folder.getObjectID(), folder.getLastModified()));
        }
    }
}
