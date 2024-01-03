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

package com.openexchange.mail.exportpdf;

import java.io.InputStream;
import com.openexchange.exception.OXException;

/**
 * {@link MailExportMailPartContainer} - Defines a container for a mail part
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportMailPartContainer {

    /**
     * Returns the base content type of the mail part, i.e. without
     * parameters, e.g. text/plain
     *
     * @return The base content type
     */
    String getBaseContentType();

    /**
     * Returns the mail part as an input stream
     *
     * @return The input stream
     * @throws OXException if an error is occurred
     */
    InputStream getInputStream() throws OXException;

    /**
     * Returns the mail part's size in bytes
     *
     * @return the mail part's size in bytes
     * @throws OXException if an error is occurred
     */
    long getSize() throws OXException;

    /**
     * Returns the title of the mail part
     *
     * @return The title of the mail part
     */
    String getTitle();

    /**
     * Returns the filename associated with the mail part
     *
     * @return The filename associated with the mail part, or <code>null</code> if there is no filename associated with the mail part
     */
    String getFileName();

    /**
     * Returns whether the mail part is inline
     *
     * @return <code>true</code> if inline; <code>false</code> otherwise
     */
    boolean isInline();

    /**
     * Returns the id of the mail part
     *
     * @return The id of the mail part
     */
    String getId();

    /**
     * Returns the image CID if the mail part is an inline image
     *
     * @return the image CID if the mail part is an inline image
     */
    String getImageCID();
}
