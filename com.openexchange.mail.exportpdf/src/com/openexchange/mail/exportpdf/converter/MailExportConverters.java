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

import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;

/**
 * {@link MailExportConverters} - Defines a collection of multiple {@link MailExportConverter}s
 * * that can/may handle a single {@link MailExportMailPartContainer}.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface MailExportConverters {

    /**
     * Converts the specified {@link MailExportMailPartContainer}
     *
     * @param mailPart The container with the mail part to convert
     * @param options the converter options
     * @return The conversion result
     */
    MailExportConversionResult convert(MailExportMailPartContainer mailPart, MailExportConverterOptions options);
    
    /**
     * Checks if the given content type is configured to be handled by the converter
     *
     * @param mailPart The {@link MailExportMailPartContainer} type to check
     * @param options The conversion options
     * @return <code>true</code> if the given mail part is handled by configuration, <code>false</code> otherwise
     * @throws OXException if an error is occurred
     */
    boolean handles(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException;
}