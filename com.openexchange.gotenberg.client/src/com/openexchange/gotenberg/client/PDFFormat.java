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

package com.openexchange.gotenberg.client;

/**
 * {@link PDFFormat} - Defines the PDF format to convert a HTML document to
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public enum PDFFormat {

    /**
     * Plain PDF, non PDF/A format
     */
    PDF("PDF"),

    /**
     * PDF-A-1a format
     */
    PDF_A_1_A("PDF/A-1a"),

    /**
     * PDF-A-1b format
     */
    PDF_A_1_B("PDF/A-1b"),

    /**
     * PDF-A-2b format
     */
    PDF_A_2_B("PDF/A-2b"),

    /**
     * PDF-A-3b format
     */
    PDF_A_3_B("PDF/A-3b");

    //------------------------------------

    private final String name;

    /**
     * Initializes a new {@link PDFFormat}.
     *
     * @param name The name of the format
     */
    PDFFormat(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the format
     *
     * @return The name of the format
     */
    public String getName() {
        return this.name;
    }

    //------------------------------------

    /**
     * Parses the {@link PDFFormat} from the given string
     *
     * @param pdfFormat The string to parse the {@link PDFFormat} from
     * @return The parsed {@link PDFFormat}
     * @throws IllegalArgumentException if the given format is unknown
     */
    public static PDFFormat createFrom(String pdfFormat) throws IllegalArgumentException {
        for (var format : PDFFormat.values()) {
            if (format.getName().equalsIgnoreCase(pdfFormat)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown pdfFormat: " + pdfFormat);
    }
}
