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

package com.openexchange.chronos.storage;

import java.sql.Connection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.openexchange.chronos.AlarmAction;
import com.openexchange.chronos.AlarmTrigger;
import com.openexchange.chronos.LockedAlarmTrigger;
import com.openexchange.exception.OXException;

/**
 * {@link AdministrativeAlarmTriggerStorage}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
public interface AdministrativeAlarmTriggerStorage {

    /**
     * Retrieves a mapping of context/account tuples to a list of alarm triggers which are either
     * not processed yet and have a trigger time before the given until value or are older than the given overdue time.
     *
     * @param con The read-write connection to use; connection is required to have write access for acquiring locked triggers
     * @param until The upper limit for the trigger time
     * @param overdueTime The overdue date
     * @param lock Whether the selected triggers should be locked or not
     * @param actions The list of actions to check for. Usually a list of available message alarm actions.
     * @return A mapping of a context/account tuples to a list of alarm triggers
     * @throws OXException If retrieval/locking fails
     */
    Map<ContextAndAccountId, List<LockedAlarmTrigger>> getAndLockTriggers(Connection con, Date until, Date overdueTime, boolean lock, AlarmAction... actions) throws OXException;

    /**
     * Drops the processing status for the given alarm trigger.
     *
     * @param con The connection to use
     * @param accountId The account identifier
     * @param contextId The context identifier
     * @param trigger The trigger to update
     * @throws OXException If dropping processing status fails
     */
    default void dropProcessingStatus(Connection con, int accountId, int contextId, AlarmTrigger trigger) throws OXException {
        dropProcessingStatus(con, Collections.singletonMap(new ContextAndAccountId(accountId, contextId), Collections.singletonList(trigger)));
    }

    /**
     * Drops the processing status for the given alarm triggers.
     *
     * @param con The connection to use
     * @param triggers The triggers to update
     * @throws OXException If dropping processing status fails
     */
    void dropProcessingStatus(Connection con, Map<ContextAndAccountId, List<AlarmTrigger>> triggers) throws OXException;

    /**
     * Retrieves and locks message alarm triggers for the given event which are not already taken by another worker.
     *
     * @param con The read-write connection to use; connection is required to have write access for acquiring locked triggers
     * @param cid The context id
     * @param account The account id
     * @param eventId The event id
     * @param lock Whether the selected triggers should be locked or not.
     * @param actions The list of actions to check for. Usually a list of available message alarm actions.
     * @return A mapping of a context/account tuples to a list of alarm triggers
     * @throws OXException If retrieval/locking fails
     */
    Map<ContextAndAccountId, List<LockedAlarmTrigger>> getMessageAlarmTriggers(Connection con, int cid, int account, String eventId, boolean lock, AlarmAction... actions) throws OXException;

}
