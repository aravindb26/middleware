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

package com.openexchange.drive.client.windows.rest;

/**
 * {@link Constants} provides constants for the windows drive updater
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public final class Constants {

    private Constants() {}

    /**
     * The address of the update XML servlet.
     */
    public static final String UPDATE_XML_SERVLET = "drive/client/windows/v1/update.xml";

    /**
     * The address of the install servlet.
     */
    public static final String INSTALL_SERVLET = "drive/client/windows/install";
    
    /**
     * The URL branding placeholder
     */
    public static final String URL_PLACEHOLDER_BRANDING = "[branding]";

    /**
     * The URL service hostname placeholder
     */
    public static final String URL_PACEHOLDER_SVC_HOST = "[svc_host]";

    /**
     * The URL service manifest path  placeholder
     */
    public static final String URL_PACEHOLDER_SVC_MANFEST = "[svc_manifest_path]";

}
