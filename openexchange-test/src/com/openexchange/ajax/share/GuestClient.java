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

package com.openexchange.ajax.share;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.appointment.action.AppointmentInsertResponse;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.contact.action.InsertResponse;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.folder.actions.VisibleFoldersRequest;
import com.openexchange.ajax.folder.actions.VisibleFoldersResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.framework.AbstractColumnsResponse;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.infostore.actions.AllInfostoreRequest;
import com.openexchange.ajax.infostore.actions.DeleteInfostoreRequest;
import com.openexchange.ajax.infostore.actions.DeleteInfostoreResponse;
import com.openexchange.ajax.infostore.actions.GetDocumentRequest;
import com.openexchange.ajax.infostore.actions.GetDocumentResponse;
import com.openexchange.ajax.infostore.actions.GetInfostoreRequest;
import com.openexchange.ajax.infostore.actions.GetInfostoreResponse;
import com.openexchange.ajax.infostore.actions.NewInfostoreRequest;
import com.openexchange.ajax.infostore.actions.NewInfostoreResponse;
import com.openexchange.ajax.infostore.actions.UpdateInfostoreRequest;
import com.openexchange.ajax.infostore.actions.UpdateInfostoreResponse;
import com.openexchange.ajax.session.actions.LoginRequest;
import com.openexchange.ajax.session.actions.LoginRequest.GuestCredentials;
import com.openexchange.ajax.session.actions.LoginResponse;
import com.openexchange.ajax.share.actions.RedeemRequest;
import com.openexchange.ajax.share.actions.RedeemResponse;
import com.openexchange.ajax.share.actions.ResolveShareRequest;
import com.openexchange.ajax.share.actions.ResolveShareResponse;
import com.openexchange.ajax.task.actions.AllRequest;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.file.storage.composition.FileID;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Permissions;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.ObjectPermission;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.modules.Module;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.java.Strings;
import com.openexchange.java.util.TimeZones;
import com.openexchange.java.util.UUIDs;
import com.openexchange.share.recipient.ShareRecipient;

