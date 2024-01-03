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
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import com.openexchange.exception.OXException;
import com.openexchange.i18n.Translator;
import com.openexchange.java.Functions.OXFunction;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.converters.header.MailHeader;
import com.openexchange.mail.exportpdf.converters.header.MailHeaderStrings;
import com.openexchange.mail.exportpdf.impl.pdf.PDFFormatConstants;
import com.openexchange.mail.exportpdf.impl.pdf.dao.PDFAttachmentMetadata;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFElementRenderer;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFactory;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontSet;
import rst.pdfbox.layout.elements.PageFormat;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.text.Alignment;
import rst.pdfbox.layout.text.Position;
import rst.pdfbox.layout.text.TextFragment;

/**
 * {@link ContentRenderer} - Renders headers and additional content into a given {@link PDDocument}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
abstract class ContentRenderer {

    private final Translator translator;

    private float lineSpacing;
    private int fontSize = 12;
    private float maxWidth = 0;
    private float yPos = 0;
    private int pageCount = 1;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link ContentRenderer}.
     *
     * @param translator The {@link Translator} to use for translation email headers
     */
    ContentRenderer(Translator translator) {
        this.translator = translator;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Helper method to add text to a paragraph, validate the height of the paragraph and catch exceptions caused by chars we cant handle
     *
     * @param value the value
     * @param paragraph the paragraph
     * @param fontSize the font size
     * @param fontSet the font set
     * @param yPos the y position
     * @param bottomMargin the bottom margin
     * @param overHead defines how many of the entries of the text array must be deleted again in case of an error. addText() adds different numbers of entries to the text array of the paragraph.
     * @return <code>true</code> if the paragraphs height is in range; <code>false</code> otherwise
     * @throws IOException if an I/O error is occurred
     */
    private boolean addTextAndCheckHeight(String value, Paragraph paragraph, int fontSize, FontSet fontSet, float yPos, float bottomMargin, int overHead) throws IOException {
        paragraph.addText(value, fontSize, fontSet.getPlainFont());
        try {
            return paragraph.getHeight() <= (yPos - bottomMargin);
        } catch (IllegalArgumentException e) {
            String fixedText = PDFElementRenderer.replaceUnknownCharacter(e, value);
            if (fixedText.equals(value)) {
                throw e;
            }
            for (int i = 0; i < overHead; i++) {
                paragraph.removeLast();
            }
            addTextAndCheckHeight(fixedText, paragraph, fontSize, fontSet, yPos, bottomMargin, 2);
            return true;
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the current line spacing
     *
     * @return The line spacing to use
     */
    protected float getLineSpacing() {
        return this.lineSpacing;
    }

    /**
     * Gets the current y position
     *
     * @return The current y position
     */
    protected float getYPos() {
        return yPos;
    }

    /**
     * Gets the font size for non-body context
     *
     * @return The font size to use for non boy content
     */
    protected int getFontSize() {
        return fontSize;
    }

    /**
     * Gets the current page count
     *
     * @return The current page count
     */
    protected int getPageCount() {
        return pageCount;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates the first, initial page for the rendering
     *
     * @param pageFormat The {@link PageFormat} used
     * @return The first, initial page to render the content into
     */
    protected abstract PDPage createPage(PageFormat pageFormat) throws OXException;

    /**
     * Creates a new page
     *
     * @param currentPage The current page
     * @return A new page to render the content into
     */
    protected abstract PDPage nextPage(PDPage currentPage) throws OXException;

    /**
     * Renders additional content, if any
     *
     * @param document The document to render the content into
     * @param pageFormat The page format to use
     * @throws OXException if an error is occurred
     */
    protected abstract void renderContent(PDDocument document, PageFormat pageFormat) throws OXException;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Sets the font size for non body content
     *
     * @param fontSize The font size for non body content
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Sets the maximum width of the document
     *
     * @param maxWidth The maximum width
     */
    public void setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * Renders the Header and body into the given document
     *
     * @param document The {@link PDDocument} to render headers and content into
     * @param fontSet The fonts to use
     * @param pageFormat The page format to use
     * @param attachmentMetadata The attachments to list in the headers
     * @param headerValueSupplier A callback function which returns a mail's header value for a given header
     * @throws IOException if an I/O error is occurred
     * @throws OXException if any other error is occurred
     */
    public void render(PDDocument document, FontSet fontSet, PageFormat pageFormat, List<PDFAttachmentMetadata> attachmentMetadata, OXFunction<MailHeader, String, OXException> headerValueSupplier) throws IOException, OXException {
        lineSpacing = new Paragraph().getLineSpacing();
        PDPage page = createPage(pageFormat);
        document.addPage(page);
        PDPageContentStream contentStream = null;
        try {
            contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true);
            float topMargin = pageFormat.getMarginTop();
            float bottomMargin = pageFormat.getMarginBottom();
            float leftMargin = pageFormat.getMarginLeft();
            float rightMargin = pageFormat.getMarginRight();
            float paragraphMaxWidth = pageFormat.getMediaBox().getWidth() - (leftMargin + rightMargin);

            float spaceWidth = PDFElementRenderer.getTextWidth(" ", getFontSize(), fontSet.getPlainFont());
            float indentationSpace = maxWidth + (PDFFormatConstants.EXTRA_SPACE * spaceWidth);
            yPos = pageFormat.getMediaBox().getHeight() - topMargin;

            //Write headers
            for (MailHeader mailHeader : MailHeader.values()) {
                //Get the actual header value
                String value = headerValueSupplier.apply(mailHeader);
                if (Strings.isEmpty(value)) {
                    continue;
                }
                Paragraph paragraph = PDFFactory.createParagraph(paragraphMaxWidth);
                paragraph.add(PDFFactory.createIndentation(translator.translate(mailHeader.getDisplayName()) + ":", indentationSpace, getFontSize(), fontSet.getBoldFont()));
                String[] values = value.split(" ");
                for (String v : values) {
                    if (addTextAndCheckHeight(v + " ", paragraph, getFontSize(), fontSet, yPos, bottomMargin, 1)) {
                        continue;
                    }
                    TextFragment lastRemoved = paragraph.removeLast();
                    paragraph.drawText(contentStream, new Position(leftMargin, yPos), Alignment.Left, (d, u, w, h) -> {
                    });
                    IOUtils.closeQuietly(contentStream);
                    pageCount++;
                    page = nextPage(page);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true);

                    paragraph = PDFFactory.createParagraph(paragraphMaxWidth);
                    paragraph.add(PDFFactory.createIndentation("", indentationSpace, getFontSize(), fontSet.getBoldFont()));
                    paragraph.addText(lastRemoved.getText(), getFontSize(), fontSet.getPlainFont());
                    yPos = pageFormat.getMediaBox().getHeight() - topMargin;
                }
                paragraph.drawText(contentStream, new Position(leftMargin, yPos), Alignment.Left, (d, u, w, h) -> {
                });
                yPos -= paragraph.getHeight();
            }

            //Write attachment meta data
            if (attachmentMetadata.isEmpty()) {
                yPos -= getFontSize() * getLineSpacing();
                PDFElementRenderer.writeHorizontalLine(contentStream, leftMargin, yPos, paragraphMaxWidth);
                renderContent(document, pageFormat);
                return;
            }
            Paragraph paragraph = PDFFactory.createParagraph(paragraphMaxWidth);
            paragraph.add(PDFFactory.createIndentation(attachmentMetadata.size() + " " + translator.translate(MailHeaderStrings.ATTACHMENTS) + ":", indentationSpace, getFontSize(), fontSet.getBoldFont()));
            for (PDFAttachmentMetadata m : attachmentMetadata) {
                if(addTextAndCheckHeight(m.displayText().substring(m.displayText().indexOf(":") + 1) + "\n", paragraph, fontSize, fontSet, yPos, bottomMargin, 2)){
                    continue;
                }
                TextFragment lastRemoved = paragraph.removeLast();
                paragraph.drawText(contentStream, new Position(leftMargin, yPos), Alignment.Left, (d, u, w, h) -> {
                });
                IOUtils.closeQuietly(contentStream);
                pageCount++;
                page = nextPage(page);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true);

                paragraph = PDFFactory.createParagraph(paragraphMaxWidth);
                paragraph.add(PDFFactory.createIndentation("", indentationSpace, getFontSize(), fontSet.getBoldFont()));
                paragraph.addText(lastRemoved.getText(), getFontSize(), fontSet.getPlainFont());
                yPos = pageFormat.getMediaBox().getHeight() - topMargin;
            }
            paragraph.drawText(contentStream, new Position(leftMargin, yPos), Alignment.Left, (d, u, w, h) -> {
            });
            yPos -= paragraph.getHeight();
            PDFElementRenderer.writeHorizontalLine(contentStream, leftMargin, yPos, paragraphMaxWidth);
            yPos -= getFontSize() * getLineSpacing();

            renderContent(document, pageFormat);
        } finally {
            IOUtils.closeQuietly(contentStream);
        }
    }
}
