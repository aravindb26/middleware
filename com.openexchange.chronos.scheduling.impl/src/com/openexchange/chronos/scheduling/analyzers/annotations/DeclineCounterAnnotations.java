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
 * {@link DeclineCounterAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class DeclineCounterAnnotations implements LocalizableStrings {

    // introductions for DECLINECOUNTER

    public static final String COUNTER_DECLINED = "The proposed changes for the appointment \"%1$s\" have been declined by %2$s.";
    public static final String COUNTER_DECLINED_SERIES = "The proposed changes for the appointment series \"%1$s\" have been declined by %2$s.";
    public static final String COUNTER_DECLINED_OCCURRENCE = "The proposed changes for an appointment of the series \"%1$s\" have been declined by %2$s.";
    public static final String COUNTER_DECLINED_ON_BEHALF = "The proposed changes for the appointment \"%1$s\" have been declined by %2$s on behalf of %3$s.";
    public static final String COUNTER_DECLINED_SERIES_ON_BEHALF = "The proposed changes for the appointment series \"%1$s\" have been declined by %2$s on behalf of %3$s.";
    public static final String COUNTER_DECLINED_OCCURRENCE_ON_BEHALF = "The proposed changes for an appointment of the series \"%1$s\" have been declined by %2$s on behalf of %3$s.";

    // state descriptions for DECLINECOUNTER

    public static final String COUNTER_DECLINED_FOR_UPDATED = "The organizer has updated the appointment in the meantime.";

    /**
     * Initializes a new {@link DeclineCounterAnnotations}.
     */
    private DeclineCounterAnnotations() {
        super();
    }

}
