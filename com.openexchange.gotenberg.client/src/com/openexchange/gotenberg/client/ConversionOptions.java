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

import java.util.Optional;

/**
 * {@link ConversionOptions}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class ConversionOptions {

    /**
     * {@link ConversionOptionsBuilder} - Builds the {@link ConversionOptions}
     */
    public static class ConversionOptionsBuilder {

        private PDFFormat pdfFormat;
        private String outputFileName;
        private PageProperties pageProperties;

        /**
         * Sets {@link PDFFormat}
         *
         * @param pdfFormat The {@link PDFFormat}
         * @return this
         */
        public ConversionOptionsBuilder withPDFFormat(PDFFormat pdfFormat) {
            this.pdfFormat = pdfFormat;
            return this;
        }

        /**
         * Sets the name the converted document should have
         *
         * @param outputFileName The name to use for the converted document
         * @return this
         */
        public ConversionOptionsBuilder withOutputFilename(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        /**
         * Sets the page properties
         *
         * @param pageProperties The page properties to set
         * @return this
         */
        public ConversionOptionsBuilder withPageProperties (PageProperties pageProperties) {
            this.pageProperties = pageProperties;
            return this;
        }

        /**
         * Builds the {@link ConversionOptions}
         *
         * @return The new {@link ConversionOptions}
         */
        public ConversionOptions build() {
            return new ConversionOptions(pdfFormat, Optional.ofNullable(outputFileName), Optional.ofNullable(pageProperties));
        }
    }

    //--------------------------------------------------------------------

    private final PDFFormat pdfFormat;
    private final Optional<String> outputFileName;
    private final Optional<PageProperties> pageProperties;

    private ConversionOptions(PDFFormat pdfFormat, Optional<String> outputFileName, Optional<PageProperties> pageProperties) {
        this.pdfFormat = pdfFormat;
        this.outputFileName = outputFileName;
        this.pageProperties = pageProperties;
    }

    //--------------------------------------------------------------------

    /**
     * Gets the pdfFormat
     *
     * @return The pdfFormat
     */
    public PDFFormat getPdfFormat() {
        return pdfFormat;
    }

    /**
     * Gets the optional {@link PageProperties}
     *
     * @return The optional {@link PageProperties}
     */
    public Optional<PageProperties> getPageProperties() {
        return pageProperties;
    }

    /**
     * Gets the optional name the output file should get
     *
     * @return The optional name, the output file should get
     */
    public Optional<String> getOutputFilename() {
        return this.outputFileName;
    }
}
