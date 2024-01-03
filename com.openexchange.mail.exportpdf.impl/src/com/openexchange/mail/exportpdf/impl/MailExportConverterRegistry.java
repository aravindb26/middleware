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

package com.openexchange.mail.exportpdf.impl;

import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converter.MailExportConverters;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverters;
import com.openexchange.mail.exportpdf.pdfa.PDFAService;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.server.ServiceLookup;

/**
 * {@link MailExportConverterRegistry} - A simple registry of keeping track various ranked {@link MailExportConverter}s
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailExportConverterRegistry {

    private final ServiceListing<MailExportConverter> converterListing;
    private final ServiceListing<MailExportHtmlBodyConverter> bodyConverterListing;
    private final ServiceLookup services;

    /**
     * Initialises a new {@link MailExportConverterRegistry}.
     *
     * @param services The service lookup instance
     * @param bodyConverterListing The service listing for the HTML Body converters
     * @param converterListing The service listing for the mail export converters
     */
    public MailExportConverterRegistry(ServiceLookup services, ServiceListing<MailExportHtmlBodyConverter> bodyConverterListing, ServiceListing<MailExportConverter> converterListing) {
        this.services = services;
        this.bodyConverterListing = bodyConverterListing;
        this.converterListing = converterListing;
    }

    /**
     * Returns the services
     *
     * @return the service lookup instance
     */
    public ServiceLookup getServices() {
        return services;
    }

    /**
     * Gets the converter(s) that support the specified content type
     *
     * @param contentType The content type
     * @param options The options
     * @return A list with mail export converters
     * @throws OXException
     */
    public MailExportConverters getConverters() throws OXException {
        return new DefaultMailExportConverters(converterListing.getServiceList(), services.getServiceSafe(PDFAService.class));
    }

    /**
     * Gets the converter(s) that support HTML body conversion with the given {@link MailExportConverterOptions}
     *
     * @return the {@link MailExportHtmlBodyConverter}
     * @throws OXException if an error is occurred
     */
    public MailExportHtmlBodyConverters getBodyConverters() throws OXException {
        return new DefaultMailExportHtmlBodyConverters(bodyConverterListing.getServiceList(), services.getServiceSafe(PDFAService.class));
    }
}
