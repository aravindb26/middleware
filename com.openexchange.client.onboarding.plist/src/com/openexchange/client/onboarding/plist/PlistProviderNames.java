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

package com.openexchange.client.onboarding.plist;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link PlistProviderNames} - The default names for several PLIST profiles.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class PlistProviderNames implements LocalizableStrings {

    /**
     * Initializes a new {@link PlistProviderNames}.
     */
    private PlistProviderNames() {
        super();
    }

    // The default name for Exchange ActiveSync profile
    public static final String PROFILE_NAME_EAS = "Exchange ActiveSync";

    // The default name for E-Mail profile
    public static final String PROFILE_NAME_MAIL = "E-Mail";

    // The default name for CardDAV profile
    public static final String PROFILE_NAME_CARDDAV = "CardDAV";

    // The default name for CalDAV profile
    public static final String PROFILE_NAME_CALDAV = "CalDAV";

}
