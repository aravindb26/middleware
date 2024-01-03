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

package com.openexchange.chronos.impl.scheduling;

import java.util.List;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.performer.ResolvePerformer;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.tools.functions.ErrorAwareBiFunction;

/**
 * {@link SchedulingUtils}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class SchedulingUtils {

    private SchedulingUtils() {
        super();
    }

    /**
     * Check if originator is the original organizer
     * 
     * @param folder The calendar folder
     * @param originalEvent The original event to get the organizer from
     * @param updatedEvent The event to update
     * @param originator The originator of a scheduling action
     * @throws OXException In case originator isn't allowed to perform the action
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6047#section-3">RFC 6037 Section 3</a>
     */
    public static void validateOrganizer(CalendarFolder folder, Event originalEvent, Event updatedEvent, CalendarUser originator) throws OXException {
        Organizer organizer = originalEvent.getOrganizer();
        if (CalendarUtils.matches(originator, organizer) && CalendarUtils.matches(updatedEvent.getOrganizer(), organizer)) {
            return;//perfect match 
        }
        /*
         * XXX RFC recommends to check against "trusted <sent-by, organizer> proxies" or leave the decision to the user.
         */
        //        if ((null != organizer.getSentBy() && CalendarUtils.matches(originator, organizer.getSentBy()))) {
        //            return;
        //        }
        throw CalendarExceptionCodes.NOT_ORGANIZER.create(folder.getId(), originalEvent.getId(), originator.getUri(), originator.getCn());
    }

    /**
     * Gets a value indicating whether the supplied scheduling message originates from an <i>internal</i> notification mail of the given
     * context, or from a regular, <i>external</i> iTIP message or another context.
     * 
     * @param session The calendar session
     * @param message The incoming scheduling message to check
     * @return <code>true</code> if the message originates from an internal notification mail, <code>false</code>, otherwise
     */
    public static boolean isInternalSchedulingResource(CalendarSession session, IncomingSchedulingMessage message) {
        return com.openexchange.chronos.scheduling.common.Utils.isInternalSchedulingResource(session, message);
    }

    /**
     * Gets a value indicating whether the supplied scheduling message originates from an <i>internal</i> notification mail of the given
     * context, and if this message is targeting the (already stored) organizer copy.
     * <p/>
     * This is usually the case for most scheduling messages sent from/to calendar users within the same context, however, there are certain
     * exceptions which are checked in this method, which lead to an additional, detached attendee copy of the scheduling object resource:
     * <ul>
     * <li>The target (calendar) user of the message has no or had no calendar access</li>
     * <li>An uninvited user added a forwarded invitation to his calendar</li>
     * </ul>
     * 
     * @param session The calendar session
     * @param storage A reference to the calendar storage to use
     * @param message The incoming scheduling message to evaluate
     * @return <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>, if the
     *         scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @see #isInternalSchedulingResource(CalendarSession, IncomingSchedulingMessage)
     */
    public static boolean usesOrganizerCopy(CalendarSession session, CalendarStorage storage, IncomingSchedulingMessage message) {
        ErrorAwareBiFunction<String, Organizer, CalendarObjectResource> storedResourceFunction = (uid, organizer) -> {
            List<Event> events = new ResolvePerformer(session, storage).lookupByUid(uid, organizer.getEntity(), EventField.ATTENDEES);
            return null != events ? new DefaultCalendarObjectResource(events) : null;
        };
        return com.openexchange.chronos.scheduling.common.Utils.usesOrganizerCopy(session, message, storedResourceFunction);
    }

}
