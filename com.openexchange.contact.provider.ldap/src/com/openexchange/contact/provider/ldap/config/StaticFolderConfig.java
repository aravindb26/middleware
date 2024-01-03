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
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getFilter;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optSearchScope;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link StaticFolderConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class StaticFolderConfig {

    /**
     * Initializes a new {@link StaticFolderConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static StaticFolderConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new StaticFolderConfig.Builder() // @formatter:off
            .name(get(configEntry, "name"))
            .contactFilter(getFilter(configEntry, "contactFilter"))
            .contactSearchScope(optSearchScope(configEntry, "contactSearchScope", SearchScope.SUB))
        .build(); // @formatter:on
    }

    private static class Builder {

        String name;
        Filter contactFilter;
        SearchScope contactSearchScope;

        Builder() {
            super();
        }

        Builder name(String value) {
            this.name = value;
            return this;
        }

        Builder contactFilter(Filter value) {
            this.contactFilter = value;
            return this;
        }

        Builder contactSearchScope(SearchScope value) {
            this.contactSearchScope = value;
            return this;
        }

        StaticFolderConfig build() {
            return new StaticFolderConfig(this);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String name;
    private final Filter contactFilter;
    private final SearchScope contactSearchScope;

    /**
     * Initializes a new {@link StaticFolderConfig}.
     *
     * @param builder The builder to use for initialization
     */
    StaticFolderConfig(StaticFolderConfig.Builder builder) {
        super();
        this.name = builder.name;
        this.contactFilter = builder.contactFilter;
        this.contactSearchScope = builder.contactSearchScope;
    }

    public StaticFolderConfig(String name, Filter contactFilter, SearchScope contactSearchScope) {
        super();
        this.name = name;
        this.contactFilter = contactFilter;
        this.contactSearchScope = contactSearchScope;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the contactFilter
     */
    public Filter getContactFilter() {
        return contactFilter;
    }

    /**
     * @return the contactSearchScope
     */
    public SearchScope getContactSearchScope() {
        return contactSearchScope;
    }

}
