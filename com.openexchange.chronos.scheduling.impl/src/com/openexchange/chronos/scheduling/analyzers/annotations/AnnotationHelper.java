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

package com.openexchange.chronos.scheduling.analyzers.annotations;

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getDisplayName;
import static com.openexchange.chronos.common.CalendarUtils.getResourceDisplayName;
import static com.openexchange.chronos.scheduling.analyzers.Utils.getLocale;
import static com.openexchange.chronos.scheduling.analyzers.Utils.getSummary;
import static com.openexchange.chronos.scheduling.analyzers.Utils.getTimeZone;
import static com.openexchange.chronos.scheduling.analyzers.annotations.AddAnnotations.ADDED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.AddAnnotations.ADDED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.AddAnnotations.ADD_UNSUPPORTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.AddAnnotations.ADD_UNSUPPORTED_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.AddAnnotations.REQUEST_REFRESH_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCELED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCELED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCELED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCELED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCELED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCELED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCEL_APPLIED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCEL_APPLIED_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCEL_APPLY_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.CANCEL_APPLY_MANUALLY_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.RESOURCE_CANCELED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.RESOURCE_CANCELED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.RESOURCE_CANCELED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.RESOURCE_CANCELED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.RESOURCE_CANCELED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CancelAnnotations.RESOURCE_CANCELED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.CHANGES_PROPOSED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.CHANGES_PROPOSED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.CHANGES_PROPOSED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.CHANGES_PROPOSED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.CHANGES_PROPOSED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.CHANGES_PROPOSED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.COUNTER_APPLY_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.COUNTER_APPLY_MANUALLY_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.COUNTER_OUTDATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.COUNTER_UNINVITED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.COUNTER_UNSUPPORTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.COUNTER_UNSUPPORTED_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.CounterAnnotations.TIME_PROPOSED_TIMES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.DeclineCounterAnnotations.COUNTER_DECLINED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.DeclineCounterAnnotations.COUNTER_DECLINED_FOR_UPDATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.DeclineCounterAnnotations.COUNTER_DECLINED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.DeclineCounterAnnotations.COUNTER_DECLINED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.DeclineCounterAnnotations.COUNTER_DECLINED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.DeclineCounterAnnotations.COUNTER_DECLINED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.COMMENT_LEFT;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.CONFLICTS;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.CONFLICTS_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.DELETED_MEANTIME;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.NOT_ATTENDING;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.NOT_FOUND;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.NOT_FOUND_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.NOT_REPLIED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.PARTICIPATION_OPTIONAL;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.REPLIED_WITH_ACCEPTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.REPLIED_WITH_DECLINED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.REPLIED_WITH_TENTATIVE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.RESOURCE_NOT_DELEGATE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.RESOURCE_NOT_REPLIED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.RESOURCE_REPLIED_WITH_ACCEPTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.RESOURCE_REPLIED_WITH_DECLINED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.RESOURCE_REPLIED_WITH_TENTATIVE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.UNALLOWED_ORGANIZER_CHANGE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.UPDATED_MEANTIME;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.USER_NOT_ATTENDING;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.USER_NOT_REPLIED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.USER_REPLIED_WITH_ACCEPTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.USER_REPLIED_WITH_DECLINED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.GenericAnnotations.USER_REPLIED_WITH_TENTATIVE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RefreshAnnotations.REFRESH;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RefreshAnnotations.REFRESH_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RefreshAnnotations.REFRESH_UNINVITED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RefreshAnnotations.SEND_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_ACCEPTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_ACCEPTED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_ACCEPTED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_ACCEPTED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_ACCEPTED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_ACCEPTED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_DECLINED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_DECLINED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_DECLINED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_DECLINED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_DECLINED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_DECLINED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_NONE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_NONE_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_NONE_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_NONE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_NONE_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_NONE_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_TENTATIVE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_TENTATIVE_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_TENTATIVE_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_TENTATIVE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_TENTATIVE_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLIED_TENTATIVE_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_APPLIED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_APPLIED_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_APPLY_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_APPLY_MANUALLY_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_DELEGATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_OUTDATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_UNINVITED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.REPLY_UPDATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_ACCEPTED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_ACCEPTED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_ACCEPTED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_DECLINED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_DECLINED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_DECLINED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_NONE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_NONE_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_NONE_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_TENTATIVE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_TENTATIVE_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.ReplyAnnotations.RESOURCE_REPLIED_TENTATIVE_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.CHANGED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.CHANGED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.CHANGED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.CHANGED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.CHANGED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.CHANGED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.DELEGATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.DELEGATED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.DELEGATED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.FORWARDED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.FORWARDED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.FORWARDED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.FORWARDED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.FORWARDED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.FORWARDED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.INVITED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.INVITED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.INVITED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.INVITED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.INVITED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.INVITED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.NO_SAVE_PERMISSIONS_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.NO_UPDATE_PERMISSIONS_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_CHANGED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_CHANGED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_CHANGED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_CHANGED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_CHANGED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_CHANGED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_INVITED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_INVITED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_INVITED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_INVITED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_INVITED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.RESOURCE_INVITED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.SAVED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.SAVED_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.SAVE_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.SAVE_MANUALLY_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.UPDATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.UPDATED_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.UPDATE_MANUALLY;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.UPDATE_MANUALLY_IN;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_DELEGATED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_DELEGATED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_DELEGATED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_FORWARDED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_FORWARDED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_FORWARDED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_FORWARDED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_FORWARDED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_FORWARDED_SERIES_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_INVITED;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_INVITED_OCCURRENCE;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_INVITED_OCCURRENCE_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_INVITED_ON_BEHALF;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_INVITED_SERIES;
import static com.openexchange.chronos.scheduling.analyzers.annotations.RequestAnnotations.USER_INVITED_SERIES_ON_BEHALF;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipantRole;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.chronos.scheduling.common.ITipAnnotationBuilder;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.regional.RegionalSettings;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.regional.RegionalSettingsUtil;
import com.openexchange.server.ServiceLookup;

