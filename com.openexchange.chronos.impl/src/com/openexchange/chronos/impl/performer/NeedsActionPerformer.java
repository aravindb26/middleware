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

import static com.openexchange.chronos.common.CalendarUtils.getEventsByUID;
import static com.openexchange.chronos.common.CalendarUtils.getFields;
import static com.openexchange.folderstorage.Permission.WRITE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.tools.functions.ErrorAwareSupplier;
import net.fortuna.ical4j.model.Date;

/**
 * {@link NeedsActionPerformer}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.4
 */
public class NeedsActionPerformer extends AbstractSearchPerformer {

    /** The participation status used when looking up events needing action */
    private static final ParticipationStatus[] PARTSTAT_NEEDS_ACTION = new ParticipationStatus[] { ParticipationStatus.NEEDS_ACTION };

    /**
     * Initializes a new {@link NeedsActionPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public NeedsActionPerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    // @formatter:off
    private static final EventField[] QUERY_FIELDS = new EventField[] { EventField.UID, EventField.SUMMARY, EventField.FOLDER_ID, EventField.LOCATION,
        EventField.DESCRIPTION, EventField.ATTACHMENTS, EventField.GEO, EventField.ORGANIZER, EventField.START_DATE, EventField.END_DATE,
        EventField.TRANSP, EventField.RECURRENCE_RULE
    };
    // @formatter:on

    /**
     * Looks up all events that 'need action' of the current session user, optionally including also events from other calendar users the
     * user has delegate access to (via shared folders, or due to delegate scheduling privileges).
     *
     * @param includeDelegates <code>true</code> to include delegates, <code>false</code> to only consider events from the current session user
     * @return The events needing action, mapped to the corresponding attendees
     */
    public Map<Attendee, EventsResult> perform(boolean includeDelegates) throws OXException {
        Map<Attendee, EventsResult> eventsPerCalendarUser = new HashMap<>();
        EventField[] fields = getFields(session, QUERY_FIELDS);
        /*
         * always get events needing action of current session user itself
         */
        eventsPerCalendarUser.put(getAttendeeNeedingAction(CalendarUserType.INDIVIDUAL, session.getUserId()), getEventsNeedingActionForCurrentUser(fields));
        if (includeDelegates) {
            /*
             * collect events needing action for resources the user has booking delegate privileges for
             */
            for (int resourceId : session.getEntityResolver().getResourcesWithDelegatePrivilege(session.getUserId())) {
                Attendee attendee = getAttendeeNeedingAction(CalendarUserType.RESOURCE, resourceId);
                eventsPerCalendarUser.put(attendee, getEventsNeedingActionForResource(resourceId, fields));
            }
            /*
             * collect events needing action for other users where the current session user has delegate access to in at least one folder
             */
            for (Entry<Integer, List<CalendarFolder>> foldersByUserId : getSharedFoldersWithWritePermissionsByUserId().entrySet()) {
                Attendee attendee = getAttendeeNeedingAction(CalendarUserType.INDIVIDUAL, i(foldersByUserId.getKey()));
                eventsPerCalendarUser.put(attendee, getEventsNeedingActionForOtherUser(attendee.getEntity(), foldersByUserId.getValue(), fields));
            }
        }
        return eventsPerCalendarUser;
    }

    /**
     * Looks up the events needing action for a certain resource from the storage and wraps them into an appropriate result.
     *
     * @param resourceId The identifier of the resource to get the events needing action for
     * @param requestedFields The fields to retrieve when loading the events
     * @return The events needing action, wrapped into an appropriate result
     */
    private EventsResult getEventsNeedingActionForResource(int resourceId, EventField[] requestedFields) {
        return getEventsResult(() -> reduceEventsNeedingAction(getEventsByUID(getEventsOfResource(resourceId, null, PARTSTAT_NEEDS_ACTION, null, requestedFields), true)));
    }

