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

import static com.openexchange.chronos.common.CalendarUtils.getFields;
import static com.openexchange.chronos.common.CalendarUtils.getObjectIDs;
import static com.openexchange.chronos.common.CalendarUtils.isClassifiedFor;
import static com.openexchange.chronos.common.CalendarUtils.isInRange;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.SearchUtils.getSearchTerm;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getFolderIdTerm;
import static com.openexchange.chronos.impl.Utils.getTimeZone;
import static com.openexchange.chronos.impl.Utils.isResolveOccurrences;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.arrays.Arrays.contains;
import static com.openexchange.tools.arrays.Arrays.containsAll;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.Check;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link AllPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class AllPerformer extends AbstractSearchPerformer {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AllPerformer.class);

    /**
     * Initializes a new {@link AllPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public AllPerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Performs the operation.
     *
     * @return The loaded events
     */
    public List<Event> perform() throws OXException {
        return perform(null, null);
    }

    /**
     * Performs the operation.
     *
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @return The loaded events
     */
    public List<Event> perform(Boolean rsvp, ParticipationStatus[] partStats) throws OXException {
        return getEventsOfUser(rsvp, partStats, null);
    }

    /**
     * Performs the operation.
     *
     * @param folderId The identifier of the parent folder to get all events from
     * @return The loaded events
     */
    public List<Event> perform(String folderId) throws OXException {
        CalendarFolder folder = getFolder(session, folderId);
        requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
        /*
         * check for possible shortcut if only id fields are requested and no recurrence expansion takes place
         */
        EventField[] requestedFields = session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class);
        EventField requestedOrderBy = session.get(CalendarParameters.PARAMETER_ORDER_BY, EventField.class);
        if (null != requestedFields && containsAll(requestedFields, EventField.ID, EventField.FOLDER_ID, EventField.SERIES_ID, EventField.UID, EventField.TIMESTAMP) &&
            (null == requestedOrderBy || contains(requestedFields, requestedOrderBy)) && false == isResolveOccurrences(session)) {
            /*
             * search events & directly pass-through results from storage
             */
            return performPassedThrough(folder, requestedFields);
        }
        /*
         * perform default search & userize the results based on the requested folder
         */
        SearchTerm<?> searchTerm = getFolderIdTerm(session, folder);
        EventField[] fields = getFieldsForStorage(requestedFields);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, getSearchOptionsForStorage(session), fields);
        events = storage.getUtilities().loadAdditionalEventData(folder.getCalendarUserId(), events, fields);
        return postProcessor(getObjectIDs(events), folder.getCalendarUserId(), requestedFields, fields).process(events, folder).getEvents();
    }

    /**
     * Performs the operation.
     *
     * @param folderIds The identifiers of the parent folders to get all events from
     * @return The loaded events
     */
    public Map<String, EventsResult> perform(List<String> folderIds) throws OXException {
        if (null == folderIds || folderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        /*
         * get folders and/or directly handle virtual ids, storing a possible exception in the results
         */
        Map<String, EventsResult> resultsPerFolderId = new HashMap<String, EventsResult>(folderIds.size());
        List<CalendarFolder> folders = initializeSearchedFolders(folderIds, null, resultsPerFolderId);
        /*
         * evaluate fields to query from storage based on requested fields
         */
        EventField[] requestedFields = session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class);
        EventField[] fields = getFieldsForStorage(requestedFields);
        SearchOptions searchOptions = getSearchOptionsForStorage(session);
        /*
         * load event data per folder & additional event data per calendar user
         */
        for (Entry<Integer, List<CalendarFolder>> entry : getFoldersPerCalendarUserId(folders).entrySet()) {
            int calendarUserId = i(entry.getKey());
            List<Event> eventsForCalendarUser = new ArrayList<Event>();
            Map<CalendarFolder, List<Event>> eventsPerFolder = new HashMap<CalendarFolder, List<Event>>(entry.getValue().size());
            for (CalendarFolder folder : entry.getValue()) {
                /*
                 * load events in folder
                 */
                try {
                    requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
                    List<Event> eventsInFolder = storage.getEventStorage().searchEvents(getFolderIdTerm(session, folder), searchOptions, fields);
                    eventsForCalendarUser.addAll(eventsInFolder);
                    eventsPerFolder.put(folder, eventsInFolder);
                } catch (OXException e) {
                    LOG.debug("Error loading events for folder {}", folder, e);
                    resultsPerFolderId.put(folder.getId(), new DefaultEventsResult(e));
                }
            }
            /*
             * batch-load additional event data for this calendar user & prepare associated event post-processor
             */
            storage.getUtilities().loadAdditionalEventData(calendarUserId, eventsForCalendarUser, fields);
            EventPostProcessor postProcessor = postProcessor(getObjectIDs(eventsForCalendarUser), calendarUserId, requestedFields, fields);
            /*
             * post process events per folder, based on each requested folder's perspective
             */
            for (Entry<CalendarFolder, List<Event>> eventsInFolder : eventsPerFolder.entrySet()) {
                CalendarFolder folder = eventsInFolder.getKey();
                try {
                    resultsPerFolderId.put(folder.getId(), postProcessor.process(eventsInFolder.getValue(), folder).getEventsResult());
                } catch (OXException e) {
                    LOG.debug("Error loading events for folder {}", folder, e);
                    resultsPerFolderId.put(folder.getId(), new DefaultEventsResult(e));
                } finally {
                    postProcessor.reset();
                }
                Check.resultSizeNotExceeded(getSelfProtection(), resultsPerFolderId, requestedFields);
            }
        }
        return resultsPerFolderId;
    }

    /**
     * Gets all events the current session user attends.
     *
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @param folderTypes The folder types to include, or <code>null</code> to include all events independently of the type of the folder
     *            they're located in
     * @return The loaded events
     */
    private List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats, Type[] folderTypes) throws OXException {
        return getEventsOfUser(rsvp, partStats, folderTypes, null);
    }

    /**
     * Performs the 'all' request in a more efficient mode where event data loaded from the storage is passed through (almost) as-is.
     * <p/>
     * May only be used if a very limited number of fields is requested by the client, and no post-processing like expanding recurring
     * event series or applying personal alarms is required.
     *
     * @param folder The calendar folder to get the event data from
     * @param requestedFields The fields as requested by the client
     * @return The loaded events
     */
    private List<Event> performPassedThrough(CalendarFolder folder, EventField[] requestedFields) throws OXException {
        /*
         * ensure to retrieve classification in shared folders to remove classified events later on
         */
        EventField[] queriedFields = requestedFields;
        if (SharedType.getInstance().equals(folder.getType()) && false == contains(queriedFields, EventField.CLASSIFICATION)) {
            queriedFields = Arrays.add(queriedFields, EventField.CLASSIFICATION);
        }
        /*
         * ensure to retrieve date/time-related fields if a range is specified
         */
        SearchOptions searchOptions = new SearchOptions(session);
        if (null != searchOptions.getFrom() || null != searchOptions.getUntil()) {
            queriedFields = Arrays.add(queriedFields, EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_ID, EventField.START_DATE, EventField.END_DATE,
                EventField.RECURRENCE_RULE, EventField.RECURRENCE_DATES, EventField.DELETE_EXCEPTION_DATES, EventField.CHANGE_EXCEPTION_DATES);
        }
        /*
         * load events in folder from storage
         */
        SearchTerm<?> searchTerm = getFolderIdTerm(session, folder);
        if (false == PublicType.getInstance().equals(folder.getType())) {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(searchTerm)
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(getSearchTerm(AttendeeField.HIDDEN, SingleOperation.ISNULL))
                    .addSearchTerm(getSearchTerm(AttendeeField.HIDDEN, SingleOperation.EQUALS, Boolean.FALSE)));
        }
        List<Event> loadedEvents = storage.getEventStorage().searchEvents(searchTerm, searchOptions, queriedFields);
        /*
         * perform basic post-processing of event data as necessary
         */
        List<Event> events = new ArrayList<Event>(loadedEvents.size());
        for (Event event : loadedEvents) {
            /*
             * re-check that event falls within requested time range
             */
            if (null != searchOptions.getFrom() || null != searchOptions.getUntil()) {
                if (isSeriesMaster(event)) {
                    if (false == session.getRecurrenceService().iterateEventOccurrences(event, searchOptions.getFrom(), searchOptions.getUntil()).hasNext()) {
                        continue;
                    }
                } else if (false == isInRange(event, searchOptions.getFrom(), searchOptions.getUntil(), getTimeZone(session))) {
                    continue;
                }
            }
            /*
             * remove 'private' event in shared folder if classified for requesting user
             */
            if (Classification.PRIVATE.equals(event.getClassification()) && SharedType.getInstance().equals(folder.getType())) {
                event = storage.getEventStorage().loadEvent(event.getId(), getFields(requestedFields));
                if (event == null) {
                    continue;
                }
                event.setAttendees(storage.getAttendeeStorage().loadAttendees(event.getId()));
                if (isClassifiedFor(event, session.getUserId())) {
                    continue;
                }
            }
            /*
             * take over only the requested fields for resulting event list
             */
            event.setFolderId(folder.getId());
            events.add(EventMapper.getInstance().copy(event, null, true, requestedFields));
        }
        return events;
    }

}
