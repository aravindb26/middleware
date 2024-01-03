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

import com.openexchange.chronos.Event;

/**
 * {@link RecurrenceInfo}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public interface RecurrenceInfo {

    /**
     * Gets a value indicating whether the event recurrence was <i>overridden</i>, hence it represents a change exception event or not.
     * 
     * @return <code>true</code> if the recurrence was overridden, <code>false</code>, otherwise
     */
    boolean isOverridden();

    /**
     * Gets a value indicating whether the event recurrence was <i>re-scheduled</i>, hence it represents a change exception event, and it
     * was changed in a substantial way compared to its original occurrence.
     * 
     * @return <code>true</code> if the recurrence was re-scheduled, <code>false</code>, otherwise
     */
    boolean isRescheduled();

    /**
     * Gets the event recurrence, which is either the change exception event in case it was <i>overridden</i>, or the regular event
     * occurrence if not.
     * 
     * @return The event recurrence
     */
    Event getRecurrence();

    /**
     * Gets the series master event. May not be set if not available (e.g. for <i>orphaned</i> change exceptions).
     * 
     * @return The series master event, or <code>null</code> if none is available
     */
    Event getSeriesMaster();

}
