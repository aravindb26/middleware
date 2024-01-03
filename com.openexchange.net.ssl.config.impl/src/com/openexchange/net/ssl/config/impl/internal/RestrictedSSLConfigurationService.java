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

package com.openexchange.net.ssl.config.impl.internal;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.net.ssl.config.TrustLevel;
import com.openexchange.net.ssl.config.TrustStoreIdAwareSSLConfigurationService;

/**
 * The {@link RestrictedSSLConfigurationService} provides user specific configuration with regards to SSL
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.8.3
 */
public class RestrictedSSLConfigurationService implements TrustStoreIdAwareSSLConfigurationService {

    private final TrustLevel trustLevel;
    private final AtomicReference<RestrictedConfig> configReference;
    private final boolean hostnameVerificationEnabled;
    private final boolean defaultTruststoreEnabled;
    private final boolean customTruststoreEnabled;
    private final String customTruststoreLocation;
    private final String customTruststoreId;
    private final String customTruststorePassword;

    /**
     * Initializes a new {@link RestrictedSSLConfigurationService}.
     *
     * @param trustLevel The trust level
     * @param configService The service to use
     */
    public RestrictedSSLConfigurationService(TrustLevel trustLevel, LeanConfigurationService configService) {
        super();
        this.trustLevel = trustLevel;
        this.configReference = new AtomicReference<RestrictedConfig>(SSLProperties.newConfig(configService));

        // The immutable configuration properties
        this.hostnameVerificationEnabled = configService.getBooleanProperty(SSLProperties.HOSTNAME_VERIFICATION_ENABLED);
        this.defaultTruststoreEnabled = configService.getBooleanProperty(SSLProperties.DEFAULT_TRUSTSTORE_ENABLED);
        this.customTruststoreEnabled = configService.getBooleanProperty(SSLProperties.CUSTOM_TRUSTSTORE_ENABLED);
        this.customTruststoreLocation = configService.getProperty(SSLProperties.CUSTOM_TRUSTSTORE_LOCATION);
        this.customTruststoreId = configService.getProperty(SSLProperties.CUSTOM_TRUSTSTORE_ID);
        this.customTruststorePassword = configService.getProperty(SSLProperties.CUSTOM_TRUSTSTORE_PASSWORD);
    }

    /**
     * Reloads the configuration
     */
    public void reload(LeanConfigurationService configService) {
        RestrictedConfig existing;
        RestrictedConfig newConfig;
        do {
            existing = configReference.get();
            newConfig = SSLProperties.newConfig(configService);
        } while (!configReference.compareAndSet(existing, newConfig));
    }

    @Override
    public boolean isWhitelisted(String hostName) {
        RestrictedConfig config = configReference.get();
        return config.isWhitelisted(hostName);
    }

    @Override
    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    @Override
    public String[] getSupportedProtocols() {
        RestrictedConfig config = configReference.get();
        return config.getProtocols();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        RestrictedConfig config = configReference.get();
        return config.getCiphers();
    }

    @Override
    public boolean isVerifyHostname() {
        return hostnameVerificationEnabled;
    }

    @Override
    public boolean isDefaultTruststoreEnabled() {
        return defaultTruststoreEnabled;
    }

    @Override
    public boolean isCustomTruststoreEnabled() {
        return customTruststoreEnabled;
    }

    @Override
    public String getCustomTruststoreLocation() {
        return customTruststoreLocation;
    }

    @Override
    public String getCustomTruststorePassword() {
        return customTruststorePassword;
    }

    @Override
    public String getCustomTruststoreId() {
        return customTruststoreId;
    }
}
