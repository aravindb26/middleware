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

import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableMap;
import com.openexchange.java.Strings;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link JavaSecurityProviderReplacer}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class JavaSecurityProviderReplacer {

    /** Simple class to delay initialization until needed */
    static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JavaSecurityProviderReplacer.class);
    }

    static final String LF = Strings.getLineSeparator();

    private static ScheduledTimerTask timerTask;
    static final AtomicReference<Map<String, Provider>> removedProvidersReference = new AtomicReference<Map<String,Provider>>();
    private static KnowThemAllProvider allProvider = null;

    /**
     * Replaces security providers.
     *
     * @param useAllProvider Whether to use all provider or not
     * @param timerService The timer service to schedule timer task
     */
    public static synchronized void replaceAndStartTimer(boolean useAllProvider, TimerService timerService) {
        Map<String, Provider> tmp = removedProvidersReference.get();
        if (tmp == null) {
            Map<String, Provider> removedOnes;
            if (useAllProvider) {
                // Use a know-them-all provider caching looked-up service instance
                Provider[] providers = getReverseProviders();
                removedOnes = new HashMap<>(providers.length);
                for (Provider provider : providers) {
                    Security.removeProvider(provider.getName());
                    removedOnes.put(provider.getName(), provider);
                }
                KnowThemAllProvider knowThemAllProvider = new KnowThemAllProvider(providers);
                Security.addProvider(knowThemAllProvider);
                allProvider = knowThemAllProvider;

                Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        knowThemAllProvider.checkChanged();
                    }
                };
                timerTask = timerService.scheduleWithFixedDelay(task, 5, 5, TimeUnit.MINUTES);
            } else {
                // Replace each provider with appropriate ServiceCachingProvider instance
                Provider[] providers = Security.getProviders();
                removedOnes = new HashMap<>(providers.length);
                for (Provider provider : providers) {
                    Security.removeProvider(provider.getName());
                    removedOnes.put(provider.getName(), provider);
                    Security.addProvider(new ServiceCachingProvider(provider));
                }

                Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        JavaSecurityProviderReplacer.checkChanged();
                    }
                };
                timerTask = timerService.scheduleWithFixedDelay(task, 5, 5, TimeUnit.MINUTES);
            }
            removedProvidersReference.set(ImmutableMap.copyOf(removedOnes));
            LoggerHolder.LOG.info("{}{}{}Replaced Java Security Providers!{}", LF, LF, "\t", LF);
        }
    }

    /**
     * Gets the currently registered providers in reverse order.
     *
     * @return The providers in reverse order
     */
    private static Provider[] getReverseProviders() {
        Provider[] providers = Security.getProviders();
        Provider[] reverseProviders = new Provider[providers.length];
        System.arraycopy(providers, 0, reverseProviders, 0, providers.length);
        reverse(reverseProviders);
        return reverseProviders;
    }

    /**
     * Restores security providers if necessary.
     * <p>
     * Does nothing at all if not previously replaced.
     */
    public static synchronized void restoreIfNecessary() {
        // Stop timer task
        ScheduledTimerTask tt = timerTask;
        if (tt != null) {
            timerTask = null;
            tt.cancel();
        }

        KnowThemAllProvider knowThemAllProvider = allProvider;
        if (knowThemAllProvider != null) {
            allProvider = null;
            Security.removeProvider(knowThemAllProvider.getName());
        }

        Map<String, Provider> tmp = removedProvidersReference.get();
        if (tmp != null) {
            for (Provider provider : tmp.values()) {
                Security.removeProvider(provider.getName());
                Security.addProvider(provider);
            }
            removedProvidersReference.set(null);
            LoggerHolder.LOG.info("{}{}{}Restored Java Security Providers!{}", LF, LF, "\t", LF);
        }
    }

    /**
     * Checks about changes to Java Security Providers.
     */
    static synchronized void checkChanged() {
        Map<String, Provider> tmp = removedProvidersReference.get();
        if (tmp != null) {
            Provider[] providers = Security.getProviders();

            Map<String, Provider> current = new HashMap<>(providers.length);
            for (Provider provider : providers) {
                current.put(provider.getName(), provider);
            }

            Map<String, Provider> newRemovedProviders = null;

            // Add new ones as well
            Set<String> newOnes = new HashSet<String>(current.keySet());
            newOnes.removeAll(tmp.keySet());
            for (String name : newOnes) {
                Provider provider = current.get(name);
                if (provider != null && !(provider instanceof ServiceCachingProvider)) {
                    Security.removeProvider(provider.getName());
                    Security.addProvider(new ServiceCachingProvider(provider));
                    if (newRemovedProviders == null) {
                        newRemovedProviders = new HashMap<String, Provider>(tmp);
                    }
                    newRemovedProviders.put(provider.getName(), provider);
                    LoggerHolder.LOG.info("{}{}{}Replaced Java Security Provider '{}'{}", LF, LF, "\t", provider.getName(), LF);
                }
            }

            // Drop removed ones
            Set<String> removedOnes = new HashSet<String>(tmp.keySet());
            removedOnes.removeAll(current.keySet());
            for (String name : removedOnes) {
                if (newRemovedProviders == null) {
                    newRemovedProviders = new HashMap<String, Provider>(tmp);
                }
                newRemovedProviders.remove(name);
            }

            if (newRemovedProviders != null) {
                removedProvidersReference.set(ImmutableMap.copyOf(newRemovedProviders));
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static void reverse(Object[] array) {
        if (array == null) {
            return;
        }

        int i = 0;
        int j = array.length - 1;
        Object tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

}
