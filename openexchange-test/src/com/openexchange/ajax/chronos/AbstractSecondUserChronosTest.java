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

package com.openexchange.ajax.chronos;

import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractSecondUserChronosTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.3
 */
public class AbstractSecondUserChronosTest extends AbstractChronosTest {

    protected UserApi userApi2;
    protected EventManager eventManager2;
    protected String defaultFolderId2;


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        SessionAwareClient apiClient2 = testUser2.getApiClient();
        userApi2 = new UserApi(apiClient2, testUser2);
        defaultFolderId2 = getDefaultFolder(userApi2.getFoldersApi());
        eventManager2 = new EventManager(userApi2, defaultFolderId2);
    }
}