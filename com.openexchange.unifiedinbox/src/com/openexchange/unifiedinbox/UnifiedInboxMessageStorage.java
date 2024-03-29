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

package com.openexchange.unifiedinbox;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.mail.dataobjects.MailFolder.ROOT_FOLDER_ID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.mail.MessagingException;
import com.openexchange.context.ContextService;
import com.openexchange.continuation.ContinuationExceptionCodes;
import com.openexchange.continuation.ContinuationRegistryService;
import com.openexchange.continuation.ContinuationResponse;
import com.openexchange.continuation.ExecutorContinuation;
import com.openexchange.continuation.ExecutorContinuation.ContinuationResponseGenerator;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Functions;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailFields;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.IMailMessageStorageExt;
import com.openexchange.mail.api.ISimplifiedThreadStructure;
import com.openexchange.mail.api.ISimplifiedThreadStructureEnhanced;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.api.MailMessageStorage;
import com.openexchange.mail.api.unified.UnifiedFullName;
import com.openexchange.mail.api.unified.UnifiedViewService;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.threader.Conversation;
import com.openexchange.mail.threader.Conversations;
import com.openexchange.mail.utils.MailMessageComparator;
import com.openexchange.mail.utils.MailMessageComparatorFactory;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedInboxManagement;
import com.openexchange.mailaccount.UnifiedInboxUID;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.unifiedinbox.copy.UnifiedInboxMessageCopier;
import com.openexchange.unifiedinbox.dataobjects.UnifiedMailMessage;
import com.openexchange.unifiedinbox.services.Services;
import com.openexchange.unifiedinbox.utility.UnifiedInboxCompletionService;
import com.openexchange.unifiedinbox.utility.UnifiedInboxUtility;
import com.openexchange.user.UserService;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * {@link UnifiedInboxMessageStorage} - The Unified Mail message storage implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UnifiedInboxMessageStorage extends MailMessageStorage implements IMailMessageStorageExt, ISimplifiedThreadStructureEnhanced, UnifiedViewService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UnifiedInboxMessageStorage.class);

    /*-
     * Members
     */

    private final Session session;
    private final int user;
    private final int cid;
    private final Context ctx;
    private final UnifiedInboxAccess access;
    private Locale locale;
    private UnifiedInboxMessageCopier copier;

    /**
     * Initializes a new {@link UnifiedInboxMessageStorage}.
     *
     * @param access The Unified Mail access
     * @param session The session providing needed user data
     * @throws OXException If context loading fails
     */
    public UnifiedInboxMessageStorage(UnifiedInboxAccess access, Session session) throws OXException {
        super();
        this.access = access;
        this.session = session;
        cid = session.getContextId();
        {
            ContextService contextService = Services.getService(ContextService.class);
            ctx = contextService.getContext(cid);
        }
        user = session.getUserId();
    }

    /**
     * Initializes a new stateless {@link UnifiedInboxMessageStorage}.
     */
    public UnifiedInboxMessageStorage() {
        super();
        access = null;
        session = null;
        user = 0;
        cid = 0;
        ctx = null;
    }

    private static MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getStoredMailAccessFor(int accountId, Session session, UnifiedInboxAccess access) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = access.getOpenedMailAccess(accountId);
        if (null == mailAccess) {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            access.storeOpenedMailAccessIfAbsent(accountId, mailAccess);
        }
        return mailAccess;
    }

    protected static void closeSafe(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
        if (null == mailAccess) {
            return;
        }
        mailAccess.close(true);
    }

    /**
     * Gets session user's locale
     *
     * @return The session user's locale
     * @throws OXException If retrieving user's locale fails
     */
    protected Locale getLocale() throws OXException {
        if (null == locale) {
            UserService userService = Services.getService(UserService.class);
            locale = userService.getUser(session.getUserId(), ctx).getLocale();
        }
        return locale;
    }

    private UnifiedInboxMessageCopier getCopier() {
        if (null == copier) {
            copier = new UnifiedInboxMessageCopier(session, access);
        }
        return copier;
    }

    private List<MailAccount> getAccounts() throws OXException {
        return getAccounts(true, access.getAccountId(), user, cid);
    }

    private static List<MailAccount> getAccounts(boolean onlyEnabled, int unifiedMailAccountId, int userId, int contextId) throws OXException {
        MailAccount[] tmp = Services.getService(MailAccountStorageService.class).getUserMailAccounts(userId, contextId);
        List<MailAccount> accounts = new ArrayList<>(tmp.length);
        for (MailAccount mailAccount : tmp) {
            if (unifiedMailAccountId != mailAccount.getId() && (!onlyEnabled || mailAccount.isUnifiedINBOXEnabled()) && !mailAccount.isDeactivated()) {
                accounts.add(mailAccount);
            }
        }
        return accounts;
    }

    private static TIntObjectMap<MailAccount> getAccountsMap(boolean onlyEnabled, int unifiedMailAccountId, int userId, int contextId) throws OXException {
        List<MailAccount> al = getAccounts(onlyEnabled, unifiedMailAccountId, userId, contextId);
        TIntObjectMap<MailAccount> accounts = new TIntObjectHashMap<MailAccount>(al.size());
        for (MailAccount account : al) {
            accounts.put(account.getId(), account);
        }
        return accounts;
    }

    /**
     * Removes such fields from given <code>MailFields</code> instance that are not supported by external mail accounts.
     * <p>
     * <ul>
     * <li>{@link MailField#AUTHENTICATION_OVERALL_RESULT}
     * <li>{@link MailField#AUTHENTICATION_MECHANISM_RESULTS}
     * <li>{@link MailField#TEXT_PREVIEW_IF_AVAILABLE}
     * <li>{@link MailField#TEXT_PREVIEW}
     * <li>{@link MailField#ATTACHMENT_NAME}
     * </ul>
     *
     * @param mfs The <code>MailFields</code> instance to remove from
     * @throws IllegalArgumentException If given <code>MailFields</code> instance is <code>null</code>
     */
    private static void removeUnsupportedFieldsForExternalAccounts(MailFields mfs) {
        if (mfs == null) {
            throw new IllegalArgumentException("Mail fields must not be null");
        }

        mfs.removeMailField(MailField.AUTHENTICATION_OVERALL_RESULT);
        mfs.removeMailField(MailField.AUTHENTICATION_MECHANISM_RESULTS);
        mfs.removeMailField(MailField.TEXT_PREVIEW_IF_AVAILABLE);
        mfs.removeMailField(MailField.TEXT_PREVIEW);
        mfs.removeMailField(MailField.ATTACHMENT_NAME);
    }

    /**
     * Fills specified header names into the given messages.
     *
     * @param headerNames The header names to fill
     * @param accountMails The messages to fill the headers into
     * @param fn The folder full name
     * @param messageStorage The message storage to query from
     * @throws OXException If filling headers fails
     */
    private static void fillHeaders(String[] headerNames, MailMessage[] accountMails, String fn, IMailMessageStorage messageStorage) throws OXException {
        int length = accountMails.length;
        MailMessage[] headers;
        {
            String[] ids = new String[length];
            for (int i = ids.length; i-- > 0;) {
                MailMessage m = accountMails[i];
                ids[i] = null == m ? null : m.getMailId();
            }
            headers = messageStorage.getMessages(fn, ids, MailFields.toArray(MailField.HEADERS));
        }

        for (int i = length; i-- > 0;) {
            MailMessage mailMessage = accountMails[i];
            if (null != mailMessage) {
                MailMessage header = headers[i];
                if (null != header) {
                    for (String headerName : headerNames) {
                        String[] values = header.getHeader(headerName);
                        if (null != values) {
                            for (String value : values) {
                                mailMessage.addHeader(headerName, value);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the mails for given identifiers.
     *
     * @param fullName The folder full name
     * @param mailIds The mail identifiers
     * @param fields The fields to fill
     * @param headerNames The optional headers
     * @param messageStorage The message storage to query from
     * @return The mails
     * @throws OXException If getting mails fails
     */
    private static MailMessage[] getMessagesFor(String fullName, List<String> mailIds, MailField[] fields, String[] headerNames, IMailMessageStorage messageStorage) throws OXException {
        return getMessagesFor(fullName, mailIds.toArray(new String[mailIds.size()]), fields, headerNames, messageStorage);
    }

    /**
     * Gets the mails for given identifiers.
     *
     * @param fullName The folder full name
     * @param mailIds The mail identifiers
     * @param fields The fields to fill
     * @param headerNames The optional headers
     * @param messageStorage The message storage to query from
     * @return The mails
     * @throws OXException If getting mails fails
     */
    private static MailMessage[] getMessagesFor(String fullName, String[] mailIds, MailField[] fields, String[] headerNames, IMailMessageStorage messageStorage) throws OXException {
        if (null == headerNames || headerNames.length <= 0) {
            // No headers to query
            return messageStorage.getMessages(fullName, mailIds, fields);
        }

        IMailMessageStorageExt messageStorageExt = messageStorage.supports(IMailMessageStorageExt.class);
        if (null != messageStorageExt) {
            // Capable message storage to query headrs directly
            return messageStorageExt.getMessages(fullName, mailIds, fields, headerNames);
        }

        MailMessage[] mails = messageStorage.getMessages(fullName, mailIds, MailFields.addIfAbsent(fields, MailField.ID));
        if (null != mails && mails.length > 0) {
            // Manually fill headers
            fillHeaders(headerNames, mails, fullName, messageStorage);
        }
        return mails;
    }

    /**
     * Searches he mails for given arguments.
     *
     * @param fullName The folder full name
     * @param indexRange The index range
     * @param sortField The field to sort by
     * @param order The Order direction
     * @param searchTerm The optional search term
     * @param fields The fields to fill
     * @param headerNames The optional headers
     * @param messageStorage The message storage to query from
     * @return The mails
     * @throws OXException If searching mails fails
     */
    private static MailMessage[] searchMessagesFor(String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] fields, String[] headerNames, IMailMessageStorage messageStorage) throws OXException {
        if (null == headerNames || headerNames.length <= 0) {
            return messageStorage.searchMessages(fullName, indexRange, sortField, order, searchTerm, fields);
        }

        IMailMessageStorageExt messageStorageExt = messageStorage.supports(IMailMessageStorageExt.class);
        if (null != messageStorageExt) {
            return messageStorageExt.searchMessages(fullName, indexRange, sortField, order, searchTerm, fields, headerNames);
        }

        MailMessage[] accountMails = messageStorage.searchMessages(fullName, indexRange, sortField, order, searchTerm, MailFields.addIfAbsent(fields, MailField.ID));
        if (null != accountMails && accountMails.length > 0) {
            // Manually fill headers
            fillHeaders(headerNames, accountMails, fullName, messageStorage);
        }
        return accountMails;
    }

    @Override
    public void releaseResources() {
        // Nothing to release
    }

    @Override
    public MailMessage[] getMessagesByMessageID(String... messageIDs) throws OXException {
        if (null == messageIDs || messageIDs.length <= 0) {
            return new MailMessage[0];
        }

        throw MailExceptionCode.UNSUPPORTED_OPERATION.create();
    }

    @Override
    public MailMessage[] getMessages(String fullName, String[] mailIds, MailField[] fields) throws OXException {
        return getMessages(fullName, mailIds, fields, null);
    }

    @Override
    public MailMessage[] getMessages(String fullName, String[] mailIds, MailField[] fields, String[] headerNames) throws OXException {
        return getMessages(fullName, mailIds, fields, headerNames, session, access);
    }

    @Override
    public MailMessage[] getMessages(UnifiedFullName fullName, String[] mailIds, MailField[] fields, Session session) throws OXException {
        int unifiedAccountId = Services.getService(UnifiedInboxManagement.class).getUnifiedINBOXAccountID(session);

        UnifiedInboxAccess access = new UnifiedInboxAccess(session, unifiedAccountId);
        access.connectInternal();

        return getMessages(fullName.getFullName(), mailIds, fields, null, session, access);
    }

    private static MailMessage[] getMessages(String fullName, String[] mailIds, final MailField[] fieldz, final String[] headerNames, Session session, final UnifiedInboxAccess access) throws OXException {
        if ((mailIds == null) || (mailIds.length == 0)) {
            return EMPTY_RETVAL;
        }
        {
            MailFields fieldSet = new MailFields(fieldz);
            if (fieldSet.contains(MailField.FULL) || fieldSet.contains(MailField.BODY)) {
                MailMessage[] mails = new MailMessage[mailIds.length];
                for (int j = 0; j < mails.length; j++) {
                    mails[j] = getMessage(fullName, mailIds[j], true, session, access);
                }
                return mails;
            }
        }
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            final MailMessage[] messages = new MailMessage[mailIds.length];
            // Parse mail IDs
            TIntObjectMap<Map<String, List<String>>> parsed = UnifiedInboxUtility.parseMailIDs(mailIds);
            // Create completion service for simultaneous access
            UnifiedInboxCompletionService<GetMessagesResult> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            // Iterate parsed map and submit a task for each iteration
            int numTasks = 0;
            TIntObjectIterator<Map<String, List<String>>> iter = parsed.iterator();
            for (int i = parsed.size(); i-- > 0;) {
                iter.advance();
                final int accountId = iter.key();
                final MailField[] fields;
                if (accountId == Account.DEFAULT_ID) {
                    fields = fieldz;
                } else {
                    MailFields mfs = new MailFields(fieldz);
                    removeUnsupportedFieldsForExternalAccounts(mfs);
                    fields = mfs.toArray();
                }
                final Map<String, List<String>> folderUIDMap = iter.value();
                numTasks++;
                completionService.submit(new AbstractAccountOperation<GetMessagesResult>(session, accountId, access.isDebug()) {

                    @Override
                    public GetMessagesResult doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws OXException {
                        // Get account's mail access
                        try {
                            for (Iterator<Map.Entry<String, List<String>>> inneriter = folderUIDMap.entrySet().iterator(); inneriter.hasNext();) {
                                Map.Entry<String, List<String>> e = inneriter.next();
                                String folder = e.getKey();
                                List<String> uids = e.getValue();
                                try {
                                    MailMessage[] mails = getMessagesFor(folder, uids, fields, headerNames, mailAccess.getMessageStorage());
                                    for (MailMessage mail : messages) {
                                        if (null != mail) {
                                            mail.setAccountId(accountId);
                                        }
                                    }
                                    return new GetMessagesResult(accountId, folder, mails);
                                } catch (OXException me) {
                                    MailConfig config = mailAccess.getMailConfig();
                                    getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\"", (null == folder ? "<unknown>" : folder), config.getServer(), config.getLogin(), me);
                                    return GetMessagesResult.EMPTY_RESULT;
                                } catch (RuntimeException rte) {
                                    MailConfig config = mailAccess.getMailConfig();
                                    getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\"", (null == folder ? "<unknown>" : folder), config.getServer(), config.getLogin(), rte);
                                    return GetMessagesResult.EMPTY_RESULT;
                                }
                            }
                        } catch (OXException e) {
                            e.setCategory(Category.CATEGORY_WARNING);
                            access.addWarnings(Collections.singleton(e));
                            getLogger().debug("", e);
                            return GetMessagesResult.EMPTY_RESULT;
                        }
                        // Return dummy object
                        return GetMessagesResult.EMPTY_RESULT;
                    }
                });
            }
            // Wait for completion of each submitted task
            int undelegatedAccountId = access.getAccountId();
            try {
                for (int i = 0; i < numTasks; i++) {
                    GetMessagesResult result = completionService.take().get();
                    insertMessage(mailIds, messages, result.accountId, result.folder, result.mails, fullName, undelegatedAccountId);
                }
                LOG.debug("Retrieval of {} messages from folder \"{}\" took {}msec.", I(mailIds.length), fullName, L(completionService.getDuration()));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
            // Return properly filled array
            return messages;
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            // Get messages
            final MailField[] fields;
            if (accountId == Account.DEFAULT_ID) {
                fields = fieldz;
            } else {
                MailFields mfs = new MailFields(fieldz);
                removeUnsupportedFieldsForExternalAccounts(mfs);
                fields = mfs.toArray();
            }
            MailMessage[] mails = getMessagesFor(fa.getFullName(), mailIds, fields, headerNames, mailAccess.getMessageStorage());
            int unifiedAccountId = access.getAccountId();
            for (MailMessage mail : mails) {
                if (null != mail) {
                    mail.setFolder(fullName);
                    mail.setAccountId(unifiedAccountId);
                    mail.setOriginalId(mail.getMailId()); // ID stays the same
                    mail.setOriginalFolder(fa);
                }
            }
            return mails;
        } finally {
            closeSafe(mailAccess);
        }
    }

    @Override
    public MailPart getImageAttachment(String fullName, String mailId, String contentId) throws OXException {
        if (Strings.isEmpty(contentId)) {
            return null;
        }
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            UnifiedInboxUID uid = new UnifiedInboxUID(mailId);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                // Get stored or newly connect MailAccess instance
                mailAccess = getStoredMailAccessFor(uid.getAccountId(), session, access);

                // Get part
                MailPart part = mailAccess.getMessageStorage().getImageAttachment(uid.getFullName(), uid.getId(), contentId);
                if (null == part) {
                    return null;
                }
                return part;
            } finally {
                // Nothing
            }
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            // Get stored or newly connect MailAccess instance
            mailAccess = getStoredMailAccessFor(fa.getAccountId(), session, access);

            // Get part
            MailPart part = mailAccess.getMessageStorage().getImageAttachment(fa.getFullname(), mailId, contentId);
            if (null == part) {
                return null;
            }
            return part;
        } finally {
            // Nothing
        }
    }

    @Override
    public MailPart getAttachment(String fullName, String mailId, String sequenceId) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            UnifiedInboxUID uid = new UnifiedInboxUID(mailId);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                // Get stored or newly connect MailAccess instance
                mailAccess = getStoredMailAccessFor(uid.getAccountId(), session, access);

                // Get part
                MailPart part = mailAccess.getMessageStorage().getAttachment(uid.getFullName(), uid.getId(), sequenceId);
                if (null == part) {
                    return null;
                }
                return part;
            } finally {
                // Nothing
            }
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            // Get stored or newly connect MailAccess instance
            mailAccess = getStoredMailAccessFor(fa.getAccountId(), session, access);

            // Get part
            MailPart part = mailAccess.getMessageStorage().getAttachment(fa.getFullname(), mailId, sequenceId);
            if (null == part) {
                return null;
            }
            return part;
        } finally {
            // Nothing
        }
    }

    @Override
    public MailMessage getMessage(String fullName, String mailId, boolean markSeen) throws OXException {
        return getMessage(fullName, mailId, markSeen, session, access);
    }

    @Override
    public MailMessage getMessage(UnifiedFullName fullName, String mailId, boolean markSeen, Session session) throws OXException {
        int unifiedAccountId = Services.getService(UnifiedInboxManagement.class).getUnifiedINBOXAccountID(session);

        UnifiedInboxAccess access = new UnifiedInboxAccess(session, unifiedAccountId);
        access.connectInternal();

        return getMessage(fullName.getFullName(), mailId, markSeen, session, access);
    }

    private static MailMessage getMessage(final String fullName, String mailId, boolean markSeen, Session session, final UnifiedInboxAccess access) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            UnifiedInboxUID uid = new UnifiedInboxUID(mailId);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                // Get stored or newly connect MailAccess instance
                int accountId = uid.getAccountId();
                mailAccess = getStoredMailAccessFor(accountId, session, access);

                // Get the message
                MailMessage mail = mailAccess.getMessageStorage().getMessage(uid.getFullName(), uid.getId(), markSeen);
                if (null == mail) {
                    return null;
                }

                // Determine unread count
                boolean wasUnseen = markSeen && mail.containsPrevSeen() && !mail.isPrevSeen();
                Future<Integer> future = null;
                if (wasUnseen) {
                    future = ThreadPools.getThreadPool().submit(new AbstractTask<Integer>() {

                        @Override
                        public Integer call() throws OXException {
                            return Integer.valueOf(access.getFolderStorage().getUnreadCounter(fullName));
                        }
                    });
                }

                // Convert to Unified Mail message
                mail = new UnifiedMailMessage(mail, access.getAccountId());
                mail.setMailId(mailId);
                mail.setFolder(fullName);
                mail.setAccountId(accountId);
                mail.setOriginalId(uid.getId());
                mail.setOriginalFolder(new FullnameArgument(accountId, uid.getFullName()));
                if (null != future) {
                    try {
                        mail.setUnreadMessages(future.get().intValue());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
                    } catch (ExecutionException e) {
                        throw ThreadPools.launderThrowable(e, OXException.class);
                    }
                }
                return mail;
            } finally {
                // Nothing
            }
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            // Get stored or newly connect MailAccess instance
            mailAccess = getStoredMailAccessFor(fa.getAccountId(), session, access);

            // Get message
            MailMessage mail = mailAccess.getMessageStorage().getMessage(fa.getFullname(), mailId, markSeen);
            if (null == mail) {
                return null;
            }

            // Prepare it
            int unifiedAccountId = access.getAccountId();
            // mail.loadContent();
            mail.setFolder(fullName);
            mail.setAccountId(unifiedAccountId);
            mail.setOriginalId(mail.getMailId()); // ID stays the same
            mail.setOriginalFolder(fa);
            return mail;
        } finally {
            // Nothing
        }
    }

    static final MailMessageComparator COMPARATOR = new MailMessageComparator(MailSortField.RECEIVED_DATE, true, null);

    @Override
    public List<List<MailMessage>> getThreadSortedMessages(final String fullName, final boolean includeSent, boolean cache, IndexRange indexRange, final long max, final MailSortField sortField, final OrderDirection order, final MailField[] mailFieldz, final String[] headerNames, final SearchTerm<?> searchTerm) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            List<MailAccount> accounts = getAccounts();
            final int undelegatedAccountId = access.getAccountId();
            MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE :  sortField;
            MailFields mfs = new MailFields(mailFieldz);
            mfs.add(MailField.getField(effectiveSortField.getField()));
            final MailField[] checkedFieldsForPrimary = mfs.toArray();
            removeUnsupportedFieldsForExternalAccounts(mfs);
            final MailField[] checkedFieldsForExternal = mfs.toArray();
            Session session = this.session;
            // Create completion service for simultaneous access
            int length = accounts.size();
            UnifiedInboxCompletionService<List<List<MailMessage>>> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            final IndexRange applicableRange = null == indexRange ? null : new IndexRange(0, indexRange.end);
            for (final MailAccount mailAccount : accounts) {
                final MailField[] checkedFields = mailAccount.isDefaultAccount() ? checkedFieldsForPrimary : checkedFieldsForExternal;
                completionService.submit(new AbstractAccountOperation<List<List<MailMessage>>>(session, mailAccount, access.isDebug()) {

                    @Override
                    public List<List<MailMessage>> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                        int accountId = mailAccount.getId();
                        String fn = null;
                        try {
                            // Get real full name
                            fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                            // Check if denoted account has such a default folder
                            if (fn == null) {
                                return Collections.emptyList();
                            }
                            // Get account's messages
                            IMailMessageStorage messageStorage = mailAccess.getMessageStorage();

                            ISimplifiedThreadStructureEnhanced structureEnhanced = messageStorage.supports(ISimplifiedThreadStructureEnhanced.class);
                            if (null != structureEnhanced) {
                                try {
                                    List<List<MailMessage>> list = structureEnhanced.getThreadSortedMessages(fn, includeSent, false, applicableRange, max, sortField, order, checkedFields, headerNames, searchTerm);
                                    List<List<MailMessage>> ret = new ArrayList<>(list.size());
                                    UnifiedInboxUID helper = new UnifiedInboxUID();
                                    for (List<MailMessage> list2 : list) {
                                        List<MailMessage> messages = new ArrayList<>(list2.size());
                                        for (MailMessage accountMail : list2) {
                                            UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                            String accountMailFolder = accountMail.getFolder();
                                            umm.setMailId(helper.setUID(accountId, accountMailFolder, accountMail.getMailId()).toString());
                                            umm.setFolder(fn.equals(accountMailFolder) ? fullName : UnifiedInboxAccess.SENT);
                                            umm.setAccountId(accountId);
                                            umm.setOriginalId(accountMail.getMailId());
                                            umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                            messages.add(umm);
                                        }
                                        ret.add(messages);
                                    }
                                    return ret;
                                } catch (OXException e) {
                                    if (!MailExceptionCode.UNSUPPORTED_OPERATION.equals(e)) {
                                        throw e;
                                    }
                                    // Use fall-back mechanism
                                }
                            }

                            ISimplifiedThreadStructure structure = messageStorage.supports(ISimplifiedThreadStructure.class);
                            if (null != structure) {
                                try {
                                    List<List<MailMessage>> list = structure.getThreadSortedMessages(fn, includeSent, false, applicableRange, max, sortField, order, checkedFields, searchTerm);

                                    if (null != headerNames && headerNames.length > 0) {
                                        MessageUtility.enrichWithHeaders(list, headerNames, messageStorage);
                                    }

                                    List<List<MailMessage>> ret = new ArrayList<>(list.size());
                                    UnifiedInboxUID helper = new UnifiedInboxUID();
                                    for (List<MailMessage> list2 : list) {
                                        List<MailMessage> messages = new ArrayList<>(list2.size());
                                        for (MailMessage accountMail : list2) {
                                            UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                            String accountMailFolder = accountMail.getFolder();
                                            umm.setMailId(helper.setUID(accountId, accountMailFolder, accountMail.getMailId()).toString());
                                            umm.setFolder(fn.equals(accountMailFolder) ? fullName : UnifiedInboxAccess.SENT);
                                            umm.setAccountId(accountId);
                                            umm.setOriginalId(accountMail.getMailId());
                                            umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                            messages.add(umm);
                                        }
                                        ret.add(messages);
                                    }
                                    return ret;
                                } catch (OXException e) {
                                    if (!MailExceptionCode.UNSUPPORTED_OPERATION.equals(e)) {
                                        throw e;
                                    }
                                    // Use fall-back mechanism
                                }
                            }
                            /*-
                             * 1. Send 'all' request with id, folder_id, level, and received_date - you need all that data.
                             *
                             * 2. Whenever level equals 0, a new thread starts (new array)
                             *
                             * 3. Add all objects (id, folder_id, received_date) to that list until level !== 0.
                             *
                             * 4. Order by received_date (ignore the internal level structure), so that the newest mails show up first.
                             *
                             * 5. Generate the real list of all threads. This must be again ordered by received_date, so that the most recent threads show up
                             *    first. id and folder_id refer to the most recent mail.
                             */
                            MailMessage[] msgArr;
                            try {
                                msgArr = messageStorage.getThreadSortedMessages(fn, applicableRange, sortField, order, null, checkedFields);
                            } catch (OXException e) {
                                msgArr = messageStorage.getAllMessages(fn, applicableRange, sortField, order, checkedFields);
                            }
                            List<List<MailMessage>> list = new LinkedList<>();
                            List<MailMessage> current = new LinkedList<>();
                            // Here we go
                            int size = msgArr.length;
                            for (int i = 0; i < size; i++) {
                                MailMessage mail = msgArr[i];
                                if (null != mail) {
                                    int threadLevel = mail.getThreadLevel();
                                    if (0 == threadLevel) {
                                        list.add(current);
                                        current = new LinkedList<>();
                                    }
                                    current.add(mail);
                                }
                            }
                            list.add(current);
                            /*
                             * Sort empty ones
                             */
                            for (Iterator<List<MailMessage>> iterator = list.iterator(); iterator.hasNext();) {
                                List<MailMessage> mails = iterator.next();
                                if (null == mails || mails.isEmpty()) {
                                    iterator.remove();
                                } else {
                                    Collections.sort(mails, COMPARATOR);
                                }
                            }
                            /*
                             * Sort root elements
                             */
                            MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE :  sortField;
                            final MailMessageComparator comparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, getLocale(), getSession(), mailAccess.getMailConfig().getMailProperties().isUserFlagsEnabled());
                            Collections.sort(list, (Comparator<List<MailMessage>>) (mails1, mails2) -> comparator.compare(mails1.get(0), mails2.get(0)));
                            List<List<MailMessage>> ret = new ArrayList<>(list.size());
                            UnifiedInboxUID helper = new UnifiedInboxUID();
                            for (List<MailMessage> list2 : list) {
                                List<MailMessage> messages = new ArrayList<>(list2.size());
                                for (MailMessage accountMail : list2) {
                                    UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                    String accountMailFolder = accountMail.getFolder();
                                    umm.setMailId(helper.setUID(accountId, accountMailFolder, accountMail.getMailId()).toString());
                                    umm.setFolder(fn.equals(accountMailFolder) ? fullName : UnifiedInboxAccess.SENT);
                                    umm.setAccountId(accountId);
                                    umm.setOriginalId(accountMail.getMailId());
                                    umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                    messages.add(umm);
                                }
                                ret.add(messages);
                            }

                            if (null != headerNames && headerNames.length > 0) {
                                MessageUtility.enrichWithHeaders(ret, headerNames, messageStorage);
                            }

                            return ret;
                        } catch (OXException e) {
                            e.setCategory(Category.CATEGORY_WARNING);
                            access.addWarnings(Collections.singleton(e));
                            getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\"", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            return Collections.emptyList();
                        } catch (RuntimeException e) {
                            getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\"", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            return Collections.emptyList();
                        }
                    }
                });
            }
            // Wait for completion of each submitted task
            try {
                List<List<MailMessage>> messages = new ArrayList<>(length << 2);
                for (int i = length; i-- > 0;) {
                    messages.addAll(completionService.take().get());
                }
                LOG.debug("getThreadSortedMessages from folder \"{}\" took {}msec.", fullName, L(completionService.getDuration()));

                // Sort them
                final MailMessageComparator comparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, locale, session, true);
                Collections.sort(messages, (Comparator<List<MailMessage>>) (o1, o2) -> comparator.compare(o1.get(0), o2.get(0)));
                // Return as array
                if (null == indexRange) {
                    return messages;
                }
                // Apply index range
                int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                if (fromIndex > messages.size()) {
                    /*
                     * Return empty iterator if start is out of range
                     */
                    return Collections.emptyList();
                }
                /*
                 * Reset end index if out of range
                 */
                if (toIndex >= messages.size()) {
                    if (fromIndex == 0) {
                        return messages;
                    }
                    toIndex = messages.size();
                }
                messages = messages.subList(fromIndex, toIndex);
                return messages;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
        }
        /*
         * Certain account's folder
         */
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            // Get account's messages
            final IMailMessageStorage messageStorage = mailAccess.getMessageStorage();
            final MailField[] mailFields;
            if (accountId == Account.DEFAULT_ID) {
                mailFields = mailFieldz;
            } else {
                MailFields mfs = new MailFields(mailFieldz);
                removeUnsupportedFieldsForExternalAccounts(mfs);
                mailFields = mfs.toArray();
            }

            ISimplifiedThreadStructureEnhanced structureEnhanced = messageStorage.supports(ISimplifiedThreadStructureEnhanced.class);
            if (null != structureEnhanced) {
                try {
                    List<List<MailMessage>> conversations = structureEnhanced.getThreadSortedMessages(fa.getFullname(), includeSent, false, indexRange, max, sortField, order, mailFields, headerNames, searchTerm);
                    int unifiedAccountId = access.getAccountId();
                    for (List<MailMessage> conversation : conversations) {
                        for (MailMessage mail : conversation) {
                            mail.setFolder(fullName);
                            mail.setAccountId(unifiedAccountId);
                            mail.setOriginalId(mail.getMailId()); // ID stays the same
                            mail.setOriginalFolder(fa);
                        }
                    }
                    return conversations;
                } catch (OXException e) {
                    if (!MailExceptionCode.UNSUPPORTED_OPERATION.equals(e)) {
                        throw e;
                    }
                    // Use fall-back mechanism
                }
            }

            ISimplifiedThreadStructure structure = messageStorage.supports(ISimplifiedThreadStructure.class);
            if (null != structure) {
                try {
                    List<List<MailMessage>> conversations = structure.getThreadSortedMessages(fa.getFullname(), includeSent, false, indexRange, max, sortField, order, mailFields, searchTerm);

                    if (null != headerNames && headerNames.length > 0) {
                        MessageUtility.enrichWithHeaders(conversations, headerNames, messageStorage);
                    }

                    int unifiedAccountId = access.getAccountId();
                    for (List<MailMessage> conversation : conversations) {
                        for (MailMessage mail : conversation) {
                            mail.setFolder(fullName);
                            mail.setAccountId(unifiedAccountId);
                            mail.setOriginalId(mail.getMailId()); // ID stays the same
                            mail.setOriginalFolder(fa);
                        }
                    }

                    return conversations;
                } catch (OXException e) {
                    if (!MailExceptionCode.UNSUPPORTED_OPERATION.equals(e)) {
                        throw e;
                    }
                    // Use fall-back mechanism
                }
            }
            /*-
             * --------------------------------------------------------------------------------------------------------------------------
             *
             * Manually do thread-sort
             *
             * Sort by references
             */
            String realFullName = fa.getFullname();
            boolean mergeWithSent = includeSent && !mailAccess.getFolderStorage().getSentFolder().equals(realFullName);
            Future<List<MailMessage>> messagesFromSentFolder;
            if (mergeWithSent) {
                final String sentFolder = mailAccess.getFolderStorage().getSentFolder();
                messagesFromSentFolder = ThreadPools.getThreadPool().submit(new AbstractTask<List<MailMessage>>() {

                    @Override
                    public List<MailMessage> call() throws Exception {
                        return Conversations.messagesFor(sentFolder, (int) max, new MailFields(mailFields), messageStorage);
                    }
                });
            } else {
                messagesFromSentFolder = null;
            }
            // For actual folder
            List<Conversation> conversations = Conversations.conversationsFor(realFullName, (int) max, new MailFields(mailFields), messageStorage);
            // Retrieve from sent folder
            if (null != messagesFromSentFolder) {
                List<MailMessage> sentMessages = getFrom(messagesFromSentFolder);
                for (Conversation conversation : conversations) {
                    for (MailMessage sentMessage : sentMessages) {
                        if (conversation.referencesOrIsReferencedBy(sentMessage)) {
                            conversation.addMessage(sentMessage);
                        }
                    }
                }
            }
            // Fold it
            Conversations.fold(conversations);
            // Comparator
            MailMessageComparator threadComparator = COMPARATOR;
            // Sort
            List<List<MailMessage>> list = new ArrayList<>(conversations.size());
            for (Conversation conversation : conversations) {
                list.add(conversation.getMessages(threadComparator));
            }
            // Sort root elements
            {
                MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE :  sortField;
                Comparator<List<MailMessage>> listComparator = getListComparator(effectiveSortField, order, getLocale(), mailAccess.getMailConfig().getMailProperties().isUserFlagsEnabled());
                Collections.sort(list, listComparator);
            }
            // Check for index range
            list = sliceMessages(list, indexRange);
            if (null != headerNames && headerNames.length > 0) {
                MessageUtility.enrichWithHeaders(list, headerNames, messageStorage);
            }
            int unifiedAccountId = access.getAccountId();
            for (List<MailMessage> conversation : list) {
                for (MailMessage mail : conversation) {
                    mail.setFolder(fullName);
                    mail.setAccountId(unifiedAccountId);
                    mail.setOriginalId(mail.getMailId()); // ID stays the same
                    mail.setOriginalFolder(fa);
                }
            }
            // Return list
            return list;
        } finally {
            closeSafe(mailAccess);
        }
    }

    @Override
    public List<List<MailMessage>> getThreadSortedMessages(final String fullName, final boolean includeSent, boolean cache, IndexRange indexRange, final long max, final MailSortField sortField, final OrderDirection order, final MailField[] mailFieldz, final SearchTerm<?> searchTerm) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            List<MailAccount> accounts = getAccounts();
            final int undelegatedAccountId = access.getAccountId();
            MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE :  sortField;
            MailFields mfs = new MailFields(mailFieldz);
            mfs.add(MailField.getField(effectiveSortField.getField()));
            final MailField[] checkedFieldsForPrimary = mfs.toArray();
            removeUnsupportedFieldsForExternalAccounts(mfs);
            final MailField[] checkedFieldsForExternal = mfs.toArray();
            Session session = this.session;
            // Create completion service for simultaneous access
            int length = accounts.size();
            UnifiedInboxCompletionService<List<List<MailMessage>>> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            final IndexRange applicableRange = null == indexRange ? null : new IndexRange(0, indexRange.end);
            for (final MailAccount mailAccount : accounts) {
                final MailField[] checkedFields = mailAccount.isDefaultAccount() ? checkedFieldsForPrimary : checkedFieldsForExternal;
                completionService.submit(new AbstractAccountOperation<List<List<MailMessage>>>(session, mailAccount, access.isDebug()) {

                    @Override
                    public List<List<MailMessage>> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                        int accountId = mailAccount.getId();
                        String fn = null;
                        try {
                            // Get real full name
                            fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                            // Check if denoted account has such a default folder
                            if (fn == null) {
                                return Collections.emptyList();
                            }
                            // Get account's messages
                            IMailMessageStorage messageStorage = mailAccess.getMessageStorage();

                            ISimplifiedThreadStructure structure = messageStorage.supports(ISimplifiedThreadStructure.class);
                            if (null != structure) {
                                try {
                                    List<List<MailMessage>> list = structure.getThreadSortedMessages(fn, includeSent, false, applicableRange, max, sortField, order, checkedFields, searchTerm);
                                    List<List<MailMessage>> ret = new ArrayList<>(list.size());
                                    UnifiedInboxUID helper = new UnifiedInboxUID();
                                    for (List<MailMessage> list2 : list) {
                                        List<MailMessage> messages = new ArrayList<>(list2.size());
                                        for (MailMessage accountMail : list2) {
                                            UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                            String accountMailFolder = accountMail.getFolder();
                                            umm.setMailId(helper.setUID(accountId, accountMailFolder, accountMail.getMailId()).toString());
                                            umm.setFolder(fn.equals(accountMailFolder) ? fullName : UnifiedInboxAccess.SENT);
                                            umm.setAccountId(accountId);
                                            umm.setOriginalId(accountMail.getMailId());
                                            umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                            messages.add(umm);
                                        }
                                        ret.add(messages);
                                    }
                                    return ret;
                                } catch (OXException e) {
                                    if (!MailExceptionCode.UNSUPPORTED_OPERATION.equals(e)) {
                                        throw e;
                                    }
                                    // Use fall-back mechanism
                                }
                            }
                            /*-
                             * 1. Send 'all' request with id, folder_id, level, and received_date - you need all that data.
                             *
                             * 2. Whenever level equals 0, a new thread starts (new array)
                             *
                             * 3. Add all objects (id, folder_id, received_date) to that list until level !== 0.
                             *
                             * 4. Order by received_date (ignore the internal level structure), so that the newest mails show up first.
                             *
                             * 5. Generate the real list of all threads. This must be again ordered by received_date, so that the most recent threads show up
                             *    first. id and folder_id refer to the most recent mail.
                             */
                            MailMessage[] msgArr;
                            try {
                                msgArr = messageStorage.getThreadSortedMessages(fn, applicableRange, sortField, order, null, checkedFields);
                            } catch (OXException e) {
                                msgArr = messageStorage.getAllMessages(fn, applicableRange, sortField, order, checkedFields);
                            }
                            List<List<MailMessage>> list = new LinkedList<>();
                            List<MailMessage> current = new LinkedList<>();
                            // Here we go
                            int size = msgArr.length;
                            for (int i = 0; i < size; i++) {
                                MailMessage mail = msgArr[i];
                                if (null != mail) {
                                    int threadLevel = mail.getThreadLevel();
                                    if (0 == threadLevel) {
                                        list.add(current);
                                        current = new LinkedList<>();
                                    }
                                    current.add(mail);
                                }
                            }
                            list.add(current);
                            /*
                             * Sort empty ones
                             */
                            for (Iterator<List<MailMessage>> iterator = list.iterator(); iterator.hasNext();) {
                                List<MailMessage> mails = iterator.next();
                                if (null == mails || mails.isEmpty()) {
                                    iterator.remove();
                                } else {
                                    Collections.sort(mails, COMPARATOR);
                                }
                            }
                            /*
                             * Sort root elements
                             */
                            MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE :  sortField;
                            MailMessageComparator comparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, getLocale(), getSession(), mailAccess.getMailConfig().getMailProperties().isUserFlagsEnabled());
                            Collections.sort(list, (Comparator<List<MailMessage>>) (mails1, mails2) -> comparator.compare(mails1.get(0), mails2.get(0)));
                            List<List<MailMessage>> ret = new ArrayList<>(list.size());
                            UnifiedInboxUID helper = new UnifiedInboxUID();
                            for (List<MailMessage> list2 : list) {
                                List<MailMessage> messages = new ArrayList<>(list2.size());
                                for (MailMessage accountMail : list2) {
                                    UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                    String accountMailFolder = accountMail.getFolder();
                                    umm.setMailId(helper.setUID(accountId, accountMailFolder, accountMail.getMailId()).toString());
                                    umm.setFolder(fn.equals(accountMailFolder) ? fullName : UnifiedInboxAccess.SENT);
                                    umm.setAccountId(accountId);
                                    umm.setOriginalId(accountMail.getMailId());
                                    umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                    messages.add(umm);
                                }
                                ret.add(messages);
                            }
                            return ret;
                        } catch (Exception e) {
                            getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\"", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            return Collections.emptyList();
                        }
                    }
                });
            }
            // Wait for completion of each submitted task
            try {
                List<List<MailMessage>> messages = new ArrayList<>(length << 2);
                for (int i = length; i-- > 0;) {
                    messages.addAll(completionService.take().get());
                }
                LOG.debug("getThreadSortedMessages from folder \"{}\" took {}msec.", fullName, L(completionService.getDuration()));

                // Sort them
                MailMessageComparator comparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, locale, session, true);
                Collections.sort(messages, (Comparator<List<MailMessage>>) (mails1, mails2) -> comparator.compare(mails1.get(0), mails2.get(0)));
                // Return as array
                if (null == indexRange) {
                    return messages;
                }
                // Apply index range
                int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                if (fromIndex > messages.size()) {
                    /*
                     * Return empty iterator if start is out of range
                     */
                    return Collections.emptyList();
                }
                /*
                 * Reset end index if out of range
                 */
                if (toIndex >= messages.size()) {
                    if (fromIndex == 0) {
                        return messages;
                    }
                    toIndex = messages.size();
                }
                messages = messages.subList(fromIndex, toIndex);
                return messages;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
        }
        /*
         * Certain account's folder
         */
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            // Get account's messages
            final IMailMessageStorage messageStorage = mailAccess.getMessageStorage();
            final MailField[] mailFields;
            if (accountId == Account.DEFAULT_ID) {
                mailFields = mailFieldz;
            } else {
                MailFields mfs = new MailFields(mailFieldz);
                removeUnsupportedFieldsForExternalAccounts(mfs);
                mailFields = mfs.toArray();
            }

            ISimplifiedThreadStructure structure = messageStorage.supports(ISimplifiedThreadStructure.class);
            if (null != structure) {
                try {
                    List<List<MailMessage>> conversations = structure.getThreadSortedMessages(fa.getFullname(), includeSent, false, indexRange, max, sortField, order, mailFields, searchTerm);
                    int unifiedAccountId = access.getAccountId();
                    for (List<MailMessage> conversation : conversations) {
                        for (MailMessage mail : conversation) {
                            mail.setFolder(fullName);
                            mail.setAccountId(unifiedAccountId);
                            mail.setOriginalId(mail.getMailId()); // ID stays the same
                            mail.setOriginalFolder(fa);
                        }
                    }
                    return conversations;
                } catch (OXException e) {
                    if (!MailExceptionCode.UNSUPPORTED_OPERATION.equals(e)) {
                        throw e;
                    }
                    // Use fall-back mechanism
                }
            }
            /*-
             * --------------------------------------------------------------------------------------------------------------------------
             *
             * Manually do thread-sort
             *
             * Sort by references
             */
            String realFullName = fa.getFullname();
            boolean mergeWithSent = includeSent && !mailAccess.getFolderStorage().getSentFolder().equals(realFullName);
            Future<List<MailMessage>> messagesFromSentFolder;
            if (mergeWithSent) {
                final String sentFolder = mailAccess.getFolderStorage().getSentFolder();
                messagesFromSentFolder = ThreadPools.getThreadPool().submit(new AbstractTask<List<MailMessage>>() {

                    @Override
                    public List<MailMessage> call() throws Exception {
                        return Conversations.messagesFor(sentFolder, (int) max, new MailFields(mailFields), messageStorage);
                    }
                });
            } else {
                messagesFromSentFolder = null;
            }
            // For actual folder
            List<Conversation> conversations = Conversations.conversationsFor(realFullName, (int) max, new MailFields(mailFields), messageStorage);
            // Retrieve from sent folder
            if (null != messagesFromSentFolder) {
                List<MailMessage> sentMessages = getFrom(messagesFromSentFolder);
                for (Conversation conversation : conversations) {
                    for (MailMessage sentMessage : sentMessages) {
                        if (conversation.referencesOrIsReferencedBy(sentMessage)) {
                            conversation.addMessage(sentMessage);
                        }
                    }
                }
            }
            // Fold it
            Conversations.fold(conversations);
            // Comparator
            MailMessageComparator threadComparator = COMPARATOR;
            // Sort
            List<List<MailMessage>> list = new ArrayList<>(conversations.size());
            for (Conversation conversation : conversations) {
                list.add(conversation.getMessages(threadComparator));
            }
            // Sort root elements
            {
                MailSortField effectiveSortField = null == sortField ? MailSortField.RECEIVED_DATE :  sortField;
                Comparator<List<MailMessage>> listComparator = getListComparator(effectiveSortField, order, getLocale(), mailAccess.getMailConfig().getMailProperties().isUserFlagsEnabled());
                Collections.sort(list, listComparator);
            }
            // Check for index range
            list = sliceMessages(list, indexRange);
            /*
             * Apply account identifier
             */
            int unifiedAccountId = access.getAccountId();
            for (List<MailMessage> conversation : list) {
                for (MailMessage mail : conversation) {
                    mail.setFolder(fullName);
                    mail.setAccountId(unifiedAccountId);
                    mail.setOriginalId(mail.getMailId()); // ID stays the same
                    mail.setOriginalFolder(fa);
                }
            }
            // Return list
            return list;
        } finally {
            closeSafe(mailAccess);
        }
    }

    private static List<List<MailMessage>> sliceMessages(List<List<MailMessage>> listOfConversations, IndexRange indexRange) {
        List<List<MailMessage>> list = listOfConversations;
        // Check for index range
        if (null != indexRange) {
            int fromIndex = indexRange.start;
            int toIndex = indexRange.end;
            int size = list.size();
            if ((fromIndex) > size) {
                // Return empty iterator if start is out of range
                return Collections.emptyList();
            }
            // Reset end index if out of range
            if (toIndex >= size) {
                if (fromIndex == 0) {
                    return list;
                }
                toIndex = size;
            }
            list = list.subList(fromIndex, toIndex);
        }
        // Return list
        return list;
    }

    private static <T> T getFrom(Future<T> f) throws OXException {
        if (null == f) {
            return null;
        }
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Keep interrupted state
            throw MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MessagingException) {
                throw MimeMailException.handleMessagingException((MessagingException) cause);
            }
            throw ThreadPools.launderThrowable(e, OXException.class);
        }

    }

    private Comparator<List<MailMessage>> getListComparator(final MailSortField sortField, final OrderDirection order, Locale locale, boolean userFlagsEnabled) {
        final MailMessageComparator comparator = MailMessageComparatorFactory.createComparator(sortField, order, locale, this.session, userFlagsEnabled);
        return (mails1, mails2) -> {
            int result = comparator.compare(mails1.get(0), mails2.get(0));
            if ((0 != result) || (MailSortField.RECEIVED_DATE != sortField)) {
                return result;
            }
            // Zero as comparison result AND primarily sorted by received-date
            MailMessage msg1 = mails1.get(0);
            MailMessage msg2 = mails2.get(0);
            String inReplyTo1 = msg1.getInReplyTo();
            String inReplyTo2 = msg2.getInReplyTo();
            if (null == inReplyTo1) {
                result = null == inReplyTo2 ? 0 : -1;
            } else {
                result = null == inReplyTo2 ? 1 : 0;
            }
            return 0 == result ? new MailMessageComparator(MailSortField.SENT_DATE, OrderDirection.DESC.equals(order), null).compare(msg1, msg2) : result;
        };
    }

    @Override
    public MailMessage[] getThreadSortedMessages(final String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, final SearchTerm<?> searchTerm, MailField[] fieldz) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            List<MailAccount> accounts = getAccounts();
            MailFields mfs = new MailFields(fieldz);
            mfs.add(MailField.getField(sortField.getField()));
            final MailField[] checkedFieldsForPrimary = mfs.toArray();
            removeUnsupportedFieldsForExternalAccounts(mfs);
            final MailField[] checkedFieldsForExternal = mfs.toArray();
            // Create completion service for simultaneous access
            int length = accounts.size();
            final int undelegatedAccountId = access.getAccountId();
            UnifiedInboxCompletionService<List<MailMessage>> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            for (final MailAccount mailAccount : accounts) {
                Session session = this.session;
                final MailField[] checkedFields = mailAccount.isDefaultAccount() ? checkedFieldsForPrimary : checkedFieldsForExternal;
                completionService.submit(new AbstractAccountOperation<List<MailMessage>>(session, mailAccount, access.isDebug()) {

                    @Override
                    public List<MailMessage> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                        int accountId = mailAccount.getId();
                        String fn = null;
                        try {
                            // Get real full name
                            fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                            // Check if denoted account has such a default folder
                            if (fn == null) {
                                return Collections.emptyList();
                            }
                            // Get account's messages
                            MailMessage[] accountMails =  mailAccess.getMessageStorage().getThreadSortedMessages(fn, null, MailSortField.RECEIVED_DATE, OrderDirection.DESC, searchTerm, checkedFields);
                            List<MailMessage> messages = new ArrayList<>(accountMails.length);
                            UnifiedInboxUID helper = new UnifiedInboxUID();
                            for (MailMessage accountMail : accountMails) {
                                if (null != accountMail) {
                                    UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                    umm.setMailId(helper.setUID(accountId, fn, accountMail.getMailId()).toString());
                                    umm.setFolder(fullName);
                                    umm.setAccountId(accountId);
                                    umm.setOriginalId(accountMail.getMailId());
                                    umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                    messages.add(umm);
                                }
                            }
                            return messages;
                        } catch (Exception e) {
                            getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\"", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            return Collections.emptyList();
                        }
                    }
                });
            }
            // Wait for completion of each submitted task
            try {
                List<MailMessage> messages = new ArrayList<>(length << 2);
                MailMessageComparator mainComparator = MailMessageComparatorFactory.createComparator(sortField, order, getLocale(), this.session, true);

                // Check if no index range requested
                if (indexRange == null) {
                    for (int i = length; i-- > 0;) {
                        messages.addAll(completionService.take().get());
                    }
                    Collections.sort(messages, mainComparator);
                    return messages.toArray(new MailMessage[messages.size()]);
                }

                // Add & sort results from first task
                messages.addAll(completionService.take().get());
                Collections.sort(messages, mainComparator);

                // Add & sort remaining results
                for (int i = length - 1; i-- > 0;) {
                    messages.addAll(completionService.take().get());
                    Collections.sort(messages, mainComparator);

                    // Cut off overlapping ones
                    int toIndex = indexRange.end;
                    if (messages.size() > toIndex) {
                        // Drop tail
                        messages.subList(toIndex, messages.size()).clear();
                    }
                }

                // Apply index range
                int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                if (fromIndex > messages.size()) {
                    // From index out of range
                    return EMPTY_RETVAL;
                }

                // Reset end index if out of range
                if (toIndex >= messages.size()) {
                    if (fromIndex == 0) {
                        return messages.toArray(new MailMessage[messages.size()]);
                    }
                    toIndex = messages.size();
                }
                messages = messages.subList(fromIndex, toIndex);
                return messages.toArray(new MailMessage[messages.size()]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            // Get account's messages
            final MailField[] fields;
            if (accountId == Account.DEFAULT_ID) {
                fields = fieldz;
            } else {
                MailFields mfs = new MailFields(fieldz);
                removeUnsupportedFieldsForExternalAccounts(mfs);
                fields = mfs.toArray();
            }
            MailMessage[] mails = mailAccess.getMessageStorage().getThreadSortedMessages(fa.getFullname(), indexRange, sortField, order, searchTerm, fields);
            int unifiedAccountId = this.access.getAccountId();
            for (MailMessage mail : mails) {
                if (null != mail) {
                    mail.setFolder(fullName);
                    mail.setAccountId(unifiedAccountId);
                    mail.setOriginalId(mail.getMailId()); // ID stays the same
                    mail.setOriginalFolder(fa);
                }
            }
            return mails;
        } finally {
                closeSafe(mailAccess);
        }
    }

    @Override
    public void clearCache() throws OXException {
        int unifiedAccountId = Services.getService(UnifiedInboxManagement.class).getUnifiedINBOXAccountID(session);
        List<MailAccount> accounts = getAccounts(true, unifiedAccountId, session.getUserId(), session.getContextId());
        for (MailAccount mailAccount : accounts) {
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                mailAccess = MailAccess.getInstance(session, mailAccount.getId());
                mailAccess.connect(true, access.isDebug());

                IMailMessageStorage messageStorage = mailAccess.getMessageStorage();
                IMailMessageStorageExt messageStorageExt = messageStorage.supports(IMailMessageStorageExt.class);
                if (null != messageStorageExt) {
                    messageStorageExt.clearCache();
                }
            } finally {
                closeSafe(mailAccess);
            }
        }
    }

    @Override
    public int getUnreadCount(final String folder, final SearchTerm<?> searchTerm) throws OXException {
        if (ROOT_FOLDER_ID.equals(folder)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(folder);
        }
        int unifiedMailAccountId = Services.getService(UnifiedInboxManagement.class).getUnifiedINBOXAccountID(session);

        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(folder)) {
            List<MailAccount> accounts = getAccounts(true, unifiedMailAccountId, session.getUserId(), session.getContextId());
            int length = accounts.size();
            UnifiedInboxCompletionService<Integer> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            for (final MailAccount mailAccount : accounts) {
                completionService.submit(new AbstractAccountOperation<Integer>(session, mailAccount.getId(), access.isDebug()) {

                    @Override
                    public Integer doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                        String fn = null;
                        try {
                            // Get real full name
                            fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, folder);
                            // Check if denoted account has such a default folder
                            if (fn == null) {
                                return I(0);
                            }
                            IMailMessageStorage messageStorage = mailAccess.getMessageStorage();
                            return I(messageStorage.getUnreadCount(folder, searchTerm));
                        } catch (OXException e) {
                            if (MailExceptionCode.ACCOUNT_DOES_NOT_EXIST.equals(e) || MimeMailExceptionCode.LOGIN_FAILED.equals(e)) {
                                getLogger().debug("Couldn't get unread count from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            } else {
                                getLogger().warn("Couldn't get unread count from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            }
                            return I(0);
                        } catch (RuntimeException e) {
                            getLogger().warn("Couldn't get unread count from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            return I(0);
                        }
                    }
                });
            }
            // Wait for completion of each submitted task
            try {
                int result = 0;
                for (int i = length; i-- > 0;) {
                    result += completionService.take().get().intValue();
                }

                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
        }

        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(folder);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            IMailMessageStorage messageStorage = mailAccess.getMessageStorage();
            return messageStorage.getUnreadCount(folder, searchTerm);

        } finally {
            closeSafe(mailAccess);
        }
    }

    @Override
    public MailMessage[] searchMessages(String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] fields) throws OXException {
        return searchMessages(fullName, indexRange, sortField, order, searchTerm, fields, null);
    }

    @Override
    public MailMessage[] searchMessages(String fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] fields, String[] headerNames) throws OXException {
        return searchMessages(fullName, indexRange, sortField, order, searchTerm, fields, headerNames, session, true, access.getAccountId(), getLocale());
    }

    @Override
    public MailMessage[] searchMessages(UnifiedFullName fullName, IndexRange indexRange, MailSortField sortField, OrderDirection order, SearchTerm<?> searchTerm, MailField[] fields, Session session) throws OXException {
        int unifiedAccountId = Services.getService(UnifiedInboxManagement.class).getUnifiedINBOXAccountID(session);

        ContextService contextService = Services.getService(ContextService.class);
        UserService userService = Services.getService(UserService.class);
        Locale locale = userService.getUser(session.getUserId(), contextService.getContext(session.getContextId())).getLocale();

        return searchMessages(fullName.getFullName(), indexRange, sortField, order, searchTerm, fields, null, session, false, unifiedAccountId, locale);
    }

    @Override
    public MailMessage[] allMessages(UnifiedFullName fullName, MailField[] fields, Session session) throws OXException {
        int unifiedAccountId = Services.getService(UnifiedInboxManagement.class).getUnifiedINBOXAccountID(session);

        ContextService contextService = Services.getService(ContextService.class);
        UserService userService = Services.getService(UserService.class);
        Locale locale = userService.getUser(session.getUserId(), contextService.getContext(session.getContextId())).getLocale();

        return searchMessages(fullName.getFullName(), null, MailSortField.RECEIVED_DATE, OrderDirection.DESC, null, fields, null, session, false, unifiedAccountId, locale);
    }

    private MailMessage[] searchMessages(final String fullName, final IndexRange indexRange, MailSortField sortField, final OrderDirection order, final SearchTerm<?> searchTerm, MailField[] fieldz, final String[] headerNames, Session session, boolean onlyEnabled, int unifiedMailAccountId, final Locale locale) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        final MailSortField effectiveSortField = determineSortFieldForSearch(fullName, sortField);
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            TIntObjectMap<MailAccount> accounts = getAccountsMap(onlyEnabled, unifiedMailAccountId, session.getUserId(), session.getContextId());
            MailFields mfs = StorageUtility.prepareMailFieldsForSearch(fieldz, effectiveSortField);
            final MailField[] checkedFieldsForPrimary = mfs.toArray();
            removeUnsupportedFieldsForExternalAccounts(mfs);
            final MailField[] checkedFieldsForExternal = mfs.toArray();
            // Create completion service for simultaneous access
            int length = accounts.size();
            final int undelegatedAccountId = unifiedMailAccountId;
            Executor executor = ThreadPools.getThreadPool().getExecutor();
            // Check for continuation service
            ContinuationRegistryService continuationRegistry = Services.optService(ContinuationRegistryService.class);
            if (null != continuationRegistry && mfs.contains(MailField.SUPPORTS_CONTINUATION) && !mfs.contains(MailField.FULL) && !mfs.contains(MailField.BODY)) {
                ExecutorContinuation<MailMessage> executorContinuation;
                MailMessageComparator comparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, locale, session, true);;
                {
                    ContinuationResponseGenerator<MailMessage> responseGenerator = new ContinuationResponseGenerator<MailMessage>() {

                        @Override
                        public ContinuationResponse<Collection<MailMessage>> responseFor(List<MailMessage> messages, boolean completed) throws OXException {
                            // Sort them
                            Collections.sort(messages, comparator);
                            // Return as array
                            if (null == indexRange) {
                                return new ContinuationResponse<Collection<MailMessage>>(messages, null, "mail", completed);
                            }
                            // Apply index range
                            int fromIndex = indexRange.start;
                            int toIndex = indexRange.end;
                            if (fromIndex > messages.size()) {
                                /*
                                 * Return empty iterator if start is out of range
                                 */
                                return new ContinuationResponse<Collection<MailMessage>>(Collections.<MailMessage> emptyList(), null, "mail", completed);
                            }
                            /*
                             * Reset end index if out of range
                             */
                            if (toIndex >= messages.size()) {
                                toIndex = messages.size();
                            }
                            return new ContinuationResponse<Collection<MailMessage>>(messages.subList(fromIndex, toIndex), null, "mail", completed);
                        }
                    };
                    executorContinuation = ExecutorContinuation.newContinuation(executor, responseGenerator);
                }
                // Submit tasks
                IndexRange applicableRange = null == indexRange ? null : new IndexRange(0, indexRange.end);
                for (MailAccount mailAccount : accounts.valueCollection()) {
                    MailField[] checkedFields = mailAccount.isDefaultAccount() ? checkedFieldsForPrimary : checkedFieldsForExternal;
                    executorContinuation.submit(new AbstractAccountOperation<Collection<MailMessage>>(session, mailAccount.getId(), access.isDebug()) {

                        @Override
                        public List<MailMessage> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                            int accountId = mailAccount.getId();
                            String fn = null;
                            try {
                                // Get real full name
                                fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                                // Check if denoted account has such a default folder
                                if (fn == null) {
                                    return Collections.emptyList();
                                }
                                // Determine sort option
                                MailSortField sortField = MailSortField.RECEIVED_DATE;
                                OrderDirection orderDir = OrderDirection.DESC;
                                if (null != indexRange) {
                                    // Apply proper sort option
                                    sortField = effectiveSortField;
                                    orderDir = order;
                                }
                                // Get account's messages
                                MailMessage[] accountMails = searchMessagesFor(fn, applicableRange, sortField, orderDir, searchTerm, checkedFields, headerNames, mailAccess.getMessageStorage());
                                if (null == accountMails || accountMails.length <= 0) {
                                    return Collections.emptyList();
                                }
                                List<MailMessage> messages = new ArrayList<>(accountMails.length);
                                UnifiedInboxUID helper = new UnifiedInboxUID();
                                String name = mailAccount.getName();
                                for (MailMessage accountMail : accountMails) {
                                    if (null != accountMail) {
                                        UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                        umm.setMailId(helper.setUID(accountId, fn, accountMail.getMailId()).toString());
                                        umm.setFolder(fullName);
                                        umm.setAccountId(accountId);
                                        umm.setAccountName(name);
                                        umm.setOriginalId(accountMail.getMailId());
                                        umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                        messages.add(umm);
                                    }
                                }
                                return messages;
                            } catch (Exception e) {
                                getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                                return Collections.emptyList();
                            }
                        }
                    });
                }
                // Add to registry
                continuationRegistry.putContinuation(executorContinuation, session);
                // Await first...
                try {
                    executorContinuation.awaitFirstResponse();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw MailExceptionCode.INTERRUPT_ERROR.create(e);
                }
                // Signal schedule to continuation
                throw ContinuationExceptionCodes.scheduledForContinuation(executorContinuation);
            }

            // The old way
            if (null != indexRange && indexRange.end > (MailProperties.getInstance().getMailFetchLimit(session.getUserId(), session.getContextId()) << 2)) {
                // Special handling for large message chunks
                // First, query only necessary fields for sorting to determine required message chunk
                MailField[] requiredForSorting = new MailFields(MailField.ID, MailField.FOLDER_ID, MailField.getField(effectiveSortField.getField()), MailField.RECEIVED_DATE).toArray();
                IndexRange applicableRange = new IndexRange(0, indexRange.end);
                UnifiedInboxCompletionService<List<MailMessage>> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
                for (MailAccount mailAccount : accounts.valueCollection()) {
                    completionService.submit(new AbstractAccountOperation<List<MailMessage>>(session, mailAccount.getId(), access.isDebug()) {

                        @Override
                        protected List<MailMessage> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                            String fn = null;
                            try {
                                // Get real full name
                                fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                                // Check if denoted account has such a default folder
                                if (fn == null) {
                                    return Collections.emptyList();
                                }
                                // Determine sort option
                                MailSortField sortField = effectiveSortField;
                                OrderDirection orderDir = order;
                                // Get account's messages
                                MailMessage[] accountMails = mailAccess.getMessageStorage().searchMessages(fn, applicableRange, sortField, orderDir, searchTerm, requiredForSorting);
                                List<MailMessage> messages = new ArrayList<>(accountMails.length);
                                for (MailMessage accountMail : accountMails) {
                                    if (accountMail != null) {
                                        accountMail.setAccountId(getAccountId());
                                        accountMail.setAccountName(mailAccount.getName());
                                        messages.add(accountMail);
                                    }
                                }
                                accountMails = null;
                                return messages;
                            } catch (OXException e) {
                                e.setCategory(Category.CATEGORY_WARNING);
                                access.addWarnings(Collections.singleton(e));
                                if (MailExceptionCode.ACCOUNT_DOES_NOT_EXIST.equals(e) || MimeMailExceptionCode.LOGIN_FAILED.equals(e)) {
                                    getLogger().debug("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                                } else {
                                    getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                                }
                                return Collections.emptyList();
                            } catch (RuntimeException e) {
                                getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                                return Collections.emptyList();
                            }
                        }
                    });
                }
                // Wait for completion of each submitted task
                MailMessage[] sortedMessages = null;
                try {
                    List<MailMessage> messages = new ArrayList<>(length << 2);
                    MailMessageComparator mainComparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, locale, session, true);

                    // Add & sort results from first task
                    messages.addAll(completionService.take().get());
                    Collections.sort(messages, mainComparator);

                    // Add & sort remaining results
                    for (int i = length - 1; i-- > 0;) {
                        messages.addAll(completionService.take().get());
                        Collections.sort(messages, mainComparator);

                        // Cut off overlapping ones
                        int toIndex = indexRange.end;
                        if (messages.size() > toIndex) {
                            // Drop tail
                            messages.subList(toIndex, messages.size()).clear();
                        }
                    }
                    LOG.debug("Searching messages from folder \"{}\" took {}msec.", fullName, L(completionService.getDuration()));

                    // Apply index range
                    int fromIndex = indexRange.start;
                    int toIndex = indexRange.end;
                    if (fromIndex > messages.size()) {
                        // From index out of range
                        return EMPTY_RETVAL;
                    }

                    // Reset end index if out of range
                    if (toIndex >= messages.size()) {
                        if (fromIndex == 0) {
                            sortedMessages = messages.toArray(new MailMessage[messages.size()]);
                        }
                        toIndex = messages.size();
                    }
                    if (sortedMessages == null) {
                        messages = messages.subList(fromIndex, toIndex);
                        sortedMessages = messages.toArray(new MailMessage[messages.size()]);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw MailExceptionCode.INTERRUPT_ERROR.create(e);
                } catch (ExecutionException e) {
                    throw ThreadPools.launderThrowable(e, OXException.class);
                }

                // Create a mapping for the messages to fetch with full data from each account
                TObjectIntMap<MailKey> message2index = new TObjectIntHashMap<>(sortedMessages.length);
                Map<AccAndFN, List<String>> messages2Fetch = new HashMap<>(accounts.size());
                int index = 0;
                for (MailMessage m : sortedMessages) {
                    // Remember index position for already sorted message
                    message2index.put(new MailKey(m), index++);
                    // Add to mapping
                    messages2Fetch.computeIfAbsent(new AccAndFN(accounts.get(m.getAccountId()), m.getFolder()), Functions.getNewArrayListFuntion()).add(m.getMailId());
                }

                // Second, query requested data from relevant messages
                AtomicReferenceArray<MailMessage> arrayToFill = new AtomicReferenceArray<>(sortedMessages.length);
                sortedMessages = null;
                UnifiedInboxCompletionService<Void> filler = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
                for (Map.Entry<AccAndFN, List<String>> entry : messages2Fetch.entrySet()) {
                    MailAccount mailAccount = entry.getKey().account;
                    MailField[] checkedFields = mailAccount.isDefaultAccount() ? checkedFieldsForPrimary : checkedFieldsForExternal;
                    filler.submit(new AbstractAccountOperation<Void>(session, mailAccount.getId(), access.isDebug()) {

                        @Override
                        public Void doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                            int accountId = mailAccount.getId();
                            String fn = entry.getKey().fullName;
                            try {
                                // Get account's messages
                                MailMessage[] accountMails = getMessagesFor(fn, entry.getValue(), checkedFields, headerNames, mailAccess.getMessageStorage());
                                UnifiedInboxUID helper = new UnifiedInboxUID();
                                String name = mailAccount.getName();
                                for (MailMessage accountMail : accountMails) {
                                    if (null != accountMail) {
                                        UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                        umm.setMailId(helper.setUID(accountId, fn, accountMail.getMailId()).toString());
                                        umm.setFolder(fullName);
                                        umm.setAccountId(accountId);
                                        umm.setAccountName(name);
                                        umm.setOriginalId(accountMail.getMailId());
                                        umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                        int index = message2index.get(new MailKey(accountId, fn, accountMail.getMailId()));
                                        if (index > 0) {
                                            arrayToFill.set(index, umm);
                                        }
                                    }
                                }
                            } catch (OXException e) {
                                e.setCategory(Category.CATEGORY_WARNING);
                                access.addWarnings(Collections.singleton(e));
                                if (MailExceptionCode.ACCOUNT_DOES_NOT_EXIST.equals(e) || MimeMailExceptionCode.LOGIN_FAILED.equals(e)) {
                                    getLogger().debug("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                                } else {
                                    getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                                }
                            } catch (RuntimeException e) {
                                getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            }
                            return null;
                        }
                    });
                }
                // Wait for completion of each submitted task
                try {
                    for (int i = messages2Fetch.size(); i-- > 0;) {
                        filler.take().get();
                    }
                    LOG.debug("Fetching messages from folder \"{}\" took {}msec.", fullName, L(completionService.getDuration()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw MailExceptionCode.INTERRUPT_ERROR.create(e);
                } catch (ExecutionException e) {
                    throw ThreadPools.launderThrowable(e, OXException.class);
                }
                MailMessage[] retval = new MailMessage[arrayToFill.length()];
                for (int i = retval.length; i-- > 0;) {
                    retval[i] = arrayToFill.getPlain(i);
                }
                return retval;
            }

            // Fetch messages with all required fields
            final IndexRange applicableRange = null == indexRange ? null : new IndexRange(0, indexRange.end);
            UnifiedInboxCompletionService<List<MailMessage>> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            for (final MailAccount mailAccount : accounts.valueCollection()) {
                final MailField[] checkedFields = mailAccount.isDefaultAccount() ? checkedFieldsForPrimary : checkedFieldsForExternal;
                completionService.submit(new AbstractAccountOperation<List<MailMessage>>(session, mailAccount.getId(), access.isDebug()) {

                    @Override
                    public List<MailMessage> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
                        int accountId = mailAccount.getId();
                        String fn = null;
                        try {
                            // Get real full name
                            fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                            // Check if denoted account has such a default folder
                            if (fn == null) {
                                return Collections.emptyList();
                            }
                            // Determine sort option
                            MailSortField sortField = MailSortField.RECEIVED_DATE;
                            OrderDirection orderDir = OrderDirection.DESC;
                            if (null != applicableRange) {
                                // Apply proper sort option
                                sortField = effectiveSortField;
                                orderDir = order;
                            }
                            // Get account's messages
                            MailMessage[] accountMails = searchMessagesFor(fn, applicableRange, sortField, orderDir, searchTerm, checkedFields, headerNames, mailAccess.getMessageStorage());
                            if (null == accountMails || accountMails.length <= 0) {
                                return Collections.emptyList();
                            }
                            List<MailMessage> messages = new ArrayList<>(accountMails.length);
                            UnifiedInboxUID helper = new UnifiedInboxUID();
                            String name = mailAccount.getName();
                            for (MailMessage accountMail : accountMails) {
                                if (null != accountMail) {
                                    UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                    umm.setMailId(helper.setUID(accountId, fn, accountMail.getMailId()).toString());
                                    umm.setFolder(fullName);
                                    umm.setAccountId(accountId);
                                    umm.setAccountName(name);
                                    umm.setOriginalId(accountMail.getMailId());
                                    umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                    messages.add(umm);
                                }
                            }
                            accountMails = null; // Help GC
                            return messages;
                        } catch (OXException e) {
                            e.setCategory(Category.CATEGORY_WARNING);
                            access.addWarnings(Collections.singleton(e));
                            if (MailExceptionCode.ACCOUNT_DOES_NOT_EXIST.equals(e) || MimeMailExceptionCode.LOGIN_FAILED.equals(e)) {
                                getLogger().debug("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            } else {
                                getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            }
                            return Collections.emptyList();
                        } catch (RuntimeException e) {
                            getLogger().warn("Couldn't get messages from folder \"{}\" from server \"{}\" for login \"{}\".", (null == fn ? "<unknown>" : fn), mailAccount.getMailServer(), mailAccount.getLogin(), e);
                            return Collections.emptyList();
                        }
                    }
                });
            }
            // Wait for completion of each submitted task
            try {
                List<MailMessage> messages = new ArrayList<>(length << 2);
                MailMessageComparator mainComparator = MailMessageComparatorFactory.createComparator(effectiveSortField, order, locale, session, true);

                // Check if no index range requested
                if (indexRange == null) {
                    for (int i = length; i-- > 0;) {
                        messages.addAll(completionService.take().get());
                    }
                    Collections.sort(messages, mainComparator);
                    return messages.toArray(new MailMessage[messages.size()]);
                }

                // Add & sort results from first task
                messages.addAll(completionService.take().get());
                Collections.sort(messages, mainComparator);

                // Add & sort remaining results
                for (int i = length - 1; i-- > 0;) {
                    messages.addAll(completionService.take().get());
                    Collections.sort(messages, mainComparator);

                    // Cut off overlapping ones
                    int toIndex = indexRange.end;
                    if (messages.size() > toIndex) {
                        // Drop tail
                        messages.subList(toIndex, messages.size()).clear();
                    }
                }

                // Apply index range
                int fromIndex = indexRange.start;
                int toIndex = indexRange.end;
                if (fromIndex > messages.size()) {
                    // From index out of range
                    return EMPTY_RETVAL;
                }

                // Reset end index if out of range
                if (toIndex >= messages.size()) {
                    if (fromIndex == 0) {
                        return messages.toArray(new MailMessage[messages.size()]);
                    }
                    toIndex = messages.size();
                }
                messages = messages.subList(fromIndex, toIndex);
                return messages.toArray(new MailMessage[messages.size()]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            // Get account's messages
            final MailField[] fields;
            if (accountId == Account.DEFAULT_ID) {
                fields = fieldz;
            } else {
                MailFields mfs = new MailFields(fieldz);
                removeUnsupportedFieldsForExternalAccounts(mfs);
                fields = mfs.toArray();
            }
            MailMessage[] mails = searchMessagesFor(fa.getFullname(), indexRange, effectiveSortField, order, searchTerm, fields, headerNames, mailAccess.getMessageStorage());
            if (null == mails || mails.length <= 0) {
                return mails;
            }

            for (MailMessage mail : mails) {
                if (null != mail) {
                    mail.setFolder(fullName);
                    mail.setAccountId(unifiedMailAccountId);
                    mail.setOriginalId(mail.getMailId()); // ID stays the same
                    mail.setOriginalFolder(fa);
                }
            }
            return mails;
        } finally {
            closeSafe(mailAccess);
        }
    }

    private static MailSortField determineSortFieldForSearch(String fullName, MailSortField requestedSortField) {
        MailSortField effectiveSortField;
        if (null == requestedSortField) {
            effectiveSortField = MailSortField.RECEIVED_DATE;
        } else {
            if (MailSortField.SENT_DATE.equals(requestedSortField)) {
                String draftsFullname = UnifiedInboxAccess.DRAFTS;
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

    @Override
    public MailMessage[] getUnreadMessages(final String fullName, final MailSortField sortField, final OrderDirection order, final MailField[] fieldz, final int limit) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            List<MailAccount> accounts = getAccounts();
            int length = accounts.size();
            final int undelegatedAccountId = access.getAccountId();
            UnifiedInboxCompletionService<List<MailMessage>> completionService = new UnifiedInboxCompletionService<>(ThreadPools.getThreadPool());
            for (final MailAccount mailAccount : accounts) {
                Session session  = this.session;
                final MailField[] fields;
                if (mailAccount.isDefaultAccount()) {
                    fields = fieldz;
                } else {
                    MailFields mfs = new MailFields(fieldz);
                    removeUnsupportedFieldsForExternalAccounts(mfs);
                    fields = mfs.toArray();
                }
                completionService.submit(new AbstractAccountOperation<List<MailMessage>>(session, mailAccount, access.isDebug()) {

                    @Override
                    public List<MailMessage> doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws Exception {
                        try {
                            int accountId = mailAccount.getId();
                            // Get real full name
                            String fn = UnifiedInboxUtility.determineAccountFullName(mailAccess, fullName);
                            // Check if denoted account has such a default folder
                            if (fn == null) {
                                return Collections.emptyList();
                            }
                            // Get account's unread messages
                            MailMessage[] accountMails = mailAccess.getMessageStorage().getUnreadMessages(fn, sortField, order, fields, limit);
                            UnifiedInboxUID helper = new UnifiedInboxUID();
                            List<MailMessage> messages = new ArrayList<>(accountMails.length);
                            for (MailMessage accountMail : accountMails) {
                                if (null != accountMail) {
                                    UnifiedMailMessage umm = new UnifiedMailMessage(accountMail, undelegatedAccountId);
                                    umm.setMailId(helper.setUID(accountId, fn, accountMail.getMailId()).toString());
                                    umm.setFolder(fullName);
                                    umm.setAccountId(accountId);
                                    umm.setOriginalId(accountMail.getMailId());
                                    umm.setOriginalFolder(new FullnameArgument(accountId, fn));
                                    messages.add(umm);
                                }
                            }
                            return messages;
                        } catch (OXException e) {
                            getLogger().debug("", e);
                            return Collections.emptyList();
                        }
                    }
                });
            }
            // Wait for completion of each submitted task
            try {
                List<MailMessage> messages = new ArrayList<>(length << 2);
                for (int i = length; i-- > 0;) {
                    messages.addAll(completionService.take().get());
                }
                LOG.debug("Retrieving unread messages from folder \"{}\" took {}msec.", fullName, L(completionService.getDuration()));

                // Sort them
                Collections.sort(messages, MailMessageComparatorFactory.createComparator(sortField, order, getLocale(), session, true));

                // Return as array
                return messages.toArray(new MailMessage[messages.size()]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            } catch (ExecutionException e) {
                throw ThreadPools.launderThrowable(e, OXException.class);
            }
        }
        FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            int accountId = fa.getAccountId();
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect(true, access.isDebug());
            // Get account's messages
            final MailField[] fields;
            if (accountId == Account.DEFAULT_ID) {
                fields = fieldz;
            } else {
                MailFields mfs = new MailFields(fieldz);
                removeUnsupportedFieldsForExternalAccounts(mfs);
                fields = mfs.toArray();
            }
            MailMessage[] mails = mailAccess.getMessageStorage().getUnreadMessages(fa.getFullname(), sortField, order, fields, limit);
            int unifiedAccountId = this.access.getAccountId();
            for (MailMessage mail : mails) {
                if (null != mail) {
                    mail.setFolder(fullName);
                    mail.setAccountId(unifiedAccountId);
                    mail.setOriginalId(mail.getMailId()); // ID stays the same
                    mail.setOriginalFolder(fa);
                }
            }
            return mails;
        } finally {
                closeSafe(mailAccess);
        }
    }

    @Override
    public void deleteMessages(String fullName, String[] mailIds, final boolean hardDelete) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            // Parse mail IDs
            TIntObjectMap<Map<String, List<String>>> parsed = UnifiedInboxUtility.parseMailIDs(mailIds);
            int size = parsed.size();
            TIntObjectIterator<Map<String, List<String>>> iter = parsed.iterator();
            // Collection of Callables
            Collection<Task<Object>> collection = new ArrayList<>(size);
            for (int i = size; i-- > 0;) {
                iter.advance();
                final int accountId = iter.key();
                final Map<String, List<String>> folderUIDMap = iter.value();
                collection.add(new AbstractAccountOperation<Object>(session, accountId, access.isDebug()) {

                    @Override
                    public Object doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws Exception {
                        // Get account's mail access
                        try {
                            int innersize = folderUIDMap.size();
                            Iterator<Map.Entry<String, List<String>>> inneriter = folderUIDMap.entrySet().iterator();
                            for (int j = 0; j < innersize; j++) {
                                Map.Entry<String, List<String>> e = inneriter.next();
                                String folder = e.getKey();
                                List<String> uids = e.getValue();
                                // Delete messages
                                mailAccess.getMessageStorage().deleteMessages(folder, uids.toArray(new String[uids.size()]), hardDelete);
                            }
                        } catch (OXException e) {
                            getLogger().debug("", e);
                        }
                        return null;
                    }
                });
            }
            ThreadPoolService executor = ThreadPools.getThreadPool();
            try {
                // Invoke all and wait for being executed
                executor.invokeAll(collection);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            }
        } else {
            FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                mailAccess = MailAccess.getInstance(session, fa.getAccountId());
                mailAccess.connect(true, access.isDebug());
                mailAccess.getMessageStorage().deleteMessages(fa.getFullname(), mailIds, hardDelete);
            } finally {
                    closeSafe(mailAccess);
            }
        }
    }

    @Override
    public String[] copyMessages(String sourceFolder, String destFolder, String[] mailIds, boolean fast) throws OXException {
        return getCopier().doCopy(sourceFolder, destFolder, mailIds, fast, false);
    }

    @Override
    public String[] moveMessages(String sourceFolder, String destFolder, String[] mailIds, boolean fast) throws OXException {
        return getCopier().doCopy(sourceFolder, destFolder, mailIds, fast, true);
    }

    @Override
    public String[] appendMessages(String destFullname, MailMessage[] mailMessages) throws OXException {
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(destFullname)) {
            // TODO: Error code OR default account?!
            throw UnifiedInboxException.Code.INVALID_DESTINATION_FOLDER.create();
        }
        // Parse destination folder
        FullnameArgument destFullnameArgument = UnifiedInboxUtility.parseNestedFullName(destFullname);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(session, destFullnameArgument.getAccountId());
            mailAccess.connect(true, access.isDebug());
            return mailAccess.getMessageStorage().appendMessages(destFullnameArgument.getFullname(), mailMessages);
        } finally {
            closeSafe(mailAccess);
        }
    }

    private static final String[] EMPTY_FLAGS = Strings.getEmptyStrings();

    @Override
    public void updateMessageFlags(String fullName, String[] mailIds, final int flags, final boolean set) throws OXException {
        updateMessageFlags(fullName, mailIds, flags, EMPTY_FLAGS, set);
    }

    @Override
    public void updateMessageFlags(String fullName, String[] mailIds, final int flags, final String[] userFlags, final boolean set) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            // Parse mail IDs
            TIntObjectMap<Map<String, List<String>>> parsed = UnifiedInboxUtility.parseMailIDs(mailIds);
            int size = parsed.size();
            TIntObjectIterator<Map<String, List<String>>> iter = parsed.iterator();
            // Collection of Callables
            Collection<Task<Object>> collection = new ArrayList<>(size);
            for (int i = size; i-- > 0;) {
                iter.advance();
                final int accountId = iter.key();
                final Map<String, List<String>> folderUIDMap = iter.value();
                collection.add(new AbstractAccountOperation<Object>(session, accountId, access.isDebug()) {

                    @Override
                    public Object doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws Exception {
                        // Get account's mail access
                        try {
                            int innersize = folderUIDMap.size();
                            Iterator<Map.Entry<String, List<String>>> inneriter = folderUIDMap.entrySet().iterator();
                            for (int j = 0; j < innersize; j++) {
                                Map.Entry<String, List<String>> e = inneriter.next();
                                String folder = e.getKey();
                                List<String> uids = e.getValue();
                                // Update flags
                                mailAccess.getMessageStorage().updateMessageFlags(folder, uids.toArray(new String[uids.size()]), flags, userFlags, set);
                            }
                        } catch (OXException e) {
                            getLogger().debug("", e);
                        }
                        return null;
                    }
                });
            }
            ThreadPoolService executor = ThreadPools.getThreadPool();
            try {
                // Invoke all and wait for being executed
                executor.invokeAll(collection);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            }
        } else {
            FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                mailAccess = MailAccess.getInstance(session, fa.getAccountId());
                mailAccess.connect(true, access.isDebug());
                mailAccess.getMessageStorage().updateMessageFlags(fa.getFullname(), mailIds, flags, set);
            } finally {
                closeSafe(mailAccess);
            }
        }
    }

    @Override
    public void updateMessageUserFlags(String fullName, String[] mailIds, final String[] flags, final boolean set) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            // Parse mail IDs
            TIntObjectMap<Map<String, List<String>>> parsed = UnifiedInboxUtility.parseMailIDs(mailIds);
            int size = parsed.size();
            TIntObjectIterator<Map<String, List<String>>> iter = parsed.iterator();
            // Collection of Callables
            Collection<Task<Object>> collection = new ArrayList<>(size);
            for (int i = size; i-- > 0;) {
                iter.advance();
                final int accountId = iter.key();
                final Map<String, List<String>> folderUIDMap = iter.value();
                collection.add(new AbstractAccountOperation<Object>(session, accountId, access.isDebug()) {

                    @Override
                    public Object doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws Exception {
                        // Get account's mail access
                        try {
                            int innersize = folderUIDMap.size();
                            Iterator<Map.Entry<String, List<String>>> inneriter = folderUIDMap.entrySet().iterator();
                            for (int j = 0; j < innersize; j++) {
                                Map.Entry<String, List<String>> e = inneriter.next();
                                String folder = e.getKey();
                                List<String> uids = e.getValue();
                                // Update flags
                                mailAccess.getMessageStorage().updateMessageUserFlags(folder, uids.toArray(new String[uids.size()]), flags, set);
                            }
                        } catch (OXException e) {
                            getLogger().debug("", e);
                        }
                        return null;
                    }
                });
            }
            ThreadPoolService executor = ThreadPools.getThreadPool();
            try {
                // Invoke all and wait for being executed
                executor.invokeAll(collection);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            }
        } else {
            FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                mailAccess = MailAccess.getInstance(session, fa.getAccountId());
                mailAccess.connect(true, access.isDebug());
                mailAccess.getMessageStorage().updateMessageUserFlags(fa.getFullname(), mailIds, flags, set);
            } finally {
                closeSafe(mailAccess);
            }
        }
    }

    @Override
    public void updateMessageColorLabel(String fullName, String[] mailIds, final int colorLabel) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw UnifiedInboxException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES.create(fullName);
        }
        if (UnifiedInboxAccess.KNOWN_FOLDERS.contains(fullName)) {
            // Parse mail IDs
            TIntObjectMap<Map<String, List<String>>> parsed = UnifiedInboxUtility.parseMailIDs(mailIds);
            int size = parsed.size();
            TIntObjectIterator<Map<String, List<String>>> iter = parsed.iterator();
            // Collection of Callables
            Collection<Task<Object>> collection = new ArrayList<>(size);
            for (int i = size; i-- > 0;) {
                iter.advance();
                final int accountId = iter.key();
                final Map<String, List<String>> folderUIDMap = iter.value();
                collection.add(new AbstractAccountOperation<Object>(session, accountId, access.isDebug()) {

                    @Override
                    public Object doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws Exception {
                        // Get account's mail access
                        try {
                            int innersize = folderUIDMap.size();
                            Iterator<Map.Entry<String, List<String>>> inneriter = folderUIDMap.entrySet().iterator();
                            for (int j = 0; j < innersize; j++) {
                                Map.Entry<String, List<String>> e = inneriter.next();
                                String folder = e.getKey();
                                List<String> uids = e.getValue();
                                // Update flags
                                mailAccess.getMessageStorage().updateMessageColorLabel(folder, uids.toArray(new String[uids.size()]), colorLabel);
                            }
                        } catch (OXException e) {
                            getLogger().debug("", e);
                        }
                        return null;
                    }
                });
            }
            ThreadPoolService executor = ThreadPools.getThreadPool();
            try {
                // Invoke all and wait for being executed
                executor.invokeAll(collection);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw MailExceptionCode.INTERRUPT_ERROR.create(e);
            }
        } else {
            FullnameArgument fa = UnifiedInboxUtility.parseNestedFullName(fullName);
            MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
            try {
                mailAccess = MailAccess.getInstance(session, fa.getAccountId());
                mailAccess.connect(true, access.isDebug());
                mailAccess.getMessageStorage().updateMessageColorLabel(fa.getFullname(), mailIds, colorLabel);
            } finally {
                closeSafe(mailAccess);
            }
        }
    }

    @Override
    public MailMessage saveDraft(String draftFullName, ComposedMailMessage composedMail) throws OXException {
        throw UnifiedInboxException.Code.DRAFTS_NOT_SUPPORTED.create();
    }

    /*-
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * +++++++++++++++++ Helper methods +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     */

    private static void insertMessage(String[] mailIds, MailMessage[] toFill, int accountId, String folder, MailMessage[] mails, String uiFullname, int undelegatedAccountId) {
        UnifiedInboxUID helper = new UnifiedInboxUID();
        for (MailMessage mail : mails) {
            if (null != mail) {
                String lookFor = helper.setUID(accountId, folder, mail.getMailId()).toString();
                int pos = -1;
                for (int l = 0; l < mailIds.length && pos == -1; l++) {
                    if (lookFor.equals(mailIds[l])) {
                        pos = l;
                    }
                }
                if (pos != -1) {
                    UnifiedMailMessage umm = new UnifiedMailMessage(mail, undelegatedAccountId);
                    toFill[pos] = umm;
                    umm.setMailId(mailIds[pos]);
                    umm.setFolder(uiFullname);
                    umm.setAccountId(accountId);
                    umm.setOriginalId(mail.getMailId());
                    umm.setOriginalFolder(new FullnameArgument(accountId, folder));
                }
            }
        }
    }

    private static class GetMessagesResult {

        static final GetMessagesResult EMPTY_RESULT = new GetMessagesResult(-1, null, new MailMessage[0]);

        MailMessage[] mails;
        String folder;
        int accountId;

        public GetMessagesResult(int accountId, String folder, MailMessage[] mails) {
            super();
            this.mails = mails;
            this.folder = folder;
            this.accountId = accountId;
        }

    }

    @Override
    public MailMessage[] getMessagesByMessageIDByFolder(String fullName, String... messageIDs) throws OXException {
        if (null == messageIDs || messageIDs.length <= 0) {
            return new MailMessage[0];
        }

        throw MailExceptionCode.UNSUPPORTED_OPERATION.create();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /** A pair of account and folder full name */
    private static final class AccAndFN {

        private final MailAccount account;
        private final String fullName;
        private final int hash;

        /**
         * Initializes a new {@link AccAndFN}.
         *
         * @param account The associated mail account
         * @param fullName The folder full name
         */
        AccAndFN(MailAccount account, String fullName) {
            super();
            this.account = account;
            this.fullName = fullName;
            final int prime = 31;
            int result = 1;
            result = prime * result + account.getId();
            result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
            this.hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof AccAndFN)) {
                return false;
            }
            final AccAndFN other = (AccAndFN) obj;
            if (account.getId() != other.account.getId()) {
                return false;
            }
            if (fullName == null) {
                if (other.fullName != null) {
                    return false;
                }
            } else if (!fullName.equals(other.fullName)) {
                return false;
            }
            return true;
        }
    }

    /** The key for a mail to determine affiliation; consisting of its account identifier, folder full name and mail identifier */
    private static class MailKey {

        private final int accountId;
        private final String fullName;
        private final String mailId;
        private final int hash;

        /**
         * Initializes a new {@link MailKey} from given mail.
         *
         * @param mail The mail providing information
         */
        MailKey(MailMessage mail) {
            this(mail.getAccountId(), mail.getFolder(), mail.getMailId());
        }

        /**
         * Initializes a new {@link MailKey}.
         *
         * @param accountId The account identifier
         * @param fullName The full name of the folder
         * @param mailId The mail identifier
         */
        MailKey(int accountId, String fullName, String mailId) {
            super();
            this.accountId = accountId;
            this.fullName = fullName;
            this.mailId = mailId;
            int prime = 31;
            int result = 1;
            result = prime * result + accountId;
            result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
            result = prime * result + ((mailId == null) ? 0 : mailId.hashCode());
            this.hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MailKey other = (MailKey) obj;
            if (accountId != other.accountId) {
                return false;
            }
            if (fullName == null) {
                if (other.fullName != null) {
                    return false;
                }
            } else if (!fullName.equals(other.fullName)) {
                return false;
            }
            if (mailId == null) {
                if (other.mailId != null) {
                    return false;
                }
            } else if (!mailId.equals(other.mailId)) {
                return false;
            }
            return true;
        }
    }

}
