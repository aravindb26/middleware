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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug30118Test}
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug30118Test extends AbstractAJAXSession {

    private Appointment appointment;

    /**
     * Initializes a new {@link Bug30118Test}.
     * 
     * @param name
     */
    public Bug30118Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        appointment = new Appointment();
        appointment.setTitle("Bug 30118 Test");
        appointment.setStartDate(D("17.12.2013 08:00"));
        appointment.setEndDate(D("18.12.2013 09:00"));
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setOccurrence(5);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setIgnoreConflicts(true);
    }

    @Test
    public void testBug30118() throws Exception {
        catm.insert(appointment);
        catm.createDeleteException(appointment.getParentFolderID(), appointment.getObjectID(), 3);
        Appointment loaded = catm.get(appointment);
        assertTrue(loaded.getDeleteException() != null && loaded.getDeleteException().length == 1, "Expected one delete Exception.");
    }

    @Test
    public void testBug30118Fulltime() throws Exception {
        appointment.setStartDate(D("17.12.2013 00:00"));
        appointment.setEndDate(D("18.12.2013 00:00"));
        appointment.setFullTime(true);
        catm.insert(appointment);
        catm.createDeleteException(appointment.getParentFolderID(), appointment.getObjectID(), 3);
        Appointment loaded = catm.get(appointment);
        assertTrue(loaded.getDeleteException() != null && loaded.getDeleteException().length == 1, "Expected one delete Exception.");
    }

    @Test
    public void testBug30118Fulltime2days() throws Exception {
        appointment.setStartDate(D("17.12.2013 00:00"));
        appointment.setEndDate(D("19.12.2013 00:00"));
        appointment.setFullTime(true);
        catm.insert(appointment);
        catm.createDeleteException(appointment.getParentFolderID(), appointment.getObjectID(), 3);
        Appointment loaded = catm.get(appointment);
        assertTrue(loaded.getDeleteException() != null && loaded.getDeleteException().length == 1, "Expected one delete Exception.");
    }
}
