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

package com.openexchange.mail.exportpdf.impl.pdf;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.mail.exportpdf.MailExportContentInformation;

/**
 * {@link PDFExportResult} - Holds the exported PDF document as a stream
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class PDFExportResult implements AutoCloseable {

    private final MailExportContentInformation contentHolder;
    private final ThresholdFileHolder fileHolder;

    /**
     * Initialises a new {@link PDFExportResult}.
     *
     * @param fileHolder The file holder with the pdf export
     * @param contentHolder The mail export content holder
     */
    public PDFExportResult(ThresholdFileHolder fileHolder, MailExportContentInformation contentHolder) {
        super();
        this.fileHolder = fileHolder;
        this.contentHolder = contentHolder;
    }

    @Override
    public void close() {
        Streams.close(fileHolder);
    }

    /**
     * Returns the stream with the exported PDF
     *
     * @return the stream with the exported PDF
     */
    public InputStream getStream() throws OXException {
        return fileHolder.getStream();
    }

    /**
     * Returns the size of the exported pdf in bytes
     *
     * @return the size of the exported pdf in bytes
     */
    public long getSize() {
        return fileHolder.getLength();
    }

    /**
     * Returns the title of the document (derived from the delegate content holder)
     *
     * @return the title of the document
     */
    public String getTitle() {
        return contentHolder.getSubject();
    }

    /**
     * Returns the creation date of the document (derived from the delegate content holder)
     *
     * @return the creation date of the document
     */
    public Date getDate() {
        return contentHolder.getSentDate();
    }

    /**
     * Returns a list with all warnings
     *
     * @return a list with all warnings
     */
    public List<OXException> getWarnings() {
        return contentHolder.getWarnings();
    }
}
