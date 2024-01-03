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
package com.openexchange.folderstorage.database;

import java.util.Locale;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.GlobalAddressBookUtils;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link LocalizedGlobalAddressBookFolder}
 *
 * A locale-sensitive global address book database folder
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LocalizedGlobalAddressBookFolder extends LocalizedDatabaseFolder {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DatabaseFolder.class);

    private static final long serialVersionUID = -1837825836010345977L;

    public LocalizedGlobalAddressBookFolder(FolderObject folderObject) {
        super(folderObject);
    }

    @Override
    public String getLocalizedName(Locale locale) {
        final Locale loc = null == locale ? LocaleTools.DEFAULT_LOCALE : locale;
        String translation = localizedNames.get(loc);
        if (null == translation) {
            ServerSession session;
            try {
                session = getSession();
                if (session != null) {
                    final String ntranslation = GlobalAddressBookUtils.getFolderName(loc, session.getContextId());
                    translation = localizedNames.putIfAbsent(loc, ntranslation);
                    if (null == translation) {
                        translation = ntranslation;
                    }
                    return translation;
                }
            } catch (OXException e) {
                LOG.debug("Unable to localize global address book folder name, assuming '{}'.", FolderStrings.SYSTEM_LDAP_FOLDER_NAME, e);
            }
            return FolderStrings.SYSTEM_LDAP_FOLDER_NAME;
        }
        return translation;
    }

}
