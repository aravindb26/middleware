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

package com.openexchange.snippet.utils.httpclient.properties;

import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.snippet.utils.httpclient.properties.SnippetImgProviderProperties.connectionTimeout;
import static com.openexchange.snippet.utils.httpclient.properties.SnippetImgProviderProperties.hardConnectTimeout;
import static com.openexchange.snippet.utils.httpclient.properties.SnippetImgProviderProperties.hardReadTimeout;
import static com.openexchange.snippet.utils.httpclient.properties.SnippetImgProviderProperties.maxConnections;
import static com.openexchange.snippet.utils.httpclient.properties.SnippetImgProviderProperties.maxConnectionsPerRoute;
import static com.openexchange.snippet.utils.httpclient.properties.SnippetImgProviderProperties.socketReadTimeout;
import org.apache.http.HttpException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;
import com.openexchange.rest.client.httpclient.util.HostCheckingRoutePlanner;
import com.openexchange.rest.client.httpclient.util.HostCheckingRoutePlanner.HostCheckerStrategy;
import com.openexchange.server.ServiceLookup;
import com.openexchange.snippet.utils.SnippetProcessor;

/**
 * {@link SnippetImgHttpProperties}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class SnippetImgHttpProperties extends DefaultHttpClientConfigProvider {

    private static final String HTTP_CLIENT_ID = "snippetimg";

    private static final HostCheckerStrategy STRATEGY = (host, hostAddress) -> {
        if (SnippetProcessor.DENIED_HOSTS.contains(hostAddress)) {
            throw new HttpException("Invalid target host: " + host.getHostName());
        }
    };

    /**
     * Gets the identifier for the HTTP client.
     *
     * @return The HTTP client identifier
     */
    public static String getHttpClientId() {
        return HTTP_CLIENT_ID;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final ServiceLookup serviceLookup;

    /**
     * Initializes a new {@link SnippetImgHttpProperties}.
     *
     * @param serviceLookup The {@link ServiceLookup}
     */
    public SnippetImgHttpProperties(ServiceLookup serviceLookup) {
        super(HTTP_CLIENT_ID, "Open-Xchange Snippet Img Client");
        this.serviceLookup = serviceLookup;
    }

    @Override
    public void modify(HttpClientBuilder builder) {
        super.modify(builder);
        builder.setRedirectStrategy(SnippetImgRedirectStrategy.getInstance());
        HostCheckingRoutePlanner.injectHostCheckingRoutePlanner(STRATEGY, builder);
    }
    @Override
    public Interests getAdditionalInterests() {
        return DefaultInterests.builder().propertiesOfInterest(SnippetImgProviderProperties.PREFIX + "*").build();
    }

    @Override
    public HttpBasicConfig configureHttpBasicConfig(HttpBasicConfig config) {
        try {
            return getFromConfiguration(config);
        } catch (OXException e) {
            LoggerFactory.getLogger(SnippetImgProviderProperties.class).warn("Unable to apply configuration for Snippet Img client", e);
        }
        config.setMaxTotalConnections(i(maxConnections.getDefaultValue(Integer.class)));
        config.setMaxConnectionsPerRoute(i(maxConnectionsPerRoute.getDefaultValue(Integer.class)));
        config.setConnectTimeout(i(connectionTimeout.getDefaultValue(Integer.class)));
        config.setSocketReadTimeout(i(socketReadTimeout.getDefaultValue(Integer.class)));
        config.setHardConnectTimeout(i(hardConnectTimeout.getDefaultValue(Integer.class)));
        config.setHardReadTimeout(i(hardReadTimeout.getDefaultValue(Integer.class)));
        return config;
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
