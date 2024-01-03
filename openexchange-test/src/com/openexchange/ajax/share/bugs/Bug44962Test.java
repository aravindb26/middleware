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

import static com.openexchange.java.Autoboxing.B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.UpdateRequest;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.share.Abstract2UserShareTest;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * {@link Bug44962Test}
 *
 * "readcreatesharedfolders" checked for infostore folders
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
@Execution(ExecutionMode.SAME_THREAD)
public class Bug44962Test extends Abstract2UserShareTest {

    protected static int randomPimModule() {
        int[] pimModules = new int[] { FolderObject.CONTACT, FolderObject.TASK, FolderObject.CALENDAR
        };
        return pimModules[random.nextInt(pimModules.length)];
    }

    @Test
    public void testInviteGuestToDriveFolderWithInviteGuestsAndReadCreateSharedFolders() throws Exception {
        inviteGuestToFolder(FolderObject.INFOSTORE, true, Boolean.TRUE, null);
    }

    @Test
    public void testDontInviteGuestToDriveFolder() throws Exception {
        inviteGuestToFolder(FolderObject.INFOSTORE, false, Boolean.FALSE, "SHR-0019");
    }

    @Test
    public void testInviteGuestToDriveFolderWithInviteGuests() throws Exception {
        inviteGuestToFolder(FolderObject.INFOSTORE, false, Boolean.TRUE, null);
    }

    @Test
    public void testInviteUserToDriveFolderWithReadCreateSharedFolders() throws Exception {
        inviteUserToFolder(FolderObject.INFOSTORE, true, Boolean.FALSE, null);
    }

    @Test
    public void testInviteUserToDriveFolderWithInviteGuestsAndReadCreateSharedFolders() throws Exception {
        inviteUserToFolder(FolderObject.INFOSTORE, true, Boolean.TRUE, null);
    }

    @Test
    public void testInviteUserToDriveFolder() throws Exception {
        inviteUserToFolder(FolderObject.INFOSTORE, false, Boolean.FALSE, null);
    }

    @Test
    public void testInviteUserToDriveFolderWithInviteGuests() throws Exception {
        inviteUserToFolder(FolderObject.INFOSTORE, false, Boolean.TRUE, null);
    }

    @Test
    public void testInviteGuestToPimFolderWithInviteGuestsAndReadCreateSharedFolders() throws Exception {
        inviteGuestToFolder(randomPimModule(), true, Boolean.TRUE, null);
    }

    @Test
    public void testDontInviteGuestToPimFolder() throws Exception {
        inviteGuestToFolder(randomPimModule(), false, Boolean.FALSE, "SHR-0019");
    }

    @Test
    public void testDontInviteGuestToPimFolderWithInviteGuests() throws Exception {
        inviteGuestToFolder(randomPimModule(), false, Boolean.TRUE, "FLD-0072");
    }

    @Test
    public void testInviteUserToPimFolderWithReadCreateSharedFolders() throws Exception {
        inviteUserToFolder(randomPimModule(), true, Boolean.FALSE, null);
    }

    @Test
    public void testInviteUserToPimFolderWithInviteGuestsAndReadCreateSharedFolders() throws Exception {
        inviteUserToFolder(randomPimModule(), true, Boolean.TRUE, null);
    }

    @Test
    public void testDontInviteUserToPimFolder() throws Exception {
        inviteUserToFolder(randomPimModule(), false, Boolean.FALSE, "FLD-0072");
    }

    @Test
    public void testDontInviteUserToPimFolderWithInviteGuests() throws Exception {
        inviteUserToFolder(randomPimModule(), false, Boolean.TRUE, "FLD-0072");
    }

    @Test
    public void testInviteGuestToPublicPimFolderWithInviteGuestsAndEditPublicFolders() throws Exception {
        inviteGuestToPublicFolder(randomPimModule(), true, Boolean.TRUE, null);
    }

    @Test
    public void testDontInviteGuestToPublicPimFolder() throws Exception {
        inviteGuestToPublicFolder(randomPimModule(), false, Boolean.FALSE, "SHR-0019");
    }

    @Test
    public void testDontInviteGuestToPublicPimFolderWithInviteGuests() throws Exception {
        inviteGuestToPublicFolder(randomPimModule(), false, Boolean.TRUE, "FLD-0005");
    }

    @Test
    public void testInviteUserToPublicPimFolderWithEditPublicFolders() throws Exception {
        inviteUserToPublicFolder(randomPimModule(), true, Boolean.FALSE, null);
    }

    @Test
    public void testInviteUserToPublicPimFolderWithInviteGuestsAndEditPublicFolders() throws Exception {
        inviteUserToPublicFolder(randomPimModule(), true, Boolean.TRUE, null);
    }

    @Test
    public void testDontInviteUserToPublicPimFolder() throws Exception {
        inviteUserToPublicFolder(randomPimModule(), false, Boolean.FALSE, "FLD-0005");
    }

    @Test
    public void testDontInviteUserToPublicPimFolderWithInviteGuests() throws Exception {
        inviteUserToPublicFolder(randomPimModule(), false, Boolean.TRUE, "FLD-0005");
    }

