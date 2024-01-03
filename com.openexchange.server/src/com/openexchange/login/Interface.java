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

package com.openexchange.login;

import com.openexchange.session.Origin;

/**
 * {@link Interface}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @deprecated The {@link Interface} is no longer needed and will be removed in the future
 */
@Deprecated
public enum Interface {

    HTTP_JSON,

    WEBDAV_XML,

    WEBDAV_INFOSTORE,

    WEBDAV_ICAL,

    WEBDAV_VCARD,

    OUTLOOK_UPDATER,

    DRIVE_UPDATER,

    MOBILECONFIG,

    MAC_UPDATER,

    CALDAV,

    CARDDAV;

    /**
     * Gets the session origin for specified login interface.
     *
     * @param interfaze The login interface
     * @return The session's origin or <code>null</code>
     */
    public static Origin originFor(Interface interfaze) {
        if (null == interfaze) {
            return null;
        }

        return switch (interfaze) {
            case CALDAV -> Origin.CALDAV;
            case CARDDAV -> Origin.CARDDAV;
            case DRIVE_UPDATER -> Origin.DRIVE_UPDATER;
            case HTTP_JSON -> Origin.HTTP_JSON;
            case OUTLOOK_UPDATER -> Origin.OUTLOOK_UPDATER;
            case WEBDAV_ICAL -> Origin.WEBDAV_ICAL;
            case WEBDAV_INFOSTORE -> Origin.WEBDAV_INFOSTORE;
            case WEBDAV_VCARD -> Origin.WEBDAV_VCARD;
            default -> null;
        };
    }

}
