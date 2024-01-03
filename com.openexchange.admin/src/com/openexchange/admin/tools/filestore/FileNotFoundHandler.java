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

package com.openexchange.admin.tools.filestore;

import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorage;
import com.openexchange.filestore.FileStorageCodes;

/**
 * {@link FileNotFoundHandler} - Handles possible {@link FileStorageCodes#FILE_NOT_FOUND} error code when trying to retrieve a source file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface FileNotFoundHandler {

    /**
     * Handles specified file that could not be retrieved.
     * <p>
     * Precisely: Invoking {@link FileStorage#getFile(String)} yielded a {@link FileStorageCodes#FILE_NOT_FOUND} error code.
     *
     * @param file The absent file
     * @param sourceStorage The source storage from which file was attempted being retrieved
     * @throws OXException If an Open-Xchange error occurs
     * @throws StorageException If handling fails
     */
    void handleFileNotFound(String file, FileStorage sourceStorage) throws OXException, StorageException;

}
