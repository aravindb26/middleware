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

import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.groupware.tools.mappings.common.ItemUpdate;

/**
 * {@link ITipEventUpdate}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public interface ITipEventUpdate extends EventUpdate, ItemUpdate<Event, EventField> {

    /**
     * Gets a value indicating whether the underlying event update has <b>all</b> of the given event fields updated.
     *
     * @param fields The fields to check
     * @return <code>true</code> if <b>all</b> fields has been changed, <code>false</code> otherwise
     */
    boolean containsAllChangesOf(EventField[] fields);

    /**
     * Gets a value indicating whether the underlying event update has <b>any changes besides</b> of the given event fields updated.
     *
     * @param fields The fields to check
     * @return <code>true</code> if there are more changes to the event then the given fields, <code>false</code> otherwise
     */
    boolean containsAnyChangesBeside(EventField[] fields);

    /**
     * Gets a value indicating whether the underlying event update has <b>exactly the same changes</b> of the given event fields updated.
     *
     * @param fields The fields to check
     * @return <code>true</code> if <b>exactly</b> the same fields has been changed, <code>false</code> otherwise
     */
    boolean containsExactTheseChanges(EventField[] fields);

    /**
     * Gets a value indicating whether only the given event fields has been changed or not 
     *
     * @param fields The fields to check
     * @return <code>true</code> of only the given fields has been changed, <code>false</code> otherwise
     */
    boolean containsOnlyChangeOf(EventField[] fields);

    /**
     * Checks if the event diff contains <b>only</b> state changes
     *
     * @return <code>true</code> for <b>only</b> state changes; otherwise <code>false</code>
     */
    boolean isAboutStateChangesOnly();

    /**
     * Checks if the event diff contains <b>only</b> state changes beside relevant fields.
     *
     * @param fields The fields to check
     * @return <code>true</code> for <b>only</b> state changes; otherwise <code>false</code>
     */
    boolean isAboutStateChangesOnly(EventField[] fields);

    /**
     * Checks if the event diff contains any state changes
     *
     * @return <code>true</code> for any state changes; otherwise <code>false</code>
     */
    boolean isAboutStateChanges();

    /**
     * Check if the event diff contains only changes besides attendee update
     *
     * @return <code>false</code> if an attendee status was changes,<code>true</code> otherwise
     */
    boolean isAboutDetailChangesOnly();

    /**
     * Gets a value indicating whether the underlying event update is about an participant status change of a certain attendee or not 
     *
     * @param identifier The identifier of the attendee
     * @return <code>true</code> if only the participant status of the given attendee is changed, <code>false</code> otherwise
     */
    boolean isAboutCertainParticipantsStateChangeOnly(String identifier);

    /**
     * Gets a value indicating whether the underlying event update is about a removal of a certain attendee or not 
     *
     * @param userId The identifier of the attendee
     * @return <code>true</code> if only the specified attendee was removed, <code>false</code> otherwise
     */
    boolean isAboutCertainParticipantsRemoval(int userId);

}
