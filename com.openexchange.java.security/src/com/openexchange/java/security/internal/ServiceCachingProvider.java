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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.java.IReference;
import com.openexchange.java.ImmutableReference;

/**
 * {@link ServiceCachingProvider} - Wraps a provider and caches the results of {@link Provider#getService(String, String)} invocations.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
class ServiceCachingProvider extends Provider {

    private static final long serialVersionUID = -4061410577932722773L;

    private final Provider provider;
    private volatile LoadingCache<TypeAndAlgorithm, IReference<Service>> serviceCache;

    /**
     * Initializes a new {@link ServiceCachingProvider}.
     *
     * @param provider The provider to delegate to
     */
    ServiceCachingProvider(Provider provider) {
        super(provider.getName(), provider.getVersion(), provider.getInfo());
        this.provider = provider;
    }

    private LoadingCache<TypeAndAlgorithm, IReference<Service>> getServiceCache() {
        LoadingCache<TypeAndAlgorithm, IReference<Service>> serviceCache = this.serviceCache;
        if (serviceCache == null) {
            synchronized (this) {
                serviceCache = this.serviceCache;
                if (serviceCache == null) {
                    Provider provider = this.provider;
                    CacheLoader<TypeAndAlgorithm, IReference<Service>> loader = new CacheLoader<TypeAndAlgorithm, IReference<Service>>() {

                        @Override
                        public IReference<Service> load(TypeAndAlgorithm key) throws NoSuchAlgorithmException {
                            // Reference for service or null
                            return ImmutableReference.immutableReferenceFor(provider.getService(key.getType(), key.getAlgorithm()));
                        }
                    };
                    serviceCache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build(loader);
                    this.serviceCache = serviceCache;
                }
            }
        }
        return serviceCache;
    }

    @Override
    public String getName() {
        return provider.getName();
    }

    @Override
    public double getVersion() {
        return provider.getVersion();
    }

    @Override
    public String getInfo() {
        return provider.getInfo();
    }

    @Override
    public String toString() {
        return provider.toString();
    }

    @Override
    public Service getService(String type, String algorithm) {
        try {
            return getServiceCache().get(new TypeAndAlgorithm(type, algorithm)).getValue();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException(cause == null ? e : cause);
        } catch (UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {
        LoadingCache<TypeAndAlgorithm, IReference<Service>> serviceCache = this.serviceCache;
        if (serviceCache != null) {
            serviceCache.invalidateAll();
        }
        provider.clear();
    }

    @Override
    public Object setProperty(String key, String value) {
        return provider.setProperty(key, value);
    }

    @Override
    public void load(Reader reader) throws IOException {
        provider.load(reader);
    }

    @Override
    public void load(InputStream inStream) throws IOException {
        provider.load(inStream);
    }

    @Override
    public void putAll(Map<?, ?> t) {
        provider.putAll(t);
    }

    @Override
    public Set<java.util.Map.Entry<Object, Object>> entrySet() {
        return provider.entrySet();
    }

    @Override
    public Set<Object> keySet() {
        return provider.keySet();
    }

    @Override
    public Collection<Object> values() {
        return provider.values();
    }

    @Override
    public int size() {
        return provider.size();
    }

    @Override
    public boolean isEmpty() {
        return provider.isEmpty();
    }

    @Override
    public Object put(Object key, Object value) {
        return provider.put(key, value);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return provider.putIfAbsent(key, value);
    }

    @Override
    public boolean contains(Object value) {
        return provider.contains(value);
    }

    @Override
    public Object remove(Object key) {
        return provider.remove(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return provider.containsValue(value);
    }

    @Override
    public boolean containsKey(Object key) {
        return provider.containsKey(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return provider.remove(key, value);
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return provider.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(Object key, Object value) {
        return provider.replace(key, value);
    }

    @Override
    public void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
        provider.replaceAll(function);
    }

    @Override
    public Object compute(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return provider.compute(key, remappingFunction);
    }

    @Override
    public Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
        return provider.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return provider.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
        return provider.merge(key, value, remappingFunction);
    }

    @Override
    public Object get(Object key) {
        return provider.get(key);
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return provider.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super Object, ? super Object> action) {
        provider.forEach(action);
    }

    @Override
    public Enumeration<Object> keys() {
        return provider.keys();
    }

    @Override
    public Enumeration<Object> elements() {
        return provider.elements();
    }

    @Override
    public String getProperty(String key) {
        return provider.getProperty(key);
    }

    @Override
    public void save(OutputStream out, String comments) {
        provider.save(out, comments);
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        provider.store(writer, comments);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        provider.store(out, comments);
    }

    @Override
    public void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        provider.loadFromXML(in);
    }

    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {
        provider.storeToXML(os, comment);
    }

    @Override
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        provider.storeToXML(os, comment, encoding);
    }

    @Override
    public Set<Service> getServices() {
        return provider.getServices();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return provider.getProperty(key, defaultValue);
    }

    @Override
    public Enumeration<?> propertyNames() {
        return provider.propertyNames();
    }

    @Override
    public Set<String> stringPropertyNames() {
        return provider.stringPropertyNames();
    }

    @Override
    public void list(PrintStream out) {
        provider.list(out);
    }

    @Override
    public void list(PrintWriter out) {
        provider.list(out);
    }
}