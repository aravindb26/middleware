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

package com.openexchange.chronos.json.action;

import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_CHECK_CONFLICTS;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_EXPAND_OCCURRENCES;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_FIELDS;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_LEFT_HAND_LIMIT;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_MASK_UID;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_ORDER;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_ORDER_BY;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_PUSH_TOKEN;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_RANGE_END;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_RANGE_START;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_RIGHT_HAND_LIMIT;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_SCHEDULING;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_UPDATE_CACHE;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.dmfs.rfc5545.DateTime;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.SchedulingControl;
import com.openexchange.chronos.common.DefaultCalendarParameters;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.SortOrder;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.TimeZones;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link ChronosJsonParameters}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class ChronosJsonParameters {

    public static final String PARAM_RANGE_START = PARAMETER_RANGE_START;
    public static final String PARAM_RANGE_END = PARAMETER_RANGE_END;
    public static final String PARAM_EXPAND = PARAMETER_EXPAND_OCCURRENCES;
    public static final String PARAM_CHECK_CONFLICTS = PARAMETER_CHECK_CONFLICTS;
    public static final String PARAM_SCHEDULING = PARAMETER_SCHEDULING;
    public static final String PARAM_RECURRENCE_ID = "recurrenceId";
    public static final String PARAM_RECURRENCE_RANGE = "recurrenceRange";
    public static final String PARAM_SEQUENCE = "sequence";
    public static final String PARAM_PUSH_TOKEN = PARAMETER_PUSH_TOKEN;
    public static final String PARAM_FIELDS = PARAMETER_FIELDS;
    public static final String PARAM_ORDER_BY = "sort";
    public static final String PARAM_ORDER = PARAMETER_ORDER;
    public static final String PARAM_UPDATE_CACHE = PARAMETER_UPDATE_CACHE;
    public static final String PARAM_MASK_UID = PARAMETER_MASK_UID;
    public static final String PARAM_LEFT_HAND_LIMIT = PARAMETER_LEFT_HAND_LIMIT;
    public static final String PARAM_RIGHT_HAND_LIMIT = PARAMETER_RIGHT_HAND_LIMIT;

    /**
     * Parses any required and optional parameter values as supplied by the client, throwing an appropriate exception in case a parameter
     * cannot be parsed or a required parameter is missing.
     *
     * @param requestData The underlying request data to parse the parameters from
     * @param requiredParameters The required parameters to parse, or an empty set if there are none
     * @param optionalParameters The optional parameters to parse, or an empty set if there are none
     * @return The parsed calendar parameters
     */
    public static CalendarParameters parseParameters(AJAXRequestData requestData, Set<String> requiredParameters, Set<String> optionalParameters) throws OXException {
        Set<String> parameters = new HashSet<String>();
        parameters.addAll(requiredParameters);
        parameters.addAll(optionalParameters);
        DefaultCalendarParameters calendarParameters = new DefaultCalendarParameters();
        for (String parameter : parameters) {
            Entry<String, ?> entry = parseParameter(requestData, parameter, requiredParameters.contains(parameter));
            if (null != entry) {
                calendarParameters.set(entry.getKey(), entry.getValue());
            }
        }
        return calendarParameters;
    }

    /**
     * Parses a certain parameter value from the given request.
     *
     * @param request The request
     * @param parameter The parameter name
     * @param required Defines if the parameter is required
     * @return The parsed value, mapped to its calendar parameter name, or <code>null</code> if not set and not required
     * @throws OXException if the parameter is required and can't be found or if the parameter can't be parsed
     */
    public static Entry<String, ?> parseParameter(AJAXRequestData request, String parameter, boolean required) throws OXException {
        String value = request.getParameter(parameter);
        if (Strings.isEmpty(value)) {
            if (false == required) {
                return null;
            }
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(parameter);
        }
        try {
            return parseParameter(parameter, value);
        } catch (IllegalArgumentException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(e, parameter, value);
        }
    }

    private static Entry<String, ?> parseParameter(String parameter, String value) throws IllegalArgumentException {
        switch (parameter) {
            case PARAM_RANGE_START:
                DateTime startTime = DateTime.parse(TimeZones.UTC, value);
                return new AbstractMap.SimpleEntry<String, Date>(PARAMETER_RANGE_START, new Date(startTime.getTimestamp()));
            case PARAM_RANGE_END:
                DateTime endTime = DateTime.parse(TimeZones.UTC, value);
                return new AbstractMap.SimpleEntry<String, Date>(PARAMETER_RANGE_END, new Date(endTime.getTimestamp()));
            case PARAM_EXPAND:
                return new AbstractMap.SimpleEntry<String, Boolean>(PARAMETER_EXPAND_OCCURRENCES, Boolean.valueOf(value));
            case PARAM_CHECK_CONFLICTS:
                return new AbstractMap.SimpleEntry<String, Boolean>(PARAMETER_CHECK_CONFLICTS, Boolean.valueOf(value));
            case PARAM_ORDER_BY:
                EventField mappedField = EventMapper.getInstance().getMappedField(value);
                if (mappedField == null) {
                    mappedField = EventField.valueOf(value.toUpperCase());
                }
                return new AbstractMap.SimpleEntry<String, EventField>(PARAMETER_ORDER_BY, mappedField);
            case PARAM_ORDER:
                return new AbstractMap.SimpleEntry<String, SortOrder.Order>(PARAMETER_ORDER, SortOrder.Order.parse(value, SortOrder.Order.ASC));
            case PARAM_FIELDS:
                return new AbstractMap.SimpleEntry<String, EventField[]>(PARAMETER_FIELDS, parseFields(value));
            case PARAM_SCHEDULING:
                SchedulingControl schedulingControl = new SchedulingControl(value);
                if (false == schedulingControl.isStandard()) {
                    throw new IllegalArgumentException("Unexpected scheduling control value \"" + value + "\"");
                }
                return new AbstractMap.SimpleEntry<String, SchedulingControl>(PARAMETER_SCHEDULING, schedulingControl);
            case PARAM_MASK_UID:
                return new AbstractMap.SimpleEntry<String, String>(PARAMETER_MASK_UID, value);
            case PARAM_PUSH_TOKEN:
                return new AbstractMap.SimpleEntry<String, String>(PARAMETER_PUSH_TOKEN, value);
            case PARAM_UPDATE_CACHE:
                return new AbstractMap.SimpleEntry<String, Boolean>(PARAMETER_UPDATE_CACHE, Boolean.valueOf(value));
            case PARAM_LEFT_HAND_LIMIT:
                return new AbstractMap.SimpleEntry<String, Integer>(PARAMETER_LEFT_HAND_LIMIT, Integer.valueOf(value));
            case PARAM_RIGHT_HAND_LIMIT:
                return new AbstractMap.SimpleEntry<String, Integer>(PARAMETER_RIGHT_HAND_LIMIT, Integer.valueOf(value));
            default:
                return null;
        }
    }

    private static EventField[] parseFields(String value) {
        if (Strings.isEmpty(value)){
            return new EventField[0];
        }

        String[] splitByColon = Strings.splitByComma(value);
        EventField[] fields = new EventField[splitByColon.length];
        int x=0;
        for(String str: splitByColon){
            EventField mappedField = EventMapper.getInstance().getMappedField(str);
            if (mappedField == null) {
                mappedField = EventField.valueOf(str.toUpperCase());
            }
            fields[x++] = mappedField;
        }
        return fields;
    }

}

