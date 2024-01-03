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

import static com.openexchange.net.ssl.config.impl.internal.CipherSuiteName.nameFor;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableList;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.java.Strings;
import com.openexchange.net.HostList;
import com.openexchange.net.ssl.config.TrustLevel;

/**
 * {@link SSLProperties} include configurations made by the administrator. This means that only server wide configurations can be found here. ConfigCascade properities should not be added here.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.8.3
 */
public enum SSLProperties implements Property {

    /* Enables logging SSL details. Have a look at http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/ReadDebug.html for more details. */
    SECURE_CONNECTIONS_DEBUG_LOGS_ENABLED("com.openexchange.net.ssl.debug.logs", Boolean.FALSE),

    DEFAULT_TRUSTSTORE_ENABLED("com.openexchange.net.ssl.default.truststore.enabled", Boolean.TRUE),

    CUSTOM_TRUSTSTORE_ENABLED("com.openexchange.net.ssl.custom.truststore.enabled", Boolean.FALSE),

    CUSTOM_TRUSTSTORE_LOCATION("com.openexchange.net.ssl.custom.truststore.path", SSLProperties.EMPTY_STRING),

    CUSTOM_TRUSTSTORE_ID("com.openexchange.net.ssl.custom.truststore.id", SSLProperties.EMPTY_STRING),

    CUSTOM_TRUSTSTORE_PASSWORD("com.openexchange.net.ssl.custom.truststore.password", SSLProperties.EMPTY_STRING),

    HOSTNAME_VERIFICATION_ENABLED("com.openexchange.net.ssl.hostname.verification.enabled", Boolean.TRUE),

    TRUST_LEVEL("com.openexchange.net.ssl.trustlevel", "all"),

    PROTOCOLS("com.openexchange.net.ssl.protocols", "TLSv1, TLSv1.1, TLSv1.2"),

    CIPHERS("com.openexchange.net.ssl.ciphersuites", null),

    TRUSTSTORE_WHITELIST("com.openexchange.net.ssl.whitelist", "127.0.0.1-127.255.255.255,localhost")

    ;

    private final String propertyName;
    private Object defaultValue;

