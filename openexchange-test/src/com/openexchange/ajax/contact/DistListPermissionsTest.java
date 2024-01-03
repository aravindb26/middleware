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

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.ContactTestManager;
import com.openexchange.test.FolderTestManager;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link DistListPermissionsTest} - Checks the distribution list handling.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DistListPermissionsTest extends AbstractManagedContactTest {

    private ContactTestManager cotm2;
    private AJAXClient client2;
    private FolderTestManager folderManager2;

    private FolderObject sharedFolder;
    private Contact referencedContact1;
    private Contact referencedContact2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client2 = testUser2.getAjaxClient();
        cotm2 = new ContactTestManager(client2);
        folderManager2 = new FolderTestManager(client2);
        /*
         * create a shared folder as user 2
         */
        sharedFolder = ftm.generateSharedFolder("DistListTest_" + UUID.randomUUID().toString(), Module.CONTACTS.getFolderConstant(), client2.getValues().getPrivateContactFolder(), new int[] { client2.getValues().getUserId(), getClient().getValues().getUserId() });
        sharedFolder = folderManager2.insertFolderOnServer(sharedFolder);
        /*
         * create two contacts in that folder
         */
        referencedContact1 = super.generateContact("Test1");
        referencedContact1.setEmail1("email1@example.com");
        referencedContact1.setEmail2("email2@example.com");
        referencedContact1.setEmail3("email3@example.com");
        referencedContact1.setParentFolderID(sharedFolder.getObjectID());
        referencedContact1 = cotm2.newAction(referencedContact1);
        referencedContact2 = super.generateContact("Test2");
        referencedContact2.setEmail1("email1@example.org");
        referencedContact2.setEmail2("email2@example.org");
        referencedContact2.setEmail3("email3@example.org");
        referencedContact2.setParentFolderID(sharedFolder.getObjectID());
        referencedContact2 = cotm2.newAction(referencedContact2);
    }

    @Test
    public void testReferencedContactFromSharedFolder() throws OXException {
        /*
         * create distribution list
         */
        Contact distributionList = generateDistributionList(1, referencedContact1, referencedContact2);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(2, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(2, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1, referencedContact2);
    }

    @Test
    public void testPrivatizeReferencedContactFromSharedFolder() throws OXException {
        /*
         * create distribution list as user 1
         */
        Contact distributionList = generateDistributionList(1, referencedContact1);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1);
        /*
         * privatize member as user 2
         */
        referencedContact1.setPrivateFlag(true);
        referencedContact1 = cotm2.updateAction(referencedContact1);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(DistributionListEntryObject.INDEPENDENT, distributionList.getDistributionList()[0].getEmailfield(), "member mail field wrong");
        assertEquals(referencedContact1.getEmail1(), distributionList.getDistributionList()[0].getEmailaddress(), "member email address");
    }

    @Test
    public void testUnPrivatizeReferencedContactFromSharedFolder() throws OXException {
        /*
         * create distribution list as user 1
         */
        Contact distributionList = generateDistributionList(1, referencedContact1);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1);
        /*
         * privatize member as user 2
         */
        referencedContact1.setPrivateFlag(true);
        referencedContact1 = cotm2.updateAction(referencedContact1);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(DistributionListEntryObject.INDEPENDENT, distributionList.getDistributionList()[0].getEmailfield(), "member mail field wrong");
        assertEquals(referencedContact1.getEmail1(), distributionList.getDistributionList()[0].getEmailaddress(), "member email address");
        /*
         * un-privatize member as user 2
         */
        referencedContact1.setPrivateFlag(false);
        referencedContact1 = cotm2.updateAction(referencedContact1);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1);
    }

    @Test
    public void testRemovePermissionsForReferencedContact() throws OXException {
        /*
         * create distribution list as user 1
         */
        Contact distributionList = generateDistributionList(1, referencedContact1);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1);
        /*
         * update permissions of shared folder
         */
        Iterator<OCLPermission> iterator = sharedFolder.getPermissions().iterator();
        while (iterator.hasNext()) {
            if (false == iterator.next().isFolderAdmin()) {
                iterator.remove();
            }
        }
        folderManager2.updateFolderOnServer(sharedFolder);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(DistributionListEntryObject.INDEPENDENT, distributionList.getDistributionList()[0].getEmailfield(), "member mail field wrong");
        assertEquals(referencedContact1.getEmail1(), distributionList.getDistributionList()[0].getEmailaddress(), "member email address");
    }

    @Test
    public void testGrantPermissionsForReferencedContact() throws OXException {
        /*
         * create distribution list as user 1
         */
        Contact distributionList = generateDistributionList(1, referencedContact1);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1);
        /*
         * update permissions of shared folder temporary
         */
        OCLPermission removedPermission = null;
        Iterator<OCLPermission> iterator = sharedFolder.getPermissions().iterator();
        while (iterator.hasNext()) {
            OCLPermission permission = iterator.next();
            if (false == permission.isFolderAdmin()) {
                removedPermission = permission;
                iterator.remove();
            }
        }
        sharedFolder = folderManager2.updateFolderOnServer(sharedFolder);
        sharedFolder.setLastModified(folderManager2.getLastResponse().getTimestamp());
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(DistributionListEntryObject.INDEPENDENT, distributionList.getDistributionList()[0].getEmailfield(), "member mail field wrong");
        assertEquals(referencedContact1.getEmail1(), distributionList.getDistributionList()[0].getEmailaddress(), "member email address");
        /*
         * update permissions of shared folder back
         */
        sharedFolder.getPermissions().add(removedPermission);
        sharedFolder = folderManager2.updateFolderOnServer(sharedFolder);
        /*
         * verify distribution list as user 1
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact1);
    }

    private Contact generateDistributionList(int mailField, Contact... referencedContacts) throws OXException {
        /*
         * create a distribtution list, referencing to the contacts
         */
        Contact distributionList = super.generateContact("List");
        List<DistributionListEntryObject> members = new ArrayList<DistributionListEntryObject>();
        for (Contact referencedContact : referencedContacts) {
            DistributionListEntryObject member = new DistributionListEntryObject();
            member.setEntryID(referencedContact.getId(true));
            member.setEmailfield(mailField);
            member.setFolderID(referencedContact.getFolderId(true));
            member.setDisplayname(referencedContact.getDisplayName());
            if (DistributionListEntryObject.EMAILFIELD1 == mailField) {
                member.setEmailaddress(referencedContact.getEmail1());
            } else if (DistributionListEntryObject.EMAILFIELD2 == mailField) {
                member.setEmailaddress(referencedContact.getEmail2());
            } else if (DistributionListEntryObject.EMAILFIELD3 == mailField) {
                member.setEmailaddress(referencedContact.getEmail3());
            } else {
                throw new IllegalArgumentException();
            }
            members.add(member);
        }
        distributionList.setDistributionList(members.toArray(new DistributionListEntryObject[0]));
        return distributionList;
    }

    private static void assertMatches(DistributionListEntryObject[] members, Contact... contacts) {
        for (Contact contact : contacts) {
            boolean found = false;
            for (DistributionListEntryObject member : members) {
                if (I(contact.getObjectID()).toString().equals(member.getEntryID())) {
                    found = true;
                    String referencedMail = null;
                    if (DistributionListEntryObject.EMAILFIELD1 == member.getEmailfield()) {
                        referencedMail = contact.getEmail1();
                    } else if (DistributionListEntryObject.EMAILFIELD2 == member.getEmailfield()) {
                        referencedMail = contact.getEmail2();
                    } else if (DistributionListEntryObject.EMAILFIELD3 == member.getEmailfield()) {
                        referencedMail = contact.getEmail3();
                    } else {
                        fail("wrong mailfiled set in member");
                    }
                    assertEquals(member.getEmailaddress(), referencedMail, "referenced mail wrong");
                }
            }
            assertTrue(found, "contact not found in distlist members");
        }
    }

}
