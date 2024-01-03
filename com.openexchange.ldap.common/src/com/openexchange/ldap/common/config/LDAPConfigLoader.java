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

import static com.openexchange.ldap.common.config.ConfigUtils.CONFIG_FILENAME;
import static com.openexchange.ldap.common.config.ConfigUtils.asMap;
import static com.openexchange.ldap.common.config.ConfigUtils.readYaml;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;

/**
 * The {@link LDAPConfigLoader} helps to load the LDAP client configuration
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LDAPConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPConfigLoader.class);

    private final ConfigurationService configurationService;

    private final ExternalLDAPConfigProviderRegistry externalProviderRegistry;

    /**
     * Initializes a new {@link LDAPConfigLoader}.
     *
     * @param configurationService A reference to the configuration service
     * @param externalProviderRegistry The external configuration provider registry
     */
    public LDAPConfigLoader(ConfigurationService configurationService, ExternalLDAPConfigProviderRegistry externalProviderRegistry) {
        super();
        this.configurationService = configurationService;
        this.externalProviderRegistry = externalProviderRegistry;
    }

    /**
     * Initializes all configured LDAP contacts provider configurations.
     *
     * @return The provider configurations associated with their identifier, or an empty map if none are configured
     */
    public List<LDAPConfig> getConfigs() {
        try {
            // @formatter:off
            List<LDAPConfig> result = new ArrayList<>(getConfigsInternal().values());
            externalProviderRegistry.getServiceList().forEach((provider) -> result.addAll(Optional.ofNullable(provider.getConfig()).orElse(Collections.emptyList())));
            // sort read write pool configurations to the end of the list so they can reference other pools
            return result.stream()
                         .sorted(READ_WRITE_POOL_TO_LAST_COMPARATOR)
                         .collect(Collectors.toList());
            // @formatter:on
        } catch (OXException e) {
            LOG.error("Error reading \"{}\"", CONFIG_FILENAME, e);
            return Collections.emptyList();
        }
    }

    /**
     * A comparator which sorts the read write pools to the end of the list
     */
    private static final Comparator<LDAPConfig> READ_WRITE_POOL_TO_LAST_COMPARATOR = new Comparator<LDAPConfig>() {

        @Override
        public int compare(LDAPConfig o1, LDAPConfig o2) {
            if (o1.getPool().getType().equals(LDAPConnectionPoolType.readWrite)) {
                // TODO: Shouldn't it be 1 to sort read-write to the end?
                return -1;
            }
            if (o2.getPool().getType().equals(LDAPConnectionPoolType.readWrite)) {
                return 1;
            }
            return 0;
        }

    };

    /**
     * Initializes all configured LDAP server configurations.
     *
     * @return The LDAP server configurations associated to their id, or an empty map if none are configured
     * @throws OXException in case the config couldn't be read
     */
    private Map<String, LDAPConfig> getConfigsInternal() throws OXException {
        Map<String, Object> yamlMap = readYaml(configurationService, CONFIG_FILENAME);
        if (null == yamlMap || yamlMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, LDAPConfig> result = new HashMap<String, LDAPConfig>(yamlMap.size());
        for (Map.Entry<String, Object> configEntry : yamlMap.entrySet()) {
            try {
                LDAPConfig existing = result.putIfAbsent(configEntry.getKey(), LDAPConfig.init(configEntry.getKey(), asMap(configEntry.getValue())));
                if (existing != null) {
                    LOG.error("Skipping already existing LDAP server configuration with the name \"{}\". Please review your configuration.", configEntry.getKey());
                }
            } catch (Exception e) {
                LOG.error("Error reading section \"{}\" of \"{}\", skipping.", configEntry.getKey(), CONFIG_FILENAME, e);
            }
        }
        return result;
    }

}
