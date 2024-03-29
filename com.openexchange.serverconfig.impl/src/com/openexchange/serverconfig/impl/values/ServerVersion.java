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

package com.openexchange.serverconfig.impl.values;

import java.util.Map;
import com.openexchange.serverconfig.ComputedServerConfigValueService;
import com.openexchange.session.Session;
import com.openexchange.version.VersionService;

/**
 * {@link ServerVersion}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class ServerVersion implements ComputedServerConfigValueService {

    private VersionService versionService;

    /**
     * Initializes a new {@link ServerVersion}.
     */
    public ServerVersion(VersionService versionService) {
        super();
        this.versionService = versionService;
    }

    @Override
    public void addValue(Map<String, Object> serverConfig, String hostName, int userId, int contextId, Session optSession) {

        if (!serverConfig.containsKey("serverVersion")) {
            serverConfig.put("serverVersion", versionService.getVersion().toString());
        }
    }
}
