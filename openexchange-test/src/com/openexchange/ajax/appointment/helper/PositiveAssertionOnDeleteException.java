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

package com.openexchange.ajax.appointment.helper;

import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Changes;
import com.openexchange.groupware.container.Expectations;
import com.openexchange.test.CalendarTestManager;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class PositiveAssertionOnDeleteException extends AbstractPositiveAssertion {

    public PositiveAssertionOnDeleteException(CalendarTestManager manager, int folder) {
        super(manager, folder);
    }

    @Override
    public void check(Appointment startAppointment, Changes changes, Expectations expectations) {
        Integer recurrencePosition = (Integer) changes.get(Appointment.RECURRENCE_POSITION);
        assertNotNull(recurrencePosition, "Missing recurrence position");
        Appointment copy = startAppointment.clone();

        approachUsedForTest = "Creation, then DeleteException";

        create(copy);
        if (manager.hasLastException()) {
            fail2("Could not create appointment, error: " + manager.getLastException());
        }

        manager.createDeleteException(copy, recurrencePosition.intValue());

        checkViaGet(copy.getParentFolderID(), copy.getObjectID(), expectations);
        checkViaList(copy.getParentFolderID(), copy.getObjectID(), expectations);
    }

}
