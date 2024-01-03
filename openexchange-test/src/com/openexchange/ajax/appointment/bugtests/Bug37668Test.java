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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug37668Test}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.8.0
 */
public class Bug37668Test extends AbstractAJAXSession {

    private Appointment appSimple;
    private TimeZone timeZone;
    private TimeZone utc = TimeZone.getTimeZone("UTC");
    private Appointment app23h;
    private Appointment app25h;
    private Appointment fulltime;
    private Appointment fulltime2days;

    /**
     * Initializes a new {@link Bug37668Test}.
     * 
     * @param name
     */
    public Bug37668Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        timeZone = getClient().getValues().getTimeZone();

        appSimple = new Appointment();
        appSimple.setStartDate(D("14.01.2015 16:00", timeZone));
        appSimple.setEndDate(D("14.01.2015 17:00", timeZone));
        appSimple.setRecurrenceType(Appointment.YEARLY);
        appSimple.setInterval(1);
        appSimple.setDayInMonth(22);
        appSimple.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appSimple.setIgnoreConflicts(true);
        appSimple.setTimezone(timeZone.getID());

        app23h = new Appointment();
        app23h.setStartDate(D("14.01.2015 16:00", timeZone));
        app23h.setEndDate(D("15.01.2015 15:00", timeZone));
        app23h.setRecurrenceType(Appointment.YEARLY);
        app23h.setInterval(1);
        app23h.setDayInMonth(22);
        app23h.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        app23h.setIgnoreConflicts(true);
        app23h.setTimezone(timeZone.getID());

        app25h = new Appointment();
        app25h.setStartDate(D("14.01.2015 16:00", timeZone));
        app25h.setEndDate(D("15.01.2015 17:00", timeZone));
        app25h.setRecurrenceType(Appointment.YEARLY);
        app25h.setInterval(1);
        app25h.setDayInMonth(22);
        app25h.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        app25h.setIgnoreConflicts(true);
        app25h.setTimezone(timeZone.getID());

        fulltime = new Appointment();
        fulltime.setStartDate(D("14.01.2015 16:00", timeZone));
        fulltime.setEndDate(D("14.01.2015 17:00", timeZone));
        fulltime.setRecurrenceType(Appointment.YEARLY);
        fulltime.setInterval(1);
        fulltime.setDayInMonth(22);
        fulltime.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        fulltime.setIgnoreConflicts(true);
        fulltime.setTimezone(timeZone.getID());
        fulltime.setFullTime(true);

