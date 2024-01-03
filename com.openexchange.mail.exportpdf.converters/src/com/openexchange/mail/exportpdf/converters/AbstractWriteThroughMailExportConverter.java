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

package com.openexchange.mail.exportpdf.converters;

import java.util.HashSet;
import java.util.Set;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converter.WriteThroughMailExportConversionResult;

/**
 * {@link AbstractWriteThroughMailExportConverter} - Abstraction for common logic
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
abstract class AbstractWriteThroughMailExportConverter implements MailExportConverter {

    private final LeanConfigurationService leanConfigService;
    private Property fileExtensions;

    /**
     * Initialises a new {@link AbstractWriteThroughMailExportConverter}.
     */
    AbstractWriteThroughMailExportConverter(LeanConfigurationService leanConfigService, Property fileExtensions) {
        super();
        this.leanConfigService = leanConfigService;
        this.fileExtensions = fileExtensions;
    }

    /**
     * Writes the mail part's input stream as is
     *
     * @param mailPart The mail part
     * @param options The options
     * @return The result
     * @throws OXException if an error is occurred
     */
    @Override
    public MailExportConversionResult convert(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        if (MailExportConverterUtil.handles(mailPart, getEnabledFileExtensions(options))) {
            return new WriteThroughMailExportConversionResult(mailPart);
        }
        return new WriteThroughMailExportConversionResult(Status.TYPE_NOT_SUPPORTED);
    }

    @Override
    public boolean handles(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        return MailExportConverterUtil.handles(mailPart, getEnabledFileExtensions(options));
    }

    /**
     * Retrieves the enabled file extensions
     * 
     * @param options the options
     * @return a set with all enabled file extensions
     */
    private Set<String> getEnabledFileExtensions(MailExportConverterOptions options) {
        String value = leanConfigService.getProperty(options.getSession().getUserId(), options.getSession().getContextId(), fileExtensions);
        return Strings.splitByComma(value, new HashSet<>());

    }
}
