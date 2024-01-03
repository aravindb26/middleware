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
 * {@link CounterAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class CounterAnnotations implements LocalizableStrings {

    // introductions for COUNTER

    public static final String TIME_PROPOSED = "%1$s has proposed a new time for the appointment \"%2$s\".";
    public static final String TIME_PROPOSED_SERIES = "%1$s has proposed a new time for the appointment series \"%2$s\".";
    public static final String TIME_PROPOSED_OCCURRENCE = "%1$s has proposed a new time for an appointment of the series \"%2$s\".";
    public static final String TIME_PROPOSED_ON_BEHALF = "%1$s has proposed a new time for the appointment \"%2$s\" on behalf of %3$s.";
    public static final String TIME_PROPOSED_SERIES_ON_BEHALF = "%1$s has proposed a new time for the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String TIME_PROPOSED_OCCURRENCE_ON_BEHALF = "%1$s has proposed a new time for an appointment of the series \"%2$s\" on behalf of %3$s.";
    public static final String TIME_PROPOSED_TIMES = "Current: %1$s. Proposed: %2$s.";

    public static final String CHANGES_PROPOSED = "%1$s has proposed changes for the appointment \"%2$s\".";
    public static final String CHANGES_PROPOSED_SERIES = "%1$s has proposed changes for the appointment series \"%2$s\".";
    public static final String CHANGES_PROPOSED_OCCURRENCE = "%1$s has proposed changes for an appointment of the series \"%2$s\".";
    public static final String CHANGES_PROPOSED_ON_BEHALF = "%1$s has proposed changes for the appointment \"%2$s\" on behalf of %3$s.";
    public static final String CHANGES_PROPOSED_SERIES_ON_BEHALF = "%1$s has proposed changes for the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String CHANGES_PROPOSED_OCCURRENCE_ON_BEHALF = "%1$s has proposed changes for an appointment of the series \"%2$s\" on behalf of %3$s.";

    // state descriptions for COUNTER

    public static final String COUNTER_OUTDATED = "The appointment was updated after the proposed changes were sent.";
    public static final String COUNTER_UNINVITED = "The message was received from a participant that is not invited to the appointment.";

    public static final String COUNTER_APPLIED = "The proposed changes have been applied to your calendar.";
    public static final String COUNTER_APPLIED_IN = "The proposed changes have been applied to the calendar of %1$s.";
    public static final String COUNTER_APPLY_MANUALLY = "The proposed changes can be applied manually to your calendar.";
    public static final String COUNTER_APPLY_MANUALLY_IN = "The proposed changes can be applied manually to the calendar of %1$s.";

    public static final String COUNTER_UNSUPPORTED = "The proposed changes cannot be applied to your calendar.";
    public static final String COUNTER_UNSUPPORTED_IN = "The proposed changes cannot be applied to the calendar of %1$s.";

    /**
     * Initializes a new {@link CounterAnnotations}.
     */
    private CounterAnnotations() {
        super();
    }

}
