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

package com.openexchange.mail.exportpdf.pdfa.collabora.impl;

import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.Property;

/**
 * {@link CollaboraPDFAProperties}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
class CollaboraPDFAProperties {

    /**
     * Enables the collabora PDF to PDF/A converter
     */
    static final Property ENABLED = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.pdfa.collabora.enabled", Boolean.FALSE);

    /**
     * The Collabora URL to use: Allows to specify a dedicated Collabora service only for PDFA creation
     */
    static final Property URL = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.pdfa.collabora.url", null);

    /**
     * The fallback URL property to use: Take it from the regular Collabora document converter
     */
    static final Property URL2 = DefaultProperty.valueOf("com.openexchange.mail.exportpdf.collabora.url", "https://localhost:9980");

    /**
     * Initialises a new {@link CollaboraPDFAProperties}.
     */
    private CollaboraPDFAProperties() {
        super();
    }
}
