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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.json.JSONException;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.ajax.infostore.apiclient.FileMovePermissionWarningTest.FolderType;
import com.openexchange.ajax.parser.ResponseParser;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.JSON;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 *
 * {@link AbstractFolderMoveWarningTest}
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Ottersbach</a>
 * @since v7.10.5
 */
public abstract class AbstractFolderMoveWarningTest extends AbstractFolderMovePermissionsTest {

    protected SessionAwareClient apiClient3;

    protected Integer userId3;

    protected AbstractFolderMoveWarningTest(String type) {
        super(type);
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        userId3 = I(testContext.acquireUser().getUserId());
    }

    protected void checkFolderMove(String sourceFolder, String destinationFolder, FolderExceptionErrorMessage warning, FolderType sourceType, FolderType targetType) throws ApiException, JSONException {
        String folderToMove = createChildFolder(sourceFolder);
        checkFolderMove(folderToMove, sourceFolder, destinationFolder, warning.create(getWarningParameters(folderToMove, sourceFolder, destinationFolder, sourceType, targetType)));
    }

    protected void checkFolderMove(String folderToMove, String sourceFolder, String destinationFolder, OXException expectedWarning) throws ApiException, JSONException {
        FolderUpdateResponse response = moveFolder(folderToMove, destinationFolder, null);

        checkResponseForWarning(response, expectedWarning);

        checkParentFolder(folderToMove, sourceFolder);

        FolderUpdateResponse responseIgnored = moveFolder(folderToMove, destinationFolder, Boolean.TRUE);
        checkResponseForWarning(responseIgnored);

        checkParentFolder(folderToMove, destinationFolder);
    }

    protected void checkParentFolder(String folderToCheck, String expectedParentFolder) throws ApiException {
        FolderResponse folderReponse = api.getFolder(folderToCheck, TREE, null, null, null);
        assertNotNull(folderReponse);
        FolderData data = folderReponse.getData();
        assertNotNull(data);
        assertEquals(expectedParentFolder, data.getFolderId(), "Not the expected parent folder.");
    }

    protected void checkRequestWithIgnoreWarnings(String toMoveFolderId, String destinationFolder) throws ApiException {
        FolderUpdateResponse responseIgnored = moveFolder(toMoveFolderId, destinationFolder, Boolean.TRUE);
        assertNotNull(responseIgnored);
        assertNull(responseIgnored.getError());
        checkParentFolder(toMoveFolderId, destinationFolder);
    }

    protected void checkResponseForWarning(FolderUpdateResponse response, OXException expectedWarning) throws JSONException {
        assertNotNull(response);
        assertNotNull(response.getError());
        Response parsedResponse = ResponseParser.parse(new JSON().serialize(response));
        assertTrue(parsedResponse.hasWarnings());
        boolean result = false;
        for (OXException warning : parsedResponse.getWarnings()) {
            if (warning.getErrorCode().contentEquals(expectedWarning.getErrorCode()) && warning.getPlainLogMessage().equals(expectedWarning.getPlainLogMessage())) {
                result = true;
            }
        }
        assertTrue(result, "Excepected: \"" + expectedWarning.getMessage() + "\"\n, but warnings only contains : \"" + parsedResponse.getWarnings().toString() + "\"");
    }

    protected void checkResponseForWarning(FolderUpdateResponse response) {
        assertNotNull(response);
        assertNotNull(response.getError());
        assertEquals(Category.CATEGORY_WARNING.toString(), response.getCategories(), "Warning expected, but no error.");

    }

    protected String createPrivateFolder(Integer sharedToUserId) throws Exception {
        return createNewFolder(sharedToUserId, BITS_REVIEWER, false, true);
    }

    protected String getFolderName(String folderId) throws ApiException {
        FolderResponse folder = api.getFolder(folderId, TREE, null, null, null);
        return folder.getData().getTitle();
    }

    protected FolderUpdateResponse moveFolder(String folderToMove, String destinationFolder, Boolean ignoreWarnings) throws ApiException {
        FolderBody folderBody = new FolderBody();
        FolderData folderData = new FolderData();
        folderData.setFolderId(destinationFolder);
        folderData.setPermissions(null);
        folderBody.setFolder(folderData);

        FolderUpdateResponse response = api.updateFolder(folderToMove, folderBody, Boolean.FALSE, L(System.currentTimeMillis()), TREE, null, Boolean.FALSE, null, Boolean.FALSE, ignoreWarnings);
        return response;
    }

    protected Object[] getWarningParameters(String sourceFolder, String targetFolder, FolderType sourceType, FolderType targetType) throws ApiException {
         return getWarningParameters(null, sourceFolder, targetFolder, sourceType, targetType);
    }

    protected Object[] getWarningParameters(String folderToMove, String sourceFolder, String targetFolder, FolderType sourceType, FolderType targetType) throws ApiException {
        String folderPath = folderToMove != null ? getPath(folderToMove, sourceFolder, sourceType) : "";
        String sourceFolderPath = getPath(sourceFolder, sourceType);
        String targetFolderPath = getPath(targetFolder, targetType);
        Object folderId = folderToMove;
        Object targetFolderId = targetFolder;
        return new Object[] { folderPath, sourceFolderPath, targetFolderPath, folderId, targetFolderId };
    }

    private String getPath(String sourceFolder, FolderType sourceType) throws ApiException {
        return getPath(null, sourceFolder, sourceType);
    }

    private String getPath(String folderToMove, String sourceFolder, FolderType sourceType) throws ApiException {
        return sourceType.getRootPath() + (sourceType.equals(FolderType.SHARED) ? getFolderName(getPrivateInfostoreFolder(testUser2.getApiClient())) + "/" : "") + getFolderName(sourceFolder) + (folderToMove != null ? "/" + getFolderName(folderToMove) : "");
    }
}
