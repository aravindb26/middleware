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

package com.openexchange.share.notification.impl.mail;

import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.notify.NotificationConfig;
import com.openexchange.groupware.notify.NotificationConfig.NotificationProperty;
import com.openexchange.java.Strings;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.notification.mail.MailData;
import com.openexchange.notification.mail.NotificationMailFactory;
import com.openexchange.server.ServiceLookup;
import com.openexchange.user.User;


/**
 * {@link ShareNotificationMail}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
abstract class ShareNotificationMail {

    private final ServiceLookup services;
    private final MailData mailData;

    protected ShareNotificationMail(ServiceLookup services, MailData mailData) {
        super();
        this.services = services;
        this.mailData = mailData;
    }

    public ComposedMailMessage compose() throws OXException {
        NotificationMailFactory notificationMailFactory = services.getService(NotificationMailFactory.class);
        return notificationMailFactory.createMail(mailData);
    }

    /**
     * Get the mail address to use for the sharing user 
     *
     * @param context The context
     * @param sharingUser The sharing users
     * @return The mail address
     * @throws OXException If loading of mail storage data fails
     */
    protected static String getEmail(Context context, User sharingUser) throws OXException {
        String email = null;
        if ("defaultSenderAddress".equalsIgnoreCase(NotificationConfig.getProperty(NotificationProperty.FROM_SOURCE, "primaryMail"))) {
            email = UserSettingMailStorage.getInstance().loadUserSettingMail(sharingUser.getId(), context).getSendAddr();
        }
        if (Strings.isEmpty(email)) {
            return sharingUser.getMail();
        }
        return email;
    }

}
