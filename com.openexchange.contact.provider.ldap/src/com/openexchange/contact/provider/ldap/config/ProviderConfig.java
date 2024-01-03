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

import static com.openexchange.contact.provider.ldap.config.ConfigUtils.get;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getMap;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optBoolean;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optInt;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optMap;
import java.util.Map;
import java.util.Optional;
import com.openexchange.contact.provider.ldap.mapping.LdapMapper;
import com.openexchange.exception.OXException;

/**
 * {@link ProviderConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ProviderConfig {

    /**
     * Initializes a new {@link ProviderConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @param mappings The configured contact field to LDAP attribute mappings
     * @return The parsed configuration
     */
    public static ProviderConfig init(Map<String, Object> configEntry, Map<String, Object> mappings) throws OXException {
        return new ProviderConfig.Builder() // @formatter:off
            .name(get(configEntry, "name"))
            .maxPageSize(optInt(configEntry, "maxPageSize", 500))
            .cacheConfig(CacheConfig.init(optMap(configEntry, "cache")))
            .isDeletedSupport(optBoolean(configEntry, "isDeletedSupport", false))
            .ldapClientId(get(configEntry, "ldapClientId"))
            .mapper(LdapMapper.init(getMap(mappings, get(configEntry, "mappings"))))
            .foldersConfig(FoldersConfig.init(getMap(configEntry, "folders")))
        .build(); // @formatter:on
    }

    private static class Builder {

        String name;
        int maxPageSize;
        boolean isDeletedSupport;
        String ldapClientId;
        LdapMapper mapper;
        FoldersConfig foldersConfig;
        Optional<CacheConfig> cacheConfig;

        Builder() {
            super();
        }

        Builder name(String value) {
            this.name = value;
            return this;
        }

        Builder cacheConfig(CacheConfig value) {
            this.cacheConfig = Optional.ofNullable(value);
            return this;
        }

        Builder isDeletedSupport(boolean value) {
            this.isDeletedSupport = value;
            return this;
        }

        Builder ldapClientId(String value) {
            this.ldapClientId = value;
            return this;
        }

        Builder maxPageSize(int value) {
            this.maxPageSize = value;
            return this;
        }

        Builder foldersConfig(FoldersConfig value) {
            this.foldersConfig = value;
            return this;
        }

        Builder mapper(LdapMapper value) {
            this.mapper = value;
            return this;
        }

        ProviderConfig build() {
            return new ProviderConfig(this);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String name;
    private final String ldapClientId;
    private final int maxPageSize;
    private final LdapMapper mapper;
    private final FoldersConfig foldersConfig;
    private final boolean isDeletedSupport;
    private final Optional<CacheConfig> cacheConfig;

    /**
     * Initializes a new {@link ProviderConfig}.
     *
     * @param builder The builder to use for initialization
     */
    ProviderConfig(ProviderConfig.Builder builder) {
        super();
        this.name = builder.name;
        this.maxPageSize = builder.maxPageSize;
        this.ldapClientId = builder.ldapClientId;
        this.mapper = builder.mapper;
        this.foldersConfig = builder.foldersConfig;
        this.isDeletedSupport = builder.isDeletedSupport;
        this.cacheConfig = builder.cacheConfig;
    }

    /**
     * Gets the display name for the contacts provider.
     *
     * @return The display name for the contacts provider
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the configured requested maximum size for paged results. "0" disables paged results.
     *
     * @return The requested maximum page size to use, or <code>0</code> if not restricted
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * Specifies if support for querying deleted objects is enabled or not. When enabled, deleted objects are identified with the filter
     * <code>isDeleted=TRUE</code>, which is usually only available in Active Directory (as control with OID 1.2.840.113556.1.4.417).
     *
     * @return <code>true</code> if the special tombstone control is available, <code>false</code>, otherwise
     */
    public boolean isDeletedSupport() {
        return isDeletedSupport;
    }

    /**
     * Gets the underlying LDAP attribute mapper.
     *
     * @return The LDAP mapper
     */
    public LdapMapper getMapper() {
        return mapper;
    }

    /**
     * Gets the identifier of the LDAP client configuration settings to use.
     *
     * @return The LDAP client identifier
     */
    public String getLdapClientId() {
        return ldapClientId;
    }

    /**
     * Gets the configured folder configuration mode.
     *
     * @return The folders configuration mode
     */
    public FoldersConfig getFoldersConfig() {
        return foldersConfig;
    }

    /**
     * Gets the cacheConfig
     *
     * @return The cacheConfig
     */
    public Optional<CacheConfig> optCacheConfig() {
        return cacheConfig;
    }

}
