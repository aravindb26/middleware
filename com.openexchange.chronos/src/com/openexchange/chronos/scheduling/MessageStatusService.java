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

import com.openexchange.annotation.NonNull;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;

/**
 * {@link MessageStatusService} - Service that handles the message status information for an incoming scheduling message
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
@SingletonService
public interface MessageStatusService {

    /**
     * Sets a specific state for the given message
     * 
     * @param session The calendar session of the user
     * @param message The message to mark
     * @param status The status of the message
     * @throws OXException In case of error
     */
    void setMessageStatus(Session session, IncomingSchedulingMessage message, MessageStatus status) throws OXException;

    /**
     * Gets the status for the incoming scheduling message
     * 
     * @param message The incoming message
     * @return The state of the message describes as {@link com.openexchange.chronos.scheduling.MessageStatus} or <code>null</code> if not set for the message
     */
    public @NonNull MessageStatus getMessageStatus(IncomingSchedulingMessage message);

}
