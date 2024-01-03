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

package com.openexchange.pns.transport.webhooks;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

/**
 * {@link WebhooksPushNotificationMetrics} - Utility class for Webhook metrics.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class WebhooksPushNotificationMetrics {

    private static final String PREFIX = "appsuite.webhook.";

    /**
     * Initializes a new {@link WebhooksPushNotificationMetrics}.
     */
    private WebhooksPushNotificationMetrics() {
        super();
    }

    /**
     * Get a {@link Timer} for a Webhook, a dedicated method and a specific result.
     *
     * @param success Whether call to Webhook end-point was successful or not
     * @param client The client identifier
     * @param webhookId The Webhook identifier
     * @param method The HTTP method
     * @param status The response status
     * @return A {@link Timer} for the combination of all tree arguments
     */
    public static Timer getRequestTimer(boolean success, String webhookId, String method, String status) {
        // @formatter:off
        return Timer.builder(PREFIX + "requests")
            .description("Duration of executed Webhook requests")
            .tag("success", success ? "OK" : "NOK")
            .tag("webhook", webhookId)
            .tag("method", method)
            .tag("status", status)
            .register(Metrics.globalRegistry);
        // @formatter:on
    }

}
