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

import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Autoboxing.l;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadables;
import com.openexchange.exception.OXException;
import com.openexchange.keystore.KeyStoreService;
import com.openexchange.keystore.KeyStoreUtil;
import com.openexchange.ldap.common.BindRequestFactory;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.ldap.common.config.LDAPCertificateStore;
import com.openexchange.ldap.common.config.LDAPConfig;
import com.openexchange.ldap.common.config.LDAPConfigLoader;
import com.openexchange.ldap.common.config.LDAPConnectionPoolConfig;
import com.openexchange.ldap.common.config.LDAPConnectionPoolType;
import com.openexchange.ldap.common.config.LDAPServer;
import com.openexchange.ldap.common.config.auth.AdminDNConfig;
import com.openexchange.ldap.common.config.auth.AuthType;
import com.openexchange.ldap.common.config.auth.CustomConfig;
import com.openexchange.ldap.common.config.auth.UserDNResolvedConfig;
import com.openexchange.ldap.common.impl.bind.OAuthBearerBindRequestFactory;
import com.openexchange.ldap.common.impl.bind.UserDNResolvedBindRequestFactory;
import com.openexchange.ldap.common.impl.bind.UserDNTemplateBindRequestFactory;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.ServiceListing;
import com.unboundid.ldap.sdk.ANONYMOUSBindRequest;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.FewestConnectionsServerSet;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPReadWriteConnectionPool;
import com.unboundid.ldap.sdk.RoundRobinDNSServerSet;
import com.unboundid.ldap.sdk.RoundRobinDNSServerSet.AddressSelectionMode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

