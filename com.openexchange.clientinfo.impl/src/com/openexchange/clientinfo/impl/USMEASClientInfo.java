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

import java.util.Locale;
import com.openexchange.clientinfo.ClientInfo;
import com.openexchange.clientinfo.ClientInfoType;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Strings;


/**
 * {@link USMEASClientInfo}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.0
 */
public class USMEASClientInfo implements ClientInfo {

    /** The generic USM/EAS client info w/o further details */
    public static USMEASClientInfo GENERIC = new USMEASClientInfo();

    private final String osFamily;
    private final String clientName;
    private final String clientVersion;

    /**
     * Initializes a new {@link USMEASClientInfo}.
     */
    private USMEASClientInfo() {
        this(null, null, null);
    }

    /**
     * Initializes a new {@link USMEASClientInfo}.
     * 
     * @param clientName
     * @param clientVersion
     * @param osFamily
     */
    public USMEASClientInfo(String clientName, String clientVersion, String osFamily) {
        super();
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.osFamily = osFamily;
    }


    @Override
    public ClientInfoType getType() {
        return ClientInfoType.EAS;
    }

    @Override
    public String getDisplayName(Locale locale) {
        return Strings.isNotEmpty(clientName) ? clientName : StringHelper.valueOf(locale).getString(ClientInfoStrings.USM_EAS_CLIENT);
    }

    @Override
    public String getOSFamily() {
        return osFamily;
    }

    @Override
    public String getOSVersion() {
        return null;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    @Override
    public String getClientVersion() {
        return clientVersion;
    }

    @Override
    public String getClientFamily() {
        return "usmeasclient";
    }

}
