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

package com.openexchange.mail.exportpdf.impl.pdf.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.mail.exportpdf.InternalConverterProperties;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFactory;
import com.openexchange.session.Session;
import rst.pdfbox.layout.elements.render.RenderContext;

/**
 * {@link PDFAttachmentWriter}
 */
public class PDFAttachmentWriter extends AbstractAttachmentWriter {

    private static final Logger LOG = LoggerFactory.getLogger(PDFAttachmentWriter.class);

    /**
     * Initialises a new {@link PDFAttachmentWriter}.
     */
    public PDFAttachmentWriter(Session session, LeanConfigurationService leanConfigService) {
        super(session, leanConfigService, InternalConverterProperties.PDF_FILE_EXTENSIONS);
    }

    @Override
    public void writeAttachment(MailExportConversionResult result, MailExportOptions options, RenderContext renderContext) {
        if (result.getStatus().equals(Status.UNAVAILABLE)) {
            return;
        }
        try (InputStream stream = result.getInputStream()) {
            if (checkNullStream(stream, result.getTitle())) {
                return;
            }
            PDDocument pdf = PDFFactory.createDocument(stream);
            closeables.add(pdf);
            if (pdf.getNumberOfPages() == 0) {
                return;
            }
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                renderContext.getPdDocument().addPage(pdf.getPage(i));
            }
        } catch (IOException e) {
            result.addWarnings(List.of(MailExportExceptionCode.IO_ERROR.create(e)));
            LOG.debug("", e);
        }
    }
}
