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

package com.openexchange.multifactor.provider.backupString.impl;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link BackupStringsLocalizationStrings}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.2
 */
public class BackupStringsLocalizationStrings implements LocalizableStrings {

    /**
     * The default name for the multifactor recovery codes
     */
    public static final String MULTIFACTOR_BACKPUP_STRINGS_DEVICE_NAME = "My Backup";

    /**
     * Error message. Primary authentication devices must be set up before backup devices.
     */
    public static final String MUST_HAVE_OTHER_DEVICES = "Primary authentication devices must be set up before backup devices.";
}
