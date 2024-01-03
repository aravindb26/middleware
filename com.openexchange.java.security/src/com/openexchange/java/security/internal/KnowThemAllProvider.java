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

package com.openexchange.java.security.internal;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.java.IReference;
import com.openexchange.java.ImmutableReference;
import com.openexchange.java.security.internal.JavaSecurityProviderReplacer.LoggerHolder;

/**
 * {@link KnowThemAllProvider} - A Java Security Provider that iterates all known providers in order to acquire the appropriate service for
 * a type and algorithm tuple. The result is cached to avoid steady look-ups.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
class KnowThemAllProvider extends Provider {

    private static final long serialVersionUID = -6836133874712166288L;

    private static final String NAME = "Open-Xchange All Provider";

    private final AtomicReference<Provider[]> providersReference;
    private final LoadingCache<TypeAndAlgorithm, IReference<Service>> serviceCache;

    /**
     * Initializes a new {@link KnowThemAllProvider}.
     *
     * @param providers All providers
     */
    KnowThemAllProvider(Provider[] providers) {
        super(NAME, 1.0D, "Open-Xchange Provider knowing all other providers");
        AtomicReference<Provider[]> providersReference = new AtomicReference<Provider[]>(providers);
        this.providersReference = providersReference;
        serviceCache = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES).build(new CacheLoader<TypeAndAlgorithm, IReference<Service>>() {

            @Override
            public IReference<Service> load(TypeAndAlgorithm key) throws NoSuchAlgorithmException {
                Provider[] providers = providersReference.get();
                if (providers != null) {
                    for (Provider provider : providers) {
                        Service service = provider.getService(key.getType(), key.getAlgorithm());
                        if (service != null) {
                            return ImmutableReference.immutableReferenceFor(service);
                        }
                    }
                }
                return ImmutableReference.immutableReferenceFor(null);
            }
        });
    }

    /**
     * Checks for new providers.
     */
    synchronized void checkChanged() {
        Provider[] providers = Security.getProviders();
        Map<String, Provider> current = new HashMap<>(providers.length);
        for (Provider provider : providers) {
            String name = provider.getName();
            if (NAME.equals(name) == false) {
                current.put(name, provider);
            }
        }
        if (current.isEmpty()) {
            return;
        }

        Provider[] knownProviders = providersReference.get();
        Map<String, Provider> tmp = new HashMap<>(knownProviders.length);
        for (Provider provider : knownProviders) {
            tmp.put(provider.getName(), provider);
        }

        // Add new ones as well
        Set<String> newOnes = new HashSet<String>(current.keySet());
        newOnes.removeAll(tmp.keySet());
        if (newOnes.isEmpty()) {
            return;
        }

        Map<String, Provider> newRemovedProviders = null;

        List<Provider> addToKnownProviders = new ArrayList<Provider>(newOnes.size());
        for (String name : newOnes) {
            Provider provider = current.get(name);
            if (provider != null) {
                Security.removeProvider(provider.getName());
                addToKnownProviders.add(provider);
                if (newRemovedProviders == null) {
                    Map<String, Provider> removedProviders = JavaSecurityProviderReplacer.removedProvidersReference.get();
                    newRemovedProviders = removedProviders == null ? new HashMap<String, Provider>() : new HashMap<String, Provider>(removedProviders);
                }
                newRemovedProviders.put(provider.getName(), provider);
                LoggerHolder.LOG.info("{}{}{}Removed Java Security Provider '{}'{}", JavaSecurityProviderReplacer.LF, JavaSecurityProviderReplacer.LF, "\t", provider.getName(), JavaSecurityProviderReplacer.LF);
            }
        }

        List<Provider> newKnownProviders = new ArrayList<Provider>(knownProviders.length + addToKnownProviders.size());
        for (Provider provider : addToKnownProviders) {
            newKnownProviders.add(provider);
        }
        for (Provider provider : knownProviders) {
            newKnownProviders.add(provider);
        }
        providersReference.set(newKnownProviders.toArray(new Provider[newKnownProviders.size()]));
        serviceCache.invalidateAll();

        if (newRemovedProviders != null) {
            synchronized (JavaSecurityProviderReplacer.class) {
                JavaSecurityProviderReplacer.removedProvidersReference.set(ImmutableMap.copyOf(newRemovedProviders));
            }
        }
    }

    @Override
    public Service getService(String type, String algorithm) {
        try {
            return serviceCache.get(new TypeAndAlgorithm(type, algorithm)).getValue();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause == null ? e : cause);
        } catch (UncheckedExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }
}