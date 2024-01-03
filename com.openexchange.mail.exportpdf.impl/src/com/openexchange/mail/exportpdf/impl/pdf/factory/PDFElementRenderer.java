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

package com.openexchange.mail.exportpdf.impl.pdf.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.java.Charsets;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontSet;
import rst.pdfbox.layout.elements.Element;
import rst.pdfbox.layout.elements.render.LayoutHint;
import rst.pdfbox.layout.elements.render.RenderContext;
import rst.pdfbox.layout.elements.render.VerticalLayout;
import rst.pdfbox.layout.elements.render.VerticalLayoutHint;
import rst.pdfbox.layout.text.Alignment;

/**
 * {@link PDFElementRenderer} - Utility class for writing different elements to a PDF page
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class PDFElementRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(PDFElementRenderer.class);
    public static final String NEWLINE = "\\r?\\n";
    public static final String DEFAULT_REPLACEMENT = "�";

    /**
     * Initialises a new {@link PDFElementRenderer}.
     */
    private PDFElementRenderer() {
        super();
    }

    /**
     * Writes the specified input stream as a text to the specified render context.
     *
     * @param stream The stream to write to
     * @param renderContext The context to write to
     * @param fontSet The font
     * @param fontSize The font size
     * @throws IOException if an I/O error is occurred
     */
    public static void renderText(InputStream stream, RenderContext renderContext, FontSet fontSet, int fontSize) throws IOException {
        renderText(IOUtils.toString(stream, Charsets.UTF_8), renderContext, fontSet.getPlainFont(), fontSize);
    }

    /**
     * Writes the specified text to the specified document
     *
     * @param text The text to write
     * @param renderContext the context
     * @param font The font set to use
     * @param fontSize The font size
     * @throws IOException
     */
    public static void renderText(String text, RenderContext renderContext, PDFont font, int fontSize) throws IOException {
        renderText(text, renderContext, font, fontSize, Alignment.Justify);
    }

    /**
     * Writes the specified text to the specified render context
     *
     * @param text The text to write
     * @param renderContext The context
     * @param font The font to use
     * @param fontSize The font size
     * @param alignment the alignment
     * @throws Exception
     */
    private static void renderText(String text, RenderContext renderContext, PDFont font, int fontSize, Alignment alignment) throws IOException {
        // Tab characters are not part of some fonts, hence we replace them with 8 spaces
        String[] lines = text.replace("\\t", " ".repeat(8)).split(NEWLINE);
        try {
            for (String line : lines) {
                renderElement(renderContext, PDFFactory.createParagraph(line, fontSize, font, alignment));
            }
        } catch (IllegalArgumentException e) {
            String fixedText = replaceUnknownCharacter(e, text);
            if (!fixedText.equals(text)) {
                renderText(fixedText, renderContext, font, fontSize, alignment);
            } else {
                throw e;
            }
        }
    }

    /**
     * @param e the exception containing the information about the unknown character
     * @param text The text in which the character should be replaced
     * @return The string with the replaced character
     */
    public static String replaceUnknownCharacter(IllegalArgumentException e, String text) {
        Pattern pattern = Pattern.compile("for U+(.*) \\([^\\\\)]*+\\)");
        Matcher matcher = pattern.matcher(e.getMessage());
        matcher.find();

        String unknownChar = matcher.group(1).substring(1);
        int unknownCharAsInt = Integer.parseInt(unknownChar, 16);
        String unknownCharAsString = Character.toString(unknownCharAsInt);

        LOG.debug("Error while rendering the Text. Were not able to render the following char: {} and replaced it.", unknownCharAsString);
        return text.replace(unknownCharAsString, DEFAULT_REPLACEMENT);
    }

    /**
     * Renders the specified element with the default {@link VerticalLayout}
     *
     * @param renderContext The render context
     * @param element The element to render
     */
    public static void renderElement(RenderContext renderContext, Element element) {
        renderElement(renderContext, element, new VerticalLayoutHint());
    }

    /**
     * Renders the specified element with the specified layout hint
     *
     * @param renderContext The render context
     * @param element The element to render
     * @param layoutHint The layout hint to use
     */
    public static void renderElement(RenderContext renderContext, Element element, LayoutHint layoutHint) {
        try {
            renderContext.render(renderContext, element, layoutHint);
        } catch (IOException e) {
            LOG.debug("", e);
        }
    }

    /**
     * Returns the text width in document space units
     *
     * @param text The text
     * @param font The font
     * @return The text width
     * @throws IOException if an I/O error is occurred
     */
    public static float getTextWidth(String text, int fontSize, PDFont font) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    /**
     * Returns the cap height (i.e. the height of a capital letter above the baseline)
     * for the specified font with the specified font size
     *
     * @param font The font
     * @param fontSize The font size
     * @return The cap height
     * @see <a href="https://en.wikipedia.org/wiki/Cap_height">Cap Height</a>
     */
    public static float getCapHeight(PDFont font, int fontSize) {
        return (font.getFontDescriptor().getCapHeight()) / 1000 * fontSize;
    }

    /////////////// LEGACY //////////////////

    /**
     * Writes a horizontal line to the specified content stream
     *
     * @param stream the page content stream to write to
     * @param x The x coordinate
     * @param y The y coordinate
     * @param xs The starting x coordinate
     * @throws IOException if an I/O error is occurred
     */
    public static void writeHorizontalLine(PDPageContentStream stream, float x, float y, float xs) throws IOException {
        stream.moveTo(x, y);
        stream.lineTo(xs, y);
        stream.stroke();
    }
}
