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

import static com.openexchange.java.Autoboxing.B;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemBody;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemUpdatesResponse;
import com.openexchange.testing.httpclient.models.InfoItemsResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;

/**
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class MWB1338 extends AbstractAPIClientSession {

    private InfostoreApi infostoreApi;
    private FoldersApi foldersApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        foldersApi = new FoldersApi(getApiClient());
        infostoreApi = new InfostoreApi(getApiClient());
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

    private String createName() {
        return "MWB1338_" + DateTime.now().getMillis();
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

    @Test
    public void test() throws Exception {

        //Create a folder and upload a new file
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
        List<InfoItemListElement> files = Arrays.asList(file1);
        InfoItemsResponse allResponse = infostoreApi.getAllInfoItemsBuilder().withFolder(folderId).withColumns("1").execute();
        Long timestamp = allResponse.getTimestamp();

        //Delete the file
        infostoreApi.deleteInfoItemsBuilder().withInfoItemListElement(files).withTimestamp(timestamp).execute();

        //As another client: Try to move the file into another folder. This must fail because the file was deleted in the meantime
        String newFolder = getPrivateInfostoreFolder(getApiClient());
        InfoItemData updateInfoItem = new InfoItemData().folderId(newFolder);
        InfoItemBody updateBody = new InfoItemBody()._file(updateInfoItem);
        Long outdatedTimeStamp = Long.valueOf(0); //Using an outdated time stamp to mimic a different client
        InfoItemUpdateResponse updatedInfoItem = infostoreApi.updateInfoItem(file1.getId(), outdatedTimeStamp , updateBody, null, B(false));
        assertThat("The file update should have caused a conflict.", updatedInfoItem.getCode(), is("IFO-1302"));

        //Performing an updates call. This must not be empty and still contain the data from the deleted file
        InfoItemUpdatesResponse updatesResponse = infostoreApi.getInfoItemUpdatesBuilder().withFolder(folderId).withColumns("1").execute();
        List<Object> data = checkResponse(updatesResponse.getError(), updatesResponse.getErrorDesc(), updatesResponse.getData());
        Optional<Object> u = data.stream().filter(  f -> ((String)f).equalsIgnoreCase(file1.getId())).findFirst();
        assertThat("The updates response data should not be empty", B(u.isPresent()), is(Boolean.TRUE));
    }
}
