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

package com.openexchange.chronos.itip.json.converter;

import static com.openexchange.chronos.itip.json.action.Utils.getTimeZone;
import static com.openexchange.tools.arrays.Collections.isNotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.json.converter.mapper.AttendeesMapping;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.chronos.json.fields.ChronosEventConflictJsonFields;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.chronos.scheduling.ITipChange;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.tools.arrays.Collections;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ITipAnalysisResultConverter}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class ITipAnalysisResultConverter implements ResultConverter {

    /** The input format this converter uses */
    public static final String INPUT_FORMAT = "iTipAnalysis";

    /**
     * Initializes a new {@link ITipAnalysisResultConverter}.
     */
    public ITipAnalysisResultConverter() {
        super();
    }

    @Override
    public String getInputFormat() {
        return INPUT_FORMAT;
    }

    @Override
    public String getOutputFormat() {
        return "json";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convert(AJAXRequestData requestData, AJAXRequestResult result, ServerSession session, Converter converter) throws OXException {
        /*
         * Prepare time zone
         */
        TimeZone timezone = getTimeZone(requestData, session);
        Object resultObject = result.getResultObject();
        if ((resultObject instanceof ITipAnalysis)) {
            /*
             * Convert single
             */
            ITipAnalysis analysis = (ITipAnalysis) resultObject;
            result.setResultObject(convertAnalysis(analysis, timezone, session), getOutputFormat());
        } else if ((resultObject instanceof List)) {
            /*
             * Convert multiple
             */
            List<ITipAnalysis> analysis = (List<ITipAnalysis>) resultObject;
            result.setResultObject(convertAnalysis(analysis, timezone, session), getOutputFormat());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private JSONArray convertAnalysis(List<ITipAnalysis> analysis, TimeZone tz, ServerSession session) throws OXException {
        JSONArray array = new JSONArray();
        for (ITipAnalysis a : analysis) {
            array.put(convertAnalysis(a, tz, session));
        }
        return array;
    }

    private JSONObject convertAnalysis(ITipAnalysis analysis, TimeZone tz, ServerSession session) throws OXException {
        JSONObject json = new JSONObject();
        if (Collections.isNullOrEmpty(analysis.getAnalyzedChanges())) {
            return json;
        }
        try {
            convertAnnotations(analysis.getMainChange(), json, tz, session);
            convertChanges(analysis.getAnalyzedChanges(), json, tz, session);
            convertActions(analysis.getMainChange(), json);
            convertTargetedAttendee(analysis.getMainChange(), json, tz);
            convertUid(analysis.getUid(), json);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
        return json;
    }

    private void convertUid(String uid, JSONObject json) throws JSONException {
        if (Strings.isEmpty(uid)) {
            return;
        }
        json.put("uid", uid);
    }

    private void convertActions(AnalyzedChange change, JSONObject json) throws JSONException {
        if (null != change && isNotEmpty(change.getActions())) {
            JSONArray actionsArray = new JSONArray(change.getActions().size());
            change.getActions().forEach(a -> actionsArray.put(a.name().toLowerCase()));
            json.put("actions", actionsArray);
        }
    }

    private void convertTargetedAttendee(AnalyzedChange change, JSONObject json, TimeZone tz) throws JSONException, OXException {
        if (null != change && null != change.getTargetedAttendee()) {
            json.put("targetedAttendee", ((AttendeesMapping<?>) EventMapper.getInstance().get(EventField.ATTENDEES)).serialize(change.getTargetedAttendee(), tz));
        }
    }

    private void convertChanges(Collection<AnalyzedChange> changes, JSONObject json, TimeZone tz, ServerSession session) throws JSONException, OXException {
        JSONArray changesArray = new JSONArray();
        for (AnalyzedChange change : changes) {
            JSONObject changeObject = new JSONObject();
            convertChange(change.getChange(), changeObject, tz, session);
            changesArray.put(changeObject);
        }

        json.put("changes", changesArray);
    }

    private void convertChange(ITipChange change, JSONObject changeObject, TimeZone tz, ServerSession session) throws JSONException, OXException {
        if (change == null) {
            return;
        }
        changeObject.put("type", change.getType().name());

        Event newEvent = change.getNewEvent();
        if (newEvent != null) {
            changeObject.put("newEvent", EventMapper.getInstance().serialize(newEvent, EventMapper.getInstance().getAssignedFields(newEvent), tz, session));
        }

        Event currentEvent = change.getCurrentEvent();
        if (currentEvent != null) {
            changeObject.put("currentEvent", EventMapper.getInstance().serialize(currentEvent, EventMapper.getInstance().getAssignedFields(currentEvent), tz, session));
        }

        Event deletedEvent = change.getDeletedEvent();
        if (deletedEvent != null) {
            changeObject.put("deletedEvent", EventMapper.getInstance().serialize(deletedEvent, EventMapper.getInstance().getAssignedFields(deletedEvent), tz, session));
        }

        List<EventConflict> conflicts = change.getConflicts();
        if (conflicts != null && !conflicts.isEmpty()) {
            JSONArray array = new JSONArray(conflicts.size());
            for (EventConflict conflict : conflicts) {
                JSONObject jsonConflict = new JSONObject(3);
                jsonConflict.put(ChronosEventConflictJsonFields.EventConflict.HARD_CONFLICT, conflict.isHardConflict());
                jsonConflict.put(ChronosEventConflictJsonFields.EventConflict.CONFLICTING_ATTENDEES, convertAttendees(conflict.getConflictingAttendees()));
                jsonConflict.put(ChronosEventConflictJsonFields.EventConflict.EVENT, EventMapper.getInstance().serialize(conflict.getConflictingEvent(), EventMapper.getInstance().getAssignedFields(conflict.getConflictingEvent()), tz, session));
                array.put(jsonConflict);
            }
            changeObject.put("conflicts", array);
        }
    }

    private JSONArray convertAttendees(List<Attendee> attendees) throws JSONException {
        JSONArray result = new JSONArray(attendees.size());
        for (Attendee attendee : attendees) {
            result.put(EventMapper.serializeCalendarUser(attendee));
        }
        return result;
    }

    private void convertAnnotations(AnalyzedChange change, JSONObject json, TimeZone tz, ServerSession session) throws JSONException, OXException {
        if (null != change && isNotEmpty(change.getAnnotations())) {
            JSONArray array = new JSONArray();
            for (ITipAnnotation annotation : change.getAnnotations()) {
                convertAnnotation(array, annotation, tz, session);
            }
            json.put("annotations", array);
        }
    }

    private void convertAnnotation(JSONArray array, ITipAnnotation annotation, TimeZone tz, ServerSession session) throws JSONException, OXException {
        String message = annotation.getMessage();
        List<Object> args = annotation.getArgs();
        if (Strings.isNotEmpty(message) && isNotEmpty(args)) {
            message = String.format(message, args.toArray(new Object[args.size()]));
        }
        JSONObject annotationObject = new JSONObject();
        annotationObject.put("message", message);
        Event event = annotation.getEvent();
        if (event != null) {
            annotationObject.put("event", EventMapper.getInstance().serialize(event, EventMapper.getInstance().getAssignedFields(event), tz, session));
        }
        Map<String, Object> additionals = annotation.getAdditionals();
        if (null != additionals) {
            for (Entry<String, Object> entry : additionals.entrySet()) {
                try {
                    annotationObject.put(entry.getKey(), entry.getValue());
                } catch (JSONException e) {
                    org.slf4j.LoggerFactory.getLogger(ITipAnalysisResultConverter.class).warn("Unable to serialize additional parameter {}, skipping.", entry.getKey(), e);
                }
            }
        }
        array.put(annotationObject);
    }

}
