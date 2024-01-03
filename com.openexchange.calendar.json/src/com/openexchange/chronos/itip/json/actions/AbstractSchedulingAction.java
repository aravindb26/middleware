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

import static com.openexchange.mail.utils.MailFolderUtility.prepareMailFolderParam;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.scheduling.IncomingSchedulingMailFactory;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 *
 * {@link AbstractSchedulingAction}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public abstract class AbstractSchedulingAction implements AJAXActionService {

    private static final String CONVERSION_MAIL_ID = "com.openexchange.mail.conversion.mailid";
    private static final String CONVERSION_MAIL_FOLDER = "com.openexchange.mail.conversion.fullname";
    private static final String CONVERSION_SEQUENCE_ID = "com.openexchange.mail.conversion.sequenceid";

    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractSchedulingAction}.
     *
     * @param services The service lookup to use
     */
    public AbstractSchedulingAction(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        /*
         * Build message
         */
        /*
         * Get request payload
         */
        Object data = requestData.getData();
        if (data == null) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }
        if (false == data instanceof JSONObject) {
            throw AjaxExceptionCodes.INVALID_JSON_REQUEST_BODY.create();
        }
        /*
         * Parse and build message
         */
        try {
            JSONObject body = (JSONObject) data;
            FullnameArgument fullnameArgument = prepareMailFolderParam(body.getString(CONVERSION_MAIL_FOLDER));
            int accountId = fullnameArgument.getAccountId();
            String folderId = fullnameArgument.getFullname();
            String mailId = body.getString(CONVERSION_MAIL_ID);
            String sequenceId = body.optString(CONVERSION_SEQUENCE_ID);

            IncomingSchedulingMailFactory factory = services.getServiceSafe(IncomingSchedulingMailFactory.class);
            CalendarSession calendarSession = Utils.initCalendarSession(services, session);
            IncomingSchedulingMessage message = factory.createPatched(calendarSession, accountId, folderId, mailId, sequenceId);
            return process(requestData, calendarSession, message);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Processes the given message
     *
     * @param request The original request
     * @param session The calendar session
     * @param message The message to process
     * @return A result
     * @throws OXException In case of error
     */
    abstract AJAXRequestResult process(AJAXRequestData request, CalendarSession session, IncomingSchedulingMessage message) throws OXException;
}
