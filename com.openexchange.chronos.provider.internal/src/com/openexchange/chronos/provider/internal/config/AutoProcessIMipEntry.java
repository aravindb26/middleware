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

package com.openexchange.chronos.provider.internal.config;

import org.json.JSONObject;
import com.openexchange.chronos.scheduling.AutoProcessIMip;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * 
 * {@link AutoProcessIMipEntry}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class AutoProcessIMipEntry extends ChronosJSlobEntry {

    private static final String AUTO_PROCESS_I_MIP = "autoProcessIMip";

    /**
     * Initializes a new {@link AutoProcessIMipEntry}.
     *
     * @param services The service lookup
     */
    public AutoProcessIMipEntry(ServiceLookup services) {
        super(services);
    }

    @Override
    public String getPath() {
        return "chronos/" + AUTO_PROCESS_I_MIP;
    }

    @Override
    public boolean isWritable(Session session) throws OXException {
        return true;
    }

    @Override
    protected Object getValue(ServerSession session, JSONObject userConfig) throws OXException {
        String autoProcessImip = userConfig.optString(AUTO_PROCESS_I_MIP);
        if (Strings.isEmpty(autoProcessImip)) {
            return AutoProcessIMip.getConfiguredValue(session.getContextId(), session.getUserId(), services.getService(LeanConfigurationService.class)).toString();
        }
        return autoProcessImip;
    }

    @Override
    protected void setValue(ServerSession session, JSONObject userConfig, Object value) throws OXException {
        if (value instanceof String) {
            String processIMip = (String) value;
            UserConfigHelper userConfigHelper = new UserConfigHelper(services);
            userConfigHelper.setAutoProcessIMip(userConfig, processIMip);
            userConfigHelper.checkUserConfig(userConfig);
        }
    }

}
