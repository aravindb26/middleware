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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.drive.client.windows.rest.osgi.Services;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsExceptionCodes;
import com.openexchange.exception.OXException;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * {@link ClusterMode} The cluster mode identifies the way drive client services will be created.
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 *
 */
public enum ClusterMode {

    /**
     *  With this setting in place kubernetes API will be called to find the
     *  available drive client services. The subsequent calls to fetch the branded manifests will be made using these services
     */
    KUBERNETES() {
        @Override
        public DriveClientService createService(String branding) throws OXException {
            LeanConfigurationService leanConfigService = Services.getServiceSafe(LeanConfigurationService.class);

            String label = leanConfigService.getProperty(DriveClientProperty.KUBERNETES_DRIVE_SERVICE_LABEL);
            String namespace = leanConfigService.getProperty(DriveClientProperty.KUBERNETES_DRIVE_SERVICE_NAMESPACE);
            //If namespace is not configured or empty try to retrieve current pod`s release namespace via environment variable
            if (Strings.isNullOrEmpty(namespace)) {
                namespace = System.getenv(leanConfigService.getProperty(DriveClientProperty.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY));
            }

            if (Strings.isNullOrEmpty(namespace)) {
                throw DriveClientWindowsExceptionCodes.MISSING_NAMESPACE.create();
            }

            try (final KubernetesClient client = new DefaultKubernetesClient()) {
                LOG.debug("Fetch services from namespace {} with label {}", namespace, label);
                ServiceList services = client.services()
                        .inNamespace(namespace)
                        .withLabel(label).list();
                if (null == services || services.getItems().size() == 0) {
                    throw DriveClientWindowsExceptionCodes.KUBERNETES_MISSING_SERVICES.create(namespace, label);
                }
                for (Service s : services.getItems()) {
                    if (branding.equals(s.getMetadata().getLabels().get(label))) {
                        LOG.debug("Add service {} fetched for branding {} to local cache.",
                            branding, s.getMetadata().getName());
                        return new KubernetesDriveClientService(s);
                    }
                }
                LOG.debug("Could not find any service in namespace {} with label {}", namespace, label);
                throw DriveClientWindowsExceptionCodes.KUBERNETES_MISSING_SERVICE.create(branding);
            } catch (KubernetesClientException kce) {
                throw DriveClientWindowsExceptionCodes.KUBERNETES_ERROR.create(kce.getCause(), kce.getMessage());
            }

        }

    },

    /**
     * With this setting in place there is no kubernetes API call. Instead a generic service will
     * be used which get the branded manifests from a configurable host. This host needs to be accessible via HTTP.
     */
    EXTERNAL() {
        @Override
        public DriveClientService createService(String branding) throws OXException {
            return new ExternalDriveClientService(branding);
        }

    };

    static final Logger LOG = LoggerFactory.getLogger(ClusterMode.class);

    /**
     * Create a new drive client service for the given branding
     *
     * @param branding The branding
     *
     * @return The drive client service
     * @throws OXException In case service could not be created
     */
    public abstract DriveClientService createService(String branding)
        throws OXException;

}
