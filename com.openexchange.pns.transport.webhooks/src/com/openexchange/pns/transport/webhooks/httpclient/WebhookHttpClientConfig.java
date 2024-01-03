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

package com.openexchange.pns.transport.webhooks.httpclient;

import static com.openexchange.java.Autoboxing.i;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.LoggerFactory;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.net.ssl.config.TrustLevel;
import com.openexchange.pns.KnownTransport;
import com.openexchange.pns.transport.webhooks.WebhooksPushNotificationProperty;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;
import com.openexchange.rest.client.httpclient.util.InternalAddressDenyingRoutePlanner;
import com.openexchange.server.ServiceLookup;

/**
 * {@link WebhookHttpClientConfig} - The HTTP client configuration for Webhooks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhookHttpClientConfig extends DefaultHttpClientConfigProvider {

    private static final String HTTP_CLIENT_IDENTIFIER = KnownTransport.WEBHOOK.getTransportId();

    private final ServiceLookup serviceLookup;

    /**
     * Initializes a new {@link SproxydHttpClientConfig}.
     *
     * @param serviceLookup The {@link ServiceLookup}
     */
    public WebhookHttpClientConfig(ServiceLookup serviceLookup) {
        super(HTTP_CLIENT_IDENTIFIER, "Open-Xchange Webhook Client");
        this.serviceLookup = serviceLookup;
    }

    @Override
    public Interests getAdditionalInterests() {
        return DefaultInterests.builder().propertiesOfInterest("com.openexchange.pns.transport.webhooks.http.*").build();
    }

    @Override
    public HttpBasicConfig configureHttpBasicConfig(HttpBasicConfig config) {
        HttpBasicConfig retval = null;
        try {
            retval = getFromConfiguration(config);
        } catch (OXException e) {
            LoggerFactory.getLogger(WebhookHttpClientConfig.class).warn("Unable to apply configuration to Webhook HTTP client", e);
        }

        if (retval == null) {
            config.setMaxTotalConnections(i(WebhookHttpClientProperties.MAX_CONNECTIONS.getDefaultValue(Integer.class)));
            config.setMaxConnectionsPerRoute(i(WebhookHttpClientProperties.MAX_CONNECTIONS_PER_ROUTE.getDefaultValue(Integer.class)));
            config.setConnectTimeout(i(WebhookHttpClientProperties.CONNECT_TIMEOUT.getDefaultValue(Integer.class)));
            config.setSocketReadTimeout(i(WebhookHttpClientProperties.SOCKET_READ_TIMEOUT.getDefaultValue(Integer.class)));
            config.setHardConnectTimeout(i(WebhookHttpClientProperties.HARD_CONNECT_TIMEOUT.getDefaultValue(Integer.class)));
            config.setHardReadTimeout(i(WebhookHttpClientProperties.HARD_READ_TIMEOUT.getDefaultValue(Integer.class)));
            retval = config;
        }

        // Check SSL configuration if "trust all" is active
        LeanConfigurationService configService = serviceLookup.getOptionalService(LeanConfigurationService.class);
        boolean allowTrustAll = configService != null && configService.getBooleanProperty(WebhooksPushNotificationProperty.ALLOW_TRUST_ALL);
        if (!allowTrustAll) {
            SSLConfigurationService optSslConfigurationService = serviceLookup.getOptionalService(SSLConfigurationService.class);
            if (optSslConfigurationService != null && TrustLevel.TRUST_ALL.equals(optSslConfigurationService.getTrustLevel())) {
                // Don't accept every SSL certificate for Webhooks. Therefore relay to Java's default one
                //new SSLContextBuilder().loadTrustMaterial(TrustStrategy)
                SSLContext context = SSLContexts.createSystemDefault();
                config.setCustomSSLConnectionSocketFactory(new SSLConnectionSocketFactory(context, optSslConfigurationService.getSupportedProtocols(), optSslConfigurationService.getSupportedCipherSuites(), NoopHostnameVerifier.INSTANCE));
            }
        }

        return retval;
    }

    @Override
    public void modify(HttpClientBuilder builder) {
        super.modify(builder);

        LeanConfigurationService configService = serviceLookup.getOptionalService(LeanConfigurationService.class);
        boolean allowLocalWebhook = configService != null && configService.getBooleanProperty(WebhooksPushNotificationProperty.ALLOW_LOCAL_WEBHOOKS);
        if (!allowLocalWebhook) {
            builder.setRedirectStrategy(WebhookRedirectStrategy.getInstance());
            InternalAddressDenyingRoutePlanner.injectInternalAddressDenyingRoutePlanner(builder);
        }
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

        config.setMaxTotalConnections(configurationService.getIntProperty(WebhookHttpClientProperties.MAX_CONNECTIONS));
        config.setMaxConnectionsPerRoute(configurationService.getIntProperty(WebhookHttpClientProperties.MAX_CONNECTIONS_PER_ROUTE));
        config.setConnectTimeout(configurationService.getIntProperty(WebhookHttpClientProperties.CONNECT_TIMEOUT));
        config.setSocketReadTimeout(configurationService.getIntProperty(WebhookHttpClientProperties.SOCKET_READ_TIMEOUT));
        config.setHardConnectTimeout(configurationService.getIntProperty(WebhookHttpClientProperties.HARD_CONNECT_TIMEOUT));
        config.setHardReadTimeout(configurationService.getIntProperty(WebhookHttpClientProperties.HARD_READ_TIMEOUT));
        return config;
    }

}
