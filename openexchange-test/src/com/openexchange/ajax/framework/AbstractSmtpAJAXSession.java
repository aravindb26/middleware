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

package com.openexchange.ajax.framework;

import com.openexchange.ajax.smtptest.MailManager;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public abstract class AbstractSmtpAJAXSession extends AbstractAPIClientSession {

    protected TestUser noReplyUser;
    private ApiClient noReplyApiClient;
    private AJAXClient noReplyAJAXClient;

    protected MailManager noReplyMailManager;
    protected MailManager mailManager;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        mailManager = new MailManager(getApiClient());
        noReplyUser = testContext.acquireNoReplyUser();
        noReplyAJAXClient = noReplyUser.getAjaxClient();
        noReplyApiClient = noReplyUser.getApiClient();
        noReplyMailManager = new MailManager(noReplyApiClient);
        noReplyMailManager.clearMails();
    }

    public AJAXClient getNoReplyAJAXClient() {
        return noReplyAJAXClient;
    }

    public ApiClient getNoReplyApiClient() {
        return noReplyApiClient;
    }
}