/**
 * {@link GuestClient}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class GuestClient extends AJAXClient {

    /**
     * The GuestClient.java.
     */
    private static final String TIMESTAMP_META = "timestamp";
    private final ResolveShareResponse shareResponse;
    private final LoginResponse loginResponse;
    private final String module;
    private final String item;
    private final String folder;
    private static final Logger LOGGER = LoggerFactory.getLogger(GuestClient.class);

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param createdBy By whom the client was created
     * @param url The share URL to access
     * @param recipient The recipient
     */
    public GuestClient(String createdBy, String url, ShareRecipient recipient) throws Exception {
        this(createdBy, url, recipient, false);
    }

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param createdBy By whom the client was created
     * @param url The share URL to access
     * @param recipient The recipient
     * @param failOnNonRedirect <code>true</code> to fail if the share resolve request is not being redirected, <code>false</code>, otherwise
     */
    public GuestClient(String createdBy, String url, ShareRecipient recipient, boolean failOnNonRedirect) throws Exception {
        this(createdBy, url, ShareTest.getUsername(recipient), ShareTest.getPassword(recipient), failOnNonRedirect);
    }

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param ajaxSession The underlying ajax session to use
     * @param url The share URL to access
     * @param recipient The recipient
     * @param failOnNonRedirect <code>true</code> to fail if the share resolve request is not being redirected, <code>false</code>, otherwise
     */
    public GuestClient(AJAXSession ajaxSession, String url, ShareRecipient recipient, boolean failOnNonRedirect) throws Exception {
        this(ajaxSession, url, recipient, failOnNonRedirect, true);
    }

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param ajaxSession The underlying ajax session to use
     * @param url The share URL to access
     * @param recipient The recipient
     * @param failOnNonRedirect <code>true</code> to fail if the share resolve request is not being redirected, <code>false</code>, otherwise
     * @param mustLogout <code>true</code> to enforce logging out on finalize, <code>false</code>, otherwise
     */
    public GuestClient(AJAXSession ajaxSession, String url, ShareRecipient recipient, boolean failOnNonRedirect, boolean mustLogout) throws Exception {
        this(ajaxSession, url, ShareTest.getUsername(recipient), ShareTest.getPassword(recipient), failOnNonRedirect, mustLogout);
    }

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param createdBy By whom the client was created
     * @param url The share URL to access
     * @param username The username to use for authentication, or <code>null</code> if not needed
     * @param password The password to use for authentication, or <code>null</code> if not needed
     */
    public GuestClient(String createdBy, String url, String username, String password) throws Exception {
        this(createdBy, url, username, password, false);
    }

    /**
     * Initializes a new {@link GuestClient}.
     * 
     * @param createdBy By whom the client was created
     * @param url The share URL to access
     * @param username The username to use for authentication, or <code>null</code> if not needed
     * @param password The password to use for authentication, or <code>null</code> if not needed
     * @param failOnNonRedirect <code>true</code> to fail if the share resolve request is not being redirected, <code>false</code>, otherwise
     * @throws Exception
     */
    public GuestClient(String createdBy, String url, String username, String password, boolean failOnNonRedirect) throws Exception {
        this(createdBy, new ClientConfig(url).setCredentials(username, password).setFailOnNonRedirect(failOnNonRedirect));
    }

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param ajaxSession The underlying ajax session to use
     * @param url The share URL to access
     * @param username The username to use for authentication, or <code>null</code> if not needed
     * @param password The password to use for authentication, or <code>null</code> if not needed
     * @param failOnNonRedirect <code>true</code> to fail if the share resolve request is not being redirected, <code>false</code>, otherwise
     * @throws Exception
     */
    public GuestClient(AJAXSession ajaxSession, String url, String username, String password, boolean failOnNonRedirect) throws Exception {
        this(ajaxSession, url, username, password, failOnNonRedirect, true);
    }

    /**
     * Initializes a new {@link GuestClient}.
     *
     * @param ajaxSession The underlying ajax session to use
     * @param url The share URL to access
     * @param username The username to use for authentication, or <code>null</code> if not needed
     * @param password The password to use for authentication, or <code>null</code> if not needed
     * @param failOnNonRedirect <code>true</code> to fail if the share resolve request is not being redirected, <code>false</code>, otherwise
     * @param mustLogout <code>true</code> to enforce logging out on finalize, <code>false</code>, otherwise
     * @throws Exception
     */
    public GuestClient(AJAXSession ajaxSession, String url, String username, String password, boolean failOnNonRedirect, boolean mustLogout) throws Exception {
        this(ajaxSession, url, username, password, null, failOnNonRedirect, mustLogout);
    }

    public GuestClient(AJAXSession ajaxSession, String url, String username, String password, String client, boolean failOnNonRedirect, boolean mustLogout) throws Exception {
        this(ajaxSession.getCreatedBy(), new ClientConfig(url).setAJAXSession(ajaxSession).setCredentials(username, password).setClient(client).setFailOnNonRedirect(failOnNonRedirect).setMustLogout(mustLogout));
    }

    public GuestClient(AJAXSession ajaxSession, String url, String username, String password, String client, boolean staySignedIn, boolean failOnNonRedirect, boolean mustLogout) throws Exception {
        this(ajaxSession.getCreatedBy(), new ClientConfig(url).setAJAXSession(ajaxSession).setCredentials(username, password).setClient(client).setStaySignedIn(staySignedIn).setFailOnNonRedirect(failOnNonRedirect).setMustLogout(mustLogout));
    }

    public GuestClient(String createdBy, ClientConfig config) throws Exception {
        super(getOrCreateSession(createdBy, config), config.mustLogout);
        Assertions.assertNotNull(config.url, "Share-URL not set");
        setHostname(new URI(config.url).getHost());
        setProtocol(new URI(config.url).getScheme());
        /*
         * resolve share
         */
        ResolveShareResponse resolveShareResponse = Executor.execute(this, new ResolveShareRequest(config.url, config.failOnNonRedirect, config.client), getProtocol(), getHostname());
        if (null != resolveShareResponse.getToken()) {
            /*
             * redeem message details via token & continue
             */
            RedeemResponse redeemResponse = execute(new RedeemRequest(resolveShareResponse.getToken(), true));
            assertFalse(redeemResponse.hasError(), redeemResponse.getErrorMessage());
            shareResponse = new ResolveShareResponse(resolveShareResponse, redeemResponse);
        } else {
            shareResponse = resolveShareResponse;
        }
        /*
         * continue share login as indicated by response
         */
        if (null != shareResponse.getLoginType() && false == "message".equals(shareResponse.getLoginType())) {
            loginResponse = login(shareResponse, config);
            getSession().setId(loginResponse.getSessionId());
            if (false == loginResponse.hasError()) {
                JSONObject data = (JSONObject) loginResponse.getData();
                module = data.has("module") ? data.getString("module") : null;
                folder = data.has("folder") ? data.getString("folder") : null;
                item = data.has("item") ? data.getString("item") : null;
            } else {
                module = null;
                folder = null;
                item = null;
            }
        } else {
            loginResponse = null;
            getSession().setId(shareResponse.getSessionID());
            module = shareResponse.getModule();
            folder = shareResponse.getFolder();
            item = shareResponse.getItem();
        }
        if (getSession().getId() == null) {
            LOGGER.error("Guest client has no session id.");
        }
    }

    private static AJAXSession getOrCreateSession(String createdBy, ClientConfig config) {
        if (config.ajaxSession == null) {
            HttpClientBuilder builder = AJAXSession.newHttpClientBuilder(createdBy, b -> b.setRedirectsEnabled(false));
            if (null != config.password) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(null != config.username ? config.username : "guest", config.password);
                credentialsProvider.setCredentials(org.apache.http.auth.AuthScope.ANY, credentials);
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
            return new AJAXSession(createdBy, builder.build());
        }

        return config.ajaxSession;
    }

    public static final class ClientConfig {

        final String url;

        String username;

        String password;

        boolean failOnNonRedirect;

        boolean mustLogout;

        boolean staySignedIn;

        String client;

        AJAXSession ajaxSession;

        public ClientConfig(String url) {
            super();
            this.url = url;
        }

        public ClientConfig setCredentials(ShareRecipient recipient) {
            this.username = ShareTest.getUsername(recipient);
            this.password = ShareTest.getPassword(recipient);
            return this;
        }

        public ClientConfig setCredentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public ClientConfig setUsername(String username) {
            this.username = username;
            return this;
        }

        public ClientConfig setPassword(String password) {
            this.password = password;
            return this;
        }

        public ClientConfig setFailOnNonRedirect(boolean failOnNonRedirect) {
            this.failOnNonRedirect = failOnNonRedirect;
            return this;
        }

        public ClientConfig setMustLogout(boolean mustLogout) {
            this.mustLogout = mustLogout;
            return this;
        }

        public ClientConfig setAJAXSession(AJAXSession ajaxSession) {
            this.ajaxSession = ajaxSession;
            return this;
        }

        public ClientConfig setClient(String client) {
            this.client = client;
            return this;
        }

        public ClientConfig setStaySignedIn(boolean staySignedIn) {
            this.staySignedIn = staySignedIn;
            return this;
        }

    }

    private LoginResponse login(ResolveShareResponse shareResponse, ClientConfig config) throws Exception {
        LoginRequest loginRequest = null;
        if ("guest".equals(shareResponse.getLoginType()) || "guest_password".equals(shareResponse.getLoginType())) {
            String loginName = Strings.isNotEmpty(shareResponse.getLoginName()) ? shareResponse.getLoginName() : config.username;
            GuestCredentials credentials = new GuestCredentials(loginName, config.password);
            loginRequest = LoginRequest.createGuestLoginRequest(shareResponse.getShare(), shareResponse.getTarget(), credentials, config.client, true, false);
        } else if ("anonymous_password".equals(shareResponse.getLoginType())) {
            loginRequest = LoginRequest.createAnonymousLoginRequest(shareResponse.getShare(), shareResponse.getTarget(), config.password, false);
        } else {
            Assertions.fail("unknown login type: " + shareResponse.getLoginType());
        }
        return Executor.execute(this, loginRequest, getProtocol(), getHostname());
    }

    public ResolveShareResponse getShareResolveResponse() {
        return shareResponse;
    }

    public LoginResponse getLoginResponse() {
        return loginResponse;
    }

    public String getModule() {
        return module;
    }

    public int getModuleID() {
        return Module.getModuleInteger(getModule());
    }

    public String getFolder() {
        return folder;
    }

    public int getIntFolder() {
        return Integer.parseInt(getFolder());
    }

    public String getItem() {
        return item;
    }

    /**
     * Checks that a share is accessible for the guest according to the granted permissions.
     *
     * @param permissions The guest permissions
     * @throws Exception
     */
    public void checkShareAccessible(FileStorageGuestObjectPermission permissions) throws Exception {
        checkFileAccessible(getFolder(), getItem(), permissions);
    }

    /**
     * Checks that a share is accessible for the guest according to the granted permissions.
     *
     * @param permissions The guest permissions
     * @param expectedContents The expected contents of the file
     * @throws Exception
     */
    public void checkShareAccessible(FileStorageGuestObjectPermission permissions, byte[] expectedContents) throws Exception {
        checkFileAccessible(getFolder(), getItem(), permissions, expectedContents);
    }

    /**
     * Checks that a share is accessible for the guest according to the granted permissions.
     *
     * @param permissions The guest permissions
     * @throws Exception
     */
    public void checkShareAccessible(OCLGuestPermission permissions) throws Exception {
        checkFolderAccessible(getFolder(), permissions);
    }

    /**
     * Checks that a folder is accessible for the guest according to the granted permissions.
     *
     * @param folderID The identifier of the folder to check
     * @param permissions The guest permissions in that folder
     * @throws Exception
     */
    public void checkFolderAccessible(String folderID, OCLGuestPermission permissions) throws Exception {
        /*
         * get folder
         */
        GetResponse getResponse = execute(new GetRequest(EnumAPI.OX_NEW, folderID));
        FolderObject folder = getResponse.getFolder();
        folder.setLastModified(getResponse.getTimestamp());
        /*
         * check item creation
         */
        String id = createItem(folder, false == permissions.canCreateObjects());
        if (null != id) {
            /*
             * check item retrieval
             */
            getItem(folder, id, false == permissions.canReadOwnObjects());
            /*
             * check item deletion
             */
            deleteItem(folder, id, false == permissions.canDeleteAllObjects());
        }
        /*
         * check item listing
         */
        getAll(getResponse.getStorageFolder(), false == permissions.canReadOwnObjects());
    }

    /**
     * Checks that a file is accessible for the guest according to the granted permissions.
     *
     * @param folderID The folder identifier of the file to check
     * @param fileID The identifier of the file to check
     * @param permissions The guest permissions for that file
     * @throws Exception
     */
    public void checkFileAccessible(String folderID, String fileID, FileStorageGuestObjectPermission permissions) throws Exception {
        checkFileAccessible(folderID, fileID, permissions, null);
    }

    /**
     * Checks that a file is accessible for the guest according to the granted permissions.
     *
     * @param folderID The folder identifier of the file to check
     * @param fileID The identifier of the file to check
     * @param permissions The guest permissions for that file
     * @param expectedContents The expected contents of the file
     * @throws Exception
     */
    public void checkFileAccessible(String folderID, String fileID, FileStorageGuestObjectPermission permissions, byte[] expectedContents) throws Exception {
        /*
         * check item retrieval
         */
        GetInfostoreRequest getInfostoreRequest = new GetInfostoreRequest(fileID);
        getInfostoreRequest.setFailOnError(permissions.canRead());
        GetInfostoreResponse getInfostoreResponse = execute(getInfostoreRequest);
        checkResponse(getInfostoreResponse, false == permissions.canRead());
        DefaultFile file = new DefaultFile(getInfostoreResponse.getDocumentMetadata());
        file.setMeta(Collections.singletonMap(TIMESTAMP_META, getInfostoreResponse.getTimestamp()));
        if (null != file.getFileName() && 0 < file.getFileSize()) {
            GetDocumentRequest getDocumentRequest = new GetDocumentRequest(folderID, fileID);
            getInfostoreRequest.setFailOnError(permissions.canRead());
            try (GetDocumentResponse getDocumentResponse = execute(getDocumentRequest)) {
                checkResponse(getDocumentResponse, false == permissions.canRead());
                byte[] contents = getDocumentResponse.getContentAsByteArray();
                if (false == permissions.canRead()) {
                    Assertions.assertNull(contents, "Contents wrong");
                } else {
                    if (null == expectedContents) {
                        Assertions.assertNotNull(contents, "Contents wrong");
                    } else {
                        Assertions.assertArrayEquals(expectedContents, contents, "Contents wrong");
                    }
                }
            }
        }

        if (permissions.canRead()) {
            /*
             * check item update
             */
            file.setFileName(file.getFileName() + "_edit");
            Date timestamp = file.getMeta() == null ? file.getLastModified() : (Date) file.getMeta().getOrDefault(TIMESTAMP_META, file.getLastModified());
            UpdateInfostoreRequest updateInfostoreRequest = new UpdateInfostoreRequest(file, new Field[] { Field.FILENAME }, timestamp);
            updateInfostoreRequest.setFailOnError(permissions.canWrite());
            UpdateInfostoreResponse updateInfostoreResponse = execute(updateInfostoreRequest);
            checkResponse(updateInfostoreResponse, false == permissions.canWrite());
            file.setLastModified(updateInfostoreResponse.getTimestamp());
        }
    }

    /**
     * Checks that a file is not accessible for the guest.
     *
     * @param fileID The identifier of the file to check
     */
    public void checkFileNotAccessible(String fileID) throws Exception {
        /*
         * check item retrieval
         */
        GetInfostoreRequest getInfostoreRequest = new GetInfostoreRequest(fileID);
        getInfostoreRequest.setFailOnError(false);
        GetInfostoreResponse getInfostoreResponse = execute(getInfostoreRequest);
        checkResponse(getInfostoreResponse, true);
    }

    /**
     * Checks that a file is accessible for the guest according to the granted permissions.
     * This method checks only for object permissions. If you shared the parent folder you need
     * to check the files accessibility otherwise.
     *
     * @param id The identifier of the file to check
     * @param permissions The guest permissions for that file
     * @throws Exception
     */
    public void checkFileAccessible(String id, OCLGuestPermission permissions) throws Exception {
        /*
         * get file
         */
        FileID fileID = new FileID(id);
        fileID.setFolderId(Integer.toString(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID));
        GetInfostoreRequest getFileRequest = new GetInfostoreRequest(fileID.toUniqueID());
        getFileRequest.setFailOnError(true);
        GetInfostoreResponse getFileResponse = execute(getFileRequest);
        File file = getFileResponse.getDocumentMetadata();
        List<FileStorageObjectPermission> objectPermissions = file.getObjectPermissions();
        if (objectPermissions == null) {
            Assertions.fail("File contains no object permission for entity " + permissions.getEntity());
        }

        FileStorageObjectPermission permissionForEntity = null;
        assertNotNull(objectPermissions);
        for (FileStorageObjectPermission p : objectPermissions) {
            if (p.getEntity() == permissions.getEntity() && p.isGroup() == permissions.isGroupPermission()) {
                permissionForEntity = p;
                break;
            }
        }

        int expected = getObjectPermissionBits(permissions.getPermissionBits());
        assertNotNull(permissionForEntity, "File contains no object permission for entity " + permissions.getEntity());
        Assertions.assertEquals(expected, permissionForEntity.getPermissions(), "Wrong permission found");
    }

    /**
     * Takes a folder permission bit mask and deduces the according object permissions.
     *
     * @param folderPermissionBits The folder permission bit mask
     * @return The object permission bits
     */
    protected static int getObjectPermissionBits(int folderPermissionBits) {
        int objectBits = ObjectPermission.NONE;
        int[] permissionBits = Permissions.parsePermissionBits(folderPermissionBits);
        int rp = permissionBits[1];
        int wp = permissionBits[2];
        int dp = permissionBits[3];
        if (dp >= Permission.DELETE_ALL_OBJECTS) {
            objectBits = ObjectPermission.DELETE;
        } else if (wp >= Permission.WRITE_ALL_OBJECTS) {
            objectBits = ObjectPermission.WRITE;
        } else if (rp >= Permission.READ_ALL_OBJECTS) {
            objectBits = ObjectPermission.READ;
        }

        return objectBits;
    }

    /**
     * Checks that a folder is not accessible for the guest.
     *
     * @param folderID The identifier of the folder to check
     * @throws Exception
     */
    public void checkFolderNotAccessible(String folderID) throws Exception {
        GetResponse getResponse = execute(new GetRequest(EnumAPI.OX_NEW, Integer.valueOf(folderID).intValue(), false));
        Assertions.assertTrue(getResponse.hasError(), "No errors in response");
        Assertions.assertNull(getResponse.getFolder(), "Folder in response");
    }

    /**
     * Checks that a module is available.
     *
     * @param moduleID The identifier of the module to be available
     */
    public void checkModuleAvailable(int moduleID) throws Exception {
        com.openexchange.ajax.config.actions.GetResponse getResponse = execute(new com.openexchange.ajax.config.actions.GetRequest(Tree.AvailableModules));
        String module = getContentType(moduleID);
        Object[] array = getResponse.getArray();
        for (Object object : array) {
            if (module.equals(object)) {
                return;
            }
        }
        Assertions.fail("Module " + getContentType(moduleID) + " not found");
    }

    /**
     * Checks that a module is not available.
     *
     * @param moduleID The identifier of the module to be not available
     */
    public void checkModuleNotAvailable(int moduleID) throws Exception {
        com.openexchange.ajax.config.actions.GetResponse getResponse = execute(new com.openexchange.ajax.config.actions.GetRequest(Tree.AvailableModules));
        String module = getContentType(moduleID);
        Object[] array = getResponse.getArray();
        for (Object object : array) {
            Assertions.assertNotEquals("Module " + getContentType(moduleID) + " found", object, module);
        }
    }

    /**
     * Checks that the share's module is available.
     */
    public void checkShareModuleAvailable() throws Exception {
        checkModuleAvailable(getModuleID());
    }

    /**
     * Checks that the share's module is available, as well as all others modules are not.
     */
    public void checkShareModuleAvailableExclusively() throws Exception {
        com.openexchange.ajax.config.actions.GetResponse getResponse = execute(new com.openexchange.ajax.config.actions.GetRequest(Tree.AvailableModules));
        Object[] array = getResponse.getArray();
        for (int moduleID : new int[] { FolderObject.CALENDAR, FolderObject.CONTACT, FolderObject.INFOSTORE, FolderObject.TASK }) {
            String module = getContentType(moduleID);
            boolean found = false;
            for (Object object : array) {
                if (module.equals(object)) {
                    found = true;
                    break;
                }
            }
            if (getModuleID() == moduleID) {
                Assertions.assertTrue(found, "Module " + module + " not found");
            } else {
                Assertions.assertFalse(found, "Module " + module + " was found");
            }
        }
    }

    private String getContentType(int module) {
        switch (module) {
            case FolderObject.CONTACT:
                return "contacts";
            case FolderObject.INFOSTORE:
                return "infostore";
            case FolderObject.TASK:
                return "tasks";
            case FolderObject.CALENDAR:
                return "calendar";
            default:
                Assertions.fail("no content type for " + getModule() + "");
                return null;
        }
    }

    /**
     * Checks that the guest client's session is "alive" by executing a "get user" request, followed by a "visible folders" request in
     * case the module is different from infostore.
     *
     * @param expectToFail <code>true</code> if the requests are expected to fail, <code>false</code>, otherwise
     */
    public void checkSessionAlive(boolean expectToFail) throws Exception {
        com.openexchange.ajax.user.actions.GetResponse getResponse = execute(new com.openexchange.ajax.user.actions.GetRequest(TimeZones.UTC, false == expectToFail));
        checkResponse(getResponse, expectToFail);
        if (FolderObject.INFOSTORE != getModuleID()) {
            String contentType = getContentType(getModuleID());
            VisibleFoldersResponse response = execute(new VisibleFoldersRequest(EnumAPI.OX_NEW, contentType, FolderObject.ALL_COLUMNS, false == expectToFail));
            checkResponse(response, expectToFail);
        }
    }

    private static void checkResponse(AbstractAJAXResponse response, boolean expectToFail) {
        Assertions.assertNotNull(response, "No response");
        if (expectToFail) {
            if (false == response.hasError()) {
                System.out.println("+++");
            }

            Assertions.assertTrue(response.hasError(), "No errors in response");
        } else {
            Assertions.assertFalse(response.hasError(), "Errors in response");
        }
    }

    private void deleteItem(FolderObject folder, String id, boolean expectToFail) throws Exception {
        int folderID = folder.getObjectID();
        Date timestamp = getFutureTimestamp();
        boolean failOnError = false == expectToFail;
        switch (folder.getModule()) {
            case FolderObject.CONTACT:
                CommonDeleteResponse deleteContactResponse = execute(new com.openexchange.ajax.contact.action.DeleteRequest(folderID, Integer.parseInt(id), timestamp, failOnError));
                checkResponse(deleteContactResponse, expectToFail);
                break;
            case FolderObject.INFOSTORE:
                DeleteInfostoreRequest deleteInfostoreRequest = new DeleteInfostoreRequest(id, String.valueOf(folderID), timestamp);
                deleteInfostoreRequest.setFailOnError(failOnError);
                DeleteInfostoreResponse deleteInfostoreResponse = execute(deleteInfostoreRequest);
                checkResponse(deleteInfostoreResponse, expectToFail);
                break;
            case FolderObject.TASK:
                CommonDeleteResponse deleteTaskResponse = execute(new com.openexchange.ajax.task.actions.DeleteRequest(folderID, Integer.parseInt(id), timestamp, failOnError));
                checkResponse(deleteTaskResponse, expectToFail);
                break;
            case FolderObject.CALENDAR:
                CommonDeleteResponse deleteAppointmentResponse = execute(new com.openexchange.ajax.appointment.action.DeleteRequest(Integer.parseInt(id), folderID, timestamp, failOnError));
                checkResponse(deleteAppointmentResponse, expectToFail);
                break;
            default:
                Assertions.fail("no delete item request for " + folder.getModule() + " implemented");
                break;
        }
    }

    public Object getItem(FolderObject folder, String id, boolean expectToFail) throws Exception {
        int folderID = folder.getObjectID();
        boolean failOnError = false == expectToFail;
        TimeZone timeZone = TimeZones.UTC;
        switch (folder.getModule()) {
            case FolderObject.CONTACT:
                com.openexchange.ajax.contact.action.GetResponse contactGetResponse = execute(new com.openexchange.ajax.contact.action.GetRequest(folderID, Integer.parseInt(id), timeZone, failOnError));
                checkResponse(contactGetResponse, expectToFail);
                return expectToFail ? null : contactGetResponse.getContact();
            case FolderObject.INFOSTORE:
                GetInfostoreRequest getInfostoreRequest = new GetInfostoreRequest(id);
                getInfostoreRequest.setFailOnError(false == expectToFail);
                GetInfostoreResponse getInfostoreResponse = execute(getInfostoreRequest);
                checkResponse(getInfostoreResponse, expectToFail);
                return expectToFail ? null : getInfostoreResponse.getDocumentMetadata();
            case FolderObject.TASK:
                com.openexchange.ajax.task.actions.GetResponse getTaskResponse = execute(new com.openexchange.ajax.task.actions.GetRequest(folderID, Integer.parseInt(id), failOnError));
                checkResponse(getTaskResponse, expectToFail);
                return expectToFail ? null : getTaskResponse.getTask(timeZone);
            case FolderObject.CALENDAR:
                com.openexchange.ajax.appointment.action.GetResponse getAppointmentResponse = execute(new com.openexchange.ajax.appointment.action.GetRequest(folderID, Integer.parseInt(id), failOnError));
                checkResponse(getAppointmentResponse, expectToFail);
                return expectToFail ? null : getAppointmentResponse.getAppointment(timeZone);
            default:
                Assertions.fail("no get item request for " + folder.getModule() + " implemented");
                return null;
        }
    }

    private String createItem(FolderObject folder, boolean expectToFail) throws Exception {
        boolean failOnError = false == expectToFail;
        int folderID = folder.getObjectID();
        TimeZone timeZone = TimeZones.UTC;
        switch (folder.getModule()) {
            case FolderObject.CONTACT:
                Contact contact = new Contact();
                contact.setParentFolderID(folderID);
                contact.setDisplayName(UUIDs.getUnformattedString(UUID.randomUUID()));
                InsertResponse insertContactResponse = execute(new com.openexchange.ajax.contact.action.InsertRequest(contact, failOnError));
                checkResponse(insertContactResponse, expectToFail);
                return expectToFail ? null : String.valueOf(insertContactResponse.getId());
            case FolderObject.INFOSTORE:
                byte[] data = UUIDs.toByteArray(UUID.randomUUID());
                File metadata = new DefaultFile();
                metadata.setFolderId(String.valueOf(folderID));
                metadata.setFileName(UUIDs.getUnformattedString(UUID.randomUUID()) + ".test");
                NewInfostoreRequest newRequest = new NewInfostoreRequest(metadata, new ByteArrayInputStream(data));
                newRequest.setFailOnError(false == expectToFail);
                NewInfostoreResponse newResponse = execute(newRequest);
                checkResponse(newResponse, expectToFail);
                return expectToFail ? null : String.valueOf(newResponse.getID());
            case FolderObject.TASK:
                Task task = new Task();
                task.setParentFolderID(folderID);
                task.setTitle(UUIDs.getUnformattedString(UUID.randomUUID()));
                com.openexchange.ajax.task.actions.InsertResponse insertTaskResponse = execute(new com.openexchange.ajax.task.actions.InsertRequest(task, timeZone, failOnError));
                checkResponse(insertTaskResponse, expectToFail);
                return expectToFail ? null : String.valueOf(insertTaskResponse.getId());
            case FolderObject.CALENDAR:
                Appointment appointment = new Appointment();
                appointment.setParentFolderID(folderID);
                appointment.setTitle(UUIDs.getUnformattedString(UUID.randomUUID()));
                appointment.setStartDate(new Date());
                appointment.setEndDate(new Date(appointment.getStartDate().getTime() + 60 * 1000 * 60));
                appointment.setIgnoreConflicts(true);
                AppointmentInsertResponse insertAppointmentResponse = execute(new com.openexchange.ajax.appointment.action.InsertRequest(appointment, timeZone, failOnError));
                checkResponse(insertAppointmentResponse, expectToFail);
                return expectToFail ? null : String.valueOf(insertAppointmentResponse.getId());
            default:
                Assertions.fail("no create item request for " + folder.getModule() + " implemented");
                return null;
        }
    }

    private AbstractColumnsResponse getAll(Folder storageFolder, boolean expectToFail) throws Exception {
        int folderId = getLegacyFolderId(storageFolder);
        switch (storageFolder.getContentType().getModule()) {
            case FolderObject.CONTACT:
                // TODO: use addressbooks api to check
                CommonAllResponse allContactResponse = execute(new com.openexchange.ajax.contact.action.AllRequest(folderId, Contact.ALL_COLUMNS));
                checkResponse(allContactResponse, expectToFail);
                return allContactResponse;
            case FolderObject.INFOSTORE:
                int[] columns = new int[] { Metadata.ID, Metadata.TITLE, Metadata.DESCRIPTION, Metadata.URL, Metadata.FOLDER_ID };
                AbstractColumnsResponse allInfostoreResponse = execute(new AllInfostoreRequest(folderId, columns, Metadata.ID, Order.ASCENDING));
                checkResponse(allInfostoreResponse, expectToFail);
                return allInfostoreResponse;
            case FolderObject.TASK:
                CommonAllResponse allTaskResponse = execute(new AllRequest(folderId, Task.ALL_COLUMNS, Task.OBJECT_ID, Order.ASCENDING));
                checkResponse(allTaskResponse, expectToFail);
                return allTaskResponse;
            case FolderObject.CALENDAR:
                // TODO: use chronos api to check
                Date start = new Date(System.currentTimeMillis() - 100000000);
                Date end = new Date(System.currentTimeMillis() + 100000000);
                CommonAllResponse allCalendarResponse = execute(new com.openexchange.ajax.appointment.action.AllRequest(folderId, CalendarDataObject.ALL_COLUMNS, start, end, TimeZones.UTC));
                checkResponse(allCalendarResponse, expectToFail);
                return allCalendarResponse;
            default:
                Assertions.fail("no all request for " + storageFolder.getContentType() + " implemented");
                return null;
        }
    }

    private CloseableHttpClient getHttpClient() {
        return getSession().getHttpClient();
    }

    private static Date getFutureTimestamp() {
        return new Date(System.currentTimeMillis() + 1000000);
    }

    public void assertNoLoginError() {
        assertFalse(loginResponse.hasError(), "Unexpected error: " + loginResponse.getErrorMessage());
    }

    private static int getLegacyFolderId(Folder folder) throws Exception {
        String id = folder.getID();
        switch (folder.getContentType().getModule()) {
            case FolderObject.CONTACT:
                try {
                    return Integer.parseInt(id);
                } catch (NumberFormatException e) {
                    return Integer.parseInt(com.openexchange.contact.provider.composition.IDMangling.getRelativeFolderId(id));
                }
            case FolderObject.CALENDAR:
                try {
                    return Integer.parseInt(id);
                } catch (NumberFormatException e) {
                    return Integer.parseInt(com.openexchange.chronos.provider.composition.IDMangling.getRelativeFolderId(id));
                }
            default:
                return Integer.parseInt(folder.getID());
        }
    }

}
