
package com.openexchange.ajax.appointment.recurrence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.action.GetContactForUserRequest;
import com.openexchange.ajax.contact.action.GetResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;

public class AppointmentParticipantsShouldBecomeUsersIfPossible extends ManagedAppointmentTest {

    @Test
    public void testExternalParticipantBecomesUserParticipantIfAddressMatches() throws Exception {
        AJAXClient client2 = testUser2.getAjaxClient();
        int user2id = client2.getValues().getUserId();
        GetResponse response = client2.execute(new GetContactForUserRequest(user2id, true, TimeZone.getDefault()));
        String user2email = response.getContact().getEmail1();

        Appointment appointment = generateDailyAppointment();
        appointment.addParticipant(new ExternalUserParticipant(user2email));
        appointment.addParticipant(new UserParticipant(user2id));

        catm.insert(appointment);
        Appointment actual = catm.get(appointment);

        boolean foundAsExternal = false, foundAsInternal = false;

        Participant[] participants = actual.getParticipants();
        for (Participant participant : participants) {
            if (participant.getType() == Participant.EXTERNAL_USER) {
                if (participant.getEmailAddress().equals(user2email)) {
                    foundAsExternal = true;
                }
            }
            if (participant.getType() == Participant.USER) {
                if (participant.getIdentifier() == user2id) {
                    foundAsInternal = true;
                }
            }
        }
        assertFalse(foundAsExternal, "Should not find user listed as external participant");
        assertTrue(foundAsInternal, "Should find user listed as internal participant");
    }

    @Test
    public void testExternalParticipantIsRemovedIfAddressMatchesUserParticipant() throws Exception {
        GetResponse response = getClient().execute(new GetContactForUserRequest(userId, true, TimeZone.getDefault()));
        String user1email = response.getContact().getEmail1();

        Appointment appointment = generateDailyAppointment();
        appointment.addParticipant(new ExternalUserParticipant(user1email));

        catm.insert(appointment);
        Appointment actual = catm.get(appointment);

        boolean foundAsExternal = false;
        int foundAsInternal = 0;

        Participant[] participants = actual.getParticipants();
        for (Participant participant : participants) {
            if (participant.getType() == Participant.EXTERNAL_USER) {
                if (participant.getEmailAddress().equals(user1email)) {
                    foundAsExternal = true;
                }
            }
            if (participant.getType() == Participant.USER) {
                if (participant.getIdentifier() == userId) {
                    foundAsInternal++;
                }
            }
        }
        assertFalse(foundAsExternal, "Should not find creator listed as external participant");
        assertEquals(1, foundAsInternal, "Should find creator listed as internal participant once and only once");
    }

    @Test
    public void testExternalParticipantBecomesUserParticipantIfAddressMatchesAfterUpdateToo() throws Exception {
        int user2id = testUser2.getUserId();
        String user2email = testUser2.getLogin();

        Appointment appointment = generateDailyAppointment();
        Appointment result = catm.insert(appointment);

        Appointment update = new Appointment();
        update.setLastModified(result.getLastModified());
        update.setObjectID(result.getObjectID());
        update.setParentFolderID(result.getParentFolderID());
        update.addParticipant(new UserParticipant(testUser.getUserId()));
        update.addParticipant(new UserParticipant(user2id));
        update.addParticipant(new ExternalUserParticipant(user2email));

        catm.update(update);

        Appointment actual = catm.get(appointment);

        boolean foundAsExternal = false, foundAsInternal = false;

        Participant[] participants = actual.getParticipants();
        for (Participant participant : participants) {
            if (participant.getType() == Participant.EXTERNAL_USER) {
                if (participant.getEmailAddress().equals(user2email)) {
                    foundAsExternal = true;
                }
            }
            if (participant.getType() == Participant.USER) {
                if (participant.getIdentifier() == user2id) {
                    foundAsInternal = true;
                }
            }
        }
        assertFalse(foundAsExternal, "Should not find user listed as external participant");
        assertTrue(foundAsInternal, "Should find user listed as internal participant");
    }

}
