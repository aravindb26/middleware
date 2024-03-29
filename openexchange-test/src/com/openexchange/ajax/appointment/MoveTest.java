
package com.openexchange.ajax.appointment;

import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.FolderTestManager;
import org.junit.jupiter.api.TestInfo;

public class MoveTest extends AppointmentTest {

    private int objectId;

    public MoveTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    @Test
    public void testMove2PrivateFolder() throws Exception {
        final Appointment appointmentObj = new Appointment();
        final String date = String.valueOf(System.currentTimeMillis());
        appointmentObj.setTitle("testMove2PrivateFolder" + date);
        appointmentObj.setStartDate(new Date(startTime));
        appointmentObj.setEndDate(new Date(endTime));
        appointmentObj.setOrganizer(getClient().getValues().getDefaultAddress());
        appointmentObj.setParentFolderID(appointmentFolderId);
        appointmentObj.setIgnoreConflicts(true);
        appointmentObj.setShownAs(Appointment.RESERVED);
        objectId = catm.insert(appointmentObj).getObjectID();

        final FolderObject folderObj = FolderTestManager.createNewFolderObject("testMove2PrivateFolder" + UUID.randomUUID().toString(), FolderObject.CALENDAR, FolderObject.PRIVATE, userId, 1);
        int targetFolder = ftm.insertFolderOnServer(folderObj).getObjectID();

        appointmentObj.setParentFolderID(targetFolder);
        catm.update(appointmentFolderId, appointmentObj);
        final Appointment loadAppointment = catm.get(targetFolder, objectId);
        appointmentObj.setObjectID(objectId);
        compareObject(appointmentObj, loadAppointment, appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate().getTime());
    }

    @Test
    public void testMove2PublicFolder() throws Exception {
        final Appointment appointmentObj = new Appointment();
        final String date = String.valueOf(System.currentTimeMillis());
        appointmentObj.setTitle("testMove2PublicFolder" + date);
        appointmentObj.setStartDate(new Date(startTime));
        appointmentObj.setEndDate(new Date(endTime));
        appointmentObj.setOrganizer(getClient().getValues().getDefaultAddress());
        appointmentObj.setParentFolderID(appointmentFolderId);
        appointmentObj.setIgnoreConflicts(true);
        appointmentObj.setShownAs(Appointment.RESERVED);
        objectId = catm.insert(appointmentObj).getObjectID();

        final FolderObject folderObj = FolderTestManager.createNewFolderObject("testMove2PublicFolder" + UUID.randomUUID().toString(), FolderObject.CALENDAR, FolderObject.PUBLIC, userId, 2);
        int targetFolder = ftm.insertFolderOnServer(folderObj).getObjectID();

        appointmentObj.setParentFolderID(targetFolder);
        catm.update(appointmentFolderId, appointmentObj);
        final Appointment loadAppointment = catm.get(targetFolder, objectId);
        appointmentObj.setObjectID(objectId);
        compareObject(appointmentObj, loadAppointment, appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate().getTime());
    }
}
