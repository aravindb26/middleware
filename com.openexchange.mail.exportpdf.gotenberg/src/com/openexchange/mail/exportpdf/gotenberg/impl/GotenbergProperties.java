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

package com.openexchange.mail.exportpdf.gotenberg.impl;

import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.Property;

/**
 * {@link GotenbergProperties}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class GotenbergProperties {

    private static final String DEFAULT_FILE_EXT = "html,htm";

    /**
     * Whether the Collabora online converter is enabled
     */
    public static final Property ENABLED = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.gotenberg.enabled", Boolean.FALSE);

    /**
     * The base URL of the Collabora Online server
     */
    public static final Property URL = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.gotenberg.url", "http://localhost:3000");

    /**
     * Specifies which PDF format to use. "PDF/A-1a", "PDF/A-2b" and "PDF/A-3b" are supported formats, or "PDF" for regular PDF
     */
    public static final Property PDF_FORMAT = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.gotenberg.pdfFormat", "PDF");

    /**
     * A comma separated list of file extensions which are actually handled by the {@link GotenbergMailExportConverter}
     */
    public static final Property FILE_EXTENSIONS = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.gotenberg.fileExtensions", DEFAULT_FILE_EXT);

    /**
     * Initialises a new {@link GotenbergProperties}.
     */
    private GotenbergProperties() {
        super();
    }
}
