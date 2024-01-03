
package com.openexchange.ajax.importexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.CSVImportRequest;
import com.openexchange.ajax.importexport.actions.CSVImportResponse;

public class Bug20516Test extends AbstractManagedContactTest {

    @Test
    public void testEmail() throws Exception {
        String ical = "Sur name,Given name,Email 1\nBroken,E-Mail,notanaddress\n";
        CSVImportRequest request = new CSVImportRequest(folderID, new ByteArrayInputStream(ical.getBytes()), false);
        CSVImportResponse response = getClient().execute(request);
        JSONArray data = (JSONArray) response.getData();
        assertEquals(1, data.length(), "Unexpected response length");
        assertTrue(data.getJSONObject(0).has("id"), "No object ID for imported contact");
        assertTrue(data.getJSONObject(0).has("code"), "No warning for imported contact");
        assertEquals("I_E-1306", data.getJSONObject(0).get("code"), "Wrong error code for imported contact");
    }

}
