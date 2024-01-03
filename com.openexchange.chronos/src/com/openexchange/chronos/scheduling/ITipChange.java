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

import java.util.List;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.service.EventConflict;

/**
 * 
 * {@link ITipChange}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public interface ITipChange {

    /**
     * {@link Type} Different types a change is about
     *
     * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
     * @since v7.10.0
     */
    public static enum Type {
        /** The change is about a new and yet to be created event */
        CREATE,
        /** The change is about an update of an known event */
        UPDATE,
        /** The change is about an known event, which shall be deleted */
        DELETE,
        /** The change is about the deletion of specific event occurrences */
        CREATE_DELETE_EXCEPTION,
        ;
    }

    /**
     * Gets the {@link Type}
     *
     * @return the type or <code>null</code> if not set
     */
    Type getType();

    /**
     * Get the new or updated event data
     *
     * @return The new or updated {@link Event} or <code>null</code>
     */
    Event getNewEvent();

    /**
     * Get the currently stored event as persisted in the DB or in case of an event occurrence, as calculated based on the master event
     *
     * @return The current event or <code>null</code>
     */
    Event getCurrentEvent();

    /**
     * Get conflicts between the new and existing events
     *
     * @return The conflicts
     */
    List<EventConflict> getConflicts();

    /**
     * Get the event that is going to be deleted
     *
     * @return The deleted event
     */
    Event getDeletedEvent();

}
