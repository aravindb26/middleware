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

package com.openexchange.deputy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.google.common.collect.ImmutableList;
import com.openexchange.deputy.DeputyModuleProvider;

/**
 * {@link DeputyModuleProviderRegistry}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyModuleProviderRegistry implements ServiceTrackerCustomizer<DeputyModuleProvider, DeputyModuleProvider> {

    private final BundleContext context;
    private final ConcurrentMap<String, List<DeputyModuleProvider>> trackedProviders;
    private final Comparator<DeputyModuleProvider> comparator;

    /**
     * Initializes a new {@link DeputyModuleProviderRegistry}.
     *
     * @param context The bundle context used for tracking
     */
    public DeputyModuleProviderRegistry(BundleContext context) {
        super();
        this.context = context;
        trackedProviders = new ConcurrentHashMap<>();
        comparator = new Comparator<DeputyModuleProvider>() {

            @Override
            public int compare(DeputyModuleProvider o1, DeputyModuleProvider o2) {
                int x = o1.getRanking();
                int y = o2.getRanking();
                return (x < y) ? 1 : ((x == y) ? 0 : -1);
            }
        };
    }

    // -------------------------------------------------- Registry methods -----------------------------------------------------------------

    /**
     * Gets the highest-ranked provider for given module identifier.
     *
     * @param moduleId The module identifier
     * @return The highest-ranked provider or <code>null</code>
     */
    public DeputyModuleProvider getHighestRankedProviderFor(String moduleId) {
        List<DeputyModuleProvider> providers = trackedProviders.get(moduleId);
        return providers == null || providers.isEmpty() ? null : providers.get(0);
    }

    /**
     * Gets all available providers for given module identifier.
     *
     * @param moduleId The module identifier
     * @return All available providers or an empty list
     */
    public List<DeputyModuleProvider> getProvidersFor(String moduleId) {
        List<DeputyModuleProvider> providers = trackedProviders.get(moduleId);
        return providers == null ? Collections.emptyList() : providers;
    }

    /**
     * Gets a listing of identifiers for available providers.
     *
     * @return A listing of identifiers for available providers
     */
    public List<String> getAvailableModuleIds() {
        List<String> providerIds = new ArrayList<String>(trackedProviders.keySet());
        Collections.sort(providerIds);
        return providerIds;
    }

    // ---------------------------------------------- Service tracker methods --------------------------------------------------------------

    @Override
    public synchronized DeputyModuleProvider addingService(ServiceReference<DeputyModuleProvider> reference) {
        DeputyModuleProvider provider = context.getService(reference);
        List<DeputyModuleProvider> providers = trackedProviders.get(provider.getModuleId());
        if (providers == null) {
            providers = Collections.singletonList(provider);
        } else {
            List<DeputyModuleProvider> newProviders = new ArrayList<DeputyModuleProvider>(providers);
            newProviders.add(provider);
            Collections.sort(newProviders, comparator);
            providers = ImmutableList.copyOf(newProviders);
        }
        trackedProviders.put(provider.getModuleId(), providers);
        return provider;
    }

    @Override
    public void modifiedService(ServiceReference<DeputyModuleProvider> reference, DeputyModuleProvider provider) {
        // Ignore
    }

    @Override
    public synchronized void removedService(ServiceReference<DeputyModuleProvider> reference, DeputyModuleProvider provider) {
        List<DeputyModuleProvider> providers = trackedProviders.get(provider.getModuleId());
        if (providers != null) {
            List<DeputyModuleProvider> newProviders = new ArrayList<DeputyModuleProvider>(providers);
            if (newProviders.remove(provider)) {
                if (newProviders.isEmpty()) {
                    trackedProviders.remove(provider.getModuleId());
                } else {
                    trackedProviders.put(provider.getModuleId(), ImmutableList.copyOf(newProviders));
                }
            }
        }
        context.ungetService(reference);
    }

}
