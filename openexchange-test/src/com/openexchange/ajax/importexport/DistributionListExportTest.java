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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.CSVExportRequest;
import com.openexchange.ajax.importexport.actions.CSVExportResponse;
import com.openexchange.ajax.importexport.actions.VCardExportRequest;
import com.openexchange.ajax.importexport.actions.VCardExportResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.importexport.csv.CSVParser;

public class DistributionListExportTest extends AbstractManagedContactTest {

    public DistributionListExportTest() {
        super();
    }

    @Test
    public void testCsvDistributionListsAreExported() throws OXException, IOException, JSONException {
        Contact list = generateContact("Distribution list");
        list.setDistributionList(new DistributionListEntryObject[] { new DistributionListEntryObject("my displayname", "myemail@adress.com", 0)
        });
        cotm.newAction(list);
        CSVExportResponse csvExportResponse = getClient().execute(new CSVExportRequest(folderID, true));
        String csvStr = (String) csvExportResponse.getData();

        CSVParser csvParser = new CSVParser(csvStr);
        List<List<String>> csv = csvParser.parse();
        assertEquals(2, csv.size(), "Should only contain the header line but no content");
    }

    @Test
    public void testCsvDistributionListsAreNotExported() throws OXException, IOException, JSONException {
        Contact list = generateContact("Distribution list");
        list.setDistributionList(new DistributionListEntryObject[] { new DistributionListEntryObject("my displayname", "myemail@adress.com", 0)
        });
        cotm.newAction(list);
        CSVExportResponse csvExportResponse = getClient().execute(new CSVExportRequest(folderID, false));
        String csvStr = (String) csvExportResponse.getData();

        CSVParser csvParser = new CSVParser(csvStr);
        List<List<String>> csv = csvParser.parse();
        assertEquals(1, csv.size(), "Should only contain the header line but no content");
    }

    @Test
    public void testVCardDistributionListsAreNotExportedByDefault() throws OXException, IOException, JSONException {
        Contact list = generateContact("Distribution list is not present");
        list.setDistributionList(new DistributionListEntryObject[] { new DistributionListEntryObject("my displayname", "myemail@adress.com", 0)
        });
        cotm.newAction(list);
        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(folderID, false));
        String vcard = (String) vcardExportResponse.getData();

        assertFalse(vcard.contains("my displayname"), "Should not contain name of contact in list");
        assertFalse(vcard.contains("myemail@adress.com"), "Should not contain e-mail of contact in list");
        assertFalse(vcard.contains("Distribution list is not present"), "Should not contain name of distribution list");

    }

    @Test
    public void testVCardDistributionListsAreNotExported() throws OXException, IOException, JSONException {
        Contact list = generateContact("Distribution list is not present");
        list.setDistributionList(new DistributionListEntryObject[] { new DistributionListEntryObject("my displayname", "myemail@adress.com", 0)
        });
        cotm.newAction(list);
        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(folderID, Boolean.FALSE, false));
        String vcard = (String) vcardExportResponse.getData();

        assertFalse(vcard.contains("my displayname"), "Should not contain name of contact in list");
        assertFalse(vcard.contains("myemail@adress.com"), "Should not contain e-mail of contact in list");
        assertFalse(vcard.contains("Distribution list is not present"), "Should not contain name of distribution list");

    }

    @Test
    public void testVCardDistributionListsAreExported() throws OXException, IOException, JSONException {
        Contact list = generateContact("Distribution list");
        list.setDistributionList(new DistributionListEntryObject[] { new DistributionListEntryObject("my displayname", "myemail@adress.com", 0)
        });
        cotm.newAction(list);
        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(folderID, Boolean.TRUE, true));
        String vcard = (String) vcardExportResponse.getData();

        assertTrue(vcard.contains("my displayname"), "Should contain name of contact in list");
        assertTrue(vcard.contains("myemail@adress.com"), "Should contain e-mail of contact in list");
        assertTrue(vcard.contains("Distribution list"), "Should contain name of distribution list");

    }

}
