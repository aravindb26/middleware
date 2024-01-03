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

package com.openexchange.dav;

/**
 * {@link DAVUserAgent}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public enum DAVUserAgent {

    /** The calendar application on MacOS */
    MAC_CALENDAR("Mac OS Calendar (CalDAV)"),
    /** The contacts application on MacOS */
    MAC_CONTACTS("Mac OS Addressbook (CardDAV)"),
    /** An iOS device */
    IOS("iOS Addressbook and Calendar (CalDAV/CardDAV)"),
    /** The reminder app on iOS */
    IOS_REMINDERS("iOS Reminders (CalDAV)"),
    /** Thunderbird lighting plugin */
    THUNDERBIRD_LIGHTNING("Mozilla Thunderbird / Lightning (CalDAV)"),
    /** The Thunderbird contacts plugin */
    THUNDERBIRD_CARDBOOK("Thunderbird CardBook (CardDAV)"),
    /** The eMClient */
    EM_CLIENT("eM Client (CalDAV/CardDAV)"),
    /** The OX Sync App for Android */
    OX_SYNC("OX Sync on Android (CalDAV/CardDAV)"),
    /** CalDAV Sync on Android */
    CALDAV_SYNC("CalDAV-Sync on Android (CalDAV)"),
    /** CardDAV Sync on Android */
    CARDDAV_SYNC("CardDAV-Sync on Android (CardDAV)"),
    /** Smooth Sync App on Android */
    SMOOTH_SYNC("SmoothSync on Android (CalDAV/CardDAV)"),
    /** DAVdroid App on Android */
    DAVDROID("DAVdroid on Android (CalDAV/CardDAV)"),
    /** DAVx5 App on Android */
    DAVX5("DAVx\u2075 on Android (CalDAV/CardDAV)"),
    /** The Outlook Synchronizer */
    OUTLOOK_CALDAV_SYNCHRONIZER("Outlook CalDav Synchronizer (CalDAV/CardDAV)"),
    /** An Windows Phone device */
    WINDOWS_PHONE("Windows Phone Contacts and Calendar (CalDAV/CardDAV)"),
    /** An Windows device */
    WINDOWS("Windows Contacts and Calendar (CalDAV/CardDAV)"),
    /** Representation of an generic CalDAV client. *NOT* for parsing */
    GENERIC_CALDAV("Sync Client (CalDAV)"),
    /** Representation of an generic CalDAV client. *NOT* for parsing */
    GENERIC_CARDDAV("Sync Client (CardDAV)"),
    /** Representation of an unknown client */
    UNKNOWN("CalDAV/CardDAV");

    private final String readableName;

    private DAVUserAgent(String readableName) {
        this.readableName = readableName;
    }

    /**
     * Gets a readable name for the user agent.
     *
     * @return The readable name
     */
    public String getReadableName() {
        return readableName;
    }

}
