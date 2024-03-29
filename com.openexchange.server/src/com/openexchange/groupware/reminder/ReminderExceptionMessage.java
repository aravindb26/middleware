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

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link ReminderExceptionMessage}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class ReminderExceptionMessage implements LocalizableStrings {

    /**
     * Initializes a new {@link ReminderExceptionMessage}.
     */
    private ReminderExceptionMessage() {
        super();
    }

    /**
     * User is missing for the reminder.
     */
    public final static String MANDATORY_FIELD_USER_DISPLAY = "Required  value \"user\" was not supplied.";

    /**
     * Identifier of the object is missing.
     */
    public final static String MANDATORY_FIELD_TARGET_ID_DISPLAY = "Required  value \"target id\" was not supplied.";

    /**
     * Alarm date for the reminder is missing.
     */
    public final static String MANDATORY_FIELD_ALARM_DISPLAY = "Required  value \"alarm date\" was not supplied.";

    public final static String INSERT_EXCEPTION_DISPLAY = "Unable to insert reminder.";

    public final static String UPDATE_EXCEPTION_DISPLAY = "Unable to update reminder.";

    public final static String DELETE_EXCEPTION_DISPLAY = "Unable to delete reminder.";

    public final static String LOAD_EXCEPTION_DISPLAY = "Unable to load reminder.";

    public final static String LIST_EXCEPTION_DISPLAY = "Unable to list reminder.";

    /** Reminder with identifier %1$d can not be found in context %2$d. */
    public final static String NOT_FOUND_DISPLAY = "Reminder with identifier %1$d can not be found in context %2$d.";

    /**
     * Folder of the object is missing.
     */
    public final static String MANDATORY_FIELD_FOLDER_DISPLAY = "Required  value \"folder\" was not supplied.";

    /**
     * Module type of the object is missing.
     */
    public final static String MANDATORY_FIELD_MODULE_DISPLAY = "Required  value \"module\" was not supplied.";

    /**
     * Updated too many reminders.
     */
    public final static String TOO_MANY_DISPLAY = "Updated too many reminders.";

    /** No target service is registered for module %1$d. */
    public final static String NO_TARGET_SERVICE_DISPLAY = "No target service is registered for module %1$d.";

    /**
     * Reminder identifier is missing.
     */
    public final static String MANDATORY_FIELD_ID_DISPLAY = "Required  value \"identifier\" was not supplied.";

    public final static String NO_PERMISSION_READ = "You do not have the appropriate permissions to read this object.";

    public final static String NO_PERMISSION_MODIFY = "You do not have the appropriate permissions to modify this object.";



}
