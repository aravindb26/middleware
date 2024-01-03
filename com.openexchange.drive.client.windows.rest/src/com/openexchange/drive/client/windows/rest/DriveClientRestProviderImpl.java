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

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.drive.client.windows.rest.osgi.Services;
import com.openexchange.annotation.NonNull;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.drive.client.windows.rest.service.internal.DriveClientWindowsExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.ManagedHttpClient;

/**
 * {@link DriveClientRestProviderImpl} Use HTTP requests to fetch drive client manifests
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public class DriveClientRestProviderImpl implements DriveClientProvider {

    /** Drive client manifest cache per branding. */
    private final Cache<String, DriveManifest> manifestCache;

    /**
     * Initializes a new {@link DriveClientRestProviderImpl}.
     */
    public DriveClientRestProviderImpl() {
        super();
        manifestCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build();
    }

    @Override
    public DriveManifest getManifest(int userId, int contextId) throws OXException {
        LeanConfigurationService leanConfigService = Services.getServiceSafe(LeanConfigurationService.class);
        String branding = leanConfigService.getProperty(userId, contextId, DriveClientProperty.BRANDING_CONF);

        DriveManifest driveManifest = manifestCache.getIfPresent(branding);
        if (driveManifest != null) {
            return driveManifest;
        }

        try {
            return manifestCache.get(branding, () -> {
                DriveClientService service = getClusterMode().createService(branding);
                if (service == null) {
                    // To be safe check and throw an exception
                    throw DriveClientWindowsExceptionCodes.KUBERNETES_MISSING_SERVICE.create(branding);
                }
                URL manifestUrl = service.getManifestURL();
                return getRestClient().getManifest(manifestUrl);
            });
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof OXException) {
                throw (OXException) ee.getCause();
            }
            throw new OXException(ee);
        }
    }

    @Override
    public void invalidateCachedBranding(String branding) throws OXException {
        manifestCache.invalidate(branding);
    }

    DriveClientWindowsRestHttpClient getRestClient() throws OXException {
        return new DriveClientWindowsRestHttpClient(initDefaultClient());
    }

    /**
     * Gets an HTTP client instance matching identifier {@link DriveClientWindowsRestHttpClient#HTTP_CLIENT_ID}
     *
     * @return The HTTP client for Drive Client retrieval
     * @throws OXException If HTTP client cannot be returned
     */
    private @NonNull ManagedHttpClient initDefaultClient() throws OXException {
        return Services.getServiceLookup()
                .getServiceSafe(HttpClientService.class)
                .getHttpClient(DriveClientWindowsRestHttpClient.HTTP_CLIENT_ID);
    }


    /**
     * Gets the cluster mode.
     *
     * @return The current cluster mode
     * @throws OXException If mode is unknown
     */
    public ClusterMode getClusterMode() throws OXException {
        LeanConfigurationService leanConfigService = Services.getServiceSafe(LeanConfigurationService.class);
        String configuredClusterType = leanConfigService.getProperty(DriveClientProperty.CLUSTER_MODE);
        try {
            return ClusterMode.valueOf(configuredClusterType);
        } catch (IllegalArgumentException iae) {
            List<String> availableTypes = Stream.of(ClusterMode.values()).map(ClusterMode::name)
                                                .collect(Collectors.toList());
            throw DriveClientWindowsExceptionCodes.UNKNOWN_CLUSTER_TYPE.create(configuredClusterType, availableTypes);
        }

    }

}
