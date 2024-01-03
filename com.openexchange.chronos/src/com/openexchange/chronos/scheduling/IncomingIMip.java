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

import com.openexchange.annotation.Nullable;

/**
 * {@link IncomingIMip}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public interface IncomingIMip extends IncomingSchedulingObject {

    /**
     * Gets the mail account identifier
     *
     * @return The mail account identifier
     */
    String getMailAccountId();

    /**
     * Gets the identifier of the mail folder the iMIP mail was received in
     *
     * @return The folder identifier
     */
    String getMailFolderId();

    /**
     * Gets the identifier of the iMIP mail
     *
     * @return The mail identifier
     */
    String getMailId();

    /**
     * Gets the unique message identifier of the iMIP mail
     *
     * @return The unique message identifier of the mail
     */
    String getMessageId();

    /**
     * Get the iTIP state of the mail
     *
     * @return The state or <code>null</code>
     */
    @Nullable
    MessageStatus getState();
}
