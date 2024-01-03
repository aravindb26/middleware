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

package com.openexchange.ajax.usecount;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.IncrementUseCountResponse;
import com.openexchange.testing.httpclient.models.UseCountIncrement;
import com.openexchange.testing.httpclient.models.UseCountIncrement.TypeEnum;
import com.openexchange.testing.httpclient.models.UserData;
import com.openexchange.testing.httpclient.modules.UserApi;

/**
 * {@link IncrementUseCountTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class IncrementUseCountTest extends AbstractUseCountTest {

    /**
     * Initializes a new {@link IncrementUseCountTest}.
     */
    public IncrementUseCountTest() {
        super();
    }

    @Test
    public void testUseCountIncrementByContactId() throws Exception {
        ContactData contactObj = createContactObject("testContactForUseCount");
        String objectId = createContact(contactObj);

        UseCountIncrement useCountIncrement = new UseCountIncrement();
        useCountIncrement.setFolderId(contactObj.getFolderId());
        useCountIncrement.setId(objectId);
        useCountIncrement.setType(TypeEnum.CONTACT);
        IncrementUseCountResponse response = usecountApi.incrementUseCount(useCountIncrement);
        assertTrue(response.getData() != null);
        assertTrue(response.getData().getSuccess() != null && response.getData().getSuccess().booleanValue());

        ContactData loadedContact = loadContact(objectId, contactObj.getFolderId());
        Integer useCount = loadedContact.getUseCount();
        assertTrue(useCount != null && useCount.intValue() == 1);
    }

    @Test
    public void testUseCountIncrementByContactIdWithCompositeFolderId() throws Exception {
        ContactData contactObj = createContactObject("testContactForUseCount");
        String objectId = createContact(contactObj);

        UseCountIncrement useCountIncrement = new UseCountIncrement();
        useCountIncrement.setFolderId("con://0/" + contactObj.getFolderId());
        useCountIncrement.setId(objectId);
        useCountIncrement.setType(TypeEnum.CONTACT);
        IncrementUseCountResponse response = usecountApi.incrementUseCount(useCountIncrement);
        assertTrue(response.getData() != null);
        assertTrue(response.getData().getSuccess() != null && response.getData().getSuccess().booleanValue());

        ContactData loadedContact = loadContact(objectId, contactObj.getFolderId());
        Integer useCount = loadedContact.getUseCount();
        assertTrue(useCount != null && useCount.intValue() == 1);
    }

    @Test
    public void testUseCountIncrementByContactIdWithAccountAndModule() throws Exception {
        ContactData contactObj = createContactObject("testContactForUseCount");
        String objectId = createContact(contactObj);

        UseCountIncrement useCountIncrement = new UseCountIncrement();
        useCountIncrement.setFolderId(contactObj.getFolderId());
        useCountIncrement.setId(objectId);
        useCountIncrement.setAccountId(Integer.valueOf(0));
        useCountIncrement.setModuleId(Integer.valueOf(3));
        useCountIncrement.setType(TypeEnum.CONTACT);
        IncrementUseCountResponse response = usecountApi.incrementUseCount(useCountIncrement);
        assertTrue(response.getData() != null);
        assertTrue(response.getData().getSuccess() != null && response.getData().getSuccess().booleanValue());

        ContactData loadedContact = loadContact(objectId, contactObj.getFolderId());
        Integer useCount = loadedContact.getUseCount();
        assertTrue(useCount != null && useCount.intValue() == 1);
    }

    @Test
    public void testUseCountIncrementByUserId() throws Exception {
        UserApi userApi = new UserApi(getApiClient());
        UserData user = userApi.getUser(Integer.toString(testUser.getUserId())).getData();

        String contactId = user.getContactId();
        ContactData loadedContact = loadContact(contactId, "6");
        Integer prevUseCount = loadedContact.getUseCount();
        if (prevUseCount == null) {
            prevUseCount = Integer.valueOf(0);
        }

        UseCountIncrement useCountIncrement = new UseCountIncrement();
        useCountIncrement.setId(Integer.toString(testUser.getUserId()));
        useCountIncrement.setType(TypeEnum.USER);
        IncrementUseCountResponse response = usecountApi.incrementUseCount(useCountIncrement);
        assertTrue(response.getData() != null);
        assertTrue(response.getData().getSuccess() != null && response.getData().getSuccess().booleanValue());

        loadedContact = loadContact(contactId, "6");
        Integer useCount = loadedContact.getUseCount();
        assertTrue(useCount != null && useCount.intValue() == prevUseCount.intValue() + 1);
    }

    @Test
    public void testUseCountIncrementByMail() throws Exception {
        ContactData contactObj = createContactObject("testContactForUseCount");
        String objectId = createContact(contactObj);
        String mail = "hebert.meier@open-xchange.com";

        UseCountIncrement useCountIncrement = new UseCountIncrement();
        useCountIncrement.setMail(mail);
        useCountIncrement.setType(TypeEnum.CONTACT);
        IncrementUseCountResponse response = usecountApi.incrementUseCount(useCountIncrement);
        assertTrue(response.getData() != null);
        assertTrue(response.getData().getSuccess() != null && response.getData().getSuccess().booleanValue());

        ContactData loadedContact = loadContact(objectId, contactObj.getFolderId());
        Integer useCount = loadedContact.getUseCount();
        assertTrue(useCount != null && useCount.intValue() == 1);
    }

}
