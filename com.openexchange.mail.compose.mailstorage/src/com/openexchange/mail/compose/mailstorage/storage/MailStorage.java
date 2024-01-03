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

package com.openexchange.mail.compose.mailstorage.storage;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.logging.LogUtility.toStringObjectFor;
import static com.openexchange.mail.compose.CompositionSpaces.getUUIDForLogging;
import static java.util.stream.Collectors.toList;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.ReadResponseTimeoutRestorer;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import com.google.common.collect.Sets;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.crypto.CryptographicServiceAuthenticationFactory;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.CapabilitySet;
import com.openexchange.config.ConfigTools;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.crypto.CryptoType;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.java.CombinedInputStream;
import com.openexchange.java.CountingOutputStream;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.Quota;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.IMailMessageStorageEnhancedDeletion;
import com.openexchange.mail.api.IMailMessageStorageMimeSupport;
import com.openexchange.mail.api.IMailStoreAware;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.crypto.CryptographicAwareMailAccessFactory;
import com.openexchange.mail.compose.Address;
import com.openexchange.mail.compose.Attachment;
import com.openexchange.mail.compose.AttachmentDescription;
import com.openexchange.mail.compose.AttachmentOrigin;
import com.openexchange.mail.compose.AttachmentStorages;
import com.openexchange.mail.compose.ByteArrayDataProvider;
import com.openexchange.mail.compose.ClientToken;
import com.openexchange.mail.compose.CompositionSpaceErrorCode;
import com.openexchange.mail.compose.CompositionSpaces;
import com.openexchange.mail.compose.DefaultAttachment;
import com.openexchange.mail.compose.HeaderUtility;
import com.openexchange.mail.compose.Message.ContentType;
import com.openexchange.mail.compose.Message.Priority;
import com.openexchange.mail.compose.MessageDescription;
import com.openexchange.mail.compose.MessageField;
import com.openexchange.mail.compose.Meta;
import com.openexchange.mail.compose.Meta.MetaType;
import com.openexchange.mail.compose.Security;
import com.openexchange.mail.compose.SharedAttachmentsInfo;
import com.openexchange.mail.compose.SharedFolderReference;
import com.openexchange.mail.compose.Type;
import com.openexchange.mail.compose.VCardAndFileName;
import com.openexchange.mail.compose.mailstorage.MailStorageCompositionSpaceConfig;
import com.openexchange.mail.compose.mailstorage.cache.CacheReference;
import com.openexchange.mail.compose.mailstorage.util.TrackingInputStream;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.SecuritySettings;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.json.compose.share.AttachmentStorageRegistry;
import com.openexchange.mail.json.compose.share.spi.AttachmentStorage;
import com.openexchange.mail.mime.HeaderCollection;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizerService;
import com.openexchange.mail.mime.filler.MimeMessageFiller;
import com.openexchange.mail.mime.processing.MimeProcessingUtility;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.NonInlineForwardPartHandler;
import com.openexchange.mail.search.ANDTerm;
import com.openexchange.mail.search.ComparisonType;
import com.openexchange.mail.search.FlagTerm;
import com.openexchange.mail.search.HeaderExistenceTerm;
import com.openexchange.mail.search.HeaderTerm;
import com.openexchange.mail.search.ORTerm;
import com.openexchange.mail.search.ReceivedDateTerm;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.service.EncryptedMailService;
import com.openexchange.mail.service.MailService;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link MailStorage} - Accesses mail storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class MailStorage implements IMailStorage {

    /** The logger constant */
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailStorage.class);

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final ServiceLookup services;

    /**
     * Initializes a new {@link MailStorage}.
     *
     * @param services The service look-up
     */
    public MailStorage(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public MailStorageResult<Optional<MailStorageId>> lookUp(MailPath draftPath, Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(true);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            Optional<MailMessage> optionalDraftMail = getMail(draftPath, mailAccess.getMessageStorage());
            if (optionalDraftMail.isPresent()) {
                Optional<UUID> optCompositionSpaceId = parseCompositionSpaceId(optionalDraftMail.get());
                if (optCompositionSpaceId.isPresent()) {
                    DefaultMailStorageId mailStorageId = new DefaultMailStorageId(draftPath, optCompositionSpaceId.get(), Optional.empty());
                    return MailStorageResult.resultFor(mailStorageId, Optional.of(mailStorageId), false, mailAccess);
                }
            }

            return MailStorageResult.resultFor(null, Optional.empty(), false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<Optional<MailStorageId>> lookUp(UUID compositionSpaceId, Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(true);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            Optional<MailPath> optionalPath = doLookUp(compositionSpaceId, mailAccess.getFolderStorage().getDraftsFolder(), mailAccess.getMessageStorage());
            if (optionalPath.isPresent()) {
                DefaultMailStorageId mailStorageId = new DefaultMailStorageId(optionalPath.orElse(null), compositionSpaceId, Optional.empty());
                return MailStorageResult.resultFor(mailStorageId, Optional.of(mailStorageId), false, mailAccess);
            }

            return MailStorageResult.resultFor(null, Optional.empty(), false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    private SearchTerm<?> craftSearchTermForLookUp(Session session) throws OXException {
        // Search for undeleted, unexpired mails having a "X-OX-Composition-Space-Id" header
        long maxIdleTimeMillis = getMaxIdleTimeMillis(session);
        SearchTerm<?> searchTerm;
        {
            HeaderTerm lookUpTerm = new HeaderTerm(HeaderUtility.HEADER_X_OX_COMPOSITION_SPACE_LOOK_UP, "true");
            HeaderExistenceTerm headerExistenceTerm = new HeaderExistenceTerm(HeaderUtility.HEADER_X_OX_COMPOSITION_SPACE_ID);
            searchTerm = new ORTerm(lookUpTerm, headerExistenceTerm);
        }
        if (maxIdleTimeMillis > 0) {
            ReceivedDateTerm receivedDateTerm = new ReceivedDateTerm(ComparisonType.GREATER_EQUALS, new Date(System.currentTimeMillis() - maxIdleTimeMillis));
            searchTerm = new ANDTerm(searchTerm, receivedDateTerm);
        }
        return new ANDTerm(searchTerm, new FlagTerm(MailMessage.FLAG_DELETED, false));
    }

    private static final MailField[] MAIL_FIELDS_COUNT = new MailField[] { MailField.ID };

    @Override
    public int getNumberOfCompositionSpaces(Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(true);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            String draftsFolder = mailAccess.getFolderStorage().getDraftsFolder();

            SearchTerm<?> searchTerm = craftSearchTermForLookUp(session);
            MailMessage[] mailMessages = mailAccess.getMessageStorage().searchMessages(draftsFolder, IndexRange.NULL, MailSortField.RECEIVED_DATE, OrderDirection.DESC, searchTerm, MAIL_FIELDS_COUNT);

            return mailMessages == null ? 0 : mailMessages.length;
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    private static final MailField[] MAIL_FIELDS_LOOK_UP = new MailField[] { MailField.ID, MailField.RECEIVED_DATE, MailField.HEADERS, MailField.SIZE };

    @Override
    public MailStorageResult<LookUpOutcome> lookUp(Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(true);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            String draftsFolder = mailAccess.getFolderStorage().getDraftsFolder();

            // Search for undeleted, unexpired mails having a "X-OX-Composition-Space-Id" header and sort them by received-date descendingly
            SearchTerm<?> searchTerm = craftSearchTermForLookUp(session);
            MailMessage[] mailMessages = mailAccess.getMessageStorage().searchMessages(draftsFolder, IndexRange.NULL, MailSortField.RECEIVED_DATE, OrderDirection.DESC, searchTerm, MAIL_FIELDS_LOOK_UP);

            // No such mails
            if (mailMessages == null || mailMessages.length == 0) {
                LOG.debug("Found no open composition spaces");
                return MailStorageResult.resultFor(null, LookUpOutcome.EMPTY, false, mailAccess);
            }

            // Filter duplicate ones
            Map<UUID, MailMessage> id2Message = new LinkedHashMap<>(mailMessages.length);
            Map<MailPath, UUID> duplicateSpaces = null;
            for (MailMessage mailMessage : mailMessages) {
                if (mailMessage != null) {
                    Optional<UUID> optCompositionSpaceId = parseCompositionSpaceId(mailMessage);
                    if (!optCompositionSpaceId.isPresent()) {
                        continue;
                    }

                    UUID compositionSpaceId = optCompositionSpaceId.get();
                    MailMessage existing = id2Message.putIfAbsent(compositionSpaceId, mailMessage);

                    if (existing != null) {
                        // Duplicate...
                        if (duplicateSpaces == null) {
                            duplicateSpaces = new HashMap<>();
                        }
                        if (mailMessage.getReceivedDate().getTime() > existing.getReceivedDate().getTime()) {
                            // Keep the newer one
                            id2Message.put(compositionSpaceId, mailMessage);
                            duplicateSpaces.put(new MailPath(Account.DEFAULT_ID, draftsFolder, existing.getMailId()), compositionSpaceId);
                        } else {
                            duplicateSpaces.put(new MailPath(Account.DEFAULT_ID, draftsFolder, mailMessage.getMailId()), compositionSpaceId);
                        }
                    }
                }
            }

            // Help GC
            mailMessages = null;

            Map<MailPath, UUID> mailPathsToUUIDs = new LinkedHashMap<>(id2Message.size());
            for (Map.Entry<UUID, MailMessage> id2MessageEntry : id2Message.entrySet()) {
                mailPathsToUUIDs.put(new MailPath(Account.DEFAULT_ID, draftsFolder, id2MessageEntry.getValue().getMailId()), id2MessageEntry.getKey());
            }
            LOG.debug("Found open composition spaces: {}", mailPathsToUUIDs.values().stream().map(uuid -> getUUIDForLogging(uuid)).collect(toList()));
            LookUpOutcome lookUpOutcome = new LookUpOutcome(mailPathsToUUIDs, duplicateSpaces == null ? Collections.emptyMap() : duplicateSpaces);
            return MailStorageResult.resultFor(null, lookUpOutcome, false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<ComposeRequestAndMeta> getForTransport(MailStorageId mailStorageId, ClientToken clientToken, AJAXRequestData request, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Map<Integer, Optional<ReadResponseTimeoutRestorer>> restorers = new HashMap<>(2);
        Map<Integer, MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage>> mailAccesses = new HashMap<>(2);
        try {
            MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> defaultMailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccesses.put(I(Account.DEFAULT_ID), defaultMailAccess);
            defaultMailAccess.connect(false);
            restorers.put(I(Account.DEFAULT_ID), setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), defaultMailAccess));

            MailMessage draftMail = requireDraftMail(mailStorageId, defaultMailAccess);

            MailMessageProcessor processor = MailMessageProcessor.initForTransport(compositionSpaceId, draftMail, session, services);
            checkClientToken(clientToken, processor.getClientToken());

            validateIfNeeded(mailStorageId, processor);

            MessageDescription currentDraft = processor.getCurrentDraft(MessageField.META, MessageField.SECURITY);
            Meta meta = currentDraft.getMeta();
            Optional<MailMessage> optRefMessage = Optional.empty();
            if (meta != null) {
                MailPath referencedMessage = null;
                MetaType metaType = meta.getType();
                if (metaType == MetaType.REPLY || metaType == MetaType.REPLY_ALL) {
                    referencedMessage = meta.getReplyFor();
                } else if (metaType == MetaType.FORWARD_INLINE) {
                    referencedMessage = meta.getForwardsFor().get(0);
                }

                if (referencedMessage != null) {
                    try {
                        optRefMessage = Optional.of(getOriginalMail(session, referencedMessage, mailService, mailAccesses, restorers, defaultMailAccess, getSecurity(currentDraft).getAuthToken()));
                    } catch (OXException e) {
                        LOG.error("Cannot not apply reference headers because fetching the referenced message failed", e);
                    }
                }
            }

            ComposeRequestAndMeta composeRequestAndMeta = new ComposeRequestAndMeta(processor.compileComposeRequest(request, optRefMessage), meta);
            return MailStorageResult.resultFor(mailStorageId, composeRequestAndMeta, true, defaultMailAccess, processor);
        } finally {
            for (Optional<ReadResponseTimeoutRestorer> optRestorer : restorers.values()) {
                if (optRestorer.isPresent()) {
                    optRestorer.get().restore();
                }
            }
            for (MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess : mailAccesses.values()) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<MessageInfo> lookUpMessage(UUID compositionSpaceId, Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        MailMessageProcessor processor = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(true);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            String draftsFolder = mailAccess.getFolderStorage().getDraftsFolder();

            IMailMessageStorage messageStorage = mailAccess.getMessageStorage();
            Optional<MailPath> optMailPath = doLookUp(compositionSpaceId, draftsFolder, messageStorage);
            if (!optMailPath.isPresent()) {
                throw CompositionSpaceErrorCode.NO_SUCH_COMPOSITION_SPACE.create(getUUIDForLogging(compositionSpaceId));
            }

            MailPath draftPath = optMailPath.get();
            MailMessage draftMail = requireDraftMail(new DefaultMailStorageId(draftPath, compositionSpaceId, Optional.empty()), mailAccess);
            if (draftMail.containsHeader(HeaderUtility.HEADER_X_OX_SHARED_FOLDER_REFERENCE)) {
                processor = MailMessageProcessor.initForWrite(compositionSpaceId, draftMail, session, services);
            } else {
                processor = MailMessageProcessor.initReadEnvelope(compositionSpaceId, draftMail, session, services);
            }
            boolean changed = processor.validate();
            MessageDescription currentDraft = processor.getCurrentDraft();
            SecuritySettings securitySettings = getSecuritySettings(currentDraft.getSecurity());
            if (changed) {
                MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
                MailPath newDraftPath = newDraft.getMailPath();
                long size = newDraft.getSize();
                if (size < 0) {
                    size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
                }

                MessageInfo messageInfo = new MessageInfo(processor.getCurrentDraft(), size, newDraft.getSentDate());
                DefaultMailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
                return MailStorageResult.resultFor(newId, messageInfo, true, mailAccess, processor);
            }

            MessageInfo messageInfo = new MessageInfo(currentDraft, draftMail.getSize(), draftMail.getSentDate());
            MailStorageId newId = new DefaultMailStorageId(draftMail.getMailPath(), compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, messageInfo, true, mailAccess);
        } catch (MissingDraftException e) {
            throw CompositionSpaceErrorCode.NO_SUCH_COMPOSITION_SPACE.create(e, getUUIDForLogging(e.getFirstMailStorageId().getCompositionSpaceId()));
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
            closeProcessorSafe(processor);
        }
    }

    @Override
    public MailStorageResult<MessageInfo> getMessage(MailStorageId mailStorageId, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        MailMessageProcessor processor = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            MailMessage draftMail = requireDraftMail(mailStorageId, mailAccess);

            processor = MailMessageProcessor.initReadEnvelope(compositionSpaceId, draftMail, session, services);
            MessageDescription currentDraft = processor.getCurrentDraft();

            MessageInfo messageInfo = new MessageInfo(currentDraft, draftMail.getSize(), draftMail.getSentDate());
            MailStorageId newId = new DefaultMailStorageId(draftMail.getMailPath(), compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, messageInfo, true, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    private static final Set<MessageField> MESSAGE_FIELDS_ALL = Sets.immutableEnumSet(EnumSet.allOf(MessageField.class));

    @Override
    public MailStorageResult<Map<UUID, MessageInfo>> getMessages(Collection<? extends MailStorageId> mailStorageIds, Set<MessageField> fields, Session session) throws OXException, MissingDraftException {
        if (mailStorageIds == null) {
            return null;
        }
        if (mailStorageIds.isEmpty()) {
            return MailStorageResult.resultFor(null, Collections.emptyMap(), false);
        }

        MailFields mailFields = toMailFields(fields);

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            MailStorageId firstMailStorageId = mailStorageIds.iterator().next();
            if (firstMailStorageId.getAccountId() != Account.DEFAULT_ID) {
                throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
            }

            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            Map<UUID, MessageInfo> result = new LinkedHashMap<>(mailStorageIds.size());
            if (mailFields.contains(MailField.FULL) || mailFields.contains(MailField.BODY)) {
                for (MailStorageId mailStorageId : mailStorageIds) {
                    UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
                    MailMessage draftMail = requireDraftMail(mailStorageId, mailAccess);
                    MailMessageProcessor processor = MailMessageProcessor.initReadEnvelope(compositionSpaceId, draftMail, session, services);
                    result.put(compositionSpaceId, new MessageInfo(processor.getCurrentDraft(MESSAGE_FIELDS_ALL),  draftMail.getSize(), draftMail.getSentDate()));
                }
            } else {
                Map<String, UUID> mailIds = new HashMap<>(mailStorageIds.size());
                for (MailStorageId mailStorageId : mailStorageIds) {
                    mailIds.put(mailStorageId.getMailId(), mailStorageId.getCompositionSpaceId());
                }

                String folderId = firstMailStorageId.getFolderId();
                mailFields.add(MailField.ID);
                mailFields.add(MailField.HEADERS); // For 'Date'
                mailFields.add(MailField.SIZE);

                MailMessage[] messages = mailAccess.getMessageStorage().getMessages(folderId, mailIds.keySet().toArray(new String[mailIds.size()]), mailFields.toArray());
                for (MailMessage mailMessage : messages) {
                    if (mailMessage != null) {
                        UUID compositionSpaceId = mailIds.remove(mailMessage.getMailId());
                        if (compositionSpaceId != null) {
                            MessageDescription messageDesc = toMessageDescription(mailMessage, fields);
                            result.put(compositionSpaceId, new MessageInfo(messageDesc, mailMessage.getSize(), mailMessage.getSentDate()));
                        }
                    }
                }
                messages = null;

                if (!mailIds.isEmpty()) {
                    List<MailStorageId> absentOnes = new ArrayList<>(mailIds.size());
                    int accountId = firstMailStorageId.getAccountId();
                    for (Map.Entry<String, UUID> mailIdEntry : mailIds.entrySet()) {
                        MailPath mailPath = new MailPath(accountId, folderId, mailIdEntry.getKey());
                        absentOnes.add(new DefaultMailStorageId(mailPath, mailIdEntry.getValue(), Optional.empty()));
                    }
                    throw new MissingDraftException(absentOnes);
                }
            }
            return MailStorageResult.resultFor(null, result, false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<MessageInfo> createNew(UUID compositionSpaceId, MessageDescription draftMessage, Optional<SharedFolderReference> optionalSharedFolderRef, ClientToken clientToken, Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(true);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            String draftsFolder = mailAccess.getFolderStorage().getDraftsFolder();
            Quota storageQuota = mailAccess.getFolderStorage().getQuotas(draftsFolder, new Quota.Type[] { Quota.Type.STORAGE })[0];

            if (storageQuota.getLimit() == 0) {
                // Not possible due to quota restrictions
                throw MailExceptionCode.UNABLE_TO_SAVE_DRAFT_QUOTA.create();
            }

            MailMessageProcessor processor = MailMessageProcessor.initNew(compositionSpaceId, optionalSharedFolderRef, clientToken, session, services);
            processor.applyUpdate(draftMessage);
            processor.addAttachments(draftMessage.getAttachments());
            if (draftMessage.getMeta().getOrigin() == Type.COPY) {
                processor.copyStoredAttachmentsIfAny();
            }
            MessageDescription update = processor.getCurrentDraft();

            ComposedMailMessage composedMessage = processor.compileDraft();
            composedMessage = applyGuardEncryption(getSecuritySettings(draftMessage.getSecurity()), composedMessage, session);
            composedMessage.setSendType(ComposeType.DRAFT);

            // Check against quota limit
            if (storageQuota.getLimitBytes() > 0) {
                checkAvailableQuota(storageQuota, new NewSizeSupplierCallable(composedMessage, LOG));
            }

            IMailMessageStorage draftMessageStorage = mailAccess.getMessageStorage();
            MailMessage savedDraft = saveDraftMail(composedMessage, draftsFolder, true, draftMessageStorage);
            long size = savedDraft.getSize();
            MailPath mailPath = savedDraft.getMailPath();
            if (size < 0) {
                size = fetchMailSize(draftMessageStorage, mailPath);
            }

            DefaultMailStorageId newId = new DefaultMailStorageId(mailPath, compositionSpaceId, processor.getFileCacheReference());
            MessageInfo messageInfo = new MessageInfo(update, size, savedDraft.getSentDate());
            return MailStorageResult.resultFor(newId, messageInfo, false, mailAccess, processor);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<MailPath> saveAsFinalDraft(MailStorageId mailStorageId, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Map<Integer, Optional<ReadResponseTimeoutRestorer>> restorers = new HashMap<>(2);
        Map<Integer, MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage>> mailAccesses = new HashMap<>(2);
        try {
            MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> defaultMailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccesses.put(I(Account.DEFAULT_ID), defaultMailAccess);
            defaultMailAccess.connect(false);
            restorers.put(I(Account.DEFAULT_ID), setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), defaultMailAccess));

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, defaultMailAccess, clientToken);
            MailMessageProcessor processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft(MessageField.META, MessageField.SECURITY, MessageField.FROM, MessageField.SENDER);

            // Determine the account identifier by sending address
            Address sendingAddress = originalDescription.getSender() != null ? originalDescription.getSender() : originalDescription.getFrom();
            int accountId = sendingAddress == null ? Account.DEFAULT_ID : resolveSender2Account(sendingAddress, session, false);

            Optional<MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage>> optTargetMailAccess;
            if (accountId == Account.DEFAULT_ID) {
                optTargetMailAccess = Optional.empty();
            } else {
                MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> otherAccess = mailService.getMailAccess(session, accountId);
                mailAccesses.put(I(accountId), otherAccess);
                otherAccess.connect(true);
                restorers.put(I(accountId), setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), otherAccess));
                optTargetMailAccess = Optional.of(otherAccess);
            }

            Security security = getSecurity(originalDescription);
            SecuritySettings securitySettings = getSecuritySettings(security);

            Meta meta = originalDescription.getMeta();
            Optional<MailMessage> optRefMessage = Optional.empty();
            if (meta != null) {
                MailPath referencedMessage = null;
                MetaType metaType = meta.getType();
                if (metaType == MetaType.REPLY || metaType == MetaType.REPLY_ALL) {
                    referencedMessage = meta.getReplyFor();
                } else if (metaType == MetaType.FORWARD_INLINE) {
                    referencedMessage = meta.getForwardsFor().get(0);
                }

                if (referencedMessage != null) {
                    try {
                        optRefMessage = Optional.of(getOriginalMail(session, referencedMessage, mailService, mailAccesses, restorers, defaultMailAccess, security.getAuthToken()));
                    } catch (OXException e) {
                        LOG.error("Cannot not apply reference headers because fetching the referenced message failed", e);
                    }
                }
            }

            MailPath newDraftPath = deleteAndSaveDraftMail(draftPath, processor, securitySettings, defaultMailAccess, DraftOptions.forFinalDraft(optRefMessage, optTargetMailAccess), session).getMailPath();
            processor.getFileCacheReference().ifPresent(r -> r.cleanUp());

            // Check for edit-draft --> Not needed since already dropped when opening composition space
            /*-
             *
            MailPath editFor = meta == null ? null : meta.getEditFor();
            if (editFor != null) {
                defaultMailAccess.getMessageStorage().deleteMessages(editFor.getFolder(), new String[] { editFor.getMailID() }, true);
            }
             *
             */

            return MailStorageResult.resultFor(null, newDraftPath, true, defaultMailAccess, processor);
        } finally {
            for (Optional<ReadResponseTimeoutRestorer> optRestorer : restorers.values()) {
                if (optRestorer.isPresent()) {
                    optRestorer.get().restore();
                }
            }
            for (MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess : mailAccesses.values()) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<MessageInfo> update(MailStorageId mailStorageId, MessageDescription newDescription, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            String authToken = newDescription.getSecurity() != null ? newDescription.getSecurity().getAuthToken() : null;
            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, mailAccess, authToken, clientToken);
            MailMessageProcessor processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            boolean changed = validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft();

            // Check for any difference
            if (!changed && originalDescription.seemsEqual(newDescription)) {
                MessageInfo messageInfo = new MessageInfo(originalDescription, processor.getOriginalSize(), processor.getDateHeader().orElse(null));
                return MailStorageResult.resultFor(mailStorageId, messageInfo, true, mailAccess, processor);
            }

            SecuritySettings securitySettings = prepareSecuritySettings(originalDescription, newDescription);
            processor.applyUpdate(newDescription);
            ApplySharedAttachmentsResult applyResult = applySharedAttachmentsChanges(originalDescription, newDescription, processor, session);

            if (ApplySharedAttachmentsResult.NOOP == applyResult) {
                Address sendingAddress = determineSendingAddress(originalDescription, newDescription);
                if (sendingAddress != null) {
                    // Sender address shall be changed
                    SharedAttachmentsInfo sharedAttachmentsInfo = getSharedAttachmentsInfo(originalDescription);
                    if (sharedAttachmentsInfo.isEnabled()) {
                        // Shared attachments already enabled
                        int accountId = resolveSender2Account(sendingAddress, session, false);
                        if (accountId != Account.DEFAULT_ID) {
                            throw CompositionSpaceErrorCode.NO_FROM_ADDRESS_FOR_NON_PRIMARY_ON_SHARED_ATTACHMENTS.create();
                        }
                    }
                }
            }

            if (newDescription.containsAttachments() && originalDescription.containsAttachments() && originalDescription.getAttachments() != null) {
                List<Attachment> attachments = newDescription.getAttachments();
                if (attachments != null) {
                    List<UUID> attachmentIds = null;
                    Set<UUID> keepAttachmentIds = attachments.isEmpty() ? Collections.emptySet() : attachments.stream().map(a -> a.getId()).collect(Collectors.toSet());
                    for (Attachment attachment : originalDescription.getAttachments()) {
                        if (keepAttachmentIds.contains(attachment.getId()) == false) {
                            if (attachmentIds == null) {
                                attachmentIds = new ArrayList<>();
                            }
                            attachmentIds.add(attachment.getId());
                        }
                    }
                    if (attachmentIds != null) {
                        processor.deleteAttachments(attachmentIds);
                    }
                }
            }

            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
            MailPath newDraftPath = newDraft.getMailPath();
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
            }
            MessageInfo messageInfo = new MessageInfo(processor.getCurrentDraft(), size, newDraft.getSentDate());
            DefaultMailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, messageInfo, true, mailAccess, processor);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<Boolean> delete(MailStorageId mailStorageId, boolean hardDelete, boolean deleteSharedAttachmentsFolderIfPresent, ClientToken clientToken, Session session) throws OXException {
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            tryCleanUpFileCacheReference(mailStorageId);

            MailMessage draftMail = requireDraftMail(mailStorageId, mailAccess, false);
            checkClientToken(clientToken, parseClientToken(draftMail));

            // In case message is a Drive Mail and associated attachments are supposed to be deleted, the message is required to be hard-deleted
            boolean hardDeleteMessage = hardDelete;
            if (deleteSharedAttachmentsFolderIfPresent) {
                String headerValue = HeaderUtility.decodeHeaderValue(draftMail.getFirstHeader(HeaderUtility.HEADER_X_OX_SHARED_ATTACHMENTS));
                SharedAttachmentsInfo sharedAttachmentsInfo = HeaderUtility.headerValue2SharedAttachments(headerValue);

                if (sharedAttachmentsInfo.isEnabled()) {
                    headerValue = HeaderUtility.decodeHeaderValue(draftMail.getFirstHeader(HeaderUtility.HEADER_X_OX_SHARED_FOLDER_REFERENCE));
                    SharedFolderReference sharedFolderRef = HeaderUtility.headerValue2SharedFolderReference(headerValue);

                    if (sharedFolderRef != null) {
                        AttachmentStorageRegistry attachmentStorageRegistry = services.getServiceSafe(AttachmentStorageRegistry.class);
                        AttachmentStorage attachmentStorage = attachmentStorageRegistry.getAttachmentStorageFor(session);
                        attachmentStorage.deleteFolder(sharedFolderRef.getFolderId(), ServerSessionAdapter.valueOf(session));

                        // Drive Mail cannot be moved to trash. Therefore:
                        hardDeleteMessage = true;
                    }
                }
            }

            IMailMessageStorageEnhancedDeletion enhancedDeletion = mailAccess.getMessageStorage().supports(IMailMessageStorageEnhancedDeletion.class);
            if (enhancedDeletion != null && enhancedDeletion.isEnhancedDeletionSupported()) {
                // Try to delete current draft mail in storage
                if (hardDeleteMessage) {
                    MailPath[] removedPaths = enhancedDeletion.hardDeleteMessages(draftPath.getFolder(), new String[] { draftPath.getMailID() });
                    Boolean deleted = Boolean.valueOf(removedPaths != null && removedPaths.length > 0 && draftPath.equals(removedPaths[0]));
                    return MailStorageResult.resultFor(mailStorageId, deleted, false, mailAccess);
                }

                MailPath[] movedPaths = enhancedDeletion.deleteMessagesEnhanced(draftPath.getFolder(), new String[] { draftPath.getMailID() }, false);
                if (movedPaths == null || movedPaths.length != 1) {
                    return MailStorageResult.resultFor(mailStorageId, Boolean.FALSE, false, mailAccess);
                }

                try {
                    MailPath trashed = movedPaths[0];
                    mailAccess.getMessageStorage().updateMessageFlags(trashed.getFolder(), new String[] { trashed.getMailID() }, MailMessage.FLAG_SEEN, true);
                } catch (Exception e) {
                    LOG.warn("Failed to set \\Seen flag on trashed draft message {} in folder {}", draftPath.getMailID(), draftPath.getFolder());
                }
                return MailStorageResult.resultFor(mailStorageId, Boolean.TRUE, false, mailAccess);
            }

            // Delete by best guess...
            mailAccess.getMessageStorage().deleteMessages(draftPath.getFolder(), new String[] { draftPath.getMailID() }, hardDeleteMessage);
            return MailStorageResult.resultFor(mailStorageId, Boolean.TRUE, false, mailAccess);
        } catch (MissingDraftException e) {
            return MailStorageResult.resultFor(mailStorageId, Boolean.FALSE, false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<NewAttachmentsInfo> addOriginalAttachments(MailStorageId mailStorageId, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Map<Integer, Optional<ReadResponseTimeoutRestorer>> restorers = new HashMap<>(2);
        Map<Integer, MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage>> mailAccesses = new HashMap<>(2);
        InputStream draftMimeStream = null;
        try {
            MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> defaultMailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccesses.put(I(Account.DEFAULT_ID), defaultMailAccess);
            defaultMailAccess.connect(false);
            restorers.put(I(Account.DEFAULT_ID), setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), defaultMailAccess));

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, defaultMailAccess, clientToken);
            MailMessageProcessor processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            boolean changed = validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft();

            Security security = getSecurity(originalDescription);

            // Acquire meta information and determine the "replyFor" path
            Meta meta = originalDescription.getMeta();
            MailPath replyFor = meta.getReplyFor();
            if (null == replyFor) {
                throw CompositionSpaceErrorCode.NO_REPLY_FOR.create();
            }

            MailMessage originalMail = getOriginalMail(session, replyFor, mailService, mailAccesses, restorers, defaultMailAccess, security.getAuthToken());
            List<Attachment> newAttachments = fetchOriginalAttachments(session, compositionSpaceId, processor, originalMail);

            List<Attachment> addedAttachments = processor.addAttachments(newAttachments);
            if (addedAttachments.isEmpty()) {
                // No attachments to add
                if (changed) {
                    SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());
                    MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, defaultMailAccess, DraftOptions.forIntermediateDraft(), session);
                    MailPath newDraftPath = newDraft.getMailPath();
                    long size = newDraft.getSize();
                    if (size < 0) {
                        size = fetchMailSize(defaultMailAccess.getMessageStorage(), newDraftPath);
                    }

                    NewAttachmentsInfo info = new NewAttachmentsInfo(Collections.emptyList(), originalDescription, size, newDraft.getSentDate());
                    DefaultMailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
                    return MailStorageResult.resultFor(newId, info, true, defaultMailAccess, processor);
                }

                NewAttachmentsInfo info = new NewAttachmentsInfo(Collections.emptyList(), originalDescription, processor.getOriginalSize(), processor.getDateHeader().orElse(null));
                DefaultMailStorageId newId = new DefaultMailStorageId(mailStorageId.getDraftPath(), compositionSpaceId, processor.getFileCacheReference());
                return MailStorageResult.resultFor(newId, info, true, defaultMailAccess, processor);
            }

            SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());

            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, defaultMailAccess, DraftOptions.forIntermediateDraft(), session);
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(defaultMailAccess.getMessageStorage(), draftPath);
            }
            NewAttachmentsInfo info = new NewAttachmentsInfo(getAttachmentIds(addedAttachments), processor.getCurrentDraft(), size, newDraft.getSentDate());
            DefaultMailStorageId newId = new DefaultMailStorageId(newDraft.getMailPath(), compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, info, true, defaultMailAccess, processor);
        } finally {
            Streams.close(draftMimeStream);
            for (Optional<ReadResponseTimeoutRestorer> optRestorer : restorers.values()) {
                if (optRestorer.isPresent()) {
                    optRestorer.get().restore();
                }
            }
            for (MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess : mailAccesses.values()) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<NewAttachmentsInfo> addVCardAttachment(MailStorageId mailStorageId, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        InputStream draftMimeStream = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, mailAccess, clientToken);
            MailMessageProcessor processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft();

            // Check by attachment origin
            for (Attachment existingAttachment : originalDescription.getAttachments()) {
                if (AttachmentOrigin.VCARD == existingAttachment.getOrigin()) {
                    // vCard already contained
                    NewAttachmentsInfo info = new NewAttachmentsInfo(getAttachmentIds(Collections.singletonList(existingAttachment)), originalDescription, processor.getOriginalSize(), processor.getDateHeader().orElse(null));
                    MailStorageId newId = new DefaultMailStorageId(mailStorageId.getDraftPath(), compositionSpaceId, processor.getFileCacheReference());
                    return MailStorageResult.resultFor(newId, info, true, mailAccess, processor);
                }
            }

            // Create vCard
            VCardAndFileName userVCard = CompositionSpaces.getUserVCard(session);

            // Check by file name
            Attachment existingVCardAttachment = null;
            for (Attachment existingAttachment : originalDescription.getAttachments()) {
                String fileName = existingAttachment.getName();
                if (fileName != null && fileName.equals(userVCard.getFileName())) {
                    // vCard already contained
                    existingVCardAttachment = existingAttachment;
                    break;
                }
            }

            // Create vCard attachment representation
            AttachmentDescription attachmentDesc = AttachmentStorages.createVCardAttachmentDescriptionFor(userVCard, compositionSpaceId, true);
            DefaultAttachment.Builder attachment = DefaultAttachment.builder(attachmentDesc);
            attachment.withDataProvider(new ByteArrayDataProvider(userVCard.getVcard()));

            // Either add or replace vCard attachment
            Attachment addedAttachment;
            if (existingVCardAttachment == null) {
                addedAttachment = processor.addAttachments(Collections.singletonList(attachment.build())).get(0);
            } else {
                attachment.withId(existingVCardAttachment.getId());
                addedAttachment = processor.replaceAttachment(attachment.build());
            }

            SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());
            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
            MailPath newDraftPath = newDraft.getMailPath();
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
            }
            NewAttachmentsInfo info = new NewAttachmentsInfo(getAttachmentIds(Collections.singletonList(addedAttachment)), processor.getCurrentDraft(), size, newDraft.getSentDate());
            MailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, info, true, mailAccess, processor);
        } finally {
            Streams.close(draftMimeStream);
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<NewAttachmentsInfo> addContactVCardAttachment(MailStorageId mailStorageId, String contactId, String folderId, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        InputStream draftMimeStream = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, mailAccess, clientToken);
            MailMessageProcessor processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft();

            // Create vCard
            VCardAndFileName contactVCard = CompositionSpaces.getContactVCard(contactId, folderId, session);

            // Create vCard attachment representation
            AttachmentDescription attachmentDesc = AttachmentStorages.createVCardAttachmentDescriptionFor(contactVCard, compositionSpaceId, false);
            DefaultAttachment.Builder attachment = DefaultAttachment.builder(attachmentDesc);
            attachment.withDataProvider(new ByteArrayDataProvider(contactVCard.getVcard()));
            Attachment vcardAttachment = attachment.build();

            // Either add or replace vCard attachment
            Attachment addedAttachment = processor.addAttachments(Collections.singletonList(vcardAttachment)).get(0);

            SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());
            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
            MailPath newDraftPath = newDraft.getMailPath();
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
            }
            NewAttachmentsInfo info = new NewAttachmentsInfo(getAttachmentIds(Collections.singletonList(addedAttachment)), processor.getCurrentDraft(), size, newDraft.getSentDate());
            MailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, info, true, mailAccess, processor);
        } finally {
            Streams.close(draftMimeStream);
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<NewAttachmentsInfo> addAttachments(MailStorageId mailStorageId, List<Attachment> attachments, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, mailAccess, clientToken);
            MailMessageProcessor processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft();

            SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());
            List<Attachment> addedAttachments = processor.addAttachments(attachments);

            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
            MailPath newDraftPath = newDraft.getMailPath();
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
            }

            NewAttachmentsInfo info = new NewAttachmentsInfo(getAttachmentIds(addedAttachments), processor.getCurrentDraft(), size, newDraft.getSentDate());
            DefaultMailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, info, true, mailAccess, processor);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<NewAttachmentsInfo> replaceAttachment(MailStorageId mailStorageId, Attachment attachment, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        MailMessageProcessor processor = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, mailAccess, clientToken);
            processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft();

            SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());
            Attachment addedAttachment = processor.replaceAttachment(attachment);

            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
            MailPath newDraftPath = newDraft.getMailPath();
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
            }

            NewAttachmentsInfo info = new NewAttachmentsInfo(getAttachmentIds(Collections.singletonList(addedAttachment)), processor.getCurrentDraft(), size, newDraft.getSentDate());
            DefaultMailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, info, true, mailAccess, processor);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }

            closeProcessorSafe(processor);
        }

    }

    @Override
    public MailStorageResult<Attachment> getAttachment(MailStorageId mailStorageId, UUID attachmentId, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), mailAccess);

            MailMessageProcessor processor = initMessageProcessorFromFileCache(mailStorageId, session, null, ClientToken.NONE);
            if (processor != null) {
                Attachment attachment = processor.getAttachment(attachmentId);
                return MailStorageResult.resultFor(mailStorageId, attachment, false, mailAccess);
            }

            MailMessage draftMail = requireDraftMail(mailStorageId, mailAccess);

            Attachment attachment = MailMessageProcessor.attachmentLookUp(attachmentId, compositionSpaceId, draftMail, session, services);
            DefaultMailStorageId newId = new DefaultMailStorageId(draftMail.getMailPath(), compositionSpaceId, mailStorageId.getFileCacheReference());
            return MailStorageResult.resultFor(newId, attachment, false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<MessageInfo> deleteAttachments(MailStorageId mailStorageId, List<UUID> attachmentIds, ClientToken clientToken, Session session) throws OXException, MissingDraftException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        InputStream draftMimeStream = null;
        MailMessageProcessor processor = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            ProcessorAndId processorAndId = initMessageProcessorFull(mailStorageId, session, mailAccess, clientToken);
            processor = processorAndId.processor;
            mailStorageId = processorAndId.id;
            draftPath = mailStorageId.getDraftPath();
            validateIfNeeded(mailStorageId, processor);
            MessageDescription originalDescription = processor.getCurrentDraft(MessageField.SECURITY);

            SecuritySettings securitySettings = getSecuritySettings(originalDescription.getSecurity());

            processor.deleteAttachments(attachmentIds);

            MailMessage newDraft = deleteAndSaveDraftMail(draftPath, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session);
            MailPath newDraftPath = newDraft.getMailPath();
            long size = newDraft.getSize();
            if (size < 0) {
                size = fetchMailSize(mailAccess.getMessageStorage(), newDraftPath);
            }

            DefaultMailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            MessageInfo messageInfo = new MessageInfo(processor.getCurrentDraft(), size, newDraft.getSentDate());
            return MailStorageResult.resultFor(newId, messageInfo, true, mailAccess, processor);
        } finally {
            Streams.close(draftMimeStream);
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }

            closeProcessorSafe(processor);
        }
    }

    @Override
    public MailStorageResult<Quota> getStorageQuota(Session session) throws OXException {
        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
            String draftsFolder = folderStorage.getDraftsFolder();
            Quota storageQuota = mailAccess.getFolderStorage().getStorageQuota(draftsFolder);
            return MailStorageResult.resultFor(null, storageQuota, false, mailAccess);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public MailStorageResult<Optional<MailPath>> validate(MailStorageId mailStorageId, Session session) throws OXException, MissingDraftException {
        // Currently this method only validates against shared attachments folder content

        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        MailPath draftPath = mailStorageId.getDraftPath();
        if (draftPath.getAccountId() != Account.DEFAULT_ID) {
            throw CompositionSpaceErrorCode.ERROR.create("Cannot operate on drafts outside of the default mail account!");
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        Optional<ReadResponseTimeoutRestorer> optRestorer = Optional.empty();
        MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = mailService.getMailAccess(session, Account.DEFAULT_ID);
            mailAccess.connect(false);
            optRestorer = setReadResponseTimeoutIfPossible(getDefaultReadResponseTimeout(), mailAccess);

            MailMessage draftMail = requireDraftMail(mailStorageId, mailAccess);

            SharedAttachmentsInfo sharedAttachmentsInfo = convertSharedAttachmentsInfo(draftMail);
            if (sharedAttachmentsInfo == null || sharedAttachmentsInfo.isDisabled()) {
                // Shared attachments not enabled
                return MailStorageResult.resultFor(null, Optional.empty(), true, mailAccess);
            }

            SharedFolderReference sharedFolderRef = convertSharedFolderReference(draftMail);
            if (sharedFolderRef == null) {
                // No shared attachments folder available
                return MailStorageResult.resultFor(null, Optional.empty(), true, mailAccess);
            }

            MailMessageProcessor processor = MailMessageProcessor.initForWrite(compositionSpaceId, draftMail, session, services);

            boolean changed = processor.validate();
            if (false == changed) {
                return MailStorageResult.resultFor(null, Optional.empty(), true, mailAccess, processor);
            }

            MessageDescription currentDraft = processor.getCurrentDraft();
            SecuritySettings securitySettings = getSecuritySettings(currentDraft.getSecurity());
            MailPath newDraftPath = deleteAndSaveDraftMail(draftMail, processor, securitySettings, mailAccess, DraftOptions.forIntermediateDraft(), session).getMailPath();
            MailStorageId newId = new DefaultMailStorageId(newDraftPath, compositionSpaceId, processor.getFileCacheReference());
            return MailStorageResult.resultFor(newId, Optional.of(newDraftPath), true, mailAccess, processor);
        } finally {
            if (optRestorer.isPresent()) {
                optRestorer.get().restore();
            }
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    private static final MailField[] MAIL_FIELDS_ID = new MailField[] { MailField.ID };

    private static Optional<MailPath> doLookUp(UUID compositionSpaceId, String draftsFolder, IMailMessageStorage messageStorage) throws OXException {
        SearchTerm<?> searchTerm = new HeaderTerm(HeaderUtility.HEADER_X_OX_COMPOSITION_SPACE_ID, UUIDs.getUnformattedString(compositionSpaceId));
        MailMessage[] mailMessages = messageStorage.searchMessages(draftsFolder, IndexRange.NULL, MailSortField.RECEIVED_DATE, OrderDirection.DESC, searchTerm, MAIL_FIELDS_ID);

        if (mailMessages == null || mailMessages.length == 0 || mailMessages[0] == null) {
            LOG.debug("Found no draft message for composition space {}", getUUIDForLogging(compositionSpaceId));
            return Optional.empty();
        }

        LOG.debug("Found draft message for composition space {}: {}", getUUIDForLogging(compositionSpaceId), mailMessages[0].getMailPath());
        return Optional.of(new MailPath(Account.DEFAULT_ID, draftsFolder, mailMessages[0].getMailId()));
    }

    private static boolean validateIfNeeded(MailStorageId mailStorageId, MailMessageProcessor processor) throws OXException {
        return (mailStorageId instanceof ValidateAwareMailStorageId) && ((ValidateAwareMailStorageId) mailStorageId).needsValidation() && processor.validate();
    }

    private long getMaxIdleTimeMillis(Session session) throws OXException {
        String defaultValue = "1W";

        ConfigViewFactory viewFactory = services.getOptionalService(ConfigViewFactory.class);
        if (null == viewFactory) {
            return ConfigTools.parseTimespan(defaultValue);
        }

        ConfigView view = viewFactory.getView(session.getUserId(), session.getContextId());
        return ConfigTools.parseTimespan(ConfigViews.getDefinedStringPropertyFrom("com.openexchange.mail.compose.maxIdleTimeMillis", defaultValue, view));
    }

    private static void closeProcessorSafe(MailMessageProcessor processor) {
        if (processor != null) {
            try {
                processor.close();
            } catch (Exception e) {
                LOG.warn("Failed to close mail message processor", e);
            }
        }
    }

    private static Security convertSecurity(MailMessage draftMail) {
        String headerValue = HeaderUtility.decodeHeaderValue(draftMail.getFirstHeader(HeaderUtility.HEADER_X_OX_SECURITY));
        return HeaderUtility.headerValue2Security(headerValue);
    }

    private static Security convertSecurity(HeaderCollection headers) {
        String headerValue = HeaderUtility.decodeHeaderValue(headers.getHeader(HeaderUtility.HEADER_X_OX_SECURITY, null));
        return HeaderUtility.headerValue2Security(headerValue);
    }

    private static SharedAttachmentsInfo convertSharedAttachmentsInfo(MailMessage draftMail) {
        String headerValue = HeaderUtility.decodeHeaderValue(draftMail.getFirstHeader(HeaderUtility.HEADER_X_OX_SHARED_ATTACHMENTS));
        return HeaderUtility.headerValue2SharedAttachments(headerValue);
    }

    private static SharedFolderReference convertSharedFolderReference(MailMessage draftMail) {
        String headerValue = HeaderUtility.decodeHeaderValue(draftMail.getFirstHeader(HeaderUtility.HEADER_X_OX_SHARED_FOLDER_REFERENCE));
        return HeaderUtility.headerValue2SharedFolderReference(headerValue);
    }

    private static MessageDescription toMessageDescription(MailMessage mailMessage, Set<MessageField> fields) {
        MessageDescription draftMessage = new MessageDescription();
        for (MessageField field : fields) {
            switch (field) {
                case ATTACHMENTS:
                    throw new UnsupportedOperationException();
                case BCC:
                    draftMessage.setBcc(MailMessageProcessor.convertAddresses(mailMessage.getBcc()));
                    break;
                case CC:
                    draftMessage.setCc(MailMessageProcessor.convertAddresses(mailMessage.getCc()));
                    break;
                case CONTENT:
                    //$FALL-THROUGH$
                case CONTENT_ENCRYPTED:
                    throw new UnsupportedOperationException();
                case CONTENT_TYPE: {
                        String headerValue = HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_CONTENT_TYPE));
                        ContentType contentType = ContentType.contentTypeFor(headerValue);
                        draftMessage.setContentType(contentType);
                    }
                    break;
                case CUSTOM_HEADERS:
                    Map<String, String> customHeaders = convertCustomHeaders(mailMessage);
                    if (customHeaders != null) {
                        draftMessage.setCustomHeaders(customHeaders);
                    }
                    break;
                case FROM:
                    draftMessage.setFrom(MailMessageProcessor.convertFirstAddress(mailMessage.getFrom()));
                    break;
                case META: {
                        String headerValue = HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_META));
                        Meta parsedMeta = HeaderUtility.headerValue2Meta(headerValue);
                        draftMessage.setMeta(parsedMeta);
                    }
                    break;
                case PRIORITY:
                    draftMessage.setPriority(convertPriority(mailMessage));
                    break;
                case REPLY_TO: {
                        String headerValue = HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_REPLY_TO));
                        List<Address> addresses = HeaderUtility.headerValue2Addresses(headerValue);
                        draftMessage.setReplyTo(addresses);
                    }
                    break;
                case REQUEST_READ_RECEIPT:
                    draftMessage.setRequestReadReceipt("true".equals(HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_READ_RECEIPT))));
                    break;
                case SECURITY: {
                        String headerValue = HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_SECURITY));
                        Security parsedSecurity = HeaderUtility.headerValue2Security(headerValue);
                        draftMessage.setSecurity(parsedSecurity);
                    }
                    break;
                case SENDER:
                    break;
                case SHARED_ATTACCHMENTS_INFO: {
                        String headerValue = HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_SHARED_ATTACHMENTS));
                        SharedAttachmentsInfo parsedSharedAttachments = HeaderUtility.headerValue2SharedAttachments(headerValue);
                        draftMessage.setSharedAttachmentsInfo(parsedSharedAttachments);
                    }
                    break;
                case SUBJECT:
                    draftMessage.setSubject(mailMessage.getSubject());
                    break;
                case TO:
                    draftMessage.setTo(MailMessageProcessor.convertAddresses(mailMessage.getTo()));
                    break;
                default:
                    break;
            }
        }
        return draftMessage;
    }

    private static Map<String, String> convertCustomHeaders(MailMessage mailMessage) {
        String headerValue = HeaderUtility.decodeHeaderValue(mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_CUSTOM_HEADERS));
        return HeaderUtility.headerValue2CustomHeaders(headerValue);
    }

    private static Priority convertPriority(MailMessage mailMessage) {
        Priority priority = null;
        String priorityStr = mailMessage.getFirstHeader(MessageHeaders.HDR_X_PRIORITY);
        if (Strings.isNotEmpty(priorityStr)) {
            try {
                int level = Integer.parseInt(priorityStr);
                priority = Priority.priorityForLevel(level);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (priority == null) {
            String importanceStr = mailMessage.getFirstHeader(MessageHeaders.HDR_IMPORTANCE);
            if (Strings.isNotEmpty(importanceStr)) {
                priority = Priority.priorityFor(importanceStr);
            }
        }

        return priority;
    }

    private static MailFields toMailFields(Set<MessageField> fields) {
        if (fields == null || fields.isEmpty()) {
            return new MailFields(MailField.FULL);
        }

        MailFields mailFields = new MailFields();
        for (MessageField messageField : fields) {
            switch (messageField) {
                case ATTACHMENTS:
                    return new MailFields(MailField.FULL);
                case BCC:
                    mailFields.add(MailField.BCC);
                    break;
                case CC:
                    mailFields.add(MailField.CC);
                    break;
                case CONTENT_ENCRYPTED:
                    // fall-through
                case CONTENT:
                    return new MailFields(MailField.FULL);
                case CONTENT_TYPE:
                    mailFields.add(MailField.HEADERS);
                    break;
                case CUSTOM_HEADERS:
                    mailFields.add(MailField.HEADERS);
                    break;
                case FROM:
                    mailFields.add(MailField.FROM);
                    break;
                case SENDER:
                    mailFields.add(MailField.SENDER);
                    break;
                case REPLY_TO:
                    mailFields.add(MailField.REPLY_TO);
                    break;
                case META:
                    mailFields.add(MailField.HEADERS);
                    break;
                case PRIORITY:
                    mailFields.add(MailField.HEADERS);
                    break;
                case REQUEST_READ_RECEIPT:
                    mailFields.add(MailField.HEADERS);
                    break;
                case SECURITY:
                    mailFields.add(MailField.HEADERS);
                    break;
                case SHARED_ATTACCHMENTS_INFO:
                    mailFields.add(MailField.HEADERS);
                    break;
                case SUBJECT:
                    mailFields.add(MailField.SUBJECT);
                    break;
                case TO:
                    mailFields.add(MailField.TO);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown message field: " + messageField);
            }
        }
        return mailFields;
    }

    private static final MailField[] MAIL_FIELDS_SIZE = new MailField[] { MailField.SIZE };

    private static long fetchMailSize(IMailMessageStorage draftMessageStorage, MailPath mailPath) {
        try {
            LOG.debug("Fetching mail size of draft {}", mailPath);
            MailMessage[] messages = draftMessageStorage.getMessages(mailPath.getFolder(), new String[] { mailPath.getMailID() }, MAIL_FIELDS_SIZE);
            if (messages != null && messages.length > 0 && messages[0] != null) {
                return messages[0].getSize();
            }

            LOG.warn("Could not fetch size of draft message due to empty response");
        } catch (OXException e) {
            LOG.warn("Error while fetching size of draft message", e);
        }

        return -1L;
    }

    private static List<UUID> getAttachmentIds(List<Attachment> attachments) {
        return attachments.stream().map(Attachment::getId).collect(toList());
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static HeadersAndStream parseHeaders(InputStream mimeStream) throws OXException {
        TrackingInputStream trackingInputStream = new TrackingInputStream(mimeStream);
        HeaderCollection hc = new HeaderCollection(trackingInputStream);
        return new HeadersAndStream(hc, new CombinedInputStream(trackingInputStream.getReadBytes(), mimeStream));
    }

    /**
     * Checks and adjusts security settings updates and returns the to-be-used
     * instance for next encryption attempt. Takes care of auth token changes to
     * be correctly applied.
     *
     * @param originalDescription {@link MessageDescription} from upstream mail message
     * @param newDescription {@link MessageDescription} from update request
     * @return The recent {@link SecuritySettings} to be used, can be <code>null</code>
     */
    private static SecuritySettings prepareSecuritySettings(MessageDescription originalDescription, MessageDescription newDescription) {
        if (originalDescription.containsNotNullSecurity() && newDescription.containsNotNullSecurity()) {
            Security newSecurity = null;
            if (newDescription.getSecurity().isEncrypt() && Strings.isEmpty(newDescription.getSecurity().getAuthToken())) {
                //we need to preserve the authentication token from the existing draft, if the caller wants us to encrypt but is missing an authToken
                //otherwise the token would get overwritten and de-cryption would fail the next time
                newSecurity = Security.builder(newDescription.getSecurity()).withAuthToken(originalDescription.getSecurity().getAuthToken()).build();
            } else if (newDescription.getSecurity().isEncrypt() == false && Strings.isNotEmpty(originalDescription.getSecurity().getAuthToken())) {
                //Remove the auth-token from the draft, because we don't need it anymore
                newSecurity = Security.builder(newDescription.getSecurity()).withAuthToken(null).build();
            }
            if (newSecurity != null) {
                newDescription.setSecurity(newSecurity);
            }
        }

        SecuritySettings securitySettings = getSecuritySettings(getSecurity(originalDescription));
        Security prevSecurity = getSecurity(originalDescription);
        if (newDescription.containsSecurity()) {
            Security newSecurity = getSecurity(newDescription);
            if (prevSecurity.isDisabled() != newSecurity.isDisabled() || !newSecurity.equals(prevSecurity)) {
                securitySettings = getSecuritySettings(newSecurity);
            }
        }

        return securitySettings;
    }

    /**
     * Fetches attachment parts from a given mail message and converts them into {@link Attachment} instances
     * that can be e.g. added to another message.
     */
    private static List<Attachment> fetchOriginalAttachments(Session session, UUID compositionSpaceId, MailMessageProcessor processor, MailMessage originalMail) throws OXException {
        if (originalMail.getContentType().startsWith("multipart/")) {
            // Grab first seen text from original message and check for possible referenced inline images
            List<String> contentIds = new ArrayList<String>();
            MimeProcessingUtility.getTextForForward(originalMail, true, false, contentIds, session);

            // Add mail's non-inline parts
            NonInlineForwardPartHandler handler = new NonInlineForwardPartHandler();
            if (false == contentIds.isEmpty()) {
                handler.setImageContentIds(contentIds);
            }

            new MailMessageParser().setInlineDetectorBehavior(true).parseMailMessage(originalMail, handler);

            List<MailPart> nonInlineParts = handler.getNonInlineParts();
            List<Attachment> newAttachments = new ArrayList<>(nonInlineParts.size());
            for (MailPart mailPart : nonInlineParts) {
                Attachment newAttachment;
                if (mailPart.containsContentDisposition() && mailPart.getContentDisposition().isInline()) {
                    newAttachment = processor.createNewInlineAttachmentFor(mailPart, compositionSpaceId, true);
                } else {
                    newAttachment = processor.createNewAttachmentFor(mailPart, compositionSpaceId, true);
                }
                newAttachments.add(newAttachment);
            }

            return newAttachments;
        }

        return Collections.emptyList();
    }

    private MailMessage getOriginalMail(Session session, MailPath mailPath, MailService mailService, Map<Integer, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage>> mailAccesses, Map<Integer, Optional<ReadResponseTimeoutRestorer>> restorers, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> defaultMailAccess, String authToken) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> access;
        if (mailPath.getAccountId() == Account.DEFAULT_ID) {
            access = defaultMailAccess;
        } else {
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> otherAccess = mailAccesses.get(I(mailPath.getAccountId()));
            if (otherAccess == null) {
                otherAccess = mailService.getMailAccess(session, mailPath.getAccountId());
                mailAccesses.put(I(mailPath.getAccountId()), otherAccess);
                otherAccess.connect(false);
                restorers.put(I(mailPath.getAccountId()), setReadResponseTimeoutIfPossible(getLongReadResponseTimeout(), otherAccess));
            }
            access = otherAccess;
        }
        Optional<MailMessage> optionalMail = getMail(mailPath.getMailID(), mailPath.getFolder(), access.getMessageStorage());
        if (optionalMail.isPresent() && mayDecrypt(session)) {
            CryptoMailRecognizerService optPgpRecognizer = services.getOptionalService(CryptoMailRecognizerService.class);
            if (optPgpRecognizer != null && !optPgpRecognizer.isCryptoMessage(optionalMail.get()) && !optPgpRecognizer.isSignedMessage(optionalMail.get())) {
                // Non-encrypted
                return optionalMail.get();
            }

            final CryptoType.PROTOCOL type = optPgpRecognizer == null ? CryptoType.PROTOCOL.PGP : optPgpRecognizer.getTypeCrypto(optionalMail.get());
            access = createCryptographicAwareAccess(access, authToken, type);
            optionalMail = getMail(mailPath.getMailID(), mailPath.getFolder(), access.getMessageStorage());
        }
        return optionalMail.orElseThrow(() -> MailExceptionCode.MAIL_NOT_FOUND.create(mailPath.getMailID(), mailPath.getFolderArgument()));
    }

    /**
     * Applies changes of the {@link SharedAttachmentsInfo} instance of the updated {@link MessageDescription} to the {@link MailMessageProcessor}.
     *
     * @param original The original message description
     * @param update The updated message description
     * @param processor The message processor
     * @param session The session
     * @return The result
     * @throws OXException
     */
    private static ApplySharedAttachmentsResult applySharedAttachmentsChanges(MessageDescription original, MessageDescription update, MailMessageProcessor processor, Session session) throws OXException {
        // Check if shared attachments feature has been enabled/disabled
        if (update.containsSharedAttachmentsInfo()) {
            SharedAttachmentsInfo prevSharedAttachmentsInfo = getSharedAttachmentsInfo(original);
            SharedAttachmentsInfo newSharedAttachmentsInfo = getSharedAttachmentsInfo(update);
            if (prevSharedAttachmentsInfo.isEnabled() != newSharedAttachmentsInfo.isEnabled()) {
                if (newSharedAttachmentsInfo.isEnabled()) {
                    // Shared attachments enabled.
                    if (false == processor.mayShareAttachments()) {
                        // User wants to share attachments, but is not allowed to do so
                        throw MailExceptionCode.SHARING_NOT_POSSIBLE.create(I(session.getUserId()), I(session.getContextId()));
                    }

                    // Primary account
                    Address sendingAddress = determineSendingAddress(original, update);
                    int accountId = sendingAddress == null ? Account.DEFAULT_ID : resolveSender2Account(sendingAddress, session, false);
                    if (accountId != Account.DEFAULT_ID) {
                        throw CompositionSpaceErrorCode.NO_SHARED_ATTACHMENTS_FOR_NON_PRIMARY.create();
                    }

                    // Save attachments into attachment storage.
                    processor.storeAttachments();
                    return ApplySharedAttachmentsResult.STORED;
                }

                // Shared attachments disabled
                processor.unstoreAttachments();
                return ApplySharedAttachmentsResult.UNSTORED;
            }
        }
        return ApplySharedAttachmentsResult.NOOP;
    }

    private static void tryCleanUpFileCacheReference(MailStorageId mailStorageId) {
        if (mailStorageId.hasFileCacheReference()) {
            try {
                mailStorageId.getFileCacheReference().get().cleanUp();
            } catch (Exception e) {
                LOG.error("Unable to clean-up sppol reference for composition space: {}", mailStorageId, e);
            }
        }
    }

    private MailMessage deleteAndSaveDraftMail(MailMessage draftMail, MailMessageProcessor processor, SecuritySettings securitySettings, MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> defaultMailAccess, DraftOptions draftOptions, Session session) throws OXException {
        return deleteAndSaveDraftMail(draftMail.getMailPath(), processor, securitySettings, defaultMailAccess, draftOptions, session);
    }

    private MailMessage deleteAndSaveDraftMail(MailPath draftPath, MailMessageProcessor processor, SecuritySettings securitySettings, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> defaultMailAccess, DraftOptions draftOptions, Session session) throws OXException {
        // Retrieve quota
        Quota storageQuota;
        if (draftOptions.asFinalDraft && draftOptions.optTargetMailAccess.isPresent()) {
            IMailFolderStorage folderStorage = draftOptions.optTargetMailAccess.get().getFolderStorage();
            storageQuota = folderStorage.getQuotas(folderStorage.getDraftsFolder(), new Quota.Type[] { Quota.Type.STORAGE })[0];
        } else {
            storageQuota = defaultMailAccess.getFolderStorage().getQuotas(draftPath.getFolder(), new Quota.Type[] { Quota.Type.STORAGE })[0];
        }

        if (storageQuota.getLimit() == 0) {
            // Not possible due to quota restrictions
            throw MailExceptionCode.UNABLE_TO_SAVE_DRAFT_QUOTA.create();
        }

        // Create the new draft mail
        ComposedMailMessage newDraftMail = draftOptions.asFinalDraft ? processor.compileFinalDraft(draftOptions.optRefMessage) : processor.compileDraft();

        newDraftMail = applyGuardEncryption(securitySettings, newDraftMail, session);
        return deleteAndSaveDraftMailSafe(draftPath, storageQuota, draftOptions.asFinalDraft ? draftOptions.optTargetMailAccess : Optional.empty(), defaultMailAccess.getMessageStorage(), newDraftMail);
    }

    /**
     * Checks available quota for enough space for a full copy followed by writing the new draft message. Only on success the former draft is deleted.
     *
     * @param draftPath The path to the current draft mail
     * @param storageQuota The storage quota providing limit and usage in bytes
     * @param optTargetMailAccess The optional storage to save to (or <code>null</code>)
     * @param defaultMessageStorage The default message storage to use
     * @param newDraftMail The new draft mail to store
     * @return The new draft message
     * @throws OXException If deleting old and storing new draft mail fails
     */
    private static MailMessage deleteAndSaveDraftMailSafe(MailPath draftPath, Quota storageQuota, Optional<MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage>> optTargetMailAccess, IMailMessageStorage defaultMessageStorage, ComposedMailMessage newDraftMail) throws OXException {
        // Check against quota limit
        if (storageQuota.getLimitBytes() > 0) {
            checkAvailableQuota(storageQuota, new NewSizeSupplierCallable(newDraftMail, LOG));
        }

        // Prepare new draft mail accordingly.
        newDraftMail.setSendType(ComposeType.DRAFT);

        // Save new draft mail (and thus delete previous draft mail)
        MailMessage savedDraft;
        if (optTargetMailAccess.isPresent()) {
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> targetMailAccess = optTargetMailAccess.get();
            savedDraft = saveDraftMail(newDraftMail, targetMailAccess.getFolderStorage().getDraftsFolder(), true, targetMailAccess.getMessageStorage());
            savedDraft.setAccountId(targetMailAccess.getAccountId());
        } else {
            savedDraft = saveDraftMail(newDraftMail, draftPath.getFolder(), true, defaultMessageStorage);
        }

        // Delete with conflict detection in case enhanced deletion is supported
        boolean deleteFailed = true;
        try {
            IMailMessageStorageEnhancedDeletion enhancedDeletion = defaultMessageStorage.supports(IMailMessageStorageEnhancedDeletion.class);
            if (enhancedDeletion == null || !enhancedDeletion.isEnhancedDeletionSupported()) {
                // Delete by best guess...
                LOG.debug("Deleting old draft {}", draftPath);
                defaultMessageStorage.deleteMessages(draftPath.getFolder(), new String[] { draftPath.getMailID() }, true);
            } else {
                // Try to delete current draft mail in storage
                LOG.debug("Hard-deleting old draft {}", draftPath);
                MailPath[] removedPaths = enhancedDeletion.hardDeleteMessages(draftPath.getFolder(), new String[] { draftPath.getMailID() });
                if (removedPaths == null || removedPaths.length <= 0 || !draftPath.equals(removedPaths[0])) {
                    LOG.warn("Another process deleted draft mail '{}' in the meantime", draftPath);
                }
            }

            // Return new draft path
            deleteFailed = false;
            return savedDraft;
        } finally {
            if (deleteFailed) {
                MailPath newDraftPath = savedDraft.getMailPath();
                LOG.debug("Delete of {} failed => deleting newly saved draft {} again", draftPath, newDraftPath);
                defaultMessageStorage.deleteMessages(newDraftPath.getFolder(), new String[] { newDraftPath.getMailID() }, true);
            }
        }
    }

    /**
     * Checks if given additional bytes fit into current quota
     *
     * @param storageQuota The storage quota known to have a limitation greater than <code>0</code> (zero)
     * @param newSizeSupplier Provides the number of bytes to store
     * @throws {@link MailExceptionCode#UNABLE_TO_SAVE_DRAFT_QUOTA} in case quota would be exceeded
     */
    private static void checkAvailableQuota(Quota storageQuota, Callable<Long> newSizeSupplier) throws OXException {
        if (!MailStorageCompositionSpaceConfig.getInstance().isEagerUploadChecksEnabled()) {
            LOG.debug("Skipping eager quota checks because they are disabled");
            return;
        }

        try {
            long newSize = newSizeSupplier.call().longValue();
            if (newSize > 0 && storageQuota.getUsageBytes() + newSize > storageQuota.getLimitBytes()) {
                // Not possible due to quota restrictions
                LOG.debug("Would exceed storage quota by {} bytes", L((storageQuota.getUsageBytes() + newSize) - storageQuota.getLimitBytes()));
                throw MailExceptionCode.UNABLE_TO_SAVE_DRAFT_QUOTA.create();
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw CompositionSpaceErrorCode.ERROR.create(e, e.getMessage());
        }
    }

    private ProcessorAndId initMessageProcessorFull(MailStorageId mailStorageId, Session session, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, ClientToken clientToken) throws OXException, MissingDraftException {
        return initMessageProcessorFull(mailStorageId, session, mailAccess, null, clientToken);
    }

    private ProcessorAndId initMessageProcessorFull(MailStorageId mailStorageId, Session session, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, String authToken, ClientToken clientToken) throws OXException, MissingDraftException {
        MailMessageProcessor processor = initMessageProcessorFromFileCache(mailStorageId, session, authToken, clientToken);
        if (processor == null) {
            InputStream mimeStream = null;
            try {
                try {
                    mimeStream = requireDraftMimeStream(mailStorageId, mailAccess, authToken);
                } catch (MissingDraftException x) {
                    // No such draft for cached draft identifier. Look-up by composition space identifier
                    Optional<MailPath> optionalDraftPath = doLookUp(mailStorageId.getCompositionSpaceId(), mailStorageId.getDraftPath().getFolder(), mailAccess.getMessageStorage());
                    if (optionalDraftPath.isPresent() == false) {
                        // No remedy
                        LOG.debug("Found no draft mail for composition space identifier: {}", CompositionSpaces.getUUIDForLogging(mailStorageId.getCompositionSpaceId()));
                        throw x;
                    }
                    mailStorageId = new DefaultMailStorageId(optionalDraftPath.get(), mailStorageId.getCompositionSpaceId(), mailStorageId.getFileCacheReference());
                    mimeStream = requireDraftMimeStream(mailStorageId, mailAccess, authToken);
                }
                processor = MailMessageProcessor.initForWrite(mailStorageId.getCompositionSpaceId(), mimeStream, session, services);
                checkClientToken(clientToken, processor.getClientToken());

                LOG.debug("Initialized message processor for composition space from fetched MIME stream: {}", mailStorageId);
            } finally {
                Streams.close(mimeStream);
            }
        }

        return new ProcessorAndId(processor, mailStorageId);
    }

    /**
     * Initializes a {@link MailMessageProcessor} from the file cache reference of given {@link MailStorageId}.
     *
     * @param mailStorageId The mail storage ID
     * @param session The user session
     * @param authToken The optionally new Guard auth token
     * @return The processor or <code>null</code> if file cache reference is missing or invalid
     * @throws OXException If initialization fails for other reasons than a missing/invalid file cache reference
     */
    private MailMessageProcessor initMessageProcessorFromFileCache(MailStorageId mailStorageId, Session session, String authToken, ClientToken clientToken) throws OXException {
        UUID compositionSpaceId = mailStorageId.getCompositionSpaceId();
        if (mailStorageId.hasValidFileCacheReference()) {
            CacheReference cacheReference = mailStorageId.getFileCacheReference().get();
            MailMessageProcessor processor = null;
            try {
                processor = MailMessageProcessor.initFromFileCache(compositionSpaceId, cacheReference, session, services);
                if (clientToken.isPresent() && clientToken.isNotEquals(processor.getClientToken())) {
                    LOG.debug("Client token mismatch for cached message. Expected: '{}' but was '{}'. Clearing cache to retry.", processor.getClientToken(), clientToken);
                    // force re-fetch to properly detect concurrent modification
                    cacheReference.cleanUp();
                    return null;
                }

                LOG.debug("Initialized message processor for composition space from file cache reference: {}", mailStorageId);
                return processor;
            } catch (OXException e) {
                if (CompositionSpaceErrorCode.IO_ERROR.equals(e)) {
                    LOG.debug("File cache reference for composition space is not readable: {}", mailStorageId, e);
                } else {
                    LOG.error("Failed to initialize message processor from file cache reference: {}", mailStorageId, e);
                    throw e;
                }
            }
        }

        return null;
    }

    private InputStream requireDraftMimeStream(MailStorageId mailStorageId, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, String authToken) throws OXException, MissingDraftException {
        MailPath draftPath = mailStorageId.getDraftPath();
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccezz = mailAccess;
        IMailMessageStorage messageStorage = mailAccezz.getMessageStorage();

        Optional<InputStream> optionalMimeStream = getMimeStream(draftPath, messageStorage);
        if (optionalMimeStream.isPresent()) {
            InputStream mimeStream = optionalMimeStream.get();
            try {
                HeadersAndStream parsedHeaders = parseHeaders(mimeStream);
                Security security = convertSecurity(parsedHeaders.headers);
                if (!security.isEncrypt()) {
                    mimeStream = null; // Avoid premature closing
                    return parsedHeaders.mimeStream;
                }

                Streams.close(mimeStream);
                mimeStream = null;

                String authTokenToUse = authToken;
                if (authTokenToUse == null) {
                    authTokenToUse = security.getAuthToken();
                }

                mailAccezz = createCryptographicAwareAccess(mailAccezz, authTokenToUse, CryptoType.getTypeFromString(security.getType()));
                messageStorage = mailAccezz.getMessageStorage();

                optionalMimeStream = getMimeStream(draftPath, messageStorage);
                if (optionalMimeStream.isPresent()) {
                    return optionalMimeStream.get();
                }
            } finally {
                Streams.close(mimeStream);
            }
        }

        throw new MissingDraftException(mailStorageId);
    }

    private static Optional<InputStream> getMimeStream(MailPath mailPath, IMailMessageStorage messageStorage) throws OXException {
        return getMimeStream(mailPath.getMailID(), mailPath.getFolder(), messageStorage);
    }

    private static Optional<InputStream> getMimeStream(String mailId, String fullName, IMailMessageStorage messageStorage) throws OXException {
        try {
            InputStream in = null;
            IMailMessageStorageMimeSupport mimeSupport = messageStorage.supports(IMailMessageStorageMimeSupport.class);
            if (mimeSupport != null && mimeSupport.isMimeSupported()) {
                in = mimeSupport.getMimeStream(fullName, mailId);
            } else {
                MailMessage mail = messageStorage.getMessage(fullName, mailId, false);
                if (mail != null) {
                    in = MimeMessageUtility.getStreamFromMailPart(mail);
                }
            }

            if (in == null) {
                LOG.debug("Failed to fetch full MIME stream of draft {}/{} because mail does not exist anymore", fullName, mailId);
            } else {
                LOG.debug("Fetched full MIME stream of draft {}/{}", fullName, mailId);
            }
            return Optional.ofNullable(in);
        } catch (OXException e) {
            if (MailExceptionCode.MAIL_NOT_FOUND.equals(e) || ExceptionUtils.isEitherOf(e, javax.mail.MessageRemovedException.class, com.sun.mail.util.MessageRemovedIOException.class)) {
                LOG.debug("Failed to fetch full MIME stream of draft {}/{} because mail does not exist anymore", fullName, mailId);
                return Optional.empty();
            }

            LOG.warn("Failed to fetch full MIME stream of draft {}/{}", fullName, mailId, e);
            throw e;
        }
    }

    /**
     * Gets a {@link MailMessage}. Tries to decrypt if required.
     *
     * @param mailStorageId The mail storage id
     * @param mailAccess The {@link MailAccess} to use
     * @return The {@link MailMessage}
     * @throws OXException
     * @throws MissingDraftException
     */
    private MailMessage requireDraftMail(MailStorageId mailStorageId, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException, MissingDraftException {
        return requireDraftMail(mailStorageId, mailAccess, true);
    }

    /**
     * Gets a {@link MailMessage}
     *
     * @param mailStorageId The mail storage id
     * @param mailAccess The {@link MailAccess} to use
     * @param decryptIfRequired <code>True</code> in order to decrypt the message if required, <code>False</code> to return the raw PGP message in case it is encrypted.
     * @return The {@link MailMessage}
     * @throws OXException
     * @throws MissingDraftException
     */
    private MailMessage requireDraftMail(MailStorageId mailStorageId, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, boolean decryptIfRequired) throws OXException, MissingDraftException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccezz = mailAccess;
        IMailMessageStorage messageStorage = mailAccezz.getMessageStorage();

        MailPath draftPath = mailStorageId.getDraftPath();

        Optional<MailMessage> optionalDraftMail = getMail(draftPath, messageStorage);
        if (optionalDraftMail.isPresent()) {
            MailMessage mailMessage = optionalDraftMail.get();

            Security security = convertSecurity(mailMessage);
            if (!decryptIfRequired || !security.isEncrypt()) {
                return mailMessage;
            }

            mailAccezz = createCryptographicAwareAccess(mailAccezz, security.getAuthToken(), CryptoType.getTypeFromString(security.getType()));
            messageStorage = mailAccezz.getMessageStorage();

            optionalDraftMail = getMail(draftPath, messageStorage);
            if (optionalDraftMail.isPresent()) {
                return optionalDraftMail.get();
            }
        }

        throw new MissingDraftException(mailStorageId);
    }

    private static Optional<MailMessage> getMail(MailPath mailPath, IMailMessageStorage messageStorage) throws OXException {
        return getMail(mailPath.getMailID(), mailPath.getFolder(), messageStorage);
    }

    private static Optional<MailMessage> getMail(String mailId, String fullName, IMailMessageStorage messageStorage) throws OXException {
        try {
            MailMessage mail = messageStorage.getMessage(fullName, mailId, false);
            if (mail == null) {
                LOG.debug("Failed to fetch full draft {}/{} because mail does not exist anymore", fullName, mailId);
            } else {
                LOG.debug("Fetched full draft {}/{}", fullName, mailId);
            }
            return Optional.ofNullable(mail);
        } catch (OXException e) {
            if (MailExceptionCode.MAIL_NOT_FOUND.equals(e) || ExceptionUtils.isEitherOf(e, javax.mail.MessageRemovedException.class, com.sun.mail.util.MessageRemovedIOException.class)) {
                LOG.debug("Failed to fetch full draft {}/{} because mail does not exist anymore", fullName, mailId);
                return Optional.empty();
            }

            LOG.warn("Failed to fetch full draft {}/{}", fullName, mailId, e);
            throw e;
        }
    }

    private static MailMessage saveDraftMail(ComposedMailMessage newDraftMail, String draftFullName, boolean markAsSeen, IMailMessageStorage messageStorage) throws OXException {
        MailMessage savedDraft;
        try {
            savedDraft = messageStorage.saveDraft(draftFullName, newDraftMail);
            LOG.debug("Saved new draft as {} with {}: {}", savedDraft.getMailPath(), HeaderUtility.HEADER_X_OX_COMPOSITION_SPACE_ID, toStringObjectFor(() -> savedDraft.getFirstHeader(HeaderUtility.HEADER_X_OX_COMPOSITION_SPACE_ID)));
        } catch (OXException e) {
            LOG.debug("Failed to save new draft", e);
            throw e;
        } catch (Exception e) {
            LOG.debug("Failed to save new draft", e);
            throw CompositionSpaceErrorCode.ERROR.create(e, e.getMessage());
        }

        if (markAsSeen) {
            try {
                messageStorage.updateMessageFlags(draftFullName, new String[] { savedDraft.getMailId() }, MailMessage.FLAG_SEEN, true);
                LOG.debug("Marked new draft {} as seen", savedDraft.getMailPath());
            } catch (Exception e) {
                LOG.debug("Failed to mark new draft {} as seen", savedDraft.getMailPath(), e);
            }
        }

        return savedDraft;
    }

    private static int resolveSender2Account(Address sendingAddress, Session session, boolean forTransport) throws OXException {
        try {
            String personal = sendingAddress.getPersonal();
            InternetAddress fromAddresss = Strings.isEmpty(personal) ? new QuotedInternetAddress(sendingAddress.getAddress()) : new QuotedInternetAddress(sendingAddress.getAddress(), personal, "UTF-8");
            return MimeMessageFiller.resolveSender2Account(ServerSessionAdapter.valueOf(session), fromAddresss, forTransport, true);
        } catch (OXException e) {
            if (MailExceptionCode.NO_TRANSPORT_SUPPORT.equals(e) || MailExceptionCode.INVALID_SENDER.equals(e) || MailAccountExceptionCodes.EXTERNAL_ACCOUNTS_DISABLED.equals(e)) {
                // Re-throw
                throw e;
            }
            // Save to default account's transport provider
            LOG.warn("{}. Using default account's transport.", e.getMessage());
            return Account.DEFAULT_ID;
        } catch (Exception e) {
            // Save to default account's transport provider
            LOG.warn("{}. Using default account's transport.", e.getMessage());
            return Account.DEFAULT_ID;
        }
    }

    private static Address determineSendingAddress(MessageDescription originalDescription, MessageDescription newDescription) {
        if (newDescription.containsSender()) {
            // Sender is either set or dropped
            if (newDescription.getSender() != null) {
                // Sender set
                return newDescription.getSender();
            }

            // Sender dropped
            if (newDescription.containsFrom()) {
                if (newDescription.getFrom() != null) {
                    // From set
                    return newDescription.getFrom();
                }

                // Both - sender and from address dropped. Use previous ones as fall-back
                return originalDescription.getSender() != null ? originalDescription.getSender() : originalDescription.getFrom();
            }
        }

        if (newDescription.containsFrom()) {
            // From is either set or dropped
            if (newDescription.getFrom() != null) {
                // From set
                return originalDescription.getSender() != null ? originalDescription.getSender() : newDescription.getFrom();
            }

            // From address dropped. Use previous ones as fall-back
        }

        return originalDescription.getSender() != null ? originalDescription.getSender() : originalDescription.getFrom();
    }

    // --------------------------------------------------------- Guard stuff ---------------------------------------------------------------

    private MailAccess<IMailFolderStorage, IMailMessageStorage> createCryptographicAwareAccess(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess, String authToken, CryptoType.PROTOCOL type) throws OXException {
        CryptographicAwareMailAccessFactory cryptoMailAccessFactory = services.getServiceSafe(CryptographicAwareMailAccessFactory.class);
        String authTokenToUse = getAuthenticationToken(authToken, mailAccess.getSession());
        return cryptoMailAccessFactory.createAccess((MailAccess<IMailFolderStorage, IMailMessageStorage>) mailAccess, mailAccess.getSession(), authTokenToUse, type);
    }

    private static final String CAPABILITY_GUARD = "guard";
    private static final String CAPABILITY_SMIME = "smime";

    /**
     * Applies given security settings to specified mail message.
     *
     * @param securitySettings The security settings to apply
     * @param mailMessage The mail message to apply to
     * @param session The session providing user data
     * @return The security-wise prepared mail message in case security settings are enabled; otherwise given mail message is returned as-is
     * @throws OXException If applying security settings fails
     */
    private ComposedMailMessage applyGuardEncryption(SecuritySettings securitySettings, ComposedMailMessage mailMessage, Session session) throws OXException {
        if (securitySettings == null || !securitySettings.isEncrypt()) {
            return mailMessage;
        }

        if (false == mayDecrypt(session)) {
            throw OXException.noPermissionForModule(CAPABILITY_GUARD);
        }

        EncryptedMailService encryptor = services.getServiceSafe(EncryptedMailService.class);
        mailMessage.setSecuritySettings(securitySettings);
        return encryptor.encryptDraftEmail(mailMessage, session, null /* encryption does not require an auth-token */);
    }

    /**
     * Private method to pull the security settings from given arguments.
     *
     * @param security The security options
     * @param session The session
     * @return The security settings if any present and set, otherwise <code>null</code>
     */
    private static SecuritySettings getSecuritySettings(Security security) {
        if (null != security && false == security.isDisabled()) {
            SecuritySettings settings = SecuritySettings.builder()
                .encrypt(security.isEncrypt())
                .pgpInline(security.isPgpInline())
                .sign(security.isSign())
                .guestLanguage(security.getLanguage())
                .guestMessage(security.getMessage())
                .pin(security.getPin())
                .msgRef(security.getMsgRef())
                .type(security.getType())
                .build();
            if (settings.anythingSet()) {
                return settings;
            }
        }
        return null;
    }

    /**
     * Gets the full authentication token for a given client token
     *
     * @param authToken The given client token
     * @param session The session
     * @return The full authentication token ready to be used with OX Guard, or null if the given authToken is null
     * @throws OXException
     */
    private String getAuthenticationToken(String authToken, Session session) throws OXException {
        if (authToken != null) {
            CryptographicServiceAuthenticationFactory authFactory = services.getServiceSafe(CryptographicServiceAuthenticationFactory.class);
            return authFactory.createAuthenticationFrom(session, authToken, null);
        }
        return null;
    }

    /**
     * Checks if session-associated user is allowed to decrypt guard mails.
     *
     * @param session The session
     * @return <code>true</code> if allowed; otherwise <code>false</code>
     * @throws OXException If check fails
     */
    private boolean mayDecrypt(Session session) throws OXException {
        CapabilityService capabilityService = services.getOptionalService(CapabilityService.class);
        if (capabilityService == null) {
            return false;
        }
        CapabilitySet capabilities = capabilityService.getCapabilities(session);
        return capabilities.contains(CAPABILITY_GUARD) || capabilities.contains(CAPABILITY_SMIME);
    }

    private static SharedAttachmentsInfo getSharedAttachmentsInfo(MessageDescription draftMessage) {
        SharedAttachmentsInfo sharedAttachmentsInfo = draftMessage.getSharedAttachmentsInfo();
        return sharedAttachmentsInfo == null ? SharedAttachmentsInfo.DISABLED : sharedAttachmentsInfo;
    }

    private static Security getSecurity(MessageDescription draftMessage) {
        Security security = draftMessage.getSecurity();
        return security == null ? Security.DISABLED : security;
    }

    /**
     * Checks the client token contained in current request against the actual one currently assigned to the
     * composition space.
     *
     * @param requestToken The token sent by client to perform the current operation
     * @param actualToken The actual token assigned to composition space
     * @throws OXException {@link CompositionSpaceErrorCode#CONCURRENT_UPDATE} if request token is present but does not
     *         match the actual one
     */
    private static void checkClientToken(ClientToken requestToken, ClientToken actualToken) throws OXException {
        if (requestToken.isPresent() && requestToken.isNotEquals(actualToken)) {
            LOG.info("Client token mismatch. Expected: '{}' but was '{}'", actualToken, requestToken);
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }
    }

    /**
     * Parses the client token from given draft mails headers
     *
     * @param draftMail The draft mail
     * @return The token
     */
    private static ClientToken parseClientToken(MailMessage draftMail) {
        ClientToken clientToken = ClientToken.NONE;
        String clientTokenValue = null;
        try {
            clientTokenValue = HeaderUtility.decodeHeaderValue(draftMail.getFirstHeader(HeaderUtility.HEADER_X_OX_CLIENT_TOKEN));
            clientToken = ClientToken.of(clientTokenValue);
            if (clientToken == ClientToken.NONE) {
                LOG.warn("Draft mail contains invalid client token: {}", clientTokenValue);
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Draft mail contains invalid client token: {}", clientTokenValue);
        }

        return clientToken;
    }

    private static Optional<UUID> parseCompositionSpaceId(MailMessage mailMessage) {
        String headerValue = null;
        try {
            headerValue = mailMessage.getFirstHeader(HeaderUtility.HEADER_X_OX_COMPOSITION_SPACE_ID);
            return Optional.of(UUIDs.fromUnformattedString(headerValue));
        } catch (IllegalArgumentException e) {
            LOG.info("Ignoring mail {} with invalid composition space ID: {}", mailMessage.getMailPath(), headerValue);
        }

        return Optional.empty();
    }

    private static Optional<ReadResponseTimeoutRestorer> setReadResponseTimeoutIfPossible(long readResponseTimeout, MailAccess<? extends IMailFolderStorage,? extends IMailMessageStorage> mailAccess) {
        if (mailAccess instanceof IMailStoreAware storeAware) {
            try {
                Store store = storeAware.getStore();
                if (store.isSetAndGetReadResponseTimeoutSupported()) {
                    return Optional.of(store.setAndGetReadResponseTimeout((int) readResponseTimeout));
                }
            } catch (Exception e) {
                LOG.warn("Failed to set read response timeout", e);
            }
        }
        return Optional.empty();
    }

    private static long getDefaultReadResponseTimeout() throws OXException {
        return MailStorageCompositionSpaceConfig.getInstance().getDefaultReadResponseTimeout();
    }

    private static long getLongReadResponseTimeout() throws OXException {
        return MailStorageCompositionSpaceConfig.getInstance().getLongReadResponseTimeout();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class HeadersAndStream {

        final HeaderCollection headers;
        final InputStream mimeStream;

        HeadersAndStream(HeaderCollection headers, InputStream mimeStream) {
            super();
            this.headers = headers;
            this.mimeStream = mimeStream;
        }
    }

    private static class DraftOptions {

        private static final DraftOptions INTERMEDIATE_DRAFT = new DraftOptions(false, Optional.empty(), Optional.empty());

        static DraftOptions forIntermediateDraft() {
            return DraftOptions.INTERMEDIATE_DRAFT;
        }

        static DraftOptions forFinalDraft(Optional<MailMessage> optRefMessage, Optional<MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage>> optTargetMailAccess) {
            return new DraftOptions(true, optRefMessage, optTargetMailAccess);
        }

        // ---------------------------------------------------------------------------------------------

        final boolean asFinalDraft;
        final Optional<MailMessage> optRefMessage;
        final Optional<MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage>> optTargetMailAccess;

        private DraftOptions(boolean asFinalDraft, Optional<MailMessage> optRefMessage, Optional<MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage>> optTargetMailAccess) {
            super();
            this.asFinalDraft = asFinalDraft;
            this.optRefMessage = optRefMessage;
            this.optTargetMailAccess = optTargetMailAccess;
        }
    }

    private static enum ApplySharedAttachmentsResult {
        NOOP, STORED, UNSTORED;
    }

    private static class NewSizeSupplierCallable implements Callable<Long> {

        private final ComposedMailMessage newDraftMail;
        private final Logger logger;

        NewSizeSupplierCallable(ComposedMailMessage newDraftMail, org.slf4j.Logger logger) {
            super();
            this.newDraftMail = newDraftMail;
            this.logger = logger;
        }

        @Override
        public Long call() throws OXException {
            return Long.valueOf(determineSize());
        }

        private long determineSize() throws OXException {
            logger.debug("Determinining size of new draft by counting");
            CountingOutputStream out = null;
            try {
                out = new CountingOutputStream();
                ((MimeMessage) newDraftMail.getContent()).writeTo(out);
                out.flush();
                return out.getCount();
            } catch (IOException e) {
                throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
            } catch (MessagingException e) {
                throw MimeMailException.handleMessagingException(e);
            } finally {
                Streams.close(out);
            }
        }
    }

    private static class ProcessorAndId {

        final MailMessageProcessor processor;
        final MailStorageId id;

        ProcessorAndId(MailMessageProcessor processor, MailStorageId id) {
            super();
            this.processor = processor;
            this.id = id;
        }
    }

}