    /**
     * Looks up the events needing action for the current session user from the storage and wraps them into an appropriate result.
     *
     * @param requestedFields The fields to retrieve when loading the events
     * @return The events needing action, wrapped into an appropriate result
     */
    private EventsResult getEventsNeedingActionForCurrentUser(EventField[] requestedFields) {
        return getEventsResult(() -> reduceEventsNeedingAction(getEventsByUID(getEventsOfUser(null, PARTSTAT_NEEDS_ACTION, null, null, requestedFields), true)));
    }

    /**
     * Looks up the events needing action for a particular user from the storage and wraps them into an appropriate result.
     * <p/>
     * Only events that are actually <i>writable</i> by the current session user are considered implicitly.
     *
     * @param userId The identifier of the user to get the events needing action for
     * @param folders The calendar folders to consider
     * @param requestedFields The fields to retrieve when loading the events
     * @return The events needing action, wrapped into an appropriate result
     */
    private EventsResult getEventsNeedingActionForOtherUser(int userId, List<CalendarFolder> folders, EventField[] requestedFields) {
        return getEventsResult(() -> {
            List<Event> eventsNeedingAction = new ArrayList<>();
            for (CalendarFolder folder : folders) {
                List<Event> eventsOfUser = getEventsOfUser(userId, null, PARTSTAT_NEEDS_ACTION, folder, requestedFields);
                for (List<Event> eventGroup : getEventsByUID(eventsOfUser, true).values()) {
                    if (null == eventGroup || eventGroup.isEmpty() || false == isWritable(eventGroup, folder)) {
                        continue;
                    }
                    eventsNeedingAction.addAll(reduceEventsNeedingAction(eventGroup));
                }
            }
            return eventsNeedingAction;
        });
    }

    /**
     * Wraps a list of events into an appropriate events result, considering an {@link OXException} from invoking the supplier as well as
     * appropriate error result.
     *
     * @param eventsSupplier To supply the events for the result
     * @return The events result, containing either the events, or the error if one occurred when getting them
     */
    private static EventsResult getEventsResult(ErrorAwareSupplier<List<Event>> eventsSupplier) {
        try {
            return new DefaultEventsResult(eventsSupplier.get());
        } catch (OXException e) {
            return new DefaultEventsResult(e);
        }
    }

    /**
     * Collects all folders shared by other users where the current session user has at least <i>write own</i> permissions for.
     *
     * @return The shared folders in lists, associated to the identifier of the sharing user
     * @throws OXException If folders cannot be retrieved properly
     */
    private Map<Integer, List<CalendarFolder>> getSharedFoldersWithWritePermissionsByUserId() throws OXException {
        /*
         * collect shared folders the user has 'write' permissions for
         */
        Map<Integer, List<CalendarFolder>> foldersByUserId = new HashMap<>();
        for (CalendarFolder folder : getFolderChooser().getVisibleFolders()) {
            if (SharedType.getInstance().equals(folder.getType()) && WRITE_OWN_OBJECTS <= folder.getOwnPermission().getWritePermission()) {
                com.openexchange.tools.arrays.Collections.put(foldersByUserId, I(folder.getCalendarUserId()), folder);
            }
        }
        return foldersByUserId;
    }

    /**
     * Initializes a new {@link Attendee} object, sets the passed entity and calendar user type properties and lets its other properties
     * be filled by the entity resolver. Also, a fixed participation status of {@link ParticipationStatus#NEEDS_ACTION} is assigned, so
     * that the attendee is ready to use in needs-action results.
     *
     * @param cuType The target calendar user type to initialize the attendee for
     * @param entity The internal entity identifier to initialize the attendee for
     * @return The attendee
     * @throws OXException If the attendee cannot be resolved
     */
    private Attendee getAttendeeNeedingAction(CalendarUserType cuType, int entity) throws OXException {
        Attendee attendee = new Attendee();
        attendee.setPartStat(ParticipationStatus.NEEDS_ACTION);
        attendee.setEntity(entity);
        attendee.setCuType(cuType);
        return session.getEntityResolver().applyEntityData(attendee);
    }

