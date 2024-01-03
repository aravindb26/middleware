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
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isSimilarICloudIMipMeCom;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.scheduling.analyzers.Utils.optComment;
import static com.openexchange.chronos.scheduling.analyzers.Utils.optResourceSentBy;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.List;
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
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.server.ServiceLookup;

/**
 * {@link CancelAnalyzer} - Analyzer for the iTIP method <code>CANCEL</code>
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.5">RFC 5546 Section 3.2.5</a>
 */
public class CancelAnalyzer extends AbstractSchedulingAnalyzer {

    /**
     * Initializes a new {@link CancelAnalyzer}.
     * 
     * @param services The services
     */
    public CancelAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.CANCEL);
    }
    
    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        boolean usesOrganizerCopy = objectResourceProvider.usesOrganizerCopy();
        for (Event patchedEvent : objectResourceProvider.getIncomingEvents()) {
            Event storedEvent = objectResourceProvider.optMatchingEvent(patchedEvent);
            if (null == storedEvent) {
                Event eventTombstone = objectResourceProvider.optMatchingTombstone(patchedEvent);
                analyzedChanges.add(analyzeUnknownEvent(session, patchedEvent, eventTombstone, originator, targetUser, usesOrganizerCopy));
            } else {
                analyzedChanges.add(analyzeKnownEvent(session, patchedEvent, storedEvent, originator, targetUser));
            }
        }
        return analyzedChanges;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#CANCEL} from the originator, where no
     * corresponding stored event (occurrence) exists for (anymore).
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param tombstoneEvent The corresponding event tombstone or occurrence, or <code>null</code> if there is none
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param usesOrganizerCopy <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>,
     *            if the scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, Event tombstoneEvent, CalendarUser originator, int targetUser, boolean usesOrganizerCopy) throws OXException {
        /*
         * add introductional annotation for the incoming message as such
         */
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        change.addAnnotations(getIntroductions(session, event, tombstoneEvent, originator));
        change.setChange(getChange(event));
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        Attendee resourceSentBy = optResourceSentBy(event, originator);
        if (null != resourceSentBy && (false == isInternal(resourceSentBy) || false == session.getEntityResolver().getSchedulingPrivilege(
            resourceSentBy.getEntity(), session.getUserId()).implies(SchedulingPrivilege.DELEGATE))) {
            /*
             * add hint that user is not allowed to handle booking requests for this resource
             */
            change.addAnnotation(annotationHelper.getResourceNotDelegateHint(resourceSentBy));
            change.addActions(ITipAction.IGNORE);
        } else if (null == tombstoneEvent) {
            /*
             * neither event, nor tombstone found anymore, say so
             */
            change.addAnnotation(null == resourceSentBy ? annotationHelper.getCancelForUnknownHint(targetUser) : 
                annotationHelper.getResourceCancelForUnknownHint(resourceSentBy));
            change.addActions(ITipAction.IGNORE);
        } else if (ITipSequence.of(event).beforeOrEquals(ITipSequence.of(tombstoneEvent))) {
            /*
             * matching or newer event tombstone exists, assume cancellation was successfully applied
             */
            if (false == usesOrganizerCopy) {
                change.addAnnotation(null == resourceSentBy ? annotationHelper.getCancelAppliedHint(targetUser) : 
                    annotationHelper.getResourceCancelAppliedHint(resourceSentBy.getEntity()));
            }
        } else {
            /*
             * repeated deletion, still include cancel applied hint
             */
            if (false == usesOrganizerCopy) {
                change.addAnnotation(null == resourceSentBy ? annotationHelper.getCancelAppliedHint(targetUser) : 
                    annotationHelper.getResourceCancelAppliedHint(resourceSentBy.getEntity()));
            }
            session.addWarning(CalendarExceptionCodes.EVENT_SEQUENCE_NOT_FOUND.create(tombstoneEvent.getId(), I(tombstoneEvent.getSequence())));
        }
        return change;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#CANCEL} from the originator, where a
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
        change.setChange(getChange(event));
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        if (ITipSequence.of(storedEvent).after(ITipSequence.of(event))) {
            /*
             * event has been updated in the meantime, say so
             */
            change.addAnnotation(annotationHelper.getCancelUpdatedHint());
            change.addActions(ITipAction.IGNORE);
        } else if (ITipSequence.of(event).afterOrEquals(ITipSequence.of(storedEvent))) {
            /*
             * technically applicable, but not yet applied, perform further checks & include suggestions as needed 
             */            
            if (false == matches(storedEvent.getOrganizer(), event.getOrganizer()) && 
                false == isSimilarICloudIMipMeCom(storedEvent.getOrganizer(), event.getOrganizer())) {
                /*
                 * organizer change, add corresponding hint & offer "ignore" action
                 */
                change.addAnnotation(annotationHelper.getOrganizerChangedHint());
                change.addActions(ITipAction.IGNORE);
            }
            change.addAnnotation(annotationHelper.getApplyCancelManuallyHint(targetUser));
            change.setTargetedAttendee(find(event.getAttendees(), targetUser));
            change.addActions(ITipAction.APPLY_REMOVE);
        } else {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Illegal sequence/dtstamp in events");
        }
        return change;
    }

    /**
     * Gets the introductional annotation(s) describing the event included in an incoming {@link SchedulingMethod#CANCEL} from the
     * originator.
     * 
     * @param session The calendar session
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param originator The originator of the scheduling message
     * @return The introductional annotations
     */
    private List<ITipAnnotation> getIntroductions(CalendarSession session, Event event, Event storedEvent, CalendarUser originator) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>(2);
        Attendee resourceSentBy = optResourceSentBy(event, originator);
        annotations.add(null == resourceSentBy ? annotationHelper.getCanceledIntroduction(event, storedEvent, originator) : 
            annotationHelper.getResourceCancelledIntroduction(event, storedEvent, originator, resourceSentBy));
        optComment(event).ifPresent((c) -> annotations.add(annotationHelper.getCancelCommentHint(c)));
        return annotations;
    }

    /**
     * Constructs a minimal {@link Change} for an event from a {@link SchedulingMethod#CANCEL} message.
     * 
     * @param incomingEvent The deleted event from the incoming message to get the change for
     * @return The change
     */
    private static DefaultChange getChange(Event incomingEvent) {
        DefaultChange change = new DefaultChange();
        change.setType(Type.DELETE);
        change.setDeleted(incomingEvent);
        return change;
    }

}
