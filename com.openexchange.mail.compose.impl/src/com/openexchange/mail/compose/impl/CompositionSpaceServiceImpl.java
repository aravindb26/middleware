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

package com.openexchange.mail.compose.impl;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.util.UUIDs.getUnformattedString;
import static com.openexchange.java.util.UUIDs.getUnformattedStringObjectFor;
import static com.openexchange.mail.compose.AttachmentResults.attachmentResultFor;
import static com.openexchange.mail.text.TextProcessing.performLineFolding;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.crypto.CryptographicServiceAuthenticationFactory;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.deputy.ActiveDeputyPermission;
import com.openexchange.deputy.DeputyService;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCodeSet;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.groupware.upload.StreamedUploadFile;
import com.openexchange.groupware.upload.StreamedUploadFileIterator;
import com.openexchange.html.HtmlService;
import com.openexchange.java.Functions;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.compose.Address;
import com.openexchange.mail.compose.Attachment;
import com.openexchange.mail.compose.AttachmentComparator;
import com.openexchange.mail.compose.AttachmentDataSource;
import com.openexchange.mail.compose.AttachmentDescription;
import com.openexchange.mail.compose.AttachmentDescriptionAndData;
import com.openexchange.mail.compose.AttachmentOrigin;
import com.openexchange.mail.compose.AttachmentResult;
import com.openexchange.mail.compose.AttachmentStorage;
import com.openexchange.mail.compose.AttachmentStorageService;
import com.openexchange.mail.compose.AttachmentStorages;
import com.openexchange.mail.compose.ClientToken;
import com.openexchange.mail.compose.CompositionSpace;
import com.openexchange.mail.compose.CompositionSpaceDescription;
import com.openexchange.mail.compose.CompositionSpaceErrorCode;
import com.openexchange.mail.compose.CompositionSpaceService;
import com.openexchange.mail.compose.CompositionSpaceStorageService;
import com.openexchange.mail.compose.CompositionSpaces;
import com.openexchange.mail.compose.ContentId;
import com.openexchange.mail.compose.CryptoUtility;
import com.openexchange.mail.compose.DeleteAfterTransportOptions;
import com.openexchange.mail.compose.HeaderUtility;
import com.openexchange.mail.compose.Message;
import com.openexchange.mail.compose.Message.Priority;
import com.openexchange.mail.compose.MessageDescription;
import com.openexchange.mail.compose.MessageField;
import com.openexchange.mail.compose.Meta;
import com.openexchange.mail.compose.Meta.MetaType;
import com.openexchange.mail.compose.OpenCompositionSpaceParameters;
import com.openexchange.mail.compose.Security;
import com.openexchange.mail.compose.SharedAttachmentsInfo;
import com.openexchange.mail.compose.Type;
import com.openexchange.mail.compose.UploadLimits;
import com.openexchange.mail.compose.VCardAndFileName;
import com.openexchange.mail.compose.impl.attachment.AttachmentImageDataSource;
import com.openexchange.mail.compose.impl.open.EditCopy;
import com.openexchange.mail.compose.impl.open.Forward;
import com.openexchange.mail.compose.impl.open.OpenState;
import com.openexchange.mail.compose.impl.open.Reply;
import com.openexchange.mail.compose.impl.open.Resend;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.SecuritySettings;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.ContentAwareComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.TextBodyMailPart;
import com.openexchange.mail.json.compose.ComposeHandler;
import com.openexchange.mail.json.compose.ComposeHandlerRegistry;
import com.openexchange.mail.json.compose.ComposeRequest;
import com.openexchange.mail.json.compose.ComposeTransportResult;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.filler.MimeMessageFiller;
import com.openexchange.mail.mime.processing.MimeProcessingUtility;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.NonInlineForwardPartHandler;
import com.openexchange.mail.service.EncryptedMailService;
import com.openexchange.mail.transport.MtaStatusInfo;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.TransportProviderRegistry;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.ContactCollectorUtility;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.MailAccounts;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link CompositionSpaceServiceImpl} - The composition space service implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class CompositionSpaceServiceImpl implements CompositionSpaceService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CompositionSpaceServiceImpl.class);

    private static final com.openexchange.mail.compose.Message.ContentType TEXT_PLAIN = com.openexchange.mail.compose.Message.ContentType.TEXT_PLAIN;

    private static final com.openexchange.mail.compose.Attachment.ContentDisposition INLINE = com.openexchange.mail.compose.Attachment.ContentDisposition.INLINE;

    private final ServiceLookup services;
    private final CompositionSpaceStorageService storageService;
    private final AttachmentStorageService attachmentStorageService;
    private final Session session;
    private volatile Set<String> octetExtensions;

    /**
     * Initializes a new {@link CompositionSpaceServiceImpl}.
     *
     * @param session The session for which the service is created
     * @param storageService The storage service
     * @param attachmentStorageService The attachment storage service
     * @param services The service look-up
     */
    public CompositionSpaceServiceImpl(Session session, CompositionSpaceStorageService storageService, AttachmentStorageService attachmentStorageService, ServiceLookup services) {
        super();
        this.session = session;
        if (null == storageService) {
            throw new IllegalArgumentException("Storage service must not be null");
        }
        if (null == attachmentStorageService) {
            throw new IllegalArgumentException("Attachment storage service must not be null");
        }
        if (null == services) {
            throw new IllegalArgumentException("Service registry must not be null");
        }
        this.storageService = storageService;
        this.attachmentStorageService = attachmentStorageService;
        this.services = services;
    }

    private CompositionSpaceStorageService getStorageService() {
        return storageService;
    }

    /**
     * Gets the attachment storage for given session.
     *
     * @return The composition space service
     * @throws OXException If composition space service cannot be returned
     */
    private AttachmentStorage getAttachmentStorage(Session session) throws OXException {
        return attachmentStorageService.getAttachmentStorageFor(session);
    }

    private Set<String> octetExtensions() {
        Set<String> tmp = octetExtensions;
        if (null == tmp) {
            synchronized (CompositionSpaceServiceImpl.class) {
                tmp = octetExtensions;
                if (null == tmp) {
                    String defaultValue = "pgp";
                    ConfigurationService service = services.getService(ConfigurationService.class);
                    if (null == service) {
                        return new HashSet<String>(Arrays.asList(defaultValue));
                    }
                    String csv = service.getProperty("com.openexchange.mail.octetExtensions", defaultValue);
                    tmp = new HashSet<String>(Arrays.asList(Strings.splitByComma(csv)));
                    octetExtensions = tmp;
                }
            }
        }
        return tmp;
    }

    private Context getContext() throws OXException {
        return session instanceof ServerSession ? ((ServerSession) session).getContext() : services.getServiceSafe(ContextService.class).getContext(session.getContextId());
    }

    @Override
    public Collection<OXException> getWarnings() {
        return Collections.emptyList();
    }

    private static final OXExceptionCodeSet CODES_COPY_TO_SENT_FOLDER_FAILED = new OXExceptionCodeSet(MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA, MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED);

    @Override
    public MailPath transportCompositionSpace(UUID compositionSpaceId, Optional<StreamedUploadFileIterator> optionalUploadedAttachments, UserSettingMail mailSettings, AJAXRequestData request, List<OXException> warnings, DeleteAfterTransportOptions deleteAfterTransportOptions, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CONCURRENT_UPDATE.create();
        }

        Message m = compositionSpace.getMessage();
        if (null == m) {
            return null;
        }

        // Check if attachments are supposed to be shared
        SharedAttachmentsInfo sharedAttachmentsInfo = m.getSharedAttachments();
        if (null != sharedAttachmentsInfo && sharedAttachmentsInfo.isEnabled() && false == mayShareAttachments(session)) {
            // User wants to share attachments, but is not allowed to do so
            throw MailExceptionCode.SHARING_NOT_POSSIBLE.create(I(session.getUserId()), I(session.getContextId()));
        }

        // Yield server session
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);

        // Check From address
        InternetAddress fromAddresss;
        {
            Address from = m.getFrom();
            if (null == from) {
                throw MailExceptionCode.MISSING_FIELD.create(MailJSONField.FROM.getKey());
            }
            fromAddresss = toMimeAddress(from);
        }

        // Optional sender
        InternetAddress senderAddress = null;
        {
            Address sender = m.getSender();
            if (null != sender) {
                senderAddress = toMimeAddress(sender);
            }
        }

        // Determine the account identifier by From address
        int accountId;
        try {
            accountId = MimeMessageFiller.resolveSender2Account(serverSession, senderAddress != null ? senderAddress : fromAddresss, true, true);
        } catch (OXException e) {
            accountId = checkTransportException(e);
        }

        // Check sender/from validity
        if (senderAddress != null && !fromAddresss.equals(senderAddress)) {
            if (accountId != Account.DEFAULT_ID && MailAccounts.isSecondaryAccount(accountId, session) == false) {
                // "on behalf of" only allowed for primary/secondary account
                throw MailExceptionCode.INVALID_SENDER.create(fromAddresss.toUnicodeString());
            }

            // Check "From" address for "send-on-behalf-of" permission
            User grantingUser = getUserByAddress(fromAddresss);
            if (grantingUser == null) {
                // No such user for given "From" address
                throw MailExceptionCode.INVALID_SENDER.create(fromAddresss.toUnicodeString());
            }

            DeputyService deputyService = services.getOptionalService(DeputyService.class);
            if (deputyService != null) {
                // If there is a deputy service, check if "send on behalf of" is explicitly NOT granted
                List<ActiveDeputyPermission> permissions = deputyService.listReverseDeputyPermissions(grantingUser.getId(), session);
                if (permissions.isEmpty() == false) {
                    /*
                     * There is at least one deputy permission for session-associated user granted by from-associated user. Thus
                     * "send on behalf of" is required to be granted.
                     */
                    boolean sendOnBehalfOf = false;
                    for (Iterator<ActiveDeputyPermission> it = permissions.iterator(); sendOnBehalfOf == false && it.hasNext();) {
                        if (it.next().isSendOnBehalfOf()) {
                            sendOnBehalfOf = true;
                        }
                    }
                    if (sendOnBehalfOf == false) {
                        // User did not grant "send-on-behalf-of" permission
                        throw MailExceptionCode.NO_ON_BEHALF_OF_PERMISSION.create(I(grantingUser.getId()), fromAddresss.toUnicodeString(), I(session.getUserId()), senderAddress.toUnicodeString(), I(session.getContextId()));
                    }
                }
            }
        }

        // Prepare text content
        List<Attachment> attachments = m.getAttachments();
        String content = m.getContent();
        if (null == content) {
            LOG.warn("Missing content in composition space {}. Using empty text instead.", getUnformattedString(compositionSpaceId));
            content = "";
        }
        if (m.getContentType().isImpliesHtml()) {
            // An HTML message...
            if ((attachments != null && !attachments.isEmpty())) {
                // ... with attachments
                Map<UUID, Attachment> fileAttachments = new LinkedHashMap<>();
                for (Attachment attachment : attachments) {
                    fileAttachments.put(attachment.getId(), attachment);
                }

                if (Strings.isNotEmpty(content)) {
                    // Replace image URLs with src="cid:1234"
                    int numOfAttachments = fileAttachments.size();
                    Map<ContentId, Attachment> contentId2InlineAttachment = new HashMap<>(numOfAttachments);
                    Map<String, Attachment> attachmentId2inlineAttachments = new HashMap<>(numOfAttachments);

                    for (Attachment attachment : fileAttachments.values()) {
                        if (INLINE == attachment.getContentDisposition() && null != attachment.getContentIdAsObject() && new ContentType(attachment.getMimeType()).startsWith("image/")) {
                            attachmentId2inlineAttachments.put(getUnformattedString(attachment.getId()), attachment);
                            contentId2InlineAttachment.put(attachment.getContentIdAsObject(), attachment);
                        }
                    }
                    content = CompositionSpaces.replaceLinkedInlineImages(content, attachmentId2inlineAttachments, contentId2InlineAttachment, fileAttachments, AttachmentImageDataSource.getInstance());
                }
            }
        }

        // Create a new compose message
        TransportProvider provider = TransportProviderRegistry.getTransportProviderBySession(serverSession, accountId);
        ComposedMailMessage sourceMessage = provider.getNewComposedMailMessage(serverSession, serverSession.getContext());
        sourceMessage.setAccountId(accountId);

        // From
        sourceMessage.addFrom(fromAddresss);

        // Sender
        if (senderAddress != null) {
            sourceMessage.addSender(senderAddress);
        }

        // Reply-To
        {
            List<Address> replyTo = m.getReplyTo();
            if (null != replyTo) {
                sourceMessage.addReplyTo(toMimeAddresses(replyTo));
            }
        }

        // Recipients
        {
            boolean anyRecipientSet = false;
            List<Address> to = m.getTo();
            if (null != to) {
                sourceMessage.addTo(toMimeAddresses(to));
                anyRecipientSet = true;
            }

            List<Address> cc = m.getCc();
            if (null != cc) {
                sourceMessage.addCc(toMimeAddresses(cc));
                anyRecipientSet = true;
            }

            List<Address> bcc = m.getBcc();
            if (null != bcc) {
                sourceMessage.addBcc(toMimeAddresses(bcc));
                anyRecipientSet = true;
            }

            if (false == anyRecipientSet) {
                throw MailExceptionCode.MISSING_FIELD.create("To");
            }
        }

        // Subject
        {
            String subject = m.getSubject();
            if (null != subject) {
                sourceMessage.setSubject(subject, true);
            }
        }

        // Sent date
        sourceMessage.setSentDate(new Date());

        // Read receipt
        if (m.isRequestReadReceipt()) {
            sourceMessage.setDispositionNotification(toMimeAddress(m.getFrom()));
        }

        // Priority
        {
            Priority priority = m.getPriority();
            sourceMessage.setHeader(MessageHeaders.HDR_X_PRIORITY, String.valueOf(priority.getLevel()));
            if (Priority.NORMAL == priority) {
                sourceMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "Normal");
            } else if (Priority.LOW == priority) {
                sourceMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "Low");
            } else {
                sourceMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "High");
            }
        }

        // Security
        {
            SecuritySettings securitySettings = getSecuritySettings (m, request);
            if (securitySettings != null) {
                sourceMessage.setSecuritySettings(securitySettings);
            }
        }

        // Custom headers
        {
            Map<String, String> customHeaders = m.getCustomHeaders();
            if (customHeaders != null) {
                for (Map.Entry<String, String> customHeader : customHeaders.entrySet()) {
                    String headerName = customHeader.getKey();
                    if (MimeMessageFiller.isCustomOrReplyHeader(headerName)) {
                        sourceMessage.setHeader(headerName, customHeader.getValue());
                    }
                }
            }
        }

        // Create a new text part instance
        TextBodyMailPart textPart = provider.getNewTextBodyPart(content);
        textPart.setContentType(m.getContentType().getId());
        if (TEXT_PLAIN == m.getContentType()) {
            textPart.setPlainText(content);
        }

        // Apply content type to compose message as well
        sourceMessage.setContentType(textPart.getContentType());
        // sourceMessage.setBodyPart(textPart); --> Happens in 'c.o.mail.json.compose.AbstractComposeHandler.doCreateTransportResult()'

        // Check for shared attachments
        Map<String, Object> params = Collections.emptyMap();
        if (null != sharedAttachmentsInfo && sharedAttachmentsInfo.isEnabled()) {
            ImmutableMap.Builder<String, Object> parameters = ImmutableMap.builder();

            try {
                JSONObject jShareAttachmentOptions = new JSONObject(6);
                jShareAttachmentOptions.put("enable", sharedAttachmentsInfo.isEnabled());
                jShareAttachmentOptions.put("autodelete", sharedAttachmentsInfo.isAutoDelete());
                String password = sharedAttachmentsInfo.getPassword();
                if (null != password) {
                    jShareAttachmentOptions.put("password", password);
                }
                Date expiryDate = sharedAttachmentsInfo.getExpiryDate();
                if (null != expiryDate) {
                    jShareAttachmentOptions.put("expiry_date", expiryDate.getTime());
                }

                parameters.put("share_attachments", jShareAttachmentOptions);
            } catch (JSONException e) {
                throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
            }

            params = parameters.build();
        }


        List<CloseableMailPartWrapper> closeableParts = null;
        MailPath sentMailPath = null;
        OXException sendFailed = null;
        MailServletInterface mailInterface = null;
        ComposeTransportResult transportResult = null;
        try {
            boolean newMessageId = AJAXRequestDataTools.parseBoolParameter(AJAXServlet.ACTION_NEW, request);

            // Collect parts
            closeableParts = getMailPartsFromAttachments(compositionSpaceId, optionalUploadedAttachments, attachments);
            List<MailPart> parts = closeableParts.stream().map(c -> c.getMailPart()).collect(Collectors.toList());

            // Create compose request to process
            ComposeRequest composeRequest = new ComposeRequest(accountId, sourceMessage, textPart, parts, params, request, warnings);

            // Determine appropriate compose handler
            ComposeHandlerRegistry handlerRegistry = services.getService(ComposeHandlerRegistry.class);
            ComposeHandler composeHandler = handlerRegistry.getComposeHandlerFor(composeRequest);

            // As new/transport message
            transportResult = composeHandler.createTransportResult(composeRequest);
            List<? extends ComposedMailMessage> composedMails = transportResult.getTransportMessages();
            ComposedMailMessage sentMessage = transportResult.getSentMessage();
            boolean transportEqualToSent = transportResult.isTransportEqualToSent();

            if (newMessageId) {
                for (ComposedMailMessage composedMail : composedMails) {
                    if (null != composedMail) {
                        composedMail.removeHeader("Message-ID");
                        composedMail.removeMessageId();
                    }
                }
                sentMessage.removeHeader("Message-ID");
                sentMessage.removeMessageId();
            }

            for (ComposedMailMessage cm : composedMails) {
                if (null != cm) {
                    cm.setSendType(ComposeType.NEW);
                }
            }

            // User settings
            UserSettingMail usm = serverSession.getUserSettingMail();
            usm.setNoSave(true);
            {
                final String paramName = "copy2Sent";
                String sCopy2Sent = request.getParameter(paramName);
                if (null != sCopy2Sent) { // Provided as URL parameter
                    if (AJAXRequestDataTools.parseBoolParameter(sCopy2Sent)) {
                        usm.setNoCopyIntoStandardSentFolder(false);
                    } else if (Boolean.FALSE.equals(AJAXRequestDataTools.parseFalseBoolParameter(sCopy2Sent))) {
                        // Explicitly deny copy to sent folder
                        usm.setNoCopyIntoStandardSentFolder(true);
                    }
                } else {
                    MailAccountStorageService mass = services.getOptionalService(MailAccountStorageService.class);
                    if (mass != null && MailAccounts.isGmailTransport(mass.getTransportAccount(accountId, serverSession.getUserId(), serverSession.getContextId()))) {
                        // Deny copy to sent folder for Gmail
                        usm.setNoCopyIntoStandardSentFolder(true);
                    }
                }
            }
            checkAndApplyLineWrapAfter(request, usm);
            for (ComposedMailMessage cm : composedMails) {
                if (null != cm) {
                    cm.setMailSettings(usm);
                }
            }
            if (null != sentMessage) {
                sentMessage.setMailSettings(usm);
            }

            mailInterface = MailServletInterface.getInstance(serverSession);

            // Reply or (inline) forward?
            Meta meta = m.getMeta();
            Optional<MailMessage> originalMail = null;
            {
                MetaType metaType = meta.getType();
                if (metaType == MetaType.REPLY || metaType == MetaType.REPLY_ALL) {
                    MailPath replyFor = meta.getReplyFor();
                    originalMail = optionalMailMessage(replyFor, mailInterface);
                    if (originalMail.isPresent()) {
                        setReplyHeaders(originalMail.get(), sourceMessage, I(MailProperties.getInstance().getMaxLengthForReferencesHeader(serverSession.getUserId(), serverSession.getContextId())));
                    } else {
                        warnings.add(MailExceptionCode.ORIGINAL_MAIL_NOT_FOUND.create(replyFor.getMailID(), replyFor.getFolder()));
                    }
                } else if (metaType == MetaType.FORWARD_INLINE) {
                    MailPath forwardFor = meta.getForwardsFor().get(0);
                    originalMail = optionalMailMessage(forwardFor, mailInterface);
                    if (originalMail.isPresent()) {
                        setReplyHeaders(originalMail.get(), sourceMessage);
                    } else {
                        warnings.add(MailExceptionCode.ORIGINAL_MAIL_NOT_FOUND.create(forwardFor.getMailID(), forwardFor.getFolder()));
                    }
                }
            }

            // Do the transport...
            try {
                HttpServletRequest servletRequest = request.optHttpServletRequest();
                String remoteAddress = null == servletRequest ? request.getRemoteAddress() : servletRequest.getRemoteAddr();
                List<String> ids = mailInterface.sendMessages(composedMails, sentMessage, transportEqualToSent, ComposeType.NEW, accountId, usm, new MtaStatusInfo(), remoteAddress);
                if (null != ids && !ids.isEmpty()) {
                    String msgIdentifier = ids.get(0);
                    try {
                        sentMailPath = MailPath.getMailPathFor(msgIdentifier);
                    } catch (Exception x) {
                        LOG.warn("Failed to parse mail path from {}", msgIdentifier, x);
                    }
                }
            } catch (OXException oxe) {
                if (!CODES_COPY_TO_SENT_FOLDER_FAILED.contains(oxe)) {
                    // Re-throw...
                    throw oxe;
                }
                sendFailed = oxe;
            }

            // Commit results as actual transport was executed
            {
                transportResult.commit();
                transportResult.finish();
                transportResult = null;
            }

            // Check if original mails needs to be marked or removed
            {
                MetaType metaType = meta.getType();
                if (metaType == MetaType.REPLY || metaType == MetaType.REPLY_ALL) {
                    if (originalMail == null || originalMail.isPresent()) {
                        MailPath replyFor = meta.getReplyFor();
                        try {
                            mailInterface.updateMessageFlags(replyFor.getFolderArgument(), new String[] { replyFor.getMailID() }, MailMessage.FLAG_ANSWERED, null, true);
                        } catch (Exception e) {
                            LOG.warn("Failed to mark original mail '{}' as answered", replyFor, e);
                            warnings.add(MailExceptionCode.FLAG_FAIL.create());
                        }
                    }
                } else if (metaType == MetaType.FORWARD_INLINE) {
                    if (originalMail == null || originalMail.isPresent()) {
                        MailPath forwardFor = meta.getForwardsFor().get(0);
                        try {
                            mailInterface.updateMessageFlags(forwardFor.getFolderArgument(), new String[] { forwardFor.getMailID() }, MailMessage.FLAG_FORWARDED, null, true);
                        } catch (Exception e) {
                            LOG.warn("Failed to mark original mail '{}' as forwarded", forwardFor, e);
                            warnings.add(MailExceptionCode.FLAG_FAIL.create());
                        }
                    }
                } else if (metaType == MetaType.FORWARD_ATTACHMENT) {
                    List<MailPath> forwardsFor = meta.getForwardsFor();
                    if (forwardsFor != null) {
                        forwardsFor = forwardsFor.stream().filter(Objects::nonNull).collect(Collectors.toList());
                        if (!forwardsFor.isEmpty()) {
                            Map<String, List<String>> groupedByFolder = new HashMap<>();
                            for (MailPath forwardFor : forwardsFor) {
                                groupedByFolder.computeIfAbsent(forwardFor.getFolderArgument(), Functions.getNewArrayListFuntion()).add(forwardFor.getMailID());
                            }
                            try {
                                for (Map.Entry<String, List<String>> e : groupedByFolder.entrySet()) {
                                    List<String> mailIds = e.getValue();
                                    mailInterface.updateMessageFlags(e.getKey(), mailIds.toArray(new String[mailIds.size()]), MailMessage.FLAG_FORWARDED, null, true);
                                }
                            } catch (Exception e) {
                                LOG.warn("Failed to mark original mails '{}' as forwarded", forwardsFor, e);
                                warnings.add(MailExceptionCode.FLAG_FAIL.create());
                            }
                        }
                    }
                }
                MailPath editFor = meta.getEditFor();
                if (null != editFor && (deleteAfterTransportOptions.isDeleteDraftAfterTransport() || MailProperties.getInstance().isDeleteDraftOnTransport(serverSession.getUserId(), serverSession.getContextId()))) {
                    try {
                        mailInterface.deleteMessages(editFor.getFolderArgument(), new String[] { editFor.getMailID() }, true);
                    } catch (Exception e) {
                        LOG.warn("Failed to delete edited draft mail '{}'", editFor, e);
                    }
                }
            }

            warnings.addAll(mailInterface.getWarnings());

            // Trigger contact collector
            try {
                boolean memorizeAddresses = ServerUserSetting.getInstance().isContactCollectOnMailTransport(serverSession.getContextId(), serverSession.getUserId()).booleanValue();
                ContactCollectorUtility.triggerContactCollector(serverSession, composedMails, memorizeAddresses, true);
            } catch (Exception e) {
                LOG.warn("Contact collector could not be triggered.", e);
            }

        } finally {
            Streams.close(closeableParts);

            if (transportResult != null) {
                transportResult.rollback();
                transportResult.finish();
                transportResult = null;
            }
            if (null != mailInterface) {
                mailInterface.close();
            }

        }

        if (deleteAfterTransportOptions.isDeleteAfterTransport()) {
            try {
                boolean closed = removeCompositionSpaceFromStorage(compositionSpaceId);
                if (closed) {
                    LOG.debug("Closed composition space '{}' after transport", getUnformattedStringObjectFor(compositionSpaceId));
                } else {
                    LOG.warn("Compositon space {} could not be closed after transport.", getUnformattedString(compositionSpaceId));
                }
            } catch (OXException e) {
                LOG.warn("Failed to close composition space {} after being transported", getUnformattedString(compositionSpaceId), e);
            }
        }

        if (sendFailed != null) {
            throw sendFailed;
        }

        return sentMailPath;
    }

    /**
     * Checks if the exception must be throw or if a fallback to the default account is possible
     *
     * @param e The error to check
     * @return The default account
     * @throws OXException
     */
    private int checkTransportException(OXException e) throws OXException {
        if (MailExceptionCode.NO_TRANSPORT_SUPPORT.equals(e) || // @formatter:off
            MailExceptionCode.INVALID_SENDER.equals(e) ||
            MailAccountExceptionCodes.EXTERNAL_ACCOUNTS_DISABLED.equals(e)) { // @formatter:on
            // Re-throw
            throw e;
        }
        LOG.warn("{}. Using default account's transport.", e.getMessage());
        // Send with default account's transport provider
        return Account.DEFAULT_ID;
    }

    private User getUserByAddress(InternetAddress fromAddresss) throws OXException {
        try {
            UserService userService = services.getServiceSafe(UserService.class);
            return userService.searchUser(fromAddresss.getAddress(), getContext(), true, false, false);
        } catch (OXException e) {
            if (LdapExceptionCode.NO_USER_BY_MAIL.equals(e)) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Converts stored and uploaded attachments into {@link MailPart} instances. After processing all attachments,
     * callers must close the returned instances.
     *
     * @param compositionSpaceId
     * @param optionalUploadedAttachments
     * @param attachments
     * @return A list of closeable wrapper instances.
     * @throws OXException
     */
    private List<CloseableMailPartWrapper> getMailPartsFromAttachments(UUID compositionSpaceId, Optional<StreamedUploadFileIterator> optionalUploadedAttachments, List<Attachment> attachments) throws OXException {
        List<CloseableMailPartWrapper> parts;
        if ((attachments != null && !attachments.isEmpty()) || optionalUploadedAttachments.isPresent()) {
            parts = new ArrayList<>();
            if (attachments != null) {
                for (Attachment attachment : attachments) {
                    parts.add(new CloseableMailPartWrapper(new AttachmentMailPart(attachment)));
                }
            }
            if (optionalUploadedAttachments.isPresent()) {
                StreamedUploadFileIterator uploadedAttachments = optionalUploadedAttachments.get();
                if (uploadedAttachments.hasNext()) {
                    List<ThresholdFileHolder> fileHolders = new LinkedList<>();
                    try {
                        do {
                            StreamedUploadFile uploadFile = uploadedAttachments.next();
                            ThresholdFileHolder fileHolder = new ThresholdFileHolder();
                            fileHolders.add(fileHolder);
                            fileHolder.write(uploadFile.getStream());

                            AttachmentDescription attachmentDescription = AttachmentStorages.createUploadFileAttachmentDescriptionFor(uploadFile, com.openexchange.mail.compose.Attachment.ContentDisposition.ATTACHMENT.getId(), compositionSpaceId);
                            parts.add(new CloseableMailPartWrapper(new ThresholdFileHolderMailPart(attachmentDescription, fileHolder)));
                        } while (uploadedAttachments.hasNext());
                        // prevent premature closing of file holders
                        fileHolders = null;
                    } catch (IOException e) {
                        throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
                    } finally {
                        Streams.close(fileHolders);
                    }
                }
            }
        } else {
            parts = Collections.emptyList();
        }
        return parts;
    }

    private static final class CloseableMailPartWrapper implements Closeable {
        private final MailPart mailPart;
        private final ThresholdFileHolder fileHolder;

        CloseableMailPartWrapper(MailPart mailPart) {
            this(mailPart, null);
        }

        CloseableMailPartWrapper(MailPart mailPart, ThresholdFileHolder fileHolder) {
            this.mailPart = mailPart;
            this.fileHolder = fileHolder;
        }

        public MailPart getMailPart() {
            return mailPart;
        }

        @Override
        public void close() throws IOException {
            if (fileHolder != null) {
                fileHolder.close();
            }
        }
    }

    /**
     * Private method to pull the security settings from given message.
     *
     * @param m The message from which to pull security settings
     * @param optRequest The optional AJAX request if authentication should be generated, may be <code>null</code>
     * @return The security settings if any present and set, otherwise <code>null</code>
     * @throws OXException If security settings cannot be returned
     */
    private SecuritySettings getSecuritySettings (Message m, AJAXRequestData optRequest) throws OXException {
        Security security = m.getSecurity();
        if (null != security && false == security.isDisabled()) {
            String authentication = null;
            if (optRequest != null) {
                CryptographicServiceAuthenticationFactory authenticationFactory = services.getOptionalService(CryptographicServiceAuthenticationFactory.class);
                if (authenticationFactory != null) {
                    authentication = authenticationFactory.createAuthenticationFrom(optRequest);
                }
            }

            SecuritySettings settings = SecuritySettings.builder()
                .encrypt(security.isEncrypt())
                .pgpInline(security.isPgpInline())
                .sign(security.isSign())
                .authentication(authentication)
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

    private void checkAndApplyLineWrapAfter(AJAXRequestData request, UserSettingMail usm) throws OXException {
        String paramName = "lineWrapAfter";
        if (request.containsParameter(paramName)) { // Provided as URL parameter
            String sLineWrapAfter = request.getParameter(paramName);
            if (null != sLineWrapAfter) {
                try {
                    int lineWrapAfter = Integer.parseInt(sLineWrapAfter.trim());
                    usm.setAutoLinebreak(lineWrapAfter <= 0 ? 0 : lineWrapAfter);
                } catch (NumberFormatException nfe) {
                    throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(nfe, paramName, sLineWrapAfter);
                }
            }
        } else {
            // Disable by default
            usm.setAutoLinebreak(0);
        }
    }

    @Override
    public MailPath saveCompositionSpaceToDraftMail(UUID compositionSpaceId, Optional<StreamedUploadFileIterator> optionalUploadedAttachments, boolean deleteAfterSave, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }

        Message m = compositionSpace.getMessage();

        MailServletInterface mailInterface = null;
        List<Closeable> closeables = null;
        try {
            MimeMessage mimeMessage = new MimeMessage(MimeDefaultSession.getDefaultSession());
            int accountId = Account.DEFAULT_ID;
            {
                InternetAddress senderAddress = null;
                Address sender = m.getSender();
                if (null != sender) {
                    senderAddress = toMimeAddress(sender);
                    mimeMessage.setSender(senderAddress);
                }

                Address from = m.getFrom();
                if (null != from) {
                    InternetAddress fromAddress = toMimeAddress(from);
                    mimeMessage.setFrom(fromAddress);

                    // Determine the account identifier by From address
                    try {
                        accountId = MimeMessageFiller.resolveSender2Account(ServerSessionAdapter.valueOf(session), senderAddress != null ? senderAddress : fromAddress, false, true);
                    } catch (OXException e) {
                        accountId = checkTransportException(e);
                    }
                }

                List<Address> replyTo = m.getReplyTo();
                if (null != replyTo) {
                    mimeMessage.setReplyTo(toMimeAddresses(replyTo));
                }
            }
            {
                List<Address> to = m.getTo();
                if (null != to && !to.isEmpty()) {
                    mimeMessage.setRecipients(MimeMessage.RecipientType.TO, toMimeAddresses(to));
                }
            }
            {
                List<Address> cc = m.getCc();
                if (null != cc && !cc.isEmpty()) {
                    mimeMessage.setRecipients(MimeMessage.RecipientType.CC, toMimeAddresses(cc));
                }
            }
            {
                List<Address> bcc = m.getBcc();
                if (null != bcc && !bcc.isEmpty()) {
                    mimeMessage.setRecipients(MimeMessage.RecipientType.BCC, toMimeAddresses(bcc));
                }
            }

            String subject = m.getSubject();
            if (null != subject) {
                mimeMessage.setSubject(subject, "UTF-8");
            }

            Flags msgFlags = new Flags();
            msgFlags.add(Flags.Flag.DRAFT);
            mimeMessage.setFlags(msgFlags, true);

            if (m.isRequestReadReceipt() && null != m.getFrom()) {
                mimeMessage.setHeader(MessageHeaders.HDR_X_OX_NOTIFICATION, toMimeAddress(m.getFrom()).toString());
            }

            Priority priority = m.getPriority();
            mimeMessage.setHeader(MessageHeaders.HDR_X_PRIORITY, String.valueOf(priority.getLevel()));
            if (Priority.NORMAL == priority) {
                mimeMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "Normal");
            } else if (Priority.LOW == priority) {
                mimeMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "Low");
            } else {
                mimeMessage.setHeader(MessageHeaders.HDR_IMPORTANCE, "High");
            }

            // Encode state to headers
            {
                mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_CONTENT_TYPE, HeaderUtility.encodeHeaderValue(19, m.getContentType().getId()));
                mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_META, HeaderUtility.encodeHeaderValue(11, HeaderUtility.meta2HeaderValue(m.getMeta())));
                mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_SECURITY, HeaderUtility.encodeHeaderValue(15, HeaderUtility.security2HeaderValue(m.getSecurity())));
                mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_SHARED_ATTACHMENTS, HeaderUtility.encodeHeaderValue(25, HeaderUtility.sharedAttachments2HeaderValue(m.getSharedAttachments())));
                if (m.isRequestReadReceipt()) {
                    mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_READ_RECEIPT, HeaderUtility.encodeHeaderValue(19, "true"));
                }
                if (m.getCustomHeaders() != null) {
                    mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_CUSTOM_HEADERS, HeaderUtility.encodeHeaderValue(19, HeaderUtility.customHeaders2HeaderValue(m.getCustomHeaders())));
                }
                {
                    List<Address> replyTo = m.getReplyTo();
                    if (replyTo != null && !replyTo.isEmpty()) {
                        mimeMessage.setHeader(HeaderUtility.HEADER_X_OX_REPLY_TO, HeaderUtility.encodeHeaderValue(13, HeaderUtility.addresses2HeaderValue(m.getReplyTo())));
                        mimeMessage.setReplyTo(toMimeAddresses(m.getReplyTo()));
                    }
                }
            }

            mailInterface = MailServletInterface.getInstance(session);

            // Reply or (inline) forward?
            Meta meta = m.getMeta();
            Optional<MailMessage> originalMail = null;
            {
                MetaType metaType = meta.getType();
                if (metaType == MetaType.REPLY || metaType == MetaType.REPLY_ALL) {
                    MailPath replyFor = meta.getReplyFor();
                    originalMail = optionalMailMessage(replyFor, mailInterface);
                    if (originalMail.isPresent()) {
                        setReplyHeaders(originalMail.get(), mimeMessage, I(MailProperties.getInstance().getMaxLengthForReferencesHeader(session.getUserId(), session.getContextId())));
                    }
                } else if (metaType == MetaType.FORWARD_INLINE) {
                    MailPath forwardFor = meta.getForwardsFor().get(0);
                    originalMail = optionalMailMessage(forwardFor, mailInterface);
                    if (originalMail.isPresent()) {
                        setReplyHeaders(originalMail.get(), mimeMessage);
                    }
                }
            }

            // Build MIME body
            String charset = MailProperties.getInstance().getDefaultMimeCharset();
            List<Attachment> attachments = m.getAttachments();
            boolean isHtml = m.getContentType().isImpliesHtml();

            if ((attachments != null && !attachments.isEmpty()) || optionalUploadedAttachments.isPresent()) {
                // With attachments
                Map<UUID, Attachment> attachmentId2inlineAttachments;
                if (attachments != null && !attachments.isEmpty()) {
                    attachmentId2inlineAttachments = new LinkedHashMap<>(attachments.size());
                    for (Attachment attachment : attachments) {
                        attachmentId2inlineAttachments.put(attachment.getId(), attachment);
                    }
                } else {
                    attachmentId2inlineAttachments = Collections.emptyMap();
                }
                closeables = fillMessageWithAttachments(compositionSpaceId, m, mimeMessage, attachmentId2inlineAttachments, optionalUploadedAttachments, charset, isHtml, session);
            } else {
                // No attachments
                fillMessageWithoutAttachments(m, mimeMessage, charset, isHtml);
            }

            ContentAwareComposedMailMessage mailMessage = new ContentAwareComposedMailMessage(mimeMessage, session, session.getContextId());

            // Security
            {
                SecuritySettings securitySettings = getSecuritySettings(m, null);
                if (securitySettings !=  null) {
                    mailMessage.setSecuritySettings(securitySettings);
                    EncryptedMailService encryptor = services.getOptionalService(EncryptedMailService.class);
                    if (encryptor != null) {
                        mailMessage = (ContentAwareComposedMailMessage) encryptor.encryptDraftEmail(mailMessage, session, null);
                    }
                }
            }

            MailPath draftPath = mailInterface.saveDraft(mailMessage, false, accountId);

            // Check if original mails needs to be removed
            {
                MailPath editFor = meta.getEditFor();
                if (null != editFor) {
                    try {
                        mailInterface.deleteMessages(editFor.getFolderArgument(), new String[] { editFor.getMailID() }, true);
                    } catch (Exception e) {
                        LOG.warn("Failed to delete edited draft mail '{}'", editFor, e);
                    }
                }
            }

            // Close mail resources
            mailInterface.close();
            mailInterface = null;

            if (deleteAfterSave) {
                try {
                    boolean closed = removeCompositionSpaceFromStorage(compositionSpaceId);
                    if (closed) {
                        LOG.debug("Closed composition space '{}' after saved as draft", getUnformattedStringObjectFor(compositionSpaceId));
                    } else {
                        LOG.warn("Compositon space {} could not be closed after saving it to a draft mail.", getUnformattedString(compositionSpaceId));
                    }
                } catch (OXException e) {
                    LOG.warn("Failed to close composition space {} after being saved to draft mail {}", getUnformattedString(compositionSpaceId), draftPath, e);
                }
            }

            return draftPath;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } finally {
            if (null != mailInterface) {
                mailInterface.close();
            }

            Streams.close(closeables);
        }
    }

    private void fillMessageWithoutAttachments(Message m, MimeMessage mimeMessage, String charset, boolean isHtml) throws OXException, MessagingException {
        String content = m.getContent();
        if (Strings.isEmpty(content)) {
            if (isHtml) {
                HtmlService htmlService = services.getService(HtmlService.class);
                content = htmlService.getConformHTML(HTML_SPACE, charset).replace(HTML_SPACE, "");
            } else {
                content = "";
            }
        } else {
            if (isHtml) {
                HtmlService htmlService = services.getService(HtmlService.class);
                content = htmlService.getConformHTML(content, charset);
            } else {
                content = performLineFolding(content, 0);
            }
        }

        MessageUtility.setText(content, charset, isHtml ? "html" : "plain", mimeMessage);
        mimeMessage.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
        mimeMessage.setHeader(MessageHeaders.HDR_CONTENT_TYPE, new StringBuilder(24).append("text/").append(isHtml ? "html" : "plain").append("; charset=").append(charset).toString());
        if (CharMatcher.ascii().matchesAllOf(content)) {
            mimeMessage.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "7bit");
        }
    }

    private List<Closeable> fillMessageWithAttachments(UUID compositionSpaceId, Message m, MimeMessage mimeMessage, Map<UUID, Attachment> fileAttachments, Optional<StreamedUploadFileIterator> optionalUploadedAttachments, String charset, boolean isHtml, Session session) throws OXException, MessagingException {
        List<Closeable> closeables = null;
        if (isHtml) {
            // An HTML message
            Map<ContentId, Attachment> contentId2InlineAttachment;

            String content = m.getContent();
            if (Strings.isEmpty(content)) {
                contentId2InlineAttachment = Collections.emptyMap();

                HtmlService htmlService = services.getService(HtmlService.class);
                content = htmlService.getConformHTML(HTML_SPACE, charset).replace(HTML_SPACE, "");
            } else {
                int numOfAttachments = fileAttachments.size();
                Map<String, Attachment> attachmentId2inlineAttachments;
                if (numOfAttachments > 0) {
                    contentId2InlineAttachment = new HashMap<>(numOfAttachments);
                    attachmentId2inlineAttachments = new HashMap<>(numOfAttachments);

                    for (Attachment attachment : fileAttachments.values()) {
                        if (INLINE == attachment.getContentDisposition() && null != attachment.getContentIdAsObject() && new ContentType(attachment.getMimeType()).startsWith("image/")) {
                            attachmentId2inlineAttachments.put(getUnformattedString(attachment.getId()), attachment);
                            contentId2InlineAttachment.put(attachment.getContentIdAsObject(), attachment);
                        }
                    }
                } else {
                    contentId2InlineAttachment = Collections.emptyMap();
                    attachmentId2inlineAttachments = Collections.emptyMap();
                }

                content = CompositionSpaces.replaceLinkedInlineImages(content, attachmentId2inlineAttachments, contentId2InlineAttachment, fileAttachments, AttachmentImageDataSource.getInstance());
                HtmlService htmlService = services.getService(HtmlService.class);
                content = htmlService.getConformHTML(content, charset);
            }

            Multipart primaryMultipart;
            if (contentId2InlineAttachment.isEmpty()) {
                // No inline images. A simple multipart message
                primaryMultipart = new MimeMultipart();

                // Add text part
                primaryMultipart.addBodyPart(createHtmlBodyPart(content, charset));

                // Add attachments
                for (Attachment attachment : fileAttachments.values()) {
                    addAttachment(attachment, primaryMultipart, session);
                }

                // Add uploaded attachments
                if (optionalUploadedAttachments.isPresent()) {
                    StreamedUploadFileIterator uploadedAttachments = optionalUploadedAttachments.get();
                    if (uploadedAttachments.hasNext()) {
                        List<Closeable> tmp = new LinkedList<>();
                        try {
                            do {
                                StreamedUploadFile uploadFile = uploadedAttachments.next();
                                AttachmentDescription attachmentDescription = AttachmentStorages.createUploadFileAttachmentDescriptionFor(uploadFile, com.openexchange.mail.compose.Attachment.ContentDisposition.ATTACHMENT.getId(), compositionSpaceId);
                                tmp.add(addAttachment(attachmentDescription, uploadFile, primaryMultipart, session));
                            } while (uploadedAttachments.hasNext());

                            closeables = tmp;
                            tmp = null;
                        } finally {
                            Streams.close(tmp);
                        }
                    }
                }
            } else {
                if (fileAttachments.isEmpty()) {
                    // Only inline images
                    primaryMultipart = createMultipartRelated(content, charset, contentId2InlineAttachment, session);
                } else {
                    // Both - file attachments and inline images
                    primaryMultipart = new MimeMultipart();

                    // Add multipart/related
                    BodyPart altBodyPart = new MimeBodyPart();
                    MessageUtility.setContent(createMultipartRelated(content, charset, contentId2InlineAttachment, session), altBodyPart);
                    primaryMultipart.addBodyPart(altBodyPart);

                    // Add remaining file attachments
                    for (Attachment fileAttachment : fileAttachments.values()) {
                        addAttachment(fileAttachment, primaryMultipart, session);
                    }
                }
            }

            mimeMessage.setContent(primaryMultipart);
        } else {
            // A plain-text message
            Multipart primaryMultipart = new MimeMultipart();

            // Add text part
            primaryMultipart.addBodyPart(createTextBodyPart(Strings.isEmpty(m.getContent()) ? "" : performLineFolding(m.getContent(), 0), charset, false));

            // Add attachments
            for (Attachment attachment : fileAttachments.values()) {
                addAttachment(attachment, primaryMultipart, session);
            }

            mimeMessage.setContent(primaryMultipart);
        }

        return closeables;
    }

    private Multipart createMultipartRelated(String wellFormedHTMLContent, String charset, Map<ContentId, Attachment> contentId2InlineAttachment, Session session) throws MessagingException, OXException {
        Multipart relatedMultipart = new MimeMultipart("related");

        relatedMultipart.addBodyPart(createHtmlBodyPart(wellFormedHTMLContent, charset), 0);

        for (Attachment inlineImage : contentId2InlineAttachment.values()) {
            addAttachment(inlineImage, relatedMultipart, session);
        }

        return relatedMultipart;
    }

    private void addAttachment(Attachment attachment, Multipart mp, Session session) throws MessagingException, OXException {
        ContentType ct = new ContentType(attachment.getMimeType());
        if (ct.startsWith(MimeTypes.MIME_MESSAGE_RFC822)) {
            addNestedMessage(attachment, mp);
            return;
        }

        // A non-message attachment
        String fileName = attachment.getName();
        if (fileName != null && (ct.startsWith(MimeTypes.MIME_APPL_OCTET) || ct.startsWith(MimeTypes.MIME_MULTIPART_OCTET))) {
            // Only "allowed" for certain files
            if (!octetExtensions().contains(extensionFor(fileName))) {
                // Try to determine MIME type
                String ct2 = MimeType2ExtMap.getContentType(fileName);
                int pos = ct2.indexOf('/');
                ct.setPrimaryType(ct2.substring(0, pos));
                ct.setSubType(ct2.substring(pos + 1));
            }
        }

        // Create MIME body part and set its content
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(new AttachmentDataSource(attachment)));

        if (fileName != null && !ct.containsNameParameter()) {
            ct.setNameParameter(fileName);
        }
        messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(ct.toString()));

        if (INLINE != attachment.getContentDisposition()) {
            // Force base64 encoding to keep data as it is
            messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "base64");
        }

        // Disposition
        String disposition = messageBodyPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
        ContentDisposition cd;
        if (disposition == null) {
            cd = new ContentDisposition(attachment.getContentDisposition().getId());
        } else {
            cd = new ContentDisposition(disposition);
            cd.setDisposition(attachment.getContentDisposition().getId());
        }
        if (fileName != null && !cd.containsFilenameParameter()) {
            cd.setFilenameParameter(fileName);
        }
        messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MimeMessageUtility.foldContentDisposition(cd.toString()));

        // Content-ID
        ContentId contentId = attachment.getContentIdAsObject();
        if (contentId != null) {
            messageBodyPart.setContentID(contentId.getContentIdForHeader());
        }

        // vCard
        if (AttachmentOrigin.VCARD == attachment.getOrigin()) {
            messageBodyPart.setHeader(MessageHeaders.HDR_X_OX_VCARD, new StringBuilder(16).append(session.getUserId()).append('@').append(session.getContextId()).toString());
        }

        // Add to parental multipart
        mp.addBodyPart(messageBodyPart);
    }

    private Closeable addAttachment(AttachmentDescription attachmentDescription, StreamedUploadFile uploadFile, Multipart mp, Session session) throws MessagingException, OXException {
        ContentType ct = new ContentType(attachmentDescription.getMimeType());
        if (ct.startsWith(MimeTypes.MIME_MESSAGE_RFC822)) {
            return addNestedMessage(attachmentDescription, uploadFile, mp);
        }

        // A non-message attachment
        String fileName = attachmentDescription.getName();
        if (fileName != null && (ct.startsWith(MimeTypes.MIME_APPL_OCTET) || ct.startsWith(MimeTypes.MIME_MULTIPART_OCTET))) {
            // Only "allowed" for certain files
            if (!octetExtensions().contains(extensionFor(fileName))) {
                // Try to determine MIME type
                String ct2 = MimeType2ExtMap.getContentType(fileName);
                int pos = ct2.indexOf('/');
                ct.setPrimaryType(ct2.substring(0, pos));
                ct.setSubType(ct2.substring(pos + 1));
            }
        }

        // Create MIME body part and set its content
        boolean error = true;
        InputStream input = null;
        ThresholdFileHolder fileHolder = null;
        try {
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            fileHolder = new ThresholdFileHolder();
            input = uploadFile.getStream();
            fileHolder.write(input);

            messageBodyPart.setDataHandler(new DataHandler(new AttachmentDescriptionDataSource(attachmentDescription, fileHolder)));

            if (fileName != null && !ct.containsNameParameter()) {
                ct.setNameParameter(fileName);
            }
            messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(ct.toString()));

            if (INLINE != attachmentDescription.getContentDisposition()) {
                // Force base64 encoding to keep data as it is
                messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "base64");
            }

            // Disposition
            String disposition = messageBodyPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
            ContentDisposition cd;
            if (disposition == null) {
                cd = new ContentDisposition(attachmentDescription.getContentDisposition().getId());
            } else {
                cd = new ContentDisposition(disposition);
                cd.setDisposition(attachmentDescription.getContentDisposition().getId());
            }
            if (fileName != null && !cd.containsFilenameParameter()) {
                cd.setFilenameParameter(fileName);
            }
            messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MimeMessageUtility.foldContentDisposition(cd.toString()));

            // Content-ID
            ContentId contentId = attachmentDescription.getContentId();
            if (contentId != null) {
                messageBodyPart.setContentID(contentId.getContentIdForHeader());
            }

            // vCard
            if (AttachmentOrigin.VCARD == attachmentDescription.getOrigin()) {
                messageBodyPart.setHeader(MessageHeaders.HDR_X_OX_VCARD, new StringBuilder(16).append(session.getUserId()).append('@').append(session.getContextId()).toString());
            }

            // Add to parental multipart
            mp.addBodyPart(messageBodyPart);

            error = false;
            return fileHolder;
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (error) {
                Streams.close(input);
                Streams.close(fileHolder);
            }
        }
    }

    private void addNestedMessage(Attachment attachment, Multipart mp) throws MessagingException, OXException {
        String fn;
        if (null == attachment.getName()) {
            InputStream data = attachment.getData();
            try {
                String subject = MimeMessageUtility.checkNonAscii(new InternetHeaders(data).getHeader(MessageHeaders.HDR_SUBJECT, null));
                if (null == subject || subject.length() == 0) {
                    fn = "part.eml";
                } else {
                    subject = MimeMessageUtility.decodeMultiEncodedHeader(MimeMessageUtility.unfold(subject));
                    fn = subject.replaceAll("\\p{Blank}+", "_") + ".eml";
                }
            } finally {
                Streams.close(data);
            }
        } else {
            fn = attachment.getName();
        }

        //Create MIME body part and set its content
        MimeBodyPart origMsgPart = new MimeBodyPart();
        origMsgPart.setDataHandler(new DataHandler(new AttachmentDataSource(attachment, MimeTypes.MIME_MESSAGE_RFC822)));

        // Content-Type
        ContentType ct = new ContentType(MimeTypes.MIME_MESSAGE_RFC822);
        if (null != fn) {
            ct.setNameParameter(fn);
        }
        origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(ct.toString()));

        // Content-Disposition
        String disposition = origMsgPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
        final ContentDisposition cd;
        if (disposition == null) {
            cd = new ContentDisposition(attachment.getContentDisposition().getId());
        } else {
            cd = new ContentDisposition(disposition);
            cd.setDisposition(attachment.getContentDisposition().getId());
        }
        if (null != fn && !cd.containsFilenameParameter()) {
            cd.setFilenameParameter(fn);
        }
        origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MimeMessageUtility.foldContentDisposition(cd.toString()));

        // Add to parental multipart
        mp.addBodyPart(origMsgPart);
    }

    private Closeable addNestedMessage(AttachmentDescription attachmentDescription, StreamedUploadFile uploadFile, Multipart mp) throws MessagingException, OXException {
        boolean error = true;
        InputStream input = null;
        ThresholdFileHolder fileHolder = null;
        try {
            String fn;
            fileHolder = new ThresholdFileHolder();
            input = uploadFile.getStream();
            fileHolder.write(input);
            fileHolder.setContentType(MimeTypes.MIME_MESSAGE_RFC822);

            if (null == attachmentDescription.getName()) {
                InputStream data = fileHolder.getStream();
                try {
                    String subject = MimeMessageUtility.checkNonAscii(new InternetHeaders(data).getHeader(MessageHeaders.HDR_SUBJECT, null));
                    if (null == subject || subject.length() == 0) {
                        fn = "part.eml";
                    } else {
                        subject = MimeMessageUtility.decodeMultiEncodedHeader(MimeMessageUtility.unfold(subject));
                        fn = subject.replaceAll("\\p{Blank}+", "_") + ".eml";
                    }
                } finally {
                    Streams.close(data);
                }
            } else {
                fn = attachmentDescription.getName();
            }

            //Create MIME body part and set its content
            MimeBodyPart origMsgPart = new MimeBodyPart();
            origMsgPart.setDataHandler(new DataHandler(new AttachmentDescriptionDataSource(attachmentDescription, fileHolder)));

            // Content-Type
            ContentType ct = new ContentType(MimeTypes.MIME_MESSAGE_RFC822);
            if (null != fn) {
                ct.setNameParameter(fn);
            }
            origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(ct.toString()));

            // Content-Disposition
            String disposition = origMsgPart.getHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, null);
            final ContentDisposition cd;
            if (disposition == null) {
                cd = new ContentDisposition(attachmentDescription.getContentDisposition().getId());
            } else {
                cd = new ContentDisposition(disposition);
                cd.setDisposition(attachmentDescription.getContentDisposition().getId());
            }
            if (null != fn && !cd.containsFilenameParameter()) {
                cd.setFilenameParameter(fn);
            }
            origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_DISPOSITION, MimeMessageUtility.foldContentDisposition(cd.toString()));

            // Add to parental multipart
            mp.addBodyPart(origMsgPart);

            error = false;
            return fileHolder;
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (error) {
                Streams.close(input);
                Streams.close(fileHolder);
            }
        }
    }

    private static final String HTML_SPACE = "&#160;";

    /**
     * Creates a body part of type <code>text/html</code> from given HTML content
     *
     * @param wellFormedHTMLContent The well-formed HTML content
     * @param charset The charset
     * @return A body part of type <code>text/html</code> from given HTML content
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a processing error occurs
     */
    private BodyPart createHtmlBodyPart(final String wellFormedHTMLContent, final String charset) throws MessagingException, OXException {
        try {
            final String contentType = new StringBuilder("text/html; charset=").append(charset).toString();
            final MimeBodyPart html = new MimeBodyPart();
            if (Strings.isEmpty(wellFormedHTMLContent)) {
                HtmlService htmlService = services.getService(HtmlService.class);
                String htmlContent = htmlService.getConformHTML(HTML_SPACE, charset).replace(HTML_SPACE, "");
                html.setDataHandler(new DataHandler(new MessageDataSource(htmlContent, contentType)));
            } else {
                html.setDataHandler(new DataHandler(new MessageDataSource(wellFormedHTMLContent, contentType)));
            }
            html.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
            html.setHeader(MessageHeaders.HDR_CONTENT_TYPE, contentType);
            return html;
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Unsupported encoding.", e);
        }
    }

    /**
     * Creates a body part of type <code>text/plain</code> for given content
     *
     * @param content The content
     * @param charset The character encoding
     * @param isHtml Whether provided content is HTML or not
     * @return A body part of type <code>text/plain</code>
     * @throws MessagingException If a messaging error occurs
     */
    private BodyPart createTextBodyPart(String content, String charset, boolean isHtml) throws MessagingException {
        /*
         * Convert HTML content to regular text. First: Create a body part for text content
         */
        MimeBodyPart text = new MimeBodyPart();
        /*
         * Define text content
         */
        String textContent;
        {
            if (content == null || content.length() == 0) {
                textContent = "";
            } else if (isHtml) {
                HtmlService htmlService = services.getService(HtmlService.class);
                textContent = performLineFolding(htmlService.html2text(content, false), 0);
            } else {
                textContent = performLineFolding(content, 0);
            }
        }
        MessageUtility.setText(textContent, charset, text);
        text.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
        text.setHeader(MessageHeaders.HDR_CONTENT_TYPE, new StringBuilder("text/plain; charset=").append(charset).toString());
        if (CharMatcher.ascii().matchesAllOf(textContent)) {
            text.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "7bit");
        }
        return text;
    }

    private static String extensionFor(String fileName) {
        if (null == fileName) {
            return null;
        }

        int pos = fileName.lastIndexOf('.');
        return Strings.asciiLowerCase(pos > 0 ? fileName.substring(pos + 1) : fileName);
    }

    @Override
    public CompositionSpace openCompositionSpace(OpenCompositionSpaceParameters parameters) throws OXException {
        UUID uuid = null;

        AttachmentStorage attachmentStorage = null;
        List<Attachment> attachments = null;
        try {
            Type type = parameters.getType();
            if (null == type) {
                type = Type.NEW;
            }

            if (parameters.isAppendOriginalAttachments() && (Type.REPLY != type && Type.REPLY_ALL != type)) {
                throw CompositionSpaceErrorCode.NO_REPLY_FOR.create();
            }

            // Generate composition space identifier
            uuid = UUID.randomUUID();

            // Compile message (draft) for the new composition space
            MessageDescription message = new MessageDescription();

            // Check for priority
            {
                Priority priority = parameters.getPriority();
                if (null != priority) {
                    message.setPriority(priority);
                }
            }

            // Check for Content-Type
            {
                com.openexchange.mail.compose.Message.ContentType contentType = parameters.getContentType();
                if (null != contentType) {
                    message.setContentType(contentType);
                }
            }

            // Check if a read receipt should be requested
            if (parameters.isRequestReadReceipt()) {
                message.setRequestReadReceipt(true);
            }

            // Check if composition space to open is supposed to be encrypted
            Boolean encrypt = B(CryptoUtility.needsEncryption(session, services));

            // Determine the meta information for the message (draft)
            if (Type.NEW == type) {
                LOG.debug("Opening new composition space '{}'", getUnformattedStringObjectFor(uuid));
                message.setMeta(Meta.META_NEW);
            } else if (Type.FAX == type) {
                LOG.debug("Opening fax composition space '{}'", getUnformattedStringObjectFor(uuid));
                message.setMeta(Meta.META_FAX);
            } else if (Type.SMS == type) {
                LOG.debug("Opening SMS composition space '{}'", getUnformattedStringObjectFor(uuid));
                message.setMeta(Meta.META_SMS);
            } else {
                OpenState args = new OpenState(uuid, message, encrypt, Meta.builder());
                try {
                    Meta.Builder metaBuilder = args.metaBuilder;
                    metaBuilder.withOrigin(type);
                    metaBuilder.withType(Meta.MetaType.metaTypeFor(type));

                    if (type == Type.FORWARD) {
                        LOG.debug("Opening forward composition space '{}'", getUnformattedStringObjectFor(uuid));
                        new Forward(attachmentStorageService, services).doOpenForForward(parameters, args, session);
                    } else if (type == Type.REPLY || type == Type.REPLY_ALL) {
                        LOG.debug("Opening reply composition space '{}'", getUnformattedStringObjectFor(uuid));
                        new Reply(attachmentStorageService, services).doOpenForReply(type == Type.REPLY_ALL, parameters, args, session);
                    } else if (type == Type.EDIT) {
                        LOG.debug("Opening edit-draft composition space '{}'", getUnformattedStringObjectFor(uuid));
                        new EditCopy(attachmentStorageService, services).doOpenForEditCopy(true, parameters, args, session);
                    } else if (type == Type.COPY) {
                        LOG.debug("Opening copy-draft composition space '{}'", getUnformattedStringObjectFor(uuid));
                        new EditCopy(attachmentStorageService, services).doOpenForEditCopy(false, parameters, args, session);
                    } else if (type == Type.RESEND) {
                        LOG.debug("Opening resend composition space '{}'", getUnformattedStringObjectFor(uuid));
                        new Resend(attachmentStorageService, services).doOpenForResend(parameters, args, session);
                    }

                    message.setMeta(metaBuilder.build());
                } catch (MessagingException e) {
                    throw MimeMailException.handleMessagingException(e);
                } finally {
                    attachmentStorage = args.attachmentStorage;
                    attachments = args.attachments;
                    if (null != args.mailInterface) {
                        args.mailInterface.close(true);
                    }
                }
            }

            // Check if vCard of session-associated user is supposed to be attached
            if (parameters.isAppendVCard()) {
                // Obtain attachment storage
                if (null == attachmentStorage) {
                    attachmentStorage = getAttachmentStorage(session);
                }

                // Create VCard
                VCardAndFileName userVCard = CompositionSpaces.getUserVCard(session);

                // Check by file name
                boolean contained = false;
                if (null != attachments) {
                    for (Iterator<Attachment> it = attachments.iterator(); !contained && it.hasNext();) {
                        Attachment existingAttachment = it.next();
                        String fileName = existingAttachment.getName();
                        if (fileName != null && fileName.equals(userVCard.getFileName())) {
                            // vCard already contained
                            contained = true;
                        }
                    }
                }
                // Compile attachment (if not contained)
                if (!contained) {
                    AttachmentDescription attachment = AttachmentStorages.createVCardAttachmentDescriptionFor(userVCard, uuid, true);
                    Attachment vcardAttachment = AttachmentStorages.saveAttachment(Streams.newByteArrayInputStream(userVCard.getVcard()), attachment, Optional.of(encrypt), session, attachmentStorage);
                    if (null == attachments) {
                        attachments = new ArrayList<>(1);
                    }
                    attachments.add(vcardAttachment);
                }
            }

            if (null != attachments) {
                Collections.sort(attachments, AttachmentComparator.getInstance());
                message.setAttachments(attachments);
            }

            message.setClientToken(parameters.getClientToken());

            CompositionSpace compositionSpace = getStorageService().openCompositionSpace(session, new CompositionSpaceDescription().setUuid(uuid).setMessage(message), Optional.of(encrypt));
            if (!compositionSpace.getId().getId().equals(uuid)) {
                // Composition space identifier is not equal to generated one
                getStorageService().closeCompositionSpace(session, compositionSpace.getId().getId());
                throw CompositionSpaceErrorCode.OPEN_FAILED.create();
            }
            LOG.debug("Opened composition space '{}'", compositionSpace.getId());
            attachments = null; // Avoid premature deletion
            return compositionSpace;
        } finally {
            if (null != attachments && null != attachmentStorage) {
                for (Attachment deleteMe : attachments) {
                    deleteAttachmentSafe(deleteMe, attachmentStorage, session);
                }
            }
        }
    }

    private void deleteAttachmentSafe(Attachment attachmentToDelete, AttachmentStorage attachmentStorage, Session session) {
        try {
            attachmentStorage.deleteAttachment(attachmentToDelete.getId(), session);
        } catch (Exception e) {
            LOG.error("Failed to delete attachment with ID {} from storage {}", getUnformattedString(attachmentToDelete.getId()), attachmentStorage.getClass().getName(), e);
        }
    }

    @Override
    public AttachmentResult addAttachmentToCompositionSpace(UUID compositionSpaceId, AttachmentDescription attachmentDesc, InputStream data, ClientToken clientToken) throws OXException {
        try {
            CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
            if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
                throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
            }

            // Obtain attachment storage
            AttachmentStorage attachmentStorage = getAttachmentStorage(session);
            Attachment newAttachment = null;
            try {
                attachmentDesc.setCompositionSpaceId(compositionSpaceId);
                newAttachment = AttachmentStorages.saveAttachment(data, attachmentDesc, session, attachmentStorage);

                boolean retry = true;
                int retryCount = 0;
                do {
                    try {
                        // Add new attachments to message
                        List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());
                        attachments.add(newAttachment);
                        Collections.sort(attachments, AttachmentComparator.getInstance());

                        // Add new attachments to composition space
                        MessageDescription md = new MessageDescription();
                        md.setAttachments(attachments);
                        CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                        compositionSpace = updatedCompositionSpace;
                        retry = false;
                    } catch (OXException e) {
                        if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                            throw e;
                        }

                        // Exponential back-off
                        exponentialBackoffWait(++retryCount, 1000L);

                        // Reload & retry
                        compositionSpace = getCompositionSpace(compositionSpaceId);
                    }
                } while (retry);

                // Everything went fine
                AttachmentResult retval = attachmentResultFor(newAttachment, compositionSpace);
                newAttachment = null;
                return retval;
            } finally {
                if (null != newAttachment) {
                    attachmentStorage.deleteAttachment(newAttachment.getId(), session);
                }
            }
        } finally {
            Streams.close(data);
        }
    }

    @Override
    public AttachmentResult addAttachmentsToCompositionSpace(UUID compositionSpaceId, List<AttachmentDescriptionAndData> descriptionsAndDatas, ClientToken clientToken) throws OXException {
        try {
            if (descriptionsAndDatas == null) {
                throw CompositionSpaceErrorCode.ERROR.create("Attachments must not be null");
            }
            int size = descriptionsAndDatas.size();
            if (size <= 0) {
                throw CompositionSpaceErrorCode.ERROR.create("Attachments must not be empty");
            }

            CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
            if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
                throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
            }

            AttachmentStorage attachmentStorage = getAttachmentStorage(session);
            List<Attachment> newAttachments = new ArrayList<Attachment>(size);
            try {
                for (AttachmentDescriptionAndData descriptionAndData : descriptionsAndDatas) {
                    AttachmentDescription attachmentDesc = descriptionAndData.getAttachmentDescription();
                    attachmentDesc.setCompositionSpaceId(compositionSpaceId);
                    newAttachments.add(AttachmentStorages.saveAttachment(descriptionAndData.getData(), attachmentDesc, session, attachmentStorage));
                }

                boolean retry = true;
                int retryCount = 0;
                do {
                    try {
                        // Add new attachments to message
                        List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());
                        attachments.addAll(newAttachments);
                        Collections.sort(attachments, AttachmentComparator.getInstance());

                        // Add new attachments to composition space
                        MessageDescription md = new MessageDescription();
                        md.setAttachments(attachments);
                        CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                        compositionSpace = updatedCompositionSpace;
                        retry = false;
                    } catch (OXException e) {
                        if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                            throw e;
                        }

                        // Exponential back-off
                        exponentialBackoffWait(++retryCount, 1000L);

                        // Reload & retry
                        compositionSpace = getCompositionSpace(compositionSpaceId);
                    }
                } while (retry);

                // Everything went fine
                AttachmentResult retval = attachmentResultFor(newAttachments, compositionSpace);
                newAttachments = null;
                return retval;
            } finally {
                if (newAttachments != null) {
                    for (Attachment newAttachment : newAttachments) {
                        if (null != newAttachment) {
                            newAttachment.close();
                        }
                    }
                }
            }
        } finally {
            if (descriptionsAndDatas != null) {
                for (AttachmentDescriptionAndData descriptionAndData : descriptionsAndDatas) {
                    descriptionAndData.closeIfNecessary();
                }
            }
        }
    }

    @Override
    public AttachmentResult replaceAttachmentInCompositionSpace(UUID compositionSpaceId, UUID attachmentId, StreamedUploadFileIterator uploadedAttachments, String disposition, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CONCURRENT_UPDATE.create();
        }

        // Check attachment existence
        {
            List<Attachment> attachments = compositionSpace.getMessage().getAttachments();
            if (null == attachments || attachments.isEmpty()) {
                String sAttachmentId = getUnformattedString(attachmentId);
                String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                LOG.debug("No such attachment {} in compositon space {}. Available attachments are: []", sAttachmentId, sCompositionSpaceId);
                throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
            }

            Attachment toReplace = null;
            for (Iterator<Attachment> it = attachments.iterator(); null == toReplace && it.hasNext();) {
                Attachment a = it.next();
                if (attachmentId.equals(a.getId())) {
                    toReplace = a;
                }
            }
            if (null == toReplace) {
                // No such attachment
                String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                String sAttachmentId = getUnformattedString(attachmentId);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No such attachment {} in compositon space {}. Available attachments are: {}", sAttachmentId, sCompositionSpaceId, generateAttachmentIdListing(compositionSpace.getMessage().getAttachments()));
                }
                throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
            }
        }

        // Obtain attachment storage
        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        Attachment newAttachment = null;
        try {
            if (uploadedAttachments.hasNext()) {
                StreamedUploadFile uploadFile = uploadedAttachments.next();
                AttachmentDescription attachment = AttachmentStorages.createUploadFileAttachmentDescriptionFor(uploadFile, disposition, compositionSpaceId);
                LogProperties.put(LogProperties.Name.FILESTORE_SPOOL, "true");
                try {
                    newAttachment = AttachmentStorages.saveAttachment(uploadFile.getStream(), attachment, session, attachmentStorage);
                } finally {
                    LogProperties.remove(LogProperties.Name.FILESTORE_SPOOL);
                }
            }

            if (newAttachment == null) {
                // Nothing added
                throw CompositionSpaceErrorCode.ERROR.create("Upload must not be empty");
            }

            boolean retry = true;
            int retryCount = 0;
            do {
                try {
                    // Replace new attachment in message
                    List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());

                    int index = 0;
                    boolean found = false;
                    for (Iterator<Attachment> it = attachments.iterator(); !found && it.hasNext();) {
                        Attachment a = it.next();
                        if (attachmentId.equals(a.getId())) {
                            found = true;
                        } else {
                            index++;
                        }
                    }

                    if (!found) {
                        // No such attachment
                        String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                        String sAttachmentId = getUnformattedString(attachmentId);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("No such attachment {} in compositon space {}. Available attachments are: {}", sAttachmentId, sCompositionSpaceId, generateAttachmentIdListing(compositionSpace.getMessage().getAttachments()));
                        }
                        throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
                    }

                    attachments.set(index, newAttachment);

                    // Replace attachment in composition space
                    MessageDescription md = new MessageDescription();
                    md.setAttachments(attachments);
                    CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                    compositionSpace = updatedCompositionSpace;
                    retry = false;
                } catch (OXException e) {
                    if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                        throw e;
                    }

                    // Exponential back-off
                    exponentialBackoffWait(++retryCount, 1000L);

                    // Reload & retry
                    compositionSpace = getCompositionSpace(compositionSpaceId);
                }
            } while (retry);

            // Everything went fine
            AttachmentResult retval = attachmentResultFor(newAttachment, compositionSpace);
            newAttachment = null;
            return retval;
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (null != newAttachment) {
                attachmentStorage.deleteAttachment(newAttachment.getId(), session);
            }
        }
    }

    @Override
    public AttachmentResult addAttachmentToCompositionSpace(UUID compositionSpaceId, StreamedUploadFileIterator uploadedAttachments, String disposition, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }

        // Obtain attachment storage
        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        List<Attachment> newAttachments = new LinkedList<Attachment>();
        try {
            if (uploadedAttachments.hasNext()) {
                LogProperties.put(LogProperties.Name.FILESTORE_SPOOL, "true");
                try {
                    do {
                        StreamedUploadFile uploadFile = uploadedAttachments.next();
                        AttachmentDescription attachment = AttachmentStorages.createUploadFileAttachmentDescriptionFor(uploadFile, disposition, compositionSpaceId);
                        Attachment newAttachment = AttachmentStorages.saveAttachment(uploadFile.getStream(), attachment, session, attachmentStorage);
                        newAttachments.add(newAttachment);
                        AttachmentStorages.isIllegalUpload(newAttachment);
                    } while (uploadedAttachments.hasNext());
                } finally {
                    LogProperties.remove(LogProperties.Name.FILESTORE_SPOOL);
                }
            }

            boolean retry = true;
            int retryCount = 0;
            do {
                try {
                    // Add new attachments to message
                    List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());
                    for (Attachment attachment : newAttachments) {
                        attachments.add(attachment);
                    }
                    Collections.sort(attachments, AttachmentComparator.getInstance());

                    // Add new attachments to composition space
                    MessageDescription md = new MessageDescription();
                    md.setAttachments(attachments);
                    CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                    compositionSpace = updatedCompositionSpace;
                    retry = false;
                } catch (OXException e) {
                    if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                        throw e;
                    }

                    // Exponential back-off
                    exponentialBackoffWait(++retryCount, 1000L);

                    // Reload & retry
                    compositionSpace = getCompositionSpace(compositionSpaceId);
                }
            } while (retry);

            // Everything went fine
            AttachmentResult retval = attachmentResultFor(newAttachments, compositionSpace);
            newAttachments = null;
            return retval;
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (null != newAttachments) {
                for (Attachment attachment : newAttachments) {
                    attachmentStorage.deleteAttachment(attachment.getId(), session);
                }
            }
        }
    }

    @Override
    public AttachmentResult addVCardToCompositionSpace(UUID compositionSpaceId, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }

        for (Attachment existingAttachment : compositionSpace.getMessage().getAttachments()) {
            if (AttachmentOrigin.VCARD == existingAttachment.getOrigin()) {
                // vCard already contained
                return attachmentResultFor(existingAttachment, compositionSpace);
            }
        }

        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        // Compile & save vCard attachment
        Attachment vcardAttachment;
        {
            // Create VCard
            VCardAndFileName userVCard = CompositionSpaces.getUserVCard(session);

            // Check by file name
            for (Attachment existingAttachment : compositionSpace.getMessage().getAttachments()) {
                String fileName = existingAttachment.getName();
                if (fileName != null && fileName.equals(userVCard.getFileName())) {
                    // vCard already contained
                    return attachmentResultFor(existingAttachment, compositionSpace);
                }
            }

            AttachmentDescription attachment = AttachmentStorages.createVCardAttachmentDescriptionFor(userVCard, compositionSpaceId, true);
            vcardAttachment = AttachmentStorages.saveAttachment(Streams.newByteArrayInputStream(userVCard.getVcard()), attachment, session, attachmentStorage);
        }

        try {
            boolean retry = true;
            int retryCount = 0;
            do {
                try {
                    // Add new attachment to message
                    List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());
                    attachments.add(vcardAttachment);
                    Collections.sort(attachments, AttachmentComparator.getInstance());

                    // Add new attachments to composition space
                    MessageDescription md = new MessageDescription().setAttachments(attachments);
                    CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                    compositionSpace = updatedCompositionSpace;
                    retry = false;
                } catch (OXException e) {
                    if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                        throw e;
                    }

                    // Exponential back-off
                    exponentialBackoffWait(++retryCount, 1000L);

                    // Reload & retry
                    compositionSpace = getCompositionSpace(compositionSpaceId);
                }

                if (retry) {
                    for (Attachment existingAttachment : compositionSpace.getMessage().getAttachments()) {
                        if (AttachmentOrigin.VCARD == existingAttachment.getOrigin()) {
                            // vCard already contained
                            return attachmentResultFor(existingAttachment, compositionSpace);
                        }

                        String fileName = existingAttachment.getName();
                        if (fileName != null && fileName.equals(vcardAttachment.getName())) {
                            // vCard already contained
                            return attachmentResultFor(existingAttachment, compositionSpace);
                        }
                    }
                }
            } while (retry);

            AttachmentResult retval = attachmentResultFor(vcardAttachment, compositionSpace);
            vcardAttachment = null;
            return retval;
        } finally {
            if (null != vcardAttachment) {
                deleteAttachmentSafe(vcardAttachment, attachmentStorage, session);
            }
        }
    }

    @Override
    public AttachmentResult addContactVCardToCompositionSpace(UUID compositionSpaceId, String contactId, String folderId, ClientToken clientToken) throws OXException {
        if (contactId == null) {
            throw CompositionSpaceErrorCode.ERROR.create("Contact identifier must not be null");
        }
        if (folderId == null) {
            throw CompositionSpaceErrorCode.ERROR.create("Folder identifier must not be null");
        }

        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }

        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        // Compile & save vCard attachment
        Attachment vcardAttachment;
        {
            // Create VCard
            VCardAndFileName contactVCard = CompositionSpaces.getContactVCard(contactId, folderId, session);
            byte[] vcard = contactVCard.getVcard();

            AttachmentDescription attachment = AttachmentStorages.createVCardAttachmentDescriptionFor(contactVCard, compositionSpaceId, false);
            vcardAttachment = AttachmentStorages.saveAttachment(Streams.newByteArrayInputStream(vcard), attachment, session, attachmentStorage);
        }

        try {
            boolean retry = true;
            int retryCount = 0;
            do {
                try {
                    // Add new attachment to message
                    List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());
                    attachments.add(vcardAttachment);
                    Collections.sort(attachments, AttachmentComparator.getInstance());

                    // Add new attachments to composition space
                    MessageDescription md = new MessageDescription().setAttachments(attachments);
                    CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                    compositionSpace = updatedCompositionSpace;
                    retry = false;
                } catch (OXException e) {
                    if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                        throw e;
                    }

                    // Exponential back-off
                    exponentialBackoffWait(++retryCount, 1000L);

                    // Reload & retry
                    compositionSpace = getCompositionSpace(compositionSpaceId);
                }
            } while (retry);

            AttachmentResult retval = attachmentResultFor(vcardAttachment, compositionSpace);
            vcardAttachment = null;
            return retval;
        } finally {
            if (null != vcardAttachment) {
                deleteAttachmentSafe(vcardAttachment, attachmentStorage, session);
            }
        }
    }

    @Override
    public CompositionSpace getCompositionSpace(UUID compositionSpaceId) throws OXException {
        CompositionSpace compositionSpace = getStorageService().getCompositionSpace(session, compositionSpaceId);
        if (null == compositionSpace) {
            throw CompositionSpaceErrorCode.NO_SUCH_COMPOSITION_SPACE.create(getUnformattedString(compositionSpaceId));
        }
        return compositionSpace;
    }

    @Override
    public List<CompositionSpace> getCompositionSpaces(MessageField[] fields) throws OXException {
        return getStorageService().getCompositionSpaces(session, fields);
    }

    @Override
    public CompositionSpace updateCompositionSpace(UUID compositionSpaceId, MessageDescription md, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = null;

        boolean retry = true;
        int retryCount = 0;
        do {
            try {
                compositionSpace = getCompositionSpace(compositionSpaceId);
                if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
                    throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
                }

                // Check if attachment identifiers are about to be changed
                Set<UUID> keepAttachmentIds = null;
                Set<UUID> oldAttachmentIds = null;
                if (md.containsAttachments() && compositionSpace.getMessage().getAttachments() != null) {
                    List<Attachment> attachments = md.getAttachments();
                    if (attachments != null) {
                        keepAttachmentIds = attachments.stream().map(a -> a.getId()).collect(Collectors.toSet());
                        oldAttachmentIds = compositionSpace.getMessage().getAttachments().stream().map(a -> a.getId()).collect(Collectors.toSet());
                    }
                }

                // Perform update
                compositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                retry = false;

                // Update successfully performed... Check for orphaned attachments
                if (null != keepAttachmentIds && null != oldAttachmentIds) {
                    AttachmentStorage attachmentStorage = getAttachmentStorage(session);

                    List<UUID> toDelete = null;
                    for (UUID oldAttachmentId : oldAttachmentIds) {
                        if (keepAttachmentIds.contains(oldAttachmentId) == false) {
                            if (toDelete == null) {
                                toDelete = new ArrayList<UUID>(oldAttachmentIds.size());
                            }
                            toDelete.add(oldAttachmentId);
                        }
                    }

                    if (toDelete != null) {
                        attachmentStorage.deleteAttachments(toDelete, session);
                    }
                }
            } catch (OXException e) {
                if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                    throw e;
                }

                // Exponential back-off
                exponentialBackoffWait(++retryCount, 1000L);

                // Reload & retry
            }
        } while (retry);

        if (null == compositionSpace) {
            throw CompositionSpaceErrorCode.NO_SUCH_COMPOSITION_SPACE.create(getUnformattedString(compositionSpaceId));
        }
        return compositionSpace;
    }

    @Override
    public boolean closeCompositionSpace(UUID compositionSpaceId, boolean hardDelete, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getStorageService().getCompositionSpace(session, compositionSpaceId);
        if (compositionSpace == null) {
            // No such composition space
            return false;
        }

        if (clientToken.isPresent()) {
            if (clientToken.isNotEquals(compositionSpace.getClientToken())) {
                throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
            }
        }

        // Auto-delete referenced draft message on edit-draft
        MailPath editFor = getEditForFrom(compositionSpace);
        if (null != editFor) {
            MailServletInterface mailInterface = null;
            try {
                mailInterface = MailServletInterface.getInstance(session);
                mailInterface.deleteMessages(editFor.getFolderArgument(), new String[] { editFor.getMailID() }, true);
            } catch (Exception e) {
                LOG.warn("Failed to delete edited draft mail '{}'", editFor, e);
            } finally {
                if (null != mailInterface) {
                    mailInterface.close();
                }
            }
        }

        return removeCompositionSpaceFromStorage(compositionSpaceId);
    }

    private MailPath getEditForFrom(CompositionSpace compositionSpace) {
        Message message = compositionSpace.getMessage();
        if (message == null) {
            return null;
        }

        Meta meta = message.getMeta();
        return meta == null ? null : meta.getEditFor();
    }

    private boolean removeCompositionSpaceFromStorage(UUID compositionSpaceId) throws OXException {
        boolean closed = getStorageService().closeCompositionSpace(session, compositionSpaceId);
        if (closed) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Closed composition space: {}", getUnformattedString(compositionSpaceId));
            }
            try {
                AttachmentStorage attachmentStorage = getAttachmentStorage(session);
                attachmentStorage.deleteAttachmentsByCompositionSpace(compositionSpaceId, session);
            } catch (Exception e) {
                LOG.warn("Failed to delete possible attachment association with composition space: {}", getUnformattedString(compositionSpaceId), e);
            }
        }
        return closed;
    }

    @Override
    public void closeExpiredCompositionSpaces(long maxIdleTimeMillis) throws OXException {
        List<UUID> deleted = getStorageService().deleteExpiredCompositionSpaces(session, maxIdleTimeMillis);
        if (null != deleted && !deleted.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closed expired composition spaces: {}", toStringFor(deleted));
            }
            try {
                AttachmentStorage attachmentStorage = getAttachmentStorage(session);
                for (UUID compositionSpaceId : deleted) {
                    try {
                        attachmentStorage.deleteAttachmentsByCompositionSpace(compositionSpaceId, session);
                    } catch (Exception e) {
                        LOG.warn("Failed to delete possible attachments associated with composition space {}", getUnformattedString(compositionSpaceId), e);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to obtain attachment storage service needed to delete possible attachments associated with expired (and therefore closed) composition spaces", e);
            }
        }
    }

    @Override
    public AttachmentResult addOriginalAttachmentsToCompositionSpace(UUID compositionSpaceId, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }

        // Acquire meta information and determine the "replyFor" path
        Meta meta = compositionSpace.getMessage().getMeta();
        MailPath replyFor = meta.getReplyFor();
        if (null == replyFor) {
            throw CompositionSpaceErrorCode.NO_REPLY_FOR.create();
        }

        // Obtain attachment storage
        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        List<Attachment> newAttachments = null;
        MailServletInterface mailInterface = null;
        try {
            mailInterface = MailServletInterface.getInstance(session);
            MailMessage originalMail = requireMailMessage(replyFor, mailInterface);

            if (!originalMail.getContentType().startsWith("multipart/")) {
                return attachmentResultFor(compositionSpace);
            }

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
            if (null == nonInlineParts || nonInlineParts.isEmpty()) {
                return attachmentResultFor(compositionSpace);
            }

            newAttachments = new ArrayList<>(nonInlineParts.size());
            int i = 0;
            for (MailPart mailPart : nonInlineParts) {
                // Compile & store attachment
                AttachmentDescription attachment = AttachmentStorages.createAttachmentDescriptionFor(mailPart, i + 1, -1L, compositionSpaceId, session);
                Attachment partAttachment = AttachmentStorages.saveAttachment(mailPart.getInputStream(), attachment, session, attachmentStorage);
                newAttachments.add(partAttachment);
                i++;
            }

            boolean retry = true;
            int retryCount = 0;
            do {
                try {
                    // Add new attachments to message
                    List<Attachment> attachments = new ArrayList<Attachment>(compositionSpace.getMessage().getAttachments());
                    for (Attachment attachment : newAttachments) {
                        attachments.add(attachment);
                    }
                    Collections.sort(attachments, AttachmentComparator.getInstance());

                    // Add new attachments to composition space
                    MessageDescription md = new MessageDescription();
                    md.setAttachments(attachments);
                    CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                    compositionSpace = updatedCompositionSpace;
                    retry = false;
                } catch (OXException e) {
                    if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                        throw e;
                    }

                    // Exponential back-off
                    exponentialBackoffWait(++retryCount, 1000L);

                    // Reload & retry
                    compositionSpace = getCompositionSpace(compositionSpaceId);
                }
            } while (retry);

            // Everything went fine
            AttachmentResult retval = attachmentResultFor(newAttachments, compositionSpace);
            newAttachments = null;
            return retval;
        } finally {
            if (null != mailInterface) {
                mailInterface.close(true);
            }
            if (null != newAttachments) {
                for (Attachment attachment : newAttachments) {
                    attachmentStorage.deleteAttachment(attachment.getId(), session);
                }
            }
        }
    }

    @Override
    public AttachmentResult getAttachment(UUID compositionSpaceId, UUID attachmentId) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);

        // Obtain attachment storage
        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        // Find the attachment to return in composition space
        {
            List<Attachment> attachments = compositionSpace.getMessage().getAttachments();
            if (null == attachments) {
                String sAttachmentId = getUnformattedString(attachmentId);
                String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                LOG.debug("No such attachment {} in compositon space {}. Available attachments are: []", sAttachmentId, sCompositionSpaceId);
                throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
            }

            Attachment toReturn = null;
            for (Iterator<Attachment> it = attachments.iterator(); null == toReturn && it.hasNext();) {
                Attachment a = it.next();
                if (attachmentId.equals(a.getId())) {
                    toReturn = a;
                }
            }
            if (null == toReturn) {
                // No such attachment
                String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                String sAttachmentId = getUnformattedString(attachmentId);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No such attachment {} in compositon space {}. Available attachments are: {}", sAttachmentId, sCompositionSpaceId, generateAttachmentIdListing(compositionSpace.getMessage().getAttachments()));
                }
                throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
            }
        }

        // Look-up attachment in attachment storage
        Attachment attachment = attachmentStorage.getAttachment(attachmentId, Optional.empty(), session);
        if (null == attachment) {
            // No such attachment. Delete non-existent attachment from composition space's references
            boolean retry = true;
            int retryCount = 0;
            do {
                try {
                    List<Attachment> existingAttachments = compositionSpace.getMessage().getAttachments();
                    List<Attachment> attachments = new ArrayList<Attachment>(existingAttachments.size());
                    for (Iterator<Attachment> it = existingAttachments.iterator(); it.hasNext();) {
                        Attachment a = it.next();
                        if (!attachmentId.equals(a.getId())) {
                            attachments.add(a);
                        }
                    }
                    Collections.sort(attachments, AttachmentComparator.getInstance());

                    MessageDescription md = new MessageDescription().setAttachments(attachments);
                    CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                    compositionSpace = updatedCompositionSpace;
                    retry = false;
                } catch (OXException e) {
                    if (CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                        // Exponential back-off
                        exponentialBackoffWait(++retryCount, 1000L);

                        // Reload & retry
                        compositionSpace = getCompositionSpace(compositionSpaceId);
                    } else {
                        LOG.warn("Failed to delete non-existent attachment {} from composition space {}", getUnformattedString(attachmentId), getUnformattedString(compositionSpaceId), e);
                        retry = false;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to delete non-existent attachment {} from composition space {}", getUnformattedString(attachmentId), getUnformattedString(compositionSpaceId), e);
                    retry = false;
                }
            } while (retry);

            throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_RESOURCE.create(getUnformattedString(attachmentId));
        }

        return attachmentResultFor(attachment, compositionSpace);
    }

    @Override
    public AttachmentResult deleteAttachment(UUID compositionSpaceId, UUID attachmentId, ClientToken clientToken) throws OXException {
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        if (clientToken.isPresent() && clientToken.isNotEquals(compositionSpace.getClientToken())) {
            throw CompositionSpaceErrorCode.CLIENT_TOKEN_MISMATCH.create();
        }

        // Obtain attachment storage
        AttachmentStorage attachmentStorage = getAttachmentStorage(session);

        boolean retry = true;
        int retryCount = 0;
        do {
            try {
                // Find the attachment to delete
                List<Attachment> attachments;
                {
                    List<Attachment> existingAttachments = compositionSpace.getMessage().getAttachments();
                    if (null == existingAttachments) {
                        String sAttachmentId = getUnformattedString(attachmentId);
                        String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                        LOG.debug("No such attachment {} in compositon space {}. Available attachments are: []", sAttachmentId, sCompositionSpaceId);
                        throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
                    }

                    attachments = new ArrayList<Attachment>(existingAttachments);
                }

                Attachment toDelete = null;
                for (Iterator<Attachment> it = attachments.iterator(); toDelete == null && it.hasNext();) {
                    Attachment attachment = it.next();
                    if (attachmentId.equals(attachment.getId())) {
                        toDelete = attachment;
                        it.remove();
                    }
                }

                if (null == toDelete) {
                    // No such attachment
                    String sAttachmentId = getUnformattedString(attachmentId);
                    String sCompositionSpaceId = getUnformattedString(compositionSpaceId);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No such attachment {} in compositon space {}. Available attachments are: {}", sAttachmentId, sCompositionSpaceId, generateAttachmentIdListing(compositionSpace.getMessage().getAttachments()));
                    }
                    throw CompositionSpaceErrorCode.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE.create(sAttachmentId, sCompositionSpaceId);
                }
                if (!attachments.isEmpty()) {
                    Collections.sort(attachments, AttachmentComparator.getInstance());
                }

                // Update composition space
                MessageDescription md = new MessageDescription().setAttachments(attachments);
                CompositionSpace updatedCompositionSpace = getStorageService().updateCompositionSpace(session, new CompositionSpaceDescription().setUuid(compositionSpaceId).setMessage(md).setLastModifiedDate(new Date(compositionSpace.getLastModified())), Optional.of(compositionSpace));
                compositionSpace = updatedCompositionSpace;
                retry = false;
            } catch (OXException e) {
                if (!CompositionSpaceErrorCode.CONCURRENT_UPDATE.equals(e)) {
                    throw e;
                }

                // Exponential back-off
                exponentialBackoffWait(++retryCount, 1000L);

                // Reload & retry
                compositionSpace = getCompositionSpace(compositionSpaceId);
            }
        } while (retry);

        // Delete the denoted attachment
        attachmentStorage.deleteAttachment(attachmentId, session);
        return attachmentResultFor(compositionSpace);
    }

    @Override
    public UploadLimits getAttachmentUploadLimits(UUID compositionSpaceId) throws OXException {
        SharedAttachmentsInfo sharedAttachmentsInfo;
        CompositionSpace compositionSpace = getCompositionSpace(compositionSpaceId);
        sharedAttachmentsInfo = compositionSpace.getMessage().getSharedAttachments();

        UploadLimits.Type type = sharedAttachmentsInfo.isEnabled() ? UploadLimits.Type.DRIVE : UploadLimits.Type.MAIL;
        return UploadLimits.get(type, session);
    }

    /**
     * Sets the appropriate headers <code>In-Reply-To</code> and <code>References</code> in specified MIME message.
     *
     * @param referencedMail The referenced mail
     * @param message The message to set in
     * @param maxReferencesLength The optional max. length for <code>"References"</code> header
     */
    private static void setReplyHeaders(MailMessage referencedMail, ComposedMailMessage message, Integer... maxReferencesLength) {
        MimeMessageFiller.setReplyHeaders(referencedMail, message, maxReferencesLength);
    }

    /**
     * Sets the appropriate headers <code>In-Reply-To</code> and <code>References</code> in specified MIME message.
     *
     * @param referencedMail The referenced mail
     * @param mimeMessage The MIME message to set in
     * @param maxReferencesLength The optional max. length for <code>"References"</code> header
     * @throws OXException If headers cannot be set
     */
    private static void setReplyHeaders(MailMessage referencedMail, MimeMessage mimeMessage, Integer... maxReferencesLength) throws OXException {
        MimeMessageFiller.setReplyHeaders(referencedMail, mimeMessage, maxReferencesLength);
    }

    /**
     * Gets referenced mail
     *
     * @param mailPath The mail path for the mail
     * @param mailInterface The service to use
     * @return The mail
     * @throws OXException If mail cannot be returned
     */
    private MailMessage requireMailMessage(MailPath mailPath, MailServletInterface mailInterface) throws OXException {
        Optional<MailMessage> optionalMailMessage = optionalMailMessage(mailPath, mailInterface);
        if (!optionalMailMessage.isPresent()) {
            throw MailExceptionCode.MAIL_NOT_FOUND.create(mailPath.getMailID(), mailPath.getFolderArgument());
        }
        return optionalMailMessage.get();
    }

    /**
     * Gets the optional referenced mail
     *
     * @param mailPath The mail path for the mail
     * @param mailInterface The service to use
     * @return The optional mail
     * @throws OXException If mail cannot be returned
     */
    private Optional<MailMessage> optionalMailMessage(MailPath mailPath, MailServletInterface mailInterface) throws OXException {
        MailMessage mailMessage = mailInterface.getMessage(mailPath.getFolderArgument(), mailPath.getMailID(), false);
        if (null == mailMessage) {
            return Optional.empty();
        }
        return Optional.of(mailMessage);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final String CAPABILITY_SHARE_MAIL_ATTACHMENTS = "share_mail_attachments";

    private boolean mayShareAttachments(Session session) throws OXException {
        CapabilityService capabilityService = services.getOptionalService(CapabilityService.class);
        return null == capabilityService ? false : capabilityService.getCapabilities(session).contains(CAPABILITY_SHARE_MAIL_ATTACHMENTS);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static InternetAddress[] toMimeAddresses(List<Address> addrs) throws OXException {
        if (null == addrs) {
            return null;
        }

        int numberOfAddresses = addrs.size();
        switch (numberOfAddresses) {
            case 0:
                return new InternetAddress[0];
            case 1: {
                Address address = addrs.get(0);
                return address == null ? new InternetAddress[0] : new InternetAddress[] { toMimeAddress(address) };
            }
            default: {
                List<InternetAddress> mimeAddresses = new ArrayList<>(numberOfAddresses);
                for (Address address : addrs) {
                    InternetAddress mimeAddress = toMimeAddress(address);
                    if (null != mimeAddress) {
                        mimeAddresses.add(mimeAddress);
                    }
                }
                return mimeAddresses.toArray(new InternetAddress[mimeAddresses.size()]);
            }
        }
    }

    private static InternetAddress toMimeAddress(Address a) throws OXException {
        if (null == a) {
            return null;
        }
        try {
            QuotedInternetAddress mimeAddress = new QuotedInternetAddress(a.getAddress(), true);
            mimeAddress.setPersonal(a.getPersonal(), "UTF-8");
            return mimeAddress;
        } catch (UnsupportedEncodingException e) {
            // Nah...
            throw OXException.general("UTF-8 charset not available", e);
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static String generateAttachmentIdListing(List<Attachment> attachments) {
        if (null == attachments) {
            return "null";
        }

        int size = attachments.size();
        if (size <= 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder(size << 4);
        sb.append('[');
        Iterator<Attachment> it = attachments.iterator();
        sb.append(getUnformattedString(it.next().getId()));
        while (it.hasNext()) {
            sb.append(", ").append(getUnformattedString(it.next().getId()));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String toStringFor(List<UUID> uuids) {
        if (null == uuids) {
            return "null";
        }

        int size = uuids.size();
        if (size <= 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder((size << 4) + 32);
        sb.append('[');
        Iterator<UUID> it = uuids.iterator();
        sb.append(getUnformattedString(it.next()));
        while (it.hasNext()) {
            sb.append(", ").append(getUnformattedString(it.next()));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Performs a wait according to exponential back-off strategy.
     * <pre>
     * (retry-count * base-millis) + random-millis
     * </pre>
     *
     * @param retryCount The current number of retries
     * @param baseMillis The base milliseconds
     */
    private static void exponentialBackoffWait(int retryCount, long baseMillis) {
        long nanosToWait = TimeUnit.NANOSECONDS.convert((retryCount * baseMillis) + ((long) (Math.random() * baseMillis)), TimeUnit.MILLISECONDS);
        LockSupport.parkNanos(nanosToWait);
    }

}
