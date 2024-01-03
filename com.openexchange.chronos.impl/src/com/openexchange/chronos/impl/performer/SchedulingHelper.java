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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.addExtendedProperty;
import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getDateInTimeZone;
import static com.openexchange.chronos.common.CalendarUtils.getRecurrenceIds;
import static com.openexchange.chronos.common.CalendarUtils.hasExternalOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.initRecurrenceRule;
import static com.openexchange.chronos.common.CalendarUtils.isAttendeeSchedulingResource;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isOrganizerSchedulingResource;
import static com.openexchange.chronos.common.CalendarUtils.isResourceOrRoom;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.impl.Utils.getCalendarUser;
import static com.openexchange.chronos.impl.Utils.isResourceCalendarFolder;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.DelegatingEvent;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.SchedulingControl;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.common.mapping.AttendeeEventUpdate;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.impl.scheduling.AttachmentDataProvider;
import com.openexchange.chronos.impl.scheduling.ChangeBuilder;
import com.openexchange.chronos.impl.scheduling.DefaultRecipientSettings;
import com.openexchange.chronos.impl.scheduling.MessageBuilder;
import com.openexchange.chronos.scheduling.SchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.changes.Change;
import com.openexchange.chronos.scheduling.changes.ChangeAction;
import com.openexchange.chronos.scheduling.changes.DescriptionService;
import com.openexchange.chronos.scheduling.changes.ScheduleChange;
import com.openexchange.chronos.scheduling.changes.SchedulingChangeService;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.chronos.service.RecurrenceIterator;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Pair;
import com.openexchange.java.util.TimeZones;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link SchedulingHelper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class SchedulingHelper {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SchedulingHelper.class);

    private final ServiceLookup services;
    private final CalendarSession session;
    private final CalendarUser calendarUser;
    private final CalendarUserType calendarUserType;
    private final CalendarFolder folder;
    private final ResultTracker tracker;

    /**
     * Initializes a new {@link SchedulingHelper}.
     *
     * @param services A service lookup reference
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     * @param tracker The underlying result tracker
     * @throws OXException If calendar user can't be found
     */
    public SchedulingHelper(ServiceLookup services, CalendarSession session, CalendarFolder folder, ResultTracker tracker) throws OXException {
        super();
        this.services = services;
        this.session = session;
        this.folder = folder;
        this.calendarUser = getCalendarUser(session, folder);
        this.calendarUserType = isResourceCalendarFolder(folder) ? CalendarUserType.RESOURCE : CalendarUserType.INDIVIDUAL;
        this.tracker = tracker;
    }

    /**
     * Tracks scheduling messages for a newly created calendar object resource in the underlying calendar folder.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#CREATE} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#CREATE} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees for an organizer scheduling resource</li>
     * </ul>
     *
     * @param createdResource The newly created calendar object resource
     */
    public void trackCreation(CalendarObjectResource createdResource) {
        trackCreation(createdResource, null);
    }

    /**
     * Tracks scheduling messages for a newly created calendar object resource in the underlying calendar folder.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#CREATE} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#CREATE} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees for an organizer scheduling resource</li>
     * </ul>
     *
     * @param createdResource The newly created calendar object resource
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    public void trackCreation(CalendarObjectResource createdResource, List<? extends CalendarUser> consideredRecipients) {
        try {
            CalendarUser originator = getOriginator(lookupCalendarUser(createdResource));
            LOG.trace("Tracking 'create' scheduling messages [originator={}, createdResource={}]", originator, createdResource);
            if (false == shouldTrack(createdResource)) {
                return;
            }
            if (false == hasExternalOrganizer(createdResource) && isIndividualCalendarOwner(calendarUser, calendarUserType) && //
                false == isActing(calendarUser) && isNotifyOnCreate(calendarUser) && shouldTrack(calendarUser, consideredRecipients)) {
                /*
                 * prepare message to calendar owner of newly created resource when acting on behalf, if enabled
                 */
                trackCreateMessage(createdResource, originator, calendarUser, calendarUserType);
            }
            if (isOrganizerSchedulingResource(createdResource, calendarUser.getEntity())) {
                /*
                 * prepare scheduling messages from organizer to attendees
                 */
                trackCreation(createdResource, originator, consideredRecipients);
            } else if (false == hasExternalOrganizer(createdResource)) {
                /*
                 * prepare scheduling messages from attendee acting on behalf of the organizer to (newly added) attendees
                 */
                CalendarUser customOriginator = injectSentBy(createdResource.getOrganizer(), originator);
                trackCreation(createdResource, customOriginator, consideredRecipients);
            }
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error tracking 'create' scheduling messsages: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks scheduling messages for a newly created calendar object resource in the underlying calendar folder, using
     * a specific originator for the generated messages. The acting user as well as the owner of the underlying calendar are skipped
     * implicitly.
     *
     * @param createdResource The newly created calendar object resource
     * @param originator The originator of the messages
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    private void trackCreation(CalendarObjectResource createdResource, CalendarUser originator, List<? extends CalendarUser> consideredRecipients) throws OXException {
        for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(createdResource, consideredRecipients).entrySet()) {
            Attendee recipient = entry.getKey();
            if (isInternal(recipient)) {
                /*
                 * prepare messages for each individual internal attendee, if enabled
                 */
                if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) && false == isActing(recipient) &&
                    false == isCalendarOwner(recipient) && isNotifyOnCreate(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    trackCreateMessage(entry.getValue(), originator, recipient, recipient.getCuType());
                }
                /*
                 * prepare messages for internal resource attendees
                 */
                if (isResourceOrRoom(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    if (isNotifyResourceAttendees()) {
                        trackCreateMessage(entry.getValue(), originator, recipient, recipient.getCuType());
                    }
                    for (int bookingDelegate : getOtherBookingDelegates(recipient)) {
                        /*
                         * inject indirect sent-by user if applicable (for mails to booking delegates, when acting on behalf)
                         */
                        CalendarObjectResource resource = entry.getValue();
                        resource = injectSentBy(resource, originator);
                        /*
                         * inject resource as originator and booking delegate as recipient
                         */
                        Attendee delegateRecipient = prepareDelegateRecipient(recipient, bookingDelegate);
                        CalendarUser delegateOriginator = prepareDelegateOriginator(recipient, originator);
                        trackCreateMessage(resource, delegateOriginator, delegateRecipient, delegateRecipient.getCuType());
                    }
                }
            } else {
                /*
                 * prepare scheduling messages for each external attendee
                 */
                if (shouldTrack(recipient, consideredRecipients)) {
                    trackCreateMessage(entry.getValue(), originator, recipient);
                }
            }
        }
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#UPDATE} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#UPDATE} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees for an organizer scheduling resource</li>
     * <li>{@link ChangeAction#UPDATE} messages to internal attendees if the current user implicitly acts on behalf of the organizer</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees if the current user implicitly acts on behalf of the organizer</li>
     * </ul>
     *
     * @param updatedResource The updated calendar object resource
     * @param eventUpdate The performed event update
     */
    public void trackUpdate(CalendarObjectResource updatedResource, EventUpdate eventUpdate) {
        trackUpdate(updatedResource, null, eventUpdate, null);
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#UPDATE} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#UPDATE} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees for an organizer scheduling resource</li>
     * <li>{@link ChangeAction#UPDATE} messages to internal attendees if the current user implicitly acts on behalf of the organizer</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees if the current user implicitly acts on behalf of the organizer</li>
     * </ul>
     *
     * @param updatedResource The updated calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is updated, or <code>null</code> if not available
     * @param eventUpdate The performed event update
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    public void trackUpdate(CalendarObjectResource updatedResource, Event seriesMaster, EventUpdate eventUpdate, List<? extends CalendarUser> consideredRecipients) {
        trackUpdate(updatedResource, seriesMaster, Collections.singletonList(eventUpdate), consideredRecipients);
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#UPDATE} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#UPDATE} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees for an organizer scheduling resource</li>
     * <li>{@link ChangeAction#UPDATE} messages to internal attendees if the current user implicitly acts on behalf of the organizer</li>
     * <li>{@link SchedulingMethod#REQUEST} messages to external attendees if the current user implicitly acts on behalf of the organizer</li>
     * </ul>
     *
     * @param updatedResource The updated calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is updated, or <code>null</code> if not available
     * @param eventUpdates The list of performed event updates
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    public void trackUpdate(CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> eventUpdates, List<? extends CalendarUser> consideredRecipients) {
        try {
            CalendarUser originator = getOriginator(lookupCalendarUser(updatedResource));
            LOG.trace("Tracking 'update' scheduling messages [originator={}, updatedResource={}, seriesMaster={}, eventUpdates={}]",
                originator, updatedResource, seriesMaster, eventUpdates);
            if (false == shouldTrack(updatedResource, eventUpdates)) {
                return;
            }
            if (isIndividualCalendarOwner(calendarUser, calendarUserType) && false == isActing(calendarUser) && isNotifyOnUpdate(calendarUser) && shouldTrack(calendarUser, consideredRecipients)) {
                /*
                 * prepare message to calendar owner of updated resource when acting on behalf, if enabled
                 */
                trackUpdateMessage(updatedResource, seriesMaster, eventUpdates, originator, calendarUser, calendarUserType);
            }
            if (isOrganizerSchedulingResource(updatedResource, calendarUser.getEntity())) {
                /*
                 * prepare scheduling messages from organizer to attendees
                 */
                trackUpdate(updatedResource, seriesMaster, eventUpdates, originator, consideredRecipients);
            } else if (hasExternalOrganizer(updatedResource)) {
                /*
                 * prepare counter proposal to external organizer once this is allowed, for now this path should not be possible
                 */
                throw new UnsupportedOperationException("COUNTER not implemented");
            } else {
                /*
                 * prepare scheduling messages from attendee acting on behalf of the organizer to attendees
                 */
                CalendarUser customOriginator = injectSentBy(updatedResource.getOrganizer(), originator);
                trackUpdate(updatedResource, seriesMaster, eventUpdates, customOriginator, consideredRecipients);
            }
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error tracking 'update' scheduling messsages: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder, using a
     * specific originator for the generated messages. The acting user as well as the owner of the underlying calendar are skipped
     * implicitly.
     *
     * @param updatedResource The updated calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is updated, or <code>null</code> if not available
     * @param eventUpdates The list of performed event updates
     * @param originator The originator of the messages
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    private void trackUpdate(CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> eventUpdates, CalendarUser originator, List<? extends CalendarUser> consideredRecipients) throws OXException {
        for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(updatedResource, consideredRecipients).entrySet()) {
            Attendee recipient = entry.getKey();
            if (isInternal(recipient)) {
                /*
                 * prepare messages for each individual internal attendee, if enabled
                 */
                if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) && false == isActing(recipient) &&
                    false == isCalendarOwner(recipient) && isNotifyOnUpdate(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    trackUpdateMessage(entry.getValue(), seriesMaster, eventUpdates, originator, recipient);
                }
                /*
                 * prepare messages for internal resource attendees
                 */
                if (isResourceOrRoom(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    if (isNotifyResourceAttendees()) {
                        trackUpdateMessage(entry.getValue(), seriesMaster, eventUpdates, originator, recipient);
                    }
                    for (int bookingDelegate : getOtherBookingDelegates(recipient)) {
                        /*
                         * inject indirect sent-by user if applicable (for mails to booking delegates, when acting on behalf)
                         */
                        CalendarObjectResource resource = entry.getValue();
                        resource = injectSentBy(resource, originator);
                        /*
                         * inject resource as originator and booking delegate as recipient
                         */
                        Attendee delegateRecipient = prepareDelegateRecipient(recipient, bookingDelegate);
                        CalendarUser delegateOriginator = prepareDelegateOriginator(recipient, originator);
                        trackUpdateMessage(resource, seriesMaster, eventUpdates, delegateOriginator, delegateRecipient);
                    }
                }
            } else {
                /*
                 * prepare scheduling messages for each external attendee
                 */
                if (shouldTrack(recipient, consideredRecipients)) {
                    trackUpdateMessage(entry.getValue(), seriesMaster, eventUpdates, originator, recipient);
                }
            }
        }
    }

    /**
     * Tracks scheduling messages for a deleted calendar object resource in the underlying calendar folder, handling both
     * attendee- and organizer scheduling resources.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#CANCEL} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#CANCEL} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#CANCEL} messages to external attendees for an organizer scheduling resource</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#CANCEL} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param deletedResource The deleted calendar object resource
     */
    public void trackDeletion(CalendarObjectResource deletedResource) {
        trackDeletion(deletedResource, null);
    }

    /**
     * Tracks scheduling messages for a deleted calendar object resource in the underlying calendar folder, handling both
     * attendee- and organizer scheduling resources.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#CANCEL} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#CANCEL} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#CANCEL} messages to external attendees for an organizer scheduling resource</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#CANCEL} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param deletedResource The deleted calendar object resource
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    public void trackDeletion(CalendarObjectResource deletedResource, List<? extends CalendarUser> consideredRecipients) {
        trackDeletion(deletedResource, null, consideredRecipients);
    }

    /**
     * Tracks scheduling messages for a deleted calendar object resource in the underlying calendar folder, handling both
     * attendee- and organizer scheduling resources.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#CANCEL} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>{@link ChangeAction#CANCEL} messages to internal attendees for an organizer scheduling resource</li>
     * <li>{@link SchedulingMethod#CANCEL} messages to external attendees for an organizer scheduling resource</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#CANCEL} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param deletedResource The deleted calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is deleted, or <code>null</code> if not available
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    public void trackDeletion(CalendarObjectResource deletedResource, Event seriesMaster, List<? extends CalendarUser> consideredRecipients) {
        try {
            CalendarUser originator = getOriginator(lookupCalendarUser(deletedResource));
            LOG.trace("Tracking 'delete' scheduling messages [originator={}, deletedResource={}, seriesMaster={}]",
                originator, deletedResource, seriesMaster);
            if (false == shouldTrack(deletedResource)) {
                return;
            }
            CalendarObjectResource resource = injectComment(deletedResource);
            /*
             * prepare message to calendar owner of deleted resource when acting on behalf, if enabled
             */
            if (isIndividualCalendarOwner(calendarUser, calendarUserType) && false == isActing(calendarUser) && isNotifyOnDelete(calendarUser) && shouldTrack(calendarUser, consideredRecipients)) {
                trackCancelMessage(deletedResource, seriesMaster, originator, calendarUser, calendarUserType);
            }
            /*
             * prepare scheduling messages from organizer to attendees
             */
            if (isOrganizerSchedulingResource(resource, calendarUser.getEntity())) {
                trackDeletion(resource, seriesMaster, originator, consideredRecipients);
            }
            /*
             * prepare scheduling messages from attendee to organizer
             */
            if (isAttendeeSchedulingResource(resource, calendarUser.getEntity())) {
                Organizer recipient = resource.getOrganizer();
                if (isInternal(recipient, CalendarUserType.INDIVIDUAL)) {
                    /*
                     * prepare reply message to internal organizer, if enabled
                     */
                    if (false == isActing(calendarUser) && isNotifyOnReply(recipient) && shouldTrack(recipient, consideredRecipients)) {
                        trackReplyMessage(resource, seriesMaster, originator, recipient, ParticipationStatus.DECLINED, optSchedulingComment());
                    }
                } else {
                    /*
                     * prepare scheduling reply to external organizer
                     */
                    if (shouldTrack(recipient, consideredRecipients)) {
                        trackReplyMessage(resource, seriesMaster, originator, recipient, ParticipationStatus.DECLINED, optSchedulingComment());
                    }
                }
            }
            if (isAttendeeSchedulingResource(resource, calendarUser.getEntity())) {
                /*
                 * prepare messages for each individual internal attendee, if enabled
                 */
                for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(resource, true).entrySet()) {
                    Attendee recipient = entry.getKey();
                    if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) &&
                        false == isActing(recipient) && false == isCalendarOwner(recipient) && isNotifyOnReplyAsAttendee(recipient) && shouldTrack(recipient, consideredRecipients)) {
                        trackReplyMessage(resource, seriesMaster, originator, recipient, recipient.getCuType(), ParticipationStatus.DECLINED, optSchedulingComment());
                    }
                }
            }
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error tracking 'delete' scheduling messsages: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks scheduling messages for a deleted calendar object resource in the underlying calendar folder, using a
     * specific originator for the generated messages. The acting user as well as the owner of the underlying calendar are skipped
     * implicitly.
     *
     * @param deletedResource The deleted calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is deleted, or <code>null</code> if not available
     * @param originator The originator of the messages
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    private void trackDeletion(CalendarObjectResource deletedResource, Event seriesMaster, CalendarUser originator, List<? extends CalendarUser> consideredRecipients) throws OXException {
        for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(injectComment(deletedResource), consideredRecipients).entrySet()) {
            Attendee recipient = entry.getKey();
            if (isInternal(recipient)) {
                /*
                 * prepare messages for each individual internal attendee, if enabled
                 */
                if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) && false == isActing(recipient) && 
                    false == isCalendarOwner(recipient) && isNotifyOnDelete(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    trackCancelMessage(entry.getValue(), seriesMaster, originator, recipient);
                }
                /*
                 * prepare messages for internal resource attendees
                 */
                if (isResourceOrRoom(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    if (isNotifyResourceAttendees()) {
                        trackCancelMessage(entry.getValue(), seriesMaster, originator, recipient);
                    }
                    for (int bookingDelegate : getOtherBookingDelegates(recipient)) {
                        /*
                         * inject indirect sent-by user if applicable (for mails to booking delegates, when acting on behalf)
                         */
                        CalendarObjectResource resource = entry.getValue();
                        resource = injectSentBy(resource, originator);
                        /*
                         * inject resource as originator and booking delegate as recipient
                         */
                        Attendee delegateRecipient = prepareDelegateRecipient(recipient, bookingDelegate);
                        CalendarUser delegateOriginator = prepareDelegateOriginator(recipient, originator);
                        trackCancelMessage(resource, seriesMaster, delegateOriginator, delegateRecipient);
                    }
                }
            } else {
                /*
                 * prepare scheduling messages for each external attendee
                 */
                if (shouldTrack(recipient, consideredRecipients)) {
                    trackCancelMessage(entry.getValue(), seriesMaster, originator, recipient);
                }
            }
        }
    }

    /**
     * Tracks scheduling messages for a single updated event in the underlying calendar folder, after the participation
     * status was changed.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#REPLY} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#REPLY} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param updatedEvent The updated event
     * @param originalAttendee The original attendee that replies
     * @param updatedAttendee The updated attendee that replies
     */
    public void trackReply(Event updatedEvent, Attendee originalAttendee, Attendee updatedAttendee) {
        trackReply(updatedEvent, null, originalAttendee, updatedAttendee);
    }

    /**
     * Tracks scheduling messages for a single updated event in the underlying calendar folder, after the participation
     * status was changed.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#REPLY} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#REPLY} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param updatedEvent The updated event
     * @param seriesMaster The series master event in case an instance of an event series is replied, or <code>null</code> if not available
     * @param originalAttendee The original attendee that replies
     * @param updatedAttendee The updated attendee that replies
     */
    public void trackReply(Event updatedEvent, Event seriesMaster, Attendee originalAttendee, Attendee updatedAttendee) {
        EventUpdate attendeeEventUpdate = new AttendeeEventUpdate(updatedEvent, originalAttendee, updatedAttendee);
        trackReply(updatedAttendee, new DefaultCalendarObjectResource(updatedEvent), seriesMaster, Collections.singletonList(attendeeEventUpdate));
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder, after the
     * participation status was changed.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#REPLY} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#REPLY} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param attendee The attendee that replies
     * @param updatedResource The updated calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is replied, or <code>null</code> if not available
     * @param attendeeEventUpdate The performed attendee event update
     */
    public void trackReply(Attendee attendee, CalendarObjectResource updatedResource, Event seriesMaster, EventUpdate attendeeEventUpdate) {
        trackReply(attendee, updatedResource, seriesMaster, Collections.singletonList(attendeeEventUpdate));
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder, after the
     * participation status was changed.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#REPLY} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#REPLY} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param attendee The attendee that replies
     * @param updatedResource The updated calendar object resource
     * @param attendeeEventUpdates The list of performed attendee event updates
     */
    public void trackReply(Attendee attendee, CalendarObjectResource updatedResource, List<EventUpdate> attendeeEventUpdates) {
        trackReply(attendee, updatedResource, null, attendeeEventUpdates);
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder, after the
     * participation status was changed.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#REPLY} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>a {@link SchedulingMethod#REPLY} message to an external organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#REPLY} messages to other internal attendees</li>
     * </ul>
     *
     * @param attendee The attendee that replies
     * @param updatedResource The updated calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is replied, or <code>null</code> if not available
     * @param attendeeEventUpdates The list of performed attendee event updates
     */
    public void trackReply(Attendee attendee, CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> attendeeEventUpdates) {
        try {
            CalendarUser originator = getOriginator(attendee);
            LOG.trace("Tracking 'reply' scheduling messages [originator={}, updatedResource={}, seriesMaster={}, attendeeEventUpdates={}]",
                originator, updatedResource, seriesMaster, attendeeEventUpdates);
            if (false == shouldTrack(updatedResource, attendeeEventUpdates)) {
                return;
            }
            /*
             * prepare message to calendar owner of updated resource when acting on behalf, if enabled
             */
            if (isIndividualCalendarOwner(calendarUser, calendarUserType) && false == isActing(calendarUser) && isNotifyOnReply(calendarUser)) {
                trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, originator, calendarUser, calendarUserType);
            }
            /*
             * prepare scheduling messages from attendee to organizer
             */
            if (isAttendeeSchedulingResource(updatedResource, calendarUser.getEntity())) {
                Organizer recipient = updatedResource.getOrganizer();
                if (isInternal(recipient, CalendarUserType.INDIVIDUAL)) {
                    /*
                     * prepare reply message to internal organizer, if enabled
                     */
                    if (false == isActing(recipient) && isNotifyOnReply(recipient)) {
                        trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, originator, recipient);
                    }
                } else {
                    /*
                     * prepare scheduling reply to external organizer
                     */
                    trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, originator, recipient);
                }
            }
            /*
             * prepare messages for internal resource attendees (upon changes by other booking delegates)
             */
            if (isResourceOrRoom(attendee) && null != originator.getSentBy()) {
                if (isNotifyResourceAttendees()) {
                    trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, originator, attendee, CalendarUserType.RESOURCE);
                }
                for (int bookingDelegate : getOtherBookingDelegates(attendee)) {
                    if (matches(updatedResource.getOrganizer(), bookingDelegate)) {
                        continue; // organizer is already notified 
                    }
                    /*
                     * inject indirect sent-by user if applicable (for mails to booking delegates, when acting on behalf)
                     */
                    CalendarObjectResource resource = new DefaultCalendarObjectResource(updatedResource.getEvents());
                    resource = injectSentBy(resource, originator);
                    /*
                     * inject resource as originator and booking delegate as recipient
                     */
                    Attendee delegateRecipient = prepareDelegateRecipient(attendee, bookingDelegate);
                    CalendarUser delegateOriginator = prepareDelegateOriginator(attendee, null != originator.getSentBy() ? originator.getSentBy() : originator);
                    trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, delegateOriginator, delegateRecipient, delegateRecipient.getCuType());
                }
            }
            /*
             * prepare messages for each individual internal attendee, if enabled
             */
            for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(updatedResource, true).entrySet()) {
                Attendee recipient = entry.getKey();
                if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) && false == matches(updatedResource.getOrganizer(), recipient) && false == isActing(recipient) && false == isCalendarOwner(recipient) && isNotifyOnReplyAsAttendee(recipient)) {
                    trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, originator, recipient, recipient.getCuType());
                }
            }            
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error tracking 'reply' scheduling messsages: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks scheduling messages for an updated calendar object resource in the underlying calendar folder, after the
     * participation status of an external attendee was changed.
     * <p/>
     * This includes:
     * <ul>
     * <li>a {@link ChangeAction#REPLY} message to the calendar owner if the current user acts on behalf of him</li>
     * <li>a {@link ChangeAction#REPLY} message to an internal organizer for an attendee scheduling resource</li>
     * <li>{@link ChangeAction#REPLY} messages to other internal attendees for an attendee scheduling resource</li>
     * </ul>
     *
     * @param updatedResource The updated calendar object resource
     * @param seriesMaster The series master event in case an instance of an event series is replied, or <code>null</code> if not available
     * @param eventUpdates The list of updated events
     */
    public void trackProcessedReply(CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> eventUpdates) {
        try {
            CalendarUser originator = getOriginator(updatedResource.getOrganizer());
            LOG.trace("Tracking messages after 'reply' has been applied [originator={}, updatedResource={}]", originator, updatedResource);
            if (false == shouldTrack(updatedResource, null)) {
                return;
            }
            /*
             * prepare message to calendar owner of updated resource when acting on behalf, if enabled
             */
            if (isIndividualCalendarOwner(calendarUser, calendarUserType) && false == isActing(calendarUser) && isNotifyOnReply(calendarUser)) {
                trackReplyMessage(updatedResource, eventUpdates, seriesMaster, originator, calendarUser, calendarUserType);
                
            }
            /*
             * prepare message to internal organizer, if enabled
             */
            CalendarUser organizer = updatedResource.getOrganizer();
            if (isInternal(organizer, CalendarUserType.INDIVIDUAL) && false == isActing(organizer) && isNotifyOnReply(organizer)) {
                trackReplyMessage(updatedResource, eventUpdates, seriesMaster, getCalendarUser(session, folder), organizer, CalendarUserType.INDIVIDUAL);
            }
            /*
             * prepare messages for each individual internal attendee, if enabled
             */
            for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(updatedResource, true).entrySet()) {
                Attendee recipient = entry.getKey();
                if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) && false == matches(updatedResource.getOrganizer(), recipient) &&
                    false == isActing(recipient) && false == isCalendarOwner(recipient) && isNotifyOnReplyAsAttendee(recipient)) {
                    trackReplyMessage(updatedResource, eventUpdates, seriesMaster, originator, recipient, recipient.getCuType());
                }
            }
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error tracking scheduling messages after 'reply' messsages: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks scheduling messages for a declined counter proposal of a calendar object resource in the underlying
     * calendar folder if the acting user is the organizer.
     *
     * @param counteredResource The calendar object resource the decline counter is targeted at
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    public void trackDeclineCounter(CalendarObjectResource counteredResource, List<? extends CalendarUser> consideredRecipients) {
        try {
            CalendarUser originator = getOriginator(lookupCalendarUser(counteredResource));
            LOG.trace("Tracking 'declinecounter' scheduling messages [originator={}, counteredResource={}]", originator, counteredResource);
            if (false == shouldTrack(counteredResource)) {
                return;
            }
            if (isOrganizerSchedulingResource(counteredResource, calendarUser.getEntity())) {
                /*
                 * prepare scheduling messages from organizer to attendees
                 */
                trackDeclineCounter(counteredResource, originator, consideredRecipients);
            }
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error tracking 'decline counter' scheduling messsages: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks scheduling messages for a declined counter proposal of a calendar object resource in the underlying
     * calendar folder, using a specific originator for the generated messages. The acting user as well as the owner of the underlying
     * calendar are skipped implicitly.
     *
     * @param counteredResource The calendar object resource the decline counter is targeted at
     * @param originator The originator of the messages
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     */
    private void trackDeclineCounter(CalendarObjectResource counteredResource, CalendarUser originator, List<? extends CalendarUser> consideredRecipients) throws OXException {
        for (Entry<Attendee, CalendarObjectResource> entry : getResourcesPerAttendee(counteredResource, consideredRecipients).entrySet()) {
            Attendee recipient = entry.getKey();
            if (isInternal(recipient)) {
                /*
                 * prepare messages for each individual internal attendee, if enabled
                 */
                if (CalendarUserType.INDIVIDUAL.matches(recipient.getCuType()) && false == isActing(recipient) && 
                    false == isCalendarOwner(recipient) && isNotifyOnCreate(recipient) && shouldTrack(recipient, consideredRecipients)) {
                    // not possible yet
                    // trackDeclineCounterNotification(entry.getValue(), originator, recipient, recipient.getCuType()); 
                }
            } else {
                /*
                 * prepare scheduling messages for each external attendee
                 */
                if (shouldTrack(recipient, consideredRecipients)) {
                    trackDeclineCounterMessage(counteredResource, originator, recipient, optSchedulingComment());
                }
            }
        }
    }

    private void trackCreateMessage(CalendarObjectResource createdResource, CalendarUser originator, Attendee recipient) throws OXException {
        trackCreateMessage(createdResource, originator, recipient, recipient.getCuType());
    }

    private void trackCreateMessage(CalendarObjectResource createdResource, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType) throws OXException {
        SchedulingMessage message = new MessageBuilder() // @formatter:off
            .setMethod(SchedulingMethod.REQUEST)
            .setOriginator(originator)
            .setRecipient(recipient)
            .setResource(injectSentBy(createdResource, originator))
            .setScheduleChange(describeCreate(createdResource, originator))
            .setAttachmentDataProvider(getAttachmentDataProvider())
            .setRecipientSettings(new DefaultRecipientSettings(services, session, originator, recipient, recipientType, createdResource))
        .build(); // @formatter:on
        LOG.trace("Tracking {}", message);
        tracker.trackSchedulingMessage(message);
    }

    private void trackUpdateMessage(CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> eventUpdates, CalendarUser originator, Attendee recipient) throws OXException {
        trackUpdateMessage(updatedResource, seriesMaster, eventUpdates, originator, recipient, recipient.getCuType());
    }

    private void trackUpdateMessage(CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> eventUpdates, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType) throws OXException {
        SchedulingMessage message = new MessageBuilder() // @formatter:off
            .setMethod(SchedulingMethod.REQUEST)
            .setOriginator(originator)
            .setRecipient(recipient)
            .setResource(injectSentBy(updatedResource, originator))
            .setScheduleChange(describeUpdate(updatedResource, seriesMaster, eventUpdates, originator, recipient))
            .setAttachmentDataProvider(getAttachmentDataProvider())
            .setRecipientSettings(new DefaultRecipientSettings(services, session, originator, recipient, recipientType, updatedResource))
        .build(); // @formatter:on
        LOG.trace("Tracking {}", message);
        tracker.trackSchedulingMessage(message);
    }

    private void trackCancelMessage(CalendarObjectResource deletedResource, Event seriesMaster, CalendarUser originator, Attendee recipient) throws OXException {
        trackCancelMessage(deletedResource, seriesMaster, originator, recipient, recipient.getCuType());
    }

    private void trackCancelMessage(CalendarObjectResource deletedResource, Event seriesMaster, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType) throws OXException {
        SchedulingMessage message = new MessageBuilder() // @formatter:off
            .setMethod(SchedulingMethod.CANCEL)
            .setOriginator(originator)
            .setRecipient(recipient)
            .setResource(injectSentBy(deletedResource, originator))
            .setScheduleChange(describeCancel(deletedResource, seriesMaster, originator, recipient))
            .setRecipientSettings(new DefaultRecipientSettings(services, session, originator, recipient, recipientType, deletedResource))
        .build(); // @formatter:on
        LOG.trace("Tracking {}", message);
        tracker.trackSchedulingMessage(message);

    }

    private void trackReplyMessage(CalendarObjectResource resource, Event seriesMaster, CalendarUser originator, Organizer recipient, ParticipationStatus partStat, String comment) throws OXException {
        trackReplyMessage(resource, seriesMaster, originator, recipient, CalendarUserType.INDIVIDUAL, partStat, comment);
    }

    private void trackReplyMessage(CalendarObjectResource resource, Event seriesMaster, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType, ParticipationStatus partStat, String comment) throws OXException {
        Pair<ScheduleChange, CalendarObjectResource> reply = describeReply(resource, seriesMaster, originator, recipient, partStat, comment);
        SchedulingMessage message = new MessageBuilder() // @formatter:off
            .setMethod(SchedulingMethod.REPLY)
            .setOriginator(originator)
            .setRecipient(recipient)
            .setResource(injectSentBy(reply.getSecond(), originator))
            .setScheduleChange(reply.getFirst())
            .setRecipientSettings(new DefaultRecipientSettings(services, session, originator, recipient, recipientType, reply.getSecond()))
        .build(); // @formatter:on
        LOG.trace("Tracking {}", message);
        tracker.trackSchedulingMessage(message);
    }

    private void trackReplyMessage(CalendarObjectResource updatedResource, List<EventUpdate> attendeeEventUpdates, Event seriesMaster, CalendarUser originator, Organizer recipient) throws OXException {
        trackReplyMessage(updatedResource, attendeeEventUpdates, seriesMaster, originator, recipient, CalendarUserType.INDIVIDUAL);
    }

    private void trackReplyMessage(CalendarObjectResource updatedResource, List<EventUpdate> attendeeEventUpdates, Event seriesMaster, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType) throws OXException {
        SchedulingMessage message = new MessageBuilder() // @formatter:off
            .setMethod(SchedulingMethod.REPLY)
            .setOriginator(originator)
            .setRecipient(recipient)
            .setResource(injectSentBy(updatedResource, originator))
            .setScheduleChange(describeReply(updatedResource, attendeeEventUpdates, seriesMaster, originator, recipient))
            .setRecipientSettings(new DefaultRecipientSettings(services, session, originator, recipient, recipientType, updatedResource))
        .build(); // @formatter:on
        LOG.trace("Tracking {}", message);
        tracker.trackSchedulingMessage(message);
    }

    private void trackDeclineCounterMessage(CalendarObjectResource counteredResource, CalendarUser originator, Attendee recipient, String comment) throws OXException {
        //@formatter:off
        SchedulingMessage message = new MessageBuilder()
            .setMethod(SchedulingMethod.DECLINECOUNTER)
            .setOriginator(originator)
            .setRecipient(recipient)
            .setResource(counteredResource)
            .setScheduleChange(describeDeclineCounter(counteredResource, originator, comment))
            .setAttachmentDataProvider(getAttachmentDataProvider())
            .setRecipientSettings(new DefaultRecipientSettings(services, session, originator, recipient, recipient.getCuType(), counteredResource))
        .build();
        LOG.trace("Tracking {}", message);
        tracker.trackSchedulingMessage(message);
        //@formatter:on
    }

    private ScheduleChange describeCreate(CalendarObjectResource createdResource, CalendarUser originator) throws OXException {
        return getSchedulingChangeService().describeCreationRequest(originator, optSchedulingComment(), createdResource);
    }

    private ScheduleChange describeUpdate(CalendarObjectResource updatedResource, Event seriesMaster, List<EventUpdate> eventUpdates, CalendarUser originator, CalendarUser recipient) throws OXException {
        List<Change> changeDescriptions = getChangeDescriptions(eventUpdates);
        if (null != seriesMaster && containsUserOrResourceDelegate(seriesMaster.getAttendees(), recipient)) {
            return getSchedulingChangeService().describeUpdateInstance(originator, optSchedulingComment(), updatedResource, seriesMaster, changeDescriptions);
        }
        return getSchedulingChangeService().describeUpdateRequest(originator, optSchedulingComment(), updatedResource, changeDescriptions);
    }

    private ScheduleChange describeCancel(CalendarObjectResource deletedResource, Event seriesMaster, CalendarUser originator, CalendarUser recipient) throws OXException {
        if (null != seriesMaster && containsUserOrResourceDelegate(seriesMaster.getAttendees(), recipient)) {
            return getSchedulingChangeService().describeCancelInstance(originator, optSchedulingComment(), deletedResource, seriesMaster);
        }
        return getSchedulingChangeService().describeCancel(originator, optSchedulingComment(), deletedResource);
    }

    private ScheduleChange describeReply(CalendarObjectResource updatedResource, List<EventUpdate> attendeeEventUpdates, Event seriesMaster, CalendarUser originator, CalendarUser recipient) throws OXException {
        List<Change> changeDescriptions = getChangeDescriptionsFor(attendeeEventUpdates, EventField.ATTENDEES);
        Attendee matchingAttendee;
        if (null != changeDescriptions && 0 < changeDescriptions.size()) {
            matchingAttendee = extractAttendee(updatedResource, originator, changeDescriptions.get(0));
        } else {
            matchingAttendee = extractAttendee(updatedResource, originator);
        }
        String comment = null != matchingAttendee ? matchingAttendee.getComment() : null;
        ParticipationStatus partStat = null != matchingAttendee ? matchingAttendee.getPartStat() : null;
        if (null != seriesMaster && containsUserOrResourceDelegate(seriesMaster.getAttendees(), recipient)) {
            return getSchedulingChangeService().describeReplyInstance(originator, comment, updatedResource, seriesMaster, changeDescriptions, partStat);
        }
        return getSchedulingChangeService().describeReply(originator, comment, updatedResource, changeDescriptions, partStat);
    }

    private Pair<ScheduleChange, CalendarObjectResource> describeReply(CalendarObjectResource resource, Event seriesMaster, CalendarUser originator, CalendarUser recipient, ParticipationStatus partStat, String comment) throws OXException {
        List<Event> updatedEvents = new ArrayList<Event>();
        List<Change> changeDescriptions = new ArrayList<Change>();
        for (Event event : resource.getEvents()) {
            EventUpdate eventUpdate = overridePartStat(event, originator, partStat, comment);
            changeDescriptions.add(getChangeDescriptionsFor(eventUpdate, EventField.ATTENDEES));
            updatedEvents.add(injectComment(eventUpdate.getUpdate(), comment));
        }
        CalendarObjectResource updatedResource = new DefaultCalendarObjectResource(updatedEvents);
        ScheduleChange change;
        if (null != seriesMaster && containsUserOrResourceDelegate(seriesMaster.getAttendees(), recipient)) {
            change = getSchedulingChangeService().describeReplyInstance(originator, comment, updatedResource, seriesMaster, changeDescriptions, partStat);
        } else {
            change = getSchedulingChangeService().describeReply(originator, comment, updatedResource, changeDescriptions, partStat);
        }
        return new Pair<ScheduleChange, CalendarObjectResource>(change, updatedResource);
    }

    private ScheduleChange describeDeclineCounter(CalendarObjectResource counteredResource, CalendarUser originator, String comment) throws OXException {
        return getSchedulingChangeService().describeDeclineCounter(originator, comment, counteredResource);
    }

    /**
     * Associates attendees of a calendar object resource to those events within the resource they are actually attending, resulting in
     * individual views of the calendar object resource.
     *
     * @param resource The calendar object resource to get the individual views for
     * @param internalOnly <code>true</code> to only consider internal attendees, <code>false</code>, otherwise
     * @return The individual views on the calendar object resource per attendee
     */
    private Map<Attendee, CalendarObjectResource> getResourcesPerAttendee(CalendarObjectResource resource, boolean internalOnly) {
        return getResourcesPerAttendee(resource, internalOnly, null);
    }

    /**
     * Associates attendees of a calendar object resource to those events within the resource they are actually attending, resulting in
     * individual views of the calendar object resource.
     *
     * @param resource The calendar object resource to get the individual views for
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     * @return The individual views on the calendar object resource per attendee
     */
    private Map<Attendee, CalendarObjectResource> getResourcesPerAttendee(CalendarObjectResource resource, Collection<? extends CalendarUser> consideredRecipients) {
        return getResourcesPerAttendee(resource, false, consideredRecipients);
    }

    /**
     * Associates attendees of a calendar object resource to those events within the resource they are actually attending, resulting in
     * individual views of the calendar object resource.
     *
     * @param resource The calendar object resource to get the individual views for
     * @param internalOnly <code>true</code> to only consider internal attendees, <code>false</code>, otherwise
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     * @return The individual views on the calendar object resource per attendee
     */
    private Map<Attendee, CalendarObjectResource> getResourcesPerAttendee(CalendarObjectResource resource, boolean internalOnly, Collection<? extends CalendarUser> consideredRecipients) {
        Map<Integer, List<Event>> eventsPerEntity = new HashMap<Integer, List<Event>>();
        Map<Integer, Attendee> attendeesPerEntity = new HashMap<Integer, Attendee>();
        Map<String, List<Event>> eventsPerUri = new HashMap<String, List<Event>>();
        Map<String, Attendee> attendeesPerUri = new HashMap<String, Attendee>();
        for (Event event : resource.getEvents()) {
            if (null == event.getAttendees()) {
                continue;
            }
            for (Attendee attendee : event.getAttendees()) {
                if (false == attendee.isHidden() && (null == consideredRecipients || contains(consideredRecipients, attendee))) {
                    if (isInternal(attendee)) {
                        com.openexchange.tools.arrays.Collections.put(eventsPerEntity, I(attendee.getEntity()), event);
                        attendeesPerEntity.put(I(attendee.getEntity()), attendee);
                    } else if (false == internalOnly) {
                        com.openexchange.tools.arrays.Collections.put(eventsPerUri, attendee.getUri(), event);
                        attendeesPerUri.put(attendee.getUri(), attendee);
                    }
                }
            }
        }


        Map<Attendee, CalendarObjectResource> resourcesPerUserAttendee = new HashMap<Attendee, CalendarObjectResource>(eventsPerEntity.size() + eventsPerUri.size());
        for (Entry<Integer, List<Event>> entry : eventsPerEntity.entrySet()) {
            resourcesPerUserAttendee.put(attendeesPerEntity.get(entry.getKey()), applyExceptionDates(entry.getValue()));
        }
        for (Entry<String, List<Event>> entry : eventsPerUri.entrySet()) {
            resourcesPerUserAttendee.put(attendeesPerUri.get(entry.getKey()), applyExceptionDates(entry.getValue()));
        }
        return resourcesPerUserAttendee;
    }

    private static CalendarObjectResource applyExceptionDates(List<Event> eventsOfAttendee) {
        CalendarObjectResource resource = new DefaultCalendarObjectResource(eventsOfAttendee);
        Event seriesMaster = resource.getSeriesMaster();
        if (null == seriesMaster || null == seriesMaster.getChangeExceptionDates() || seriesMaster.getChangeExceptionDates().isEmpty()) {
            return resource;
        }
        SortedSet<RecurrenceId> attendedChangeExceptionDates = getRecurrenceIds(resource.getChangeExceptions());
        if (attendedChangeExceptionDates.equals(seriesMaster.getChangeExceptionDates())) {
            return resource;
        }
        Event userizedSeriesMaster = Utils.applyExceptionDates(seriesMaster, attendedChangeExceptionDates);
        return new DefaultCalendarObjectResource(userizedSeriesMaster, resource.getChangeExceptions());
    }

    private String optSchedulingComment() {
        return session.get(CalendarParameters.PARAMETER_COMMENT, String.class);
    }

    private AttachmentDataProvider getAttachmentDataProvider() {
        return new AttachmentDataProvider(services, session.getContextId());
    }

    private SchedulingChangeService getSchedulingChangeService() throws OXException {
        return services.getServiceSafe(SchedulingChangeService.class);
    }

    /**
     * Looks up the effective calendar user matching the currently acting calendar user in a specific calendar object resource:
     * <ul>
     * <li>for <i>organizer scheduling resources</i>, this is the organizer</li>
     * <li>for <i>attendee scheduling resources</i>, this is the matching attendee in the first event of the resource</li>
     * <li>otherwise, this is the currently acting calendar user</li>
     * </ul>
     *
     * @param resource The calendar object resource to determine the effective calendar user for
     * @return The effective calendar user
     */
    private CalendarUser lookupCalendarUser(CalendarObjectResource resource) {
        if (isOrganizerSchedulingResource(resource, calendarUser.getEntity())) {
            return resource.getOrganizer();
        } else if (isAttendeeSchedulingResource(resource, calendarUser.getEntity())) {
            return find(resource.getFirstEvent().getAttendees(), calendarUser);
        } else {
            return calendarUser;
        }
    }

    /**
     * Constructs a calendar user representing the originator of the scheduling message, based on the calendar user in the scheduling
     * resource the action originates from, the underlying folder, and the current session's user.
     * <p/>
     * <i>External</i> calendar user's are used as-is.
     *
     * @param calendarUser The effective calendar user the action originates from, or <code>null</code> to fall back to the actual
     *            calendar user based on the parent folder
     * @return The originator for the resulting scheduling messages
     */
    private CalendarUser getOriginator(CalendarUser calendarUser) throws OXException {
        CalendarUserType cuType = (calendarUser instanceof Attendee) ? ((Attendee) calendarUser).getCuType() : CalendarUserType.INDIVIDUAL;
        CalendarUser originator = null != calendarUser ? new CalendarUser(calendarUser) : getCalendarUser(session, folder);
        if (isInternal(originator, cuType) && session.getUserId() != originator.getEntity()) {
            originator.setSentBy(session.getEntityResolver().applyEntityData(new CalendarUser(), session.getUserId()));
        }
        return originator;
    }

    private boolean shouldTrack(CalendarObjectResource resource) {
        return shouldTrack(resource, null);
    }

    /**
     * Gets a value indicating whether scheduling messages to a particular calendar user should be tracked or not, based
     * on the optional {@link CalendarParameters#PARAMETER_SCHEDULING} parameter, or an explicitly supplied whitelist of recipients.
     *
     * @param calendarUser The calendar user to check
     * @param consideredRecipients The recipients to consider, or <code>null</code> to consider all possible recipients
     * @return <code>true</code> if scheduling messages should be tracked, <code>false</code>, otherwise
     */
    private boolean shouldTrack(CalendarUser calendarUser, Collection<? extends CalendarUser> consideredRecipients) {
        /*
         * don't track if not considered explicitly
         */
        if (null != consideredRecipients && false == contains(consideredRecipients, calendarUser)) {
            LOG.trace("Recipient not considered explicitly, skip tracking of scheduling messages for {}.", calendarUser);
            return false;
        }
        /*
         * don't track if scheduling is forcibly suppressed
         */
        SchedulingControl schedulingControl = session.get(CalendarParameters.PARAMETER_SCHEDULING, SchedulingControl.class);
        if (SchedulingControl.NONE.matches(schedulingControl)) {
            LOG.trace("Scheduling is forcibly suppressed via {}, skip tracking of scheduling messages.", schedulingControl);
            return false;
        }
        if (SchedulingControl.INTERNAL_ONLY.matches(schedulingControl) && false == isInternal(calendarUser, CalendarUserType.INDIVIDUAL)) {
            LOG.trace("Scheduling to non-internal recipients is forcibly suppressed via {}, skip tracking of scheduling messages for {}.", schedulingControl, calendarUser);
            return false;
        }
        if (SchedulingControl.EXTERNAL_ONLY.matches(schedulingControl) && isInternal(calendarUser, CalendarUserType.INDIVIDUAL)) {
            LOG.trace("Scheduling to non-external recipients is forcibly suppressed via {}, skip tracking of scheduling messages for {}.", schedulingControl, calendarUser);
            return false;
        }
        /*
         * do track, otherwise
         */
        return true;
    }

    /**
     * Gets a value indicating whether scheduling messages should be tracked or not, based on the configuration, the
     * tracked events, and the optional parameter {@link CalendarParameters#PARAMETER_SCHEDULING}.
     *
     * @param resource The event resource where scheduling messages are tracked for
     * @param eventUpdates The underlying event update representing the actual changes to inspect in favor of the whole resource,
     *            or <code>null</code> if not applicable
     * @return <code>true</code> if scheduling messages should be tracked, <code>false</code>, otherwise
     */
    private boolean shouldTrack(CalendarObjectResource resource, List<EventUpdate> eventUpdates) {
        /*
         * don't track if scheduling is forcibly suppressed
         */
        SchedulingControl schedulingControl = session.get(CalendarParameters.PARAMETER_SCHEDULING, SchedulingControl.class);
        if (SchedulingControl.NONE.matches(schedulingControl)) {
            LOG.trace("Scheduling is forcibly suppressed via {}, skip tracking of scheduling messages.", schedulingControl);
            return false;
        }
        /*
         * don't track if affected events end in the past
         */
        try {
            if (null != eventUpdates && endsInPast(eventUpdates)) {
                LOG.trace("Actual changes in {} end in past, skip tracking of scheduling messages.", resource);
                return false;
            } else if (endsInPast(resource)) {
                LOG.trace("{} ends in past, skip tracking of scheduling messages.", resource);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Unexpected error checking if events end in the past, assuming they do not.", e);
        }
        /*
         * do track, otherwise
         */
        return true;
    }

    private boolean endsInPast(CalendarObjectResource resource) throws OXException {
        DateTime now = new DateTime(System.currentTimeMillis());
        TimeZone timeZone = CalendarUserType.INDIVIDUAL.matches(calendarUserType) ? session.getEntityResolver().getTimeZone(calendarUser.getEntity()) : TimeZones.UTC;
        for (Event event : resource.getEvents()) {
            if (false == endsInPast(event, now, timeZone)) {
                return false;
            }
        }
        return true;
    }

    private boolean endsInPast(List<EventUpdate> eventUpdates) throws OXException {
        DateTime now = new DateTime(System.currentTimeMillis());
        TimeZone timeZone = CalendarUserType.INDIVIDUAL.matches(calendarUserType) ? session.getEntityResolver().getTimeZone(calendarUser.getEntity()) : TimeZones.UTC;
        for (EventUpdate eventUpdate : eventUpdates) {
            if (false == endsInPast(eventUpdate, now, timeZone)) {
                return false;
            }
        }
        return true;
    }

    private boolean endsInPast(EventUpdate eventUpdate, DateTime now, TimeZone timeZone) throws OXException {
        if (null != eventUpdate.getOriginal() && false == endsInPast(eventUpdate.getOriginal(), now, timeZone)) {
            return false;
        }
        if (null != eventUpdate.getUpdate() && false == endsInPast(eventUpdate.getUpdate(), now, timeZone)) {
            return false;
        }
        return true;
    }

    private boolean endsInPast(Event event, DateTime now, TimeZone timeZone) throws OXException {
        /*
         * check event end time (in given timezone if floating)
         */
        DateTime dtNow = null != now ? now : new DateTime(System.currentTimeMillis());
        DateTime eventEnd = getEndDate(event);
        /*
         * if series master event, check end of recurrence
         */
        if (null != event.getRecurrenceRule() && null == event.getRecurrenceId()) {
            RecurrenceRule rule = initRecurrenceRule(event.getRecurrenceRule());
            if (null != rule.getUntil()) {
                eventEnd = rule.getUntil(); // fixed until
            } else if (null != rule.getCount()) {
                RecurrenceIterator<Event> iterator = session.getRecurrenceService().iterateEventOccurrences(event, null, null);
                while (iterator.hasNext()) {
                    eventEnd = getEndDate(iterator.next());
                    if (eventEnd.after(dtNow) && (false == eventEnd.isFloating() || null == timeZone || getDateInTimeZone(eventEnd, timeZone) > dtNow.getTimestamp())) {
                        break; // this occurrence already ends after now in given timezone
                    }
                }
            } else {
                return false; // infinite recurrence
            }
        }
        if (eventEnd.isFloating() && null != timeZone) {
            return getDateInTimeZone(eventEnd, timeZone) < dtNow.getTimestamp();
        }
        return eventEnd.before(dtNow);
    }

    private boolean isNotifyOnCreate(CalendarUser calendarUser) {
        return session.getConfig().isNotifyOnCreate(calendarUser.getEntity());
    }

    private boolean isNotifyOnUpdate(CalendarUser calendarUser) {
        return session.getConfig().isNotifyOnUpdate(calendarUser.getEntity());
    }

    private boolean isNotifyOnDelete(CalendarUser calendarUser) {
        return session.getConfig().isNotifyOnDelete(calendarUser.getEntity());
    }

    private boolean isNotifyOnReply(CalendarUser calendarUser) {
        return session.getConfig().isNotifyOnReply(calendarUser.getEntity());
    }

    private boolean isNotifyOnReplyAsAttendee(CalendarUser calendarUser) {
        return session.getConfig().isNotifyOnReplyAsAttendee(calendarUser.getEntity());
    }

    private boolean isNotifyResourceAttendees() {
        return session.getConfig().isNotifyResourceAttendees();
    }

    private boolean isActing(CalendarUser calendarUser) {
        return isActing(calendarUser.getEntity());
    }

    private boolean isActing(int userId) {
        return session.getUserId() == userId;
    }

    private boolean isCalendarOwner(CalendarUser calendarUser) {
        return isCalendarOwner(calendarUser.getEntity());
    }

    private boolean isIndividualCalendarOwner(CalendarUser calendarUser, CalendarUserType cuType) {
        return isCalendarOwner(calendarUser) && CalendarUserType.INDIVIDUAL.matches(cuType);
    }

    private boolean isCalendarOwner(int userId) {
        return calendarUser.getEntity() == userId;
    }

    /**
     * Initializes an event update that indicates an updated participation status of the attendee matching a specific calendar user.
     *
     * @param event The event to override the participation status update in
     * @param calendarUser The calendar user to override the participation status for
     * @param partStat The participation status to indicate for the matching attendee
     * @param comment An optional comment from the attendee to indicate
     * @return An event update that indicates the updated participation status accordingly
     */
    private static AttendeeEventUpdate overridePartStat(Event event, CalendarUser calendarUser, ParticipationStatus partStat, String comment) throws OXException {
        Attendee originalAttendee = CalendarUtils.find(event.getAttendees(), calendarUser);
        if (null == originalAttendee) {
            throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(I(calendarUser.getEntity()), event.getId());
        }
        Attendee updatedAttendee = AttendeeMapper.getInstance().copy(originalAttendee, null, (AttendeeField[]) null);
        updatedAttendee.setPartStat(partStat);
        updatedAttendee.setComment(comment);
        updatedAttendee.setTimestamp(event.getTimestamp());
        return new AttendeeEventUpdate(event, originalAttendee, updatedAttendee);
    }

    /**
     * Gets the (first) attendee matching a specific calendar user found in the supplied calendar object resource.
     *
     * @param resource The calendar object resource to get the attendee comment from
     * @param calendarUser The calendar user to lookup the attendee for
     * @return The matching attendee, or <code>null</code> if not set or found
     */
    private static Attendee extractAttendee(CalendarObjectResource resource, CalendarUser calendarUser) {
        for (Event event : resource.getEvents()) {
            Attendee attende = CalendarUtils.find(event.getAttendees(), calendarUser);
            if (null != attende) {
                return attende;
            }
        }
        return null;
    }

    /**
     * Gets the (first) attendee matching a specific calendar user found in the supplied calendar object resource.
     *
     * @param resource The calendar object resource to get the attendee comment from
     * @param calendarUser The calendar user to lookup the attendee for
     * @param changeDescription The change description to prefer the attendee for, or <code>null</code> if not available
     * @return The matching attendee, or <code>null</code> if not set or found
     */
    private static Attendee extractAttendee(CalendarObjectResource resource, CalendarUser calendarUser, Change changeDescription) {
        /*
         * extract attendee from matching change decription if possible
         */
        if (null != changeDescription) {
            Event event = resource.getChangeException(changeDescription.getRecurrenceId());
            if (null != event) {
                Attendee attende = CalendarUtils.find(event.getAttendees(), calendarUser);
                if (null != attende) {
                    return attende;
                }
            }
        }
        /*
         * extract first matching attendee from resource, otherwise
         */
        for (Event event : resource.getEvents()) {
            Attendee attende = CalendarUtils.find(event.getAttendees(), calendarUser);
            if (null != attende) {
                return attende;
            }
        }
        return null;
    }

    private static DateTime getEndDate(Event event) {
        DateTime endDate = event.getEndDate();
        if (null == endDate) {
            endDate = event.getStartDate();
            if (endDate.isAllDay()) {
                endDate = endDate.addDuration(new Duration(1, 1, 0));
            }
        }
        return endDate;
    }

    /**
     * Injects a calendar user to specify the user that is acting on behalf of the organizer.
     * 
     * @param organizer The organizer where the sent-by parameter should be set
     * @param sentBy The calendar user acting on behalf of the organizer to inject
     * @return A new organizer property enriched by the sent-by relationship, or the passed organizer as-is if not applicable
     */
    private static Organizer injectSentBy(Organizer organizer, CalendarUser sentBy) {
        if (null == organizer || null == sentBy && null == organizer.getSentBy()) {
            return organizer;
        }
        Organizer patchedOrganizer = new Organizer(organizer);
        patchedOrganizer.setSentBy(sentBy);
        return patchedOrganizer;
    }

    /**
     * Injects a calendar user to specify the user that is acting on behalf of the attendee.
     * 
     * @param attendee The attendee where the sent-by parameter should be set
     * @param sentBy The calendar user acting on behalf of the attendee to inject
     * @return A new attendee property enriched by the sent-by relationship, or the passed attendee as-is if not applicable
     */
    private static Attendee injectSentBy(Attendee attendee, CalendarUser sentBy) {
        if (null == attendee || null == sentBy && null == attendee.getSentBy()) {
            return attendee;
        }
        Attendee patchedAttendee;
        try {
            patchedAttendee = AttendeeMapper.getInstance().copy(attendee, null, (AttendeeField[]) null);
        } catch (OXException e) {
            throw new IllegalStateException(e);
        }
        patchedAttendee.setSentBy(sentBy);
        return patchedAttendee;
    }

    private static Event injectSentBy(Event event, CalendarUser originator) {
        CalendarUser sentBy = originator.getSentBy();
        ArrayList<Attendee> patchedAttendees;
        Attendee matchingAttendee = find(event.getAttendees(), originator);
        if (null == matchingAttendee) {
            patchedAttendees = null;
        } else {
            patchedAttendees = new ArrayList<Attendee>(event.getAttendees().size());
            for (Attendee attendee : event.getAttendees()) {
                patchedAttendees.add(matches(attendee, matchingAttendee) ? injectSentBy(attendee, sentBy) : attendee);
            }
        }
        Organizer patchedOrganizer = matches(event.getOrganizer(), originator) ? injectSentBy(event.getOrganizer(), sentBy) : null;
        return new DelegatingEvent(event) {

            @Override
            public Organizer getOrganizer() {
                return null != patchedOrganizer ? patchedOrganizer : super.getOrganizer();
            }

            @Override
            public boolean containsOrganizer() {
                return true;
            }

            @Override
            public List<Attendee> getAttendees() {
                return null != patchedAttendees ? patchedAttendees : super.getAttendees();
            }

            @Override
            public boolean containsAttendees() {
                return true;
            }
        };
    }

    private static CalendarObjectResource injectSentBy(CalendarObjectResource resource, CalendarUser originator) {
        if (null == originator || null == originator.getSentBy() || null == resource || null == resource.getEvents()) {
            return resource;
        }
        List<Event> patchedEvents = new ArrayList<Event>(resource.getEvents().size());
        for (Event event : resource.getEvents()) {
            patchedEvents.add(injectSentBy(event, originator));
        }
        return new DefaultCalendarObjectResource(patchedEvents);
    }

    /**
     * Builds change descriptions for a list of event updates.
     *
     * @param eventUpdates The event updates to get the descriptions for
     * @return The change descriptions
     * @see DescriptionService#describe(EventUpdate, EventField...)
     */
    private List<Change> getChangeDescriptions(List<EventUpdate> eventUpdates) throws OXException {
        List<Change> changes = new ArrayList<Change>(eventUpdates.size());
        for (EventUpdate eventUpdate : eventUpdates) {
            changes.add(getChangeDescription(eventUpdate));
        }
        return changes;
    }

    /**
     * Builds change descriptions for an event update.
     *
     * @param eventUpdate The event update to get the description for
     * @return The change description
     * @see DescriptionService#describe(EventUpdate, EventField...)
     */
    private Change getChangeDescription(EventUpdate eventUpdate) throws OXException {
        return new ChangeBuilder()
            .setDescriptions(services.getServiceSafe(DescriptionService.class).describe(eventUpdate))
            .setRecurrenceId(eventUpdate.getUpdate().getRecurrenceId())
        .build();
    }

    /**
     * Builds change descriptions for specific event fields of multiple event updates.
     *
     * @param eventUpdates The event updates to get the descriptions for
     * @param fields The event fields to include in the descriptions
     * @return The change description
     * @see DescriptionService#describeOnly(EventUpdate, EventField...)
     */
    private List<Change> getChangeDescriptionsFor(List<EventUpdate> eventUpdates, EventField... fields) throws OXException {
        List<Change> changes = new ArrayList<Change>(eventUpdates.size());
        for (EventUpdate eventUpdate : eventUpdates) {
            changes.add(getChangeDescriptionsFor(eventUpdate, fields));
        }
        return changes;
    }

    /**
     * Builds change descriptions for specific event fields of an event update.
     *
     * @param eventUpdate The event update to get the description for
     * @param fields The event fields to include in the description
     * @return The change description
     * @see DescriptionService#describeOnly(EventUpdate, EventField...)
     */
    private Change getChangeDescriptionsFor(EventUpdate eventUpdate, EventField... fields) throws OXException {
        return new ChangeBuilder()
            .setDescriptions(services.getServiceSafe(DescriptionService.class).describeOnly(eventUpdate, fields))
            .setRecurrenceId(eventUpdate.getUpdate().getRecurrenceId())
        .build();
    }

    private CalendarUser prepareDelegateOriginator(Attendee resourceAttendee, CalendarUser originator) throws OXException {
        Attendee calendarUser = AttendeeMapper.getInstance().copy(resourceAttendee, null, (AttendeeField[]) null);
        calendarUser.setSentBy(originator);
        return calendarUser;
    }

    private Attendee prepareDelegateRecipient(Attendee resourceAttendee, int bookingDelegate) throws OXException {
        Attendee calendarUser = session.getEntityResolver().prepareUserAttendee(bookingDelegate);
        calendarUser.setSentBy(resourceAttendee);
        return calendarUser;
    }

    /**
     * Get the booking delegates to send a message to, implicitly skipping the currently acting user.
     *
     * @param recipient The resource attendee to resolve the delegates for
     * @return The booking delegates
     * @throws OXException In case delegates can't be resolved
     */
    private int[] getOtherBookingDelegates(Attendee recipient) throws OXException {
        int[] bookingDelegates = session.getEntityResolver().getBookingDelegates(recipient.getEntity());
        if (Arrays.contains(bookingDelegates, session.getUserId())) {
            return Arrays.remove(bookingDelegates, session.getUserId());
        }
        return bookingDelegates;
    }

    /**
     * Prepares a delegating event upon the supplied one which indicates an extended property named <code>COMMENT</code> with the given
     * value.
     * <p/>
     * This is usually used to transport an additional comment from the scheduling message's originator to the recipient, e.g. from
     * attendee to organizer or vice-versa.
     * 
     * @param delegate The underlying event to decorate with the comment
     * @param comment The comment to inject
     * @return A delegating event decorated with the comment property, or the passed event reference if the comment was empty
     * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.8.1.4">RFC 5545, section 3.8.1.4</a>
     */
    private static Event injectComment(Event delegate, String comment) {
        if (Strings.isEmpty(comment)) {
            return delegate;
        }
        return new DelegatingEvent(delegate) {

            @Override
            public boolean containsExtendedProperties() {
                return true;
            }

            @Override
            public ExtendedProperties getExtendedProperties() {
                return addExtendedProperty(super.getExtendedProperties(), new ExtendedProperty("COMMENT", comment), true);
            }
        };
    }

    /**
     * Prepares a new calendar object resource based on delegating events upon the supplied one which indicate an extended property
     * named <code>COMMENT</code> with the value found in the calendar parameters.
     * <p/>
     * This is usually used to transport an additional comment from the scheduling message's originator to the recipient, e.g. from
     * attendee to organizer or vice-versa.
     * 
     * @param objectResource The calendar object resource whose events to decorate with the comment
     * @return A new calendar object resource with the comment properties, or the passed object resource reference if the comment was empty
     * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.8.1.4">RFC 5545, section 3.8.1.4</a>
     * @see #optSchedulingComment()
     */
    private CalendarObjectResource injectComment(CalendarObjectResource objectResource) {
        return injectComment(objectResource, optSchedulingComment());
    }

    /**
     * Prepares a new calendar object resource based on delegating events upon the supplied one which indicate an extended property
     * named <code>COMMENT</code> with the given value.
     * <p/>
     * This is usually used to transport an additional comment from the scheduling message's originator to the recipient, e.g. from
     * attendee to organizer or vice-versa.
     * 
     * @param objectResource The calendar object resource whose events to decorate with the comment
     * @param comment The comment to inject
     * @return A new calendar object resource with the comment properties, or the passed object resource reference if the comment was empty
     * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.8.1.4">RFC 5545, section 3.8.1.4</a>
     */
    private static CalendarObjectResource injectComment(CalendarObjectResource objectResource, String comment) {
        if (Strings.isEmpty(comment)) {
            return objectResource;
        }
        List<Event> adjustedEvents = new ArrayList<Event>(objectResource.getEvents().size());
        for (Event event : objectResource.getEvents()) {
            adjustedEvents.add(injectComment(event, comment));
        }
        return new DefaultCalendarObjectResource(adjustedEvents);
    }

    /**
     * Gets a value indicating whether a specific calendar user is present in a collection of calendar users. A user is present if
     * <li>the calendar user is found via {@link CalendarUtils#contains(Collection, CalendarUser)}</li>
     * <li>the calendar user is a resource delegate as per {@link #prepareDelegateRecipient(Attendee, int)} and the resource is found
     * within the list of attendees</li>
     *
     * @param calendarUsers The calendar users to search
     * @param calendarUser The calendar user to lookup
     * @return <code>true</code> if the calendar user is contained in the collection of attendees, <code>false</code>, otherwise
     * @see CalendarUtils#contains(Collection, CalendarUser)
     * @see #prepareDelegateRecipient(Attendee, int)
     */
    private static boolean containsUserOrResourceDelegate(List<Attendee> attendees, CalendarUser calendarUser) {
        if (null == calendarUser) {
            return false;
        }
        /*
         * Check if user is contained
         */
        if (contains(attendees, calendarUser)) {
            return true;
        }
        /*
         * Check if the user is a resource delegate and the resource is contained in the attendees instead
         */
        CalendarUser sentBy = calendarUser.getSentBy();
        if (null == sentBy || sentBy.getEntity() <= 0) {
            return false;
        }
        return filter(attendees, Boolean.TRUE, CalendarUserType.RESOURCE, CalendarUserType.ROOM).stream()//Filter for internal resources
            .filter(a -> a.getEntity() == sentBy.getEntity()).findAny().isPresent();
    }

}
