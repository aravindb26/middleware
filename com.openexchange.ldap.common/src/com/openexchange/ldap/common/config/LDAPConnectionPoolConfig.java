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

package com.openexchange.ldap.common.config;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.ldap.common.config.ConfigUtils.asList;
import static com.openexchange.ldap.common.config.ConfigUtils.asMap;
import static com.openexchange.ldap.common.config.ConfigUtils.get;
import static com.openexchange.ldap.common.config.ConfigUtils.getEnum;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.ListSet;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.unboundid.ldap.sdk.RoundRobinDNSServerSet.AddressSelectionMode;

/**
 * {@link LDAPConnectionPoolConfig} contains the configuration for the ldap connection pool
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LDAPConnectionPoolConfig {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPConnectionPoolConfig.class);

    /**
     * Creates a {@link LDAPConnectionPoolConfig} from a given map
     *
     * @param configEntry The configuration map
     * @return The new {@link LDAPConnectionPoolConfig}
     * @throws OXException
     */
    public static LDAPConnectionPoolConfig init(Map<String, Object> configEntry) throws OXException {
        Builder builder = new Builder().type(getEnum(configEntry, "type", LDAPConnectionPoolType.class));
        for (Fields field : getRequiredFields(builder.type)) {
            applyField(configEntry, field, builder);
        }
        return builder.build();
    }

    /**
     * Converts a list of objects to a list of {@link LDAPServer}
     *
     * @param servers The list of server objects
     * @return The list of {@link LDAPServer}
     * @throws OXException
     */
    private static List<LDAPServer> toLDAPServer(List<Object> servers) throws OXException {
        List<LDAPServer> result = new ListSet<LDAPServer>(servers.size());
        for (Object o : servers) {
            result.add(LDAPServer.init(asMap(o)));
        }
        return result;
    }

    /**
     * {@link Builder} is a builder for {@link LDAPConnectionPoolConfig}
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.6
     */
    private static class Builder {

        LDAPConnectionPoolType type;
        List<LDAPServer> hosts;

        int initialConnections = 5;
        int maxConnections = 20;

        Optional<AddressSelectionMode> addressSelectionMode = Optional.empty();

        Optional<Boolean> onlyDns = Optional.empty();
        Optional<Boolean> abandonOnTimeout = Optional.empty();
        Optional<Boolean> keepAlive = Optional.empty();
        Optional<Boolean> retryFailedOperations = Optional.empty();
        Optional<Boolean> synchronousMode = Optional.empty();
        Optional<Boolean> tcpNoDelay = Optional.empty();
        Optional<Boolean> followReferrals = Optional.empty();
        Optional<Boolean> createIfNecessary = Optional.empty();

        Optional<String> readPool = Optional.empty();
        Optional<String> writePool = Optional.empty();

        Optional<Integer> connectionTimeoutMillis = Optional.empty();
        Optional<Integer> maxMessageSize = Optional.empty();
        Optional<Integer> referralHopLimit = Optional.empty();

        Optional<Long> cacheTimeoutMillis = Optional.empty();
        Optional<Long> maxConnectionAgeMillis = Optional.empty();
        Optional<Long> maxWaitTimeMillis = Optional.empty();
        Optional<Long> responseTimeoutMillis = Optional.empty();
        Optional<Long> healthCheckIntervalMillis = Optional.empty();

        Optional<LDAPCertificateStore> keyStore = Optional.empty();
        Optional<LDAPCertificateStore> trustStore = Optional.empty();

        Builder() {
            super();
        }

        Builder type(LDAPConnectionPoolType type) {
            this.type = type;
            return this;
        }

        Builder hosts(List<LDAPServer> hosts) {
            this.hosts = hosts;
            return this;
        }

        Builder readPool(String read) {
            this.readPool = Optional.ofNullable(read);
            return this;
        }

        Builder writePool(String write) {
            this.writePool = Optional.ofNullable(write);
            return this;
        }

        Builder addressSelectionMode(String addressSelectionMode) {
            this.addressSelectionMode = addressSelectionMode == null ? Optional.empty() : Optional.ofNullable(AddressSelectionMode.forName(addressSelectionMode));
            return this;
        }

        Builder cacheTimeoutMillis(Number value) {
            this.cacheTimeoutMillis = Optional.ofNullable(null == value ? null : L(value.longValue()));
            return this;
        }

        Builder initialConnections(int initCons) {
            this.initialConnections = initCons;
            return this;
        }

        Builder maxConnections(int maxCons) {
            this.maxConnections = maxCons;
            return this;
        }

        Builder connectionTimeoutMillis(Number value) {
            this.connectionTimeoutMillis = Optional.ofNullable(null == value ? null : I(value.intValue()));
            return this;
        }

        Builder maxMessageSize(Number value) {
            this.maxMessageSize = Optional.ofNullable(null == value ? null : I(value.intValue()));
            return this;
        }

        Builder referralHopLimit(Number value) {
            this.referralHopLimit = Optional.ofNullable(null == value ? null : I(value.intValue()));
            return this;
        }

        Builder maxConnectionAgeMillis(Number value) {
            this.maxConnectionAgeMillis = Optional.ofNullable(null == value ? null : L(value.longValue()));
            return this;
        }

        Builder maxWaitTimeMillis(Number value) {
            this.maxWaitTimeMillis = Optional.ofNullable(null == value ? null : L(value.longValue()));
            return this;
        }

        Builder responseTimeoutMillis(Number value) {
            this.responseTimeoutMillis = Optional.ofNullable(null == value ? null : L(value.longValue()));
            return this;
        }

        Builder healthCheckIntervalMillis(Number value) {
            this.responseTimeoutMillis = Optional.ofNullable(null == value ? null : L(value.longValue()));
            return this;
        }

        Builder onlyDns(Boolean onlyDns) {
            this.onlyDns = Optional.ofNullable(onlyDns);
            return this;
        }

        Builder abandonOnTimeout(Boolean abandonOnTimeout) {
            this.abandonOnTimeout = Optional.ofNullable(abandonOnTimeout);
            return this;
        }

        Builder keepAlive(Boolean keepAlive) {
            this.keepAlive = Optional.ofNullable(keepAlive);
            return this;
        }

        Builder retryFailedOperations(Boolean retryFailedOperations) {
            this.retryFailedOperations = Optional.ofNullable(retryFailedOperations);
            return this;
        }

        Builder synchronousMode(Boolean synchronousMode) {
            this.synchronousMode = Optional.ofNullable(synchronousMode);
            return this;
        }

        Builder tcpNoDelay(Boolean tcpNoDelay) {
            this.tcpNoDelay = Optional.ofNullable(tcpNoDelay);
            return this;
        }

        Builder followReferrals(Boolean followReferrals) {
            this.followReferrals = Optional.ofNullable(followReferrals);
            return this;
        }

        Builder createIfNecessary(Boolean createIfNecessary) {
            this.createIfNecessary = Optional.ofNullable(createIfNecessary);
            return this;
        }

        Builder keyStore(LDAPCertificateStore keyStore) {
            this.keyStore = Optional.ofNullable(keyStore);
            return this;
        }

        Builder trustStore(LDAPCertificateStore trustStore) {
            this.trustStore = Optional.ofNullable(trustStore);
            return this;
        }

        LDAPConnectionPoolConfig build() {
            return new LDAPConnectionPoolConfig(this);
        }

    }

    /**
     * Initializes a new {@link LDAPConnectionPoolConfig}.
     *
     * @param b The {@link Builder}
     */
    public LDAPConnectionPoolConfig(Builder b) {
        this.type = b.type;
        this.hosts = b.hosts;
        this.addressSelectionMode = b.addressSelectionMode;
        this.cacheTimeoutMillis = b.cacheTimeoutMillis;
        this.onlyDns = b(b.onlyDns.orElse(Boolean.FALSE));
        this.readPool = b.readPool;
        this.writePool = b.writePool;
        this.initialConnections = b.initialConnections;
        this.maxConnections = b.maxConnections;

        this.abandonOnTimeout = b.abandonOnTimeout;
        this.keepAlive = b.keepAlive;
        this.retryFailedOperations = b.retryFailedOperations;
        this.synchronousMode = b.synchronousMode;
        this.followReferrals = b.followReferrals;
        this.createIfNecessary = b.createIfNecessary;

        this.maxConnectionAgeMillis = b.maxConnectionAgeMillis;
        this.maxWaitTimeMillis = b.maxWaitTimeMillis;
        this.connectionTimeoutMillis = b.connectionTimeoutMillis;
        this.maxMessageSize = b.maxMessageSize;
        this.referralHopLimit = b.referralHopLimit;
        this.responseTimeoutMillis = b.responseTimeoutMillis;
        this.tcpNoDelay = b.tcpNoDelay;
        this.healthCheckIntervalMillis = b.healthCheckIntervalMillis;

        this.trustStore = b.trustStore;
        this.keyStore = b.keyStore;
    }

    private final LDAPConnectionPoolType type;
    private final List<LDAPServer> hosts;
    private final Optional<Long> cacheTimeoutMillis;
    private final boolean onlyDns;
    private final int initialConnections;
    private final int maxConnections;

    private final Optional<String> readPool;
    private final Optional<String> writePool;
    private final Optional<AddressSelectionMode> addressSelectionMode;

    private final Optional<Boolean> abandonOnTimeout;
    private final Optional<Boolean> keepAlive;
    private final Optional<Boolean> retryFailedOperations;
    private final Optional<Boolean> synchronousMode;
    private final Optional<Boolean> tcpNoDelay;
    private final Optional<Boolean> followReferrals;
    private final Optional<Boolean> createIfNecessary;

    private final Optional<Long> maxConnectionAgeMillis;
    private final Optional<Long> maxWaitTimeMillis;
    private final Optional<Long> responseTimeoutMillis;
    private final Optional<Long> healthCheckIntervalMillis;

    private final Optional<Integer> connectionTimeoutMillis;
    private final Optional<Integer> maxMessageSize;
    private final Optional<Integer> referralHopLimit;

    private final Optional<LDAPCertificateStore> trustStore;
    private final Optional<LDAPCertificateStore> keyStore;

    /**
     * Gets the
     * getType
     *
     * @return
     */
    public LDAPConnectionPoolType getType() {
        return type;
    }

    /**
     * Gets the list of {@link LDAPServer}s of this {@link LDAPConnectionPoolConfig}
     *
     * @return The list of {@link LDAPServer}
     */
    public List<LDAPServer> getHosts() {
        return hosts;
    }

    /**
     * Gets the optional {@link AddressSelectionMode}
     *
     * @return The optional {@link AddressSelectionMode}
     *
     */
    public Optional<AddressSelectionMode> optAddressSelectionMode() {
        return addressSelectionMode;
    }

    /**
     * Gets the optional cache timeout millis of the dns cache
     *
     * @return The optional cache timeout millis
     */
    public Optional<Long> optCacheTimeoutMillis() {
        return cacheTimeoutMillis;
    }

    /**
     * Gets the writePool
     *
     * @return The writePool
     */
    public Optional<String> optWritePool() {
        return writePool;
    }

    /**
     * Gets the readPool
     *
     * @return The readPool
     */
    public Optional<String> optReadPool() {
        return readPool;
    }

    /**
     * Gets the initialConnections
     *
     * @return The initialConnections
     */
    public int getInitialConnections() {
        return initialConnections;
    }

    /**
     * Gets the maxConnections
     *
     * @return The maxConnections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Whether to use only the system DNS to find the ldap server or to also use other jdni provider
     *
     * @return <code>true</code> if only the system dns shall be used, <code>false</code> otherwise
     */
    public boolean onlyDns() {
        return onlyDns;
    }

    /**
     * Gets the optional abandonOnTimeout
     *
     * @return The optional abandonOnTimeout
     */
    public Optional<Boolean> optAbandonOnTimeout() {
        return abandonOnTimeout;
    }

    /**
     * Gets the optional keepAlive
     *
     * @return The optional keepAlive
     */
    public Optional<Boolean> optKeepAlive() {
        return keepAlive;
    }

    /**
     * Gets the optional retryFailedOperations
     *
     * @return The optional retryFailedOperations
     */
    public Optional<Boolean> optRetryFailedOperations() {
        return retryFailedOperations;
    }

    /**
     * Gets the optional synchronousMode
     *
     * @return The optional synchronousMode
     */
    public Optional<Boolean> optSynchronousMode() {
        return synchronousMode;
    }

    /**
     * Gets the optional tcpNoDelay
     *
     * @return The optional tcpNoDelay
     */
    public Optional<Boolean> optTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Gets the optional followReferrals
     *
     * @return The optional followReferrals
     */
    public Optional<Boolean> optFollowReferrals() {
        return followReferrals;
    }

    /**
     * Gets the optional createIfNecessary
     *
     * @return The optional createIfNecessary
     */
    public Optional<Boolean> optCreateIfNecessary() {
        return createIfNecessary;
    }

    /**
     * Gets the optional maxConnectionAgeMillis
     *
     * @return The optional maxConnectionAgeMillis
     */
    public Optional<Long> optMaxConnectionAgeMillis() {
        return maxConnectionAgeMillis;
    }

    /**
     * Gets the optional maxWaitTimeMillis
     *
     * @return The optional maxWaitTimeMillis
     */
    public Optional<Long> optMaxWaitTimeMillis() {
        return maxWaitTimeMillis;
    }

    /**
     * Gets the optional responseTimeoutMillis
     *
     * @return The optional responseTimeoutMillis
     */
    public Optional<Long> optResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }

    /**
     * Gets the optional healthCheckIntervalMillis
     *
     * @return The optional healthCheckIntervalMillis
     */
    public Optional<Long> optHealthCheckIntervalMillis() {
        return healthCheckIntervalMillis;
    }

    /**
     * Gets the optional connectionTimeoutMillis
     *
     * @return The optional connectionTimeoutMillis
     */
    public Optional<Integer> optConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    /**
     * Gets the optional maxMessageSize
     *
     * @return The optional maxMessageSize
     */
    public Optional<Integer> optMaxMessageSize() {
        return maxMessageSize;
    }

    /**
     * Gets the optional referralHopLimit
     *
     * @return The optional referralHopLimit
     */
    public Optional<Integer> optReferralHopLimit() {
        return referralHopLimit;
    }

    /**
     * Gets the optional keyStore
     *
     * @return The optional keyStore
     */
    public Optional<LDAPCertificateStore> optKeyStore() {
        return keyStore;
    }

    /**
     * Gets the optional trustStore
     *
     * @return The optional trustStore
     */
    public Optional<LDAPCertificateStore> optTrustStore() {
        return trustStore;
    }

    /**
     * Applies the given field to the builder
     *
     * @param config
     * @param field
     * @param b
     * @throws OXException
     */
    static void applyField(Map<String, Object> config, Fields field, Builder b) throws OXException {
        switch(field) {
            case addressSelectionMode:
                applyGeneric(config, field, String.class, (o) -> b.addressSelectionMode(o));
                return;
            case cacheTimeoutMillis:
                applyGeneric(config, field, Number.class, (o) -> b.cacheTimeoutMillis(o));
                return;
            case readPool:
                b.readPool(get(config, field.getFieldName()));
                return;
            case writePool:
                b.writePool(get(config, field.getFieldName()));
                return;
            case hosts:
                if (config.containsKey(field.getFieldName())) {
                    b.hosts(toLDAPServer(asList(config.get(field.getFieldName()))));
                    return;
                }
                if (config.containsKey(field.optAlias().orElseThrow(() -> new OXException()))) {
                    b.hosts(toLDAPServer(Collections.singletonList(config.get(field.optAlias().get()))));
                    return;
                }
                throw LDAPCommonErrorCodes.PROPERTY_MISSING.create(field.getFieldName());
            case abandonOnTimeout:
                applyGeneric(config, field, Boolean.class, (o) -> b.abandonOnTimeout(o));
                return;
            case connectionTimeoutMillis:
                applyGeneric(config, field, Number.class, (o) -> b.connectionTimeoutMillis(o));
                return;
            case createIfNecessary:
                applyGeneric(config, field, Boolean.class, (o) -> b.createIfNecessary(o));
                return;
            case followReferrals:
                applyGeneric(config, field, Boolean.class, (o) -> b.followReferrals(o));
                return;
            case healthCheckIntervalMillis:
                applyGeneric(config, field, Number.class, (o) -> b.healthCheckIntervalMillis(o));
                return;
            case onlyDns:
                applyGeneric(config, field, Boolean.class, (o) -> b.onlyDns(o));
                return;
            case keepAlive:
                applyGeneric(config, field, Boolean.class, (o) -> b.keepAlive(o));
                return;
            case maxConnectionAgeMillis:
                applyGeneric(config, field, Number.class, (o) -> b.maxConnectionAgeMillis(o));
                return;
            case maxMessageSize:
                applyGeneric(config, field, Number.class, (o) -> b.maxMessageSize(o));
                return;
            case maxWaitTimeMillis:
                applyGeneric(config, field, Number.class, (o) -> b.maxWaitTimeMillis(o));
                return;
            case poolMax:
                applyGeneric(config, field, Integer.class, (o) -> b.maxConnections(i(o)));
                return;
            case poolMin:
                applyGeneric(config, field, Integer.class, (o) -> b.initialConnections(i(o)));
                return;
            case referralHopLimit:
                applyGeneric(config, field, Number.class, (o) -> b.referralHopLimit(o));
                return;
            case responseTimeoutMillis:
                applyGeneric(config, field, Number.class, (o) -> b.responseTimeoutMillis(o));
                return;
            case retryFailedOperations:
                applyGeneric(config, field, Boolean.class, (o) -> b.retryFailedOperations(o));
                return;
            case synchronousMode:
                applyGeneric(config, field, Boolean.class, (o) -> b.synchronousMode(o));
                return;
            case tcpNoDelay:
                applyGeneric(config, field, Boolean.class, (o) -> b.tcpNoDelay(o));
                return;
            case keyStore:
                Object keyStoreConf = config.get(field.getFieldName());
                if (keyStoreConf != null) {
                    b.keyStore(LDAPCertificateStore.init(asMap(keyStoreConf)));
                }
                return;
            case trustStore:
                Object trustStoreConf = config.get(field.getFieldName());
                if (trustStoreConf != null) {
                    b.trustStore(LDAPCertificateStore.init(asMap(trustStoreConf)));
                }
                return;
            default:
                // Should not happen
                LOG.error("Ignoring unknown field `{}`", field);
                return;
        }
    }


    @SuppressWarnings("unchecked")
    private static <T> void applyGeneric(Map<String, Object> config, Fields field, Class<? extends T> clazz, Consumer<T> c) throws OXException {
        Object o = config.get(field.getFieldName());
        if (o == null) {
            return;
        }
        try {
            c.accept((T) o);
        } catch (ClassCastException e) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create(new Exception("Unexpected error converting value \"" + o + "\" for \"" + field + "\" to " + clazz, e));
        }        
    }

    private static final EnumSet<Fields> SIMPLE_POOL = EnumSet.complementOf(EnumSet.of(Fields.writePool, Fields.readPool, Fields.addressSelectionMode, Fields.onlyDns, Fields.cacheTimeoutMillis));
    private static final EnumSet<Fields> DNS_POOL = EnumSet.complementOf(EnumSet.of(Fields.writePool, Fields.readPool));
    private static final EnumSet<Fields> READ_WRITE_POOL = EnumSet.of(Fields.readPool, Fields.writePool);

    private static EnumSet<Fields> getRequiredFields(LDAPConnectionPoolType type) throws OXException {
        switch (type) {
            case dnsRoundRobin:
                return DNS_POOL;
            case readWrite:
                return READ_WRITE_POOL;
            case failover:
            case fewestConnections:
            case roundRobin:
            case simple:
                return SIMPLE_POOL;
            default:
                break;

        }
        throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create("Unable to create fieldset for the configured server type");
    }

    /**
     * {@link Fields} contains all available fields of the {@link LDAPConnectionPoolConfig} configuration
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v7.10.6
     */
    private static enum Fields {

        cacheTimeoutMillis("cacheTimeoutMillis"),
        addressSelectionMode("addressSelectionMode"),
        onlyDns("onlyDns"),
        poolMin("pool-min"),
        poolMax("pool-max"),
        hosts("hosts", "host"),
        trustStore("trust-store"),
        keyStore("key-store"),
        maxConnectionAgeMillis("maxConnectionAgeMillis"),
        maxWaitTimeMillis("maxWaitTimeMillis"),
        createIfNecessary("createIfNecessary"),
        retryFailedOperations("retryFailedOperations"),
        readPool("readPool"),
        writePool("writePool"),
        abandonOnTimeout("abandonOnTimeout"),
        connectionTimeoutMillis("connectionTimeoutMillis"),
        responseTimeoutMillis("responseTimeoutMillis"),
        keepAlive("keepAlive"),
        tcpNoDelay("tcpNoDelay"),
        synchronousMode("synchronousMode"),
        healthCheckIntervalMillis("healthCheckIntervalMillis"),
        followReferrals("followReferrals"),
        referralHopLimit("referralHopLimit"),
        maxMessageSize("maxMessageSize")

        ;

        private String fieldName;
        private Optional<String> alias;

        /**
         * Initializes a new {@link LDAPConnectionPoolConfig.Fields}.
         */
        private Fields(String fieldname) {
            this.fieldName = fieldname;
            this.alias = Optional.empty();
        }

        /**
         * Initializes a new {@link LDAPConnectionPoolConfig.Fields}.
         */
        private Fields(String fieldname, String alias) {
            this.fieldName = fieldname;
            this.alias = Optional.ofNullable(alias);
        }

        /**
         * Gets the fieldName
         *
         * @return The fieldName
         */
        public String getFieldName() {
            return fieldName;
        }


        /**
         * Gets the alias
         *
         * @return The alias
         */
        public Optional<String> optAlias() {
            return alias;
        }

    }

}

