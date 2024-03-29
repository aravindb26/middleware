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

package com.openexchange.mail.mime.processing;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Strings.asciiLowerCase;
import static com.openexchange.java.Strings.isEmpty;
import static com.openexchange.mail.mime.InternetAddressFilter.filter;
import static com.openexchange.mail.mime.filler.MimeMessageFiller.setReplyHeaders;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.parseAddressList;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.unfold;
import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message.RecipientType;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.html.HtmlService;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.image.ImageLocation;
import com.openexchange.java.CharsetDetector;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.mail.api.FromAddressProvider;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.conversion.InlineImageDataSource;
import com.openexchange.mail.dataobjects.CompositeMailMessage;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.InternetAddressFilter;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeSmilFixer;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.MimeMessageConverter;
import com.openexchange.mail.mime.dataobjects.MimeMailMessage;
import com.openexchange.mail.mime.dataobjects.NestedMessageMailPart;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.InlineContentHandler;
import com.openexchange.mail.text.HtmlProcessing;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.mailaccount.UnifiedInboxUID;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.regex.MatcherReplacer;
import com.openexchange.user.User;

/**
 * {@link MimeReply} - MIME message reply.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MimeReply extends AbstractMimeProcessing {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MimeReply.class);

    private static final String PREFIX_RE = "Re: ";

    /**
     * No instantiation.
     */
    private MimeReply() {
        super();
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(MailMessage originalMail, boolean replyAll, Session session, int accountId) throws OXException {
        return getReplyMail(originalMail, replyAll, session, accountId, null, false);
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param setFrom <code>true</code> to set 'From' header; otherwise <code>false</code> to leave it
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(MailMessage originalMail, boolean replyAll, Session session, int accountId, boolean setFrom) throws OXException {
        return getReplyMail(originalMail, replyAll, session, accountId, null, setFrom);
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param usm The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(MailMessage originalMail, boolean replyAll, Session session, int accountId, UserSettingMail usm) throws OXException {
        return getReplyMail(originalMail, replyAll, session, accountId, usm, false);
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param usm The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @param setFrom <code>true</code> to set 'From' header; otherwise <code>false</code> to leave it
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(MailMessage originalMail, boolean replyAll, Session session, int accountId, UserSettingMail usm, boolean setFrom) throws OXException {
        return getReplyMail(originalMail, replyAll, session, accountId, usm, setFrom ? FromAddressProvider.byAccountId() : FromAddressProvider.none());
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMail The referenced original mail
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param usm The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @param fromAddressProvider The provider for <code>"From"</code> address
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    public static MailMessage getReplyMail(MailMessage originalMail, boolean replyAll, Session session, int accountId, UserSettingMail usm, FromAddressProvider fromAddressProvider) throws OXException {
        boolean preferToAsRecipient = false;
        final String originalMailFolder = originalMail.getFolder();
        MailPath msgref = null;
        if (originalMail.getMailId() != null && originalMailFolder != null) {
            msgref = new MailPath(accountId, originalMailFolder, originalMail.getMailId());
            /*
             * Properly set preferToAsRecipient dependent on whether original mail's folder denotes the default sent folder or drafts folder
             */
            final String[] arr =
                MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderArray());
            if (arr == null) {
                MailAccess<?, ?> mailAccess = null;
                try {
                    mailAccess = MailAccess.getInstance(session, accountId);
                    mailAccess.connect();
                    final IMailFolderStorage folderStorage = mailAccess.getFolderStorage();
                    preferToAsRecipient =
                        originalMailFolder.equals(folderStorage.getSentFolder()) || originalMailFolder.equals(folderStorage.getDraftsFolder());
                } finally {
                    if (null != mailAccess) {
                        mailAccess.close(false);
                    }
                }
            } else {
                preferToAsRecipient =
                    originalMailFolder.equals(arr[StorageUtility.INDEX_SENT]) || originalMailFolder.equals(arr[StorageUtility.INDEX_DRAFTS]);
            }
        }
        return getReplyMail(
            originalMail,
            msgref,
            replyAll,
            preferToAsRecipient,
            session,
            accountId,
            MimeDefaultSession.getDefaultSession(),
            usm, fromAddressProvider);
    }

    private static final Pattern PAT_META_CT = Pattern.compile("<meta[^>]*?http-equiv=\"?content-type\"?[^>]*?>", Pattern.CASE_INSENSITIVE);

    private static String replaceMetaEquiv(String html, ContentType contentType) {
        final Matcher m = PAT_META_CT.matcher(html);
        final MatcherReplacer mr = new MatcherReplacer(m, html);
        final StringBuilder replaceBuffer = new StringBuilder(html.length());
        if (m.find()) {
            replaceBuffer.append("<meta http-equiv=\"Content-Type\" content=\"").append(Strings.toLowerCase(contentType.getBaseType()));
            replaceBuffer.append("; charset=").append(contentType.getCharsetParameter()).append("\" />");
            final String replacement = replaceBuffer.toString();
            replaceBuffer.setLength(0);
            mr.appendLiteralReplacement(replaceBuffer, replacement);
        }
        mr.appendTail(replaceBuffer);
        return replaceBuffer.toString();
    }

    /**
     * Composes a reply message from specified original message based on MIME objects from <code>JavaMail</code> API.
     *
     * @param originalMsg The referenced original message
     * @param msgref The message reference
     * @param replyAll <code>true</code> to reply to all; otherwise <code>false</code>
     * @param preferToAsRecipient <code>true</code> to prefer header 'To' as recipient; otherwise <code>false</code> to prefer 'Reply-To'
     * @param session The session containing needed user data
     * @param accountId The account ID
     * @param mailSession The mail session
     * @param userSettingMail The user mail settings to use; leave to <code>null</code> to obtain from specified session
     * @param fromAddressProvider The provider for <code>"From"</code> address
     * @return An instance of {@link MailMessage} representing an user-editable reply mail
     * @throws OXException If reply mail cannot be composed
     */
    private static MailMessage getReplyMail(MailMessage originalMsg, MailPath msgref, boolean replyAll, boolean preferToAsRecipient, Session session, int accountId, javax.mail.Session mailSession, UserSettingMail userSettingMail, FromAddressProvider fromAddressProvider) throws OXException {
        try {
            originalMsg.setAccountId(accountId);
            MailMessage origMsg;
            {
                final ContentType contentType = originalMsg.getContentType();
                if (contentType.startsWith("multipart/related") && ("application/smil".equals(asciiLowerCase(contentType.getParameter("type"))))) {
                    origMsg = MimeSmilFixer.getInstance().process(originalMsg);
                } else {
                    origMsg = originalMsg;
                }
            }
            Context ctx = ContextStorage.getStorageContext(session.getContextId());
            UserSettingMail usm = userSettingMail == null ? UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx) : userSettingMail;
            /*
             * New MIME message with a dummy session
             */
            final MimeMessage replyMsg = new MimeMessage(MimeDefaultSession.getDefaultSession());
            /*
             * Set headers of reply message
             */
            {
                String rawSubject;
                {
                    String subjectHdrValue = MimeMessageUtility.checkNonAscii(origMsg.getHeader(MessageHeaders.HDR_SUBJECT, null));
                    if (subjectHdrValue == null) {
                        rawSubject = "";
                    } else {
                        rawSubject = unfold(subjectHdrValue);
                    }
                }
                String decodedSubject = MimeMessageUtility.decodeMultiEncodedHeader(rawSubject);
                String newSubject;
                if (decodedSubject.regionMatches(true, 0, PREFIX_RE, 0, 4)) {
                    newSubject = decodedSubject;
                } else {
                    newSubject = new StringBuilder().append(PREFIX_RE).append(decodedSubject).toString();
                }
                replyMsg.setSubject(newSubject, MailProperties.getInstance().getDefaultMimeCharset());
            }
            /*
             * Set "From"
             */
            InternetAddress from = null;
            if (null != fromAddressProvider) {
                if (fromAddressProvider.isDetectBy()) {
                    from = MimeProcessingUtility.determinePossibleFrom(false, origMsg, accountId, session, ctx);
                    /*
                     * Set if a "From" candidate applies
                     */
                    if (null != from) {
                        replyMsg.setFrom(from);
                    }
                } else if (fromAddressProvider.isSpecified()) {
                    from = fromAddressProvider.getFromAddress();
                    if (null != from) {
                        replyMsg.setFrom(from);
                    }
                }
            }
            /*
             * Set the appropriate recipients. Taken from RFC 822 section 4.4.4: If the "Reply-To" field exists, then the reply should go to
             * the addresses indicated in that field and not to the address(es) indicated in the "From" field.
             */
            InternetAddress[] recipientAddrs;
            if (preferToAsRecipient) {
                final String hdrVal = origMsg.getHeader(MessageHeaders.HDR_TO, MessageHeaders.HDR_ADDR_DELIM);
                if (null == hdrVal) {
                    recipientAddrs = new InternetAddress[0];
                } else {
                    recipientAddrs = parseAddressList(hdrVal, true);
                }
            } else {
                Set<InternetAddress> tmpSet = new LinkedHashSet<InternetAddress>(4);
                boolean fromAdded;
                {
                    final String[] replyTo = origMsg.getHeader(MessageHeaders.HDR_REPLY_TO);
                    if (MimeMessageUtility.isEmptyHeader(replyTo)) {
                        String owner = null == msgref ? null : MimeProcessingUtility.getFolderOwnerIfShared(msgref.getFolder(), msgref.getAccountId(), session);
                        if (null != owner) {
                            final User[] users = UserStorage.getInstance().searchUserByMailLogin(owner, ctx);
                            if (null != users && users.length > 0) {
                                InternetAddress onBehalfOf = new QuotedInternetAddress(users[0].getMail(), false);
                                replyMsg.setFrom(onBehalfOf);
                                QuotedInternetAddress sender = new QuotedInternetAddress(usm.getSendAddr(), false);
                                replyMsg.setSender(sender);
                            }
                        }
                        /*
                         * Set from as recipient
                         */
                        tmpSet.addAll(Arrays.asList(origMsg.getFrom()));
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
                        tmpSet.addAll(Arrays.asList(origMsg.getFrom()));
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
                    MimeProcessingUtility.addUserAliases(filter, session, ctx);
                } else {
                    // Check for Unified Mail account
                    ServerServiceRegistry registry = ServerServiceRegistry.getInstance();
                    UnifiedInboxManagement management = registry.getService(UnifiedInboxManagement.class);
                    if ((null != management) && (accountId == management.getUnifiedINBOXAccountID(session))) {
                        int realAccountId;
                        try {
                            UnifiedInboxUID uid = new UnifiedInboxUID(origMsg.getMailId());
                            realAccountId = uid.getAccountId();
                        } catch (OXException e) {
                            // No Unified Mail identifier
                            FullnameArgument fa = UnifiedInboxUID.parsePossibleNestedFullName(origMsg.getFolder());
                            realAccountId = null == fa ? Account.DEFAULT_ID : fa.getAccountId();
                        }

                        if (realAccountId == Account.DEFAULT_ID) {
                            MimeProcessingUtility.addUserAliases(filter, session, ctx);
                        } else {
                            MailAccountStorageService mass = registry.getService(MailAccountStorageService.class);
                            if (null == mass) {
                                MimeProcessingUtility.addUserAliases(filter, session, ctx);
                            } else {
                                filter.add(new QuotedInternetAddress(mass.getMailAccount(realAccountId, session.getUserId(), session.getContextId()).getPrimaryAddress(), false));
                            }
                        }
                    } else {
                        MailAccountStorageService mass = registry.getService(MailAccountStorageService.class);
                        if (null == mass) {
                            MimeProcessingUtility.addUserAliases(filter, session, ctx);
                        } else {
                            filter.add(new QuotedInternetAddress(mass.getMailAccount(accountId, session.getUserId(), session.getContextId()).getPrimaryAddress(), false));
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
                String hdrVal = origMsg.getHeader(MessageHeaders.HDR_TO, MessageHeaders.HDR_ADDR_DELIM);
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
                        replyMsg.addRecipients(RecipientType.TO, recipientAddrs);
                        // All other into 'Cc'
                        filteredAddrs.removeAll(Arrays.asList(recipientAddrs));
                        replyMsg.addRecipients(RecipientType.CC, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                    } else {
                        replyMsg.addRecipients(RecipientType.TO, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                    }
                } else if (toAddrs != null) {
                    final Set<InternetAddress> tmpSet = new HashSet<InternetAddress>(Arrays.asList(recipientAddrs));
                    tmpSet.removeAll(Arrays.asList(toAddrs));
                    if (tmpSet.isEmpty()) {
                        /*
                         * The message was sent from the user to himself. In this special case allow user's own address in field 'To' to
                         * avoid an empty 'To' field
                         */
                        replyMsg.addRecipients(RecipientType.TO, recipientAddrs);
                    }
                }
                /*
                 * Filter recipients from 'Cc' field
                 */
                filteredAddrs.clear();
                hdrVal = origMsg.getHeader(MessageHeaders.HDR_CC, MessageHeaders.HDR_ADDR_DELIM);
                if (hdrVal != null) {
                    filteredAddrs.addAll(filter(filter, parseAddressList(unfold(hdrVal), true)));
                }
                if (!filteredAddrs.isEmpty()) {
                    replyMsg.addRecipients(RecipientType.CC, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                }
                /*
                 * Filter recipients from 'Bcc' field
                 */
                filteredAddrs.clear();
                hdrVal = origMsg.getHeader(MessageHeaders.HDR_BCC, MessageHeaders.HDR_ADDR_DELIM);
                if (hdrVal != null) {
                    filteredAddrs.addAll(filter(filter, parseAddressList(unfold(hdrVal), true)));
                }
                if (!filteredAddrs.isEmpty()) {
                    replyMsg.addRecipients(RecipientType.BCC, filteredAddrs.toArray(new InternetAddress[filteredAddrs.size()]));
                }
            } else {
                /*
                 * Plain reply: Just put original sender into 'To' field
                 */
                replyMsg.addRecipients(RecipientType.TO, recipientAddrs);
            }
            /*
             * Check whether to attach original message
             */
            if (usm.getAttachOriginalMessage() > 0) {
                /*
                 * Append original message
                 */
                final CompositeMailMessage compositeMail;
                {
                    final Multipart multipart = new MimeMultipart();
                    /*
                     * Add empty text content as message's body
                     */
                    final MimeBodyPart textPart = new MimeBodyPart();
                    MessageUtility.setText("", MailProperties.getInstance().getDefaultMimeCharset(), "plain", textPart);
                    // textPart.setText("", MailProperties.getInstance().getDefaultMimeCharset(), "plain");
                    textPart.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                    textPart.setHeader(
                        MessageHeaders.HDR_CONTENT_TYPE,
                        Strings.replaceSequenceWith(MimeTypes.MIME_TEXT_PLAIN_TEMPL, "#CS#", MailProperties.getInstance().getDefaultMimeCharset()));
                    multipart.addBodyPart(textPart);
                    MessageUtility.setContent(multipart, replyMsg);
                    // forwardMsg.setContent(multipart);
                    replyMsg.saveChanges();
                    // Remove generated Message-Id header
                    replyMsg.removeHeader(MessageHeaders.HDR_MESSAGE_ID);
                    compositeMail = new CompositeMailMessage(MimeMessageConverter.convertMessage(replyMsg));
                }
                // Attach original message
                {
                    final MailMessage nested;
                    if (originalMsg instanceof MimeMailMessage) {
                        nested = MimeMessageConverter.convertMessage(((MimeMailMessage) originalMsg).getMimeMessage());
                    } else {
                        nested = MimeMessageConverter.convertMessage(new MimeMessage(MimeDefaultSession.getDefaultSession(), MimeMessageUtility.getStreamFromMailPart(originalMsg)));
                    }
                    nested.setMsgref(originalMsg.getMailPath());
                    compositeMail.addAdditionalParts(new NestedMessageMailPart(nested));
                }
                // Return
                return compositeMail;
            }
            /*
             * Set mail text of reply message
             */
            if (usm.isIgnoreOriginalMailTextOnReply()) {
                /*
                 * Add empty text content as message's body
                 */
                MessageUtility.setText("", MailProperties.getInstance().getDefaultMimeCharset(), replyMsg);
                // replyMsg.setText("", MailProperties.getInstance().getDefaultMimeCharset(), "plain");
                replyMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                ContentType newContentType = new ContentType().setPrimaryType("text").setSubType("plain").setCharsetParameter(MailProperties.getInstance().getDefaultMimeCharset());
                newContentType.setParameter("nature", "virtual");
                replyMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(newContentType.toString()));
                final MailMessage replyMail = MimeMessageConverter.convertMessage(replyMsg);
                if (null != msgref) {
                    replyMail.setMsgref(msgref);
                }
                return replyMail;
            }
            /*
             * Add reply text
             */
            final ContentType retvalContentType = new ContentType();
            String replyText;
            {
                final List<String> list = new LinkedList<String>();
                final User user = UserStorage.getInstance().getUser(session.getUserId(), ctx);
                final Locale locale = user.getLocale();
                final LocaleAndTimeZone ltz = new LocaleAndTimeZone(locale, user.getTimeZone());
                generateReplyText(origMsg, retvalContentType, StringHelper.valueOf(locale), ltz, usm, session, accountId, list);

                final StringBuilder replyTextBuilder = new StringBuilder(8192 << 1);
                for (int i = list.size() - 1; i >= 0; i--) {
                    replyTextBuilder.append(list.get(i));
                }
                if (retvalContentType.getPrimaryType() == null) {
                    retvalContentType.setContentType(MimeTypes.MIME_TEXT_PLAIN);
                }
                {
                    final String cs = retvalContentType.getCharsetParameter();
                    if (cs == null || "US-ASCII".equalsIgnoreCase(cs) || !CharsetDetector.isValid(cs) || MessageUtility.isSpecialCharset(cs)) {
                        // Select default charset
                        retvalContentType.setCharsetParameter(MailProperties.getInstance().getDefaultMimeCharset());
                    }
                }
                if (replyTextBuilder.length() <= 0) {
                    /*
                     * No reply text found at all
                     */
                    retvalContentType.setParameter("nature", "virtual");
                    String replyPrefix = generatePrefixText(MailStrings.REPLY_PREFIX, ltz, origMsg, session);
                    boolean isHtml = retvalContentType.startsWith(TEXT_HTM);
                    if (isHtml) {
                        replyPrefix = HtmlProcessing.htmlFormat(new StringBuilder(replyPrefix.length() + 1).append(replyPrefix).append('\n').append('\n').toString());
                    } else {
                        replyPrefix = new StringBuilder(replyPrefix.length() + 1).append(replyPrefix).append('\n').append('\n').toString();
                    }
                    replyTextBuilder.append(replyPrefix);
                }
                replyText = replyTextBuilder.toString();
            }
            /*
             * Compose reply mail
             */
            final MailMessage replyMail;
            /*-
             * Withhold inline images. Those images are inserted through image service framework on message transport
             *
            if (retvalContentType.isMimeType(MIMETypes.MIME_TEXT_HTM_ALL) && MIMEMessageUtility.hasEmbeddedImages(replyText)) {
                // Prepare to append inline content
                final Multipart multiRelated = new MimeMultipart("related");
                {
                    final MimeBodyPart text = new MimeBodyPart();
                    text.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType());
                    text.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                    text.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.fold(14, retvalContentType.toString()));
                    multiRelated.addBodyPart(text);
                }
                replyMsg.setContent(multiRelated);
                replyMsg.saveChanges();
                replyMail = new CompositeMailMessage(MIMEMessageConverter.convertMessage(replyMsg));
                // Append inline content
                appendInlineContent(originalMsg, (CompositeMailMessage) replyMail, MIMEMessageUtility.getContentIDs(replyText));
            } else {
                // Set message's content directly to reply text
                replyMsg.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType());
                replyMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
                replyMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMEMessageUtility.fold(14, retvalContentType.toString()));
                replyMsg.saveChanges();
                replyMail = MIMEMessageConverter.convertMessage(replyMsg);
            }
             */
            /*
             * Set message's content directly to reply text
             */
            if (retvalContentType.startsWith("text/htm")) {
                retvalContentType.setCharsetParameter("UTF-8");
                replyText = replaceMetaEquiv(replyText, retvalContentType);
                HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                if (htmlService != null) {
                    replyText = htmlService.checkBaseTag(replyText, true);
                }
            }
            MessageUtility.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType(), replyMsg);
            // replyMsg.setText(replyText, retvalContentType.getCharsetParameter(), retvalContentType.getSubType());
            replyMsg.setHeader(MessageHeaders.HDR_MIME_VERSION, "1.0");
            replyMsg.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MimeMessageUtility.foldContentType(retvalContentType.toString()));
            replyMsg.saveChanges();
            // Remove generated Message-Id for template message
            replyMsg.removeHeader(MessageHeaders.HDR_MESSAGE_ID);
            setReplyHeaders(origMsg, replyMsg, I(MailProperties.getInstance().getMaxLengthForReferencesHeader(session.getUserId(), session.getContextId())));
            replyMail = MimeMessageConverter.convertMessage(replyMsg);
            if (null != msgref) {
                replyMail.setMsgref(msgref);
            }
            // Copy security setting
            replyMail.setSecurityResult(originalMsg.getSecurityResult());
            // Copy authenticity setting
            replyMail.setAuthenticityResult(originalMsg.getAuthenticityResult());

            return replyMail;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (IOException e) {
            if ((e instanceof com.sun.mail.util.MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }

    }

    private static void appendInlineContent(MailMessage originalMail, CompositeMailMessage replyMail, List<String> cids) throws OXException {
        final InlineContentHandler handler = new InlineContentHandler(cids);
        new MailMessageParser().parseMailMessage(originalMail, handler);
        final Map<String, MailPart> inlineContents = handler.getInlineContents();
        for (String cid : cids) {
            final MailPart part = inlineContents.get(cid);
            if (null != part) {
                replyMail.addAdditionalParts(part);
            }
        }
    }

    private static final String MULTIPART = "multipart/";

    private static final String TEXT = "text/";

    private static final String TEXT_HTM = "text/htm";

    // private static final Pattern PATTERN_BODY = Pattern.compile("<body[^>]*?>", Pattern.CASE_INSENSITIVE);

    /**
     * Gathers all text bodies and appends them to given text builder.
     *
     * @param msg The root message
     * @param retvalContentType The return value's content type
     * @param strHelper The i18n string helper
     * @return <code>true</code> if any text was found; otherwise <code>false</code>
     * @throws OXException
     * @throws MessagingException
     * @throws IOException
     */
    static boolean generateReplyText(MailMessage msg, ContentType retvalContentType, StringHelper strHelper, LocaleAndTimeZone ltz, UserSettingMail usm, Session session, int accountId, List<String> replyTexts) throws OXException, MessagingException, IOException {
        final StringBuilder textBuilder = new StringBuilder(8192);
        final ContentType contentType = msg.getContentType();
        boolean found = false;
        if (contentType.startsWith(MULTIPART)) {
            final ParameterContainer pc =
                new ParameterContainer(retvalContentType, textBuilder, strHelper, usm, session, msg, ltz, replyTexts);
            found |= gatherAllTextContents(msg, contentType, accountId, pc);
        } else if (contentType.startsWith(TEXT) && !MimeProcessingUtility.isSpecial(contentType.getBaseType())) {
            if (retvalContentType.getPrimaryType() == null) {
                final String text = MimeProcessingUtility.handleInlineTextPart(msg, contentType, usm.isDisplayHtmlInlineContent());
                retvalContentType.setContentType(contentType);
                textBuilder.append(text);
            } else {
                final String text = MimeProcessingUtility.handleInlineTextPart(msg, contentType, usm.isDisplayHtmlInlineContent());
                MimeProcessingUtility.appendRightVersion(retvalContentType, contentType, text, textBuilder);
            }
            found = true;
        }
        if (found && !usm.isDropReplyForwardPrefix()) {
            boolean isHtml = retvalContentType.startsWith(TEXT_HTM);
            String replyPrefix = generatePrefixText(MailStrings.REPLY_PREFIX, ltz, msg, session);
            {
                char nextLine = '\n';
                if (isHtml) {
                    replyPrefix = HtmlProcessing.htmlFormat(new StringBuilder(replyPrefix.length() + 1).append(replyPrefix).append(nextLine).append(nextLine).toString());
                } else {
                    replyPrefix = new StringBuilder(replyPrefix.length() + 1).append(replyPrefix).append(nextLine).append(nextLine).toString();
                }
            }
            if (isHtml) {
                HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                if (null != htmlService) {
                    String wellFormedHtmlDoc = htmlService.getWellFormedHTMLDocument(textBuilder.toString());
                    textBuilder.setLength(0);
                    textBuilder.append(wellFormedHtmlDoc);
                }
            }
            /*-
             * Surround with quote
             *
             * Check whether reply prefix is included in quoted text or is prepended (not quoted)
             */
            final boolean prependReplyPrefx;
            {
                final ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                prependReplyPrefx = null == service ? false : service.getBoolProperty("com.openexchange.mail.prependReplyPrefx", false);
            }

            if (prependReplyPrefx) {
                /*
                 * Prepend to quoted reply text
                 */
                final String replyTextBody;
                if (isHtml) {
                    replyTextBody = citeHtml(textBuilder.toString());
                } else {
                    replyTextBody = citeText(textBuilder.toString());
                }
                textBuilder.setLength(0);
                if (isHtml) {
                    textBuilder.append("<br>");
                } else {
                    textBuilder.append('\n');
                }
                textBuilder.append(replyPrefix);
                textBuilder.append(replyTextBody);
            } else {
                /*
                 * Include in quoted reply text
                 */
                final String replyTextBody;
                if (isHtml) {
                    textBuilder.insert(getBodyTagEndPos(textBuilder), replyPrefix);
                    replyTextBody = citeHtml(textBuilder.toString());
                } else {
                    textBuilder.insert(0, replyPrefix);
                    textBuilder.insert(replyPrefix.length(), '\n');
                    replyTextBody = citeText(textBuilder.toString());
                }
                textBuilder.setLength(0);
                textBuilder.append(replyTextBody);
                if (isHtml) {
                    textBuilder.insert(getBlockquoteTagStartPos(textBuilder), "<br>");
                } else {
                    textBuilder.insert(0, '\n');
                }
            }
        }
        replyTexts.add(textBuilder.toString());
        // parentTextBuilder.append(textBuilder);
        return found;
    }

    private static final Pattern PATTERN_BODY_TAG = Pattern.compile("<body[^>]*?>", Pattern.CASE_INSENSITIVE);
    private static int getBodyTagEndPos(CharSequence textBuilder) {
        final Matcher m = PATTERN_BODY_TAG.matcher(textBuilder);
        return m.find() ? m.end() : 0;
    }

    private static final Pattern PATTERN_BLOCKQUOTE_TAG = Pattern.compile("<blockquote[^>]*?>", Pattern.CASE_INSENSITIVE);
    private static int getBlockquoteTagStartPos(CharSequence textBuilder) {
        final Matcher m = PATTERN_BLOCKQUOTE_TAG.matcher(textBuilder);
        return m.find() ? m.start() : 0;
    }

    /**
     * Gathers all text bodies and appends them to given text builder.
     *
     * @return <code>true</code> if any text was found; otherwise <code>false</code>
     * @throws OXException If a mail error occurs
     * @throws MessagingException If a messaging error occurs
     * @throws IOException If an I/O error occurs
     */
    private static boolean gatherAllTextContents(MailPart multipartPart, ContentType mpContentType, int accountId, ParameterContainer pc) throws OXException, MessagingException, IOException {
        final int count = multipartPart.getEnclosedCount();
        final ContentType partContentType = new ContentType();
        final boolean htmlPreferred = pc.usm.isDisplayHtmlInlineContent();
        boolean found = false;
        if (htmlPreferred && count >= 2 && mpContentType.startsWithAny(MimeTypes.MIME_MULTIPART_ALTERNATIVE, MimeTypes.MIME_MULTIPART_RELATED)) {
            /*
             * Prefer HTML content within multipart/alternative part
             */
            found = getTextContent(true, false, multipartPart, count, partContentType, accountId, pc);
            if (!found) {
                /*
                 * No HTML part found, retry with any text part
                 */
                found = getTextContent(false, false, multipartPart, count, partContentType, accountId, pc);
            }
        } else {
            /*
             * Get any text content
             */
            found = getTextContent(false, !htmlPreferred && mpContentType.startsWithAny(MimeTypes.MIME_MULTIPART_ALTERNATIVE, MimeTypes.MIME_MULTIPART_RELATED), multipartPart, count, partContentType, accountId, pc);
            if (!found) {
                /*
                 * No HTML part found, retry with any text part
                 */
                found = getTextContent(false, false, multipartPart, count, partContentType, accountId, pc);
            }
        }
        /*
         * Look for enclosed messages in any case
         */
        for (int i = count - 1; i >= 0; i--) {
            final MailPart part = multipartPart.getEnclosedMailPart(i);
            if (!"attachment".equals(asciiLowerCase(part.getContentDisposition().getDisposition()))) {
                partContentType.setContentType(part.getContentType());
                if (partContentType.startsWith(MimeTypes.MIME_MESSAGE_RFC822)) {
                    final MailMessage enclosedMsg = (MailMessage) part.getContent();
                    found |= generateReplyText(
                        enclosedMsg,
                        pc.retvalContentType,
                        pc.strHelper,
                        pc.ltz,
                        pc.usm,
                        pc.session,
                        accountId,
                        pc.replyTexts);
                } else if (MimeProcessingUtility.fileNameEndsWith(".eml", part, partContentType)) {
                    /*
                     * Create message from input stream
                     */
                    final InputStream is = MimeMessageUtility.getStreamFromMailPart(part);
                    try {
                        final MailMessage attachedMsg = MimeMessageConverter.convertMessage(is);
                        found |= generateReplyText(
                            attachedMsg,
                            pc.retvalContentType,
                            pc.strHelper,
                            pc.ltz,
                            pc.usm,
                            pc.session,
                            accountId,
                            pc.replyTexts);
                    } finally {
                        Streams.close(is);
                    }
                }
            }
        }
        return found;
    }

    private static boolean getTextContent(boolean preferHTML, boolean avoidHTML, MailPart multipartPart, int count, ContentType partContentType, int accountId, ParameterContainer pc) throws OXException, MessagingException, IOException {
        boolean found = false;
        if (preferHTML) {
            for (int i = 0; !found && i < count; i++) {
                final MailPart part = multipartPart.getEnclosedMailPart(i);
                partContentType.setContentType(part.getContentType());
                if (partContentType.startsWith(TEXT_HTM) && MimeProcessingUtility.isInline(part, partContentType) && !MimeProcessingUtility.isSpecial(partContentType.getBaseType())) {
                    boolean first = true;
                    if (pc.retvalContentType.getPrimaryType() == null) {
                        pc.retvalContentType.setContentType(partContentType);
                        final String charset = MessageUtility.checkCharset(part, partContentType);
                        pc.retvalContentType.setCharsetParameter(charset);
                        final String text = MimeProcessingUtility.readContent(part, charset);
                        pc.textBuilder.append(text);
                    } else {
                        first = false;
                        final String charset = MessageUtility.checkCharset(part, partContentType);
                        partContentType.setCharsetParameter(charset);
                        final String text =
                            MimeProcessingUtility.handleInlineTextPart(part, partContentType, pc.usm.isDisplayHtmlInlineContent());
                        MimeProcessingUtility.appendRightVersion(pc.retvalContentType, partContentType, text, pc.textBuilder);
                    }
                    if (first && multipartPart.getContentType().startsWith("multipart/mixed")) {
                        for (int j = i + 1; j < count; j++) {
                            final MailPart nextPart = multipartPart.getEnclosedMailPart(j);
                            final ContentType nextContentType = nextPart.getContentType();
                            if (nextContentType.startsWith(TEXT_HTM) && MimeProcessingUtility.isInline(nextPart, nextContentType) && !MimeProcessingUtility.isSpecial(nextContentType.getBaseType())) {
                                final String charset = MessageUtility.checkCharset(nextPart, partContentType);
                                final String text = MimeProcessingUtility.readContent(nextPart, charset);
                                pc.textBuilder.append(text);
                            } else if (nextContentType.startsWith("image/") && MimeProcessingUtility.isInline(nextPart, nextContentType)) {
                                final String imageURL;
                                String fileName = nextPart.getFileName();
                                {
                                    final InlineImageDataSource imgSource = InlineImageDataSource.getInstance();
                                    if (null == fileName) {
                                        final String ext = MimeType2ExtMap.getFileExtension(nextContentType.getBaseType());
                                        fileName = new StringBuilder("image").append(j).append('.').append(ext).toString();
                                    }
                                    final ImageLocation imageLocation = new ImageLocation.Builder(fileName).folder(prepareFullname(accountId, pc.origMail.getFolder())).id(pc.origMail.getMailId()).build();
                                    imageURL = imgSource.generateUrl(imageLocation, pc.session);
                                }
                                final String imgTag = "<img src=\"" + imageURL + "&scaleType=contain&width=800\" alt=\"\" style=\"display: block\" id=\"" + fileName + "\">";
                                pc.textBuilder.append(imgTag);
                            }
                        }
                        return true;
                    }
                    found = true;
                } else if (partContentType.startsWith(MULTIPART)) {
                    found |= gatherAllTextContents(part, partContentType, accountId, pc);
                }
            }
            if (found) {
                return true;
            }
        }
        /*
         * Any text/* part
         */
        found = false;
        for (int i = 0; i < count; i++) {
            final MailPart part = multipartPart.getEnclosedMailPart(i);
            partContentType.setContentType(part.getContentType());
            if (partContentType.startsWith(TEXT) && (avoidHTML ? !partContentType.startsWith(TEXT_HTM) : true) && MimeProcessingUtility.isInline(part, partContentType) && !MimeProcessingUtility.isSpecial(partContentType.getBaseType())) {
                boolean first = true;
                if (pc.retvalContentType.getPrimaryType() == null) {
                    String text = MimeProcessingUtility.handleInlineTextPart(part, partContentType, pc.usm.isDisplayHtmlInlineContent());
                    if (isEmpty(text)) {
                        String htmlContent = getHtmlContent(multipartPart, count);
                        if (null != htmlContent) {
                            HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                            text = null == htmlService ? "" : htmlService.html2text(htmlContent, true);
                        }
                    }
                    pc.retvalContentType.setContentType(partContentType);
                    final String charset = MessageUtility.checkCharset(part, partContentType);
                    pc.retvalContentType.setCharsetParameter(charset);
                    pc.textBuilder.append(text);
                } else {
                    first = false;
                    final String charset = MessageUtility.checkCharset(part, partContentType);
                    partContentType.setCharsetParameter(charset);
                    final String text = MimeProcessingUtility.handleInlineTextPart(part, partContentType, pc.usm.isDisplayHtmlInlineContent());
                    MimeProcessingUtility.appendRightVersion(pc.retvalContentType, partContentType, text, pc.textBuilder);
                }
                if (first && multipartPart.getContentType().startsWith("multipart/mixed")) {
                    for (int j = i + 1; j < count; j++) {
                        final MailPart nextPart = multipartPart.getEnclosedMailPart(j);
                        final ContentType nextContentType = nextPart.getContentType();
                        if (nextContentType.startsWith(TEXT) && (avoidHTML ? !nextContentType.startsWith(TEXT_HTM) : true) && MimeProcessingUtility.isInline(nextPart, nextContentType) && !MimeProcessingUtility.isSpecial(nextContentType.getBaseType())) {
                            String text = MimeProcessingUtility.handleInlineTextPart(nextPart, nextContentType, pc.usm.isDisplayHtmlInlineContent());
                            if (text != null && text.length() > 0) {
                                if (pc.retvalContentType.startsWith(TEXT_HTM)) {
                                    if (nextContentType.startsWith(TEXT_HTM)) {
                                        pc.textBuilder.append(text);
                                    } else {
                                        // Don't append non-HTML to HTML
                                    }
                                } else {
                                    if (nextContentType.startsWith(TEXT_HTM)) {
                                        // Don't append HTML to non-HTML
                                    } else {
                                        pc.textBuilder.append(text);
                                    }
                                }
                            }
                        } else if (nextContentType.startsWith("image/") && MimeProcessingUtility.isInline(nextPart, nextContentType) && pc.retvalContentType.startsWith(TEXT_HTM)) {
                            final String imageURL;
                            String fileName = nextPart.getFileName();
                            {
                                final InlineImageDataSource imgSource = InlineImageDataSource.getInstance();
                                if (null == fileName) {
                                    final String ext = MimeType2ExtMap.getFileExtension(nextContentType.getBaseType());
                                    fileName = new StringBuilder("image").append(j).append('.').append(ext).toString();
                                }
                                final ImageLocation imageLocation = new ImageLocation.Builder(fileName).folder(prepareFullname(accountId, pc.origMail.getFolder())).id(pc.origMail.getMailId()).build();
                                imageURL = imgSource.generateUrl(imageLocation, pc.session);
                            }
                            final String imgTag = "<img src=\"" + imageURL + "&scaleType=contain&width=800\" alt=\"\" style=\"display: block\" id=\"" + fileName + "\">";
                            pc.textBuilder.append(imgTag);
                        } else if (nextContentType.startsWith(MULTIPART)) {
                            gatherAllTextContents(nextPart, nextContentType, accountId, pc);
                        }
                    }

                    if (pc.retvalContentType.startsWith(TEXT_HTM)) {
                        HtmlService htmlService = ServerServiceRegistry.getInstance().getService(HtmlService.class);
                        if (htmlService != null) {
                            String compositeHtml = pc.textBuilder.toString();
                            pc.textBuilder.setLength(0);
                            pc.textBuilder.append(htmlService.getWellFormedHTMLDocument(compositeHtml));
                        }
                    }

                    return true;
                }
                found = true;
            } else if (partContentType.startsWith(MULTIPART)) {
                if (!found || !multipartPart.getContentType().startsWith("multipart/alternative")) {
                    found |= gatherAllTextContents(part, partContentType, accountId, pc);
                }
            }
        }
        return found;
    }

    private static String getHtmlContent(MailPart multipartPart, int count) throws OXException, IOException {
        boolean found = false;
        for (int i = 0; !found && i < count; i++) {
            final MailPart part = multipartPart.getEnclosedMailPart(i);
            final ContentType partContentType = part.getContentType();
            if (partContentType.startsWith(TEXT_HTM) && MimeProcessingUtility.isInline(part, partContentType) && !MimeProcessingUtility.isSpecial(partContentType.getBaseType())) {
                final String charset = MessageUtility.checkCharset(part, partContentType);
                return MimeProcessingUtility.readContent(part, charset);
            }
        }
        return null;
    }

    /*-
     * ---------------------------------------- Stuff to cite plain text ----------------------------------------
     */

    private static final Pattern PATTERN_TEXT_CITE = Pattern.compile("^", Pattern.MULTILINE);

    private static String citeText(String textContent) {
        return PATTERN_TEXT_CITE.matcher(textContent).replaceAll("> ");
    }

    /*-
     * ---------------------------------------- Stuff to cite HTML text ----------------------------------------
     */

    private static final Pattern PATTERN_HTML_START = Pattern.compile("<html[^>]*?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_HTML_END = Pattern.compile("</html>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_BODY_START = Pattern.compile("<body[^>]*?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_BODY_END = Pattern.compile("</body>", Pattern.CASE_INSENSITIVE);

    private static final String BLOCKQUOTE_START =
        "<blockquote type=\"cite\" style=\"position: relative; margin-left: 0px; padding-left: 10px; border-left: solid 1px blue;\">\n";

    private static final String BLOCKQUOTE_END = "</blockquote>\n<br>&nbsp;";

    private static String citeHtml(String htmlContent) {
        StringBuffer sb = new StringBuffer(htmlContent.length());

        // Inject blockquote start
        Matcher m = PATTERN_BODY_START.matcher(htmlContent);
        if (m.find()) {
            m.appendReplacement(sb, "$0" + BLOCKQUOTE_START);
            m.appendTail(sb);
        } else {
            m = PATTERN_HTML_START.matcher(htmlContent);
            if (m.find()) {
                m.appendReplacement(sb, "$0" + BLOCKQUOTE_START);
                m.appendTail(sb);
            } else {
                sb.append(BLOCKQUOTE_START);
                sb.append(htmlContent);
            }
        }

        // Inject blockquote end
        String s = sb.toString();
        sb.setLength(0);
        m = PATTERN_BODY_END.matcher(s);
        if (m.find()) {
            m.appendReplacement(sb, BLOCKQUOTE_END + "$0");
            m.appendTail(sb);
        } else {
            m = PATTERN_HTML_END.matcher(s);
            if (m.find()) {
                m.appendReplacement(sb, BLOCKQUOTE_END + "$0");
                int matcherEnd = m.end();
                if (matcherEnd < s.length()) {
                    final String tail = s.substring(matcherEnd);
                    if (!isEmpty(tail) && hasContent(tail)) {
                        sb.append(BLOCKQUOTE_START);
                        sb.append(tail);
                        sb.append(BLOCKQUOTE_END);
                    } else {
                        m.appendTail(sb);
                    }
                } else {
                    m.appendTail(sb);
                }
            } else {
                m.appendTail(sb);
                sb.append(BLOCKQUOTE_END);
            }
        }
        return sb.toString();
    }

    private static final class ParameterContainer {

        final ContentType retvalContentType;
        final StringBuilder textBuilder;
        final StringHelper strHelper;
        final UserSettingMail usm;
        final Session session;
        final MailMessage origMail;
        final LocaleAndTimeZone ltz;
        final List<String> replyTexts;

        ParameterContainer(ContentType retvalContentType, StringBuilder textBuilder, StringHelper strHelper, UserSettingMail usm, Session session, MailMessage origMail, LocaleAndTimeZone ltz, List<String> replyTexts) {
            super();
            this.origMail = origMail;
            this.session = session;
            this.retvalContentType = retvalContentType;
            this.textBuilder = textBuilder;
            this.strHelper = strHelper;
            this.usm = usm;
            this.ltz = ltz;
            this.replyTexts = replyTexts;
        }
    }

    private static final Pattern PATTERN_CONTENT = Pattern.compile("(<[a-zA-Z]+[^>]*?>)?\\p{L}+");

    private static boolean hasContent(String html) {
        return PATTERN_CONTENT.matcher(html).find();
    }

}
