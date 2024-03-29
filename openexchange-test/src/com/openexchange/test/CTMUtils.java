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

package com.openexchange.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.parser.AppointmentParser;
import com.openexchange.ajax.parser.ParticipantParser;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.GroupParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.ResourceGroupParticipant;
import com.openexchange.groupware.container.ResourceParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.container.participants.ConfirmableParticipant;

/**
 * {@link CTMUtils}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.3
 */
public class CTMUtils {

    public static Appointment[] jsonArray2AppointmentArray(final JSONArray jsonArray, final TimeZone userTimeZone) throws Exception {
        final Appointment[] appointmentArray = new Appointment[jsonArray.length()];
        final AppointmentParser appointmentParser = new AppointmentParser(userTimeZone);

        for (int a = 0; a < appointmentArray.length; a++) {
            appointmentArray[a] = new Appointment();
            final JSONObject jObj = jsonArray.getJSONObject(a);

            appointmentParser.parse(appointmentArray[a], jObj);
        }

        return appointmentArray;
    }

    public static Appointment[] jsonArray2AppointmentArray(final JSONArray jsonArray, final int[] cols, final TimeZone userTimeZone) throws JSONException, OXException {
        final Appointment[] appointmentArray = new Appointment[jsonArray.length()];

        for (int a = 0; a < appointmentArray.length; a++) {
            appointmentArray[a] = new Appointment();
            parseCols(cols, jsonArray.getJSONArray(a), appointmentArray[a]);

            if (!appointmentArray[a].getFullTime()) {
                final Date startDate = appointmentArray[a].getStartDate();
                final Date endDate = appointmentArray[a].getEndDate();

                if (startDate != null && endDate != null) {
                    final int startOffset = userTimeZone.getOffset(startDate.getTime());
                    final int endOffset = userTimeZone.getOffset(endDate.getTime());
                    appointmentArray[a].setStartDate(new Date(startDate.getTime() - startOffset));
                    appointmentArray[a].setEndDate(new Date(endDate.getTime() - endOffset));
                }
            }
        }

        return appointmentArray;
    }

    public static void parseCols(final int[] cols, final JSONArray jsonArray, final Appointment appointmentObj) throws JSONException, OXException {
        assertEquals(cols.length, jsonArray.length(), "compare array size with cols size");

        for (int a = 0; a < cols.length; a++) {
            parse(a, cols[a], jsonArray, appointmentObj);
        }
    }

