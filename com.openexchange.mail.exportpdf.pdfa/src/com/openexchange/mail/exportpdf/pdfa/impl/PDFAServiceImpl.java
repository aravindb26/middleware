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

package com.openexchange.mail.exportpdf.pdfa.impl;

import static com.openexchange.java.Autoboxing.L;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;
import com.openexchange.mail.exportpdf.pdfa.PDFAConverter;
import com.openexchange.mail.exportpdf.pdfa.PDFAService;
import com.openexchange.mail.exportpdf.pdfa.exceptions.PDFAServiceExceptionCodes;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.session.Session;

/**
 * {@link PDFAServiceImpl} - The implementation of the {@link PDFAService}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class PDFAServiceImpl implements PDFAService {

    private static final Logger LOG = LoggerFactory.getLogger(PDFAServiceImpl.class);
    private final ServiceListing<PDFAConverter> converterListing;

    /**
     * Initializes a new {@link PDFAServiceImpl}.
     *
     * @param converterListing The {@link ServiceListing} containing all the available {@link PDFAConverter} to use
     */
    public PDFAServiceImpl(ServiceListing<PDFAConverter> converterListing) {
        this.converterListing = Objects.requireNonNull(converterListing, "converterListing must not be null");
    }

    private MailExportConversionResult convert(Session session, PDFAVersion pdfaVersion, InputStream pdfData) {
        if (converterListing.getServiceList() == null || converterListing.getServiceList().isEmpty()) {
            LOG.debug("No PDF/A converter available for {}", pdfaVersion);
            return new EmptyMailExportConversionResult(Status.UNAVAILABLE);
        }

        var warnings = new ArrayList<OXException>();
        for (PDFAConverter converter : converterListing.getServiceList()) {
            LOG.trace("Trying to convert to {} using {}...", pdfaVersion, converter);
            long start = System.nanoTime();
            try {
                MailExportConversionResult result = converter.convert(session, pdfaVersion, pdfData);
                if (!Status.SUCCESS.equals(result.getStatus())) {
                    LOG.debug("Got status {} when converting the HTML body using {}.", result.getStatus(), converter);
                    warnings.addAll(result.getWarnings());
                    continue;
                }
                LOG.debug("Successfully converted to {} via {}", pdfaVersion, converter);
                return result;
            } catch (Exception e) {
                LOG.debug("Error converting to {} using {}: {}", pdfaVersion, converter, e.getMessage(), e);
                warnings.add((e instanceof OXException oxe) ? oxe : new OXException(e));
            } finally {
                LOG.trace("Conversion via {} finished, {}ms elapsed.", converter, L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
            }
        }

        LOG.debug("No successful conversion to {}", pdfaVersion);
        return new EmptyMailExportConversionResult(Status.ERROR, warnings, null);
    }

    @Override
    public MailExportConversionResult convert(Session session, PDFAVersion pdfVersion, MailExportConversionResult result) throws OXException {
        return convert(session, pdfVersion, result, null);
    }

    @Override
    public MailExportConversionResult convert(Session session, PDFAVersion pdfVersion, MailExportConversionResult result, Function<MailExportConversionResult, PDFAVersion> versionSupplier) throws OXException {
        if (!result.getStatus().equals(Status.SUCCESS)) {
            /* corrupted result object; nothing to do */
            return result;
        }

        PDFAVersion currentVersion = result.getPDFAVersion();
        if (currentVersion.equals(pdfVersion)) {
            /* already in the desired version; nothing to do */
            return result;
        }
        if (versionSupplier != null && currentVersion.equals(PDFAVersion.UNKNOWN)) {
            /* if the PDF/A version is unknown, we can try to parse/determine it */
            currentVersion = versionSupplier.apply(result);
            if (currentVersion.equals(pdfVersion)) {
                /* already in the desired version; nothing to do */
                return result;
            }
        }

        /* convert the result into the desired PDF version */
        var warnings = new ArrayList<OXException>();
        try (InputStream pdfData = result.getInputStream()) {
            MailExportConversionResult pdfaConversionResult = convert(session, pdfVersion, pdfData);
            if (!Status.SUCCESS.equals(pdfaConversionResult.getStatus())) {
                String title = pdfaConversionResult.getTitle().orElseGet(UUID.randomUUID()::toString);
                LOG.debug("Got status {} when converting {} using PDFA/Service.", pdfaConversionResult.getStatus(), title);
                warnings.addAll(pdfaConversionResult.getWarnings());
            }
            return pdfaConversionResult;
        } catch (IOException e) {
            throw PDFAServiceExceptionCodes.IO_ERROR.create(e, e.getMessage());
        }
    }
}
