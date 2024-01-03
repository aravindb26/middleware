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

package com.openexchange.pubsub;

import java.util.UUID;

/**
 * {@link Message} - A received message via a channel.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public interface Message<D> {

    /**
     * Whether this message has its origin from a remote sender.
     *
     * @return <code>true</code> if remote origin; otherwise <code>false</code>
     */
    boolean isRemote();

    /**
     * Gets this message's sender identifier,
     *
     * @return The sender identifier
     */
    UUID getSenderId();

    /**
     * Gets the message's data.
     *
     * @return The message's data
     */
    D getData();

}
