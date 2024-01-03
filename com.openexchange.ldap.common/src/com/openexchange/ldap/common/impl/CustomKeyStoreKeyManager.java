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

package com.openexchange.ldap.common.impl;

import java.io.Serializable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.unboundid.util.CryptoHelper;
import com.unboundid.util.Validator;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.WrapperKeyManager;

/**
 * {@link CustomKeyStoreKeyManager} is a alternative implementation of the {@link KeyStoreKeyManager} which allows to provide the keystore directly
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class CustomKeyStoreKeyManager extends WrapperKeyManager implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(CustomKeyStoreKeyManager.class);

    private static final long serialVersionUID = 2113776571569138561L;

    /**
     * Creates a new instance
     *
     * @param keyStore The keystore to use
     * @param keyStorePassword The password to use to access the contents of the key
     *            store. It may be {@code null} if no password is required.
     * @param certificateAlias The alias of the certificate that should be selected.
     *            It may be {@code null} if any acceptable certificate found in the
     *            {@link KeyStore} may be used.
     * @param validateKeyStore Indicates whether to validate that the provided
     *            key store is acceptable and can actually be used
     *            to obtain a valid certificate. If a certificate
     *            alias was specified, then this will ensure that
     *            the key store contains a valid private key entry
     *            with that alias. If no certificate alias was
     *            specified, then this will ensure that the key
     *            store contains at least one valid private key
     *            entry.
     *
     * @throws KeyStoreException If a problem occurs while initializing or if validation fails
     */
    public CustomKeyStoreKeyManager(final KeyStore keyStore, final char[] keyStorePassword, final String certificateAlias, final boolean validateKeyStore) throws KeyStoreException {
        super(getKeyManagers(keyStore, keyStorePassword, certificateAlias, validateKeyStore), certificateAlias);
    }

    /**
     * Retrieves the set of {@link KeyManager}s that will be wrapped by this {@link KeyManager}.
     *
     * @param ks The {@link KeyStore} to use
     * @param keyStorePassword The password to use to access the contents of the key
     *            store. It may be {@code null} if no password is required.
     * @param certificateAlias The nickname of the certificate that should be
     *            selected. It may be {@code null} if any acceptable certificate
     *            found in the {@link KeyStore} may be used.
     * @param validateKeyStore Indicates whether to validate that the provided
     *            key store is acceptable and can actually be used
     *            to obtain a valid certificate. If a certificate
     *            alias was specified, then this will ensure that
     *            the key store contains a valid private key entry
     *            with that alias. If no certificate alias was
     *            specified, then this will ensure that the key
     *            store contains at least one valid private key
     *            entry.
     *
     * @return The set of key managers that will be wrapped by this key manager.
     *
     * @throws KeyStoreException If a problem occurs while initializing this key
     *             manager, or if validation fails.
     */
    private static KeyManager[] getKeyManagers(final KeyStore ks, final char[] keyStorePassword, final String certificateAlias, final boolean validateKeyStore) throws KeyStoreException {
        Validator.ensureNotNull(ks);

        if (validateKeyStore) {
            validateKeyStore(ks, keyStorePassword, certificateAlias);
        }

        try {
            final KeyManagerFactory factory = CryptoHelper.getKeyManagerFactory();
            factory.init(ks, keyStorePassword);
            return factory.getKeyManagers();
        } catch (final Exception e) {
            throw new KeyStoreException(LDAPCommonErrorCodes.INVALID_CONFIG.create(e));
        }
    }

    /**
     * Validates that the provided key store has an appropriate private key entry
     * in which all certificates in the chain are currently valid
     *
     * @param keyStore The key store to examine. It must not be {@code null}.
     * @param keyStorePassword The password to use to access the contents of the {@link KeyStore}. It may be {@code null} if no password is required.
     * @param certificateAlias The nickname of the certificate that should be selected. It may be {@code null} if any
     *            acceptable certificate found in the {@link KeyStore} may be used.
     *
     * @throws KeyStoreException If a validation error was encountered.
     */
    private static void validateKeyStore(final KeyStore keyStore, final char[] keyStorePassword, final String certificateAlias) throws KeyStoreException {
        final KeyStore.ProtectionParameter protectionParameter;
        if (keyStorePassword == null) {
            protectionParameter = null;
        } else {
            protectionParameter = new KeyStore.PasswordProtection(keyStorePassword);
        }

        try {
            if (certificateAlias == null) {
                final StringBuilder invalidMessages = new StringBuilder();
                final Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    final String alias = aliases.nextElement();
                    if (!keyStore.isKeyEntry(alias)) {
                        continue;
                    }

                    try {
                        ensureAllCertificatesInChainAreValid((KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, protectionParameter));

                        // We found a private key entry in which all certificates in the
                        // chain are within their validity window, so we'll assume that
                        // it's acceptable.
                        return;
                    } catch (final Exception e) {
                        if (invalidMessages.length() > 0) {
                            invalidMessages.append("  ");
                        }
                        invalidMessages.append(e.getMessage());
                    }
                }

                if (invalidMessages.length() > 0) {
                    LOG.error("Unbale to find a valid certificate chain: {}", invalidMessages.toString());
                } else {
                    LOG.error("No certificates found the given keystore");
                }
                throw new KeyStoreException(LDAPCommonErrorCodes.INVALID_CONFIG.create());
            }

            // Check certificate alias
            if (false == keyStore.containsAlias(certificateAlias) || false == keyStore.isKeyEntry(certificateAlias)) {
                LOG.error("No key certificate with the alias {} found", certificateAlias);
                throw new KeyStoreException(LDAPCommonErrorCodes.INVALID_CONFIG.create());
            }

            final KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(certificateAlias, protectionParameter);
            ensureAllCertificatesInChainAreValid(entry);
        } catch (final KeyStoreException e) {
            throw e;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException e) {
            LOG.error("Unable to retrieve certificate with alias {}", certificateAlias, e);
            throw new KeyStoreException(LDAPCommonErrorCodes.INVALID_CONFIG.create());
        }
    }

    private static void ensureAllCertificatesInChainAreValid(final KeyStore.PrivateKeyEntry entry) throws KeyStoreException {
        if (null == entry) {
            throw new KeyStoreException(LDAPCommonErrorCodes.INVALID_CONFIG.create());
        }
        for (final Certificate cert : entry.getCertificateChain()) {
            if (cert instanceof X509Certificate) {
                try {
                    ((X509Certificate) cert).checkValidity();
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    throw new KeyStoreException(e);
                }
            }
        }
    }

}
