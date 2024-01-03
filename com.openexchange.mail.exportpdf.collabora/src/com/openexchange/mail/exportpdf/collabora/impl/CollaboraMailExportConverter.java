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

package com.openexchange.mail.exportpdf.collabora.impl;

import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.collabora.online.client.CollaboraClient;
import com.openexchange.collabora.online.client.CollaboraClientAccess;
import com.openexchange.collabora.online.client.conversion.ConversionFormat;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.MailExportConverterOptions;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
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
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;

/**
 * {@link CollaboraMailExportConverter} - Performs a mail export by using "Collabora online"
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class CollaboraMailExportConverter implements MailExportConverter {

    private final ServiceLookup serviceLookup;

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link CollaboraMailExportConverter}.
     *
     * @param serviceLookup The {@link ServiceLookup} to use
     */
    public CollaboraMailExportConverter(ServiceLookup serviceLookup) {
        this.serviceLookup = serviceLookup;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

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
        return getConfigurationService().getProperty(session.getUserId(), session.getContextId(), CollaboraProperties.URL);
    }

    /**
     * Returns a list of file extensions which should be handled
     *
     * @param session The session
     * @return A set of file extensions which should be handled
     * @throws OXException if an error is occurred
     */
    HashSet<String> getEnabledFileExtensions(Session session) throws OXException {
        String property = getConfigurationService().getProperty(session.getUserId(), session.getContextId(), CollaboraProperties.FILE_EXTENSIONS);
        return Strings.splitByComma(property, new HashSet<>());
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
     * Gets a {@link CollaboraClient} for accessing the "Collabora online" service
     *
     * @param baseUri the base URI used to connect to to the "Collabora online" service
     * @return The {@link CollaboraClient}
     * @throws OXException if the {@link CollaboraClientAccess} is absent
     */
    private CollaboraClient getCollaboraClient(String baseUri) throws OXException {
        return this.serviceLookup.getServiceSafe(CollaboraClientAccess.class).getClient(baseUri);
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks whether this Collabora converted is enabled by configuration
     *
     * @return <code>True</code> if the Collabora service is enabled by configuration, <code>False</code> otherwise
     */
    boolean isEnabled(Session session) throws OXException {
        return getConfigurationService().getBooleanProperty(session.getUserId(), session.getContextId(), CollaboraProperties.ENABLED);
    }

    /**
     * Converts the the given data to PDF
     *
     * @param data The data to convert
     * @param contentType The content-type of the data
     * @param options The {@link MailExportConverterOptions} to use
     * @return The conversion result as {@link MailExportConversionResult}
     * @throws OXException if an error is occurred
     */
    MailExportConversionResult convertDocument(InputStream data, String contentType, MailExportConverterOptions options) throws OXException {
        Locale locale = getLocale(options.getSession());
        CollaboraClient client = getCollaboraClient(getBaseURI(options.getSession()));
        ThresholdFileHolder fileHolder = null;
        try {
            fileHolder = client.getConversionResult(data, contentType, ConversionFormat.PDF_A_3_B, locale);
            MailExportConversionResult result = new FileHolderConversionResult(Status.SUCCESS, PDFAVersion.PDFA_3_B, fileHolder);
            fileHolder = null;
            return result;
        } finally {
            Streams.close(fileHolder);
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public MailExportConversionResult convert(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        Session session = options.getSession();
        if (!isEnabled(session)) {
            return new EmptyMailExportConversionResult(Status.DISABLED);
        }
        if (!handles(mailPart, options)) {
            return new EmptyMailExportConversionResult(Status.TYPE_NOT_SUPPORTED);
        }

        try (InputStream dataStream = mailPart.getInputStream()) {
            return convertDocument(dataStream, mailPart.getBaseContentType(), options);
        } catch (IOException e) {
            throw MailExportExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public boolean handles(MailExportMailPartContainer mailPart, MailExportConverterOptions options) throws OXException {
        return MailExportConverterUtil.handles(mailPart, getEnabledFileExtensions(options.getSession()));
    }
}
