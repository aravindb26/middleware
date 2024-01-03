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

package com.openexchange.config.cascade.impl;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import com.openexchange.config.cascade.BasicProperty;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigCascadeExceptionCodes;
import com.openexchange.config.cascade.ConfigProperty;
import com.openexchange.config.cascade.ConfigProviderService;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.exception.OXException;
import com.openexchange.java.ImmutableReference;
import com.openexchange.tools.strings.StringParser;

/**
 * {@link ConfigCascade}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class ConfigCascade implements ConfigViewFactory {

    /** The logger constant */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigCascade.class);

    // ----------------------------------------- Cache stuff -------------------------------------------------------------------------------

    private static final Object PRESENT = new Object();

    /** The mapping for cached property names: context -&gt; user -&gt; property-name */
    static final ConcurrentMap<Integer, ConcurrentMap<Integer, Map<String, Object>>> USER_KEYS = new ConcurrentHashMap<>(32, 0.9F, 1);

    /** The cache for config-cascade values */
    static final Cache<PropKey, ImmutableReference<String>> CACHED_VALUES;
    static {
        // Specify removal listener that cares about adjusting other maps and firing event
        RemovalListener<PropKey, ImmutableReference<String>> removalListener = notification -> {
            PropKey key = notification.getKey();
            ConcurrentMap<Integer, Map<String, Object>> map = USER_KEYS.get(I(key.contextId));
            if (null != map) {
                Map<String, Object> propNames = map.get(I(key.userId));
                if (propNames != null) {
                    propNames.remove(key.propertyName);
                }
            }
        };
        CACHED_VALUES = CacheBuilder.newBuilder().expireAfterWrite(360, TimeUnit.SECONDS).removalListener(removalListener).build();
    }

    /**
     * Clears all cached values.
     */
    public static void clearCachedValues() {
        CACHED_VALUES.invalidateAll();
        USER_KEYS.clear();
        LOG.debug("Cleared all cached values of config-cascade");
    }

    /**
     * Clears the cache values.
     *
     * @param propertyName The name of the property
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void clearCachedValue(String propertyName, int userId, int contextId) {
        CACHED_VALUES.invalidate(new PropKey(propertyName, userId, contextId));
    }

    /**
     * Clears all cached values of given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public static void clearCachedValuesOfUser(int userId, int contextId) {
        Integer iContextId = Integer.valueOf(contextId);
        ConcurrentMap<Integer, Map<String, Object>> map = USER_KEYS.get(iContextId);
        if (null == map) {
            return;
        }

        Integer iUserId = Integer.valueOf(userId);
        Map<String, Object> values = map.remove(iUserId);
        if (values == null || values.isEmpty()) {
            return;
        }

        for (String propertyName : new HashSet<>(values.keySet())) {
            // Adjusting other maps is performed in RemovalListener...
            CACHED_VALUES.invalidate(new PropKey(propertyName, userId, contextId));
        }
        LOG.debug("Cleared all cached values of config-cascade for user {} in context {}", I(userId), I(contextId));
    }

    /**
     * Clears all cached values of given context.
     *
     * @param contextId The context identifier
     */
    public static void clearCachedValuesOfContext(int contextId) {
        Integer iContextId = Integer.valueOf(contextId);
        ConcurrentMap<Integer, Map<String, Object>> map = USER_KEYS.get(iContextId);
        if (null == map) {
            return;
        }

        for (Map.Entry<Integer, Map<String, Object>> propNamesEntry : map.entrySet()) {
            int userId = propNamesEntry.getKey().intValue();
            Map<String, Object> propNames = propNamesEntry.getValue();
            for (String propertyName : new HashSet<>(propNames.keySet())) {
                // Adjusting other maps is performed in RemovalListener...
                CACHED_VALUES.invalidate(new PropKey(propertyName, userId, contextId));
            }
        }
        LOG.debug("Cleared all cached values of config-cascade for context {}", I(contextId));
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final ConcurrentMap<String, ConfigProviderService> providers;
    private final SearchPath searchPath;
    private final AtomicReference<StringParser> stringParserReference;
    private final boolean useConfigCascadeCache;

    /**
     * Initializes a new {@link ConfigCascade}.
     *
     * @param useConfigCascadeCache Whether to use config-cascade cache
     */
    public ConfigCascade(boolean useConfigCascadeCache) {
        super();
        this.useConfigCascadeCache = useConfigCascadeCache;
        ConcurrentMap<String, ConfigProviderService> providers = new ConcurrentHashMap<String, ConfigProviderService>(8, 0.9F, 1);
        this.providers = providers;
        searchPath = new SearchPath(providers);
        stringParserReference = new AtomicReference<StringParser>(null);
    }

    /**
     * Sets given provider with specified scope.
     *
     * @param scope The provider's scope
     * @param configProvider The provider
     */
    public void setProvider(String scope, ConfigProviderService configProvider) {
        providers.put(scope, configProvider);
        // Adjusting other maps is performed in RemovalListener...
        CACHED_VALUES.invalidateAll();
    }

    @Override
    public void clearCache() throws OXException {
        // Adjusting other maps is performed in RemovalListener...
        CACHED_VALUES.invalidateAll();
    }

    @Override
    public ConfigView getView(int userId, int contextId) {
        int user = userId <= 0 ? -1 : userId;
        int context = contextId <= 0 ? -1 : contextId;
        return new View(user, context, providers, searchPath.getSearchPathReference(), getConfigProviders(), stringParserReference.get(), useConfigCascadeCache);
    }

    @Override
    public ConfigView getView() {
        return new View(-1, -1, providers, searchPath.getSearchPathReference(), getConfigProviders(), stringParserReference.get(), useConfigCascadeCache);
    }

    public void setSearchPath(String... searchPath) {
        this.searchPath.setSearchPath(searchPath);
    }

    @Override
    public String[] getSearchPath() {
        return searchPath.getSearchPathReference();
    }

    protected List<ConfigProviderService> getConfigProviders() {
        return searchPath.getConfigProviders();
    }

    public void setStringParser(StringParser stringParser) {
        stringParserReference.set(stringParser);
    }

    // ------------------------------------------------------------------------------------------

    private static final class View implements ConfigView {

        final int contextId;
        final int userId;
        final String[] searchPath;
        final ConcurrentMap<String, ConfigProviderService> providers;
        final StringParser stringParser;
        final boolean useConfigCascadeCache;
        private final List<ConfigProviderService> configProviders;

        View(int userId, int contextId, ConcurrentMap<String, ConfigProviderService> providers, String[] searchPath, List<ConfigProviderService> configProviders, StringParser stringParser, boolean useConfigCascadeCache) {
            super();
            this.userId = userId;
            this.contextId = contextId;
            this.providers = providers;
            this.searchPath = searchPath;
            this.configProviders = configProviders;
            this.stringParser = stringParser;
            this.useConfigCascadeCache = useConfigCascadeCache;
        }

        @Override
        public <T> void set(String scope, String propertyName, T value) throws OXException {
            ((ConfigProperty<T>) property(scope, propertyName, value.getClass())).set(value);
        }

        @Override
        public <T> T get(String propertyName, Class<T> coerceTo) throws OXException {
            return property(propertyName, coerceTo).get();
        }

        @Override
        public <T> T opt(String propertyName, java.lang.Class<T> coerceTo, T defaultValue) throws OXException {
            T value = property(propertyName, coerceTo).get();
            return value != null ? value : defaultValue;
        }

        @Override
        public <T> ConfigProperty<T> property(String scope, String propertyName, Class<T> coerceTo) throws OXException {
            ConfigProviderService configProviderService = providers.get(scope);
            if (configProviderService == null) {
                // No such config provider for specified scope
                return new CoercingConfigProperty<T>(coerceTo, new NonExistentBasicProperty(propertyName, scope), stringParser, null);
            }
            Runnable clearTask = () -> clearCachedValue(propertyName, userId, contextId);
            return new CoercingConfigProperty<T>(coerceTo, configProviderService.get(propertyName, contextId, userId), stringParser, clearTask);
        }

        @Override
        public <T> ComposedConfigProperty<T> property(String propertyName, Class<T> coerceTo) {
            return new CoercingComposedConfigProperty<T>(coerceTo, new DefaultComposedConfigProperty(propertyName, this), stringParser);
        }

        @Override
        public Map<String, ComposedConfigProperty<String>> all() throws OXException {
            Set<String> names = new HashSet<String>();
            for (ConfigProviderService provider : configProviders) {
                names.addAll(provider.getAllPropertyNames(contextId, userId));
            }

            Map<String, ComposedConfigProperty<String>> properties = new HashMap<String, ComposedConfigProperty<String>>();
            for (String name : names) {
                properties.put(name, property(name, String.class));
            }

            return properties;
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class DefaultComposedConfigProperty implements ComposedConfigProperty<String> {

        private final String propertyName;
        private final View view;
        private String[] overriddenStrings; // DefaultComposedConfigProperty is always created per thread. No need for atomicity

        DefaultComposedConfigProperty(String propertyName, View view) {
            super();
            this.propertyName = propertyName;
            this.view = view;
            overriddenStrings = null;
        }

        private String getFinalScope() throws OXException {
            return view.property(propertyName, String.class).precedence(ConfigViewScope.SERVER, ConfigViewScope.RESELLER, ConfigViewScope.CONTEXT, ConfigViewScope.USER).get("final");
        }

        private List<ConfigProviderService> getConfigProviders(String finalScope) {
            String[] overriddenStrings = this.overriddenStrings;
            String[] s = (overriddenStrings == null) ? view.searchPath : overriddenStrings;

            List<ConfigProviderService> p = new ArrayList<ConfigProviderService>(s.length);
            boolean collect = false;
            for (String scope : s) {
                collect = collect || finalScope == null || finalScope.equals(scope);

                if (collect) {
                    ConfigProviderService providerService = view.providers.get(scope);
                    if (providerService != null) {
                        p.add(providerService);
                    }
                }
            }
            return p;
        }

        @Override
        public String get() throws OXException {
            if (!view.useConfigCascadeCache || this.overriddenStrings != null) {
                // Don't use cache Thus load value regularly...
                return loadValue();
            }

            // Avoid creating instance of `LoadValueCallable` for each invocation & pre-check via `getIfPresent()`
            PropKey propKey = new PropKey(propertyName, view.userId, view.contextId);
            ImmutableReference<String> cached = CACHED_VALUES.getIfPresent(propKey);
            if (cached != null) {
                LOG.debug("ConfigCascade: Returning cached value for {}", propKey);
                return cached.getValue();
            }

            // Well, then get the value from cache, obtaining it from loader if necessary.
            try {
                return CACHED_VALUES.get(propKey, new LoadValueCallable(propKey, this)).getValue();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                throw cause instanceof OXException ? (OXException) cause : new OXException(cause);
            }
        }

        /**
         * Loads the value from suitable config provider; invoked by <code>LoadValueCallable</code> instance.
         *
         * @return The loaded value or <code>null</code>
         * @throws OXException If loading value fails
         */
        String loadValue() throws OXException {
            String finalScope = getFinalScope();
            for (ConfigProviderService provider : getConfigProviders(finalScope)) {
                String value = provider.get(propertyName, view.contextId, view.userId).get();
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public String get(String metadataName) throws OXException {
            for (ConfigProviderService provider : getConfigProviders(null)) {
                String value = provider.get(propertyName, view.contextId, view.userId).get(metadataName);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public String getScope() throws OXException {
            String finalScope = getFinalScope();
            for (ConfigProviderService provider : getConfigProviders(finalScope)) {
                String value = provider.get(propertyName, view.contextId, view.userId).get();
                if (value != null) {
                    return provider.getScope();
                }
            }
            return null;
        }

        @Override
        public <M> M get(String metadataName, Class<M> m) throws OXException {
            for (ConfigProviderService provider : getConfigProviders(null)) {
                String value = provider.get(propertyName, view.contextId, view.userId).get(metadataName);
                if (value != null) {
                    M parsed = view.stringParser.parse(value, m);
                    if (parsed == null) {
                        throw ConfigCascadeExceptionCodes.COULD_NOT_COERCE_VALUE.create(value, m.getName());
                    }
                    return parsed;
                }
            }
            return null;
        }

        @Override
        public List<String> getMetadataNames() throws OXException {
            Set<String> metadataNames = new HashSet<String>();
            for (ConfigProviderService provider : getConfigProviders(null)) {
                BasicProperty basicProperty = provider.get(propertyName, view.contextId, view.userId);
                if (basicProperty != null) {
                    metadataNames.addAll(basicProperty.getMetadataNames());
                }
            }
            return new ArrayList<String>(metadataNames);
        }

        @Override
        public <M> ComposedConfigProperty<String> set(String metadataName, M value) throws OXException {
            throw new UnsupportedOperationException("Unscoped set is not supported");
        }

        @Override
        public ComposedConfigProperty<String> set(String value) throws OXException {
            throw new UnsupportedOperationException("Unscoped set is not supported");
        }

        @Override
        public ComposedConfigProperty<String> precedence(ConfigViewScope... scopes) throws OXException {
            String[] scopez = new String[scopes.length];
            for (int i = scopez.length; i-- > 0;) {
                scopez[i] = scopes[i].getScopeName();
            }
            overriddenStrings = scopez;
            return this;
        }

        @Override
        public ComposedConfigProperty<String> precedence(String... scopes) throws OXException {
            overriddenStrings = scopes;
            return this;
        }

        @Override
        public boolean isDefined() throws OXException {
            String finalScope = getFinalScope();
            for (ConfigProviderService provider : getConfigProviders(finalScope)) {
                boolean defined = provider.get(propertyName, view.contextId,view.userId).isDefined();
                if (defined) {
                    return defined;
                }
            }
            return false;
        }

        @Override
        public <M> ComposedConfigProperty<M> to(Class<M> otherType) throws OXException {
            return new CoercingComposedConfigProperty<M>(otherType, this, view.stringParser);
        }
    }

    /** Used by cache when loading a value from suitable config provider is needed */
    private static class LoadValueCallable implements Callable<ImmutableReference<String>> {

        private final PropKey key;
        private final DefaultComposedConfigProperty property;

        LoadValueCallable(PropKey key, DefaultComposedConfigProperty property) {
            super();
            this.key = key;
            this.property = property;
        }

        @Override
        public ImmutableReference<String> call() throws Exception {
            ImmutableReference<String> valueRef = new ImmutableReference<String>(property.loadValue());
            addValueToOtherMap(key);
            LOG.debug("ConfigCascade: Loaed value for {}", key);
            return valueRef;
        }

        /**
         * Adds specified key's fields (property name, user identifier, and context identifier) to other map.
         *
         * @param key The key providing the fields to add
         */
        private static void addValueToOtherMap(PropKey key) {
            Integer iContextId = Integer.valueOf(key.contextId);
            ConcurrentMap<Integer, Map<String, Object>> map = USER_KEYS.get(iContextId);
            if (null == map) {
                ConcurrentMap<Integer, Map<String, Object>> newMap = new ConcurrentHashMap<Integer, Map<String,Object>>(32, 0.9F, 1);
                map = USER_KEYS.putIfAbsent(iContextId, newMap);
                if (null == map) {
                    map = newMap;
                }
            }

            Integer iUserId = Integer.valueOf(key.userId);
            Map<String, Object> values = map.get(iUserId);
            if (values == null) {
                Map<String, Object> newSet = new ConcurrentHashMap<String, Object>(16, 0.9F, 1);
                values = map.putIfAbsent(iUserId, newSet);
                if (null == values) {
                    values = newSet;
                }
            }
            values.put(key.propertyName, PRESENT);
        }
    }

    private static class NonExistentBasicProperty implements BasicProperty {

        private final String property;
        private final String scope;

        NonExistentBasicProperty(String property, String scope) {
            super();
            this.property = property;
            this.scope = scope;
        }

        @Override
        public void set(String metadataName, String value) throws OXException {
            throw ConfigCascadeExceptionCodes.CAN_NOT_DEFINE_METADATA.create(metadataName, scope);
        }

        @Override
        public void set(String value) throws OXException {
            throw ConfigCascadeExceptionCodes.CAN_NOT_SET_PROPERTY.create(property, scope);
        }

        @Override
        public boolean isDefined() throws OXException {
            return false;
        }

        @Override
        public List<String> getMetadataNames() throws OXException {
            return Collections.emptyList();
        }

        @Override
        public String get(String metadataName) throws OXException {
            return null;
        }

        @Override
        public String get() throws OXException {
            return null;
        }
    }

    private static final class SearchPath {

        private final ConcurrentMap<String, ConfigProviderService> providers;
        private final AtomicReference<String[]> searchPathReference;
        private final AtomicReference<List<ConfigProviderService>> path;

        SearchPath(ConcurrentMap<String, ConfigProviderService> providers) {
            super();
            this.providers = providers;
            searchPathReference = new AtomicReference<String[]>(null);
            List<ConfigProviderService> path = Collections.emptyList();
            this.path = new AtomicReference<List<ConfigProviderService>>(path);
        }

        void setSearchPath(String... searchPath) {
            this.searchPathReference.set(searchPath);
            this.path.set(null); // Enforce re-initialization
        }

        String[] getSearchPathReference() {
            return searchPathReference.get();
        }

        List<ConfigProviderService> getConfigProviders() {
            List<ConfigProviderService> path = this.path.get();
            if (null == path) {
                synchronized (this) {
                    path = this.path.get();
                    if (null == path) {
                        path = computePathFrom(searchPathReference.get(), providers);
                        this.path.set(path);
                    }
                }
            }
            return path;
        }

        private static List<ConfigProviderService> computePathFrom(String[] searchPath, ConcurrentMap<String, ConfigProviderService> providers) {
            if (null == searchPath) {
                return Collections.emptyList();
            }

            ImmutableList.Builder<ConfigProviderService> p = ImmutableList.builder();
            for (String scope : searchPath) {
                ConfigProviderService configProvider = providers.get(scope);
                if (null == configProvider) {
                    throw new IllegalStateException("No such config provider for scope: " + scope);
                }
                p.add(configProvider);
            }
            return p.build();
        }
    }

}
