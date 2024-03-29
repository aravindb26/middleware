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

package com.openexchange.file.storage.boxcom;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.boxcom.access.BoxOAuthAccess;
import com.openexchange.oauth.access.OAuthAccess;
import com.openexchange.session.Session;

/**
 * {@link AbstractBoxResourceAccess}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractBoxResourceAccess {

    /** Status code (400) indicating a bad request. */
    protected static final int SC_BAD_REQUEST = 400;

    /** Status code (401) indicating that the request requires HTTP authentication. */
    protected static final int SC_UNAUTHORIZED = 401;

    /** Status code (404) indicating that the requested resource is not available. */
    protected static final int SC_NOT_FOUND = 404;

    protected final BoxOAuthAccess boxAccess;
    protected final Session session;
    protected final FileStorageAccount account;
    protected final String rootFolderId;

    /**
     * Initializes a new {@link AbstractBoxResourceAccess}.
     *
     * @param boxAccess The box access
     * @param account The {@link FileStorageAccount}
     * @param session The groupware session
     */
    protected AbstractBoxResourceAccess(BoxOAuthAccess boxAccess, FileStorageAccount account, Session session) {
        super();
        this.boxAccess = boxAccess;
        this.account = account;
        this.session = session;
        rootFolderId = "0";
    }

    /**
     * Performs given closure.
     *
     * @param closure The closure to perform
     * @return The return value
     * @throws OXException If performing closure fails
     */
    protected <R> R perform(BoxClosure<R> closure) throws OXException {
        return closure.perform(this, boxAccess, session);
    }

    /**
     * Checks if given typed object is trashed
     *
     * @param folder The typed object to check
     * @return <code>true</code> if typed object is trashed; otherwise <code>false</code>
     */
    protected boolean isFolderTrashed(BoxFolder.Info folder) {
        return hasTrashedParent(folder);
    }

    /**
     * Checks (recursively) whether the specified box folder has a trashed parent
     *
     * @param boxFolder The box folder
     * @return <code>true</code> if the parent folder is trashed; otherwise <code>false</code>
     */
    private boolean hasTrashedParent(BoxFolder.Info boxFolder) {
        BoxFolder.Info parent = boxFolder.getParent();
        if (null == parent) {
            return false;
        }
        if ("trash".equals(parent.getID())) {
            return true;
        }
        return hasTrashedParent(parent);
    }

    /**
     * Checks if given file is trashed
     *
     * @param fileInfo The file to check
     * @return <code>true</code> if the file is trashed; otherwise <code>false</code>
     */
    protected boolean isFileTrashed(BoxFile.Info fileInfo) {
        return fileInfo.getTrashedAt() != null;
    }

    /**
     * Checks the file's validity
     *
     * @param fileInfo The file's validity
     * @throws OXException if the specified file was trashed
     */
    protected void checkFileValidity(BoxFile.Info fileInfo) throws OXException {
        if (isFileTrashed(fileInfo)) {
            throw FileStorageExceptionCodes.NOT_A_FILE.create(BoxConstants.ID, fileInfo.getID());
        }
    }

    /**
     * Handles authentication error.
     *
     * @param e The authentication error
     * @param session The associated session
     * @return The re-initialized Box.com access
     * @throws OXException If authentication error could not be handled
     */
    protected BoxOAuthAccess handleAuthError(BoxAPIException e, Session session) throws OXException {
        try {
            boxAccess.initialize();
            return boxAccess;
        } catch (OXException oxe) {
            Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractBoxResourceAccess.class);
            logger.warn("Could not re-initialize Box.com access", oxe);

            throw FileStorageExceptionCodes.PROTOCOL_ERROR.create(e, BoxConstants.ID, e.getMessage());
        }
    }

    /**
     * Handles given HTTP response error.
     *
     * @param identifier The optional identifier for associated Box.com resource
     * @param e The HTTP error
     * @return The resulting exception
     * @throws BoxAPIException If specified {@code BoxAPIException} instance should be handled outside
     */
    protected OXException handleHttpResponseError(String identifier, String accountId, BoxAPIException e) {
        if (null != identifier && SC_NOT_FOUND == e.getResponseCode()) {
            return FileStorageExceptionCodes.NOT_FOUND.create(e, "Box", identifier);
        }
        if (SC_UNAUTHORIZED == e.getResponseCode()) {
            throw e;
        }
        if (accountId != null && e.getResponseCode() == SC_BAD_REQUEST) {
            try {
                JSONObject responseBody = JSONServices.parseObject(e.getResponse());
                String errorDesc = responseBody.getString("error_description");
                if (errorDesc.equals("Refresh token has expired")) {
                    try {
                        //TODO: refresh token
                        boxAccess.initialize();
                    } catch (OXException ex) {
                        return ex;
                    }
                }
            } catch (JSONException e1) {
                return FileStorageExceptionCodes.JSON_ERROR.create(e1, e.getMessage());
            }
        }
        return FileStorageExceptionCodes.PROTOCOL_ERROR.create(e, "HTTP", e.getResponseCode() + " " + e.getResponse());
    }

    /**
     * Gets the Box.com folder identifier from given file storage folder identifier
     *
     * @param folderId The file storage folder identifier
     * @return The appropriate Box.com folder identifier
     */
    protected String toBoxFolderId(String folderId) {
        return FileStorageFolder.ROOT_FULLNAME.equals(folderId) ? rootFolderId : folderId;
    }

    /**
     * Gets the file storage folder identifier from given Box.com folder identifier
     *
     * @param boxId The Box.com folder identifier
     * @return The appropriate file storage folder identifier
     */
    protected String toFileStorageFolderId(String boxId) {
        return rootFolderId.equals(boxId) || "0".equals(boxId) ? FileStorageFolder.ROOT_FULLNAME : boxId;
    }

    /**
     * Get a {@link BoxAPIConnection} from the {@link OAuthAccess}
     *
     * @return A {@link BoxAPIException}
     * @throws OXException if the API connection cannot be retrieved
     */
    protected BoxAPIConnection getAPIConnection() throws OXException {
        boxAccess.ensureNotExpired();
        return boxAccess.<BoxAPIConnection> getClient().client;
    }
}
