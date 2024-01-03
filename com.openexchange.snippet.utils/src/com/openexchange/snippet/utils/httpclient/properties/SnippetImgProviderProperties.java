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

package com.openexchange.snippet.utils.httpclient.properties;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.Property;

/**
 * {@link SnippetImgProviderProperties}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public enum SnippetImgProviderProperties implements Property {

    /**
     * Defines the maximum possible number of connections used
     */
    maxConnections(I(100), SnippetImgProviderProperties.PREFIX),
    /**
     * Defines the maximum possible number of connections per host used
     */
    maxConnectionsPerRoute(I(100), SnippetImgProviderProperties.PREFIX),
    /**
     * Defines the connection timeout
     */
    connectionTimeout(I(10000), SnippetImgProviderProperties.PREFIX),
    /**
     * Defines the timeout on waiting to read data
     */
    socketReadTimeout(I(30000), SnippetImgProviderProperties.PREFIX),
    /**
     * Defines the hard connection timeout
     */
    hardConnectTimeout(I(30000), SnippetImgProviderProperties.PREFIX),
    /**
     * Defines the hard timeout when reading data
     */
    hardReadTimeout(I(120000), SnippetImgProviderProperties.PREFIX),
    ;

    public static final String PREFIX = "com.openexchange.snippet.";

    private static final String EMPTY = "";
    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link SnippetImgProviderProperties}.
     */
    private SnippetImgProviderProperties() {
        this(EMPTY);
    }

    /**
     * Initializes a new {@link SnippetImgProviderProperties}.
     *
     * @param defaultValue The default value of the property
     */
    private SnippetImgProviderProperties(Object defaultValue) {
        this(defaultValue, PREFIX);
    }

    /**
     * Initializes a new {@link SnippetImgProviderProperties}.
     *
     * @param defaultValue The default value of the property
     * @param optional Whether the property is optional
     */
    private SnippetImgProviderProperties(Object defaultValue, String fqn) {
        this.defaultValue = defaultValue;
        this.fqn = fqn;
    }

    /**
     * Returns the fully qualified name of the property
     *
     * @return the fully qualified name of the property
     */
    @Override
    public String getFQPropertyName() {
        return fqn + name();
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