/**
 * {@link AnnotationHelper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class AnnotationHelper {

    private final CalendarSession session;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link AnnotationHelper}.
     * 
     * @param services The services
     * @param session The calendar session
     */
    public AnnotationHelper(ServiceLookup services, CalendarSession session) {
        super();
        this.services = services;
        this.session = session;
    }

    /**
     * Gets the generic "organizer has changed" informational hint.
     * 
     * @return The organizer changed hint
     */
    public ITipAnnotation getOrganizerChangedHint() {
        return buildAnnotation(session, UNALLOWED_ORGANIZER_CHANGE);
    }

    /**
     * Gets the descriptions for the calendar user's participation status in the event.
     * 
     * @param event The event to get the participation status descriptions for
     * @param targetUser The targeted calendar user
     * @return The descriptions
     * @throws OXException In case annotation can't be created
     */
    public List<ITipAnnotation> getPartStatDescriptions(Event event, int targetUser) throws OXException {
        Attendee attendee = find(event.getAttendees(), targetUser);
        if (null == attendee) {
            return Collections.singletonList(targetUser == session.getUserId() ? buildAnnotation(session, NOT_ATTENDING) : 
                buildAnnotation(session, USER_NOT_ATTENDING, getDisplayName(session, targetUser)));
        }
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>(2);
        if (ParticipantRole.OPT_PARTICIPANT.matches(attendee.getRole())) {
            annotations.add(buildAnnotation(session, PARTICIPATION_OPTIONAL));
        }
        ParticipationStatus partStat = attendee.getPartStat();
        Map<String, Object> additionals = Collections.singletonMap("partStat", (null != partStat ? partStat : ParticipationStatus.NEEDS_ACTION).getValue()); 
        if (ParticipationStatus.ACCEPTED.matches(partStat)) {
            annotations.add(targetUser == session.getUserId() ? buildAnnotation(session, REPLIED_WITH_ACCEPTED, additionals) : 
                buildAnnotation(session, USER_REPLIED_WITH_ACCEPTED, additionals, getDisplayName(session, targetUser)));
        } else if (ParticipationStatus.TENTATIVE.matches(partStat)) {
            annotations.add(targetUser == session.getUserId() ? buildAnnotation(session, REPLIED_WITH_TENTATIVE, additionals) : 
                buildAnnotation(session, USER_REPLIED_WITH_TENTATIVE, additionals, getDisplayName(session, targetUser)));
        } else if (ParticipationStatus.DECLINED.matches(partStat)) {
            annotations.add(targetUser == session.getUserId() ? buildAnnotation(session, REPLIED_WITH_DECLINED, additionals) : 
                buildAnnotation(session, USER_REPLIED_WITH_DECLINED, additionals, getDisplayName(session, targetUser)));
        } else {
            annotations.add(targetUser == session.getUserId() ? buildAnnotation(session, NOT_REPLIED, additionals) : 
                buildAnnotation(session, USER_NOT_REPLIED, additionals, getDisplayName(session, targetUser)));
        }
        return annotations;
    }

    /**
     * Gets the descriptions for a resource calendar user's participation status in the event.
     * 
     * @param event The event to get the participation status descriptions for
     * @param resourceId The id of the resource targeted by the scheduling message
     * @return The descriptions
     * @throws OXException In case annotation can't be created
     */
    public List<ITipAnnotation> getResourcePartStatDescriptions(Event event, int resourceId) throws OXException {
        Attendee attendee = find(event.getAttendees(), resourceId);
        if (null == attendee) {
            return Collections.singletonList(buildAnnotation(session, USER_NOT_ATTENDING, getResourceDisplayName(session, resourceId)));
        }
        List<ITipAnnotation> annotations = new ArrayList<ITipAnnotation>(2);
        if (ParticipantRole.OPT_PARTICIPANT.matches(attendee.getRole())) {
            annotations.add(buildAnnotation(session, PARTICIPATION_OPTIONAL));
        }
        ParticipationStatus partStat = attendee.getPartStat();
        Map<String, Object> additionals = Collections.singletonMap("partStat", (null != partStat ? partStat : ParticipationStatus.NEEDS_ACTION).getValue());
        if (ParticipationStatus.ACCEPTED.matches(partStat)) {
            annotations.add(buildAnnotation(session, RESOURCE_REPLIED_WITH_ACCEPTED, additionals, getResourceDisplayName(session, resourceId)));
        } else if (ParticipationStatus.TENTATIVE.matches(partStat)) {
            annotations.add(buildAnnotation(session, RESOURCE_REPLIED_WITH_TENTATIVE, additionals, getResourceDisplayName(session, resourceId)));
        } else if (ParticipationStatus.DECLINED.matches(partStat)) {
            annotations.add(buildAnnotation(session, RESOURCE_REPLIED_WITH_DECLINED, additionals, getResourceDisplayName(session, resourceId)));
        } else {
            annotations.add(buildAnnotation(session, RESOURCE_NOT_REPLIED, additionals, getResourceDisplayName(session, resourceId)));
        }
        return annotations;
    }

    /**
     * Constructs the hint that another appointment conflicts with the incoming one in the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getConflictsHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, CONFLICTS) : 
            buildAnnotation(session, CONFLICTS_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that another appointment conflicts with the incoming one in the resource's calendar.
     * 
     * @param resourceId The id of the resource targeted by the scheduling message
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceConflictsHint(int resourceId) throws OXException {
        return buildAnnotation(session, CONFLICTS_IN, getResourceDisplayName(session, resourceId));
    }

    /**
     * Constructs the hint that an appointment needs to be saved manually to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getSaveManuallyHint(int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, SAVE_MANUALLY);
        }
        return buildAnnotation(session, SAVE_MANUALLY_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that an appointment needs to be updated manually to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getUpdateManuallyHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, UPDATE_MANUALLY) : 
            buildAnnotation(session, UPDATE_MANUALLY_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that an appointment cannot be saved to the user's calendar due to insufficient permissions.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getInsufficientPermissionForSave(int targetUser) throws OXException {
        return buildAnnotation(session, NO_SAVE_PERMISSIONS_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that an appointment cannot be updated in the user's calendar due to insufficient permissions.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getInsufficientPermissionForUpdateHint(int targetUser) throws OXException {
        return buildAnnotation(session, NO_UPDATE_PERMISSIONS_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that the changes were already applied to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getUpdatedHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, UPDATED) : 
            buildAnnotation(session, UPDATED_IN, getDisplayName(session, targetUser));
    }
    
    /**
     * Constructs the hint that the changes were already applied to the resource's calendar.
     * 
     * @param resourceId The id of the resource targeted by the scheduling message
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceUpdatedHint(int resourceId) throws OXException {
        return buildAnnotation(session, UPDATED_IN, getResourceDisplayName(session, resourceId));
    }

    /**
     * Constructs the hint that an invitation was saved to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getSavedHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, SAVED) : 
            buildAnnotation(session, SAVED_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that an invitation was saved to the resource's calendar.
     * 
     * @param resourceId The id of the resource targeted by the scheduling message
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceSavedHint(int resourceId) throws OXException {
        return buildAnnotation(session, SAVED_IN, getResourceDisplayName(session, resourceId));
    }

    /**
     * Constructs the hint that the current user is not allowed to handle booking requests for a certain resource attendee.
     * 
     * @param resource The resource targeted by the scheduling message
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceNotDelegateHint(CalendarUser resource) throws OXException {
        return buildAnnotation(session, RESOURCE_NOT_DELEGATE, getResourceDisplayName(session, resource));
    }

    /**
     * Constructs the hint that an updated invitation has already been received in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getOutdatedHint() {
        return buildAnnotation(session, UPDATED_MEANTIME);
    }

    /**
     * Constructs the hint that the event has been cancelled in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getDeletedHint() {
        return buildAnnotation(session, DELETED_MEANTIME);
    }

    /**
     * Gets the introductional annotation describing that an invitation was delegated to the calendar user from another user.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param delegator The attendee who has been identified as the delegator of the invitation
     * @param targetUser The user id of the scheduling message's recipient
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getDelegatedIntroduction(Event event, CalendarUser originator, CalendarUser delegator, int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? DELEGATED_SERIES : indicatesOccurrence(event) ? DELEGATED_OCCURRENCE : DELEGATED,
                getDisplayName(delegator, event), getSummary(event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? USER_DELEGATED_SERIES : indicatesOccurrence(event) ? USER_DELEGATED_OCCURRENCE : USER_DELEGATED,
            getDisplayName(delegator, event), getSummary(event), getDisplayName(originator, event), getDisplayName(session, targetUser));
    }

    /**
     * Gets the introductional annotation describing that an invitation for a new appointment was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getInvitedIntroduction(Event event, CalendarUser originator, int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            if (null != originator.getSentBy()) {
                return buildAnnotation(session, 
                    indicatesSeries(event) ? INVITED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? INVITED_OCCURRENCE_ON_BEHALF : INVITED_ON_BEHALF,
                    getSummary(event), getDisplayName(originator.getSentBy(), event), getDisplayName(originator, event));
            } 
            return buildAnnotation(session, 
                indicatesSeries(event) ? INVITED_SERIES : indicatesOccurrence(event) ? INVITED_OCCURRENCE : INVITED,
                getSummary(event), getDisplayName(originator, event));
        }
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? USER_INVITED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? USER_INVITED_OCCURRENCE_ON_BEHALF : USER_INVITED_ON_BEHALF,
                getDisplayName(session, targetUser), getSummary(event), getDisplayName(originator.getSentBy(), event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? USER_INVITED_SERIES : indicatesOccurrence(event) ? USER_INVITED_OCCURRENCE : USER_INVITED,
            getDisplayName(session, targetUser), getSummary(event), getDisplayName(originator, event));
    }

    /**
     * Gets the introductional annotation describing that a booking request for a resource attendee was received by the calendar user who
     * acts as booking delegate for the resource.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param resource The resource targeted by the booking request
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceAddedIntroduction(Event event, CalendarUser originator, CalendarUser resource) throws OXException {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? RESOURCE_INVITED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? RESOURCE_INVITED_OCCURRENCE_ON_BEHALF : RESOURCE_INVITED_ON_BEHALF,
                getDisplayName(originator, event), getDisplayName(originator.getSentBy(), event), getResourceDisplayName(session, resource), getSummary(event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? RESOURCE_INVITED_SERIES : indicatesOccurrence(event) ? RESOURCE_INVITED_OCCURRENCE : RESOURCE_INVITED,
            getDisplayName(originator, event), getResourceDisplayName(session, resource), getSummary(event));
    }

    /**
     * Gets the introductional annotation describing that a forwarded invitation for a new appointment was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param targetUser The user id of the scheduling message's recipient
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getForwardedIntroduction(Event event, CalendarUser originator, int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            if (null != originator.getSentBy()) {
                return buildAnnotation(session, 
                    indicatesSeries(event) ? FORWARDED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? FORWARDED_OCCURRENCE_ON_BEHALF : FORWARDED_ON_BEHALF,
                    getSummary(event), getDisplayName(originator.getSentBy(), event), getDisplayName(originator, event));
            } 
            return buildAnnotation(session, 
                indicatesSeries(event) ? FORWARDED_SERIES : indicatesOccurrence(event) ? FORWARDED_OCCURRENCE : FORWARDED,
                getSummary(event), getDisplayName(originator, event));
        } 
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? USER_FORWARDED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? USER_FORWARDED_OCCURRENCE_ON_BEHALF : USER_FORWARDED_ON_BEHALF,
                getDisplayName(session, targetUser), getSummary(event), getDisplayName(originator.getSentBy(), event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? USER_FORWARDED_SERIES : indicatesOccurrence(event) ? USER_FORWARDED_OCCURRENCE : USER_FORWARDED,
            getDisplayName(session, targetUser), getSummary(event), getDisplayName(originator, event));
    }

    /**
     * Gets the introductional annotation describing that an updated invitation for an existing event was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @return The introductional annotation
     */
    public ITipAnnotation getChangedIntroduction(Event event, CalendarUser originator) {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? CHANGED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? CHANGED_OCCURRENCE_ON_BEHALF : CHANGED_ON_BEHALF,
                getDisplayName(originator.getSentBy(), event), getSummary(event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? CHANGED_SERIES : indicatesOccurrence(event) ? CHANGED_OCCURRENCE : CHANGED,
            getDisplayName(originator, event), getSummary(event));
    }

    /**
     * Gets the introductional annotation describing that an updated booking request for a resource attendee was received by the calendar
     * user who acts as booking delegate for the resource.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @param resource The resource targeted by the booking request
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceChangedIntroduction(Event event, CalendarUser originator, CalendarUser resource) throws OXException {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? RESOURCE_CHANGED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? RESOURCE_CHANGED_OCCURRENCE_ON_BEHALF : RESOURCE_CHANGED_ON_BEHALF,
                getDisplayName(originator, event), getSummary(event), getResourceDisplayName(session, resource), getDisplayName(originator.getSentBy(), event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? RESOURCE_CHANGED_SERIES : indicatesOccurrence(event) ? RESOURCE_CHANGED_OCCURRENCE : RESOURCE_CHANGED,
            getSummary(event), getResourceDisplayName(session, resource), getDisplayName(originator, event));
    }

    /**
     * Constructs the hint that the organizer left a comment along with a request.
     * 
     * @param comment The comment
     * @return The annotation
     */
    public ITipAnnotation getRequestCommentHint(String comment) {
        return buildAnnotation(session, COMMENT_LEFT, comment);
    }

    /**
     * Gets the introductional annotation describing that a reply for an existing event was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the reply is targeted at, or <code>null</code> if not available
     * @param replyingAttendee The replying attendee as originator of the scheduling message
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getRepliedIntroduction(Event event, Event storedEvent, Attendee replyingAttendee) {
        String summary = getSummary(storedEvent, event);
        ParticipationStatus partStat = replyingAttendee.getPartStat();
        Map<String, Object> additionals = Collections.singletonMap("partStat", (null != partStat ? partStat : ParticipationStatus.NEEDS_ACTION).getValue());
        if (null != replyingAttendee.getSentBy()) {
            if (ParticipationStatus.ACCEPTED.matches(partStat)) {
                return buildAnnotation(session, 
                    indicatesSeries(event) ? REPLIED_ACCEPTED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? REPLIED_ACCEPTED_OCCURRENCE_ON_BEHALF : REPLIED_ACCEPTED_ON_BEHALF,
                    additionals, getDisplayName(replyingAttendee.getSentBy(), event), summary, getDisplayName(replyingAttendee, event));
            }
            if (ParticipationStatus.TENTATIVE.matches(partStat)) {
                return buildAnnotation(session, 
                    indicatesSeries(event) ? REPLIED_TENTATIVE_SERIES_ON_BEHALF : indicatesOccurrence(event) ? REPLIED_TENTATIVE_OCCURRENCE_ON_BEHALF : REPLIED_TENTATIVE_ON_BEHALF,
                    additionals, getDisplayName(replyingAttendee.getSentBy(), event), summary, getDisplayName(replyingAttendee, event));
            }
            if (ParticipationStatus.DECLINED.matches(partStat)) {
                return buildAnnotation(session, 
                    indicatesSeries(event) ? REPLIED_DECLINED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? REPLIED_DECLINED_OCCURRENCE_ON_BEHALF : REPLIED_DECLINED_ON_BEHALF,
                    additionals, getDisplayName(replyingAttendee.getSentBy(), event), summary, getDisplayName(replyingAttendee, event));
            }
            return buildAnnotation(session, 
                indicatesSeries(event) ? REPLIED_NONE_SERIES_ON_BEHALF : indicatesOccurrence(event) ? REPLIED_NONE_OCCURRENCE_ON_BEHALF : REPLIED_NONE_ON_BEHALF,
                additionals, getDisplayName(replyingAttendee.getSentBy(), event), summary, getDisplayName(replyingAttendee, event));
        }
        if (ParticipationStatus.ACCEPTED.matches(partStat)) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? REPLIED_ACCEPTED_SERIES : indicatesOccurrence(event) ? REPLIED_ACCEPTED_OCCURRENCE : REPLIED_ACCEPTED,
                additionals, getDisplayName(replyingAttendee, event), summary);
        }
        if (ParticipationStatus.TENTATIVE.matches(partStat)) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? REPLIED_TENTATIVE_SERIES : indicatesOccurrence(event) ? REPLIED_TENTATIVE_OCCURRENCE : REPLIED_TENTATIVE,
                additionals, getDisplayName(replyingAttendee, event), summary);
        }
        if (ParticipationStatus.DECLINED.matches(partStat)) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? REPLIED_DECLINED_SERIES : indicatesOccurrence(event) ? REPLIED_DECLINED_OCCURRENCE : REPLIED_DECLINED,
                additionals, getDisplayName(replyingAttendee, event), summary);
        }
        return buildAnnotation(session, 
            indicatesSeries(event) ? REPLIED_NONE_SERIES : indicatesOccurrence(event) ? REPLIED_NONE_OCCURRENCE : REPLIED_NONE,
            additionals, getDisplayName(replyingAttendee, event), summary);
    }

    /**
     * Gets the introductional annotation describing that a reply from a booking delegate of a resource for an existing event was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the reply is targeted at, or <code>null</code> if not available
     * @param replyingAttendee The replying resource attendee including its booking delegate in its <code>SENT-BY</code> property
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceRepliedIntroduction(Event event, Event storedEvent, Attendee replyingAttendee) throws OXException {
        String summary = getSummary(storedEvent, event);
        ParticipationStatus partStat = replyingAttendee.getPartStat();
        Map<String, Object> additionals = Collections.singletonMap("partStat", (null != partStat ? partStat : ParticipationStatus.NEEDS_ACTION).getValue());
        if (ParticipationStatus.ACCEPTED.matches(partStat)) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? RESOURCE_REPLIED_ACCEPTED_SERIES : indicatesOccurrence(event) ? RESOURCE_REPLIED_ACCEPTED_OCCURRENCE : RESOURCE_REPLIED_ACCEPTED,
                additionals, getDisplayName(replyingAttendee.getSentBy(), event), getResourceDisplayName(session, replyingAttendee), summary);
        }
        if (ParticipationStatus.TENTATIVE.matches(partStat)) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? RESOURCE_REPLIED_TENTATIVE_SERIES : indicatesOccurrence(event) ? RESOURCE_REPLIED_TENTATIVE_OCCURRENCE : RESOURCE_REPLIED_TENTATIVE,
                additionals, getDisplayName(replyingAttendee.getSentBy(), event), getResourceDisplayName(session, replyingAttendee), summary);
        }
        if (ParticipationStatus.DECLINED.matches(partStat)) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? RESOURCE_REPLIED_DECLINED_SERIES : indicatesOccurrence(event) ? RESOURCE_REPLIED_DECLINED_OCCURRENCE : RESOURCE_REPLIED_DECLINED,
                additionals, getDisplayName(replyingAttendee.getSentBy(), event), getResourceDisplayName(session, replyingAttendee), summary);
        }
        return buildAnnotation(session, 
            indicatesSeries(event) ? RESOURCE_REPLIED_NONE_SERIES : indicatesOccurrence(event) ? RESOURCE_REPLIED_NONE_OCCURRENCE : RESOURCE_REPLIED_NONE,
            additionals, getDisplayName(replyingAttendee.getSentBy(), event), getResourceDisplayName(session, replyingAttendee), summary);
    }

    private static boolean indicatesSeries(Event event) {
        return null != event.getRecurrenceRule() && false == indicatesOccurrence(event);
    }

    private static boolean indicatesOccurrence(Event event) {
        return null != event.getRecurrenceId();
    }

    /**
     * Constructs the hint that an attendee sent a comment along with his response.
     * 
     * @param comment The comment
     * @return The annotation
     */
    public ITipAnnotation getReplyCommentHint(String comment) {
        return buildAnnotation(session, COMMENT_LEFT, comment);
    }

    /**
     * Constructs the hint that the appointment has been updated in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getOutdatedReplyHint() {
        return buildAnnotation(session, REPLY_OUTDATED);
    }

    /**
     * Constructs the hint that the reply was received from an uninvited attendee.
     * 
     * @return The annotation
     */
    public ITipAnnotation getReplyFromUninvitedHint() {
        return buildAnnotation(session, REPLY_UNINVITED);
    }

    /**
     * Constructs the hint that the reply was received from an uninvited attendee whose participation was delegated by another one.
     * 
     * @param delegator The calendar user that delegated the participation
     * @return The annotation
     */
    public ITipAnnotation getReplyFromDelegateHint(CalendarUser delegator) {
        return buildAnnotation(session, REPLY_DELEGATED, getDisplayName(delegator));
    }

    /**
     * Constructs the hint that the appointment has been deleted in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getReplyForDeletedHint() {
        return buildAnnotation(session, DELETED_MEANTIME);
    }

    /**
     * Constructs the hint that an updated response was received in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getReplyUpdatedHint() {
        return buildAnnotation(session, REPLY_UPDATED);
    }

    /**
     * Constructs the hint that a reply was received for an unknown appointment.
     * 
     * @param targetUser The user id of the scheduling message's recipientString summary = getSummary(storedEvent);
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getReplyForUnknownHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, NOT_FOUND) : 
            buildAnnotation(session, NOT_FOUND_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that a reply was already applied to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getReplyAppliedHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, REPLY_APPLIED) : 
            buildAnnotation(session, REPLY_APPLIED_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that a reply needs to be saved manually to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getApplyReplyManuallyHint(int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, REPLY_APPLY_MANUALLY);
        }
        return buildAnnotation(session, REPLY_APPLY_MANUALLY_IN, getDisplayName(session, targetUser));
    }

    /**
     * Gets the introductional annotation describing that an event cancellation was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the cancelation is targeted at, or <code>null</code> if not available
     * @param originator The originator of the scheduling message
     * @return The introductional annotation
     */
    public ITipAnnotation getCanceledIntroduction(Event event, Event storedEvent, CalendarUser originator) {
        String summary = getSummary(storedEvent, event);
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, indicatesSeries(event)
                ? CANCELED_SERIES_ON_BEHALF
                : indicatesOccurrence(event)
                    ? CANCELED_OCCURRENCE_ON_BEHALF
                    : CANCELED_ON_BEHALF, summary, getDisplayName(originator.getSentBy(), event), getDisplayName(originator, event));
        }
        return buildAnnotation(session, indicatesSeries(event)
            ? CANCELED_SERIES
            : indicatesOccurrence(event)
                ? CANCELED_OCCURRENCE
                : CANCELED, summary, getDisplayName(originator, event));
    }

    /**
     * Gets the introductional annotation describing that an event cancellation was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the cancellation is targeted at, or <code>null</code> if not available
     * @param originator The originator of the scheduling message
     * @param resource The resource targeted by the scheduling message
     * @return The introductional annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceCancelledIntroduction(Event event, Event storedEvent, CalendarUser originator, CalendarUser resource) throws OXException {
        String summary = getSummary(storedEvent, event);
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? RESOURCE_CANCELED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? RESOURCE_CANCELED_OCCURRENCE_ON_BEHALF : RESOURCE_CANCELED_ON_BEHALF, 
                getDisplayName(originator.getSentBy(), event), getResourceDisplayName(session, resource), summary, getDisplayName(originator, event));
        }
        return buildAnnotation(session, 
            indicatesSeries(event) ? RESOURCE_CANCELED_SERIES : indicatesOccurrence(event) ? RESOURCE_CANCELED_OCCURRENCE : RESOURCE_CANCELED, 
            getResourceDisplayName(session, resource), summary, getDisplayName(originator, event));
    }

    /**
     * Constructs the hint that a cancel message was already applied to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getCancelAppliedHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, CANCEL_APPLIED) : 
            buildAnnotation(session, CANCEL_APPLIED_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that a cancel message was already applied to the resource's calendar.
     * 
     * @param resourceId The id of the resource targeted by the scheduling message
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceCancelAppliedHint(int resourceId) throws OXException {
        return buildAnnotation(session, CANCEL_APPLIED_IN, getResourceDisplayName(session, resourceId));
    }

    /**
     * Constructs the hint that a cancel message needs to be applied manually to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getApplyCancelManuallyHint(int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, CANCEL_APPLY_MANUALLY);
        }
        return buildAnnotation(session, CANCEL_APPLY_MANUALLY_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that an updated invitation was received in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getCancelUpdatedHint() {
        return buildAnnotation(session, UPDATED_MEANTIME);
    }

    /**
     * Constructs the hint that a cancellation was received for an unknown appointment.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getCancelForUnknownHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, NOT_FOUND) : 
            buildAnnotation(session, NOT_FOUND_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that a cancellation was received for an unknown appointment.
     * 
     * @param resource The resource targeted by the scheduling message
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getResourceCancelForUnknownHint(CalendarUser resource) throws OXException {
        return buildAnnotation(session, NOT_FOUND_IN, getResourceDisplayName(session, resource));
    }

    /**
     * Constructs the hint that the organizer left a comment along with a cancellation.
     * 
     * @param comment The comment
     * @return The annotation
     */
    public ITipAnnotation getCancelCommentHint(String comment) {
        return buildAnnotation(session, COMMENT_LEFT, comment);
    }

    /**
     * Gets the introductional annotation describing that an additional instance to a series was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param originator The originator of the scheduling message
     * @return The introductional annotation
     */
    public ITipAnnotation getAddIntroduction(Event event, CalendarUser originator) {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, ADDED_OCCURRENCE_ON_BEHALF, 
                getDisplayName(originator.getSentBy(), event), getSummary(event), getDisplayName(originator, event));
        }
        return buildAnnotation(session, ADDED_OCCURRENCE, getDisplayName(originator, event), getSummary(event));
    }

    /**
     * Constructs the hint that an additional instance cannot be applied to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getAddUnsupportedHint(int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, ADD_UNSUPPORTED);
        }
        return buildAnnotation(session, ADD_UNSUPPORTED_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that one should ask the organizer for a refreshed invitation.
     * 
     * @return The annotation
     */
    public ITipAnnotation getAskForRefreshHint() {
        return buildAnnotation(session, REQUEST_REFRESH_MANUALLY);
    }

    /**
     * Gets the introductory annotation describing that an event refresh request was received.
     * 
     * @param originator The originator of the scheduling message
     * @return The introductory annotation
     */
    public ITipAnnotation getRefreshIntroduction(CalendarUser originator) {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, REFRESH_ON_BEHALF, getDisplayName(originator.getSentBy()), getDisplayName(originator));
        }
        return buildAnnotation(session, REFRESH, getDisplayName(originator));
    }

    /**
     * Constructs the hint that the appointment for refresh could not be found in the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getRefreshForUnknownHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, NOT_FOUND) : 
            buildAnnotation(session, NOT_FOUND_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that the refresh was received from an uninvited attendee.
     * 
     * @return The annotation
     */
    public ITipAnnotation getRefreshFromUninvitedHint() {
        return buildAnnotation(session, REFRESH_UNINVITED);
    }

    /**
     * Constructs the hint that the response to a refresh needs to be sent manually.
     * 
     * @param event The event that is refreshed
     * @return The annotation
     */
    public ITipAnnotation getSendManuallyHint(Event event) {
        return buildAnnotation(session, SEND_MANUALLY, getSummary(event));
    }

    /**
     * Gets the introductional annotation describing that a counter proposal with a new timeslot for an existing event was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the counter is targeted at, or <code>null</code> if not available
     * @param originator The originator of the scheduling message
     * @return The introductional annotation
     */
    public ITipAnnotation getCounterTimeIntroduction(Event event, Event storedEvent, CalendarUser originator) {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? TIME_PROPOSED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? TIME_PROPOSED_OCCURRENCE_ON_BEHALF : TIME_PROPOSED_ON_BEHALF,
                getDisplayName(originator.getSentBy(), event), getSummary(storedEvent, event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? TIME_PROPOSED_SERIES : indicatesOccurrence(event) ? TIME_PROPOSED_OCCURRENCE : TIME_PROPOSED,
            getDisplayName(originator, event), getSummary(storedEvent, event));
    }

    /**
     * Gets the introductional annotation describing the current and proposed date-time of a counter proposal.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the counter is targeted at
     * @return The introductional annotation
     */
    public ITipAnnotation getCounterProposedTimes(Event event, Event storedEvent) {
        return buildAnnotation(session, TIME_PROPOSED_TIMES, getProposedTimeArg(storedEvent), getProposedTimeArg(event));
    }

    /**
     * Gets the introductional annotation describing that a counter proposal with arbitrary changes for an existing event was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the counter is targeted at, or <code>null</code> if not available
     * @param originator The originator of the scheduling message
     * @return The introductional annotation
     */
    public ITipAnnotation getCounterChangesIntroduction(Event event, Event storedEvent, CalendarUser originator) {
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? CHANGES_PROPOSED_SERIES_ON_BEHALF : indicatesOccurrence(event) ? CHANGES_PROPOSED_OCCURRENCE_ON_BEHALF : CHANGES_PROPOSED_ON_BEHALF,
                getDisplayName(originator.getSentBy(), event), getSummary(storedEvent, event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? CHANGES_PROPOSED_SERIES : indicatesOccurrence(event) ? CHANGES_PROPOSED_OCCURRENCE : CHANGES_PROPOSED,
            getDisplayName(originator, event), getSummary(storedEvent, event));
    }

    /**
     * Constructs the hint that the organizer left a comment along with a counter.
     * 
     * @param comment The comment
     * @return The annotation
     */
    public ITipAnnotation getCounterCommentHint(String comment) {
        return buildAnnotation(session, COMMENT_LEFT, comment);
    }

    /**
     * Constructs the hint that the counter was received from an uninvited attendee.
     * 
     * @return The annotation
     */
    public ITipAnnotation getCounterFromUninvitedHint() {
        return buildAnnotation(session, COUNTER_UNINVITED);
    }

    /**
     * Constructs the hint that the appointment has been updated in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getOutdatedCounterHint() {
        return buildAnnotation(session, COUNTER_OUTDATED);
    }
    
    /**
     * Constructs the hint that a reply was received for an unknown appointment.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getCounterForUnknownHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, NOT_FOUND) : 
            buildAnnotation(session, NOT_FOUND_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that a counter proposal needs to be saved manually to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getApplyCounterManuallyHint(int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, COUNTER_APPLY_MANUALLY);
        }
        return buildAnnotation(session, COUNTER_APPLY_MANUALLY_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that a counter proposal cannot be applied to the user's calendar.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case annotation can't be created
     */
    public ITipAnnotation getCounterUnsupportedHint(int targetUser) throws OXException {
        if (targetUser == session.getUserId()) {
            return buildAnnotation(session, COUNTER_UNSUPPORTED);
        }
        return buildAnnotation(session, COUNTER_UNSUPPORTED_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that the appointment has been deleted in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getCounterForDeletedHint() {
        return buildAnnotation(session, NOT_FOUND);
    }

    /**
     * Gets the introductional annotation describing that a declined counter proposal was received.
     * 
     * @param event The (patched) event or occurrence from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event the counter decline is targeted at, or <code>null</code> if not available
     * @param originator The originator of the scheduling message
     * @return The introductional annotation
     */
    public ITipAnnotation getCounterDeclinedIntroduction(Event event, Event storedEvent, CalendarUser originator) {
        String summary = getSummary(storedEvent, event);
        if (null != originator.getSentBy()) {
            return buildAnnotation(session, 
                indicatesSeries(event) ? COUNTER_DECLINED_ON_BEHALF : indicatesOccurrence(event) ? COUNTER_DECLINED_OCCURRENCE_ON_BEHALF : COUNTER_DECLINED_ON_BEHALF,
                summary, getDisplayName(originator.getSentBy(), event), getDisplayName(originator, event));
        } 
        return buildAnnotation(session, 
            indicatesSeries(event) ? COUNTER_DECLINED_SERIES : indicatesOccurrence(event) ? COUNTER_DECLINED_OCCURRENCE : COUNTER_DECLINED,
            summary, getDisplayName(originator, event));
    }
    
    /**
     * Constructs the hint that an updated invitation was received in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getCounterDeclinedUpdatedHint() {
        return buildAnnotation(session, UPDATED_MEANTIME);
    }

    /**
     * Constructs the hint that the organizer copy is apparently updated in the meantime.
     * 
     * @return The annotation
     */
    public ITipAnnotation getCounterDeclinedForUpdatedHint() {
        return buildAnnotation(session, COUNTER_DECLINED_FOR_UPDATED);
    }

    /**
     * Constructs the hint that a decline counter was received for an unknown appointment.
     * 
     * @param targetUser The user id of the scheduling message's recipient
     * @return The annotation
     * @throws OXException In case target user can't be found
     */
    public ITipAnnotation getCounterDeclinedForUnknownHint(int targetUser) throws OXException {
        return targetUser == session.getUserId() ? buildAnnotation(session, NOT_FOUND) : 
            buildAnnotation(session, NOT_FOUND_IN, getDisplayName(session, targetUser));
    }

    /**
     * Constructs the hint that the organizer left a comment along with declining the counter.
     * 
     * @param comment The comment
     * @return The annotation
     */
    public ITipAnnotation getCounterDeclinedCommentHint(String comment) {
        return buildAnnotation(session, COMMENT_LEFT, comment);
    }

    /**
     * Builds an iTIP annotation out of a message string and format arguments.
     * 
     * @param session The calendar session
     * @param message The message to take over into the annotation
     * @param args Format arguments for the message string
     * @return The iTIP annotation
     */
    private static ITipAnnotation buildAnnotation(CalendarSession session, String message, Object... args) {
        return buildAnnotation(session, message, null, args);
    }

    /**
     * Builds an iTIP annotation out of a message string and format arguments.
     * 
     * @param session The calendar session
     * @param message The message to take over into the annotation
     * @param additionals A map containing additional parameters of the annotation, or <code>null</code> if not set
     * @param args Format arguments for the message string
     * @return The iTIP annotation
     */
    private static ITipAnnotation buildAnnotation(CalendarSession session, String message, Map<String, Object> additionals, Object... args) {
        List<Object> arguments = null != args && 0 < args.length ? Arrays.asList(args) : Collections.emptyList();
        return ITipAnnotationBuilder.newBuilder().locale(null == session ? null : getLocale(session)).message(message).additionals(additionals).args(arguments).build();
    }

    private String getProposedTimeArg(Event event) {
        /*
         * prepare date/time formats based on session user's preferences
         */
        RegionalSettingsService regionalSettingsService = services.getOptionalService(RegionalSettingsService.class);
        RegionalSettings regionalSettings = null != regionalSettingsService ? regionalSettingsService.get(session.getContextId(), session.getUserId()) : null;
        Date startDate = new Date(event.getStartDate().getTimestamp());
        Date endDate = new Date(event.getEndDate().getTimestamp());
        DateFormat longDate = RegionalSettingsUtil.getDateFormat(regionalSettings, DateFormat.LONG, getLocale(session));
        if (event.getStartDate().isAllDay()) {
            longDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            endDate = new Date(endDate.getTime() - 1000); // Move this before midnight, so the time formatting routines don't lie
        } else {
            longDate.setTimeZone(getTimeZone(session));
        }
        DateFormat time = RegionalSettingsUtil.getTimeFormat(regionalSettings, DateFormat.SHORT, getLocale(session));
        time.setTimeZone(getTimeZone(session));
        /*
         * format event start- and enddate accordingly
         */
        if (isSameDay(event.getStartDate(), event.getEndDate())) {
            if (event.getStartDate().isAllDay()) {
                return String.format("%s (%s)", longDate.format(startDate), Messages.FULL_TIME);
            }
            return String.format("%s - %s", longDate.format(startDate) + " " + time.format(startDate), time.format(endDate));
        }
        if (event.getStartDate().isAllDay()) {
            return String.format("%s - %s (%s)", longDate.format(startDate), longDate.format(endDate), Messages.FULL_TIME);
        }
        return String.format("%s - %s", longDate.format(startDate) + " " + time.format(startDate), longDate.format(endDate) + " " + time.format(endDate));
    }

    private static boolean isSameDay(DateTime startDate, DateTime endDate) {
        return null != startDate && null != endDate && startDate.getYear() == endDate.getYear() && startDate.getMonth() == endDate.getMonth() && startDate.getDayOfMonth() == endDate.getDayOfMonth();
    }

}
