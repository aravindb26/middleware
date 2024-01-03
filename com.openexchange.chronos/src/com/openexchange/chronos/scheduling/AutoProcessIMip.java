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

import com.openexchange.annotation.NonNull;
import com.openexchange.annotation.Nullable;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;

/**
 * 
 * {@link AutoProcessIMip}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public enum AutoProcessIMip {

    /**
     * Always allows automatically processing of incoming iMIP messages
     */
    ALWAYS,
    /**
     * Doesn't allow automatically processing of incoming iMIP messages
     */
    NEVER,
    /**
     * Does only allow processing of incoming iMIP messages if the messages
     * <li> comes from a known attendee or the organizer</li>
     * <li> comes from a known contact from the users address book</li>
     */
    KNOWN;

    /**
     * Get the default value for the automatically processing of incoming iMIP messages
     *
     * @return The default value to use
     */
    private static @NonNull AutoProcessIMip getDefault() {
        return KNOWN;
    }

    /** The server property for the auto processing of iMIP messages, if no user setting is set */
    private static final Property AUTO_PROCESS_IMIP_PROPERTY = DefaultProperty.valueOf("com.openexchange.calendar.autoProcessIMip", getDefault().name());

    /**
     * Get the server configured value for auto processing iMIP messages
     *
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param leanConfigurationService The optional configuration service, can be <code>null</code>
     * @return The configured value or the default
     */
    public static @NonNull AutoProcessIMip getConfiguredValue(int contextId, int userId, @Nullable LeanConfigurationService leanConfigurationService) {
        if (null == leanConfigurationService) {
            return getDefault();
        }
        String value = leanConfigurationService.getProperty(userId, contextId, AutoProcessIMip.AUTO_PROCESS_IMIP_PROPERTY);
        for (AutoProcessIMip autoProcessIMip : values()) {
            if (autoProcessIMip.name().equalsIgnoreCase(value)) {
                return autoProcessIMip;
            }
        }
        return getDefault();
    }
}
