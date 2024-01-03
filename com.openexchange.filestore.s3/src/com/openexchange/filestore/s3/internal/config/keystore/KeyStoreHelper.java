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

package com.openexchange.filestore.s3.internal.config.keystore;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadables;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.keystore.KeyStoreProvider;
import com.openexchange.keystore.KeyStoreService;

/**
 * {@link KeyStoreHelper}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class KeyStoreHelper implements ForcedReloadable {

    private static final Logger LOG = LoggerFactory.getLogger(KeyStoreHelper.class);

    private final LeanConfigurationService leanService;
    private final Map<String, ConfigAwareKeyStoreProviderImpl> providers = new ConcurrentHashMap<>();

    private final KeyStoreService keyStoreService;

    /**
     * Initializes a new {@link KeyStoreHelper}.
     *
     * @param leanService The {@link LeanConfigurationService}
     * @param keyStoreService The keystore service
     */
    public KeyStoreHelper(LeanConfigurationService leanService, KeyStoreService keyStoreService) {
        super();
        Objects.requireNonNull(keyStoreService);
        Objects.requireNonNull(leanService);
        this.leanService = leanService;
        this.keyStoreService = keyStoreService;
    }

    public KeyStoreProvider createProvider(KeystoreProviderConfig config) throws OXException {
        ConfigAwareKeyStoreProviderImpl cachedProvider = providers.get(config.getId());
        if (cachedProvider != null) {
            return cachedProvider;
        }
        ConfigAwareKeyStoreProviderImpl result = new ConfigAwareKeyStoreProviderImpl(config, leanService, keyStoreService);
        ConfigAwareKeyStoreProviderImpl existingProvider = providers.putIfAbsent(config.getId(), result);
        return existingProvider == null ? result : existingProvider;
    }

    @Override
    public Interests getInterests() {
        return Reloadables.getInterestsForAll();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        // @formatter:off
        providers.values()
                 .stream()
                 .forEach(provider -> reload(provider));
        // @formatter:on
    }

    /**
     * Reloads a given {@link ConfigAwareKeyStoreProviderImpl}.
     *
     * @param ksProvider The {@link ConfigAwareKeyStoreProviderImpl} to reload
     */
    private void reload(ConfigAwareKeyStoreProviderImpl ksProvider) {
        try {
            ksProvider.reloadAndNotify(false);
        } catch (OXException e) {
            LOG.error("Unable to reload keystore: " + e.getMessage(), e);
        }
    }

}
