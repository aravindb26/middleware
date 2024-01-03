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

import static com.openexchange.chronos.common.CalendarUtils.getObjectIDs;
import static com.openexchange.chronos.common.CalendarUtils.getSearchTerm;
import static com.openexchange.chronos.common.CalendarUtils.isResourceCalendarFolderId;
import static com.openexchange.chronos.impl.Utils.getCalendarUserId;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getFolderIdTerm;
import static com.openexchange.chronos.impl.Utils.getResourceCalendarFolder;
import static com.openexchange.chronos.impl.Utils.getVisibleFolders;
import static com.openexchange.chronos.impl.Utils.isEnforceDefaultAttendee;
import static com.openexchange.chronos.impl.Utils.parseResourceEntity;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.DefaultCalendarParameters;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link AbstractSearchPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class AbstractSearchPerformer extends AbstractQueryPerformer {

    /** The synthetic identifier of the virtual 'all my events' calendar */
    protected static final String VIRTUAL_ALL = "all";

    /** The synthetic identifier of the virtual 'all my events in public folders' calendar */
    protected static final String VIRTUAL_ALL_PUBLIC = "allPublic";

    /**
     * Initializes a new {@link AbstractSearchPerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public AbstractSearchPerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Initializes the calendar folders where a search request is targeted at. In case a <i>virtual</i> folder identifier is contained in
     * the client-supplied list of folder identifiers, this special search is performed immediately and the found events are put into the
     * supplied results map. For all other folder identifiers, the corresponding calendar folders are returned.
     * 
     * @param folderIds The client-supplied list of folder identifiers the search is targeted at, or <code>null</code> to search in all visible calendars
     * @param searchTerm The search term, or <code>null</code> if all visible events from each folder are requested
     * @param resultsPerFolderId The results map to store found events from <i>virtual</i> folders if needed
     * @return The calendar folders the search is targeted at
     */
    protected List<CalendarFolder> initializeSearchedFolders(List<String> folderIds, SearchTerm<?> searchTerm, Map<String, EventsResult> resultsPerFolderId) throws OXException {
        /*
         * get folders and/or directly handle virtual ids, storing a possible exception in the results
         */
        if (null == folderIds) {
            return getVisibleFolders(session, false, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
        }
        List<CalendarFolder> folders = new ArrayList<CalendarFolder>(folderIds.size());
        for (String folderId : folderIds) {
            try {
                if (VIRTUAL_ALL.equals(folderId)) {
                    /*
                     * add all events the user attends from all folders
                     */
                    resultsPerFolderId.put(folderId, new DefaultEventsResult(getEventsOfUser(null, null, null, searchTerm)));
                } else if (VIRTUAL_ALL_PUBLIC.equals(folderId)) {
                    /*
                     * add all events the user attends from all public folders
                     */
                    resultsPerFolderId.put(folderId, new DefaultEventsResult(getEventsOfUser(null, null, new Type[] { PublicType.getInstance() }, searchTerm)));
                } else if (isResourceCalendarFolderId(folderId)) {
                    /*
                     * add all events with the resource
                     */
                    resultsPerFolderId.put(folderId, new DefaultEventsResult(getEventsOfResource(parseResourceEntity(session, folderId), null, null, searchTerm)));
                } else {
                    /*
                     * remember folder for batch-retrieval
                     */
                    folders.add(getFolder(session, folderId));
                }
            } catch (OXException e) {
                /*
                 * track error for folder
                 */
                resultsPerFolderId.put(folderId, new DefaultEventsResult(e));
            }
        }
        return folders;
    }
    
    /**
     * Gets all events the current session user attends.
     *
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param folderTypes The folder types to include, or <code>null</code> to include all events independently of the type of the folder
     *            they're located in
     * @param additionalTerm An additional arbitrary search term to further restrict the results, or <code>null</code> if not set
     * @return The loaded events
     */
    protected List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats, Type[] folderTypes, SearchTerm<?> additionalTerm) throws OXException {
        return getEventsOfUser(rsvp, partStats, folderTypes, additionalTerm, session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class));
    }

    /**
     * Gets all events the current session user attends.
     *
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param folderTypes The folder types to include, or <code>null</code> to include all events independently of the type of the folder
     *            they're located in
     * @param additionalTerm An additional arbitrary search term to further restrict the results, or <code>null</code> if not set
     * @param requestedFields The requested event fields to retrieve from the storage
     * @return The loaded events
     */
    protected List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats, Type[] folderTypes, SearchTerm<?> additionalTerm, EventField[] requestedFields) throws OXException {
        /*
         * build suitable search term for events of user entity
         */
        SearchTerm<?> searchTerm = buildSearchTerm(session.getUserId(), rsvp, partStats, folderTypes, additionalTerm);
        /*
         * perform search, userize & post-process the results for the current session's user
         */
        EventField[] fields = getFieldsForStorage(requestedFields);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, getSearchOptionsForStorage(session), fields);
        events = storage.getUtilities().loadAdditionalEventData(session.getUserId(), events, fields);
        return postProcessor(getObjectIDs(events), session.getUserId(), requestedFields, fields).process(events, session.getUserId()).getEvents();
    }

    /**
     * Gets all events a specific internal user attends in a specific folder.
     *
     * @param entity The entity identifier of the user to get the events for
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param folder The calendar folder to lookup the events in
     * @param requestedFields The requested event fields to retrieve from the storage
     * @return The loaded events
     */
    protected List<Event> getEventsOfUser(int entity, Boolean rsvp, ParticipationStatus[] partStats, CalendarFolder folder, EventField[] requestedFields) throws OXException {
        /*
         * build suitable search term for events of user entity in folder
         */
        SearchTerm<?> searchTerm = buildSearchTerm(entity, rsvp, partStats, null, getFolderIdTerm(session, folder));
        /*
         * perform search, userize & post-process the results for the current session's user
         */
        EventField[] fields = getFieldsForStorage(requestedFields);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, getSearchOptionsForStorage(session), fields);
        events = storage.getUtilities().loadAdditionalEventData(entity, events, fields);
        return postProcessor(getObjectIDs(events), entity, requestedFields, fields).process(events, folder).getEvents();
    }

    /**
     * Gets all events a specific internal resource attends.
     *
     * @param entity The entity identifier of the resource to get the events for
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the resource attendee's
     *            rsvp status
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the resource
     *            attendee's participation status
     * @param additionalTerm An additional arbitrary search term to further restrict the results, or <code>null</code> if not set
     * @return The loaded events
     */
    protected List<Event> getEventsOfResource(int entity, Boolean rsvp, ParticipationStatus[] partStats, SearchTerm<?> additionalTerm) throws OXException {
        return getEventsOfResource(entity, rsvp, partStats, additionalTerm, session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class));
    }

    /**
     * Gets all events a specific internal resource attends.
     *
     * @param entity The entity identifier of the resource to get the events for
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the resource attendee's
     *            rsvp status
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the resource
     *            attendee's participation status
     * @param additionalTerm An additional arbitrary search term to further restrict the results, or <code>null</code> if not set
     * @param requestedFields The requested event fields to retrieve from the storage
     * @return The loaded events
     */
    protected List<Event> getEventsOfResource(int entity, Boolean rsvp, ParticipationStatus[] partStats, SearchTerm<?> additionalTerm, EventField[] requestedFields) throws OXException {
        /*
         * build suitable search term for events of entity
         */
        SearchTerm<?> searchTerm = buildSearchTerm(entity, rsvp, partStats, null, additionalTerm);
        boolean skipAnonymized = null != additionalTerm;
        /*
         * perform search, userize & post-process the results for the current session's user
         */
        EventField[] fields = getFieldsForStorage(requestedFields);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, getSearchOptionsForStorage(session), fields);
        events = storage.getUtilities().loadAdditionalEventData(entity, events, fields);
        CalendarFolder resourceCalendar = getResourceCalendarFolder(session, entity);
        return postProcessor(getObjectIDs(events), entity, requestedFields, fields).process(events, resourceCalendar, skipAnonymized).getEvents();
    }

    /**
     * Builds a search term to get lookup all events a specific internal attendee (user or resource) attends.
     *
     * @param entity The entity identifier of the attendee to get the events for
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @param folderTypes The folder types to include, or <code>null</code> to include all events independently of the type of the folder
     *            they're located in
     * @param searchTerm An additional arbitrary search term to further restrict the results, or <code>null</code> if not set
     * @return The search term
     */
    private SearchTerm<?> buildSearchTerm(int entity, Boolean rsvp, ParticipationStatus[] partStats, Type[] folderTypes, SearchTerm<?> additionalTerm) {
        /*
         * consider events the internal entity attends
         */
        SearchTerm<?> searchTerm = getSearchTerm(AttendeeField.ENTITY, SingleOperation.EQUALS, I(entity));
        if (null != rsvp) {
            /*
             * only include events with matching rsvp
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(searchTerm)
                .addSearchTerm(getSearchTerm(AttendeeField.RSVP, SingleOperation.EQUALS, rsvp))
            ;
        }
        if (null != partStats && 0 < partStats.length) {
            /*
             * only include events with matching participation status
             */
            if (1 == partStats.length) {
                searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                    .addSearchTerm(searchTerm)
                    .addSearchTerm(getSearchTerm(AttendeeField.PARTSTAT, SingleOperation.EQUALS, partStats[0]))
                ;
            } else {
                CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
                for (ParticipationStatus partStat : partStats) {
                    orTerm.addSearchTerm(getSearchTerm(AttendeeField.PARTSTAT, SingleOperation.EQUALS, partStat));
                }
                searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(searchTerm).addSearchTerm(orTerm);
            }
        }
        if (false == isEnforceDefaultAttendee(session)) {
            /*
             * also include not group-scheduled events associated with the entity
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.OR)
                .addSearchTerm(getSearchTerm(EventField.CALENDAR_USER, SingleOperation.EQUALS, I(entity)))
                .addSearchTerm(searchTerm)
            ;
        }
        boolean includePrivate = null == folderTypes || Arrays.contains(folderTypes, PrivateType.getInstance());
        boolean includePublic = null == folderTypes || Arrays.contains(folderTypes, PublicType.getInstance());
        if (includePublic && false == includePrivate) {
            /*
             * only include events in public folders (that have a common folder identifier assigned)
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                // arithmetic comparison with 'NULL' will also return false
                .addSearchTerm(searchTerm)
                .addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.GREATER_THAN, I(0)))
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(getSearchTerm(AttendeeField.FOLDER_ID, SingleOperation.ISNULL))
                    .addSearchTerm(getSearchTerm(AttendeeField.FOLDER_ID, SingleOperation.LESS_OR_EQUAL, I(0))))
            ;
        } else if (false == includePublic && includePrivate) {
            /*
             * only include events in non-public folders (that have no common folder identifier assigned)
             */
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(searchTerm)
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.ISNULL))
                    .addSearchTerm(getSearchTerm(EventField.FOLDER_ID, SingleOperation.EQUALS, I(0)))
            );
        }
        if (null != additionalTerm) {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND).addSearchTerm(searchTerm).addSearchTerm(additionalTerm);
        }
        return searchTerm;
    }

    /**
     * Gets the (possible adjusted) search options to pass down to the storage in case a subsequent <i>post-processing</i> of the events
     * will take place, based on the supplied calendar parameters.
     * <p/>
     * In case the resulting events are <i>post-processed</i>, sorting is done by the {@link EventPostProcessor}, so that the storage does
     * not need to consider an <code>ORDER BY ...</code> clause.
     *
     * @param parameters The parameters to get the storage search options from
     * @return The search options to use for storage operations
     */
    protected static SearchOptions getSearchOptionsForStorage(CalendarParameters parameters) {
        Integer leftHandLimit = parameters.get(CalendarParameters.PARAMETER_LEFT_HAND_LIMIT, Integer.class);
        Integer rightHandLimit = parameters.get(CalendarParameters.PARAMETER_RIGHT_HAND_LIMIT, Integer.class);
        EventField by = parameters.get(CalendarParameters.PARAMETER_ORDER_BY, EventField.class);
        if (null == by || null != leftHandLimit || null != rightHandLimit) {
            /*
             * no order by, or order by with limit, pass-through to storage as-is
             */
            return new SearchOptions(parameters);
        }
        /*
         * ignore order by when getting data from storage
         */
        return new SearchOptions(new DefaultCalendarParameters(parameters)
            .set(CalendarParameters.PARAMETER_ORDER, null)
            .set(CalendarParameters.PARAMETER_ORDER_BY, null))
        ;
    }

    /**
     * Builds a map that associates the effective calendar user identifier with its corresponding calendar folder(s).
     * 
     * @param folders The folders to map per calendar user id
     * @return The folders, mapped per calendar user id
     */
    protected static Map<Integer, List<CalendarFolder>> getFoldersPerCalendarUserId(List<CalendarFolder> folders) {
        Map<Integer, List<CalendarFolder>> foldersPerCalendarUserId = new HashMap<Integer, List<CalendarFolder>>();
        for (CalendarFolder folder : folders) {
            com.openexchange.tools.arrays.Collections.put(foldersPerCalendarUserId, I(getCalendarUserId(folder)), folder);
        }
        return foldersPerCalendarUserId;
    }

}
