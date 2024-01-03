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

package com.openexchange.contact.provider.ldap.config;

import static com.openexchange.contact.provider.ldap.config.ConfigUtils.CONFIG_FILENAME;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.MAPPING_FILENAME;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.PROVIDER_ID_PREFIX;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.asMap;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.readYaml;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ProviderConfigFactory}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ProviderConfigFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ProviderConfigFactory.class);

    private final ServiceLookup services;

    /**
     * Initializes a new {@link ProviderConfigFactory}.
     *
     * @param services A reference to the service look-up
     */
    public ProviderConfigFactory(ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Initializes all configured LDAP contacts provider configurations.
     *
     * @return The provider configurations associated to their identifier, or an empty map if none are configured
     */
    public Map<String, ProviderConfig> getConfigs() {
        try {
            return getConfigs(readYaml(services.getServiceSafe(ConfigurationService.class), CONFIG_FILENAME));
        } catch (OXException e) {
            LOG.error("Error reading \"{}\"", CONFIG_FILENAME, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Initializes all configured LDAP contacts provider configurations.
     *
     * @param yamlMap The parsed YAML file as map to read out the provider configurations from
     * @return The provider configurations associated to their identifier, or an empty map if none are configured
     */
    public Map<String, ProviderConfig> getConfigs(Map<String, Object> yamlMap) {
        if (null == yamlMap || yamlMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> mappings;
        try {
            mappings = readYaml(services.getServiceSafe(ConfigurationService.class), MAPPING_FILENAME);
        } catch (OXException e) {
            LOG.error("Error reading \"{}\", skipping LDAP provider initialization.", MAPPING_FILENAME, e);
            return Collections.emptyMap();
        }
        Map<String, ProviderConfig> providerConfigs = new HashMap<String, ProviderConfig>(yamlMap.size());
        for (Entry<String, Object> configEntry : yamlMap.entrySet()) {
            try {
                providerConfigs.put(PROVIDER_ID_PREFIX + configEntry.getKey(), ProviderConfig.init(asMap(configEntry.getValue()), mappings));
            } catch (Exception e) {
                LOG.error("Error reading section \"{}\" of \"{}\", skipping.", configEntry.getKey(), CONFIG_FILENAME, e);
            }
        }
        return providerConfigs;
    }

}
