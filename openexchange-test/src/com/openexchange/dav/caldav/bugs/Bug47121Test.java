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

package com.openexchange.dav.caldav.bugs;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.Config;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.CalendarTestManager;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * {@link Bug47121Test}
 *
 * DAVCollection.protocolException null
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.3
 */
public class Bug47121Test extends Abstract2UserCalDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.MACOS_10_7_3;
    }

    private CalendarTestManager manager2;
    private FolderObject subfolder;
    private String sharedFolderID;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager2 = new CalendarTestManager(client2);
        manager2.setFailOnError(true);
        manager2.resetDefaultFolderPermissions();
        ftm.setClient(client2);
        FolderObject calendarFolder = ftm.getFolderFromServer(manager2.getPrivateFolder());
        String subFolderName = "testfolder_" + randomUID();
        FolderObject folder = new FolderObject();
        folder.setFolderName(subFolderName);
        folder.setParentFolderID(calendarFolder.getObjectID());
        folder.setModule(calendarFolder.getModule());
        folder.setType(calendarFolder.getType());
        OCLPermission perm = new OCLPermission();
        perm.setEntity(getClient().getValues().getUserId());
        perm.setGroupPermission(false);
        perm.setAllPermission(OCLPermission.CREATE_SUB_FOLDERS, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        List<OCLPermission> permissions = calendarFolder.getPermissions();
        permissions.add(perm);
        folder.setPermissions(calendarFolder.getPermissions());
        subfolder= ftm.insertFolderOnServer(folder);
        sharedFolderID = String.valueOf(subfolder.getObjectID());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUnsubscribeFromSharedCollection(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * try to delete the shared folder collection & expect positive response code
         */
        DeleteMethod delete = null;
        try {
            String href = Config.getPathPrefix() + "/caldav/" + encodeFolderID(sharedFolderID);
            delete = new DeleteMethod(getBaseUri() + href + "/");
            Assertions.assertEquals(HttpServletResponse.SC_NO_CONTENT, webDAVClient.executeMethod(delete), "response code wrong");
        } finally {
            release(delete);
        }
        /*
         * verify that folder was not deleted
         */
        Assertions.assertNotNull(getFolder(Integer.parseInt(sharedFolderID)));
    }

}
