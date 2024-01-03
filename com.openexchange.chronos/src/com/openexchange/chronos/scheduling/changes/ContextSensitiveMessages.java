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

package com.openexchange.chronos.scheduling.changes;

import com.openexchange.chronos.ParticipationStatus;

/**
 * {@link ContextSensitiveMessages} - Generates a context sensitive message
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.8.1
 */
public interface ContextSensitiveMessages {

    /**
     * {@link Context}
     *
     * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
     * @since v7.8.1
     */
    public enum Context {
        /** If the messages attribute is a verb */
        VERB,
        /** If the messages attribute is an adjective */
        ADJECTIVE;
    }

    /**
     * Generates a message describing an <code>accepted</code> event
     *
     * @param ctxt The context to put the message in
     * @return The message
     */
    String accepted(Context ctxt);

    /**
     * Generates a message describing an <code>declined</code> event
     *
     * @param ctxt The context to put the message in
     * @return The message
     */
    String declined(Context ctxt);

    /**
     * Generates a message describing an <code>tentetive accepted</code> event
     *
     * @param ctxt The context to put the message in
     * @return The message
     */
    String tentative(Context ctxt);

    /**
     * Describes the participant status
     *
     * @param status The status
     * @param ctxt The context to put the message in
     * @return The message
     */
    String partStat(ParticipationStatus status, Context ctxt);

}
