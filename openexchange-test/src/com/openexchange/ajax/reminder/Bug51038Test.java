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

package com.openexchange.ajax.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.framework.Abstrac2UserAJAXSession;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.ajax.reminder.actions.RangeRequest;
import com.openexchange.ajax.reminder.actions.RangeResponse;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.test.common.groupware.calendar.TimeTools;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Bug51038Test extends Abstrac2UserAJAXSession {

    private int nextYear;
    private ReminderObject reminder;
    private Appointment appointment;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1;

        appointment = new Appointment();
        appointment.setTitle(this.getClass().getSimpleName());
        appointment.setStartDate(TimeTools.D("17.01." + nextYear + " 11:00"));
        appointment.setEndDate(TimeTools.D("17.01." + nextYear + " 12:00"));
        appointment.setAlarm(15);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setIgnoreConflicts(true);

        catm.insert(appointment);
    }

    @Test
    public void testBug51038() throws Exception {
        Calendar cal = Calendar.getInstance(getClient().getValues().getTimeZone());
        cal.setTime(appointment.getEndDate());
        RangeRequest rangeReq = new RangeRequest(cal.getTime());
        RangeResponse rangeResp = getClient().execute(rangeReq);

        final JSONArray array = (JSONArray) rangeResp.getData();
        final JSONObject json = array.getJSONObject(0);
        long id = DataParser.parseLong(json, DataFields.ID);
        long lastModified = DataParser.parseLong(json, "last_modified");
        Integer recurrencePosition = DataParser.parseInteger(json, "recurrence_position");
        com.openexchange.ajax.reminder.actions.DeleteRequest delReminderReq = new com.openexchange.ajax.reminder.actions.DeleteRequest(id, recurrencePosition, lastModified, false);
        CommonDeleteResponse response = testUser2.getAjaxClient().execute(delReminderReq);

        assertTrue(response.hasError(), "Expected error.");

        rangeReq = new RangeRequest(cal.getTime());
        rangeResp = getClient().execute(rangeReq);
        reminder = ReminderTools.searchByTarget(rangeResp.getReminder(getClient().getValues().getTimeZone()), appointment.getObjectID());
        int reminderId = reminder.getObjectId();
        reminder = ReminderTools.searchByTarget(rangeResp.getReminder(getClient().getValues().getTimeZone()), appointment.getObjectID());
        assertNotNull(reminder, "Missing reminder.");
        assertEquals(reminderId, reminder.getObjectId(), "Wrong reminder.");
    }
}
