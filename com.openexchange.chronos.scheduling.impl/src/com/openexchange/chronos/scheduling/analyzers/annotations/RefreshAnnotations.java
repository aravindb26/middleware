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
 * {@link RefreshAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class RefreshAnnotations implements LocalizableStrings {

    // introductions for REFRESH

    public static final String REFRESH = "%1$s would like to be brought up to date about an appointment.";
    public static final String REFRESH_ON_BEHALF = "%1$s would like to be brought up to date about an appointment on behalf of %2$s.";

    // state descriptions for REFRESH

    public static final String SEND_MANUALLY = "An updated invitation for the appointment \"%1$s\" can be sent manually.";
    public static final String REFRESH_UNINVITED = "The message was received from a participant that is not invited to the appointment.";

    /**
     * Initializes a new {@link RefreshAnnotations}.
     */
    private RefreshAnnotations() {
        super();
    }

}