    private void setReadCreateSharedFoldersAndEditPublicFolders(boolean readCreateSharedFolders, boolean editPublicFolders) throws Exception {
        ConfigAwareProvisioningService.getService().changeModuleAccess(client2.getValues().getContextId(), client2.getValues().getUserId(), //@formatter:off
            UserModuleAccess.Builder.newInstance()
                .readCreateSharedFolders(B(readCreateSharedFolders))
                .editPublicFolders(B(editPublicFolders)).build(),
             testUser2.getCreatedBy());//@formatter:on
    }

    private void setInviteGuests(Boolean inviteGuests) throws Exception {
        changeCapability("invite_guests", inviteGuests);
    }

    private void changeCapability(String capability, Boolean value) throws Exception {
        if (null == value) {
            ConfigAwareProvisioningService.getService().changeCapability(client2.getValues().getContextId(), client2.getValues().getUserId(), Collections.<String> emptySet(), Collections.<String> emptySet(), Collections.singleton(capability), testUser2.getCreatedBy());
        } else if (Boolean.TRUE.equals(value)) {
            ConfigAwareProvisioningService.getService().changeCapability(client2.getValues().getContextId(), client2.getValues().getUserId(),  Collections.singleton(capability), Collections.<String> emptySet(), Collections.<String> emptySet(), testUser2.getCreatedBy());
        } else {
            ConfigAwareProvisioningService.getService().changeCapability(client2.getValues().getContextId(), client2.getValues().getUserId(), Collections.<String> emptySet(), Collections.singleton(capability), Collections.<String> emptySet(), testUser2.getCreatedBy());
        }
    }

    private void prepareUser(boolean readCreateSharedFolders, boolean editPublicFolders, Boolean inviteGuests) throws Exception {
        setInviteGuests(inviteGuests);
        setReadCreateSharedFoldersAndEditPublicFolders(readCreateSharedFolders, editPublicFolders);
    }

    private void inviteGuestToFolder(int module, boolean readCreateSharedFolders, Boolean inviteGuests, String expectedError) throws Exception {
        prepareUser(readCreateSharedFolders, false, inviteGuests);
        FolderObject folder = insertPrivateFolder(client2, EnumAPI.OX_NEW, module, getDefaultFolder(client2, module));
        folder.getPermissions().add(createNamedGuestPermission());
        UpdateRequest updateRequest = new UpdateRequest(EnumAPI.OX_NEW, folder);
        updateRequest.setFailOnError(false);
        executeAndCheck(updateRequest, expectedError);
    }

    private void inviteGuestToPublicFolder(int module, boolean editPublicFolders, Boolean inviteGuests, String expectedError) throws Exception {
        setReadCreateSharedFoldersAndEditPublicFolders(false, true);
        FolderObject folder = insertPublicFolder(client2, EnumAPI.OX_NEW, module);
        prepareUser(false, editPublicFolders, inviteGuests);
        folder.getPermissions().add(createNamedGuestPermission());
        UpdateRequest updateRequest = new UpdateRequest(EnumAPI.OX_NEW, folder);
        updateRequest.setFailOnError(false);
        executeAndCheck(updateRequest, expectedError);
    }

    private void inviteUserToFolder(int module, boolean readCreateSharedFolders, Boolean inviteGuests, String expectedError) throws Exception {
        prepareUser(readCreateSharedFolders, false, inviteGuests);
        FolderObject folder = insertPrivateFolder(client2, EnumAPI.OX_NEW, module, getDefaultFolder(client2, module));
        OCLPermission permission = new OCLPermission(getClient().getValues().getUserId(), folder.getObjectID());
        permission.setAllPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS);
        folder.getPermissions().add(permission);
        UpdateRequest updateRequest = new UpdateRequest(EnumAPI.OX_NEW, folder);
        updateRequest.setFailOnError(false);
        executeAndCheck(updateRequest, expectedError);
    }

    private void inviteUserToPublicFolder(int module, boolean editPublicFolders, Boolean inviteGuests, String expectedError) throws Exception {
        setReadCreateSharedFoldersAndEditPublicFolders(false, true);
        FolderObject folder = insertPublicFolder(client2, EnumAPI.OX_NEW, module);
        prepareUser(false, editPublicFolders, inviteGuests);
        OCLPermission permission = new OCLPermission(getClient().getValues().getUserId(), folder.getObjectID());
        permission.setAllPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS);
        folder.getPermissions().add(permission);
        UpdateRequest updateRequest = new UpdateRequest(EnumAPI.OX_NEW, folder);
        updateRequest.setFailOnError(false);
        executeAndCheck(updateRequest, expectedError);
    }

    private void executeAndCheck(AJAXRequest<?> request, String expectedError) throws Exception {
        AbstractAJAXResponse response = client2.execute(request);
        if (null == expectedError) {
            /*
             * expect no errors
             */
            assertFalse(response.hasError(), response.getErrorMessage());
        } else {
            /*
             * expect to fail with error code
             */
            assertTrue(response.hasError(), "No error in response");
            assertEquals(expectedError, response.getException().getErrorCode(), "Unexpected error code");
        }
    }

}
