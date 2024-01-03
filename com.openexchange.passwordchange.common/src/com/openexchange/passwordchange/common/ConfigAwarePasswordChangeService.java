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

package com.openexchange.passwordchange.common;

import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * 
 * {@link ConfigAwarePasswordChangeService}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.4
 */
public abstract class ConfigAwarePasswordChangeService extends AbstractPasswordChangeService {

    /**
     * Initializes a new {@link ConfigAwarePasswordChangeService}.
     * 
     * @param services The services
     * @throws OXException If services are missing
     */
    public ConfigAwarePasswordChangeService(ServiceLookup services) throws OXException {
        super(services);
    }

    @Override
    public boolean isEnabled(int contextId, int userId) {
        return configService.getBooleanProperty(userId, contextId, PasswordChangeProperties.ENABLED, Map.of("providerId", getProviderId()));
    }

    /**
     * Get the provider identifier of the service
     * <p>
     * Used for property lookups
     *
     * @return The provider identifier
     */
    protected abstract String getProviderId();

}
