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

import static com.openexchange.chronos.itip.json.action.Utils.convertToResult;
import static com.openexchange.chronos.scheduling.common.Utils.optSentByResource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.chronos.scheduling.MessageStatusService;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CreateResult;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link IncomingSchedulingAction}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class IncomingSchedulingAction extends AbstractSchedulingAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomingSchedulingAction.class);

    private final List<SchedulingMethod> methods;

    /**
     * Initializes a new {@link IncomingSchedulingAction}.
     * 
     * @param method The scheduling method to handle
     * @param services The service lookup
     */
    public IncomingSchedulingAction(ServiceLookup services, SchedulingMethod... method) {
        super(services);
        this.methods = Arrays.asList(method);
    }
    
    @Override
    AJAXRequestResult process(AJAXRequestData requestData, IDBasedCalendarAccess access, IncomingSchedulingMessage message) throws OXException {
        if (canPerform(message)) {
            return perform(requestData, message, access, Utils.getTimeZone(requestData, access.getSession()));
        }
        return null;
    }

    /**
     * Gets a value indicating whether this instance can handle a specific message or not
     *
     * @param message The incoming message to get the method from
     * @return <code>true</code> if this instance can handle the update, <code>false</code> otherwise
     */
    public boolean canPerform(IncomingSchedulingMessage message) {
        return methods.contains(message.getMethod());
    }

    /**
     * Tries to apply the designated method by applying updates to the calendar.
     *
     * @param request The request
     * @param message The incoming message
     * @param access The access to the calendar
     * @param tz The timezone for the user
     * @return Either an result for the client or
     *         <code>null</code> if the request could not be served, e.g. when the method
     *         in the calendar doesn't match the expected method to handle
     * @throws OXException In case of an error while updating
     */
    public AJAXRequestResult perform(AJAXRequestData request, IncomingSchedulingMessage message, IDBasedCalendarAccess access, TimeZone tz) throws OXException {
        /*
         * patch imported calendar & perform scheduling operation
         */
        CalendarResult result = perform(request, message, access);
        if (null == result) {
            return null;
        }
        /*
         * Mark message as processed
         */
        services.getServiceSafe(MessageStatusService.class).setMessageStatus(access.getSession(), message, MessageStatus.APPLIED);
        /*
         * Transform results to align with expected API output
         */
        List<Event> updatedEvents = new ArrayList<>();
        for (CreateResult createResult : result.getCreations()) {
            updatedEvents.add(createResult.getCreatedEvent());
        }
        for (UpdateResult updateResult : result.getUpdates()) {
            updatedEvents.add(updateResult.getUpdate());
        }
        try {
            return convertToResult(access.getSession(), tz, updatedEvents);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e);
        }
    }

    /**
     * Performs the designated method announced set for this instance and performs
     * the corresponding update in the calendar.
     *
     * @param request The request to get the information from
     * @param message The incoming message
     * @param access The access to the calendar
     * @return A {@link CalendarResult} of the update or
     *         <code>null</code> to indicate that no processing has been performed
     * @throws OXException In case of error
     * @see <a href="https://tools.ietf.org/html/rfc5546">RFC 5546</a>
     */
    private CalendarResult perform(AJAXRequestData request, IncomingSchedulingMessage message, IDBasedCalendarAccess access) throws OXException {
        return access.getSchedulingAccess().handleIncomingScheduling(SchedulingSource.API, message, getAttendee(request, message));
    }

    @Override
    protected IDBasedCalendarAccess initAccess(AJAXRequestData requestData) throws OXException {
        IDBasedCalendarAccess calendarAccess = super.initAccess(requestData);
        if (ITipAction.ACCEPT_AND_IGNORE_CONFLICTS.name().equalsIgnoreCase(requestData.getAction())) {
            calendarAccess.set(CalendarParameters.PARAMETER_CHECK_CONFLICTS, Boolean.FALSE);
        }
        if (ITipAction.DECLINECOUNTER.name().equalsIgnoreCase(requestData.getAction())) {
            calendarAccess.set(CalendarParameters.PARAMETER_DECLINE_COUNTER, Boolean.TRUE);
        }
        if (ITipAction.APPLY_PROPOSAL.name().equalsIgnoreCase(requestData.getAction())) {
            calendarAccess.set(CalendarParameters.PARAMETER_COUNTER_FIELDS, new EventField[] { EventField.START_DATE, EventField.END_DATE });
        }
        return calendarAccess;
    }

    /**
     * Prepares and updates the user attendee which is going to reply.
     *
     * @param request The request
     * @param message The message
     * @param session The user session
     * @return The attendee with the new participant status to reply with or <code>null</code> to not trigger a participant status update
     */
    private Attendee getAttendee(AJAXRequestData request, IncomingSchedulingMessage message) throws OXException {
        if (SchedulingMethod.ADD.equals(message.getMethod()) || SchedulingMethod.REQUEST.equals(message.getMethod())) {
            Attendee update = new Attendee();
            String serverUid = services.getServiceSafe(DatabaseService.class).getServerUid();
            int sentByResource = optSentByResource(serverUid, request.getSession().getContextId(), message);
            if (0 < sentByResource) {
                update.setEntity(sentByResource);
                update.setCuType(CalendarUserType.RESOURCE);
            } else {
                update.setEntity(message.getTargetUser());
                update.setCuType(CalendarUserType.INDIVIDUAL);
            }
            update.setPartStat(getPartStat(request.getAction()));
            update.setComment(getComment(request));
            return update;
        }
        return null;
    }

    private static ParticipationStatus getPartStat(String action) {
        switch (action.toLowerCase()) {
            case "accept_and_ignore_conflicts":
            case "accept":
                return ParticipationStatus.ACCEPTED;
            case "tentative":
                return ParticipationStatus.TENTATIVE;
            case "decline":
                return ParticipationStatus.DECLINED;
            default:
                return null;
        }
    }

    private static String getComment(AJAXRequestData request) {
        try {
            return request.getParameter("message", String.class, true);
        } catch (OXException e) {
            LOGGER.debug("Unable to get comment", e);
        }
        return null;
    }

}
