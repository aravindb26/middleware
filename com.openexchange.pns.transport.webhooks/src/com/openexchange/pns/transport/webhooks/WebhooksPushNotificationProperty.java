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

import com.openexchange.config.lean.Property;
import com.openexchange.pns.transport.webhooks.httpclient.WebhookHttpClientProperties;

/**
 * {@link WebhooksPushNotificationProperty} - The enumeration of properties for Webhook Push Notifications.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum WebhooksPushNotificationProperty implements Property {

    /**
     * Whether Webhook Push Notifications are enabled.
     */
    ENABLED("enabled", Boolean.TRUE),
    /**
     * Whether only HTTPS is accepted when communicating with a Webhook.
     */
    HTTPS_ONLY("httpsOnly", Boolean.TRUE),
    /**
     * Whether SSL configuration for "trust all" is allowed.
     */
    ALLOW_TRUST_ALL("allowTrustAll", Boolean.FALSE),
    /**
     * Whether Webhooks having end-point set to an internal address are allowed.
     */
    ALLOW_LOCAL_WEBHOOKS("allowLocalWebhooks", Boolean.FALSE),

    ;

    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link WebhookHttpClientProperties}.
     *
     * @param appendix The FQN appendix
     * @param defaultValue The default value of the property
     */
    private WebhooksPushNotificationProperty(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.pns.transport.webhooks." + appendix;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the fully qualified name of the property
     *
     * @return the fully qualified name of the property
     */
    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    /**
     * Returns the default value of this property
     *
     * @return the default value of this property
     */
    @Override
    public <T extends Object> T getDefaultValue(Class<T> cls) {
        if (defaultValue.getClass().isAssignableFrom(cls)) {
            return cls.cast(defaultValue);
        }
        throw new IllegalArgumentException("The object cannot be converted to the specified type '" + cls.getCanonicalName() + "'");
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
