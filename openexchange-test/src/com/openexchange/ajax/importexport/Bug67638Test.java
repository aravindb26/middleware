
package com.openexchange.ajax.importexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.CSVImportRequest;
import com.openexchange.ajax.importexport.actions.CSVImportResponse;
import com.openexchange.groupware.container.Contact;


/**
 * {@link Bug67638}
 * 
 * import api: country not mapped
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Ottersbach</a>
 * @since v7.10.3
 */
public class Bug67638Test extends AbstractManagedContactTest {

    @Test
    public void testImportCountry() throws Exception {
        /*
         * import csv
         */
        String csv = "First Name,Middle Name,Last Name,E-mail Address,E-mail 2 Address,E-mail 3 Address,Home Phone,Home Phone 2,Business Phone,Business Phone 2,Mobile Phone,Other Phone,Primary Phone,Home Street,Home City,Home State,Home Postal Code,Home Country/Region,Other Street,Other City,Other State,Other Postal Code,Other Country/Region,Business Street,Business City,Business State,Business Postal Code,Business Country/Region,Notes\n" + "Hans,,Wurst,hans@example.com,,,,,,,+11 111 1111,,,Homestreet 23,Hometown,NRW,44135,Dortmund,,,TestState,,TestCountry,Business St. 23,Berlin,Berlin,13370,Germany,\"Some notes\n" + "\n" + "For Hans\"";

        CSVImportRequest request = new CSVImportRequest(folderID, new ByteArrayInputStream(csv.getBytes()), false);
        CSVImportResponse response = getClient().execute(request);
        /*
         * verify response
         */
        assertNotNull(response, "No response");
        assertFalse(response.hasError(), "response has error");
        JSONArray data = (JSONArray) response.getData();
        assertNotNull(data, "got no data");
        assertEquals(1, data.length());
        assertTrue(data.getJSONObject(0).has("id"), "No object ID for contact");
        /*
         * verify imported contacts
         */
        Contact contact = cotm.getAction(folderID, data.getJSONObject(0).getInt("id"));
        assertNotNull(contact, "Imported contact not found");
        assertEquals("NRW", contact.getStateHome());
        assertEquals("Berlin", contact.getStateBusiness());
        assertEquals("TestState", contact.getStateOther());
        assertEquals("Dortmund", contact.getCountryHome());
        assertEquals("Germany", contact.getCountryBusiness());
        assertEquals("TestCountry", contact.getCountryOther());
    }

}
