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

package com.openexchange.pns.transport.webhooks.osgi;

import static com.openexchange.osgi.Tools.withRanking;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.pns.PushMessageGeneratorRegistry;
import com.openexchange.pns.PushNotificationTransport;
import com.openexchange.pns.PushSubscriptionListener;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.transport.webhooks.WebhookPushClientChecker;
import com.openexchange.pns.transport.webhooks.WebhookPushSubscriptionListener;
import com.openexchange.pns.transport.webhooks.WebhooksPushNotificationTransport;
import com.openexchange.pns.transport.webhooks.httpclient.WebhookHttpClientConfig;
import com.openexchange.push.PushClientChecker;
import com.openexchange.push.clients.PushClientProviderFactory;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.rest.client.httpclient.SpecificHttpClientConfigProvider;
import com.openexchange.webhooks.WebhookRegistry;

/**
 * {@link WebhooksPushNotificationTransportActivator} - Activator for <i>com.openexchange.pns.transport.webhooks</i> bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhooksPushNotificationTransportActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link WebhooksPushNotificationTransportActivator}.
     */
    public WebhooksPushNotificationTransportActivator() {
        super();
    }

    @Override
    protected boolean stopOnServiceUnavailability() {
        return true;
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, // @formatter:off
                                LeanConfigurationService.class,
                                PushSubscriptionRegistry.class,
                                PushMessageGeneratorRegistry.class,
                                ConfigViewFactory.class,
                                PushClientProviderFactory.class,
                                WebhookRegistry.class,
                                HttpClientService.class,
                                SSLConfigurationService.class,
                                SSLSocketFactoryProvider.class
        }; // @formatter:on
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        registerService(PushSubscriptionListener.class, new WebhookPushSubscriptionListener(this));
        registerService(PushClientChecker.class, new WebhookPushClientChecker(this), withRanking(100));

        registerService(PushNotificationTransport.class, new WebhooksPushNotificationTransport(getServiceSafe(HttpClientService.class), // @formatter:off
                                                                                          getServiceSafe(PushSubscriptionRegistry.class),
                                                                                          getServiceSafe(PushMessageGeneratorRegistry.class),
                                                                                          getServiceSafe(ConfigViewFactory.class),
                                                                                          this)); // @formatter:on

        registerService(ForcedReloadable.class, configservice -> WebhooksPushNotificationTransport.invalidateEnabledCache());

        // Register HTTP client config
        registerService(SpecificHttpClientConfigProvider.class, new WebhookHttpClientConfig(this));
    }

}
