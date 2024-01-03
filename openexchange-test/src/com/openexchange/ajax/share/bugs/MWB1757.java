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

package com.openexchange.ajax.share.bugs;

import static com.openexchange.java.Autoboxing.L;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.share.Abstract2UserShareTest;
import com.openexchange.file.storage.DefaultFileStorageObjectPermission;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.httpclient.models.InfoItemBody;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.modules.InfostoreApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB1757} - OX Drive Access/Permission issue for file owner of shared file
 *
 * Removing permissions for a user whose infostore module access was removed fails.
 *
 * As user1: Share a file to user2
 * As admin: remove complete drive/infostore access for user2
 * As user1: Remove permissions for user 2 to access the file -> This fails
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class MWB1757 extends Abstract2UserShareTest {

    private InfostoreApi infostoreApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        infostoreApi = new InfostoreApi(apiClient1);
    }

    @Test
    public void removeShareForNonInfostoreUserTest() throws Exception {

        //As user 1: Share a file to user 2
        DefaultFileStorageObjectPermission permissions = new DefaultFileStorageObjectPermission(testUser2.getUserId(), false, FileStorageObjectPermission.READ);
        File sharedFile = insertSharedFile(getDefaultFolder(FolderObject.INFOSTORE), permissions);

        //As admin: remove drive/infostore access from user 2
        // @formatter:off
        ConfigAwareProvisioningService.getService().changeModuleAccess(testUser2,
            UserModuleAccess.Builder.newInstance()
                .Webmail(Boolean.TRUE)
                .calendar(Boolean.TRUE)
                .infostore(Boolean.FALSE)
            .build(),
            getClass().getSimpleName());
        // @formatter:on

        //As user1: Remove permissions for user 2  to access the file
        //This was causing an error in the MW
        InfoItemData fileData = new InfoItemData();
        fileData.setObjectPermissions(Collections.emptyList());
        InfoItemBody body = new InfoItemBody();
        body.setFile(fileData);
        // @formatter:off
        InfoItemUpdateResponse response = infostoreApi.updateInfoItemBuilder()
            .withId(sharedFile.getId())
            .withTimestamp(L(Long.MAX_VALUE))
            .withInfoItemBody(body)
            .execute();
        // @formatter:on
        checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

}
