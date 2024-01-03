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

package com.openexchange.chronos.json.converter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.chronos.service.RecurrenceInfo;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link RecurrenceInfoResultConverter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RecurrenceInfoResultConverter extends EventResultConverter {

    @SuppressWarnings("hiding")
    public static final String INPUT_FORMAT = "eventRecurrenceInfo";

    /**
     * Initializes a new {@link RecurrenceInfoResultConverter}.
     *
     * @param services A service lookup reference
     */
    public RecurrenceInfoResultConverter(ServiceLookup services) {
        super(services);
    }

    @Override
    public String getInputFormat() {
        return INPUT_FORMAT;
    }

    @Override
    public void convert(AJAXRequestData requestData, AJAXRequestResult result, ServerSession session, Converter converter) throws OXException {
        /*
         * check and convert result object
         */
        Object resultObject = result.getResultObject();
        if (false == (resultObject instanceof RecurrenceInfo)) {
            throw new UnsupportedOperationException();
        }
        resultObject = convertRecurrenceInfo((RecurrenceInfo) resultObject, getTimeZoneID(requestData, session), session, isExtendedEntities(requestData));
        result.setResultObject(resultObject, getOutputFormat());
    }

    private JSONValue convertRecurrenceInfo(RecurrenceInfo recurrenceInfo, String timeZoneID, ServerSession session, boolean extendedEntities) throws OXException {
        if (null == recurrenceInfo) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(4);
        try {
            jsonObject.put("overridden", recurrenceInfo.isOverridden());
            jsonObject.put("rescheduled", recurrenceInfo.isRescheduled());
            jsonObject.putOpt("recurrenceEvent", convertEvent(recurrenceInfo.getRecurrence(), timeZoneID, session, null, extendedEntities));
            jsonObject.putOpt("masterEvent", convertEvent(recurrenceInfo.getSeriesMaster(), timeZoneID, session, null, extendedEntities));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
        return jsonObject;
    }

}
