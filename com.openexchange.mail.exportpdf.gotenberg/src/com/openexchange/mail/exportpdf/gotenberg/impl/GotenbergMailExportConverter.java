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

package com.openexchange.mail.exportpdf.gotenberg.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.gotenberg.client.ConversionOptions;
import com.openexchange.gotenberg.client.ConversionOptions.ConversionOptionsBuilder;
import com.openexchange.gotenberg.client.GotenbergClient;
import com.openexchange.gotenberg.client.GotenbergClient.Document;
import com.openexchange.gotenberg.client.GotenbergClient.HTMLDocument;
import com.openexchange.gotenberg.client.GotenbergClientAccess;
import com.openexchange.gotenberg.client.PDFFormat;
import com.openexchange.gotenberg.client.PageProperties;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.MailExportConverter;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;
import com.openexchange.mail.exportpdf.converters.FileHolderConversionResult;
import com.openexchange.mail.exportpdf.converters.MailExportConverterUtil;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link GotenbergMailExportConverter}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class GotenbergMailExportConverter implements MailExportConverter {

    private static final Logger LOG = LoggerFactory.getLogger(GotenbergMailExportConverter.class);
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final float INCH = 25.4f;

    private final ServiceLookup serviceLookup;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link GotenbergMailExportConverter}.
     *
     * @param serviceLookup The {@link ServiceLookup}
     */
    public GotenbergMailExportConverter(ServiceLookup serviceLookup) {
        this.serviceLookup = serviceLookup;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Internal method to obtain a {@link GotenbergClient} for accessing the "Gotenberg" server
     *
     * @param baseUri The base URI used to connect to the "Gotenberg" service
     * @return The {@link GotenbergClient}
     * @throws OXException if an error is occurred
     */
    private GotenbergClient getClient(String baseUri) throws OXException {
        return this.serviceLookup.getServiceSafe(GotenbergClientAccess.class).getClient(baseUri);
    }

    /**
     * Internal method to obtain the {@link LeanConfigurationService}
     *
     * @return The {@link LeanConfigurationService} if available
     * @throws OXException If the service is not available
     */
    private LeanConfigurationService getConfigurationService() throws OXException {
        return this.serviceLookup.getServiceSafe(LeanConfigurationService.class);
    }

    /**
     * Returns a list of file extensions which should be handled
     *
     * @param session The session
     * @return A set of file extensions which should be handled
     */
    HashSet<String> getEnabledFileExtensions(Session session) throws OXException {
        String property = getConfigurationService().getProperty(session.getUserId(), session.getContextId(), GotenbergProperties.FILE_EXTENSIONS);
        return Strings.splitByComma(property, new HashSet<>());
    }

    /**
     * Gets the PDF Format in which the resulting PDF should be returned from gotenberg
     *
     * @param session The {@link Session}
     * @return The configured {@link PDFFormat} which should be used with gotenberg
     */
    private PDFFormat getPDFFormat(Session session) throws OXException {
        String property = getConfigurationService().getProperty(session.getUserId(), session.getContextId(), GotenbergProperties.PDF_FORMAT);
        try {
            return Strings.isNotEmpty(property) ? PDFFormat.createFrom(property) : PDFFormat.PDF;
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to parse unknown pdfaFormat {}", property, e);
            return PDFFormat.PDF;
        }
    }

    /**
     * Get the {@link PDFAVersion} for the given {@link PDFFormat}
     *
     * @param pdfFormat The {@link PDFFormat} to get the {@link PDFAVersion} for
     * @return The {@link PDFAVersion} for the given {@link PDFFormat}
     */
    private PDFAVersion getPDFVersion(PDFFormat pdfFormat) {
        return switch (pdfFormat) {
            case PDF_A_1_A -> PDFAVersion.PDFA_1_A;
            case PDF_A_1_B -> PDFAVersion.PDFA_1_B;
            case PDF_A_2_B -> PDFAVersion.PDFA_2_B;
            case PDF_A_3_B -> PDFAVersion.PDFA_3_B;
            default -> PDFAVersion.UNKNOWN;
        };
    }

    /**
     * Internal method to obtain the "Gotenberg" base URI from configuration
     *
     * @return The configured base URI to connect to
     * @throws OXException if the base URI cannot be fetched
     */
    private String getBaseURI(Session session) throws OXException {
        return getConfigurationService().getProperty(session.getUserId(), session.getContextId(), GotenbergProperties.URL);
    }

    /**
     * Checks if the given mailPart is a HTML body
     *
     * @param mailPart The part to check
     * @return <code>True</code> if the given maiPart is HTML, <code>False</code> otherwise
     */
    private boolean isHTML(MailExportMailPartContainer mailPart) {
        if (mailPart == null || mailPart.getBaseContentType() == null) {
            return false;
        }
        return CONTENT_TYPE_HTML.equalsIgnoreCase(mailPart.getBaseContentType());
    }

    /**
     * Converts the given amount of millimetres to inch
     *
     * @param ppi The PPI (Point per inch) required for conversion
     * @param millimetres The amount of pixel to convert to inch
     * @return The amount of millimetres converted to inch
     */
    private static float mmToInch(float ppi, float millimetres) {
        return millimetres / ppi;
    }

    /**
     * Converts the given amount of millimetres to inch
     *
     * @param millimetres The amount of millimetres to convert to inch
     * @return The amount of millimetres converted to inch
     */
    private float mmToInch(float millimetres) {
        return mmToInch(INCH, millimetres);
    }

    /**
     * Gets the {@link PageProperties} from the given {@link MailExportConverterOptions}
     *
     * @param options The options to get the {@link PageProperties} for
     * @return The {@link PageProperties} extracted for the given {@link MailExportConverterOptions}
     */
    private PageProperties getPageProperties(MailExportConverterOptions options) {
        //@formatter:off
        return new PageProperties.PagePropertiesBuilder()
            .withPaperHeight(mmToInch(options.getHeight()))
            .withPaperWidth(mmToInch(options.getWidth()))
            .withMarginTop(mmToInch(options.getTopMargin()))
            .withMarginBottom(mmToInch(options.getBottomMargin()))
            .withMarginLeft(mmToInch(options.getLeftMargin()))
            .withMarginRight(mmToInch(options.getRightMargin()))
        .build();
        //@formatter:on
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks whether this Gotenberg converter is enabled by configuration
     *
     * @return <code>True</code> if the Gotenberg service is enabled by configuration, <code>False</code> otherwise
     */
    boolean isEnabled(Session session) throws OXException {
        return getConfigurationService().getBooleanProperty(session.getUserId(), session.getContextId(), GotenbergProperties.ENABLED);
    }

    /**
     * Converts the given {@link HTMLDocument} into a PDF
     *
     * @param options The options to use
     * @param html The actual {@link Document} to convert
     * @param additionalFiles A collection of additional {@link Document}s referenced in the {@link Document} (for example inline images)
     * @return The result of the conversion as {@link MailExportConversionResult}
     * @throws OXException if an error is occurred
     */
    MailExportConversionResult convertHTML(MailExportConverterOptions options, Document html, Collection<Document> additionalFiles) throws OXException {
        GotenbergClient client = getClient(getBaseURI(options.getSession()));
        PDFFormat pdfFormat = getPDFFormat(options.getSession());
        ConversionOptions converterOptions = new ConversionOptionsBuilder().withPDFFormat(pdfFormat).withPageProperties(getPageProperties(options)).build();
        ThresholdFileHolder fileHolder = null;
        try {
            fileHolder = client.getHTMLConversionResult(converterOptions, html, additionalFiles);
            FileHolderConversionResult conversionResult = new FileHolderConversionResult(Status.SUCCESS, getPDFVersion(pdfFormat), fileHolder);
            fileHolder = null;
            return conversionResult;
        } finally {
            Streams.close(fileHolder);
        }
    }

    /**
     * Converts the given {@link Document} into a PDF
     *
     * @param options THe options to use
     * @param document The {@link Document} to convert
     * @return The result of the conversion as {@link MailExportConversionResult}
     * @throws OXException if an error is occurred
     */
    MailExportConversionResult convertDocument(MailExportConverterOptions options, Document document) throws OXException {
        GotenbergClient client = getClient(getBaseURI(options.getSession()));
        PDFFormat pdfFormat = getPDFFormat(options.getSession());
        ConversionOptions converterOptions = new ConversionOptionsBuilder().withPDFFormat(pdfFormat).withPageProperties(getPageProperties(options)).build();
        ThresholdFileHolder fileHolder = null;
        try {
            fileHolder = client.getDocumentConversionResult(converterOptions, Collections.singletonList(document));
            FileHolderConversionResult conversionResult = new FileHolderConversionResult(Status.SUCCESS, getPDFVersion(pdfFormat), fileHolder);
            fileHolder = null;
            return conversionResult;
        } finally {
            Streams.close(fileHolder);
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public MailExportConversionResult convert(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        if (!isEnabled(options.getSession())) {
            return new EmptyMailExportConversionResult(Status.DISABLED);
        }
        if (!handles(mailPart, options)) {
            return new EmptyMailExportConversionResult(Status.TYPE_NOT_SUPPORTED);
        }

        if (isHTML(mailPart)) {
            /* The given attachment is an HTML file */
            return convertHTML(options, new GotenbergDocument(mailPart), Collections.emptyList());
        }

        /* The given attachment is a non-HTML document */
        return convertDocument(options, new GotenbergDocument(mailPart));
    }

    @Override
    public boolean handles(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        return MailExportConverterUtil.handles(mailPart, getEnabledFileExtensions(options.getSession()));
    }
}
