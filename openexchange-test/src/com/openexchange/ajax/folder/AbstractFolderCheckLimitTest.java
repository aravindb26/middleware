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

package com.openexchange.ajax.folder;

import static com.openexchange.java.Autoboxing.L;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.FolderCheckLimitsData;
import com.openexchange.testing.httpclient.models.FolderCheckLimitsFiles;
import com.openexchange.testing.httpclient.models.FolderCheckLimitsResponse;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractFolderCheckLimitTest}
 *
 * @author <a href="mailto:jan-oliver.huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10.1
 */
public class AbstractFolderCheckLimitTest extends AbstractConfigAwareAPIClientSession {

    private static final String FILE_NAME = "quotaCheck";
    private static final String FILE_ENDING = ".txt";
    protected String quotaTestFolderId;

    private FoldersApi foldersApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        foldersApi = new FoldersApi(getApiClient());
        quotaTestFolderId = getPrivateInfostoreFolder();
    }

    @Override
    public TestClassConfig getTestConfig() {
        // @formatter:off
        return TestClassConfig.builder()
                              
                              .withUserConfig(TestUserConfig.builder()
                                                            .withQuota(L(100l))
                                                            .build())
                              .build();
        // @formatter:on
    }

    private String getPrivateInfostoreFolder() throws Exception {
        ConfigApi configApi = new ConfigApi(getApiClient());
        ConfigResponse configNode = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath());
        return (configNode.getData()).toString();
    }

    protected FolderCheckLimitsResponse checkLimits(FolderCheckLimitsData body, String folderId, String type) throws ApiException {
        return foldersApi.checkLimits(folderId, type, body);
    }

    /**
     * Creates a data object for the quota check
     *
     * @param folder The filestore folder
     * @param files The file data object
     * @return data, the object to start the request with
     */
    protected FolderCheckLimitsData createQuotaCheckData(List<FolderCheckLimitsFiles> files) {
        FolderCheckLimitsData infoItemQuotaCheckData = new FolderCheckLimitsData();
        infoItemQuotaCheckData.setFiles(files);
        return infoItemQuotaCheckData;
    }

    /**
     * Creates a data object with file meta data
     *
     * @param fileSize The file size
     * @return data, the object which contains the file size and the file name
     */
    protected FolderCheckLimitsFiles createQuotaCheckFiles(Long fileSize) {
        return createQuotaCheckFiles(getFileName(), fileSize);
    }

    /**
     * Creates a data object with file meta data
     *
     * @param fileName The file name.
     * @param fileSize The file size.
     * @return data, the object which contains the file size and the file name
     */
    protected FolderCheckLimitsFiles createQuotaCheckFiles(String fileName, Long fileSize) {
        FolderCheckLimitsFiles infoItemQuotaCheckFile = new FolderCheckLimitsFiles();
        infoItemQuotaCheckFile.setName(fileName);
        infoItemQuotaCheckFile.setSize(fileSize);
        return infoItemQuotaCheckFile;
    }

    /**
     * Creates a file name
     *
     * @return String The file name
     */
    public String getFileName() {
        return FILE_NAME + UUID.randomUUID() + FILE_ENDING;
    }

    private static final Map<String, String> CONFIG = new HashMap<>();

    static {
        CONFIG.put("com.openexchange.quota.infostore", "2");
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return CONFIG;
    }

    @Override
    protected String getScope() {
        return "user";
    }
}
