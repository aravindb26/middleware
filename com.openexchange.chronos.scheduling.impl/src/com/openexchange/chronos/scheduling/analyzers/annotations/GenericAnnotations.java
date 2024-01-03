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
 * {@link GenericAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class GenericAnnotations implements LocalizableStrings {

    public static final String COMMENT_LEFT = "The following comment was left: \"%1$s\"";
    public static final String UPDATED_MEANTIME = "An updated invitation for this appointment was received in the meantime.";
    public static final String DELETED_MEANTIME = "The appointment has been deleted in the meantime.";
    public static final String NOT_FOUND = "The appointment could not be found in your calendar.";
    public static final String NOT_FOUND_IN = "The appointment could not be found in the calendar of %1$s.";
    public static final String UNALLOWED_ORGANIZER_CHANGE = "The organizer of the appointment has changed. This operation is not allowed.";

    public static final String CONFLICTS = "The appointment conflicts with another one in your calendar.";
    public static final String CONFLICTS_IN = "The appointment conflicts with another one in the calendar of %1$s.";

    public static final String NOT_ATTENDING = "You are not attending the appointment.";
    public static final String USER_NOT_ATTENDING = "%1$s is not attending the appointment.";
    public static final String PARTICIPATION_OPTIONAL = "Participation is optional.";
    public static final String NOT_REPLIED = "You have not yet replied to the invitation.";
    public static final String USER_NOT_REPLIED = "%1$s has not yet replied to the invitation.";
    public static final String RESOURCE_NOT_REPLIED = "The booking request for the resource %1$s is pending your approval.";
    public static final String RESOURCE_NOT_DELEGATE = "You are not allowed to handle booking requests for the resource %1$s.";

    public static final String REPLIED_WITH_ACCEPTED = "You have accepted the invitation.";
    public static final String USER_REPLIED_WITH_ACCEPTED = "%1$s has accepted the invitation.";
    public static final String RESOURCE_REPLIED_WITH_ACCEPTED = "The booking request for the resource %1$s has been accepted.";
    public static final String REPLIED_WITH_TENTATIVE = "You have tentatively accepted the invitation.";
    public static final String USER_REPLIED_WITH_TENTATIVE = "%1$s has tentatively accepted the invitation.";
    public static final String RESOURCE_REPLIED_WITH_TENTATIVE = "The booking request for the resource %1$s has been tentatively accepted.";
    public static final String REPLIED_WITH_DECLINED = "You have declined the invitation.";
    public static final String USER_REPLIED_WITH_DECLINED = "%1$s has declined the invitation.";
    public static final String RESOURCE_REPLIED_WITH_DECLINED = "The booking request for the resource %1$s has been declined.";

    /**
     * Initializes a new {@link GenericAnnotations}.
     */
    private GenericAnnotations() {
        super();
    }

}
