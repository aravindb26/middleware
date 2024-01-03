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

package com.openexchange.sessiond.redis.util;

import com.openexchange.java.Strings;
import com.openexchange.session.Session;
import net.gcardone.junidecode.Junidecode;

/**
 * {@link BrandNames} - Utility class for brand names.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.12.0
 */
public final class BrandNames {

    /**
     * Initializes a new {@link BrandNames}.
     */
    private BrandNames() {
        super();
    }

    /**
     * Gets the identifier representation for given brand name.
     *
     * @param session The session to acquire the brand identifier from
     * @return The identifier representation or <code>null</code> if session carries no {@link Session#PARAM_BRAND brand name} in its parameters
     */
    public static String getBrandIdentifierFrom(Session session) {
        return getBrandIdentifierFor((String) session.getParameter(Session.PARAM_BRAND));
    }

    /**
     * Gets the brand identifier for given brand name.
     *
     * @param brandName The brand name
     * @return The brand identifier or <code>null</code> if given brand name was <code>null</code>
     */
    public static String getBrandIdentifierFor(String brandName) {
        if (brandName == null) {
            return null;
        }
        return Strings.asciiLowerCase(Junidecode.unidecode(Strings.replaceWhitespacesWith(brandName, "_")));
    }

}
