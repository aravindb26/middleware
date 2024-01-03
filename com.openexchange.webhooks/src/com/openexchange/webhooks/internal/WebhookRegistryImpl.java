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

package com.openexchange.webhooks.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.java.Strings;
import com.openexchange.webhooks.WebhookInfo;
import com.openexchange.webhooks.WebhookRegistry;

/**
 * {@link WebhookRegistryImpl} - The Webhook registry implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebhookRegistryImpl implements WebhookRegistry {

    private static final WebhookRegistryImpl INSTANCE = new WebhookRegistryImpl();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static WebhookRegistryImpl getInstance() {
        return INSTANCE;
    }

    // ----------------------------------------------------------------------------------------------------------------------------------

    private final AtomicReference<Map<String, WebhookInfo>> webhookConfigReference;

    /**
     * Initializes a new {@link WebhookRegistryImpl}.
     */
    private WebhookRegistryImpl() {
        super();
        this.webhookConfigReference = new AtomicReference<Map<String, WebhookInfo>>(Collections.emptyMap());
    }

    /**
     * Sets the Webhook mapping to use.
     *
     * @param webhooks The (immutable) Webhook mapping
     */
    public void setWebhookConfig(Map<String, WebhookInfo> webhooks) {
        this.webhookConfigReference.set(webhooks);
    }

    @Override
    public Map<String, WebhookInfo> getConfiguredWebhooks() {
        return webhookConfigReference.get();
    }

    @Override
    public Optional<WebhookInfo> getConfiguredWebhook(String webhookId) {
        return Strings.isEmpty(webhookId) ? Optional.empty() : Optional.ofNullable(webhookConfigReference.get().get(webhookId));
    }

}
