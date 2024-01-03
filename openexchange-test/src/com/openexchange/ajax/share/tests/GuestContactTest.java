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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.action.DeleteRequest;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.user.actions.GetRequest;
import com.openexchange.ajax.user.actions.GetResponse;
import com.openexchange.ajax.user.actions.UpdateRequest;
import com.openexchange.ajax.user.actions.UpdateResponse;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.share.recipient.GuestRecipient;
import com.openexchange.user.User;

/**
 * {@link GuestContactTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.0
 */
public class GuestContactTest extends ShareTest {

    /**
     * Initializes a new {@link GuestContactTest}.
     *
     * @param name
     */
    public GuestContactTest() {
        super();
    }

    @Test
    public void testCreateGuestContact() throws Exception {
        /*
         * create folder and a shared file inside
         */

        FolderObject folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE));
        OCLGuestPermission guestOCLPermission = createNamedGuestPermission();
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(guestOCLPermission);
        String guestName = ((GuestRecipient) guestOCLPermission.getRecipient()).getDisplayName();
        String guestMail = ((GuestRecipient) guestOCLPermission.getRecipient()).getEmailAddress();
        File file = insertSharedFile(folder.getObjectID(), guestPermission);
        /*
         * check permissions
         */
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission permission : file.getObjectPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        checkPermissions(guestPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertTrue(guest.getEntity() > -1, "Guest id must not be -1");
        GuestClient guestClient = resolveShare(discoverShareURL(guestOCLPermission.getApiClient(), guest), guestPermission.getRecipient());
        GetRequest guestGetRequest = new GetRequest(guest.getEntity(), guestClient.getValues().getTimeZone());
        GetResponse guestGetResponse = guestClient.execute(guestGetRequest);
        Contact guestContact = guestGetResponse.getContact();
        GetRequest getRequest = new GetRequest(guest.getEntity(), getClient().getValues().getTimeZone());
        GetResponse getResponse = getClient().execute(getRequest);
        Contact contact = getResponse.getContact();
        assertEquals(contact, guestContact, "Contacts does not match");
        assertNotNull(contact, "Contact is null.");
        assertEquals(guestName, contact.getDisplayName(), "Wrong display name.");
        assertEquals(guestMail, contact.getEmail1(), "Wrong email address.");
        assertTrue(contact.getObjectID() != 0, "Contact id is 0.");
    }

    @Test
    public void testUpdateGuestContact() throws Exception {
        /*
         * create folder and a shared file inside
         */
        FolderObject folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE));
        OCLGuestPermission oclGuestPermission = createNamedGuestPermission();
        String guestName = oclGuestPermission.getName();
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(oclGuestPermission);
        File file = insertSharedFile(folder.getObjectID(), guestPermission);
        /*
         * check permissions
         */
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission permission : file.getObjectPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        checkPermissions(guestPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertTrue(guest.getEntity() > -1, "Guest id must not be -1");
        GuestClient guestClient = resolveShare(discoverShareURL(oclGuestPermission.getApiClient(), guest), guestPermission.getRecipient());
        GetRequest guestGetRequest = new GetRequest(guest.getEntity(), guestClient.getValues().getTimeZone());
        GetResponse guestGetResponse = guestClient.execute(guestGetRequest);
        Contact guestContact = guestGetResponse.getContact();
        User guestUser = guestGetResponse.getUser();
        /*
         * update guest's display name
         */
        guestContact.setDisplayName(guestName + "_modified");
        UpdateRequest updateRequest = new UpdateRequest(guestContact, guestUser);
        UpdateResponse updateResponse = guestClient.execute(updateRequest);
        assertFalse(updateResponse.hasError(), updateResponse.getErrorMessage());
        /*
         * check guest
         */
        GetRequest getRequest = new GetRequest(guest.getEntity(), getClient().getValues().getTimeZone());
        GetResponse getResponse = getClient().execute(getRequest);
        Contact contact = getResponse.getContact();
        assertEquals(guestName + "_modified", contact.getDisplayName(), "Display name was not updated");
        /*
         * try to update guest's display name as sharing user
         */
        contact.setDisplayName(guestName);
        UpdateRequest updateRequest2 = new UpdateRequest(guestContact, guestUser, false);
        UpdateResponse updateResponse2 = getClient().execute(updateRequest2);
        assertTrue(updateResponse2.hasError(), "Client was able to update foreign contact.");
        assertEquals(ContactExceptionCodes.NO_CHANGE_PERMISSION.getNumber(), updateResponse2.getException().getCode());
    }

    @Test
    public void testOtherUser() throws Exception {
        /*
         * create folder and a shared file inside
         */
        FolderObject folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE));
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(createNamedGuestPermission());
        GuestRecipient guestRecipient = (GuestRecipient) guestPermission.getRecipient();
        String guestName = guestRecipient.getDisplayName();
        String guestMail = guestRecipient.getEmailAddress();
        File file = insertSharedFile(folder.getObjectID(), guestPermission);
        /*
         * check permissions
         */
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission permission : file.getObjectPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        checkPermissions(guestPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        assertTrue(guest.getEntity() > -1, "Guest id must not be -1");
        /*
         * get guest contact as other user
         */
        AJAXClient secondClient = testUser2.getAjaxClient();
        GetRequest getRequest = new GetRequest(guest.getEntity(), secondClient.getValues().getTimeZone());
        GetResponse getResponse = secondClient.execute(getRequest);
        assertFalse(getResponse.hasError(), "Contact could not be loaded.");
        Contact contact = getResponse.getContact();
        User user = getResponse.getUser();
        assertNotEquals(guestName, contact.getDisplayName(), "Wrong contact loaded.");
        assertNotEquals(guestMail, contact.getEmail1(), "Wrong contact loaded.");
        /*
         * try to update guest's display name as foreign user
         */
        contact.setDisplayName("This should not work");
        UpdateRequest updateRequest = new UpdateRequest(contact, user, false);
        UpdateResponse updateResponse = secondClient.execute(updateRequest);
        assertTrue(updateResponse.hasError(), "Any user can change contact data.");
        assertEquals(ContactExceptionCodes.NO_CHANGE_PERMISSION.getNumber(), updateResponse.getException().getCode());
        /*
         * try to delete guest contact as foreign user
         */
        DeleteRequest deleteRequest = new DeleteRequest(contact, false);
        CommonDeleteResponse deleteResponse = secondClient.execute(deleteRequest);
        assertTrue(deleteResponse.hasError(), "Any user can delete any shares.");
    }

}
