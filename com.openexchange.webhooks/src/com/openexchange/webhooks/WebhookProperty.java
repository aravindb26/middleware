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

import com.openexchange.config.lean.Property;

/**
 * {@link WebhookProperty} - Enumeration of properties for Webhooks.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum WebhookProperty implements Property {

    /**
     * Specifies a comma-separated list of Webhook identifiers that are considered as enabled.
     */
    ENABLED_IDS("enabledIds", ""),
    ;

    private final Object defaultValue;
    private final String fqn;

    /**
     * Initializes a new {@link RedisProperty}.
     *
     * @param appendix The appendix for full-qualified name
     * @param defaultValue The default value
     */
    private WebhookProperty(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.webhooks." + appendix;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the fully qualified name for the property
     *
     * @return the fully qualified name for the property
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
    public Object getDefaultValue() {
        return defaultValue;
    }

}
