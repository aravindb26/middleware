
package com.openexchange.ajax.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.AppointmentTest;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.FolderTestManager;
import org.junit.jupiter.api.TestInfo;

public class ConfirmTest extends AppointmentTest {

    private CalendarTestManager ctm2;
    private FolderTestManager ftm2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        ctm2 = new CalendarTestManager(testUser2.getAjaxClient());
        ftm2 = new FolderTestManager(testUser2.getAjaxClient());
    }

    @Test
    public void testConfirm() throws Exception {

        FolderObject user2CalendarFolder = ftm2.getFolderFromServer(testUser2.getAjaxClient().getValues().getPrivateAppointmentFolder());
        final int secondUserId = testUser2.getUserId();

        Appointment appointmentObj = createAppointmentObject("testConfirm");
        final com.openexchange.groupware.container.Participant[] participants = new com.openexchange.groupware.container.Participant[2];
        participants[0] = new UserParticipant(userId);
        participants[1] = new UserParticipant(secondUserId);
        appointmentObj.setParticipants(participants);
        appointmentObj.setIgnoreConflicts(true);

        final int objectId = catm.insert(appointmentObj).getObjectID();

        appointmentObj = ctm2.get(user2CalendarFolder.getObjectID(), objectId);
        ctm2.confirm(appointmentObj, Appointment.ACCEPT, "Yap.");

        final Appointment loadAppointment = catm.get(appointmentFolderId, objectId);

        boolean found = false;

        final UserParticipant[] users = loadAppointment.getUsers();
        for (UserParticipant user : users) {
            if (user.getIdentifier() == secondUserId) {
                found = true;
                assertEquals(Appointment.ACCEPT, user.getConfirm(), "wrong confirm status");
            }
        }

        assertTrue(found, "user participant with id " + secondUserId + " not found");
    }
}
