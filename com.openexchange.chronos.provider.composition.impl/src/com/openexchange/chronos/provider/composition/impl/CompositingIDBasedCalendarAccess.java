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

package com.openexchange.chronos.provider.composition.impl;

import static com.openexchange.chronos.common.CalendarUtils.extractEMailAddress;
import static com.openexchange.chronos.provider.CalendarAccount.DEFAULT_ACCOUNT;
import static com.openexchange.chronos.provider.composition.IDMangling.getAccountId;
import static com.openexchange.chronos.provider.composition.IDMangling.getRelativeFolderId;
import static com.openexchange.chronos.provider.composition.IDMangling.getRelativeId;
import static com.openexchange.chronos.provider.composition.IDMangling.getUniqueFolderId;
import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.getRelativeIdsPerAccountId;
import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.withRelativeID;
import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.withUniqueEventIDs;
import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.withUniqueID;
import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.withUniqueIDs;
import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.withUniqueIDsFromResultsPerAttendee;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.osgi.Tools.requireService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import org.dmfs.rfc5545.DateTime;
import org.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.Check;
import com.openexchange.chronos.common.DefaultCalendarResult;
import com.openexchange.chronos.common.DefaultErrorAwareCalendarResult;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.common.FreeBusyUtils;
import com.openexchange.chronos.common.SelfProtectionFactory;
import com.openexchange.chronos.common.SelfProtectionFactory.SelfProtection;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.AccountAwareCalendarFolder;
import com.openexchange.chronos.provider.CalendarAccess;
import com.openexchange.chronos.provider.CalendarAccount;
import com.openexchange.chronos.provider.CalendarCapability;
import com.openexchange.chronos.provider.CalendarFolder;
import com.openexchange.chronos.provider.CalendarPermission;
import com.openexchange.chronos.provider.CalendarProviderRegistry;
import com.openexchange.chronos.provider.DefaultCalendarFolder;
import com.openexchange.chronos.provider.DefaultCalendarPermission;
import com.openexchange.chronos.provider.FreeBusyProvider;
import com.openexchange.chronos.provider.UsedForSync;
import com.openexchange.chronos.provider.account.CalendarAccountService;
import com.openexchange.chronos.provider.basic.BasicCalendarAccess;
import com.openexchange.chronos.provider.basic.CalendarSettings;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.provider.composition.IDBasedSchedulingAccess;
import com.openexchange.chronos.provider.composition.impl.idmangling.IDManglingCalendarResult;
import com.openexchange.chronos.provider.composition.impl.idmangling.IDManglingEventsResult;
import com.openexchange.chronos.provider.composition.impl.idmangling.IDManglingITipAnalysis;
import com.openexchange.chronos.provider.composition.impl.idmangling.IDManglingImportResult;
import com.openexchange.chronos.provider.composition.impl.idmangling.IDManglingUpdatesResult;
import com.openexchange.chronos.provider.extensions.BasicCTagAware;
import com.openexchange.chronos.provider.extensions.BasicSearchAware;
import com.openexchange.chronos.provider.extensions.BasicSyncAware;
import com.openexchange.chronos.provider.extensions.CTagAware;
import com.openexchange.chronos.provider.extensions.FolderSearchAware;
import com.openexchange.chronos.provider.extensions.FolderSyncAware;
import com.openexchange.chronos.provider.extensions.PersonalAlarmAware;
import com.openexchange.chronos.provider.extensions.SyncAware;
import com.openexchange.chronos.provider.folder.FolderCalendarAccess;
import com.openexchange.chronos.provider.groupware.GroupwareCalendarAccess;
import com.openexchange.chronos.provider.groupware.GroupwareCalendarFolder;
import com.openexchange.chronos.provider.groupware.GroupwareFolderType;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingSource;
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
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.search.SearchTerm;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link CompositingIDBasedCalendarAccess}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CompositingIDBasedCalendarAccess extends AbstractCompositingIDBasedCalendarAccess implements IDBasedCalendarAccess, IDBasedSchedulingAccess {

    private SelfProtection protection = null;

    /**
     * Initializes a new {@link CompositingIDBasedCalendarAccess}.
     *
     * @param session The session to create the ID-based access for
     * @param parameters The calendar parameters to take over, or <code>null</code> if there are none
     * @param providerRegistry A reference to the calendar provider registry
     * @param services A service lookup reference
     */
    public CompositingIDBasedCalendarAccess(Session session, CalendarParameters parameters, CalendarProviderRegistry providerRegistry, ServiceLookup services) throws OXException {
        super(session, parameters, providerRegistry, services);
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public List<OXException> getWarnings() {
        return warnings;
    }

    @Override
    public IDBasedSchedulingAccess getSchedulingAccess() {
        return this;
    }

    @Override
    public Event getEvent(EventID eventID) throws OXException {
        CalendarAccount account = getAccount(getAccountId(eventID.getFolderID()));
        try {
            EventID relativeEventID = getRelativeId(eventID);
            CalendarAccess access = getAccess(account.getAccountId());
            Event event;
            if ((access instanceof FolderCalendarAccess)) {
                event = ((FolderCalendarAccess) access).getEvent(relativeEventID.getFolderID(), relativeEventID.getObjectID(), relativeEventID.getRecurrenceID());
            } else if ((access instanceof BasicCalendarAccess)) {
                Check.parentFolderMatches(relativeEventID, BasicCalendarAccess.FOLDER_ID);
                event = ((BasicCalendarAccess) access).getEvent(relativeEventID.getObjectID(), relativeEventID.getRecurrenceID());
            } else {
                throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
            }
            return withUniqueID(event, account.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public List<Event> getEvents(List<EventID> eventIDs) throws OXException {
        /*
         * get events from each account
         */
        Map<Integer, List<EventID>> idsPerAccountId = getRelativeIdsPerAccountId(eventIDs);
        Map<Integer, List<Event>> eventsPerAccountId = new HashMap<Integer, List<Event>>(idsPerAccountId.size());
        for (Entry<Integer, List<EventID>> entry : idsPerAccountId.entrySet()) {
            CalendarAccount account = getAccount(i(entry.getKey()));
            try {
                CalendarAccess access = getAccess(account.getAccountId());
                if ((access instanceof FolderCalendarAccess)) {
                    eventsPerAccountId.put(I(account.getAccountId()), ((FolderCalendarAccess) access).getEvents(entry.getValue()));
                } else if ((access instanceof BasicCalendarAccess)) {
                    Check.parentFolderMatches(entry.getValue(), BasicCalendarAccess.FOLDER_ID);
                    eventsPerAccountId.put(I(account.getAccountId()), ((BasicCalendarAccess) access).getEvents(entry.getValue()));
                } else {
                    throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
                }
            } catch (OXException e) {
                throw withUniqueIDs(e, account.getAccountId());
            }
        }
        /*
         * order resulting events as requested
         */
        List<Event> events = new ArrayList<Event>(eventIDs.size());
        for (EventID requestedID : eventIDs) {
            int accountId = getAccountId(requestedID.getFolderID());
            Event event = find(eventsPerAccountId.get(I(accountId)), getRelativeId(requestedID));
            events.add(null != event ? withUniqueID(event, accountId) : null);
        }
        return events;
    }

    @Override
    public List<Event> getChangeExceptions(String folderId, String seriesId) throws OXException {
        CalendarAccount account = getAccount(getAccountId(folderId));
        try {
            CalendarAccess access = getAccess(account.getAccountId());
            if ((access instanceof FolderCalendarAccess)) {
                List<Event> changeExceptions = ((FolderCalendarAccess) access).getChangeExceptions(getRelativeFolderId(folderId), seriesId);
                return withUniqueIDs(changeExceptions, account.getAccountId());
            }
            if ((access instanceof BasicCalendarAccess)) {
                Check.folderMatches(getRelativeFolderId(folderId), BasicCalendarAccess.FOLDER_ID);
                List<Event> changeExceptions = ((BasicCalendarAccess) access).getChangeExceptions(seriesId);
                return withUniqueIDs(changeExceptions, account.getAccountId());
            }
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public Map<String, EventsResult> getEventsInFolders(List<String> folderIds) throws OXException {
        Map<String, EventsResult> eventsResults = new HashMap<String, EventsResult>(folderIds.size());
        /*
         * get folder identifiers per account & track possible errors
         */
        Map<String, OXException> errorsPerFolderId = new HashMap<String, OXException>();
        Map<CalendarAccount, List<String>> relativeFolderIdsPerAccount = getRelativeFolderIdsPerAccount(folderIds, errorsPerFolderId);
        eventsResults.putAll(getErrorResults(errorsPerFolderId));
        /*
         * get events results per account
         */
        if (1 == relativeFolderIdsPerAccount.size()) {
            Entry<CalendarAccount, List<String>> entry = relativeFolderIdsPerAccount.entrySet().iterator().next();
            eventsResults.putAll(getEventsInFolders(entry.getKey(), entry.getValue()));
        } else {
            CompletionService<Map<String, EventsResult>> completionService = getCompletionService();
            for (Entry<CalendarAccount, List<String>> entry : relativeFolderIdsPerAccount.entrySet()) {
                completionService.submit(() -> getEventsInFolders(entry.getKey(), entry.getValue()));
            }
            eventsResults.putAll(collectEventsResults(completionService, relativeFolderIdsPerAccount.size()));
        }
        return getOrderedResults(eventsResults, folderIds);
    }

    @Override
    public List<Event> getEventsOfUser() throws OXException {
        try {
            return withUniqueIDs(getInternalAccess().getEventsOfUser(), DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public List<Event> getEventsOfUser(Boolean rsvp, ParticipationStatus[] partStats) throws OXException {
        try {
            return withUniqueIDs(getInternalAccess().getEventsOfUser(rsvp, partStats), DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public Map<Attendee, EventsResult> getEventsNeedingAction(boolean includeDelegates) throws OXException {
        try {
            return withUniqueIDsFromResultsPerAttendee(getInternalAccess().getEventsNeedingAction(includeDelegates), DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public Event resolveEvent(String eventId, Integer sequence) throws OXException {
        try {
            Event event = getInternalAccess().resolveEvent(eventId, sequence);
            return null == event ? null : withUniqueID(event, DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public <O> Map<String, EventsResult> searchEvents(List<String> folderIds, SearchTerm<O> term) throws OXException {
        if (null == folderIds || folderIds.isEmpty()) {
            return searchEvents(term);
        }
        Map<String, EventsResult> eventsResults = new HashMap<>(folderIds.size());
        // Get folder identifiers per account & track possible errors
        Map<String, OXException> errorsPerFolderId = new HashMap<String, OXException>();
        Map<CalendarAccount, List<String>> relativeFolderIdsPerAccount = getRelativeFolderIdsPerAccount(folderIds, errorsPerFolderId);
        eventsResults.putAll(getErrorResults(errorsPerFolderId));

        // Get events results per account
        if (1 == relativeFolderIdsPerAccount.size()) {
            Entry<CalendarAccount, List<String>> entry = relativeFolderIdsPerAccount.entrySet().iterator().next();
            eventsResults.putAll(searchEventsInFolders(entry.getKey(), entry.getValue(), term));
        } else {
            CompletionService<Map<String, EventsResult>> completionService = getCompletionService();
            for (Entry<CalendarAccount, List<String>> entry : relativeFolderIdsPerAccount.entrySet()) {
                completionService.submit(() -> searchEventsInFolders(entry.getKey(), entry.getValue(), term));
            }
            eventsResults.putAll(collectEventsResults(completionService, relativeFolderIdsPerAccount.size()));
        }
        return getOrderedResults(eventsResults, folderIds);
    }

    @Override
    public UpdatesResult getUpdatedEventsInFolder(String folderId, long updatedSince) throws OXException {
        CalendarAccount account = getAccount(getAccountId(folderId));
        try {
            CalendarAccess access = getAccess(account.getAccountId(), SyncAware.class);
            if ((access instanceof FolderSyncAware)) {
                UpdatesResult updatesResult = ((FolderSyncAware) access).getUpdatedEventsInFolder(getRelativeFolderId(folderId), updatedSince);
                return new IDManglingUpdatesResult(updatesResult, account.getAccountId());
            } else if ((access instanceof BasicSyncAware)) {
                Check.folderMatches(getRelativeFolderId(folderId), BasicCalendarAccess.FOLDER_ID);
                UpdatesResult updatesResult = ((BasicSyncAware) access).getUpdatedEvents(updatedSince);
                return new IDManglingUpdatesResult(updatesResult, account.getAccountId());
            } else {
                throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
            }
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public UpdatesResult getUpdatedEventsOfUser(long updatedSince) throws OXException {
        try {
            UpdatesResult updatesResult = getInternalAccess().getUpdatedEventsOfUser(updatedSince);
            return new IDManglingUpdatesResult(updatesResult, DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public List<Event> resolveResource(String folderId, String resourceName) throws OXException {
        CalendarAccount account = getAccount(getAccountId(folderId));
        try {
            CalendarAccess access = getAccess(account.getAccountId(), SyncAware.class);
            if ((access instanceof FolderSyncAware)) {
                List<Event> events = ((FolderSyncAware) access).resolveResource(getRelativeFolderId(folderId), resourceName);
                return withUniqueIDs(events, account.getAccountId());
            } else if ((access instanceof BasicSyncAware)) {
                Check.folderMatches(getRelativeFolderId(folderId), BasicCalendarAccess.FOLDER_ID);
                List<Event> events = ((BasicSyncAware) access).resolveResource(resourceName);
                return withUniqueIDs(events, account.getAccountId());
            } else {
                throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
            }
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public Map<String, EventsResult> resolveResources(String folderId, List<String> resourceNames) throws OXException {
        CalendarAccount account = getAccount(getAccountId(folderId));
        Map<String, EventsResult> eventsResults;
        try {
            CalendarAccess access = getAccess(account.getAccountId(), SyncAware.class);
            if ((access instanceof FolderSyncAware)) {
                eventsResults = ((FolderSyncAware) access).resolveResources(getRelativeFolderId(folderId), resourceNames);
            } else if ((access instanceof BasicSyncAware)) {
                Check.folderMatches(getRelativeFolderId(folderId), BasicCalendarAccess.FOLDER_ID);
                eventsResults = ((BasicSyncAware) access).resolveResources(resourceNames);
            } else {
                throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
            }
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
        if (null == eventsResults || eventsResults.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, EventsResult> results = new HashMap<String, EventsResult>(eventsResults.size());
        for (Map.Entry<String, EventsResult> entry : eventsResults.entrySet()) {
            results.put(entry.getKey(), new IDManglingEventsResult(entry.getValue(), account.getAccountId()));
        }
        return results;
    }

    @Override
    public Map<String, SequenceResult> getSequenceNumbers(List<String> folderIds) throws OXException {
        Map<CalendarAccount, List<String>> foldersPerAccount = getRelativeFolderIdsPerAccount(folderIds);
        if (foldersPerAccount.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SequenceResult> sequenceResults = new HashMap<String, SequenceResult>(folderIds.size());
        for (Map.Entry<CalendarAccount, List<String>> entry : foldersPerAccount.entrySet()) {
            CalendarAccount account = entry.getKey();
            try {
                CalendarAccess access = getAccess(account.getAccountId(), SyncAware.class);
                if ((access instanceof FolderSyncAware)) {
                    for (String folderId : entry.getValue()) {
                        SequenceResult sequenceResult = ((FolderSyncAware) access).getSequenceNumber(folderId);
                        sequenceResults.put(getUniqueFolderId(account.getAccountId(), folderId), sequenceResult);
                    }
                } else if ((access instanceof BasicSyncAware)) {
                    for (String folderId : entry.getValue()) {
                        Check.folderMatches(folderId, BasicCalendarAccess.FOLDER_ID);
                        SequenceResult sequenceResult = ((BasicSyncAware) access).getSequenceNumber();
                        sequenceResults.put(getUniqueFolderId(account.getAccountId(), folderId), sequenceResult);
                    }
                } else {
                    throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
                }
            } catch (OXException e) {
                throw withUniqueIDs(e, account.getAccountId());
            }
        }
        return sequenceResults;
    }

    @Override
    public CalendarFolder getDefaultFolder() throws OXException {
        try {
            GroupwareCalendarFolder defaultFolder = getInternalAccess().getDefaultFolder();
            return withUniqueID(defaultFolder, DEFAULT_ACCOUNT);
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public List<AccountAwareCalendarFolder> getVisibleFolders(GroupwareFolderType type) throws OXException {
        List<AccountAwareCalendarFolder> folders = new ArrayList<AccountAwareCalendarFolder>();
        for (CalendarAccount account : getAccounts()) {
            try {
                folders.addAll(withUniqueID(getVisibleFolders(account, type), account));
            } catch (OXException e) {
                throw withUniqueIDs(e, account.getAccountId());
            }
        }
        return folders;
    }

    @Override
    public AccountAwareCalendarFolder getFolder(String folderId) throws OXException {
        CalendarAccount account = getAccount(getAccountId(folderId));
        try {
            return withUniqueID(getFolder(account, getRelativeFolderId(folderId)), account);
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public List<AccountAwareCalendarFolder> getFolders(List<String> folderIds) throws OXException {
        Map<CalendarAccount, List<String>> foldersPerAccount = getRelativeFolderIdsPerAccount(folderIds);
        if (foldersPerAccount.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccountAwareCalendarFolder> folders = new ArrayList<AccountAwareCalendarFolder>(folderIds.size());
        for (Map.Entry<CalendarAccount, List<String>> entry : foldersPerAccount.entrySet()) {
            CalendarAccount account = entry.getKey();
            try {
                for (String folderId : entry.getValue()) {
                    folders.add(withUniqueID(getFolder(account, folderId), account));
                }
            } catch (OXException e) {
                throw withUniqueIDs(e, account.getAccountId());
            }
        }
        return folders;
    }

    @Override
    public CalendarResult createEvent(String folderId, Event event) throws OXException {
        int accountId = getAccountId(folderId);
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.createEvent(getRelativeFolderId(folderId), withRelativeID(event));
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public CalendarResult putResource(String folderId, CalendarObjectResource resource, boolean replace) throws OXException {
        int accountId = getAccountId(folderId);
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.putResource(getRelativeFolderId(folderId), withRelativeID(resource), replace);
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public CalendarResult updateEvent(EventID eventID, Event event, long clientTimestamp) throws OXException {
        int accountId = getAccountId(eventID.getFolderID());
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.updateEvent(getRelativeId(eventID), withRelativeID(event), clientTimestamp);
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public CalendarResult moveEvent(EventID eventID, String targetFolderId, long clientTimestamp) throws OXException {

        //TODO: Move between groupware accesses?

        int accountId = getAccountId(eventID.getFolderID());
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.moveEvent(getRelativeId(eventID), getRelativeFolderId(targetFolderId), clientTimestamp);
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public CalendarResult updateAttendee(EventID eventID, Attendee attendee, List<Alarm> alarms, long clientTimestamp) throws OXException {
        int accountId = getAccountId(eventID.getFolderID());
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.updateAttendee(getRelativeId(eventID), attendee, alarms, clientTimestamp);
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public CalendarResult updateAlarms(EventID eventID, List<Alarm> alarms, long clientTimestamp) throws OXException {
        CalendarAccount account = getAccount(getAccountId(eventID.getFolderID()));
        try {
            PersonalAlarmAware calendarAccess = getAccess(account.getAccountId(), PersonalAlarmAware.class);
            CalendarResult result = calendarAccess.updateAlarms(getRelativeId(eventID), alarms, clientTimestamp);
            return new IDManglingCalendarResult(result, account.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public CalendarResult changeOrganizer(EventID eventID, Organizer organizer, long clientTimestamp) throws OXException {
        int accountId = getAccountId(eventID.getFolderID());
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.changeOrganizer(getRelativeId(eventID), organizer, clientTimestamp);
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public CalendarResult deleteEvent(EventID eventID, long clientTimestamp) throws OXException {
        ErrorAwareCalendarResult result = deleteEvents(Collections.singletonList(eventID), clientTimestamp).get(eventID);
        if (null == result) {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("No delete result for " + eventID);
        }
        if (null != result.getError()) {
            throw result.getError();
        }
        return result;
    }

    @Override
    public Map<EventID, ErrorAwareCalendarResult> deleteEvents(List<EventID> eventIDs, long clientTimestamp) {
        Map<EventID, ErrorAwareCalendarResult> results = new HashMap<EventID, ErrorAwareCalendarResult>(eventIDs.size());
        /*
         * get event identifiers per account & track possible errors
         */
        Map<EventID, OXException> errorsPerEventId = new HashMap<EventID, OXException>();
        Map<CalendarAccount, List<EventID>> relativeEventIdsPerAccount = getRelativeEventIdsPerAccount(eventIDs, errorsPerEventId);
        results.putAll(getErrorCalendarResults(errorsPerEventId));
        /*
         * delete events per account & return appropriate result
         */
        for (Entry<CalendarAccount, List<EventID>> entry : relativeEventIdsPerAccount.entrySet()) {
            results.putAll(deleteEvents(entry.getKey(), entry.getValue(), clientTimestamp));
        }
        return getOrderedResults(results, eventIDs);
    }

    @Override
    public CalendarResult splitSeries(EventID eventID, DateTime splitPoint, String uid, long clientTimestamp) throws OXException {
        int accountId = getAccountId(eventID.getFolderID());
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            CalendarResult result = calendarAccess.splitSeries(getRelativeId(eventID), splitPoint, uid, clientTimestamp);
            return new IDManglingCalendarResult(result, accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public List<ImportResult> importEvents(String folderId, List<Event> events) throws OXException {
        int accountId = getAccountId(folderId);
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(accountId);
            List<ImportResult> results = calendarAccess.importEvents(getRelativeFolderId(folderId), events);
            if (null == results) {
                return null;
            }
            List<ImportResult> importResultsWithUniqueId = new ArrayList<ImportResult>(results.size());
            for (ImportResult result : results) {
                importResultsWithUniqueId.add(new IDManglingImportResult(result, accountId));
            }
            return importResultsWithUniqueId;
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public List<AlarmTrigger> getAlarmTriggers(Set<String> actions) throws OXException {
        List<AlarmTrigger> result = new ArrayList<AlarmTrigger>();
        for (CalendarAccount account : getAccounts(CalendarCapability.ALARMS)) {
            try {
                for (AlarmTrigger trigger : getAccess(account, PersonalAlarmAware.class).getAlarmTriggers(actions)) {
                    trigger.setFolder(getUniqueFolderId(account.getAccountId(), trigger.getFolder()));
                    result.add(trigger);
                }
            } catch (OXException e) {
                warnings.add(withUniqueIDs(e, account.getAccountId()));
            }
        }
        if (1 < result.size()) {
            Collections.sort(result);
        }
        return result;
    }

    @Override
    public IFileHolder getAttachment(EventID eventID, int managedId) throws OXException {
        int accountId = getAccountId(eventID.getFolderID());
        try {
            EventID relativeEventID = getRelativeId(eventID);
            return getGroupwareAccess(accountId).getAttachment(relativeEventID, managedId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public Map<Attendee, FreeBusyResult> queryFreeBusy(List<Attendee> attendees, Date from, Date until, boolean merge) throws OXException {
        List<FreeBusyProvider> freeBusyProviders = getFreeBusyProviders();
        if (freeBusyProviders.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, List<FreeBusyResult>> results;
        if (1 == freeBusyProviders.size()) {
            results = queryFreeBusy(freeBusyProviders.get(0), attendees, from, until, merge);
        } else {
            CompletionService<Map<Attendee, List<FreeBusyResult>>> completionService = getCompletionService();
            for (FreeBusyProvider freeBusyProvider : freeBusyProviders) {
                completionService.submit(() -> queryFreeBusy(freeBusyProvider, attendees, from, until, merge));
            }
            results = collectFreeBusyResults(completionService, freeBusyProviders.size());
        }
        /*
         * build combined results, maintaining client-supplied order of attendees
         */
        Map<Attendee, FreeBusyResult> combinedResults = new LinkedHashMap<Attendee, FreeBusyResult>(results.size());
        for (Attendee attendee : attendees) {
            List<FreeBusyResult> freeBusyResults = results.get(attendee);
            if (null == freeBusyResults) {
                OXException e = CalendarExceptionCodes.FREEBUSY_NOT_AVAILABLE.create(extractEMailAddress(attendee.getUri()));
                combinedResults.put(attendee, new FreeBusyResult(null, Collections.singletonList(e)));
            } else {
                combinedResults.put(attendee, merge ? FreeBusyUtils.merge(freeBusyResults) : FreeBusyUtils.combine(freeBusyResults));
            }
        }
        return combinedResults;
    }

    @Override
    public String createFolder(String providerId, CalendarFolder folder, JSONObject userConfig) throws OXException {
        /*
         * create folder within matching folder-aware account targeted by parent folder if set
         */
        String parentFolderId = (folder instanceof GroupwareCalendarFolder) ? ((GroupwareCalendarFolder) folder).getParentId() : null;
        if (Strings.isNotEmpty(parentFolderId)) {
            int accountId = getAccountId(parentFolderId);
            CalendarAccount existingAccount = optAccount(accountId);
            if (null != existingAccount && (null == providerId || providerId.equals(existingAccount.getProviderId()))) {
                try {
                    FolderCalendarAccess calendarAccess = getAccess(accountId, FolderCalendarAccess.class);
                    String folderId = calendarAccess.createFolder(withRelativeID(folder));
                    return getUniqueFolderId(existingAccount.getAccountId(), folderId, (calendarAccess instanceof GroupwareCalendarAccess));
                } catch (OXException e) {
                    throw withUniqueIDs(e, existingAccount.getAccountId());
                }
            }
        }
        /*
         * dynamically create new account for provider, otherwise
         */
        if (null == providerId) {
            throw CalendarExceptionCodes.MANDATORY_FIELD.create("provider");
        }
        CalendarSettings settings = getBasicCalendarSettings(folder, userConfig);
        CalendarAccount newAccount = requireService(CalendarAccountService.class, services).createAccount(session, providerId, settings, this);
        return getUniqueFolderId(newAccount.getAccountId(), BasicCalendarAccess.FOLDER_ID);
    }

    @Override
    public String updateFolder(String folderId, CalendarFolder folder, JSONObject userConfig, long clientTimestamp) throws OXException {
        int accountId = getAccountId(folderId);
        try {
            CalendarAccess calendarAccess = getAccess(accountId);
            if ((calendarAccess instanceof FolderCalendarAccess)) {
                /*
                 * update folder within folder-aware account
                 */
                String updatedId = ((FolderCalendarAccess) calendarAccess).updateFolder(getRelativeFolderId(folderId), withRelativeID(folder), clientTimestamp);
                /*
                 * additionally update account settings as needed
                 */
                if (null != userConfig) {
                    requireService(CalendarAccountService.class, services).updateAccount(session, accountId, userConfig, clientTimestamp, this);
                }
                return getUniqueFolderId(accountId, updatedId, (calendarAccess instanceof GroupwareCalendarAccess));
            }
            /*
             * update account settings
             */
            Check.folderMatches(getRelativeFolderId(folderId), BasicCalendarAccess.FOLDER_ID);
            CalendarSettings settings = getBasicCalendarSettings(folder, userConfig);
            CalendarAccount updatedAccount = requireService(CalendarAccountService.class, services).updateAccount(session, accountId, settings, clientTimestamp, this);
            return getUniqueFolderId(updatedAccount.getAccountId(), BasicCalendarAccess.FOLDER_ID);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public void deleteFolder(String folderId, long clientTimestamp) throws OXException {
        int accountId = getAccountId(folderId);
        try {
            if (isFolderCalendarProvider(accountId)) {
                CalendarAccess calendarAccess = getAccess(accountId);
                /*
                 * delete folder in calendar account
                 */
                ((FolderCalendarAccess) calendarAccess).deleteFolder(getRelativeFolderId(folderId), clientTimestamp);
            } else {
                /*
                 * delete whole calendar account if not folder-aware
                 */
                Check.folderMatches(getRelativeFolderId(folderId), BasicCalendarAccess.FOLDER_ID);
                requireService(CalendarAccountService.class, services).deleteAccount(session, accountId, clientTimestamp, this);
            }
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    @Override
    public String getCTag(String folderID) throws OXException {
        CalendarAccount account = getAccount(getAccountId(folderID));
        try {
            CalendarAccess access = getAccess(account.getAccountId(), CTagAware.class);
            if ((access instanceof BasicCTagAware)) {
                return ((BasicCTagAware) access).getCTag();
            }
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
        } catch (OXException e) {
            throw withUniqueIDs(e, account.getAccountId());
        }
    }

    @Override
    public RecurrenceInfo getRecurrenceInfo(EventID eventID) throws OXException {
        int accountId = getAccountId(eventID.getFolderID());
        try {
            return withUniqueID(getGroupwareAccess(accountId).getRecurrenceInfo(getRelativeId(eventID)), accountId);
        } catch (OXException e) {
            throw withUniqueIDs(e, accountId);
        }
    }

    /**
     * Gets all visible folders of a certain type in a specific calendar account.
     * <p/>
     * In case of certain errors (provider not available or disabled by capability), a placeholder folder for the non-functional account
     * is returned automatically.
     *
     * @param account The calendar account to get the visible folders from
     * @param type The groupware folder type
     * @return The visible folders (with <i>relative</i> identifiers), or an empty list if there are none
     */
    private List<? extends CalendarFolder> getVisibleFolders(CalendarAccount account, GroupwareFolderType type) throws OXException {
        /*
         * query or build visible folders for calendar account
         */
        CalendarAccess access = getAccess(account.getAccountId());
        if ((access instanceof GroupwareCalendarAccess)) {
            return ((GroupwareCalendarAccess) access).getVisibleFolders(type);
        }
        if (false == GroupwareFolderType.PRIVATE.equals(type)) {
            return Collections.emptyList();
        }
        if ((access instanceof BasicCalendarAccess)) {
            return Collections.singletonList(getBasicCalendarFolder((BasicCalendarAccess) access, isAutoProvisioned(account)));
        }
        if ((access instanceof FolderCalendarAccess)) {
            return ((FolderCalendarAccess) access).getVisibleFolders();
        }
        /*
         * unsupported, otherwise (should not get here, though)
         */
        throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
    }

    /**
     * Gets a folder in a specific calendar account.
     *
     * @param account The calendar account to get the folder from
     * @param folderId The <i>relative</i> identifier of the folder to get
     * @return The folder (with <i>relative</i> identifiers)
     */
    private CalendarFolder getFolder(CalendarAccount account, String folderId) throws OXException {
        /*
         * query or get the folder from account
         */
        CalendarAccess access = getAccess(account.getAccountId());
        if ((access instanceof FolderCalendarAccess)) {
            return ((FolderCalendarAccess) access).getFolder(folderId);
        }
        if ((access instanceof BasicCalendarAccess)) {
            Check.folderMatches(folderId, BasicCalendarAccess.FOLDER_ID);
            return getBasicCalendarFolder((BasicCalendarAccess) access, isAutoProvisioned(account));
        }
        /*
         * unsupported, otherwise (should not get here, though)
         */
        throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
    }

    /**
     * Gets all events in a list of folders from a specific calendar account. Potential errors are placed in the results implicitly.
     *
     * @param account The calendar account
     * @param folderIds The relative identifiers of the folders to get the events from
     * @return The events results per folder, already adjusted to contain unique composite identifiers
     */
    private Map<String, EventsResult> getEventsInFolders(CalendarAccount account, List<String> folderIds) {
        Map<String, EventsResult> eventsPerFolderId = new HashMap<String, EventsResult>(folderIds.size());
        try {
            CalendarAccess access = getAccess(account);
            if ((access instanceof FolderCalendarAccess)) {
                eventsPerFolderId.putAll(((FolderCalendarAccess) access).getEventsInFolders(folderIds));
            } else if ((access instanceof BasicCalendarAccess)) {
                for (String folderId : folderIds) {
                    try {
                        Check.folderMatches(folderId, BasicCalendarAccess.FOLDER_ID);
                        eventsPerFolderId.put(folderId, new DefaultEventsResult(((BasicCalendarAccess) access).getEvents()));
                    } catch (OXException e) {
                        eventsPerFolderId.put(folderId, new DefaultEventsResult(e));
                    }
                }
            } else {
                throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
            }
        } catch (OXException e) {
            for (String folderId : folderIds) {
                eventsPerFolderId.put(folderId, new DefaultEventsResult(e));
            }
        }
        return withUniqueIDs(eventsPerFolderId, account.getAccountId());
    }

    /**
     * Deletes a list of events from a specific calendar account. Potential errors are placed in the results implicitly.
     *
     * @param account The calendar account
     * @param eventIds The relative identifiers of the events to delete
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     * @return The results per event identifier, already adjusted to contain unique composite identifiers
     */
    private Map<EventID, ErrorAwareCalendarResult> deleteEvents(CalendarAccount account, List<EventID> eventIds, long clientTimestamp) {
        Map<EventID, ErrorAwareCalendarResult> results = new HashMap<EventID, ErrorAwareCalendarResult>(eventIds.size());
        try {
            GroupwareCalendarAccess calendarAccess = getGroupwareAccess(account);
            for (EventID eventId : eventIds) {
                try {
                    CalendarResult result = calendarAccess.deleteEvent(eventId, clientTimestamp);
                    results.put(eventId, new DefaultErrorAwareCalendarResult(result, Collections.emptyList(), null));
                } catch (OXException e) {
                    DefaultCalendarResult result = new DefaultCalendarResult(session, session.getUserId(), eventId.getFolderID(), null, null, null);
                    results.put(eventId, new DefaultErrorAwareCalendarResult(result, Collections.emptyList(), e));
                }
            }
        } catch (OXException e) {
            for (EventID eventId : eventIds) {
                DefaultCalendarResult result = new DefaultCalendarResult(session, session.getUserId(), eventId.getFolderID(), null, null, null);
                results.put(eventId, new DefaultErrorAwareCalendarResult(result, Collections.emptyList(), e));
            }
        }
        return withUniqueEventIDs(results, account.getAccountId());
    }

    /**
     * Searches for events in all accounts with the specified {@link SearchTerm}.
     *
     * @param term the {@link SearchTerm}
     * @return The found events per folder
     */
    private Map<String, EventsResult> searchEvents(SearchTerm<?> term) throws OXException {
        List<CalendarAccount> accounts = getAccounts(CalendarCapability.SEARCH);
        if (accounts.isEmpty()) {
            return ImmutableMap.of();
        }
        if (1 == accounts.size()) {
            return searchEventsInFolders(accounts.get(0), null, term);
        }
        CompletionService<Map<String, EventsResult>> completionService = getCompletionService();
        for (CalendarAccount account : accounts) {
            completionService.submit(() -> searchEventsInFolders(account, null, term));
        }
        return collectEventsResults(completionService, accounts.size());
    }

    /**
     * Searches events in the specified folders.
     *
     * @param account The account
     * @param folderIds The folder identifiers
     * @param term The search term
     * @return The found events per folder
     */
    private Map<String, EventsResult> searchEventsInFolders(CalendarAccount account, List<String> folderIds, SearchTerm<?> term) {
        Map<String, EventsResult> eventsPerFolderId = new HashMap<String, EventsResult>();
        try {
            CalendarAccess access = getAccess(account);
            if ((access instanceof FolderSearchAware)) {
                eventsPerFolderId.putAll(((FolderSearchAware) access).searchEvents(folderIds, term));
            } else if ((access instanceof BasicSearchAware)) {
                if (null != folderIds) {
                    for (String folderId : folderIds) {
                        Check.folderMatches(folderId, BasicCalendarAccess.FOLDER_ID);
                    }
                }
                List<Event> events = ((BasicSearchAware) access).searchEvents(term);
                eventsPerFolderId.put(BasicCalendarAccess.FOLDER_ID, new DefaultEventsResult(events));
            } else {
                throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
            }
        } catch (OXException e) {
            if (null != folderIds) {
                for (String folderId : folderIds) {
                    eventsPerFolderId.put(folderId, new DefaultEventsResult(e));
                }
            } else {
                warnings.add(e);
            }
        }
        return withUniqueIDs(eventsPerFolderId, account.getAccountId());
    }

    /**
     * Performs a free/busy time lookup for a list of attendees using a specific free/busy provider. Potential errors are placed in the
     * results as warnings implicitly.
     *
     * @param provider The provider to use
     * @param attendees The queried attendees
     * @param from The start of the requested time range
     * @param until The end of the requested time range
     * @param merge <code>true</code> to merge the resulting free/busy-times, <code>false</code>, otherwise
     * @return The free/busy results per attendee, already adjusted to contains unqiue composite identifiers
     */
    private Map<Attendee, List<FreeBusyResult>> queryFreeBusy(FreeBusyProvider provider, List<Attendee> attendees, Date from, Date until, boolean merge) {
        if (null == attendees || attendees.isEmpty()) {
            return Collections.emptyMap();
        }
        /*
         * query free/busy data
         */
        Map<Attendee, List<FreeBusyResult>> results = new HashMap<Attendee, List<FreeBusyResult>>(attendees.size());
        Map<Attendee, Map<Integer, FreeBusyResult>> resultsInAccounts;
        try {
            resultsInAccounts = provider.query(session, attendees, from, until, merge, this);
        } catch (OXException e) {
            /*
             * create appropriate error result
             */
            FreeBusyResult errorResult = new FreeBusyResult(Collections.emptyList(), Collections.singletonList(e));
            for (Attendee attendee : attendees) {
                results.put(attendee, Collections.singletonList(errorResult));
            }
            return results;
        }
        /*
         * collect results for attendee from all accounts, enhanced by fully qualified identifiers as needed
         */
        if (null != resultsInAccounts && 0 < resultsInAccounts.size()) {
            for (Entry<Attendee, Map<Integer, FreeBusyResult>> resultsForAttendee : resultsInAccounts.entrySet()) {
                Attendee attendee = CalendarUtils.find(attendees, resultsForAttendee.getKey());
                if (null == attendee) {
                    LOG.debug("Skipping unexpected attendee {} in free/busy results from provider {}", attendee, provider);
                    continue;
                }
                for (Entry<Integer, FreeBusyResult> resultsForAccount : resultsForAttendee.getValue().entrySet()) {
                    FreeBusyResult result = resultsForAccount.getValue();
                    if (false == FreeBusyProvider.NO_ACCOUNT.equals(resultsForAccount.getKey())) {
                        result = withUniqueID(resultsForAccount.getValue(), i(resultsForAccount.getKey()));
                    }
                    com.openexchange.tools.arrays.Collections.put(results, attendee, result);
                }
            }
        }
        return results;
    }

    /**
     * Takes a specific number of event list results from the completion service, and adds them to a single resulting, sorted list of
     * events.
     *
     * @param completionService The completion service to take the results from
     * @param count The number of results to collect
     * @return The resulting list of events
     */
    private Map<String, EventsResult> collectEventsResults(CompletionService<Map<String, EventsResult>> completionService, int count) throws OXException {
        Map<String, EventsResult> results = new HashMap<String, EventsResult>();
        for (int i = 0; i < count; i++) {
            try {
                results.putAll(completionService.take().get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (null != cause && (e.getCause() instanceof OXException)) {
                    throw (OXException) cause;
                }
                throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
            Check.resultSizeNotExceeded(getSelfProtection(), results, get(CalendarParameters.PARAMETER_FIELDS, EventField[].class));
        }
        return results;
    }

    private Map<Attendee, List<FreeBusyResult>> collectFreeBusyResults(CompletionService<Map<Attendee, List<FreeBusyResult>>> completionService, int count) throws OXException {
        Map<Attendee, List<FreeBusyResult>> results = new HashMap<Attendee, List<FreeBusyResult>>();
        for (int i = 0; i < count; i++) {
            try {
                for (Entry<Attendee, List<FreeBusyResult>> entry : completionService.take().get().entrySet()) {
                    com.openexchange.tools.arrays.Collections.put(results, entry.getKey(), entry.getValue());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (null != cause && (e.getCause() instanceof OXException)) {
                    throw (OXException) cause;
                }
                throw CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
        return results;
    }

    private SelfProtection getSelfProtection() {
        if (protection == null) {
            LeanConfigurationService leanConfigurationService = services.getService(LeanConfigurationService.class);
            protection = SelfProtectionFactory.createSelfProtection(leanConfigurationService);
        }
        return protection;
    }

    private static Event find(List<Event> events, EventID eventID) {
        return find(events, eventID.getFolderID(), eventID.getObjectID(), eventID.getRecurrenceID());
    }

    private static Event find(List<Event> events, String folderId, String eventId, RecurrenceId recurrenceId) {
        if (null != events) {
            for (Event event : events) {
                if (null != event && eventId.equals(event.getId()) &&
                    (folderId.equals(event.getFolderId()) || folderId.equals(BasicCalendarAccess.FOLDER_ID) && null == event.getFolderId())) {
                    if (null == recurrenceId || recurrenceId.matches(event.getRecurrenceId())) {
                        return event;
                    }
                }
            }
        }
        return null;
    }

    private CalendarFolder getBasicCalendarFolder(BasicCalendarAccess calendarAccess, boolean autoProvisioned) {
        DefaultCalendarFolder folder = new DefaultCalendarFolder();
        folder.setId(BasicCalendarAccess.FOLDER_ID);
        CalendarSettings settings = calendarAccess.getSettings();
        folder.setAccountError(settings.getError());
        folder.setExtendedProperties(settings.getExtendedProperties());
        folder.setName(settings.getName());
        folder.setLastModified(settings.getLastModified());
        folder.setSubscribed(B(settings.isSubscribed()));
        folder.setUsedForSync(settings.getUsedForSync().orElse(UsedForSync.DEFAULT));
        folder.setPermissions(Collections.singletonList(new DefaultCalendarPermission(
            String.valueOf(session.getUserId()), session.getUserId(), null,
            CalendarPermission.READ_FOLDER, CalendarPermission.READ_ALL_OBJECTS, CalendarPermission.NO_PERMISSIONS,
            CalendarPermission.NO_PERMISSIONS, false == autoProvisioned, false, 0)));
        folder.setSupportedCapabilites(CalendarCapability.getCapabilities(calendarAccess.getClass()));
        return folder;
    }

    private CalendarSettings getBasicCalendarSettings(CalendarFolder calendarFolder, JSONObject userConfig) {
        CalendarSettings settings = new CalendarSettings();
        if (null != calendarFolder.getExtendedProperties()) {
            settings.setExtendedProperties(calendarFolder.getExtendedProperties());
        }
        if (null != calendarFolder.getAccountError()) {
            settings.setError(calendarFolder.getAccountError());
        }
        if (null != calendarFolder.getName()) {
            settings.setName(calendarFolder.getName());
        }
        if (null != calendarFolder.getLastModified()) {
            settings.setLastModified(calendarFolder.getLastModified());
        }
        if (null != userConfig) {
            settings.setConfig(userConfig);
        }
        if (null != calendarFolder.isSubscribed()) {
            settings.setSubscribed(b(calendarFolder.isSubscribed()));
        }
        if (null != calendarFolder.getUsedForSync()) {
            settings.setUsedForSync(calendarFolder.getUsedForSync());
        }
        return settings;
    }

    /**
     * Creates a map whose entries are in the same order as the identifiers were requested by the client.
     *
     * @param resultsPerId The unordered results map
     * @param requestedIds The identifiers in a list as requested from the client
     * @return The ordered results
     */
    private static <K, V> Map<K, V> getOrderedResults(Map<K, V> resultsPerId, List<K> requestedIds) {
        if (null != requestedIds && null != resultsPerId && 1 < requestedIds.size()) {
            LinkedHashMap<K, V> sortedResults = new LinkedHashMap<K, V>(requestedIds.size());
            for (K id : requestedIds) {
                sortedResults.put(id, resultsPerId.get(id));
            }
        }
        return resultsPerId;
    }

    /*
     * ============================== IDBasedSchedulingAccess ==============================
     */

    @Override
    public IncomingSchedulingMessage createPatchedMessage(int accountId, String folderId, String mailId, String sequenceId) throws OXException {
        try {
            return getInternalAccess().createPatchedMessage(accountId, folderId, mailId, sequenceId);
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public ITipAnalysis analyze(IncomingSchedulingMessage message) throws OXException {
        try {
            ITipAnalysis analysis = getInternalAccess().analyze(message);
            return new IDManglingITipAnalysis(analysis, DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

    @Override
    public CalendarResult handleIncomingScheduling(SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException {
        try {
            CalendarResult result = getInternalAccess().handleIncomingScheduling(source, message, attendee);
            return new IDManglingCalendarResult(result, DEFAULT_ACCOUNT.getAccountId());
        } catch (OXException e) {
            throw withUniqueIDs(e, DEFAULT_ACCOUNT.getAccountId());
        }
    }

}
