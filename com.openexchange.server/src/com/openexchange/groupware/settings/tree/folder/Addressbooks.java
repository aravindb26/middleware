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

package com.openexchange.groupware.settings.tree.folder;

import static com.openexchange.contact.common.ContactsAccount.DEFAULT_ACCOUNT;
import static com.openexchange.contact.common.ContactsAccount.ID_PREFIX;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.ReadOnlyValue;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;
import com.openexchange.tools.id.IDMangler;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.user.User;

/**
 * {@link Addressbooks}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since 7.10.6
 */
public class Addressbooks implements PreferencesItemService {
    
    /**
     * Default constructor.
     */
    public Addressbooks() {
        super();
    }

    @Override
    public String[] getPath() {
        return new String[] { "folder", "addressbooks" };
    }

    @Override
    public IValueHandler getSharedValue() {
        return new ReadOnlyValue() {

            @Override
            public boolean isAvailable(UserConfiguration userConfig) {
                return userConfig.hasContact();
            }

            @Override
            public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws OXException {
                if (false == user.isGuest()) {
                    String folderId = Integer.toString(new OXFolderAccess(ctx).getDefaultFolderID(user.getId(), FolderObject.CONTACT));
                    setting.setSingleValue(IDMangler.mangle(ID_PREFIX, Integer.toString(DEFAULT_ACCOUNT.getAccountId()), folderId));
                }
            }
        };
    }
}
