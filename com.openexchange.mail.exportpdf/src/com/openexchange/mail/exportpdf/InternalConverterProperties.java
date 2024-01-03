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

package com.openexchange.mail.exportpdf;

import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.Property;

/**
 * {@link InternalConverterProperties}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class InternalConverterProperties {

    private static final String DEFAULT_TEXT_FILE_EXT = "txt,xml,md,js,java,py";

    /**
     * A comma separated list of text file extensions which are actually handled by the {@link InternalConverterProperties}
     */
    public static final Property TEXT_FILE_EXTENSIONS = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.internal-converter.text.fileExtensions", DEFAULT_TEXT_FILE_EXT);

    private static final String DEFAULT_IMAGE_FILE_EXT = "jpg,jpeg,png,gif";

    /**
     * A comma separated list of image file extensions which are actually handled by the {@link InternalConverterProperties}
     */
    public static final Property IMAGE_FILE_EXTENSIONS = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.internal-converter.image.fileExtensions", DEFAULT_IMAGE_FILE_EXT);

    private static final String DEFAULT_PDF_FILE_EXT = "pdf";

    /**
     * A comma separated list of PDF file extensions which are actually handled by the {@link InternalConverterProperties}
     */
    public static final Property PDF_FILE_EXTENSIONS = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.internal-converter.pdf.fileExtensions", DEFAULT_PDF_FILE_EXT);

    /**
     * Initialises a new {@link InternalConverterProperties}.
     */
    private InternalConverterProperties() {
        super();
    }
}
