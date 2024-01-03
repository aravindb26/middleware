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

package com.openexchange.net;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.net.utils.Strings;

/**
 * {@link NetUtility} - A utility class for network-related parsing/processing.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.4
 */
public class NetUtility {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NetUtility.class);

    /**
     * Initializes a new {@link NetUtility}.
     */
    private NetUtility() {
        super();
    }

    private static final class InetAddressCache {

        private final Object lock;
        private final LinkedHashMap<?, ?> cache;

        InetAddressCache(LinkedHashMap<?, ?> cache, Object lock) {
            super();
            this.cache = cache;
            this.lock = lock;
        }

        void clearCache() {
            synchronized (lock) {
                cache.clear();
            }
        }

        void clearCacheFor(String hostName) {
            synchronized (lock) {
                cache.remove(Strings.asciiLowerCase(hostName));
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class InetAddressCaches {

        private final InetAddressCache addressCache;
        private final InetAddressCache negativeCache;

        InetAddressCaches(InetAddressCache addressCache, InetAddressCache negativeCache) {
            super();
            this.addressCache = addressCache;
            this.negativeCache = negativeCache;
        }

        void clearCaches() {
            addressCache.clearCache();
            addressCache.clearCache();
        }

        void clearCachesFor(String hostName) {
            addressCache.clearCacheFor(hostName);
            addressCache.clearCacheFor(hostName);
        }
    }

    private static volatile InetAddressCaches caches;

    private static InetAddressCaches getInetAddressCaches() {
        InetAddressCaches tmp = caches;
        if (null == tmp) {
            synchronized (NetUtility.class) {
                tmp = caches;
                if (null == tmp) {
                    try {
                        // Acquire the "addressCache" cache instance for positive DNS look-ups
                        Field addressCacheField = InetAddress.class.getDeclaredField("addressCache");
                        addressCacheField.setAccessible(true);
                        Object addressCache = addressCacheField.get(null);

                        // Acquire the "negativeCache" cache instance for negative DNS look-ups
                        Field negativeCacheField = InetAddress.class.getDeclaredField("negativeCache");
                        negativeCacheField.setAccessible(true);
                        Object negativeCache = negativeCacheField.get(null);

                        // Acquire the inner LinkedHashMap instances from both caches - "addressCache" cache and "negativeCache" cache
                        Class<?> cacheClazz = Class.forName("java.net.InetAddress$Cache");
                        Field cacheField = cacheClazz.getDeclaredField("cache");
                        cacheField.setAccessible(true);
                        LinkedHashMap<?, ?> posCache = LinkedHashMap.class.cast(cacheField.get(addressCache));
                        LinkedHashMap<?, ?> negCache = LinkedHashMap.class.cast(cacheField.get(negativeCache));

                        // Both - addressCache and negativeCache -  are guarded by addressCache mutex
                        InetAddressCache positiveInetAddressCache = new InetAddressCache(posCache, addressCache);
                        InetAddressCache negativeInetAddressCache = new InetAddressCache(negCache, addressCache);

                        tmp = new InetAddressCaches(positiveInetAddressCache, negativeInetAddressCache);
                        caches = tmp;
                    } catch (Exception e) {
                        LOGGER.error("Failed to initialze fields for InetAddress cache", e);
                    }
                }
            }
        }
        return tmp;
    }

    /**
     * Flushes the JVM-internal caches for DNS look-ups.
     */
    public static void flushInetAddressCache() {
        InetAddressCaches caches = getInetAddressCaches();
        if (null == caches) {
            LOGGER.warn("Failed to flush DNS look-up caches");
            return;
        }
        caches.clearCaches();
    }

    /**
     * Flushes the specified host name from JVM-internal caches for DNS look-ups.
     *
     * @param hostName The host name to remove
     */
    public static void flushInetAddressCacheFor(String hostName) {
        if (Strings.isEmpty(hostName)) {
            return;
        }

        InetAddressCaches caches = getInetAddressCaches();
        if (null == caches) {
            LOGGER.warn("Failed to flush DNS look-up caches for {}", hostName);
            return;
        }
        caches.clearCachesFor(hostName);
    }



    /**
     * Gets a listing of available protocols; e.g. <code>"SSLv3 TLSv1 TLSv1.1 TLSv1.2"</code>
     *
     * @return The protocols' listing
     */
    public static String getProtocolsListing() {
        List<String> protocols = getProtocols();
        StringBuilder sb = new StringBuilder();
        for (String protocol : protocols) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(protocol);
        }
        return sb.toString();
    }

    /**
     * Gets an array containing available protocols.
     *
     * @return The array containing available protocols
     */
    public static String[] getProtocolsArray() {
        List<String> protocols = getProtocols();
        return protocols.toArray(new String[protocols.size()]);
    }

    /**
     * Gets an unmodifiable list containing all the installed providers.
     * <p>
     * The order of the providers in the {@link List} is their alphabetical order.
     *
     * @return an unmodifiable list containing all the installed provider
     */
    public static List<String> getProtocols() {
        List<String> protocols = new LinkedList<String>();
        for (Provider provider : Security.getProviders()) {
            parseProvider(protocols, provider);
        }
        Collections.sort(protocols);
        return Collections.unmodifiableList(protocols);
    }

    /**
     * Parses the SSL protocols of the specified {@link Provider} and adds them to the specified {@link List}
     *
     * @param protocols The {@link List} with all parsed protocols
     * @param provider The {@link Provider} for which to parse the SSL protocols
     */
    private static void parseProvider(List<String> protocols, Provider provider) {
        for (Object prop : provider.keySet()) {
            if (!(prop instanceof String)) {
                continue;
            }
            String key = (String) prop;
            if (key.startsWith("SSLContext.") && !key.equals("SSLContext.Default") && key.matches(".*[0-9].*")) {
                protocols.add(key.substring("SSLContext.".length()));
            } else if (key.startsWith("Alg.Alias.SSLContext.") && key.matches(".*[0-9].*")) {
                protocols.add(key.substring("Alg.Alias.SSLContext.".length()));
            }
        }
    }

}
