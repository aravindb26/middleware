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
 * {@link ReplyAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ReplyAnnotations implements LocalizableStrings {

    // introductions for REPLY

    public static final String REPLIED_ACCEPTED = "%1$s has accepted the invitation to the appointment \"%2$s\".";
    public static final String REPLIED_ACCEPTED_SERIES = "%1$s has accepted the invitation to the appointment series \"%2$s\".";
    public static final String REPLIED_ACCEPTED_OCCURRENCE = "%1$s has accepted the invitation to an appointment of the series \"%2$s\".";
    public static final String REPLIED_ACCEPTED_ON_BEHALF = "%1$s has accepted the invitation to the appointment \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_ACCEPTED_SERIES_ON_BEHALF = "%1$s has accepted the invitation to the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_ACCEPTED_OCCURRENCE_ON_BEHALF = "%1$s has accepted the invitation to an appointment of the series \"%2$s\" on behalf of %3$s.";
    public static final String RESOURCE_REPLIED_ACCEPTED = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_ACCEPTED;
    public static final String RESOURCE_REPLIED_ACCEPTED_SERIES = "%1$s has accepted the booking request for the resource %2$s for the appointment series %3$s.";
    public static final String RESOURCE_REPLIED_ACCEPTED_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_ACCEPTED_INSTANCE;

    public static final String REPLIED_TENTATIVE = "%1$s has tentatively accepted the invitation to the appointment \"%2$s\".";
    public static final String REPLIED_TENTATIVE_SERIES = "%1$s has tentatively accepted the invitation to the appointment series \"%2$s\".";
    public static final String REPLIED_TENTATIVE_OCCURRENCE = "%1$s has tentatively accepted the invitation to an appointment of the series \"%2$s\".";
    public static final String REPLIED_TENTATIVE_ON_BEHALF = "%1$s has tentatively accepted the invitation to the appointment \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_TENTATIVE_SERIES_ON_BEHALF = "%1$s has tentatively accepted the invitation to the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_TENTATIVE_OCCURRENCE_ON_BEHALF = "%1$s has tentatively accepted the invitation to an appointment of the series \"%2$s\" on behalf of %3$s.";
    public static final String RESOURCE_REPLIED_TENTATIVE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_TENTATIVE;
    public static final String RESOURCE_REPLIED_TENTATIVE_SERIES = "%1$s has tentatively accepted the booking request for the resource %2$s for the appointment series %3$s.";
    public static final String RESOURCE_REPLIED_TENTATIVE_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_TENTATIVE_INSTANCE;

    public static final String REPLIED_DECLINED = "%1$s has declined the invitation to the appointment \"%2$s\".";
    public static final String REPLIED_DECLINED_SERIES = "%1$s has declined the invitation to the appointment series \"%2$s\".";
    public static final String REPLIED_DECLINED_OCCURRENCE = "%1$s has declined the invitation to an appointment of the series \"%2$s\".";
    public static final String REPLIED_DECLINED_ON_BEHALF = "%1$s has declined the invitation to the appointment \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_DECLINED_SERIES_ON_BEHALF = "%1$s has declined the invitation to the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_DECLINED_OCCURRENCE_ON_BEHALF = "%1$s has declined the invitation to an appointment of the series \"%2$s\" on behalf of %3$s.";
    public static final String RESOURCE_REPLIED_DECLINED = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_DECLINED;
    public static final String RESOURCE_REPLIED_DECLINED_SERIES = "%1$s has declined the booking request for the resource %2$s for the appointment series %3$s.";
    public static final String RESOURCE_REPLIED_DECLINED_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_DECLINED_INSTANCE;

    public static final String REPLIED_NONE = "%1$s has set the status to 'none' for the invitation to the appointment \"%2$s\".";
    public static final String REPLIED_NONE_SERIES = "%1$s has set the status to 'none' for the invitation to the appointment series \"%2$s\".";
    public static final String REPLIED_NONE_OCCURRENCE = "%1$s has set the status to 'none' for the invitation to an appointment of the series \"%2$s\".";
    public static final String REPLIED_NONE_ON_BEHALF = "%1$s has set the status to 'none' for the invitation to the appointment \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_NONE_SERIES_ON_BEHALF = "%1$s has set the status to 'none' for the invitation to the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String REPLIED_NONE_OCCURRENCE_ON_BEHALF = "%1$s has set the status to 'none' for the invitation to an appointment of the series \"%2$s\" on behalf of %3$s.";
    public static final String RESOURCE_REPLIED_NONE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_NEEDS_ACTION;
    public static final String RESOURCE_REPLIED_NONE_SERIES = "%1$s has deferred the booking request for the resource %2$s for the appointment series %3$s.";
    public static final String RESOURCE_REPLIED_NONE_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_NEEDS_ACTION_INSTANCE;

    // state descriptions for REPLY

    public static final String REPLY_APPLIED = "The response has been applied to your calendar.";
    public static final String REPLY_APPLIED_IN = "The response has been applied to the calendar of %1$s.";

    public static final String REPLY_APPLY_MANUALLY = "The response needs to be applied manually to your calendar.";
    public static final String REPLY_APPLY_MANUALLY_IN = "The response needs to be applied manually to the calendar of %1$s.";

    public static final String REPLY_OUTDATED = "The appointment was updated after the response from this participant was sent.";
    public static final String REPLY_UPDATED = "An updated response from this participant was received in the meantime.";

    public static final String REPLY_UNINVITED = "The response was received from a participant that is not invited to the appointment.";
    public static final String REPLY_DELEGATED = "The participation has been delegated by %1$s.";

    /**
     * Initializes a new {@link ReplyAnnotations}.
     */
    private ReplyAnnotations() {
        super();
    }

}
