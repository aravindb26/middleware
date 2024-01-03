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

package com.openexchange.mail.compose.mailstorage.open;

import static com.openexchange.mail.mime.InternetAddressFilter.filter;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.parseAddressList;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.unfold;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.html.HtmlSanitizeOptions;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.api.FromAddressProvider;
import com.openexchange.mail.compose.Attachment;
import com.openexchange.mail.compose.AttachmentDescription;
import com.openexchange.mail.compose.AttachmentStorages;
import com.openexchange.mail.compose.CompositionSpaces;
import com.openexchange.mail.compose.ContentId;
import com.openexchange.mail.compose.DefaultAttachment;
import com.openexchange.mail.compose.Message;
import com.openexchange.mail.compose.OpenCompositionSpaceParameters;
import com.openexchange.mail.compose.mailstorage.MailStorageCompositionSpaceImageDataSource;
import com.openexchange.mail.compose.mailstorage.ThresholdFileHolderDataProvider;
import com.openexchange.mail.compose.mailstorage.ThresholdFileHolderFactory;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.InternetAddressFilter;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.processing.MimeProcessingUtility;
import com.openexchange.mail.mime.processing.TextAndContentType;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.InlineContentHandler;
import com.openexchange.mail.parser.handlers.NonInlineForwardPartHandler;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.mailaccount.UnifiedInboxUID;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * {@link Reply} - Utility class to open a composition space for a reply.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class Reply extends AbstractOpener {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Reply.class);
    }

    /**
     * Initializes a new {@link Reply}.
     *
     * @param services The service look-up
     */
    public Reply(ServiceLookup services) {
        super(services);
    }

    private static final String PREFIX_RE = "Re: ";

    /**
     * Prepares opening a composition space for a reply.
     *
     * @param replyAll Whether a reply to all recipients should be prepared
     * @param parameters The parameters
     * @param state The state
     * @param session The session
     * @throws OXException If an Open-Xchange error occurs
     * @throws MessagingException If a messaging error occurs
     */
    public void doOpenForReply(boolean replyAll, OpenCompositionSpaceParameters parameters, OpenState state, Session session) throws OXException, MessagingException {
        MailPath replyFor = parameters.getReferencedMails().get(0);
        state.metaBuilder.withReplyFor(replyFor);
        state.mailInterface = MailServletInterface.getInstanceWithDecryptionSupport(session, null);
        MailMessage originalMail = requireMailMessage(replyFor, state.mailInterface);
        state.metaBuilder.withDate(originalMail.getSentDate());
        state.metaBuilder.withTimeZoneId(CompositionSpaces.optSentDateTimeZoneId(originalMail).orElse(null));

        Context context = getContext(session);
        UserSettingMail usm = parameters.getMailSettings();

        int accountId = replyFor.getAccountId();

        // Check if original message's "From" address is session-associated user
        Optional<InternetAddress> optionalValidatedFrom = MimeProcessingUtility.validateFrom(originalMail, accountId, session, context);
        boolean preferToAsRecipient = optionalValidatedFrom.isPresent();

        // Reply, pre-set subject
        String origSubject = originalMail.getSubject();
        if (Strings.isEmpty(origSubject)) {
            state.message.setSubject(PREFIX_RE);
        } else {
            if (origSubject.regionMatches(true, 0, PREFIX_RE, 0, PREFIX_RE.length())) {
                state.message.setSubject(origSubject);
            } else {
                state.message.setSubject(new StringBuilder(PREFIX_RE.length() + origSubject.length()).append(PREFIX_RE).append(origSubject).toString());
            }
        }

        // Determine "From"
        InternetAddress from = preferToAsRecipient && optionalValidatedFrom.isPresent() ? optionalValidatedFrom.get() : null;

        // Set "From" address
        if (from == null) {
            FromAddressProvider fromAddressProvider = FromAddressProvider.byAccountId();
            if (null != fromAddressProvider) {
                if (fromAddressProvider.isDetectBy()) {
                    from = MimeProcessingUtility.determinePossibleFrom(false, originalMail, accountId, session, context);
                    /*
                     * Set if a "From" candidate applies
                     */
                    if (null != from) {
                        state.message.setFrom(toAddress(from, false));
                    }
                } else if (fromAddressProvider.isSpecified()) {
                    from = fromAddressProvider.getFromAddress();
                    if (null != from) {
                        state.message.setFrom(toAddress(from, false));
                    }
                }
            }
        } else {
            state.message.setFrom(toAddress(from, false));
        }

        /*
         * Set the appropriate recipients. Taken from RFC 822 section 4.4.4: If the "Reply-To" field exists, then the reply should go to
         * the addresses indicated in that field and not to the address(es) indicated in the "From" field.
         */
        InternetAddress[] recipientAddrs;
        if (preferToAsRecipient) {
            String[] replyTo = originalMail.getHeader(MessageHeaders.HDR_REPLY_TO);
            if (MimeMessageUtility.isEmptyHeader(replyTo)) {
                recipientAddrs = originalMail.getTo();
            } else {
                /*
                 * Message holds header 'Reply-To'
                 */
                List<InternetAddress> toList = Arrays.asList(MimeMessageUtility.getAddressHeader(unfold(replyTo[0])));
                recipientAddrs = toList.toArray(new InternetAddress[toList.size()]);
            }
        } else {
            Set<InternetAddress> tmpSet = new LinkedHashSet<InternetAddress>(4);
            boolean fromAdded;
            {
                String[] replyTo = originalMail.getHeader(MessageHeaders.HDR_REPLY_TO);
                if (MimeMessageUtility.isEmptyHeader(replyTo)) {
                    String owner = MimeProcessingUtility.getFolderOwnerIfShared(replyFor.getFolder(), replyFor.getAccountId(), session);
                    if (null != owner) {
                        final User[] users = UserStorage.getInstance().searchUserByMailLogin(owner, context);
                        if (null != users && users.length > 0) {
                            InternetAddress onBehalfOf;
                            {
                                UserSettingMailStorage settingMailStorage = UserSettingMailStorage.getInstance();
                                Optional<String> optSenderAddress = settingMailStorage.getSenderAddress(users[0].getId(), context, null);
                                if (optSenderAddress.isPresent()) {
                                    onBehalfOf = new QuotedInternetAddress(optSenderAddress.get(), false);
                                } else {
                                    onBehalfOf = new QuotedInternetAddress(users[0].getMail(), false);
                                }
                            }
                            state.message.setFrom(toAddress(onBehalfOf, false));
                            QuotedInternetAddress sender = new QuotedInternetAddress(usm.getSendAddr(), false);
                            state.message.setSender(toAddress(sender, false));
                        }
                    }
                    /*
                     * Set from as recipient
                     */
                    tmpSet.addAll(Arrays.asList(originalMail.getFrom()));
                    fromAdded = true;
                } else {
                    /*
                     * Message holds header 'Reply-To'
                     */
                    tmpSet.addAll(Arrays.asList(MimeMessageUtility.getAddressHeader(unfold(replyTo[0]))));
                    fromAdded = false;
                }
            }
            if (replyAll) {
                /*-
                 * Check 'From' has been added
                 */
                if (!fromAdded) {
                    tmpSet.addAll(Arrays.asList(originalMail.getFrom()));
                }
            }
            recipientAddrs = tmpSet.toArray(new InternetAddress[tmpSet.size()]);
        }

        if (replyAll) {
            /*
             * Create a filter which is used to sort out addresses before adding them to either field 'To' or 'Cc'
             */
            InternetAddressFilter filter = new InternetAddressFilter();
            if (null != from) {
                filter.add(from);
            }
            /*
             * Add user's address to filter
             */
            if (accountId == Account.DEFAULT_ID) {
                MimeProcessingUtility.addUserAliases(filter, session, context);
            } else {
                // Check for Unified Mail account
                UnifiedInboxManagement management = services.getService(UnifiedInboxManagement.class);
                if (null == management) {
                    MailAccountStorageService mass = services.getService(MailAccountStorageService.class);
                    if (null == mass) {
                        MimeProcessingUtility.addUserAliases(filter, session, context);
                    } else {
                        String primaryAddress = mass.getMailAccount(accountId, session.getUserId(), session.getContextId()).getPrimaryAddress();
                        if (primaryAddress.indexOf(UnifiedInboxManagement.MAIL_ADDRESS_DOMAIN_PART) > 0) {
                            int realAccountId;
                            try {
                                UnifiedInboxUID uid = new UnifiedInboxUID(originalMail.getMailId());
                                realAccountId = uid.getAccountId();
                            } catch (OXException e) {
                                // No Unified Mail identifier
                                LoggerHolder.LOG.trace("", e);
                                FullnameArgument fa = UnifiedInboxUID.parsePossibleNestedFullName(originalMail.getFolder());
                                realAccountId = null == fa ? Account.DEFAULT_ID : fa.getAccountId();
                            }

                            if (realAccountId == Account.DEFAULT_ID) {
                                MimeProcessingUtility.addUserAliases(filter, session, context);
                            } else {
                                filter.add(new QuotedInternetAddress(mass.getMailAccount(realAccountId, session.getUserId(), session.getContextId()).getPrimaryAddress(), false));
                            }
                        } else {
                            filter.add(new QuotedInternetAddress(primaryAddress, false));
                        }
                    }
                } else {
                    if (accountId == management.getUnifiedINBOXAccountID(session)) {
                        int realAccountId;
                        try {
                            UnifiedInboxUID uid = new UnifiedInboxUID(originalMail.getMailId());
                            realAccountId = uid.getAccountId();
                        } catch (OXException e) {
                            // No Unified Mail identifier
                            LoggerHolder.LOG.trace("", e);
                            FullnameArgument fa = UnifiedInboxUID.parsePossibleNestedFullName(originalMail.getFolder());
                            realAccountId = null == fa ? Account.DEFAULT_ID : fa.getAccountId();
                        }

                        if (realAccountId == Account.DEFAULT_ID) {
                            MimeProcessingUtility.addUserAliases(filter, session, context);
                        } else {
                            MailAccountStorageService mass = services.getService(MailAccountStorageService.class);
                            if (null == mass) {
                                MimeProcessingUtility.addUserAliases(filter, session, context);
                            } else {
                                filter.add(new QuotedInternetAddress(mass.getMailAccount(realAccountId, session.getUserId(), session.getContextId()).getPrimaryAddress(), false));
                            }
                        }
                    } else {
                        MailAccountStorageService mass = services.getService(MailAccountStorageService.class);
                        if (null == mass) {
                            MimeProcessingUtility.addUserAliases(filter, session, context);
                        } else {
                            filter.add(new QuotedInternetAddress(mass.getMailAccount(accountId, session.getUserId(), session.getContextId()).getPrimaryAddress(), false));
                        }
                    }
                }
            }
            /*
             * Determine if other original recipients should be added to 'Cc'.
             */
            final boolean replyallcc = usm.isReplyAllCc();
            /*
             * Filter the recipients of 'Reply-To'/'From' field
             */
            final Set<InternetAddress> filteredAddrs = filter(filter, recipientAddrs);
            /*
             * Add filtered recipients from 'To' field
             */
            String hdrVal = originalMail.getHeader(MessageHeaders.HDR_TO, MessageHeaders.HDR_ADDR_DELIM);
            InternetAddress[] toAddrs = null;
            if (hdrVal != null) {
                filteredAddrs.addAll(filter(filter, (toAddrs = parseAddressList(hdrVal, true))));
            }
            /*
             * ... and add filtered addresses to either 'To' or 'Cc' field
             */
            if (!filteredAddrs.isEmpty()) {
                if (replyallcc) {
                    // Put original sender into 'To'
                    state.message.setTo(toAddresses(recipientAddrs));
                    // All other into 'Cc'
                    filteredAddrs.removeAll(Arrays.asList(recipientAddrs));
                    state.message.setCc(toAddresses(filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()])));
                } else {
                    state.message.setTo(toAddresses(filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()])));
                }
            } else if (toAddrs != null) {
                final Set<InternetAddress> tmpSet = new HashSet<InternetAddress>(Arrays.asList(recipientAddrs));
                tmpSet.removeAll(Arrays.asList(toAddrs));
                if (tmpSet.isEmpty()) {
                    /*
                     * The message was sent from the user to himself. In this special case allow user's own address in field 'To' to
                     * avoid an empty 'To' field
                     */
                    state.message.setTo(toAddresses(recipientAddrs));
                }
            }
            /*
             * Filter recipients from 'Cc' field
             */
            filteredAddrs.clear();
            hdrVal = originalMail.getHeader(MessageHeaders.HDR_CC, MessageHeaders.HDR_ADDR_DELIM);
            if (hdrVal != null) {
                filteredAddrs.addAll(filter(filter, parseAddressList(unfold(hdrVal), true)));
            }
            if (!filteredAddrs.isEmpty()) {
                state.message.addCc(toAddresses(filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()])));
            }
            /*
             * Filter recipients from 'Bcc' field
             */
            filteredAddrs.clear();
            hdrVal = originalMail.getHeader(MessageHeaders.HDR_BCC, MessageHeaders.HDR_ADDR_DELIM);
            if (hdrVal != null) {
                filteredAddrs.addAll(filter(filter, parseAddressList(unfold(hdrVal), true)));
            }
            if (!filteredAddrs.isEmpty()) {
                state.message.addBcc(toAddresses(filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()])));
            }
        } else {
            /*
             * Plain reply: Just put original sender into 'To' field
             */
            state.message.setTo(toAddresses(recipientAddrs));
        }

        // Check whether to attach original message
        if (usm.getAttachOriginalMessage() > 0) {
            ThresholdFileHolder sink = ThresholdFileHolderFactory.getInstance().createFileHolder(session);
            try {
                originalMail.writeTo(sink.asOutputStream());

                // Compile attachment
                AttachmentDescription attachmentDesc = AttachmentStorages.createAttachmentDescriptionFor(originalMail, 0, sink.getLength(), state.compositionSpaceId);
                DefaultAttachment.Builder attachment = DefaultAttachment.builder(attachmentDesc);
                if (attachmentDesc.getId() == null) {
                    attachment.withId(UUID.randomUUID());
                }
                attachment.withDataProvider(new ThresholdFileHolderDataProvider(sink));

                Attachment emlAttachment = attachment.build();
                state.attachments = new ArrayList<>(1);
                state.attachments.add(emlAttachment);
                sink = null; // Avoid premature closing
            } finally {
                Streams.close(sink);
            }
        }

        {
            Message.ContentType desiredContentType = parameters.getContentType();
            boolean allowHtmlContent = desiredContentType == null ? usm.isDisplayHtmlInlineContent() : desiredContentType.isImpliesHtml();
            TextAndContentType textForReply = usm.isIgnoreOriginalMailTextOnReply() ? null : MimeProcessingUtility.getTextForReply(originalMail, allowHtmlContent, false, session);
            if (null == textForReply) {
                state.message.setContent("");
                state.message.setContentType(desiredContentType == null ? (usm.isDisplayHtmlInlineContent() ? TEXT_HTML : TEXT_PLAIN) : desiredContentType);
            } else {
                state.message.setContent(textForReply.getText());
                state.message.setContentType(textForReply.isHtml() ? (desiredContentType == null || !desiredContentType.isImpliesHtml() ? TEXT_HTML : desiredContentType) : TEXT_PLAIN);
            }
        }

        // Add mail's inline images
        List<String> contentIds = new ArrayList<String>();
        if (TEXT_HTML == state.message.getContentType()) {
            contentIds.addAll(MimeMessageUtility.getContentIDs(state.message.getContent()));

            if (!contentIds.isEmpty()) {
                InlineContentHandler inlineHandler = new InlineContentHandler(contentIds);
                new MailMessageParser().setInlineDetectorBehavior(true).parseMailMessage(originalMail, inlineHandler);
                Map<String, MailPart> inlineParts = inlineHandler.getInlineContents();
                if (null != inlineParts && !inlineParts.isEmpty()) {
                    if (null == state.attachments) {
                        state.attachments = new ArrayList<>(inlineParts.size());
                    }

                    Map<ContentId, Attachment> inlineAttachments = new HashMap<ContentId, Attachment>(inlineParts.size());
                    int i = 0;
                    for (Map.Entry<String, MailPart> inlineEntry : inlineParts.entrySet()) {
                        MailPart mailPart = inlineEntry.getValue();
                        ThresholdFileHolder sink = ThresholdFileHolderFactory.getInstance().createFileHolder(session);
                        try {
                            sink.write(mailPart.getInputStream());

                            // Compile attachment
                            ContentId contentId = ContentId.valueOf(inlineEntry.getKey());
                            AttachmentDescription attachmentDesc = AttachmentStorages.createInlineAttachmentDescriptionFor(mailPart, contentId, i + 1, state.compositionSpaceId);
                            DefaultAttachment.Builder attachment = DefaultAttachment.builder(attachmentDesc);
                            if (attachmentDesc.getId() == null) {
                                attachment.withId(UUID.randomUUID());
                            }
                            attachment.withDataProvider(new ThresholdFileHolderDataProvider(sink));

                            Attachment partAttachment = attachment.build();
                            state.attachments.add(partAttachment);
                            inlineAttachments.put(contentId, partAttachment);
                            sink = null; // Avoid premature closing
                        } finally {
                            Streams.close(sink);
                        }
                        i++;
                    }

                    String content = CompositionSpaces.replaceCidInlineImages(state.message.getContent(), Optional.of(state.compositionSpaceId), inlineAttachments, MailStorageCompositionSpaceImageDataSource.getInstance(), session);
                    if (parameters.isDropExternalImages()) {
                        HtmlSanitizeOptions.Builder optionsBuilder = HtmlSanitizeOptions.builder().setSession(session);
                        optionsBuilder.setDropExternalImages(true).setSanitize(false);
                        content = CompositionSpaces.sanitizeHtmlContent(content, optionsBuilder.build());
                    }
                    state.message.setContent(content);
                }
            } else {
                if (parameters.isDropExternalImages()) {
                    String content = state.message.getContent();
                    HtmlSanitizeOptions.Builder optionsBuilder = HtmlSanitizeOptions.builder().setSession(session);
                    optionsBuilder.setDropExternalImages(true).setSanitize(false);
                    content = CompositionSpaces.sanitizeHtmlContent(content, optionsBuilder.build());
                    state.message.setContent(content);
                }
            }
        }

        if (parameters.isAppendOriginalAttachments()) {
            // Add mail's non-inline parts
            NonInlineForwardPartHandler handler = new NonInlineForwardPartHandler();
            if (false == contentIds.isEmpty()) {
                handler.setImageContentIds(contentIds);
            }
            new MailMessageParser().setInlineDetectorBehavior(true).parseMailMessage(originalMail, handler);
            List<MailPart> nonInlineParts = handler.getNonInlineParts();
            if (null != nonInlineParts && !nonInlineParts.isEmpty()) {
                // Obtain attachment storage
                if (null == state.attachments) {
                    state.attachments = new ArrayList<>(nonInlineParts.size());
                }

                int i = state.attachments.size();
                for (MailPart mailPart : nonInlineParts) {
                    ThresholdFileHolder sink = ThresholdFileHolderFactory.getInstance().createFileHolder(session);
                    try {
                        sink.write(mailPart.getInputStream());

                        // Compile attachment
                        AttachmentDescription attachmentDesc = AttachmentStorages.createAttachmentDescriptionFor(mailPart, i + 1, sink.getLength(), state.compositionSpaceId, session);
                        DefaultAttachment.Builder attachment = DefaultAttachment.builder(attachmentDesc);
                        if (attachmentDesc.getId() == null) {
                            attachment.withId(UUID.randomUUID());
                        }
                        attachment.withDataProvider(new ThresholdFileHolderDataProvider(sink));

                        Attachment partAttachment = attachment.build();
                        state.attachments.add(partAttachment);
                        sink = null; // Avoid premature closing
                    } finally {
                        Streams.close(sink);
                    }
                    i++;
                }
            }
        }
    }

}
