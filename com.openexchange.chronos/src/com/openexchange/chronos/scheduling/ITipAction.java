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

package com.openexchange.chronos.scheduling;

/**
 * {@link ITipAction}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public enum ITipAction {

    /**
     * Cases:
     * 1. Create Appointment if not existing
     * 2. Set user accepted
     * 3. Add user to existing appointment if not participant (rights?!)
     * 4. If Attendee: Answer with REPLY
     */
    ACCEPT,

    /**
     * see accept
     * (implicit ignore conflicts)
     */
    DECLINE,

    /**
     * see accept
     * (implicit ignore conflicts)
     */
    TENTATIVE,

    /**
     * see accept with "ignore conflicts"
     */
    ACCEPT_AND_IGNORE_CONFLICTS,

    /**
     * Ignores the mail and avoids to analyze the mail again
     */
    IGNORE,

    /**
     * As attendee, trigger a <code>REFRESH</code> message to the organizer to ask for an updated event copy.
     */
    REQUEST_REFRESH,

    /**
     * As organizer, trigger a <code>REQUEST</code> message with an updated event copy to an attendee.
     */
    SEND_REFRESH,

    /**
     * for organizer:
     * add participant
     * Send a REQUEST mail
     */
    ACCEPT_PARTY_CRASHER,

    /**
     * just mail
     * Send a DECLINECOUNTER mail
     */
    DECLINECOUNTER,

    /**
     * As organizer, apply an incoming <code>COUNTER</code> message and update the targeted appointment(s) in the calendar.
     */
    APPLY_PROPOSAL,

    /**
     * As attendee, take over an incoming <code>CANCEL</code> message and remove the targeted appointment(s) from the calendar.
     */
    APPLY_REMOVE,

    /**
     * As organizer, store an incoming <code>REPLY</code> message for the targeted appointment(s) in the calendar.
     */
    APPLY_RESPONSE,

    /**
     * As attendee, apply an incoming <code>REQUEST</code> message and create the appointment(s) in the calendar.
     */
    APPLY_CREATE,

    /**
     * As attendee, take over an incoming <code>REQUEST</code> message and update the targeted appointment(s) in the calendar.
     */
    APPLY_CHANGE,

    ;
}
