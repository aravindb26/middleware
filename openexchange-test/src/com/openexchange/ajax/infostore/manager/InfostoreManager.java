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

package com.openexchange.ajax.infostore.manager;

import static com.openexchange.java.Autoboxing.L;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.junit.Assert;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.modules.InfostoreApi;

/**
 * {@link InfostoreManager}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class InfostoreManager {

    private final List<String> managedFileIds;

    private final InfostoreApi infostoreApi;

    /**
     * Initializes a new {@link InfostoreManager}.
     * 
     * @param infostoreApi The infostore api client
     */
    public InfostoreManager(InfostoreApi infostoreApi) {
        super();
        this.infostoreApi = infostoreApi;
        this.managedFileIds = new LinkedList<>();
    }

    /**
     * Clean up. Deletes all managed folders, i.e. folders created via this manager.
     *
     * @throws ApiException if an api error is occurred
     */
    public void cleanUp() throws ApiException {
        List<InfoItemListElement> list = managedFileIds.parallelStream().map((f) -> new InfoItemListElement().folder(f)).toList();
        infostoreApi.deleteInfoItems(L(Long.MAX_VALUE), list, Boolean.TRUE, null);
    }

    /**
     * Uploads the specified data to the specified folder
     *
     * @param folder The folder to upload the data
     * @param filename the filename
     * @param data the actual data
     * @return The file id
     * @throws ApiException if an api error is occurred
     */
    public String uploadFile(String folder, String filename, byte[] data) throws ApiException {
        InfoItemUpdateResponse response = infostoreApi.uploadInfoItem(folder, filename, data, null, null, null, null, null, null, null, null, null, null, null, Long.valueOf(data.length), Boolean.FALSE, Boolean.FALSE, null, null);
        Assert.assertNull(response.getErrorDesc(), response.getError());
        Assert.assertNotNull(response.getData());
        String fileId = response.getData();
        managedFileIds.add(fileId);
        return fileId;
    }

}
