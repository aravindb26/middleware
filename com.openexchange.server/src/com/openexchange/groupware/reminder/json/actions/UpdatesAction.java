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

package com.openexchange.groupware.reminder.json.actions;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.writer.ReminderWriter;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.UpdatesResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.reminder.ReminderObject;
import com.openexchange.groupware.reminder.ReminderService;
import com.openexchange.groupware.reminder.json.ReminderAJAXRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link UpdatesAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractReminderAction.MODULE, type = RestrictedAction.Type.READ)
public final class UpdatesAction extends AbstractReminderAction {

    /**
     * Initializes a new {@link UpdatesAction}.
     */
    public UpdatesAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final ReminderAJAXRequest req) throws OXException, JSONException {
        final Date timestamp = req.checkDate(AJAXServlet.PARAMETER_TIMESTAMP);
        final TimeZone timeZone;
        {
            final String timeZoneId = req.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            timeZone = null == timeZoneId ? req.getTimeZone() : getTimeZone(timeZoneId);
        }
        final ReminderWriter reminderWriter = new ReminderWriter(timeZone);
        final JSONArray jsonResponseArray = new JSONArray();
        final ServerSession session = req.getSession();
        final ReminderService reminderService = ServerServiceRegistry.getInstance().getService(ReminderService.class, true);

        List<ReminderObject> reminders = reminderService.listModifiedReminder(session, session.getUserId(), timestamp);
        for (ReminderObject reminder : reminders) {
            try {
                if (isRequested(req, reminder) && hasModulePermission(reminder, session) && stillAccepted(reminder, session)) {
                    final JSONObject jsonReminderObj = new JSONObject(12);
                    reminderWriter.writeObject(reminder, jsonReminderObj);
                    jsonResponseArray.put(jsonReminderObj);
                }
            } catch (OXException e) {
                checkError(reminderService, reminder, session, e);
            }
        }
        if (req.getOptModules() == null || req.getOptModules().isEmpty() || req.getOptModules().contains(I(Types.APPOINTMENT))) {
            CalendarService calendarService = ServerServiceRegistry.getInstance().getService(CalendarService.class);
            CalendarSession calendarSession = calendarService.init(session);
            calendarSession.set(CalendarParameters.PARAMETER_RANGE_END, new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            UpdatesResult updatedEventsOfUser = calendarService.getUpdatedEventsOfUser(calendarSession, timestamp.getTime());

            List<AlarmTrigger> triggers = calendarService.getAlarmTriggers(calendarSession, Collections.singleton("DISPLAY"));
            List<Integer> updatedAlarmIds = new ArrayList<>();
            for (Event eve : updatedEventsOfUser.getNewAndModifiedEvents()) {
                for (Alarm alarm : eve.getAlarms()) {
                    updatedAlarmIds.add(I(alarm.getId()));
                }
            }
            for (AlarmTrigger trigger : triggers) {
                if (updatedAlarmIds.contains(trigger.getAlarm())) {
                    convertAlarmTrigger2Reminder(calendarSession, trigger, reminderWriter, jsonResponseArray);
                }
            }
        }

        return new AJAXRequestResult(jsonResponseArray, timestamp, "json");
    }

}
