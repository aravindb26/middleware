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

import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.isAttendee;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isInternalUser;
import static com.openexchange.chronos.common.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.isPublicClassification;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.impl.Utils.isBookingDelegate;
import static com.openexchange.chronos.impl.Utils.isParticipating;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.includeForFreeBusy;
import java.util.List;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link AbstractFreeBusyPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class AbstractFreeBusyPerformer extends AbstractQueryPerformer {

    /**
     * Initializes a new {@link AbstractFreeBusyPerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     */
    protected AbstractFreeBusyPerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Reads the attendee data from storage
     *
     * @param events The events to load
     * @param internal whether to only consider internal attendees or not
     * @return The {@link Event}s containing the attendee data
     * @throws OXException
     */
    protected List<Event> readAttendeeData(List<Event> events, Boolean internal) throws OXException {
        return FreeBusyPerformerUtil.readAttendeeData(events, internal, storage);
    }

    /**
     * Gets the timezone to consider for <i>floating</i> dates of a specific attendee.
     * <p/>
     * For <i>internal</i>, individual calendar user attendees, this is the configured timezone of the user; otherwise, the timezone of
     * the current session's user is used.
     *
     * @param attendee The attendee to get the timezone to consider for <i>floating</i> dates for
     * @return The timezone
     */
    protected TimeZone getTimeZone(Attendee attendee) throws OXException {
        if (isInternal(attendee) && CalendarUserType.INDIVIDUAL.equals(attendee.getCuType())) {
            return session.getEntityResolver().getTimeZone(attendee.getEntity());
        }
        return Utils.getTimeZone(session);
    }

    /**
     * Gets a value indicating whether a certain event is visible or <i>opaque to</i> free/busy results in the view of the current
     * session's user or not.
     *
     * @param event The event to check
     * @return <code>true</code> if the event should be considered, <code>false</code>, otherwise
     */
    protected boolean considerForFreeBusy(Event event) {
        String maskUid = session.get(CalendarParameters.PARAMETER_MASK_UID, String.class);
        if (null != maskUid && maskUid.equals(event.getUid())) {
            return false;
        }

        // exclude foreign events classified as 'private' (but keep 'confidential' ones)
        int userId = session.getUserId();
        return isPublicClassification(event) || Classification.CONFIDENTIAL.equals(event.getClassification()) ||
            matches(event.getCalendarUser(), userId) || isOrganizer(event, userId) || isAttendee(event, userId);
    }

    /**
     * Optionally extracts a resource attendee from an event where the current user acts as booking delegate for.
     * 
     * @param event The event to get the resource attendee from
     * @return The resource attendee, or <code>null</code> if no attendee where the user acts as booking delegate for was found
     */
    protected Attendee optResourceAttendeeWithDelegatePrivilege(Event event) {
        for (Attendee resourceAttendee : filter(event.getAttendees(), Boolean.TRUE, CalendarUserType.RESOURCE, CalendarUserType.ROOM)) {
            try {
                if (isBookingDelegate(session, resourceAttendee.getEntity())) {
                    return resourceAttendee;
                }
            } catch (OXException e) {
                session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(e, "Unexpected error checking scheduling privileges for resource"));
            }
        }
        return null;
    }

    /**
     * Gets a value indicating whether a certain event of a specific internal attendee is visible or <i>opaque to</i> free/busy
     * results in the view of the current session's user or not.
     * <p/>
     * Only applicable for context-internal events/attendees.
     * 
     * @param event The event to check
     * @param attendee The attendee to check
     * @return <code>true</code> if the event should be considered, <code>false</code>, otherwise
     */
    protected boolean considerForFreeBusy(Event event, Attendee attendee) {
        /*
         * exclude if event uid is ignored
         */
        String maskUid = session.get(CalendarParameters.PARAMETER_MASK_UID, String.class);
        if (null != maskUid && maskUid.equals(event.getUid())) {
            return false;
        }
        /*
         * exclude if checked attendee does not attend, or if attendee's participation status denotes a non-opaque transparency
         */
        if (false == includeForFreeBusy(event, attendee)) {
            return false;
        }
        /*
         * always include events where the session user participates, regardless of restricted event visibility
         */
        if (isParticipating(session.getUserId(), event)) {
            return true;
        }
        /*
         * exclude events classified as 'private'
         */
        if (Classification.PRIVATE.equals(event.getClassification())) {
            return false;
        }
        /*
         * exclude inaccessible events if attendee's configured free/busy visibility is concealed
         */
        if (isInternalUser(attendee)) {
            try {
                if (FreeBusyVisibility.NONE.equals(session.getConfig().getFreeBusyVisibility(attendee.getEntity()))) {
                    CalendarFolder folder = getFolderChooser().chooseFolder(event);
                    if (null == folder || false == Utils.isVisible(folder, event)) {
                        return false;
                    }
                }
            } catch (OXException e) {
                session.addWarning(e);
                return false;
            }
        }
        /*
         * include, otherwise
         */
        return true;
    }

}
