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
import io.fabric8.kubernetes.api.model.Service;

/**
 * {@link KubernetesDriveClientService} is an abstraction which provides access to
 * drive client services running inside a kubernetes cluster.
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */

public class KubernetesDriveClientService implements DriveClientService {

    private final String serviceHostname;

    /**
     * Initializes a new {@link KubernetesDriveClientService}.
     *
     * @param v1Service The kubernetes software service model
     */
    public KubernetesDriveClientService(Service v1Service) {
        super();

        if (v1Service == null) {
            serviceHostname = "";
        } else {
            StringBuilder builder = new StringBuilder();
            serviceHostname = builder.append(v1Service.getMetadata().getName())
                                       .append(".")
                                       .append(v1Service.getMetadata().getNamespace())
                                       .append(".svc.cluster.local").toString();
        }

    }

    @Override
    public URL getManifestURL() throws OXException {
        try {
            return getKubernetesManifestUrlAndReplace();
        } catch (MalformedURLException mue) {
            throw new OXException(mue);
        }
    }


    /**
     * Create the service manifest url and replace placeholders if needed.
     * <p/>
     * @see {@link DriveClientProperty#ROUTING_KUBERNETES_MANIFESTURL}
     *
     * @return The service manifest URL
     * @throws OXException if {@link LeanConfigurationService} is unavailable
     * @throws MalformedURLException if URL could not be created
     */
    private URL getKubernetesManifestUrlAndReplace() throws MalformedURLException, OXException {
        LeanConfigurationService leanConfigService = Services.getServiceSafe(LeanConfigurationService.class);
        String manifestUrl = leanConfigService.getProperty(DriveClientProperty.ROUTING_KUBERNETES_MANIFESTURL);

        return new URL (manifestUrl
                         .replace(Constants.URL_PACEHOLDER_SVC_HOST, serviceHostname)
                         .replace(Constants.URL_PACEHOLDER_SVC_MANFEST , "manifest.json"));
    }

}
