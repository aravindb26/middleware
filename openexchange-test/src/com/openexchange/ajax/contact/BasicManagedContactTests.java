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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.Order;
import com.openexchange.test.ContactTestManager;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class BasicManagedContactTests extends AbstractManagedContactTest {

    @Test
    public void testCreateAndGetContact() {
        Contact expected = generateContact();

        cotm.newAction(expected);

        Contact actual = cotm.getAction(folderID, expected.getObjectID());

        assertEquals(expected.getSurName(), actual.getSurName(), "Surname should match");
        assertEquals(expected.getGivenName(), actual.getGivenName(), "Given name should match");
    }

    @Test
    public void testDeleteContact() {
        Contact expected = generateContact();

        cotm.newAction(expected);

        cotm.deleteAction(expected);

        Contact found = cotm.getAction(folderID, expected.getObjectID());
        assertNull(found, "Should not find a contact after deletion");
    }

    @Test
    public void testGetAllContacts() {
        int numberBefore = cotm.allAction(folderID).length;

        Contact expected = generateContact();

        cotm.newAction(expected);

        Contact[] allContactsOnServer = cotm.allAction(folderID);

        assertEquals(numberBefore + 1, allContactsOnServer.length, "Should find exactly one more contact");
    }

    @Test
    public void testGetAllContactsWithColumns() {
        int numberBefore = cotm.allAction(folderID).length;

        Contact expected = generateContact();

        cotm.newAction(expected);

        Contact[] allContactsOnServer = cotm.allAction(folderID, new int[] { 1, 4, 5, 20 });

        assertEquals(numberBefore + 1, allContactsOnServer.length, "Should find exactly one more contact");

        Contact actual = null;
        for (Contact temp : allContactsOnServer) {
            if (temp.getObjectID() == expected.getObjectID()) {
                actual = temp;
            }
        }
        assertNotNull(actual, "Should find new contact in response of AllRequest");
        assertTrue(actual.contains(1), "Should contain field #1");
        assertTrue(actual.contains(4), "Should contain field #4");
        assertTrue(actual.contains(5), "Should contain field #5");
        assertTrue(actual.contains(20), "Should contain field #20");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetAllContactsOrderedByCollationAscending() {
        List<String> sinograph = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684", "\u9e45", "\u5bcc", "\u54e5", "\u6cb3", "\u6d01", "\u79d1", "\u4e86", "\u4e48", "\u5462", "\u54e6", "\u6279", "\u4e03", "\u5982", "\u56db", "\u8e22", "\u5c4b", "\u897f", "\u8863", "\u5b50");

        for (String graphem : sinograph) {
            cotm.newAction(ContactTestManager.generateContact(folderID, graphem));
        }

        int fieldNum = ContactField.SUR_NAME.getNumber();
        Contact[] allContacts = cotm.allAction(folderID, new int[] { 1, 4, 5, 20, fieldNum }, fieldNum, Order.ASCENDING, "gb2312");

        for (int i = 0, len = sinograph.size(); i < len; i++) {
            String expected = sinograph.get(i);
            assertEquals(expected, allContacts[i].getSurName(), "Element #" + i);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetAllContactsOrderedByCollationDescending() {
        List<String> sinograph = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684", "\u9e45", "\u5bcc", "\u54e5", "\u6cb3", "\u6d01", "\u79d1", "\u4e86", "\u4e48", "\u5462", "\u54e6", "\u6279", "\u4e03", "\u5982", "\u56db", "\u8e22", "\u5c4b", "\u897f", "\u8863", "\u5b50");

        for (String graphem : sinograph) {
            cotm.newAction(ContactTestManager.generateContact(folderID, graphem));
        }

        int fieldNum = ContactField.SUR_NAME.getNumber();
        Contact[] allContacts = cotm.allAction(folderID, new int[] { 1, 4, 5, 20, fieldNum }, fieldNum, Order.DESCENDING, "gb2312");

        for (int i = 0, len = sinograph.size(); i < len; i++) {
            String expected = sinograph.get(len - i - 1);
            assertEquals(expected, allContacts[i].getSurName(), "Element #" + i);
        }
    }

    /**
     * The "wonder field" is 607, which implies sorting by last name, company name, email1, email2 and display name.
     * Currently not testing e-mail since it is not supported yet.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testGetAllContactsOrderedByCollationOrderedByWonderField() {
        List<String> sinograph = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684", "\u9e45", "\u5bcc", "\u54e5", "\u6cb3", "\u6d01"); //,"\u79d1","\u4e86","\u4e48","\u5462","\u54e6","\u6279","\u4e03","\u5982","\u56db","\u8e22","\u5c4b","\u897f","\u8863","\u5b50");

        List<String> lastNames = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684");
        List<String> displayNames = Arrays.asList("\u9e45", "\u5bcc");
        List<String> companyNames = Arrays.asList("\u54e5", "\u6cb3", "\u6d01");
        //List<String> email1s = Arrays.asList( "\u79d1@somewhere.com","\u4e86@somewhere.com","\u4e48@somewhere.com");
        //List<String> email2s = Arrays.asList( "\u5462@somewhere.com","\u54e6@somewhere.com","\u6279@somewhere.com","\u4e03@somewhere.com");

        List<List<String>> values = Arrays.asList(lastNames, displayNames, companyNames); //,email1s, email2s);
        List<ContactField> fields = Arrays.asList(ContactField.SUR_NAME, ContactField.DISPLAY_NAME, ContactField.COMPANY); //ContactField.EMAIL1, ContactField.EMAIL2 );

        int valPos = 0;
        for (ContactField field : fields) {
            List<String> values2 = values.get(valPos++);
            for (String value : values2) {
                Contact tmp = new Contact();
                tmp.setParentFolderID(folderID);
                tmp.set(field.getNumber(), value);
                tmp.setInfo(value); //we'll use this field to compare afterwards
                cotm.newAction(tmp);
            }
        }

        int fieldNum = ContactField.SUR_NAME.getNumber();
        Contact[] allContacts = cotm.allAction(folderID, new int[] { 1, 4, 5, 20, fieldNum, ContactField.INFO.getNumber() }, -1, Order.ASCENDING, "gb2312");

        for (int i = 0, len = sinograph.size(); i < len; i++) {
            String expected = sinograph.get(i);
            assertEquals(expected, allContacts[i].getInfo(), "Element #" + i);
        }
    }

    @Test
    public void testUpdateContactAndGetUpdates() {
        Contact expected = generateContact();

        cotm.newAction(expected);

        expected.setDisplayName("Display name");

        Contact update = cotm.updateAction(expected);

        Contact[] updated = cotm.updatesAction(folderID, new Date(update.getLastModified().getTime() - 1));

        assertEquals(1, updated.length, "Only one contact should have been updated");

        Contact actual = updated[0];
        assertEquals(expected.getDisplayName(), actual.getDisplayName(), "Display name should have been updated");
    }

    @Test
    public void testAllWithGBK() throws Exception {
        cotm.newAction(ContactTestManager.generateContact(getClient().getValues().getPrivateContactFolder()));

        //{"action":"all","module":"contacts","columns":"20,1,5,2,602","folder":"66","collation":"gbk","sort":"502","order":"asc"}
        Contact[] allAction = cotm.allAction(getClient().getValues().getPrivateContactFolder(), new int[] { 20, 1, 5, 2, 602 }, 502, Order.ASCENDING, "gbk");
        assertTrue(allAction.length > 0, "Should find more than 0 contacts in the private contact folder");
    }
}
