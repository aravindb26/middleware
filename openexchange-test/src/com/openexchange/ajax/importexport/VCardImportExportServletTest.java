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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.importexport.actions.VCardExportRequest;
import com.openexchange.ajax.importexport.actions.VCardExportResponse;
import com.openexchange.ajax.importexport.actions.VCardImportRequest;
import com.openexchange.ajax.importexport.actions.VCardImportResponse;
import com.openexchange.groupware.container.FolderObject;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests the VCard imports and exports by using the servlets.
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a>
 *
 */
public class VCardImportExportServletTest extends AbstractImportExportServletTest {

    private int folderId;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        FolderObject folder = ftm.insertFolderOnServer(ftm.generatePrivateFolder("vcard-contact-roundtrip-" + UUID.randomUUID().toString(),
            FolderObject.CONTACT, getClient().getValues().getPrivateContactFolder(), getClient().getValues().getUserId()));
        folderId = folder.getObjectID();
    }

    @Test
    public void testVCardRoundtrip() throws Exception {
        //test: import
        InputStream is = new ByteArrayInputStream(IMPORT_VCARD.getBytes());
        VCardImportRequest req = new VCardImportRequest(folderId, is);

        VCardImportResponse resp = getClient().execute(req);
        assertNull(resp.getException());

        //test: export
        VCardExportRequest exportReq = new VCardExportRequest(folderId, true);
        VCardExportResponse exportResp = getClient().execute(exportReq);
        String resultingVCard = exportResp.getVCard();

        String[] result = resultingVCard.split("\n");
        //finally: checking
        for (Entry<String, String> element : VCARD_ELEMENTS.entrySet()) {
            assertTrue(resultingVCard.contains(element.getKey()), "Missing element: " + element.getKey());
            for (String r : result) {
                if (r.startsWith(element.getKey())) {
                    assertTrue(r.contains(element.getValue()), "Missing value " + element.getValue());
                    break;
                }
            }
        }
    }

    @Test
    public void testMultiVCardRoundtrip() throws Exception {
        //test: import
        InputStream is = new ByteArrayInputStream((IMPORT_VCARD + IMPORT_VCARD_2).getBytes());
        VCardImportRequest req = new VCardImportRequest(folderId, is, true);
        getClient().execute(req);

        //test: export
        VCardExportRequest exportReq = new VCardExportRequest(folderId, true);
        VCardExportResponse resp = getClient().execute(exportReq);
        assertNotNull(resp.getVCard());

        String resultingVCard = resp.getVCard();
        System.out.println(resultingVCard);
        String[] resultingVCards = resultingVCard.split("END:VCARD\\r?\\nBEGIN:VCARD");
        Assertions.assertEquals(2, resultingVCards.length, "Expected two vCards.");
        String[] result0 = resultingVCards[0].split("\n");
        String[] result1 = resultingVCards[1].split("\n");

        //finally: checking
        for (Entry<String, String> element : VCARD_ELEMENTS.entrySet()) {
            assertTrue(resultingVCard.contains(element.getKey()), "Missing element: " + element.getKey());
            for (String r : result0) {
                if (r.startsWith(element.getKey())) {
                    assertTrue(r.contains(element.getValue()), "Missing value " + element.getValue());
                    break;
                }
            }
        }
        for (Entry<String, String> element : VCARD_ELEMENTS_2.entrySet()) {
            assertTrue(resultingVCard.contains(element.getKey()), "Missing element: " + element.getKey());
            for (String r : result1) {
                if (r.startsWith(element.getKey())) {
                    assertTrue(r.contains(element.getValue()), "Missing value " + element.getValue());
                    break;
                }
            }
        }
    }

}
