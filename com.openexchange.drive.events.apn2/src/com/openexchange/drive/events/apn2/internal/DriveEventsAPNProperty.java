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

package com.openexchange.drive.events.apn2.internal;

import com.openexchange.config.lean.Property;


/**
 * {@link DriveEventsAPNProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.2
 */
public enum DriveEventsAPNProperty implements Property {

    /**
     * Enables or disables push event notifications to iOS fileprovider clients using the HTTP/2 based version of the Apple Push
     * Notification service (APNS HTTP/2). This requires a valid configuration for the APNS certificate and keys.
     * <p/>
     * Default: false
     */
    IOS_FILEPROVIDER_ENABLED("apn2.ios.enabled", Boolean.FALSE),

    /**
     * Specifies the identifier of the push client configuration to use for push event notifications to iOS fileprovider clients.
     * <p/>
     * Default: com.openexchange.mobile.drive.pushkit.fileprovider
     */
    IOS_FILEPROVIDER_CLIENTID("apn2.ios.clientId", "drive-mobile-ios"),

    /**
     * Enables or disables push event notifications to iOS legacy clients using the HTTP/2 based version of the Apple Push
     * Notification service (APNS HTTP/2). This requires a valid configuration for the APNS certificate and keys.
     * <p/>
     * Default: false
     */
    IOS_LEGACY_ENABLED("apn.ios.enabled", Boolean.FALSE),

    /**
     * Specifies the identifier of the push client configuration to use for push event notifications to iOS legacy clients.
     * <p/>
     * Default: com.openexchange.mobile.drive.pushkit.fileprovider
     */
    IOS_LEGACY_CLIENTID("apn.ios.clientId", "drive-mobile-ios-old"),

    /**
     * Enables or disables push event notifications to macOS fileprovider clients using the HTTP/2 based version of the Apple Push
     * Notification service (APNS HTTP/2). This requires a valid configuration for the APNS certificate and keys.
     * <p/>
     * Default: false
     */
    MACOS_FILEPROVIDER_ENABLED("apn2.macos.enabled", Boolean.FALSE),

    /**
     * Specifies the identifier of the push client configuration to use for push event notifications to macOS fileprovider clients.
     * <p/>
     * Default: drive-desktop-macos
     */
    MACOS_FILEPROVIDER_CLIENTID("apn2.macos.clientId", "drive-desktop-macos"),
    ;

    private static final String PREFIX = "com.openexchange.drive.events.";

    private final String suffix;
    private final Object defaultValue;

    private DriveEventsAPNProperty(String suffix, Object defaultValue) {
        this.suffix = suffix;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + suffix;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