    /**
     * Reduces the technically based list of events that need action to a minimum list that really needs action by a user.
     * <p/>
     * If multiple overridden instances of a recurring event series could be reduced into the series master event, this series master
     * event's change exception dates are adjusted implicitly so that they no longer list those exception dates that were skipped.
     *
     * @param eventGroup The events from a calendar object resource (event series including any change exceptions)
     * @return {@link List} of {@link Event}s that need user action
     */
    protected List<Event> reduceEventsNeedingAction(List<Event> series) throws OXException {
        if (series == null || series.size() == 0 || series.size() == 1) {
            return series;
        }
        List<Event> sortedSeries = CalendarUtils.sortSeriesMasterFirst(series);
        Event master = sortedSeries.get(0);
        if (false == CalendarUtils.isSeriesMaster(master)) {
            return series;
        }
        List<Event> filteredEvents = new ArrayList<>(series.size());
        SortedSet<RecurrenceId> newChangeExceptionDates = new TreeSet<>();
        if (null != master.getChangeExceptionDates()) {
            newChangeExceptionDates.addAll(master.getChangeExceptionDates());
        }
        Date timestamp = new Date();
        for (int i = 1; i < sortedSeries.size(); i++) {
            Event event = sortedSeries.get(i);
            if (false == Objects.equals(master.getSeriesId(), event.getSeriesId())) {
                session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create("Skipping unrelated " + event));
                continue;
            }
            Event originalOccurrence = prepareException(master, event.getRecurrenceId(), event.getId(), timestamp);
            /*
             * include re-scheduled change exceptions, otherwise skip & remove from master's change exception dates
             */
            if (Utils.isReschedule(originalOccurrence, event)) {
                filteredEvents.add(event);
            } else {
                newChangeExceptionDates.removeIf(r -> r.matches(event.getRecurrenceId()));
            }
        }
        master.setChangeExceptionDates(newChangeExceptionDates);
        filteredEvents.add(master);
        return filteredEvents;
    }

    /**
     * Reduces the technically based list of events that need action to a minimum list that really needs action by a user.
     * <p/>
     * If multiple overridden instances of a recurring event series could be reduced into the series master event, this series master
     * event's change exception dates are adjusted implicitly so that they no longer list those exception dates that were skipped.
     *
     * @param eventsByUID {@link Map} containing all events in a mapped way which means each event series including any change exceptions are grouped separately.
     * @return {@link List} of {@link Event}s that need user action
     * @throws OXException
     */
    protected List<Event> reduceEventsNeedingAction(Map<String, List<Event>> eventsByUID) throws OXException {
        if (eventsByUID == null || eventsByUID.size() == 0) {
            return Collections.emptyList();
        }
        List<Event> filteredEvents = new ArrayList<>();
        for (List<Event> series : eventsByUID.values()) {
            List<Event> eventsNeedingAction = reduceEventsNeedingAction(series);
            if (null != eventsNeedingAction) {
                filteredEvents.addAll(eventsNeedingAction);
            }
        }
        return filteredEvents;
    }

    /**
     * Gets a value indicating whether the user is able to <i>write</i> all events in the supplied event group or not.
     *
     * @param eventGroup The events to check
     * @param folder The folder representing the user's view on the events
     * @return <code>true</code> if the events are writable, <code>false</code>, otherwise
     */
    private static boolean isWritable(List<Event> eventGroup, CalendarFolder folder) {
        int writePermission = folder.getOwnPermission().getWritePermission();
        if (WRITE_ALL_OBJECTS == writePermission) {
            return true;
        }
        if (WRITE_OWN_OBJECTS == writePermission) {
            int userId = folder.getSession().getUserId();
            for (Event event : eventGroup) {
                if (null == event.getCreatedBy() || userId != event.getCreatedBy().getEntity()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
