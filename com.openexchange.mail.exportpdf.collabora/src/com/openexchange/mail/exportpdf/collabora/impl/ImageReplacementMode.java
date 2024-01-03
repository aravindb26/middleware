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

/**
 * {@link ImageReplacementMode} - The mode defines how inline images are replaced in HTML content body before/during conversion
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public enum ImageReplacementMode {

    /**
     * Inline images will be replaced with their BASE64 payload
     */
    BASE64("base64"),

    /**
     * Inline images will be replaced with a distributed file-management URL pointing to the image data
     */
    DISTRIBUTED_FILE("distributedFile")

    ;

    private final String name;

    /**
     * Initializes a new {@link ImageReplacementMode}.
     *
     * @param name The name of the mode
     */
    private ImageReplacementMode(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the mode
     *
     * @return The name of the mode
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the {@link ImageReplacementMode} for the specified string
     *
     * @param s The string to get the {@link ImageReplacementMode} for
     * @return The {@link ImageReplacementMode} for the specified string, or {@link ImageReplacementMode#DISTRIBUTED_FILE} as fallback/default
     */
    public static ImageReplacementMode parse(String s) {
        ImageReplacementMode defaultMode = ImageReplacementMode.DISTRIBUTED_FILE;
        try {
            for (var mode : ImageReplacementMode.values()) {
                if (mode.getName().equalsIgnoreCase(s)) {
                    return mode;
                }
            }
            return defaultMode;
        } catch (@SuppressWarnings("unused") Exception e) {
            return defaultMode;
        }
    }
}
