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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.importexport.actions.CSVImportRequest;
import com.openexchange.ajax.importexport.actions.CSVImportResponse;
import com.openexchange.ajax.importexport.actions.ICalImportRequest;
import com.openexchange.ajax.importexport.actions.ICalImportResponse;
import com.openexchange.ajax.importexport.actions.OutlookCSVImportRequest;
import com.openexchange.ajax.importexport.actions.OutlookCSVImportResponse;
import com.openexchange.ajax.importexport.actions.VCardImportRequest;
import com.openexchange.ajax.importexport.actions.VCardImportResponse;

/**
 * This test verifies if the problem described in bug 9209 does not appear
 * anymore.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Bug9209Test extends AbstractAJAXSession {

    /**
     * Test byte array.
     */
    private static final byte[] TEST_BYTES = new byte[] { 'a', 'b', 'c' };

    /**
     * Default constructor.
     * 
     * @param name name of the test.
     */
    public Bug9209Test() {
        super();
    }

    /**
     * Verifies if bug 9209 appears again for CSV files.
     * 
     * @throws Throwable if an exception occurs.
     */
    @Test
    public void test9209CSV() throws Throwable {
        final AJAXClient client = getClient();
        final CSVImportResponse iResponse = Tools.importCSV(client, new CSVImportRequest(client.getValues().getPrivateContactFolder(), new ByteArrayInputStream(TEST_BYTES), false));
        assertTrue(iResponse.hasError(), "CSV importer does not give an error.");
    }

    /**
     * Verifies if bug 9209 appears again for iCal files.
     * 
     * @throws Throwable if an exception occurs.
     */
    @Test
    public void test9209ICal() throws Throwable {
        final AJAXClient client = getClient();
        final ICalImportResponse iResponse = Tools.importICal(client, new ICalImportRequest(client.getValues().getPrivateAppointmentFolder(), new ByteArrayInputStream(TEST_BYTES), false));
        //the last version of the ical4j parser does not fail on weird inputs, but does nothing
        assertEquals(0, ((JSONArray) iResponse.getData()).length(), "Response data should be empty");
        assertNull(iResponse.getConflicts(), "No conflicts should be found");
        assertFalse(iResponse.hasError(), "ICal importer should not give an error.");
    }

    /**
     * Verifies if bug 9209 appears again for vCard files.
     * 
     * @throws Throwable if an exception occurs.
     */
    @Test
    public void test9209VCard() throws Throwable {
        final AJAXClient client = getClient();
        final VCardImportResponse iResponse = Tools.importVCard(client, new VCardImportRequest(client.getValues().getPrivateContactFolder(), new ByteArrayInputStream(TEST_BYTES), false));
        assertTrue(iResponse.hasError(), "VCard importer does not give an error.");
    }

    /**
     * Verifies if bug 9209 appears again for Outlook CSV files.
     * 
     * @throws Throwable if an exception occurs.
     */
    @Test
    public void test9209OutlookCSV() throws Throwable {
        final AJAXClient client = getClient();
        final OutlookCSVImportResponse iResponse = Tools.importOutlookCSV(client, new OutlookCSVImportRequest(client.getValues().getPrivateContactFolder(), new ByteArrayInputStream(TEST_BYTES), false));
        assertTrue(iResponse.hasError(), "Outlook CSV importer does not give an error.");
    }
}
