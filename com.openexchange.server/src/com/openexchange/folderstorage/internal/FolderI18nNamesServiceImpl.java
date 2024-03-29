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

package com.openexchange.folderstorage.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FolderI18nNamesService;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.modules.Module;
import com.openexchange.i18n.I18nService;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link FolderI18nNamesServiceImpl} - Provides the localized folder names for specified folder modules.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderI18nNamesServiceImpl implements FolderI18nNamesService {

    private static final FolderI18nNamesServiceImpl INSTANCE = new FolderI18nNamesServiceImpl();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static FolderI18nNamesServiceImpl getInstance() {
        return INSTANCE;
    }

    private final Map<Locale, I18nService> services;
    private final Map<Integer, Set<String>> identifiersPerModule;
    private final Map<Integer, Set<String>> i18nNamesPerModule;

    /**
     * Initializes a new {@link FolderI18nNamesServiceImpl}.
     */
    private FolderI18nNamesServiceImpl() {
        super();
        services = new ConcurrentHashMap<Locale, I18nService>();
        identifiersPerModule = new ConcurrentHashMap<Integer, Set<String>>();
        i18nNamesPerModule = new ConcurrentHashMap<Integer, Set<String>>();
        initIdentifiers();
    }

    private void initIdentifiers() {
        /*
         * mail
         */
        Set<String> identifiers = new HashSet<String>();
        identifiers.add(MailStrings.INBOX);
        identifiers.add(MailStrings.SPAM);
        identifiers.add(MailStrings.DRAFTS);
        identifiers.add(MailStrings.TRASH);
        identifiers.add(MailStrings.SENT);
        identifiers.add(MailStrings.SENT_ALT);
        identifiers.add(MailStrings.CONFIRMED_HAM);
        identifiers.add(MailStrings.CONFIRMED_HAM_ALT);
        identifiers.add(MailStrings.CONFIRMED_SPAM);
        identifiers.add(MailStrings.CONFIRMED_SPAM_ALT);
        identifiers.add(MailStrings.ARCHIVE);
        identifiersPerModule.put(Integer.valueOf(Module.MAIL.getFolderConstant()), identifiers);
        /*
         * calendar
         */
        identifiers = new HashSet<String>();
        identifiers.add(FolderStrings.DEFAULT_CALENDAR_FOLDER_NAME);
        identifiers.add(FolderStrings.VIRTUAL_LIST_CALENDAR_FOLDER_NAME);
        identifiersPerModule.put(Integer.valueOf(Module.CALENDAR.getFolderConstant()), identifiers);
        /*
         * contacts
         */
        identifiers = new HashSet<String>();
        identifiers.add(FolderStrings.DEFAULT_CONTACT_COLLECT_FOLDER_NAME);
        identifiers.add(FolderStrings.DEFAULT_CONTACT_FOLDER_NAME);
        /*identifiers.add(FolderStrings.SYSTEM_GLOBAL_FOLDER_NAME);*/ // finally dropped
        identifiers.add(FolderStrings.SYSTEM_LDAP_FOLDER_NAME);
        identifiers.add(FolderStrings.INTERNAL_USERS_NAME);
        identifiers.add(FolderStrings.ALL_USERS_NAME);
        identifiers.add(FolderStrings.VIRTUAL_LIST_CONTACT_FOLDER_NAME);
        identifiersPerModule.put(Integer.valueOf(Module.CONTACTS.getFolderConstant()), identifiers);
        /*
         * tasks
         */
        identifiers = new HashSet<String>();
        identifiers.add(FolderStrings.DEFAULT_TASK_FOLDER_NAME);
        identifiers.add(FolderStrings.VIRTUAL_LIST_TASK_FOLDER_NAME);
        identifiersPerModule.put(Integer.valueOf(Module.TASK.getFolderConstant()), identifiers);
        /*
         * infostore
         */
        identifiers = new HashSet<String>();
        identifiers.add(FolderStrings.DEFAULT_EMAIL_ATTACHMENTS_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_INFOSTORE_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_PUBLIC_INFOSTORE_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_TRASH_INFOSTORE_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_INFOSTORE_FOLDER_NAME);
        identifiers.add(FolderStrings.VIRTUAL_LIST_INFOSTORE_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_FILES_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_FILES_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_PUBLIC_FILES_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_TRASH_FILES_FOLDER_NAME);
        identifiers.add(FolderStrings.VIRTUAL_LIST_FILES_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_PICTURES_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_DOCUMENTS_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_MUSIC_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_VIDEOS_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_USER_TEMPLATES_FOLDER_NAME);
        identifiersPerModule.put(Integer.valueOf(Module.INFOSTORE.getFolderConstant()), identifiers);
        /*
         * system
         */
        identifiers = new HashSet<String>();
        identifiers.add(FolderStrings.SYSTEM_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_OX_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_PRIVATE_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_PUBLIC_FOLDER_NAME);
        identifiers.add(FolderStrings.SYSTEM_SHARED_FOLDER_NAME);
        identifiersPerModule.put(Integer.valueOf(Module.SYSTEM.getFolderConstant()), identifiers);
    }

    /**
     * Adds specified service.
     *
     * @param service The service to add
     */
    public void addService(final I18nService service) {
        services.put(service.getLocale(), service);
        i18nNamesPerModule.clear();
    }

    /**
     * Removes specified service.
     *
     * @param service The service to remove
     */
    public void removeService(final I18nService service) {
        services.remove(service.getLocale());
        i18nNamesPerModule.clear();
    }

    @Override
    public Set<String> getI18nNamesFor(int...modules) {
        Map<Integer, Set<String>> i18nNamesPerModule = getI18nNamesPerModule();
        Set<String> i18nNames = new HashSet<String>();
        if (null == modules || 0 == modules.length) {
            for (Set<String> names : i18nNamesPerModule.values()) {
                i18nNames.addAll(names);
            }
        } else {
            for (Entry<Integer, Set<String>> entry : i18nNamesPerModule.entrySet()) {
                if (Arrays.contains(modules, entry.getKey().intValue())) {
                    i18nNames.addAll(entry.getValue());
                }
            }
        }
        return Collections.unmodifiableSet(i18nNames);
    }

    @Override
    public Set<String> getI18nNamesFor(int module, String... folderStrings) throws OXException {
        Set<String> knownIdentifiers = identifiersPerModule.get(Integer.valueOf(module));
        if (null == knownIdentifiers || 0 == knownIdentifiers.size()) {
            return Collections.emptySet();
        }
        Set<String> i18nNames = new HashSet<String>();
        for (String folderString : folderStrings) {
            if (knownIdentifiers.contains(folderString)) {
                i18nNames.add(folderString);
                for (I18nService service : services.values()) {
                    i18nNames.add(service.getLocalized(folderString));
                }
            }
        }
        return i18nNames;
    }

    /**
     * Gets the known i18n names per module, initializing the map as needed.
     *
     * @return The i18n names per module
     */
    private Map<Integer, Set<String>> getI18nNamesPerModule() {
        if (i18nNamesPerModule.isEmpty()) {
            synchronized (services) {
                if (i18nNamesPerModule.isEmpty()) {
                    Set<String> reservedNames = getFolderReservedNames();
                    for (Entry<Integer, Set<String>> entry : identifiersPerModule.entrySet()) {
                        Set<String> i18nNames = new HashSet<String>();
                        /*
                         * add custom reserved names and default names for default locale as-is
                         */
                        i18nNames.addAll(reservedNames);
                        i18nNames.addAll(entry.getValue());
                        /*
                         * insert translated names for all services
                         */
                        for (I18nService service : services.values()) {
                            for (String identifier : entry.getValue()) {
                                i18nNames.add(service.getLocalized(identifier));
                            }
                        }

                        i18nNamesPerModule.put(entry.getKey(), i18nNames);
                    }
                }
            }
        }
        return i18nNamesPerModule;
    }

    /**
     * Gets all reserved names as defined in the <code>folder-reserved-names</code> configuration property.
     *
     * @return The reserved names, or an empty set if none are defined
     */
    private static Set<String> getFolderReservedNames() {
        Set<String> reservedNames = new HashSet<String>();
        ConfigurationService configService = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        if (null != configService) {
            String text = configService.getText("folder-reserved-names");
            if (Strings.isNotEmpty(text)) {
                for (String line : Strings.splitByLineSeparator(text)) {
                    if (Strings.isNotEmpty(line) && '#' != line.charAt(0)) {
                        for (String value : Strings.splitByComma(line)) {
                            String processedValue = processValue(value);
                            if (null != processedValue) {
                                reservedNames.add(processedValue);
                            }
                        }
                    }
                }
            }
        }
        return reservedNames;
    }

    private static String processValue(final String value) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        String val = value.trim();
        if ('#' == val.charAt(0)) { // Comment line
            return null;
        }
        final int mlen = val.length() - 1;
        if ('"' == val.charAt(0) && '"' == val.charAt(mlen)) {
            if (0 == mlen) {
                return null;
            }
            val = val.substring(1, mlen);
            if (com.openexchange.java.Strings.isEmpty(val)) {
                return null;
            }
        }
        return val;
    }

}
