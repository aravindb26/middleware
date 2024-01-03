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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactResponse;
import com.openexchange.testing.httpclient.models.ContactUpdateData;
import com.openexchange.testing.httpclient.models.ContactUpdateResponse;
import com.openexchange.testing.httpclient.models.DistributionListMember;
import com.openexchange.testing.httpclient.models.DistributionListMember.MailFieldEnum;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * {@link MWB1643Test}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MWB1643Test extends ContactProviderTest {

    @Test
    @Order(1)
    public void testMailUpdate() throws Exception {
        String folderId = getDefaultContactFolderId();
        String email = "known_user@example.org";
        /*
         * Create contact
         */
        String contactId;
        {
            ContactUpdateResponse updateResponse = addressbooksApi.createContactInAddressbookBuilder()//@formatter:off
            .withContactData(new ContactData()
                .folderId(folderId)
                .email1(email)
                .firstName("Known")
                .lastName("User"))
            .execute();//@formatter:on
            ContactUpdateData data = checkResponse(updateResponse.getError(), updateResponse.getErrorDesc(), updateResponse.getData());
            assertThat(data, is(not(nullValue())));
            contactId = data.getId();
        }
        /*
         * Create distribution list with the contact
         */
        ContactUpdateResponse response = addressbooksApi.createContactInAddressbookBuilder()//@formatter:off
            .withContactData(new ContactData()
                .markAsDistributionlist(Boolean.TRUE)
                .displayName("DistributionList for MWB-1643")
                .folderId(folderId)
                .addDistributionListItem(
                    new DistributionListMember().mail(email).id(contactId).folderId(folderId).mailField(MailFieldEnum.NUMBER_1))
                .addDistributionListItem(
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
        assertThat(distListData.getNumberOfDistributionList(), is(I(2)));
        List<String> contactIds = distListData.getDistributionList().stream().filter(m -> Strings.isNotEmpty(m.getId())).map(m -> m.getId()).toList();
        assertThat(I(contactIds.size()), is(I(1))); // Check that only the internal user is referenced
        assertFalse(distListData.getDistributionList().get(1).getMail().equals(distListData.getDistributionList().get(0).getMail()));
        /*
         * Remove mail address and check result
         */
        {
            ContactUpdateResponse updateResponse = addressbooksApi.updateContactInAddressbookBuilder()//@formatter:off
            .withFolder(folderId)
            .withId(contactId)
            .withTimestamp(Long.valueOf(System.currentTimeMillis()))
            .withContactData(new ContactData()
                .id(contactId)
                .folderId(folderId)
                .email1(""))
            .execute();//@formatter:on
            checkResponse(updateResponse.getError(), updateResponse.getErrorDesc());
            ContactResponse contactResponse = addressbooksApi.getContactFromAddressbook(contactId, folderId);
            ContactData contactData = checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
            assertThat("Mail wasn't removed", contactData.getEmail1(), is(emptyOrNullString()));
        }
        /*
         * Get list again
         */
        ContactResponse contactResponse = addressbooksApi.getContactFromAddressbook(distListId, folderId);
        distListData = checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
        assertThat("No dist list", b(distListData.getMarkAsDistributionlist()));
        assertThat(distListData.getNumberOfDistributionList(), is(I(2)));
        /*
         * Check that the bug is fixed - previously referenced member should have been converted to an independent one implicitly
         */
        List<DistributionListMember> independentMembers = distListData.getDistributionList().stream().filter(m -> Strings.isEmpty(m.getId())).toList();
        assertThat(I(independentMembers.size()), is(I(2)));
        assertThat(L(independentMembers.stream().filter(m -> "foo@bar.com".equals(m.getMail())).count()), is(L(1)));
        assertThat(L(independentMembers.stream().filter(m -> email.equals(m.getMail())).count()), is(L(1)));
        List<DistributionListMember> internalMembers = distListData.getDistributionList().stream().filter(m -> Strings.isNotEmpty(m.getId())).toList();
        assertThat(I(internalMembers.size()), is(I(0)));
    }

    @Test
    @Order(2)
    public void testDisplayNameUpdate() throws Exception {
        String folderId = getDefaultContactFolderId();
        String email = "known_user@example.org";
        String displayName = "Known, User";
        /*
         * Create contact
         */
        String contactId;
        {
            ContactUpdateResponse updateResponse = addressbooksApi.createContactInAddressbookBuilder()//@formatter:off
                .withContactData(new ContactData()
                    .privateFlag(Boolean.FALSE)
                    .folderId(folderId)
                    .email1(email)
                    .displayName(displayName)
                    .firstName("Known")
                    .lastName("User"))
                .execute();//@formatter:on
            ContactUpdateData data = checkResponse(updateResponse.getError(), updateResponse.getErrorDesc(), updateResponse.getData());
            assertThat(data, is(not(nullValue())));
            contactId = data.getId();
        }
        /*
         * Create distribution list with the contact
         */
        ContactUpdateResponse response = addressbooksApi.createContactInAddressbookBuilder()//@formatter:off
            .withContactData(new ContactData()
                .markAsDistributionlist(Boolean.TRUE)
                .displayName("DistributionList for MWB-1617")
                .folderId(folderId)
                .addDistributionListItem(
                    new DistributionListMember().mail(email).id(contactId).folderId(folderId).displayName(displayName).mailField(MailFieldEnum.NUMBER_1))
                .addDistributionListItem(
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
        assertThat(distListData.getNumberOfDistributionList(), is(I(2)));
        {
            List<DistributionListMember> internalMembers = distListData.getDistributionList().stream().filter(m -> Strings.isNotEmpty(m.getId())).toList();
            assertThat(I(internalMembers.size()), is(I(1)));
            assertThat(internalMembers.get(0).getDisplayName(), is(displayName));
        }
        /*
         * Change name(s) and check result
         */
        String firstName = "Hubertus";
        String lastName = "von Olpe";
        displayName = firstName + ", " + lastName;
        {
            ContactUpdateResponse updateResponse = addressbooksApi.updateContactInAddressbookBuilder()//@formatter:off
                .withFolder(folderId)
                .withId(contactId)
                .withTimestamp(Long.valueOf(System.currentTimeMillis()))
                .withContactData(new ContactData()
                    .displayName(displayName)
                    .firstName(firstName)
                    .lastName(lastName))
                .execute();//@formatter:on
            checkResponse(updateResponse.getError(), updateResponse.getErrorDesc());
            ContactResponse contactResponse = addressbooksApi.getContactFromAddressbook(contactId, folderId);
            ContactData contactData = checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
            assertThat("Name wasn't changed", contactData.getDisplayName(), is(displayName));
        }
        /*
         * Get list again
         */
        ContactResponse contactResponse = addressbooksApi.getContactFromAddressbook(distListId, folderId);
        distListData = checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
        assertThat("No dist list", b(distListData.getMarkAsDistributionlist()));
        assertThat(distListData.getNumberOfDistributionList(), is(I(2)));
        /*
         * Check that the bug is fixed
         */
        List<DistributionListMember> independentMembers = distListData.getDistributionList().stream().filter(m -> Strings.isEmpty(m.getId())).toList();
        assertThat(I(independentMembers.size()), is(I(1)));
        assertThat(independentMembers.get(0).getDisplayName(), is("foo"));
        List<DistributionListMember> internalMembers = distListData.getDistributionList().stream().filter(m -> Strings.isNotEmpty(m.getId())).toList();
        assertThat(I(internalMembers.size()), is(I(1)));
        assertThat(internalMembers.get(0).getDisplayName(), is(displayName));
        assertFalse(distListData.getDistributionList().get(1).getDisplayName().equals(distListData.getDistributionList().get(0).getDisplayName()), "Both display names are the same now!");
    }

    private String getDefaultContactFolderId() throws ApiException {
        ArrayList<ArrayList<Object>> folders = folderManager.listFolders(PARENT_FOLDER, FOLDER_COLUMNS, Boolean.FALSE);
        assertThat(I(folders.size()), greaterThan(I(1)));
        Optional<ArrayList<Object>> internalProvider = folders.stream().filter(folder -> folder.get(1).equals("Contacts")).findFirst();
        assertThat("No fitting provider found", B(internalProvider.isPresent()), is(Boolean.TRUE));
        return (String) internalProvider.get().get(0);
    }

}
