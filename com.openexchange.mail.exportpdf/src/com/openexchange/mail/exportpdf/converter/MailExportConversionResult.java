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

package com.openexchange.mail.exportpdf.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import com.openexchange.exception.OXException;

/**
 * {@link MailExportConversionResult} - The conversion result of a mail export
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportConversionResult extends AutoCloseable {

    public enum Status {

        /** Converted successfully */
        SUCCESS,

        /** Conversion failed with error */
        ERROR,

        /** The converter is currently disabled */
        DISABLED,

        /** No converter available */
        UNAVAILABLE,

        /** The content-type of the given conversion object is not supported */
        TYPE_NOT_SUPPORTED,

        /**
         * The attachment is corrupt
         */
        ATTACHMENT_CORRUPT

        ;
    }

    /**
     * Gets the conversion result status.
     *
     * @return The status
     */
    Status getStatus();

    /**
     * Gets the {@link PDFAVersion} of the resulting PDF data
     *
     * @return The {@link PDFAVersion} of the resulting PDF data
     */
    PDFAVersion getPDFAVersion();

    /**
     * Returns any warnings that might have occurred during the mail conversion
     *
     * @return any warnings that might have occurred during the mail conversion
     */
    List<OXException> getWarnings();

    /**
     * Returns the converted data as an input stream.
     *
     * @return the converted email as an input stream
     * @throws IOException If creating the stream fails
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns the content-type of the converted mail part
     *
     * @return the content-type of the converted mail part
     */
    String getContentType();

    /**
     * Returns the optional title of the mail part, i.e.
     * if it is a message, it returns the subject, if it is a
     * file it returns the filename.
     *
     * @return The optional title of the mail part
     */
    Optional<String> getTitle();

    /**
     * @param warnings The warnings to add
     */
    void addWarnings(List<OXException> warnings);
}