    public static void parse(final int pos, final int field, final JSONArray jsonArray, final Appointment appointmentObj) throws JSONException, OXException {
        switch (field) {
            case Appointment.ALARM:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setAlarm(jsonArray.getInt(pos));
                }
                break;
            case Appointment.OBJECT_ID:
                appointmentObj.setObjectID(jsonArray.getInt(pos));
                break;
            case Appointment.FOLDER_ID:
                appointmentObj.setParentFolderID(jsonArray.getInt(pos));
                break;
            case Appointment.TITLE:
                appointmentObj.setTitle(jsonArray.getString(pos));
                break;
            case Appointment.CREATION_DATE:
                appointmentObj.setCreationDate(new Date(jsonArray.getLong(pos)));
                break;
            case Appointment.LAST_MODIFIED:
                appointmentObj.setLastModified(new Date(jsonArray.getLong(pos)));
                break;
            case Appointment.START_DATE:
                appointmentObj.setStartDate(new Date(jsonArray.getLong(pos)));
                break;
            case Appointment.END_DATE:
                appointmentObj.setEndDate(new Date(jsonArray.getLong(pos)));
                break;
            case Appointment.SHOWN_AS:
                appointmentObj.setShownAs(jsonArray.getInt(pos));
                break;
            case Appointment.LOCATION:
                appointmentObj.setLocation(jsonArray.getString(pos));
                break;
            case Appointment.FULL_TIME:
                if (false == jsonArray.isNull(pos)) {
                    appointmentObj.setFullTime(jsonArray.getBoolean(pos));
                }
                break;
            case Appointment.PRIVATE_FLAG:
                if (false == jsonArray.isNull(pos)) {
                    appointmentObj.setPrivateFlag(jsonArray.getBoolean(pos));
                }
                break;
            case Appointment.CATEGORIES:
                appointmentObj.setCategories(jsonArray.getString(pos));
                break;
            case Appointment.COLOR_LABEL:
                appointmentObj.setLabel(jsonArray.getInt(pos));
                break;
            case Appointment.NOTE:
                appointmentObj.setNote(jsonArray.getString(pos));
                break;
            case Appointment.RECURRENCE_POSITION:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setRecurrencePosition(jsonArray.getInt(pos));
                }
                break;
            case Appointment.RECURRENCE_TYPE:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setRecurrenceType(jsonArray.getInt(pos));
                }
                break;
            case Appointment.RECURRENCE_ID:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setRecurrenceID(jsonArray.getInt(pos));
                }
                break;
            case Appointment.INTERVAL:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setInterval(jsonArray.getInt(pos));
                }
                break;
            case Appointment.DAYS:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setDays(jsonArray.getInt(pos));
                }
                break;
            case Appointment.DAY_IN_MONTH:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setDayInMonth(jsonArray.getInt(pos));
                }
                break;
            case Appointment.MONTH:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setMonth(jsonArray.getInt(pos));
                }
                break;
            case Appointment.UNTIL:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setUntil(new Date(jsonArray.getLong(pos)));
                }
                break;
            case Appointment.RECURRENCE_COUNT:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setOccurrence(jsonArray.getInt(pos));
                }
                break;
            case Appointment.TIMEZONE:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setTimezone(jsonArray.getString(pos));
                }
                break;
            case Appointment.RECURRENCE_START:
                if (!jsonArray.isNull(pos)) {
                    appointmentObj.setRecurringStart(jsonArray.getLong(pos));
                }
                break;
            case Appointment.PARTICIPANTS:
                appointmentObj.setParticipants(parseParticipants(jsonArray.getJSONArray(pos)));
                break;
            case Appointment.USERS:
                appointmentObj.setUsers(parseUserParticipants(jsonArray.getJSONArray(pos)));
                break;
            case Appointment.CHANGE_EXCEPTIONS:
                if (!jsonArray.isNull(pos)) {
                    final JSONArray changeExceptions = jsonArray.getJSONArray(pos);
                    appointmentObj.setChangeExceptions(parseExceptions(changeExceptions));
                }
                break;
            case Appointment.DELETE_EXCEPTIONS:
                if (!jsonArray.isNull(pos)) {
                    final JSONArray deleteExceptions = jsonArray.getJSONArray(pos);
                    appointmentObj.setDeleteExceptions(parseExceptions(deleteExceptions));
                }
                break;
            case Appointment.ORGANIZER:
                appointmentObj.setOrganizer(jsonArray.getString(pos));
                break;
            case Appointment.UID:
                appointmentObj.setUid(jsonArray.getString(pos));
                break;
            case Appointment.SEQUENCE:
                appointmentObj.setSequence(jsonArray.getInt(pos));
                break;
            case Appointment.CONFIRMATIONS:
                appointmentObj.setConfirmations(parseConfirmableParticipants(jsonArray.getJSONArray(pos)));
                break;
        }
    }

    public static Date[] parseExceptions(final JSONArray jsonArray) throws JSONException {
        final Date[] exceptions = new Date[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            exceptions[i] = new Date(jsonArray.getLong(i));
        }
        return exceptions;
    }

    public static UserParticipant[] parseUserParticipants(final JSONArray userParticipantArr) throws JSONException {
        final List<UserParticipant> userParticipants = new LinkedList<UserParticipant>();
        for (int i = 0, size = userParticipantArr.length(); i < size; i++) {
            final JSONObject participantObj = userParticipantArr.getJSONObject(i);
            UserParticipant userParticipant = new UserParticipant(participantObj.getInt("id"));
            if (participantObj.has("confirmation")) {
                userParticipant.setConfirm(participantObj.getInt("confirmation"));
            }
            if (participantObj.has("confirmmessage")) {
                userParticipant.setConfirmMessage(participantObj.getString("confirmmessage"));
            }

            userParticipants.add(userParticipant);
        }
        return userParticipants.toArray(new UserParticipant[userParticipants.size()]);
    }

    public static ConfirmableParticipant[] parseConfirmableParticipants(JSONArray jsonArray) throws JSONException {
        ParticipantParser parser = new ParticipantParser();
        List<ConfirmableParticipant> confirmations = new ArrayList<ConfirmableParticipant>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonConfirmation = jsonArray.getJSONObject(i);
            confirmations.add(parser.parseConfirmation(true, jsonConfirmation));
        }
        return confirmations.toArray(new ConfirmableParticipant[confirmations.size()]);
    }

    public static Participant[] parseParticipants(final JSONArray jsonArray) throws JSONException, OXException {
        final Participant[] participant = new Participant[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject jparticipant = jsonArray.getJSONObject(i);
            final int type = jparticipant.getInt("type");
            final int id;
            if (jparticipant.has("id")) {
                id = jparticipant.getInt("id");
            } else {
                id = Participant.NO_ID;
            }
            final String mail = jparticipant.optString("mail");
            Participant p = null;
            switch (type) {
                case Participant.USER:
                    if (Participant.NO_ID == id) {
                        throw new JSONException("JSONObject[id] not found.");
                    }
                    final UserParticipant user = new UserParticipant(id);
                    if (jparticipant.has("confirmation")) {
                        user.setConfirm(jparticipant.getInt("confirmation"));
                    }
                    if (jparticipant.has("confirmmessage")) {
                        user.setConfirmMessage(jparticipant.getString("confirmmessage"));
                    }
                    p = user;
                    break;
                case Participant.GROUP:
                    if (Participant.NO_ID == id) {
                        throw new JSONException("JSONObject[id] not found.");
                    }
                    p = new GroupParticipant(id);
                    break;
                case Participant.RESOURCE:
                    if (Participant.NO_ID == id) {
                        throw new JSONException("JSONObject[id] not found.");
                    }
                    p = new ResourceParticipant(id);
                    break;
                case Participant.RESOURCEGROUP:
                    if (Participant.NO_ID == id) {
                        throw new JSONException("JSONObject[id] not found.");
                    }
                    p = new ResourceGroupParticipant(id);
                    break;
                case Participant.EXTERNAL_USER:
                    if (null == mail) {
                        throw new JSONException("JSONObject[mail] not found.");
                    }
                    p = new ExternalUserParticipant(mail);
                    break;
                default:
                    throw new OXException().setLogMessage("invalidType");
            }
            participant[i] = p;
        }

        return participant;
    }

}
