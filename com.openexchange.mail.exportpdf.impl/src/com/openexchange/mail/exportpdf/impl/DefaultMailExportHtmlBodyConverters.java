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

import static com.openexchange.java.Autoboxing.L;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.exportpdf.MailExportBodyInformation;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverter;
import com.openexchange.mail.exportpdf.converter.MailExportHtmlBodyConverters;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;
import com.openexchange.mail.exportpdf.pdfa.PDFAService;

/**
 * {@link DefaultMailExportHtmlBodyConverters} - A collection of multiple {@link MailExportHtmlBodyConverter}s
 * that can/may handle a single {@link MailPart}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class DefaultMailExportHtmlBodyConverters implements MailExportHtmlBodyConverters {

    private static final PDFAVersion PDFA_TARGET_VERSION = PDFAVersion.PDFA_3_B;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMailExportHtmlBodyConverters.class);

    private final List<MailExportHtmlBodyConverter> bodyConverters;
    private final PDFAService pdfaService;

    /**
     * Initializes a new {@link DefaultMailExportHtmlBodyConverters}.
     *
     * @param bodyConverters A list with HTML body converters
     */
    public DefaultMailExportHtmlBodyConverters(List<MailExportHtmlBodyConverter> bodyConverters, PDFAService pdfaService) {
        this.bodyConverters = bodyConverters;
        this.pdfaService = pdfaService;
    }

    @Override
    public MailExportConversionResult convertHtmlBody(MailExportBodyInformation mailBody, MailExportConverterOptions options) {
        if (null == bodyConverters || bodyConverters.isEmpty()) {
            LOG.debug("No converter available for the HTML body.");
            return new EmptyMailExportConversionResult(Status.UNAVAILABLE);
        }
        List<OXException> warnings = new LinkedList<>();
        for (MailExportHtmlBodyConverter converter : bodyConverters) {
            MailExportConversionResult conversionResult = null;
            boolean success = false;
            LOG.trace("Trying to convert the HTML body using {}...", converter);
            long start = System.nanoTime();
            try {
                conversionResult = converter.convertHtmlBody(mailBody, options);
                if (null == conversionResult) {
                    String msg = "Got no result from " + converter + " when trying to convert the HTML body";
                    warnings.add(MailExportExceptionCode.UNEXPECTED_ERROR.create(msg));
                    LOG.warn(msg);
                    continue;
                }
                if (!Status.SUCCESS.equals(conversionResult.getStatus())) {
                    LOG.debug("Got status {} when converting the HTML body using {}.", conversionResult.getStatus(), converter);
                    warnings.addAll(conversionResult.getWarnings());
                    continue;
                }
                if (conversionResult.getPDFAVersion() == PDFA_TARGET_VERSION) {
                    LOG.debug("Successfully converted HTML body via {}, resulting content type: {}", converter, conversionResult.getContentType());
                    success = true;
                    conversionResult.addWarnings(warnings);
                    return conversionResult;
                }
                /* Ensure the created export result is the desired PDF/A format, or perform another conversion step otherwise */
                MailExportConversionResult pdfaConversionResult = null;
                boolean pdfaSuccess = false;
                try {
                    pdfaConversionResult = pdfaService.convert(options.getSession(), PDFA_TARGET_VERSION, conversionResult);
                    if (!Status.SUCCESS.equals(pdfaConversionResult.getStatus())) {
                        LOG.debug("Got status {} when converting {} to PDF/A.", pdfaConversionResult.getStatus(), pdfaConversionResult.getTitle());
                        warnings.addAll(pdfaConversionResult.getWarnings());
                        continue;
                    }
                    pdfaSuccess = true;
                    return pdfaConversionResult;
                } finally {
                    if (!pdfaSuccess) {
                        Streams.close(pdfaConversionResult);
                    }
                    Streams.close(conversionResult);
                    conversionResult = null;
                }
            } catch (OXException e) {
                LOG.debug("Error converting HTML body using {}: {}", converter, e.getMessage(), e);
                warnings.add(e);
            } catch (Exception e) {
                LOG.warn("Unexpected error converting HTML body using {}: {}", converter, e.getMessage(), e);
                warnings.add(new OXException(e));
            } finally {
                if (null != conversionResult && !success) {
                    Streams.close(conversionResult);
                }
                LOG.trace("Conversion via {} finished, {}ms elapsed.", converter, L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
            }
        }
        LOG.debug("No successful conversion for HTML body");
        return new EmptyMailExportConversionResult(Status.ERROR, warnings, null);
    }
}
