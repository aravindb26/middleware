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

package com.openexchange.subscribe.dav.httpclient.properties;

import org.apache.http.protocol.HttpContext;
import com.openexchange.rest.client.httpclient.util.LocationCheckingRedirectStrategy;

/**
 * {@link DAVSubscribeRedirectStrategy}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class DAVSubscribeRedirectStrategy extends LocationCheckingRedirectStrategy {

    private static final DAVSubscribeRedirectStrategy MYINSTANCE = new DAVSubscribeRedirectStrategy();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static DAVSubscribeRedirectStrategy getInstance() {
        return MYINSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private DAVSubscribeRedirectStrategy() {
        super();
    }

    @Override
    protected boolean isInternalHostAllowed(HttpContext context) {
        return false;
    }
}
