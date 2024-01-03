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

package com.openexchange.mail.exportpdf.pdfa;

import java.io.InputStream;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;
import com.openexchange.session.Session;

/**
 * {@link PDFAConverter} - Converts ordinary PDF files into PDF/A files
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public interface PDFAConverter {

    /**
     * Converts a ordinary PDFA document into PDF/A
     *
     * @param session The {@link Session} related to the conversion request
     * @param pdfVersion The {@link PDFAVersion} to convert the given PDF data into
     * @param pdfData The actual, ordinary PDF data to convert to PDF/A
     * @return The result as {@link PDFAConvertionResult}
     * @throws OXException
     */
    public MailExportConversionResult convert(Session session, PDFAVersion pdfVersion, InputStream pdfData) throws OXException;
}
