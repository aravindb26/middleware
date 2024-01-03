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

package com.openexchange.collabora.online.client.conversion;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * {@link ConversionFormat} - Defines supported target formats for document conversion via "Collabora online"
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public enum ConversionFormat {

    /**
     * Plain text format
     */
    TXT("txt", "text/plain"),

    /**
     * PNG image format
     */
    PNG("png", "image/png"),

    /**
     * PDF 1.5 format
     */
    PDF_1_5("pdf", "application/pdf", new ConversionFormatParameter("PDFVer", "PDF-1.5")),

    /**
     * PDF 1.6 format
     */
    PDF_1_6("pdf", "application/pdf", new ConversionFormatParameter("PDFVer", "PDF-1.6")),

    /**
     * PDF-A-1b format
     */
    PDF_A_1_B("pdf", "application/pdf", new ConversionFormatParameter("PDFVer", "PDF/A-1b")),

    /**
     * PDF-A-2b format
     */
    PDF_A_2_B("pdf", "application/pdf", new ConversionFormatParameter("PDFVer", "PDF/A-2b")),

    /**
     * PDF-A-3b format
     */
    PDF_A_3_B("pdf", "application/pdf", new ConversionFormatParameter("PDFVer", "PDF/A-3b"))

    ;

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * {@link ConversionFormatParameter} Defines a sub parameter of a {@link ConversionFormat}
     *
     * <p>
     * Each {@link ConversionFormat} can have multiple sub-parameters.
     * <br>
     * They can be used, for example, to define sub-formats (like PDF/A-2b), or other format specific settings in the future
     * </p>
     */
    public record ConversionFormatParameter(String name, String value) {}

    //-----------------------------------------------------------------------------------------------------------------

    private final String name;
    private final String contentType;
    private final List<ConversionFormatParameter> parameters;

    /**
     * Initializes a new {@link ConversionFormat}.
     *
     * @param name The name of the format
     * @param contentType The content-type of the format
     * @param parameters the sub-parameters, or
     */
    ConversionFormat(String name, String contentType, ConversionFormatParameter... parameters) {
        this.name = Objects.requireNonNull(name, "name must not ne null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        this.parameters = Arrays.asList(parameters);
    }

    /**
     * Gets the name of the {@link ConversionFormat}
     *
     * @return The name of the {@link ConversionFormat}
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the content type of the {@link ConversionFormat}
     *
     * @return The content type of the {@link ConversionFormat}
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets a list of {@link ConversionFormatParameter}s related to this {@link ConversionFormat}
     *
     * @return A list of {@link ConversionFormatParameter}s for this {@link ConversionFormat}
     */
    public List<ConversionFormatParameter> getParameters() {
        return parameters;
    }
}
