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

import com.openexchange.java.Strings;

/**
 * {@link WebhookUriValidationMode} - An enumeration of possibilities how to validate client-specified URI
 * for a Webhook end-point against the URI from configured Webhook end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum WebhookUriValidationMode {

    /**
     * No requirements given. Any client-specified URI is accepted.
     */
    NONE("none"),
    /**
     * The client-specified and configured URI for a Webhook end-point are required to start with same prefix;<br>
     * e.g. <code>hostname</code> -&gt; <code>port</code> -&gt; <code>path</code>
     */
    PREFIX("prefix"),
    /**
     * The client-specified and configured URI for a Webhook end-point are required to be exactly the same.
     */
    EXACT("exact"),
    ;

    private final String identifier;

    private WebhookUriValidationMode(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the identifier.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the validation mode for given identifier.
     *
     * @param identifier The identifier to look-up by
     * @return The looked-up mode or <code>null</code>
     */
    public static WebhookUriValidationMode modeFor(String identifier) {
        return modeFor(identifier, null);
    }

    /**
     * Gets the validation mode for given identifier.
     *
     * @param identifier The identifier to look-up by
     * @param defaultValue The default value to return in case no such validation mode can be found
     * @return The looked-up mode or <code>defaultValue</code>
     */
    public static WebhookUriValidationMode modeFor(String identifier, WebhookUriValidationMode defaultValue) {
        if (Strings.isEmpty(identifier)) {
            return null;
        }

        String match = Strings.asciiLowerCase(identifier.trim());
        for (WebhookUriValidationMode mode : WebhookUriValidationMode.values()) {
            if (match.equals(mode.identifier)) {
                return mode;
            }
        }
        return defaultValue;
    }

}
