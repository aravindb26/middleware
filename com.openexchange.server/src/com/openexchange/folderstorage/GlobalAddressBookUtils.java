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
package com.openexchange.folderstorage;

import static com.openexchange.folderstorage.GlobalAddressBookProperties.ALL_USERS_ID;
import static com.openexchange.folderstorage.GlobalAddressBookProperties.CUSTOM;
import static com.openexchange.folderstorage.GlobalAddressBookProperties.GLOBAL_ADDRESS_BOOK_ID;
import static com.openexchange.folderstorage.GlobalAddressBookProperties.INTERNAL_USERS_ID;
import static com.openexchange.groupware.i18n.FolderStrings.ALL_USERS_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.INTERNAL_USERS_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.SYSTEM_LDAP_FOLDER_NAME;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.i18n.I18nService;
import com.openexchange.i18n.I18nServiceRegistry;
import com.openexchange.i18n.Locale2LanguageMapping;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * {@link GlobalAddressBookUtils}
 * 
 * Global address book utility class
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class GlobalAddressBookUtils {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GlobalAddressBookUtils.class);

    /**
     * Gets the translated global address book folder name
     * 
     * @param locale The user's locale
     * @param contextId The user's context id to retrieve the GAB folder name setting
     * @return The translated global address book folder name
     */
    public static String getFolderName(Locale locale, int contextId) {
        LeanConfigurationService configService = ServerServiceRegistry.getInstance().getService(LeanConfigurationService.class);
        if (configService != null) {
            String identifier = configService.getProperty(-1, contextId, GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER);
            switch (identifier) {
                case GLOBAL_ADDRESS_BOOK_ID:
                    return StringHelper.valueOf(locale).getString(SYSTEM_LDAP_FOLDER_NAME);
                case INTERNAL_USERS_ID:
                    return StringHelper.valueOf(locale).getString(INTERNAL_USERS_NAME);
                case ALL_USERS_ID:
                    return StringHelper.valueOf(locale).getString(ALL_USERS_NAME);
                case CUSTOM:
                    return getCustomFolderName(configService, contextId, locale);
                default:
                    LOG.warn("Unknown value \"{}\" for \"{}\", falling back to defaults.", identifier, GlobalAddressBookProperties.GAB_FOLDER_NAME_IDENTIFIER.getFQPropertyName());
                    break;

            }
        }
        return StringHelper.valueOf(locale).getString(ALL_USERS_NAME);
    }

    /**
     * Gets the custom translated global address book folder name (see com.openexchange.contacts.customGabFolderName.[locale])
     * 
     * @param configService {@link LeanConfigurationService} reference
     * @param contextId The user's context id to retrieve the locale specific GAB folder name setting
     * @param locale The user's locale
     * @return The custom translated global address book folder name
     */
    private static String getCustomFolderName(LeanConfigurationService configService, int contextId, Locale userLocale) {
        String localizedCustomName = configService.getProperty(-1, contextId, GlobalAddressBookProperties.CUSTOM_LOCALIZED_GAB_FOLDER_NAME, Collections.singletonMap("locale", userLocale.toString()));
        if (localizedCustomName == null) {
            // find translation by best effort
            localizedCustomName = getCustomFolderNameByMapping(configService, contextId, userLocale);
            if (localizedCustomName == null) {
                localizedCustomName = getCustomFolderNameByI18nServices(configService, contextId, userLocale);
                if (localizedCustomName == null) {
                    // fallback to untranslated custom name
                    String defaultCustomName = configService.getProperty(-1, contextId, GlobalAddressBookProperties.CUSTOM_GAB_FOLDER_NAME);
                    if (defaultCustomName == null) {
                        return StringHelper.valueOf(userLocale).getString(ALL_USERS_NAME);
                    }
                    return defaultCustomName;
                }
            }
        }
        return localizedCustomName;
    }

    /**
     * Tries to find a translation by using a pre-defined mapping {@link Locale2LanguageMapping}
     * 
     * @param configService {@link LeanConfigurationService} reference
     * @param contextId The user's context id to retrieve the locale specific GAB folder name setting
     * @param locale The user's locale
     * @return The custom translated global address book folder name or <code>null</code> if not found
     */
    private static String getCustomFolderNameByMapping(LeanConfigurationService configService, int contextId, Locale userLocale) {
        String mappedLanguage = Strings.replaceSequenceWith(Locale2LanguageMapping.getLanguageForLocale(userLocale.toLanguageTag()), "-", "_");
        if (mappedLanguage != null) {
            return configService.getProperty(-1, contextId, GlobalAddressBookProperties.CUSTOM_LOCALIZED_GAB_FOLDER_NAME, Collections.singletonMap("locale", mappedLanguage));
        }
        return null;
    }

    /**
     * Tries to find a translation by using all available i18n services
     * 
     * @param configService {@link LeanConfigurationService} reference
     * @param contextId The user's context id to retrieve the locale specific GAB folder name setting
     * @param locale The user's locale
     * @return The custom translated global address book folder name or <code>null</code> if not found
     */
    private static String getCustomFolderNameByI18nServices(LeanConfigurationService configService, int contextId, Locale userLocale) {
        String language = userLocale.getLanguage();
        if (Strings.isNotEmpty(language)) {
            Collection<I18nService> services;
            try {
                services = ServerServiceRegistry.getInstance().getService(I18nServiceRegistry.class).getI18nServices();
            } catch (OXException e) {
                LOG.error("Unable to get the currently available i18n services", e);
                return null;
            }
            for (I18nService service : services) {
                Locale loc = service.getLocale();
                if (language.equals(loc.getLanguage())) {
                    if (language.equalsIgnoreCase(loc.getCountry())) {
                        String localizedCustomName = configService.getProperty(-1, contextId, GlobalAddressBookProperties.CUSTOM_LOCALIZED_GAB_FOLDER_NAME, Collections.singletonMap("locale", loc.toString()));
                        if (localizedCustomName != null) {
                            return localizedCustomName;  
                        }
                    }
                }
            }
        }
        return null;
    }

}
