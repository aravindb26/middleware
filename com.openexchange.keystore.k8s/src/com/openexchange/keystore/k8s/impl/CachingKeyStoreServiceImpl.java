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

package com.openexchange.keystore.k8s.impl;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.keystore.KeyStoreService;
import com.openexchange.keystore.KeyStoreUtil;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher.Action;

/**
 * {@link CachingKeyStoreServiceImpl} is a {@link KeyStoreService} which caches the keystores streams
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class CachingKeyStoreServiceImpl extends AbstractKeyStoreWatcher implements KeyStoreService, Closeable {

    private static final String SECRET_LABEL = "OX_KEYSTORE";

    private static final class LoggerHolder {
        public static final Logger LOG = LoggerFactory.getLogger(CachingKeyStoreServiceImpl.class);
    }

    // @formatter:off
    final LoadingCache<String, Optional<byte[]>> cache = CacheBuilder.newBuilder()
                                                                     .maximumSize(100)
                                                                     .build(new CacheLoader<String, Optional<byte[]>>() {
                                                                                   @Override
                                                                                   public Optional<byte[]> load(String key) throws Exception {
                                                                                       return loadSecret(key);
                                                                                   }
                                                                            });
    // @formatter:on

    /**
     * Initializes a new {@link CachingKeyStoreServiceImpl}.
     */
    public CachingKeyStoreServiceImpl() {
        super(SECRET_LABEL);
    }

    @Override
    public Optional<byte[]> optSecret(String keystoreId) {
        if (Strings.isEmpty(keystoreId)) {
            return Optional.empty();
        }
        try {
            return cache.get(keystoreId);
        } catch (ExecutionException e) {
            // unexpected error
            LoggerHolder.LOG.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<KeyStore> optKeyStore(String keystoreId, Optional<String> optType, Optional<String> optPassword) throws OXException {
        Optional<byte[]> optKeystoreData = optSecret(keystoreId);
        if (optKeystoreData.filter(bytes -> bytes.length > 0).isPresent() == false) {
            return Optional.empty();
        }
        return Optional.ofNullable(KeyStoreUtil.toKeyStore(new ByteArrayInputStream(optKeystoreData.get()), optType, optPassword));
    }

    @Override
    void handleChangedKeystore(Action action, String id, Secret secret) {
        cache.invalidate(id);
    }

    // ------------------------------- internal methods ---------------------------------------------

    /**
     * Loads a secret from k8s if not cached
     *
     * @param keystoreId The keystore id
     * @return The optional secret
     * @throws OXException in case an error occurred while accessing the k8s api
     */
    private Optional<byte[]> loadSecret(String keystoreId) throws OXException {
        try {
            // @formatter:off
            return getClient().secrets()
                              .withLabel(SECRET_LABEL, keystoreId)
                              .list()
                              .getItems()
                              .stream()
                              .findAny()
                              .map(secret -> getKeyStoreFromSecret(secret));
            // @formatter:on
        } catch (KubernetesClientException e) {
            throw OXException.general("Unexpected kubernetes client exception", e);
        }
    }

    /**
     * Extracts the keystore data from a given {@link Secret}
     *
     * @param secret The secret
     * @return The keystore data
     */
    byte[] getKeyStoreFromSecret(Secret secret) {
        // @formatter:off
        return secret.getData()
                     .entrySet()
                     .stream()
                     .findFirst()
                     .map(entry -> Base64.getDecoder().decode(entry.getValue()))
                     .orElse(new byte[0]);
        //@formatter:on
    }

}
