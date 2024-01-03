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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mailaccount.actions.MailAccountGetRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountGetResponse;
import com.openexchange.ajax.mailaccount.actions.MailAccountInsertRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountInsertResponse;
import com.openexchange.mailaccount.MailAccountDescription;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MailAccountStartTlsTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.2
 */
public class MailAccountStartTlsTest extends AbstractMailAccountTest {

    private MailAccountDescription mailAccount;

    public MailAccountStartTlsTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        mailAccount = null;
    }

    @Test
    public void testCreateMailAccountWithStartTls() throws Exception {
        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        acc.setMailStartTls(true);
        acc.setTransportStartTls(true);
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc, false);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.hasError(), resp.getErrorMessage());
        resp.fillObject(acc);

        MailAccountGetRequest getReq = new MailAccountGetRequest(acc.getId());
        MailAccountGetResponse getResp = getClient().execute(getReq);
        assertFalse(getResp.hasError(), getResp.getErrorMessage());
        mailAccount = getResp.getAsDescription();
        assertTrue(mailAccount.isMailStartTls());
        assertTrue(mailAccount.isTransportStartTls());
    }

    //         @Test
    //     public void testUpdateMailAccountWithStartTls() throws Exception {
    //        MailAccountDescription acc = createMailAccountObject();
    //        acc.setName(UUID.randomUUID().toString());
    //        MailAccountInsertRequest req = new MailAccountInsertRequest(acc, false);
    //        MailAccountInsertResponse resp = client.execute(req);
    //        assertFalse(resp.getErrorMessage(), resp.hasError());
    //        resp.fillObject(acc);
    //
    //        MailAccountGetRequest getReq = new MailAccountGetRequest(acc.getId());
    //        MailAccountGetResponse getResp = client.execute(getReq);
    //        assertFalse(getResp.getErrorMessage(), getResp.hasError());
    //        mailAccount = getResp.getAsDescription();
    //        assertFalse(mailAccount.isMailStartTls());
    //        assertFalse(mailAccount.isTransportStartTls());
    //        mailAccount.setMailStartTls(true);
    //        mailAccount.setTransportStartTls(true);
    //        MailAccountUpdateRequest updateReq = new MailAccountUpdateRequest(mailAccount);
    //        MailAccountUpdateResponse updateResp = client.execute(updateReq);
    //        assertFalse(updateResp.getErrorMessage(), updateResp.hasError());
    //
    //        getReq = new MailAccountGetRequest(mailAccount.getId());
    //        getResp = client.execute(getReq);
    //        assertFalse(getResp.getErrorMessage(), getResp.hasError());
    //        mailAccount = getResp.getAsDescription();
    //        assertTrue(mailAccount.isMailStartTls());
    //        assertTrue(mailAccount.isTransportStartTls());
    //    }

    @Test
    public void testCreateMailAccountWithDefaults() throws Exception {
        MailAccountDescription acc = createMailAccountObject();
        acc.setName(UUID.randomUUID().toString());
        MailAccountInsertRequest req = new MailAccountInsertRequest(acc, false);
        MailAccountInsertResponse resp = getClient().execute(req);
        assertFalse(resp.hasError(), resp.getErrorMessage());
        resp.fillObject(acc);

        MailAccountGetRequest getReq = new MailAccountGetRequest(acc.getId());
        MailAccountGetResponse getResp = getClient().execute(getReq);
        assertFalse(getResp.hasError(), getResp.getErrorMessage());
        mailAccount = getResp.getAsDescription();
        assertFalse(mailAccount.isMailStartTls());
        assertFalse(mailAccount.isTransportStartTls());
    }

}
