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

package com.openexchange.chronos.provider.ical.httpclient.properties;

import static com.openexchange.chronos.provider.ical.httpclient.properties.ICalCalendarProviderProperties.connectionTimeout;
import static com.openexchange.chronos.provider.ical.httpclient.properties.ICalCalendarProviderProperties.hardConnectTimeout;
import static com.openexchange.chronos.provider.ical.httpclient.properties.ICalCalendarProviderProperties.hardReadTimeout;
import static com.openexchange.chronos.provider.ical.httpclient.properties.ICalCalendarProviderProperties.maxConnections;
import static com.openexchange.chronos.provider.ical.httpclient.properties.ICalCalendarProviderProperties.maxConnectionsPerRoute;
import static com.openexchange.chronos.provider.ical.httpclient.properties.ICalCalendarProviderProperties.socketReadTimeout;
import static com.openexchange.java.Autoboxing.i;
import java.util.Optional;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.configuration.ServerProperty;
import com.openexchange.exception.OXException;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;
import com.openexchange.rest.client.httpclient.util.HostCheckingRoutePlanner;
import com.openexchange.server.ServiceLookup;
import com.openexchange.version.VersionService;

/**
 * {@link ICalCalendarHttpProperties}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class ICalCalendarHttpProperties extends DefaultHttpClientConfigProvider {

    private final ServiceLookup serviceLookup;

    /**
     * Initializes a new {@link ICalCalendarHttpProperties}.
     *
     * @param serviceLookup The {@link ServiceLookup}
     */
    public ICalCalendarHttpProperties(ServiceLookup serviceLookup) {
        super("icalfeed", VersionService.NAME + "/", Optional.ofNullable(serviceLookup.getService(VersionService.class)));
        this.serviceLookup = serviceLookup;
    }

    @Override
    public Interests getAdditionalInterests() {
        return DefaultInterests.builder().propertiesOfInterest(ICalCalendarProviderProperties.PREFIX + "*").build();
    }

    @Override
    public HttpBasicConfig configureHttpBasicConfig(HttpBasicConfig config) {
        try {
            return getFromConfiguration(config);
        } catch (OXException e) {
            LoggerFactory.getLogger(ICalCalendarHttpProperties.class).warn("Unable to apply configuration for ICalFeed HTTP client", e);
        }
        config.setMaxTotalConnections(i(maxConnections.getDefaultValue(Integer.class)));
        config.setMaxConnectionsPerRoute(i(maxConnectionsPerRoute.getDefaultValue(Integer.class)));
        config.setConnectTimeout(i(connectionTimeout.getDefaultValue(Integer.class)));
        config.setSocketReadTimeout(i(socketReadTimeout.getDefaultValue(Integer.class)));
        config.setHardConnectTimeout(i(hardConnectTimeout.getDefaultValue(Integer.class)));
        config.setHardReadTimeout(i(hardReadTimeout.getDefaultValue(Integer.class)));
        return config;
    }

    @Override
    public void modify(HttpClientBuilder builder) {
        super.modify(builder);
        boolean production = serviceLookup.getService(LeanConfigurationService.class).getBooleanProperty(ServerProperty.production);
        if (!production) {
            return;
        }
        builder.setRedirectStrategy(BlacklistAwareRedirectStrategy.getInstance());
        HostCheckingRoutePlanner.injectHostCheckingRoutePlanner((host, hostAddress) ->  {
            if (ICalCalendarProviderProperties.isBlacklisted(hostAddress)) {
                throw new ProtocolException("Invalid redirect URI: " + host.getHostName() + ". The URI is not allowed due to configuration.");
            }
        }, builder);
    }

    /**
     * Configures the {@link HttpBasicConfig} with values from configuration
     *
     * @param config The {@link HttpBasicConfig} to configure
     * @return the configured {@link HttpBasicConfig}
     * @throws OXException in case the {@link LeanConfigurationService} is missing
     */
    private HttpBasicConfig getFromConfiguration(HttpBasicConfig config) throws OXException {
        LeanConfigurationService configurationService = serviceLookup.getServiceSafe(LeanConfigurationService.class);

        config.setMaxTotalConnections(configurationService.getIntProperty(maxConnections));
        config.setMaxConnectionsPerRoute(configurationService.getIntProperty(maxConnectionsPerRoute));
        config.setConnectTimeout(configurationService.getIntProperty(connectionTimeout));
        config.setSocketReadTimeout(configurationService.getIntProperty(socketReadTimeout));
        config.setHardConnectTimeout(configurationService.getIntProperty(hardConnectTimeout));
        config.setHardReadTimeout(configurationService.getIntProperty(hardReadTimeout));
        return config;
    }
}
