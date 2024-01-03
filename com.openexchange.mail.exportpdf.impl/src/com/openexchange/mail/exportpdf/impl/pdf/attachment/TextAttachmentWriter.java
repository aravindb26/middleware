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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.InternalConverterProperties;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.MailExportProperty;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFElementRenderer;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFontUtils;
import com.openexchange.session.Session;
import rst.pdfbox.layout.elements.render.RenderContext;

/**
 * {@link TextAttachmentWriter}
 */
public class TextAttachmentWriter extends AbstractAttachmentWriter {

    private static final Logger LOG = LoggerFactory.getLogger(TextAttachmentWriter.class);

    /**
     * Initialises a new {@link TextAttachmentWriter}.
     */
    public TextAttachmentWriter(Session session, LeanConfigurationService leanConfigService) {
        super(session, leanConfigService, InternalConverterProperties.TEXT_FILE_EXTENSIONS);
    }

    @Override
    public void writeAttachment(MailExportConversionResult result, MailExportOptions options, RenderContext renderContext) {
        try (InputStream stream = result.getInputStream()) {
            if (checkNullStream(stream, result.getTitle())) {
                return;
            }
            renderContext.newPage();
            PDFElementRenderer.renderText(stream, renderContext, PDFFontUtils.getAlternativeFont(renderContext.getPdDocument()), getBodyFontSize());
        } catch (IOException e) {
            result.addWarnings(List.of(MailExportExceptionCode.IO_ERROR.create(e)));
            LOG.debug("", e);
        } catch (OXException e) {
            result.addWarnings(List.of(e));
            LOG.debug("", e);
        }
    }

    /**
     * Returns the body font size
     *
     * @return The body font size
     */
    private int getBodyFontSize() {
        return leanConfigService.getIntProperty(session.getUserId(), session.getContextId(), MailExportProperty.bodyFontSize);
    }
}
