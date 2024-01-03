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
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getEnum;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.getProtectableValue;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.contact.provider.ldap.LdapFolderFilter;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link FoldersConfig}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public abstract class FoldersConfig {

    /**
     * Initializes a new {@link FixedAttributesFoldersConfig} from the supplied .yaml-based provider configuration section.
     * 
     * @param configEntry The provider configuration section to parse
     * @return The parsed configuration, or <code>null</code> if the passed map was <code>null</code>
     */
    public static FoldersConfig init(Map<String, Object> configEntry) throws OXException {
        if (null == configEntry) {
            return null;
        }
        FoldersConfig.Builder builder = new FoldersConfig.Builder() // @formatter:off
            .usedForSync(getProtectableValue(configEntry, "usedForSync", Boolean.class))
            .usedInPicker(getProtectableValue(configEntry, "usedInPicker", Boolean.class))
            .shownInTree(getProtectableValue(configEntry, "shownInTree", Boolean.class))
        ; // @formatter:on
        switch (getEnum(configEntry, "mode", FolderConfigMode.class)) {
            case DYNAMICATTRIBUTES:
                return DynamicAttributesFoldersConfig.init(asMap(configEntry.get("dynamicAttributes")), builder);
            case FIXEDATTRIBUTES:
                return FixedAttributesFoldersConfig.init(asMap(configEntry.get("fixedAttributes")), builder);
            case STATIC:
                return StaticFoldersConfig.init(asMap(configEntry.get("static")), builder);
            default:
                throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create("mode");
        }
    }

    protected static class Builder {

        ProtectableValue<Boolean> usedForSync;
        ProtectableValue<Boolean> usedInPicker;
        ProtectableValue<Boolean> shownInTree;

        Builder() {
            super();
        }

        Builder usedForSync(ProtectableValue<Boolean> value) {
            this.usedForSync = value;
            return this;
        }

        Builder usedInPicker(ProtectableValue<Boolean> value) {
            this.usedInPicker = value;
            return this;
        }

        Builder shownInTree(ProtectableValue<Boolean> value) {
            this.shownInTree = value;
            return this;
        }
    }

    protected final ProtectableValue<Boolean> usedForSync;
    protected final ProtectableValue<Boolean> usedInPicker;
    protected final ProtectableValue<Boolean> shownInTree;

    /**
     * Initializes a new {@link FoldersConfig}.
     * 
     * @param builder The builder to use for initialization
     */
    FoldersConfig(FoldersConfig.Builder builder) {
        super();
        this.usedForSync = builder.usedForSync;
        this.usedInPicker = builder.usedInPicker;
        this.shownInTree = builder.shownInTree;
    }

    /**
     * Gets a value indicating if the addressbook folders can be synchronized to external clients via CardDAV or not. If set to "false",
     * the folders are only available in the web client. If set to "true", folders can be activated for synchronization.
     * 
     * @return The "usedForSync" folder configuration
     */
    public ProtectableValue<Boolean> isUsedForSync() {
        return usedForSync;
    }

    /**
     * Gets a value indicating if whether addressbook folders will be available in the contact picker dialog of App Suite. If enabled,
     * contacts from this provider can be looked up through this dialog, otherwise they are hidden.
     * 
     * @return The "usedInPicker" folder configuration
     */
    public ProtectableValue<Boolean> isUsedInPicker() {
        return usedInPicker;
    }

    /**
     * Gets a value indicating if whether addressbook folders will be shown as 'subscribed' folders in the tree or not. If enabled, the
     * folders will appear in the contacts module of App Suite as regular, subscribed folder. Otherwise, they're treated as hidden,
     * unsubscribed folders.
     * 
     * @return The "shownInTree" folder configuration
     */
    public ProtectableValue<Boolean> isShownInTree() {
        return shownInTree;
    }

    /**
     * Gets a 'common' contact filter suitable to search for all contact entries across all configured folders.
     * 
     * @return The common contact filter
     */
    public abstract Filter getCommonContactFilter();

    /**
     * Gets the search scope to use in combination with the 'common' contact filter.
     * 
     * @return The search scope
     */
    public abstract SearchScope getCommonContactSearchScope();

    /**
     * Gets all configured folder filters, mapped to the corresponding folder identifiers.
     * 
     * @param config The underlying provider config
     * @param connectionProvider The LDAP connection provider to use if required
     * @param session The current user's session
     * @return All folder filters, mapped to the corresponding folder identifiers
     */
    public abstract Map<String, LdapFolderFilter> getFolderFilters(ProviderConfig config, LDAPConnectionProvider connectionProvider, Session session) throws OXException;

    /**
     * Initializes a new LDAP filter from the supplied filter template, replacing a placeholder string with the given replacement.
     * 
     * @param filterTemplate The filter template
     * @param placeHolder The placeholder string
     * @param replacement The replacement
     * @return The prepared filter
     */
    protected static Filter prepareFilter(String filterTemplate, String placeHolder, String replacement) throws OXException {
        try {
            return Filter.create(filterTemplate.replaceAll(Pattern.quote(placeHolder), Matcher.quoteReplacement(replacement)));
        } catch (PatternSyntaxException | LDAPException e) {
            throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(e, filterTemplate);
        }
    }

}
