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

package com.openexchange.chronos.common;

import com.openexchange.chronos.Event;
import com.openexchange.chronos.service.RecurrenceInfo;

/**
 * {@link DefaultRecurrenceInfo}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DefaultRecurrenceInfo implements RecurrenceInfo {

    private final boolean overridden;
    private final boolean rescheduled;
    private final Event masterEvent;
    private final Event recurrenceEvent;

    /**
     * Initializes a new {@link DefaultRecurrenceInfo}.
     * 
     * @param overridden <code>true</code> if the recurrence was overridden, <code>false</code>, otherwise
     * @param rescheduled <code>true</code> if the recurrence was re-scheduled, <code>false</code>, otherwise
     * @param masterEvent The series master event, or <code>null</code> if none is available
     * @param recurrenceEvent The event recurrence
     */
    public DefaultRecurrenceInfo(boolean overridden, boolean rescheduled, Event masterEvent, Event recurrenceEvent) {
        super();
        this.overridden = overridden;
        this.rescheduled = rescheduled;
        this.masterEvent = masterEvent;
        this.recurrenceEvent = recurrenceEvent;
    }

    @Override
    public boolean isOverridden() {
        return overridden;
    }

    @Override
    public boolean isRescheduled() {
        return rescheduled;
    }

    @Override
    public Event getRecurrence() {
        return recurrenceEvent;
    }

    @Override
    public Event getSeriesMaster() {
        return masterEvent;
    }

    @Override
    public String toString() {
        return "DefaultRecurrenceInfo [overridden=" + overridden + ", rescheduled=" + rescheduled + ", masterEvent=" + masterEvent + ", recurrenceEvent=" + recurrenceEvent + "]";
    }

}
