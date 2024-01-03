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
package com.openexchange.ajax.contact;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.models.ContactDeletionsResponse;
import com.openexchange.testing.httpclient.models.ContactListElement;
import com.openexchange.testing.httpclient.models.InfoItemExport;
import com.openexchange.testing.httpclient.modules.ExportApi;
import com.openexchange.testing.httpclient.modules.ImportApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ImportExportVCardProviderTest}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class ImportExportVCardProviderTest extends ImportExportContactProviderTest {

    private ImportApi importApi;
    private ExportApi exportApi;

    //@formatter:off
    private static final List<TestColumn> TEST_COLUMNS = Arrays.asList(new TestColumn[] {
        new TestColumn(501, "Given name"),
        new TestColumn(502, "Sur name"),
        new TestColumn(555, "Email 1"),
    });
    //@formatter:on

    private static final String TEST_COLUMNS_IDS = ((Supplier<String>) () -> {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < TEST_COLUMNS.size(); i++) {
            ids.append(TEST_COLUMNS.get(i).id);
            if (i < TEST_COLUMNS.size() - 1) {
                ids.append(',');
            }
        }
        return ids.toString();
    }).get();

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.importApi = new ImportApi(getApiClient());
        this.exportApi = new ExportApi(getApiClient());
    }

    /**
     * Internal method to parse the TestContacts from the given vcard data
     *
     * @param csvData The full vcardData
     * @return A list of parsed {@link TestContact}
     */
    private List<TestContact> parseContactsFromVCard(String vcardData) {
        List<TestContact> ret = new ArrayList<>();
        final Pattern vcardDelim = Pattern.compile("BEGIN:VCARD(.*?)END:VCARD", Pattern.DOTALL);
        Matcher matcher = vcardDelim.matcher(vcardData);
        while (matcher.find()) {
            String vCard = matcher.group(1);
            TestContact contact = new TestContact();
            contact.email1 = getValueFromString(vCard, Pattern.compile("^EMAIL;TYPE=.*?:(.*?$)", Pattern.MULTILINE));
            contact.surName = getValueFromString(vCard,Pattern.compile( "^N:(.*);", Pattern.MULTILINE));
            contact.givenName = getValueFromString(vCard, Pattern.compile("^N:.*;(.*?)$", Pattern.MULTILINE));
            ret.add(contact);
        }
        return ret;
    }

    /**
     * Test exporting contacts as VCard
     *
     * @throws Exception
     */
    @Test
    public void testExportVCard() throws Exception {
        //Get all contacts from the test provider
        String folderId = getAccountFolderId();
        List<TestContact> contacts = getContactsInFolder(folderId);
        assertThat("At least one contact should be present.", B(contacts.isEmpty()), is(Boolean.FALSE));

        //Export all contacts from the folder as vCard data and parse the contacts
        String csvStr = exportApi.exportAsVCardGetReq(folderId, Boolean.TRUE);
        List<TestContact> vcardContacts = parseContactsFromVCard(csvStr);
        assertThat("There should be one VCard entry for each contact", I(vcardContacts.size()), is(I(contacts.size())));

        //Assert that the exported vCard contacts have the same values than the original contacts via the contacts API
        assertContactsEquals(contacts, vcardContacts, TEST_COLUMNS_IDS);
    }

    /**
     * Test batch export for certain contacts
     *
     * @throws Exception
     */
    @Test
    public void testExportBatchVCard() throws Exception {

        //Get two contacts from the test provider
        String folderId = getAccountFolderId();
        List<TestContact> contacts = getContactsInFolder(folderId, 2);

        //@formatter:off
        List<InfoItemExport> contactsToExport = Arrays.asList(
            new InfoItemExport[] {
                new InfoItemExport().id(contacts.get(0).id).folderId(contacts.get(0).folderId),
                new InfoItemExport().id(contacts.get(1).id).folderId(contacts.get(1).folderId),
        });
        //@formatter:on

        //Export the contacts
        List<TestContact> vcardContacts = parseContactsFromVCard(exportApi.exportAsVCardPutReq(contactsToExport));

        //Assert that the exported vCard contacts have the same value than the original contacts fetched via the contacts API
        assertContactsEquals(contacts, vcardContacts, TEST_COLUMNS_IDS);
    }

    @Test
    public void testImportVCard() throws Exception {

        //Get the first contact
        String folderId = getAccountFolderId();
        List<TestContact> contacts = getContactsInFolder(folderId, 1);

        //Export the first contact
        List<InfoItemExport> contactsToExport = Collections.singletonList(new InfoItemExport().id(contacts.get(0).id).folderId(contacts.get(0).folderId));
        String vcardData = exportApi.exportAsVCardPutReq(contactsToExport);
        assertTrue(Strings.isNotEmpty(vcardData));
        List<TestContact> csvContacts = parseContactsFromVCard(vcardData);
        assertContactsEquals(contacts, csvContacts, TEST_COLUMNS_IDS);

        //Delete the contact
        ContactDeletionsResponse deleteResult = addressbooksApi.deleteContactsFromAddressbook(L(new Date().getTime()), Collections.singletonList(new ContactListElement().id(contacts.get(0).id).folder(contacts.get(0).folderId)));
        checkResponse(deleteResult.getError(), deleteResult.getErrorDesc());
        //check it's gone
        try {
            getContact(folderId, contacts.get(0).id);
        } catch (AssertionError e) {
            assertThat("Contact should not be present anymore", e.getMessage(), containsString("Contact not found in folder"));
        }

        //Import it again
        File vCardFile = File.createTempFile(this.getClass().getName(), ".vCard");
        vCardFile.deleteOnExit();
        FileUtils.writeByteArrayToFile(vCardFile, vcardData.getBytes(StandardCharsets.UTF_8));
        String importResult = importApi.importVCard(folderId, vCardFile, Boolean.FALSE);
        assertThat(importResult, (is(not(nullValue()))));
        //Extract folder ID and the new contact ID from the import response
        String newContactId = assertValueFromString(importResult, CONTACT_ID_REPSONSE_PATTERN);
        String folderIdImported = assertValueFromString(importResult, FOLDER_ID_REPSONSE_PATTERN);

        //Check that the contact was imported into the right folder
        assertThat("The contact should have been exported into the right folder", folderIdImported, is(folderId));

        //check that the contact is present again
        TestContact importedContact = getContact(folderId, newContactId);
        contacts.get(0).id = newContactId;
        assertContactsEquals(contacts.get(0), importedContact, TEST_COLUMNS_IDS);
    }
}
