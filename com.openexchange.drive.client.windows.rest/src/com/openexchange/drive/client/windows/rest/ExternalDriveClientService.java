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

import java.net.MalformedURLException;
import java.net.URL;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.drive.client.windows.rest.osgi.Services;
import com.openexchange.exception.OXException;

/**
 * {@link ExternalDriveClientService} is an abstraction which provides access to
 * drive client services hosted externally.
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */

public class ExternalDriveClientService implements DriveClientService {

    private final String branding;

    /**
     * Initializes a new {@link ExternalDriveClientService}.
     */
    public ExternalDriveClientService(String branding) {
        super();
        this.branding = branding;
    }

    @Override
    public URL getManifestURL() throws OXException {
        try {
            return getExternalManifestUrlAndReplace();
        } catch (MalformedURLException mue) {
            throw new OXException(mue);
        }
    }


    /**
     * Create the external manifest url and replace the branding placeholder if needed.
     * <p/>
     * @see {@link DriveClientProperty#ROUTING_EXTERNAL_MANIFESTURL}
     *
     * @return The manifest URL
     * @throws OXException if {@link LeanConfigurationService} is unavailable
     * @throws MalformedURLException if URL could not be created
     */
    private URL getExternalManifestUrlAndReplace() throws MalformedURLException, OXException {
        LeanConfigurationService leanConfigService = Services.getServiceSafe(LeanConfigurationService.class);
        String manifestUrl = leanConfigService.getProperty(DriveClientProperty.ROUTING_EXTERNAL_MANIFESTURL);

        if (manifestUrl.contains(Constants.URL_PLACEHOLDER_BRANDING)) {
            return new URL (manifestUrl.replace(Constants.URL_PLACEHOLDER_BRANDING, this.branding));
        }
        return new URL(manifestUrl);
    }

}
