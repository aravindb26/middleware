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

package com.openexchange.contact.provider.ldap.config;

import static com.openexchange.contact.provider.ldap.config.ConfigUtils.opt;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optBoolean;
import java.util.Map;
import java.util.Optional;

/**
 * {@link CacheConfig}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class CacheConfig {

    /**
     * Initializes a new {@link CacheConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The cache configuration section to parse
     * @return The parsed configuration or <code>null</code> in case the config entry is null
     */
    public static CacheConfig init(Map<String, Object> configEntry) {
        if (configEntry == null) {
            return null;
        }
        return new CacheConfig.Builder() // @formatter:off
            .useCache(optBoolean(configEntry, "useCache", false))
            .cachedFields(opt(configEntry, "cachedFields"))
        .build(); // @formatter:on
    }

    private static class Builder {

        boolean useCache;
        Optional<String> cachedFields;

        Builder() {
            super();
        }

        Builder useCache(boolean value) {
            this.useCache = value;
            return this;
        }

        Builder cachedFields(String value) {
            this.cachedFields = Optional.ofNullable(value);
            return this;
        }

        CacheConfig build() {
            return new CacheConfig(this);
        }
    }

    private final Optional<String> cachedFields;
    private final boolean useCache;

    /**
     * Initializes a new {@link CacheConfig}.
     *
     * @param builder The builder to use for initialization
     */
    CacheConfig(CacheConfig.Builder builder) {
        super();
        this.useCache = builder.useCache;
        this.cachedFields = builder.cachedFields;
    }

    /**
     * Whether the cache should be used or not
     *
     * @return <code>true</code> if the cache should be used, <code>false</code> otherwise
     */
    public boolean useCache() {
        return useCache;
    }

    /**
     * The optional cached fields
     *
     * @return A comma separated list of contact fields
     */
    public Optional<String> optCachedFields() {
        return cachedFields;
    }

}