    private SSLProperties(final String propertyName, final Object defaultValue) {
        this.propertyName = propertyName;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return propertyName;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    // ------------------------------------------------------------------------------------------------------------

    private static final String EMPTY_STRING = "";

    private static final AtomicReference<List<String>> DEFAULT_CIPHER_SUITES_REFERENCE = new AtomicReference<List<String>>(null);
    private static final AtomicReference<List<String>> SUPPORTED_CIPHER_SUITES_REFERENCE = new AtomicReference<List<String>>(null);
    private static final AtomicReference<List<String>> SUPPORTED_PROTOCOLS_REFERENCE = new AtomicReference<List<String>>(null);

    /**
     * Initializes the JVM's supported cipher suites that are the names of the cipher suites, which could be enabled for use on an SSL connection.
     */
    public static void initJvmDefaults() {
        javax.net.ssl.SSLSocketFactory sslSocketFactory = (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();

        String[] jvmSupported = sslSocketFactory.getSupportedCipherSuites();
        SUPPORTED_CIPHER_SUITES_REFERENCE.set(ImmutableList.<String> copyOf(jvmSupported));

        String[] jvmDefaults = sslSocketFactory.getDefaultCipherSuites();
        DEFAULT_CIPHER_SUITES_REFERENCE.set(ImmutableList.<String> copyOf(jvmDefaults));

        // Auto-detect protocols
        List<String> protocols = new LinkedList<String>();
        // Allow the specification of a specific provider (or set?)
        for (Provider provider : Security.getProviders()) {
            for (Object prop : provider.keySet()) {
                String key = (String) prop;
                if (key.startsWith("SSLContext.") && !key.equals("SSLContext.Default") && key.matches(".*[0-9].*")) {
                    protocols.add(key.substring("SSLContext.".length()));
                } else if (key.startsWith("Alg.Alias.SSLContext.") && key.matches(".*[0-9].*")) {
                    protocols.add(key.substring("Alg.Alias.SSLContext.".length()));
                }
            }
        }
        Collections.sort(protocols); // Should give us a nice sort-order by default
        SUPPORTED_PROTOCOLS_REFERENCE.set(ImmutableList.<String> copyOf(protocols));
    }

    /**
     * Gets the names of the cipher suites which could be enabled for use on an SSL connection.
     * <p>
     * Normally, only a subset of these will actually be enabled by default, since this list may include cipher suites which do not meet quality of service requirements for those defaults.
     * Such cipher suites are useful in specialized applications.
     *
     * @return The list of cipher suites (or <code>null</code> if not initialized)
     */
    public static List<String> getSupportedCipherSuites() {
        return SUPPORTED_CIPHER_SUITES_REFERENCE.get();
    }

    /**
     * Gets the list of cipher suites which are enabled by default.
     * <p>
     * Unless a different list is enabled, handshaking on an SSL connection will use one of these cipher suites.
     * The minimum quality of service for these defaults requires confidentiality protection and server authentication (that is, no anonymous cipher suites).
     *
     * @return The list of cipher suites (or <code>null</code> if not initialized)
     */
    public static List<String> getDefaultCipherSuites() {
        return DEFAULT_CIPHER_SUITES_REFERENCE.get();
    }

    /**
     * Gets the list of protocols which could be enabled for use on an SSL connection.
     *
     * @return The list of protocols (or <code>null</code> if not initialized)
     */
    public static List<String> getSupportedProtocols() {
        return SUPPORTED_PROTOCOLS_REFERENCE.get();
    }

    /**
     * Gets the configured trust level.
     *
     * @param service The service to use
     * @return The trust level
     */
    public static TrustLevel trustLevel(LeanConfigurationService service) {
        if (null == service) {
            org.slf4j.LoggerFactory.getLogger(SSLProperties.class).info("ConfigurationService not yet available. Use default value for 'com.openexchange.net.ssl.trustlevel'.");
            return TrustLevel.TRUST_ALL;
        }
        String prop = service.getProperty(TRUST_LEVEL);
        return TrustLevel.find(prop);
    }

    /**
     * Creates a new <code>RestrictedConfig</code> instance reading configuration from specified service.
     *
     * @param service The service to use
     * @return The created <code>RestrictedConfig</code> instance
     */
    public static RestrictedConfig newConfig(LeanConfigurationService service) {
        String[] protocols;
        {
            String prop = service.getProperty(PROTOCOLS);
            protocols = Strings.splitByComma(prop);
        }

        String[] ciphers;
        {
            String prop = service.getProperty(CIPHERS);
            String[] tmp = null == prop ? getJvmApplicableCipherSuites(CIPHERS_DEFAULT_DESIRED, true) : getJvmApplicableCipherSuites(Arrays.asList(Strings.splitByComma(prop)), false);
            ciphers = tmp;
        }

        HostList whitelistedHosts;
        {
            String prop = service.getProperty(TRUSTSTORE_WHITELIST);
            if (Strings.isNotEmpty(prop)) {
                prop = prop.trim();
            }
            HostList tmp = HostList.valueOf(prop);
            whitelistedHosts = tmp;
        }

        return new RestrictedConfig(protocols, ciphers, whitelistedHosts);
    }


    static final List<String> CIPHERS_DEFAULT_DESIRED = ImmutableList.<String> builder().add(
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384").build();

    private static String[] getJvmApplicableCipherSuites(List<String> desiredCipherSuites, boolean allowDefaultOnes) {
        // Get the cipher suites actually supported by running JVM
        List<String> jvmDefaults = SUPPORTED_CIPHER_SUITES_REFERENCE.get();
        Map<CipherSuiteName, String> supportedCipherSuites = new HashMap<>(jvmDefaults.size());
        for (String jvmDefaultCipherSuite : jvmDefaults) {
            supportedCipherSuites.put(nameFor(jvmDefaultCipherSuite), jvmDefaultCipherSuite);
        }

        // Grab the desired ones from JVM's default cipher suites
        List<String> defaultCipherSuites = new ArrayList<>(desiredCipherSuites.size());
        for (String desiredCipherSuite : desiredCipherSuites) {
            String supportedCipherSuite = supportedCipherSuites.get(nameFor(desiredCipherSuite));
            if (null != supportedCipherSuite) {
                defaultCipherSuites.add(desiredCipherSuite);
            }
        }

        if (defaultCipherSuites.isEmpty()) {
            // None of desired cipher suites is supported by JVM...
            if (allowDefaultOnes) {
                // Just use JVM default ones
                return jvmDefaults.toArray(new String[jvmDefaults.size()]);
            }

            // Illegal configuration
            throw new IllegalStateException("None of the desired cipher suites is actually supported by the Java Virtual Machine. Please list supported ones in value for 'com.openexchange.net.ssl.ciphersuites'.");
        }

        return defaultCipherSuites.toArray(new String[defaultCipherSuites.size()]);
    }

}
