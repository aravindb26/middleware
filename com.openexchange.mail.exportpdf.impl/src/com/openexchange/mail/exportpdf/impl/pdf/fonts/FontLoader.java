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

package com.openexchange.mail.exportpdf.impl.pdf.fonts;

import java.io.IOException;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.impl.pdf.factory.PDFFactory;

/**
 * {@link FontLoader} - Finds, loads and embeds fonts
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class FontLoader {

    /**
     * NOTO_SANS_MONO_REGULAR_TTF
     */
    private static final String NOTO_SANS_MONO_REGULAR_TTF = "NotoSansMono-Regular.ttf";

    /**
     * {@link FontFamily} The font family
     *
     */
    public enum FontFamily {
        NOTO_SANS,
        NOTO_SANS_MONO
    }

    //@formatter:off
    private static record FontRessource(String regular, String bold, String italic, String boldItalic) {}
    private static final Map<FontFamily, FontRessource> EMBEDDED_FONT_MAP = Map.of(
        FontFamily.NOTO_SANS, new FontRessource("NotoSans-Regular.ttf", "NotoSans-Bold.ttf", "NotoSans-Italic.ttf", "NotoSans-BoldItalic.ttf"),
        FontFamily.NOTO_SANS_MONO, new FontRessource(NOTO_SANS_MONO_REGULAR_TTF, "NotoSansMono-Bold.ttf", NOTO_SANS_MONO_REGULAR_TTF, NOTO_SANS_MONO_REGULAR_TTF)
    );
    //@formatter:on

    /**
     * Loads a {@link FontSet} by it's family name and embeds it into the given document
     *
     * @param document The {@link PDDocument} to embed the fonts into
     * @param fontFamily The font family to load and embed
     * @return The loaded and embedded fonts as {@link FontSet}
     * @throws OXException if no {@link FontSet} could be loaded by the given font family
     */
    public static FontSet loadFontSet(PDDocument document, FontFamily fontFamily) throws OXException {
        FontRessource font = EMBEDDED_FONT_MAP.get(fontFamily);
        if (font != null) {
            try {
                //@formatter:off
                return new FontSet(font.regular() != null ? PDFFactory.createFont(document, font.regular()) : null,
                                   font.bold() != null ? PDFFactory.createFont(document, font.bold()) : null,
                                   font.italic() != null ? PDFFactory.createFont(document, font.italic()) : null,
                                   font.boldItalic() != null ? PDFFactory.createFont(document, font.boldItalic()) : null);
                //@formatter:on
            } catch (IOException e) {
                throw MailExportExceptionCode.FONT_LOADING_ERROR.create(e, fontFamily, e.getMessage());
            }
        }
        throw MailExportExceptionCode.FONT_LOADING_ERROR.create(fontFamily, "Unknown font family.");
    }
}
