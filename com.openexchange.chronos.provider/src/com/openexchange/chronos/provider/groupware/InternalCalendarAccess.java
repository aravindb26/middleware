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

package com.openexchange.chronos.provider.groupware;

import java.util.List;
import java.util.Map;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.provider.extensions.PermissionAware;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.UpdatesResult;
import com.openexchange.exception.OXException;

/**
 * {@link InternalCalendarAccess}
 * 
 * The {@link GroupwareCalendarAccess} for the default internal calendar.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.5
 */
public interface InternalCalendarAccess extends GroupwareCalendarAccess, PermissionAware {

    /**
     * Gets the default calendar folder.
     *
     * @return The default folder
     */
    GroupwareCalendarFolder getDefaultFolder() throws OXException;

    /**
     * Gets all events of the session's user.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER_BY}</li>
     * <li>{@link CalendarParameters#PARAMETER_EXPAND_OCCURRENCES}</li>
     * </ul>
     *
     * @return The events
     */
    List<Event> getEventsOfUser() throws OXException;

    /**
     * Looks up all events that 'need action' of the current session user, optionally including also events from other calendar users the
     * user has delegate access to (via shared folders, or due to delegate scheduling privileges on resources).
     * <p/>
     * Overridden instances of recurring event series are only included in the response, if they're considered as <i>re-scheduled</i>
     * compared to the regular event series and therefore won't be affected by accepting/declining the whole series implicitly.
     * <p/>
     * <b>Note:</b> Only events from the internal <i>groupware</i> calendar provider are considered.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER_BY}</li>
     * <li>{@link CalendarParameters#PARAMETER_RIGHT_HAND_LIMIT}</li>
     * <li>{@link CalendarParameters#PARAMETER_LEFT_HAND_LIMIT}</li>
     * </ul>
     * 
     * @param includeDelegates <code>true</code> to include delegates, <code>false</code> to only consider events from the current session user
     * @return The events needing action, mapped to the corresponding attendees
     */
    Map<Attendee, EventsResult> getEventsNeedingAction(boolean includeDelegates) throws OXException;

    /**
     * Gets all events the session's user attends in, having a particular participation status.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER_BY}</li>
     * <li>{@link CalendarParameters#PARAMETER_RIGHT_HAND_LIMIT}</li>
     * <li>{@link CalendarParameters#PARAMETER_LEFT_HAND_LIMIT}</li>
     * </ul>
     *
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @return The events
     */
    List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats) throws OXException;

    /**
     * Gets lists of new and updated as well as deleted events since a specific timestamp of a user.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE} ("changed" and "deleted")</li>
     * <li>{@link CalendarParameters#PARAMETER_EXPAND_OCCURRENCES}</li>
     * </ul>
     *
     * @param updatedSince The timestamp since when the updates should be retrieved
     * @return The updates result yielding lists of new/modified and deleted events
     */
    UpdatesResult getUpdatedEventsOfUser(long updatedSince) throws OXException;

    /**
     * Resolves an event identifier to an event, and returns it in the perspective of the calendar session's user, i.e. having an
     * appropriate parent folder identifier assigned.
     *
     * @param eventId The identifier of the event to resolve
     * @param sequence The expected sequence number to match, or <code>null</code> to resolve independently of the event's sequence number
     * @return The resolved event from the user's point of view, or <code>null</code> if not found
     */
    Event resolveEvent(String eventId, Integer sequence) throws OXException;

    /**
     * Create a patched scheduling messages based on the given mail
     *
     * @param mailAccountId The mail account identifier
     * @param mailFolderId The folder identifier the mail is in
     * @param mailId The mail identifier
     * @param sequenceId The sequence identifier of the mails attachment the iCAL is in, can be <code>null</code>
     * @return A parsed and patched message
     * @throws OXException In case message can't be parsed
     */
    IncomingSchedulingMessage createPatchedMessage(int mailAccountId, String mailFolderId, String mailId, String sequenceId) throws OXException;

    /**
     * Analysis the given message
     *
     * @param message The message to analyze
     * @return The analysis result as {@link ITipAnalysis}
     * @throws OXException In case message can't be analyzed
     */
    ITipAnalysis analyze(IncomingSchedulingMessage message) throws OXException;

    /**
     * Handles the incoming scheduling messages and applies it to the calendar
     *
     * @param source The API source the scheduling is performed from
     * @param message The message to process
     * @param attendee The attendee to update along the operation, can be <code>null</code>
     * @return A calendar result of the operation
     * @throws OXException In case the message can't be processed
     */
    CalendarResult handleIncomingScheduling(SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException;

}
