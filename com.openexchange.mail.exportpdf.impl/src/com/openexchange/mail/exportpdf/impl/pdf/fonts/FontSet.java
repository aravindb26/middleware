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

import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * {@link FontSet} - Represents a set of fonts (regular, <b>bold</b>, <i>italic</i> and <b><i>bold-italic</i></b>)
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class FontSet {

    private final PDFont regular;
    private final PDFont bold;
    private final PDFont italic;
    private final PDFont boldItalic;

    /**
     * Initializes a new {@link FontSet}.
     *
     * @param regular The regular font
     * @param bold The bold font
     * @param italic The italic font
     * @param boldItalic The bold-italic font
     */
    public FontSet(PDFont regular, PDFont bold, PDFont italic, PDFont boldItalic) {
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
    }

    /**
     * Gets the regular/plain font
     *
     * @return The regular/plain font
     */
    public PDFont getPlainFont() {
        return regular;
    }

    /**
     * Gets the <b>bold</b> font
     *
     * @return The <b>bold</b> font
     */
    public PDFont getBoldFont() {
        return bold;
    }

    /**
     * Gets the <i>italic</i> font
     *
     * @return The <i>italic</i> font
     */
    public PDFont getItalicFont() {
        return italic;
    }

    /**
     * Gets the <b><i>boldItalic</i></b> font
     *
     * @return The <b><i>boldItalic</i></b> font
     */
    public PDFont getBoldItalicFont() {
        return boldItalic;
    }
}
