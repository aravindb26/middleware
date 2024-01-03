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

package com.openexchange.groupware.settings.tree.modules.mail;

import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.ReadOnlyValue;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.jslob.ConfigTreeEquivalent;
import com.openexchange.mail.FlaggingMode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * {@link MailFlaggedModePreferenceItem}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.4
 */
public class MailFlaggedModePreferenceItem implements PreferencesItemService, ConfigTreeEquivalent {

    @Override
    public String[] getPath() {
        return new String[] { "modules", "mail", "features", "flag", "flag" };
    }

    @Override
    public IValueHandler getSharedValue() {
        return new ReadOnlyValue() {

            @Override
            public boolean isAvailable(UserConfiguration userConfig) {
                return true;
            }

            @Override
            public void getValue(Session session, Context ctx, User user, UserConfiguration userConfig, Setting setting) throws OXException {
                ConfigViewFactory factory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);
                if (factory == null){
                    setting.setSingleValue(Boolean.TRUE);
                    return;
                }

                FlaggingMode mode = FlaggingMode.getFlaggingMode(session, factory);
                switch (mode) {
                    case COLOR_ONLY:
                        setting.setSingleValue(Boolean.FALSE);
                        break;
                    case FLAGGED_AND_COLOR:
                        setting.setSingleValue(Boolean.TRUE);
                        break;
                    case FLAGGED_IMPLICIT:
                        setting.setSingleValue(Boolean.FALSE);
                        break;
                    case FLAGGED_ONLY:
                        setting.setSingleValue(Boolean.TRUE);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public String getConfigTreePath() {
        return "modules/mail/features/flag/flag";
    }

    @Override
    public String getJslobPath() {
        return "io.ox/mail//features/flag/star";
    }

}
