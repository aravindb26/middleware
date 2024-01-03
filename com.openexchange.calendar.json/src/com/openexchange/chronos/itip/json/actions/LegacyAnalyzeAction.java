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

package com.openexchange.chronos.itip.json.actions;

import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.itip.json.ITipAnalysisWriter;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link LegacyAnalyzeAction}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class LegacyAnalyzeAction extends AbstractSchedulingAction {

    /**
     * Initializes a new {@link LegacyAnalyzeAction}.
     * 
     * @param services The service lookup
     */
    public LegacyAnalyzeAction(ServiceLookup services) {
        super(services);
    }

    @Override
    AJAXRequestResult process(AJAXRequestData requestData, CalendarSession calendarSession, IncomingSchedulingMessage message) throws OXException {
        /*
         * Prepare and perform analyze
         */
        ITipProcessorService processorService = services.getServiceSafe(ITipProcessorService.class);
        ITipAnalysis analyze = processorService.analyze(message, calendarSession);
        if (null != analyze) {
            try {
                JSONObject object = new JSONObject(10);
                ITipAnalysisWriter writer = new ITipAnalysisWriter(Utils.getTimeZone(requestData, calendarSession.getSession()), calendarSession, services);
                writer.write(analyze, object);

                JSONArray jsonArray = new JSONArray(1);
                jsonArray.add(0, object);
                return new AJAXRequestResult(jsonArray, new Date());
            } catch (JSONException e) {
                throw OXException.general(e.getMessage(), e);
            }
        }
        throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Unable to create analysis");
    }

}
