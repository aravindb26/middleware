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
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;

/**
 * {@link DistListTest} - Checks the distribution list handling.
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DistListTest extends AbstractManagedContactTest {

    public DistListTest() {
        super();
    }

    @Test
    public void testCreateWithoutMembers() throws OXException {
        /*
         * create empty distribution list
         */
        Contact distributionList = super.generateContact("List");
        distributionList.setDistributionList(new DistributionListEntryObject[] { new DistributionListEntryObject("dn", "mail@example.com", DistributionListEntryObject.INDEPENDENT) });
        cotm.setFailOnError(true);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");

    }

    @Test
    public void testCreateWithContactWithoutEMail() {
        /*
         * create contact
         */
        Contact referencedContact = super.generateContact("Test");
        referencedContact = cotm.newAction(referencedContact);
        /*
         * try to create distribution list, expecting an error
         */
        Contact distributionList = super.generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[1];
        members[0] = new DistributionListEntryObject();
        members[0].setEntryID(referencedContact.getId(true));
        members[0].setFolderID(referencedContact.getFolderId(true));
        members[0].setDisplayname(referencedContact.getDisplayName());
        members[0].setEmailfield(DistributionListEntryObject.EMAILFIELD1);
        distributionList.setDistributionList(members);
        cotm.newAction(distributionList);
        AbstractAJAXResponse newResponse = cotm.getLastResponse();
        assertTrue(newResponse.hasError());
        assertNotNull(newResponse.getException());
        assertEquals("CON-0100", newResponse.getException().getErrorCode());
    }

    @Test
    public void testCreateWithOneOffWithoutEMail() throws OXException {
        /*
         * prepare distribution list
         */
        Contact distributionList = super.generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[1];
        members[0] = new DistributionListEntryObject();
        members[0].setDisplayname("Test no mail");
        members[0].setEmailfield(DistributionListEntryObject.INDEPENDENT);
        distributionList.setDistributionList(members);
        /*
         * try to create distribution list
         */
        distributionList = cotm.newAction(distributionList);
        AbstractAJAXResponse lastResponse = cotm.getLastResponse();
        assertNotNull(lastResponse.getErrorMessage(), "no error message");
        assertTrue(ContactExceptionCodes.EMAIL_MANDATORY_FOR_EXTERNAL_MEMBERS.equals(lastResponse.getException()), "wrong exception");
        /*
         * try to create with empty address instead
         */
        members[0].setEmailaddress("");
        distributionList = cotm.newAction(distributionList);
        lastResponse = cotm.getLastResponse();
        assertNotNull(lastResponse.getErrorMessage(), "no error message");
        assertTrue(ContactExceptionCodes.EMAIL_MANDATORY_FOR_EXTERNAL_MEMBERS.equals(lastResponse.getException()), "wrong exception");
    }

    @Test
    public void testRemoveEMailFromOneOff() throws OXException {
        /*
         * create distribution list
         */
        Contact distributionList = super.generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[1];
        members[0] = new DistributionListEntryObject();
        members[0].setDisplayname("Test OneOff");
        members[0].setEmailaddress("hallo@example.com");
        members[0].setEmailfield(DistributionListEntryObject.INDEPENDENT);
        distributionList.setDistributionList(members);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        /*
         * try to update distribution list
         */
        distributionList.getDistributionList()[0].setEmailaddress(null);
        distributionList = cotm.newAction(distributionList);
        AbstractAJAXResponse lastResponse = cotm.getLastResponse();
        assertNotNull(lastResponse.getErrorMessage(), "no error message");
        assertTrue(ContactExceptionCodes.EMAIL_MANDATORY_FOR_EXTERNAL_MEMBERS.equals(lastResponse.getException()), "wrong exception");
        /*
         * try to update with empty address instead
         */
        distributionList.getDistributionList()[0].setEmailaddress("");
        distributionList = cotm.newAction(distributionList);
        lastResponse = cotm.getLastResponse();
        assertNotNull(lastResponse.getErrorMessage(), "no error message");
        assertTrue(ContactExceptionCodes.EMAIL_MANDATORY_FOR_EXTERNAL_MEMBERS.equals(lastResponse.getException()), "wrong exception");
    }

    @Test
    public void testDeleteReferencedContact() throws OXException {
        /*
         * create contact
         */
        Contact referencedContact = super.generateContact("Test");
        referencedContact.setEmail1("mail@example.com");
        referencedContact = cotm.newAction(referencedContact);
        /*
         * create distribution list
         */
        Contact distributionList = super.generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[1];
        members[0] = new DistributionListEntryObject();
        members[0].setEntryID(referencedContact.getId(true));
        members[0].setFolderID(referencedContact.getFolderId(true));
        members[0].setDisplayname(referencedContact.getDisplayName());
        members[0].setEmailaddress(referencedContact.getEmail1());
        members[0].setEmailfield(DistributionListEntryObject.EMAILFIELD1);
        distributionList.setDistributionList(members);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact);
        /*
         * delete referenced contact
         */
        cotm.deleteAction(referencedContact);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(DistributionListEntryObject.INDEPENDENT, distributionList.getDistributionList()[0].getEmailfield(), "member mail field wrong");
        assertEquals(referencedContact.getEmail1(), distributionList.getDistributionList()[0].getEmailaddress(), "member email address");
    }

    @Test
    public void testDeleteReferencedContactAfterUpdate() throws OXException {
        /*
         * create contact
         */
        Contact referencedContact = super.generateContact("Test");
        referencedContact.setEmail1("mail@example.com");
        referencedContact = cotm.newAction(referencedContact);
        /*
         * create distribution list
         */
        Contact distributionList = super.generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[1];
        members[0] = new DistributionListEntryObject();
        members[0].setEntryID(referencedContact.getId(true));
        members[0].setFolderID(referencedContact.getFolderId(true));
        members[0].setDisplayname(referencedContact.getDisplayName());
        members[0].setEmailaddress(referencedContact.getEmail1());
        members[0].setEmailfield(DistributionListEntryObject.EMAILFIELD1);
        distributionList.setDistributionList(members);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact);
        /*
         * update referenced contact
         */
        referencedContact.setEmail1("mail_edit@example.com");
        referencedContact = cotm.updateAction(referencedContact);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertMatches(distributionList.getDistributionList(), referencedContact);
        /*
         * delete referenced contact
         */
        cotm.deleteAction(referencedContact);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(DistributionListEntryObject.INDEPENDENT, distributionList.getDistributionList()[0].getEmailfield(), "member mail field wrong");
        assertEquals(referencedContact.getEmail1(), distributionList.getDistributionList()[0].getEmailaddress(), "member email address");
    }

    @Test
    public void testAddReferencedContactWithoutFolderID() throws OXException {
        /*
         * create contact
         */
        Contact referencedContact = super.generateContact("Test");
        referencedContact.setEmail1("mail@example.com");
        referencedContact = cotm.newAction(referencedContact);
        /*
         * create distribution list
         */
        Contact distributionList = super.generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[1];
        members[0] = new DistributionListEntryObject();
        members[0].setEntryID(referencedContact.getId(true));
        members[0].setDisplayname(referencedContact.getDisplayName());
        members[0].setEmailaddress(referencedContact.getEmail1());
        members[0].setEmailfield(DistributionListEntryObject.EMAILFIELD1);
        distributionList.setDistributionList(members);
        distributionList = cotm.newAction(distributionList);
        /*
         * verify distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(1, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(1, distributionList.getDistributionList().length, "member count wrong");
        assertEquals(I(referencedContact.getObjectID()).toString(), distributionList.getDistributionList()[0].getEntryID(), "referenced object id wrong");
        assertEquals(I(referencedContact.getParentFolderID()).toString(), distributionList.getDistributionList()[0].getFolderID(), "referenced folder id wrong");
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
