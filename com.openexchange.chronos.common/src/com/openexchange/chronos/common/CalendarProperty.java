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

package com.openexchange.chronos.common;

import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.config.lean.Property;

/**
 * {@link CalendarProperty} - Enumeration of calendar-related properties
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public enum CalendarProperty implements Property {

    /**
     * Defines the default free/busy visibility setting to assume unless overridden by the user.
     * <p/>
     * Possible values are:
     * <ul>
     * <li><code>none</code> to not expose a user's availability to others at all</li>
     * <li><code>internal-only</code> to make the free/busy data available to other users within the same context</li>
     * <li><code>all</code> to expose availability data also beyond context boundaries (i.e. for cross-context- or other external access
     * if configured)</li>
     * </ul>
     */
    FREE_BUSY_VISIBILITY_DEFAULT("freeBusyVisibility.default", FreeBusyVisibility.ALL.getClientIdentifier()),

    /**
     * Configures if the default free/busy visibility setting may be overridden by users or not.
     */
    FREE_BUSY_VISIBILITY_PROTECTED("freeBusyVisibility.protected", Boolean.FALSE),

    /**
     * Enables or disables free/busy lookups to and from other contexts of the platform. If this property is enabled for a user,
     * availability data of users that reside in other contexts, having the property enabled as well, will be accessible.
     * <p/>
     * The property has to be enabled for the source and the target context of the free/busy lookup.
     */
    ENABLE_CROSS_CONTEXT_FREE_BUSY("enableCrossContextFreeBusy", Boolean.FALSE),

    /**
     * Enables or disables conflict checks to and from other contexts of the platform. If this property is enabled for a user,
     * the conflict checks when creating/updating an appointment also be performed for attendees that reside in other contexts, in case
     * they have the property enabled as well.
     * <p/>
     * The property has to be enabled for the source and the target context of the conflict check.
     */
    ENABLE_CROSS_CONTEXT_CONFLICTS("enableCrossContextConflicts", Boolean.FALSE),

    ;

    private static final String NAME_PREFIX = "com.openexchange.calendar.";

    private final String nameSuffix;
    private final Object defaultValue;

    /**
     * Initializes a new {@link CalendarProperty}.
     * 
     * @param The property name suffix
     * @param defaultValue The default value
     */
    private CalendarProperty(String nameSuffix, Object defaultValue) {
        this.nameSuffix = nameSuffix;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the fully qualified name of the property.
     *
     * @return The fully qualified name of the property
     */
    @Override
    public String getFQPropertyName() {
        return NAME_PREFIX + nameSuffix;
    }

    /**
     * Returns the default value of this property.
     *
     * @return The default value of this property
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
