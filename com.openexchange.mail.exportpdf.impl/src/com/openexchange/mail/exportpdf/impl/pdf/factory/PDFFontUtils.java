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

import org.apache.pdfbox.pdmodel.PDDocument;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontLoader;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontLoader.FontFamily;
import com.openexchange.mail.exportpdf.impl.pdf.fonts.FontSet;

/**
 * {@link PDFFontUtils}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class PDFFontUtils {

    /**
     * Initialises a new {@link PDFFontUtils}.
     */
    private PDFFontUtils() {
        super();
    }

    /**
     * Returns the main {@link FontSet} to use and embeds it into the given document
     *
     * @param document The document to embed the font to
     * @return The main font
     * @throws OXException if an error is occurred
     */
    public static FontSet getFont(PDDocument document) throws OXException {
        return FontLoader.loadFontSet(document, FontFamily.NOTO_SANS);
    }

    /**
     * Returns the alternative {@link FontSet} to use and embeds it into the given document
     *
     * @param document The document to embed the font to
     * @return The alternative font
     * @throws OXException if an error is occurred
     */
    public static FontSet getAlternativeFont(PDDocument document) throws OXException {
        return FontLoader.loadFontSet(document, FontFamily.NOTO_SANS_MONO);
    }
}
