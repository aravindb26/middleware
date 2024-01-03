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

package com.openexchange.mail.exportpdf.pdfa.collabora.impl;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.collabora.online.client.CollaboraClient;
import com.openexchange.collabora.online.client.CollaboraClientAccess;
import com.openexchange.collabora.online.client.conversion.ConversionFormat;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.converter.EmptyMailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult.Status;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;
import com.openexchange.mail.exportpdf.converters.FileHolderConversionResult;
import com.openexchange.mail.exportpdf.pdfa.PDFAConverter;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;

/**
 * {@link CollaboraPDFAConverter} - converts a given PDF to PDF/A using "Collabora"
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraPDFAConverter implements PDFAConverter {

    /* @formatter:off */

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final Map<PDFAVersion, ConversionFormat> VERSIONS = Map.of(
            PDFAVersion.PDFA_1_B, ConversionFormat.PDF_A_1_B,
            PDFAVersion.PDFA_2_B, ConversionFormat.PDF_A_2_B,
            PDFAVersion.PDFA_3_B, ConversionFormat.PDF_A_3_B
    );

    /* @formatter:on */

    private final ServiceLookup serviceLookup;

    /**
     * Initializes a new {@link CollaboraPDFAConverter}.
     *
     * @param serviceLookup The {@link ServiceLookup} to use
     */
    public CollaboraPDFAConverter(ServiceLookup serviceLookup) {
        this.serviceLookup = serviceLookup;
    }

    //---------------------------------------------------------------------------------------------

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
     * Internal method to obtain the {@link UserService}
     *
     * @return The {@link UserService} if available
     * @throws OXException If the service is not available
     */
    private UserService getUserService() throws OXException {
        return this.serviceLookup.getServiceSafe(UserService.class);
    }

    /**
     * Internal method to obtain the "Collabora online" base URI from configuration
     *
     * @return The configured base URI to connect to
     * @throws OXException if the base URI cannot be fetched
     */
    private String getBaseURI(Session session) throws OXException {
        String url = getConfigurationService().getProperty(session.getUserId(), session.getContextId(), CollaboraPDFAProperties.URL);
        return Strings.isNotEmpty(url) ? url : getConfigurationService().getProperty(session.getUserId(), session.getContextId(), CollaboraPDFAProperties.URL2);
    }

    /**
     * Gets a {@link CollaboraClient} for accessing the "Collabora online" service
     *
     * @param baseUri the base URI used to connect to to the "Collabora online" service
     * @return The {@link CollaboraClient}
     * @throws OXException if the {@link CollaboraClientAccess} is absent
     */
    private CollaboraClient getCollaboraClient(String baseUri) throws OXException {
        return this.serviceLookup.getServiceSafe(CollaboraClientAccess.class).getClient(baseUri);
    }

    /**
     * Gets the correct Collabora {@link ConversionFormat} for the given {@link PDFAVersion}
     *
     * @param format The {@link PDFAVersion}
     * @return The correct Collabora {@link ConversionFormat} for the given {@link PDFAVersion}
     * @throws IllegalArgumentException if the given format is not supported or unknown
     */
    private ConversionFormat getCollaboraFormat(PDFAVersion format) {
        if (VERSIONS.containsKey(format)) {
            return VERSIONS.get(format);
        }
        throw new IllegalArgumentException("Unknown PDFAFormat: %s".formatted(format));
    }

    /**
     * Internal method to obtain the locale related to the given {@link Session}
     *
     * @param session The {@link Session} to get the locale for
     * @return The locale of the given {@link Session}
     * @throws OXException if the locale of the user cannot be fetched
     */
    private Locale getLocale(Session session) throws OXException {
        if (session instanceof ServerSession serverSession) {
            return serverSession.getUser().getLocale();
        }
        return getUserService().getUser(session.getUserId(), session.getContextId()).getLocale();
    }

    /**
     * Internal method to check if the Collabora PDF/A converter is enabled for the given {@link Session}
     *
     * @param session The {@link Session} to check
     * @return <code>True</code> if the Collabora PDF/A converter is enabled for the given {@link Session}, <code>False</code> otherwise
     * @throws OXException
     */
    private boolean isEnabled(Session session) throws OXException {
        return getConfigurationService().getBooleanProperty(session.getUserId(), session.getContextId(), CollaboraPDFAProperties.ENABLED);
    }

    //---------------------------------------------------------------------------------------------

    @Override
    public MailExportConversionResult convert(Session session, PDFAVersion pdfVersion, InputStream pdfData) throws OXException {
        if (!isEnabled(session) || !VERSIONS.keySet().contains(pdfVersion)) {
            return new EmptyMailExportConversionResult(Status.DISABLED);
        }

        CollaboraClient client = getCollaboraClient(getBaseURI(session));
        ThresholdFileHolder pdfaFileHolder = client.getConversionResult(pdfData, CONTENT_TYPE_PDF, getCollaboraFormat(pdfVersion), getLocale(session));
        return new FileHolderConversionResult(Status.SUCCESS, pdfVersion, pdfaFileHolder);
    }
}
