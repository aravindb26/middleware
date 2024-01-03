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

package com.openexchange.mail.exportpdf.converter;

import static com.openexchange.java.Autoboxing.I;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link PDFAVersion} - Enumerates PDF/A versions
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public enum PDFAVersion {

    UNKNOWN(null, null, null),

    PDFA_1_B("1.4", I(1), "B"),
    PDFA_1_A("1.4", I(1), "A"),

    PDFA_2_B("1.7", I(2), "B"),
    PDFA_2_A("1.7", I(2), "A"),
    PDFA_2_U("1.7", I(2), "U"),

    PDFA_3_B("1.7", I(3), "B"),
    PDFA_3_A("1.7", I(3), "A"),
    PDFA_3_U("1.7", I(3), "U"),

    PDFA_4_F("2.0", I(4), "F"),
    PDFA_4_E("2.0", I(4), "E"),

    ;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * The PDF version string
     */
    private String version;

    /*
     * The PDF/A version identifier (e.g 1,2,3 or 4)
     */
    private Integer part;
    /**
     * The PDF/A conformance level (A,B,U,F, or E)
     */
    private String conformanceLevel;

    /**
     * Initializes a new {@link PDFAVersion}.
     *
     * @param version The PDF version identifier
     * @param part The PDF/A version indetifier
     * @param conformanceLevel The PDF/A conformance level
     */
    private PDFAVersion(String version, Integer part, String conformanceLevel) {
        this.version = version;
        this.part = part;
        this.conformanceLevel = conformanceLevel;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the version string of the {@link PDFAVersion}
     *
     * @return The version string of the {@link PDFAVersion}
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the PDF/A version identifier
     *
     * @return The PDF/A version identifier if present, or <code>null</code> otherwise
     */
    public Integer getPDFAPart() {
        return part;
    }

    /**
     * Gets the PDF/A conformance level
     *
     * @return The PDF/A conformance level if present, or <code>null</code> otherwise
     */
    public String getConformanceLevel() {
        return conformanceLevel;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses a {@link PDFAVersion} by it' PDF/A version string, and conformance level
     *
     * @param part The PDF/A version
     * @param conformanceLevel The PDF/A conformance level
     * @return The {@link PDFAVersion} based on the given information, or {@link PDFAVersion#UNKNOWN}
     *         if the information do not match a known version
     */
    public static PDFAVersion parse(Integer part, String conformanceLevel) {
        //@formatter:off
        Optional<PDFAVersion> parsed = Stream.of(values()).filter(v -> v.part.equals(part) &&
                                                                      v.conformanceLevel.equals(conformanceLevel)).findFirst();
        return parsed.orElse(UNKNOWN);
        //@formatter:on
    }
}
