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

package com.openexchange.webhooks.osgi;

import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.webhooks.WebhookRegistry;
import com.openexchange.webhooks.internal.WebhookConfigParser;
import com.openexchange.webhooks.internal.WebhookRegistryImpl;

/**
 * {@link WebhookActivator} - Activator for Webhook bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.14.0
 */
public class WebhookActivator extends HousekeepingActivator implements Reloadable {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebhookActivator.class);

    /**
     * Initializes a new {@link WebhookActivator}.
     */
    public WebhookActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        WebhookRegistryImpl.getInstance().setWebhookConfig(WebhookConfigParser.parseWebhookConfig(getServiceSafe(ConfigurationService.class)));
        registerService(WebhookRegistry.class, WebhookRegistryImpl.getInstance());
        registerService(Reloadable.class, this);
        LOG.info("Bundle successfully started: {}", context.getBundle().getSymbolicName());
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        LOG.info("Bundle successfully stopped: {}", context.getBundle().getSymbolicName());
    }

    // -----------------------------------------------------------------------------------------------------------

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().configFileNames(WebhookConfigParser.getConfigfileWebhookYamlFile()).build();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        try {
            WebhookRegistryImpl.getInstance().setWebhookConfig(WebhookConfigParser.parseWebhookConfig(configService));
        } catch (Exception e) {
            LOG.error("Failed to parse Webhook configuration", e);
        }
    }

}
