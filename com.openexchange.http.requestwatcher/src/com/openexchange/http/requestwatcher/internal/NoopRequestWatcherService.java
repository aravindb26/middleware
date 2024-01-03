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

package com.openexchange.http.requestwatcher.internal;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.openexchange.http.requestwatcher.osgi.services.RequestRegistryEntry;
import com.openexchange.http.requestwatcher.osgi.services.RequestWatcherService;


/**
 * {@link NoopRequestWatcherService} - The request watch doing nothing at all.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class NoopRequestWatcherService implements RequestWatcherService {

    /**
     * Initializes a new {@link NoopRequestWatcherService}.
     */
    public NoopRequestWatcherService() {
        super();
    }

    @Override
    public RequestRegistryEntry registerRequest(HttpServletRequest request, HttpServletResponse response, Thread thread, Map<String, String> propertyMap) {
        return null;
    }

    @Override
    public boolean unregisterRequest(RequestRegistryEntry registryEntry) {
        return false;
    }

    @Override
    public boolean stopWatching() {
        return true;
    }

}
