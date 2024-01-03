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

package com.openexchange.ajax.mailaccount;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.MailAccountData;
import com.openexchange.testing.httpclient.models.MailAccountUpdateResponse;
import com.openexchange.testing.httpclient.models.MailAccountsResponse;
import com.openexchange.testing.httpclient.modules.MailaccountApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ChangePrimaryMailAccountNameTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.4
 */
public class ChangePrimaryMailAccountNameTest extends AbstractAPIClientSession {

    private MailaccountApi accountApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        accountApi = new MailaccountApi(getApiClient());
    }

    /**
     * Tests that the primary mail account name is changeable.
     *
     * @throws ApiException
     */
    @Test
    public void testUpdateOfPrimaryMailAccountName() throws ApiException {
        MailAccountData primAccount = getPrimaryAccount();
        String oldName = primAccount.getName();
        String newName = "name_"+System.currentTimeMillis();
        updateMailAccount(primAccount, newName);
        primAccount = getPrimaryAccount();
        assertEquals(newName, primAccount.getName(), "The name didn't change but should have!");
        // undo change
        updateMailAccount(primAccount, oldName);
    }

    private MailAccountData getPrimaryAccount() throws ApiException {
        final String columns = Attribute.ID_LITERAL.getId()+","+Attribute.NAME_LITERAL.getId();
        MailAccountsResponse response = accountApi.getAllAccounts(columns);

        assertNull(response.getError(), response.getErrorDesc());
        assertNotNull(response.getData());
        @SuppressWarnings("unchecked") ArrayList<ArrayList<Object>> data = (ArrayList<ArrayList<Object>>) response.getData();

        for (final ArrayList<Object> account : data) {
            if (account.size() > 0 && ((Number) account.get(0)).intValue() == 0) {
                MailAccountData result = new MailAccountData();
                result.setId(I(((Number) account.get(0)).intValue()));
                result.setName((String) account.get(1));
                return result;
            }
        }
        fail("Did not find the primary mail account in response");
        return null;
    }

    private void updateMailAccount(MailAccountData account, String newName) throws ApiException {
        account.setName(newName);
        MailAccountUpdateResponse updateAccount = accountApi.updateAccount(account);
        assertNull(updateAccount.getError(), updateAccount.getErrorDesc());
        assertNotNull(updateAccount.getData());
    }

}
