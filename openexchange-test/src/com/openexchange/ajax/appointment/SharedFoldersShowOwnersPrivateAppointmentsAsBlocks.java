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

package com.openexchange.ajax.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.appointment.action.AllRequest;
import com.openexchange.ajax.appointment.action.AppointmentUpdatesResponse;
import com.openexchange.ajax.appointment.action.GetRequest;
import com.openexchange.ajax.appointment.action.GetResponse;
import com.openexchange.ajax.appointment.action.ListRequest;
import com.openexchange.ajax.appointment.action.UpdatesRequest;
import com.openexchange.ajax.appointment.recurrence.ManagedAppointmentTest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import org.junit.jupiter.api.TestInfo;

/**
 * US 1601 on server side (TA1698)
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class SharedFoldersShowOwnersPrivateAppointmentsAsBlocks extends ManagedAppointmentTest {

    /**
     *
     */
    private static final int[] COLUMNS = new int[] { Appointment.OBJECT_ID, Appointment.FOLDER_ID, Appointment.TITLE, Appointment.START_DATE, Appointment.END_DATE };
    private AJAXClient client1;
    private AJAXClient client2;
    private FolderObject sharedFolder;
    private Date start;
    private Date end;
    private Date startRange;
    private Date endRange;
    private String privateAppointmentTitle;
    private String publicAppointmentTitle;
    private int privateAppointmentID;
    private int publicAppointmentID;
    private Appointment privateAppointment;
    private Appointment publicAppointment;

    public SharedFoldersShowOwnersPrivateAppointmentsAsBlocks() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client1 = getClient();
        client2 = testUser2.getAjaxClient();
        UserValues values = client1.getValues();
        int module = Module.CALENDAR.getFolderConstant();
        sharedFolder = ftm.generateSharedFolder("us1601_shared_" + (new Date().getTime()), module, values.getPrivateAppointmentFolder(), new int[] { values.getUserId(), client2.getValues().getUserId() });
        sharedFolder = ftm.insertFolderOnServer(sharedFolder);

        start = D("13.01.2010 07:00");
        end = D("13.01.2010 07:30");
        startRange = D("12.01.2010 07:00");
        endRange = D("14.01.2010 07:30");

        privateAppointmentTitle = "This should be private";
        privateAppointment = new Appointment();
        privateAppointment.setParentFolderID(sharedFolder.getObjectID());
        privateAppointment.setTitle(privateAppointmentTitle);
        privateAppointment.setStartDate(start);
        privateAppointment.setEndDate(end);
        privateAppointment.setPrivateFlag(true);
        catm.insert(privateAppointment);
        privateAppointmentID = privateAppointment.getObjectID();

        publicAppointmentTitle = "This should be public";
        publicAppointment = new Appointment();
        publicAppointment.setParentFolderID(sharedFolder.getObjectID());
        publicAppointment.setTitle(publicAppointmentTitle);
        publicAppointment.setStartDate(start);
        publicAppointment.setEndDate(end);
        publicAppointment.setPrivateFlag(false);
        catm.insert(publicAppointment);
        publicAppointmentID = publicAppointment.getObjectID();
    }

    @Test
    public void testShouldFindABlockForAPrivateAppointmentViaAll() throws Exception {
        CommonAllResponse response = client2.execute(new AllRequest(sharedFolder.getObjectID(), COLUMNS, startRange, endRange, TimeZone.getDefault(), true));
        int namePos = response.getColumnPos(Appointment.TITLE);
        Object[][] objects = response.getArray();
        assertEquals(2, objects.length, "Should find two elements, a private and a public one");

        Object[] app1 = objects[0];
        Object[] app2 = objects[1];

        assertTrue(app1[namePos].equals(publicAppointmentTitle) || app2[namePos].equals(publicAppointmentTitle), "One of the two should be the public one");
        assertFalse(app1[namePos].equals(privateAppointmentTitle) || app2[namePos].equals(privateAppointmentTitle), "None of the two should be the private title");
    }

    @Test
    public void testShouldFindABlockForAPrivateAppointmentViaList() throws Exception {
        CommonListResponse response = client2.execute(new ListRequest(ListIDs.l(new int[] { sharedFolder.getObjectID(), publicAppointmentID }, new int[] { sharedFolder.getObjectID(), privateAppointmentID }), COLUMNS, true));
        int namePos = response.getColumnPos(Appointment.TITLE);
        Object[][] objects = response.getArray();
        assertEquals(2, objects.length, "Should find two elements, a private and a public one");

        Object[] app1 = objects[0];
        Object[] app2 = objects[1];

        assertTrue(app1[namePos].equals(publicAppointmentTitle) || app2[namePos].equals(publicAppointmentTitle), "One of the two should be the public one");
        assertFalse(app1[namePos].equals(privateAppointmentTitle) || app2[namePos].equals(privateAppointmentTitle), "None of the two should be the private title");
    }

    @Test
    public void testShouldFindABlockForAPrivateAppointmentViaUpdates() throws Exception {
        AppointmentUpdatesResponse response = client2.execute(new UpdatesRequest(sharedFolder.getObjectID(), COLUMNS, new Date(privateAppointment.getLastModified().getTime() - 1), true));
        int namePos = response.getColumnPos(Appointment.TITLE);
        Object[][] objects = response.getArray();
        assertEquals(2, objects.length, "Should find two elements, a private and a public one");

        Object[] app1 = objects[0];
        Object[] app2 = objects[1];

        assertTrue(app1[namePos].equals(publicAppointmentTitle) || app2[namePos].equals(publicAppointmentTitle), "One of the two should be the public one");
        assertFalse(app1[namePos].equals(privateAppointmentTitle) || app2[namePos].equals(privateAppointmentTitle), "None of the two should be the private title");
    }

    @Test
    public void testShouldNotAnonymizeOwnPrivateAppointments() throws OXException, IOException, JSONException {
        CommonListResponse response = client1.execute(new ListRequest(ListIDs.l(new int[] { sharedFolder.getObjectID(), publicAppointmentID }, new int[] { sharedFolder.getObjectID(), privateAppointmentID }), COLUMNS, true));
        int namePos = response.getColumnPos(Appointment.TITLE);
        Object[][] objects = response.getArray();
        assertEquals(2, objects.length, "Should find two elements, a private and a public one");

        Object[] app1 = objects[0];
        Object[] app2 = objects[1];

        assertTrue(app1[namePos].equals(publicAppointmentTitle) || app2[namePos].equals(publicAppointmentTitle), "One of the two should be the public one");
        assertTrue(app1[namePos].equals(privateAppointmentTitle) || app2[namePos].equals(privateAppointmentTitle), "One should be the _unchanged_ private one");
    }

    @Test
    public void testShouldNotAllowToGetFullPrivateAppointmentsForNonOwner() throws Exception, Exception, Exception, Exception { //this is actually a bug that has been around for some time
        GetResponse response = client2.execute(new GetRequest(privateAppointment));
        Appointment expected = response.getAppointment(timeZone);
        assertFalse(privateAppointmentTitle.equals(expected.getTitle()), "Title should be anonymized");
    }

    @Test
    public void testShouldStillAllowToGetFullPrivateAppointmentsForOwner() throws Exception, Exception, Exception, Exception { //this is actually a bug that has been around for some time
        GetResponse response = client1.execute(new GetRequest(privateAppointment));
        Appointment expected = response.getAppointment(timeZone);
        assertTrue(privateAppointmentTitle.equals(expected.getTitle()), "Title should not be anonymized");
    }

    @Test
    public void testShouldShowRecurrences() {}

}
