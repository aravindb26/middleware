
package com.openexchange.ajax.importexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.CSVExportRequest;
import com.openexchange.ajax.importexport.actions.CSVExportResponse;
import com.openexchange.ajax.importexport.actions.CSVImportRequest;
import com.openexchange.ajax.importexport.actions.CSVImportResponse;
import com.openexchange.groupware.container.Contact;

/**
 * {@link Bug32200Test}
 *
 * csv-contact-import of categories does not work
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug32200Test extends AbstractManagedContactTest {

    /**
     * Initializes a new {@link Bug32200Test}.
     *
     * @param name The test name
     */
    public Bug32200Test() {
        super();
    }

    @Test
    public void testImportCategories() throws Exception {
        String categories = "Wichtig,Firma,Neu";
        String csv = "\"Sur name\",\"Given name\",\"Email 1\",\"Categories\"\n" + "\"Walter\",\"Otto\",\"otto.walter@example.com\",\"" + categories + "\"\n";
        CSVImportRequest request = new CSVImportRequest(folderID, new ByteArrayInputStream(csv.getBytes()), false);
        CSVImportResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "response has error");
        JSONArray data = (JSONArray) response.getData();
        assertNotNull(data, "got no data");
        assertEquals(1, data.length());
        Contact contact = cotm.getAction(folderID, data.getJSONObject(0).getInt("id"));
        assertNotNull(contact, "imported contact not found");
        assertNotNull(contact.getCategories(), "no categories imported");
        assertEquals(categories, contact.getCategories(), "wrong categories imported");
    }

    @Test
    public void testExportCategories() throws Exception {
        String categories = "Unwichtig,Privat,Alt";
        Contact contact = generateContact(getClass().getName());
        contact.setCategories(categories);
        cotm.newAction(contact);
        CSVExportResponse csvExportResponse = getClient().execute(new CSVExportRequest(folderID));
        String csv = String.valueOf(csvExportResponse.getData());
        assertNotNull(csv, "no data exported");
        assertTrue(csv.contains(categories), "categories not exported");
    }

}
