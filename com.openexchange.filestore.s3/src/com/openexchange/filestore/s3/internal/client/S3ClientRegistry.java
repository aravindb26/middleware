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

package com.openexchange.filestore.s3.internal.client;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.s3.internal.S3FileStorage;
import com.openexchange.filestore.s3.internal.config.S3ClientConfig;
import com.openexchange.filestore.s3.internal.config.S3ClientProperty;
import com.openexchange.filestore.s3.internal.config.S3Property;
import com.openexchange.filestore.s3.internal.config.keystore.KeyStoreChangeListener;
import com.openexchange.filestore.s3.internal.config.keystore.KeyStoreHelper;
import com.openexchange.server.ServiceLookup;

/**
 * {@link S3ClientRegistry}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.4
 */
public class S3ClientRegistry implements Reloadable {

    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(S3ClientRegistry.class);
    }

    private final S3ClientFactory clientFactory;
    private final ServiceLookup services;
    private final Cache<String, S3FileStorageClient> clients;
    private final KeyStoreHelper helper;

    /**
     * Initializes a new {@link S3ClientRegistry}.
     *
     * @param services The {@link ServiceLookup} containing a {@link LeanConfigurationService}
     * @param helper The {@link KeyStoreHelper}
     */
    public S3ClientRegistry(ServiceLookup services, KeyStoreHelper helper) {
        super();
        this.clientFactory = new S3ClientFactory();
        this.services = services;
        this.helper = helper;
        clients = CacheBuilder.newBuilder()
            .weakValues()
            .removalListener(new RemovalListener<String, S3FileStorageClient>() {

                @Override
                public void onRemoval(RemovalNotification<String, S3FileStorageClient> notification) {
                    S3FileStorageClient s3FileStorageClient = notification.getValue();
                    if (s3FileStorageClient != null) {
                        s3FileStorageClient.getSdkClient().shutdown();
                    }
                }
            })
            .build();
    }

    /**
     * Get an {@link S3FileStorageClient} for the given client configuration. If none exists yet a new instance is created
     * and put into the registry. Instances are weakly referenced by the registry and vanish after the last strong reference
     * to them was garbage collected. Make sure to include the returned instance as member to the respective {@link S3FileStorage}
     * instance.
     *
     * @param clientConfig The client configuration
     * @return The already existing or new {@link S3FilestoreClient}
     * @throws OXException If S3 storage client cannot be created
     */
    public S3FileStorageClient getOrCreate(S3ClientConfig clientConfig) throws OXException {
        String key = clientConfig.getClientID().orElse(clientConfig.getFilestoreID());
        S3FileStorageClient client = clients.getIfPresent(key);
        if (client == null) {
            try {
                client = clients.get(key, createLoaderFor(clientConfig, keystore -> clients.invalidate(key)));
            } catch (ExecutionException | UncheckedExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof OXException) {
                    throw (OXException) cause;
                }
                throw OXException.general("Failed initializing S3 client.", cause);
            }
        }
        return client;
    }

    private Callable<? extends S3FileStorageClient> createLoaderFor(S3ClientConfig clientConfig, KeyStoreChangeListener listener) {
        return new S3ClientLoaderCallable(clientConfig, this.clientFactory, this.helper, listener);
    }

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().propertiesOfInterest(
            S3ClientProperty.getInterestsWildcard(),
            S3Property.getInterestsWildcard())
            .build();
    }

    @Override
    public void reloadConfiguration(ConfigurationService notUsed) {
        try {
            LeanConfigurationService configService = services.getServiceSafe(LeanConfigurationService.class);
            List<S3FileStorageClient> toRemove = new LinkedList<>();
            ConcurrentMap<String, S3FileStorageClient> clientsMap = clients.asMap();
            for (Map.Entry<String, S3FileStorageClient> entry : clientsMap.entrySet()) {
                S3FileStorageClient client = entry.getValue();
                int recentFingerprint = S3ClientConfig.getFingerprint(configService, client.getScope(), entry.getKey());
                if (recentFingerprint != client.getConfigFingerprint()) {
                    toRemove.add(client);
                }
            }

            for (S3FileStorageClient client : toRemove) {
                clientsMap.remove(client.getKey(), client);
            }
        } catch (Exception e) {
            LoggerHolder.LOG.error("Configuration reload failed for S3 clients", e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class S3ClientLoaderCallable implements Callable<S3FileStorageClient> {

        private final S3ClientConfig clientConfig;
        private final S3ClientFactory clientFactory;
        private final KeyStoreChangeListener listener;
        private final KeyStoreHelper helper;

        /**
         * Initializes a new {@link S3ClientLoaderCallable}.
         *
         * @param clientConfig
         * @param clientFactory
         * @param helper
         * @param listener
         */
        S3ClientLoaderCallable(S3ClientConfig clientConfig, S3ClientFactory clientFactory, KeyStoreHelper helper, KeyStoreChangeListener listener) {
            this.clientConfig = clientConfig;
            this.clientFactory = clientFactory;
            this.listener = listener;
            this.helper = helper;
        }

        @Override
        public S3FileStorageClient call() throws Exception {
            return clientFactory.initS3Client(clientConfig, helper, listener);
        }
    }

}
