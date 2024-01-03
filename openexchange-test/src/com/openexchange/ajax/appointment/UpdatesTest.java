
package com.openexchange.ajax.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.ajax.framework.AbstractUpdatesRequest.Ignore;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.container.FolderChildObject;

public class UpdatesTest extends AppointmentTest {

    private final static int[] _appointmentFields = { DataObject.OBJECT_ID, DataObject.CREATED_BY, DataObject.CREATION_DATE, DataObject.LAST_MODIFIED, DataObject.LAST_MODIFIED_UTC, DataObject.MODIFIED_BY, FolderChildObject.FOLDER_ID, CommonObject.PRIVATE_FLAG, CommonObject.CATEGORIES, CalendarObject.TITLE, Appointment.LOCATION, CalendarObject.START_DATE, CalendarObject.END_DATE, CalendarObject.NOTE, CalendarObject.RECURRENCE_TYPE, CalendarObject.INTERVAL, CalendarObject.RECURRENCE_COUNT, CalendarObject.PARTICIPANTS, CalendarObject.USERS, CalendarObject.ALARM, CalendarObject.NOTIFICATION, Appointment.SHOWN_AS, Appointment.FULL_TIME, Appointment.COLOR_LABEL, Appointment.TIMEZONE, Appointment.RECURRENCE_START };

    @Test
    public void testModified() {
        catm.updates(appointmentFolderId, _appointmentFields, new Date(System.currentTimeMillis() - (dayInMillis * 7)), false);
        assertFalse(catm.getLastResponse().hasError());
    }

    @Test
    public void testDeleted() {
        catm.updates(appointmentFolderId, _appointmentFields, new Date(System.currentTimeMillis() - (dayInMillis * 7)), false, Ignore.CHANGED, null, null);
        assertFalse(catm.getLastResponse().hasError());
    }

    @Test
    public void testModifiedWithoutFolderId() throws Exception {
        final Date start = new Date(System.currentTimeMillis() - (7 * dayInMillis));
        final Date end = new Date(System.currentTimeMillis() + (7 * dayInMillis));

        final Appointment appointmentObj = createAppointmentObject("testModifiedWithoutFolderId");
        appointmentObj.setIgnoreConflicts(true);
        final int objectId = catm.insert(appointmentObj).getObjectID();

        final Appointment loadAppointment = catm.get(appointmentFolderId, objectId);
        final Date modified = loadAppointment.getLastModified();

        final List<Appointment> appointmentArray = catm.updates(appointmentFolderId, _appointmentFields, decrementDate(modified), false, Ignore.DELETED, start, end);

        assertTrue(appointmentArray.size() > 0, "no appointment object in response");
        boolean found = false;

        for (int a = 0; a < appointmentArray.size(); a++) {
            if (appointmentArray.get(a).getObjectID() == objectId) {
                found = true;
            }
        }

        assertTrue(found, "created object not found in response");
    }

    @Test
    public void testModifiedWithoutFolderIdExtended() throws Exception {
        final Date start = new Date(System.currentTimeMillis() - (7 * dayInMillis));
        final Date end = new Date(System.currentTimeMillis() + (7 * dayInMillis));

        Appointment appointmentObj = createAppointmentObject("testModifiedWithoutFolderIdExtended");
        appointmentObj.setIgnoreConflicts(true);
        final int objectId1 = catm.insert(appointmentObj).getObjectID();

        Appointment loadAppointment = catm.get(appointmentFolderId, objectId1);
        Date modified = loadAppointment.getLastModified();

        List<Appointment> appointmentArray = catm.updates(appointmentFolderId, _appointmentFields, decrementDate(modified), false, Ignore.DELETED, start, end);

        assertTrue(appointmentArray.size() > 0, "no appointment object in response");
        boolean found1 = false;

        for (int a = 0; a < appointmentArray.size(); a++) {
            if (appointmentArray.get(a).getObjectID() == objectId1) {
                found1 = true;
            }
        }
        assertTrue(found1, "created object not found in response");

        appointmentObj = createAppointmentObject("testModifiedWithoutFolderIdExtended");
        appointmentObj.setIgnoreConflicts(true);

        final int objectId2 = catm.insert(appointmentObj).getObjectID();

        loadAppointment = catm.get(appointmentFolderId, objectId2);
        modified = loadAppointment.getLastModified();

        appointmentArray = catm.updates(appointmentFolderId, _appointmentFields, decrementDate(modified), false, Ignore.DELETED, start, end);

        assertTrue(appointmentArray.size() > 0, "no appointment object in response");
        found1 = false;
        boolean found2 = false;

        for (int a = 0; a < appointmentArray.size(); a++) {
            if (appointmentArray.get(a).getObjectID() == objectId1) {
                found1 = true;
            } else if (appointmentArray.get(a).getObjectID() == objectId2) {
                found2 = true;
            }
        }

        assertFalse(found1, "invalid object id in reponse");
        assertTrue(found2, "created object not found in response");
    }

    @Test
    public void testModifiedWithoutFolderIdWithFutureTimestamp() throws Exception {
        final Date start = new Date(System.currentTimeMillis() - (7 * dayInMillis));
        final Date end = new Date(System.currentTimeMillis() + (7 * dayInMillis));

        final Appointment appointmentObj = createAppointmentObject("testModifiedWithoutFolderIdWithFutureTimestamp");
        appointmentObj.setIgnoreConflicts(true);
        final int objectId = catm.insert(appointmentObj).getObjectID();

        final Appointment loadAppointment = catm.get(appointmentFolderId, objectId);
        final Date modified = new Date(loadAppointment.getLastModified().getTime() + (7 * dayInMillis));

        final List<Appointment> appointmentArray = catm.updates(appointmentFolderId, _appointmentFields, modified, false, Ignore.DELETED, start, end);

        assertEquals(0, appointmentArray.size(), "unexpected data in response");
    }

    @Test
    public void testModifiedRecurrenceAppointment() {
        final Date start = new Date(System.currentTimeMillis() - (7 * dayInMillis));
        final Date end = new Date(System.currentTimeMillis() + (7 * dayInMillis));

        final Appointment appointmentObj = createAppointmentObject("testModifiedRecurrenceAppointment");
        appointmentObj.setRecurrenceType(Appointment.DAILY);
        appointmentObj.setInterval(1);
        appointmentObj.setOccurrence(5);
        appointmentObj.setIgnoreConflicts(true);
        final int objectId = catm.insert(appointmentObj).getObjectID();

        appointmentObj.setObjectID(objectId);

        final List<Appointment> appointmentArray = catm.updates(appointmentFolderId, _appointmentFields, new Date(0), false, Ignore.DELETED, start, end);

        boolean found = false;

        for (int a = 0; a < appointmentArray.size(); a++) {
            if (objectId == appointmentArray.get(a).getObjectID()) {
                compareObject(appointmentObj, appointmentArray.get(a));
                found = true;
                break;
            }
        }

        assertTrue(found, "object with object_id: " + objectId + " not found in response");
    }

    private static Date decrementDate(final Date date) {
        return new Date(date.getTime() - 1);
    }

}
