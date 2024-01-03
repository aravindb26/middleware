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

package com.openexchange.chronos.service;

import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * {@link SchedulingUtilities}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
@SingletonService
public interface SchedulingUtilities {

    /**
     * Processes a {@link SchedulingMethod#ADD} and creates series exceptions
     *
     * @param calendarSession The calendar session
     * @param source The source from which the scheduling has been triggered
     * @param message The message to process
     * @param attendee The attendee to update or <code>null</code> to just apply the changes
     * @return A userized calendar result of the update
     * @throws OXException if adding fails
     */
    CalendarResult processAdd(CalendarSession calendarSession, SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException;

    /**
     * Processes a {@link SchedulingMethod#CANCEL} and removes the event(s)
     *
     * @param calendarSession The calendar session
     * @param source The source from which the scheduling has been triggered
     * @param message The message to process
     * @return A userized calendar result of the update
     * @throws OXException if updating fails
     */
    CalendarResult processCancel(CalendarSession calendarSession, SchedulingSource source, IncomingSchedulingMessage message) throws OXException;

    /**
     * Processes a {@link SchedulingMethod#REPLY} and updates the event(s)
     *
     * @param calendarSession The calendar session
     * @param source The source from which the scheduling has been triggered
     * @param message The message to process
     * @return A userized calendar result of the update
     * @throws OXException if updating fails
     */
    CalendarResult processReply(CalendarSession calendarSession, SchedulingSource source, IncomingSchedulingMessage message) throws OXException;

    /**
     * Processes a {@link SchedulingMethod#REQUEST} and create or updates the event(s)
     *
     * @param calendarSession The calendar session
     * @param source The source from which the scheduling has been triggered
     * @param message The message to process
     * @param attendee The attendee to update or <code>null</code> to just apply the changes
     * @return A userized calendar result of the update
     * @throws OXException if updating fails
     */
    CalendarResult processRequest(CalendarSession calendarSession, SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException;

    /**
     * Processes a {@link SchedulingMethod#REFRESH} and create or updates the event(s)
     *
     * @param calendarSession The calendar session
     * @param message The message to process
     * @return A userized calendar result of the update
     * @throws OXException if updating fails
     */
    CalendarResult processRefresh(CalendarSession calendarSession, IncomingSchedulingMessage message) throws OXException;

    /**
     * Processes a {@link SchedulingMethod#COUNTER} either by either accepting the proposed changes and sending updated
     * <code>REQUEST</code> messages to the attendees, or by declining the changes and sending a <code>DECLINECOUNTER</code> message to
     * the countering attendee.
     *
     * @param calendarSession The calendar session
     * @param source The source from which the scheduling has been triggered
     * @param message The message to process
     * @param decline <code>true</code> to decline the counter proposal, <code>false</code> to accept it
     * @return A userized calendar result of the update
     * @throws OXException if processing fails
     */
    CalendarResult processCounter(CalendarSession calendarSession, SchedulingSource source, IncomingSchedulingMessage message, boolean decline) throws OXException;

}
