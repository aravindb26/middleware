
package com.openexchange.ajax.folder.api2;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.framework.Abstrac2UserAJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;

public class ChangePermissionsTest extends Abstrac2UserAJAXSession {

    private FolderObject folder;


    public void notestChangePermissionsSuccess() throws Exception {
        String folderName = "ChangePermissionsTest Folder" + UUID.randomUUID().toString().replaceAll("-", "");
        folder = ftm.generatePublicFolder(folderName, FolderObject.INFOSTORE, client1.getValues().getPrivateInfostoreFolder(), new int[] { client1.getValues().getUserId() });
        final InsertRequest insertFolderReq = new InsertRequest(EnumAPI.OUTLOOK, folder, false);
        final InsertResponse insertFolderResp = client1.execute(insertFolderReq);

        assertNull(insertFolderResp.getException(), "Inserting folder caused exception.");
        insertFolderResp.fillObject(folder);

        {
            ArrayList<OCLPermission> allPermissions = new ArrayList<OCLPermission>();
            {
                OCLPermission permissions = new OCLPermission();
                permissions.setEntity(client1.getValues().getUserId());
                permissions.setGroupPermission(false);
                permissions.setFolderAdmin(true);
                permissions.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
                allPermissions.add(permissions);
            }
            {
                OCLPermission permissions = new OCLPermission();
                permissions.setEntity(client2.getValues().getUserId());
                permissions.setGroupPermission(false);
                permissions.setFolderAdmin(false);
                permissions.setAllPermission(OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
                allPermissions.add(permissions);
            }
            folder.setPermissions(allPermissions);
        }

        folder = ftm.updateFolderOnServer(folder);
        assertTrue(2 == folder.getNonSystemPermissionsAsArray().length, "Unexpected number of permissions");
    }

    @Test
    public void testChangePermissionsFail() throws Exception {
        String folderName = "ChangePermissionsTest Folder" + UUID.randomUUID().toString().replaceAll("-", "");
        folder = ftm.generatePublicFolder(folderName, FolderObject.INFOSTORE, client1.getValues().getInfostoreTrashFolder(), new int[] { client1.getValues().getUserId() });
        final InsertRequest insertFolderReq = new InsertRequest(EnumAPI.OUTLOOK, folder, false);
        final InsertResponse insertFolderResp = client1.execute(insertFolderReq);

        assertNull(insertFolderResp.getException(), "Inserting folder caused exception.");
        insertFolderResp.fillObject(folder);

        {
            ArrayList<OCLPermission> allPermissions = new ArrayList<OCLPermission>();
            {
                OCLPermission permissions = new OCLPermission();
                permissions.setEntity(client1.getValues().getUserId());
                permissions.setGroupPermission(false);
                permissions.setFolderAdmin(true);
                permissions.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
                allPermissions.add(permissions);
            }
            {
                OCLPermission permissions = new OCLPermission();
                permissions.setEntity(client2.getValues().getUserId());
                permissions.setGroupPermission(false);
                permissions.setFolderAdmin(false);
                permissions.setAllPermission(OCLPermission.READ_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
                allPermissions.add(permissions);
            }
            folder.setPermissions(allPermissions);
        }

        folder = ftm.updateFolderOnServer(folder, false);
        AbstractAJAXResponse lastResponse = ftm.getLastResponse();
        assertNotNull(lastResponse.getException(), "Updating trash folder permissions not denied, but should.");
    }
}
