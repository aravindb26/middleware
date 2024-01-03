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

import static com.openexchange.contact.provider.ldap.config.ConfigUtils.asMap;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getArray;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getFilter;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optSearchScope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.common.io.BaseEncoding;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.contact.provider.ldap.LdapFolderFilter;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link StaticFoldersConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class StaticFoldersConfig extends FoldersConfig {

    /**
     * Initializes a new {@link StaticFoldersConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @param parent The generic folder config builder to take over the common settings from
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static StaticFoldersConfig init(Map<String, Object> configEntry, FoldersConfig.Builder parent) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new StaticFoldersConfig.Builder(parent) // @formatter:off
            .commonContactFilter(getFilter(configEntry, "commonContactFilter"))
            .commonContactSearchScope(optSearchScope(configEntry, "commonContactSearchScope", SearchScope.SUB))
            .folders(parseFolders(getArray(configEntry, "folders")))
        .build(); // @formatter:on
    }

    private static StaticFolderConfig[] parseFolders(ArrayList<?> array) throws OXException {
        if (null == array || array.isEmpty()) {
            throw ConfigurationExceptionCodes.PROPERTY_MISSING.create("folders");
        }
        StaticFolderConfig[] folders = new StaticFolderConfig[array.size()];
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> configEntry;
            try {
                configEntry = asMap(array.get(i));
            } catch (Exception e) {
                throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(e, "folders");
            }
            folders[i] = StaticFolderConfig.init(configEntry);
        }
        return folders;
    }

    private static class Builder extends FoldersConfig.Builder {

        Filter commonContactFilter;
        SearchScope commonContactSearchScope;
        StaticFolderConfig[] folders;

        Builder(FoldersConfig.Builder parent) {
            super();
            this.usedForSync = parent.usedForSync;
            this.shownInTree = parent.shownInTree;
            this.usedInPicker = parent.usedInPicker;
        }

        Builder folders(StaticFolderConfig[] value) {
            this.folders = value;
            return this;
        }

        Builder commonContactFilter(Filter value) {
            this.commonContactFilter = value;
            return this;
        }

        Builder commonContactSearchScope(SearchScope value) {
            this.commonContactSearchScope = value;
            return this;
        }

        StaticFoldersConfig build() {
            return new StaticFoldersConfig(this);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final StaticFolderConfig[] folders;
    private final Filter commonContactFilter;
    private final SearchScope commonContactSearchScope;
    private final Map<String, LdapFolderFilter> folderFilters;

    /**
     * Initializes a new {@link StaticFoldersConfig}.
     *
     * @param builder The builder to use for initialization
     */
    StaticFoldersConfig(StaticFoldersConfig.Builder builder) {
        super(builder);
        this.folders = builder.folders;
        this.commonContactFilter = builder.commonContactFilter;
        this.commonContactSearchScope = builder.commonContactSearchScope;
        this.folderFilters = initFolderFilters(folders);
    }

    public StaticFolderConfig[] getFolders() {
        return folders;
    }

    @Override
    public Filter getCommonContactFilter() {
        return commonContactFilter;
    }

    @Override
    public SearchScope getCommonContactSearchScope() {
        return commonContactSearchScope;
    }

    @Override
    public Map<String, LdapFolderFilter> getFolderFilters(ProviderConfig config, LDAPConnectionProvider connectionHolder, Session session) throws OXException {
        return folderFilters;
    }

    private Map<String, LdapFolderFilter> initFolderFilters(StaticFolderConfig[] folderConfigs) {
        if (null == folderConfigs) {
            return Collections.emptyMap();
        }
        Map<String, LdapFolderFilter> folderFilters = new LinkedHashMap<String, LdapFolderFilter>(folderConfigs.length);
        for (StaticFolderConfig folderConfig : folderConfigs) {
            String folderId = BaseEncoding.base64().omitPadding().encode(folderConfig.getName().getBytes(Charsets.UTF_8));
            LdapFolderFilter folderFilter = new LdapFolderFilter(folderConfig.getName(), folderConfig.getContactFilter(), folderConfig.getContactSearchScope());
            folderFilters.put(folderId, folderFilter);
        }
        return folderFilters;
    }

}
