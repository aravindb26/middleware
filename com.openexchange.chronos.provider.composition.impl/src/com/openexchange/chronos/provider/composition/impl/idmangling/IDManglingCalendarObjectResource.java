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

package com.openexchange.chronos.provider.composition.impl.idmangling;

import java.util.Date;
import java.util.List;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.RecurrenceId;

/**
 * 
 * {@link IDManglingCalendarObjectResource}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class IDManglingCalendarObjectResource implements CalendarObjectResource {

    private final CalendarObjectResource delegatee;
    private final int accountId;

    /**
     * Initializes a new {@link IDManglingCalendarObjectResource}.
     * 
     * @param delegatee The delegatee to delegate methods calls to
     * @param accountId The account identifier
     *
     */
    public IDManglingCalendarObjectResource(CalendarObjectResource delegatee, int accountId) {
        super();
        this.delegatee = delegatee;
        this.accountId = accountId;
    }

    @Override
    public String getUid() {
        return delegatee.getUid();
    }

    @Override
    public Organizer getOrganizer() {
        return delegatee.getOrganizer();
    }

    @Override
    public List<Event> getEvents() {
        return IDMangling.withUniqueIDs(delegatee.getEvents(), accountId);
    }

    @Override
    public Event getSeriesMaster() {
        return IDMangling.withUniqueID(delegatee.getSeriesMaster(), accountId);
    }

    @Override
    public List<Event> getChangeExceptions() {
        return IDMangling.withUniqueIDs(delegatee.getChangeExceptions(), accountId);
    }

    @Override
    public Event getFirstEvent() {
        return IDMangling.withUniqueID(delegatee.getFirstEvent(), accountId);
    }

    @Override
    public Event getChangeException(RecurrenceId recurrenceId) {
        return IDMangling.withUniqueID(delegatee.getChangeException(recurrenceId), accountId);
    }

    @Override
    public Date getTimestamp() {
        return delegatee.getTimestamp();
    }

}
