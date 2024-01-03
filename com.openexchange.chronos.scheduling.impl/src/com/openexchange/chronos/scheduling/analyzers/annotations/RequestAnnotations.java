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
 * {@link RequestAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class RequestAnnotations implements LocalizableStrings {

    // introductions for REQUEST

    public static final String INVITED = "You have been invited to the appointment \"%1$s\" by %2$s.";
    public static final String INVITED_SERIES = "You have been invited to the appointment series \"%1$s\" by %2$s.";
    public static final String INVITED_OCCURRENCE = "You have been invited to an appointment of the series \"%1$s\" by %2$s.";
    public static final String INVITED_ON_BEHALF = "You have been invited to the appointment \"%1$s\" by %2$s on behalf of %3$s.";
    public static final String INVITED_SERIES_ON_BEHALF = "You have been invited to the appointment series \"%1$s\" by %2$s on behalf of %3$s.";
    public static final String INVITED_OCCURRENCE_ON_BEHALF = "You have been invited to an appointment of the series \"%1$s\" by %2$s on behalf of %3$s.";

    public static final String USER_INVITED = "%1$s has been invited to the appointment \"%2$s\" by %3$s.";
    public static final String USER_INVITED_SERIES = "%1$s has been invited to the appointment series \"%2$s\" by %3$s.";
    public static final String USER_INVITED_OCCURRENCE = "%1$s has been invited to an appointment of the series \"%2$s\" by %3$s.";
    public static final String USER_INVITED_ON_BEHALF = "%1$s has been invited to the appointment \"%2$s\" by %3$s on behalf of %4$s.";
    public static final String USER_INVITED_SERIES_ON_BEHALF = "%1$s has been invited to the appointment series \"%2$s\" by %3$s on behalf of %4$s.";
    public static final String USER_INVITED_OCCURRENCE_ON_BEHALF = "%1$s has been invited to an appointment of the series \"%2$s\" by %3$s on behalf of %4$s.";

    public static final String RESOURCE_INVITED = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_CREATE;
    public static final String RESOURCE_INVITED_SERIES = "%1$s has added the resource %2$s to the appointment series %3$s.";
    public static final String RESOURCE_INVITED_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_CREATE_INSTANCE;
    public static final String RESOURCE_INVITED_ON_BEHALF = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_CREATE_ON_BEHALF;
    public static final String RESOURCE_INVITED_SERIES_ON_BEHALF = "On behalf of %1$s, %2$s has added the resource %3$s to the appointment series %4$s.";
    public static final String RESOURCE_INVITED_OCCURRENCE_ON_BEHALF = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_CREATE_ON_BEHALF_INSTANCE;

    public static final String CHANGED = "%1$s has changed the appointment \"%2$s\".";
    public static final String CHANGED_SERIES = "%1$s has changed the appointment series \"%2$s\".";
    public static final String CHANGED_OCCURRENCE = "%1$s has changed an appointment of the series \"%2$s\".";
    public static final String CHANGED_ON_BEHALF = "%1$s has changed the appointment \"%2$s\" on behalf of %3$s.";
    public static final String CHANGED_SERIES_ON_BEHALF = "%1$s has changed the appointment series \"%2$s\" on behalf of %3$s.";
    public static final String CHANGED_OCCURRENCE_ON_BEHALF = "%1$s has changed an appointment of the series \"%2$s\" on behalf of %3$s.";

    public static final String RESOURCE_CHANGED = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_UPDATE;
    public static final String RESOURCE_CHANGED_SERIES = "The appointment series %1$s with the resource %2$s has been changed by %3$s.";
    public static final String RESOURCE_CHANGED_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_UPDATE_INSTANCE;
    public static final String RESOURCE_CHANGED_ON_BEHALF = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_UPDATE_ON_BEHALF;
    public static final String RESOURCE_CHANGED_SERIES_ON_BEHALF = "On behalf of %1$s, the appointment series %2$s with the resource %3$s has been changed by %4$s.";
    public static final String RESOURCE_CHANGED_OCCURRENCE_ON_BEHALF = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_UPDATE_ON_BEHALF_INSTANCE;

    public static final String DELEGATED = "%1$s delegated the invitation to the appointment \"%2$s\" organized by %3$s to you.";
    public static final String DELEGATED_SERIES = "%1$s delegated the invitation to the appointment series \"%2$s\" organized by %3$s to you.";
    public static final String DELEGATED_OCCURRENCE = "%1$s delegated the invitation to an appointment of the series \"%2$s\" organized by %3$s to you.";
    public static final String USER_DELEGATED = "%1$s delegated the invitation to the appointment \"%2$s\" organized by %3$s to %4$s.";
    public static final String USER_DELEGATED_SERIES = "%1$s delegated the invitation to the appointment series \"%2$s\" organized by %3$s to %4$s.";
    public static final String USER_DELEGATED_OCCURRENCE = "%1$s delegated the invitation to an appointment of the series \"%2$s\" organized by %3$s to %4$s.";

    public static final String FORWARDED = "You have received a forwarded invitation to the appointment \"%1$s\" organized by %2$s.";
    public static final String FORWARDED_SERIES = "You have received a forwarded invitation to the appointment series \"%1$s\" organized by %2$s.";
    public static final String FORWARDED_OCCURRENCE = "You have received a forwarded invitation to an appointment of the series \"%1$s\" organized by %2$s.";
    public static final String FORWARDED_ON_BEHALF = "You have received a forwarded invitation to the appointment \"%1$s\" organized by %2$s on behalf of %3$s.";
    public static final String FORWARDED_SERIES_ON_BEHALF = "You have received a forwarded invitation to the appointment series \"%1$s\" by %2$s organized on behalf of %3$s.";
    public static final String FORWARDED_OCCURRENCE_ON_BEHALF = "You have received a forwarded invitation to an appointment of the series \"%1$s\" organized by %2$s on behalf of %3$s.";

    public static final String USER_FORWARDED = "%1$s has received a forwarded invitation to the appointment \"%2$s\" organized by %3$s.";
    public static final String USER_FORWARDED_SERIES = "%1$s has received a forwarded invitation to the appointment series \"%2$s\" organized by %3$s.";
    public static final String USER_FORWARDED_OCCURRENCE = "%1$s has received a forwarded invitation to an appointment of the series \"%2$s\" organized by %3$s.";
    public static final String USER_FORWARDED_ON_BEHALF = "%1$s has received a forwarded invitation to the appointment \"%2$s\" organized by %3$s on behalf of %4$s.";
    public static final String USER_FORWARDED_SERIES_ON_BEHALF = "%1$s has received a forwarded invitation to the appointment series \"%2$s\" organized by %3$s on behalf of %4$s.";
    public static final String USER_FORWARDED_OCCURRENCE_ON_BEHALF = "%1$s has received a forwarded invitation to an appointment of the series \"%2$s\" organized by %3$s on behalf of %4$s.";

    // state descriptions for REQUEST

    public static final String SAVED = "The invitation has been saved to your calendar.";
    public static final String SAVED_IN = "The invitation has been saved to the calendar of %1$s.";
    public static final String UPDATED = "The changes have been applied to your calendar.";
    public static final String UPDATED_IN = "The changes have been applied to the calendar of %1$s.";

    public static final String SAVE_MANUALLY = "The invitation needs to be added manually to your calendar.";
    public static final String SAVE_MANUALLY_IN = "The invitation needs to be added manually to the calendar of %1$s.";
    public static final String UPDATE_MANUALLY = "The changes need to be applied manually to your calendar.";
    public static final String UPDATE_MANUALLY_IN = "The changes need to be applied manually to the calendar of %1$s.";

    public static final String NO_SAVE_PERMISSIONS_IN = "You don't have enough permissions to save the invitation to the calendar of %1$s.";
    public static final String NO_UPDATE_PERMISSIONS_IN = "You don't have enough permissions to apply the changes to the calendar of %1$s.";

    /**
     * Initializes a new {@link RequestAnnotations}.
     */
    private RequestAnnotations() {
        super();
    }

}
