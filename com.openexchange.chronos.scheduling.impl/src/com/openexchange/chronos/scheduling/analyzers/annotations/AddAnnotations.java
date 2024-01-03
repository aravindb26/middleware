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

package com.openexchange.chronos.scheduling.analyzers.annotations;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link AddAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class AddAnnotations implements LocalizableStrings {

    // introductions for ADD

    public static final String ADDED_OCCURRENCE = "%1$s has added an appointment to the series \"%2$s\".";
    public static final String ADDED_OCCURRENCE_ON_BEHALF = "%1$s has added an appointment to the series \"%2$s\" on behalf of %3$s.";

    // state descriptions for ADD

    public static final String ADD_UNSUPPORTED = "The changes cannot be applied to your calendar.";
    public static final String ADD_UNSUPPORTED_IN = "The changes cannot be applied to the calendar of %1$s.";

    public static final String REQUEST_REFRESH_MANUALLY = "An updated invitation from the organizer should be requested manually.";

    /**
     * Initializes a new {@link AddAnnotations}.
     */
    private AddAnnotations() {
        super();
    }

}
