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

package com.openexchange.ajax.importexport;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.CSVExportRequest;
import com.openexchange.ajax.importexport.actions.CSVExportResponse;
import com.openexchange.ajax.importexport.actions.CSVImportRequest;
import com.openexchange.ajax.importexport.actions.CSVImportResponse;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.importexport.csv.CSVParser;

/**
 * Tests the CSV imports and exports (rewritten from webdav + servlet to test cotm).
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a>
 *
 */
public class CSVImportExportServletTest extends AbstractManagedContactTest {

    String CSV = "Given name,Email 1, Display name\n Prinz, tobias.prinz@open-xchange.com, Tobias Prinz\nLaguna, francisco.laguna@open-xchange.com, Francisco Laguna\n";
    private ContactField field;

    public CSVImportExportServletTest() {
        super();
    }

    @SuppressWarnings("deprecation")
    public Map<ContactField, Integer> getPositions(List<List<String>> csv) {
        HashMap<ContactField, Integer> result = new HashMap<ContactField, Integer>();
        List<String> headers = csv.get(0);
        for (int i = 0; i < headers.size(); i++) {
            field = ContactField.getByDisplayName(headers.get(i));
            if (field != null) {
                result.put(field, I(i));
            }
        }
        return result;
    }

    public void notestCSVRoundtrip() throws Exception {
        getClient().execute(new CSVImportRequest(folderID, new ByteArrayInputStream(CSV.getBytes())));
        CSVExportResponse exportResponse = getClient().execute(new CSVExportRequest(folderID));

        CSVParser parser = new CSVParser();
        List<List<String>> expected = parser.parse(CSV);
        List<List<String>> actual = parser.parse((String) exportResponse.getData());
        Map<ContactField, Integer> positions = getPositions(actual);

        for (int i = 1; i <= 2; i++) {
            assertEquals(expected.get(i).get(0), actual.get(i).get(positions.get(ContactField.GIVEN_NAME).intValue()), "Mismatch of given name in row #" + i);
            assertEquals(expected.get(i).get(1), actual.get(i).get(positions.get(ContactField.EMAIL1).intValue()), "Mismatch of email 1 in row #" + i);
            assertEquals(expected.get(i).get(2), actual.get(i).get(positions.get(ContactField.DISPLAY_NAME).intValue()), "Mismatch of display name in row #" + i);
        }
    }

    @Test
    public void testUnknownFile() throws Exception {
        final String insertedCSV = "bla1\nbla2,bla3";

        CSVImportResponse importResponse = getClient().execute(new CSVImportRequest(folderID, new ByteArrayInputStream(insertedCSV.getBytes()), false));
        assertEquals("I_E-0804", importResponse.getException().getErrorCode(), "Unexpected error code: " + importResponse.getException());
    }

    @Test
    public void testEmptyFileUploaded() throws Exception {
        final InputStream is = new ByteArrayInputStream("Given name,Email 1, Display name".getBytes());
        CSVImportResponse importResponse = getClient().execute(new CSVImportRequest(folderID, is, false));
        assertEquals("I_E-1315", importResponse.getException().getErrorCode(), "Unexpected error code: " + importResponse.getException());
    }

    public void notestDoubleImport() throws Exception {
        getClient().execute(new CSVImportRequest(folderID, new ByteArrayInputStream(CSV.getBytes())));
        getClient().execute(new CSVImportRequest(folderID, new ByteArrayInputStream(CSV.getBytes())));
        getClient().execute(new CSVExportRequest(folderID));

        CSVParser parser = new CSVParser();
        List<List<String>> expected = parser.parse(CSV);
        assertEquals(3, expected.size());
    }
}
