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

package com.openexchange.mail.exportpdf.converters.images;

import java.util.regex.Pattern;
import com.openexchange.java.Strings;

/**
 * {@link InlineImageUtils} - Utilities for inline images handling
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class InlineImageUtils {

    /**
     * The REGEX {@link Pattern} for matching an inline image source with its content id (CID)
     */
    public static final Pattern CID_REGEX = Pattern.compile("src=\"CID:(.*?)\"", Pattern.CASE_INSENSITIVE);

    /**
     * Initialises a new {@link InlineImageUtils}.
     */
    private InlineImageUtils() {
        super();
    }

    /**
     * Removes leading "<"- and trailing ">"-characters from the given CID if present
     *
     * @param contentId The CID
     * @return The CID without leading "<"- and trailing ">"-characters
     */
    public static String normalizeCID(String contentId) {
        String cid = contentId;
        if (Strings.isEmpty(cid)) {
            return cid;
        }

        if (cid.startsWith("<")) {
            cid = cid.substring(1);
        }

        if (cid.endsWith(">")) {
            cid = cid.substring(0, cid.length() - 1);
        }

        return cid;
    }
}
