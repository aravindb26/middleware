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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.AllRequest;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.ListRequest;
import com.openexchange.ajax.appointment.action.SearchRequest;
import com.openexchange.ajax.appointment.action.SearchResponse;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.appointment.helper.ParticipantStorage;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.AbstractColumnsResponse;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.ajax.parser.ParticipantParser;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.participants.ConfirmableParticipant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;

/**
 * Checks if the calendar component correctly fills the confirmations JSON appointment attributes.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ConfirmationsSeriesTest extends AbstractAJAXSession {

    private static final int[] COLUMNS = { Appointment.OBJECT_ID, Appointment.FOLDER_ID, Appointment.CONFIRMATIONS };
    private AJAXClient client;
    private int folderId;
    private TimeZone tz;
    private Appointment appointment;
    private ExternalUserParticipant participant;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
        folderId = client.getValues().getPrivateAppointmentFolder();
        tz = client.getValues().getTimeZone();
        appointment = new Appointment();
        appointment.setTitle("Test series appointment for testing confirmations");
        Calendar calendar = TimeTools.createCalendar(tz);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.setParentFolderID(folderId);
        appointment.setIgnoreConflicts(true);
        participant = new ExternalUserParticipant("external1@example.com");
        participant.setDisplayName("External user");
        appointment.addParticipant(participant);
        participant = new ExternalUserParticipant("external2@example.com");
        participant.setDisplayName("External user 2");
        appointment.addParticipant(participant);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        client.execute(new InsertRequest(appointment, tz)).fillAppointment(appointment);
    }

    @Test
    public void testChangeException() throws Throwable {
        GetResponse response1 = client.execute(new GetRequest(appointment.getParentFolderID(), appointment.getObjectID(), 3));
        Appointment occurrence3 = response1.getAppointment(tz);
        Appointment changeException = new Appointment();
        changeException.setObjectID(appointment.getObjectID());
        changeException.setParentFolderID(appointment.getParentFolderID());
        changeException.setLastModified(appointment.getLastModified());
        changeException.setRecurrencePosition(occurrence3.getRecurrencePosition());
        changeException.setTitle("Change exception appointment for testing confirmations");
        changeException.setIgnoreConflicts(true);
        UpdateResponse response2 = client.execute(new UpdateRequest(changeException, tz));
        appointment.setLastModified(response2.getTimestamp());
        GetResponse response3 = client.execute(new GetRequest(appointment));
        checkConfirmations(ParticipantStorage.extractExternal(appointment.getParticipants()), response3.getAppointment(tz).getConfirmations());
        Date rangeStart = TimeTools.getAPIDate(tz, occurrence3.getStartDate(), 0);
        Date rangeEnd = TimeTools.getAPIDate(tz, occurrence3.getEndDate(), 1);
        CommonAllResponse response4 = client.execute(new AllRequest(folderId, COLUMNS, rangeStart, rangeEnd, tz));
        checkConfirmations(ParticipantStorage.extractExternal(appointment.getParticipants()), findConfirmations(response4, response2.getId()));
        CommonListResponse response5 = client.execute(new ListRequest(ListIDs.l(new int[] { folderId, response2.getId() }), COLUMNS));
        checkConfirmations(ParticipantStorage.extractExternal(appointment.getParticipants()), findConfirmations(response5, response2.getId()));
        SearchResponse response6 = client.execute(new SearchRequest("*", folderId, COLUMNS));
        checkConfirmations(ParticipantStorage.extractExternal(appointment.getParticipants()), findConfirmations(response6, response2.getId()));
    }

    private JSONArray findConfirmations(AbstractColumnsResponse response, int id) {
        int objectIdPos = response.getColumnPos(Appointment.OBJECT_ID);
        int confirmationsPos = response.getColumnPos(Appointment.CONFIRMATIONS);
        JSONArray jsonConfirmations = null;
        for (Object[] tmp : response) {
            if (id == ((Integer) tmp[objectIdPos]).intValue()) {
                jsonConfirmations = (JSONArray) tmp[confirmationsPos];
            }
        }
        return jsonConfirmations;
    }

    private void checkConfirmations(ExternalUserParticipant[] expected, JSONArray jsonConfirmations) throws JSONException {
        assertNotNull(jsonConfirmations, "Response does not contain confirmations.");
        ParticipantParser parser = new ParticipantParser();
        List<ConfirmableParticipant> confirmations = new ArrayList<ConfirmableParticipant>();
        int length = jsonConfirmations.length();
        for (int i = 0; i < length; i++) {
            JSONObject jsonConfirmation = jsonConfirmations.getJSONObject(i);
            confirmations.add(parser.parseConfirmation(true, jsonConfirmation));
        }
        checkConfirmations(expected, confirmations.toArray(new ConfirmableParticipant[confirmations.size()]));
    }

    private void checkConfirmations(ExternalUserParticipant[] expected, ConfirmableParticipant[] actual) {
        assertNotNull(actual, "Response does not contain any confirmations.");
        // Following expected must be one more if internal user participants get its way into the confirmations array.
        assertEquals(expected.length, actual.length, "Number of external participant confirmations does not match.");
        Arrays.sort(expected);
        Arrays.sort(actual);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].getEmailAddress(), actual[i].getEmailAddress(), "Mailaddress of external participant does not match.");
            assertEquals(expected[i].getDisplayName(), actual[i].getDisplayName(), "Display name of external participant does not match.");
            assertEquals(expected[i].getStatus(), actual[i].getStatus(), "Confirm status does not match.");
            assertEquals(expected[i].getMessage(), actual[i].getMessage(), "Confirm message does not match.");
        }
    }
}
