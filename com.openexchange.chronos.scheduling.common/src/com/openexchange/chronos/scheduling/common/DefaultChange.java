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

package com.openexchange.chronos.scheduling.common;

import java.util.List;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.scheduling.ITipChange;
import com.openexchange.chronos.service.EventConflict;

/**
 * 
 * {@link DefaultChange}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class DefaultChange implements ITipChange {

    private Type type;

    private Event currentEvent;

    private Event newEvent;

    private List<EventConflict> conflicts;

    private Event deleted;

    /**
     * Set the type
     *
     * @param type The {@link com.openexchange.chronos.scheduling.ITipChange.Type}
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Gets the {@link com.openexchange.chronos.scheduling.ITipChange.Type}
     *
     * @return the type or <code>null</code> if not set
     */
    @Override
    public Type getType() {
        return type;
    }

    /**
     * Get the new or updated event data
     *
     * @return The new or updated {@link Event} or <code>null</code>
     */
    @Override
    public Event getNewEvent() {
        return newEvent;
    }

    /**
     * Sets the new or updated event data
     *
     * @param newEvent The {@link Event}
     */
    public void setNewEvent(Event newEvent) {
        this.newEvent = newEvent;
    }

    /**
     * Get the currently stored event as persisted in the DB or in case of an event occurrence, as calculated based on the master event
     *
     * @return The current event or <code>null</code>
     */
    @Override
    public Event getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Sets the current event as saved in the DB
     *
     * @param currentEvent The current event
     */
    public void setCurrentEvent(Event currentEvent) {
        this.currentEvent = currentEvent;
    }

    /**
     * Get conflicts between the new and existing events
     *
     * @return The conflicts
     */
    @Override
    public List<EventConflict> getConflicts() {
        return conflicts;
    }

    /**
     * Set conflicts between the new and existing events
     *
     * @param conflicts The conflicts to set
     */
    public void setConflicts(List<EventConflict> conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Get the event that is going to be deleted
     *
     * @return The deleted event
     */
    @Override
    public Event getDeletedEvent() {
        return deleted;
    }

    /**
     * Set the event that is going to be deleted
     *
     * @param deleted The deleted event
     */
    public void setDeleted(Event deleted) {
        this.deleted = deleted;
    }

}
