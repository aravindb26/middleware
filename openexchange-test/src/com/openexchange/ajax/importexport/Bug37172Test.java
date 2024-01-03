
package com.openexchange.ajax.importexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.VCardImportRequest;
import com.openexchange.ajax.importexport.actions.VCardImportResponse;
import com.openexchange.groupware.container.Contact;
import com.openexchange.java.Charsets;
import com.openexchange.java.util.UUIDs;

/**
 * {@link Bug37172Test}
 *
 *
 *
 * @author <a href="mailto:lars.hoogestraat@open-xchange.com">Lars Hoogestraat</a>
 */
public class Bug37172Test extends AbstractManagedContactTest {

    /**
     * Initializes a new {@link Bug37172Test}.
     *
     * @param name The test name
     */
    public Bug37172Test() {
        super();
    }

    @Test
    public void testNotLosingPhoneNumbers() throws Exception {
        /*
         * prepare vCard
         */
        String uid = UUIDs.getUnformattedStringFromRandom();
        String vCard = "BEGIN:VCARD\r\n" + "VERSION:3.0\r\n" + "N:;Test;;;\r\n" + "UID:" + uid + "\r\n" + "REV:2015-03-09T23:04:44+00:00\r\n" + "FN:Test\r\n" + "PRODID:-//ownCloud//NONSGML Contacts 0.3.0.18//EN\r\n" + "EMAIL;TYPE=WORK:test@abc123.de\r\n" + "TEL;TYPE=CELL:0151 123456789\r\n" + "TEL;TYPE=HOME:0911 9876543\r\n" + "TEL;TYPE=HOME:0160 123456\r\n" + "IMPP;X-SERVICE-TYPE=jabber:xmpp:87654321\r\n" + "TEL;TYPE=WORK:0912 12345678\r\n" + "END:VCARD\r\n";
        /*
         * import
         */
        VCardImportRequest importRequest = new VCardImportRequest(folderID, new ByteArrayInputStream(vCard.getBytes(Charsets.UTF_8)));
        VCardImportResponse importResponse = getClient().execute(importRequest);
        JSONArray data = (JSONArray) importResponse.getData();
        assertTrue(null != data && 0 < data.length(), "got no data from import request");
        JSONObject jsonObject = data.getJSONObject(0);
        assertNotNull(jsonObject, "got no data from import request");
        int objectID = jsonObject.optInt("id");
        assertTrue(0 < objectID, "got no object id from import request");
        /*
         * verify imported data
         */
        Contact contact = cotm.getAction(folderID, objectID);
        assertEquals("Test", contact.getGivenName(), "firstname wrong");
        assertEquals(null, contact.getSurName(), "lastname wrong");
        assertEquals("0151 123456789", contact.getCellularTelephone1(), "cellular phone wrong");
        assertEquals("0911 9876543", contact.getTelephoneHome1(), "home phone wrong");
        assertEquals("0160 123456", contact.getTelephoneHome2(), "home phone alternative wrong");
        assertEquals("0912 12345678", contact.getTelephoneBusiness1(), "company phone wrong");
        assertEquals("xmpp:87654321", contact.getInstantMessenger1(), "xmpp jabber wrong");
        assertEquals("test@abc123.de", contact.getEmail1(), "email wrong");
    }

    @Test
    public void testNotLosingPhoneNumbersAlt() throws Exception {
        /*
         * create contact
         */
        String uid = UUIDs.getUnformattedStringFromRandom();
        String vCard = "BEGIN:VCARD\r\n" + "VERSION:3.0\r\n" + "N:;Test;;;\r\n" + "UID:" + uid + "\r\n" + "REV:2015-03-09T23:04:44+00:00\r\n" + "FN:Test\r\n" + "PRODID:-//ownCloud//NONSGML Contacts 0.3.0.18//EN\r\n" + "EMAIL;TYPE=WORK:test@abc123.de\r\n" + "TEL;TYPE=CELL:0151 123456789\r\n" + "TEL;TYPE=home,voice:0911 9876543\r\n" + "TEL;TYPE=home,voice:0160 123456\r\n" + "IMPP;X-SERVICE-TYPE=jabber:xmpp:87654321\r\n" + "TEL;TYPE=WORK,voice:0912 12345678\r\n" + "END:VCARD\r\n";
        /*
         * import
         */
        VCardImportRequest importRequest = new VCardImportRequest(folderID, new ByteArrayInputStream(vCard.getBytes(Charsets.UTF_8)));
        VCardImportResponse importResponse = getClient().execute(importRequest);
        JSONArray data = (JSONArray) importResponse.getData();
        assertTrue(null != data && 0 < data.length(), "got no data from import request");
        JSONObject jsonObject = data.getJSONObject(0);
        assertNotNull(jsonObject, "got no data from import request");
        int objectID = jsonObject.optInt("id");
        assertTrue(0 < objectID, "got no object id from import request");

        Contact contact = cotm.getAction(folderID, objectID);
        assertEquals(uid, contact.getUid(), "uid wrong");
        assertEquals("Test", contact.getGivenName(), "firstname wrong");
        assertEquals(null, contact.getSurName(), "lastname wrong");
        assertEquals("0151 123456789", contact.getCellularTelephone1(), "cellular phone wrong");
        assertEquals("0911 9876543", contact.getTelephoneHome1(), "home phone wrong");
        assertEquals("0160 123456", contact.getTelephoneHome2(), "home phone alternative wrong");
        assertEquals("0912 12345678", contact.getTelephoneBusiness1(), "company phone wrong");
        assertEquals("xmpp:87654321", contact.getInstantMessenger1(), "xmpp jabber wrong");
        assertEquals("test@abc123.de", contact.getEmail1(), "email wrong");
    }
}
