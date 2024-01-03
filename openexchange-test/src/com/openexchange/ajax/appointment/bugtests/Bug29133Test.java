
package com.openexchange.ajax.appointment.bugtests;

import static com.openexchange.test.common.groupware.calendar.TimeTools.D;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug29133Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug29133Test extends AbstractAJAXSession {

    private CalendarTestManager ctmB;

    private Appointment appointmentA;

    private Appointment appointmentB;

    private AJAXClient clientB;

    public Bug29133Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        clientB = testUser2.getAjaxClient();
        ctmB = new CalendarTestManager(clientB);

        appointmentA = new Appointment();
        appointmentA.setTitle("Bug 29133 Test");
        appointmentA.setStartDate(D("14.10.2013 08:00"));
        appointmentA.setEndDate(D("14.10.2013 09:00"));
        appointmentA.setRecurrenceType(Appointment.WEEKLY);
        appointmentA.setDays(Appointment.TUESDAY);
        appointmentA.setInterval(1);
        appointmentA.setIgnoreConflicts(true);
        appointmentA.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointmentA.setUsers(new UserParticipant[] { new UserParticipant(getClient().getValues().getUserId()), new UserParticipant(clientB.getValues().getUserId()) });
        appointmentA.setParticipants(new Participant[] { new UserParticipant(getClient().getValues().getUserId()), new UserParticipant(clientB.getValues().getUserId()) });

        catm.insert(appointmentA);
        appointmentB = catm.createIdentifyingCopy(appointmentA);
        appointmentB.setParentFolderID(clientB.getValues().getPrivateAppointmentFolder());
    }

    @Test
    public void testBug29133() throws Exception {
        ctmB.confirm(appointmentB, Appointment.ACCEPT, "message");
        checkStatus();
        catm.confirm(appointmentA, Appointment.ACCEPT, "message");
        checkStatus();
        Appointment update = catm.createIdentifyingCopy(appointmentA);
        update.setAlarm(30);
        update.setLastModified(new Date(Long.MAX_VALUE));
        catm.update(update);
        checkStatus();
    }

    private void checkStatus() throws Exception {
        Appointment load = catm.get(appointmentA);
        assertEquals(2, load.getUsers().length, "Wrong amount of users.");
        assertEquals(Appointment.ACCEPT, load.getUsers()[0].getConfirm(), "Wrong confirm status.");
        assertEquals(Appointment.ACCEPT, load.getUsers()[1].getConfirm(), "Wrong confirm status.");
    }
}
