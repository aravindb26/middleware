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

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportProperty;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontSet;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.stream.CountingInputStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import rst.pdfbox.layout.elements.HorizontalRuler;
import rst.pdfbox.layout.elements.Orientation;
import rst.pdfbox.layout.elements.PageFormat;
import rst.pdfbox.layout.elements.PageFormat.PageFormatBuilder;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.shape.Stroke;
import rst.pdfbox.layout.text.Alignment;
import rst.pdfbox.layout.text.Indent;
import rst.pdfbox.layout.text.SpaceUnit;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

/**
 * {@link PDFFactory} - Factory for creating various PDF elements
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class PDFFactory {

    private static final String COLOUR_PROFILE = "sRGB_CS_profile.icm";
    private static final String COLOUR_PROFILE_NAME = "sRGB IEC61966-2.1";
    private static final String COLOUR_REGISTRY = "https://www.color.org";

    /**
     * Defines the amount of pixels in 1 centimetre
     *
     * @see <a href="https://www.w3.org/TR/css3-values/#absolute-lengths">Absolute Lengths</a>
     */
    private static final float PIXELS_IN_1_CM = 96f;

    /** The internally used DPI of PDF documents */
    private static final int DPI = 72;

    private static final float UNITS_TO_PIXELS = PIXELS_IN_1_CM / DPI;

    /** The static factor to convert from millimetres to units */
    private static final float MM_TO_UNITS = 1 / (10 * 2.54f) * DPI;

    private static final long ONE_GB = 1024L * 1024L * 1024L;
    public static final PDColor BLUE = createColor(0, 0, 255);
    public static final PDColor BLACK = createColor(0, 0, 0);

    /**
     * Initialises a new {@link PDFFactory}.
     */
    private PDFFactory() {
        super();
    }

    //<editor-fold desc="PDF-Layout">
    ///////////////////////////////// PDF-Layout /////////////////////////////////

    /**
     * Creates a new paragraph with the specified max width
     *
     * @param maxWidth The max width of the paragraph
     * @return The paragraph
     */
    public static Paragraph createParagraph(float maxWidth) {
        Paragraph p = new Paragraph();
        p.setMaxWidth(maxWidth);
        return p;
    }

    /**
     * Creates a new paragraph with the specified text (justified), font size and font
     *
     * @param text the text to populate the paragraph
     * @param fontSize the font size
     * @param font The font to use
     * @return The paragraph
     * @throws IOException if an I/O error is occurred
     */
    public static Paragraph createParagraph(String text, float fontSize, FontSet font) throws IOException {
        return createParagraph(text, fontSize, font.getPlainFont(), Alignment.Justify);
    }

    /**
     * Creates a new paragraph with the specified text (justified), font size and font
     *
     * @param text the text to populate the paragraph
     * @param fontSize the font size
     * @param font the font to use
     * @return The paragraph
     * @throws IOException if an I/O error is occurred
     */
    public static Paragraph createParagraph(String text, float fontSize, PDFont font) throws IOException {
        return createParagraph(text, fontSize, font, Alignment.Justify);
    }

    /**
     * Creates a new paragraph with the specified text, font size and font
     *
     * @param text the text to populate the paragraph
     * @param fontSize the font size
     * @param font the font to use
     * @param alignment The text alignment
     * @return The paragraph
     * @throws IOException if an I/O error is occurred
     */
    public static Paragraph createParagraph(String text, float fontSize, PDFont font, Alignment alignment) throws IOException {
        Paragraph paragraph = new Paragraph();
        paragraph.addText(text, fontSize, font);
        paragraph.setAlignment(alignment);
        return paragraph;
    }

    /**
     * Creates a horizontal line
     *
     * @return the horizontal line
     */
    public static HorizontalRuler createHorizontalLine() {
        return new HorizontalRuler(new Stroke(), Color.DARK_GRAY);
    }

    /**
     * Creates a paragraph with a new line
     *
     * @param fontSize the font size
     * @param fontSet The font set
     * @return The new paragraph with the new line
     * @throws IOException if an I/O error is occurred
     */
    public static Paragraph createNewLine(float fontSize, FontSet fontSet) throws IOException {
        return createParagraph("\n", fontSize, fontSet);
    }

    /**
     * Creates a new indentation with the specified text
     *
     * @param text The text
     * @param space the space of the indentation
     * @param fontSize The font size
     * @param font The font
     * @return the new Indentation
     * @throws IOException if an I/O error is occurred
     */
    public static Indent createIndentation(String text, float space, int fontSize, PDFont font) throws IOException {
        return new Indent(text, space, SpaceUnit.pt, fontSize, font);
    }

    /**
     * Creates the page format with the specified margins
     *
     * @param pageFormat The desired page format
     * @param topMargin The top margin (in millimeters)
     * @param bottomMargin The bottom margin (in millimeters)
     * @param leftMargin The left margin (in millimeters)
     * @param rightMargin The right margin (in millimeters)
     * @return The page format
     */
    private static PageFormat createPageFormat(String pageFormat, float topMargin, float bottomMargin, float leftMargin, float rightMargin) {
        PageFormatBuilder pageFormatBuilder = PageFormat.with().marginTop(millimeterToUnits(topMargin)).marginBottom(millimeterToUnits(bottomMargin)).marginLeft(millimeterToUnits(leftMargin)).marginRight(millimeterToUnits(rightMargin));
        return pageFormat.equalsIgnoreCase("letter") ? pageFormatBuilder.letter().build() : pageFormatBuilder.A4().build();
    }

    /**
     * Creates the page format with respect to the specified options
     *
     * @param options The options
     * @return The {@link PageFormat}
     */
    public static PageFormat createPageFormat(MailExportConverterOptions options) {
        //@formatter:off
        return PageFormat.with()
            .mediaBox(new PDRectangle(options.getWidth(), options.getHeight()))
            .margins(
                millimeterToUnits(options.getLeftMargin()),
                millimeterToUnits(options.getRightMargin()),
                millimeterToUnits(options.getTopMargin()),
                millimeterToUnits(options.getBottomMargin())).build();
        //@formatter:on
    }

    /**
     * Converts a length given in millimetres into point units, obeying the internally used DPI for generated PDF documents.
     *
     * @param value The value to convert in millimetres
     * @return The converted value in points
     */
    public static float millimeterToUnits(float value) {
        return MM_TO_UNITS * value;
    }

    /**
     * Converts the specified units to millimetres
     *
     * @param value The units to convert
     * @return The millimetres
     */
    public static float unitsToMillimeter(float value) {
        return value / MM_TO_UNITS;
    }

    /**
     * Converts the specified units value to pixels
     *
     * @param value The units value to convert
     * @return The converted value in pixels
     */
    public static float unitsToPixels(float value) {
        return value * UNITS_TO_PIXELS;
    }

    //////////////////////////////////////////////////////////////////////////////
    //</editor-fold>

    //<editor-fold desc="PDFBox">
    ///////////////////////////////// PDFBox ///////////////////////////////////////

    /**
     * Creates a page according to the page format with the default orientation {@link PDFPageOrientation#PORTRAIT}.
     *
     * @param pageFormat The page format
     * @return The new page with the specified page format
     */
    public static PDPage createPage(PageFormat pageFormat) {
        return new PDPage(pageFormat.getMediaBox());
    }

    /**
     * Creates a page with the specified orientation
     *
     * @param pageFormat The page format
     * @param orientation The page orientation
     * @param services The service lookup
     * @param session The session
     * @return The new page
     * @throws OXException if an error is occurred
     */
    public static PDPage createPage(PageFormat pageFormat, Orientation orientation) {
        if (Orientation.Portrait.equals(orientation)) {
            return createPage(pageFormat);
        }
        return new PDPage(new PDRectangle(pageFormat.getMediaBox().getHeight(), pageFormat.getMediaBox().getWidth()));
    }

    /**
     * Loads the colour profile for the specified document
     *
     * @param document The document
     * @return The output indent describing the colour profile of the document
     * @throws IOException if an I/O error is occurred
     */
    public static PDOutputIntent loadColourProfile(PDDocument document) throws IOException {
        try (InputStream stream = loadResource(COLOUR_PROFILE)) {
            PDOutputIntent intent = new PDOutputIntent(document, stream);
            intent.setInfo(COLOUR_PROFILE_NAME);
            intent.setOutputCondition(COLOUR_PROFILE_NAME);
            intent.setOutputConditionIdentifier(COLOUR_PROFILE_NAME);
            intent.setRegistryName(COLOUR_REGISTRY);
            return intent;
        }
    }

    /**
     * Creates a {@link PDFont} for the given document and embeds the font into it
     *
     * @param doc the document The document to embed the font into
     * @param fontStream The The steam to load the font from
     * @return The {@link PDFont}
     * @throws IOException if an I/O error is occurred
     */
    public static PDFont createFont(PDDocument doc, InputStream fontStream) throws IOException {
        return PDType0Font.load(doc, fontStream);
    }

    /**
     * Creates a {@link PDFont} for the given document and embeds the font into it
     *
     * @param doc the document The document to embed the font into
     * @param resourceFileName The name of the resource to load the font file from
     * @return The {@link PDFont}
     * @throws IOException if an I/O error is occurred
     */
    public static PDFont createFont(PDDocument doc, String resourceFileName) throws IOException {
        try (InputStream fontStream = loadResource(resourceFileName)) {
            return PDType0Font.load(doc, fontStream);
        }
    }

    /**
     * Creates a new PDF document from the specified stream
     *
     * @param stream The stream
     * @return The new document
     * @throws IOException if an I/O error is occurred
     */
    public static PDDocument createDocument(InputStream stream) throws IOException {
        return PDDocument.load(stream, getMemoryUsageSettings());
    }

    /**
     * Creates the page format for the rendered PDF document based on the requested format,
     * falling back to the default format for the session user's locale.
     *
     * @param session The session
     * @param leanConfigService The lean config service
     * @param requestedPageFormat The requested page format, or <code>null</code> to stick with the defaults
     * @return The page format
     * @throws OXException If page format cannot be determined properly
     */
    public static PageFormat createPageFormat(Session session, LeanConfigurationService leanConfigService, String requestedPageFormat) throws OXException {
        String pageFormat = requestedPageFormat;
        float topMargin = getFloatProperty(session, leanConfigService, MailExportProperty.pageMarginTop);
        float bottomMargin = getFloatProperty(session, leanConfigService, MailExportProperty.pageMarginBottom);
        float leftMargin = getFloatProperty(session, leanConfigService, MailExportProperty.pageMarginLeft);
        float rightMargin = getFloatProperty(session, leanConfigService, MailExportProperty.pageMarginRight);
        if (Strings.isEmpty(pageFormat)) {
            String country = ServerSessionAdapter.valueOf(session).getUser().getLocale().getCountry();
            pageFormat = (Locale.US.getCountry().equals(country) || Locale.CANADA.getCountry().equals(country)) ? "letter" : "A4";
        }
        return PDFFactory.createPageFormat(pageFormat, topMargin, bottomMargin, leftMargin, rightMargin);
    }

    /**
     * Creates new memory usage settings with 1GB as spool space
     *
     * @return The memory usage settings
     */
    private static MemoryUsageSetting getMemoryUsageSettings() {
        return MemoryUsageSetting.setupTempFileOnly(ONE_GB);
    }

    /**
     * Creates a new content stream for placing PDF elements in a page
     *
     * @param document the document
     * @param page the page
     * @return the new stream
     * @throws IOException if an I/O error is occurred
     */
    public static PDPageContentStream createPageContentStream(PDDocument document, PDPage page) throws IOException {
        return new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
    }

    /**
     * Creates a colour from the specified parameters
     *
     * @param r the red value (between 0 and 255 inclusive)
     * @param g the green value (between 0 and 255 inclusive)
     * @param b the blue value (between 0 and 255 inclusive)
     * @return The PDF colour
     */
    public static PDColor createColor(float r, float g, float b) {
        if (r < 0 || r > 255 || b < 0 || b > 255 || g < 0 || g > 255) {
            throw new IllegalArgumentException("Invalid rgb values: " + r + ", " + g + ", " + b + ". Only values between 0 and 255 are allowed");
        }
        float[] components = new float[] { r, g, b };
        return new PDColor(components, PDDeviceRGB.INSTANCE);
    }

    /**
     * Initializes a new embedded file on the passed {@link PDDocument} for the supplied data.
     *
     * @param document The document to create the embedded file
     * @param inputStream The data of the embedded file
     * @param contentType The content type to take over as subtype of the embedded file
     * @param modificationDate The modification date to take over
     * @return The embedded file
     * @throws IOException If embedding the data fails
     */
    public static PDEmbeddedFile createEmbeddedFile(PDDocument document, InputStream inputStream, String contentType, Calendar modificationDate) throws IOException {
        CountingInputStream countingStream = null;
        try {
            countingStream = new CountingInputStream(inputStream, -1L);
            PDEmbeddedFile embeddedFile = new PDEmbeddedFile(document, countingStream); // copies passed input stream
            embeddedFile.setSubtype(contentType);
            embeddedFile.setSize((int) countingStream.getCount());
            embeddedFile.setModDate(modificationDate);
            return embeddedFile;
        } finally {
            Streams.close(countingStream);
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    //</editor-fold>

    //<editor-fold desc="Custom/Hybrid">
    ///////////////////////////////// Custom/Hybrid ///////////////////////////////////////

    /**
     * Loads the specified resource
     *
     * @param resource The resource path
     * @return The input stream with the loaded resource
     * @throws FileNotFoundException if the resource path points to a non-existing file
     */
    public static InputStream loadResource(String resource) throws FileNotFoundException {
        InputStream resourceStream = PDFFactory.class.getClassLoader().getResourceAsStream(resource);
        if (resourceStream == null) {
            throw new FileNotFoundException("The given resource could not be loaded: %s".formatted(resource));
        }
        return resourceStream;
    }

    /**
     * Retrieves the float value of the specified property
     *
     * @param session The session
     * @param configurationService The lean configuration service
     * @param property The property
     * @return The value of the property
     */
    private static float getFloatProperty(Session session, LeanConfigurationService configurationService, MailExportProperty property) {
        return configurationService.getFloatProperty(session.getUserId(), session.getContextId(), property);
    }
    //</editor-fold>
}
