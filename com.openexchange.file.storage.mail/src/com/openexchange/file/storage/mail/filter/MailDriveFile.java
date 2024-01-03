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

package com.openexchange.file.storage.mail.filter;

import java.util.Date;

/**
 * {@link MailDriveFile} - Represents a file from Mail Drive.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public interface MailDriveFile {

    /**
     * Gets the mail metadata for this file.
     *
     * @return The mail metadata, or <code>null</code> if not yet parsed
     */
    MailMetadata getMetadata();

    /**
     * Gets the identifier.
     *
     * @return The identifier
     */
    String getId();

    /**
     * Gets the folder identifier.
     *
     * @return The folder identifier
     */
    String getFolderId();

    /**
     * Gets the file name
     *
     * @return The file name
     */
    String getFileName();

    /**
     * Gets the identifier of the user who created this file.
     *
     * @return The creator identifier
     */
    int getCreatedBy();

    /**
     * Gets the identifier of the user who modified this file.
     *
     * @return The modifier identifier
     */
    int getModifiedBy();

    /**
     * Gets the creation date.
     *
     * @return The creation date
     */
    Date getCreated();

    /**
     * Gets the last-modified date.
     *
     * @return The last-modified date
     */
    Date getLastModified();

    /**
     * Gets the MIME type.
     *
     * @return The MIME type
     */
    String getFileMIMEType();

    /**
     * Gets the size.
     *
     * @return The size in bytes or <code>-1</code>
     */
    long getFileSize();

    /**
     * Checks whether {@link #getFileSize()} returns the exact size w/o any encodings (e.g. base64) applied.
     *
     * @return <code>true</code> for exact size; otherwise <code>false</code>
     */
    boolean isAccurateSize();

}
