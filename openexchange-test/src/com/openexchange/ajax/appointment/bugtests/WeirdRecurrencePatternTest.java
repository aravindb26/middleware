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
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.SetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.Appointment;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link WeirdRecurrencePatternTest}
 * 
 * This tests a series, where the implicit end lies in a different timezone offset than the start.
 * See: Daylight Saving Time.
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.8.0
 */
public class WeirdRecurrencePatternTest extends AbstractAJAXSession {

    private Appointment appointment;
    private TimeZone tz;

    /**
     * Initializes a new {@link WeirdRecurrencePatternTest}.
     * 
     * @param name
     */
    public WeirdRecurrencePatternTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        tz = TimeZone.getTimeZone("Europe/Berlin");
        SetRequest setRequest = new SetRequest(Tree.TimeZone, tz.getID());
        getClient().execute(setRequest);

        catm.setTimezone(tz);
        appointment = new Appointment();
        appointment.setTitle("hiliowequhe234123.3");
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setIgnoreConflicts(true);
    }

    @Test
    public void testPattern() throws Exception {

        appointment.setStartDate(D("06.01.2015 15:30", tz));
        appointment.setEndDate(D("06.01.2015 16:30", tz));
        appointment.setTimezone(tz.getID());
        catm.insert(appointment);

        Appointment loaded = catm.get(appointment.getParentFolderID(), appointment.getObjectID());
        assertEquals(D("06.01.2015 15:30", tz), loaded.getStartDate(), "Wrong start date.");
        assertEquals(D("06.01.2015 16:30", tz), loaded.getEndDate(), "Wrong end date.");
    }

}
