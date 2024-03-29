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

package com.openexchange.chronos.itip.json.action;

import java.util.Date;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.json.converter.CalendarResultConverter;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.chronos.scheduling.MessageStatusService;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link ApplyAction}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class ApplyAction extends AbstractSchedulingAction {

    /**
     * Initializes a new {@link ApplyAction}.
     * 
     * @param services The service lookup to use
     */
    public ApplyAction(ServiceLookup services) {
        super(services);
    }
    
    @Override
    AJAXRequestResult process(AJAXRequestData request, IDBasedCalendarAccess access, IncomingSchedulingMessage message) throws OXException {
        /*
         * Send to scheduling broker, without any attendee to update, mark message as processed afterwards
         */
        CalendarResult calendarResult = access.getSchedulingAccess().handleIncomingScheduling(SchedulingSource.API, message);
        if (null == calendarResult) {
            return new AJAXRequestResult(null, new Date());
        }
        services.getServiceSafe(MessageStatusService.class).setMessageStatus(access.getSession(), message, MessageStatus.APPLIED);
        return new AJAXRequestResult(calendarResult, new Date(calendarResult.getTimestamp()), CalendarResultConverter.INPUT_FORMAT);
    }

}
