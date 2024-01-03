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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import com.openexchange.i18n.Translator;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.converter.DefaultMailExportConverterOptions;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFactory;
import rst.pdfbox.layout.elements.PageFormat;

/**
 * {@link CalculatingContentRenderer} - A {@link ContentRenderer} which simulates rendering in order to calculate header spaces and offsets
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CalculatingContentRenderer extends ContentRenderer {

    private final DefaultMailExportConverterOptions converterOptions;

    private int mailHeaderLines;
    private float topMarginOffset;
    private int headerPageCount;

    /**
     * Initializes a new {@link CalculatingContentRenderer}.
     *
     * @param translator The {@link Translator} to use for translation email headers
     * @param converterOptions The relevant {@link MailExportConverterOptions}
     */
    public CalculatingContentRenderer(Translator translator, DefaultMailExportConverterOptions converterOptions) {
        super(translator);
        this.converterOptions = converterOptions;
    }

    /**
     * Gets the amount of calculated mail header lines
     *
     * @return The amount of mail header lines
     */
    public int getMailHeaderLines() {
        return mailHeaderLines;
    }

    /**
     * Gets the calculated top margin offset
     *
     * @return The top margin offset
     */
    public float getTopMarginOffset() {
        return topMarginOffset;
    }

    /**
     * Gets the calculated header page count
     *
     * @return The header page count
     */
    public int getHeaderPageCount() {
        return headerPageCount;
    }

    @Override
    protected PDPage createPage(PageFormat pageFormat) {
        return new PDPage(new PDRectangle(converterOptions.getWidth(), converterOptions.getHeight()));
    }

    @Override
    protected PDPage nextPage(PDPage currentPage) {
        return new PDPage(currentPage.getMediaBox());
    }

    @Override
    protected void renderContent(PDDocument document, PageFormat pageFormat) {
        float offset = pageFormat.getMediaBox().getHeight() - getYPos();
        float lines = offset / getFontSize() / getLineSpacing() + 1;

        //Set results
        mailHeaderLines = Math.round(lines);
        topMarginOffset = PDFFactory.unitsToMillimeter(offset - pageFormat.getMarginTop());
        headerPageCount = document.getNumberOfPages();
    }
}
