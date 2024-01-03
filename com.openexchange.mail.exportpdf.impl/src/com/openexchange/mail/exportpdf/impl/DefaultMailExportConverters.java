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
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converter.MailExportConverters;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;
import com.openexchange.mail.exportpdf.pdfa.PDFAService;

/**
 * {@link DefaultMailExportConverters} - A collection of multiple {@link MailExportConverter}s
 * that can/may handle a single {@link MailPart}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DefaultMailExportConverters implements MailExportConverters {

    private static final PDFAVersion PDFA_TARGET_VERSION = PDFAVersion.PDFA_3_B;
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMailExportConverters.class);

    private final List<MailExportConverter> converters;
    private final PDFAService pdfaService;

    /**
     * Default constructor
     *
     * @param converters A list with converters
     * @param pdfService The {@link PDFAService} for converting non-PDF/A results to PDF/A
     */
    public DefaultMailExportConverters(List<MailExportConverter> converters, PDFAService pdfaService) {
        this.converters = converters;
        this.pdfaService = pdfaService;
    }

    @Override
    public MailExportConversionResult convert(MailExportMailPartContainer mailPart, MailExportConverterOptions options) {
        if (null == converters || converters.isEmpty()) {
            LOG.debug("No converter available for {}.", mailPart);
            return new EmptyMailExportConversionResult(Status.UNAVAILABLE, null, mailPart.getTitle());
        }
        List<OXException> warnings = new LinkedList<>();
        for (MailExportConverter converter : converters) {
            MailExportConversionResult conversionResult = null;
            boolean success = false;
            LOG.trace("Trying to convert {} using {}...", mailPart, converter);
            long start = System.nanoTime();
            try {
                conversionResult = converter.convert(mailPart, options);
                if (null == conversionResult) {
                    String msg = "Got no result from " + converter + " when trying to convert " + mailPart + '.';
                    warnings.add(MailExportExceptionCode.UNEXPECTED_ERROR.create(msg));
                    LOG.warn(msg);
                    continue;
                }
                if (!Status.SUCCESS.equals(conversionResult.getStatus())) {
                    LOG.debug("Got status {} when converting {} using {}.", conversionResult.getStatus(), mailPart, converter);
                    warnings.addAll(conversionResult.getWarnings());
                    continue;
                }
                if (!CONTENT_TYPE_PDF.equalsIgnoreCase(conversionResult.getContentType()) || conversionResult.getPDFAVersion() == PDFA_TARGET_VERSION) {
                    LOG.debug("Successfully converted {} via {}, resulting content type: {}", mailPart, converter, conversionResult.getContentType());
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
                        LOG.debug("Got status {} when converting {} to PDF/A .", pdfaConversionResult.getStatus(), mailPart);
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
                LOG.debug("Error converting {} using {}: {}", mailPart, converter, e.getMessage(), e);
                warnings.add(e);
            } catch (Exception e) {
                LOG.warn("Unexpected error converting {} using {}: {}", mailPart, converter, e.getMessage(), e);
                warnings.add(new OXException(e));
            } finally {
                if (null != conversionResult && !success) {
                    Streams.close(conversionResult);
                }
                LOG.trace("Conversion via {} finished, {}ms elapsed.", converter, L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
            }
        }
        LOG.debug("No successful conversion for {}", mailPart);
        return new EmptyMailExportConversionResult(Status.ERROR, warnings, mailPart.getTitle());
    }

    @Override
    public boolean handles(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        if (null == converters || converters.isEmpty()) {
            LOG.debug("No converter available for {}.", mailPart);
            return false;
        }
        for (MailExportConverter converter : converters) {
            if (converter.handles(mailPart, options)) {
                return true;
            }
        }
        return false;
    }
}
