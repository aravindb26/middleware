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

package com.openexchange.groupware.reminder;

import java.sql.Connection;
import java.util.Date;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.reminder.internal.RdbReminderStorage;
import com.openexchange.user.User;

/**
 * {@link ReminderStorage} - The reminder storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface ReminderStorage {

    /** The singleton reminder storage instance */
    static final ReminderStorage SINGLETON = new RdbReminderStorage();

    /**
     * Gets the reminder storage instance.
     *
     * @return The storage instance
     */
    public static ReminderStorage getInstance() {
        return SINGLETON;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Lists the reminders from given user whose alarm date is before given end date.
     *
     * @param ctx The context
     * @param user The user
     * @param end The end date
     * @return The reminders or empty array
     * @throws OXException If listing fails
     */
    ReminderObject[] selectReminder(final Context ctx, final User user, final Date end) throws OXException;

    /**
     * Lists the reminders from given user whose alarm date is before given end date using given connection.
     *
     * @param ctx The context
     * @param con The connection to use
     * @param user The user
     * @param end The end date
     * @return The reminders or empty array
     * @throws OXException If listing fails
     */
    ReminderObject[] selectReminder(Context ctx, Connection con, User user, Date end) throws OXException;

    /**
     * Lists the reminders from given user whose alarm date is before given end date using given connection.
     *
     * @param ctx The context
     * @param con The connection to use
     * @param userId The user identifier
     * @param end The end date
     * @return The reminders or empty array
     * @throws OXException If listing fails
     */
    ReminderObject[] selectReminder(Context ctx, Connection con, int userId, Date end) throws OXException;

    /**
     * Deletes specified reminder.
     *
     * @param ctx The context
     * @param reminder The reminder to delete
     * @throws OXException If deletion fails
     */
    void deleteReminder(final Context ctx, final ReminderObject reminder) throws OXException;

    /**
     * Deletes specified reminder using given connection.
     *
     * @param con The connection to use
     * @param ctxId The context identifier
     * @param reminderId The reminder identifier
     * @throws OXException If deletion fails
     */
    void deleteReminder(Connection con, int ctxId, int reminderId) throws OXException;

    /**
     * Writes specified reminder into storage.
     *
     * @param ctx The context
     * @param reminder The reminder to write
     * @throws OXException If write operation fails
     */
    void writeReminder(final Context ctx, final ReminderObject reminder) throws OXException;

    /**
     * Writes specified reminder into storage using given connection.
     *
     * @param con The connection to use
     * @param ctxId The context identifier
     * @param reminder The reminder to write
     * @throws OXException If write operation fails
     */
    void writeReminder(Connection con, int ctxId, ReminderObject reminder) throws OXException;
}
