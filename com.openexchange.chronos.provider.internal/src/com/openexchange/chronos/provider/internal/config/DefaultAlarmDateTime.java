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
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DefaultAlarmDateTime}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DefaultAlarmDateTime extends ChronosJSlobEntry {

    /**
     * Initializes a new {@link DefaultAlarmDateTime}.
     *
     * @param services A service lookup reference
     */
    public DefaultAlarmDateTime(ServiceLookup services) {
        super(services);
    }

    @Override
    public String getPath() {
        return "chronos/defaultAlarmDateTime";
    }

    @Override
    public boolean isWritable(Session session) throws OXException {
        return true;
    }

    @Override
    protected Object getValue(ServerSession session, JSONObject userConfig) throws OXException {
        return userConfig.optJSONArray("defaultAlarmDateTime");
    }

    @Override
    protected void setValue(ServerSession session, JSONObject userConfig, Object value) throws OXException {
        userConfig.putSafe("defaultAlarmDateTime", value);
        new UserConfigHelper(services).checkUserConfig(userConfig);
    }

}
