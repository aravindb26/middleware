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

package com.openexchange.ajax.chronos.manager;

import static com.openexchange.ajax.framework.ClientCommons.checkResponse;
import static com.openexchange.java.Autoboxing.i;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.CalendarFolderFactory;
import com.openexchange.ajax.folder.manager.FolderPermissionsBits;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.FoldersApi;

/**
 * {@link CalendarFolderManager}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CalendarFolderManager {

    /** {@value #DEFAULT_ACCOUNT_ID} */
    public static final String DEFAULT_ACCOUNT_ID = "0";
    /** {@value #DEFAULT_FOLDER_ID} */
    public static final String DEFAULT_FOLDER_ID = "1";
    /** {@value #PUBLIC_FOLDER_ID} */
    public static final String PUBLIC_FOLDER_ID = "2";
    /** {@value #TREE_ID} */
    public static final String TREE_ID = "0";
    /** {@value #MODULE} */
    public static final String MODULE = "calendar";
    /** {@value #DEFAULT_ACCOUNT_PROVIDER_ID} */
    public static final String DEFAULT_ACCOUNT_PROVIDER_ID = "chronos";
    /** {@value #ICAL_ACCOUNT_PROVIDER_ID} */
    public static final String ICAL_ACCOUNT_PROVIDER_ID = "ical";
    /** {@value #CALENDAR_MODULE} */
    public static final String CALENDAR_MODULE = "calendar";
    /** {@value #EVENT_MODULE} */
    public static final String EVENT_MODULE = "event";

    private final List<String> folderIds;

    private final FoldersApi foldersApi;
    private final UserApi userApi;

    /**
     * Initialises a new {@link CalendarFolderManager}.
     */
    public CalendarFolderManager(UserApi userApi, FoldersApi foldersApi) {
        super();
        folderIds = new ArrayList<>(4);
        this.userApi = userApi;
        this.foldersApi = foldersApi;
    }

    /**
     * Clean up. Deletes all folders created via this manager.
     */
    public void cleanUp() {
        try {
            foldersApi.deleteFolders(folderIds, TREE_ID, null, CALENDAR_MODULE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, null, Boolean.FALSE);
        } catch (ApiException e) {
            System.err.println("Could not clean up the calendar folders for user " + userApi.getCalUser() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *
     * @param module
     * @param providerId
     * @param title
     * @param config
     * @param extendedProperties
     * @return
     * @throws ApiException
     * @throws ChronosApiException
     */
    public String createFolder(String module, String providerId, String title, JSONObject config, JSONObject extendedProperties) throws ApiException, ChronosApiException {
        return createFolder(module, providerId, title, config, extendedProperties, false);
    }

    /**
     * Creates a folder for the specified module and provider
     *
     * @param module The module
     * @param providerId The provider identifier
     * @param config The configuration
     * @param extendedProperties The extended properties of the folder
     * @return The folder identifier
     * @throws ApiException If an API error is occurred
     * @throws ChronosApiException
     */
    public String createFolder(String module, String providerId, String title, JSONObject config, JSONObject extendedProperties, boolean expectedException) throws ApiException, ChronosApiException {
        NewFolderBody body = CalendarFolderFactory.createFolderBody(module, providerId, title, Boolean.TRUE, config, extendedProperties);
        FolderUpdateResponse response = foldersApi.createFolder(DEFAULT_FOLDER_ID, body, TREE_ID, module, null, null);
        if (expectedException) {
            assertNotNull(response.getError(), "An error was expected");
            throw new ChronosApiException(response.getCode(), response.getError());
        }
        return handleCreation(response).getData();
    }

    /**
     * Creates a folder based on the given data
     *
     * @param body The calendar folder
     * @return The folder identifier
     * @throws ApiException In case of error
     */
    public String createFolder(NewFolderBody body) throws ApiException {
        return createFolder(body, DEFAULT_FOLDER_ID);
    }

    /**
     * Creates a folder based on the given data
     *
     * @param body The calendar folder
     * @param parentFolderId The parent folder ID to create the new folder in
     * @return The folder identifier
     * @throws ApiException In case of error
     */
    public String createFolder(NewFolderBody body, String parentFolderId) throws ApiException {
        FolderUpdateResponse response = foldersApi.createFolder(parentFolderId, body, CalendarFolderManager.TREE_ID, CalendarFolderManager.MODULE, null, null);
        return handleCreation(response).getData();
    }

    /**
     * Creates a new calendar folder
     *
     * @param folderName The folder name
     * @param owner The owner of the calendar folder
     * @return The folder identifier
     * @throws ApiException In case of error
     */
    public String createCalendarFolder(String folderName, int owner) throws ApiException {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        FolderPermission permission = FolderPermissionsBits.OWNER.getPermission(owner);
        folder.setPermissions(Collections.singletonList(permission));
        folder.setModule("event");
        folder.setTitle(folderName);
        folder.setSubscribed(Boolean.TRUE);
        body.setFolder(folder);
        return createFolder(body);
    }

    /**
     * Creates a new public calendar folder
     *
     * @param folderName The folder name
     * @return The folder identifier
     * @throws ApiException In case of error
     */
    public String createPublicCalendarFolder(String folderName) throws ApiException {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule("event");
        folder.setTitle(folderName);
        folder.setSubscribed(Boolean.TRUE);
        body.setFolder(folder);
        return createFolder(body, PUBLIC_FOLDER_ID);
    }

    /**
     * Retrieves the folder with the specified identifier
     *
     * @param folderId The folder identifier
     * @return the {@link FolderData}
     * @throws ApiException if an API error is occurred
     * @throws ChronosApiException
     */
    public FolderData getFolder(String folderId) throws ApiException, ChronosApiException {
        return getFolder(folderId, false);
    }

    /**
     * Retrieves the folder with the specified identifier
     *
     * @param folderId The folder identifier
     * @return the {@link FolderData}
     * @throws ApiException if an API error is occurred
     * @throws ChronosApiException
     */
    public FolderData getFolder(String folderId, boolean expectedException) throws ApiException, ChronosApiException {
        FolderResponse response = foldersApi.getFolder(folderId, TREE_ID, CALENDAR_MODULE, null, null);
        if (expectedException) {
            assertNotNull(response.getError(), "An error was expected");
            throw new ChronosApiException(response.getCode(), response.getError());
        }
        return checkResponse(response.getError(), response.getErrorDesc(), response.getCategories(), response).getData();
    }

    /**
     * Updates the folder with the specified identifier
     *
     * @param defaultFolderId The folder identifier
     * @return The {@link FolderUpdateResponse}
     * @throws ApiException if an API error is occurred
     * @throws ChronosApiException
     */
    public FolderUpdateResponse updateFolder(FolderData folderData) throws ApiException, ChronosApiException {
        return updateFolder(folderData, false);
    }

    /**
     * Updates the folder with the specified identifier
     *
     * @param defaultFolderId The folder identifier
     * @return The {@link FolderUpdateResponse}
     * @throws ApiException if an API error is occurred
     * @throws ChronosApiException
     */
    public FolderUpdateResponse updateFolder(FolderData folderData, boolean expectedException) throws ApiException, ChronosApiException {
        FolderBody body = new FolderBody();
        body.setFolder(folderData);
        FolderUpdateResponse response = foldersApi.updateFolder(folderData.getId(), body, Boolean.FALSE, folderData.getLastModifiedUtc(), TREE_ID, CALENDAR_MODULE, Boolean.TRUE, null, null, null);
        if (expectedException) {
            assertNotNull(response.getError(), "An error was expected");
            throw new ChronosApiException(response.getCode(), response.getError());
        }
        return checkResponse(response.getError(), response.getErrorDesc(), response.getCategories(), response);
    }

    /**
     * Deletes the folder with the specified identifier
     *
     * @param folderId The folder identifier
     * @throws ApiException if an API error is occurred
     */
    public void deleteFolder(String folderId) throws ApiException {
        this.deleteFolders(Collections.singletonList(folderId));
    }

    /**
     * Deletes the folders with the specified identifier
     *
     * @param defaultFolderId The folder identifiers
     * @throws ApiException if an API error is occurred
     */
    public void deleteFolders(List<String> folders) throws ApiException {
        foldersApi.deleteFolders(folders, TREE_ID, null, CALENDAR_MODULE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, null, Boolean.FALSE);
        for (String folder : folders) {
            folderIds.remove(folder);
        }
    }

    /**
     * Handles the creation response and remembers the folder id
     *
     * @param response The {@link FolderUpdateResponse}
     * @return The {@link FolderUpdateResponse}
     * @throws ApiException if an API error is occurred
     */
    private FolderUpdateResponse handleCreation(FolderUpdateResponse response) {
        FolderUpdateResponse data = checkResponse(response.getError(), response.getErrorDesc(), response.getCategories(), response);
        folderIds.add(data.getData());
        return data;
    }

    /**
     * Shares an existing folder to another user with author permissions
     *
     * @param data The existing folder to share
     * @param testUser The user to share the folder to
     * @return The updated folder
     * @throws ApiException In case of error
     */
    public FolderData shareFolder(FolderData data, TestUser testUser) throws ApiException {
        return shareFolder(data, testUser, FolderPermissionsBits.AUTHOR);
    }

    /**
     * Shares an existing folder to another user
     *
     * @param data The existing folder to share
     * @param testUser The user to share the folder to
     * @param bits The permissions for the user
     * @return The updated folder
     * @throws ApiException In case of error
     */
    public FolderData shareFolder(FolderData data, TestUser testUser, FolderPermissionsBits bits) throws ApiException {
        /*
         * Build delta folder with updated permissions
         */
        List<FolderPermission> permissions;
        permissions = new ArrayList<>(data.getPermissions());
        FolderPermission permission = bits.getPermission(testUser.getUserId());
        permissions.add(permission);
        FolderData folderData = new FolderData();
        folderData.setId(data.getId());
        folderData.setPermissions(permissions);
        folderData.setAccountId(data.getAccountId());
        folderData.setFolderId(data.getFolderId());
        /*
         * Update folder with new permissions
         */
        FolderBody body = new FolderBody();
        body.setFolder(folderData);
        // @formatter:off
        FolderUpdateResponse response = foldersApi.updateFolderBuilder()
            .withId(folderData.getId())
            .withFolderBody(body)
            .withTimestamp(folderData.getLastModifiedUtc())
            .withTree(TREE_ID)
            .withCascadePermissions(TRUE)
            .execute();
        // @formatter:on
        checkResponse(response.getError(), response.getErrorDesc(), response.getCategories(), response);
        /*
         * Get folder and check permissions
         */
        FolderResponse folder = foldersApi.getFolderBuilder().withId(folderData.getId()).withTree(TREE_ID).execute();
        FolderData updatedData = checkResponse(folder.getError(), folder.getErrorDesc(), folder.getData());
        List<FolderPermission> updatedPermissions = updatedData.getPermissions();
        Optional<FolderPermission> newPermission = updatedPermissions.stream().filter(p -> null != p.getEntity() && testUser.getUserId() == i(p.getEntity())).findAny();
        assertTrue(newPermission.isPresent(), "New permissions doesn't exist: " + updatedPermissions.toString());
        assertThat("Wrong permissions bits", newPermission.get().getBits(), is(permission.getBits()));

        return updatedData;
    }
}