/**
 * {@link LDAPServiceImpl} is the singleton implementation of the {@link LDAPInterface}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LDAPServiceImpl implements LDAPService, ForcedReloadable {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPServiceImpl.class);
    private static final Supplier<OXException> INVALID_CONFIG_ERROR_SUPPLIER = () -> LDAPCommonErrorCodes.INVALID_CONFIG.create();

    private final ConcurrentHashMap<String, ConnectionPoolWrapper> pools = new ConcurrentHashMap<String, ConnectionPoolWrapper>(1);
    private final LockService lockservice;
    private final LDAPConfigLoader loader;
    private final ServiceListing<BindRequestFactory> bindRequestFactories;
    private final KeyStoreService keyStoreService;

    /**
     * Initializes a new {@link LDAPServiceImpl}.
     *
     * @param lockservice The lock service
     * @param loader The ldap configuration loader
     * @param bindRequestFactories The factories to use
     * @throws OXException In case lock can't be get
     */
    public LDAPServiceImpl(LockService lockservice, LDAPConfigLoader loader, ServiceListing<BindRequestFactory> bindRequestFactories, KeyStoreService keyStoreService) throws OXException {
        super();
        this.loader = loader;
        this.lockservice = lockservice;
        this.bindRequestFactories = bindRequestFactories;
        this.keyStoreService = keyStoreService;
        Lock lock = lockservice.getSelfCleaningLockFor(LDAPServiceImpl.class.getName());
        lock.lock();
        try {
            loader.getConfigs().forEach(config -> register(config));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Registers a new ldap client pool
     *
     * @param config The ldap pool configuration
     */
    public void register(LDAPConfig config) {
        pools.put(config.getName(), new ConnectionPoolWrapper(config));
    }

    /**
     * Creates an {@link AuthAwareLDAPConnectionProvider} for the given {@link LDAPConfig}
     *
     * @param config The {@link LDAPConfig}
     * @return The {@link AuthAwareLDAPConnectionProvider}
     * @throws OXException in case the configuration is invalid
     */
    AuthAwareLDAPConnectionProvider createLDAPInterface(LDAPConfig config) throws OXException {
        if (config.getPool().getType().equals(LDAPConnectionPoolType.readWrite)) {
            return wrap(createReadWriteInterface(config), config);
        }

        LDAPConnectionPool pool = config.getPool().getType() == LDAPConnectionPoolType.simple ? createSingleServerConnectionPool(config) : createMultipleServerConnectionPool(config);
        return wrap(configurePool(config.getPool(), pool), config);
    }

    /**
     * Configures a given {@link LDAPConnectionPool} with additional configurations
     *
     * @param poolConfig The {@link LDAPConnectionPoolConfig} configuration
     * @param pool The pool to configure
     * @return The configured {@link LDAPConnectionPool}
     */
    private static LDAPInterface configurePool(LDAPConnectionPoolConfig poolConfig, LDAPConnectionPool pool) {
        poolConfig.optMaxConnectionAgeMillis().ifPresent(max -> pool.setMaxConnectionAgeMillis(l(max)));
        poolConfig.optHealthCheckIntervalMillis().ifPresent(interval -> pool.setHealthCheckIntervalMillis(l(interval)));
        poolConfig.optMaxWaitTimeMillis().ifPresent(max -> pool.setMaxWaitTimeMillis(l(max)));

        poolConfig.optRetryFailedOperations().ifPresent(retry -> pool.setRetryFailedOperationsDueToInvalidConnections(b(retry)));
        poolConfig.optCreateIfNecessary().ifPresent(create -> pool.setCreateIfNecessary(b(create)));
        return pool;
    }

    /**
     * Wraps an {@link LDAPInterface} in an {@link AuthAwareLDAPConnectionProvider}
     *
     * @param connection The {@link LDAPInterface} to wrap
     * @param config The {@link LDAPConfig}
     * @return The AuthAwareLDAPConnectionHolder
     * @throws OXException in case the configuration is invalid
     */
    private AuthAwareLDAPConnectionProvider wrap(LDAPInterface connection, LDAPConfig config) throws OXException {
        Optional<BindRequestFactory> optBindFactory = optBindFactory(config, connection);
        if (optBindFactory.isPresent()) {
            return new AuthAwareLDAPConnectionProvider(connection, optBindFactory.get(), config.getBaseDN());
        }
        return new AuthAwareLDAPConnectionProvider(connection, null, config.getBaseDN());
    }

    /**
     * Gets a {@link BindRequestFactory} for the given {@link LDAPConfig}
     *
     * @param config The {@link LDAPConfig}
     * @param connection The {@link LDAPInterface}
     * @return The optional {@link BindRequestFactory} for the given configuration
     * @throws OXException in case the configuration is invalid
     */
    public Optional<BindRequestFactory> optBindFactory(LDAPConfig config, LDAPInterface connection) throws OXException {
        return switch (config.getAuthConfig().getType()) {
            case ADMINDN, ANONYMOUS -> Optional.empty();
            default -> Optional.empty();
            case USERDN_RESOLVED -> Optional.of(new UserDNResolvedBindRequestFactory(config.getAuthConfig().optUserDNResolvedConfig().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create()), connection));
            case USERDN_TEMPLATE -> Optional.of(new UserDNTemplateBindRequestFactory(config.getAuthConfig().optUserDNTemplateConfig().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create())));
            case OAUTHBEARER -> Optional.of(new OAuthBearerBindRequestFactory());
            case CUSTOM -> Optional.of(getCustomBindRequestFactory(config));
        };
    }

    /**
     * Gets the {@link BindRequestFactory} specified in config
     *
     * @param config The {@link LDAPConfig}
     * @return The {@link BindRequestFactory} specified in config
     * @throws OXException in case the configured {@link BindRequestFactory} has not been registered
     */
    private BindRequestFactory getCustomBindRequestFactory(LDAPConfig config) throws OXException {
        CustomConfig customConfig = config.getAuthConfig().optCustomConfig().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create());
        // @formatter:off
        return bindRequestFactories.getServiceList()
                                   .stream()
                                   .filter(factory -> factory.getId().equals(customConfig.getId()))
                                   .findAny()
                                   .orElseThrow(() -> LDAPCommonErrorCodes.BIND_NOT_FOUND.create(customConfig.getId()));
        // @formatter:on
    }

    /**
     * Creates a {@link LDAPReadWriteConnectionPool} for the given configuration
     *
     * @param config the {@link LDAPConfig}
     * @return The {@link LDAPInterface}
     * @throws OXException in case the configuration is invalid
     */
    private LDAPInterface createReadWriteInterface(LDAPConfig config) throws OXException {

        String readPoolId = config.getPool().optReadPool().orElseThrow(INVALID_CONFIG_ERROR_SUPPLIER);
        String writePoolId = config.getPool().optWritePool().orElseThrow(INVALID_CONFIG_ERROR_SUPPLIER);
        // @formatter:off
        return new LDAPReadWriteConnectionPool(optConnectionPool(readPoolId).orElseThrow(INVALID_CONFIG_ERROR_SUPPLIER),
                                               optConnectionPool(writePoolId).orElseThrow(INVALID_CONFIG_ERROR_SUPPLIER));
        // @formatter:on
    }

    /**
     * Gets a LDAP connection to a single server
     *
     * @param config The {@link LDAPConfig}
     * @return The {@link LDAPInterface}
     * @throws OXException in case the configuration is invalid or no connection could be established
     */
    private LDAPConnectionPool createSingleServerConnectionPool(LDAPConfig config) throws OXException {

        LDAPConnectionOptions options = createLDAPConnectionOption(config);
        Optional<SocketFactory> socketFactory = optSocketFactory(config.getPool());
        LDAPServer server = config.getPool().getHosts().get(0);
        LDAPConnection connection;
        try {
            connection = new LDAPConnection(socketFactory.orElse(null), options, server.getHost(), server.getPort());
            connection.bind(createBindRequest(config));
            // @formatter:off
            return new LDAPConnectionPool(connection,
                                          config.getPool().getInitialConnections(),
                                          config.getPool().getMaxConnections(),
                                          Math.min(config.getPool().getInitialConnections(), 10),
                                          null,
                                          true);
            // @formatter:on
        } catch (LDAPException e) {
            throw LDAPCommonErrorCodes.CONNECTION_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Creates the ldap connection options for the given configuration
     *
     * @param config The {@link LDAPConfig}
     * @return The {@link LDAPConnectionOptions}
     */
    private LDAPConnectionOptions createLDAPConnectionOption(LDAPConfig config) {
        LDAPConnectionOptions options = new LDAPConnectionOptions();
        LDAPConnectionPoolConfig poolConfig = config.getPool();
        // bool options
        poolConfig.optAbandonOnTimeout().ifPresent(abandon -> options.setAbandonOnTimeout(b(abandon)));
        poolConfig.optKeepAlive().ifPresent(keepAlive -> options.setUseKeepAlive(b(keepAlive)));
        poolConfig.optSynchronousMode().ifPresent(useSync -> options.setUseSynchronousMode(b(useSync)));
        poolConfig.optTcpNoDelay().ifPresent(noDelay -> options.setUseTCPNoDelay(b(noDelay)));
        poolConfig.optFollowReferrals().ifPresent(follow -> options.setFollowReferrals(b(follow)));
        // integer options
        poolConfig.optConnectionTimeoutMillis().ifPresent(timeout -> options.setConnectTimeoutMillis(i(timeout)));
        poolConfig.optMaxMessageSize().ifPresent(size -> options.setMaxMessageSize(i(size)));
        poolConfig.optReferralHopLimit().ifPresent(limit -> options.setReferralHopLimit(i(limit)));
        // long options
        poolConfig.optResponseTimeoutMillis().ifPresent(timeout -> options.setResponseTimeoutMillis(l(timeout)));
        return options;
    }

    /**
     * If necessary creates the ssl {@link SocketFactory} for the ldap connection
     *
     * @param config The {@link LDAPConnectionPoolConfig} configuration
     * @return The optional {@link SocketFactory}
     * @throws OXException in case the configuration is invalid
     */
    private Optional<SocketFactory> optSocketFactory(LDAPConnectionPoolConfig config) throws OXException {
        final Optional<KeyManager> keyManager = optKeyManager(config);
        final Optional<TrustManager> trustManager = optTrustManager(config);
        if (keyManager.isPresent() == false && trustManager.isPresent() == false) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SSLUtil(keyManager.orElse(null), trustManager.orElse(null)).createSSLSocketFactory());
        } catch (final GeneralSecurityException e) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create(e);
        }
    }

    /**
     * If necessary creates a new {@link KeyManager} for the given configuration
     *
     * @param config The {@link LDAPConnectionPoolConfig} configuration
     * @return The optional {@link KeyManager}
     * @throws OXException in case the configuration is invalid
     */
    private Optional<KeyManager> optKeyManager(LDAPConnectionPoolConfig config) throws OXException {
        if (config.optKeyStore().isPresent() == false) {
            return Optional.empty();
        }
        LDAPCertificateStore ldapKeystore = config.optKeyStore().get();

        if (false == (ldapKeystore.optFile().isPresent() || ldapKeystore.optId().isPresent())) {
            return Optional.empty();
        }

        Optional<KeyStore> optTrustStore = getKeyStore(ldapKeystore);
        if (optTrustStore.isPresent() == false) {
            return Optional.empty();
        }

        try {
            // @formatter:off
            return Optional.of(new CustomKeyStoreKeyManager(optTrustStore.get(),
                                                            ldapKeystore.optPassword().map(pw -> pw.toCharArray()).orElse(null),
                                                            ldapKeystore.optAlias().orElse(null),
                                                            b(ldapKeystore.optExamineValidityDates().orElse(Boolean.FALSE))));
            // @formatter:on
        } catch (final KeyStoreException e) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create(e);
        }
    }

    /**
     * If necessary creates a new {@link TrustManager} for the given configuration
     *
     * @param config The {@link LDAPConnectionPoolConfig} configuration
     * @return The optional {@link TrustManager}
     * @throws OXException
     */
    private Optional<TrustManager> optTrustManager(LDAPConnectionPoolConfig config) throws OXException {
        if (config.optTrustStore().isPresent() == false) {
            return Optional.empty();
        }
        LDAPCertificateStore ldapTrustStore = config.optTrustStore().get();
        boolean examineValidityDates = b(ldapTrustStore.optExamineValidityDates().orElse(Boolean.FALSE));
        if (ldapTrustStore.optFile().isPresent() == false && b(ldapTrustStore.optTrustAll().orElse(Boolean.FALSE))) {
            return Optional.of(new TrustAllTrustManager(examineValidityDates));
        }

        if (false == (ldapTrustStore.optFile().isPresent() || ldapTrustStore.optId().isPresent())) {
            return Optional.empty();
        }

        Optional<KeyStore> optTrustStore = getKeyStore(ldapTrustStore);
        if (optTrustStore.isPresent() == false) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ValidityCheckingTrustStoreTrustManager(optTrustStore.get(), examineValidityDates));
        } catch (CertificateException e) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create(e);
        }
    }

    private Optional<KeyStore> getKeyStore(LDAPCertificateStore store) throws OXException {
        if (false == (store.optFile().isPresent() || store.optId().isPresent())) {
            return Optional.empty();
        }

        KeyStore keystore = null;
        try {
            if (store.optId().isPresent()) {
                keystore = keyStoreService.optKeyStore(store.optId().get(), Optional.empty(), store.optPassword()).orElse(null);
            }
            if (keystore == null && store.optFile().isPresent()) {
                try (FileInputStream stream = new FileInputStream(store.optFile().get())) {
                    keystore = KeyStoreUtil.toKeyStore(stream, Optional.empty(), store.optPassword());
                }
            }
        } catch (OXException | IOException e) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create(e);
        }

        return Optional.ofNullable(keystore);
    }

    /**
     * Creates a {@link LDAPConnectionPool} for multiple servers
     *
     * @param config The {@link LDAPConfig}
     * @return The {@link LDAPConnectionPool}
     * @throws OXException in case the configuration is invalid or if the connection couldn't be established
     */
    private LDAPConnectionPool createMultipleServerConnectionPool(LDAPConfig config) throws OXException {
        try {
            // @formatter:off
            return new LDAPConnectionPool(createServerSet(config,
                                                          optSocketFactory(config.getPool()).orElse(null),
                                                          createLDAPConnectionOption(config)),
                                          createBindRequest(config),
                                          config.getPool().getInitialConnections(),
                                          config.getPool().getMaxConnections(),
                                          Math.min(config.getPool().getInitialConnections(), 10),
                                          null,
                                          true);
            // @formatter:on
        } catch (LDAPException e) {
            throw LDAPCommonErrorCodes.CONNECTION_ERROR.create(e, e.getMessage());
        }
    }

    /**
     *
     * Creates a {@link ServerSet} for the given configuration
     *
     * @param config The {@link LDAPConfig}
     * @param socketFactory The socket factory or null
     * @param conOptions The {@link LDAPConnectionOptions} or null
     * @return The {@link ServerSet}
     * @throws OXException in case a server set cannot be created
     */
    private ServerSet createServerSet(LDAPConfig config, SocketFactory socketFactory, LDAPConnectionOptions conOptions) throws OXException {
        // @formatter:off
        LDAPConnectionPoolConfig ldapPool = config.getPool();
        String[] hosts = ldapPool.getHosts()
                                 .stream()
                                 .map((s) -> s.getHost())
                                 .collect(Collectors.toList())
                                 .toArray(new String[ldapPool.getHosts().size()]);
        int[] ports = ldapPool.getHosts()
                              .stream()
                              .mapToInt((s) -> s.getPort())
                              .toArray();
        // @formatter:on
        switch(ldapPool.getType()) {

            case readWrite:
                throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create("ServerSet not supported for readWrite pools");
            case failover:
                return new FailoverServerSet(hosts, ports);
            case dnsRoundRobin:
                // @formatter:off
                return new RoundRobinDNSServerSet(hosts[0],
                                                  ports[0],
                                                  getAddressSelectionMode(ldapPool),
                                                  l(ldapPool.optCacheTimeoutMillis().orElse(L(-1))),
                                                  ldapPool.onlyDns() ? "dns:" : null,
                                                  socketFactory,
                                                  conOptions);
                // @formatter:on
            case simple:
            default:
                return new SingleServerSet(hosts[0], ports[0], socketFactory, conOptions);
            case fewestConnections:
                // @formatter:off
                return new FewestConnectionsServerSet(hosts,
                                                      ports,
                                                      socketFactory,
                                                      conOptions,
                                                      createBindRequest(config),
                                                      null);
                // @formatter:on
            case roundRobin:
                // @formatter:off
                return new RoundRobinServerSet(hosts,
                                               ports,
                                               socketFactory,
                                               conOptions,
                                               createBindRequest(config),
                                               null);
                // @formatter:on
        }
    }

    /**
     * Gets the configured {@link AddressSelectionMode} or {@link AddressSelectionMode#RANDOM}
     *
     * @param config The {@link LDAPConnectionPoolConfig} configuration
     * @return the {@link AddressSelectionMode}
     */
    private AddressSelectionMode getAddressSelectionMode(LDAPConnectionPoolConfig config) {
        return config.optAddressSelectionMode().orElse(AddressSelectionMode.RANDOM);
    }

    /**
     * Creates the bind request to use for the given {@link LDAPConfig}
     *
     * @param config The {@link LDAPConfig}
     * @return The {@link BindRequest}
     * @throws OXException in case the configuration is invalid
     */
    private BindRequest createBindRequest(LDAPConfig config) throws OXException {
        switch (config.getAuthConfig().getType()) {
            default:
            case ADMINDN:
                return createAdminDNBindRequest(config);
            case ANONYMOUS:
                return createANONYMOUSBindRequest();
            case USERDN_RESOLVED:
                UserDNResolvedConfig userDNConf = config.getAuthConfig().optUserDNResolvedConfig().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create());
                if (userDNConf.getSearchAuthType().equals(AuthType.ADMINDN)) {
                    return createAdminDNBindRequest(config);
                }
                return createANONYMOUSBindRequest();
            case USERDN_TEMPLATE:
            case OAUTHBEARER:
                if (config.getAuthConfig().optAdminDNConfig().isPresent()) {
                    return createAdminDNBindRequest(config);
                }
                return createANONYMOUSBindRequest();
            case CUSTOM:
                CustomConfig customConfig = config.getAuthConfig().optCustomConfig().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create());
                if (customConfig.getInitPoolAuthType().equals(AuthType.ADMINDN)) {
                    return createAdminDNBindRequest(config);
                }
                return createANONYMOUSBindRequest();
        }
    }

    /**
     * Creates an anonymous bind request
     *
     * @return A new {@link ANONYMOUSBindRequest}
     */
    private static BindRequest createANONYMOUSBindRequest() {
        return new ANONYMOUSBindRequest("OX Appsuite");
    }

    /**
     * Creates an admin dn bind request
     *
     * @param config The configuration
     * @return The {@link BindRequest}
     * @throws OXException in case the configuration is invalid
     */
    private BindRequest createAdminDNBindRequest(LDAPConfig config) throws OXException {
        AdminDNConfig admindnConf = config.getAuthConfig().optAdminDNConfig().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create());
        return new SimpleBindRequest(admindnConf.getDn(), admindnConf.getPassword());
    }

    /**
     * Gets the pool with the given name
     *
     * @param name The pool name
     * @return The optional {@link LDAPConnectionProvider}
     */
    private Optional<LDAPConnectionProvider> optPool(String name) {
        try {
            Optional<ConnectionPoolWrapper> optWrapper = Optional.ofNullable(pools.get(name));
            return optWrapper.isPresent() ? Optional.of(optWrapper.get().getProvider()) : Optional.empty();
        } catch (OXException e) {
            LOG.error("Unable to create connection pool for config {}: {}", name, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Gets the optional {@link LDAPConnectionPool}
     *
     * @param name The pool name
     * @return The optional {@link LDAPConnectionPool}
     * @throws OXException in case the bind operation fails
     */
    private Optional<LDAPConnectionPool> optConnectionPool(String name) throws OXException {
        Optional<LDAPConnectionProvider> optPool = optPool(name);
        if (optPool.isPresent() == false) {
            return Optional.empty();
        }
        LDAPInterface result = optPool.get().getConnection(null);
        return result instanceof LDAPConnectionPool ? Optional.of((LDAPConnectionPool) result) : Optional.empty();
    }

    @Override
    public LDAPConnectionProvider getConnection(String id) throws OXException {
        return optPool(id).orElseThrow(() -> LDAPCommonErrorCodes.NOT_FOUND.create(id));
    }

    @Override
    public Interests getInterests() {
        return Reloadables.getInterestsForAll();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        Lock lock;
        try {
            lock = lockservice.getSelfCleaningLockFor(LDAPServiceImpl.class.getName());
            lock.lock();
            try {
                pools.clear();
                loader.getConfigs().forEach(config -> register(config));
            } finally {
                lock.unlock();
            }
        } catch (OXException e) {
            LOG.error("Unable to reload ldap configuration.", e);
        }

    }

    /**
     * The {@link ConnectionPoolWrapper} is used to lazy load LDAP connection pools
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.6
     */
    private class ConnectionPoolWrapper {

        private final LDAPConfig config;
        private final AtomicReference<AuthAwareLDAPConnectionProvider> providerRef;

        /**
         * Initializes a new {@link ConnectionPoolWrapper}.
         *
         * @param config The ldap configuration
         */
        ConnectionPoolWrapper(LDAPConfig config) {
            super();
            this.config = config;
            this.providerRef = new AtomicReference<>(null);
        }

        /**
         * Gets the existing provider or creates one if necessary
         *
         * @return The {@link AuthAwareLDAPConnectionProvider}
         * @throws OXException In case an error occurred while creating the LDAP connection
         */
        AuthAwareLDAPConnectionProvider getProvider() throws OXException {
            AuthAwareLDAPConnectionProvider provider = providerRef.get();
            if (provider == null) {
                synchronized (this) {
                    provider = providerRef.get();
                    if (provider == null) {
                        provider = createLDAPInterface(config);
                        providerRef.set(provider);
                    }
                }
            }
            return provider;
        }

    }

}
