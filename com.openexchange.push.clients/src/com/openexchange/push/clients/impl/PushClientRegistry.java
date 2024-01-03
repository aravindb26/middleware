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
package com.openexchange.push.clients.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.NearRegistryServiceTracker;
import com.openexchange.push.clients.PushClientFactory;
import com.openexchange.push.clients.PushClientProvider;
import com.openexchange.push.clients.PushClientProviderFactory;

/**
 * {@link PushClientRegistry}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
@SuppressWarnings("rawtypes")
public class PushClientRegistry extends NearRegistryServiceTracker<PushClientFactory> implements PushClientProviderFactory, ForcedReloadable {

    private static class LoggerHolder {
        public static final Logger LOG = LoggerFactory.getLogger(PushClientRegistry.class);
    }

    /**
     * Initializes a new {@link PushClientRegistry}.
     *
     * @param context
     * @param configService
     */
    public PushClientRegistry(BundleContext context, ConfigurationService configService) {
        super(context, PushClientFactory.class);
        init(configService);
    }

    private final AtomicReference<Map<String, Map<String, Object>>> type2configRef = new AtomicReference<>(); // Map<String, Map<String, Object>> ymlFiles = new ConcurrentHashMap<>();
    private final Map<String, PushClientTypeWrapper> clients = new ConcurrentHashMap<>();

    @Override
    protected PushClientFactory onServiceAvailable(PushClientFactory service) {
        if (hasDuplicate(service)) {
            // Duplicate reader
            LoggerHolder.LOG.error("Duplicate push client config reader found for type {}", service.getType());
            return service;
        }

        readConfigs(service);
        return service;
    }

    @Override
    public void removedService(ServiceReference<PushClientFactory> reference, PushClientFactory service) {
        super.removedService(reference, service);
        clients.values().removeIf((wrapper) -> wrapper.getType().equals(service.getType()));
    }

    /**
     * Loads the config ymls and parses them with existing {@link PushClientFactory}s
     * 
     * @param configService The configuration service to use
     */
    @SuppressWarnings("unchecked")
    void init(ConfigurationService configService) {
        // load yml files
        Map<String, Object> yamlInFolder = configService.getYamlInFolder("pushClientConfig");
        // Flatten the files into a stream of configs (a mapping of clientId -> config)
        // @formatter:off
        Stream<Map<String, Map<String, Object>>> ymlFilesStream = yamlInFolder.values()
                                                                              .stream()
                                                                              .filter(yml -> yml instanceof Map<?,?>)
                                                                              .map(yml -> (Map<String, Map<String, Object>>) yml);
        // @formatter:on
        // Combine maps into one
        Map<String, Map<String, Object>> clientId2Config = ymlFilesStream.reduce(new HashMap(), (result, entry) -> {
            result.putAll(entry);
            return result;
        });

        // extract type
        // @formatter:off
         Map<String, Map<String, Object>> result = clientId2Config.entrySet()
                                                                  .stream()
                                                                  .collect(Collectors.groupingBy(entry -> (String) Optional.ofNullable(entry.getValue().get("_type")).orElse("_unknown"),
                                                                                                 Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
        // @formatter:on
        if (result.containsKey("_unknown")) {
            // @formatter:off
            String clients = result.get("_unknown")
                                   .keySet()
                                   .stream()
                                   .map(clientId -> "'" + clientId + "'")
                                   .collect(Collectors.joining(","));
            LoggerHolder.LOG.error("Found push client configurations without a '_type' field. Please check the configuration for the following clients: {}", clients);
            // @formatter:on
        }
        type2configRef.set(result);
        // invalidate client cache and read all load all configs (in case of reload)
        clients.clear();
        getServiceList().forEach(reader -> readConfigs(reader));
    }

    /**
     * Reads the config for all configs with a type that matches the given reader
     *
     * @param reader The reader to use
     */
    private void readConfigs(PushClientFactory reader) {
        Map<String, Map<String, Object>> type2configMap = type2configRef.get();
        if (type2configMap == null || type2configMap.containsKey(reader.getType()) == false) {
            // No map or no config with the readers type
            return;
        }
        // @formatter:off
        type2configMap.get(reader.getType())
                      .entrySet()
                      .forEach(config -> readConfig(reader, config));
        // @formatter:on
    }

    /**
     * Reads a single config entry with the given reader and adds the result to the clients map
     *
     * @param reader The reader to use
     * @param entry The entry to read
     */
    void readConfig(PushClientFactory<?> reader, Entry<String, Object> entry) {
        String id = entry.getKey();
        Object tmp = entry.getValue();
        if (tmp instanceof Map<?, ?> == false) {
            // config is invalid -> log error and skip it
            LoggerHolder.LOG.error("The push client config for id {} is invalid. Skipping it.", id);
            return;
        }
        @SuppressWarnings("unchecked") Map<String, Object> config = (Map<String, Object>) tmp;
        Object client;
        try {
            client = reader.create(config);
        } catch (OXException e) {
            // Log error and skip it
            LoggerHolder.LOG.error("Unable to load configuration for push client with id {}. Skipping it.", id, e);
            return;
        }
        PushClientTypeWrapper wrapper = new PushClientTypeWrapper(client, reader.getType());
        if (clients.putIfAbsent(id, wrapper) != null) {
            // Duplicate client id -> log error and skip
            LoggerHolder.LOG.warn("A push client config with the id {} already exists and will be skippped. Please check you configuration.", id);
            return;
        }
    }


    /**
     * Checks if a {@link PushClientFactory} with the same type already exists
     *
     * @param reader The {@link PushClientFactory} to check
     * @return <code>true</code> if there exists a duplicate {@link PushClientFactory}, <code>false</code> otherwise
     */
    private boolean hasDuplicate(PushClientFactory reader) {
        // @formatter:off
        return getServiceList().stream()
                               .anyMatch(otherReader -> otherReader.getType()
                                                                   .equals(reader.getType()));
        // @formatter:on
    }

    /**
     * Gets the client with the given id
     *
     * @param id The client id
     * @return The client
     */
    protected Optional<Object> getClient(String id) {
        PushClientTypeWrapper wrapper = clients.get(id);
        if (wrapper == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(wrapper.getClient());
    }

    @Override
    public <T> PushClientProvider<T> createProvider(Class<T> clazz) {
        return new PushClientConverter<>(clazz, this);
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        init(configService);
    }

}
