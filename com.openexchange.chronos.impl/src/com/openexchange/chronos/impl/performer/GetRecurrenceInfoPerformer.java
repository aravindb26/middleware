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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.getOccurrence;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.impl.Utils.getCalendarUserId;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.isInFolder;
import static com.openexchange.chronos.impl.Utils.isReschedule;
import static com.openexchange.chronos.impl.Utils.isVisible;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultRecurrenceInfo;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.RecurrenceInfo;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link GetRecurrenceInfoPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class GetRecurrenceInfoPerformer extends AbstractQueryPerformer {

    /**
     * Initializes a new {@link GetRecurrenceInfoPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public GetRecurrenceInfoPerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Performs the operation.
     *
     * @param folderId The identifier of the parent folder to read the event in
     * @param eventId The identifier of the event to get the recurrence info for
     * @param recurrenceId The recurrence identifier of the occurrence to get, optionally <code>null</code> if the targeted event is already an exception
     * @return The recurrence info
     */
    public RecurrenceInfo perform(String folderId, String eventId, RecurrenceId recurrenceId) throws OXException {
        /*
         * load targeted event from storage, check permissions & userize
         */
        CalendarFolder folder = getFolder(session, folderId, false, true);
        EventField[] fields = null;
        Event event = checkAndUserize(folder, storage.getEventStorage().loadEvent(eventId, fields), fields);
        if (null == event) {
            throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(eventId);
        }
        /*
         * determine series master and recurrence based on event targeted by client
         */
        Event recurrenceEvent = null;
        Event masterEvent = null;
        if (isSeriesMaster(event)) {
            /*
             * event id points to series master event, lookup targeted recurrence
             */
            if (null == recurrenceId) {
                throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create(eventId, recurrenceId);
            }
            masterEvent = event;
            if (contains(masterEvent.getChangeExceptionDates(), recurrenceId)) {
                recurrenceEvent = checkAndUserize(folder, storage.getEventStorage().loadException(eventId, recurrenceId, fields), fields);
            } else {
                recurrenceEvent = getOccurrence(session.getRecurrenceService(), masterEvent, recurrenceId);
            }
            if (null == recurrenceEvent || false == recurrenceId.matches(recurrenceEvent.getRecurrenceId())) {
                throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(eventId, recurrenceId);
            }
        } else if (isSeriesException(event)) {
            /*
             * event id points to change exception event, lookup series master
             */
            if (null != recurrenceId && false == recurrenceId.matches(event.getRecurrenceId()) || null == event.getRecurrenceId()) {
                throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(eventId, recurrenceId);
            }
            recurrenceEvent = event;
            masterEvent = checkAndUserize(folder, storage.getEventStorage().loadEvent(recurrenceEvent.getSeriesId(), fields), fields);
        } else {
            throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(eventId, recurrenceId);
        }
        /*
         * evaluate & return appropriate recurrence info result
         */
        return getRecurrenceInfo(masterEvent, recurrenceEvent);
    }

    /**
     * Creates a recurrence info result for the supplied series master and recurrence event instances.
     * 
     * @param masterEvent The series master event, or <code>null</code> if there is none
     * @param recurrenceEvent The event recurrence
     * @return The recurrence info result
     */
    private RecurrenceInfo getRecurrenceInfo(Event masterEvent, Event recurrenceEvent) throws OXException {
        if (null == masterEvent) {
            return new DefaultRecurrenceInfo(true, true, masterEvent, recurrenceEvent); // orphaned exception w/o access to master
        }
        if (recurrenceEvent.getId().equals(recurrenceEvent.getSeriesId())) {
            return new DefaultRecurrenceInfo(false, false, masterEvent, recurrenceEvent); // non-overridden instance
        }
        boolean rescheduled = isReschedule(prepareOccurrence(masterEvent, recurrenceEvent.getRecurrenceId()), recurrenceEvent);
        return new DefaultRecurrenceInfo(true, rescheduled, masterEvent, recurrenceEvent); // potentially re-scheduled, overridden instance
    }

    /**
     * Loads additional event data for the supplied event, checks access permissions and <i>userizes</i> the event for the current
     * session user.
     * 
     * @param folder The calendar folder the event is read in
     * @param event The event to check and userize
     * @param fields The additional event fields to load, or <code>null</code> to load all available data
     * @return The userized event, or <code>null</code> if there was none
     */
    private Event checkAndUserize(CalendarFolder folder, Event event, EventField[] fields) throws OXException {
        if (null == event) {
            return null;
        }
        event = storage.getUtilities().loadAdditionalEventData(getCalendarUserId(folder), event, fields);
        if (false == isInFolder(event, folder) || false == isVisible(folder, event)) {
            return null;
        }
        return postProcessor().process(event, folder).getFirstEvent();
    }

}
