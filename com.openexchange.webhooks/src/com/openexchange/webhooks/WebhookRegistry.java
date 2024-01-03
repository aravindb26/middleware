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

package com.openexchange.webhooks;

import com.openexchange.osgi.annotation.SingletonService;
import java.util.Map;
import java.util.Optional;

/**
 * {@link WebhookRegistry} - The registry for configured/registered Webhook end-points.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@SingletonService
public interface WebhookRegistry {

    /**
     * Gets all available configured Webhooks.
     *
     * @return The configured Webhooks
     */
    Map<String, WebhookInfo> getConfiguredWebhooks();

    /**
     * Gets the configured Webhook for given Webhook identifier.
     *
     * @param webhookId The Webhook identifier
     * @return The configured Webhook or empty
     */
    Optional<WebhookInfo> getConfiguredWebhook(String webhookId);

}
