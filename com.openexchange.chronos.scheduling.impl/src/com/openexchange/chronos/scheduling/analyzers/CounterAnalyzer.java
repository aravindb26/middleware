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

package com.openexchange.chronos.scheduling.analyzers;

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedPropertyValue;
import static com.openexchange.chronos.scheduling.analyzers.Utils.asAttendee;
import static com.openexchange.chronos.scheduling.analyzers.Utils.optComment;
import static com.openexchange.chronos.scheduling.analyzers.Utils.selectAttendee;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.mapping.DefaultEventUpdate;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.chronos.scheduling.ITipChange.Type;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.analyzers.annotations.AnnotationHelper;
import com.openexchange.chronos.scheduling.changes.Change;
import com.openexchange.chronos.scheduling.common.DefaultChange;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link CounterAnalyzer} - Analyzer for the iTIP method <code>COUNTER</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.7">RFC 5546 Section 3.2.7</a>
 */
public class CounterAnalyzer extends AbstractSchedulingAnalyzer {

    /**
     * Initializes a new {@link CounterAnalyzer}.
     * 
     * @param services The services
     */
    public CounterAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.COUNTER);
    }

    @Override
    protected EventField[] getFieldsToLoad() {
        return null;
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        String prodId = objectResourceProvider.getAdditional("PRODID", String.class).orElse(null);
        for (Event patchedEvent : objectResourceProvider.getIncomingEvents()) {
            Event storedEvent = objectResourceProvider.optMatchingEvent(patchedEvent);
            Attendee counteringAttendee = selectAttendee(patchedEvent, originator, true);
            if (null == counteringAttendee) {
                /*
                 * attendee may have removed himself, use generic representation for originator as placeholder
                 */
                counteringAttendee = asAttendee(originator, CalendarUserType.INDIVIDUAL);
            }
            if (null == storedEvent) {
                Event eventTombstone = objectResourceProvider.optMatchingTombstone(patchedEvent);
                analyzedChanges.add(analyzeUnknownEvent(session, patchedEvent, eventTombstone, counteringAttendee, targetUser, prodId));
            } else {
                analyzedChanges.add(analyzeKnownEvent(session, patchedEvent, storedEvent, counteringAttendee, targetUser, prodId));
            }
        }
        return analyzedChanges;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#REPLY} from the originator, where a
     * corresponding stored event (occurrence) exists for.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event or occurrence
     * @param counteringAttendee The countering attendee as originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param prodId The product identifier as found in the incoming calendar resource, or <code>null</code> if not set
     * @return The analyzed change
     */
    private AnalyzedChange analyzeKnownEvent(CalendarSession session, Event event, Event storedEvent, Attendee counteringAttendee, int targetUser, String prodId) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation(s) for the incoming message as such, and the designated change
         */
        change.addAnnotations(getIntroductions(session, event, storedEvent, counteringAttendee, prodId));
        change.setChange(getChange(event));
        change.addActions(ITipAction.IGNORE);
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        if (storedEvent.getSequence() > event.getSequence()) {
            /*
             * event has been updated in the meantime, say so
             */
            change.addAnnotation(annotationHelper.getOutdatedCounterHint());
            change.addActions(ITipAction.DECLINECOUNTER);
            return change;
        }
        Attendee storedAttendee = find(storedEvent.getAttendees(), counteringAttendee);
        if (null == storedAttendee) {
            /*
             * uninvited attendee, add corresponding hint & only offer to ignore
             */
            change.addAnnotation(annotationHelper.getCounterFromUninvitedHint());
            change.addActions(ITipAction.DECLINECOUNTER);
        } else if (event.getSequence() == storedEvent.getSequence()) {
            /*
             * technically applicable if time change only, but not yet applied, say so & add corresponding apply options
             */
            if (considerAsTimeChangeOnly(event, storedEvent, prodId)) {
                change.addAnnotation(annotationHelper.getApplyCounterManuallyHint(targetUser));
                change.addActions(ITipAction.APPLY_PROPOSAL, ITipAction.DECLINECOUNTER);
            } else {
                change.addAnnotation(annotationHelper.getCounterUnsupportedHint(targetUser));
                change.addActions(ITipAction.DECLINECOUNTER);
            }
        }
        return change;
    }
    
    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#REPLY} from the originator, where a
     * corresponding stored event (occurrence) does not or no longer exist for.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param tombstoneEvent The corresponding event tombstone or occurrence, or <code>null</code> if there is none
     * @param counteringAttendee The replying attendee as originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param prodId The product identifier as found in the incoming calendar resource, or <code>null</code> if not set
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, Event tombstoneEvent, Attendee counteringAttendee, int targetUser, String prodId) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation for the incoming message as such
         */
        change.addAnnotations(getIntroductions(session, event, tombstoneEvent, counteringAttendee, prodId));
        change.setChange(getChange(event));
        change.addActions(ITipAction.IGNORE);
        if (null != tombstoneEvent && tombstoneEvent.getSequence() >= event.getSequence() && tombstoneEvent.getDtStamp() > event.getDtStamp()) {
            /*
             * event has been deleted in the meantime, say so
             */
            change.addAnnotation(annotationHelper.getCounterForDeletedHint());
        } else {
            /*
             * counter to unknown event, say so
             */
            change.addAnnotation(annotationHelper.getCounterForUnknownHint(targetUser));
        }
        return change;
    }

    /**
     * Gets the introductional annotation describing the event included in an incoming {@link SchedulingMethod#COUNTER} from the originator.
     * 
     * @param session The calendar session
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param counteringAttendee The replying attendee as originator of the scheduling message
     * @param prodId The product identifier as found in the incoming calendar resource, or <code>null</code> if not set
     * @return The introductional annotation(s)
     */
    private List<ITipAnnotation> getIntroductions(CalendarSession session, Event event, Event storedEvent, Attendee counteringAttendee, String prodId) {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>();
        /*
         * add introduction for the counter proposal
         */
        if (false == considerAsTimeChangeOnly(event, storedEvent, prodId)) {
            annotations.add(annotationHelper.getCounterChangesIntroduction(event, storedEvent, counteringAttendee));
        } else {
            annotations.add(annotationHelper.getCounterTimeIntroduction(event, storedEvent, counteringAttendee));
            if (null != storedEvent && null != event && null != event.getStartDate() && null != event.getEndDate()) {
                annotations.add(annotationHelper.getCounterProposedTimes(event, storedEvent));
            }
        }
        /*
         * add hint about a changed participation status as needed, as well as a comment if set
         */
        if (containsPartStatChange(storedEvent, counteringAttendee)) {
            annotations.add(annotationHelper.getRepliedIntroduction(event, storedEvent, counteringAttendee));
        }
        optComment(event).ifPresent((c) -> annotations.add(annotationHelper.getCounterCommentHint(c)));
        return annotations;
    }
    
    private static boolean considerAsTimeChangeOnly(Event event, Event storedEvent, String prodId) {
        /*
         * check for extended properties used by Outlook / Exchange
         */

        String msOriginalStart = optExtendedPropertyValue(event.getExtendedProperties(), "X-MS-OLK-ORIGINALSTART", String.class);
        String msOriginalEnd = optExtendedPropertyValue(event.getExtendedProperties(), "X-MS-OLK-ORIGINALEND", String.class);
        if (Strings.isNotEmpty(msOriginalEnd) && Strings.isNotEmpty(msOriginalStart)) {
            return true;
        }
        /*
         * check if PRODID indicates Google, which uses date-time counter proposals, only
         */
        if (null != prodId && prodId.contains("Google Calendar")) {
            return true;
        }
        /*
         * use generic checks, otherwise
         */
        EventField[] ignoredFields = new EventField[] {
            EventField.ID, EventField.FOLDER_ID, EventField.SERIES_ID, EventField.UID, EventField.FILENAME, EventField.RECURRENCE_ID, EventField.CALENDAR_USER, EventField.FLAGS,
            EventField.SEQUENCE, EventField.DTSTAMP, EventField.TIMESTAMP, EventField.CREATED, EventField.CREATED_BY, EventField.LAST_MODIFIED, EventField.MODIFIED_BY,
            EventField.CHANGE_EXCEPTION_DATES, EventField.ATTENDEE_PRIVILEGES
        };        
        DefaultEventUpdate eventUpdate = new DefaultEventUpdate(storedEvent, event, true, ignoredFields);
        Set<EventField> updatedFields = new HashSet<EventField>(eventUpdate.getUpdatedFields());
        return updatedFields.removeAll(Arrays.asList(EventField.START_DATE, EventField.END_DATE)) && updatedFields.isEmpty();
    }

    private static boolean containsPartStatChange(Event storedEvent, Attendee counteringAttendee) {
        if (null != counteringAttendee.getPartStat()) {
            Attendee originalAttendee = find(storedEvent.getAttendees(), counteringAttendee);
            return null != originalAttendee && false == counteringAttendee.getPartStat().matches(originalAttendee.getPartStat());
        }
        return false;
    }

    /**
     * Constructs a minimal {@link Change} for an event from a {@link SchedulingMethod#COUNTER} message.
     * 
     * @param incomingEvent The event from the incoming reply message to get the change for
     * @return The change
     */
    private static DefaultChange getChange(Event incomingEvent) {
        DefaultChange change = new DefaultChange();
        change.setType(Type.UPDATE);
        change.setNewEvent(incomingEvent);
        return change;
    }

}
