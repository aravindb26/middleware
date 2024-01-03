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

import static com.openexchange.chronos.common.CalendarUtils.isSimilarICloudIMipMeCom;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.scheduling.analyzers.Utils.optComment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.chronos.scheduling.ITipChange.Type;
import com.openexchange.chronos.scheduling.ITipSequence;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.analyzers.annotations.AnnotationHelper;
import com.openexchange.chronos.scheduling.changes.Change;
import com.openexchange.chronos.scheduling.common.DefaultChange;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link DeclineCounterAnalyzer} - Analyzer for the iTIP method <code>DECLINECOUNTER</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.8">RFC 5546 Section 3.2.8</a>
 */
public class DeclineCounterAnalyzer extends AbstractSchedulingAnalyzer {

    private final static Logger LOG = LoggerFactory.getLogger(DeclineCounterAnalyzer.class);

    /**
     * Initializes a new {@link DeclineCounterAnalyzer}.
     * 
     * @param services The services
     */
    public DeclineCounterAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.DECLINECOUNTER);
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        for (Event patchedEvent : objectResourceProvider.getIncomingEvents()) {
            Event storedEvent = objectResourceProvider.optMatchingEvent(patchedEvent);
            if (null == storedEvent) {
                analyzedChanges.add(analyzeUnknownEvent(session, patchedEvent, originator, targetUser));
            } else {
                analyzedChanges.add(analyzeKnownEvent(session, patchedEvent, storedEvent, originator, targetUser));
            }
        }
        return analyzedChanges;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#DECLINECOUNTER} from the originator, where no
     * corresponding stored event (occurrence) exists for (anymore).
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, CalendarUser originator, int targetUser) throws OXException {
        /*
         * add introductional annotation for the incoming message as such
         */
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        change.addAnnotations(getIntroductions(session, event, null, originator));
        /*
         * targeted event no longer found, offer to refresh the scheduling object resource or to ignore
         */
        change.addAnnotation(annotationHelper.getCounterDeclinedForUnknownHint(targetUser));
        change.addActions(ITipAction.REQUEST_REFRESH, ITipAction.IGNORE);
        return change;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#DECLINECOUNTER} from the originator, where a
     * corresponding stored event (occurrence) exists for.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event or occurrence
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The analyzed change
     */
    private AnalyzedChange analyzeKnownEvent(CalendarSession session, Event event, Event storedEvent, CalendarUser originator, int targetUser) throws OXException {
        /*
         * add introductional annotation for the incoming message as such
         */
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        change.addAnnotations(getIntroductions(session, event, storedEvent, originator));
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        if (ITipSequence.of(storedEvent).after(ITipSequence.of(event))) {
            /*
             * event has been updated in the meantime, say so
             */
            change.setChange(getChange(session, event, targetUser, false));
            change.addAnnotation(annotationHelper.getCounterDeclinedUpdatedHint());
            change.addActions(ITipAction.IGNORE);
        } else if (event.getSequence() > storedEvent.getSequence()) {
            /*
             * decline counter response with higher sequence number, suggest to refresh local copy
             */
            change.addAnnotation(annotationHelper.getCounterDeclinedForUpdatedHint());
            change.addActions(ITipAction.REQUEST_REFRESH);
        } else if (ITipSequence.of(event).afterOrEquals(ITipSequence.of(storedEvent))) {
            /*
             * decline counter for currently stored event, perform further checks & include suggestions as needed
             */
            if (false == matches(storedEvent.getOrganizer(), event.getOrganizer()) && //
                false == isSimilarICloudIMipMeCom(storedEvent.getOrganizer(), event.getOrganizer())) {
                /*
                 * organizer change, add corresponding hint & offer "ignore" action
                 */
                change.addAnnotation(annotationHelper.getOrganizerChangedHint());
                change.addActions(ITipAction.IGNORE);
            }
            /*
             * add hint about the current participation status and add actions to change it (with or w/o conflicts)
             */
            change.setChange(getChange(session, event, targetUser, true));
            change.addAnnotations(annotationHelper.getPartStatDescriptions(event, targetUser));
            if (com.openexchange.tools.arrays.Collections.isNotEmpty(change.getChange().getConflicts())) {
                change.addAnnotation(annotationHelper.getConflictsHint(targetUser));
                change.addActions(ITipAction.DECLINE, ITipAction.TENTATIVE, ITipAction.ACCEPT_AND_IGNORE_CONFLICTS);
            } else {
                change.addActions(ITipAction.DECLINE, ITipAction.TENTATIVE, ITipAction.ACCEPT);
            }
        } else {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Illegal sequence/dtstamp in events");
        }
        return change;
    }

    /**
     * Gets the introductional annotation(s) describing the event included in an incoming {@link SchedulingMethod#DECLINECOUNTER} from the
     * originator.
     * 
     * @param session The calendar session
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param originator The originator of the scheduling message
     * @return The introductional annotations
     */
    private List<ITipAnnotation> getIntroductions(CalendarSession session, Event event, Event storedEvent, CalendarUser originator) {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>(2);
        annotations.add(annotationHelper.getCounterDeclinedIntroduction(event, storedEvent, originator));
        optComment(event).ifPresent((c) -> annotations.add(annotationHelper.getCounterDeclinedCommentHint(c)));
        return annotations;
    }

    /**
     * Constructs a minimal {@link Change} for an event from a {@link SchedulingMethod#DECLINECOUNTER} message.
     * 
     * @param session The calendar session
     * @param storedEvent The currently stored event (occurrence) where the decline counter is targeted at
     * @param targetUser The user id of the scheduling message's recipient
     * @param checkConflicts <code>true</code> to check for conflicts in the targeted user's calendar, <code>false</code> otherwise
     * @return The change
     */
    private static DefaultChange getChange(CalendarSession session, Event storedEvent, int targetUser, boolean checkConflicts) {
        DefaultChange change = new DefaultChange();
        change.setType(Type.UPDATE);
        change.setCurrentEvent(storedEvent);
        if (checkConflicts) {
            try {
                Attendee attendee = session.getEntityResolver().prepareUserAttendee(targetUser);
                change.setConflicts(session.getFreeBusyService().checkForConflicts(session, storedEvent, Collections.singletonList(attendee)));
            } catch (OXException e) {
                LOG.warn("Unexpected error checking for conflicts for incoming event from DECLINECOUNTER message", e);
                session.addWarning(e);
            }
        }
        return change;
    }

}
