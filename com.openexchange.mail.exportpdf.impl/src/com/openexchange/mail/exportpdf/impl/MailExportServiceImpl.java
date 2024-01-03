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

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.CapabilitySet;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.crypto.CryptoType.PROTOCOL;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.Quota;
import com.openexchange.file.storage.composition.FilenameValidationUtils;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.file.storage.composition.IDBasedFolderAccess;
import com.openexchange.file.storage.composition.IDBasedFolderAccessFactory;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.crypto.CryptographicAwareMailAccessFactory;
import com.openexchange.mail.exportpdf.DefaultMailExportResult;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.MailExportResult;
import com.openexchange.mail.exportpdf.MailExportService;
import com.openexchange.mail.exportpdf.impl.pdf.PDFExportResult;
import com.openexchange.mail.exportpdf.impl.pdf.PDFInputStreamConverter;
import com.openexchange.mail.service.MailService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.serverconfig.ServerConfigService;
import com.openexchange.session.Session;

/**
 * {@link MailExportServiceImpl} - The mail export service implementation
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailExportServiceImpl implements MailExportService {

    private static final String FALLBACK_NAME = "export";
    private static final String CAPABILITY_NAME = "mail_export_pdf";

    private final MailService mailService;
    private final IDBasedFileAccessFactory fileAccessFactory;
    private final IDBasedFolderAccessFactory folderAccessFactory;
    private final ServiceLookup services;
    private final ServerConfigService serverConfigService;
    private final MailExportConverterRegistry registry;

    /**
     * Initialises a new {@link MailExportServiceImpl}.
     *
     * @param serverConfigService The server config service
     * @param mailService the mail service
     * @param fileAccessFactory the file access factory
     * @param folderAccessFactory the folder access factory
     * @param registry the mail export converter registry
     */
    public MailExportServiceImpl(ServerConfigService serverConfigService, MailService mailService, IDBasedFileAccessFactory fileAccessFactory, IDBasedFolderAccessFactory folderAccessFactory, MailExportConverterRegistry registry, ServiceLookup services) {
        super();
        this.serverConfigService = serverConfigService;
        this.mailService = mailService;
        this.registry = registry;
        this.services = services;
        this.fileAccessFactory = fileAccessFactory;
        this.folderAccessFactory = folderAccessFactory;
    }

    @Override
    public MailExportResult exportMail(Session session, MailExportOptions options) throws OXException {
        CapabilitySet capabilities = services.getServiceSafe(CapabilityService.class).getCapabilities(session);
        if (!capabilities.contains(CAPABILITY_NAME)) {
            throw MailExportExceptionCode.MISSING_CAPABILITIES.create(CAPABILITY_NAME);
        }
        IDBasedFolderAccess folderAccess = folderAccessFactory.createAccess(session);
        if (Strings.isEmpty(options.getDestinationFolderId()) || !folderAccess.exists(options.getDestinationFolderId())) {
            throw FileStorageExceptionCodes.FOLDER_NOT_FOUND.create(options.getDestinationFolderId(), "0", "", I(session.getUserId()), I(session.getContextId()));
        }
        try (PDFExportResult exportResult = convertMail(session, options)) {
            checkQuota(folderAccess, options.getDestinationFolderId(), exportResult.getSize());
            String fileId = saveDocument(session, options.getDestinationFolderId(), exportResult);
            return new DefaultMailExportResult(fileId, exportResult.getWarnings());
        }
    }

    /**
     * Converts the email to a pdf document
     *
     * @param session the session
     * @param options the mail export options
     * @return The export result
     * @throws OXException if an error is occurred
     */
    private PDFExportResult convertMail(Session session, MailExportOptions options) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try (PDFInputStreamConverter inputStreamConverter = new PDFInputStreamConverter(services, serverConfigService, services.getServiceSafe(LeanConfigurationService.class), services.getServiceSafe(TranslatorFactory.class), registry, session, options)) {
            mailAccess = getMailAccess(session, options);
            return inputStreamConverter.convert(mailAccess);
        } finally {
            MailAccess.closeInstance(mailAccess, false);
        }
    }

    /**
     * Saves the document
     *
     * @param session The session
     * @param folderId The folder id
     * @param exportResult The exported result
     * @return The file identifier of the saved file
     * @throws OXException if an error is occurred
     */
    private String saveDocument(Session session, String folderId, PDFExportResult exportResult) throws OXException {
        IDBasedFileAccess fileAccess = fileAccessFactory.createAccess(session);
        try {
            fileAccess.startTransaction();
            try (InputStream inputStream = exportResult.getStream()) {
                String result = fileAccess.saveDocument(createMetadata(exportResult, folderId), inputStream, FileStorageFileAccess.UNDEFINED_SEQUENCE_NUMBER);
                fileAccess.commit();
                return result;
            }
        } catch (OXException e) {
            fileAccess.rollback();
            throw e;
        } catch (IOException e) {
            fileAccess.rollback();
            throw MailExceptionCode.IO_ERROR.create(e);
        } finally {
            fileAccess.finish();
        }
    }

    /**
     * Retrieves the mail access
     *
     * @param session The session
     * @param options The options
     * @return The mail access
     * @throws OXException if an error is occurred
     */
    private MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getMailAccess(Session session, MailExportOptions options) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, options.getAccountId());
            if (options.isEncrypted()) {
                mailAccess = createCryptoMailAccess(mailAccess, options.getDecryptionToken());
            }
            mailAccess.connect(false);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> retval = mailAccess;
            mailAccess = null; // Avoid premature closing
            return retval;
        } finally {
            MailAccess.closeInstance(mailAccess, false);
        }
    }

    /**
     * Wraps the given mail access with crypto (guard) capabilities
     *
     * @param mailAccess The {@link MailAccess} to wrap
     * @param authToken The authentication-token required for some cryptographic actions, or <code>null</code> if the token is not required or should be obtained from the session.
     * @return The mail access enhanced with cryptographic guard capabilities
     * @throws OXException if the crypto mail access is not available
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> createCryptoMailAccess(MailAccess mailAccess, String authToken) throws OXException {
        return services.getServiceSafe(CryptographicAwareMailAccessFactory.class).createAccess(mailAccess, mailAccess.getSession(), authToken, PROTOCOL.PGP);
    }

    /**
     * Check for enough quota in destination
     *
     * @param folderAccess The folder access
     * @param destinationFolderId the destination folder id
     * @param totalLength The total length that will be stored
     * @throws OXException if quota is not enough
     */
    private void checkQuota(IDBasedFolderAccess folderAccess, String destinationFolderId, long totalLength) throws OXException {
        if (totalLength <= 0) {
            return;
        }
        Quota storageQuota = folderAccess.getStorageQuota(destinationFolderId);
        if (null == storageQuota || Quota.UNLIMITED == storageQuota.getLimit()) {
            return;
        }
        if (storageQuota.getUsage() + totalLength > storageQuota.getLimit()) {
            throw FileStorageExceptionCodes.QUOTA_REACHED.create();
        }
    }

    /**
     * Creates the file metadata from the specified parameters
     *
     * @param exportResult The export result
     * @param folderId The drive folder id
     * @return The {@link File} with the metadata
     */
    private static File createMetadata(PDFExportResult exportResult, String folderId) {
        File metadata = new DefaultFile();
        String subject = FilenameValidationUtils.sanitizeName(exportResult.getTitle(), FALLBACK_NAME);
        metadata.setFileName(subject + ".pdf");
        metadata.setTitle(subject);
        metadata.setFolderId(folderId);
        metadata.setLastModified(new Date());
        metadata.setCreated(exportResult.getDate());
        metadata.setVersion("1");
        metadata.setFileMIMEType("application/pdf");
        return metadata;
    }
}
