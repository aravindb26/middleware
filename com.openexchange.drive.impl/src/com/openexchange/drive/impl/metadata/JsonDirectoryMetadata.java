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

package com.openexchange.drive.impl.metadata;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import com.openexchange.drive.DriveExceptionCodes;
import com.openexchange.drive.impl.DriveUtils;
import com.openexchange.drive.impl.internal.SyncSession;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFileStorageFolder;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageCapability;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageFolderPermissionType;
import com.openexchange.file.storage.FileStoragePermission;
import com.openexchange.file.storage.TypeAware;
import com.openexchange.file.storage.composition.FolderID;
import com.openexchange.folderstorage.Permissions;
import com.openexchange.folderstorage.type.DocumentsType;
import com.openexchange.folderstorage.type.MusicType;
import com.openexchange.folderstorage.type.PicturesType;
import com.openexchange.folderstorage.type.TemplatesType;
import com.openexchange.folderstorage.type.TrashType;
import com.openexchange.folderstorage.type.VideosType;
import com.openexchange.java.Strings;
import com.openexchange.share.GuestInfo;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.user.User;

/**
 * {@link JsonDirectoryMetadata}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class JsonDirectoryMetadata extends AbstractJsonMetadata {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JsonDirectoryMetadata.class);
    }

    private final FileStorageFolder folder;
    private final String folderID;

    /**
     * Initializes a new {@link JsonDirectoryMetadata}.
     *
     * @param session The sync session
     * @param folder The folder to create the metadata for
     */
    public JsonDirectoryMetadata(SyncSession session, FileStorageFolder folder) {
        super(session);
        this.folder = folder;
        this.folderID = folder.getId();
    }

    /**
     * Builds the JSON representation of this directory metadata.
     *
     * @return A JSON object holding the metadata information
     */
    public JSONObject build() throws OXException {
        return build(true);
    }

    /**
     * Builds the JSON representation of this directory metadata.
     *
     * @param includeFiles <code>true</code> to include metadata of the contained files, <code>false</code>, otherwise
     * @return A JSON object holding the metadata information
     */
    public JSONObject build(boolean includeFiles) throws OXException {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", folder.getId());
            String localizedName = folder.getLocalizedName(session.getDriveSession().getLocale());
            if (Strings.isNotEmpty(localizedName)) {
                jsonObject.put("localized_name", localizedName);
            }
            if (folder.isDefaultFolder()) {
                jsonObject.put("default_folder", true);
            }
            if (folder.hasSubfolders()) {
                jsonObject.put("has_subfolders", true);
            }
            if (false == DriveUtils.isSynchronizable(folderID, session.getConfig())) {
                jsonObject.put("not_synchronizable", true);
            }
            if ((folder instanceof TypeAware)) {
                switch (((TypeAware) folder).getType()) {
                    case DOCUMENTS_FOLDER:
                        jsonObject.put("type", DocumentsType.getInstance().getType());
                        break;
                    case TEMPLATES_FOLDER:
                        jsonObject.put("type", TemplatesType.getInstance().getType());
                        break;
                    case MUSIC_FOLDER:
                        jsonObject.put("type", MusicType.getInstance().getType());
                        break;
                    case PICTURES_FOLDER:
                        jsonObject.put("type", PicturesType.getInstance().getType());
                        break;
                    case TRASH_FOLDER:
                        jsonObject.put("type", TrashType.getInstance().getType());
                        break;
                    case VIDEOS_FOLDER:
                        jsonObject.put("type", VideosType.getInstance().getType());
                        break;
                    default:
                        break;
                }
            }
            Set<String> capabilities = folder.getCapabilities();
            if (null != capabilities && capabilities.contains(FileStorageFolder.CAPABILITY_PERMISSIONS)) {
                session.getPermissionResolver().cacheFileStorageFolderPermissionEntities(Collections.singletonList(folder));
                jsonObject.put("own_rights", createPermissionBits(folder.getOwnPermission()));
                jsonObject.putOpt("permissions", getJSONPermissions(folder.getPermissions(), false));
                jsonObject.putOpt("extended_permissions", getJSONPermissions(folder.getPermissions(), true));
                jsonObject.put("jump", new JSONArray(Collections.singleton("permissions")));
                if (isShared(folder)) {
                    jsonObject.put("shared", true);
                }
                if (folder.getOwnPermission().isAdmin()) {
                    jsonObject.put("shareable", true);
                }
            }
            if (includeFiles) {
                jsonObject.putOpt("files", getJSONFiles());
            }
            return jsonObject;
        } catch (JSONException e) {
            throw DriveExceptionCodes.IO_ERROR.create(e, e.getMessage());
        }
    }

    private JSONArray getJSONFiles() throws JSONException, OXException {
        List<FileStorageCapability> specialCapabilites = new ArrayList<FileStorageCapability>();
        List<Field> fields = new ArrayList<Field>(Arrays.asList(
            Field.CREATED, Field.LAST_MODIFIED, Field.FILENAME, Field.CREATED_BY, Field.MODIFIED_BY, Field.FILE_MIMETYPE, Field.FILE_SIZE));
        FolderID folderID = new FolderID(this.folderID);
        if (session.getStorage().supports(folderID, FileStorageCapability.OBJECT_PERMISSIONS)) {
            specialCapabilites.add(FileStorageCapability.OBJECT_PERMISSIONS);
            fields.add(Field.OBJECT_PERMISSIONS);
            fields.add(Field.SHAREABLE);
        }
        if (session.getStorage().supports(folderID, FileStorageCapability.LOCKS)) {
            specialCapabilites.add(FileStorageCapability.LOCKS);
            fields.add(Field.LOCKED_UNTIL);
        }
        if (session.getStorage().supports(folderID, FileStorageCapability.FILE_VERSIONS)) {
            specialCapabilites.add(FileStorageCapability.FILE_VERSIONS);
            fields.add(Field.NUMBER_OF_VERSIONS);
            fields.add(Field.VERSION);
            fields.add(Field.VERSION_COMMENT);
        }
        List<File> files = session.getStorage().getFilesInFolder(this.folderID, false, false, null, fields);
        return getJSONFiles(files, specialCapabilites.toArray(new FileStorageCapability[specialCapabilites.size()]));
    }

    private JSONArray getJSONFiles(List<File> files, FileStorageCapability[] specialCapabilities) throws JSONException, OXException {
        if (null == files) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(files.size());
        for (File file : files) {
            if (false == (file instanceof DriveMetadata)) {
                jsonArray.put(new JsonFileMetadata(session, file).build(specialCapabilities));
            }
        }
        return jsonArray;
    }

    private JSONArray getJSONPermissions(List<FileStoragePermission> permissions, boolean extended) throws JSONException, OXException {
        if (null == permissions) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(permissions.size());
        for (FileStoragePermission permission : permissions) {
            if (extended) {
                jsonArray.put(getExtendedJSONPermission(permission));
            } else {
                if (FileStorageFolderPermissionType.INHERITED.equals(permission.getType())) {
                    continue; // skip inherited permissions for plain permission array implicitly
                }
                jsonArray.put(getJSONPermission(permission));
            }
        }
        return jsonArray;
    }

    private JSONObject getJSONPermission(FileStoragePermission permission) throws JSONException {
        JSONObject jsonObject = new JSONObject(4);
        jsonObject.put("bits", createPermissionBits(permission));
        jsonObject.put("entity", permission.getEntity());
        jsonObject.put("group", permission.isGroup());
        return jsonObject;
    }

    private JSONObject getExtendedJSONPermission(FileStoragePermission permission) throws JSONException, OXException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("entity", permission.getEntity());
        jsonObject.put("bits", Permissions.createPermissionBits(permission.getFolderPermission(), permission.getReadPermission(),
            permission.getWritePermission(), permission.getDeletePermission(), permission.isAdmin()));
        if (FileStorageFolderPermissionType.INHERITED.equals(permission.getType())) {
            jsonObject.put("isInherited", true);
            jsonObject.put("isInheritedFrom", permission.getPermissionLegator());
        }
        if (permission.isGroup()) {
            jsonObject.put("type", "group");
            addGroupInfo(jsonObject, session.getPermissionResolver().getGroup(permission.getEntity()));
        } else {
            User user = session.getPermissionResolver().getUser(permission.getEntity());
            if (null == user) {
                LoggerHolder.LOGGER.debug("Can't resolve user entity {} for folder {}", I(permission.getEntity()), folder);
            } else if (user.isGuest()) {
                GuestInfo guest = session.getPermissionResolver().getGuest(user.getId());
                if (guest == null) {
                    int contextId = session.getServerSession().getContextId();
                    throw ShareExceptionCodes.UNEXPECTED_ERROR.create("Could not resolve guest info for ID " + user.getId() + " in context " + contextId + ". " +
                        "It might have been deleted in the mean time or is in an inconsistent state.");
                }

                jsonObject.put("type", guest.getRecipientType().toString().toLowerCase());
                if (RecipientType.ANONYMOUS.equals(guest.getRecipientType())) {
                    if (FileStorageFolderPermissionType.INHERITED.equals(permission.getType())) {
                        DefaultFileStorageFolder legator = new DefaultFileStorageFolder();
                        legator.setId(permission.getPermissionLegator());
                        addShareInfo(jsonObject, session.getPermissionResolver().getShare(legator, permission.getEntity()));
                    } else {
                        addShareInfo(jsonObject, session.getPermissionResolver().getShare(folder, permission.getEntity()));
                    }
                } else {
                    addUserInfo(jsonObject, user);
                }
            } else {
                jsonObject.put("type", "user");
                addUserInfo(jsonObject, user);
            }
        }
        return jsonObject;
    }

    private boolean isShared(FileStorageFolder folder) {
        List<FileStoragePermission> permissions = folder.getPermissions();
        if (null != permissions && 0 < permissions.size()) {
            int userID = session.getServerSession().getUserId();
            for (FileStoragePermission permission : permissions) {
                if (FileStorageFolderPermissionType.INHERITED.equals(permission.getType())) {
                    continue; // skip inherited permissions for plain permission array implicitly
                }
                if (permission.getEntity() != userID) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int createPermissionBits(FileStoragePermission permission) {
        return Permissions.createPermissionBits(permission.getFolderPermission(), permission.getReadPermission(),
            permission.getWritePermission(), permission.getDeletePermission(), permission.isAdmin());
    }

}
