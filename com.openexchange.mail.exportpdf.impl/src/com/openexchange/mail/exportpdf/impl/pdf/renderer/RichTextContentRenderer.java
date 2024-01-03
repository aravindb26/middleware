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

package com.openexchange.mail.exportpdf.impl.pdf.renderer;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import com.openexchange.exception.OXException;
import com.openexchange.i18n.Translator;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.PageFormat;
import rst.pdfbox.layout.elements.render.RenderContext;

/**
 * {@link RichTextContentRenderer} - The default implementation for rendering headers and rich text body into a given {@link PDDocument}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class RichTextContentRenderer extends ContentRenderer {

    private final MailExportConverterOptions converterOptions;
    private final PDDocument mailBody;
    private RenderContext renderContext;

    /**
     * Initializes a new {@link RichTextContentRenderer}.
     *
     * @param translator The {@link Translator} to use for translation email headers
     * @param converterOptions The relevant {@link MailExportConverterOptions}
     * @param mailBody The rich text body to render
     */
    public RichTextContentRenderer(Translator translator, MailExportConverterOptions converterOptions, PDDocument mailBody) {
        super(translator);
        this.converterOptions = converterOptions;
        this.mailBody = mailBody;
    }

    /**
     * Gets the {@link RenderContext} created during rendering
     *
     * @return The {@link RenderContext} created during rendering
     */
    public RenderContext getRenderContext() {
        return renderContext;
    }

    @Override
    protected PDPage createPage(PageFormat pageFormat) {
        return converterOptions.getHeaderPageCount() > 1 ? new PDPage(pageFormat.getMediaBox()) : mailBody.getPage(0);
    }

    @Override
    protected PDPage nextPage(PDPage currentPage) {
        return converterOptions.getHeaderPageCount() == getPageCount() ? mailBody.getPage(0) : new PDPage(currentPage.getMediaBox());
    }

    @Override
    protected void renderContent(PDDocument document, PageFormat pageFormat) throws OXException {
        if (renderContext == null) {
            try {
                renderContext = new RenderContext(new Document(pageFormat), document);
            } catch (IOException e) {
                throw MailExportExceptionCode.IO_ERROR.create(e.getMessage(), e);
            }
        }
        document.removePage(document.getNumberOfPages() - 1);
        for (int i = 1; i < mailBody.getNumberOfPages(); i++) {
            document.addPage(mailBody.getPage(i));
        }
    }
}
