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
import static com.openexchange.chronos.common.CalendarUtils.isResourceOrRoom;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedParameterValue;
import static com.openexchange.chronos.scheduling.analyzers.Utils.selectAttendee;
import java.util.ArrayList;
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
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ReplyAnalyzer} - Analyzer for the iTIP method <code>REPLY</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.3">RFC 5546 Section 3.2.3</a>
 */
public class ReplyAnalyzer extends AbstractSchedulingAnalyzer {

    private final static Logger LOG = LoggerFactory.getLogger(ReplyAnalyzer.class);

    /**
     * Initializes a new {@link ReplyAnalyzer}.
     * 
     * @param services The services
     */
    public ReplyAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.REPLY);
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        boolean usesOrganizerCopy = objectResourceProvider.usesOrganizerCopy();
        for (Event patchedEvent : objectResourceProvider.getIncomingEvents()) {
            Event storedEvent = objectResourceProvider.optMatchingEvent(patchedEvent);
            Attendee replyingAttendee = selectAttendee(patchedEvent, originator);
            if (null == replyingAttendee) {
                LOG.debug("No replying attendee for originator {} found in incoming event [uid={}, recurrenceId={}], skipping.", 
                    originator, patchedEvent.getUid(), patchedEvent.getRecurrenceId());
                session.addWarning(CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(originator, patchedEvent.getRecurrenceId()));
            } else if (null == storedEvent) {
                Event eventTombstone = objectResourceProvider.optMatchingTombstone(patchedEvent);
                analyzedChanges.add(analyzeUnknownEvent(session, patchedEvent, eventTombstone, replyingAttendee, targetUser));
            } else {
                analyzedChanges.add(analyzeKnownEvent(session, patchedEvent, storedEvent, replyingAttendee, targetUser, usesOrganizerCopy));
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
     * @param replyingAttendee The replying attendee as originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param usesOrganizerCopy <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>,
     *            if the scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @return The analyzed change
     */
    private AnalyzedChange analyzeKnownEvent(CalendarSession session, Event event, Event storedEvent, Attendee replyingAttendee, int targetUser, boolean usesOrganizerCopy) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation(s) for the incoming message as such, and the designated change
         */
        change.addAnnotations(getIntroductions(session, event, storedEvent, replyingAttendee));
        change.setChange(getChange(event));
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        if (storedEvent.getSequence() > event.getSequence()) {
            /*
             * event has been updated in the meantime, say so
             */
            change.addAnnotation(annotationHelper.getOutdatedReplyHint());
            change.addActions(ITipAction.IGNORE);
            return change;
        }
        Attendee storedAttendee = find(storedEvent.getAttendees(), replyingAttendee);
        if (null == storedAttendee) {
            /*
             * previously uninvited attendee, add corresponding delegation or party-crasher hint
             */
            String delegatedFrom = optExtendedParameterValue(replyingAttendee.getExtendedParameters(), "DELEGATED-FROM");
            Attendee delegator = find(storedEvent.getAttendees(), delegatedFrom);
            change.addAnnotation(null == delegator ? annotationHelper.getReplyFromUninvitedHint() : annotationHelper.getReplyFromDelegateHint(delegator));
            change.addAnnotation(annotationHelper.getApplyReplyManuallyHint(targetUser));
            change.addActions(ITipAction.IGNORE, ITipAction.ACCEPT_PARTY_CRASHER);
        } else if (event.getSequence() <= storedEvent.getSequence() && ITipSequence.of(storedAttendee).getDtStamp() > ITipSequence.of(event).getDtStamp()) {
            /*
             * updated reply was received in the meantime, say so 
             */
            change.addAnnotation(annotationHelper.getReplyUpdatedHint());
            change.addActions(ITipAction.IGNORE);
        } else if (event.getSequence() == storedEvent.getSequence() && ITipSequence.of(storedAttendee).getDtStamp() == ITipSequence.of(event).getDtStamp()) {
            /*
             * incoming attendee matches the attendee in stored event copy, say so
             */
            if (false == usesOrganizerCopy) {
                change.addAnnotation(annotationHelper.getReplyAppliedHint(targetUser));
            }
        } else if (ITipSequence.of(event).after(ITipSequence.of(storedAttendee))) {
            /*
             * technically applicable, but not yet applied, say so & add corresponding apply options
             */
            change.addAnnotation(annotationHelper.getApplyReplyManuallyHint(targetUser));
            change.addActions(ITipAction.APPLY_RESPONSE);
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
     * @param replyingAttendee The replying attendee as originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, Event tombstoneEvent, Attendee replyingAttendee, int targetUser) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation for the incoming message as such
         */
        change.addAnnotations(getIntroductions(session, event, tombstoneEvent, replyingAttendee));
        change.setChange(getChange(event));
        change.addActions(ITipAction.IGNORE);
        if (null != tombstoneEvent && ITipSequence.of(tombstoneEvent).after(ITipSequence.of(event))) {
            /*
             * event has been deleted in the meantime, say so
             */
            change.addAnnotation(annotationHelper.getReplyForDeletedHint());
        } else {
            /*
             * reply to unknown event, say so
             */
            change.addAnnotation(annotationHelper.getReplyForUnknownHint(targetUser));
        }
        return change;
    }

    /**
     * Gets the introductional annotation describing the event included in an incoming {@link SchedulingMethod#REPLY} from the originator.
     * 
     * @param session The calendar session
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param replyingAttendee The replying attendee as originator of the scheduling message
     * @return The introductional annotation(s)
     */
    private List<ITipAnnotation> getIntroductions(CalendarSession session, Event event, Event storedEvent, Attendee replyingAttendee) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>(2);
        if (isInternal(replyingAttendee) && isResourceOrRoom(replyingAttendee) && null != replyingAttendee.getSentBy()) {
            annotations.add(annotationHelper.getResourceRepliedIntroduction(event, storedEvent, replyingAttendee));
        } else {
            annotations.add(annotationHelper.getRepliedIntroduction(event, storedEvent, replyingAttendee));
        }
        if (Strings.isNotEmpty(replyingAttendee.getComment())) {
            annotations.add(annotationHelper.getReplyCommentHint(replyingAttendee.getComment()));
        }
        return annotations;
    }
    
    /**
     * Constructs a minimal {@link Change} for an event from a {@link SchedulingMethod#REPLY} message.
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
