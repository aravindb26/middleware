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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.folder.actions.UpdateRequest;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.notification.ShareNotificationService.Transport;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;

/**
 * {@link MWB2118Test}
 *
 * No Option to Prevent Creation of Guest Users with Specific Email Addresses
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MWB2118Test extends ShareTest {

    
    @Override
    public TestClassConfig getTestConfig() {
        Map<String, String> contextConfig = Collections.singletonMap("com.openexchange.share.guestEmailCheckRegex", "^((?!(?:@example\\.com\\s*$)|(?:@example\\.org\\s*$)).)*$");
        return TestClassConfig.builder()
            .withContexts(2)
            .withContextConfig(TestContextConfig.builder().withConfig(contextConfig).build())
        .build();
    }

    @ParameterizedTest
    @ValueSource(strings =
    { "otto@example.com", "horst@example.org" })
    public void testInviteGuestWithInvalidEmailAddress(String invalidEmailAddress) throws Exception {
        /*
         * prepare named guest permission and a private folder
         */
        OCLGuestPermission guestPermission = createNamedGuestPermission(invalidEmailAddress, "Invited Guest", null);
        FolderObject folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE));
        /*
         * try and share the folder to the guest, expecting an exception
         */
        folder.addPermission(guestPermission);
        UpdateRequest request = new UpdateRequest(EnumAPI.OX_NEW, folder);
        request.setNotifyPermissionEntities(Transport.MAIL);
        request.setFailOnError(false);
        InsertResponse response = getClient().execute(request);
        OXException expectedException = response.getException();
        assertNotNull(expectedException);
        assertEquals("SHR-0030", expectedException.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings =
    { "horst@hallo.example.com", "example.com@test.invalid" })
    public void testInviteGuestWithValidEmailAddress(String validEmailAddress) throws Exception {
        /*
         * prepare named guest permission and a private folder
         */
        OCLGuestPermission guestPermission = createNamedGuestPermission(validEmailAddress, "Invited Guest", null);
        FolderObject folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE));
        /*
         * try and share the folder to the guest, expecting no exception
         */
        folder.addPermission(guestPermission);
        folder = updateFolder(EnumAPI.OX_NEW, folder);
        /*
         * verify set permissions
         */
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created folder found");
        checkPermissions(guestPermission, matchingPermission);
    }

}
