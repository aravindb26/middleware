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

package com.openexchange.mail.exportpdf.collabora.impl;

import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.Property;

/**
 * {@link CollaboraProperties}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraProperties {

    private static final String DEFAULT_FILE_EXT = """
        sxw,
        odt,
        fodt,
        sxc,
        ods,
        fods,
        sxi,
        odp,
        fodp,
        sxd,
        odg,
        fodg,
        odc,
        sxg,
        odm,
        stw,
        ott,
        otm,
        stc,
        ots,
        sti,
        otp
        std,
        otg,
        odb,
        oxt,
        doc,
        dot
        xls,
        ppt,
        docx,
        docm,
        dotx,
        dotm,
        xltx,
        xltm,
        xlsx,
        xlsb,
        xlsm,
        pptx,
        pptm,
        potx,
        potm,
        wpd,
        pdb,
        hwp,
        wps,
        wri,
        wk1,
        cgm,
        dxf,
        emf,
        wmf,
        cdr,
        vsd,
        pub,
        vss,
        lrf,
        gnumeric,
        mw,
        numbers,
        p65,
        pdf,
        jpg,
        jpeg,
        gif,
        png,
        dif,
        slk,
        csv,
        dbf,
        oth,
        rtf,
        txt,
        html,
        htm,
        xml
        """;

    /**
     * Whether or not the collabora online converter is enabled
     */
    public static final Property ENABLED = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.collabora.enabled", Boolean.FALSE);

    /**
     * The base URL of the Collabora Online server
     */
    public static final Property URL = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.collabora.url", "https://localhost:9980");

    /**
     * A comma separated list of file extensions which are actually handled by the {@link CollaboraMailExportConverter}
     */
    public static final Property FILE_EXTENSIONS = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.collabora.fileExtensions", DEFAULT_FILE_EXT);

    /**
     * The mode defines how to handle/replace inline images
     */
    public static final Property IMAGE_REPLACEMENT_MODE = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.collabora.imageReplacementMode", "distributedFile");

    /**
     * Initialises a new {@link CollaboraProperties}.
     */
    private CollaboraProperties() {
        super();
    }
}
