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

package com.openexchange.mail.exportpdf.impl.pdf;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link PDFAttachmentTag}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public enum PDFAttachmentTag {
    APPENDED_PREVIEW,
    EMBEDDED_PREVIEW,
    EMBEDDED_RAW,
    EMBEDDED_NON_CONVERTIBLE;

    private static final Map<String, PDFAttachmentTag> tags;
    static {
        tags = new HashMap<>();
        for (PDFAttachmentTag tag : PDFAttachmentTag.values()) {
            tags.put(tag.name(), tag);
        }
    }

    /**
     * Parses the tag
     *
     * @param tag The tag to parse
     * @return The parsed {@link PDFAttachmentTag}
     * @throws IllegalArgumentException if an unknown tag is specified
     */
    public static PDFAttachmentTag parseFrom(String tag) {
        PDFAttachmentTag t = tags.get(tag);
        if (t == null) {
            throw new IllegalArgumentException("Unknown tag specified '" + tag + "'");
        }
        return t;
    }
}
