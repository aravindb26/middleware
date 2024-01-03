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

package com.openexchange.ajax.share.tests;

import static com.openexchange.ajax.framework.config.util.TestConfigurationChangeUtil.changeConfigWithOwnClient;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.framework.config.util.TestConfigurationChangeUtil.Scopes;
import com.openexchange.ajax.passwordchange.actions.PasswordChangeUpdateRequest;
import com.openexchange.ajax.passwordchange.actions.PasswordChangeUpdateResponse;
import com.openexchange.ajax.session.actions.LoginResponse;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.util.UUIDs;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link EmptyGuestPasswordTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.0
 */
public class EmptyGuestPasswordTest extends ShareTest {

    private FolderObject folder;

    public EmptyGuestPasswordTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo info) throws Exception {
        super.setUp(info);
        changeConfigWithOwnClient(testUser, ImmutableMap.of(//
            "com.openexchange.passwordchange.db.enabled", Boolean.TRUE.toString()), Scopes.CONTEXT, null);
    }

    @Test
    public void testEmptyPassword() throws Exception {
        OCLGuestPermission perm = createNamedGuestPermission(false);
        folder = insertSharedFolder(EnumAPI.OX_NEW, Module.INFOSTORE.getFolderConstant(), getClient().getValues().getPrivateInfostoreFolder(), perm);
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created folder found");
        checkPermissions(perm, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(EnumAPI.OX_NEW, Module.INFOSTORE.getFolderConstant(), folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(perm, guest);
        /*
         * check access to share - a login request must not have been performed
         */
        String shareURL = discoverShareURL(perm.getApiClient(), guest);
        GuestClient guestClient = resolveShare(shareURL);
        assertNull(guestClient.getLoginResponse());
        /*
         * Set password for guest user
         */
        String newPW = UUIDs.getUnformattedStringFromRandom();
        PasswordChangeUpdateRequest pwChangeReq = new PasswordChangeUpdateRequest(newPW, "", true);
        PasswordChangeUpdateResponse pwChangeResp = guestClient.execute(pwChangeReq);
        assertFalse(pwChangeResp.hasWarnings());
        assertFalse(pwChangeResp.hasError());
        guestClient.logout();
        /*
         * Re-login with PW
         */
        guestClient = resolveShare(shareURL, ShareTest.getUsername(perm.getRecipient()), newPW);
        LoginResponse response = guestClient.getLoginResponse();
        assertNotNull(response);
        assertFalse(response.hasError());
        assertNotNull(response.getSessionId());
        /*
         * Empty password should now fail with LoginExceptionCodes.INVALID_CREDENTIALS
         */
        guestClient = resolveShare(shareURL);
        response = guestClient.getLoginResponse();
        assertNotNull(response);
        assertTrue(response.hasError());
    }

}
