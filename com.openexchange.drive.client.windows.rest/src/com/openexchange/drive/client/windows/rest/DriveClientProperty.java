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

import com.openexchange.config.lean.Property;

/**
 * {@link DriveClientProperty}
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 *
 */
public enum DriveClientProperty implements Property {

    /**
     * The name of the drive branding identifier which will be used to find the drive client manifest.
     * <p/>
     * Property: com.openexchange.drive.client.windows.branding<br>
     * Default: "generic"<br>
     * Reloadable: true
     */
    BRANDING_CONF("com.openexchange.drive.client.windows.branding", "generic"),
    /**
     * The name of the drive updater template file.
     * <p/>
     * Property: com.openexchange.drive.client.windows.templating.updater.file<br>
     * Default: "oxdrive_update.tmpl"<br>
     * Reloadable: true
     */
    TEMPLATING_UPDATER_FILE("com.openexchange.drive.client.windows.templating.updater.file", "oxdrive_update.tmpl"),
    /**
     * The url which will be used to fetch drive client manifests when running in {@link ClusterMode#EXTERNAL}. The placeholder [branding]
     * will be replaced with the configured branding taken from com.openexchange.drive.client.windows.branding
     * <p/>
     * Property: com.openexchange.drive.client.windows.external.manifestUrl<br>
     * Default: "https://drive.example.com/appsuite/[branding]/drive/client/windows/manifest.json"<br>
     * Reloadable: true
     */
    ROUTING_EXTERNAL_MANIFESTURL("com.openexchange.drive.client.windows.external.manifestUrl", "https://drive.example.com/appsuite/[branding]/drive/client/windows/manifest.json"),
    /**
     * The url which will be used to fetch drive client manifests when running in {@link ClusterMode#KUBERNETES}. By default this will result in an URL which look like
     * <code>http://[servicename].[namespace].svc.cluster.local/manifest.json<code>
     * <p/>
     * Property: com.openexchange.drive.client.windows.kubernetes.manifestUrl<br>
     * Default: "http://[svc_host]/[svc_manifest_path]"<br>
     * Reloadable: true
     */
    ROUTING_KUBERNETES_MANIFESTURL("com.openexchange.drive.client.windows.kubernetes.manifestUrl", "http://[svc_host]/[svc_manifest_path]"),
    /**
     * The cluster mode which will be used to determine where to find valid drive client services.
     * <p/>
     * Property: com.openexchange.drive.client.windows.mode<br>
     * Default: KUBERNETES<br>
     * Reloadable: true
     */
    CLUSTER_MODE("com.openexchange.drive.client.windows.mode", "KUBERNETES"),
    /**
     * Restrict the list of returned drive client services by their labels in case of running in cluster mode {@link ClusterMode#KUBERNETES}.
     * <p/>
     * Property: com.openexchange.drive.client.windows.kubernetes.drive.service.label<br>
     * Default: "drive-client"<br>
     * Reloadable: true
     */
    KUBERNETES_DRIVE_SERVICE_LABEL("com.openexchange.drive.client.windows.kubernetes.drive.service.label", "drive-client"),
    /**
     * Restrict the list of returned drive client services by their namespace in case of running in cluster mode {@link ClusterMode#KUBERNETES}.
     * <p/>
     * Property: com.openexchange.drive.client.windows.kubernetes.drive.service.namespace<br>
     * Default: ""<br>
     * Reloadable: true
     */
    KUBERNETES_DRIVE_SERVICE_NAMESPACE("com.openexchange.drive.client.windows.kubernetes.drive.service.namespace", ""),
    /**
     * The system property containing the pod namespace. Restrict the list of returned drive client services in case of running in
     * cluster mode {@link ClusterMode#KUBERNETES}.
     * Will be used as fallback if com.openexchange.drive.client.windows.kubernetes.drive.service.namespace is empty / not configured.
     * <p/>
     * Property: com.openexchange.drive.client.windows.kubernetes.namespace.env.variable.name<br>
     * Default: "POD_NAMESPACE"<br>
     * Reloadable: true
     */
    KUBERNETES_NAMESPACE_SYSTEM_PROPERTY("com.openexchange.drive.client.windows.kubernetes.namespace.env.variable.name", "POD_NAMESPACE");


    private final String propertyName;
    private final Object defaultValue;

    private DriveClientProperty(String propertyName, Object defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return this.propertyName;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

}
