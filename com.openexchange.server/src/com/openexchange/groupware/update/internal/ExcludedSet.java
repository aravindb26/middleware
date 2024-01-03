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

package com.openexchange.groupware.update.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableSet;
import com.openexchange.config.ConfigurationService;

/**
 * This class contains the list of excluded update tasks. The configuration can be done by the configuration file
 * excludedupdatetasks.properties.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ExcludedSet implements UpdateTaskSet<String> {

    private static final Logger LOG = LoggerFactory.getLogger(ExcludedSet.class);

    private static final ExcludedSet SINGLETON = new ExcludedSet();
    private static final String CONFIG_FILE_NAME = "excludedupdatetasks.properties";

    /**
     * Gets the singleton instance
     *
     * @return The instance
     */
    public static ExcludedSet getInstance() {
        return SINGLETON;
    }

    // -----------------------------------------------------------------------------------------------------

    private final AtomicReference<Set<String>> taskSetRef = new AtomicReference<Set<String>>(Collections.emptySet());

    /**
     * Initialises a new {@link ExcludedSet}.
     */
    private ExcludedSet() {
        super();
    }

    /**
     * Loads <code>"excludedupdatetasks.properties"</code> file.
     *
     * @param configService The service to use
     */
    public void configure(ConfigurationService configService) {
        Properties props = loadProperties(configService);
        int size = props.size();
        if (size <= 0) {
            return;
        }

        ImmutableSet.Builder<String> taskSet = ImmutableSet.builderWithExpectedSize(size);
        String propertyName = NamespaceAwareExcludedSet.PROPERTY.getFQPropertyName();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String className = entry.getKey().toString().trim();
            if (false == propertyName.equals(className)) {
                taskSet.add(className);
            }
        }
        taskSetRef.set(taskSet.build());
        UpdateTaskCollection.getInstance().dirtyVersion();
    }

    /**
     * Loads the properties
     *
     * @param configService The {@link ConfigurationService}
     * @return The {@link Properties} found in {@value #CONFIG_FILE_NAME} or an empty {@link Properties} set
     */
    private Properties loadProperties(ConfigurationService configService) {
        try {
            return configService.getFile(CONFIG_FILE_NAME);
        } catch (Exception e) {
            LOG.warn("No '{}' file found in configuration folder with excluded update tasks.", CONFIG_FILE_NAME, e);
            return new Properties();
        }
    }

    @Override
    public Set<String> getTaskSet() {
        return taskSetRef.get();
    }

}
