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

package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.contact.action.UpdateRequest;
import com.openexchange.ajax.folder.Create;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.util.UUIDs;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.common.test.TestClassConfig;

/**
 * {@link MWB1998Test}
 * 
 * "Read own/delete all" permissions allows moving other users contacts to own address book
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MWB1998Test extends AbstractContactTest {

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withUserPerContext(2).build();
    }

    private AJAXClient client1;
    private AJAXClient client2;
    private FolderObject testFolder;
    private Contact testContact;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client1 = testUser.getAjaxClient();
        client2 = testUser2.getAjaxClient();
        /*
         * User 2 creates a new contact folder, assigns "create objects, read own objects, none write, delete all objects" permissions to User 1
         */
        testFolder = Create.createPrivateFolder(UUIDs.getUnformattedStringFromRandom(), FolderObject.CONTACT, client2.getValues().getUserId(), Create.ocl(client1.getValues().getUserId(), false, false, OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_OWN_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_ALL_OBJECTS));
        testFolder.setParentFolderID(client2.getValues().getPrivateContactFolder());
        com.openexchange.ajax.folder.actions.InsertRequest folderInsertRequest = new com.openexchange.ajax.folder.actions.InsertRequest(EnumAPI.OX_NEW, testFolder);
        com.openexchange.ajax.folder.actions.InsertResponse folderInsertResponse = client2.execute(folderInsertRequest);
        testFolder = client2.execute(new com.openexchange.ajax.folder.actions.GetRequest(EnumAPI.OX_NEW, folderInsertResponse.getId())).getFolder();
        /*
         * User 2 creates a contact at that folder
         */
        testContact = new Contact();
        testContact.setSurName(UUIDs.getUnformattedStringFromRandom());
        testContact.setParentFolderID(testFolder.getObjectID());
        com.openexchange.ajax.contact.action.InsertResponse contactInsertResponse = client2.execute(new com.openexchange.ajax.contact.action.InsertRequest(testContact));
        testContact = client2.execute(new com.openexchange.ajax.contact.action.GetRequest(testFolder.getObjectID(), contactInsertResponse.getId(), client2.getValues().getTimeZone())).getContact();
    }

    @Test
    public void testMoveForeignContact() throws Exception {
        /*
         * User 1 uses the "move" feature, provides User 2's folder and the contact-id to move the object to his own contact folder
         */
        Contact contactUpdate = new Contact();
        contactUpdate.setObjectID(testContact.getObjectID());
        contactUpdate.setParentFolderID(client1.getValues().getPrivateContactFolder());
        contactUpdate.setLastModified(testContact.getLastModified());
        com.openexchange.ajax.contact.action.UpdateResponse contactUpdateResponse = client1.execute(new UpdateRequest(testFolder.getObjectID(), contactUpdate, false));
        assertTrue(contactUpdateResponse.hasError());
        assertFalse(0 < contactUpdateResponse.getId());
        Contact reloadedContact = client2.execute(new com.openexchange.ajax.contact.action.GetRequest(testFolder.getObjectID(), testContact.getObjectID(), client2.getValues().getTimeZone())).getContact();
        assertEquals(testFolder.getObjectID(), reloadedContact.getParentFolderID());
    }

}
