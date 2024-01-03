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

package com.openexchange.mail.exportpdf.converters.header;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link MailHeaderStrings}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailHeaderStrings implements LocalizableStrings {

    public static final String SUBJECT = "Subject";
    public static final String DATE = "Date";
    public static final String FROM = "From";
    public static final String VIA = "Via";
    public static final String TO = "To";
    public static final String CC = "Copy";
    public static final String BCC = "Blind Copy";

    public static final String PREVIEW = "Preview";
    public static final String ATTACHMENTS = "Attachment(s)";

    /**
     * Initialises a new {@link MailHeaderStrings}.
     */
    private MailHeaderStrings() {
        super();
    }
}
