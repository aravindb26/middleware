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

package com.openexchange.chronos.scheduling;

import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;

/**
 * 
 * {@link PushedIMipResolveMode}
 *
 * @author <a href="mailto:martin.herfurthr@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.6
 */
public enum PushedIMipResolveMode {

    /**
     * Only the email's recipient is considered and resolved through the configured MailResolver service.
     */
    RECIPIENTONLY,
    /**
     * The push message's username is expected in <code>userid@contextid</code> notation.
     */
    SYNTHETIC,
    /**
     * The push message's username is the mail login, which is then resolved through a configured MailLoginResolver service.
     */
    MAILLOGIN,
    /**
     * The push message's username is expected in <code>username@loginmapping</code> notation and resolved through corresponding login mappings.
     */
    LOGININFO;

    private static final String NAME = "com.openexchange.calendar.pushedIMipResolveMode";

    /** Configures how iMIP mails that are pushed to the server will be associated to groupware users. */
    private static final Property PUSHED_IMIP_RESOLVE_MODE = DefaultProperty.valueOf(NAME, getDefaultMode().name());

    /**
     * The default mode on how groupware users are associated
     *
     * @return The default mode
     */
    private static PushedIMipResolveMode getDefaultMode() {
        return RECIPIENTONLY;
    }

    /**
     * Get the server configured value for push IMIP resolve mode
     *
     * @param config The {@link LeanConfigurationService}
     * @return The mode which is configured
     */
    public static PushedIMipResolveMode getConfiguredValue(LeanConfigurationService config) {
        String property = config.getProperty(PUSHED_IMIP_RESOLVE_MODE);
        for (PushedIMipResolveMode mode : values()) {
            if (mode.name().equalsIgnoreCase(property)) {
                return mode;
            }
        }
        return getDefaultMode();
    }
}
