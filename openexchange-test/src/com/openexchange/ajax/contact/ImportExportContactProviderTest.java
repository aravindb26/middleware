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

package com.openexchange.ajax.contact;

import java.util.regex.Pattern;

/**
 * {@link ContactProviderTest} - Base class for testing import and export function of contacts from/into a different contact provider (i.e com.openexchange.contact.provider.test)
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public abstract class ImportExportContactProviderTest extends ContactProviderTest {

    protected static final Pattern CONTACT_ID_REPSONSE_PATTERN = Pattern.compile("\"id\":\"(.*?)\"");
    protected static final Pattern FOLDER_ID_REPSONSE_PATTERN = Pattern.compile("\"folder_id\":\"(.*?)\"");
}
