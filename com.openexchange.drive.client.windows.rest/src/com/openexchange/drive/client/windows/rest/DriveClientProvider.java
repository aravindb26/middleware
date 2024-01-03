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

package com.openexchange.drive.client.windows.rest;

import com.openexchange.exception.OXException;

/**
 * {@link DriveClientProvider}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public interface DriveClientProvider {

    /**
     * Perform a call to the drive service manifest endpoint
     *
     * @param userId The user id
     * @param contextId The context id
     *
     * @return The manifest as java object
     * @throws OXException If HTTP endpoint is not reachable or response could not be consumed
     */
    DriveManifest getManifest(int userId, int contextId) throws OXException;

    /**
     * Invalidate a cached manifest for the given branding
     *
     * @param branding The branding
     *
     */
    void invalidateCachedBranding(String branding) throws OXException;

}
