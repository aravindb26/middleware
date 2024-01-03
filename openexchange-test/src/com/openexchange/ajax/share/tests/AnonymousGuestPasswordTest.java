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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.passwordchange.actions.PasswordChangeUpdateRequest;
import com.openexchange.ajax.passwordchange.actions.PasswordChangeUpdateResponse;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.share.actions.UpdateLinkRequest;
import com.openexchange.ajax.share.actions.UpdateLinkResponse;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.recipient.AnonymousRecipient;
import com.openexchange.share.recipient.RecipientType;

/**
 * {@link AnonymousGuestPasswordTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AnonymousGuestPasswordTest extends ShareTest {

    /**
     * Initializes a new {@link AnonymousGuestPasswordTest}.
     *
     * @param name The test name
     */
    public AnonymousGuestPasswordTest() {
        super();
    }

    @Test
    public void testUpdatePasswordForAnonymousGuest() throws Exception {
        OCLGuestPermission guestPermission = createAnonymousGuestPermission();
        /*
         * create folder shared to guest user
         */
        int module = randomModule();
        EnumAPI api = randomFolderAPI();
        FolderObject folder = insertSharedFolder(api, module, getDefaultFolder(module), guestPermission);
        /*
         * check permissions
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
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertEquals(RecipientType.ANONYMOUS, guest.getType());
        assertNull(guest.getPassword(), "Password is set");
        /*
         * update recipient, set a password for the anonymous guest
         */
        ShareTarget shareTarget = new ShareTarget(module, String.valueOf(folder.getObjectID()));
        UpdateLinkRequest updateLinkRequest = new UpdateLinkRequest(shareTarget, getClient().getValues().getTimeZone(), folder.getLastModified().getTime());
        updateLinkRequest.setPassword("secret");
        ((AnonymousRecipient) guestPermission.getRecipient()).setPassword("secret"); // for subsequent comparison
        UpdateLinkResponse updateLinkResponse = getClient().execute(updateLinkRequest);
        Date clientTimestamp = updateLinkResponse.getTimestamp();
        /*
         * discover & check guest
         */
        guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertEquals(RecipientType.ANONYMOUS, guest.getType());
        assertNotNull(guest.getPassword(), "Password not set");
        assertEquals("secret", guest.getPassword(), "Password wrong");
        /*
         * update recipient, change password for the anonymous guest
         */
        updateLinkRequest = new UpdateLinkRequest(shareTarget, getClient().getValues().getTimeZone(), clientTimestamp.getTime());
        updateLinkRequest.setPassword("geheim");
        ((AnonymousRecipient) guestPermission.getRecipient()).setPassword("geheim"); // for subsequent comparison
        updateLinkResponse = getClient().execute(updateLinkRequest);
        clientTimestamp = updateLinkResponse.getTimestamp();
        /*
         * discover & check guest
         */
        guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertEquals(RecipientType.ANONYMOUS, guest.getType());
        assertNotNull(guest.getPassword(), "Password not set");
        assertEquals("geheim", guest.getPassword(), "Password wrong");
        /*
         * update recipient remove password for the anonymous guest
         */
        updateLinkRequest = new UpdateLinkRequest(shareTarget, getClient().getValues().getTimeZone(), clientTimestamp.getTime());
        updateLinkRequest.setPassword(null);
        ((AnonymousRecipient) guestPermission.getRecipient()).setPassword(null); // for subsequent comparison
        updateLinkResponse = getClient().execute(updateLinkRequest);
        clientTimestamp = updateLinkResponse.getTimestamp();
        /*
         * discover & check guest
         */
        guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertEquals(RecipientType.ANONYMOUS, guest.getType());
        assertNull(guest.getPassword(), "Password is set");
    }

    @Test
    public void testDontAllowAnonymousGuestPasswordUpdate() throws Exception {
        OCLGuestPermission guestPermission = createAnonymousGuestPermission("secret");
        /*
         * create folder shared to guest user
         */
        int module = randomModule();
        EnumAPI api = randomFolderAPI();
        FolderObject folder = insertSharedFolder(api, module, getDefaultFolder(module), guestPermission);
        /*
         * check permissions
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
        guestPermission.setEntity(matchingPermission.getEntity());
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), guestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        /*
         * try to update password
         */
        String newPassword = "secret2";
        PasswordChangeUpdateRequest updateRequest = new PasswordChangeUpdateRequest(newPassword, ((AnonymousRecipient) guestPermission.getRecipient()).getPassword(), false);
        PasswordChangeUpdateResponse response = guestClient.execute(updateRequest);
        assertTrue(response.hasError(), "No errors in response");
        /*
         * check if share link still accessible with old password
         */
        guestClient = resolveShare(guest, guestPermission.getRecipient(), guestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        /*
         * check access to share with new password
         */
        GuestClient revokedGuestClient = new GuestClient(testContext.getUsedBy(), guest.getShareURL(), null, newPassword, false);
        assertTrue(revokedGuestClient.getLoginResponse().hasError(), "No errors during login with new password");
        assertNull(revokedGuestClient.getLoginResponse().getSessionId(), "Got session ID from login with new password");
    }

}
