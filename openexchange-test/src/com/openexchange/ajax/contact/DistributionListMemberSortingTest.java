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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactResponse;
import com.openexchange.testing.httpclient.models.DistributionListMember;
import com.openexchange.testing.httpclient.models.DistributionListMember.MailFieldEnum;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link DistributionListMemberSortingTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.2
 */
public class DistributionListMemberSortingTest extends AbstractApiClientContactTest {

    private ContactData contactObj;
    private String conId;
    private String conAID;
    private String conBID;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        ContactData contact2 = createContactObject("Contact");
        contact2.setFirstName("BBB");
        contact2.setLastName("BBB");
        conBID = createContact(contact2);
        contact2.setId(conBID);

        ContactData contact1 = createContactObject("Contact");
        contact1.setFirstName("AAA");
        contact1.setLastName("AAA");
        conAID = createContact(contact1);
        contact1.setId(conAID);

        contactObj = new ContactData();
        contactObj.setDisplayName("DistributionList");
        contactObj.setMarkAsDistributionlist(Boolean.TRUE);
        List<DistributionListMember> members = new ArrayList<>();
        members.add(getMemberFromContact(contact1));
        members.add(getMemberFromContact(contact2));
        contactObj.setDistributionList(members);
        contactObj.setFolderId(contactFolderId);
        conId = createContact(contactObj);
    }

    private DistributionListMember getMemberFromContact(ContactData con) {
        DistributionListMember result = new DistributionListMember();
        result.setDisplayName(con.getDisplayName());
        result.setFolderId(con.getFolderId());
        result.setId(con.getId());
        result.setMail(con.getEmail1());
        result.setMailField(MailFieldEnum.NUMBER_1);
        return result;
    }

    @Test
    public void testSorting() throws ApiException {
        List<DistributionListMember> distributionList = getDisList();

        Assertions.assertEquals(conAID, distributionList.get(0).getId());
        Assertions.assertNotNull(distributionList.get(0).getSortName(), "Sortname must not be null!");
        Assertions.assertEquals(conBID, distributionList.get(1).getId());
        Assertions.assertNotNull(distributionList.get(1).getSortName(), "Sortname must not be null!");
    }

    @Test
    public void testUpdatesDisplayname() throws Exception {
        List<DistributionListMember> distributionList = getDisList();

        DistributionListMember distributionListMemberA = distributionList.get(0);
        Assertions.assertEquals(conAID, distributionListMemberA.getId());
        Assertions.assertNotNull(distributionList.get(0).getSortName(), "Sortname must not be null!");
        Assertions.assertEquals(conBID, distributionList.get(1).getId());
        Assertions.assertNotNull(distributionList.get(1).getSortName(), "Sortname must not be null!");

        /*
         * Update contact display name, expect new display name in distribution list
         */
        ContactData delta = new ContactData();
        String displayName = "TotallyAwesomeNew Name";
        delta.setId(distributionListMemberA.getId());
        delta.setDisplayName(displayName);
        updateContact(delta, contactFolderId);
        distributionList = getDisList();

        distributionListMemberA = distributionList.get(0);
        Assertions.assertEquals(conAID, distributionListMemberA.getId());
        Assertions.assertEquals(displayName, distributionListMemberA.getDisplayName());
        Assertions.assertNotNull(distributionList.get(0).getSortName(), "Sortname must not be null!");
        Assertions.assertEquals(conBID, distributionList.get(1).getId());
        Assertions.assertNotNull(distributionList.get(1).getSortName(), "Sortname must not be null!");

    }

    private List<DistributionListMember> getDisList() throws ApiException {
        ContactResponse response = contactsApi.getContact(conId, contactFolderId);
        Assertions.assertNull(response.getErrorDesc(), response.getError());
        Assertions.assertNotNull(response.getData(), "Data shouldn't be null");
        ContactData data = response.getData();
        List<DistributionListMember> distributionList = data.getDistributionList();

        Assertions.assertNotNull(distributionList, "Missing members");
        Assertions.assertEquals(2, distributionList.size(), "Wrong number of members.");

        return distributionList;
    }

}
