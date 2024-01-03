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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.VCardExportRequest;
import com.openexchange.ajax.importexport.actions.VCardExportResponse;
import com.openexchange.ajax.importexport.actions.VCardImportRequest;
import com.openexchange.ajax.importexport.actions.VCardImportResponse;
import com.openexchange.groupware.container.Contact;
import com.openexchange.java.Charsets;

/**
 * {@link MWB1233Test}
 *
 * Importing VCF with newline leads to ignored text
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public class MWB1233Test extends AbstractManagedContactTest {

    @Test
    public void testAddressLabels() throws Exception {
        /*
         * prepare vCard
         */
        String uid = UUID.randomUUID().toString();
        String vCard = // @formatter:off
            "BEGIN:VCARD\r\n" +
            "VERSION:3.0\r\n" +
            "UID:" + uid + "\r\n" +
            "FN:TestName\r\n" +
            "N:;TestName\r\n" +
            "X-EVOLUTION-FILE-AS:TestName\r\n" +
            "X-MOZILLA-HTML:FALSE\r\n" +
            "PRODID:-//Open-Xchange//7.10.4-Rev22//EN\r\n" +
            "ADR;TYPE=HOME:;HomeAddressLine2\\nHomeAddressLine3;HomeAddressLine1;TestCity\r\n" +
            " ;;;TestCountry\r\n" +
            "ADR;TYPE=WORK,pref:;WorkAddressLine2\\nWorkAddressLine3;WorkAddressLine1;Wor\r\n" +
            " kCity;;;WorkCity\r\n" +
            "LABEL;TYPE=work,pref:WorkAddressLine1\\nWorkAddressLine2\\nWorkAddressLine3\\n\r\n" +
            " WorkCity\\nWorkCity\r\n" +
            "REV:2021-05-14T15:53:55Z\r\n" +
            "X-EVOLUTION-WEBDAV-ETAG:http://www.open-xchange.com/etags/486-1621007635139\r\n" +
            "END:VCARD\r\n"
        ; // @formatter:on
        /*
         * import & check contact
         */
        Contact importedContact = importAndFetch(vCard);
        assertEquals("HomeAddressLine1", importedContact.getStreetHome());
        assertEquals("TestCity", importedContact.getCityHome());
        assertEquals("TestCountry", importedContact.getCountryHome());
        assertEquals("WorkAddressLine1", importedContact.getStreetBusiness());
        assertEquals("WorkCity", importedContact.getCityBusiness());
        assertEquals("WorkCity", importedContact.getCountryBusiness());
        String newLine = System.lineSeparator();
        // @formatter:off
        assertEquals(
            "WorkAddressLine1" + newLine +
            "WorkAddressLine2" + newLine +
            "WorkAddressLine3" + newLine +
            "WorkCity" + newLine +
            "WorkCity", importedContact.getAddressBusiness());
        // @formatter:on
        /*
         * export contact again & check
         */
        vCard = export();
        assertTrue(vCard.contains(";HomeAddressLine2\\nHomeAddressLine3;HomeAddressLine1;TestCity"));
        assertTrue(vCard.contains(";WorkAddressLine2\\nWorkAddressLine3;WorkAddressLine1;"));
        assertTrue(vCard.contains(":WorkAddressLine1\\nWorkAddressLine2\\nWorkAddressLine3\\n"));
    }

    private Contact importAndFetch(String vCard) throws Exception {
        VCardImportRequest importRequest = new VCardImportRequest(folderID, new ByteArrayInputStream(vCard.getBytes(Charsets.UTF_8)));
        VCardImportResponse importResponse = getClient().execute(importRequest);
        JSONArray data = (JSONArray) importResponse.getData();
        assertTrue(null != data && 0 < data.length(), "got no data from import request");
        JSONObject jsonObject = data.getJSONObject(0);
        assertNotNull(jsonObject, "got no data from import request");
        int objectID = jsonObject.optInt("id");
        assertTrue(0 < objectID, "got no object id from import request");
        return cotm.getAction(folderID, objectID);
    }

    private String export() throws Exception {
        VCardExportRequest exportRequest = new VCardExportRequest(folderID, false);
        VCardExportResponse exportResponse = cotm.getClient().execute(exportRequest);
        return exportResponse.getVCard();
    }

}
