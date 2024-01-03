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
import static com.openexchange.java.Autoboxing.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.contact.provider.ContactsProviders;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CapabilityData;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactResponse;
import com.openexchange.testing.httpclient.models.ContactsResponse;
import com.openexchange.testing.httpclient.modules.AddressbooksApi;
import com.openexchange.testing.httpclient.modules.CapabilitiesApi;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ContactProviderTest} - Base class for tests in a different contact provider (i.e com.openexchange.contact.provider.test)
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public abstract class ContactProviderTest extends AbstractAPIClientSession {

    private static final String CONTACT_TEST_PROVIDER_ID = "com.openexchange.contact.provider.test"; // com.openexchange.contact.provider.test.impl.TestContactsProvider.PROVIDER_ID;
    private static final String CONTACT_TEST_PROVIDER_NAME = "c.o.contact.provider.test"; //com.openexchange.contact.provider.test.impl.TestContactsProvider.PROVIDER_DISPLAY_NAME;
    protected static final String PARENT_FOLDER = "1";
    protected static final String FOLDER_COLUMNS = "1,300";
    private static final String CONTACT_COLUMNS = "1,501,502,555,606";
    private static final String ID_COLUMN = "20";

    protected FolderManager folderManager;
    protected ContactsApi contactsApi;
    protected AddressbooksApi addressbooksApi;


    static class TestContact {

        String id;
        String folderId;
        String givenName;
        String surName;
        String email1;
        String image1_url;

        public TestContact setPropertyById(int column, Object value) {
            switch (column) {
                case 1:
                    id = (String) value;
                    break;
                case 20:
                    folderId = (String) value;
                    break;
                case 501:
                    givenName = (String) value;
                    break;
                case 502:
                    surName = (String) value;
                    break;
                case 555:
                    email1 = (String) value;
                    break;
                case 606:
                    image1_url = (String) value;
                    break;
                default:
                    break;
            }
            return this;
        }

        public Object getPropertyById(int column) {
            switch (column) {
                case 1:
                    return id;
                case 20:
                    return folderId;
                case 501:
                    return givenName;
                case 502:
                    return surName;
                case 555:
                    return email1;
                case 606:
                    return image1_url;
                default:
                    return null;
            }
        }
    }

    protected static class TestColumn {

        int id;
        String name;

        public TestColumn(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * Extracts a pattern from from the given string.
     * <p>
     * This will extract the first matching group of the pattern.
     * </p>
     *
     * @param string The result to extract the pattern from
     * @param pattern The pattern to extract
     * @return The first matching group of the given pattern
     */
    protected String assertValueFromString(String string, Pattern pattern) {
        String value = getValueFromString(string, pattern);
        assertThat("Pattern not found in the response", value, is(not(nullValue())));
        return value;
    }

    /**
     * Extracts a pattern from from the given string.
     * <p>
     * This will extract the first matching group of the pattern.
     * </p>
     *
     * @param string The result to extract the pattern from
     * @param pattern The pattern to extract
     * @return The first matching group of the given pattern
     */
    protected String getValueFromString(String string, Pattern pattern) {
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Internal method to check if two list of contacts are considered as equals
     *
     * @param list1 The first list
     * @param list2 The second list
     * @param columnsToCheck The columns to check for equality
     */
    protected void assertContactsEquals(List<TestContact> list1, List<TestContact> list2, String columnsToCheck) {
        assertThat("Lists of contacts should have the same size", I(list1.size()), is(I(list2.size())));
        List<Integer> columnIds = Arrays.asList(columnsToCheck.split(",")).stream().map(c -> Integer.valueOf(c)).collect(Collectors.toList());
        for (int i = 0; i < list1.size(); i++) {
            TestContact contact1 = list1.get(i);
            TestContact contact2 = list2.get(i);
            for (int p = 0; p < columnIds.size(); p++) {
                assertThat(contact2.getPropertyById(i(columnIds.get(p))), is(contact1.getPropertyById(i(columnIds.get(p)))));
            }
        }
    }

    /**
     * Internal method to check if two contacts are considered as equals
     *
     * @param contact1 The first contact
     * @param contact2 The second contact
     * @param columnsToCheck The columns to check for equality
     */
    protected void assertContactsEquals(TestContact contact1, TestContact contact2, String columnsToCheck) {
        assertContactsEquals(Collections.singletonList(contact1), Collections.singletonList(contact2), columnsToCheck);
    }

    /**
     * Gets the contacts within a given contact folder
     *
     * @param folderId The ID of the folder to get the contacts from
     * @return A list of contacts retrieved from the folder
     * @throws ApiException
     */
    protected List<TestContact> getContactsInFolder(String folderId) throws ApiException {
        ContactsResponse contactResponse = addressbooksApi.getAllContactsFromAddressbook(folderId, CONTACT_COLUMNS, ID_COLUMN, "ASC", null);
        ArrayList<ArrayList<Object>> contacts = (ArrayList<ArrayList<Object>>) checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
        return contacts.stream().map(c -> {
            TestContact contact = new TestContact();
            contact.folderId = folderId;
            contact.id = (String) c.get(0);
            contact.givenName = (String) c.get(1);
            contact.surName = (String) c.get(2);
            contact.email1 = (String) c.get((3));
            contact.image1_url = (String) c.get((4));
            return contact;
        }).collect(Collectors.toList());
    }

    /**
     * Gets a specific amount of contacts from a given folder
     *
     * @param folderId The ID of the folder to get the contacts from
     * @param amount The amount of contacts to get
     * @return A list of contacts retrieved from the folder
     * @throws ApiException
     */
    protected List<TestContact> getContactsInFolder(String folderId, int amount) throws ApiException {
        List<TestContact> contacts = getContactsInFolder(folderId);
        assertThat(String.format("At least %d contact(s) should be present.", I(amount)), I(contacts.size()), is(greaterThanOrEqualTo(I(amount))));
        return contacts.subList(0, amount);
    }

    /**
     * Gets a single contact
     *
     * @param folderId The ID of the folder to get the contact from
     * @param id The ID of the contact to get
     * @return The contact
     * @throws ApiException
     */
    protected TestContact getContact(String folderId, String id) throws ApiException {
        ContactResponse contactResponse = addressbooksApi.getContactFromAddressbook(id, folderId);
        ContactData response = checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
        TestContact contact = new TestContact();
        contact.folderId = response.getFolderId();
        contact.id = response.getId();
        contact.givenName = response.getFirstName();
        contact.surName = response.getLastName();
        contact.email1 = response.getEmail1();
        contact.image1_url = response.getImage1Url();
        return contact;
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderManager = new FolderManager(new FolderApi(getApiClient(), testUser), String.valueOf(EnumAPI.OX_NEW.getTreeId()));
        contactsApi = new ContactsApi(getApiClient());
        addressbooksApi = new AddressbooksApi(getApiClient());
    }

    /**
     * Internal method to get the ID of the test provider's contact folder
     *
     * @return The ID of the contact folder provided by the test provider
     * @throws ApiException
     */
    protected String getAccountFolderId() throws ApiException {
        ArrayList<ArrayList<Object>> folders = folderManager.listFolders(PARENT_FOLDER, FOLDER_COLUMNS, Boolean.FALSE);
        assertThat(I(folders.size()), greaterThan(I(1)));
        Optional<ArrayList<Object>> testProviderFolder = folders.stream().filter(folder -> folder.get(1).equals(CONTACT_TEST_PROVIDER_NAME)).findFirst();
        if (false == testProviderFolder.isPresent()) {
            String capability = ContactsProviders.getCapabilityName(CONTACT_TEST_PROVIDER_ID);
            CapabilitiesApi capabilitiesApi = new CapabilitiesApi(getApiClient());
            CapabilityData capabilityData = capabilitiesApi.getCapability(capability).getData();
            assertTrue(null != capabilityData && capability.equals(capabilityData.getId()), "Capability " + capability + " not set");
        }
        assertThat("The test provider's contact folder must be accessible", B(testProviderFolder.isPresent()), is(Boolean.TRUE));
        return (String) testProviderFolder.get().get(0);
    }

}
