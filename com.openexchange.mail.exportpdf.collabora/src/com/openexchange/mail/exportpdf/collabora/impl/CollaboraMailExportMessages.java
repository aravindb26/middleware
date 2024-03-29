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

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link CollaboraMailExportMessages}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraMailExportMessages implements LocalizableStrings {

    /**
     * The content-type "%1$s" is not supported"
     */
    public static final String UNSUPPORTED_CONTENT_TYPE = "The content-type \"%1$s\" is not supported";

    /**
     * The service is disabled
     */
    public static final String SERVICE_DISABLED = "The service is disabled";

    /**
     * Initialises a new {@link CollaboraMailExportMessages}.
     */
    private CollaboraMailExportMessages() {
        super();
    }
}
