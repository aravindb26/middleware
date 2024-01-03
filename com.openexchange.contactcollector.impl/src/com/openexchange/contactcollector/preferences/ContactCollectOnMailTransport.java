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

package com.openexchange.contactcollector.preferences;

import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class ContactCollectOnMailTransport implements PreferencesItemService {

    private static final String[] PATH = new String[] { "modules", "mail", "contactCollectOnMailTransport" };

    public ContactCollectOnMailTransport() {
        super();
    }

    @Override
    public String[] getPath() {
        return PATH;
    }

    @Override
    public IValueHandler getSharedValue() {
        return new IValueHandler() {

            @Override
            public int getId() {
                return -1;
            }

            @Override
            public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws OXException {
                final Boolean value = ServerUserSetting.getInstance().isContactCollectOnMailTransport(ctx.getContextId(), user.getId());
                setting.setSingleValue(value);
            }

            @Override
            public boolean isAvailable(final UserConfiguration userConfig) {
                return userConfig.hasWebMail() && userConfig.hasContact() && userConfig.isCollectEmailAddresses();
            }

            @Override
            public boolean isWritable() {
                return true;
            }

            @Override
            public void writeValue(
                final Session session, final Context ctx, final User user, final Setting setting) throws OXException {
                final boolean value = Boolean.parseBoolean(String.valueOf(setting.getSingleValue()));
                final ServerUserSetting sus = ServerUserSetting.getInstance();
                if (value != getPrevValue(session).booleanValue()) {
                    sus.setContactCollectOnMailTransport(ctx.getContextId(), user.getId(), value);
                }
            }

            private Boolean getPrevValue(final Session session) throws OXException {
                @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) session.getParameter("__serverUserSetting");
                if (null != map) {
                    return (Boolean) map.get("contactCollectOnMailTransport");
                }
                return ServerUserSetting.getInstance().isContactCollectOnMailTransport(session.getContextId(), session.getUserId());
            }
        };
    }

}