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
 * {@link CancelAnnotations}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class CancelAnnotations implements LocalizableStrings {

    // introductions for CANCEL

    public static final String CANCELED = "The appointment \"%1$s\" has been canceled by %2$s.";
    public static final String CANCELED_SERIES = "The appointment series \"%1$s\" has been canceled by %2$s.";
    public static final String CANCELED_OCCURRENCE = "An appointment of the series \"%1$s\" has been canceled by %2$s.";
    public static final String CANCELED_ON_BEHALF = "The appointment \"%1$s\" has been canceled by %2$s on behalf of %3$s.";
    public static final String CANCELED_SERIES_ON_BEHALF = "The appointment series \"%1$s\" has been canceled by %2$s on behalf of %3$s.";
    public static final String CANCELED_OCCURRENCE_ON_BEHALF = "An appointment of the series \"%1$s\" has been canceled by %2$s on behalf of %3$s.";

    public static final String RESOURCE_CANCELED = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_DELETE;
    public static final String RESOURCE_CANCELED_SERIES = "The resource %1$s has been removed from the appointment series %2$s by %3$s, or the appointment has been deleted.";
    public static final String RESOURCE_CANCELED_OCCURRENCE = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_DELETE_INSTANCE;
    public static final String RESOURCE_CANCELED_ON_BEHALF = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_DELETE_ON_BEHALF;
    public static final String RESOURCE_CANCELED_SERIES_ON_BEHALF = "On behalf of %1$s, the resource %2$s has been removed from the appointment series %3$s by %4$s, or the appointment has been deleted.";
    public static final String RESOURCE_CANCELED_OCCURRENCE_ON_BEHALF = com.openexchange.chronos.scheduling.common.Messages.RESOURCE_DELETE_ON_BEHALF_INSTANCE;

    // state descriptions for CANCEL

    public static final String CANCEL_APPLIED = "The appointment has been removed from your calendar.";
    public static final String CANCEL_APPLIED_IN = "The appointment has been removed accordingly from the calendar of %1$s.";

    public static final String CANCEL_APPLY_MANUALLY = "The appointment needs to be removed manually from your calendar.";
    public static final String CANCEL_APPLY_MANUALLY_IN = "The appointment needs to be removed manually from the calendar of %1$s.";

    /**
     * Initializes a new {@link CancelAnnotations}.
     */
    private CancelAnnotations() {
        super();
    }

}
