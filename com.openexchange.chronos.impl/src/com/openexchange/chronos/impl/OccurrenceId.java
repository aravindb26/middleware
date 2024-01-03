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

package com.openexchange.chronos.impl;

import com.openexchange.chronos.Event;
import com.openexchange.chronos.RecurrenceId;

/**
 * {@link OccurrenceId}
 * 
 * Used as uid / recurrence id key for events in different contexts.
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public final class OccurrenceId {

    private final String uid;
    private final RecurrenceId recurrenceId;
    private final int hashCode;

    /**
     * Initializes a new {@link OccurrenceId}.
     *
     * @param event The event to generate the ID for
     */
    public OccurrenceId(Event event) {
        this(event.getUid(), event.getRecurrenceId());
    }

    /**
     * Initializes a new {@link OccurrenceId}.
     *
     * @param uid The unique identifier of the event
     * @param recurrenceId The recurrence identifier for this ID
     */
    public OccurrenceId(String uid, RecurrenceId recurrenceId) {
        super();
        this.uid = uid;
        this.recurrenceId = recurrenceId;
        this.hashCode = _hashCode();
    }

    private int _hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((recurrenceId == null) ? 0 : recurrenceId.hashCode());
        result = prime * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OccurrenceId other = (OccurrenceId) obj;
        if (recurrenceId == null) {
            if (other.recurrenceId != null)
                return false;
        } else if (!recurrenceId.equals(other.recurrenceId))
            return false;
        if (uid == null) {
            if (other.uid != null)
                return false;
        } else if (!uid.equals(other.uid))
            return false;
        return true;
    }

}
