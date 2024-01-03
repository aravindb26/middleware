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

package com.openexchange.chronos.impl;

import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.postProcess;
import static com.openexchange.java.Autoboxing.L;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.dmfs.rfc5545.DateTime;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.SchedulingControl;
import com.openexchange.chronos.UnmodifiableEvent;
import com.openexchange.chronos.impl.performer.AlarmTriggersPerformer;
import com.openexchange.chronos.impl.performer.AllPerformer;
import com.openexchange.chronos.impl.performer.ChangeExceptionsPerformer;
import com.openexchange.chronos.impl.performer.ChangeOrganizerPerformer;
import com.openexchange.chronos.impl.performer.ClearPerformer;
import com.openexchange.chronos.impl.performer.CreatePerformer;
import com.openexchange.chronos.impl.performer.DeletePerformer;
import com.openexchange.chronos.impl.performer.GetAttachmentPerformer;
import com.openexchange.chronos.impl.performer.GetPerformer;
import com.openexchange.chronos.impl.performer.ImportPerformer;
import com.openexchange.chronos.impl.performer.ListPerformer;
import com.openexchange.chronos.impl.performer.MovePerformer;
import com.openexchange.chronos.impl.performer.NeedsActionPerformer;
import com.openexchange.chronos.impl.performer.PutPerformer;
import com.openexchange.chronos.impl.performer.SearchPerformer;
import com.openexchange.chronos.impl.performer.SequenceNumberPerformer;
import com.openexchange.chronos.impl.performer.SplitPerformer;
import com.openexchange.chronos.impl.performer.TouchPerformer;
import com.openexchange.chronos.impl.performer.UpdateAlarmsPerformer;
import com.openexchange.chronos.impl.performer.UpdateAttendeePerformer;
import com.openexchange.chronos.impl.performer.UpdatePerformer;
import com.openexchange.chronos.impl.performer.UpdatesPerformer;
import com.openexchange.chronos.impl.session.DefaultCalendarSession;
import com.openexchange.chronos.service.CalendarInterceptor;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.CalendarServiceUtilities;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.ImportResult;
import com.openexchange.chronos.service.SequenceResult;
import com.openexchange.chronos.service.UpdatesResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.search.SearchTerm;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link CalendarServiceImpl}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CalendarServiceImpl implements CalendarService {

    private final ServiceLookup services;
    private final CalendarServiceUtilities utilities;

    /**
     * Initializes a new {@link CalendarServiceImpl}.
     *
     * @param services A service lookup reference
     * @param interceptors The calendar interceptor service set to use
     */
    public CalendarServiceImpl(ServiceLookup services, ServiceSet<CalendarInterceptor> interceptors) {
        super();
        this.services = services;
        this.utilities = new CalendarServiceUtilitiesImpl(interceptors);
    }

    @Override
    public CalendarSession init(Session session) throws OXException {
        return init(session, null);
    }

    @Override
    public CalendarSession init(Session session, CalendarParameters parameters) throws OXException {
        DefaultCalendarSession calendarSession = new DefaultCalendarSession(session, this);
        if (null != parameters) {
            for (Entry<String, Object> entry : parameters.entrySet()) {
                calendarSession.set(entry.getKey(), entry.getValue());
            }
        }
        return calendarSession;
    }

    @Override
    public CalendarServiceUtilities getUtilities() {
        return utilities;
    }

    @Override
    public List<Event> getChangeExceptions(CalendarSession session, String folderId, String objectId) throws OXException {
        return new InternalCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ChangeExceptionsPerformer(session, storage).perform(folderId, objectId);
            }
        }.executeQuery();
    }

    @Override
    public SequenceResult getSequenceNumber(CalendarSession session, String folderId) throws OXException {
        return new InternalCalendarStorageOperation<SequenceResult>(session) {

            @Override
            protected SequenceResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new SequenceNumberPerformer(session, storage).perform(folderId);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> searchEvents(CalendarSession session, String[] folderIds, String pattern) throws OXException {
        return new InternalCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new SearchPerformer(session, storage).perform(folderIds, pattern);
            }
        }.executeQuery();
    }

    @Override
    public <O> Map<String, EventsResult> searchEvents(CalendarSession session, List<String> folderIds,SearchTerm<O> term) throws OXException {
        return new InternalCalendarStorageOperation<Map<String, EventsResult>>(session) {

            @Override
            protected Map<String, EventsResult> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new SearchPerformer(session, storage).perform(folderIds, term);
            }
        }.executeQuery();
    }

    @Override
    public Event getEvent(CalendarSession session, String folderId, EventID eventId) throws OXException {
        return new InternalCalendarStorageOperation<Event>(session) {

            @Override
            protected Event execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new GetPerformer(session, storage).perform(folderId, eventId.getObjectID(), eventId.getRecurrenceID());
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEvents(CalendarSession session, final List<EventID> eventIDs) throws OXException {
        return new InternalCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ListPerformer(session, storage).perform(eventIDs);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEventsInFolder(CalendarSession session, String folderId) throws OXException {
        return new InternalCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AllPerformer(session, storage).perform(folderId);
            }
        }.executeQuery();
    }

    @Override
    public Map<String, EventsResult> getEventsInFolders(CalendarSession session, List<String> folderIds) throws OXException {
        return new InternalCalendarStorageOperation<Map<String, EventsResult>>(session) {

            @Override
            protected Map<String, EventsResult> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AllPerformer(session, storage).perform(folderIds);
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEventsOfUser(final CalendarSession session) throws OXException {
        return new InternalCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AllPerformer(session, storage).perform();
            }
        }.executeQuery();
    }

    @Override
    public List<Event> getEventsOfUser(final CalendarSession session, Boolean rsvp, ParticipationStatus[] partStats) throws OXException {
        return new InternalCalendarStorageOperation<List<Event>>(session) {

            @Override
            protected List<Event> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AllPerformer(session, storage).perform(rsvp, partStats);
            }
        }.executeQuery();
    }

    @Override
    public Map<Attendee, EventsResult> getEventsNeedingAction(CalendarSession session, boolean includeDelegates) throws OXException {
        return new InternalCalendarStorageOperation<Map<Attendee, EventsResult>>(session) {

            @Override
            protected Map<Attendee, EventsResult> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new NeedsActionPerformer(session, storage).perform(includeDelegates);
            }
        }.executeQuery();
    }

    @Override
    public UpdatesResult getUpdatedEventsInFolder(CalendarSession session, String folderId, long updatedSince) throws OXException {
        return new InternalCalendarStorageOperation<UpdatesResult>(session) {

            @Override
            protected UpdatesResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdatesPerformer(session, storage).perform(folderId, updatedSince);
            }
        }.executeQuery();
    }

    @Override
    public UpdatesResult getUpdatedEventsOfUser(CalendarSession session, final long updatedSince) throws OXException {
        return new InternalCalendarStorageOperation<UpdatesResult>(session) {

            @Override
            protected UpdatesResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdatesPerformer(session, storage).perform(updatedSince);
            }
        }.executeQuery();
    }

    @Override
    public CalendarResult createEvent(CalendarSession session, final String folderId, final Event event) throws OXException {
        /*
         * insert event, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new CreatePerformer(storage, session, getFolder(session, folderId)).perform(new UnmodifiableEvent(event));
            }
        }.executeUpdate(), true).getUserizedResult();
    }

    @Override
    public CalendarResult putResource(CalendarSession session, String folderId, CalendarObjectResource resource, boolean replace) throws OXException {
        /*
         * put event(s), trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new PutPerformer(storage, session, getFolder(session, folderId)).perform(resource, replace);
            }
        }.executeUpdate(), true).getUserizedResult();
    }

    @Override
    public CalendarResult updateEvent(CalendarSession session, final EventID eventID, final Event event, final long clientTimestamp) throws OXException {
        /*
         * update event, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdatePerformer(storage, session, getFolder(session, eventID.getFolderID(), true, true)).perform(eventID.getObjectID(), eventID.getRecurrenceID(), new UnmodifiableEvent(event), clientTimestamp);
            }

        }.executeUpdate(), true).getUserizedResult();
    }

    @Override
    public CalendarResult touchEvent(CalendarSession session, final EventID eventID) throws OXException {
        /*
         * touch event, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new TouchPerformer(storage, session, getFolder(session, eventID.getFolderID())).perform(eventID.getObjectID());
            }

        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult moveEvent(CalendarSession session, final EventID eventID, final String folderId, final long clientTimestamp) throws OXException {
        /*
         * move event, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new MovePerformer(storage, session, getFolder(session, eventID.getFolderID())).perform(eventID.getObjectID(), getFolder(session, folderId), clientTimestamp);
            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult updateAttendee(CalendarSession session, EventID eventID, Attendee attendee, List<Alarm> alarms, long clientTimestamp) throws OXException {
        /*
         * update attendee, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdateAttendeePerformer(storage, session, getFolder(session, eventID.getFolderID(), false, true)).perform(eventID.getObjectID(), eventID.getRecurrenceID(), attendee, alarms, L(clientTimestamp));

            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult updateAlarms(CalendarSession session, EventID eventID, List<Alarm> alarms, long clientTimestamp) throws OXException {
        /*
         * update alarms, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new UpdateAlarmsPerformer(storage, session, getFolder(session, eventID.getFolderID(), false)).perform(eventID.getObjectID(), eventID.getRecurrenceID(), alarms, L(clientTimestamp));

            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult changeOrganizer(CalendarSession session, EventID eventID, Organizer organizer, long clientTimestamp) throws OXException {
        /*
         * change organizer, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ChangeOrganizerPerformer(storage, session, getFolder(session, eventID.getFolderID())).perform(eventID.getObjectID(), eventID.getRecurrenceID(), organizer, L(clientTimestamp));

            }
        }.executeUpdate(), true).getUserizedResult();
    }

    @Override
    public CalendarResult deleteEvent(CalendarSession session, final EventID eventID, final long clientTimestamp) throws OXException {
        /*
         * delete event, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new DeletePerformer(storage, session, getFolder(session, eventID.getFolderID(), true, true)).perform(eventID.getObjectID(), eventID.getRecurrenceID(), clientTimestamp);

            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult splitSeries(CalendarSession session, EventID eventID, DateTime splitPoint, String uid, long clientTimestamp) throws OXException {
        /*
         * split event series, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new SplitPerformer(storage, session, getFolder(session, eventID.getFolderID())).perform(eventID.getObjectID(), splitPoint, uid, clientTimestamp);

            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public CalendarResult clearEvents(CalendarSession session, final String folderId, final long clientTimestamp) throws OXException {
        /*
         * clear events, trigger post-processing & return userized result
         */
        return postProcess(services, new InternalCalendarStorageOperation<InternalCalendarResult>(session) {

            @Override
            protected InternalCalendarResult execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new ClearPerformer(storage, session, getFolder(session, folderId)).perform(clientTimestamp);

            }
        }.executeUpdate()).getUserizedResult();
    }

    @Override
    public List<ImportResult> importEvents(CalendarSession session, String folderID, List<Event> events) throws OXException {
        /*
         * assign adjusted default values and perform the import
         */
        SchedulingControl oldScheduling = session.get(CalendarParameters.PARAMETER_SCHEDULING, SchedulingControl.class);
        Boolean oldIgnoreStorageWarnings = session.get(CalendarParameters.PARAMETER_IGNORE_STORAGE_WARNINGS, Boolean.class);
        Boolean oldCheckConflicts = session.get(CalendarParameters.PARAMETER_CHECK_CONFLICTS, Boolean.class);
        List<InternalImportResult> results;
        try {
            if (null == oldScheduling) {
                session.set(CalendarParameters.PARAMETER_SCHEDULING, SchedulingControl.NONE);
            }
            if (null == oldIgnoreStorageWarnings) {
                session.set(CalendarParameters.PARAMETER_IGNORE_STORAGE_WARNINGS, Boolean.TRUE);
            }
            if (null == oldCheckConflicts) {
                session.set(CalendarParameters.PARAMETER_CHECK_CONFLICTS, Boolean.FALSE);
            }
            results = new ImportPerformer(session, getFolder(session, folderID)).perform(events);
        } finally {
            session.set(CalendarParameters.PARAMETER_SCHEDULING, oldScheduling);
            session.set(CalendarParameters.PARAMETER_IGNORE_STORAGE_WARNINGS, oldIgnoreStorageWarnings);
            session.set(CalendarParameters.PARAMETER_CHECK_CONFLICTS, oldCheckConflicts);
        }
        /*
         * notify handlers & return userized result
         */
        postProcess(services, results, false);
        List<ImportResult> importResults = new ArrayList<ImportResult>(results.size());
        for (InternalImportResult result : results) {
            importResults.add(result.getImportResult());
        }
        return importResults;
    }

    @Override
    public List<AlarmTrigger> getAlarmTriggers(CalendarSession session, Set<String> actions) throws OXException {
        return new InternalCalendarStorageOperation<List<AlarmTrigger>>(session) {

            @Override
            protected List<AlarmTrigger> execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new AlarmTriggersPerformer(session, storage).perform(actions);
            }
        }.executeQuery();
    }

    @Override
    public IFileHolder getAttachment(CalendarSession session, EventID eventID, int managedId) throws OXException {
        return new InternalCalendarStorageOperation<IFileHolder>(session) {

            @Override
            protected IFileHolder execute(CalendarSession session, CalendarStorage storage) throws OXException {
                return new GetAttachmentPerformer(session, storage).performGetAttachment(eventID.getFolderID(), eventID.getObjectID(), managedId);
            }
        }.executeQuery();
    }
}
