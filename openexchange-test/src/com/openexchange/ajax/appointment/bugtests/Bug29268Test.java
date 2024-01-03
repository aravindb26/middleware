
package com.openexchange.ajax.appointment.bugtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug29268Test}
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug29268Test extends AbstractAJAXSession {

    private Appointment appointment;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        appointment = new Appointment();
        appointment.setTitle("Bug 29268 Test");
        Calendar start = TimeTools.createCalendar(getClient().getValues().getTimeZone());
        start.add(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 8);
        Calendar end = TimeTools.createCalendar(getClient().getValues().getTimeZone());
        end.add(Calendar.DAY_OF_MONTH, 1);
        end.set(Calendar.HOUR_OF_DAY, 9);
        appointment.setStartDate(start.getTime());
        appointment.setEndDate(end.getTime());
        appointment.setAlarm(0);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setIgnoreConflicts(true);
        catm.insert(appointment);
    }

    @Test
    public void testBug29268() throws Exception {
        Appointment getAppointment = catm.get(appointment);
        assertTrue(getAppointment.containsAlarm(), "Missing alarm value for get request.");
        assertEquals(0, getAppointment.getAlarm(), "Wrong alarm value for get request.");

        ListIDs listIDs = new ListIDs(appointment.getParentFolderID(), appointment.getObjectID());
        List<Appointment> listAppointment = catm.list(listIDs, new int[] { Appointment.ALARM });
        assertTrue(listAppointment.get(0).containsAlarm(), "Missing alarm value for list request.");
        assertEquals(0, listAppointment.get(0).getAlarm(), "Wrong alarm value for list request.");
    }

}
