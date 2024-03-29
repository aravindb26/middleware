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

package com.openexchange.ajax.appointment.bugtests;

import static com.openexchange.ajax.folder.Create.ocl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.InsertRequest;
import com.openexchange.ajax.appointment.action.UpdateRequest;
import com.openexchange.ajax.appointment.action.UpdateResponse;
import com.openexchange.ajax.folder.Create;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.ajax.participant.ParticipantTools;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.common.groupware.calendar.TimeTools;

/**
 * Checks if a changed appointment in a shared folder looses all its participants.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Bug10154Test extends AbstractAJAXSession {

    /**
     * @param name test name.
     */
    public Bug10154Test() {
        super();
    }

    /**
     * A creates a shared folder and an appointment with participants. B changes
     * the participant in the folder and A verifies if its participants get lost.
     */
    @Test
    public void testParticipantsLost() throws Throwable {
        final AJAXClient clientA = getClient();
        final int userIdA = clientA.getValues().getUserId();
        final AJAXClient clientB = testUser2.getAjaxClient();
        final FolderObject folder = Create.folder(FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "Folder to test bug 10154", FolderObject.CALENDAR, FolderObject.PRIVATE, ocl(userIdA, false, true, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION), ocl(clientB.getValues().getUserId(), false, false, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION));
        {
            final CommonInsertResponse response = clientA.execute(new com.openexchange.ajax.folder.actions.InsertRequest(EnumAPI.OX_OLD, folder));
            response.fillObject(folder);
        }
        try {
            final TimeZone tzA = clientA.getValues().getTimeZone();
            final Appointment appointment = new Appointment();
            final List<Participant> onInsert = ParticipantTools.createParticipants(userIdA, clientB.getValues().getUserId());
            final Participant[] expected = onInsert.toArray(new Participant[onInsert.size()]);
            {
                appointment.setTitle("Test for bug 10154");
                appointment.setParentFolderID(folder.getObjectID());
                appointment.setStartDate(new Date(TimeTools.getHour(0, tzA)));
                appointment.setEndDate(new Date(TimeTools.getHour(1, tzA)));
                appointment.setParticipants(onInsert);
                appointment.setIgnoreConflicts(true);
                final InsertRequest request = new InsertRequest(appointment, tzA);
                final CommonInsertResponse response = clientA.execute(request);
                appointment.setLastModified(response.getTimestamp());
                appointment.setObjectID(response.getId());
            }
            final TimeZone tzB = clientB.getValues().getTimeZone();
            {
                final Appointment change = new Appointment();
                change.setObjectID(appointment.getObjectID());
                change.setParentFolderID(folder.getObjectID());
                change.setLastModified(appointment.getLastModified());
                change.setStartDate(new Date(TimeTools.getHour(1, tzB)));
                change.setEndDate(new Date(TimeTools.getHour(2, tzB)));
                change.setIgnoreConflicts(true);
                final UpdateRequest request = new UpdateRequest(change, tzB);
                final UpdateResponse response = clientB.execute(request);
                appointment.setLastModified(response.getTimestamp());
            }
            {
                final GetRequest request = new GetRequest(folder.getObjectID(), appointment.getObjectID());
                final GetResponse response = clientA.execute(request);
                final Appointment reload = response.getAppointment(tzA);
                final Participant[] participants = reload.getParticipants();
                assertEquals(expected.length, participants.length, "Participants should not be changed.");
                final Comparator<Participant> comparator = new Comparator<Participant>() {

                    @Override
                    public int compare(final Participant o1, final Participant o2) {
                        return o1.getIdentifier() - o2.getIdentifier();
                    }
                };
                Arrays.sort(expected, comparator);
                Arrays.sort(participants, comparator);
                for (int i = 0; i < expected.length; i++) {
                    assertEquals(expected[i].getIdentifier(), participants[i].getIdentifier(), "Participants should not be changed.");
                }
            }
        } finally {
            clientA.execute(new com.openexchange.ajax.folder.actions.DeleteRequest(EnumAPI.OX_OLD, folder.getObjectID(), folder.getLastModified()));
        }
    }
}
