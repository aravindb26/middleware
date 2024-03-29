
package com.openexchange.ajax.appointment.bugtests;

import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.FolderTestManager;

public class Bug4541Test extends AppointmentTest {

    @Test
    public void testBug4541() throws Exception {
        final FolderObject folderObj = new FolderObject();
        folderObj.setFolderName("testBug4541" + System.currentTimeMillis());
        folderObj.setParentFolderID(FolderObject.PRIVATE);
        folderObj.setModule(FolderObject.CALENDAR);
        folderObj.setType(FolderObject.PRIVATE);

        final OCLPermission[] permission = new OCLPermission[] { FolderTestManager.createPermission(userId, false, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION),
        };

        folderObj.setPermissionsAsArray(permission);

        final int newFolderId = ftm.insertFolderOnServer(folderObj).getObjectID();

        final Appointment appointmentObj = createAppointmentObject("testBug4541");
        appointmentObj.setParentFolderID(newFolderId);
        appointmentObj.setIgnoreConflicts(true);

        final com.openexchange.groupware.container.Participant[] participants = new com.openexchange.groupware.container.Participant[1];
        participants[0] = new UserParticipant(userId);

        appointmentObj.setParticipants(participants);

        final int objectId = catm.insert(appointmentObj).getObjectID();
        appointmentObj.setObjectID(objectId);

        Appointment loadAppointment = catm.get(newFolderId, objectId);
        compareObject(appointmentObj, loadAppointment, appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate().getTime());

        appointmentObj.setTitle("testBug4541 - update");
        appointmentObj.removeParentFolderID();

        catm.update(newFolderId, appointmentObj);
        appointmentObj.setParentFolderID(newFolderId);

        loadAppointment = catm.get(newFolderId, objectId);
        compareObject(appointmentObj, loadAppointment, appointmentObj.getStartDate().getTime(), appointmentObj.getEndDate().getTime());
    }
}
