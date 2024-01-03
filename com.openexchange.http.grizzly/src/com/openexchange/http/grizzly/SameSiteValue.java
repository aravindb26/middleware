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

package com.openexchange.http.grizzly;

import com.openexchange.java.Strings;

/**
 * {@link SameSiteValue} - Possible values for the <code>"SameSite"</code> cookie attribute;
 * see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">here</a>.
 * <pre>
 * Cookie: mycookie=myvalue; HttpOnly; SameSite=strict
 * </pre>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public enum SameSiteValue {

    /**
     * "None" value for <code>"SameSite"</code> cookie attribute.
     * <p>
     * Cookies will be sent in all contexts, i.e. in responses to both first-party and cross-site requests.
     * If SameSite=None is set, the cookie Secure attribute must also be set (or the cookie will be blocked).
     */
    NONE("None"),
    /**
     * <code>"Lax"</code> value (default) for <code>"SameSite"</code> cookie attribute.
     * <p>
     * Cookies are not sent on normal cross-site sub-requests (for example to load images or frames into a third party site),
     * but are sent when a user is navigating to the origin site (i.e., when following a link).
     * <p>
     * This is the default cookie value if SameSite has not been explicitly specified in recent browser versions
     * (see the "SameSite: Defaults to Lax" feature in the Browser Compatibility).
     */
    LAX("Lax"),
    /**
     * <code>"Strict"</code> value for <code>"SameSite"</code> cookie attribute.
     * <p>
     * Cookies will only be sent in a first-party context and not be sent along with requests initiated by third party web sites.
     */
    STRICT("Strict");

    private final String value;

    private SameSiteValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value.
     *
     * @return The value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the <code>"SameSite"</code> value for given identifier.
     *
     * @param id The identifier to look-up
     * @return The <code>"SameSite"</code> value or <code>null</code>
     */
    public static SameSiteValue sameSiteValueFor(String id) {
        if (Strings.isEmpty(id)) {
            return null;
        }

        String lookUp = id.trim();
        for (SameSiteValue ssv : SameSiteValue.values()) {
            if (lookUp.equalsIgnoreCase(ssv.value)) {
                return ssv;
            }
        }
        return null;
    }

}
