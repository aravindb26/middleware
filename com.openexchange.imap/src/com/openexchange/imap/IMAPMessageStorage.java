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

package com.openexchange.imap;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.openexchange.mail.dataobjects.MailFolder.ROOT_FOLDER_ID;
import static com.openexchange.mail.mime.utils.MimeMessageUtility.fold;
import static com.openexchange.mail.mime.utils.MimeStorageUtility.getFetchProfile;
import static com.openexchange.mail.utils.StorageUtility.prepareMailFieldsForSearch;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.mail.FetchProfile;
import javax.mail.FetchProfile.Item;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.StoreClosedException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.lang.ArrayUtils;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.imap.OperationKey.Type;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.cache.ListLsubEntry;
import com.openexchange.imap.cache.RightsCache;
import com.openexchange.imap.cache.UserFlagsCache;
import com.openexchange.imap.command.AbstractIMAPCommand;
import com.openexchange.imap.command.CopyIMAPCommand;
import com.openexchange.imap.command.FlagsIMAPCommand;
import com.openexchange.imap.command.MailMessageFetchIMAPCommand;
import com.openexchange.imap.command.MessageFetchIMAPCommand;
import com.openexchange.imap.command.MessageFetchIMAPCommand.FetchProfileModifier;
import com.openexchange.imap.command.MoveIMAPCommand;
import com.openexchange.imap.command.RangeSortingInterceptor;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.config.IMAPProperties;
import com.openexchange.imap.config.IMAPReloadable;
import com.openexchange.imap.dataobjects.IMAPMailFilterResult;
import com.openexchange.imap.protection.IMAPSelfProtection;
import com.openexchange.imap.protection.IMAPSelfProtectionFactory;
import com.openexchange.imap.search.IMAPSearch;
import com.openexchange.imap.services.Services;
import com.openexchange.imap.sort.IMAPSort;
import com.openexchange.imap.sort.IMAPSort.ImapSortResult;
import com.openexchange.imap.util.AppendEmptyMessageTracer;
import com.openexchange.imap.util.IMAPRuntimeException;
import com.openexchange.imap.util.IMAPSessionStorageAccess;
import com.openexchange.imap.util.ImapUtility;
import com.openexchange.imap.util.PartNotFoundException;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.IMailMessageStorageBatch;
import com.openexchange.mail.api.IMailMessageStorageBatchCopyMove;
import com.openexchange.mail.api.IMailMessageStorageDelegator;
import com.openexchange.mail.api.IMailMessageStorageEnhancedDeletion;
import com.openexchange.mail.api.IMailMessageStorageExt;
import com.openexchange.mail.api.IMailMessageStorageMailFilterApplication;
import com.openexchange.mail.api.IMailMessageStorageMimeSupport;
import com.openexchange.mail.api.IMailMessageStorageThreadReferences;
import com.openexchange.mail.api.ISimplifiedThreadStructureEnhanced;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.dataobjects.IDMailMessage;
import com.openexchange.mail.dataobjects.MailFilterResult;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.MailThread;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.ContentAware;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeCleanUp;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.converters.ConverterConfig;
import com.openexchange.mail.mime.converters.DefaultConverterConfig;
import com.openexchange.mail.mime.converters.MimeMessageConverter;
import com.openexchange.mail.mime.dataobjects.MimeRawSource;
import com.openexchange.mail.mime.filler.MimeMessageFiller;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.mime.utils.MimeStorageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.MailPartHandler;
import com.openexchange.mail.search.ANDTerm;
import com.openexchange.mail.search.CapabilitiesAndOptions;
import com.openexchange.mail.search.FlagTerm;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.search.UserFlagTerm;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.MailMessageComparatorFactory;
import com.openexchange.mail.uuencode.UUEncodedMultiPart;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.SpamHandler;
import com.openexchange.spamhandler.SpamHandlerRegistry;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.openexchange.version.VersionService;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.Filter;
import com.sun.mail.imap.FilterResult;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;
import com.sun.mail.util.MessageRemovedIOException;
import com.sun.mail.util.ReadableMime;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * {@link IMAPMessageStorage} - The IMAP implementation of message storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPMessageStorage extends IMAPFolderWorker implements IMailMessageStorageExt, IMailMessageStorageBatch, ISimplifiedThreadStructureEnhanced, IMailMessageStorageMimeSupport, IMailMessageStorageBatchCopyMove, IMailMessageStorageThreadReferences, IMailMessageStorageEnhancedDeletion, IMailMessageStorageMailFilterApplication {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IMAPMessageStorage.class);

    private static final int READ_ONLY = Folder.READ_ONLY;

    private static final int READ_WRITE = Folder.READ_WRITE;

    /*-
     * Flag constants
     */

    /**
     * This message is a draft. This flag is set by clients to indicate that the message is a draft message.
     */
    private static final Flag DRAFT = Flags.Flag.DRAFT;

    /**
     * This message is marked deleted. Clients set this flag to mark a message as deleted. The expunge operation on a folder removes all
     * messages in that folder that are marked for deletion.
     */
    private static final Flag DELETED = Flags.Flag.DELETED;

    /**
     * The Flags object initialized with the \Draft system flag.
     */
    private static final Flags FLAGS_DRAFT = new Flags(DRAFT);

    /**
     * The Flags object initialized with the \Deleted system flag.
     */
    private static final Flags FLAGS_DELETED = new Flags(DELETED);

    /*-
     * String constants
     */

    private static final boolean LOOK_UP_INBOX_ONLY = true;

    private static volatile Boolean useImapThreaderIfSupported;
    /** <b>Only</b> applies to: getThreadSortedMessages(...) in ISimplifiedThreadStructure. Default is <code>false</code> */
    static boolean useImapThreaderIfSupported() {
        Boolean b = useImapThreaderIfSupported;
        if (null == b) {
            synchronized (IMAPMessageStorage.class) {
                b = useImapThreaderIfSupported;
                if (null == b) {
                    final ConfigurationService service = Services.getService(ConfigurationService.class);
                    b = Boolean.valueOf(null != service && service.getBoolProperty("com.openexchange.imap.useImapThreaderIfSupported", false));
                    useImapThreaderIfSupported = b;
                }
            }
        }
        return b.booleanValue();
    }

    /** Whether ESORT is allowed to be utilized */
    static boolean allowESORT(Session session) {
        return allowESORT(session.getUserId(), session.getContextId());
    }

    /** Whether ESORT is allowed to be utilized */
    static boolean allowESORT(int userId, int contextId) {
        return IMAPProperties.getInstance().allowESORT(userId, contextId);
    }

    /** Whether SORT=DISPLAY is allowed to be utilized */
    public static boolean allowSORTDISPLAY(Session session, int accountId) throws OXException {
        return allowSORTDISPLAY(session.getUserId(), session.getContextId(), accountId);
    }

    /** Whether SORT=DISPLAY is allowed to be utilized */
    public static boolean allowSORTDISPLAY(int userId, int contextId, int accountId) throws OXException {
        ConfigViewFactory factory = Services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(userId, contextId);

        if (Account.DEFAULT_ID == accountId) {
            ComposedConfigProperty<Boolean> property = view.property("com.openexchange.imap.primary.allowSORTDISPLAY", boolean.class);
            Boolean b = property.get();
            if (b != null) {
                return b.booleanValue();
            }
        }

        return IMAPProperties.getInstance().allowSORTDISPLAY(userId, contextId);
    }

    /** Whether in-app sort is supposed to be utilized if IMAP-side SORT fails with a "NO" response */
    public static boolean fallbackOnFailedSORT(Session session, int accountId) throws OXException {
        return fallbackOnFailedSORT(session.getUserId(), session.getContextId(), accountId);
    }

    /** Whether in-app sort is supposed to be utilized if IMAP-side SORT fails with a "NO" response */
    public static boolean fallbackOnFailedSORT(int userId, int contextId, int accountId) throws OXException {
        ConfigViewFactory factory = Services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(userId, contextId);

        if (Account.DEFAULT_ID == accountId) {
            ComposedConfigProperty<Boolean> property = view.property("com.openexchange.imap.primary.fallbackOnFailedSORT", boolean.class);
            Boolean b = property.get();
            if (b != null) {
                return b.booleanValue();
            }
        }

        return IMAPProperties.getInstance().fallbackOnFailedSORT(userId, contextId);
    }

    /** The full name for the virtual "all messages" folder */
    public static String allMessagesFolder(Session session) throws OXException {
        return allMessagesFolder(session.getUserId(), session.getContextId());
    }

    /** The full name for the virtual "all messages" folder */
    public static String allMessagesFolder(int userId, int contextId) throws OXException {
        ConfigViewFactory factory = Services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(userId, contextId);

        ComposedConfigProperty<String> property = view.property("com.openexchange.find.basic.mail.allMessagesFolder", String.class);
        String fn = property.get();
        return Strings.isEmpty(fn) ? null : fn;
    }

    static {
        IMAPReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                useImapThreaderIfSupported = null;
            }

            @Override
            public Interests getInterests() {
                return Reloadables.interestsForProperties("com.openexchange.imap.useImapThreaderIfSupported");
            }
        });
    }

    /**
     * Gets the prepared mail fields in case full name denotes the special "all messages" folder.
     *
     * @param mailFields The requested mail fields by client
     * @param fullName The full name
     * @param session The session to check for
     * @return The prepared mail fields
     * @throws OXException If invocation fails
     */
    static MailFields prepareMailFieldsForVirtualFolder(MailField[] mailFields, String fullName, Session session) throws OXException {
        MailFields fields = new MailFields(mailFields);
        prepareMailFieldsForVirtualFolder(fields, fullName, session);
        return fields;
    }

    /**
     * Gets the prepared mail fields in case full name denotes the special "all messages" folder.
     *
     * @param mailFields The current mail fields
     * @param fullName The full name
     * @param session The session to check for
     * @return The prepared mail fields
     * @throws OXException If invocation fails
     */
    static void prepareMailFieldsForVirtualFolder(MailFields mailFields, String fullName, Session session) throws OXException {
        if (null == fullName || !fullName.equals(allMessagesFolder(session))) {
            return;
        }

        mailFields.add(MailField.ORIGINAL_FOLDER_ID);
        mailFields.add(MailField.ORIGINAL_ID);
    }

    private static final int MB_5 = 5_242_880; /* 5MB */

    /*-
     * Members
     */

    private MailAccount mailAccount;
    private Locale locale;
    private IIMAPProperties imapProperties;
    private final IMAPFolderStorage imapFolderStorage;
    private IMAPSelfProtection selfProtection;

    /**
     * Initializes a new {@link IMAPMessageStorage}.
     *
     * @param imapStore The IMAP store
     * @param imapAccess The IMAP access
     * @param session The session providing needed user data
     * @throws OXException If initialization fails
     */
    public IMAPMessageStorage(IMAPStore imapStore, IMAPAccess imapAccess, Session session) throws OXException {
        super(imapStore, imapAccess, session);
        imapFolderStorage = imapAccess.getFolderStorage();
    }

    /**
     * Handles specified {@link MessagingException} instance.
     *
     * @param e The {@link MessagingException} instance
     * @return The appropriate {@link OXException} instance
     */
    public OXException handleMessagingException(MessagingException e) {
        return handleMessagingException(null, e);
    }

    /**
     * Handles specified {@link MessagingException} instance.
     *
     * @param optFullName The optional full name
     * @param e The {@link MessagingException} instance
     * @return The appropriate {@link OXException} instance
     */
    public OXException handleMessagingException(String optFullName, MessagingException e) {
        if (null != optFullName && MimeMailException.isInUseException(e)) {
            IMAPFolderWorker.markForFailFast(imapStore, optFullName, e);
        }
        return IMAPException.handleMessagingException(e, imapConfig, session, accountId, null == optFullName ? null : mapFor("fullName", optFullName));
    }

    /**
     * Gets the IMAP self-protection.
     *
     * @return The IMAP self-protection
     * @throws OXException If IAP self-protection cannot be returned
     */
    private IMAPSelfProtection getIMAPSelfProtection() throws OXException {
        IMAPSelfProtection selfProtection = this.selfProtection;
        if (selfProtection == null) {
            selfProtection = IMAPSelfProtectionFactory.getInstance().createSelfProtectionFor(session);
            this.selfProtection = selfProtection;
        }
        return selfProtection;
    }

    /**
     * Gets the mail account
     *
     * @return The mail account
     * @throws OXException If mail account cannot be returned
     */
    MailAccount getMailAccount() throws OXException {
        MailAccount mailAccount = this.mailAccount;
        if (mailAccount == null) {
            try {
                final MailAccountStorageService storageService = Services.getService(MailAccountStorageService.class);
                if (null == storageService) {
                    throw ServiceExceptionCode.absentService(MailAccountStorageService.class);
                }
                mailAccount = storageService.getMailAccount(accountId, session.getUserId(), session.getContextId());
                this.mailAccount = mailAccount;
            } catch (RuntimeException e) {
                throw handleRuntimeException(e);
            }
        }
        return mailAccount;
    }

    /**
     * Gets the user's locale
     *
     * @return The locale
     * @throws OXException If locale cannot be returned
     */
    Locale getLocale() throws OXException {
        if (locale == null) {
            try {
                if (session instanceof ServerSession) {
                    locale = ((ServerSession) session).getUser().getLocale();
                } else {
                    final UserService userService = Services.getService(UserService.class);
                    locale = userService.getUser(session.getUserId(), ctx).getLocale();
                }
            } catch (RuntimeException e) {
                throw handleRuntimeException(e);
            }
        }
        return locale;
    }

    /**
     * Gets the IMAP properties
     *
     * @return The IMAP properties
     */
    IIMAPProperties getIMAPProperties() {
        if (null == imapProperties) {
            imapProperties = imapConfig.getIMAPProperties();
        }
        return imapProperties;
    }

    /**
     * Opens the denoted folder in read-only mode.
     *
     * @param fullName The full name of the folder to open
     * @throws OXException If opening folder fails
     */
    void openReadOnly(String fullName) throws OXException {
        try {
            imapFolder = setAndOpenFolder(imapFolder, fullName, READ_ONLY);
        } catch (MessagingException e) {
            final Exception next = e.getNextException();
            if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
            }
            throw handleMessagingException(fullName, e);
        }
    }

    /**
     * Gets the associated IMAP store.
     *
     * @return The IMAP store
     */
    public IMAPStore getImapStore() {
        return imapStore;
    }

    /**
     * Gets the current IMAP folder in use.
     *
     * @return The IMAP folder or <code>null</code>
     */
    public IMAPFolder getImapFolder() {
        return imapFolder;
    }

    /**
     * Gets the IMAP configuration
     *
     * @return The IMAP configuration
     */
    public IMAPConfig getImapConfig() {
        return imapConfig;
    }

    private boolean hasIMAP4rev1() {
        return imapConfig.getImapCapabilities().hasIMAP4rev1();
    }

    private void checkMessagesLimit(int numberOfMessages) throws OXException {
        int maxNumberOfMessages = getIMAPSelfProtection().getMaxNumberOfMessages();
        if (maxNumberOfMessages > 0 && maxNumberOfMessages < numberOfMessages) {
            throw IMAPException.create(IMAPException.Code.MAX_NUMBER_OF_MESSAGES_EXCEEDED, I(maxNumberOfMessages));
        }
    }

    @Override
    public void clearCache() throws OXException {
        IMAPFolderWorker.clearCache(imapFolder);
    }

    @Override
    public MailMessage[] getMessages(String fullName, String[] mailIds, MailField[] mailFields, String[] headerNames) throws OXException {
        if ((mailIds == null) || (mailIds.length == 0)) {
            return EMPTY_RETVAL;
        }
        checkMessagesLimit(mailIds.length);
        return getMessagesInternal(fullName, uids2longs(mailIds), mailFields, headerNames);
    }

    @Override
    public MailMessage[] getMessagesLong(String fullName, long[] mailIds, MailField[] mailFields) throws OXException {
        if ((mailIds == null) || (mailIds.length == 0)) {
            return EMPTY_RETVAL;
        }
        checkMessagesLimit(mailIds.length);
        return getMessagesInternal(fullName, mailIds, mailFields, null);
    }

    private MailMessage[] getMessagesInternal(String fullName, long[] uids, MailField[] mailFields, String[] headerNames) throws OXException {
        final MailFields fieldSet = new MailFields(mailFields);
        prepareMailFieldsForVirtualFolder(fieldSet, fullName, session);
        /*
         * Check for field FULL
         */
        if (fieldSet.contains(MailField.FULL) || fieldSet.contains(MailField.BODY)) {
            /*
             * Determine number of unread messages for that folder in advance
             */
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            int numUnreadMessages;
            try {
                if (0 >= imapFolder.getMessageCount()) {
                    return new MailMessage[uids.length];
                }
                numUnreadMessages = IMAPCommandsCollection.getUnread(imapFolder);
            } catch (MessagingException e) {
                if (ImapUtility.isInvalidMessageset(e)) {
                    return new MailMessage[0];
                }
                throw handleMessagingException(fullName, e);
            }
            /*
             * Query messages
             */
            final MailMessage[] mails = new MailMessage[uids.length];
            for (int j = mails.length; j-- > 0;) {
                try {
                    mails[j] = getMessageLongInternal(imapFolder, fullName, uids[j], false, numUnreadMessages);
                } catch (OXException e) {
                    e.setCategory(Category.CATEGORY_WARNING);
                    imapAccess.addWarnings(Collections.singletonList(e));
                    mails[j] = null;
                } catch (Exception e) {
                    final OXException oxe = MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
                    oxe.setCategory(Category.CATEGORY_WARNING);
                    imapAccess.addWarnings(Collections.singletonList(oxe));
                    mails[j] = null;
                }
            }
            return mails;
        }
        /*
         * Get messages with given fields filled
         */
        try {
            openReadOnly(fullName);
            /*
             * Fetch desired messages by given UIDs. Turn UIDs to corresponding sequence numbers to maintain order cause some IMAP servers
             * ignore the order of UIDs provided in a "UID FETCH" command.
             */
            final MailMessage[] messages;
            {
                FetchProfile fetchProfile = getFetchProfile(fieldSet.toArray(), headerNames, null, null, getIMAPProperties().isFastFetch(), examineHasAttachmentUserFlags && fieldSet.containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
                if (imapConfig.asMap().containsKey("UIDPLUS")) {
                    long[] valids = filterNegativeElements(uids);
                    TLongObjectHashMap<MailMessage> fetchedMsgs = fetchValidWithFallbackFor(fullName, valids, valids.length, fetchProfile, hasIMAP4rev1(), false);
                    /*
                     * Fill array
                     */
                    messages = new MailMessage[uids.length];
                    for (int i = uids.length; i-- > 0;) {
                        long uid = uids[i];
                        messages[i] = uid < 0 ? null : fetchedMsgs.get(uid);
                    }
                } else {
                    long[] valids = filterNegativeElements(uids);
                    TLongIntMap seqNumsMap = IMAPCommandsCollection.uids2SeqNumsMap(imapFolder, valids);
                    TLongObjectMap<MailMessage> fetchedMsgs = fetchValidWithFallbackFor(fullName, seqNumsMap.values(), seqNumsMap.size(), fetchProfile, hasIMAP4rev1(), true);
                    /*
                     * Fill array
                     */
                    messages = new MailMessage[uids.length];
                    for (int i = uids.length; i-- > 0;) {
                        long uid = uids[i];
                        messages[i] = uid < 0 ? null : fetchedMsgs.get(seqNumsMap.get(uid));
                    }
                }
            }
            /*
             * Check field existence
             */
            MimeMessageConverter.checkFieldExistence(messages, mailFields);
            if (fieldSet.contains(MailField.ACCOUNT_NAME) || fieldSet.contains(MailField.FULL)) {
                return setAccountInfo(messages);
            }
            return messages;
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return new MailMessage[0];
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            clearCache(imapFolder);
        }
    }

    private TLongObjectHashMap<MailMessage> fetchValidWithFallbackFor(String fullName, Object array, int len, FetchProfile fetchProfile, boolean isRev1, boolean seqnum) throws OXException {
        final String key = new StringBuilder(16).append(accountId).append(".imap.fetch.modifier").toString();
        final FetchProfile fp = fetchProfile;
        int retry = 0;
        while (true) {
            try {
                final FetchProfileModifier modifier = (FetchProfileModifier) session.getParameter(key);
                if (null == modifier) {
                    // session.setParameter(key, FetchIMAPCommand.DEFAULT_PROFILE_MODIFIER);
                    return fetchValidFor(fullName, array, len, fp, isRev1, seqnum, false);
                }
                return fetchValidFor(fullName, array, len, modifier.modify(fp), isRev1, seqnum, modifier.byContentTypeHeader());
            } catch (FolderClosedException | StoreClosedException e) {
                throw handleMessagingException(imapFolder.getFullName(), e);
            } catch (MessagingException e) {
                final Exception nextException = e.getNextException();
                if ((nextException instanceof BadCommandException) || (nextException instanceof CommandFailedException)) {
                    if (LOG.isDebugEnabled()) {
                        final StringBuilder sb = new StringBuilder(128).append("Fetch with fetch profile failed: ");
                        for (Item item : fetchProfile.getItems()) {
                            sb.append(item.getClass().getSimpleName()).append(',');
                        }
                        for (String name : fetchProfile.getHeaderNames()) {
                            sb.append(name).append(',');
                        }
                        sb.setLength(sb.length() - 1);
                        LOG.debug(sb.toString(), e);
                    }
                    if (0 == retry) {
                        session.setParameter(key, MessageFetchIMAPCommand.NO_BODYSTRUCTURE_PROFILE_MODIFIER);
                        retry++;
                    } else if (1 == retry) {
                        session.setParameter(key, MessageFetchIMAPCommand.HEADERLESS_PROFILE_MODIFIER);
                        retry++;
                    } else {
                        throw handleMessagingException(imapFolder.getFullName(), e);
                    }
                } else {
                    throw handleMessagingException(imapFolder.getFullName(), e);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                /*
                 * May occur while parsing invalid BODYSTRUCTURE response
                 */
                if (LOG.isDebugEnabled()) {
                    final StringBuilder sb = new StringBuilder(128).append("Fetch with fetch profile failed: ");
                    for (Item item : fetchProfile.getItems()) {
                        sb.append(item.getClass().getSimpleName()).append(',');
                    }
                    for (String name : fetchProfile.getHeaderNames()) {
                        sb.append(name).append(',');
                    }
                    sb.setLength(sb.length() - 1);
                    LOG.debug(sb.toString(), e);
                }
                if (0 == retry) {
                    session.setParameter(key, MessageFetchIMAPCommand.NO_BODYSTRUCTURE_PROFILE_MODIFIER);
                    retry++;
                } else if (1 == retry) {
                    session.setParameter(key, MessageFetchIMAPCommand.HEADERLESS_PROFILE_MODIFIER);
                    retry++;
                } else {
                    throw handleRuntimeException(e);
                }
            } catch (RuntimeException e) {
                throw handleRuntimeException(e);
            }
        }
    }

    private TLongObjectHashMap<MailMessage> fetchValidFor(String fullName, Object array, int len, FetchProfile fetchProfile, boolean isRev1, boolean seqnum, boolean byContentType) throws MessagingException, OXException {
        if (null == imapFolder || !imapFolder.checkOpen()) {
            openReadOnly(fullName);
        }
        // final MailMessage[] tmp = new NewFetchIMAPCommand(imapFolder, getSeparator(imapFolder), isRev1, array, fetchProfile, false,
        // false, false).setDetermineAttachmentByHeader(byContentType).doCommand();
        final MailMessageFetchIMAPCommand command;
        if (array instanceof long[]) {
            command = new MailMessageFetchIMAPCommand(imapFolder, isRev1, (long[]) array, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, Optional.empty()).setDetermineAttachmentByHeader(byContentType);
        } else {
            command = new MailMessageFetchIMAPCommand(imapFolder, isRev1, (int[]) array, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, Optional.empty()).setDetermineAttachmentByHeader(byContentType);
        }
        final long start = System.currentTimeMillis();
        final MailMessage[] tmp = command.doCommand();
        final long time = System.currentTimeMillis() - start;
        mailInterfaceMonitor.addUseTime(time);
        LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(len), Long.valueOf(time));
        final TLongObjectHashMap<MailMessage> map = new TLongObjectHashMap<>(len);
        for (MailMessage mailMessage : tmp) {
            if (null != mailMessage) {
                IDMailMessage idmm = (IDMailMessage) mailMessage;
                map.put(seqnum ? idmm.getSeqnum() : idmm.getUid(), idmm);
            }

        }
        return map;
    }

    @SuppressWarnings("unused")
    @Override
    public MailMessage[] getMessagesByMessageID(String... messageIDs) throws OXException {
        if ((messageIDs == null) || (messageIDs.length == 0)) {
            return EMPTY_RETVAL;
        }
        final int length = messageIDs.length;
        checkMessagesLimit(length);
        try {
            int count = 0;
            final MailMessage[] retval = new MailMessage[length];
            try {
                imapFolder = setAndOpenFolder(imapFolder, "INBOX", READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, "INBOX");
                }
                throw handleMessagingException("INBOX", e);
            }
            final long[] uids = IMAPCommandsCollection.messageId2UID(imapFolder, messageIDs);
            for (int i = 0; i < uids.length; i++) {
                final long uid = uids[i];
                if (uid != -1) {
                    retval[i] = new IDMailMessage(String.valueOf(uid), "INBOX");
                    count++;
                }
            }
            if (count == length || LOOK_UP_INBOX_ONLY) {
                return retval;
            }
            /*
             * Look-up other folders
             */
            recursiveMessageIDLookUp((IMAPFolder) imapStore.getDefaultFolder(), messageIDs, retval, count);
            return retval;
        } catch (MessagingException e) {
            throw handleMessagingException(imapFolder.getFullName(), e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private int recursiveMessageIDLookUp(IMAPFolder parentFolder, String[] messageIDs, MailMessage[] retval, int countArg) throws OXException, MessagingException {
        int count = countArg;
        final Folder[] folders = parentFolder.list();
        for (int i = 0; count >= 0 && i < folders.length; i++) {
            final String fullName = folders[i].getFullName();
            final IMAPFolder imapFolder = setAndOpenFolder(fullName, READ_ONLY);
            final long[] uids = IMAPCommandsCollection.messageId2UID(imapFolder, messageIDs);
            for (int k = 0; k < uids.length; k++) {
                final long uid = uids[k];
                if (uid != -1) {
                    retval[k] = new IDMailMessage(fullName, Long.toString(uid));
                    count++;
                }
            }
            if (count == messageIDs.length) {
                return -1;
            }
            count = recursiveMessageIDLookUp(imapFolder, messageIDs, retval, count);
        }
        return count;
    }

    private static final FetchProfile FETCH_PROFILE_PART = new FetchProfile(UIDFolder.FetchProfileItem.UID, FetchProfile.Item.CONTENT_INFO);

    @Override
    public MailPart getAttachmentLong(String fullName, long msgUID, String sequenceId) throws OXException {
        if (msgUID < 0 || null == sequenceId) {
            return null;
        }
        try {
            openReadOnly(fullName);
            if (0 >= imapFolder.getMessageCount()) {
                return null;
            }
            /*
             * Check Content-Type
             */
            Optional<BODYSTRUCTURE> optBodystructure = Optional.empty();
            boolean useGetPart = true;
            {
                imapFolder.fetch(null, new long[] { msgUID }, FETCH_PROFILE_PART, null);
                IMAPMessage msg = (IMAPMessage) imapFolder.getMessageByUID(msgUID);
                if (null == msg) {
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(Long.valueOf(msgUID), fullName);
                }
                try {
                    BODYSTRUCTURE bodystructure = msg.getBodystructure();
                    optBodystructure = Optional.of(bodystructure);
                    if (indicatesTnefOrSignedContent(bodystructure)) {
                        useGetPart = false;
                    }
                } catch (MessagingException e) {
                    if (!"Unable to load BODYSTRUCTURE".equals(e.getMessage())) {
                        throw e;
                    }

                    // Grab from copied IMAP message
                    useGetPart = false;
                }
            }
            /*
             * Try by Content-ID
             */
            if (useGetPart) {
                try {
                    final MailPart part = IMAPCommandsCollection.getPart(imapFolder, msgUID, sequenceId, false, true, optBodystructure);
                    if (null != part) {
                        // Appropriate part found -- check for special content
                        final ContentType contentType = part.getContentType();
                        if (!isTNEFMimeType(contentType) && !isUUEncoded(part, contentType)) {
                            if (!part.containsSequenceId()) {
                                part.setSequenceId(sequenceId);
                            }
                            if (MailMessageParser.isRfc3464Or3798(contentType.getBaseType()) && (part.getFileName() == null || part.getFileName().startsWith("Part_"))) {
                                part.setFileName(contentType.getSubType() + ".txt");
                            }
                            return part;
                        }
                    }
                } catch (MessageRemovedException e) {
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(e, Long.valueOf(msgUID), fullName);
                } catch (PartNotFoundException e) {
                    throw MailExceptionCode.ATTACHMENT_NOT_FOUND.create(e, sequenceId, Long.valueOf(msgUID), fullName);
                } catch (IOException e) {
                    if ((e instanceof MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                        throw MailExceptionCode.MAIL_NOT_FOUND.create(e, Long.valueOf(msgUID), fullName);
                    }
                    // Ignore
                } catch (Exception e) {
                    // Ignore
                    LOG.trace("", e);
                }
            }
            /*
             * Regular look-up
             */
            final MailMessage mail = getMessageLong(fullName, msgUID, false);
            if (null == mail) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(Long.valueOf(msgUID), fullName);
            }
            final MailPartHandler handler = new MailPartHandler(sequenceId);
            new MailMessageParser().parseMailMessage(mail, handler);
            final MailPart mailPart = handler.getMailPart();
            if (mailPart == null) {
                throw MailExceptionCode.ATTACHMENT_NOT_FOUND.create(sequenceId, Long.valueOf(msgUID), fullName);
            }
            if (!mailPart.containsSequenceId()) {
                mailPart.setSequenceId(sequenceId);
            }
            return mailPart;
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return null;
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /**
     * Checks if given <code>BODYSTRUCTURE</code> contains TNEF or <code>"multipart/signed"</code> content.
     *
     * @param bodystructure The <code>BODYSTRUCTURE</code> to examine
     * @return <code>true</code> if either TNEF or <code>"multipart/signed"</code> is contained; otherwise <code>false</code> for neither nor
     */
    private static boolean indicatesTnefOrSignedContent(BODYSTRUCTURE bodystructure) {
        return indicatesTnefOrSignedContent(bodystructure, new StringBuilder());
    }

    /**
     * Checks if given <code>BODYSTRUCTURE</code> contains TNEF or <code>"multipart/signed"</code> content.
     *
     * @param bodystructure The <code>BODYSTRUCTURE</code> to examine
     * @param mimeTypeBuilder The <code>StringBuilder</code> instance to use for building the MIME type string
     * @return <code>true</code> if either TNEF or <code>"multipart/signed"</code> is contained; otherwise <code>false</code> for neither nor
     */
    private static boolean indicatesTnefOrSignedContent(BODYSTRUCTURE bodystructure, StringBuilder mimeTypeBuilder) {
        if (bodystructure == null) {
            return false;
        }

        if (bodystructure.type != null && bodystructure.subtype != null) {
            mimeTypeBuilder.setLength(0);
            String mimeType = mimeTypeBuilder.append(Strings.asciiLowerCase(bodystructure.type.trim())).append('/').append(Strings.asciiLowerCase(bodystructure.subtype.trim())).toString();
            if (mimeType.startsWith("application/ms-tnef") || mimeType.startsWith("application/vnd.ms-tnef")) {
                return true;
            }
            if (mimeType.startsWith("multipart/signed")) {
                return true;
            }
        }

        if (bodystructure.bodies != null) {
            for (BODYSTRUCTURE body : bodystructure.bodies) {
                if (indicatesTnefOrSignedContent(body, mimeTypeBuilder)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTNEFMimeType(ContentType contentType) {
        // note that application/ms-tnefx was also observed in the wild
        return contentType != null && (contentType.startsWith("application/ms-tnef") || contentType.startsWith("application/vnd.ms-tnef"));
    }

    private static boolean isUUEncoded(MailPart part, ContentType contentType) throws OXException, IOException {
        if (null == part) {
            return false;
        }
        if (!contentType.startsWith("text/plain")) {
            return false;
        }
        return UUEncodedMultiPart.isUUEncoded(MimeMessageUtility.readContent(part, contentType));
    }

    @Override
    public MailPart getImageAttachmentLong(String fullName, long msgUID, String contentId) throws OXException {
        if (msgUID < 0 || null == contentId) {
            return null;
        }
        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            if (0 >= imapFolder.getMessageCount()) {
                return null;
            }
            /*
             * Check Content-Type
             */
            Optional<BODYSTRUCTURE> optBodystructure = Optional.empty();
            MimeMessage msg;
            boolean useGetPart = true;
            {
                imapFolder.fetch(null, new long[] { msgUID }, FETCH_PROFILE_PART, null);
                msg = (IMAPMessage) imapFolder.getMessageByUID(msgUID);
                if (null == msg) {
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(Long.valueOf(msgUID), fullName);
                }
                try {
                    // Already pre-fetched
                    BODYSTRUCTURE bodystructure = ((IMAPMessage) msg).getBodystructure();
                    optBodystructure = Optional.of(bodystructure);
                    if (indicatesTnefOrSignedContent(bodystructure)) {
                        useGetPart = false;
                    }
                } catch (MessagingException e) {
                    if (!"Unable to load BODYSTRUCTURE".equals(e.getMessage())) {
                        throw e;
                    }

                    // Grab from copied IMAP message
                    msg = MimeMessageUtility.newMimeMessage(((IMAPMessage) msg).getMimeStream(), null);
                    useGetPart = false;
                }
            }
            /*
             * Try by Content-ID
             */
            if (useGetPart) {
                try {
                    MailPart partByContentId = IMAPCommandsCollection.getPartByContentId(imapFolder, msgUID, contentId, false, true, optBodystructure);
                    if (null != partByContentId) {
                        return partByContentId;
                    }
                } catch (PartNotFoundException e) {
                    throw MailExceptionCode.IMAGE_ATTACHMENT_NOT_FOUND.create(e, contentId, Long.valueOf(msgUID), fullName);
                } catch (Exception e) {
                    // Ignore
                    LOG.trace("", e);
                }
            }
            /*
             * Regular look-up
             */
            Optional<String> optContentType = Optional.empty();
            if (msg instanceof IMAPMessage) {
                // Already pre-fetched
                optContentType = Optional.of(((IMAPMessage) msg).getContentType());
            }
            Part p = examinePart(msg, contentId, optContentType);
            if (null == p) {
                // Retry...
                MimeMessage tmp = MimeMessageUtility.mimeMessageFrom(msg);
                p = examinePart(tmp, contentId, Optional.empty());
                if (null == p) {
                    throw MailExceptionCode.IMAGE_ATTACHMENT_NOT_FOUND.create(contentId, Long.valueOf(msgUID), fullName);
                }
            }
            return MimeMessageConverter.convertPart(p, false);
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return null;
            }
            throw handleMessagingException(fullName, e);
        } catch (IOException e) {
            if ((e instanceof MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private static final String SUFFIX = "@" + VersionService.NAME;

    private Part examinePart(Part part, String contentId, Optional<String> optContentType) throws OXException {
        try {
            String ct = Strings.toLowerCase(optContentType.isPresent() ? optContentType.get() : getFirstHeaderFrom(MessageHeaders.HDR_CONTENT_TYPE, part));

            String realFilename = null;
            boolean considerAsImage = false;
            if (null == ct) {
                realFilename = getRealFilename(part);
                if (Strings.isNotEmpty(realFilename) && MimeType2ExtMap.getContentType(realFilename, "").startsWith("image/")) {
                    considerAsImage = true;
                }
            } else if (ct.startsWith("image/")) {
                considerAsImage = true;
            }

            if (considerAsImage) {
                String partContentId = getFirstHeaderFrom(MessageHeaders.HDR_CONTENT_ID, part);
                if (null == partContentId) {
                    /*
                     * Compare with file name
                     */
                    if (null == realFilename) {
                        realFilename = getRealFilename(part);
                    }
                    if (MimeMessageUtility.equalsCID(contentId, realFilename)) {
                        return part;
                    }
                }
                /*
                 * Compare with Content-Id
                 */
                if (MimeMessageUtility.equalsCID(contentId, partContentId, SUFFIX)) {
                    return part;
                }
                /*
                 * Compare with file name
                 */
                if (null == realFilename) {
                    realFilename = getRealFilename(part);
                }
                if (MimeMessageUtility.equalsCID(contentId, realFilename)) {
                    return part;
                }
            } else if (null != ct && ct.startsWith("multipart/")) {
                Multipart m;
                {
                    final Object content = part.getContent();
                    if (content instanceof Multipart) {
                        m = (Multipart) content;
                    } else {
                        m = new MimeMultipart(part.getDataHandler().getDataSource());
                    }
                }
                int count = m.getCount();
                for (int i = 0; i < count; i++) {
                    final Part p = examinePart(m.getBodyPart(i), contentId, Optional.empty());
                    if (null != p) {
                        return p;
                    }
                }
            }
            return null;
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return null;
            }
            throw handleMessagingException(imapFolder.getFullName(), e);
        } catch (IOException e) {
            if ((e instanceof MessageRemovedIOException) || (e.getCause() instanceof MessageRemovedException)) {
                throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
            }
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static String getRealFilename(Part part) throws MessagingException {
        String fileName;
        try {
            fileName = part.getFileName();
        } catch (javax.mail.internet.ParseException e) {
            // JavaMail failed to parse Content-Disposition header
            LOG.trace("JavaMail failed to parse Content-Disposition header", e);
            fileName = null;
        }
        if (fileName != null) {
            return fileName;
        }
        final String hdr = getFirstHeaderFrom(MessageHeaders.HDR_CONTENT_DISPOSITION, part);
        if (hdr == null) {
            return getContentTypeFilename(part);
        }
        try {
            final String retval = new ContentDisposition(hdr).getFilenameParameter();
            if (retval == null) {
                return getContentTypeFilename(part);
            }
            return retval;
        } catch (OXException e) {
            LOG.trace("Failed to parse Content-Disposition header", e);
            return getContentTypeFilename(part);
        }
    }

    private static String getContentTypeFilename(Part part) throws MessagingException {
        final String hdr = getFirstHeaderFrom(MessageHeaders.HDR_CONTENT_TYPE, part);
        if (hdr == null || hdr.length() == 0) {
            return null;
        }
        try {
            return new ContentType(hdr).getNameParameter();
        } catch (OXException e) {
            LOG.error("", e);
            return null;
        }
    }

    private static String getFirstHeaderFrom(String name, Part part) throws MessagingException {
        return MimeMessageUtility.getHeader(name, null, part);
    }

    private static final FetchProfile FETCH_PROFILE_ENVELOPE = new FetchProfile(FetchProfile.Item.ENVELOPE);

    @Override
    public InputStream getMimeStream(String fullName, String id) throws OXException {
        try {
            int desiredMode = READ_ONLY;
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            if (0 >= imapFolder.getMessageCount()) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            long uid = parseUnsignedLong(id);
            IMAPMessage msg;
            try {
                msg = (IMAPMessage) imapFolder.getMessageByUID(uid);
            } catch (java.lang.IndexOutOfBoundsException | MessageRemovedException e) {
                /*
                 * Obviously message was removed in the meantime
                 */
                LOG.trace("Obviously message was removed in the meantime", e);
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            } catch (MessagingException e) {
                final Exception cause = e.getNextException();
                if (!(cause instanceof BadCommandException)) {
                    throw e;
                }
                // Hm... Something weird with executed "UID FETCH" command; retry manually...
                final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, new long[] { uid });
                if ((null == seqNums) || (0 == seqNums.length)) {
                    LOG.warn("No message with UID '{}' found in folder '{}'", id, fullName, cause);
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
                }
                final int msgnum = seqNums[0];
                if (msgnum < 1) {
                    /*
                     * message-numbers start at 1
                     */
                    LOG.warn("No message with UID '{}' found in folder '{}'", id, fullName, cause);
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
                }
                msg = (IMAPMessage) imapFolder.getMessage(msgnum);
            }
            if (msg == null || msg.isExpunged()) {
                // throw new OXException(OXException.Code.MAIL_NOT_FOUND,
                // String.valueOf(msgUID), imapFolder
                // .toString());
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            msg.setPeek(true);
            int blkSize = imapStore.getFetchBlockSize();
            try {
                imapStore.setFetchBlockSize(-1);
                return msg.getMimeStream();
            } finally {
                // Restore fetch block size
                imapStore.setFetchBlockSize(blkSize);
            }
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void writeMimeMessage(String fullName, String id, OutputStream os) throws OXException {
        try {
            int desiredMode = READ_ONLY;
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            if (0 >= imapFolder.getMessageCount()) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            long uid = parseUnsignedLong(id);
            IMAPMessage msg;
            try {
                msg = (IMAPMessage) imapFolder.getMessageByUID(uid);
            } catch (java.lang.IndexOutOfBoundsException | MessageRemovedException e) {
                /*
                 * Obviously message was removed in the meantime
                 */
                LOG.trace("Obviously message was removed in the meantime", e);
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            } catch (MessagingException e) {
                final Exception cause = e.getNextException();
                if (!(cause instanceof BadCommandException)) {
                    throw e;
                }
                // Hm... Something weird with executed "UID FETCH" command; retry manually...
                final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, new long[] { uid });
                if ((null == seqNums) || (0 == seqNums.length)) {
                    LOG.warn("No message with UID '{}' found in folder '{}'", id, fullName, cause);
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
                }
                final int msgnum = seqNums[0];
                if (msgnum < 1) {
                    /*
                     * message-numbers start at 1
                     */
                    LOG.warn("No message with UID '{}' found in folder '{}'", id, fullName, cause);
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
                }
                msg = (IMAPMessage) imapFolder.getMessage(msgnum);
            }
            if (msg == null || msg.isExpunged()) {
                // throw new OXException(OXException.Code.MAIL_NOT_FOUND,
                // String.valueOf(msgUID), imapFolder
                // .toString());
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            msg.setPeek(true);
            msg.writeTo(os);
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            throw handleMessagingException(fullName, e);
        } catch (IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public Message getMimeMessage(String fullName, String id, boolean markSeen) throws OXException {
        if (null == id) {
            throw MailExceptionCode.MAIL_NOT_FOUND.create("null", fullName);
        }
        try {
            final int desiredMode = markSeen ? READ_WRITE : READ_ONLY;
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            if (0 >= imapFolder.getMessageCount()) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            final long uid = parseUnsignedLong(id);
            IMAPMessage msg;
            try {
                final long start = System.currentTimeMillis();
                imapFolder.fetch(null, new long[] { uid }, FETCH_PROFILE_ENVELOPE, null);
                msg = (IMAPMessage) imapFolder.getMessageByUID(uid);
                mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
            } catch (java.lang.IndexOutOfBoundsException | MessageRemovedException e) {
                /*
                 * Obviously message was removed in the meantime
                 */
                LOG.trace("Obviously message was removed in the meantime", e);
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            } catch (MessagingException e) {
                final Exception cause = e.getNextException();
                if (!(cause instanceof BadCommandException)) {
                    throw e;
                }
                // Hm... Something weird with executed "UID FETCH" command; retry manually...
                final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, new long[] { uid });
                if ((null == seqNums) || (0 == seqNums.length)) {
                    LOG.warn("No message with UID '{}' found in folder '{}'", id, fullName, cause);
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
                }
                final int msgnum = seqNums[0];
                if (msgnum < 1) {
                    /*
                     * message-numbers start at 1
                     */
                    LOG.warn("No message with UID '{}' found in folder '{}'", id, fullName, cause);
                    throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
                }
                msg = (IMAPMessage) imapFolder.getMessage(msgnum);
            }
            if (msg == null || msg.isExpunged()) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            msg.setPeek(!markSeen);
            msg.setUID(uid);
            return msg;
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                throw MailExceptionCode.MAIL_NOT_FOUND.create(id, fullName);
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /**
     * Checks existence for a message denoted by given identifier.
     *
     * @param fullName The mailbox full name
     * @param msgUID The message identifier
     * @return <code>true</code> if such a message exists; otherwise <code>false</code> if absent
     * @throws OXException If operation fails
     */
    public boolean exists(String fullName, String msgUID)  throws OXException {
        return existsLong(fullName, parseUnsignedLong(msgUID));
    }

    /**
     * Checks existence for a message denoted by given identifier.
     *
     * @param fullName The mailbox full name
     * @param msgUID The message identifier
     * @return <code>true</code> if such a message exists; otherwise <code>false</code> if absent
     * @throws OXException If operation fails
     */
    public boolean existsLong(String fullName, long msgUID)  throws OXException {
        if (msgUID < 0) {
            return false;
        }
        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            if (0 >= imapFolder.getMessageCount()) {
                return false;
            }
            try {
                return null != imapFolder.getMessageByUID(msgUID);
            } catch (java.lang.IndexOutOfBoundsException | MessageRemovedException e) {
                /*
                 * Obviously message was removed in the meantime
                 */
                LOG.trace("Obviously message was removed in the meantime", e);
                return false;
            } catch (MessagingException e) {
                final Exception cause = e.getNextException();
                if (!(cause instanceof BadCommandException)) {
                    throw e;
                }
                // Hm... Something weird with executed "UID FETCH" command; retry manually...
                final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, new long[] { msgUID });
                if ((null == seqNums) || (0 == seqNums.length)) {
                    LOG.debug("No message with UID '{}' found in folder '{}'", Long.valueOf(msgUID), fullName, cause);
                    return false;
                }
                final int msgnum = seqNums[0];
                if (msgnum < 1) {
                    /*
                     * message-numbers start at 1
                     */
                    LOG.debug("No message with UID '{}' found in folder '{}'", Long.valueOf(msgUID), fullName, cause);
                    return false;
                }
                return true;
            }
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return false;
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private static final FetchProfile FETCH_PROFILE_GET = new FetchProfile(UIDFolder.FetchProfileItem.UID,
                                                                           FetchProfile.Item.ENVELOPE,
                                                                           FetchProfile.Item.FLAGS,
                                                                           FetchProfile.Item.CONTENT_INFO,
                                                                           FetchProfile.Item.SIZE,
                                                                           IMAPFolder.FetchProfileItem.HEADERS);

    private static final FetchProfile FETCH_PROFILE_GET_FOR_VIRTUAL = new FetchProfile(UIDFolder.FetchProfileItem.UID,
                                                                                       FetchProfile.Item.ENVELOPE,
                                                                                       FetchProfile.Item.FLAGS,
                                                                                       FetchProfile.Item.CONTENT_INFO,
                                                                                       FetchProfile.Item.SIZE,
                                                                                       IMAPFolder.FetchProfileItem.HEADERS,
                                                                                       MimeStorageUtility.ORIGINAL_MAILBOX,
                                                                                       MimeStorageUtility.ORIGINAL_UID);

    @Override
    public MailMessage getMessageLong(String fullName, long msgUID, boolean markSeen) throws OXException {
        try {
            int desiredMode = markSeen ? READ_WRITE : READ_ONLY;
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            if (0 >= imapFolder.getMessageCount()) {
                return null;
            }
            return getMessageLongInternal(imapFolder, fullName, msgUID, markSeen, -1);
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return null;
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private MailMessage getMessageLongInternal(IMAPFolder imapFolder, String fullName, long msgUID, boolean markSeen, int numUnreadMessages) throws OXException {
        if (msgUID < 0) {
            return null;
        }
        try {
            IMAPMessage msg;
            try {
                long start = System.currentTimeMillis();
                // Force to pre-load envelope data
                imapFolder.fetch(null, new long[] { msgUID }, null != fullName && fullName.equals(allMessagesFolder(session)) ? FETCH_PROFILE_GET_FOR_VIRTUAL : FETCH_PROFILE_GET, null);
                msg = (IMAPMessage) imapFolder.getMessageByUID(msgUID);
                if (null == msg) {
                    return null;
                }
                long duration = System.currentTimeMillis() - start;

                if (duration > 1000L) {
                    LOG.warn("Retrieval of message {} in folder {} from IMAP mailbox {} took {}msec", Long.valueOf(msgUID), fullName, imapStore, Long.valueOf(duration));
                }

                mailInterfaceMonitor.addUseTime(duration);
            } catch (java.lang.NullPointerException | java.lang.IndexOutOfBoundsException | MessageRemovedException e) {
                /*
                 * Obviously message was removed in the meantime
                 */
                LOG.trace("Obviously message was removed in the meantime", e);
                return null;
            } catch (MessagingException e) {
                final Exception cause = e.getNextException();
                if (!(cause instanceof BadCommandException)) {
                    throw e;
                }
                // Hm... Something weird with executed "UID FETCH" command; retry manually...
                final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, new long[] { msgUID });
                if ((null == seqNums) || (0 == seqNums.length)) {
                    LOG.debug("No message with UID '{}' found in folder '{}'", Long.valueOf(msgUID), fullName, cause);
                    return null;
                }
                final int msgnum = seqNums[0];
                if (msgnum < 1) {
                    /*
                     * message-numbers start at 1
                     */
                    LOG.debug("No message with UID '{}' found in folder '{}'", Long.valueOf(msgUID), fullName, cause);
                    return null;
                }
                msg = (IMAPMessage) imapFolder.getMessage(msgnum);
            }

            // Check existence
            if (msg == null || msg.isExpunged()) {
                return null;
            }
            msg.setUID(msgUID);
            msg.setPeek(!markSeen);

            // Convert to a MailMessage instance
            MailMessage mail;
            try {
                long size = msg.getSize();
                Long origUid = (Long) msg.getItem("X-REAL-UID");
                String origFolder = (String) msg.getItem("X-MAILBOX");
                if (size > MB_5 && isComplex(msg)) {
                    int blkSize = imapStore.getFetchBlockSize();
                    try {
                        // Convert from copied MIME message
                        imapStore.setFetchBlockSize(MB_5);
                        mail = convertFromCopy(msg);
                    } finally {
                        // Restore fetch block size
                        imapStore.setFetchBlockSize(blkSize);
                    }
                } else {
                    // Ensure BODYSTRUCTURE is valid
                    try {
                        msg.getBodystructure();
                        ConverterConfig config = new DefaultConverterConfig(imapAccess.getMailConfig(), false, false);
                        mail = MimeMessageConverter.convertMessage(msg, config);
                    } catch (MessagingException e) {
                        if (!"Unable to load BODYSTRUCTURE".equals(e.getMessage())) {
                            throw e;
                        }

                        // Convert from copied MIME message
                        mail = convertFromCopy(msg);
                    }
                }
                mail.setAccountId(accountId);
                mail.setFolder(fullName);
                if (null != origUid) {
                    mail.setOriginalId(origUid.toString());
                }
                if (null != origFolder) {
                    mail.setOriginalFolder(new FullnameArgument(accountId, origFolder));
                }
                mail.setMailId(Long.toString(msgUID));
                if (numUnreadMessages >= 0) {
                    mail.setUnreadMessages(numUnreadMessages);
                } else {
                    try {
                        mail.setUnreadMessages(IMAPCommandsCollection.getUnread(imapFolder));
                    } catch (Exception e) {
                        LOG.error("Failed to retrieve count for unread/unseen messages from folder '{}'", fullName, e);
                    }
                }
                mail.setMailStructure(MimeMessageUtility.parseMailStructure(msg.getBodystructure(), null, null, mail));
            } catch (OXException e) {
                if (MimeMailExceptionCode.MESSAGE_REMOVED.equals(e) || MailExceptionCode.MAIL_NOT_FOUND.equals(e) || MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.equals(e)) {
                    /*
                     * Obviously message was removed in the meantime
                     */
                    return null;
                }
                /*
                 * Check for generic messaging error
                 */
                if (MimeMailExceptionCode.MESSAGING_ERROR.equals(e)) {
                    /*-
                     * Detected generic messaging error. This most likely hints to a severe JavaMail problem.
                     *
                     * Perform some debug logs for traceability...
                     */
                    LOG.debug("Generic messaging error occurred for mail \"{}\" in folder \"{}\" with login \"{}\" on server \"{}\" (user={}, context={})", Long.valueOf(msgUID), fullName, imapConfig.getLogin(), imapConfig.getServer(), Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), e);
                }
                throw e;
            } catch (java.lang.IndexOutOfBoundsException e) {
                /*
                 * Obviously message was removed in the meantime
                 */
                LOG.trace("Obviously message was removed in the meantime", e);
                return null;
            }
            if (!mail.isSeen() && markSeen) {
                mail.setPrevSeen(false);
                if (imapConfig.isSupportsACLs()) {
                    try {
                        if (aclExtension.canKeepSeen(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                            /*
                             * User has \KEEP_SEEN right: Switch \Seen flag
                             */
                            setSeenFlag(mail, msg);
                        }
                    } catch (MessagingException e) {
                        LOG.warn("/SEEN flag could not be set on message #{} in folder {}", mail.getMailId(), mail.getFolder(), e);
                    }
                } else {
                    setSeenFlag(mail, msg);
                }
            }
            clearCache(imapFolder);
            return setAccountInfo(mail);
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return null;
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private MailMessage convertFromCopy(IMAPMessage msg) throws OXException, MessagingException {
        MimeMessage copy = MimeMessageUtility.newMimeMessage(msg.getMimeStream(), null);
        MailMessage mail = MimeMessageConverter.convertMessage(copy, false);
        // Set flags and received date
        MimeMessageConverter.parseFlags(msg.getFlags(), examineHasAttachmentUserFlags, mail);
        if (!mail.containsColorLabel()) {
            mail.setColorLabel(MailMessage.COLOR_LABEL_NONE);
        }
        mail.setReceivedDate(msg.getReceivedDate());
        return mail;
    }

    private static boolean isComplex(IMAPMessage msg) throws MessagingException {
        try {
            BODYSTRUCTURE bodystructure = msg.getBodystructure();
            int threshold = 10;
            return countNested(bodystructure, 0, threshold) >= threshold;
        } catch (MessagingException e) {
            if (!"Unable to load BODYSTRUCTURE".equals(e.getMessage())) {
                throw e;
            }

            return true;
        }
    }

    private static int countNested(BODYSTRUCTURE bodystructure, int current, int threshold) {
        int count = current;
        if (count >= threshold) {
            return count;
        }

        if (bodystructure.isNested()) {
            count++;
        }

        BODYSTRUCTURE[] bodies = bodystructure.bodies;
        if (null != bodies) {
            for (BODYSTRUCTURE subbody : bodies) {
                count = countNested(subbody, count, threshold);
                if (count >= threshold) {
                    return count;
                }
            }
        }

        return count;
    }

    private static void setSeenFlag(MailMessage mail, IMAPMessage msg) {
        try {
            // Set \Seen fag explicitly although actually fetching content applies \Seen flag automatically,
            // but we cannot know for sure that content will really be fetched. Therefore accept a possibly
            // unnecessary "STORE <seqnum> +FLAGS (\Seen)" command
            msg.setFlags(FLAGS_SEEN, true);
            mail.setFlag(MailMessage.FLAG_SEEN, true);
            final int cur = mail.getUnreadMessages();
            mail.setUnreadMessages(cur <= 0 ? 0 : (cur - 1));
        } catch (Exception e) {
            LOG.warn("/SEEN flag could not be set on message #{} in folder {}", mail.getMailId(), mail.getFolder(), e);
        }
    }

    @Override
    public MailMessage[] searchMessages(String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] mailFields) throws OXException {
        return searchMessages(fullName, indexRange, sortField, order, searchTerm, mailFields, null);
    }

    @Override
    public int getUnreadCount(String folder, SearchTerm<?> searchTerm) throws OXException {
        try {
            openReadOnly(folder);
        } catch (OXException e) {
            if (IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.equals(e)) {
                return 0;
            }
            throw e;
        }
        try {
            searchTerm.applyCapabilities(new CapabilitiesAndOptions(imapConfig.getCapabilities(), true));
            SearchTerm<?> unseenSearchTerm = new ANDTerm(searchTerm, new FlagTerm(MailMessage.FLAG_SEEN, false));
            return IMAPSearch.searchMessages(imapFolder, unseenSearchTerm, imapConfig, session).length;
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return 0;
            }
            throw handleMessagingException(folder, e);
        }
    }

    @Override
    public MailMessage[] searchMessages(String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] mailFields, String[] headerNames) throws OXException {
        try {
            openReadOnly(fullName);
        } catch (OXException e) {
            if (IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.equals(e)) {
                return EMPTY_RETVAL;
            }
            throw e;
        }

        try {
            int messageCount = imapFolder.getMessageCount();
            if (messageCount <= 0) {
                return EMPTY_RETVAL;
            }

            MailSortField effectiveSortField = determineSortFieldForSearch(fullName, sortField);
            MailFields effectiveFields = new MailFields(mailFields);
            prepareMailFieldsForVirtualFolder(effectiveFields, fullName, session);
            SearchTerm<?> searchTermToUse = prepareSearchTerm(searchTerm);
            MailMessage[] mailMessages;
            if (searchViaIMAP(searchTermToUse == null ? new MailFields() : new MailFields(MailField.getMailFieldsFromSearchTerm(searchTermToUse)))) {
                try {
                    mailMessages = performIMAPSearch(effectiveSortField, order, searchTermToUse, effectiveFields, indexRange, headerNames, messageCount);
                } catch (OXException e) {
                    if (!IMAPException.Code.UNSUPPORTED_SORT_FIELD.equals(e)) {
                        throw e;
                    }
                    // Fall back to in-app search&sort
                    mailMessages = performInAppSearch(effectiveSortField, order, searchTermToUse, effectiveFields, indexRange, headerNames, messageCount);
                }
            } else {
                mailMessages = performInAppSearch(effectiveSortField, order, searchTermToUse, effectiveFields, indexRange, headerNames, messageCount);
            }

            if (mailMessages.length == 0) {
                mailMessages = EMPTY_RETVAL;
            }

            return mailMessages;
        } catch (MessagingException e) {
            if (ImapUtility.isInvalidMessageset(e)) {
                return new MailMessage[0];
            }
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            clearCache(imapFolder);
        }
    }

    /**
     * Applies IMAP configurations to search term (e.g. ignoreDeleted).
     *
     * @param searchTerm The search term
     * @return the prepared {@link SearchTerm}
     */
    private SearchTerm<?> prepareSearchTerm(SearchTerm<?> searchTerm) {
        if (IMAPProperties.getInstance().isIgnoreDeletedMails(this.session.getUserId(), this.session.getContextId())) {
            if (searchTerm == null) {
                return new FlagTerm(MailMessage.FLAG_DELETED, false);
            }
            searchTerm.applyCapabilities(new CapabilitiesAndOptions(imapConfig.getCapabilities(), true));
            ImapConfigSearchTermVisitor imapConfigSearchTermVisitor = new ImapConfigSearchTermVisitor();
            searchTerm.accept(imapConfigSearchTermVisitor);
            return imapConfigSearchTermVisitor.checkTerm(searchTerm);
        }

        if (searchTerm != null) {
            searchTerm.applyCapabilities(new CapabilitiesAndOptions(imapConfig.getCapabilities(), true));
        }
        return searchTerm;
    }

    private boolean searchViaIMAP(MailFields fields) throws MessagingException {
        if (!fields.contains(MailField.BODY) && !fields.contains(MailField.FULL)) {
            if (imapConfig.isImapSearch()) {
                return true;
            }

            int msgCount = imapFolder.getMessageCount();
            return (msgCount > getIMAPProperties().getMailFetchLimit());
        }

        if (imapConfig.forceImapSearch()) {
            return true;
        }

        int msgCount = imapFolder.getMessageCount();
        return (msgCount >= getIMAPProperties().getMailFetchLimit());
    }

    private MailMessage[] performIMAPSearch(MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailFields mailFields, IndexRange indexRange, String[] headerNames, int messageCount) throws MessagingException, OXException {
        boolean hasSort = imapConfig.getCapabilities().hasSort();
        boolean fallbackOnFailedSORT = fallbackOnFailedSORT(session, accountId);
        MailFields fields = mailFields;
        if (hasSort && IMAPSort.isValidSortField(sortField)) {
            /*
             * Use SORT command as it allows searching and sorting at once (https://tools.ietf.org/html/rfc5256)
             */
            int[] msgIds;
            {
                ImapSortResult result = IMAPSort.sortMessages(imapFolder, searchTerm, sortField, order, indexRange, allowESORT(session), allowSORTDISPLAY(session, accountId), fallbackOnFailedSORT, imapConfig, session);
                msgIds = result.msgIds;
                if (!result.rangeApplied) {
                    msgIds = applyIndexRange(msgIds, indexRange);
                }
                if (msgIds.length == 0) {
                    if (messageCount > 0 && searchTerm == null) {
                        return fetchSortAndSlice(null, sortField, order, fields, indexRange, headerNames, messageCount);
                    }
                    return EMPTY_RETVAL;
                }
                checkMessagesLimit(msgIds.length);
            }

            /*
             * Fetch (possibly) filtered and sorted sequence numbers
             */
            MailMessage[] mailMessages;
            {
                boolean fetchBody = fields.contains(MailField.BODY) || fields.contains(MailField.FULL);
                FetchProfile fetchProfile = getFetchProfile(fields.toArray(), headerNames, null, null, getIMAPProperties().isFastFetch(), examineHasAttachmentUserFlags && fields.containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
                if (fetchBody) {
                    List<MailMessage> list = fetchMessages(msgIds, fetchProfile);
                    mailMessages = list.toArray(new MailMessage[list.size()]);
                } else {
                    /*
                     * Body content not requested, we simply return IDMailMessage objects filled with requested fields
                     */
                    MailMessage[] tmp = fetchMessages(msgIds, fetchProfile, hasIMAP4rev1(), messageCount);
                    mailMessages = setAccountInfo(tmp);
                }
            }

            if (mailMessages.length == 0) {
                return EMPTY_RETVAL;
            }

            return mailMessages;
        }

        // Check for special sort field
        fields = prepareMailFieldsForSearch(fields, sortField);
        if (hasSort && MailSortField.isFlagSortField(sortField) && null == searchTerm) {
            Optional<int[]> optSeqNumsToFetch = getSeqNumsToFetch(sortField, order, indexRange, fallbackOnFailedSORT);
            if (optSeqNumsToFetch.isEmpty()) {
                // no messages with the given flag
                return performIMAPSearch(MailSortField.RECEIVED_DATE, OrderDirection.DESC, null, fields, indexRange, headerNames, messageCount);
            }
            int[] seqNumsToFetch = optSeqNumsToFetch.get();
            if (seqNumsToFetch.length == 0) {
                return EMPTY_RETVAL;
            }
            checkMessagesLimit(seqNumsToFetch.length);

            MailMessage[] mailMessages;
            {
                boolean fetchBody = fields.contains(MailField.BODY) || fields.contains(MailField.FULL);
                FetchProfile fetchProfile = getFetchProfile(fields.toArray(), headerNames, null, null, getIMAPProperties().isFastFetch(), examineHasAttachmentUserFlags && fields.containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
                if (fetchBody) {
                    List<MailMessage> list = fetchMessages(seqNumsToFetch, fetchProfile);
                    mailMessages = list.toArray(new MailMessage[list.size()]);
                } else {
                    /*
                     * Body content not requested, we simply return IDMailMessage objects filled with requested fields
                     */
                    MailMessage[] tmp = fetchMessages(seqNumsToFetch, fetchProfile, hasIMAP4rev1(), messageCount);
                    mailMessages = setAccountInfo(tmp);
                }
            }

            return mailMessages;
        }

        // Fall-back path...
        int[] msgIds = null == searchTerm ? null : IMAPSearch.issueIMAPSearch(imapFolder, searchTerm, session.getUserId(), session.getContextId());
        /*
         * Do application sort
         */
        return fetchSortAndSlice(msgIds, sortField, order, fields, indexRange, headerNames, messageCount);
    }

    private Optional<int[]> getSeqNumsToFetch(MailSortField sortField, OrderDirection order, IndexRange indexRange, boolean fallbackOnFailedSORT ) throws MessagingException, OXException {
        int[] unflaggedSeqNums = null;
        int[] flaggedSeqNums = null;

        if (OrderDirection.DESC == order) {
            SearchTerm<?> flagSearchTerm = createFlagsSearchTermFor(sortField, false);
            unflaggedSeqNums = IMAPSort.sortMessages(imapFolder, flagSearchTerm, MailSortField.RECEIVED_DATE, OrderDirection.DESC, null, false, false, fallbackOnFailedSORT, imapConfig, session).msgIds;

            if (unflaggedSeqNums.length == 0) {
                // No unflagged messages at all
                return Optional.empty();
            }

            if (null != indexRange && indexRange.start < unflaggedSeqNums.length && indexRange.end <= unflaggedSeqNums.length) {
                // Complete requested range can be served
                return Optional.of(applyIndexRange(unflaggedSeqNums, indexRange));
            }
        } else {
            SearchTerm<?> flagSearchTerm = createFlagsSearchTermFor(sortField, true);
            flaggedSeqNums = IMAPSort.sortMessages(imapFolder, flagSearchTerm, MailSortField.RECEIVED_DATE, OrderDirection.DESC, null, false, false, fallbackOnFailedSORT, imapConfig, session).msgIds;

            if (flaggedSeqNums.length == 0) {
                // No flagged messages at all
                return Optional.empty();
            }

            if (null != indexRange && indexRange.start < flaggedSeqNums.length && indexRange.end <= flaggedSeqNums.length) {
                // Complete requested range can be served
                return Optional.of(applyIndexRange(flaggedSeqNums, indexRange));
            }
        }

        if (null == unflaggedSeqNums) {
            SearchTerm<?> flagSearchTerm = createFlagsSearchTermFor(sortField, false);
            unflaggedSeqNums = IMAPSort.sortMessages(imapFolder, flagSearchTerm, MailSortField.RECEIVED_DATE, OrderDirection.DESC, null, false, false, fallbackOnFailedSORT, imapConfig, session).msgIds;
        }
        if (null == flaggedSeqNums) {
            SearchTerm<?> flagSearchTerm = createFlagsSearchTermFor(sortField, true);
            flaggedSeqNums = IMAPSort.sortMessages(imapFolder, flagSearchTerm, MailSortField.RECEIVED_DATE, OrderDirection.DESC, null, false, false, fallbackOnFailedSORT, imapConfig, session).msgIds;
        }

        int numberOfMessages = unflaggedSeqNums.length + flaggedSeqNums.length;
        int[] sortedSeqNums = new int[numberOfMessages];
        if (OrderDirection.DESC == order) {
            System.arraycopy(unflaggedSeqNums, 0, sortedSeqNums, 0, unflaggedSeqNums.length);
            System.arraycopy(flaggedSeqNums, 0, sortedSeqNums, unflaggedSeqNums.length, flaggedSeqNums.length);
        } else {
            System.arraycopy(flaggedSeqNums, 0, sortedSeqNums, 0, flaggedSeqNums.length);
            System.arraycopy(unflaggedSeqNums, 0, sortedSeqNums, flaggedSeqNums.length, unflaggedSeqNums.length);
        }
        return Optional.of(applyIndexRange(sortedSeqNums, indexRange));
    }

    private SearchTerm<?> createFlagsSearchTermFor(MailSortField sortField, boolean set) throws OXException {
        int flag = 0;
        switch (sortField) {
            case FLAG_ANSWERED:
                flag = MailMessage.FLAG_ANSWERED;
                break;
            case FLAG_DRAFT:
                flag = MailMessage.FLAG_DRAFT;
                break;
            case FLAG_FLAGGED:
                flag = MailMessage.FLAG_FLAGGED;
                break;
            case FLAG_SEEN:
                flag = MailMessage.FLAG_SEEN;
                break;
            case FLAG_FORWARDED:
                try {
                    boolean supportsUserFlags = UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId);
                    if (!supportsUserFlags) {
                        throw MailExceptionCode.UNSUPPORTED_OPERATION.create();
                    }
                    flag = MailMessage.FLAG_FORWARDED;
                } catch (MessagingException e) {
                    throw handleMessagingException(imapFolder.getFullName(), e);
                }
                break;
            case FLAG_HAS_ATTACHMENT:
                if (!examineHasAttachmentUserFlags) {
                    throw MailExceptionCode.UNSUPPORTED_OPERATION.create();
                }
                return new UserFlagTerm(MailMessage.USER_HAS_ATTACHMENT, set);
            default:
                break;
        }

        return new FlagTerm(flag, set);
    }

    private MailMessage[] performInAppSearch(MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailFields usedFields, IndexRange indexRange, String[] headerNames, int messageCount) throws MessagingException, OXException {
        int[] seqnums = null;
        if (searchTerm != null) {
            MailFields mailFields = new MailFields(MailField.getMailFieldsFromSearchTerm(searchTerm));
            int chunkSize = -1;
            if (mailFields.contains(MailField.BODY) || mailFields.contains(MailField.FULL)) {
                chunkSize = 100;
            }

            seqnums = IMAPSearch.searchByTerm(imapFolder, searchTerm, chunkSize, messageCount);
            checkMessagesLimit(seqnums.length);
        }

        return fetchSortAndSlice(seqnums, sortField, order, usedFields, indexRange, headerNames, messageCount);
    }

    private MailMessage[] fetchSortAndSlice(int[] seqnums, MailSortField sortField, OrderDirection order, MailFields fields, IndexRange indexRange, String[] headerNames, int messageCount) throws OXException, MessagingException {
        boolean fastFetch = getIMAPProperties().isFastFetch();

        if (null == indexRange) {
            // Fetch them all
            checkMessagesLimit(seqnums != null ? seqnums.length : messageCount);
            FetchProfile fetchProfile = getFetchProfile(fields.toArray(), headerNames, null, null, fastFetch, examineHasAttachmentUserFlags && fields.containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
            List<MailMessage> list;
            if (fields.contains(MailField.BODY) || fields.contains(MailField.FULL)) { // Fetch BODY
                list = fetchMessages(seqnums, fetchProfile);
            } else {
                MailMessage[] tmp = fetchMessages(seqnums, fetchProfile, hasIMAP4rev1(), messageCount);
                list = Arrays.stream(tmp).filter(Objects::nonNull).collect(Collectors.toList());
            }

            if (list.isEmpty()) {
                return EMPTY_RETVAL;
            }

            // Sort
            Collections.sort(list, MailMessageComparatorFactory.createComparator(sortField, order, getLocale(), getSession(), getIMAPProperties().isUserFlagsEnabled()));

            // Return
            MailMessage[] mailMessages = list.toArray(new MailMessage[list.size()]);
            return mailMessages.length > 0 ? mailMessages : EMPTY_RETVAL;
        }

        // A certain range is requested, thus grab messages only with ID and sort field information
        boolean hasIMAP4rev1 = hasIMAP4rev1();
        if (sortField == MailSortField.RECEIVED_DATE) {
            // Sorting by INTERNALDATE can be done by numeric message number as well
            int[] msgnums;
            if (seqnums == null) {
                msgnums = newMsgNums(messageCount);
            } else {
                msgnums = Arrays.copyOf(seqnums, seqnums.length);
                Arrays.sort(msgnums);
            }
            if (order == OrderDirection.DESC) {
                ArrayUtils.reverse(msgnums);
            }
            int fromIndex = indexRange.start;
            if ((fromIndex) > msgnums.length) {
                // Return empty array if start is out of range
                return new MailMessage[0];
            }

            // Reset end index if out of range
            int toIndex = indexRange.end;
            if (toIndex > msgnums.length) {
                toIndex = msgnums.length;
            }

            int[] seqnumsToFetch = new int[toIndex - fromIndex];
            checkMessagesLimit(seqnumsToFetch.length);
            for (int k = fromIndex, i = 0; k < toIndex; k++, i++) {
                seqnumsToFetch[i] = msgnums[k];
            }

            // Fetch with proper attributes by UID
            FetchProfile fetchProfile = getFetchProfile(fields.toArray(), headerNames, null, null, fastFetch, examineHasAttachmentUserFlags && fields.containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
            MailMessage[] mailMessages;
            boolean fetchBody = fields.contains(MailField.BODY) || fields.contains(MailField.FULL);
            if (fetchBody) {
                List<MailMessage> tmp = fetchMessages(seqnumsToFetch, fetchProfile);
                mailMessages = tmp.toArray(new MailMessage[tmp.size()]);
            } else {
                mailMessages = fetchMessages(seqnumsToFetch, fetchProfile, hasIMAP4rev1, messageCount);
            }
            setAccountInfo(mailMessages);
            return mailMessages;
        }

        // By another sort field
        MailMessage[] sortedRange;
        {
            // Inject interceptor to retrieve sorted range
            RangeSortingInterceptor interceptor = new RangeSortingInterceptor(indexRange, MailMessageComparatorFactory.createComparator(sortField, order, getLocale(), getSession(), getIMAPProperties().isUserFlagsEnabled()));
            MailField[] mailFields = new MailField[] { MailField.ID, MailField.toField(sortField.getListField()) };
            FetchProfile fp = getFetchProfile(mailFields, fastFetch, examineHasAttachmentUserFlags && new MailFields(mailFields).containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
            sortedRange = fetchMessages(seqnums, fp, hasIMAP4rev1, messageCount, Optional.of(interceptor));
        }

        if (sortedRange.length <= 0) {
            return EMPTY_RETVAL;
        }

        // Determine UIDs
        checkMessagesLimit(sortedRange.length);
        long[] uids = new long[sortedRange.length];
        for (int i = sortedRange.length; i-- > 0;) {
            uids[i] = ((IDMailMessage) sortedRange[i]).getUid();
        }
        sortedRange = null;

        // Fetch with proper attributes by UID
        FetchProfile fetchProfile = getFetchProfile(fields.toArray(), headerNames, null, null, fastFetch, examineHasAttachmentUserFlags && fields.containsAny(MailField.ATTACHMENT, MailField.CONTENT_TYPE), previewMode);
        MailMessage[] mailMessages;
        boolean fetchBody = fields.contains(MailField.BODY) || fields.contains(MailField.FULL);
        if (fetchBody) {
            List<MailMessage> tmp = fetchMessages(uids, fetchProfile);
            mailMessages = tmp.toArray(new MailMessage[tmp.size()]);
        } else {
            mailMessages = fetchMessages(uids, fetchProfile, hasIMAP4rev1);
        }
        setAccountInfo(mailMessages);
        return mailMessages;
    }

    private static int[] newMsgNums(int messageCount) {
        int[] retval = new int[messageCount];
        for (int i = messageCount; i-- > 0;) {
            retval[i] = i + 1;
        }
        return retval;
    }

    private MailMessage[] fetchMessages(long[] uids, FetchProfile fetchProfile, boolean hasIMAP4rev1) throws MessagingException {
        try {
            long start = System.currentTimeMillis();
            MailMessage[] mailMessages = new MailMessageFetchIMAPCommand(imapFolder, hasIMAP4rev1, uids, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, Optional.empty()).doCommand();
            long time = System.currentTimeMillis() - start;
            mailInterfaceMonitor.addUseTime(time);
            LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(uids.length), Long.valueOf(time));
            return mailMessages;
        } catch (MessagingException e) {
            if (!MimeMailException.isCommandFailedException(e)) {
                throw e;
            }

            // Chunk-wise
            List<MailMessage> l = new LinkedList<>();
            int length = uids.length;
            int chunkSize = 25;

            int off = 0;
            while (off < length) {
                int end = off + chunkSize;
                long[] muids;

                if (end > length) {
                    end = length;
                    muids = new long[end - off];
                } else {
                    muids = new long[chunkSize];
                }

                System.arraycopy(uids, off, muids, 0, muids.length);

                long start = System.currentTimeMillis();
                MailMessage[] mms = new MailMessageFetchIMAPCommand(imapFolder, hasIMAP4rev1, muids, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, Optional.empty()).doCommand();
                long time = System.currentTimeMillis() - start;
                mailInterfaceMonitor.addUseTime(time);
                LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(muids.length), Long.valueOf(time));

                for (MailMessage mail : mms) {
                    l.add(mail);
                }

                off = end;
            }

            return l.toArray(new MailMessage[l.size()]);
        }
    }

    private MailMessage[] fetchMessages(int[] seqnums, FetchProfile fetchProfile, boolean hasIMAP4rev1, int messageCount) throws MessagingException, OXException {
        if (null == seqnums) {
            if (messageCount > 0) {
                checkMessagesLimit(messageCount);
            }
        } else {
            checkMessagesLimit(seqnums.length);
        }
        return fetchMessages(seqnums, fetchProfile, hasIMAP4rev1, messageCount, Optional.empty());
    }

    private MailMessage[] fetchMessages(int[] seqnums, FetchProfile fetchProfile, boolean hasIMAP4rev1, int messageCount, Optional<RangeSortingInterceptor> optionalInterceptor) throws MessagingException {
        try {
            long start = System.currentTimeMillis();
            MailMessage[] mailMessages;
            if (null == seqnums) {
                mailMessages = new MailMessageFetchIMAPCommand(imapFolder, hasIMAP4rev1, messageCount, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, optionalInterceptor).doCommand();
            } else {
                mailMessages = new MailMessageFetchIMAPCommand(imapFolder, hasIMAP4rev1, seqnums, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, optionalInterceptor).doCommand();
            }
            long time = System.currentTimeMillis() - start;
            mailInterfaceMonitor.addUseTime(time);
            LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(mailMessages.length), Long.valueOf(time));
            return mailMessages;
        } catch (MessagingException e) {
            if (!MimeMailException.isCommandFailedException(e)) {
                throw e;
            }

            // Chunk-wise
            List<MailMessage> l = new LinkedList<>();
            int length = null == seqnums ? imapFolder.getMessageCount() : seqnums.length;
            int chunkSize = 25;

            int off = 0;
            while (off < length) {
                int end = off + chunkSize;
                int[] mseqnums;

                if (end > length) {
                    end = length;
                    mseqnums = new int[end - off];
                } else {
                    mseqnums = new int[chunkSize];
                }

                if (null == seqnums) {
                    for (int i = mseqnums.length, v = end; i-- > 0;) {
                        mseqnums[i] = v--;
                    }
                } else {
                    System.arraycopy(seqnums, off, mseqnums, 0, mseqnums.length);
                }

                long start = System.currentTimeMillis();
                MailMessage[] mms = new MailMessageFetchIMAPCommand(imapFolder, hasIMAP4rev1, mseqnums, fetchProfile, imapServerInfo, examineHasAttachmentUserFlags, previewMode, Optional.empty()).doCommand();
                long time = System.currentTimeMillis() - start;
                mailInterfaceMonitor.addUseTime(time);
                LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(mseqnums.length), Long.valueOf(time));

                for (MailMessage mail : mms) {
                    l.add(mail);
                }

                off = end;
            }

            if (optionalInterceptor.isPresent()) {
                RangeSortingInterceptor interceptor = optionalInterceptor.get();
                Collections.sort(l, interceptor.getComparator());
                l = applyIndexRange(l, interceptor.getIndexRange());
            }

            return l.toArray(new MailMessage[l.size()]);
        }
    }

    /**
     * Pre-fetches a set of messages based on the given {@link FetchProfile} and
     * converts them to {@link MailMessage}s which are backed by their original {@link Message}
     * objects.
     *
     * @param uids The UIDs to fetch or <code>null</code> to fetch all mails
     * @param fetchProfile
     * @return
     * @throws MessagingException
     * @throws OXException
     */
    private List<MailMessage> fetchMessages(long[] uids, FetchProfile fetchProfile) throws MessagingException, OXException {
        return fetchMessages(uids == null ? imapFolder.getMessages() : imapFolder.getMessagesByUID(uids), fetchProfile);
    }

    /**
     * Pre-fetches a set of messages based on the given {@link FetchProfile} and
     * converts them to {@link MailMessage}s which are backed by their original {@link Message}
     * objects.
     *
     * @param msgIds The IDs to fetch or <code>null</code> to fetch all mails
     * @param fetchProfile
     * @return
     * @throws MessagingException
     * @throws OXException
     */
    private List<MailMessage> fetchMessages(int[] msgIds, FetchProfile fetchProfile) throws MessagingException, OXException {
        return fetchMessages(msgIds == null ? imapFolder.getMessages() : imapFolder.getMessages(msgIds), fetchProfile);
    }

    private List<MailMessage> fetchMessages(Message[] messages, FetchProfile fetchProfile) throws MessagingException, OXException {
        Message[] msgs = messages;
        try {
            long start = System.currentTimeMillis();
            imapFolder.fetch(msgs, fetchProfile);
            long time = System.currentTimeMillis() - start;
            mailInterfaceMonitor.addUseTime(time);
            LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(msgs.length), Long.valueOf(time));
        } catch (MessagingException e) {
            if (!MimeMailException.isCommandFailedException(e)) {
                throw e;
            }

            // Chunk-wise
            List<Message> l = new LinkedList<>();
            int length = msgs.length;
            int chunkSize = 25;

            int off = 0;
            while (off < length) {
                int end = off + chunkSize;
                Message[] mmsg;

                if (end > length) {
                    end = length;
                    mmsg = new Message[end - off];
                } else {
                    mmsg = new Message[chunkSize];
                }

                System.arraycopy(msgs, off, mmsg, 0, mmsg.length);

                long start = System.currentTimeMillis();
                imapFolder.fetch(mmsg, fetchProfile);
                long time = System.currentTimeMillis() - start;
                mailInterfaceMonitor.addUseTime(time);
                LOG.debug("IMAP fetch for {} messages took {}msec", Integer.valueOf(mmsg.length), Long.valueOf(time));

                for (Message m : mmsg) {
                    if (null != m) {
                        l.add(m);
                    }
                }

                off = end;
            }

            msgs = l.toArray(new Message[l.size()]);
        }

        if (msgs.length == 0) {
            return Collections.emptyList();
        }

        try {
            MailAccount mailAccount = getMailAccount();
            int unreadMessages = IMAPCommandsCollection.getUnread(imapFolder);
            return Arrays.stream(msgs).filter(message -> message != null && !message.isExpunged()).map(message -> toMailMessageUnchecked(mailAccount, imapFolder.getFullName(), message, unreadMessages)).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IMAPRuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException oxe) {
                throw oxe;
            }
            if (cause instanceof MessagingException me) {
                throw me;
            }
            throw IMAPException.IMAPCode.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        }
    }

    private static int[] applyIndexRange(int[] sortSeqNums, IndexRange indexRange) {
        if (indexRange == null) {
            return sortSeqNums;
        }

        if (sortSeqNums.length == 0) {
            return new int[0];
        }

        int fromIndex = indexRange.start;
        if ((fromIndex) > sortSeqNums.length) {
            /*
             * Return empty iterator if start is out of range
             */
            return new int[0];
        }

        int toIndex = indexRange.end;
        /*
         * Reset end index if out of range
         */
        if (toIndex > sortSeqNums.length) {
            toIndex = sortSeqNums.length;
        }

        int retvalLength = toIndex - fromIndex;
        int[] retval = new int[retvalLength];
        System.arraycopy(sortSeqNums, fromIndex, retval, 0, retvalLength);
        return retval;
    }

    private static List<MailMessage> applyIndexRange(List<MailMessage> mails, IndexRange indexRange) {
        if (indexRange == null) {
            return mails;
        }
        if (mails == null) {
            return Collections.emptyList();
        }
        int size = mails.size();
        if (size <= 0) {
            return Collections.emptyList();
        }

        final int fromIndex = indexRange.start;
        if ((fromIndex) > size) {
            // Return empty list if start is out of range
            return Collections.emptyList();
        }

        // Reset end index if out of range
        int toIndex = indexRange.end;
        if (toIndex >= size) {
            toIndex = size;
        }
        return mails.subList(fromIndex, toIndex);
    }

    private MailSortField determineSortFieldForSearch(String fullName, MailSortField requestedSortField) throws OXException {
        final MailSortField effectiveSortField;
        if (null == requestedSortField) {
            effectiveSortField = MailSortField.RECEIVED_DATE;
        } else {
            if (MailSortField.SENT_DATE == requestedSortField) {
                final String draftsFullname = imapAccess.getFolderStorage().getDraftsFolder();
                if (fullName.equals(draftsFullname)) {
                    effectiveSortField = MailSortField.RECEIVED_DATE;
                } else {
                    effectiveSortField = requestedSortField;
                }
            } else {
                effectiveSortField = requestedSortField;
            }
        }

        return effectiveSortField;
    }

    /**
     * Converts a {@link Message} to a {@link MailMessage} that is backed by the original object.
     *
     * @param mailAccount The mail account associated with the operation
     * @param fullName The full name
     * @param message The message to convert
     * @param unreadMessages The number of unread messages in IMAP folder
     * @return The appropriate mail message representation
     * @throws IMAPRuntimeException The wrapping exception for either <code>MessagingException</code> or <code>OXException</code>
     */
    private MailMessage toMailMessageUnchecked(MailAccount mailAccount, String fullName, Message message, int unreadMessages) {
        try {
            return toMailMessage(mailAccount, fullName, message, unreadMessages);
        } catch (MessagingException | OXException e) {
            throw IMAPRuntimeException.runtimeExceptionFor(e);
        }
    }

    /**
     * Converts a {@link Message} to a {@link MailMessage} that is backed by the original object.
     *
     * @param mailAccount The mail account associated with the operation
     * @param fullName The full name
     * @param message The message to convert
     * @param unreadMessages The number of unread messages in IMAP folder
     * @return The appropriate mail message representation
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If an Open-Xchange error occurs
     */
    private MailMessage toMailMessage(MailAccount mailAccount, String fullName, Message message, int unreadMessages) throws MessagingException, OXException {
        final IMAPMessage imapMessage = (IMAPMessage) message;
        final long msgUID = imapFolder.getUID(message);
        imapMessage.setUID(msgUID);
        imapMessage.setPeek(true);
        final MailMessage mail;
        try {
            mail = MimeMessageConverter.convertMessage(imapMessage, false);
            mail.setFolder(fullName);
            mail.setMailId(Long.toString(msgUID));
            mail.setUnreadMessages(unreadMessages);
            mail.setAccountId(mailAccount.getId());
            mail.setAccountName(mailAccount.getName());
            return mail;
        } catch (OXException e) {
            if (MimeMailExceptionCode.MESSAGE_REMOVED.equals(e) || MailExceptionCode.MAIL_NOT_FOUND.equals(e) || MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.equals(e)) {
                /*
                 * Obviously message was removed in the meantime
                 */
                return null;
            }
            /*
             * Check for generic messaging error
             */
            if (MimeMailExceptionCode.MESSAGING_ERROR.equals(e)) {
                /*-
                 * Detected generic messaging error. This most likely hints to a severe JavaMail problem.
                 *
                 * Perform some debug logs for traceability...
                 */
                LOG.debug("Generic messaging error occurred for mail \"{}\" in folder \"{}\" with login \"{}\" on server \"{}\" (user={}, context={})", Long.valueOf(msgUID), fullName, imapConfig.getLogin(), imapConfig.getServer(), Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), e);
            }
            throw e;
        } catch (@SuppressWarnings("unused") java.lang.IndexOutOfBoundsException e) {
            /*
             * Obviously message was removed in the meantime
             */
            return null;
        }
    }

    @Override
    public List<List<MailMessage>> getThreadSortedMessages(String fullName, boolean includeSent, boolean cache, IndexRange indexRange, long max, MailSortField sortField, OrderDirection order, MailField[] mailFields, SearchTerm<?> searchTerm) throws OXException {
        return getThreadSortedMessages(fullName, includeSent, cache, indexRange, max, sortField, order, mailFields, null, searchTerm);
    }

    @Override
    public List<List<MailMessage>> getThreadSortedMessages(String fullName, boolean includeSent, boolean cache, IndexRange indexRange, long max, MailSortField sortField, OrderDirection order, MailField[] mailFields, String[] headerNames, SearchTerm<?> searchTerm) throws OXException {
        IMAPConversationWorker conversationWorker = new IMAPConversationWorker(this, imapFolderStorage);
        SearchTerm<?> searchTermToUse = prepareSearchTerm(searchTerm);
        return conversationWorker.getThreadSortedMessages(fullName, includeSent, cache, indexRange, max, sortField, order, mailFields, headerNames, searchTermToUse);
    }

    @Override
    public MailMessage[] getThreadSortedMessages(String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] mailFields) throws OXException {
        IMAPConversationWorker conversationWorker = new IMAPConversationWorker(this, imapFolderStorage);
        SearchTerm<?> searchTermToUse = prepareSearchTerm(searchTerm);
        return conversationWorker.getThreadSortedMessages(fullName, indexRange, sortField, order, searchTermToUse, mailFields);
    }

    @Override
    public boolean isThreadReferencesSupported() throws OXException {
        return imapConfig.getImapCapabilities().hasThreadReferences();
    }

    @Override
    public List<MailThread> getThreadReferences(String fullName, int size, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] mailFields, String[] headerNames) throws OXException {
        IMAPConversationWorker conversationWorker = new IMAPConversationWorker(this, imapFolderStorage);
        SearchTerm<?> searchTermToUse = prepareSearchTerm(searchTerm);
        return conversationWorker.getThreadReferences(fullName, size, sortField, order, searchTermToUse, mailFields, headerNames);
    }

    @Override
    public boolean isMailFilterApplicationSupported() throws OXException {
        return imapConfig.getImapCapabilities().hasMailFilterApplication();
    }

    @Override
    public List<MailFilterResult> applyMailFilterScript(String fullName, String mailFilterScript, SearchTerm<?> searchTerm, boolean acceptOkFilterResults) throws OXException {
        if (Strings.isEmpty(mailFilterScript)) {
            // Nothing to apply
            return Collections.emptyList();
        }

        int filterReadTimeout = getIMAPProperties().getFilterReadTimeout();
        javax.mail.ReadTimeoutRestorer prevTimeout = null;
        try {
            /*
             * Apply special read timeout (if any)
             */
            if (filterReadTimeout >= 0) {
                prevTimeout = imapStore.setAndGetReadTimeout(filterReadTimeout == 0 ? Integer.MAX_VALUE : filterReadTimeout);
            }
            /*
             * Open and check user rights on source folder
             */
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            /*
             * Apply filter
             */
            javax.mail.search.SearchTerm jmsSearchTerm;
            if (searchTerm == null) {
                jmsSearchTerm = null;
            } else {
                searchTerm.applyCapabilities(new CapabilitiesAndOptions(imapConfig.getCapabilities(), false));
                if (searchTerm.containsWildcard()) {
                    jmsSearchTerm = searchTerm.getNonWildcardJavaMailSearchTerm();
                } else {
                    jmsSearchTerm = searchTerm.getJavaMailSearchTerm();
                }
            }
            LOG.debug("Going to apply the following mail filter script to folder \"{}\" {} a search term:{}{}", fullName, jmsSearchTerm == null ? "without" : "with", Strings.getLineSeparator(), mailFilterScript);
            FilterResult[] filterResults = imapFolder.filter(Filter.getScriptFilter(mailFilterScript), jmsSearchTerm, acceptOkFilterResults);
            /*
             * Return results
             */
            return Arrays.stream(filterResults).filter(Objects::nonNull).map(fr -> (MailFilterResult) new IMAPMailFilterResult(fr)).collect(Collectors.toList());
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (prevTimeout != null) {
                try {
                    prevTimeout.restore();
                } catch (Exception e) {
                    LOG.warn("Failed to restore read timeout", e);
                }
            }
        }
    }

    @Override
    public MailMessage[] getUnreadMessages(String fullName, MailSortField sortField, OrderDirection order, MailField[] mailFields, int limit) throws OXException {
        try {
            openReadOnly(fullName);
            MailMessage[] mails;
            {
                /*
                 * Ensure mail ID is contained in requested fields
                 */
                final MailFields fieldSet = new MailFields(mailFields);
                IMAPMessageStorage.prepareMailFieldsForVirtualFolder(fieldSet, fullName, session);
                final MailField[] fields = fieldSet.toArray();
                /*
                 * Get ( & fetch) new messages
                 */
                Message[] msgs = IMAPCommandsCollection.getUnreadMessages(imapFolder, fields, sortField, order, getIMAPProperties().isFastFetch(), limit, imapServerInfo, session, imapConfig);
                if ((msgs == null) || (msgs.length == 0) || limit == 0) {
                    return EMPTY_RETVAL;
                }
                checkMessagesLimit(limit > 0 ? limit : msgs.length);
                /*
                 * Sort
                 */
                mails = convert2Mails(msgs, fields);
                if (fieldSet.contains(MailField.ACCOUNT_NAME) || fieldSet.contains(MailField.FULL)) {
                    setAccountInfo(mails);
                }
                final List<MailMessage> msgList = Arrays.asList(mails);
                Collections.sort(msgList, MailMessageComparatorFactory.createComparator(sortField, order, getLocale(), imapFolderStorage.getSession(), getIMAPProperties().isUserFlagsEnabled()));

                mails = msgList.toArray(mails);
            }
            /*
             * Check for limit
             */
            if (limit > 0 && limit < mails.length) {
                final MailMessage[] retval = new MailMessage[limit];
                System.arraycopy(mails, 0, retval, 0, limit);
                mails = retval;
            }
            return mails;
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            clearCache(imapFolder);
        }
    }

    @Override
    public void deleteMessagesLong(String fullName, long[] msgUIDs, boolean hardDelete) throws OXException {
        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights( imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            /*
             * Set marker
             */
            final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                if (hardDelete || getUserSettingMail().isHardDeleteMsgs()) {
                    blockwiseDeletion(msgUIDs, false, null, DeleteOptions.optionsForNeiherNor());
                    notifyIMAPFolderModification(fullName);
                    return;
                }
                final String trashFullname = imapAccess.getFolderStorage().getTrashFolder();
                if (null == trashFullname) {
                    LOG.error("\n\tDefault trash folder is not set: aborting delete operation");
                    throw IMAPException.create(IMAPException.Code.MISSING_DEFAULT_FOLDER_NAME, imapConfig, session, "trash");
                }
                final boolean backup = (!isSubfolderOf(fullName, trashFullname, getSeparator(imapFolder)));
                blockwiseDeletion(msgUIDs, backup, backup ? trashFullname : null, DeleteOptions.optionsForNeiherNor());
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedSessionData(msgUIDs, accountId, session, fullName);
                }
                notifyIMAPFolderModification(fullName);
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public boolean isEnhancedDeletionSupported() throws OXException {
        return imapConfig.getImapCapabilities().hasUIDPlus();
    }

    @Override
    public MailPath[] hardDeleteMessages(String fullName, String[] mailIds) throws OXException {
        if (null == mailIds) {
            return new MailPath[0];
        }
        if (0 == mailIds.length) {
            return new MailPath[0];
        }

        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights( imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            long[] msgUIDs = uids2longs(mailIds);
            /*
             * Set marker
             */
            final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                MailPath[] removedPaths = blockwiseDeletion(msgUIDs, false, null, DeleteOptions.optionsForReturnRemovedOnes());
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedSessionData(msgUIDs, accountId, session, fullName);
                }
                notifyIMAPFolderModification(fullName);
                return removedPaths;
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailPath[] deleteMessagesEnhanced(String fullName, String[] mailIds, boolean hardDelete) throws OXException {
        if (null == mailIds) {
            return new MailPath[0];
        }
        if (0 == mailIds.length) {
            return new MailPath[0];
        }

        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights( imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            long[] msgUIDs = uids2longs(mailIds);
            /*
             * Set marker
             */
            final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                if (hardDelete || getUserSettingMail().isHardDeleteMsgs()) {
                    blockwiseDeletion(msgUIDs, false, null, DeleteOptions.optionsForNeiherNor());
                    notifyIMAPFolderModification(fullName);
                    return new MailPath[0];
                }
                final String trashFullname = imapAccess.getFolderStorage().getTrashFolder();
                if (null == trashFullname) {
                    LOG.error("\n\tDefault trash folder is not set: aborting delete operation");
                    throw IMAPException.create(IMAPException.Code.MISSING_DEFAULT_FOLDER_NAME, imapConfig, session, "trash");
                }
                final boolean backup = (!isSubfolderOf(fullName, trashFullname, getSeparator(imapFolder)));
                MailPath[] targetPaths = blockwiseDeletion(msgUIDs, backup, backup ? trashFullname : null, backup ? DeleteOptions.optionsForReturnTargetPaths() : DeleteOptions.optionsForNeiherNor());
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedSessionData(msgUIDs, accountId, session, fullName);
                }
                notifyIMAPFolderModification(fullName);
                return targetPaths;
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private MailPath[] blockwiseDeletion(long[] msgUIDs, boolean backup, String trashFullname, DeleteOptions deleteOptions) throws OXException, MessagingException {
        if (0 == msgUIDs.length) {
            // Nothing to do on empty ID array
            return deleteOptions.returnTargetPaths || deleteOptions.returnRemovedOnes ? new MailPath[0] : null;
        }
        List<MailPath> targetPaths = deleteOptions.returnTargetPaths ? new ArrayList<>(msgUIDs.length) : null;
        List<MailPath> removedPaths = deleteOptions.returnRemovedOnes ? new ArrayList<>(msgUIDs.length) : null;
        long[] remain;
        int blockSize = getIMAPProperties().getBlockSize();
        if (blockSize > 0 && msgUIDs.length > blockSize) {
            /*
             * Block-wise deletion
             */
            int offset = 0;
            long[] tmp = new long[blockSize];
            for (int len = msgUIDs.length; len > blockSize; len -= blockSize) {
                System.arraycopy(msgUIDs, offset, tmp, 0, tmp.length);
                offset += blockSize;
                MailPath[] chunk = deleteByUIDs(trashFullname, backup, tmp, deleteOptions);
                if (targetPaths != null && chunk != null) {
                    for (MailPath mailPath : chunk) {
                        targetPaths.add(mailPath); // Supports null elements
                    }
                }
                if (removedPaths != null && chunk != null) {
                    for (MailPath mailPath : chunk) {
                        removedPaths.add(mailPath); // Supports null elements
                    }
                }
            }
            remain = new long[msgUIDs.length - offset];
            System.arraycopy(msgUIDs, offset, remain, 0, remain.length);
        } else {
            remain = msgUIDs;
        }
        MailPath[] chunk = deleteByUIDs(trashFullname, backup, remain, deleteOptions);
        if (targetPaths != null && chunk != null) {
            for (MailPath mailPath : chunk) {
                targetPaths.add(mailPath); // Supports null elements
            }
        }
        if (removedPaths != null && chunk != null) {
            for (MailPath mailPath : chunk) {
                removedPaths.add(mailPath); // Supports null elements
            }
        }
        /*
         * Close folder to force JavaMail-internal message cache update
         */
        imapFolder.close(false);
        resetIMAPFolder();
        if (deleteOptions.returnTargetPaths) {
            return targetPaths != null ? targetPaths.toArray(new MailPath[targetPaths.size()]) : null;
        }
        return deleteOptions.returnRemovedOnes && removedPaths != null ? removedPaths.toArray(new MailPath[removedPaths.size()]) : null;
    }

    private MailPath[] deleteByUIDs(String trashFullname, boolean backup, long[] uids, DeleteOptions deleteOptions) throws OXException, MessagingException {
        MailPath[] targetPaths = null;
        if (backup) {
            /*
             * Copy messages to folder "TRASH"
             */
            final boolean supportsMove = imapConfig.asMap().containsKey("MOVE");
            try {
                AbstractIMAPCommand<long[]> command;
                if (supportsMove) {
                    command = new MoveIMAPCommand(imapFolder, uids, trashFullname, false, !deleteOptions.returnTargetPaths);
                } else {
                    command = new CopyIMAPCommand(imapFolder, uids, trashFullname, false, !deleteOptions.returnTargetPaths);
                }
                long[] targetUids = command.doCommand();
                if (deleteOptions.returnTargetPaths) {
                    targetPaths = new MailPath[targetUids.length];
                    for (int i = targetUids.length; i-- > 0;) {
                        long uid = targetUids[i];
                        targetPaths[i] = uid < 0 ? null : new MailPath(accountId, trashFullname, Long.toString(uid));
                    }
                }
            } catch (MessagingException e) {
                final String err = Strings.toLowerCase(e.getMessage());
                if (err.indexOf("[nonexistent]") >= 0) {
                    // Obviously message does not/no more exist
                    return deleteOptions.returnTargetPaths ? new MailPath[0] : null;
                }
                if (err.indexOf("quota") >= 0) {
                    /*
                     * We face an Over-Quota-Exception
                     */
                    throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e);
                }
                final Exception nestedExc = e.getNextException();
                if (nestedExc != null) {
                    if (Strings.toLowerCase(nestedExc.getMessage()).indexOf("quota") >= 0) {
                        /*
                         * We face an Over-Quota-Exception
                         */
                        throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e);
                    }
                    if (nestedExc instanceof ProtocolException pe) {
                        OXException oxe = MimeMailException.handleProtocolExceptionByResponseCode(pe, imapConfig, session, imapFolder);
                        if (null != oxe) {
                            throw oxe;
                        }
                    }
                }
                throw IMAPException.create(IMAPException.Code.MOVE_ON_DELETE_FAILED, imapConfig, session, e);
            }
            if (supportsMove) {
                return deleteOptions.returnTargetPaths ? targetPaths : null;
            }
        }
        /*
         * Mark messages as \DELETED...
         */
        new FlagsIMAPCommand(imapFolder, uids, FLAGS_DELETED, true, true, false).doCommand();
        /*
         * ... and perform EXPUNGE
         */
        try {
            if (deleteOptions.returnRemovedOnes) {
                long[] removedOnes = IMAPCommandsCollection.uidExpungeWithFallback(imapFolder, uids, imapConfig.getImapCapabilities().hasUIDPlus(), true);
                MailPath[] removedPaths = new MailPath[removedOnes.length];
                for (int i = 0; i < removedOnes.length; i++) {
                    long uid = removedOnes[i];
                    removedPaths[i] = new MailPath(accountId, imapFolder.getFullName(), Long.toString(uid));
                }
                return removedPaths;
            }

            IMAPCommandsCollection.uidExpungeWithFallback(imapFolder, uids, imapConfig.getImapCapabilities().hasUIDPlus());
        } catch (FolderClosedException | StoreClosedException e) {
            /*
             * Not possible to retry since connection is broken
             */
            MailConfig mailConfig = imapAccess.getMailConfig();
            throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, mailConfig.getServer(), mailConfig.getLogin(), I(mailConfig.getMailProperties().getConnectTimeout()));
        } catch (MessagingException e) {
            throw IMAPException.create(
                IMAPException.Code.UID_EXPUNGE_FAILED,
                imapConfig,
                session,
                e,
                Arrays.toString(uids),
                imapFolder.getFullName(),
                e.getMessage());
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
        /*
         * Return target paths (if demanded)
         */
        return deleteOptions.returnTargetPaths ? targetPaths : null;
    }

    @Override
    public void copyMessages(String sourceFolder, String destFolder) throws OXException {
        copyOrMoveAllMessages(sourceFolder, destFolder, false);
    }

    @Override
    public void moveMessages(String sourceFolder, String destFolder) throws OXException {
        copyOrMoveAllMessages(sourceFolder, destFolder, true);
    }

    private void copyOrMoveAllMessages(String sourceFullName, String destFullName, boolean move) throws OXException {
        if (ROOT_FOLDER_ID.equals(destFullName)) {
            throw IMAPException.create(IMAPException.Code.NO_ROOT_MOVE, imapConfig, session);
        }
        if ((sourceFullName == null) || (sourceFullName.length() == 0)) {
            throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "source");
        } else if ((destFullName == null) || (destFullName.length() == 0)) {
            throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "target");
        } else if (sourceFullName.equals(destFullName) && move) {
            // Source equals destination, just return the message ids without throwing an exception or doing anything
            return;
        }

        try {
            /*
             * Open and check user rights on source folder
             */
            try {
                imapFolder = setAndOpenFolder(imapFolder, sourceFullName, move ? READ_WRITE : READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, sourceFullName);
                }
                throw handleMessagingException(sourceFullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (move && imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }

            {
                /*
                 * Open and check user rights on destination folder
                 */
                final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullName);
                {
                    final ListLsubEntry listEntry = ListLsubCache.getCachedLISTEntry(destFullName, accountId, destFolder, session, imapConfig.getIMAPProperties());
                    if (!"INBOX".equals(destFullName) && !listEntry.exists()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, destFullName);
                    }
                    if (!listEntry.canOpen()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, destFullName);
                    }
                }
                try {
                    /*
                     * Check if COPY/APPEND is allowed on destination folder
                     */
                    if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(
                        destFolder,
                        true,
                        session,
                        accountId))) {
                        throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, destFolder.getFullName());
                    }
                } catch (MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, destFolder.getFullName());
                }
            }

            if (imapFolder.getMessageCount() <= 0) {
                // Folder is empty
                return;
            }

            boolean supportsMove = move && imapConfig.asMap().containsKey("MOVE");

            AbstractIMAPCommand<long[]> command;
            if (supportsMove) {
                command = new MoveIMAPCommand(imapFolder, destFullName);
            } else {
                command = new CopyIMAPCommand(imapFolder, destFullName);
            }
            command.doCommand();

            if (move && !supportsMove) {
                new FlagsIMAPCommand(imapFolder, FLAGS_DELETED, true, true).doCommand();
                IMAPCommandsCollection.fastExpunge(imapFolder);
            }
        } catch (MessagingException e) {
            throw handleMessagingException(sourceFullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public long[] copyMessagesLong(String sourceFolder, String destFolder, long[] mailIds, boolean fast) throws OXException {
        return copyOrMoveMessages(sourceFolder, destFolder, mailIds, false, fast);
    }

    @Override
    public long[] moveMessagesLong(String sourceFolder, String destFolder, long[] mailIds, boolean fast) throws OXException {
        if (ROOT_FOLDER_ID.equals(destFolder)) {
            throw IMAPException.create(IMAPException.Code.NO_ROOT_MOVE, imapConfig, session);
        }
        return copyOrMoveMessages(sourceFolder, destFolder, mailIds, true, fast);
    }

    private long[] copyOrMoveMessages(String sourceFullName, String destFullName, long[] mailIds, boolean move, boolean fast) throws OXException {
        try {
            if (null == mailIds) {
                throw IMAPException.create(IMAPException.Code.MISSING_PARAMETER, imapConfig, session, "mailIDs");
            } else if ((sourceFullName == null) || (sourceFullName.length() == 0)) {
                throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "source");
            } else if ((destFullName == null) || (destFullName.length() == 0)) {
                throw IMAPException.create(IMAPException.Code.MISSING_SOURCE_TARGET_FOLDER_ON_MOVE, imapConfig, session, "target");
            } else if (sourceFullName.equals(destFullName) && move) {
                // Source equals destination, just return the message ids without throwing an exception or doing anything
                return mailIds;
            } else if (0 == mailIds.length) {
                // Nothing to move
                return new long[0];
            }
            /*
             * Open and check user rights on source folder
             */
            try {
                imapFolder = setAndOpenFolder(imapFolder, sourceFullName, move ? READ_WRITE : READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, sourceFullName);
                }
                throw handleMessagingException(sourceFullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (move && imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            {
                /*
                 * Open and check user rights on destination folder
                 */
                final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullName);
                {
                    final ListLsubEntry listEntry = ListLsubCache.getCachedLISTEntry(destFullName, accountId, destFolder, session, imapConfig.getIMAPProperties());
                    if (!"INBOX".equals(destFullName) && !listEntry.exists()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, destFullName);
                    }
                    if (!listEntry.canOpen()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, destFullName);
                    }
                }
                try {
                    /*
                     * Check if COPY/APPEND is allowed on destination folder
                     */
                    if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(
                        destFolder,
                        true,
                        session,
                        accountId))) {
                        throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, destFolder.getFullName());
                    }
                } catch (MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, destFolder.getFullName());
                }
            }
            /*
             * Set marker
             */
            final OperationKey opKey = new OperationKey(Type.MSG_COPY, accountId, sourceFullName, destFullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Copy operation
                 */
                final long[] result = new long[mailIds.length];
                final int blockSize = getIMAPProperties().getBlockSize();
                int offset = 0;
                final long[] remain;
                if (blockSize > 0 && mailIds.length > blockSize) {
                    /*
                     * Block-wise deletion
                     */
                    final long[] tmp = new long[blockSize];
                    for (int len = mailIds.length; len > blockSize; len -= blockSize) {
                        System.arraycopy(mailIds, offset, tmp, 0, tmp.length);
                        final long[] uids = copyOrMoveByUID(move, fast, destFullName, tmp);
                        /*
                         * Append UIDs
                         */
                        System.arraycopy(uids, 0, result, offset, uids.length);
                        offset += blockSize;
                    }
                    remain = new long[mailIds.length - offset];
                    System.arraycopy(mailIds, offset, remain, 0, remain.length);
                } else {
                    remain = mailIds;
                }
                final long[] uids = copyOrMoveByUID(move, fast, destFullName, remain);
                System.arraycopy(uids, 0, result, offset, uids.length);
                if (move) {
                    /*
                     * Force folder cache update through a close
                     */
                    imapFolder.close(false);
                    resetIMAPFolder();
                }
                final String draftFullname = imapAccess.getFolderStorage().getDraftsFolder();
                if (destFullName.equals(draftFullname)) {
                    /*
                     * A copy/move to drafts folder. Ensure to set \Draft flag.
                     */
                    final IMAPFolder destFolder = setAndOpenFolder(destFullName, READ_WRITE);
                    try {
                        if (destFolder.getMessageCount() > 0) {
                            final long start = System.currentTimeMillis();
                            new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, true, true).doCommand();
                            LOG.debug("A copy/move to default drafts folder => All messages' \\Draft flag in {} set in {}msec", destFullName, Long.valueOf(System.currentTimeMillis() - start));
                        }
                    } finally {
                        destFolder.close(false);
                    }
                } else if (sourceFullName.equals(draftFullname)) {
                    /*
                     * A copy/move from drafts folder. Ensure to unset \Draft flag.
                     */
                    final IMAPFolder destFolder = setAndOpenFolder(destFullName, READ_WRITE);
                    try {
                        final long start = System.currentTimeMillis();
                        new FlagsIMAPCommand(destFolder, FLAGS_DRAFT, false, true).doCommand();
                        LOG.debug("A copy/move from default drafts folder => All messages' \\Draft flag in {} unset in {}msec", destFullName, Long.valueOf(System.currentTimeMillis() - start));
                    } finally {
                        destFolder.close(false);
                    }
                }
                if (move && IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedSessionData(mailIds, accountId, session, sourceFullName);
                }
                return result;
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(sourceFullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private long[] copyOrMoveByUID(boolean move, boolean fast, String destFullName, long[] tmp) throws MessagingException, OXException {
        final boolean supportsMove = move && imapConfig.asMap().containsKey("MOVE");
        final AbstractIMAPCommand<long[]> command;
        if (supportsMove) {
            command = new MoveIMAPCommand(imapFolder, tmp, destFullName, false, fast);
        } else {
            command = new CopyIMAPCommand(imapFolder, tmp, destFullName, false, fast);
        }
        long[] uids = command.doCommand();
        if (!fast && ((uids == null) || noUIDsAssigned(uids, tmp.length))) {
            /*
             * Invalid UIDs
             */
            uids = getDestinationUIDs(tmp, destFullName);
        }
        if (supportsMove) {
            return uids;
        }
        if (move) {
            new FlagsIMAPCommand(imapFolder, tmp, FLAGS_DELETED, true, true, false).doCommand();
            try {
                IMAPCommandsCollection.uidExpungeWithFallback(imapFolder, tmp, imapConfig.getImapCapabilities().hasUIDPlus());
            } catch (FolderClosedException | StoreClosedException e) {
                /*
                 * Not possible to retry since connection is broken
                 */
                MailConfig mailConfig = imapAccess.getMailConfig();
                throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, mailConfig.getServer(), mailConfig.getLogin(), I(mailConfig.getMailProperties().getConnectTimeout()));
            } catch (MessagingException e) {
                if (e.getNextException() instanceof ProtocolException) {
                    final ProtocolException protocolException = (ProtocolException) e.getNextException();
                    final Response response = protocolException.getResponse();
                    if (response != null && response.isBYE()) {
                        /*
                         * The BYE response is always untagged, and indicates that the server is about to close the connection.
                         */
                        MailConfig mailConfig = imapAccess.getMailConfig();
                        throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, mailConfig.getServer(), mailConfig.getLogin(), I(mailConfig.getMailProperties().getConnectTimeout()));
                    }
                    final Throwable cause = protocolException.getCause();
                    if (cause instanceof StoreClosedException || cause instanceof FolderClosedException) {
                        /*
                         * Connection is down. No retry.
                         */
                        MailConfig mailConfig = imapAccess.getMailConfig();
                        throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, mailConfig.getServer(), mailConfig.getLogin(), I(mailConfig.getMailProperties().getConnectTimeout()));
                    }
                }
                throw IMAPException.create(IMAPException.Code.UID_EXPUNGE_FAILED, imapConfig, session, e, Arrays.toString(tmp), imapFolder.getFullName(), e.getMessage());
            }
        }
        return uids;
    }

    @Override
    public boolean isMimeSupported() {
        return true;
    }

    @Override
    public String[] appendMimeMessages(String destFullName, Message[] msgs) throws OXException {
        if (null == msgs) {
            return Strings.getEmptyStrings();
        }
        final int length = msgs.length;
        if (length == 0) {
            return Strings.getEmptyStrings();
        }
        try {
            /*
             * Open and check user rights on source folder
             */
            try {
                imapFolder = setAndOpenFolder(imapFolder, destFullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, destFullName);
                }
                throw handleMessagingException(destFullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            final OperationKey opKey = new OperationKey(Type.MSG_APPEND, accountId, destFullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Check if destination folder supports user flags
                 */
                if (!imapFolder.getPermanentFlags().contains(Flags.Flag.USER)) {
                    /*
                     * Remove all user flags from messages before appending to folder
                     */
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = removeUserFlagsFromMessage(msgs[i]);
                    }
                }
                /*
                 * Mark first message for later lookup
                 */
                final List<Message> filteredMsgs = filterNullElements(msgs);
                if (filteredMsgs.isEmpty()) {
                    return Strings.getEmptyStrings();
                }
                final String hash = randomUUID();
                /*
                 * Try to set marker header
                 */
                try {
                    Message message = filteredMsgs.get(0);
                    /*
                     * Check for empty content
                     */
                    AppendEmptyMessageTracer.checkForEmptyMessage(message, destFullName, imapConfig);
                    /*
                     * Set marker
                     */
                    message.setHeader(MessageHeaders.HDR_X_OX_MARKER, fold(13, hash));
                } catch (Exception e) {
                    // Is read-only -- create a copy from first message
                    LOG.trace("", e);
                    final MimeMessage newMessage;
                    final Message removed = filteredMsgs.remove(0);
                    if (removed instanceof ReadableMime) {
                        newMessage = new MimeMessage(MimeDefaultSession.getDefaultSession(), ((ReadableMime) removed).getMimeStream());
                        newMessage.setFlags(removed.getFlags(), true);
                    } else {
                        newMessage = new MimeMessage(MimeDefaultSession.getDefaultSession(), MimeMessageUtility.getStreamFromPart(removed));
                        newMessage.setFlags(removed.getFlags(), true);
                    }
                    newMessage.setHeader(MessageHeaders.HDR_X_OX_MARKER, fold(13, hash));
                    filteredMsgs.add(0, newMessage);
                }
                /*
                 * ... and append them to folder
                 */
                String[] newUids = null;
                final boolean hasUIDPlus = imapConfig.getImapCapabilities().hasUIDPlus();
                try {
                    if (hasUIDPlus) {
                        // Perform append expecting APPENUID response code
                        newUids = longs2uids(checkAndConvertAppendUID(imapFolder.appendUIDMessages(filteredMsgs.toArray(new Message[filteredMsgs.size()]))));
                    } else {
                        // Perform simple append
                        imapFolder.appendMessages(filteredMsgs.toArray(new Message[filteredMsgs.size()]));
                    }
                } catch (MessagingException e) {
                    if (MimeMailException.isOverQuotaException(e)) {
                        // Special handling for over-quota error
                        String sentFullname = imapAccess.getFolderStorage().getSentFolder();
                        if (null != sentFullname && sentFullname.equals(destFullName)) {
                            throw MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.create(e);
                        }
                        throw MimeMailException.handleMessagingException(e, imapConfig, session);
                    }

                    OXException oxe = handleMessagingException(destFullName, e);
                    if (MimeMailExceptionCode.PROCESSING_ERROR.equals(oxe)) {
                        throw IMAPException.create(IMAPException.Code.INVALID_MESSAGE, imapConfig, session, e);
                    }
                    throw oxe;
                }
                if (null != newUids && newUids.length > 0) {
                    /*
                     * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
                     */
                    notifyIMAPFolderModification(destFullName);
                    if (newUids.length >= length) {
                        return newUids;
                    }
                    final String[] longs = new String[length];
                    for (int i = 0, k = 0; i < length; i++) {
                        if (null != msgs[i]) {
                            longs[i] = newUids[k++];
                        }
                    }
                    return longs;
                }
                /*-
                 * OK, go the long way:
                 * 1. Find the marker in folder's messages
                 * 2. Get the UIDs from found message's position
                 */
                if (hasUIDPlus) {
                    /*
                     * Missing UID information in APPENDUID response
                     */
                    LOG.warn("Missing UID information in APPENDUID response");
                }
                newUids = new String[msgs.length];
                {
                    final long[] uids = IMAPCommandsCollection.findMarker(hash, newUids.length, imapFolder);
                    final int uLen = uids.length;
                    if (uLen == 0) {
                        Arrays.fill(newUids, null);
                    } else {
                        for (int i = 0; i < uLen; i++) {
                            newUids[i] = Long.toString(uids[i]);
                        }
                    }
                }
                /*
                 * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
                 */
                notifyIMAPFolderModification(destFullName);
                if (newUids.length >= length) {
                    return newUids;
                }
                final String[] longs = new String[length];
                for (int i = 0, k = 0; i < length; i++) {
                    if (null != msgs[i]) {
                        longs[i] = newUids[k++];
                    }
                }
                return longs;
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(destFullName, e);
        } catch (MessageRemovedIOException e) {
            throw MailExceptionCode.MAIL_NOT_FOUND_SIMPLE.create(e);
        } catch (IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public long[] appendMessagesLong(String destFullName, MailMessage[] mailMessages) throws OXException {
        if (null == mailMessages) {
            return new long[0];
        }
        final int length = mailMessages.length;
        if (length == 0) {
            return new long[0];
        }
        Message[] msgs = null;
        try {
            /*
             * Open and check user rights on source folder
             */
            try {
                imapFolder = setAndOpenFolder(imapFolder, destFullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, destFullName);
                }
                throw handleMessagingException(destFullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canInsert(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_INSERT_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            final OperationKey opKey = new OperationKey(Type.MSG_APPEND, accountId, destFullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Drop special "x-original-headers" header
                 */
                for (MailMessage mail : filterNullElements(mailMessages)) {
                    mail.removeHeader("x-original-headers");
                }
                /*
                 * Convert messages to JavaMail message objects
                 */
                msgs = new Message[length];
                {
                    MailMessage m = mailMessages[0];
                    if (null != m) {
                        msgs[0] = asMessage(m, MimeMessageConverter.BEHAVIOR_CLONE);
                    }
                }
                for (int i = 1; i < length; i++) {
                    MailMessage m = mailMessages[i];
                    if (null != m) {
                        msgs[i] = asMessage(m, MimeMessageConverter.BEHAVIOR_CLONE | MimeMessageConverter.BEHAVIOR_STREAM2FILE);
                    }
                }
                /*
                 * Check if destination folder supports user flags
                 */
                if (!imapFolder.getPermanentFlags().contains(Flags.Flag.USER)) {
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = removeUserFlagsFromMessage(msgs[i]);
                    }
                }
                /*
                 * Mark first message for later lookup
                 */
                final List<Message> filteredMsgs = filterNullElements(msgs);
                if (filteredMsgs.isEmpty()) {
                    return new long[0];
                }
                final String hash = randomUUID();
                try {
                    Message message = filteredMsgs.get(0);
                    /*
                     * Check for empty content
                     */
                    AppendEmptyMessageTracer.checkForEmptyMessage(message, destFullName, imapConfig);
                    /*
                     * Set marker
                     */
                    message.setHeader(MessageHeaders.HDR_X_OX_MARKER, fold(13, hash));
                } catch (Exception e) {
                    // Is read-only -- create a copy from first message
                    LOG.trace("", e);
                    final MimeMessage newMessage;
                    final Message removed = filteredMsgs.remove(0);
                    if (removed instanceof ReadableMime) {
                        newMessage = MimeMessageUtility.newMimeMessage(((ReadableMime) removed).getMimeStream(), removed.getReceivedDate());
                        newMessage.setFlags(removed.getFlags(), true);
                    } else {
                        newMessage = MimeMessageUtility.cloneMessage(removed, removed.getReceivedDate());
                        newMessage.setFlags(removed.getFlags(), true);
                    }
                    newMessage.setHeader(MessageHeaders.HDR_X_OX_MARKER, fold(13, hash));
                    filteredMsgs.add(0, newMessage);
                }
                /*
                 * ... and append them to folder
                 */
                long[] retval = null;
                final boolean hasUIDPlus = imapConfig.getImapCapabilities().hasUIDPlus();
                try {
                    if (hasUIDPlus) {
                        // Perform append expecting APPENDUID response code
                        retval = checkAndConvertAppendUID(imapFolder.appendUIDMessages(filteredMsgs.toArray(new Message[filteredMsgs.size()])));
                    } else {
                        // Perform simple append
                        imapFolder.appendMessages(filteredMsgs.toArray(new Message[filteredMsgs.size()]));
                    }
                } catch (MessagingException e) {
                    if (MimeMailException.isOverQuotaException(e)) {
                        // Special handling for over-quota error
                        String sentFullname = imapAccess.getFolderStorage().getSentFolder();
                        if (null != sentFullname && sentFullname.equals(destFullName)) {
                            throw MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.create(e);
                        }
                        throw MimeMailException.handleMessagingException(e, imapConfig, session);
                    }

                    OXException oxe = handleMessagingException(destFullName, e);
                    if (MimeMailExceptionCode.PROCESSING_ERROR.equals(oxe)) {
                        throw IMAPException.create(IMAPException.Code.INVALID_MESSAGE, imapConfig, session, e);
                    }
                    throw oxe;
                }
                if (null != retval && retval.length > 0) {
                    /*
                     * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
                     */
                    notifyIMAPFolderModification(destFullName);
                    if (retval.length >= length) {
                        return retval;
                    }
                    final long[] longs = new long[length];
                    Arrays.fill(longs, -1L);
                    for (int i = 0, k = 0; i < length; i++) {
                        final MailMessage m = mailMessages[i];
                        if (null != m) {
                            longs[i] = retval[k++];
                        }
                    }
                    return longs;
                }
                /*-
                 * OK, go the long way:
                 * 1. Find the marker in folder's messages
                 * 2. Get the UIDs from found message's position
                 */
                if (hasUIDPlus) {
                    /*
                     * Missing UID information in APPENDUID response
                     */
                    LOG.warn("Missing UID information in APPENDUID response for folder {} from IMAP server {} using login {}", imapFolder.getFullName(), imapConfig.getServer(), imapConfig.getLogin());
                }
                retval = new long[msgs.length];
                final long[] uids = IMAPCommandsCollection.findMarker(hash, retval.length, imapFolder);
                if (uids.length == 0) {
                    Arrays.fill(retval, -1L);
                } else {
                    System.arraycopy(uids, 0, retval, 0, uids.length);
                }
                /*
                 * Close affected IMAP folder to ensure consistency regarding IMAFolder's internal cache.
                 */
                notifyIMAPFolderModification(destFullName);
                if (retval.length >= length) {
                    return retval;
                }
                final long[] longs = new long[length];
                Arrays.fill(longs, -1L);
                for (int i = 0, k = 0; i < length; i++) {
                    final MailMessage m = mailMessages[i];
                    if (null != m) {
                        longs[i] = retval[k++];
                    }
                }
                return longs;
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            if (LOG.isDebugEnabled()) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException) {
                    final StringBuilder sb = new StringBuilder(8192);
                    sb.append("\r\nAPPEND command failed. Printing messages' headers for debugging purpose:\r\n");
                    for (int i = 0; i < mailMessages.length; i++) {
                        final MailMessage mailMessage = mailMessages[i];
                        if (null != mailMessage) {
                            sb.append("----------------------------------------------------\r\n\r\n");
                            sb.append(i + 1).append(". message's header:\r\n");
                            sb.append(mailMessage.getHeaders().toString());
                            sb.append("----------------------------------------------------\r\n\r\n");
                        }
                    }
                    LOG.debug(sb.toString());
                }
            }
            throw handleMessagingException(destFullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (null != msgs) {
                for (Message message : msgs) {
                    if (message instanceof MimeCleanUp) {
                        ((MimeCleanUp) message).cleanUp();
                    }
                }
            }
        }
    }

    private static Message asMessage(MailMessage m, int behavior) throws OXException {
        String messageId = m.getHeader("Message-ID", null);
        Message message;
        if (m instanceof MimeRawSource) {
            Part part = ((MimeRawSource) m).getPart();
            if (part instanceof Message) {
                message = (Message) part;
                messageId = null;
            } else {
                message = MimeMessageConverter.convertMailMessage(m, behavior);
            }
        } else {
            message = MimeMessageConverter.convertMailMessage(m, behavior);
        }
        if (null != messageId) {
            try {
                message.setHeader("Message-ID", messageId);
            } catch (MessagingException e) {
                LOG.warn("Failed to keep \"Message-ID\" header.", e);
            }
        }
        return message;
    }


    @Override
    public void updateMessageFlagsLong(String fullName, long[] msgUIDs, int flagsArg, boolean set) throws OXException {
        updateMessageFlagsLong(fullName, msgUIDs, flagsArg, ArrayUtils.EMPTY_STRING_ARRAY, set);
    }

    @Override
    public void updateMessageFlagsLong(String fullName, long[] msgUIDs, int flagsArg, String[] userFlags, boolean set) throws OXException {
        if (null == msgUIDs || 0 == msgUIDs.length) {
            // Nothing to do
            return;
        }
        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            // final OperationKey opKey = new OperationKey(Type.MSG_FLAGS_UPDATE, accountId, new Object[] { fullName });
            // final boolean marked = setMarker(opKey);
            try {
                /*
                 * Remove non user-alterable system flags
                 */
                int flags = flagsArg;
                flags &= ~MailMessage.FLAG_RECENT;
                flags &= ~MailMessage.FLAG_USER;
                /*
                 * Set new flags...
                 */
                final Rights myRights = imapConfig.isSupportsACLs() ? RightsCache.getCachedRights(imapFolder, true, session, accountId) : null;
                final Flags affectedFlags = new Flags();
                boolean applyFlags = false;
                if (((flags & MailMessage.FLAG_ANSWERED) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_UPDATE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(Flags.Flag.ANSWERED);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_DELETED) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(DELETED);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_DRAFT) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(DRAFT);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_FLAGGED) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(Flags.Flag.FLAGGED);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_SEEN) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canKeepSeen(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_KEEP_SEEN_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(Flags.Flag.SEEN);
                    applyFlags = true;
                }
                /*
                 * Check for forwarded flag (supported through user flags)
                 */
                Boolean supportsUserFlags = null;
                if (((flags & MailMessage.FLAG_FORWARDED) > 0)) {
                    supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                    if (supportsUserFlags.booleanValue()) {
                        if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                            throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                        }
                        affectedFlags.add(MailMessage.USER_FORWARDED);
                        applyFlags = true;
                    } else {
                        LOG.debug(
                            "IMAP server {} does not support user flags. Skipping forwarded flag.",
                            imapConfig.getImapServerSocketAddress());
                    }
                }
                /*
                 * Check for read acknowledgment flag (supported through user flags)
                 */
                if (((flags & MailMessage.FLAG_READ_ACK) > 0)) {
                    if (null == supportsUserFlags) {
                        supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                    }
                    if (supportsUserFlags.booleanValue()) {
                        if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                            throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                        }
                        affectedFlags.add(MailMessage.USER_READ_ACK);
                        applyFlags = true;
                    } else {
                        LOG.debug("IMAP server {} does not support user flags. Skipping read-ack flag.", imapConfig.getImapServerSocketAddress());
                    }
                }

                for (String userFlag : userFlags) {
                    if (Strings.isNotEmpty(userFlag)) {
                        affectedFlags.add(userFlag.trim());
                        applyFlags = true;
                    }
                }

                if (applyFlags) {
                    final long start = System.currentTimeMillis();
                    new FlagsIMAPCommand(imapFolder, msgUIDs, affectedFlags, set, true, false).doCommand();
                    LOG.debug("Flags applied to {} messages in {}msec", Integer.valueOf(msgUIDs.length), Long.valueOf(System.currentTimeMillis() - start));
                }
                /*
                 * Check for spam action
                 */
                if (getUserSettingMail().isSpamEnabled() && ((flags & MailMessage.FLAG_SPAM) > 0)) {
                    handleSpamByUID(msgUIDs, set, true, fullName, READ_WRITE);
                } else {
                    /*
                     * Force JavaMail's cache update through folder closure
                     */
                    imapFolder.close(false);
                    resetIMAPFolder();
                }
            } finally {
                // if (marked) {
                //    unsetMarker(opKey);
                // }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageFlags(String fullName, int flagsArg, boolean set) throws OXException {
        updateMessageFlags(fullName, flagsArg, ArrayUtils.EMPTY_STRING_ARRAY, set);
    }

    @Override
    public void updateMessageFlags(String fullName, int flagsArg, String[] userFlags, boolean set) throws OXException {
        if (null == fullName) {
            // Nothing to do
            return;
        }
        try {
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            final OperationKey opKey = new OperationKey(Type.MSG_FLAGS_UPDATE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Remove non user-alterable system flags
                 */
                int flags = flagsArg;
                flags &= ~MailMessage.FLAG_RECENT;
                flags &= ~MailMessage.FLAG_USER;
                /*
                 * Set new flags...
                 */
                final Rights myRights = imapConfig.isSupportsACLs() ? RightsCache.getCachedRights(imapFolder, true, session, accountId) : null;
                final Flags affectedFlags = new Flags();
                boolean applyFlags = false;
                if (((flags & MailMessage.FLAG_ANSWERED) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_UPDATE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(Flags.Flag.ANSWERED);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_DELETED) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canDeleteMessages(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(DELETED);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_DRAFT) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(DRAFT);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_FLAGGED) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(Flags.Flag.FLAGGED);
                    applyFlags = true;
                }
                if (((flags & MailMessage.FLAG_SEEN) > 0)) {
                    if (imapConfig.isSupportsACLs() && !aclExtension.canKeepSeen(myRights)) {
                        throw IMAPException.create(IMAPException.Code.NO_KEEP_SEEN_ACCESS, imapConfig, session, imapFolder.getFullName());
                    }
                    affectedFlags.add(Flags.Flag.SEEN);
                    applyFlags = true;
                }
                /*
                 * Check for forwarded flag (supported through user flags)
                 */
                Boolean supportsUserFlags = null;
                if (((flags & MailMessage.FLAG_FORWARDED) > 0)) {
                    supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                    if (supportsUserFlags.booleanValue()) {
                        if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                            throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                        }
                        affectedFlags.add(MailMessage.USER_FORWARDED);
                        applyFlags = true;
                    } else {
                        LOG.debug("IMAP server {} does not support user flags. Skipping forwarded flag.", imapConfig.getImapServerSocketAddress());
                    }
                }
                /*
                 * Check for read acknowledgment flag (supported through user flags)
                 */
                if (((flags & MailMessage.FLAG_READ_ACK) > 0)) {
                    if (null == supportsUserFlags) {
                        supportsUserFlags = Boolean.valueOf(UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId));
                    }
                    if (supportsUserFlags.booleanValue()) {
                        if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(myRights)) {
                            throw IMAPException.create(IMAPException.Code.NO_WRITE_ACCESS, imapConfig, session, imapFolder.getFullName());
                        }
                        affectedFlags.add(MailMessage.USER_READ_ACK);
                        applyFlags = true;
                    } else {
                        LOG.debug("IMAP server {} does not support user flags. Skipping read-ack flag.", imapConfig.getImapServerSocketAddress());
                    }
                }

                for (String userFlag : userFlags) {
                    if (Strings.isNotEmpty(userFlag)) {
                        affectedFlags.add(userFlag = userFlag.trim());
                        applyFlags = true;
                    }
                }

                if (applyFlags) {
                    final long start = System.currentTimeMillis();
                    new FlagsIMAPCommand(imapFolder, affectedFlags, set, true).doCommand();
                    LOG.debug("Flags applied to all messages in {}msec", Long.valueOf(System.currentTimeMillis() - start));
                }
                /*
                 * Check for spam action
                 */
                if (getUserSettingMail().isSpamEnabled() && ((flags & MailMessage.FLAG_SPAM) > 0)) {
                    final long[] uids = IMAPCommandsCollection.getUIDs(imapFolder);
                    handleSpamByUID(uids, set, true, fullName, READ_WRITE);
                } else {
                    /*
                     * Force JavaMail's cache update through folder closure
                     */
                    imapFolder.close(false);
                    resetIMAPFolder();
                }
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageUserFlagsLong(String fullName, long[] mailIds, String[] flags, boolean set) throws OXException {
        if (null == mailIds || 0 == mailIds.length) {
            // Nothing to do
            return;
        }
        if (null == flags || 0 == flags.length) {
            // Nothing to do
            return;
        }
        Set<String> flags2Set = new LinkedHashSet<String>(flags.length);
        for (String flag : flags) {
            if (Strings.isNotEmpty(flag)) {
                flags2Set.add(flag.trim());
            }
        }
        if (flags2Set.isEmpty()) {
            // Nothing to do
            return;
        }
        try {
            if (!getIMAPProperties().isUserFlagsEnabled()) {
                /*
                 * User flags are disabled
                 */
                LOG.debug("User flags are disabled or not supported. Update of color flag ignored.");
                return;
            }
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_UPDATE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            if (!UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId)) {
                LOG.error("Folder \"{}\" does not support user-defined flags. Update of color flag ignored.", imapFolder.getFullName());
                return;
            }
            final OperationKey opKey = new OperationKey(Type.MSG_USER_FLAGS_UPDATE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Remove all old color label flag(s) and set new color label flag
                 */
                IMAPCommandsCollection.setUserFlags(imapFolder, mailIds, flags2Set.toArray(new String[flags2Set.size()]), set);

                /*
                 * Force JavaMail's cache update through folder closure
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageColorLabelLong(String fullName, long[] msgUIDs, int colorLabel) throws OXException {
        if (null == msgUIDs || 0 == msgUIDs.length) {
            // Nothing to do
            return;
        }
        try {
            if (!getIMAPProperties().isUserFlagsEnabled()) {
                /*
                 * User flags are disabled
                 */
                LOG.debug("User flags are disabled or not supported. Update of color flag ignored.");
                return;
            }
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_UPDATE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            if (!UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId)) {
                LOG.error("Folder \"{}\" does not support user-defined flags. Update of color flag ignored.", imapFolder.getFullName());
                return;
            }
            final OperationKey opKey = new OperationKey(Type.MSG_LABEL_UPDATE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Remove all old color label flag(s) and set new color label flag
                 */
                IMAPCommandsCollection.clearAndSetColorLabelSafely(imapFolder, msgUIDs, MailMessage.getColorLabelStringValue(colorLabel));

                /*
                 * Force JavaMail's cache update through folder closure
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public void updateMessageColorLabel(String fullName, int colorLabel) throws OXException {
        if (null == fullName) {
            // Nothing to do
            return;
        }
        try {
            if (!getIMAPProperties().isUserFlagsEnabled()) {
                /*
                 * User flags are disabled
                 */
                LOG.debug("User flags are disabled or not supported. Update of color flag ignored.");
                return;
            }
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_WRITE);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                }
                throw handleMessagingException(fullName, e);
            }
            try {
                if (!holdsMessages()) {
                    throw IMAPException.create(
                        IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES,
                        imapConfig,
                        session,
                        imapFolder.getFullName());
                }
                if (imapConfig.isSupportsACLs() && !aclExtension.canWrite(RightsCache.getCachedRights(imapFolder, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_UPDATE_ACCESS, imapConfig, session, imapFolder.getFullName());
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, imapFolder.getFullName());
            }
            if (!UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId)) {
                LOG.error("Folder \"{}\" does not support user-defined flags. Update of color flag ignored.", imapFolder.getFullName());
                return;
            }
            final OperationKey opKey = new OperationKey(Type.MSG_LABEL_UPDATE, accountId, fullName);
            final boolean marked = setMarker(opKey);
            try {
                /*
                 * Remove all old color label flag(s) and set new color label flag
                 */

                IMAPCommandsCollection.clearAndSetColorLabelSafely(imapFolder, null, MailMessage.getColorLabelStringValue(colorLabel));
                /*
                 * Force JavaMail's cache update through folder closure
                 */
                imapFolder.close(false);
                resetIMAPFolder();
            } finally {
                if (marked) {
                    unsetMarker(opKey);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailMessage saveDraft(String draftFullName, ComposedMailMessage composedMail) throws OXException {
        try {
            /*
             * Message content available?
             */
            MimeMessage mimeMessage = null;
            if (composedMail instanceof ContentAware) {
                try {
                    final Object content = composedMail.getContent();
                    if (content instanceof MimeMessage) {
                        mimeMessage = (MimeMessage) content;
                        mimeMessage.removeHeader("x-original-headers");

                        /*
                         * Set common headers
                         */
                        MimeMessageFiller filler = new MimeMessageFiller(session, ctx);
                        filler.setAccountId(accountId);
                        filler.setCommonHeaders(mimeMessage);
                        mimeMessage.setFlag(DRAFT, true);
                    }
                } catch (@SuppressWarnings("unused") Exception e) {
                    mimeMessage = null;
                }
            }

            long uid;
            /*
             * Fill message
             */
            MailMessage appendedMail;
            if (mimeMessage == null) {
                mimeMessage = new MimeMessage(imapAccess.getMailSession());
                try {
                    UserSettingMail customSettings = composedMail.getMailSettings();
                    MimeMessageFiller filler = null == customSettings ? new MimeMessageFiller(session, ctx) : new MimeMessageFiller(session, ctx, customSettings);
                    filler.setAccountId(accountId);
                    composedMail.setFiller(filler);
                    /*
                     * Set headers
                     */
                    filler.setMessageHeaders(composedMail, mimeMessage);
                    /*
                     * Set common headers
                     */
                    filler.setCommonHeaders(mimeMessage);
                    /*
                     * Fill body
                     */
                    filler.fillMailBody(composedMail, mimeMessage, ComposeType.NEW);
                    mimeMessage.setFlag(DRAFT, true);
                    mimeMessage.saveChanges();
                    // Remove generated Message-Id for template message
                    mimeMessage.removeHeader(MessageHeaders.HDR_MESSAGE_ID);
                    /*
                     * Append message to draft folder
                     */
                    appendedMail = MimeMessageConverter.convertMessage(mimeMessage, false);
                    uid = appendMessagesLong(draftFullName, new MailMessage[] { appendedMail })[0];
                } finally {
                    composedMail.cleanUp();
                }
            } else {
                /*
                 * Append message to draft folder
                 */
                appendedMail = MimeMessageConverter.convertMessage(mimeMessage, false);
                uid = appendMessagesLong(draftFullName, new MailMessage[] { appendedMail })[0];
            }
            /*
             * Check for draft-edit operation: Delete old version
             */
            final MailPath msgref = composedMail.getMsgref();
            if (msgref != null && draftFullName.equals(msgref.getFolder())) {
                final ComposeType sendType = composedMail.getSendType();
                if (null == sendType || ComposeType.DRAFT_EDIT == sendType) {
                    if (accountId != msgref.getAccountId()) {
                        LOG.warn("Differing account ID in msgref attribute.\nMessage storage account ID: {}.\nmsgref account ID: {}", Integer.valueOf(accountId), Integer.valueOf(msgref.getAccountId()), new Throwable());
                    }
                    deleteMessagesLong(msgref.getFolder(), new long[] { parseUnsignedLong(msgref.getMailID()) }, true);
                    composedMail.setMsgref(null);
                }
            }
            /*
             * Force folder update
             */
            notifyIMAPFolderModification(draftFullName);
            /*
             * Return draft mail
             */
            appendedMail.setMailId(Long.toString(uid));
            appendedMail.setFolder(draftFullName);
            appendedMail.setAccountId(accountId);
            return appendedMail;
        } catch (OXException ex) {
            if (MailExceptionCode.COPY_TO_SENT_FOLDER_FAILED_QUOTA.equals(ex) || MimeMailExceptionCode.QUOTA_EXCEEDED.equals(ex)) {
                throw MailExceptionCode.UNABLE_TO_SAVE_DRAFT_QUOTA.create(ex);
            }
            throw ex;
        } catch (MessagingException e) {
            throw handleMessagingException(draftFullName, e);
        } catch (IOException e) {
            throw IMAPException.create(IMAPException.Code.IO_ERROR, imapConfig, session, e, e.getMessage());
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailMessage[] getNewAndModifiedMessages(String fullName, MailField[] fields) throws OXException {
        return EMPTY_RETVAL;
        // return getChangedMessages(folder, fields, 0);
    }

    @Override
    public MailMessage[] getDeletedMessages(String fullName, MailField[] fields) throws OXException {
        return EMPTY_RETVAL;
        // return getChangedMessages(folder, fields, 1);
    }

    /*-
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * +++++++++++++++++ Helper methods +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     */

    private void unsetMarker(OperationKey key) {
        OperationKey.unsetMarker(key, session);
    }

    private boolean setMarker(OperationKey key) throws OXException {
        final int result = OperationKey.setMarker(key, session);
        if (result < 0) {
            // In use...
            throw MimeMailExceptionCode.IN_USE_ERROR_EXT.create(
                imapConfig.getServer(),
                imapConfig.getLogin(),
                Integer.valueOf(session.getUserId()),
                Integer.valueOf(session.getContextId()),
                MimeMailException.appendInfo("Mailbox is currently in use.", imapFolder));
        }
        return result > 0;
    }

    private static boolean noUIDsAssigned(long[] arr, int expectedLen) {
        final long[] tmp = new long[expectedLen];
        Arrays.fill(tmp, -1L);
        return Arrays.equals(arr, tmp);
    }

    /**
     * Determines the corresponding UIDs in destination folder
     *
     * @param msgUIDs The UIDs in source folder
     * @param destFullName The destination folder's full name
     * @return The corresponding UIDs in destination folder
     * @throws MessagingException
     * @throws OXException
     */
    private long[] getDestinationUIDs(long[] msgUIDs, String destFullName) throws MessagingException, OXException {
        /*
         * No COPYUID present in response code. Since UIDs are assigned in strictly ascending order in the mailbox (refer to IMAPv4 rfc3501,
         * section 2.3.1.1), we can discover corresponding UIDs by selecting the destination mailbox and detecting the location of messages
         * placed in the destination mailbox by using FETCH and/or SEARCH commands (e.g., for Message-ID or some unique marker placed in the
         * message in an APPEND).
         */
        final long[] retval = new long[msgUIDs.length];
        Arrays.fill(retval, -1L);
        if (!IMAPCommandsCollection.canBeOpened(imapFolder, destFullName, READ_ONLY)) {
            // No look-up possible
            return retval;
        }
        final String messageId;
        {
            int minIndex = 0;
            long minVal = msgUIDs[0];
            for (int i = 1; i < msgUIDs.length; i++) {
                if (msgUIDs[i] < minVal) {
                    minIndex = i;
                    minVal = msgUIDs[i];
                }
            }
            final IMAPMessage imapMessage = (IMAPMessage) (imapFolder.getMessageByUID(msgUIDs[minIndex]));
            if (imapMessage == null) {
                /*
                 * No message found whose UID matches msgUIDs[minIndex]
                 */
                messageId = null;
            } else {
                messageId = imapMessage.getMessageID();
            }
        }
        if (messageId != null) {
            final IMAPFolder destFolder = (IMAPFolder) imapStore.getFolder(destFullName);
            destFolder.open(READ_ONLY);
            try {
                /*
                 * Find this message ID in destination folder
                 */
                long startUID = IMAPCommandsCollection.messageId2UID(destFolder, messageId)[0];
                if (startUID != -1) {
                    for (int i = 0; i < msgUIDs.length; i++) {
                        retval[i] = startUID++;
                    }
                }
            } finally {
                closeSafe(destFolder);
            }
        }
        return retval;
    }

    private void handleSpamByUID(long[] msgUIDs, boolean isSpam, boolean move, String fullName, int desiredMode) throws OXException {
        /*
         * Check for spam handling
         */
        if (getUserSettingMail().isSpamEnabled()) {
            final boolean locatedInSpamFolder = imapAccess.getFolderStorage().getSpamFolder().equals(imapFolder.getFullName());
            if (isSpam) {
                if (locatedInSpamFolder) {
                    /*
                     * A message that already has been detected as spam should again be learned as spam: Abort.
                     */
                    return;
                }
                /*
                 * Handle spam
                 */
                {
                    final SpamHandler spamHandler = SpamHandlerRegistry.getSpamHandlerBySession(session, accountId, IMAPProvider.getInstance());
                    spamHandler.handleSpam(accountId, imapFolder.getFullName(), longs2uids(msgUIDs), move, session);
                    /*
                     * Close and reopen to force internal message cache update
                     */
                    resetIMAPFolder();
                    try {
                        imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
                    } catch (MessagingException e) {
                        final Exception next = e.getNextException();
                        if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                            throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                        }
                        throw handleMessagingException(fullName, e);
                    }
                }
                return;
            }
            if (!locatedInSpamFolder) {
                /*
                 * A message that already has been detected as ham should again be learned as ham: Abort.
                 */
                return;
            }
            /*
             * Handle ham.
             */
            {
                final SpamHandler spamHandler = SpamHandlerRegistry.getSpamHandlerBySession(session, accountId, IMAPProvider.getInstance());
                spamHandler.handleHam(
                    accountId,
                    imapFolder.getFullName(),
                    longs2uids(msgUIDs),
                    move,
                    session);
                /*
                 * Close and reopen to force internal message cache update
                 */
                resetIMAPFolder();
                try {
                    imapFolder = setAndOpenFolder(imapFolder, fullName, desiredMode);
                } catch (MessagingException e) {
                    final Exception next = e.getNextException();
                    if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                        throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, fullName);
                    }
                    throw handleMessagingException(fullName, e);
                }
            }
        }
    }

    /**
     * Checks and converts specified APPENDUID response.
     *
     * @param appendUIDs The APPENDUID response
     * @return An array of long for each valid {@link AppendUID} element or a zero size array of long if an invalid {@link AppendUID}
     *         element was detected.
     */
    private static long[] checkAndConvertAppendUID(AppendUID[] appendUIDs) {
        if (appendUIDs == null || appendUIDs.length == 0) {
            return new long[0];
        }
        final long[] retval = new long[appendUIDs.length];
        for (int i = 0; i < appendUIDs.length; i++) {
            if (appendUIDs[i] == null) {
                /*
                 * A null element means the server didn't return UID information for the appended message.
                 */
                return new long[0];
            }
            retval[i] = appendUIDs[i].uid;
        }
        return retval;
    }

    /**
     * Removes all user flags from given message's flags
     *
     * @param message The message whose user flags shall be removed
     * @throws MessagingException If removing user flags fails
     * @throws OXException If removing user flags fails
     */
    private static Message removeUserFlagsFromMessage(Message message) throws MessagingException, OXException {
        if (null == message) {
            return null;
        }

        String[] userFlags = message.getFlags().getUserFlags();
        if (userFlags.length > 0) {
            /*
             * Remove gathered user flags from message's flags; flags which do not occur in flags object are unaffected.
             */
            return removeUserFlagsFromCopyOf(message);
        }
        return message;
    }

    private static Message removeUserFlagsFromCopyOf(Message message) throws OXException, MessagingException {
        // Copy/clone given message
        MimeMessage newMessage;
        if (message instanceof ReadableMime) {
            newMessage = MimeMessageUtility.newMimeMessage(((ReadableMime) message).getMimeStream(), message.getReceivedDate());
        } else {
            newMessage = MimeMessageUtility.cloneMessage(message, message.getReceivedDate());
        }

        // Drop user flags from it
        Flags flags = new Flags(message.getFlags()).removeAllUserFlags();
        newMessage.setFlags(flags, true);
        return newMessage;
    }

    /**
     * Generates a UUID using {@link UUID#randomUUID()}; e.g.:<br>
     * <i>a5aa65cb-6c7e-4089-9ce2-b107d21b9d15</i>
     *
     * @return A UUID string
     */
    private static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets account ID and name in given instance of {@link MailMessage}.
     *
     * @param mailMessages The {@link MailMessage} instance
     * @return The given instance of {@link MailMessage} with account ID and name set
     * @throws OXException If mail account cannot be obtained
     */
    MailMessage setAccountInfo(MailMessage mailMessage) throws OXException {
        if (null == mailMessage) {
            return null;
        }
        final MailAccount account = getMailAccount();
        mailMessage.setAccountId(account.getId());
        mailMessage.setAccountName(account.getName());
        return mailMessage;
    }

    /**
     * Sets account ID and name in given instances of {@link MailMessage}.
     *
     * @param mailMessages The {@link MailMessage} instances
     * @return The given instances of {@link MailMessage} each with account ID and name set
     * @throws OXException If mail account cannot be obtained
     */
    MailMessage[] setAccountInfo(MailMessage[] mailMessages) throws OXException {
        return setAccountInfo(mailMessages, -1);
    }

    /**
     * Sets account ID and name in given instances of {@link MailMessage}.
     *
     * @param mailMessages The {@link MailMessage} instances
     * @param numberUnreadMessage The number of unread messages
     * @return The given instances of {@link MailMessage} each with account ID and name set
     * @throws OXException If mail account cannot be obtained
     */
    private MailMessage[] setAccountInfo(MailMessage[] mailMessages, int numberUnreadMessage) throws OXException {
        final MailAccount account = getMailAccount();
        final String name = account.getName();
        final int id = account.getId();
        for (int i = 0; i < mailMessages.length; i++) {
            final MailMessage mailMessage = mailMessages[i];
            if (null != mailMessage) {
                mailMessage.setAccountId(id);
                mailMessage.setAccountName(name);
                if (numberUnreadMessage > 0) {
                    mailMessage.setUnreadMessages(numberUnreadMessage);
                }
            }
        }
        return mailMessages;
    }

    /**
     * Sets account ID and name in given instances of {@link MailMessage}.
     *
     * @param mailMessages The {@link MailMessage} instances
     * @return The given instances of {@link MailMessage} each with account ID and name set
     * @throws OXException If mail account cannot be obtained
     */
    <C extends Collection<MailMessage>, W extends Collection<C>> W setAccountInfo2(W col) throws OXException {
        return setAccountInfo2(col, getMailAccount());
    }

    /**
     * Sets account ID and name in given instances of {@link MailMessage}.
     *
     * @param mailMessages The {@link MailMessage} instances
     * @return The given instances of {@link MailMessage} each with account ID and name set
     */
    static <C extends Collection<MailMessage>, W extends Collection<C>> W setAccountInfo2(W col, MailAccount account) {
        final String name = account.getName();
        final int id = account.getId();
        for (C mailMessages : col) {
            for (MailMessage mailMessage : mailMessages) {
                if (null != mailMessage) {
                    mailMessage.setAccountId(id);
                    mailMessage.setAccountName(name);
                }
            }
        }
        return col;
    }

    private MailMessage[] convert2Mails(Message[] msgs, MailField[] fields) throws OXException {
        return convert2Mails(msgs, fields, null, false);
    }

    /**
     * Converts given MIME messages to {@link MailMessage} instances.
     *
     * @param msgs The MIME messages to convert
     * @param fields The fields to consider
     * @param includeBody Whether body is included
     * @return The {@link MailMessage} instances
     * @throws OXException If conversion fails
     */
    MailMessage[] convert2Mails(Message[] msgs, MailField[] fields, boolean includeBody) throws OXException {
        return convert2Mails(msgs, fields, null, includeBody);
    }

    private MailMessage[] convert2Mails(Message[] msgs, MailField[] fields, String[] headerNames, boolean includeBody) throws OXException {
        return MimeMessageConverter.convertMessages(msgs, fields, headerNames, includeBody, imapConfig);
    }

    /**
     * Gets the separator character
     *
     * @param imapFolder The IMAP folder to use
     * @return The separator character
     * @throws OXException If separator character cannot be return
     * @throws MessagingException If a messaging error occurs
     */
    private char getSeparator(IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry("INBOX", imapFolder).getSeparator();
    }

    private ListLsubEntry getLISTEntry(String fullName, IMAPFolder imapFolder) throws OXException, MessagingException {
        return ListLsubCache.getCachedLISTEntry(fullName, accountId, imapFolder, session, imapConfig.getIMAPProperties());
    }

    private static boolean isSubfolderOf(String fullName, String possibleParent, char separator) {
        if (!fullName.startsWith(possibleParent)) {
            return false;
        }
        final int length = possibleParent.length();
        if (length >= fullName.length()) {
            return true;
        }
        return fullName.charAt(length) == separator;
    }

    /**
     * Closes given IMAP folder safely.
     *
     * @param imapFolder The IMAP folder to close
     */
    static void closeSafe(IMAPFolder imapFolder) {
        if (null != imapFolder) {
            try {
                imapFolder.close(false);
            } catch (Exception e) {
                // Ignore
                LOG.trace("", e);
            }
        }
    }

    private static Map<String, Object> mapFor(String... pairs) {
        if (null == pairs) {
            throw new IllegalArgumentException("Pairs must not be null");
        }
        int length = pairs.length;
        if (0 == length || (length % 2) != 0) {
            throw new IllegalArgumentException("Pairs must not be empty or of uneven length");
        }
        Map<String, Object> map = new HashMap<>(length >> 1);
        for (int i = 0; i < length; i+=2) {
            map.put(pairs[i], pairs[i+1]);
        }
        return map;
    }

    private static <E> List<E> filterNullElements(E[] elements) {
        return null == elements ? Collections.emptyList() : Arrays.stream(elements).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static long[] filterNegativeElements(long[] uids) {
        int i = 0;
        boolean fine = true;
        while (fine && i < uids.length) {
            if (uids[i] < 0) {
                fine = false;
            } else {
                i++;
            }
        }

        if (fine) {
            return uids;
        }

        TLongList valids = new TLongArrayList(uids.length);
        valids.add(uids, 0, i);
        for (int j = i + 1; j < uids.length; j++) {
            long uid = uids[j];
            if (uid >= 0) {
                valids.add(uid);
            }
        }
        return valids.toArray();
    }

    /**
     * Checks given fetch profile to only contain fetch items and no single headers<br>
     * In case {@link IMAPProperties#allowFetchSingleHeaders()} signals <code>true</code>.
     *
     * @param fetchProfile The fetch profile to check
     * @return The checked fetch profile
     */
    static FetchProfile checkFetchProfile(FetchProfile fetchProfile) {
        if (null == fetchProfile || IMAPProperties.getInstance().allowFetchSingleHeaders()) {
            return fetchProfile;
        }

        FetchProfile newFetchProfile = new FetchProfile();
        for (Item item : fetchProfile.getItems()) {
            newFetchProfile.add(item);
        }
        return newFetchProfile;
    }

    @Override
    public MailMessage[] getMessagesByMessageIDByFolder(String fullName, String... messageIDs) throws OXException {
        try {
            final int length = messageIDs.length;
            final MailMessage[] retval = new MailMessage[length];
            try {
                imapFolder = setAndOpenFolder(imapFolder, fullName, READ_ONLY);
            } catch (MessagingException e) {
                final Exception next = e.getNextException();
                if (next instanceof CommandFailedException && (Strings.toUpperCase(next.getMessage()).indexOf("[NOPERM]") >= 0)) {
                    throw IMAPException.create(IMAPException.Code.NO_FOLDER_OPEN, imapConfig, session, e, "INBOX");
                }
                throw handleMessagingException(fullName, e);
            }
            final long[] uids = IMAPCommandsCollection.messageId2UID(imapFolder, messageIDs);
            if (uids.length == length) {
                for (int i = 0; i < uids.length; i++) {
                    final long uid = uids[i];
                    if (uid != -1) {
                        retval[i] = new IDMailMessage(String.valueOf(uid), fullName);
                    }
                }
            }
            return retval;
        } catch (MessagingException e) {
            throw handleMessagingException(imapFolder.getFullName(), e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /**
     * Gets the IMAP message storage instance from given mail access.
     *
     * @param mailAccess The connected mail access
     * @return The IMAP message storage or <code>null</code>
     * @throws OXException If IMAP message storage could not be extracted
     */
    protected static IMAPMessageStorage getImapMessageStorageFrom(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
        IMailMessageStorage mstore = mailAccess.getMessageStorage();
        if (!(mstore instanceof IMAPMessageStorage)) {
            if (!(mstore instanceof IMailMessageStorageDelegator)) {
                return null;
            }
            mstore = ((IMailMessageStorageDelegator) mstore).getDelegateMessageStorage();
            if (!(mstore instanceof IMAPMessageStorage)) {
                return null;
            }
        }
        return (IMAPMessageStorage) mstore;
    }

    private static final class DeleteOptions {

        private static final DeleteOptions NEITHER_NOR = new DeleteOptions(false, false);

        static DeleteOptions optionsForNeiherNor() {
            return NEITHER_NOR;
        }

        static DeleteOptions optionsForReturnTargetPaths() {
            return new DeleteOptions(true, false);
        }

        static DeleteOptions optionsForReturnRemovedOnes() {
            return new DeleteOptions(false, true);
        }

        final boolean returnTargetPaths;
        final boolean returnRemovedOnes;

        private DeleteOptions(boolean returnTargetPaths, boolean returnRemovedOnes) {
            super();
            this.returnTargetPaths = returnTargetPaths;
            this.returnRemovedOnes = returnRemovedOnes;
        }


    }
}
