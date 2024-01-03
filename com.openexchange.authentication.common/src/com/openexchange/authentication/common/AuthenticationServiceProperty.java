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

package com.openexchange.authentication.common;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.config.lean.Property;


/**
 * {@link AuthenticationServiceProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public enum AuthenticationServiceProperty implements Property {

    /**
     * A ranking for the {@link AuthenticationService}, defaults to '0'
     */
    ranking(I(0)),

    /**
     * Optional comma-separated list of host names this service is responsible for. Default: blank
     */
    hostnames(),

    /**
     * Optional comma-separated list of headers which requests must contain to use this authentication service. If header depends on value,
     * use notation 'headerName=headerValue'.
     */
    customHeaders(),

    /**
     * Optional comma-separated list of cookies which requests must contain to use this authentication service. If cookie depends on value,
     * use notation 'cookieName=cookieValue'.
     */
    customCookies(),

    ;

    public final static String PREFIX = "com.openexchange.authentication.[provider].";
    public final static String OPTIONAL_FIELD = "provider";

    private final Object defaultValue;

    /**
     * Initializes a new {@link AuthenticationServiceProperty} with no default value
     */
    private AuthenticationServiceProperty() {
        this.defaultValue = null;
    }

    /**
     * Initializes a new {@link AuthenticationServiceProperty} with the given default value.
     *
     * @param defaultValue The default value
     */
    private AuthenticationServiceProperty(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
