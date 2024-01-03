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

package com.openexchange.ajax.folder.api2;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.folder.actions.PathRequest;
import com.openexchange.ajax.folder.actions.PathResponse;
import com.openexchange.ajax.framework.Abstrac2UserAJAXSession;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.common.test.PermissionTools;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug16163Test}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Bug16163Test extends Abstrac2UserAJAXSession {

    private static final int[] ATTRIBUTES = { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME };
    private FolderObject testFolder;
    private int appointmentFolder;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testFolder = new FolderObject();
        testFolder.setModule(FolderObject.CALENDAR);
        appointmentFolder = client1.getValues().getPrivateAppointmentFolder();
        testFolder.setParentFolderID(appointmentFolder);
        testFolder.setPermissions(PermissionTools.P(I(client1.getValues().getUserId()), "a/a", I(client2.getValues().getUserId()), "v"));
        testFolder.setFolderName("testFolder4Bug16163-" + System.currentTimeMillis());
        final InsertRequest iReq = new InsertRequest(EnumAPI.OUTLOOK, testFolder);
        final InsertResponse iResp = client1.execute(iReq);
        iResp.fillObject(testFolder);
        // Unfortunately no timestamp when creating a mail folder through Outlook folder tree.
        testFolder.setLastModified(new Date());
    }

    @Test
    public void testPathRequestWorks() throws Throwable {
        {
            // Fill cache with database folder.
            final GetRequest request = new GetRequest(EnumAPI.OX_NEW, testFolder.getObjectID());
            client1.execute(request);
        }
        {
            // Fill cache with outlook folder.
            final GetRequest request = new GetRequest(EnumAPI.OUTLOOK, testFolder.getObjectID());
            client1.execute(request);
        }
        {
            // Test with user seeing a shared folder.
            final PathRequest request = new PathRequest(EnumAPI.OUTLOOK, testFolder.getObjectID(), ATTRIBUTES, false);
            final PathResponse response = client2.execute(request);
            final Object[][] data = response.getArray();
            assertFalse(response.hasError(), "Path request should work for that folder, but failed with: " + response.getErrorMessage());
            final int idPos = response.getColumnPos(FolderObject.OBJECT_ID);
            assertTrue(idPos >= 0, "Response should contain folder identifier.");
            final int namePos = response.getColumnPos(FolderObject.FOLDER_NAME);
            assertTrue(namePos >= 0, "Response should contain folder names.");
            assertTrue(data.length >= 4, "Path on Outlook like tree should have at least 4 parts, but has " + data.length);
            assertEquals(Integer.toString(testFolder.getObjectID()), data[0][idPos], "Path should start with test folder but is folder " + data[0][namePos]);
            if (4 == data.length) {
                assertEquals(FolderObject.SHARED_PREFIX + client1.getValues().getUserId(), data[1][idPos], "Parent of created folder should be virtual shared user folder but is " + data[1][namePos]);
                assertEquals(Integer.toString(FolderObject.SYSTEM_SHARED_FOLDER_ID), data[2][idPos], "Parent of virtual shared user folder should be system shared root folder but is " + data[2][namePos]);
                assertEquals(FolderStorage.PRIVATE_ID, data[3][idPos], "Root folder should be IPM_ROOT but is " + data[3][namePos]);
            } else {
                assertEquals(Integer.toString(appointmentFolder), data[1][idPos], "Parent of created folder should be user1's Calendar folder but is " + data[1][namePos]);
                assertEquals(FolderObject.SHARED_PREFIX + client1.getValues().getUserId(), data[2][idPos], "Parent of created folder should be virtual shared user folder but is " + data[2][namePos]);
                assertEquals(Integer.toString(FolderObject.SYSTEM_SHARED_FOLDER_ID), data[3][idPos], "Parent of virtual shared user folder should be system shared root folder but is " + data[3][namePos]);
                assertEquals(FolderStorage.PRIVATE_ID, data[4][idPos], "Root folder should be IPM_ROOT but is " + data[4][namePos]);
            }
        }
        {
            // Check cached folder.
            final GetRequest request = new GetRequest(EnumAPI.OUTLOOK, testFolder.getObjectID());
            final GetResponse response = client1.execute(request);
            assertEquals(testFolder.getObjectID(), response.getFolder().getObjectID(), "Identifier of cached folder is broken.");
        }
        {
            // Test with sharing user if caching breaks his path.
            final PathRequest request = new PathRequest(EnumAPI.OUTLOOK, testFolder.getObjectID(), ATTRIBUTES, false);
            final PathResponse response = client1.execute(request);
            final Object[][] data = response.getArray();
            assertFalse(response.hasError(), "Path request should work for that folder.");
            final int idPos = response.getColumnPos(FolderObject.OBJECT_ID);
            assertTrue(idPos >= 0, "Response should contain folder identifier.");
            final int namePos = response.getColumnPos(FolderObject.FOLDER_NAME);
            assertTrue(namePos >= 0, "Response should contain folder names.");
            assertEquals(3, data.length, "Path on Outlook like tree should have 3 parts.");
            assertEquals(Integer.toString(testFolder.getObjectID()), data[0][idPos], "Path should start with test folder but is folder " + data[0][namePos]);
            assertEquals(Integer.toString(appointmentFolder), data[1][idPos], "Parent of created folder should be users private calendar folder but is " + data[1][namePos]);
            assertEquals(FolderStorage.PRIVATE_ID, data[2][idPos], "Root folder should be IPM_ROOT but is " + data[2][namePos]);
        }
    }
}
