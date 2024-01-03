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

package com.openexchange.session;

import java.util.Arrays;
import java.util.EnumSet;
import com.openexchange.java.Strings;

/**
 * {@link Origin} - An enumeration for the origin of a spawned session.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 * @deprecated The {@link Origin} is no longer needed and is going to be removed in the future
 */
@Deprecated
public enum Origin {

    /**
     * HTTP/JSON channel was used to create the session.
     */
    HTTP_JSON,
    /**
     * The WebDAV access to Drive/InfoStore was used to create the session.
     */
    WEBDAV_INFOSTORE,
    /**
     * The WebDAV iCal access was used to create the session.
     */
    WEBDAV_ICAL,
    /**
     * The WebDAV vCard access was used to create the session.
     */
    WEBDAV_VCARD,
    /**
     * The Outlook Updater access was used to create the session.
     */
    OUTLOOK_UPDATER,
    /**
     * The Drive Updater access was used to create the session.
     */
    DRIVE_UPDATER,
    /**
     * The CalDAV access was used to create the session.
     */
    CALDAV,
    /**
     * The CardDAV access was used to create the session.
     */
    CARDDAV,
    /**
     * A synthetic session generated by the application.
     */
    SYNTHETIC;

    /**
     * Gets the origin for specified identifier
     *
     * @param origin The identifier
     * @return The origin or <code>null</code>
     */
    public static Origin originFor(String origin) {
        if (Strings.isEmpty(origin)) {
            return null;
        }

        String lookUp = Strings.toUpperCase(origin);
        for (Origin o : Origin.values()) {
            if (lookUp.equals(o.name())) {
                return o;
            }
        }
        return null;
    }

    /**
     * Checks if specified session is allowed to act in context of specified origins.
     *
     * @param session The session
     * @param origins The allowed origins
     * @return <code>true</code> if allowed; otherwise <code>false</code>
     */
    public static boolean isAllowed(Session session, Origin... origins ) {
        return (null == session || null == session.getOrigin() || null == origins || 0 == origins.length) || EnumSet.copyOf(Arrays.asList(origins)).contains(session.getOrigin());
    }

}
