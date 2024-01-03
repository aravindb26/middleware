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
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.test.common.contact.ContactDataParser.DEFAULT_CONTACT_COLUMNS;
import static com.openexchange.test.common.contact.ContactDataParser.GAB;
import static com.openexchange.test.common.contact.ContactDataParser.getAllContactsFromAddressbook;
import static com.openexchange.test.common.contact.ContactDataParser.parseContactsData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactResponse;
import com.openexchange.testing.httpclient.models.ContactUpdateData;
import com.openexchange.testing.httpclient.models.ContactUpdateResponse;
import com.openexchange.testing.httpclient.models.DistributionListMember;
import com.openexchange.testing.httpclient.models.DistributionListMember.MailFieldEnum;
import com.openexchange.testing.httpclient.modules.AddressbooksApi;

/**
 * {@link MWB1611Test}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public class MWB1611Test extends ContactProviderTest {

    @Test
    public void testSameContactDuplicate() throws Exception {
        /*
         * Get contact data of user and second mail address
         */
        List<ContactData> contactsData = parseContactsData(//
            getAllContactsFromAddressbook(addressbooksApi, GAB, DEFAULT_CONTACT_COLUMNS, 20),// 
            DEFAULT_CONTACT_COLUMNS);
        ContactData contactData = contactsData.stream().filter(c -> null != c.getUserId() && testUser2.getUserId() == i(c.getUserId())).findAny().orElseThrow();
        /*
         * Update contact data of second user to have an second mail address available
         */
        String secondMail = "second@example.org";
        String contactId = contactData.getId();
        {
            ContactUpdateResponse updateResponse = new AddressbooksApi(testUser2.getApiClient()).updateContactInAddressbookBuilder()//@formatter:off
            .withFolder(GAB)
            .withId(contactId)
            .withTimestamp(Long.valueOf(System.currentTimeMillis()))
            .withContactData(new ContactData()
                .id(contactId)
                .folderId(GAB)
                .email1(contactData.getEmail1())
                .email2(secondMail))
            .execute();//@formatter:on
            checkResponse(updateResponse.getError(), updateResponse.getErrorDesc());
        }
        /*
         * Create distribution list with another user of the context in it
         */
        String folderId = getDefaultContactFolderId();
        ContactUpdateResponse response = addressbooksApi.createContactInAddressbookBuilder()//@formatter:off
            .withContactData(new ContactData()
                .markAsDistributionlist(Boolean.TRUE)
                .displayName("DistributionList with duplicates")
                .folderId(folderId)
                .addDistributionListItem(
                    new DistributionListMember().mail(testUser2.getLogin()).id(contactId).folderId(contactData.getFolderId()).mailField(MailFieldEnum.NUMBER_1))
                .addDistributionListItem( // Add duplicate
                    new DistributionListMember().mail(testUser2.getLogin()).id(contactId).folderId(contactData.getFolderId()).mailField(MailFieldEnum.NUMBER_1))
                .addDistributionListItem(
                    new DistributionListMember().mail(secondMail).id(contactId).folderId(contactData.getFolderId()).mailField(MailFieldEnum.NUMBER_2))
                .addDistributionListItem(
                    new DistributionListMember().mail("foo@bar.com").displayName("foo").mailField(MailFieldEnum.NUMBER_0))
                .addDistributionListItem( // Add duplicate
                    new DistributionListMember().mail("foo@bar.com").displayName("foo").mailField(MailFieldEnum.NUMBER_0)))
            .execute();//@formatter:on
        String distListId;
        {
            ContactUpdateData distListData = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
            distListId = distListData.getId();
            assertThat(distListId, is(not(emptyOrNullString())));
        }
        ContactResponse distListResponse = addressbooksApi.getContactFromAddressbook(distListId, folderId);
        ContactData distListData = checkResponse(distListResponse.getError(), distListResponse.getErrorDesc(), distListResponse.getData());
        assertThat("No dist list", b(distListData.getMarkAsDistributionlist()));
        assertThat(distListData.getNumberOfDistributionList(), is(I(3)));
        List<String> contactIds = distListData.getDistributionList().stream().filter(m -> Strings.isNotEmpty(m.getId())).map(m -> m.getId()).toList();
        assertThat(I(contactIds.size()), is(I(2))); // Check that only the internal user is referenced
        /*
         * Update list with duplicates and a new entry
         */
        ContactUpdateResponse updateResponse = addressbooksApi.updateContactInAddressbookBuilder()//@formatter:off
            .withFolder(folderId)
            .withTimestamp(L(System.currentTimeMillis()))
            .withId(distListId)
            .withContactData(new ContactData()
                .markAsDistributionlist(Boolean.TRUE)
                .folderId(folderId)
                .id(distListId)
                .distributionList(distListData.getDistributionList())
                .addDistributionListItem( // Add duplicate
                    new DistributionListMember().mail(testUser2.getLogin()).id(contactId).folderId(contactData.getFolderId()).mailField(MailFieldEnum.NUMBER_1))
                .addDistributionListItem( // Add duplicate
                    new DistributionListMember().mail(secondMail).id(contactId).folderId(contactData.getFolderId()).mailField(MailFieldEnum.NUMBER_2))
                .addDistributionListItem( // Add duplicate
                    new DistributionListMember().mail("foo@bar.com").displayName("foo").mailField(MailFieldEnum.NUMBER_0))
                .addDistributionListItem( // new entry
                    new DistributionListMember().mail("bar@foo.com").displayName("bar").mailField(MailFieldEnum.NUMBER_0)))
            .execute();//@formatter:on

        checkResponse(updateResponse.getError(), updateResponse.getErrorDesc());
        distListResponse = addressbooksApi.getContactFromAddressbook(distListId, folderId);
        distListData = checkResponse(distListResponse.getError(), distListResponse.getErrorDesc(), distListResponse.getData());
        assertThat("No dist list", b(distListData.getMarkAsDistributionlist()));
        assertThat(distListData.getNumberOfDistributionList(), is(I(4)));
        contactIds = distListData.getDistributionList().stream().filter(m -> Strings.isNotEmpty(m.getId())).map(m -> m.getId()).toList();
        assertThat(I(contactIds.size()), is(I(2))); // Check that only the internal user is referenced
    }

    private String getDefaultContactFolderId() throws ApiException {
        ArrayList<ArrayList<Object>> folders = folderManager.listFolders(PARENT_FOLDER, FOLDER_COLUMNS, Boolean.FALSE);
        assertThat(I(folders.size()), greaterThan(I(1)));
        Optional<ArrayList<Object>> internalProvider = folders.stream().filter(folder -> folder.get(1).equals("Contacts")).findFirst();
        assertThat("No fitting provider found", B(internalProvider.isPresent()), is(Boolean.TRUE));
        return (String) internalProvider.get().get(0);
    }

}
