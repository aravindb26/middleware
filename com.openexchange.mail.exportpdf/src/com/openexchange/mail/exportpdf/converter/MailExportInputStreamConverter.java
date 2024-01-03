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

package com.openexchange.mail.exportpdf.converter;

import java.io.InputStream;
import com.openexchange.exception.OXException;
import com.openexchange.java.SizeKnowingInputStream;
import com.openexchange.mail.exportpdf.MailExportContentInformation;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.session.Session;

/**
 * {@link MailExportInputStreamConverter} - Converts the mail parts within the {@link MailExportContentHolder}
 * to an {@link InputStream}.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportInputStreamConverter {

    /**
     * Converts the specified content holder to an {@link InputStream}
     *
     * @param session The session
     * @param contentInformation The content information
     * @param options The mail export options
     * @return returns the {@link InputStream}
     * @throws OXException if an error is occurred
     */
    SizeKnowingInputStream convert(Session session, MailExportContentInformation contentInformation, MailExportOptions options) throws OXException;
}
