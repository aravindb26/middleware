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

package com.openexchange.chronos.itip.json;

import static com.openexchange.tools.arrays.Collections.isNotEmpty;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.calendar.json.actions.chronos.DefaultEventConverter;
import com.openexchange.calendar.json.compat.AppointmentWriter;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.chronos.scheduling.ITipChange;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link ITipAnalysisWriter}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class ITipAnalysisWriter {

    private final AppointmentWriter appointmentWriter;
    private final DefaultEventConverter eventConverter;

    /**
     * 
     * Initializes a new {@link ITipAnalysisWriter} which converts an analysis to a JSON object
     *
     * @param timezone The timezone of the user
     * @param session The user session
     * @param services The services
     * @throws OXException In case of error
     */
    public ITipAnalysisWriter(TimeZone timezone, CalendarSession session, ServiceLookup services) throws OXException {
        super();
        this.appointmentWriter = new AppointmentWriter(timezone).setSession(new ServerSessionAdapter(session.getSession()));
        eventConverter = new DefaultEventConverter(services, session);
    }

    /**
     * Converts an analysis and write it into a JSON object, using appointments
     *
     * @param analysis The analysis
     * @param object The object
     * @throws JSONException In case of error
     * @throws OXException In case of error
     */
    public void write(final ITipAnalysis analysis, final JSONObject object) throws JSONException, OXException {
        if (null != analysis.getMethod()) {
            object.put("messageType", analysis.getMethod().name().toLowerCase());
        }
        if (analysis.getUid() != null) {
            object.put("uid", analysis.getUid());
        }
        List<AnalyzedChange> analyzedChanges = analysis.getAnalyzedChanges();
        if (isNotEmpty(analyzedChanges)) {
            writeAnnotations(analysis.getMainChange(), object);
            writeChanges(analyzedChanges, object);
            writeActions(analysis.getMainChange(), object);
        }
    }

    private void writeActions(AnalyzedChange change, final JSONObject object) throws JSONException {
        if (null != change && isNotEmpty(change.getActions())) {
            JSONArray actionsArray = new JSONArray(change.getActions().size());
            change.getActions().forEach(a -> actionsArray.put(a.name().toLowerCase()));
            object.put("actions", actionsArray);
        }
    }

    private void writeChanges(List<AnalyzedChange> analyzedChanges, final JSONObject object) throws JSONException, OXException {
        JSONArray changesArray = new JSONArray();
        for (AnalyzedChange change : analyzedChanges) {
            JSONObject changeObject = new JSONObject();
            writeChange(change.getChange(), changeObject);
            changesArray.put(changeObject);
        }

        object.put("changes", changesArray);
    }

    private void writeChange(final ITipChange change, final JSONObject changeObject) throws JSONException, OXException {
        if (change == null) {
            return;
        }
        changeObject.put("type", change.getType().name().toLowerCase());
        final Event newAppointment = change.getNewEvent();
        if (newAppointment != null) {
            final JSONObject newAppointmentObject = new JSONObject();
            appointmentWriter.writeAppointment(eventConverter.getAppointment(newAppointment), newAppointmentObject);
            changeObject.put("newAppointment", newAppointmentObject);
        }

        final Event currentAppointment = change.getCurrentEvent();
        if (currentAppointment != null) {
            final JSONObject currentAppointmentObject = new JSONObject();
            appointmentWriter.writeAppointment(eventConverter.getAppointment(currentAppointment), currentAppointmentObject);
            changeObject.put("currentAppointment", currentAppointmentObject);
        }

        final Event deletedAppointment = change.getDeletedEvent();
        if (deletedAppointment != null) {
            final JSONObject deletedAppointmentObject = new JSONObject();
            appointmentWriter.writeAppointment(eventConverter.getAppointment(deletedAppointment), deletedAppointmentObject);
            changeObject.put("deletedAppointment", deletedAppointmentObject);
        }

        final List<EventConflict> conflicts = change.getConflicts();
        if (conflicts != null && !conflicts.isEmpty()) {
            final JSONArray array = new JSONArray();
            for (EventConflict conflict : conflicts) {
                final JSONObject conflictObject = new JSONObject();
                appointmentWriter.writeAppointment(eventConverter.getAppointment(conflict.getConflictingEvent()), conflictObject);
                array.put(conflictObject);
            }
            changeObject.put("conflicts", array);
        }
    }

    private void writeAnnotations(AnalyzedChange change, final JSONObject object) throws JSONException, OXException {
        if (null != change && isNotEmpty(change.getAnnotations())) {
            JSONArray array = new JSONArray();
            for (ITipAnnotation annotation : change.getAnnotations()) {
                writeAnnotation(array, annotation);
            }
            object.put("annotations", array);
        }
    }

    private void writeAnnotation(JSONArray array, ITipAnnotation annotation) throws JSONException, OXException {
        String message = annotation.getMessage();
        List<Object> args = annotation.getArgs();
        if (Strings.isNotEmpty(message) && isNotEmpty(args)) {
            message = String.format(message, args.toArray(new Object[args.size()]));
        }
        JSONObject annotationObject = new JSONObject();
        annotationObject.put("message", message);
        Event event = annotation.getEvent();
        if (event != null) {
            JSONObject appointmentObject = new JSONObject();
            appointmentWriter.writeAppointment(eventConverter.getAppointment(event), appointmentObject);
            annotationObject.put("appointment", appointmentObject);
        }
        array.put(annotationObject);
    }

}
