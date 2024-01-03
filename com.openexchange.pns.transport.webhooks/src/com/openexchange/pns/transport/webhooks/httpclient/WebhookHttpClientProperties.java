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

package com.openexchange.pns.transport.webhooks.httpclient;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.Property;

/**
 * {@link WebhookHttpClientProperties} - The HttpClient properties for Webhooks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum WebhookHttpClientProperties implements Property {

    /**
     * Defines the maximum possible number of connections used
     */
    MAX_CONNECTIONS("maxConnections", I(100)),
    /**
     * Defines the maximum possible number of connections per host used
     */
    MAX_CONNECTIONS_PER_ROUTE("maxConnectionsPerHost", I(100)),
    /**
     * Defines the connection timeout
     */
    CONNECT_TIMEOUT("connectionTimeout", I(2500)),
    /**
     * Defines the timeout on waiting to read data
     */
    SOCKET_READ_TIMEOUT("socketReadTimeout", I(2500)),
    /**
     * Defines the hard connection timeout
     */
    HARD_CONNECT_TIMEOUT("hardConnectTimeout", I(30000)),
    /**
     * Defines the hard timeout when reading data
     */
    HARD_READ_TIMEOUT("hardReadTimeout", I(120000)),
    ;

    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link WebhookHttpClientProperties}.
     *
     * @param appendix The FQN appendix
     * @param defaultValue The default value of the property
     */
    private WebhookHttpClientProperties(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.pns.transport.webhooks.http." + appendix;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the fully qualified name of the property
     *
     * @return the fully qualified name of the property
     */
    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    /**
     * Returns the default value of this property
     *
     * @return the default value of this property
     */
    @Override
    public <T extends Object> T getDefaultValue(Class<T> cls) {
        if (defaultValue.getClass().isAssignableFrom(cls)) {
            return cls.cast(defaultValue);
        }
        throw new IllegalArgumentException("The object cannot be converted to the specified type '" + cls.getCanonicalName() + "'");
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