        fulltime2days = new Appointment();
        fulltime2days.setStartDate(D("14.01.2015 10:00", timeZone));
        fulltime2days.setEndDate(D("16.01.2015 10:00", timeZone));
        fulltime2days.setRecurrenceType(Appointment.YEARLY);
        fulltime2days.setInterval(1);
        fulltime2days.setDayInMonth(22);
        fulltime2days.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        fulltime2days.setIgnoreConflicts(true);
        fulltime2days.setTimezone(timeZone.getID());
        fulltime2days.setFullTime(true);
    }

    @Test
    public void testBug37668_JAN() throws Exception {
        doTest(Calendar.JANUARY);
    }

    @Test
    public void testBug37668_FEB() throws Exception {
        doTest(Calendar.FEBRUARY);
    }

    @Test
    public void testBug37668_MAR() throws Exception {
        doTest(Calendar.MARCH);
    }

    @Test
    public void testBug37668_APR() throws Exception {
        doTest(Calendar.APRIL);
    }

    @Test
    public void testBug37668_MAY() throws Exception {
        doTest(Calendar.MAY);
    }

    @Test
    public void testBug37668_JUN() throws Exception {
        doTest(Calendar.JUNE);
    }

    @Test
    public void testBug37668_JUL() throws Exception {
        doTest(Calendar.JULY);
    }

    @Test
    public void testBug37668_AUG() throws Exception {
        doTest(Calendar.AUGUST);
    }

    @Test
    public void testBug37668_SEP() throws Exception {
        doTest(Calendar.SEPTEMBER);
    }

    @Test
    public void testBug37668_OCT() throws Exception {
        doTest(Calendar.OCTOBER);
    }

    @Test
    public void testBug37668_NOV() throws Exception {
        doTest(Calendar.NOVEMBER);
    }

    @Test
    public void testBug37668_DEC() throws Exception {
        doTest(Calendar.DECEMBER);
    }

    private void doTest(int month) throws Exception {
        appSimple.setMonth(month);
        appSimple.setTitle("Bug 37668 Test (" + (month + 1) + ") simple.");

        app23h.setMonth(month);
        app23h.setTitle("Bug 37668 Test (" + (month + 1) + ") 23h.");

        app25h.setMonth(month);
        app25h.setTitle("Bug 37668 Test (" + (month + 1) + ") 25h.");

        fulltime.setMonth(month);
        fulltime.setTitle("Bug 37668 Test (" + (month + 1) + ") fulltime.");

        fulltime2days.setMonth(month);
        fulltime2days.setTitle("Bug 37668 Test (" + (month + 1) + ") fulltime 2 days.");

        catm.insert(appSimple);
        catm.insert(app23h);
        catm.insert(app25h);
        catm.insert(fulltime);
        catm.insert(fulltime2days);

        Appointment delete = new Appointment();
        delete.setObjectID(appSimple.getObjectID());
        delete.setParentFolderID(appSimple.getParentFolderID());
        delete.setRecurrencePosition(1);
        delete.setLastModified(new Date(Long.MAX_VALUE));
        catm.delete(delete);

        delete.setObjectID(app25h.getObjectID());
        delete.setParentFolderID(app25h.getParentFolderID());
        catm.delete(delete);

        delete.setObjectID(app23h.getObjectID());
        delete.setParentFolderID(app23h.getParentFolderID());
        catm.delete(delete);

        delete.setObjectID(fulltime.getObjectID());
        delete.setParentFolderID(fulltime.getParentFolderID());
        catm.delete(delete);

        delete.setObjectID(fulltime2days.getObjectID());
        delete.setParentFolderID(fulltime2days.getParentFolderID());
        catm.delete(delete);

        Appointment loadSimple = catm.get(appSimple.getParentFolderID(), appSimple.getObjectID());
        assertEquals(D("22." + (month + 1) + ".2015 16:00", timeZone), loadSimple.getStartDate(), "Wrong start date. (" + loadSimple.getTitle() + ")");
        assertEquals(D("22." + (month + 1) + ".2015 17:00", timeZone), loadSimple.getEndDate(), "Wrong end date. (" + loadSimple.getTitle() + ")");
        assertNotNull(loadSimple.getDeleteException(), "Expected a delete Exception.");
        assertEquals(1, loadSimple.getDeleteException().length, "Expected a delete Exception.");

        Appointment load23h = catm.get(app23h.getParentFolderID(), app23h.getObjectID());
        assertEquals(D("22." + (month + 1) + ".2015 16:00", timeZone), load23h.getStartDate(), "Wrong start date. (" + load23h.getTitle() + ")");
        assertEquals(D("23." + (month + 1) + ".2015 15:00", timeZone), load23h.getEndDate(), "Wrong end date. (" + load23h.getTitle() + ")");
        assertNotNull(load23h.getDeleteException(), "Expected a delete Exception.");
        assertEquals(1, load23h.getDeleteException().length, "Expected a delete Exception.");

        Appointment load25h = catm.get(app25h.getParentFolderID(), app25h.getObjectID());
        assertEquals(D("22." + (month + 1) + ".2015 16:00", timeZone), load25h.getStartDate(), "Wrong start date. (" + load25h.getTitle() + ")");
        assertEquals(D("23." + (month + 1) + ".2015 17:00", timeZone), load25h.getEndDate(), "Wrong end date. (" + load25h.getTitle() + ")");
        assertNotNull(load25h.getDeleteException(), "Expected a delete Exception.");
        assertEquals(1, load25h.getDeleteException().length, "Expected a delete Exception.");

        Appointment loadFulltime = catm.get(fulltime.getParentFolderID(), fulltime.getObjectID());
        assertEquals(D("22." + (month + 1) + ".2015 00:00", utc), loadFulltime.getStartDate(), "Wrong start date. (" + loadFulltime.getTitle() + ")");
        assertEquals(D("23." + (month + 1) + ".2015 00:00", utc), loadFulltime.getEndDate(), "Wrong end date. (" + loadFulltime.getTitle() + ")");
        assertNotNull(loadFulltime.getDeleteException(), "Expected a delete Exception.");
        assertEquals(1, loadFulltime.getDeleteException().length, "Expected a delete Exception.");

        Appointment loadFulltime2days = catm.get(fulltime2days.getParentFolderID(), fulltime2days.getObjectID());
        assertEquals(D("22." + (month + 1) + ".2015 00:00", utc), loadFulltime2days.getStartDate(), "Wrong start date. (" + loadFulltime2days.getTitle() + ")");
        assertEquals(D("24." + (month + 1) + ".2015 00:00", utc), loadFulltime2days.getEndDate(), "Wrong end date. (" + loadFulltime2days.getTitle() + ")");
        assertNotNull(loadFulltime2days.getDeleteException(), "Expected a delete Exception.");
        assertEquals(1, loadFulltime2days.getDeleteException().length, "Expected a delete Exception.");
    }
}
