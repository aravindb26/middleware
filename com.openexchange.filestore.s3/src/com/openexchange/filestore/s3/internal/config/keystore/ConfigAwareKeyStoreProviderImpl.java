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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.keystore.KeyStoreProvider;
import com.openexchange.keystore.KeyStoreService;
import com.openexchange.keystore.KeyStoreUtil;

/**
 * {@link ConfigAwareKeyStoreProviderImpl} is a provider for a single keystore which access is defined via properties.
 *
 * This provider supports file based certificates as well as {@link KeyStoreService} based keystores.
 * It can also be configured to use both. Inm that case the {@link KeyStoreService} is used first and the file based keystore is used as a fallback.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class ConfigAwareKeyStoreProviderImpl implements KeyStoreProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigAwareKeyStoreProviderImpl.class);

    private static final KeyStoreInfo NO_KEYSTORE = new KeyStoreInfo(null, null);

    private final KeystoreProviderConfig config;
    private final LeanConfigurationService leanconfigService;
    private final AtomicReference<KeyStoreInfo> storeReference = new AtomicReference<KeyStoreInfo>(NO_KEYSTORE);

    private Optional<String> optKeyStorePassword = Optional.empty();

    private final KeyStoreService keyStoreService;

    /**
     * Initializes a new {@link ConfigAwareKeyStoreProviderImpl}.
     *
     * @param config The configuration
     * @param leanConfigurationService The {@link LeanConfigurationService}
     * @param keyStoreService The keystore service
     * @throws OXException If keystore can't be accessed
     * @throws {@link NullPointerException} if configuration or service is missing
     */
    ConfigAwareKeyStoreProviderImpl(KeystoreProviderConfig config, LeanConfigurationService leanConfigurationService, KeyStoreService keyStoreService) throws OXException, NullPointerException {
        Objects.requireNonNull(leanConfigurationService);
        Objects.requireNonNull(config);
        Objects.requireNonNull(keyStoreService);
        this.config = config;
        this.leanconfigService = leanConfigurationService;
        this.keyStoreService = keyStoreService;
        reloadInternal();
    }

    /**
     * Reloads the {@link KeyStore} held by this instance.
     *
     * @param forceReload Whether to force reload or not
     * @return <code>true</code> if the underlying keystore changed, <code>false</code> otherwise
     * @throws OXException if the keystore could not be loaded
     */
    public synchronized boolean reloadAndNotify(boolean forceReload) throws OXException {
        if (forceReload == false && config.isReloadManually()) {
            return false;
        }
        boolean result = reloadInternal();
        if (result) {
            config.optChangeListener().ifPresent(l -> l.notify(optKeyStore()));
        }
        return result;
    }

    @Override
    public boolean reload() throws OXException {
        return reloadAndNotify(true);
    }

    /**
     * (Re)-loads the keystore
     *
     * @return <code>true</code> if the keystore changed, <code>false</code> otherwise
     * @throws OXException If keystore can't be accessed
     */
    private boolean reloadInternal() throws OXException {
        KeyStore store = null;
        byte[] currentMD5 = null;
        optKeyStorePassword = config.optKeyStorePasswordProperty().map(prop -> leanconfigService.getProperty(prop));
        DigestInputStream in = null;
        try {
            Optional<String> id = config.optKeyStoreIdProperty().map(prop -> leanconfigService.getProperty(prop));

            if (id.isPresent()) {
                // Try loading with help of the KeystoreService
                Optional<byte[]> keyStoreData = keyStoreService.optSecret(id.get());
                if (keyStoreData.isPresent()) {
                    in = new DigestInputStream(new ByteArrayInputStream(keyStoreData.get()), MessageDigest.getInstance("MD5"));
                } else {
                    LOG.warn("Keystore id {} is configured but no keystore with that id can be found. Falling back to file based loading.", id);
                }
            }

            if (in == null) {
                // The keystore id is either null or the keystore couldn`t be loaded. Trying to load it via local keystore file instead
                String keystorePath = config.optKeyStorePathProperty().map(prop -> leanconfigService.getProperty(prop)).orElse(null);

                // Remove current keystore if path is not configured anymore
                if (Strings.isEmpty(keystorePath)) {
                    if (NO_KEYSTORE == storeReference.get()) {
                        // No store configured
                        return false;
                    }

                    // Keystore was removed
                    storeReference.set(NO_KEYSTORE);
                    return true;
                }

                File keyStoreFile = new File(stripPath(keystorePath));
                if (false == keyStoreFile.exists() || false == keyStoreFile.isFile()) {
                    throw new FileNotFoundException("The key store does not exist.");
                }
                in = new DigestInputStream(new FileInputStream(keyStoreFile), MessageDigest.getInstance("MD5"));
            }

            // Convert byte stream to keystore
            Optional<String> type = config.optKeyStoreType().isPresent() ? config.optKeyStoreType() : config.optKeyStoreTypeProperty().map(prop -> leanconfigService.getProperty(prop));
            store = KeyStoreUtil.toKeyStore(in, type, optKeyStorePassword);
            currentMD5 = in.getMessageDigest().digest();

            if (false == MessageDigest.isEqual(currentMD5, storeReference.get().getMd5Sum())) {
                storeReference.set(new KeyStoreInfo(store, currentMD5));
                return true;
            }
            return false;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw OXException.general("Unable to access key store: " + e.getMessage(), e);
        } finally {
            Streams.close(in);
        }
    }

    private boolean isConfigured() {
        KeyStoreInfo storeInfo = storeReference.get();
        return storeInfo.getMd5Sum() != null && null != storeInfo.getKeystore();
    }

    @Override
    public Optional<KeyStore> optKeyStore() {
        return isConfigured() ? Optional.ofNullable(storeReference.get().getKeystore()) : Optional.empty();
    }

    private String stripPath(String keystorePath) {
        return keystorePath.substring(keystorePath.lastIndexOf(":") + 1, keystorePath.length());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(ConfigAwareKeyStoreProviderImpl.class.getName());
        KeyStoreInfo storeInfo = storeReference.get();
        sb.append("=[storeHash:").append(Arrays.toString(storeInfo.getMd5Sum()));
        config.optKeyStoreIdProperty().ifPresent(id -> sb.append(",keystoreIdProperty:").append(id.getFQPropertyName()));
        config.optKeyStorePathProperty().ifPresent(path -> sb.append(",keystorePathPropertyName:").append(path.getFQPropertyName()));
        config.optKeyStorePasswordProperty().ifPresent(pass -> sb.append(",passwordPropertyName:").append(pass.getFQPropertyName()));
        sb.append(",keystore:").append(null != storeInfo.getKeystore() ? storeInfo.getKeystore().toString() : "");
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        KeyStoreInfo storeInfo = storeReference.get();
        final int prime = 31;
        int result = 1;
        result = prime * result + (storeInfo == null ? 0 : storeInfo.hashCode());
        result = prime * result + (config.getId() == null ? 0 : config.getId().hashCode());
        result = prime * result + i(config.optKeyStorePasswordProperty().map(prop -> I(prop.getFQPropertyName().hashCode())).orElse(I(0)));
        result = prime * result + i(config.optKeyStorePathProperty().map(prop -> I(prop.getFQPropertyName().hashCode())).orElse(I(0)));
        result = prime * result + i(config.optKeyStoreIdProperty().map(prop -> I(prop.getFQPropertyName().hashCode())).orElse(I(0)));
        result = prime * result + i(config.optKeyStoreTypeProperty().map(prop -> I(prop.getFQPropertyName().hashCode())).orElse(I(0)));
        result = prime * result + i(config.optKeyStoreType().map(type -> I(type.hashCode())).orElse(I(0)));
        result = prime * result + i(config.optOptionals().map(map -> I(map.hashCode())).orElse(I(0)));
        result = prime * result + (config.isReloadManually() ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConfigAwareKeyStoreProviderImpl other = (ConfigAwareKeyStoreProviderImpl) obj;
        KeyStoreInfo storeInfo = storeReference.get();
        KeyStoreInfo otherStoreInfo = other.storeReference.get();
        if (storeInfo == null) {
            if (otherStoreInfo != null) {
                return false;
            }
        } else if (!storeInfo.equals(otherStoreInfo)) {
            return false;
        }
        if (config.optKeyStorePasswordProperty().equals(other.config.optKeyStorePasswordProperty()) == false) {
            return false;
        }
        if (config.optKeyStorePathProperty().equals(other.config.optKeyStorePathProperty()) == false) {
            return false;
        }
        if (config.optKeyStoreIdProperty().equals(other.config.optKeyStoreIdProperty()) == false) {
            return false;
        }
        return true;
    }

    @Override
    public String getId() {
        return config.getId();
    }

    @Override
    public Optional<String> optPassword() {
        return optKeyStorePassword;
    }

}

