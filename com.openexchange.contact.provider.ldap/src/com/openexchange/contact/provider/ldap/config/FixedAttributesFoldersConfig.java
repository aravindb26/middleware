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

import static com.openexchange.config.YamlUtils.get;
import static com.openexchange.config.YamlUtils.getArray;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.optSearchScope;
import static com.openexchange.java.Strings.getEmptyStrings;
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
 * {@link FixedAttributesFoldersConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class FixedAttributesFoldersConfig extends FoldersConfig {

    /**
     * Initializes a new {@link FixedAttributesFoldersConfig} from the supplied .yaml-based provider configuration section.
     *
     * @param configEntry The provider configuration section to parse
     * @param parent The generic folder config builder to take over the common settings from
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static FixedAttributesFoldersConfig init(Map<String, Object> configEntry, FoldersConfig.Builder parent) throws OXException {
        if (null == configEntry) {
            return null;
        }
        return new FixedAttributesFoldersConfig.Builder(parent) // @formatter:off
            .contactFilterTemplate(get(configEntry, "contactFilterTemplate"))
            .contactSearchScope(optSearchScope(configEntry, "contactSearchScope", SearchScope.SUB))
            .attributeValues(parseAttributeValues(getArray(configEntry, "attributeValues")))
        .build(); // @formatter:on
    }

    private static String[] parseAttributeValues(ArrayList<?> array) throws OXException {
        if (null == array) {
            return null;
        }

        int length = array.size();
        if (length <= 0) {
            return getEmptyStrings();
        }

        String[] attributeValues = new String[length];
        int index = 0;
        for (Object arrayEelement : array) {
            try {
                attributeValues[index++] = (String) arrayEelement;
            } catch (Exception e) {
                throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(e, "attributeValues");
            }
        }
        return attributeValues;
    }

    private static class Builder extends FoldersConfig.Builder {

        String[] attributeValues;
        String contactFilterTemplate;
        SearchScope contactSearchScope;

        Builder(FoldersConfig.Builder parent) {
            super();
            this.usedForSync = parent.usedForSync;
            this.shownInTree = parent.shownInTree;
            this.usedInPicker = parent.usedInPicker;
        }

        Builder attributeValues(String[] value) {
            this.attributeValues = value;
            return this;
        }

        Builder contactFilterTemplate(String value) {
            this.contactFilterTemplate = value;
            return this;
        }

        Builder contactSearchScope(SearchScope value) {
            this.contactSearchScope = value;
            return this;
        }

        FixedAttributesFoldersConfig build() throws OXException {
            return new FixedAttributesFoldersConfig(this);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String[] attributeValues;
    private final String contactFilterTemplate;
    private final SearchScope contactSearchScope;
    private final Map<String, LdapFolderFilter> folderFilters;
    private final Filter commonContactFilter;

    /**
     * Initializes a new {@link FixedAttributesFoldersConfig}.
     *
     * @param builder The builder to use for initialization
     */
    FixedAttributesFoldersConfig(FixedAttributesFoldersConfig.Builder builder) throws OXException {
        super(builder);
        this.attributeValues = builder.attributeValues;
        this.contactFilterTemplate = builder.contactFilterTemplate;
        this.contactSearchScope = builder.contactSearchScope;
        this.folderFilters = initFolderFilters(attributeValues, contactFilterTemplate, contactSearchScope);
        this.commonContactFilter = prepareFilter(contactFilterTemplate, "[value]", "*");
    }

    /**
     * @return the attributeName
     */
    public String[] getAttributeValues() {
        return attributeValues;
    }

    /**
     * @return the contactFilter
     */
    public String getContactFilterTemplate() {
        return contactFilterTemplate;
    }

    /**
     * @return the contactSearchScope
     */
    public SearchScope getContactSearchScope() {
        return contactSearchScope;
    }

    /**
     * @return the contactFilter
     */
    @Override
    public Filter getCommonContactFilter() {
        return commonContactFilter;
    }

    /**
     * @return the contactSearchScope
     */
    @Override
    public SearchScope getCommonContactSearchScope() {
        return getContactSearchScope();
    }

    @Override
    public Map<String, LdapFolderFilter> getFolderFilters(ProviderConfig config, LDAPConnectionProvider connectionHolder, Session session) throws OXException {
        return folderFilters;
    }

    private static Map<String, LdapFolderFilter> initFolderFilters(String[] attributeValues, String contactFilterTemplate, SearchScope contactSearchScope) throws OXException {
        if (null == attributeValues) {
            return Collections.emptyMap();
        }
        Map<String, LdapFolderFilter> folderFilters = new LinkedHashMap<String, LdapFolderFilter>(attributeValues.length);
        for (String attributeValue : attributeValues) {
            String folderId = BaseEncoding.base64().omitPadding().encode(attributeValue.getBytes(Charsets.UTF_8));
            Filter contactFilter = prepareFilter(contactFilterTemplate, "[value]", Filter.encodeValue(attributeValue));
            LdapFolderFilter folderFilter = new LdapFolderFilter(attributeValue, contactFilter, contactSearchScope);
            folderFilters.put(folderId, folderFilter);
        }
        return folderFilters;
    }

}
