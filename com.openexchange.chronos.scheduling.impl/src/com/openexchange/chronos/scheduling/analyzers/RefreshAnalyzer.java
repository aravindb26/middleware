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
import static com.openexchange.chronos.scheduling.analyzers.Utils.optComment;
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
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.analyzers.annotations.AnnotationHelper;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link RefreshAnalyzer} - Analyzer for the iTIP method <code>REFREHSH</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.6">RFC 5546 Section 3.2.6</a>
 */
public class RefreshAnalyzer extends AbstractSchedulingAnalyzer {

    private final static Logger LOG = LoggerFactory.getLogger(RefreshAnalyzer.class);

    /**
     * Initializes a new {@link RefreshAnalyzer}.
     * 
     * @param services The services
     */
    public RefreshAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.REFRESH);
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        List<AnalyzedChange> analyzedChanges = new ArrayList<AnalyzedChange>();
        for (Event patchedEvent : objectResourceProvider.getIncomingEvents()) {
            Event storedEvent = objectResourceProvider.optMatchingEvent(patchedEvent);
            Attendee refreshingAttendee = selectAttendee(patchedEvent, originator);
            if (null == refreshingAttendee) {
                LOG.debug("No refreshing attendee for originator {} found in incoming event [uid={}, recurrenceId={}], skipping.", 
                    originator, patchedEvent.getUid(), patchedEvent.getRecurrenceId());
                session.addWarning(CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(originator, patchedEvent.getRecurrenceId()));
            } else if (null == storedEvent) {
                analyzedChanges.add(analyzeUnknownEvent(session, patchedEvent, refreshingAttendee, targetUser));
            } else {
                analyzedChanges.add(analyzeKnownEvent(session, patchedEvent, storedEvent, refreshingAttendee));
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
     * @param refreshingAttendee The replying attendee as originator of the scheduling message
     * @return The analyzed change
     */
    private AnalyzedChange analyzeKnownEvent(CalendarSession session, Event event, Event storedEvent, Attendee refreshingAttendee) {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation(s) for the incoming message as such
         */
        change.addAnnotations(getIntroductions(session, event, refreshingAttendee));
        /*
         * add further annotation(s) based on the current state of the scheduling object resource & derive changes
         */
        Attendee storedAttendee = find(storedEvent.getAttendees(), refreshingAttendee);
        if (null == storedAttendee) {
            /*
             * uninvited attendee, add corresponding hint & only offer to ignore
             */
            change.addAnnotation(annotationHelper.getRefreshFromUninvitedHint());
            change.addActions(ITipAction.IGNORE);
        } else {
            /*
             * refresh for known event
             */
            change.addAnnotation(annotationHelper.getSendManuallyHint(storedEvent));
            change.addActions(ITipAction.SEND_REFRESH, ITipAction.IGNORE);
        }
        return change;
    }

    /**
     * Analyzes the changes for a single event as part of an incoming {@link SchedulingMethod#REPLY} from the originator, where a
     * corresponding stored event (occurrence) does not or no longer exist for.
     * 
     * @param session The underlying calendar session
     * @param event The (patched) event from the incoming scheduling object resource
     * @param replyingAttendee The replying attendee as originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The analyzed change
     */
    private AnalyzedChange analyzeUnknownEvent(CalendarSession session, Event event, Attendee replyingAttendee, int targetUser) throws OXException {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        AnalyzedChange change = new AnalyzedChange();
        /*
         * add introductional annotation for the incoming message as such
         */
        change.addAnnotations(getIntroductions(session, event, replyingAttendee));
        /*
         * refresh for an unknown event, say so
         */
        annotationHelper.getRefreshForUnknownHint(targetUser);
        change.addActions(ITipAction.IGNORE);
        return change;
    }

    /**
     * Gets the introductory annotation describing the event included in an incoming {@link SchedulingMethod#REFRESH} from the originator.
     * 
     * @param session The calendar session
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param refreshingAttendee The refreshing attendee as originator of the scheduling message
     * @return The introductory annotation(s)
     */
    private List<ITipAnnotation> getIntroductions(CalendarSession session, Event event, Attendee refreshingAttendee) {
        AnnotationHelper annotationHelper = new AnnotationHelper(services, session);
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>(2);
        annotations.add(annotationHelper.getRefreshIntroduction(refreshingAttendee));
        optComment(event).ifPresent((c) -> annotations.add(annotationHelper.getCancelCommentHint(c)));
        return annotations;
    }

}
