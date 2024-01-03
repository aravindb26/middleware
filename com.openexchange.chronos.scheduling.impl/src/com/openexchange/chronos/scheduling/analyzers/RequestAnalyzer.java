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

import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isSimilarICloudIMipMeCom;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedParameterValue;
import static com.openexchange.chronos.scheduling.analyzers.Utils.matchesITipRevision;
import static com.openexchange.chronos.scheduling.analyzers.Utils.optComment;
import static com.openexchange.chronos.scheduling.analyzers.Utils.optResourceSentBy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import com.openexchange.chronos.scheduling.changes.ChangeAction;
import com.openexchange.chronos.scheduling.common.DefaultChange;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.server.ServiceLookup;

/**
 * {@link RequestAnalyzer} - Analyzer for the iTIP method <code>REQUEST</code>
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.2">RFC 5546 Section 3.2.2</a>
 */
public class RequestAnalyzer extends AbstractSchedulingAnalyzer {

    private final static Logger LOG = LoggerFactory.getLogger(RequestAnalyzer.class);

    /**
     * Initializes a new {@link RequestAnalyzer}.
     * 
     * @param services The services
     */
    public RequestAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.REQUEST);
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        Optional<ChangeAction> changeAction = Optional.ofNullable(objectResourceProvider.optItipData().isPresent() ? objectResourceProvider.optItipData().get().getAction() : null);
        boolean usesOrganizerCopy = objectResourceProvider.usesOrganizerCopy();
        for (Event event : objectResourceProvider.getIncomingEvents()) {
            Event storedEvent = objectResourceProvider.optMatchingEvent(event);
            if (null == storedEvent) {
                Event eventTombstone = objectResourceProvider.optMatchingTombstone(event);
                analyzedChanges.add(analyzeUnknownEvent(session, event, eventTombstone, originator, targetUser, changeAction, usesOrganizerCopy));
            } else {
                analyzedChanges.add(analyzeKnownEvent(session, event, storedEvent, originator, targetUser, changeAction, usesOrganizerCopy));
            }
        }
        return analyzedChanges;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#REQUEST} from the originator, where no
     * corresponding stored event (occurrence) exists for yet.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param tombstoneEvent The corresponding event tombstone or occurrence, or <code>null</code> if there is none
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param changeAction The optional change action hinting to whether a creation or update was received, if available
     * @param usesOrganizerCopy <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>,
     *            if the scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, Event tombstoneEvent, CalendarUser originator, int targetUser, Optional<ChangeAction> changeAction, boolean usesOrganizerCopy) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        if (usesOrganizerCopy || null != tombstoneEvent && ITipSequence.of(tombstoneEvent).after(ITipSequence.of(event))) {
            /*
             * (internal) event has been deleted in the meantime, say so
             */
            change.addAnnotations(getIntroductions(session, event, tombstoneEvent, originator, targetUser, changeAction));
            change.addAnnotation(annotationHelper.getDeletedHint());
            change.setChange(getChange(session, event, null, Type.CREATE, targetUser, false));
            change.addActions(ITipAction.IGNORE);
        } else {
            Attendee resourceSentBy = optResourceSentBy(event, originator);
            if (null != resourceSentBy) {
                /*
                 * don't offer "apply" action on foreign mails sent to resource booking delegates
                 */
                change.addAnnotations(getIntroductions(session, event, null, originator, targetUser, changeAction));
                change.addAnnotation(annotationHelper.getResourceNotDelegateHint(resourceSentBy));
                change.setChange(getChange(session, event, null, Type.CREATE, targetUser, false));
                change.setTargetedAttendee(resourceSentBy);
                change.addActions(ITipAction.IGNORE);
            } else {
                /*
                 * add introduction about the incoming message and a "save manually" hint, as well as the corresponding actions
                 */
                change.addAnnotations(getIntroductions(session, event, null, originator, targetUser, changeAction));
                change.addAnnotation(annotationHelper.getSaveManuallyHint(targetUser));
                change.addActions(ITipAction.APPLY_CREATE);
                change.setChange(getChange(session, event, null, Type.CREATE, targetUser, true));
                change.setTargetedAttendee(find(event.getAttendees(), targetUser));
                /*
                 * add hint about the current participation status and add actions to change it (with or w/o conflicts)
                 */
                addPartStatHintAndActions(annotationHelper, change, event, targetUser);
            }
        }
        return change;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#REQUEST} from the originator, where a
     * corresponding stored event (occurrence) exists for.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event or occurrence
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param changeAction The optional change action hinting to whether a creation or update was received, if available
     * @param usesOrganizerCopy <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>,
     *            if the scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @return The analyzed change
     */
    private AnalyzedChange analyzeKnownEvent(CalendarSession session, Event event, Event storedEvent, CalendarUser originator, int targetUser, Optional<ChangeAction> changeAction, boolean usesOrganizerCopy) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation for the incoming message as such
         */
        change.addAnnotations(getIntroductions(session, event, storedEvent, originator, targetUser, changeAction));
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        if (matchesITipRevision(event, storedEvent)) {
            /*
             * incoming event matches the stored event copy, say so
             */
            Type changeType = getChangeType(event, changeAction);
            Attendee resourceSentBy = optResourceSentBy(event, originator);
            if (null != resourceSentBy) {
                change.setChange(getChange(session, event, storedEvent, changeType, resourceSentBy));
                change.setTargetedAttendee(resourceSentBy);
                if (false == usesOrganizerCopy) {
                    change.addAnnotation(Type.CREATE.equals(changeType) ? 
                        annotationHelper.getResourceSavedHint(resourceSentBy.getEntity()) : annotationHelper.getResourceUpdatedHint(resourceSentBy.getEntity()));
                }
                if (isInternal(resourceSentBy) && session.getEntityResolver().getSchedulingPrivilege(
                    resourceSentBy.getEntity(), session.getUserId()).implies(SchedulingPrivilege.DELEGATE)) {
                    /*
                     * add hint about the current participation status and add actions to change it (with or w/o conflicts)
                     */
                    addResourcePartStatHintAndActions(annotationHelper, change, storedEvent, resourceSentBy.getEntity());
                } else {
                    /*
                     * add hint that user is not allowed to handle booking requests for this resource
                     */
                    change.addAnnotation(annotationHelper.getResourceNotDelegateHint(resourceSentBy));
                    change.addActions(ITipAction.IGNORE);
                }
            } else {
                change.setChange(getChange(session, event, storedEvent, changeType, targetUser, true));
                change.setTargetedAttendee(find(event.getAttendees(), targetUser));
                if (false == usesOrganizerCopy) {
                    change.addAnnotation(Type.CREATE.equals(changeType) ? annotationHelper.getSavedHint(targetUser) : annotationHelper.getUpdatedHint(targetUser)); 
                }
                /*
                 * add hint about the current participation status and add actions to change it (with or w/o conflicts)
                 */
                addPartStatHintAndActions(annotationHelper, change, storedEvent, targetUser);
            }
        } else if (ITipSequence.of(storedEvent).after(ITipSequence.of(event))) {
            /*
             * event has been updated in the meantime, say so
             */
            change.addAnnotation(annotationHelper.getOutdatedHint());
            change.setChange(getChange(session, event, storedEvent, Type.UPDATE, targetUser, false));
            change.addActions(ITipAction.IGNORE);
        } else if (ITipSequence.of(storedEvent).before(ITipSequence.of(event))) {
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
            change.addAnnotation(annotationHelper.getUpdateManuallyHint(targetUser));
            change.setChange(getChange(session, event, storedEvent, Type.UPDATE, targetUser, true));
            change.setTargetedAttendee(find(event.getAttendees(), targetUser));
            change.addActions(ITipAction.APPLY_CHANGE);
            /*
             * add hint about the current participation status and add actions to change it (with or w/o conflicts)
             */
            addPartStatHintAndActions(annotationHelper, change, event, targetUser);
        } else {
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Illegal sequence/dtstamp in events");
        }
        return change;
    }

    /**
     * Gets the type to be used for the iTIP change.
     * 
     * @param event The (patched) event from the incoming scheduling object resource
     * @param changeAction The optional change action hinting to whether a creation or update was received, if available
     * @return The change type, either {@link Type#UPDATE} or {@link Type#CREATE}
     */
    private static Type getChangeType(Event event, Optional<ChangeAction> changeAction) {
        if (changeAction.isPresent()) {
            if (ChangeAction.CREATE.equals(changeAction.get())) {
                return Type.CREATE;
            }
            if (ChangeAction.UPDATE.equals(changeAction.get())) {
                return Type.UPDATE;
            }
        }
        return 0 == event.getSequence() ? Type.CREATE : Type.UPDATE;
    }

    /**
     * Gets the introductional annotation(s) describing the event included in an incoming {@link SchedulingMethod#REQUEST} from the
     * originator.
     * 
     * @param session The calendar session
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @param changeAction The optional change action hinting to whether a creation or update was received, if available
     * @return The introductional annotations
     */
    private List<ITipAnnotation> getIntroductions(CalendarSession session, Event event, Event storedEvent, CalendarUser originator, int targetUser, Optional<ChangeAction> changeAction) throws OXException {
        List<ITipAnnotation> annotations = new ArrayList<>(2);
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        ChangeAction action = changeAction.orElse(null == storedEvent || 0 == event.getSequence() ? ChangeAction.CREATE : ChangeAction.UPDATE);
        if (ChangeAction.CREATE.equals(action)) {
            Attendee delegator = optDelegator(event, targetUser);
            Attendee resourceSentBy = optResourceSentBy(event, originator);
            if (null != delegator) {
                /*
                 * new delegated appointment, use "delegated" annotations
                 */
                annotations.add(annotationHelper.getDelegatedIntroduction(event, originator, delegator, targetUser));
            } else if (null != resourceSentBy) {
                /*
                 * new booking request for resource (presumably) managed by user, use "resource added" annotations
                 */
                annotations.add(annotationHelper.getResourceAddedIntroduction(event, originator, resourceSentBy));
            } else if (false == contains(event.getAttendees(), targetUser)) {
                /*
                 * new appointment, but not invited, use "forwarded" annotations
                 */
                annotations.add(annotationHelper.getForwardedIntroduction(event, originator, targetUser));
            } else {
                /*
                 * new appointment (from targeted user's point of view), use "invited to" or "forwarded" annotations
                 */
                annotations.add(annotationHelper.getInvitedIntroduction(event, originator, targetUser));
            }
        } else {
            Attendee resourceSentBy = optResourceSentBy(event, originator);
            if (null != resourceSentBy) {
                /*
                 * update of booking request for resource (presumably) managed by user, use "resource changed" annotations
                 */
                annotations.add(annotationHelper.getResourceChangedIntroduction(event, originator, resourceSentBy));
            } else {
                /*
                 * update of existing appointment (from targeted user's point of view), use "has updated" annotations
                 */
                annotations.add(annotationHelper.getChangedIntroduction(event, originator));
            }
        }
        optComment(event).ifPresent((c) -> annotations.add(annotationHelper.getRequestCommentHint(c)));
        return annotations;
    }

    private static Attendee optDelegator(Event event, int targetUser) {
        Attendee calendarUserAttendee = find(event.getAttendees(), targetUser);
        if (null != calendarUserAttendee) {
            String delegatedFrom = optExtendedParameterValue(calendarUserAttendee.getExtendedParameters(), "DELEGATED-FROM");
            return find(event.getAttendees(), delegatedFrom);
        }
        return null;
    }

    /**
     * Constructs a minimal {@link Change} for an event from a {@link SchedulingMethod#REQUEST} message.
     * 
     * @param session The calendar session
     * @param incomingEvent The (patched) event from the incoming message to get the change for
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param type The type of change, either {@link Type#UPDATE} or {@link Type#CREATE}
     * @param targetUser The user id of the scheduling message's recipient
     * @param checkConflicts <code>true</code> to check for conflicts in the targeted user's calendar, <code>false</code> otherwise
     * @return The change
     */
    private static DefaultChange getChange(CalendarSession session, Event incomingEvent, Event storedEvent, Type type, int targetUser, boolean checkConflicts) {
        Attendee attendee = null;
        if (checkConflicts) {
            try {
                attendee = session.getEntityResolver().prepareUserAttendee(targetUser);
            } catch (OXException e) {
                LOG.warn("Unexpected error getting attendee for incoming event from REQUEST message", e);
                session.addWarning(e);
            }
        }
        return getChange(session, incomingEvent, storedEvent, type, attendee);
    }

    /**
     * Constructs a minimal {@link Change} for an event from a {@link SchedulingMethod#REQUEST} message.
     * 
     * @param session The calendar session
     * @param incomingEvent The (patched) event from the incoming message to get the change for
     * @param storedEvent The corresponding currently stored event or occurrence, or <code>null</code> if not applicable
     * @param type The type of change, either {@link Type#UPDATE} or {@link Type#CREATE}
     * @param targetedAttendee The attendee representing the scheduling message's recipient to use for conflict checks, or <code>null</code> to not check for conflicts
     * @return The change
     */
    private static DefaultChange getChange(CalendarSession session, Event incomingEvent, Event storedEvent, Type type, Attendee targetedAttendee) {
        DefaultChange change = new DefaultChange();
        change.setType(type);
        change.setNewEvent(incomingEvent);
        change.setCurrentEvent(storedEvent);
        if (null != targetedAttendee) {
            try {
                change.setConflicts(session.getFreeBusyService().checkForConflicts(session, incomingEvent, Collections.singletonList(targetedAttendee)));
            } catch (OXException e) {
                LOG.warn("Unexpected error checking for conflicts for incoming event from REQUEST message", e);
                session.addWarning(e);
            }
        }
        return change;
    }

    /**
     * Adds a hint about the current participation status and actions to change it (with or w/o conflicts) to the passed analyzed change.
     * 
     * @param annotationHelper The annotation helper to use
     * @param change The analyzed change to populate with the actions and annotation
     * @param event The incoming event in question
     * @param targetUser The user id of the scheduling message's recipient
     */
    private static void addPartStatHintAndActions(AnnotationHelper annotationHelper, AnalyzedChange change, Event event, int targetUser) throws OXException {
        change.addAnnotations(annotationHelper.getPartStatDescriptions(event, targetUser));
        if (com.openexchange.tools.arrays.Collections.isNotEmpty(change.getChange().getConflicts())) {
            change.addAnnotation(annotationHelper.getConflictsHint(targetUser));
            change.addActions(ITipAction.DECLINE, ITipAction.TENTATIVE, ITipAction.ACCEPT_AND_IGNORE_CONFLICTS);
        } else {
            change.addActions(ITipAction.DECLINE, ITipAction.TENTATIVE, ITipAction.ACCEPT);
        }
    }

    /**
     * Adds a hint about the current participation status and actions to change it (with or w/o conflicts) to the passed analyzed change.
     * 
     * @param annotationHelper The annotation helper to use
     * @param change The analyzed change to populate with the actions and annotation
     * @param event The incoming event in question
     * @param resourceId The id of the resource targeted by the scheduling message
     */
    private static void addResourcePartStatHintAndActions(AnnotationHelper annotationHelper, AnalyzedChange change, Event event, int resourceId) throws OXException {
        change.addAnnotations(annotationHelper.getResourcePartStatDescriptions(event, resourceId));
        if (com.openexchange.tools.arrays.Collections.isNotEmpty(change.getChange().getConflicts())) {
            change.addAnnotation(annotationHelper.getResourceConflictsHint(resourceId));
            change.addActions(ITipAction.DECLINE, ITipAction.TENTATIVE, ITipAction.ACCEPT_AND_IGNORE_CONFLICTS);
        } else {
            change.addActions(ITipAction.DECLINE, ITipAction.TENTATIVE, ITipAction.ACCEPT);
        }
    }

}
