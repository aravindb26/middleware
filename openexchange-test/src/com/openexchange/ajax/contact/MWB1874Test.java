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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.contact.action.UpdateRequest;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.java.util.UUIDs;
import org.junit.jupiter.api.Test;

/**
 * {@link MWB1874Test}
 * 
 * Emptying email addresses in contacts used in distribution lists leads to incomplete entries
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 8.0.0
 */
public class MWB1874Test extends AbstractManagedContactTest {

    @Test
    public void testDeleteMailAddresses() throws Exception {
        /*
         * create contact with email1, email2 and email3
         */
        Contact contact = generateContact("Contact");
        contact.setEmail1(UUIDs.getUnformattedStringFromRandom() + "@example.org");
        contact.setEmail2(UUIDs.getUnformattedStringFromRandom() + "@example.org");
        contact.setEmail3(UUIDs.getUnformattedStringFromRandom() + "@example.org");
        contact = cotm.newAction(contact);
        contact = cotm.getAction(contact);
        /*
         * create distribution list that references all the contact's mail addresses
         */
        Contact distributionList = generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[3];
        for (int i = 0; i < members.length; i++) {
            members[i] = new DistributionListEntryObject();
            members[i].setDisplayname(contact.getDisplayName());
            members[i].setEmailaddress(0 == i ? contact.getEmail1() : 1 == i ? contact.getEmail2() : contact.getEmail3());
            members[i].setEmailfield(0 == i ? DistributionListEntryObject.EMAILFIELD1 : 1 == i ? DistributionListEntryObject.EMAILFIELD2 : DistributionListEntryObject.EMAILFIELD3);
            members[i].setEntryID(contact.getId(true));
            members[i].setFolderID(contact.getFolderId());
        }
        distributionList.setDistributionList(members);
        distributionList = cotm.newAction(distributionList);
        distributionList = cotm.getAction(distributionList);
        /*
         * get & verify created distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(3, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(3, distributionList.getDistributionList().length, "member count wrong");
        assertContains(distributionList.getDistributionList(), contact.getEmail1());
        assertContains(distributionList.getDistributionList(), contact.getEmail2());
        assertContains(distributionList.getDistributionList(), contact.getEmail3());
        /*
         * update contact & remove email addresses
         */
        Contact updatedContact = new Contact();
        updatedContact.setObjectID(contact.getObjectID());
        updatedContact.setParentFolderID(contact.getParentFolderID());
        updatedContact.setLastModified(contact.getLastModified());
        UpdateRequest updateRequest = new UpdateRequest(updatedContact) {

            @Override
            protected JSONObject convert(Contact contactObj) throws JSONException {
                JSONObject jsonObject = super.convert(contactObj);
                jsonObject.put(ContactFields.EMAIL1, JSONObject.NULL);
                jsonObject.put(ContactFields.EMAIL2, JSONObject.NULL);
                jsonObject.put(ContactFields.EMAIL3, JSONObject.NULL);
                return jsonObject;
            }
        };
        Response response = getClient().execute(updateRequest).getResponse();
        assertNull(response.getException());
        /*
         * verify the mail addresses were removed
         */
        updatedContact = cotm.getAction(updatedContact);
        assertNull(updatedContact.getEmail1());
        assertNull(updatedContact.getEmail2());
        assertNull(updatedContact.getEmail3());
        /*
         * verify that the distribution list is still using the fallbacks now
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(3, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(3, distributionList.getDistributionList().length, "member count wrong");
        assertContains(distributionList.getDistributionList(), contact.getEmail1());
        assertContains(distributionList.getDistributionList(), contact.getEmail2());
        assertContains(distributionList.getDistributionList(), contact.getEmail3());
    }


    private static void assertContains(DistributionListEntryObject[] distributionList, String email) {
        assertNotNull(distributionList);
        for (DistributionListEntryObject dleo : distributionList) {
            if (Objects.equals(email, dleo.getEmailaddress())) {
                return;
            }
        }
        fail(email + " not found in list");
    }

}
