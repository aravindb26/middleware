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

package com.openexchange.clientinfo.impl;

import com.openexchange.ajax.Client;
import com.openexchange.clientinfo.ClientInfo;
import com.openexchange.clientinfo.ClientInfoProvider;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;

/**
 * {@link USMEASClientInfoProvider}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.0
 */
public class USMEASClientInfoProvider implements ClientInfoProvider {

    private static final String USMEAS_CLIENT_ID = Client.USM_EAS.getClientId();

    private static final String OS_FAMILY_WINDOWS = "windows";
    private static final String OS_FAMILY_ANDROID = "android";
    private static final String OS_FAMILY_IOS = "ios";

    /**
     * Initializes a new {@link USMEASClientInfoProvider}.
     */
    public USMEASClientInfoProvider() {
        super();
    }

    @Override
    public ClientInfo getClientInfo(Session session) {
        if (null == session || false == USMEAS_CLIENT_ID.equals(session.getClient())) {
            return null;
        }
        return extractClientInfo((String) session.getParameter(Session.PARAM_DEVICE_USER_AGENT));
    }

    @Override
    public ClientInfo getClientInfo(String clientId) {
        return USMEAS_CLIENT_ID.equals(clientId) ? USMEASClientInfo.GENERIC : null;
    }

    private static USMEASClientInfo extractClientInfo(String userAgent) {
        if (Strings.isEmpty(userAgent)) {
            return USMEASClientInfo.GENERIC;
        }
        if (userAgent.startsWith("MSFT-WIN-3/1")) {
            return new USMEASClientInfo("Windows Mail (EAS)", userAgent.substring(11), OS_FAMILY_WINDOWS);
        }
        if (userAgent.startsWith("Outlook/")) {
            return new USMEASClientInfo("Microsoft Outlook (EAS)", userAgent.substring(8), OS_FAMILY_WINDOWS);
        }
        if (userAgent.startsWith("Apple-iPad")) {
            return new USMEASClientInfo("Apple iPad (EAS)", null, OS_FAMILY_IOS);
        }
        if (userAgent.startsWith("Apple-iPhone")) {
            return new USMEASClientInfo("Apple iPhone (EAS)", null, OS_FAMILY_IOS);
        }
        if (userAgent.startsWith("Outlook-iOS-Android")) {
            return new USMEASClientInfo("Outlook Mobile (EAS)", null, null);
        }
        if (userAgent.startsWith("Nine-")) {
            return new USMEASClientInfo("Nine - Email & Calendar (EAS)", null, OS_FAMILY_ANDROID);
        }
        if (userAgent.startsWith("Android-Mail")) {
            return new USMEASClientInfo("Gmail (EAS)", null, OS_FAMILY_ANDROID);
        }
        if (userAgent.startsWith("Android-SAMSUNG-") || userAgent.startsWith("Android-SAMSUNG-SGH") || userAgent.startsWith("SAMSUNG-SM-")) {
            return new USMEASClientInfo("Samsung Email (EAS)", null, OS_FAMILY_ANDROID);
        }
        if (userAgent.startsWith("Android-LG-")) {
            return new USMEASClientInfo("LG Email (EAS)", null, OS_FAMILY_ANDROID);
        }
        return USMEASClientInfo.GENERIC;
    }

}
