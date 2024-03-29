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

package com.openexchange.dav.carddav.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;

/**
 * {@link BulkImportTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public class BulkImportTest extends CardDAVTest {

    public BulkImportTest() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testBulkImportWithSimilarityCheck(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        final String syncToken = super.fetchSyncToken();
        /*
         * create contact
         */

        final String firstName = "test";
        final String lastName = "horst";
        final String uid = UUID.nameUUIDFromBytes((firstName + lastName + "_bulk_contact").getBytes()).toString();
        final String firstName2 = "test2";
        final String lastName2 = "horst2";
        final String uid2 = UUID.nameUUIDFromBytes((firstName2 + lastName2 + "_bulk_contact").getBytes()).toString();
        final String firstName3 = "test3";
        final String lastName3 = "horst3";
        final String uid3 = UUID.nameUUIDFromBytes((firstName3 + lastName3 + "_bulk_contact").getBytes()).toString();

        final String email1 = uid + "@domain.com";
        final String email2 = uid2 + "@domain.com";
        final String vCard1 = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName + ";" + firstName + ";;;" + "\r\n" + "FN:" + firstName + " " + lastName + "\r\n" + "ORG:test3;" + "\r\n" + "EMAIL;type=INTERNET;type=WORK;type=pref:" + email1 + "\r\n" + "TEL;type=WORK;type=pref:24235423" + "\r\n" + "TEL;type=CELL:352-3534" + "\r\n" + "TEL;type=HOME:346346" + "\r\n" + "UID:" + uid + "\r\n" + "REV:" + super.formatAsUTC(new Date()) + "\r\n" + "PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" + "END:VCARD" + "\r\n";
        final String vCard2 = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName2 + ";" + firstName2 + ";;;" + "\r\n" + "FN:" + firstName2 + " " + lastName2 + "\r\n" + "ORG:test3;" + "\r\n" + "EMAIL;type=INTERNET;type=WORK;type=pref:" + email2 + "\r\n" + "TEL;type=WORK;type=pref:24235423" + "\r\n" + "TEL;type=CELL:352-3534" + "\r\n" + "TEL;type=HOME:346346" + "\r\n" + "UID:" + uid2 + "\r\n" + "REV:" + super.formatAsUTC(new Date()) + "\r\n" + "PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" + "END:VCARD" + "\r\n";
        final String vCard3 = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName3 + ";" + firstName3 + ";;;" + "\r\n" + "FN:" + firstName3 + " " + lastName3 + "\r\n" + "ORG:test3;" + "\r\n" + "EMAIL;type=INTERNET;type=WORK;type=pref:" + email1 + "\r\n" + "TEL;type=WORK;type=pref:24235423" + "\r\n" + "TEL;type=CELL:352-3534" + "\r\n" + "TEL;type=HOME:346346" + "\r\n" + "UID:" + uid3 + "\r\n" + "REV:" + super.formatAsUTC(new Date()) + "\r\n" + "PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" + "END:VCARD" + "\r\n";

        final String vCard = vCard2 + vCard3;

        // Delete existing contacts
        super.delete(uid);
        super.delete(uid2);

        // Import first contacts (ignore similarity)
        String xmlResponse = super.postVCard(vCard1, 0);
        Document xmlDoc = loadXMLFromString(xmlResponse);
        NodeList list = xmlDoc.getElementsByTagName("D:href");
        assertEquals(1, list.getLength(), "Unexpected href count");
        Node node = list.item(0);
        String hrefContent = node.getTextContent();
        assertNotNull(hrefContent, "Response does not contain a href");
        assertTrue(!hrefContent.isEmpty(), "Response does not contain a href");

        // Import contacts 2 and 3
        xmlResponse = super.postVCard(vCard, 1);
        xmlDoc = loadXMLFromString(xmlResponse);
        list = xmlDoc.getElementsByTagName("D:href");
        assertEquals(3, list.getLength(), "Unexpected href count");
        node = list.item(0);
        hrefContent = node.getTextContent();
        assertNotNull(hrefContent, "Response does not contain a href");
        assertTrue(!hrefContent.isEmpty(), "Response does not contain a href");

        node = list.item(1);
        hrefContent = node.getTextContent();
        assertTrue(hrefContent == null || hrefContent.isEmpty(), "Response does contain a href, but it shouldn't");
        NodeList noSimilarContacts = xmlDoc.getElementsByTagName("OX:no-similar-contact");
        assertEquals(1, noSimilarContacts.getLength(), "Unexpected no-similar-contact count");

        /*
         * verify contacts on server
         */
        final Contact contact = super.getContact(uid);
        super.rememberForCleanUp(contact);
        assertEquals(uid, contact.getUid(), "uid wrong");
        assertEquals(firstName, contact.getGivenName(), "firstname wrong");
        assertEquals(lastName, contact.getSurName(), "lastname wrong");

        final Contact contact2 = super.getContact(uid2);
        super.rememberForCleanUp(contact2);
        assertEquals(uid2, contact2.getUid(), "uid wrong");
        assertEquals(firstName2, contact2.getGivenName(), "firstname wrong");
        assertEquals(lastName2, contact2.getSurName(), "lastname wrong");

        // verify contact 3 is not on server
        final Contact contact3 = super.getContact(uid3);
        assertNull(contact3, "Contact3 is not null");

        /*
         * verify contact on client
         */
        final Map<String, String> eTags = super.syncCollection(syncToken);
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        final List<VCardResource> addressData = super.addressbookMultiget(eTags.keySet());
        final VCardResource card = assertContains(uid, addressData);
        assertEquals(firstName, card.getGivenName(), "N wrong");
        assertEquals(lastName, card.getFamilyName(), "N wrong");
        assertEquals(firstName + " " + lastName, card.getFN(), "FN wrong");

        final VCardResource card2 = assertContains(uid2, addressData);
        assertEquals(firstName2, card2.getGivenName(), "N wrong");
        assertEquals(lastName2, card2.getFamilyName(), "N wrong");
        assertEquals(firstName2 + " " + lastName2, card2.getFN(), "FN wrong");

        assertNotContains(uid3, addressData);
    }

    /**
     * Test-case for bug 61873
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testBulkImportWithTooBigVCard(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        final String firstName = "test";
        final String lastName = "horst";
        final String uid = UUID.nameUUIDFromBytes((firstName + lastName + "_bulk_contact").getBytes()).toString();
        final String firstName2 = RandomStringUtils.randomAlphabetic(3000000);
        final String lastName2 = "horst2";
        final String uid2 = UUID.nameUUIDFromBytes((firstName2 + lastName2 + "_bulk_contact").getBytes()).toString();

        final String email1 = uid + "@domain.com";
        final String email2 = uid2 + "@domain.com";
        final String vCard1 = // @formatter:off
            "BEGIN:VCARD" + "\r\n" +
            "VERSION:3.0" + "\r\n" +
            "N:" + lastName + ";" + firstName + ";;;" + "\r\n" +
            "FN:" + firstName + " " + lastName + "\r\n" +
            "ORG:test3;" + "\r\n" +
            "EMAIL;type=INTERNET;type=WORK;type=pref:" + email1 + "\r\n" +
            "TEL;type=WORK;type=pref:24235423" + "\r\n" +
            "TEL;type=CELL:352-3534" + "\r\n" +
            "TEL;type=HOME:346346" + "\r\n" + "UID:" + uid + "\r\n" +
            "REV:" + super.formatAsUTC(new Date()) + "\r\n" +
            "PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" +
            "END:VCARD" + "\r\n"
        ; // @formatter:on
        final String vCard2 = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "N:" + lastName2 + ";" + firstName2 + ";;;" + "\r\n" + "FN:" + firstName2 + " " + lastName2 + "\r\n" + "ORG:test3;" + "\r\n" + "EMAIL;type=INTERNET;type=WORK;type=pref:" + email2 + "\r\n" + "TEL;type=WORK;type=pref:24235423" + "\r\n" + "TEL;type=CELL:352-3534" + "\r\n" + "TEL;type=HOME:346346" + "\r\n" + "UID:" + uid2 + "\r\n" + "REV:" + super.formatAsUTC(new Date()) + "\r\n" + "PRODID:-//Apple Inc.//AddressBook 6.0//EN" + "\r\n" + "END:VCARD" + "\r\n";

        final String vCard = vCard1 + vCard2;

        // Delete existing contacts
        super.delete(uid);
        super.delete(uid2);

        // Import contacts 1 and 2
        String xmlResponse = super.postVCard(vCard, 0);
        Document xmlDoc = loadXMLFromString(xmlResponse);
        NodeList list = xmlDoc.getElementsByTagName("D:href");
        assertEquals(2, list.getLength(), "Unexpected href count");
    }

    private static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

}
