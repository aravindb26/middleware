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

package com.openexchange.ajax.infostore;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.folder.manager.FolderFactory;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemBody;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.InfoItemPermission;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemsResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;

/**
 * {@link MWB1062} - "Timestamps of /infostore?action=all and /infostore?action=updates are incompatible"
 * <br>
 * <br>
 * Ensures that the time stamp returned by infostore?action=all is also computed by considering deleted files
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class MWB1062 extends AbstractAPIClientSession {

    private InfostoreApi infostoreApi, infostoreApi2;
    private FoldersApi foldersApi, foldersApi2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        foldersApi = new FoldersApi(getApiClient());
        foldersApi2 = new FoldersApi(testUser2.getApiClient());
        infostoreApi = new InfostoreApi(getApiClient());
        infostoreApi2 = new InfostoreApi(testUser2.getApiClient());
    }

    private String getPrivateInfostoreFolder(ApiClient client) throws ApiException {
        ConfigApi configApi = new ConfigApi(client);
        ConfigResponse configNode = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath());
        Object data = checkResponse(configNode);
        assertThat(data, is(notNullValue()));
        assertThat(data.toString().toLowerCase(), is(not("null")));
        return String.valueOf(data);
    }

    private Object checkResponse(ConfigResponse resp) {
        assertThat(resp.getErrorDesc(), resp.getError(), is(nullValue()));
        assertThat(resp.getData(), is(not(nullValue())));
        return resp.getData();
    }

    private String createName() {
        return "MWB1062_" + DateTime.now().getMillis();
    }

    private InfoItemListElement uploadFile(InfostoreApi api, String folderId, String fileName, byte[] content) throws ApiException {
        //@formatter:off
        InfoItemUpdateResponse uploadResponse = api.uploadInfoItemBuilder()
                                                    .withFolderId(folderId)
                                                    .withFilename(fileName)
                                                    .withBody(content)
                                                    .execute();
        //@formatter:on
        assertThat(uploadResponse, is(notNullValue()));
        checkResponse(uploadResponse.getError(), uploadResponse.getErrorDesc());

        InfoItemListElement fileElement = new InfoItemListElement();
        fileElement.setFolder(folderId);
        fileElement.setId(uploadResponse.getData());
        return fileElement;
    }

    /**
     * Tests that deleted files are also considered when computing the sequence number during an "all"-request for a regular folder.
     *
     * @throws Exception
     */
    @Test
    public void testMWB1062_RegularFolder() throws Exception {

        //Create a folder with two new files
        NewFolderBody newFolderBody = new NewFolderBody();
        newFolderBody.setFolder(FolderFactory.getSimpleFolder(createName(), FolderManager.INFOSTORE));
        //@formatter:off
        FolderUpdateResponse createResponse = foldersApi.createFolderBuilder()
                                                    .withFolderId(getPrivateInfostoreFolder(getApiClient()))
                                                    .withTree("1")
                                                    .withNewFolderBody(newFolderBody)
                                              .execute();
        //@formatter:on
        String folderId = checkResponse(createResponse.getError(), createResponse.getErrorDesc(), createResponse.getData());
        InfoItemListElement file1 = uploadFile(infostoreApi, folderId, createName(), "Some file content".getBytes(StandardCharsets.UTF_8));
        InfoItemListElement file2 = uploadFile(infostoreApi, folderId, createName(), "more content".getBytes(StandardCharsets.UTF_8));
        List<InfoItemListElement> files = Arrays.asList(file1, file2);
        InfoItemsResponse allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder(folderId).withColumns("1").execute();
        Long timestamp = allResponse.getTimestamp();

        //Delete them
        infostoreApi.deleteInfoItemsBuilder().withInfoItemListElement(files).withTimestamp(timestamp).execute();

        //Check the time stamp of the all-request after deleting some files
        allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder(folderId).withColumns("1").execute();
        timestamp = allResponse.getTimestamp();

        //The time stamp must not be "0" anymore because we do now consider deleted files as well
        assertThat(timestamp, is(greaterThan(Long.valueOf(0))));
    }

    /**
     * Tests that deleted files are also considered when computing the sequence number during an "all"-request for the "shared files" folder 10
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMWB1062_Folder10() throws Exception {

        //As user 2: Create a file
        final String fileName = createName();
        String folderIdUser2 = getPrivateInfostoreFolder(testUser2.getApiClient());
        InfoItemListElement file1 = uploadFile(infostoreApi2, folderIdUser2, fileName, "Some file content".getBytes(StandardCharsets.UTF_8));
        List<InfoItemListElement> files = Arrays.asList(file1);
        InfoItemsResponse allResponse = infostoreApi2.getAllInfoItemsBuilder().withFolder(folderIdUser2).withColumns("1").execute();
        Long timestamp = allResponse.getTimestamp();
        //...and share it with user 1
        InfoItemPermission permission = new InfoItemPermission();
        permission.setEntity(I(testUser.getUserId()));
        permission.setGroup(Boolean.FALSE);
        permission.setBits(InfoItemPermission.BitsEnum.NUMBER_1 /* read only */);
        InfoItemData file1Data = new InfoItemData();
        file1Data.setObjectPermissions(Collections.singletonList(permission));
        InfoItemBody body = new InfoItemBody();
        body._file(file1Data);
        //@formatter:off
        InfoItemUpdateResponse updateResponse = infostoreApi2.updateInfoItemBuilder()
                                                   .withId(file1.getId())
                                                   .withInfoItemBody(body)
                                                   .withTimestamp(timestamp)
                                                .execute();
        //@formatter:on
        timestamp = updateResponse.getTimestamp();

        //As user 1: check if the file got shared
        allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder("10").withColumns("1,700").execute();
        List<List<String>> data = (List<List<String>>) checkResponse(allResponse.getError(), allResponse.getErrorDesc(), allResponse.getData());
        assertThat(L(data.stream().filter(l -> l.contains(fileName)).count()), is(Long.valueOf(1)));
        assertThat(allResponse.getTimestamp(), is(greaterThan(Long.valueOf(0))));

        //As user 2: remove the file
        infostoreApi2.deleteInfoItemsBuilder().withInfoItemListElement(files).withTimestamp(timestamp).execute();

        //As user 1:
        //The file should be gone. Also Check the time stamp of the all-request after deletion. It must not be "0".
        allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder("10").withColumns("1,700").execute();
        List<List<String>> data2 = (List<List<String>>) checkResponse(allResponse.getError(), allResponse.getErrorDesc(), allResponse.getData());
        assertThat(L(data2.stream().filter(l -> l.contains(fileName)).count()), is(Long.valueOf(0)));
        assertThat(allResponse.getTimestamp(), is(greaterThan(Long.valueOf(0))));
    }

    /**
     * Tests that deleted files are also considered when computing the sequence number during an "all"-request for a shared folder where the user has only rights to read/write own objects
     *
     * @throws Exception
     */
    @Test
    public void testMWB1062_SharedFolder_AccessOnlyOwnObjects() throws Exception {

        //As User 2: Create a folder
        NewFolderBody newFolderBody = new NewFolderBody();
        newFolderBody.setFolder(FolderFactory.getSimpleFolder(createName(), FolderManager.INFOSTORE));
        //@formatter:off
        FolderUpdateResponse createResponse = foldersApi2.createFolderBuilder()
                                                    .withFolderId(getPrivateInfostoreFolder(testUser2.getApiClient()))
                                                    .withTree("1")
                                                    .withNewFolderBody(newFolderBody)
                                              .execute();
        String folderId = checkResponse(createResponse.getError(), createResponse.getErrorDesc(), createResponse.getData());
        //@formatter:on
        //..and share it with user 1 so that he is only allowed to read/write his own objects
        FolderData folderData = new FolderData();
        FolderPermission permission = new FolderPermission();
        permission.setEntity(I(testUser.getUserId()));
        permission.setGroup(Boolean.FALSE);
        permission.setBits(I(2113666)); // = 1 0000001 0000001 0000010  = 1(delete only own objects) 0000001(1=modify only own objects) 0000001(1=read only own objects) 0000010(4=create subfolders)
        folderData.setPermissions(Collections.singletonList(permission));
        foldersApi2.updateFolderBuilder().withId(folderId).withFolderBody(new FolderBody().folder(folderData)).execute();

        //As user 1: Create an own file in the shared folder
        InfoItemListElement file = uploadFile(infostoreApi, folderId, createName(), "Some file content".getBytes(StandardCharsets.UTF_8));
        InfoItemsResponse allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder(folderId).withColumns("1").execute();
        Long timestamp = allResponse.getTimestamp();
        //..and delete it
        infostoreApi.deleteInfoItemsBuilder().withInfoItemListElement(Collections.singletonList(file)).withTimestamp(timestamp).execute();

        //Perform an all request on that folder and ensure the time stampt is correct
        allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder(folderId).withColumns("1").execute();
        timestamp = allResponse.getTimestamp();
        //The time stamp must not be "0" anymore because we do now consider deleted files as well
        assertThat(timestamp, is(greaterThan(Long.valueOf(0))));
    }
}
