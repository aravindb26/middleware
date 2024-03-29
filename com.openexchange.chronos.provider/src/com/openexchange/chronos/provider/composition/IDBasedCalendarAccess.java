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

package com.openexchange.chronos.provider.composition;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dmfs.rfc5545.DateTime;
import org.json.JSONObject;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.SchedulingControl;
import com.openexchange.chronos.provider.AccountAwareCalendarFolder;
import com.openexchange.chronos.provider.CalendarFolder;
import com.openexchange.chronos.provider.basic.BasicCalendarProvider;
import com.openexchange.chronos.provider.extensions.PersonalAlarmAware;
import com.openexchange.chronos.provider.extensions.SearchAware;
import com.openexchange.chronos.provider.extensions.SyncAware;
import com.openexchange.chronos.provider.folder.FolderCalendarProvider;
import com.openexchange.chronos.provider.groupware.GroupwareFolderType;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.ErrorAwareCalendarResult;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.FreeBusyResult;
import com.openexchange.chronos.service.ImportResult;
import com.openexchange.chronos.service.RecurrenceInfo;
import com.openexchange.chronos.service.SequenceResult;
import com.openexchange.chronos.service.UpdatesResult;
import com.openexchange.exception.OXException;
import com.openexchange.search.SearchTerm;
import com.openexchange.session.Session;
import com.openexchange.tx.TransactionAware;

