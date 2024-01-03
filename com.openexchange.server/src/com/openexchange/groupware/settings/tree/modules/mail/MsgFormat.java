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

import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.SettingExceptionCodes;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class MsgFormat implements PreferencesItemService {

    /**
     * Default constructor.
     */
    public MsgFormat() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getPath() {
        return new String[] { "modules", "mail", "msgformat" };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IValueHandler getSharedValue() {
        return new IValueHandler() {

            @Override
            public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws OXException {
                final UserSettingMail settings = UserSettingMailStorage.getInstance().getUserSettingMail(user.getId(), ctx);
                if (null != settings) {
                    // html vs. text vs. alternative
                    String format = "html";
                    {
                        int msgFormat = settings.getMsgFormat();
                        switch (msgFormat) {
                            case UserSettingMail.MSG_FORMAT_TEXT_ONLY:
                                format = "text";
                                break;
                            case UserSettingMail.MSG_FORMAT_HTML_ONLY:
                                format = "html";
                                break;
                            case UserSettingMail.MSG_FORMAT_BOTH:
                                format = "alternative";
                                break;
                        }
                    }
                    setting.setSingleValue(format);
                }
            }

            @Override
            public boolean isAvailable(final UserConfiguration userConfig) {
                return userConfig.hasWebMail();
            }

            @Override
            public boolean isWritable() {
                return true;
            }

            @Override
            public void writeValue(final Session session, final Context ctx, final User user, final Setting setting) throws OXException {
                final UserSettingMailStorage storage = UserSettingMailStorage.getInstance();
                final UserSettingMail usm = storage.getUserSettingMail(user.getId(), ctx);
                if (null != usm) {
                    int msgFormat = UserSettingMail.MSG_FORMAT_HTML_ONLY;
                    {
                        final String s = setting.getSingleValue().toString();
                        if (com.openexchange.java.Strings.isEmpty(s)) {
                            return;
                        }
                        if ("text".equalsIgnoreCase(s)) {
                            msgFormat = UserSettingMail.MSG_FORMAT_TEXT_ONLY;
                        } else if ("html".equalsIgnoreCase(s)) {
                            msgFormat = UserSettingMail.MSG_FORMAT_HTML_ONLY;
                        } else if ("alternative".equalsIgnoreCase(s)) {
                            msgFormat = UserSettingMail.MSG_FORMAT_BOTH;
                        }
                    }

                    try {
                        if (msgFormat != usm.getMsgFormat()) {
                            usm.setMsgFormat(msgFormat);
                            storage.saveUserSettingMail(usm, user.getId(), ctx);
                        }
                    } catch (NumberFormatException e) {
                        throw SettingExceptionCodes.JSON_READ_ERROR.create(e);
                    }
                }
            }

            @Override
            public int getId() {
                return -1;
            }
        };
    }
}
