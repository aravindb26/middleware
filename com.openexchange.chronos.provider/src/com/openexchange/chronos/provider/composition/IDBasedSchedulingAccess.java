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

package com.openexchange.chronos.provider.composition;

import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.exception.OXException;
import com.openexchange.tx.TransactionAware;

/**
 * {@link IDBasedSchedulingAccess}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public interface IDBasedSchedulingAccess extends TransactionAware, CalendarParameters {

    /**
     * Create a patched scheduling messages based on the given mail
     *
     * @param accountId The mail account identifier
     * @param folderId The folder identifier the mail is in
     * @param mailId The mail identifier
     * @param sequenceId The sequence identifier of the mails attachment the iCAL is in, can be <code>null</code>
     * @return A parsed and patched message
     * @throws OXException In case message can't be parsed
     */
    IncomingSchedulingMessage createPatchedMessage(int accountId, String folderId, String mailId, String sequenceId) throws OXException;

    /**
     * Analysis the given message
     *
     * @param message The message to analyze
     * @return The analysis result as {@link ITipAnalysis}
     * @throws OXException In case message can't be analyzed
     */
    ITipAnalysis analyze(IncomingSchedulingMessage message) throws OXException;

    /**
     * Handles the incoming scheduling messages and applies it to the calendar
     *
     * @param source The API source the scheduling is performed from
     * @param message The message to process
     * @return A calendar result of the operation
     * @throws OXException In case the message can't be processed
     */
    default CalendarResult handleIncomingScheduling(SchedulingSource source, IncomingSchedulingMessage message) throws OXException {
        return handleIncomingScheduling(source, message, null);
    }

    /**
     * Handles the incoming scheduling messages and applies it to the calendar
     *
     * @param source The API source the scheduling is performed from
     * @param message The message to process
     * @param attendee The attendee to update along the operation, can be <code>null</code>
     * @return A calendar result of the operation
     * @throws OXException In case the message can't be processed
     */
    CalendarResult handleIncomingScheduling(SchedulingSource source, IncomingSchedulingMessage message, Attendee attendee) throws OXException;

}