/**
 * {@link IDBasedCalendarAccess}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public interface IDBasedCalendarAccess extends TransactionAware, CalendarParameters {

    /**
     * Gets the session associated with this calendar access instance.
     *
     * @return The session the access was initialized for
     */
    Session getSession();

    /**
     * Gets a list of warnings that occurred during processing.
     *
     * @return A list if warnings, or an empty list if there were none
     */
    List<OXException> getWarnings();

    /**
     * Gets the access to the scheduling related functionality
     *
     * @return The {@link IDBasedSchedulingAccess}
     */
    IDBasedSchedulingAccess getSchedulingAccess();

    /**
     * Gets the user's default calendar folder.
     *
     * @return The default calendar folder
     */
    CalendarFolder getDefaultFolder() throws OXException;

    /**
     * Gets a list of all visible calendar folders.
     *
     * @param type The type to get the visible folders for
     * @return A list of all visible calendar folders of the type
     */
    List<AccountAwareCalendarFolder> getVisibleFolders(GroupwareFolderType type) throws OXException;

    /**
     * Gets a specific calendar folder.
     *
     * @param folderId The fully qualified identifier of the folder to get
     * @return The calendar folder (including information about the underlying account)
     */
    AccountAwareCalendarFolder getFolder(String folderId) throws OXException;

    /**
     * Gets multiple calendar folders.
     *
     * @param folderIds The fully qualified identifiers of the folders to get
     * @return The calendar folders (including information about the underlying account)
     */
    List<AccountAwareCalendarFolder> getFolders(List<String> folderIds) throws OXException;

    /**
     * Create a new calendar folder.
     * <p/>
     * Depending on the capabilities of the targeted calendar provider, either a new subfolder is created within an existing calendar
     * account (of a {@link FolderCalendarProvider}), or a new calendar account representing a calendar subscription (of a
     * {@link BasicCalendarProvider}) is created implicitly, resulting in a new virtual folder.
     *
     * @param providerId The fully qualified identifier of the parent folder, or <code>null</code> if not needed
     * @param folder Calendar folder data to take over for the new calendar account
     * @param userConfig Arbitrary user configuration data for the new calendar account, or <code>null</code> if not needed
     * @return The fully qualified identifier of the newly created folder
     */
    String createFolder(String providerId, CalendarFolder folder, JSONObject userConfig) throws OXException;

    /**
     * Updates a calendar folder.
     * <p/>
     * Depending on the capabilities of the underlying calendar provider, also arbitrary account properties can be updated.
     *
     * @param folderId The fully qualified identifier of the folder to update
     * @param folder The updated calendar folder data
     * @param userConfig Arbitrary user configuration data for the calendar account, or <code>null</code> if not needed
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The (possibly changed) fully qualified identifier of the updated folder
     */
    String updateFolder(String folderId, CalendarFolder folder, JSONObject userConfig, long clientTimestamp) throws OXException;

    /**
     * Deletes a calendar folder.
     *
     * @param folderId The fully qualified identifier of the folder to delete
     */
    void deleteFolder(String folderId, long clientTimestamp) throws OXException;

    /**
     * Gets all events in a specific calendar folder.
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
     * @param folderId The fully qualified identifier of the folder to get the events from
     * @return The events
     */
    default List<Event> getEventsInFolder(String folderId) throws OXException {
        EventsResult eventsResult = getEventsInFolders(Collections.singletonList(folderId)).get(folderId);
        if (null == eventsResult) {
            return Collections.emptyList();
        }
        if (null != eventsResult.getError()) {
            throw eventsResult.getError();
        }
        return eventsResult.getEvents();
    }

    /**
     * Gets all events from one or more specific calendar folders.
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
     * @param folderId The fully qualified identifiers of the folders to get the events from
     * @return The resulting events per requested folder
     */
    Map<String, EventsResult> getEventsInFolders(List<String> folderIds) throws OXException;

    /**
     * Gets all events of the session's user attends in.
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
     * <li>{@link CalendarParameters#PARAMETER_EXPAND_OCCURRENCES}</li>
     * </ul>
     *
     * @return The events
     */
    List<Event> getEventsOfUser() throws OXException;

    /**
     * Gets all events the session's user attends in, having a particular participation status.
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
     * @param partStats The participation status to include, or <code>null</code> to include all events independently of the user
     *            attendee's participation status
     * @param rsvp The reply expectation to include, or <code>null</code> to include all events independently of the user attendee's
     *            rsvp status
     * @return The events
     */
    List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats) throws OXException;

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
     * Resolves an event identifier to an event, and returns it in the perspective of the current session's user, i.e. having an
     * appropriate parent folder identifier assigned.
     * <p/>
     * <b>Note:</b> Only events from the internal <i>groupware</i> calendar provider are considered.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param eventId The identifier of the event to resolve
     * @param sequence The expected sequence number to match, or <code>null</code> to resolve independently of the event's sequence number
     * @return The resolved event from the user's point of view, or <code>null</code> if not found
     */
    Event resolveEvent(String eventId, Integer sequence) throws OXException;

    /**
     * Searches for events in the specified folders using the specified {@link SearchTerm}.
     *
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER}</li>
     * <li>{@link CalendarParameters#PARAMETER_ORDER_BY}</li>
     * <li>{@link CalendarParameters#PARAMETER_EXPAND_OCCURRENCES}</li>
     *
     * @param <O> The search term type
     * @param folderIds The identifiers of the folders to perform the search in, or <code>null</code> to search across all visible folders
     *            of the user's {@link SearchAware} calendar accounts
     * @param term The {@link SearchTerm}
     * @return The found events per folder, or an empty map if there are none
     */
    <O> Map<String, EventsResult> searchEvents(List<String> folderIds, SearchTerm<O> term) throws OXException;

    /**
     * Gets lists of new and updated as well as deleted events since a specific timestamp in a folder.
     * <p/>
     * <b>Note:</b> Only available for {@link SyncAware} calendar providers.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE} ("changed", "deleted" and "count")</li>
     * <li>{@link CalendarParameters#PARAMETER_EXPAND_OCCURRENCES}</li>
     * </ul>
     *
     * @param folderId The fully qualified identifier of the folder to get the updated events from
     * @param updatedSince The timestamp since when the updates should be retrieved
     * @return The updates result yielding lists of new/modified and deleted events
     */
    UpdatesResult getUpdatedEventsInFolder(String folderId, long updatedSince) throws OXException;

    /**
     * Gets lists of new and updated as well as deleted events since a specific timestamp of a user.
     * <p/>
     * <b>Note:</b> Only events from the internal <i>groupware</i> calendar provider are considered.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE} ("changed", "deleted" and "count")</li>
     * <li>{@link CalendarParameters#PARAMETER_EXPAND_OCCURRENCES}</li>
     * </ul>
     *
     * @param updatedSince The timestamp since when the updates should be retrieved
     * @return The updates result yielding lists of new/modified and deleted events
     */
    UpdatesResult getUpdatedEventsOfUser(long updatedSince) throws OXException;

    /**
     * Resolves a specific event (and any overridden instances or <i>change exceptions</i>) by its externally used resource name, which
     * typically matches the event's UID or filename property. The lookup is performed within a specific folder in a case-sensitive way.
     * If an event series with overridden instances is matched, the series master event will be the first event in the returned list.
     * <p/>
     * <b>Note:</b> Only available for {@link SyncAware} calendar providers.
     * <p/>
     * It is also possible that only overridden instances of an event series are returned, which may be the case for <i>detached</i>
     * instances where the user has no access to the corresponding series master event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to resolve the resource name in
     * @param resourceName The resource name to resolve
     * @return The resolved event(s), or <code>null</code> if no matching event was found
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a>
     */
    List<Event> resolveResource(String folderId, String resourceName) throws OXException;

    /**
     * Resolves multiple events (and any overridden instances or <i>change exceptions</i>) by their externally used resource name, which
     * typically matches the event's UID or filename property. The lookup is performed within a specific folder in a case-sensitive way.
     * If an event series with overridden instances is matched, the series master event will be the first event in the returned list of
     * the corresponding events result.
     * <p/>
     * It is also possible that only overridden instances of an event series are returned, which may be the case for <i>detached</i>
     * instances where the user has no access to the corresponding series master event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to resolve the resource names in
     * @param resourceNames The resource names to resolve
     * @return The resolved event(s), mapped to their corresponding resource name
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a>
     */
    Map<String, EventsResult> resolveResources(String folderId, List<String> resourceNames) throws OXException;

    /**
     * Gets the sequence numbers of one or more calendar folders, which is the highest highest timestamp of all contained items. Distinct
     * object access permissions (e.g. <i>read own</i>) are not considered. Additionally, the actual item count in each of the folders is
     * returned, aiding proper detection of removed items during incremental synchronizations.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE} ("count")</li>
     * </ul>
     *
     * @param folderIds The identifiers of the folders to get the sequence numbers for
     * @return The sequence number results, mapped to their corresponding folder identifier
     */
    Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException;

    /**
     * Gets a specific event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param eventID The identifier of the event to get
     * @return The event
     */
    Event getEvent(EventID eventID) throws OXException;

    /**
     * Gets a list of events.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param eventIDs A list of the identifiers of the events to get
     * @return The events
     */
    List<Event> getEvents(List<EventID> eventIDs) throws OXException;

    /**
     * Gets all change exceptions of a recurring event series.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param folderId The fully qualified identifier of the parent folder of the event series to get the change exceptions for
     * @param seriesId The identifier of the series to get the change exceptions for
     * @return The change exceptions, or an empty list if there are none
     */
    List<Event> getChangeExceptions(String folderId, String seriesId) throws OXException;

    /**
     * Creates a new event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_CHECK_CONFLICTS}</li>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}</li>
     * <li>{@link CalendarParameters#PARAMETER_TRACK_ATTENDEE_USAGE}</li>
     * </ul>
     *
     * @param folderId The fully qualified identifier of the parent folder to create the event in
     * @param event The event data to create
     * @return The create result
     */
    CalendarResult createEvent(String folderId, Event event) throws OXException;

    /**
     * Puts a new or updated <i>calendar object resource</i>, i.e. an event and/or its change exceptions, to the calendar. In case the
     * calendar resource already exists under the perspective of the parent folder, it is updated implicitly: No longer indicated events
     * are removed, new ones are added, and existing ones are updated.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_CHECK_CONFLICTS}</li>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}</li>
     * <li>{@link CalendarParameters#PARAMETER_TRACK_ATTENDEE_USAGE}</li>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE_FORBIDDEN_ATTENDEE_CHANGES}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to add/update the calendar object resource in
     * @param resource The calendar object resource to store
     * @param replace <code>true</code> to automatically remove stored events that are no longer present in the supplied resource, <code>false</code>, otherwise
     * @return The calendar result
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a><br/>
     *      <a href="https://tools.ietf.org/html/rfc6638#section-3.1">RFC 6638, section 3.1</a>
     */
    CalendarResult putResource(String folderId, CalendarObjectResource resource, boolean replace) throws OXException;

    /**
     * Updates an existing event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_CHECK_CONFLICTS}</li>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}</li>
     * <li>{@link CalendarParameters#PARAMETER_TRACK_ATTENDEE_USAGE}</li>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE_FORBIDDEN_ATTENDEE_CHANGES}</li>
     * </ul>
     *
     * @param eventID The identifier of the event to update
     * @param event The event data to update
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The update result
     */
    CalendarResult updateEvent(EventID eventID, Event event, long clientTimestamp) throws OXException;

    /**
     * Moves an existing event into another folder.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_CHECK_CONFLICTS}</li>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}</li>
     * </ul>
     *
     * @param eventID The identifier of the event to move
     * @param targetFolderId The fully qualified identifier of the destination folder to move the event into
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The move result
     */
    CalendarResult moveEvent(EventID eventID, String targetFolderId, long clientTimestamp) throws OXException;

    /**
     * Updates a specific attendee of an existing event.
     *
     * @param eventID The identifier of the event to get
     * @param attendee The attendee to update
     * @param alarms The alarms to update, or <code>null</code> to not change alarms, or an empty array to delete any existing alarms
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The update result
     */
    CalendarResult updateAttendee(EventID eventID, Attendee attendee, List<Alarm> alarms, long clientTimestamp) throws OXException;

    /**
     * Updates the user's personal alarms of a specific event, independently of the user's write access permissions for the corresponding
     * event.
     * <p/>
     * <b>Note:</b> Only available for {@link PersonalAlarmAware} calendar providers.
     * <p/>
     *
     * @param eventID The identifier of the event to update the alarms for
     * @param alarms The updated list of alarms to apply, or <code>null</code> to remove any previously stored alarms
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The update result
     */
    CalendarResult updateAlarms(EventID eventID, List<Alarm> alarms, long clientTimestamp) throws OXException;

    /**
     * Updates the event's organizer to the new one.
     * <p>
     * Current restrictions are:
     *
     * <li>The event has to be a group scheduled event</li>
     * <li>All attendees of the event have to be internal</li>
     * <li>The new organizer must be an internal user</li>
     * <li>The change has to be performed for one of these:
     * <ul> a single event</ul>
     * <ul> a series master, efficiently updating for the complete series</ul>
     * <ul> a specific recurrence of the series, efficiently performing a series split. Only allowed if {@link com.openexchange.chronos.RecurrenceRange#THISANDFUTURE} is set</ul>
     * </li>
     *
     * @param eventID The {@link EventID} of the event to change. Optional having a recurrence ID set to perform a series split.
     * @param organizer The new organizer
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The updated event
     * @throws OXException In case the organizer change is not allowed
     */
    CalendarResult changeOrganizer(EventID eventID, Organizer organizer, long clientTimestamp) throws OXException;

    /**
     * Deletes an existing event.
     *
     * @param eventID The identifier of the event to delete
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The delete result
     */
    CalendarResult deleteEvent(EventID eventID, long clientTimestamp) throws OXException;

    /**
     * Deletes multiple existing events.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}</li>
     * </ul>
     *
     * @param eventIDs The identifiers of the event to delete
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The delete results per requested identifier
     */
    Map<EventID, ErrorAwareCalendarResult> deleteEvents(List<EventID> eventIDs, long clientTimestamp) throws OXException;

    /**
     * Splits an existing event series into two separate event series.
     * <p/>
     * <b>Note:</b> Only available for the internal <i>groupware</i> calendar provider.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}</li>
     * </ul>
     *
     * @param eventID The identifier of the event series to split
     * @param splitPoint The date or date-time where the split is to occur
     * @param uid A new unique identifier to assign to the new part of the series, or <code>null</code> if not set
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The split result
     */
    CalendarResult splitSeries(EventID eventID, DateTime splitPoint, String uid, long clientTimestamp) throws OXException;

    /**
     * Imports a list of events into a specific folder.
     * <p/>
     * <b>Note:</b> Only available for the internal <i>groupware</i> calendar provider.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_CHECK_CONFLICTS}, defaulting to {@link Boolean#FALSE} unless overridden</li>
     * <li>{@link CalendarParameters#PARAMETER_IGNORE_STORAGE_WARNINGS}, defaulting to {@link Boolean#TRUE} unless overridden</li>
     * <li>{@link CalendarParameters#PARAMETER_SCHEDULING}, defaulting to {@link SchedulingControl#NONE} unless overridden</li>
     * <li>{@link CalendarParameters#UID_CONFLICT_STRATEGY}</li>
     * </ul>
     *
     * @param folderId The fully qualified identifier of the target folder
     * @param events The events to import
     * @return A list of results holding further information about each imported event
     */
    List<ImportResult> importEvents(String folderId, List<Event> events) throws OXException;

    /**
     * Retrieves all upcoming alarm triggers until the given time.
     *
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_START}</li>
     * <li>{@link CalendarParameters#PARAMETER_RANGE_END}</li>
     * </ul>
     *
     * @param actions The actions to retrieve
     * @return A list of upcoming alarm triggers
     * @throws OXException
     */
    List<AlarmTrigger> getAlarmTriggers(Set<String> actions) throws OXException;

    /**
     * Retrieves the {@link IFileHolder} with the specified managed identifier from the {@link Event}
     * with the specified {@link EventID}
     *
     * @param eventID The {@link Event} identifier
     * @param managedId The managed identifier of the {@link Attachment}
     * @return The {@link IFileHolder}
     * @throws OXException if an error is occurred
     */
    IFileHolder getAttachment(EventID eventID, int managedId) throws OXException;

    /**
     * Queries the free/busy time for a list of attendees.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_MASK_UID}</li>
     * </ul>
     *
     * @param attendees The queried attendees
     * @param from The start of the requested time range
     * @param until The end of the requested time range
     * @param merge <code>true</code> to merge the resulting free/busy-times, <code>false</code>, otherwise
     * @return The free/busy results for each of the queried attendees
     */
    Map<Attendee, FreeBusyResult> queryFreeBusy(List<Attendee> attendees, Date from, Date until, boolean merge) throws OXException;

    /**
     * Retrieves the CTag for a folder.
     *
     * @param folderID The fully qualified identifier of the folder.
     * @return the CTag for this folder.
     * @throws OXException if an error occurs.
     */
    String getCTag(String folderID) throws OXException;

    /**
     * Looks up details for a specific recurrence from an event series. This includes the series master, as well as the recurrence event,
     * along with information whether it is an overridden and/or re-scheduled occurrence compared to the regular series.
     * <p/>
     * This can either be performed for an event series identifier along with the targeted recurrence id, or for an already overridden
     * instance.
     * <p/>
     * <b>Note:</b> Only available for the internal <i>groupware</i> calendar provider.
     *
     * @param eventID The event identifier to get the recurrence info for; either the series master id along the targeted recurrence id,
     *            or the id of an existing change exception event
     * @return The recurrence info
     */
    RecurrenceInfo getRecurrenceInfo(EventID eventID) throws OXException;

}
