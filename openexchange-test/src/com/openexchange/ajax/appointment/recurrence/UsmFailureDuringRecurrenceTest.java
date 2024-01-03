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

package com.openexchange.ajax.appointment.recurrence;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Changes;
import com.openexchange.groupware.container.Expectations;
import org.junit.jupiter.api.TestInfo;

/**
 * Bug 15645
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class UsmFailureDuringRecurrenceTest extends ManagedAppointmentTest {

    private Appointment app;

    public UsmFailureDuringRecurrenceTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        app = generateYearlyAppointment();
        catm.setTimezone(TimeZone.getTimeZone("UTC"));
    }

    //I think the message should be more like "Bullshit, you cannot make a change exception a series"    @Test
    @Test
    public void testFailWhenTryingToMakeAChangeExceptionASeries() {
        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_POSITION, I(1));
        changes.put(Appointment.RECURRENCE_TYPE, I(3));
        changes.put(Appointment.DAY_IN_MONTH, I(23));
        changes.put(Appointment.MONTH, I(3));
        changes.put(Appointment.UNTIL, D("31/12/2025 00:00"));
        failTest(changes, "Incomplete recurring information: missing interval");
    }

    //I think it should be an exception like "Bullshit, you cannot make a change exception a series"    @Test
    @Test
    public void testShouldFailWhenTryingToMakeAChangeExceptionASeriesButDoesNot() throws Exception {
        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_POSITION, I(1));
        changes.put(Appointment.RECURRENCE_TYPE, I(Appointment.MONTHLY));
        changes.put(Appointment.DAY_IN_MONTH, I(23));
        changes.put(Appointment.MONTH, I(3));
        changes.put(Appointment.UNTIL, D("31/12/2025 00:00"));
        changes.put(Appointment.INTERVAL, I(1));

        Expectations expectationsForSeries = new Expectations();
        expectationsForSeries.put(Appointment.RECURRENCE_TYPE, I(Appointment.YEARLY));

        Expectations expectationsForException = new Expectations();
        expectationsForException.put(Appointment.RECURRENCE_POSITION, I(1));
        expectationsForException.put(Appointment.RECURRENCE_TYPE, I(0));

        succeedTest(changes, expectationsForSeries, expectationsForException);
    }

    @Test
    public void testFailOnAChangeExceptionWithoutInterval() {
        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_POSITION, I(1));
        changes.put(Appointment.START_DATE, D("31/12/2025 00:00"));

        Expectations expectationsForException = new Expectations(changes);
        expectationsForException.put(Appointment.RECURRENCE_TYPE, I(1));
        expectationsForException.put(Appointment.DAY_IN_MONTH, null);
        expectationsForException.put(Appointment.MONTH, null);
        expectationsForException.put(Appointment.UNTIL, null);
        expectationsForException.put(Appointment.INTERVAL, null);

        failTest(changes, "Incomplete recurring information: missing interval.");
    }

    @Test
    public void testShouldAllowToCreateAChangeException() throws Exception {
        Date start = D("31.12.2025 00:00");
        Date end = D("31.12.2025 01:00");
        Changes changes = new Changes();
        changes.put(Appointment.RECURRENCE_POSITION, I(1));
        changes.put(Appointment.START_DATE, start);
        changes.put(Appointment.END_DATE, end);
        changes.put(Appointment.INTERVAL, I(1));

        Expectations expectationsForException = new Expectations();
        expectationsForException.put(Appointment.RECURRENCE_POSITION, I(1));
        expectationsForException.put(Appointment.RECURRENCE_TYPE, I(0));
        expectationsForException.put(Appointment.UNTIL, null);
        expectationsForException.put(Appointment.INTERVAL, null);
        expectationsForException.put(Appointment.MONTH, null);
        expectationsForException.put(Appointment.DAY_IN_MONTH, null);

        succeedTest(changes, null, expectationsForException);
    }

    @Test
    public void testShouldFailWhenTryingToDeleteExceptionOnNormalAppointment() {
        app = new Appointment();
        app.setParentFolderID(folder.getObjectID());
        app.setStartDate(D("31.12.2025 00:00"));
        app.setEndDate(D("31.12.2025 01:00"));

        catm.insert(app);
        app.setRecurrencePosition(1);
        catm.delete(app, false);
        assertTrue(catm.hasLastException(), "Should fail");
        /*
         * won't go further because exception is not wrapped nicely,
         * so this is just a boring JSON exception on the client side.
         */
    }

    private void succeedTest(Changes changes, Expectations expectationsForSeries, Expectations expectationsForException) throws OXException {
        catm.insert(app);
        assertFalse(catm.hasLastException(), "Creation was expected to work");

        Appointment update = new Appointment();
        update.setParentFolderID(app.getParentFolderID());
        update.setObjectID(app.getObjectID());
        update.setLastModified(app.getLastModified());
        changes.update(update);
        catm.update(update);

        if (update.containsRecurrencePosition()) {
            assertFalse(app.getObjectID() == update.getObjectID(), "Appointment and change exception should have different IDs");
        }

        assertFalse(catm.hasLastException(), "Update was expected to work");

        if (expectationsForSeries != null) {
            Appointment actualSeries = catm.get(app);
            assertFalse(catm.hasLastException(), "Getting the series was expected to work");
            expectationsForSeries.verify("[series]", actualSeries);
        }

        if (expectationsForException != null) {
            Appointment actualChangeException = catm.get(update);
            assertFalse(catm.hasLastException(), "Getting the update was expected to work");
            expectationsForException.verify("[change exception]", actualChangeException);
        }
    }

    private void failTest(Changes changes, String errorCode) {
        catm.insert(app);

        Appointment update = new Appointment();
        update.setParentFolderID(app.getParentFolderID());
        update.setObjectID(app.getObjectID());
        update.setLastModified(app.getLastModified());
        changes.update(update);
        catm.update(update);

        assertTrue(catm.hasLastException(), "Was expected to fail");
        Exception exception = catm.getLastException();
        assertTrue(exception.getMessage().contains(errorCode), "Expected message was " + errorCode + ", but got: " + exception.getMessage());
    }

}
