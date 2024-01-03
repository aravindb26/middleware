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

import static com.openexchange.imap.converters.IMAPFolderConverter.getUserFrom;
import static com.openexchange.imap.converters.IMAPFolderConverter.startsWithOneOf;
import static com.openexchange.imap.util.FolderUtility.canBeOpened;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Strings.quoteReplacement;
import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import static com.openexchange.mail.dataobjects.MailFolder.ROOT_FOLDER_ID;
import static com.openexchange.mail.dataobjects.MailFolder.ROOT_FOLDER_NAME;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.Quota.Resource;
import javax.mail.StoreClosedException;
import javax.mail.internet.idn.IDNA;
import javax.mail.search.FlagTerm;
import org.json.JSONObject;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.deputy.DeputyService;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.imap.OperationKey.Type;
import com.openexchange.imap.acl.ACLExtension;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.cache.ListLsubEntry;
import com.openexchange.imap.cache.ListLsubRuntimeException;
import com.openexchange.imap.cache.MBoxEnabledCache;
import com.openexchange.imap.cache.NamespacesCache;
import com.openexchange.imap.cache.RightsCache;
import com.openexchange.imap.cache.RootSubfoldersEnabledCache;
import com.openexchange.imap.cache.UserFlagsCache;
import com.openexchange.imap.command.CopyIMAPCommand;
import com.openexchange.imap.command.FlagsIMAPCommand;
import com.openexchange.imap.command.MoveIMAPCommand;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.config.IMAPProperties;
import com.openexchange.imap.converters.IMAPFolderConverter;
import com.openexchange.imap.dataobjects.IMAPMailFolder;
import com.openexchange.imap.entity2acl.Entity2ACL;
import com.openexchange.imap.entity2acl.Entity2ACLArgs;
import com.openexchange.imap.entity2acl.Entity2ACLArgsImpl;
import com.openexchange.imap.entity2acl.Entity2ACLExceptionCode;
import com.openexchange.imap.entity2acl.UserGroupID;
import com.openexchange.imap.entity2acl.UserInfo;
import com.openexchange.imap.namespace.Namespace;
import com.openexchange.imap.namespace.Namespaces;
import com.openexchange.imap.services.Services;
import com.openexchange.imap.util.FolderUtility;
import com.openexchange.imap.util.IMAPDeputyMetadataUtility;
import com.openexchange.imap.util.IMAPSessionStorageAccess;
import com.openexchange.java.Collators;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.log.LogProperties;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.api.IMailFolderStorageDefaultFolderAware;
import com.openexchange.mail.api.IMailFolderStorageEnhanced2;
import com.openexchange.mail.api.IMailFolderStorageInfoSupport;
import com.openexchange.mail.api.IMailFolderStorageStatusSupport;
import com.openexchange.mail.api.IMailFolderStorageTrashAware;
import com.openexchange.mail.api.IMailSharedFolderPathResolver;
import com.openexchange.mail.api.MailFolderStorage;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolder.DefaultFolderType;
import com.openexchange.mail.dataobjects.MailFolderDescription;
import com.openexchange.mail.dataobjects.MailFolderInfo;
import com.openexchange.mail.dataobjects.MailFolderStatus;
import com.openexchange.mail.mime.FolderCreationFailedException;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.permission.DefaultMailPermission;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.MailAccounts;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.behavior.CallerRunsBehavior;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import com.openexchange.user.UserExceptionCode;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.ResponseCode;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.DefaultFolder;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;
import com.sun.mail.imap.protocol.ListInfo;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TCharHashSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link IMAPFolderStorage} - The IMAP folder storage implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPFolderStorage extends MailFolderStorage implements IMailFolderStorageEnhanced2, IMailFolderStorageInfoSupport, IMailFolderStorageDefaultFolderAware, IMailSharedFolderPathResolver, IMailFolderStorageStatusSupport, IMailFolderStorageTrashAware {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IMAPFolderStorage.class);

    private static final String STR_INBOX = "INBOX";

    private final IMAPStore imapStore;
    private final IMAPAccess imapAccess;
    private final int accountId;
    private final Session session;
    private final Context ctx;
    private final IMAPConfig imapConfig;
    protected final boolean examineHasAttachmentUserFlags;

    private Character separator;
    private IMAPDefaultFolderChecker checker;

    /**
     * Initializes a new {@link IMAPFolderStorage}
     *
     * @param imapStore The IMAP store
     * @param imapAccess The IMAP access
     * @param session The session providing needed user data
     * @throws OXException If context loading fails
     */
    public IMAPFolderStorage(IMAPStore imapStore, IMAPAccess imapAccess, Session session) throws OXException {
        super();
        this.imapStore = imapStore;
        this.imapAccess = imapAccess;
        accountId = imapAccess.getAccountId();
        this.session = session;
        ctx = session instanceof ServerSession ? ((ServerSession) session).getContext() : ContextStorage.getStorageContext(session.getContextId());
        imapConfig = imapAccess.getIMAPConfig();
        examineHasAttachmentUserFlags = imapConfig.getCapabilities().hasAttachmentMarker();
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
     * Gets the associated session.
     *
     * @return The session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the associated context.
     *
     * @return The context
     */
    public Context getContext() {
        return ctx;
    }

    /**
     * Gets the associated IMAP configuration.
     *
     * @return The IMAP configuration
     */
    public IMAPConfig getImapConfig() {
        return imapConfig;
    }

    /**
     * Gets the IMAP access.
     *
     * @return The IMAP access
     */
    public IMAPAccess getImapAccess() {
        return imapAccess;
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
     * Gets the associated account identifier.
     *
     * @return The account identifier
     */
    public int getAccountId() {
        return accountId;
    }

    private IMAPDefaultFolderChecker getChecker() {
        if (null == checker) {
            Map<String, String> caps = imapConfig.asMap();
            boolean hasMetadata = caps.containsKey(IMAPCapabilities.CAP_METADATA); // http://tools.ietf.org/html/rfc5464#section-1
            if (caps.containsKey("SPECIAL-USE")) {
                // Supports SPECIAL-USE capability
                boolean hasCreateSpecialUse = caps.containsKey("CREATE-SPECIAL-USE"); // http://tools.ietf.org/html/rfc6154#section-3
                checker = new SpecialUseDefaultFolderChecker(accountId, session, ctx, imapStore, imapAccess, hasCreateSpecialUse, hasMetadata);
            } else {
                checker = new IMAPDefaultFolderChecker(accountId, session, ctx, imapStore, imapAccess, hasMetadata);
            }
        }
        return checker;
    }

    private char getSeparator() throws MessagingException {
        if (null == separator) {
            try {
                separator = Character.valueOf(ListLsubCache.getSeparator(accountId, (DefaultFolder) imapStore.getDefaultFolder(), session, imapConfig.getIMAPProperties()));
            } catch (OXException e) {
                Throwable cause = e.getCause();
                if (cause instanceof MessagingException) {
                    throw (MessagingException) cause;
                }
                throw (cause instanceof Exception) ? new MessagingException(cause.getMessage(), (Exception) cause) : new MessagingException(e.getMessage(), e);
            }
        }
        return separator.charValue();
    }

    private char setAndGetSeparator(char sep) {
        if (null == separator) {
            separator = Character.valueOf(sep);
        }
        return separator.charValue();
    }

    @Override
    public boolean isStatusSupported() throws OXException {
    	return true;
    }

    @Override
    public MailFolderStatus getFolderStatus(String fullName) throws OXException {
    	try {
            final String fn = (ROOT_FOLDER_ID.equals(fullName) ? "" : fullName);
            // Retrieve folder...
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder f;
            if (0 == fn.length()) {
                f = (IMAPFolder) imapStore.getDefaultFolder();
            } else {
                f = (IMAPFolder) imapStore.getFolder(fullName);
            }

            // ... and check existence
            boolean exists = f.exists();
            if (!exists) {

                try {
                    f.open(Folder.READ_ONLY);
                    exists = true;
                } catch (@SuppressWarnings("unused") javax.mail.FolderNotFoundException e) {
                    exists = false;
                } finally {
                    if (exists) {
                        f.close(false);
                    }
                }

                if (!exists) {
                    f = checkForNamespaceFolder(fn);
                    if (null == f) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                }
            }

            return MailFolderStatus.builder()
                .nextId(Long.toString(f.getUIDNext()))
                .total(f.getMessageCount())
                .unread(f.getUnreadMessageCount())
                .validity(Long.toString(f.getUIDValidity()))
                .build();
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public boolean isResolvingSharedFolderPathSupported(String folder) {
        return imapConfig.getCapabilities().hasPermissions();
    }

    @Override
    public String resolveSharedFolderPath(String fullName, int targetUserId) throws OXException {
        if (!imapConfig.getCapabilities().hasPermissions()) {
            return fullName;
        }

        if (ROOT_FOLDER_ID.equals(fullName)) {
            throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
        }

        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder imapFolder = getIMAPFolder(fullName);
            if (!doesExist(imapFolder, true)) {
                imapFolder = checkForNamespaceFolder(fullName);
                if (null == imapFolder) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }

            // Check for shared namespace
            Namespaces namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
            char separator = getSeparator();
            List<Namespace> sharedNamespaces = namespaces.getShared();
            for (Namespace sharedNamespace : sharedNamespaces) {
                if (Strings.isNotEmpty(sharedNamespace.getPrefix()) && (fullName.equals(sharedNamespace.getFullName()) || fullName.startsWith(sharedNamespace.getPrefix()))) {
                    // Full name is in shared namespace. No adjusting needed.
                    return fullName;
                }
            }

            // Check for user namespace
            List<Namespace> userNamespaces = namespaces.getOtherUsers();
            if (userNamespaces.isEmpty()) {
                throw MailExceptionCode.UNEXPECTED_ERROR.create("IMAP account has no user namespace");
            }
            Namespace namespace = userNamespaces.get(0);
            if (Strings.isEmpty(namespace.getPrefix())) {
                throw MailExceptionCode.UNEXPECTED_ERROR.create("IMAP account has no user namespace");
            }

            // Determines session user's ACL name
            Entity2ACLArgs entity2AclArgs = IMAPFolderConverter.getEntity2AclArgs(targetUserId, session, imapFolder, imapConfig);
            String aclName = Entity2ACL.getInstance(imapStore, imapConfig).getACLName(session.getUserId(), ctx, entity2AclArgs);

            // Check whether shared INBOX should be visible as "shared/user" or "shared/user/INBOX"
            boolean includeSharedInboxExplicitly = IMAPProperties.getInstance().includeSharedInboxExplicitly(session.getUserId(), session.getContextId());

            // Compose the full name from target user point of view
            StringBuilder fullNameBuilder = new StringBuilder(namespace.getPrefix()).append(aclName);
            if (STR_INBOX.equals(fullName)) {
                if (includeSharedInboxExplicitly) {
                    fullNameBuilder.append(separator).append(STR_INBOX);
                }
                return fullNameBuilder.toString();
            }

            String appendix = fullName;
            if (!includeSharedInboxExplicitly) {
                String inboxPrefix = new StringBuilder(STR_INBOX).append(separator).toString();
                if (appendix.startsWith(inboxPrefix)) {
                    appendix = appendix.substring(inboxPrefix.length());
                }
            }

            return fullNameBuilder.append(separator).append(appendix).toString();
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (IMAPException e) {
            throw e;
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public boolean isInfoSupported() throws OXException {
        return true;
    }

    @Override
    public MailFolderInfo getFolderInfo(String fullName) throws OXException {
        try {
            final String fn = (ROOT_FOLDER_ID.equals(fullName) ? "" : fullName);
            final ListLsubEntry entry = ListLsubCache.getCachedLISTEntry(fn, accountId, imapStore, session, imapConfig.getIMAPProperties());
            if (entry.exists()) {
                return toFolderInfo(entry);
            }

            // Retrieve folder...
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder f;
            if (0 == fn.length()) {
                f = (IMAPFolder) imapStore.getDefaultFolder();
            } else {
                f = (IMAPFolder) imapStore.getFolder(fullName);
            }

            // ... and check existence
            ListLsubEntry listEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null != listEntry && listEntry.exists()) {
                return toFolderInfo(listEntry);
            }

            ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, f);
            if (null == listInfo && !canBeOpened(f)) {
                f = checkForNamespaceFolder(fn);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }

            return IMAPFolderConverter.convertFolder(f, session, imapAccess, ctx).asMailFolderInfo(accountId);
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public List<MailFolderInfo> getAllFolderInfos(boolean subscribedOnly) throws OXException {
        return getFolderInfos(null, subscribedOnly);
    }

    @Override
    public List<MailFolderInfo> getFolderInfos(String optParentFullName, boolean subscribedOnly) throws OXException {
        try {
            String pfn = null;
            if (null != optParentFullName) {
                pfn = ROOT_FOLDER_ID.equals(optParentFullName) ? "" : optParentFullName;
            }
            final List<ListLsubEntry> allEntries = ListLsubCache.getAllEntries(pfn, accountId, subscribedOnly, imapStore, session, imapConfig.getIMAPProperties());

            // User's locale
            final Locale locale = (session instanceof ServerSession ? ((ServerSession) session).getUser() : UserStorage.getInstance().getUser(session.getUserId(), session.getContextId())).getLocale();

            // Check whether to consider standard folders
            if (!considerStandardFolders(pfn)) {
                final List<MailFolderInfo> retval = new ArrayList<>(allEntries.size());

                // Fill list
                for (ListLsubEntry entry : allEntries) {
                    final MailFolderInfo mfi = toFolderInfo(entry);
                    retval.add(mfi);
                }

                // Sort & return
                Collections.sort(retval, new FullDisplayNameComparator(locale));
                return retval;
            }

            // ------------------------------------------------------------------------------------------------------ //
            // Fill map
            final int size = allEntries.size();
            final Map<String, MailFolderInfo> map = new HashMap<>(size);
            for (ListLsubEntry entry : allEntries) {
                final MailFolderInfo mfi = toFolderInfo(entry);
                map.put(mfi.getFullname(), mfi);
            }

            // Determine standard folders
            {
                StringHelper stringHelper = StringHelper.valueOf(locale);

                {
                    MailFolderInfo mfi = map.get("INBOX");
                    if (null != mfi) {
                        mfi.setDefaultFolder(true);
                        mfi.setDefaultFolderType(DefaultFolderType.INBOX);
                        mfi.setDisplayName(stringHelper.getString(MailStrings.INBOX));
                    }
                }

                int len;
                {
                    // Detect if spam option is enabled
                    UserSettingMail usm = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx);
                    if (usm == null) {
                        throw IMAPException.IMAPCode.UNEXPECTED_ERROR.create("Unable to load mail settings.");
                    }
                    len = usm.isSpamOptionEnabled() ? 6 : 4;
                }
                for (int index = 0; index < len; index++) {
                    String fn;
                    try {
                        fn = getChecker().getDefaultFolder(index);
                    } catch (Exception e) {
                        LOG.warn("Failed to get standard folder full name for {}", IMAPDefaultFolderChecker.getFallbackName(index), e);
                        fn = null;
                    }
                    if (null != fn) {
                        MailFolderInfo mfi = map.get(fn);
                        if (null != mfi) {
                            mfi.setDefaultFolder(true);

                            switch (index) {
                            case StorageUtility.INDEX_CONFIRMED_HAM:
                                mfi.setDefaultFolderType(DefaultFolderType.CONFIRMED_HAM);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.CONFIRMED_HAM));
                                break;
                            case StorageUtility.INDEX_CONFIRMED_SPAM:
                                mfi.setDefaultFolderType(DefaultFolderType.CONFIRMED_SPAM);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.CONFIRMED_SPAM));
                                break;
                            case StorageUtility.INDEX_DRAFTS:
                                mfi.setDefaultFolderType(DefaultFolderType.DRAFTS);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.DRAFTS));
                                break;
                            case StorageUtility.INDEX_SENT:
                                mfi.setDefaultFolderType(DefaultFolderType.SENT);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.SENT));
                                break;
                            case StorageUtility.INDEX_SPAM:
                                mfi.setDefaultFolderType(DefaultFolderType.SPAM);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.SPAM));
                                break;
                            case StorageUtility.INDEX_TRASH:
                                mfi.setDefaultFolderType(DefaultFolderType.TRASH);
                                mfi.setDisplayName(stringHelper.getString(MailStrings.TRASH));
                                break;
                            default:
                                break;
                            }

                        }
                    }
                }
            }

            // Sort & return
            final List<MailFolderInfo> retval = new ArrayList<>(map.values());
            Collections.sort(retval, new FullDisplayNameComparator(locale));
            return retval;
        } catch (MessagingException e) {
            throw handleMessagingException(e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private boolean considerStandardFolders(String parentFullName) throws OXException {
        final boolean considerStandardFolders;
        if (null == parentFullName) {
            considerStandardFolders = true;
        } else {
            final String prefix = getDefaultFolderPrefix();
            final int pLength = prefix.length();
            considerStandardFolders = 0 == pLength ? 0 == parentFullName.length() : (parentFullName.equals(prefix.substring(0, pLength - 1)));
        }
        return considerStandardFolders;
    }

    private MailFolderInfo toFolderInfo(ListLsubEntry entry) throws MessagingException {
        final MailFolderInfo mfi = new MailFolderInfo();
        mfi.setAccountId(accountId);
        char sep = entry.getSeparator();
        mfi.setSeparator(sep);
        final String fullName = entry.getFullName();

        mfi.setName(entry.getName());
        mfi.setHoldsFolders(entry.hasInferiors());
        mfi.setHoldsMessages(entry.canOpen());
        mfi.setSubscribed(true);

        if (entry.hasChildren()) {
            mfi.setSubfolders(true);
            mfi.setNumSubfolders(entry.getChildren().size());
        } else {
            mfi.setSubfolders(false);
            mfi.setNumSubfolders(0);
        }

        mfi.setSubscribedSubfolders(entry.hasChildren());

        if (0 == fullName.length()) {
            mfi.setRootFolder(true);
            mfi.setParentFullname(null);
            mfi.setFullname(ROOT_FOLDER_ID);
        } else {
            mfi.setDefaultFolder(false);
            mfi.setDefaultFolderType(DefaultFolderType.NONE);
            if (entry.getParent() == null) {
                mfi.setParentFullname(null);
            } else {
                String pf = entry.getParent().getFullName();
                mfi.setParentFullname(0 == pf.length() ? ROOT_FOLDER_ID : pf);
            }
            mfi.setFullname(fullName);
        }

        if (imapConfig.getImapCapabilities().hasNamespace()) {
            List<Namespace> userNamespaces = NamespacesCache.getUserNamespaces(imapStore, true, session, accountId);
            boolean shared = false;
            for (int i = 0; !shared && i < userNamespaces.size(); i++) {
                Namespace userNamespace = userNamespaces.get(i);
                if (Strings.isNotEmpty(userNamespace.getPrefix())) {
                    if (fullName.equals(userNamespace.getFullName())) {
                        Optional<User> optUser = getUserFrom(session);
                        if (optUser.isPresent()) {
                            mfi.setName(StringHelper.valueOf(optUser.get().getLocale()).getString(FolderStrings.SYSTEM_SHARED_FOLDER_NAME));
                        }
                        shared = true;
                    } else {
                        String prefix = userNamespace.getPrefix();
                        if (fullName.startsWith(prefix)) {
                            shared = true;
                            if (fullName.indexOf(sep, prefix.length()) < 0) {
                                // E.g. "shared/berta@context1.ox.test"
                                String owner = fullName.substring(prefix.length());
                                Optional<UserInfo> optUserInfo = Entity2ACL.getUserInfoFor(owner, fullName, getSeparator(), imapAccess, true);
                                if (optUserInfo.isPresent()) {
                                    Optional<String> optionalDisplayName = optUserInfo.get().getOptionalDisplayName();
                                    if (optionalDisplayName.isPresent()) {
                                        mfi.setName(optionalDisplayName.get());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mfi.setShared(shared);

            boolean isPublic = false;
            if (!shared) {
                List<Namespace> sharedNamespaces = NamespacesCache.getSharedNamespaces(imapStore, true, session, accountId);
                List<Namespace> personalNamespaces = NamespacesCache.getPersonalNamespaces(imapStore, true, session, accountId);
                for (int i = 0; !isPublic && i < sharedNamespaces.size(); i++) {
                    Namespace sharedNamespace = sharedNamespaces.get(i);
                    if (Strings.isNotEmpty(sharedNamespace.getPrefix())) {
                        if (fullName.equals(sharedNamespace.getFullName())) {
                            Optional<User> optUser = getUserFrom(session);
                            if (optUser.isPresent()) {
                                mfi.setName(StringHelper.valueOf(optUser.get().getLocale()).getString(FolderStrings.SYSTEM_PUBLIC_FOLDER_NAME));
                            }
                            isPublic = true;
                        } else {
                            String prefix = sharedNamespace.getPrefix();
                            if (fullName.startsWith(prefix)) {
                                isPublic = true;
                            }
                        }
                    } else if (!startsWithOneOf(fullName, personalNamespaces, userNamespaces)) {
                        isPublic = true;
                    }
                }
            }
            mfi.setPublic(isPublic);
        } else {
            mfi.setShared(false);
            mfi.setPublic(false);
        }

        return mfi;
    }

    @Override
    public int[] getTotalAndUnreadCounter(String fullName) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            return new int[] { 0, 0 };
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            boolean ignoreDeletedMailsForFolderCount = IMAPProperties.getInstance().isIgnoreDeletedMails(this.session.getUserId(), this.session.getContextId());
            ListLsubEntry listEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null != listEntry && listEntry.exists()) {
                try {
                    return listEntry.canOpen() ? IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, ignoreDeletedMailsForFolderCount) : new int[] { 0, 0 };
                } catch (@SuppressWarnings("unused") MessagingException e) {
                    return new int[] { 0, 0 };
                }
            }

            ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, (IMAPFolder) imapStore.getDefaultFolder());
            if (null == listInfo) {
                IMAPFolder f = getIMAPFolder(fullName);
                if (canBeOpened(f)) {
                    return IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, ignoreDeletedMailsForFolderCount);
                }

                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
                return new int[] { 0, 0 };
            }

            try {
                return listInfo.canOpen ? IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, ignoreDeletedMailsForFolderCount) : new int[] { 0, 0 };
            } catch (@SuppressWarnings("unused") MessagingException e) {
                return new int[] { 0, 0 };
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public int getUnreadCounter(String fullName) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            return 0;
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            boolean ignoreDeletedMailsForFolderCount = IMAPProperties.getInstance().isIgnoreDeletedMails(this.session.getUserId(), this.session.getContextId());

            ListLsubEntry listEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null != listEntry && listEntry.exists()) {
                try {
                    return listEntry.canOpen() ? IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, ignoreDeletedMailsForFolderCount)[1] : 0;
                } catch (@SuppressWarnings("unused") MessagingException e) {
                    return 0;
                }
            }

            ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, (IMAPFolder) imapStore.getDefaultFolder());
            if (null == listInfo) {
                IMAPFolder f = getIMAPFolder(fullName);
                if (canBeOpened(f)) {
                    return IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, ignoreDeletedMailsForFolderCount)[1];
                }

                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
                return 0;
            }

            try {
                return listInfo.canOpen ? IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, ignoreDeletedMailsForFolderCount)[1] : 0;
            } catch (@SuppressWarnings("unused") MessagingException e) {
                return 0;
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public int getNewCounter(String fullName) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            return 0;
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);

            ListLsubEntry listEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null != listEntry && listEntry.exists()) {
                try {
                    return listEntry.canOpen() ? IMAPCommandsCollection.getRecent(imapStore, fullName) : 0;
                } catch (@SuppressWarnings("unused") MessagingException e) {
                    return 0;
                }
            }

            ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, (IMAPFolder) imapStore.getDefaultFolder());
            if (null == listInfo) {
                IMAPFolder f = getIMAPFolder(fullName);
                if (canBeOpened(f)) {
                    return IMAPCommandsCollection.getRecent(imapStore, fullName);
                }

                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
                return 0;
            }

            try {
                return listInfo.canOpen ? IMAPCommandsCollection.getRecent(imapStore, fullName) : 0;
            } catch (@SuppressWarnings("unused") MessagingException e) {
                return 0;
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public int getTotalCounter(String fullName) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            return 0;
        }
        try {
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            ListLsubEntry listEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null != listEntry && listEntry.exists()) {
                try {
                    return listEntry.canOpen() ? IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, false)[0] : 0;
                } catch (@SuppressWarnings("unused") MessagingException e) {
                    return 0;
                }
            }

            ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, (IMAPFolder) imapStore.getDefaultFolder());
            if (null == listInfo) {
                IMAPFolder f = getIMAPFolder(fullName);
                if (canBeOpened(f)) {
                    return IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, false)[0];
                }

                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
                return 0;
            }

            try {
                return listInfo.canOpen ? IMAPCommandsCollection.getTotalAndUnread(imapStore, fullName, false)[0] : 0;
            } catch (@SuppressWarnings("unused") MessagingException e) {
                return 0;
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public boolean exists(String fullName) throws OXException {
        try {
            if (ROOT_FOLDER_ID.equals(fullName) || STR_INBOX.equals(fullName)) {
                return true;
            }

            ListLsubEntry listEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null != listEntry && listEntry.exists()) {
                return true;
            }

            if (null != IMAPCommandsCollection.getListInfo(fullName, (IMAPFolder) imapStore.getDefaultFolder()) || canBeOpened(getIMAPFolder(fullName))) {
                return true;
            }

            if (checkForNamespaceFolder(fullName) != null) { // NOSONARLINT
                return true;
            }

            return false;
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailFolder getFolder(String fullName) throws OXException {
        return FolderUtility.loadFolder(fullName, this);
    }

    /**
     * Gets the personal namespace folder.
     *
     * @return The personal namespace folder or <code>null</code>
     * @throws OXException If operation fails
     */
    public String getPersonalNamespace() throws OXException {
        try {
            if (!imapConfig.getImapCapabilities().hasNamespace()) {
                return null;
            }

            List<Namespace> personalNamespaces = NamespacesCache.getPersonalNamespaces(imapStore, true, session, accountId);
            return personalNamespaces.isEmpty() ? null : personalNamespaces.get(0).getFullName();
        } catch (MessagingException e) {
            throw handleMessagingException(e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public MailFolder[] getSubfolders(String parentFullName, boolean all) throws OXException {
        try {
            IMAPFolderWorker.checkFailFast(imapStore, parentFullName);
            if (ROOT_FOLDER_ID.equals(parentFullName)) {
                final IMAPFolder parent = (IMAPFolder) imapStore.getDefaultFolder();
                final boolean subscribed = (!getImapConfig().getIMAPProperties().isIgnoreSubscription() && !all);
                /*
                 * Request subfolders the usual way
                 */
                final List<ListLsubEntry> subfolders;
                /*
                 * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
                 */
                final List<String> additionalFullNames = new ArrayList<>(4);
                synchronized (parent) {
                    subfolders = new ArrayList<>();
                    {
                        List<ListLsubEntry> children;
                        if (subscribed) {
                            children = getLSUBEntry("", parent).getChildren();
                        } else {
                            children = getLISTEntry("", parent).getChildren();
                        }
                        subfolders.addAll(children);
                        boolean containsInbox = false;
                        for (int i = 0; i < children.size() && !containsInbox; i++) {
                            containsInbox = STR_INBOX.equals(children.get(i).getFullName());
                        }
                        if (!containsInbox) {
                            /*
                             * Add folder INBOX manually
                             */
                            subfolders.add(0, getLISTEntry(STR_INBOX, parent));
                        }
                    }
                    if (imapConfig.getImapCapabilities().hasNamespace()) {
                        /*
                         * Merge with namespace folders
                         */
                        Namespaces namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
                        List<Namespace> personalNamespaces = namespaces.getPersonal();
                        if (null == personalNamespaces || 1 != personalNamespaces.size() || !STR_INBOX.equals(personalNamespaces.get(0).getFullName())) {
                            /*
                             * Personal namespace(s) does not only consist of INBOX folder
                             */
                            mergeWithNamespaceFolders(subfolders, personalNamespaces, subscribed, parent, additionalFullNames);
                        }
                        mergeWithNamespaceFolders(subfolders, namespaces.getOtherUsers(), subscribed, parent, additionalFullNames);
                        mergeWithNamespaceFolders(subfolders, namespaces.getShared(), subscribed, parent, additionalFullNames);
                    }
                }
                /*
                 * Output subfolders
                 */
                final List<MailFolder> list = new ArrayList<>(subfolders.size() + additionalFullNames.size());
                for (ListLsubEntry subfolder : subfolders) {
                    final String subfolderFullName = subfolder.getFullName();
                    try {
                        list.add(FolderUtility.loadFolder(subfolderFullName, this));
                    } catch (OXException e) {
                        if (MimeMailExceptionCode.FOLDER_NOT_FOUND.getNumber() != e.getCode()) {
                            throw e;
                        }
                        /*
                         * Obviously folder does (no more) exist
                         */
                        ListLsubCache.removeCachedEntry(subfolderFullName, accountId, session);
                        RightsCache.removeCachedRights(subfolderFullName, session, accountId);
                        UserFlagsCache.removeUserFlags(subfolderFullName, session, accountId);
                    }
                }
                for (String fn : additionalFullNames) {
                    final MailFolder namespaceFolder = namespaceFolderFor(fn, parent, subscribed);
                    if (null != namespaceFolder) {
                        list.add(namespaceFolder);
                    }
                }
                return list.toArray(new MailFolder[list.size()]);
            }
            IMAPFolder parent = getIMAPFolder(parentFullName);
            final ListLsubEntry parentEntry = getLISTEntry(parentFullName, parent);
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            if (doesExist(parentEntry)) {
                /*
                 * Holds LOOK-UP right?
                 */
                if (imapConfig.isSupportsACLs() && parentEntry.canOpen()) {
                    try {
                        if (!imapConfig.getACLExtension().canLookUp(RightsCache.getCachedRights(parent, true, session, accountId))) {
                            throw IMAPException.create(IMAPException.Code.NO_LOOKUP_ACCESS, imapConfig, session, parentFullName);
                        }
                    } catch (MessagingException e) {
                        if (!startsWithNamespaceFolder(parentFullName)) {
                            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, parentFullName);
                        }
                    }
                }
                return getSubfolderArray(all, parent);
            }
            /*
             * Check for namespace folder
             */
            parent = checkForNamespaceFolder(parentFullName);
            if (null != parent) {
                return getSubfolderArray(all, parent);
            }
            return EMPTY_PATH;
        } catch (MessagingException e) {
            throw handleMessagingException(parentFullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private MailFolder namespaceFolderFor(String fn, IMAPFolder parent, boolean subscribed) throws OXException, MessagingException {
        final ListLsubEntry listEntry = getLISTEntry(fn, parent);
        if (null == listEntry || !listEntry.exists() || (subscribed && !listEntry.isSubscribed())) {
            return null;
        }
        final IMAPMailFolder mailFolder = new IMAPMailFolder();
        mailFolder.setRootFolder(false);
        mailFolder.setExists(true);
        mailFolder.setSeparator(listEntry.getSeparator());
        mailFolder.setFullname(fn);
        mailFolder.setName(listEntry.getName());
        // TODO: Set proper user display name
        mailFolder.setHoldsMessages(listEntry.canOpen());
        mailFolder.setHoldsFolders(listEntry.hasInferiors());
        mailFolder.setNonExistent(false);
        mailFolder.setShared(true);
        mailFolder.setSubfolders(listEntry.hasChildren());
        mailFolder.setSubscribed(listEntry.isSubscribed());
        mailFolder.setSubscribedSubfolders(ListLsubCache.hasAnySubscribedSubfolder(fn, accountId, parent, session, imapConfig.getIMAPProperties()));
        mailFolder.setParentFullname(null);
        mailFolder.setDefaultFolder(false);
        mailFolder.setDefaultFolderType(DefaultFolderType.NONE);
        {
            final DefaultMailPermission perm = new DefaultMailPermission();
            perm.setAllPermission(0,0,0,0);
            perm.setFolderAdmin(false);
            perm.setEntity(session.getUserId());
            perm.setGroupPermission(false);
            mailFolder.setOwnPermission(perm);
            mailFolder.addPermission(perm);
        }
        mailFolder.setMessageCount(-1);
        mailFolder.setNewMessageCount(-1);
        mailFolder.setUnreadMessageCount(-1);
        mailFolder.setDeletedMessageCount(-1);
        mailFolder.setSupportsUserFlags(false);
        return mailFolder;
    }

    private MailFolder[] getSubfolderArray(boolean all, IMAPFolder parent) throws MessagingException, OXException {
        final boolean subscribed = !getImapConfig().getIMAPProperties().isIgnoreSubscription() && !all;
        final List<ListLsubEntry> subfolders;
        {
            ListLsubEntry entry = subscribed ? getLSUBEntry(parent) : getLISTEntry(parent);
            subfolders = new ArrayList<>(entry.getChildren());
        }
        /*
         * Merge with namespace folders if NAMESPACE capability is present
         */
        final List<String> additionalFullNames = new ArrayList<>(4);
        if (imapConfig.getImapCapabilities().hasNamespace()) {
            Namespaces namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
            mergeWithNamespaceFolders(subfolders, namespaces.getPersonal(), subscribed, parent, additionalFullNames);
            mergeWithNamespaceFolders(subfolders, namespaces.getOtherUsers(), subscribed, parent, additionalFullNames);
            mergeWithNamespaceFolders(subfolders, namespaces.getShared(), subscribed, parent, additionalFullNames);
        }
        /*
         * Convert to MailFolder instances
         */
        final List<MailFolder> list = new ArrayList<>(subfolders.size() + (additionalFullNames.isEmpty() ? 0 : additionalFullNames.size()));
        for (ListLsubEntry current : subfolders) {
            final MailFolder mailFolder = FolderUtility.loadFolder(current.getFullName(), this);
            if (mailFolder.exists()) {
                list.add(mailFolder);
            }
        }
        if (!additionalFullNames.isEmpty()) {
            for (String fn : additionalFullNames) {
                list.add(FolderUtility.loadFolder(fn, this));
            }
        }
        return list.toArray(new MailFolder[list.size()]);
    }

    private static void mergeWithNamespaceFolders(List<ListLsubEntry> subfolders, List<Namespace> namespaces, boolean subscribed, IMAPFolder parent, List<String> additionalFullNames) throws MessagingException {
        int numOfNs = namespaces.size();
        if (numOfNs <= 0) {
            return;
        }

        Namespace[] validOnes = namespaces.toArray(new Namespace[numOfNs]);
        String parentFullname = parent.getFullName();
        Set<String> subfolderFullNames = subfolders.stream().map(ListLsubEntry::getFullName).collect(Collectors.toSet());

        for (int i = 0; i < numOfNs; i++) {
            validOnes[i] = examineNamespaceFullName(validOnes[i], parentFullname, subfolderFullNames);
        }

        if (subscribed) {
            /*
             * Remove not-subscribed namespace folders
             */
            for (int i = 0; i < validOnes.length; i++) {
                Namespace ns = validOnes[i];
                if (ns != null && !IMAPCommandsCollection.isSubscribed(ns.getFullName(), ns.getDelimiter(), true, parent)) {
                    validOnes[i] = null;
                }
            }
        }
        /*
         * Add remaining namespace folders to subfolder list
         */
        for (Namespace ns : validOnes) {
            if (ns != null) {
                additionalFullNames.add(ns.getFullName());
            }
        }
    }

    private static Namespace examineNamespaceFullName(Namespace ns, String parentFullname, Set<String> subfolderFullNames) {
        String nsFullName = ns.getFullName();
        if ((nsFullName == null) || (nsFullName.length() == 0)) {
            // Invalid full name
            return null;
        }

        // Check if namespace folder's prefix matches parent full name ; e.g "INBOX" or "INBOX/#shared"
        int pos = nsFullName.lastIndexOf(ns.getDelimiter());
        if (pos > 0) {
            // Located below other folder than root
            if (!nsFullName.substring(0, pos).equals(parentFullname)) {
                return null;
            }
        } else if (0 != parentFullname.length()) { // Should be located below root
            return null;
        }

        // Check if already contained in passed list
        if (subfolderFullNames.contains(nsFullName)) {
            // Namespace folder already contained in subfolder list
            return null;
        }
        return ns;
    }

    /**
     * Checks if given full name matches a namespace folder
     *
     * @param fullName The folder's full name
     * @return The corresponding namespace folder or <code>null</code>
     * @throws MessagingException If operation fails
     */
    public IMAPFolder checkForNamespaceFolder(String fullName) throws MessagingException {
        return checkForNamespaceFolder0(fullName, null);
    }

    /**
     * Checks if given full name matches a namespace folder or one of its subfolders.
     *
     * @param fullName The folder's full name
     * @param folder The folder associated with the full name
     * @return The corresponding namespace folder or <code>null</code>
     * @throws MessagingException If operation fails
     */
    public IMAPFolder checkForNamespaceFolder(String fullName, IMAPFolder folder) throws MessagingException {
        if (null == folder) {
            throw new MessagingException("IMAP folder must not be null");
        }
        return checkForNamespaceFolder0(fullName, folder);
    }

    private IMAPFolder checkForNamespaceFolder0(String fullName, IMAPFolder folder) throws MessagingException {
        if (NamespacesCache.containedInPersonalNamespaces(fullName, imapStore, true, session, accountId)) {
            return new NamespaceFolder(imapStore, fullName, getSeparator());
        }
        if (NamespacesCache.containedInUserNamespaces(fullName, imapStore, true, session, accountId)) {
            return new NamespaceFolder(imapStore, fullName, getSeparator());
        }
        if (NamespacesCache.containedInSharedNamespaces(fullName, imapStore, true, session, accountId)) {
            return new NamespaceFolder(imapStore, fullName, getSeparator());
        }
        if (null == folder) {
            return null;
        }
        return startsWithNamespaceFolder(fullName) ? folder : null;
    }

    @Override
    public MailFolder getRootFolder() throws OXException {
        return FolderUtility.loadFolder(MailFolder.ROOT_FOLDER_ID, this);
    }

    @Override
    public void checkDefaultFolders() throws OXException {
        getChecker().checkDefaultFolders();
    }

    @Override
    public Map<String, String> getSpecialUseFolder() throws OXException {
        try {
            Map<String, String> retval = new HashMap<>(5);
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(IMAPDefaultFolderChecker.INBOX);
            Collection<ListLsubEntry> entries = null;
            entries = ListLsubCache.getSentEntry(-1, imapFolder, session, imapConfig.getIMAPProperties());
            if (!entries.isEmpty()) {
                retval.put(SENT, entries.toArray(new ListLsubEntry[entries.size()])[0].getFullName());
            }
            entries = ListLsubCache.getDraftsEntry(-1, imapFolder, session, imapConfig.getIMAPProperties());
            if (!entries.isEmpty()) {
                retval.put(DRAFTS, entries.toArray(new ListLsubEntry[entries.size()])[0].getFullName());
            }
            entries = ListLsubCache.getJunkEntry(-1, imapFolder, session, imapConfig.getIMAPProperties());
            if (!entries.isEmpty()) {
                retval.put(SPAM, entries.toArray(new ListLsubEntry[entries.size()])[0].getFullName());
            }
            entries = ListLsubCache.getTrashEntry(-1, imapFolder, session, imapConfig.getIMAPProperties());
            if (!entries.isEmpty()) {
                retval.put(TRASH, entries.toArray(new ListLsubEntry[entries.size()])[0].getFullName());
            }
            entries = ListLsubCache.getArchiveEntry(-1, imapFolder, session, imapConfig.getIMAPProperties());
            if (!entries.isEmpty()) {
                retval.put(ARCHIVE, entries.toArray(new ListLsubEntry[entries.size()])[0].getFullName());
            }
            return retval;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    private static final int FOLDER_TYPE = (Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);

    @Override
    public String createFolder(MailFolderDescription toCreate) throws OXException {
        final String name = toCreate.getName();
        if (Strings.isEmpty(name)) {
            throw MailExceptionCode.INVALID_FOLDER_NAME_EMPTY.create();
        }
        int maxMailboxNameLength = IMAPProperties.getInstance().getMaxMailboxNameLength(session.getUserId(), session.getContextId());
        if (name.length() > maxMailboxNameLength) {
            throw MailExceptionCode.INVALID_FOLDER_NAME_TOO_LONG.create(Integer.valueOf(maxMailboxNameLength));
        }
        {
            char sep = toCreate.getSeparator();
            if (sep <= '\0') {
                sep = getSeparator(imapStore);
                toCreate.setSeparator(sep);
            }
        }
        boolean created = false;
        IMAPFolder createMe = null;
        boolean subscribed = false;
        try {
            /*
             * Insert
             */
            String parentFullname = toCreate.getParentFullname();
            final boolean isParentDefault;
            IMAPFolder parent;
            if (ROOT_FOLDER_ID.equals(parentFullname)) {
                parent = (IMAPFolder) imapStore.getDefaultFolder();
                parentFullname = "";
                isParentDefault = true;
            } else {
                if (toCreate.containsSeparator() && !checkFolderPathValidity(parentFullname, toCreate.getSeparator())) {
                    throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, toCreate.getName(), invalidCharsString(toCreate.getSeparator(), session.getUserId(), session.getContextId()));
                }
                parent = getIMAPFolder(parentFullname);
                isParentDefault = false;
            }
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (parent) {
                ListLsubEntry parentEntry = ListLsubCache.getCachedLISTEntry(parentFullname, accountId, parent, session, imapConfig.getIMAPProperties());
                if (parentEntry.exists()) {
                    /*
                     * Check if parent holds folders
                     */
                    if (!parentEntry.hasInferiors()) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS, imapConfig, session, isParentDefault ? ROOT_FOLDER_NAME : parentFullname);
                    }
                } else {
                    if (!doesExist(parent, true)) {
                        ListInfo listInfo = IMAPCommandsCollection.getListInfo(parentFullname, parent);
                        if (null == listInfo && !canBeOpened(parent)) {
                            parent = checkForNamespaceFolder(parentFullname);
                            if (null == parent) {
                                throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, parentFullname);
                            }
                        } else if (!listInfo.hasInferiors) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS, imapConfig, session, isParentDefault ? ROOT_FOLDER_NAME : parentFullname);
                        }
                    }
                }
                /*
                 * Check ACLs if enabled
                 */
                if (imapConfig.isSupportsACLs()) {
                    try {
                        if (isParentDefault) {
                            if (!(RootSubfoldersEnabledCache.isRootSubfoldersEnabled(imapConfig, (DefaultFolder) parent, session))) {
                                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, ROOT_FOLDER_NAME);
                            }
                        } else {
                            if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(parent, true, session, accountId))) {
                                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, parentFullname);
                            }
                        }
                    } catch (MessagingException e) {
                        /*
                         * MYRIGHTS command failed for given mailbox
                         */
                        if (!imapConfig.getImapCapabilities().hasNamespace() || !NamespacesCache.containedInPersonalNamespaces(parentFullname, imapStore, true, session, accountId)) {
                            /*
                             * No namespace support or given parent is NOT covered by user's personal namespaces.
                             */
                            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, isParentDefault ? ROOT_FOLDER_NAME : parentFullname);
                        }
                        LOG.debug("MYRIGHTS command failed on namespace folder", e);
                    }
                }
                /*
                 * Check if IMAP server is in MBox format; meaning folder either hold messages or subfolders but not both
                 */
                final char separator = getSeparator(parent);
                final boolean mboxEnabled =
                    MBoxEnabledCache.isMBoxEnabled(imapConfig, parent, new StringBuilder(parent.getFullName()).append(separator).toString());
                if (!checkFolderNameValidity(name, separator, mboxEnabled, session)) {
                    throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, name, invalidCharsString(separator, session.getUserId(), session.getContextId()));
                }
                if (isParentDefault) {
                    /*
                     * Below default folder
                     */
                    createMe = getIMAPFolder(name);
                } else {
                    createMe =
                        (IMAPFolder) imapStore.getFolder(new StringBuilder(parent.getFullName()).append(separator).append(name).toString());
                }
                /*
                 * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
                 */
                synchronized (createMe) {
                    int ftype = FOLDER_TYPE;
                    if (mboxEnabled) {
                        ftype = getNameOf(createMe).endsWith(String.valueOf(separator)) ? Folder.HOLDS_FOLDERS : Folder.HOLDS_MESSAGES;
                    }
                    try {
                        created = createMe.create(ftype, true);
                        if (!created) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, createMe.getFullName(), isParentDefault ? ROOT_FOLDER_NAME : parent.getFullName());
                        }
                    } catch (MessagingException e) {
                        if (!"Unsupported type".equals(e.getMessage())) {
                            Exception nextException = e.getNextException();
                            if (nextException instanceof com.sun.mail.iap.BadCommandException) {
                                // Bad input for associated IMAP server
                                throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, e, createMe.getFullName(), isParentDefault ? ROOT_FOLDER_NAME : parent.getFullName());
                            }
                            if (nextException instanceof CommandFailedException cxf) {
                                // Command failed
                                ResponseCode responseCode = cxf.getKnownResponseCode();
                                if (ResponseCode.LIMIT == responseCode) {
                                    // Apparently too many folders
                                    throw IMAPException.create(IMAPException.Code.TOO_MANY_FOLDERS, imapConfig, session, e, createMe.getFullName());
                                }
                            }

                            FolderCreationFailedException fcfe = new FolderCreationFailedException(createMe.getFullName(), "IMAP folder could not be created", e);
                            throw MimeMailException.handleMessagingException(fcfe, imapConfig, session);
                        }
                        LOG.warn("IMAP folder creation failed due to unsupported type. Going to retry with fallback type HOLDS-MESSAGES.", e);
                        created = createMe.create(Folder.HOLDS_MESSAGES);
                        if (!created) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, e, createMe.getFullName(), isParentDefault ? ROOT_FOLDER_NAME : parent.getFullName());
                        }
                        LOG.info("IMAP folder created with fallback type HOLDS_MESSAGES");
                    }
                    /*
                     * Subscribe
                     */
                    if (!getImapConfig().getIMAPProperties().isSupportSubscription()) {
                        subscribed = true;
                    } else if (toCreate.containsSubscribed()) {
                        subscribed = toCreate.isSubscribed();
                    } else {
                        subscribed = true;
                    }
                    boolean subscribeWasSuccessful = IMAPCommandsCollection.forceSetSubscribed(imapStore, createMe.getFullName(), subscribed);
                    if (!subscribeWasSuccessful) {
                        subscribed = false;
                        OXException warning = IMAPException.create(IMAPException.Code.SUBSCRIBE_AFTER_CREATE_FAILED, imapConfig, session, null, createMe.getFullName());
                        imapAccess.addWarnings(Collections.singletonList(warning));
                    }
                    /*
                     * Apply ACLs if supported by IMAP server
                     */
                    if (imapConfig.isSupportsACLs() && toCreate.containsPermissions()) {
                        Optional<ACL[]> optInitialACLs = getACLSafe(createMe);
                        if (optInitialACLs.isPresent()) {
                            ACL[] initialACLs = optInitialACLs.get();
                            ACL[] newACLs = permissions2ACL(toCreate.getPermissions(), createMe);
                            Entity2ACL entity2ACL = getEntity2ACL();
                            Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session, createMe, imapConfig);
                            Map<String, ACL> m = acl2map(newACLs);
                            if (!equals(initialACLs, m, entity2ACL, args)) {
                                ACLExtension aclExtension = imapConfig.getACLExtension();
                                if (aclExtension.canSetACL(createMe.myRights())) {
                                    // Check for admin
                                    {
                                        boolean adminFound = false;
                                        for (int i = 0; (i < newACLs.length) && !adminFound; i++) {
                                            if (aclExtension.canSetACL(newACLs[i].getRights())) {
                                                adminFound = true;
                                            }
                                        }
                                        if (!adminFound) {
                                            throw IMAPException.create(IMAPException.Code.NO_ADMIN_ACL, imapConfig, session, createMe.getFullName());
                                        }
                                    }
                                    /*
                                     * Apply new ACLs
                                     */
                                    List<ACL> validatedACL = new ArrayList<>(initialACLs.length);
                                    final Map<String, ACL> om = acl2map(initialACLs);
                                    for (int i = 0; i < newACLs.length; i++) {
                                        ACL validated = validate(newACLs[i], om, Optional.empty(), null);
                                        if (null != validated) {
                                            createMe.addACL(validated);
                                            validatedACL.add(validated);
                                        }
                                    }
                                    /*
                                     * Remove other ACLs
                                     */
                                    ACL[] removedACLs = getRemovedACLs(m, initialACLs);
                                    if (removedACLs.length > 0) {
                                        for (int i = 0; i < removedACLs.length; i++) {
                                            if (isKnownEntity(removedACLs[i].getName(), entity2ACL, ctx, args)) {
                                                createMe.removeACL(removedACLs[i].getName());
                                            }
                                        }
                                    }
                                    // Affected users, too
                                    dropListLsubCachesForOther(validatedACL.toArray(new ACL[validatedACL.size()]), createMe.getFullName());
                                } else {
                                    /*
                                     * Add a warning
                                     */
                                    imapAccess.addWarnings(Collections.<OXException> singletonList(IMAPException.create(IMAPException.Code.NO_ADMINISTER_ACCESS_ON_INITIAL, imapConfig, session, createMe.getFullName())));
                                    // Affected users, too
                                    dropListLsubCachesForOther(initialACLs, createMe.getFullName());
                                }
                            } else {
                                // Affected users, too
                                dropListLsubCachesForOther(initialACLs, createMe.getFullName());
                            }
                        }
                    }
                    return createMe.getFullName();
                }
            }
        } catch (MessagingException e) {
            if (createMe != null && created) {
                try {
                    if (doesExist(createMe, false)) {
                        createMe.delete(true);
                        created = false;
                    }
                } catch (Exception e2) {
                    LOG.error("Temporary created IMAP folder \"{}could not be deleted", createMe.getFullName(), e2);
                }
            }

            if (MimeMailException.isExistsException(e)) {
                // Assume outdated cache as client expected that such a folder does not exist
                ListLsubCache.clearCache(accountId, session);
                throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, name);
            }

            throw handleMessagingException(e);
        } catch (OXException e) {
            /*
             * No folder deletion on IMAP error "NO_ADMINISTER_ACCESS_ON_INITIAL"
             */
            if (e.isPrefix("MSG") && IMAPException.Code.NO_ADMINISTER_ACCESS_ON_INITIAL.getNumber() != e.getCode()) {
                if (createMe != null && created) {
                    try {
                        if (doesExist(createMe, false)) {
                            createMe.delete(true);
                            created = false;
                        }
                    } catch (Exception e2) {
                        LOG.error("Temporary created IMAP folder \"{}\" could not be deleted", createMe.getFullName(), e2);
                    }
                }
            }
            throw e;
        } catch (Exception e) {
            if (createMe != null && created) {
                try {
                    if (doesExist(createMe, false)) {
                        createMe.delete(true);
                        created = false;
                    }
                } catch (Exception e2) {
                    LOG.error("Temporary created IMAP folder \"{}\" could not be deleted", createMe.getFullName(), e2);
                }
            }
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            if (createMe != null) {
                if (created) {
                    try {
                        ListLsubCache.addSingle(createMe, subscribed, accountId, session, imapConfig.getIMAPProperties());
                    } catch (@SuppressWarnings("unused") Exception e) {
                        // Updating LIST/LSUB cache failed
                        ListLsubCache.clearCache(accountId, session);
                    } finally {
                        closeSafe(createMe);
                    }
                } else {
                    closeSafe(createMe);
                }
            }
        }
    }

    private Entity2ACL getEntity2ACL() throws OXException {
        return Entity2ACL.getInstance(imapStore, imapConfig);
    }

    @Override
    public String renameFolder(String fullName, String newName) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName)) {
            final String name;
            if (accountId == Account.DEFAULT_ID) {
                name = MailFolder.ROOT_FOLDER_NAME;
            } else {
                final MailAccountStorageService mass = Services.getService(MailAccountStorageService.class);
                if (null == mass) {
                    name = MailFolder.ROOT_FOLDER_NAME;
                } else {
                    name = mass.getMailAccount(accountId, session.getUserId(), session.getContextId()).getName();
                }

            }
            throw IMAPException.create(IMAPException.Code.NO_RENAME_ACCESS, imapConfig, session, name);
        }
        try {
            IMAPFolder renameMe = getIMAPFolder(fullName);
            char separator;
            boolean canOpen;
            ListLsubEntry renameEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null == renameEntry || !renameEntry.exists()) {
                ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, renameMe);
                if (null == listInfo) {
                    if (false == canBeOpened(renameMe)) {
                        renameMe = checkForNamespaceFolder(fullName);
                        if (null == renameMe) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                        }
                        throw IMAPException.create(IMAPException.Code.NO_ADMINISTER_ACCESS, imapConfig, session, fullName);
                    }
                    separator = getSeparator();
                    canOpen = true;
                } else {
                    separator = listInfo.separator;
                    canOpen = listInfo.canOpen;
                }
            } else {
                separator = renameEntry.getSeparator();
                canOpen = renameEntry.canOpen();
            }
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (renameMe) {
                if (imapConfig.isSupportsACLs() && canOpen) {
                    try {
                        if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(renameMe, true, session, accountId))) {
                            throw IMAPException.create(IMAPException.Code.NO_RENAME_ACCESS, imapConfig, session, fullName);
                        }
                    } catch (MessagingException e) {
                        /*
                         * MYRIGHTS command failed for given mailbox
                         */
                        throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                    }
                }
                if (getChecker().isDefaultFolder(fullName, true)) {
                    throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, fullName);
                }
                if (Strings.isEmpty(newName)) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME_EMPTY.create();
                }
                if (newName.indexOf(separator) != -1) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME2.create(newName, Character.toString(separator));
                }
                {
                    int maxMailboxNameLength = IMAPProperties.getInstance().getMaxMailboxNameLength(session.getUserId(), session.getContextId());
                    if (newName.length() > maxMailboxNameLength) {
                        throw MailExceptionCode.INVALID_FOLDER_NAME_TOO_LONG.create(Integer.valueOf(maxMailboxNameLength));
                    }
                }
                /*
                 * Notify message storage about outstanding rename
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                /*-
                 * Perform rename operation
                 *
                 * Rename can only be invoked on a closed folder
                 */
                if (renameMe.isOpen()) {
                    renameMe.close(false);
                }
                final boolean mboxEnabled;
                final IMAPFolder renameFolder;
                {
                    final String parentFullName = renameMe.getParent().getFullName();
                    final StringBuilder tmp = new StringBuilder();
                    if (parentFullName.length() > 0) {
                        tmp.append(parentFullName).append(separator);
                    }
                    tmp.append(newName);
                    renameFolder = (IMAPFolder) imapStore.getFolder(tmp.toString());
                    /*
                     * Check for MBox
                     */
                    mboxEnabled = MBoxEnabledCache.isMBoxEnabled(imapConfig, renameFolder, new StringBuilder(parentFullName).append(separator).toString());
                }
                ListLsubEntry testEntry = ListLsubCache.optCachedLISTEntry(renameFolder.getFullName(), accountId, renameFolder, session, imapConfig.getIMAPProperties());
                if (testEntry.exists()) {
                    throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, renameFolder.getFullName());
                }
                if (!checkFolderNameValidity(newName, separator, mboxEnabled, session)) {
                    throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator, session.getUserId(), session.getContextId()));
                }
                /*
                 * Remember ACLs
                 */
                Optional<ACL[]> optInitialACLs = Optional.empty();
                if (imapConfig.isSupportsACLs()) {
                    optInitialACLs = getACLSafe(renameMe);
                }
                /*
                 * Remember subscription status
                 */
                Map<String, Boolean> subscriptionStatus;
                final String newFullName = renameFolder.getFullName();
                final String oldFullName = renameMe.getFullName();
                try {
                    subscriptionStatus = getSubscriptionStatus(renameMe, separator, oldFullName, newFullName);
                } catch (@SuppressWarnings("unused") MessagingException e) {
                    LOG.warn("Subscription status of folder \"{}\" and its subfolders could not be stored prior to rename operation", renameMe.getFullName());
                    subscriptionStatus = null;
                }
                removeSessionData(renameMe.getFullName(), separator, renameMe);
                /*
                 * Unsubscribe sub-tree
                 */
                if (null != subscriptionStatus && subscriptionStatus.size() > 1) {
                    setFolderSubscription(renameMe.getFullName(), renameMe, false);
                } else {
                    IMAPCommandsCollection.forceSetSubscribed(imapStore, renameMe.getFullName(), false);
                }
                /*
                 * Rename
                 */
                boolean success = false;
                try {
                    if (renameMe.isOpen()) {
                        renameMe.close(false);
                    }

                    long start = System.currentTimeMillis();
                    IMAPCommandsCollection.renameFolder(renameMe, separator, renameFolder);
                    long duration = System.currentTimeMillis() - start;
                    success = true;
                    mailInterfaceMonitor.addUseTime(duration);
                } catch (MessagingException e) {
                    /*
                     * Rename failed
                     */
                    throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, e, renameMe.getFullName(), newFullName, e.getMessage());
                } finally {
                    if (!success) {
                        setFolderSubscription(renameMe.getFullName(), renameMe, true);
                    }
                }
                /*
                 * Success?
                 */
                if (!success) {
                    throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, renameMe.getFullName(), newFullName, "<not-available>");
                }
                renameMe = getIMAPFolder(newFullName);
                /*
                 * Apply remembered subscription status
                 */
                if (subscriptionStatus == null) {
                    /*
                     * At least subscribe to renamed folder
                     */
                    IMAPCommandsCollection.forceSetSubscribed(imapStore, renameMe.getFullName(), true);
                } else {
                    if (subscriptionStatus.size() > 1) {
                        applySubscriptionStatus(renameMe.getFullName(), separator, renameMe, subscriptionStatus);
                    } else {
                        boolean subscribe = subscriptionStatus.values().iterator().next().booleanValue();
                        IMAPCommandsCollection.forceSetSubscribed(imapStore, renameMe.getFullName(), subscribe);
                    }
                }
                /*
                 * Drop for other users (if any)
                 */
                dropListLsubCachesForOther(optInitialACLs, renameMe.getFullName());
                /*
                 * Return new full name
                 */
                return renameMe.getFullName();
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (OXException e) {
            throw e;
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    @Override
    public String moveFolder(String fullName, String newFullname) throws OXException {
        if (ROOT_FOLDER_ID.equals(fullName) || ROOT_FOLDER_ID.equals(newFullname)) {
            throw IMAPException.create(IMAPException.Code.NO_ROOT_MOVE, imapConfig, session);
        }
        try {
            if (ROOT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder moveMe = getIMAPFolder(fullName);
            char separator;
            ListLsubEntry moveMeEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
            if (null == moveMeEntry || !moveMeEntry.exists()) {
                ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, moveMe);
                if (null == listInfo) {
                    if (!canBeOpened(moveMe)) {
                        moveMe = checkForNamespaceFolder(fullName);
                        if (null == moveMe) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                        }
                        throw IMAPException.create(IMAPException.Code.NO_ADMINISTER_ACCESS, imapConfig, session, fullName);
                    }
                    separator = getSeparator();
                } else {
                    separator = listInfo.separator;
                }
            } else {
                separator = moveMeEntry.getSeparator();
            }
            ListLsubCache.clearCache(accountId, session);
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (moveMe) {
                /*
                 * Notify message storage about outstanding move
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                final String oldParent = moveMe.getParent().getFullName();
                final String newParent;
                final String newName;
                {
                    final int pos = newFullname.lastIndexOf(separator);
                    if (pos == -1) {
                        newParent = "";
                        newName = newFullname;
                    } else {
                        if (pos == newFullname.length() - 1) {
                            throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newFullname, invalidCharsString(separator, session.getUserId(), session.getContextId()));
                        }
                        newParent = newFullname.substring(0, pos);
                        newName = newFullname.substring(pos + 1);
                        if (!checkFolderPathValidity(newParent, separator)) {
                            throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator, session.getUserId(), session.getContextId()));
                        }
                    }
                }
                int maxMailboxNameLength = IMAPProperties.getInstance().getMaxMailboxNameLength(session.getUserId(), session.getContextId());
                if (newName.length() > maxMailboxNameLength) {
                    throw MailExceptionCode.INVALID_FOLDER_NAME_TOO_LONG.create(Integer.valueOf(maxMailboxNameLength));
                }
                Optional<ACL[]> optInitialACLs = Optional.empty();
                if (imapConfig.isSupportsACLs()) {
                    optInitialACLs = getACLSafe(moveMe);
                }
                /*
                 * Check for move
                 */
                final boolean move = !newParent.equals(oldParent);
                /*
                 * Check for rename. Rename must not be performed if a move has already been done
                 */
                final boolean rename = (!move && !newName.equals(getNameOf(moveMe)));
                if (move) {
                    /*
                     * Perform move operation
                     */
                    final String oldFullname = moveMe.getFullName();
                    if (getChecker().isDefaultFolder(oldFullname, true)) {
                        throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, oldFullname);
                    }
                    IMAPFolder destFolder;
                    final boolean isDestRoot;
                    if ("".equals(newParent)) {
                        destFolder = (IMAPFolder) imapStore.getDefaultFolder();
                        isDestRoot = true;
                    } else {
                        destFolder = getIMAPFolder(newParent);
                        isDestRoot = false;
                    }
                    if (!doesExist(destFolder, false)) {
                        destFolder = checkForNamespaceFolder(newParent);
                        if (null == destFolder) {
                            /*
                             * Destination folder could not be found, thus an invalid name was specified by user
                             */
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, isDestRoot ? ROOT_FOLDER_NAME : newParent);
                        }
                    }
                    synchronized (destFolder) {
                        if ((destFolder.getType() & Folder.HOLDS_FOLDERS) == 0) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS, imapConfig, session, isDestRoot ? ROOT_FOLDER_NAME : destFolder.getFullName());
                        }
                        if (imapConfig.isSupportsACLs() && ((destFolder.getType() & Folder.HOLDS_MESSAGES) > 0)) {
                            try {
                                if (isDestRoot) {
                                    if (!(RootSubfoldersEnabledCache.isRootSubfoldersEnabled(imapConfig, (DefaultFolder) destFolder, session))) {
                                        throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, ROOT_FOLDER_NAME);
                                    }
                                } else {
                                    if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(destFolder, true, session, accountId))) {
                                        throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, newParent);
                                    }
                                }
                            } catch (MessagingException e) {
                                /*
                                 * MYRIGHTS command failed for given mailbox
                                 */
                                if (!imapConfig.getImapCapabilities().hasNamespace() || !NamespacesCache.containedInPersonalNamespaces(newParent, imapStore, true, session, accountId)) {
                                    /*
                                     * No namespace support or given parent is NOT covered by user's personal namespaces.
                                     */
                                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, isDestRoot ? ROOT_FOLDER_NAME : newParent);
                                }
                                LOG.debug("MYRIGHTS command failed on namespace folder", e);
                            }
                        }
                        final boolean mboxEnabled = MBoxEnabledCache.isMBoxEnabled(imapConfig, destFolder, new StringBuilder(destFolder.getFullName()).append(separator).toString());
                        if (!checkFolderNameValidity(newName, separator, mboxEnabled, session)) {
                            throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator, session.getUserId(), session.getContextId()));
                        }
                        if (isSubfolderOf(destFolder.getFullName(), oldFullname, separator)) {
                            throw IMAPException.create(IMAPException.Code.NO_MOVE_TO_SUBFLD, imapConfig, session, getNameOf(moveMe), getNameOf(destFolder));
                        }
                        moveMe = moveFolder(moveMe, destFolder, newName);
                    }
                }
                /*
                 * Is rename operation?
                 */
                if (rename) {
                    /*
                     * Perform rename operation
                     */
                    if (getChecker().isDefaultFolder(moveMe.getFullName(), true)) {
                        throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, moveMe.getFullName());
                    } else if (imapConfig.isSupportsACLs() && ((moveMe.getType() & Folder.HOLDS_MESSAGES) > 0)) {
                        try {
                            if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(moveMe, true, session, accountId))) {
                                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, moveMe.getFullName());
                            }
                        } catch (MessagingException e) {
                            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, moveMe.getFullName());
                        }
                    }
                    /*
                     * Rename can only be invoked on a closed folder
                     */
                    if (moveMe.isOpen()) {
                        moveMe.close(false);
                    }
                    final boolean mboxEnabled;
                    final IMAPFolder renameFolder;
                    {
                        final IMAPFolder par = (IMAPFolder) moveMe.getParent();
                        final String parentFullName = par.getFullName();
                        final StringBuilder tmp = new StringBuilder();
                        if (parentFullName.length() > 0) {
                            tmp.append(parentFullName).append(separator);
                        }
                        tmp.append(newName);
                        renameFolder = (IMAPFolder) imapStore.getFolder(tmp.toString());
                        /*
                         * Check for MBox
                         */
                        mboxEnabled = MBoxEnabledCache.isMBoxEnabled(imapConfig, par, new StringBuilder(par.getFullName()).append(separator).toString());
                    }
                    if (doesExist(renameFolder, false)) {
                        throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, renameFolder.getFullName());
                    }
                    if (!checkFolderNameValidity(newName, separator, mboxEnabled, session)) {
                        throw IMAPException.create(IMAPException.Code.INVALID_FOLDER_NAME, imapConfig, session, newName, invalidCharsString(separator, session.getUserId(), session.getContextId()));
                    }
                    /*
                     * Remember subscription status
                     */
                    Map<String, Boolean> subscriptionStatus;
                    final String newFullName = renameFolder.getFullName();
                    final String oldFullName = moveMe.getFullName();
                    try {
                        subscriptionStatus = getSubscriptionStatus(moveMe, separator, oldFullName, newFullName);
                    } catch (@SuppressWarnings("unused") MessagingException e) {
                        LOG.warn("Subscription status of folder \"{}\" and its subfolders could not be stored prior to rename operation", moveMe.getFullName());
                        subscriptionStatus = null;
                    }
                    removeSessionData(moveMe.getFullName(), separator, moveMe);
                    /*
                     * Unsubscribe sub-tree
                     */
                    if (null != subscriptionStatus && subscriptionStatus.size() > 1) {
                        setFolderSubscription(moveMe.getFullName(), moveMe, false);
                    } else {
                        IMAPCommandsCollection.forceSetSubscribed(imapStore, moveMe.getFullName(), false);
                    }
                    /*
                     * Rename
                     */
                    boolean success = false;
                    try {
                        if (moveMe.isOpen()) {
                            moveMe.close(false);
                        } else {
                            // Enforce close
                            IMAPCommandsCollection.forceCloseCommand(moveMe);
                        }
                        final long start = System.currentTimeMillis();
                        IMAPCommandsCollection.renameFolder(moveMe, separator, renameFolder);
                        success = true;
                        mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                    } catch (MessagingException e) {
                        /*
                         * Rename failed
                         */
                        throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, e, moveMe.getFullName(), newFullName, e.getMessage());

                    } finally {
                        if (!success) {
                            setFolderSubscription(moveMe.getFullName(), moveMe, true);
                        }
                    }
                    /*
                     * Success?
                     */
                    if (!success) {
                        throw IMAPException.create(IMAPException.Code.RENAME_FAILED, imapConfig, session, moveMe.getFullName(), newFullName, "<not-available>");
                    }
                    moveMe = getIMAPFolder(oldFullName);
                    if (doesExist(moveMe, false)) {
                        deleteFolder(moveMe, separator);
                    }
                    moveMe = getIMAPFolder(newFullName);
                    /*
                     * Apply remembered subscription status
                     */
                    if (subscriptionStatus == null) {
                        /*
                         * At least subscribe to renamed folder
                         */
                        IMAPCommandsCollection.forceSetSubscribed(imapStore, moveMe.getFullName(), true);
                    } else {
                        if (subscriptionStatus.size() > 1) {
                            applySubscriptionStatus(moveMe.getFullName(), separator, moveMe, subscriptionStatus);
                        } else {
                            boolean subscribe = subscriptionStatus.values().iterator().next().booleanValue();
                            IMAPCommandsCollection.forceSetSubscribed(imapStore, moveMe.getFullName(), subscribe);
                        }
                    }
                }
                /*
                 * Drop for other users (if any)
                 */
                dropListLsubCachesForOther(optInitialACLs, moveMe.getFullName());
                return moveMe.getFullName();
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (OXException e) {
            throw e;
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    @Override
    public String updateFolder(String fullName, MailFolderDescription toUpdate) throws OXException {
        boolean changed = false;
        try {
            if (ROOT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder updateMe = getIMAPFolder(fullName);
            ListLsubEntry updateMeEntry = ListLsubCache.getCachedLISTEntry(fullName, accountId, updateMe, session, imapConfig.getIMAPProperties());
            if (!updateMeEntry.exists()) {
                ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, updateMe);
                if (null == listInfo && !canBeOpened(updateMe)) {
                    updateMe = checkForNamespaceFolder(fullName);
                    if (null == updateMe) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                }
            }
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (updateMe) {
                /*
                 * Check for standard folder & possible subscribe operation
                 */
                final IMAPDefaultFolderChecker checker = getChecker();
                boolean defaultFolder = false;
                if ("INBOX".equals(fullName)
                    || fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_TRASH))
                    || fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_DRAFTS))
                    || fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_SENT))
                    || fullName.equals(checker.getDefaultFolder(StorageUtility.INDEX_SPAM))) {

                    defaultFolder = true;
                }

                boolean performSubscription = getImapConfig().getIMAPProperties().isIgnoreSubscription() && defaultFolder ? false : performSubscribe(toUpdate, updateMeEntry);
                if (performSubscription && defaultFolder && !toUpdate.isSubscribed()) {
                    OXException warning = IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UNSUBSCRIBE, imapConfig, session, fullName);
                    warning.setCategory(OXException.CATEGORY_WARNING);
                    imapAccess.addWarnings(Collections.singletonList(warning));
                    performSubscription = false;
                }
                /*
                 * Notify message storage
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                /*
                 * Proceed update
                 */
                if (imapConfig.isSupportsACLs() && toUpdate.containsPermissions()) {
                	MailFolder mailFolder = getFolder(fullName);
                	if (!mailFolder.getOwnPermission().isFolderAdmin()) {
                		throw MailExceptionCode.INSUFFICIENT_PERMISSIONS.create();
                	}
                	Optional<ACL[]> optOldACLs = getACLSafe(updateMe);
                    if (optOldACLs.isPresent()) {
                        ACL[] oldACLs = optOldACLs.get();
                        final ACL[] newACLs = permissions2ACL(toUpdate.getPermissions(), updateMe);
                        final Entity2ACL entity2ACL = getEntity2ACL();
                        final Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session, updateMe, imapConfig);
                        final Map<String, ACL> m = acl2map(newACLs);
                        if (!equals(oldACLs, m, entity2ACL, args)) {
                            /*
                             * Default folder is affected, check if owner still holds full rights
                             */
                            final ACLExtension aclExtension = imapConfig.getACLExtension();
                            if (getChecker().isDefaultFolder(updateMe.getFullName()) && !stillHoldsFullRights(updateMe, newACLs, aclExtension)) {
                                throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_UPDATE, imapConfig, session, updateMe.getFullName());
                            }
                            if (!aclExtension.canSetACL(RightsCache.getCachedRights(updateMe, true, session, accountId))) {
                                throw IMAPException.create(IMAPException.Code.NO_ADMINISTER_ACCESS, imapConfig, session, updateMe.getFullName());
                            }
                            if (!NamespacesCache.startsWithAnyOfSharedNamespaces(fullName, imapStore, true, session, accountId) && !NamespacesCache.startsWithAnyOfUserNamespaces(fullName, imapStore, true, session, accountId) && !stillHoldsAdministerRights(updateMe, newACLs, aclExtension)) {
                                throw IMAPException.create(IMAPException.Code.OWNER_MUST_BE_ADMIN, imapConfig, session, updateMe.getFullName());
                            }
                            /*
                             * Check new ACLs
                             */
                            if (newACLs.length == 0) {
                                throw IMAPException.create(IMAPException.Code.NO_ADMIN_ACL, imapConfig, session, updateMe.getFullName());
                            }
                            {
                                boolean adminFound = false;
                                for (int i = 0; (i < newACLs.length) && !adminFound; i++) {
                                    if (aclExtension.canSetACL(newACLs[i].getRights())) {
                                        adminFound = true;
                                    }
                                }
                                if (!adminFound) {
                                    throw IMAPException.create(IMAPException.Code.NO_ADMIN_ACL, imapConfig, session, updateMe.getFullName());
                                }
                            }
                            Optional<JSONObject> optionalMetadata = IMAPDeputyMetadataUtility.getDeputyMetadata(updateMe);
                            final List<ACL> entities = new LinkedList<>();
                            /*
                             * Remove deleted ACLs
                             */
                            final ACL[] removedACLs = getRemovedACLs(m, oldACLs);
                            if (removedACLs.length > 0) {
                                if (optionalMetadata.isPresent()) {
                                    if (!"true".equals(LogProperties.get(LogProperties.Name.DEPUTY))) {
                                        DeputyService deputyService = Services.optService(DeputyService.class);
                                        if (deputyService != null) {
                                            JSONObject jMetadata = optionalMetadata.get();
                                            Set<String> aclNames = Arrays.stream(removedACLs).map(acl -> acl.getName()).collect(Collectors.toSet());
                                            for (Map.Entry<String, Object> e : jMetadata.entrySet()) {
                                                String deputyId = e.getKey();
                                                if (deputyService.existsDeputyPermission(deputyId, session.getContextId())) {
                                                    JSONObject jDeputyMetadata = (JSONObject) e.getValue();
                                                    String aclName = jDeputyMetadata.optString("name", null);
                                                    if (aclName != null && aclNames.contains(aclName)) {
                                                        throw IMAPException.create(IMAPException.Code.NO_DEPUTY_PERMISSION_DELETE, imapConfig, session, aclName, updateMe.getFullName());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                for (int i = 0; i < removedACLs.length; i++) {
                                    final String entityName = removedACLs[i].getName();
                                    if (isKnownEntity(entityName, entity2ACL, ctx, args)) {
                                        updateMe.removeACL(entityName);
                                        entities.add(removedACLs[i]);
                                        changed = true;
                                    }
                                }
                            }
                            /*
                             * Change existing ACLs according to new ACLs
                             */
                            final Map<String, ACL> om = acl2map(oldACLs);
                            for (int i = 0; i < newACLs.length; i++) {
                                ACL newACL = newACLs[i];
                                ACL validated = validate(newACL, om, optionalMetadata, updateMe.getFullName());
                                if (null != validated) {
                                    updateMe.addACL(validated);
                                    entities.add(newACL);
                                    changed = true;
                                }
                            }
                            /*
                             * Since the ACLs have changed remove cached rights
                             */
                            RightsCache.removeCachedRights(updateMe, session, accountId);
                            /*
                             * Does affect ListLsubCache of other users, too
                             */
                            dropListLsubCachesForOther(entities.toArray(new ACL[entities.size()]), updateMe.getFullName());
                        }
                    }
                }
                if (performSubscription) {
                    /*
                     * Try to subscribe & handle possible error
                     */
                    try {
                        boolean subscribe = toUpdate.isSubscribed();
                        setSubscribed(subscribe, updateMe, true);
                        changed = true;
                    } catch (MessagingException me) {
                        Exception cause = me.getNextException();
                        if (cause instanceof CommandFailedException) {
                            // Encountered NO response for subscribe attempt
                            if (imapConfig.isSupportsACLs()) {
                                if ((updateMe.getType() & Folder.HOLDS_MESSAGES) > 0) {
                                    try {
                                        if (!imapConfig.getACLExtension().canLookUp(RightsCache.getCachedRights(updateMe, true, session, accountId))) {
                                            throw IMAPException.create(IMAPException.Code.NO_LOOKUP_ACCESS, imapConfig, session, fullName);
                                        }
                                    } catch (MessagingException e) {
                                        throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                                    }
                                } else {
                                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, fullName);
                                }
                            }
                        }
                        throw handleMessagingException(fullName, me);
                    }
                }
                return updateMe.getFullName();
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (OXException e) {
            throw e;
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (changed) {
                ListLsubCache.clearCache(accountId, session);
            }
        }
    }

    private static void setSubscribed(boolean subscribe, IMAPFolder imapFolder, boolean errorOnFailedCommand) throws MessagingException {
        if (errorOnFailedCommand) {
            // Quit with an exception in case a "NO" response happens
            final String fullName = imapFolder.getFullName();
            imapFolder.doCommand(p -> {
                if (subscribe) {
                    p.subscribe(fullName);
                } else {
                    p.unsubscribe(fullName);
                }
                return null;
            });
        } else {
            // JavaMail routine performs 'IMAPFolder.doCommandIgnoreFailure()', which discards a possible CommandFailedException
            imapFolder.setSubscribed(subscribe);
        }
        if (subscribe) {
            // Ensure parent gets subscribed, too
            try {
                IMAPFolder parent = (IMAPFolder) imapFolder.getParent();
                if ((null != parent) && (parent.getFullName().length() > 0)) {
                    setSubscribed(subscribe, parent, false);
                }
            } catch (@SuppressWarnings("unused") Exception e) {
                // Ignore
            }
        }
    }

    private static boolean performSubscribe(MailFolderDescription toUpdate, ListLsubEntry updateMe) {
        return toUpdate.containsSubscribed() && (toUpdate.isSubscribed() != updateMe.isSubscribed());
    }

    private void deleteTemporaryCreatedFolder(IMAPFolder temporaryFolder) throws OXException, MessagingException {
        if (doesExist(temporaryFolder, false)) {
            try {
                temporaryFolder.delete(true);
            } catch (MessagingException e1) {
                LOG.error("Temporary created folder could not be deleted: {}", temporaryFolder.getFullName(), e1);
            }
        }
    }

    @Override
    public String deleteFolder(String fullName, boolean hardDelete) throws OXException {
        boolean clearListLsubCache = false;
        try {
            if (ROOT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder deleteMe = getIMAPFolder(fullName);
            char separator;
            {
                ListLsubEntry deleteMeEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
                if (null != deleteMeEntry) {
                    if (!deleteMeEntry.exists()) {
                        deleteMe = checkForNamespaceFolder(fullName);
                        if (null == deleteMe) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                        }
                    }
                    separator = deleteMeEntry.getSeparator();
                } else {
                    ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, deleteMe);
                    if (null == listInfo) {
                        if (false == canBeOpened(deleteMe)) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                        }
                        separator = getSeparator();
                    } else {
                        separator = listInfo.separator;
                    }
                }
            }
            clearListLsubCache = true;
            synchronized (deleteMe) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                if (hardDelete) {
                    /*
                     * Delete permanently
                     */
                    deleteFolder(deleteMe, separator);
                } else {
                    String trashFullName = getTrashFolder();
                    if (isSubfolderOf(getParentFullName(fullName, separator), trashFullName, separator) || !inferiors(trashFullName, deleteMe)) {
                        /*
                         * Delete permanently
                         */
                        deleteFolder(deleteMe, separator);
                    } else {
                        /*
                         * Just move this folder to trash
                         */
                        imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFullName);
                        final String name = getNameOf(deleteMe);
                        int appendix = 1;
                        final StringBuilder sb = new StringBuilder();
                        sb.append(trashFullName).append(separator).append(name);
                        while (ListLsubCache.getCachedLISTEntry(sb.toString(), accountId, deleteMe, session, imapConfig.getIMAPProperties()).exists()) {
                            /*
                             * A folder of the same name already exists. Append appropriate appendix to folder name and check existence again.
                             */
                            sb.setLength(0);
                            sb.append(trashFullName).append(separator).append(name).append('_').append(++appendix);
                        }
                        IMAPFolder trashFolder = (IMAPFolder) imapStore.getFolder(trashFullName);
                        IMAPFolder newFolder = (IMAPFolder) imapStore.getFolder(sb.toString());
                        synchronized (newFolder) {
                            try {
                                moveFolder(deleteMe, trashFolder, newFolder, false);
                            } catch (OXException | MessagingException e) {
                                deleteTemporaryCreatedFolder(newFolder);
                                throw e;
                            }
                        }
                    }
                }
                return fullName;
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (clearListLsubCache) {
                ListLsubCache.clearCache(accountId, session);
            }
        }
    }

    @Override
    public String trashFolder(String fullName) throws OXException {
        boolean clearListLsubCache = false;
        try {
            if (ROOT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder deleteMe = getIMAPFolder(fullName);
            char separator;
            {
                ListLsubEntry deleteMeEntry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
                if (null != deleteMeEntry) {
                    if (!deleteMeEntry.exists()) {
                        deleteMe = checkForNamespaceFolder(fullName);
                        if (null == deleteMe) {
                            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                        }
                    }
                    separator = deleteMeEntry.getSeparator();
                } else {
                    ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, deleteMe);
                    if (null == listInfo) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                    }
                    separator = listInfo.separator;
                }
            }
            clearListLsubCache = true;
            synchronized (deleteMe) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                String trashFullName = getTrashFolder();
                if (isSubfolderOf(getParentFullName(fullName, separator), trashFullName, separator) || !inferiors(trashFullName, deleteMe)) {
                    /*
                     * Delete permanently
                     */
                    deleteFolder(deleteMe, separator);
                    return null;
                }
                /*
                 * Just move this folder to trash
                 */
                imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFullName);
                final String name = getNameOf(deleteMe);
                int appendix = 1;
                final StringBuilder sb = new StringBuilder();
                sb.append(trashFullName).append(separator).append(name);
                while (ListLsubCache.getCachedLISTEntry(sb.toString(), accountId, deleteMe, session, imapConfig.getIMAPProperties()).exists()) {
                    /*
                     * A folder of the same name already exists. Append appropriate appendix to folder name and check existence again.
                     */
                    sb.setLength(0);
                    sb.append(trashFullName).append(separator).append(name).append('_').append(++appendix);
                }
                IMAPFolder trashFolder = (IMAPFolder) imapStore.getFolder(trashFullName);
                IMAPFolder newFolder = (IMAPFolder) imapStore.getFolder(sb.toString());
                synchronized (newFolder) {
                    try {
                        moveFolder(deleteMe, trashFolder, newFolder, false);
                    } catch (OXException | MessagingException e) {
                        deleteTemporaryCreatedFolder(newFolder);
                        throw e;
                    }
                }
                return newFolder.getFullName();
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            if (clearListLsubCache) {
                ListLsubCache.clearCache(accountId, session);
            }
        }
    }

    private boolean inferiors(String fullName, IMAPFolder imapFolder) throws OXException, MessagingException {
        ListLsubEntry entry = ListLsubCache.tryCachedLISTEntry(fullName, accountId, session);
        if (null != entry) {
            return entry.hasInferiors();
        }

        ListInfo listInfo = IMAPCommandsCollection.getListInfo(fullName, imapFolder);
        return null == listInfo ? false : listInfo.hasInferiors;
    }

    private static String getParentFullName(String fullName, char separator) {
        int pos = fullName.indexOf(separator);
        return pos < 0 || pos == fullName.length() - 1 ? "" : fullName.substring(0, pos);

    }

    private static final Flags FLAGS_DELETED = new Flags(Flags.Flag.DELETED);

    @Override
    public void expungeFolder(String fullName) throws OXException {
        expungeFolder(fullName, false);
    }

    @Override
    public void expungeFolder(String fullName, boolean hardDelete) throws OXException {
        try {
            if (ROOT_FOLDER_ID.equals(fullName)) {
                return;
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder f = getIMAPFolderWithRecentListener(fullName);
            if (!doesExist(f, true)) {
                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            synchronized (f) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                try {
                    if (!isSelectable(f)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, f.getFullName());
                    }
                    if (imapConfig.isSupportsACLs()) {
                        final Rights myrights = RightsCache.getCachedRights(f, true, session, accountId);
                        if (!imapConfig.getACLExtension().canRead(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, f.getFullName());
                        }
                        if (!imapConfig.getACLExtension().canDeleteMessages(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, f.getFullName());
                        }
                    }
                } catch (MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, f.getFullName());
                }
                /*
                 * Remove from session storage
                 */
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, fullName);
                }
                final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, fullName);
                boolean marked = false;
                f.open(Folder.READ_WRITE);
                try {
                    final int msgCount = f.getMessageCount();
                    if (msgCount <= 0) {
                        /*
                         * Empty folder
                         */
                        return;
                    }

                    marked = setMarker(opKey, f);

                    String trashFullname = getTrashFolder();
                    boolean hardDeleteMsgsByConfig = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isHardDeleteMsgs();
                    boolean isTrash = trashFullname.equals(fullName) || isSubfolderOf(fullName, trashFullname, getSeparator());
                    final boolean backup = (!hardDelete && !hardDeleteMsgsByConfig && !isTrash);
                    if (backup) {
                        imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFullname);
                        final Message[] candidates = f.search(new FlagTerm(FLAGS_DELETED, true));
                        if (null != candidates && candidates.length > 0) {
                            f.copyMessages(candidates, imapStore.getFolder(trashFullname));
                        }
                    }
                }  finally {
                    if (marked) {
                        unsetMarker(opKey);
                    }
                    f.close(true);
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        } finally {
            ListLsubCache.clearCache(accountId, session);
        }
    }

    @Override
    public void clearFolder(String fullName, boolean hardDelete) throws OXException {
        try {
            if (ROOT_FOLDER_ID.equals(fullName)) {
                throw MailExceptionCode.NO_ROOT_FOLDER_MODIFY_DELETE.create();
            }
            IMAPFolderWorker.checkFailFast(imapStore, fullName);
            IMAPFolder f = getIMAPFolderWithRecentListener(fullName);
            if (!doesExist(f, true)) {
                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            synchronized (f) {
                imapAccess.getMessageStorage().notifyIMAPFolderModification(fullName);
                try {
                    if (!isSelectable(f)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, f.getFullName());
                    }
                    if (imapConfig.isSupportsACLs()) {
                        final Rights myrights = RightsCache.getCachedRights(f, true, session, accountId);
                        if (!imapConfig.getACLExtension().canRead(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, f.getFullName());
                        }
                        if (!imapConfig.getACLExtension().canDeleteMessages(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_DELETE_ACCESS, imapConfig, session, f.getFullName());
                        }
                    }
                } catch (MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, f.getFullName());
                }
                /*
                 * Remove from session storage
                 */
                if (IMAPSessionStorageAccess.isEnabled()) {
                    IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, fullName);
                }
                final OperationKey opKey = new OperationKey(Type.MSG_DELETE, accountId, fullName);
                boolean marked = false;
                f.open(Folder.READ_WRITE);
                try {
                    int msgCount = f.getMessageCount();

                    String trashFullname = getTrashFolder();
                    boolean hardDeleteMsgsByConfig = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx).isHardDeleteMsgs();
                    boolean isTrash = trashFullname.equals(fullName) || isSubfolderOf(fullName, trashFullname, getSeparator());
                    boolean backup = (!hardDelete && !hardDeleteMsgsByConfig && !isTrash);
                    if (backup) {
                        imapAccess.getMessageStorage().notifyIMAPFolderModification(trashFullname);
                    }

                    // Check for simple "clear them all"
                    if (!backup) {
                        // Just hard-delete messages
                        boolean manyMessages = msgCount > MailProperties.getInstance().getMailFetchLimit(session.getUserId(), session.getContextId());
                        boolean clearMessages = true;
                        if (allowDeleteTrashThroughRename(f, isTrash, manyMessages)) {
                            // Rename can only be invoked on a closed folder
                            f.close(false);

                            // Perform delete-through-rename
                            marked = setMarker(opKey, f);
                            boolean deletionFailed = !deleteTrashThroughRename(f);
                            if (deletionFailed) {
                                // Re-open folder
                                f.open(Folder.READ_WRITE);
                            }
                            clearMessages = deletionFailed;
                        }
                        if (clearMessages) {
                            if (msgCount <= 0) {
                                // Empty folder
                                return;
                            }

                            // Just clear folder in "fire & forget" fashion
                            if (!marked) {
                                marked = setMarker(opKey, f);
                            }
                            expungeFor(f, manyMessages);
                        }
                        return;
                    }

                    // Regular clear: Move messages to trash (if any)
                    if (msgCount <= 0) {
                        // Empty folder
                        return;
                    }

                    marked = setMarker(opKey, f);
                    boolean supportsMove = imapConfig.asMap().containsKey("MOVE");
                    int blockSize = imapConfig.getIMAPProperties().getBlockSize();
                    if (blockSize > 0) {
                        // Block-wise deletion
                        while (msgCount > blockSize) {
                            // Don't adapt sequence number since folder expunge already resets message numbering
                            boolean deleteRequired = true;
                            if (backup) {
                                try {
                                    if (supportsMove) {
                                        new MoveIMAPCommand(f, 1, blockSize, trashFullname).doCommand();
                                        deleteRequired = false;
                                    } else {
                                        new CopyIMAPCommand(f, 1, blockSize, trashFullname).doCommand();
                                    }
                                } catch (MessagingException e) {
                                    if (e.getMessage().indexOf("Over quota") > -1) {
                                        // Over-Quota-Exception
                                        throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e);
                                    }
                                    final Exception nestedExc = e.getNextException();
                                    if (nestedExc != null) {
                                        if (nestedExc.getMessage().indexOf("Over quota") > -1) {
                                            // Over-Quota-Exception
                                            throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e);
                                        }
                                        if (nestedExc instanceof ProtocolException pe) {
                                            OXException oxe = MimeMailException.handleProtocolExceptionByResponseCode(pe, imapConfig, session, f);
                                            if (null != oxe) {
                                                throw oxe;
                                            }
                                        }
                                    }
                                    throw IMAPException.create(IMAPException.Code.MOVE_ON_DELETE_FAILED, imapConfig, session, e);
                                }
                            }
                            if (deleteRequired) {
                                // Delete through storing \Deleted flag...
                                new FlagsIMAPCommand(f, 1, blockSize, FLAGS_DELETED, true, true).doCommand();
                                // ... and perform EXPUNGE
                                try {
                                    IMAPCommandsCollection.fastExpunge(f);
                                } catch (FolderClosedException | StoreClosedException e) {
                                    // Not possible to retry since connection is broken
                                    throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, imapConfig.getServer(), imapConfig.getLogin(), I(imapConfig.getIMAPProperties().getConnectTimeout()));
                                }
                            }
                            // Decrement
                            msgCount -= blockSize;
                        }
                    }
                    if (msgCount == 0) {
                        // All messages already cleared through previous block-wise deletion
                        return;
                    }
                    if (backup) {
                        try {
                            if (supportsMove) {
                                new MoveIMAPCommand(f, trashFullname).doCommand();
                                return;
                            }
                            new CopyIMAPCommand(f, trashFullname).doCommand();
                        } catch (MessagingException e) {
                            Exception nestedExc = e.getNextException();
                            if (nestedExc instanceof final CommandFailedException exc) {
                                if (exc.getMessage().indexOf("Over quota") > -1) {
                                    // Over-Quota-Exception
                                    throw MailExceptionCode.DELETE_FAILED_OVER_QUOTA.create(e);
                                }
                            }
                            if (nestedExc instanceof ProtocolException pe) {
                                OXException oxe = MimeMailException.handleProtocolExceptionByResponseCode(pe, imapConfig, session, f);
                                if (null != oxe) {
                                    throw oxe;
                                }
                            }
                            throw IMAPException.create(IMAPException.Code.MOVE_ON_DELETE_FAILED, imapConfig, session, e);
                        }
                    }
                    // Delete through storing \Deleted flag...
                    new FlagsIMAPCommand(f, FLAGS_DELETED, true, true).doCommand();
                    // ... and perform EXPUNGE
                    final long start = System.currentTimeMillis();
                    IMAPCommandsCollection.fastExpunge(f);
                    mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                } finally {
                    closeSafe(f);
                    if (marked) {
                        unsetMarker(opKey);
                    }
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /**
     * Performs a fire-and-forget expunge for given IMAP folder.
     * <p>
     * <ol>
     * <li>Mark all contained messages as \Deleted</li>
     * <li>Issue <code>EXPUNGE</code> command</li>
     * </ol>
     *
     * @param f The IMAP folder
     * @param fireAndForget Whether to perform fire-and-forget or to await server response when <code>EXPUNGE</code> is issued
     * @throws OXException If operation fails
     */
    private void expungeFor(IMAPFolder f, boolean fireAndForget) throws OXException {
        try {
            // Set \Deleted flag on all contained messages
            new FlagsIMAPCommand(f, FLAGS_DELETED, true, true).doCommand();

            // Issue the EXPUNGE command...
            if (fireAndForget) {
                // ... with fire-and-forget marker
                LogProperties.put(LogProperties.Name.IMAP_FIRE_AND_FORGET, "true");
                try {
                    IMAPCommandsCollection.fastExpunge(f);
                } catch (FolderClosedException | StoreClosedException e) {
                    com.sun.mail.iap.FireAndForgetException fireAndForgetException = ExceptionUtils.extractFrom(e, com.sun.mail.iap.FireAndForgetException.class);
                    if (fireAndForgetException == null) {
                        // Not possible to retry since connection is broken
                        throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, imapConfig.getServer(), imapConfig.getLogin(), I(imapConfig.getIMAPProperties().getConnectTimeout()));
                    }
                    // Expected...
                    LOG.debug("IMAP EXPUNGE command issued in \"fire & forget\" fashion. This has been performed intentionally and is nothing to worry about.", fireAndForgetException);
                } finally {
                    LogProperties.remove(LogProperties.Name.IMAP_FIRE_AND_FORGET);
                }
            } else {
                // ... without fire-and-forget marker
                try {
                    IMAPCommandsCollection.fastExpunge(f);
                } catch (FolderClosedException | StoreClosedException e) {
                    throw IMAPException.create(IMAPException.Code.CONNECT_ERROR, imapConfig, session, e, imapConfig.getServer(), imapConfig.getLogin(), I(imapConfig.getIMAPProperties().getConnectTimeout()));
                }
            }
        } catch (MessagingException e) {
            throw handleMessagingException(f.getFullName(), e);
        }
    }

    /**
     * Performs a delete-through-rename for specified IMAP folder.
     *
     * @param f The IMAP folder
     * @return <code>true</code> if successfully performed; otherwise <code>false</code>
     */
    private boolean deleteTrashThroughRename(IMAPFolder f) {
        String fullName = f.getFullName();
        boolean clearListLsubCache = false;
        try {
            // Check if current folder carries \Trash special-use annotation
            Map<String, String> caps = imapConfig.asMap();
            boolean hasMetadata = caps.containsKey(IMAPCapabilities.CAP_METADATA); // http://tools.ietf.org/html/rfc5464#section-1
            boolean hasCreateSpecialUse = caps.containsKey("CREATE-SPECIAL-USE"); // http://tools.ietf.org/html/rfc6154#section-3
            boolean isSpecialUseTrashFolder = false;
            if (caps.containsKey("SPECIAL-USE")) {
                // Supports SPECIAL-USE capability
                Collection<ListLsubEntry> trashEntries = ListLsubCache.getTrashEntry(accountId, f, session, imapConfig.getIMAPProperties());
                if (!trashEntries.isEmpty()) {
                    for (Iterator<ListLsubEntry> it = trashEntries.iterator(); !isSpecialUseTrashFolder && it.hasNext();) {
                        ListLsubEntry entry = it.next();
                        if (fullName.equals(entry.getFullName())) {
                            isSpecialUseTrashFolder = true;
                        }
                    }
                }
            }

            // Prepare renaming
            IMAPFolder parent = (IMAPFolder) f.getParent();
            char separator = getSeparator(parent);
            String parentFullName = parent.getFullName();
            StringBuilder tmp = new StringBuilder();
            if (parentFullName.length() > 0) {
                tmp.append(parentFullName).append(separator);
            }
            Optional<String> optPurgeFolder = optSpecialPurgeFolder();
            if (optPurgeFolder.isPresent()) {
                tmp.append(optPurgeFolder.get()).append(separator);
            }
            tmp.append(UUIDs.getUnformattedStringFromRandom());
            IMAPFolder renameFolder = (IMAPFolder) imapStore.getFolder(tmp.toString());
            tmp = null;

            // Remember ACLs
            Optional<ACL[]> optInitialACLs = Optional.empty();
            if (imapConfig.isSupportsACLs()) {
                optInitialACLs = getACLSafe(f);
            }

            // Drop SPECIAL-USE (if necessary)
            if (isSpecialUseTrashFolder && hasMetadata && isAllowedToSetSpecialUse()) {
                IMAPCommandsCollection.dropSpecialUses(f);
            }

            // Rename...
            IMAPCommandsCollection.renameFolder(f, separator, renameFolder);
            clearListLsubCache = true;

            // Re-create folder
            boolean mboxEnabled = MBoxEnabledCache.isMBoxEnabled(imapConfig, parent, new StringBuilder(parent.getFullName()).append(separator).toString());
            int ftype = mboxEnabled ? Folder.HOLDS_MESSAGES : FOLDER_TYPE;
            IMAPFolder createMe;
            if (ROOT_FOLDER_ID.equals(parentFullName)) {
                createMe = getIMAPFolder(f.getName());
            } else {
                createMe = (IMAPFolder) imapStore.getFolder(new StringBuilder(parent.getFullName()).append(separator).append(f.getName()).toString());
            }
            try { // NOSONARLINT
                if (isSpecialUseTrashFolder && isAllowedToSetSpecialUse()) {
                    if (hasCreateSpecialUse) {
                        List<String> trashSpecialUse = Collections.singletonList("\\Trash");
                        IMAPCommandsCollection.createFolder(createMe, separator, ftype, false, trashSpecialUse);
                    } else {
                        if (!createMe.create(ftype, true)) {
                            // Revert rename
                            IMAPCommandsCollection.renameFolder(renameFolder, separator, f);
                            return false;
                        }
                        if (hasMetadata) {
                            List<String> trashSpecialUse = Collections.singletonList("\\Trash");
                            IMAPCommandsCollection.setSpecialUses(f, trashSpecialUse);
                        }
                    }
                } else {
                    if (!createMe.create(ftype, true)) {
                        // Revert rename
                        IMAPCommandsCollection.renameFolder(renameFolder, separator, f);
                        return false;
                    }
                }
            } catch (MessagingException e) {
                if (!"Unsupported type".equals(e.getMessage())) {
                    // Revert rename
                    IMAPCommandsCollection.renameFolder(renameFolder, separator, f);
                    return false;
                }
                LOG.warn("IMAP folder creation failed due to unsupported type. Going to retry with fallback type HOLDS-MESSAGES.", e);
                if (!createMe.create(Folder.HOLDS_MESSAGES)) {
                    // Revert rename
                    IMAPCommandsCollection.renameFolder(renameFolder, separator, f);
                    return false;
                }
                LOG.info("IMAP folder created with fallback type HOLDS_MESSAGES");
            }

            // Apply previous ACLs
            if (optInitialACLs.isPresent()) {
                ACL[] initialACLs = optInitialACLs.get();
                if (initialACLs.length > 1) {
                    for (ACL acl : initialACLs) {
                        try {
                            createMe.addACL(acl);
                        } catch (Exception e) {
                            LOG.warn("Failed to restore ACL '{}' on newly created folder {}", acl, createMe.getFullName(), e);
                        }
                    }
                }
            }

            if (optPurgeFolder.isEmpty()) {
                deleteRenamedFolder(renameFolder);
            }

            return true;
        } catch (Exception e) {
            // Delete-through-rename failed for any reason
            LOG.warn("Failed to delete IMAP folder {} through rename-and-recreate cycle. Performing clearing the regular way...", fullName, e);
            return false;
        } finally {
            if (clearListLsubCache) {
                ListLsubCache.clearCache(accountId, session);
            }
        }
    }

    /**
     * Deletes the renamed folder
     *
     * @param renameFolder The renamed trash folder
     * @throws MessagingException
     */
    private void deleteRenamedFolder(IMAPFolder renameFolder) throws MessagingException {
        // Delete renamed folder (preferably asynchronous)
        ThreadPoolService threadPool = Services.optService(ThreadPoolService.class);
        if (threadPool == null) {
            // No thread pool available
            renameFolder.delete(true);
            return;
        }
        String toDeleteFullName = renameFolder.getFullName();
        Optional<IMAPStore> optNewImapStore = newConnectedImapStore();
        if (optNewImapStore.isEmpty()) {
            // Failed to establish a new connected IMAP store
            renameFolder.delete(true);
            return;
        }
        threadPool.submit(new IMAPFolderDeleteTask(optNewImapStore.get(), toDeleteFullName), CallerRunsBehavior.<Void> getInstance());
    }

    private static final String PURGE_FOLDER_PROPERTY = "com.openexchange.imap.purgeFolder";

    /**
     * Gets the optional purge folder to use for trash deletion.
     *
     * @return The optional purge folder
     */
    private Optional<String> optSpecialPurgeFolder() {
        ConfigViewFactory configViewFactory = Services.getService(ConfigViewFactory.class);
        if (null == configViewFactory) {
            return Optional.empty();
        }
        try {
            ConfigView view = configViewFactory.getView(session.getUserId(), session.getContextId());
            String value = view.opt(PURGE_FOLDER_PROPERTY, String.class, null);
            return Optional.ofNullable(Strings.isEmpty(value) ? null : value.trim());
        } catch (OXException e) {
            LOG.error("", e);
            return Optional.empty();
        }
    }

    private static final String SET_SPECIAL_USE_PROPERTY = "com.openexchange.imap.setSpecialUseFlags";

    private boolean isAllowedToSetSpecialUse() {
        boolean setSpecialUseFlags = false;
        ConfigViewFactory configViewFactory = Services.getService(ConfigViewFactory.class);
        if (null != configViewFactory) {
            try {
                ConfigView view = configViewFactory.getView(session.getUserId(), session.getContextId());
                ComposedConfigProperty<Boolean> property = view.property(SET_SPECIAL_USE_PROPERTY, Boolean.class);
                Boolean b = property.get();
                if (null != b) {
                    setSpecialUseFlags = b.booleanValue();
                }
            } catch (OXException e) {
                LOG.error("", e);
                // continue with default value
            }
        }
        return setSpecialUseFlags;
    }

    private Optional<IMAPStore> newConnectedImapStore() {
        try {
            return Optional.of(imapAccess.newConnectedImapStore());
        } catch (Exception e) {
            LOG.warn("Failed to establish new connected IMAP store", e);
            return Optional.empty();
        }
    }

    /**
     * Whether delete-through-rename should be performed.
     *
     * @param f The affected IMAP folder
     * @param isTrash Whether IMAP folder is standard trash folder or is a subfolder of it
     * @param manyMessages Whether IMAP folder is considered to hold many messages
     * @return <code>true</code> to perform delete-through-rename; otherwise <code>false</code>
     */
    private boolean allowDeleteTrashThroughRename(IMAPFolder f, boolean isTrash, boolean manyMessages) {
        // Only allow for trash folder containing many messages
        return isAllowDeleteTrashThroughRename() && isTrash && manyMessages;
    }

    private static final String DELETE_TRASH_THROUGH_RENAME_PROPERTY = "com.openexchange.imap.allowDeleteTrashThroughRename";

    private boolean isAllowDeleteTrashThroughRename() {
        boolean allowDeleteTrashThroughRename = false;
        ConfigViewFactory configViewFactory = Services.getService(ConfigViewFactory.class);
        if (null != configViewFactory) {
            try {
                ConfigView view = configViewFactory.getView(session.getUserId(), session.getContextId());
                ComposedConfigProperty<Boolean> property = view.property(DELETE_TRASH_THROUGH_RENAME_PROPERTY, Boolean.class);
                Boolean b = property.get();
                if (null != b) {
                    allowDeleteTrashThroughRename = b.booleanValue();
                }
            } catch (OXException e) {
                LOG.error("", e);
                // continue with default value
            }
        }
        return allowDeleteTrashThroughRename;
    }

    private boolean isSelectable(IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(imapFolder).canOpen();
    }

    @Override
    public MailFolder[] getPath2DefaultFolder(String fullName) throws OXException {
        try {
            if (fullName.equals(ROOT_FOLDER_ID)) {
                return EMPTY_PATH;
            }
            IMAPFolder f = getIMAPFolder(fullName);
            if (!doesExist(f, true)) {
                f = checkForNamespaceFolder(fullName);
                if (null == f) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
            }
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            if (imapConfig.isSupportsACLs() && isSelectable(f)) {
                try {
                    if (!imapConfig.getACLExtension().canLookUp(RightsCache.getCachedRights(f, true, session, accountId))) {
                        throw IMAPException.create(IMAPException.Code.NO_LOOKUP_ACCESS, imapConfig, session, fullName);
                    }
                } catch (MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                }
            }
            List<MailFolder> list = new ArrayList<>();
            String defaultFolder = "";
            for (String fn; !(fn = f.getFullName()).equals(defaultFolder);) {
                list.add(FolderUtility.loadFolder(fn, this));
                f = (IMAPFolder) f.getParent();
            }
            return list.toArray(new MailFolder[list.size()]);
        } catch (MessagingException e) {
            throw handleMessagingException(fullName, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public String getDefaultFolderPrefix() throws OXException {
        final String urlName = urlName("ns-", imapConfig.getServer(), imapConfig.getPort(), imapConfig.getLogin(), imapConfig.getPassword());
        String defaultFolderPrefix = (String) session.getParameter(urlName);
        if (null == defaultFolderPrefix) {
            defaultFolderPrefix = getDefaultFolderPrefix0();
            session.setParameter(urlName, defaultFolderPrefix);
        }
        return defaultFolderPrefix;
    }

    private String getDefaultFolderPrefix0() throws OXException {
        try {
            // Special handling for GMail...
            {
                final String server = Strings.asciiLowerCase(imapConfig.getServer());
                if ("imap.gmail.com".equals(server) || "imap.googlemail.com".equals(server)) {
                    /*
                     * Look-up special GMail folder: [GMail], [Google Mail], ...
                     */
                    ListLsubEntry rootEntry = ListLsubCache.getCachedLISTEntry("", accountId, imapStore, session, imapConfig.getIMAPProperties());
                    List<ListLsubEntry> children = rootEntry.getChildren();
                    String prefix = "[G";
                    for (ListLsubEntry child : children) {
                        String fullName = child.getFullName();
                        if (fullName.startsWith(prefix)) {
                            return fullName + child.getSeparator();
                        }
                    }
                }
            }

            // Try NAMESPACE command
            char separator;
            boolean detectedByNamespace = false;
            String prefix = null;
            try {
                List<Namespace> namespaces = NamespacesCache.getPersonalNamespaces(imapStore, true, session, accountId);
                if (namespaces.isEmpty()) {
                    // No namespaces available
                    // Determine separator character
                    separator = ListLsubCache.getSeparator(accountId, (DefaultFolder) imapStore.getDefaultFolder(), session, imapConfig.getIMAPProperties());
                    setAndGetSeparator(separator);
                    String prefixByInferiors = prefixByInferiors(separator);
                    LOG.info("IMAP server {} does not provide a personal namespace for login {}. Using fall-back \"by inferiors\" detection: \"{}\" (user={}, context={})", imapConfig.getServer(), imapConfig.getLogin(), prefixByInferiors, Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
                    return prefixByInferiors;
                }
                prefix = namespaces.get(0).getPrefix();
                separator = namespaces.get(0).getDelimiter();
                setAndGetSeparator(separator);
                detectedByNamespace = true;
            } catch (MessagingException e) {
                // Determine separator character
                separator = ListLsubCache.getSeparator(accountId, (DefaultFolder) imapStore.getDefaultFolder(), session, imapConfig.getIMAPProperties());
                setAndGetSeparator(separator);
                String prefixByInferiors = prefixByInferiors(separator);
                LOG.info("NAMESPACE command failed for any reason on IMAP server {} for login {}. Using fall-back \"by inferiors\" detection: \"{}\" (user={}, context={})", imapConfig.getServer(), imapConfig.getLogin(), prefixByInferiors, Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()), e);
                return prefixByInferiors;
            }

            if (prefix.length() != 0) {
                return prefix;
            }

            // The empty prefix so far; verify against root-folder capability
            DefaultFolder defaultFolder = (DefaultFolder) imapStore.getDefaultFolder();
            if (!RootSubfoldersEnabledCache.isRootSubfoldersEnabled(imapConfig, defaultFolder, session)) {
                // Impossible to create folders on root level; hence adjust prefix to be: "INBOX" + <separator>
                if (detectedByNamespace) {
                    // Strange... Since NAMESPACE tells to use root level, but IMAP server denies to create such folders.
                    LOG.warn("\n\n\tNAMESPACE from IMAP server {} indicates to use root level for login {}, but IMAP server denies to create such folders!\n", imapConfig.getServer(), imapConfig.getLogin());
                }
                return new StringBuilder(STR_INBOX).append(separator).toString();
            }

            // Grant empty prefix as standard folder prefix
            return prefix;
        } catch (MessagingException e) {
            throw handleMessagingException(e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private String prefixByInferiors(char separator) throws OXException {
        try {
            final DefaultFolder defaultFolder = (DefaultFolder) imapStore.getDefaultFolder();
            if (!RootSubfoldersEnabledCache.isRootSubfoldersEnabled(imapConfig, defaultFolder, session) || MailProperties.getInstance().isAllowNestedDefaultFolderOnAltNamespace()) {
                return new StringBuilder(STR_INBOX).append(separator).toString();
            }
            return "";
        } catch (MessagingException e) {
            throw handleMessagingException(e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    @Override
    public String getConfirmedHamFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_CONFIRMED_HAM);
    }

    @Override
    public String getConfirmedSpamFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_CONFIRMED_SPAM);
    }

    @Override
    public String getDraftsFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_DRAFTS);
    }

    @Override
    public String getSentFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_SENT);
    }

    @Override
    public String getSpamFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_SPAM);
    }

    @Override
    public String getTrashFolder() throws OXException {
        return getChecker().getDefaultFolder(StorageUtility.INDEX_TRASH);
    }

    @Override
    public void releaseResources() throws IMAPException {
        // Nothing to release
    }

    @Override
    public com.openexchange.mail.Quota[] getQuotas(String folder, com.openexchange.mail.Quota.Type[] types) throws OXException {
        try {
            final String fullName = folder == null ? STR_INBOX : folder;
            final boolean isRootFolder = fullName.equals(ROOT_FOLDER_ID);
            final IMAPFolder f = (IMAPFolder) (isRootFolder ? imapStore.getDefaultFolder() : imapStore.getFolder(fullName));
            /*
             * Obtain folder lock once to avoid multiple acquire/releases when invoking folder's getXXX() methods
             */
            synchronized (f) {
                if (!isRootFolder && !doesExist(f, true)) {
                    throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
                }
                try {
                    if (!isSelectable(f)) {
                        throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_MESSAGES, imapConfig, session, fullName);
                    }
                    if (imapConfig.isSupportsACLs()) {
                        final Rights myrights = RightsCache.getCachedRights(f, true, session, accountId);
                        if (!imapConfig.getACLExtension().canRead(myrights)) {
                            throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, fullName);
                        }
                    }
                } catch (MessagingException e) {
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
                }
                if (!imapConfig.getImapCapabilities().hasQuota()) {
                    return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                }
                Quota[] folderQuota = null;
                try {
                    final long start = System.currentTimeMillis();
                    folderQuota = f.getQuota();
                    mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                } catch (MessagingException mexc) {
                    if (mexc.getNextException() instanceof ParsingException) {
                        try {
                            final long start = System.currentTimeMillis();
                            folderQuota = IMAPCommandsCollection.getQuotaRoot(f);
                            mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
                        } catch (MessagingException inner) {
                            LOG.warn("", inner);
                            return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                        }
                    } else {
                        throw mexc;
                    }
                }
                if (folderQuota == null || folderQuota.length == 0) {
                    return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                }
                final Quota.Resource[] resources = folderQuota[0].resources;
                if (resources == null || resources.length == 0) {
                    return com.openexchange.mail.Quota.getUnlimitedQuotas(types);
                }
                final com.openexchange.mail.Quota[] quotas = new com.openexchange.mail.Quota[types.length];
                for (int i = 0; i < types.length; i++) {
                    final String typeStr = types[i].toString();
                    /*
                     * Find corresponding resource to current type
                     */
                    Resource resource = null;
                    for (int k = 0; k < resources.length && resource == null; k++) {
                        if (typeStr.equalsIgnoreCase(resources[k].name)) {
                            resource = resources[k];
                        }
                    }
                    if (resource == null) {
                        /*
                         * No quota limitation found that applies to current resource type
                         */
                        quotas[i] = com.openexchange.mail.Quota.getUnlimitedQuota(types[i]);
                    } else {
                        quotas[i] = new com.openexchange.mail.Quota(resource.limit, resource.usage, types[i]);
                    }
                }
                return quotas;
            }
        } catch (MessagingException e) {
            throw handleMessagingException(folder, e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /*
     * ++++++++++++++++++ Helper methods ++++++++++++++++++
     */

    private void unsetMarker(OperationKey key) {
        OperationKey.unsetMarker(key, session);
    }

    private boolean setMarker(OperationKey key, Folder imapFolder) throws OXException {
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

    private static void closeSafe(Folder folder) {
        if (null != folder) {
            try {
                folder.close(false);
            } catch (@SuppressWarnings("unused") Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Get the ACL list of specified folder
     *
     * @param imapFolder The IMAP folder
     * @return The ACL list or <code>null</code> if any error occurred
     */
    private static Optional<ACL[]> getACLSafe(IMAPFolder imapFolder) {
        try {
            return Optional.ofNullable(imapFolder.getACL());
        } catch (MessagingException e) {
            LOG.debug("", e);
            return Optional.empty();
        }
    }

    /**
     * Get the ACL list of specified folder
     *
     * @param fullName The full name denoting the folder
     * @param imapFolder The IMAP folder provding the protocol to use
     * @return The ACL list or <code>null</code> if any error occurred
     */
    private static Optional<ACL[]> getACLSafe(String fullName, IMAPFolder imapFolder) {
        try {
            return Optional.ofNullable(IMAPCommandsCollection.getACL(fullName, imapFolder));
        } catch (MessagingException e) {
            LOG.debug("", e);
            return Optional.empty();
        }
    }

    private void deleteFolder(IMAPFolder deleteMe, char separator) throws OXException, MessagingException {
        final String fullName = deleteMe.getFullName();
        if (getChecker().isDefaultFolder(fullName, true)) {
            throw IMAPException.create(IMAPException.Code.NO_DEFAULT_FOLDER_DELETE, imapConfig, session, fullName);
        } else if (!doesExist(deleteMe, false)) {
            throw IMAPException.create(IMAPException.Code.FOLDER_NOT_FOUND, imapConfig, session, fullName);
        }
        try {
            if (imapConfig.isSupportsACLs() && ((deleteMe.getType() & Folder.HOLDS_MESSAGES) > 0) && !imapConfig.getACLExtension().canDeleteMailbox(
                RightsCache.getCachedRights(deleteMe, true, session, accountId))) {
                throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, fullName);
            }
        } catch (MessagingException e) {
            throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, fullName);
        }
        if (deleteMe.isOpen()) {
            deleteMe.close(false);
        }
        Optional<ACL[]> optOldACLs = imapConfig.isSupportsACLs() ? getACLSafe(deleteMe) : Optional.empty();
        /*
         * Unsubscribe prior to deletion
         */
        IMAPCommandsCollection.forceSetSubscribed(imapStore, fullName, false);
        // Unsubscribe subfolders
        ListInfo[] subSubscriptions = IMAPCommandsCollection.listAllSubfolders(fullName, separator, deleteMe);
        for (ListInfo info : subSubscriptions) {
            IMAPCommandsCollection.forceSetSubscribed(imapStore, info.name, false);
        }

        removeSessionData(deleteMe.getFullName(), separator, deleteMe);
        final long start = System.currentTimeMillis();
        if (!deleteMe.delete(true)) {
            throw IMAPException.create(IMAPException.Code.DELETE_FAILED, imapConfig, session, fullName);
        }
        mailInterfaceMonitor.addUseTime(System.currentTimeMillis() - start);
        /*
         * Remove cache entries
         */
        RightsCache.removeCachedRights(deleteMe, session, accountId);
        UserFlagsCache.removeUserFlags(deleteMe, session, accountId);
        // Affected users, too
        dropListLsubCachesForOther(optOldACLs, fullName);
    }

    private boolean stillHoldsFullRights(IMAPFolder defaultFolder, ACL[] newACLs, ACLExtension aclExtension) throws OXException {
        /*
         * Ensure that owner still holds full rights
         */
        final String ownerACLName =
            getEntity2ACL().getACLName(session.getUserId(), ctx, IMAPFolderConverter.getEntity2AclArgs(session, defaultFolder, imapConfig));
        final Rights fullRights = aclExtension.getFullRights();
        for (ACL newACL : newACLs) {
            if (newACL.getName().equals(ownerACLName) && newACL.getRights().contains(fullRights)) {
                return true;
            }
        }
        return false;
    }

    private boolean stillHoldsAdministerRights(IMAPFolder imapFolder, ACL[] newACLs, ACLExtension aclExtension) throws OXException {
        /*
         * Ensure that owner still holds full rights
         */
        String ownerACLName = getEntity2ACL().getACLName(session.getUserId(), ctx, IMAPFolderConverter.getEntity2AclArgs(session, imapFolder, imapConfig));
        for (ACL newACL : newACLs) {
            if (newACL.getName().equals(ownerACLName) && aclExtension.containsFolderAdminRights(newACL.getRights())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Boolean> getSubscriptionStatus(IMAPFolder f, char separator, String oldFullName, String newFullName) throws MessagingException, OXException {
        final Map<String, Boolean> retval = new HashMap<>();
        getSubscriptionStatus(retval, f.getFullName(), separator, f, oldFullName, newFullName);
        return retval;
    }

    private void getSubscriptionStatus(Map<String, Boolean> m, String fullName, char separator, IMAPFolder f, String oldFullName, String newFullName) throws MessagingException, OXException {
        for (ListInfo listInfo : IMAPCommandsCollection.listSubfolders(fullName, separator, f)) {
            getSubscriptionStatus(m, listInfo.name, separator, f, oldFullName, newFullName);
        }

        ListLsubEntry testEntry = ListLsubCache.getCachedLISTEntry(fullName, accountId, f, session, imapConfig.getIMAPProperties());
        m.put(fullName.replaceFirst(Pattern.quote(oldFullName), quoteReplacement(newFullName)), Boolean.valueOf(testEntry.isSubscribed()));
    }

    private void setFolderSubscription(String fullName, IMAPFolder f, boolean subscribed) throws MessagingException, OXException {
        ListLsubEntry testEntry = ListLsubCache.getCachedLISTEntry(fullName, accountId, f, session, imapConfig.getIMAPProperties());
        if (testEntry.hasInferiors()) {
            for (ListInfo listInfo : IMAPCommandsCollection.listSubfolders(fullName, testEntry.getSeparator(), f)) {
                setFolderSubscription(listInfo.name, f, subscribed);
            }
        }
        IMAPCommandsCollection.forceSetSubscribed(imapStore, fullName, subscribed);
    }

    private void applySubscriptionStatus(String fullName, char separator, IMAPFolder f, Map<String, Boolean> m) throws MessagingException, OXException {
        for (ListInfo listInfo : IMAPCommandsCollection.listSubfolders(fullName, separator, f)) {
            applySubscriptionStatus(listInfo.name, separator, f, m);
        }

        Boolean b = m.get(fullName);
        if (b == null) {
            LOG.warn("No stored subscription status found for {}", fullName);
            b = Boolean.TRUE;
        }
        IMAPCommandsCollection.forceSetSubscribed(imapStore, fullName, b.booleanValue());
    }

    private IMAPFolder moveFolder(IMAPFolder toMove, IMAPFolder destFolder, String folderName) throws MessagingException, OXException {
        String name = folderName;
        if (name == null) {
            name = getNameOf(toMove);
        }
        final String destFullname = destFolder.getFullName();
        final IMAPFolder newFolder;
        {
            final StringBuilder sb = new StringBuilder();
            if (destFullname.length() > 0) {
                sb.append(destFullname).append(getSeparator(destFolder));
            }
            sb.append(name);
            newFolder = (IMAPFolder) imapStore.getFolder(sb.toString());
        }
        return moveFolder(toMove, destFolder, newFolder, true);
    }

    private IMAPFolder moveFolder(IMAPFolder toMove, IMAPFolder destFolder, IMAPFolder newFolder, boolean checkForDuplicate) throws MessagingException, OXException {
        String destFullName = destFolder.getFullName();
        char separator;
        {
            ListLsubEntry listEntry = getLISTEntry(destFullName, destFolder);
            if (!listEntry.hasInferiors()) {
                throw IMAPException.create(IMAPException.Code.FOLDER_DOES_NOT_HOLD_FOLDERS, imapConfig, session, destFullName);
            }
            separator = listEntry.getSeparator();
        }

        String moveFullname = toMove.getFullName();
        if (imapConfig.isSupportsACLs() && (getLISTEntry(moveFullname, toMove).hasInferiors())) {
            try {
                if (!imapConfig.getACLExtension().canRead(RightsCache.getCachedRights(toMove, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_READ_ACCESS, imapConfig, session, moveFullname);
                }
            } catch (MessagingException e) {
                throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, moveFullname);
            }
            try {
                if (!imapConfig.getACLExtension().canCreate(RightsCache.getCachedRights(toMove, true, session, accountId))) {
                    throw IMAPException.create(IMAPException.Code.NO_CREATE_ACCESS, imapConfig, session, moveFullname);
                }
            } catch (MessagingException e) {
                /*
                 * MYRIGHTS command failed for given mailbox
                 */
                if (!imapConfig.getImapCapabilities().hasNamespace() || !NamespacesCache.containedInPersonalNamespaces(moveFullname, imapStore, true, session, accountId)) {
                    /*
                     * No namespace support or given parent is NOT covered by user's personal namespaces.
                     */
                    throw IMAPException.create(IMAPException.Code.NO_ACCESS, imapConfig, session, e, moveFullname);
                }
                LOG.debug("MYRIGHTS command failed on namespace folder", e);
            }
        }
        /*
         * Move by creating a new folder, copying all messages and deleting old folder
         */
        if (checkForDuplicate && newFolder.exists()) {
            throw IMAPException.create(IMAPException.Code.DUPLICATE_FOLDER, imapConfig, session, getNameOf(newFolder));
        }
        /*
         * Examine
         */
        Set<String> entityNames = imapConfig.isSupportsACLs() ? new HashSet<>(8) : null;
        Set<String> oldFullNames = new HashSet<>(8);
        Map<String, Boolean> subscriptions = new HashMap<>(8);
        gatherFolderInfo(toMove.getFullName(), toMove, moveFullname.length(), newFolder.getFullName(), subscriptions, oldFullNames, entityNames, new StringBuilder(32));
        /*-
         * Check if move operation may be executed through a RENAME command
         * (requires target and destination reside in the same namespace)
         */
        if (isSameNamespace(toMove.getFullName(), newFolder.getFullName())) {
            /*
             * Perform RENAME
             */
            boolean throwException = true;
            try {
                long start = System.currentTimeMillis();
                IMAPCommandsCollection.renameFolder(toMove, separator, newFolder);
                long duration = System.currentTimeMillis() - start;
                throwException = false;
                mailInterfaceMonitor.addUseTime(duration);
            } catch (MessagingException e) {
                // Rename failed
                throwException = false;
                throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, e, newFolder.getFullName(), destFolder instanceof DefaultFolder ? ROOT_FOLDER_NAME : destFullName);
            }
            if (throwException) {
                throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, newFolder.getFullName(), destFolder instanceof DefaultFolder ? ROOT_FOLDER_NAME : destFullName);
            }
        } else {
            /*
             * Perform CREATE for each folder in line, copy its messages and DELETE afterwards
             */
            LinkedList<IMAPFolder> createdOnes = new LinkedList<>();
            LinkedList<IMAPFolder> copiedOnes = new LinkedList<>();
            boolean error = true;
            try {
                doRecursiveCopy(toMove, newFolder, setAndGetSeparator(toMove.getSeparator()), copiedOnes, createdOnes);
                error = false;

                for (IMAPFolder iapf : copiedOnes) {
                    deleteFolderSafe(iapf);
                }
            } finally {
                if (error) {
                    for (IMAPFolder iapf : createdOnes) {
                        deleteFolderSafe(iapf);
                    }
                }
            }
        }
        /*
         * Apply original subscription status
         */
        for (Entry<String, Boolean> entry : subscriptions.entrySet()) {
            String fullName = entry.getKey();
            try {
                imapStore.getFolder(fullName).setSubscribed(entry.getValue().booleanValue());
            } catch (Exception e) {
                // Restoring subscription status failed
                LOG.warn("Could not restore subscription status for folder {} during IMAP folder move operation", fullName, e);
            }
        }
        /*
         * Delete/unsubscribe old folder
         */
        for (String oldFullName : oldFullNames) {
            IMAPCommandsCollection.forceSetSubscribed(imapStore, oldFullName, false);
        }
        /*
         * Notify message storage
         */
        imapAccess.getMessageStorage().notifyIMAPFolderModification(moveFullname);
        /*
         * Remove cache entries
         */
        RightsCache.removeCachedRights(moveFullname, session, accountId);
        UserFlagsCache.removeUserFlags(moveFullname, session, accountId);
        if (IMAPSessionStorageAccess.isEnabled()) {
            IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, moveFullname);
        }
        // Affected users, too
        dropListLsubCachesForOther(entityNames, moveFullname);

        return newFolder;
    }

    private void doRecursiveCopy(IMAPFolder toCopy, IMAPFolder newFolder, char separator, LinkedList<IMAPFolder> copiedOnes, LinkedList<IMAPFolder> createdOnes) throws MessagingException, OXException {
        if (!newFolder.create(toCopy.getType())) {
            Folder destFolder = newFolder.getParent();
            throw IMAPException.create(IMAPException.Code.FOLDER_CREATION_FAILED, imapConfig, session, newFolder.getFullName(), destFolder instanceof DefaultFolder ? ROOT_FOLDER_NAME : destFolder.getFullName());
        }

        // Remember as created
        createdOnes.addFirst(newFolder);

        // Align ACLs (if not moved to trash)
        if (!isSubfolderOf(newFolder.getFullName(), getTrashFolder(), separator)) {
            Optional<ACL[]> optAcls = getACLSafe(toCopy);
            if (optAcls.isPresent()) {
                Entity2ACL entity2ACL = getEntity2ACL();

                for (ACL acl : optAcls.get()) {
                    String name = acl.getName();
                    Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session, toCopy, imapConfig);
                    try {
                        int entityId = entity2ACL.getEntityID(name, ctx, args).getId();

                        args = IMAPFolderConverter.getEntity2AclArgs(session, newFolder, imapConfig);
                        name = entity2ACL.getACLName(entityId, ctx, args);

                        ACL newAcl = new ACL(name, acl.getRights());
                        newFolder.addACL(newAcl);
                    } catch (OXException e) {
                        if (!Entity2ACLExceptionCode.RESOLVE_USER_FAILED.equals(e)) {
                            throw e;
                        }

                        // Take over as-is
                        newFolder.addACL(acl);
                    }
                }
            }
        }

        // Copy all messages
        if (toCopy.getMessageCount() > 0) {
            toCopy.open(Folder.READ_ONLY);
            try {
                new CopyIMAPCommand(toCopy, newFolder.getFullName()).doCommand();
            } finally {
                toCopy.close(false);
            }
        }

        // Remember as copied
        copiedOnes.addFirst(toCopy);

        // Check for subfolders
        for (Folder subfolder : toCopy.list()) {
            IMAPFolder newSubfolder = (IMAPFolder) imapStore.getFolder(newFolder.getFullName() + separator + subfolder.getName());
            doRecursiveCopy((IMAPFolder) subfolder, newSubfolder, separator, copiedOnes, createdOnes);
        }
    }

    private static void deleteFolderSafe(IMAPFolder imapFolder) {
        if (null != imapFolder) {
            try {
                imapFolder.delete(false);
            } catch (Exception x) {
                LOG.error("Temporary created folder could not be deleted: {}", imapFolder.getFullName(), x);
            }
        }
    }

    private void gatherFolderInfo(String fullName, IMAPFolder f, int oldPathLen, String newPath, Map<String, Boolean> subscriptions, Set<String> oldFullNames, Set<String> entityNames, StringBuilder sb) throws MessagingException, OXException {
        if (null != entityNames) {
            extractEntityNames(getACLSafe(fullName, f), entityNames);
        }
        ListLsubEntry testEntry = ListLsubCache.getCachedLISTEntry(fullName, accountId, f, session, imapConfig.getIMAPProperties());
        {
            oldFullNames.add(fullName);
            sb.setLength(0);
            String nFullName = sb.append(newPath).append(fullName.substring(oldPathLen)).toString();
            subscriptions.put(nFullName, Boolean.valueOf(testEntry.isSubscribed()));
        }
        for (ListInfo listInfo : IMAPCommandsCollection.listSubfolders(fullName, testEntry.getSeparator(), f)) {
            gatherFolderInfo(listInfo.name, f, oldPathLen, newPath, subscriptions, oldFullNames, entityNames, sb);
        }
    }

    private ACL[] permissions2ACL(OCLPermission[] perms, IMAPFolder imapFolder) throws OXException {
        List<ACL> acls = new ArrayList<>(perms.length);
        Entity2ACLArgs entity2AclArgs = IMAPFolderConverter.getEntity2AclArgs(session, imapFolder, imapConfig);
        for (int i = 0; i < perms.length; i++) {
            ACLPermission aclPermission = getACLPermission(perms[i]);
            try {
                acls.add(aclPermission.getPermissionACL(entity2AclArgs, imapConfig, imapStore, ctx));
            } catch (OXException e) {
                if (Entity2ACLExceptionCode.UNKNOWN_USER.equals(e)) {
                    // Obviously the user is not known, skip
                    LOG.debug("User {} is not known on IMAP server \"{}\"", Integer.valueOf(aclPermission.getEntity()), imapConfig.getServer());
                } else if (UserExceptionCode.USER_NOT_FOUND.equals(e)) {
                    // Obviously the user is not known, skip
                    LOG.debug("User {} is not known on IMAP server \"{}\"", Integer.valueOf(aclPermission.getEntity()), imapConfig.getServer());
                } else {
                    throw e;
                }
            }
        }
        return acls.toArray(new ACL[acls.size()]);
    }

    private static ACLPermission getACLPermission(OCLPermission permission) {
        if (permission instanceof ACLPermission) {
            return (ACLPermission) permission;
        }
        ACLPermission retval = new ACLPermission();
        retval.setEntity(permission.getEntity());
        retval.setDeleteObjectPermission(permission.getDeletePermission());
        retval.setFolderAdmin(permission.isFolderAdmin());
        retval.setFolderPermission(permission.getFolderPermission());
        retval.setGroupPermission(permission.isGroupPermission());
        retval.setName(permission.getName());
        retval.setReadObjectPermission(permission.getReadPermission());
        retval.setSystem(permission.getSystem());
        retval.setWriteObjectPermission(permission.getWritePermission());
        return retval;
    }

    private static ACL[] getRemovedACLs(Map<String, ACL> newACLs, ACL[] oldACLs) {
        final List<ACL> retval = new ArrayList<>();
        for (ACL oldACL : oldACLs) {
            if (null == newACLs.get(oldACL.getName())) {
                retval.add(oldACL);
            }
        }
        return retval.toArray(new ACL[retval.size()]);
    }

    private static boolean isKnownEntity(String entity, Entity2ACL entity2ACL, Context ctx, Entity2ACLArgs args) {
        try {
            return !UserGroupID.NULL.equals(entity2ACL.getEntityID(entity, ctx, args));
        } catch (@SuppressWarnings("unused") OXException e) {
            return false;
        }
    }

    private boolean equals(ACL[] oldACLs, Map<String, ACL> newACLs, Entity2ACL entity2ACL, Entity2ACLArgs args) {
        int examined = 0;
        for (ACL oldACL : oldACLs) {
            final String oldName = oldACL.getName();
            if (isKnownEntity(oldName, entity2ACL, ctx, args)) {
                final ACL newACL = newACLs.get(oldName/* .toLowerCase(Locale.ENGLISH) */);
                if (null == newACL) {
                    // No corresponding entity in new ACLs
                    return false;
                }
                // Remember number of corresponding entities
                examined++;
                // Check ACLS' rights ignoring POST right
                if (!equalRights(oldACL.getRights().toString(), newACL.getRights().toString(), true)) {
                    return false;
                }
            }
        }
        return (examined == newACLs.size());
    }

    private void dropListLsubCachesForOther(Optional<ACL[]> optAcls, String fullName) throws MessagingException {
        dropListLsubCachesForOther(optAcls.orElse(null), fullName);
    }

    private void dropListLsubCachesForOther(ACL[] acls, String fullName) throws MessagingException {
        if (acls == null || 0 == acls.length) {
            return;
        }

        Namespaces namespaces = null;
        String myLogin = imapConfig.getLogin();
        int myUserId = session.getUserId();
        for (ACL acl : acls) {
            if (null != acl) {
                String entityName = acl.getName();
                if (null != entityName && !myLogin.equals(entityName) && !"owner".equals(entityName)) {
                    try {
                        if (namespaces == null) {
                            namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
                        }
                        Entity2ACLArgs args = new Entity2ACLArgsImpl(session, accountId, new StringBuilder(36).append(IDNA.toASCII(imapConfig.getServer())).append(':').append(imapConfig.getPort()).toString(), session.getUserId(), fullName, getSeparator(), namespaces, MailAccounts.isSecondaryAccount(accountId, session), imapConfig.getImapCapabilities().hasResolve());
                        List<UserGroupID> res = Entity2ACL.getInstance(imapStore, imapConfig).getEntityIDs(entityName, ctx, args);
                        for (UserGroupID user : res) {
                            int userId = user.getId();
                            if (userId >= 0 && user.isUser() && userId != myUserId) {
                                ListLsubCache.dropFor(userId, ctx.getContextId());
                                MailSessionCache.clearFor(userId, ctx.getContextId());
                            }
                        }
                    } catch (OXException e) {
                        LOG.debug("Could not resolve users for entity name {}", entityName, e);
                    }
                }
            }
        }
    }

    private void dropListLsubCachesForOther(Collection<String> entityNames, String fullName) throws MessagingException {
        if (null == entityNames || entityNames.isEmpty()) {
            return;
        }

        Namespaces namespaces = null;
        String myLogin = imapConfig.getLogin();
        int myUserId = session.getUserId();
        for (String entityName : entityNames) {
            if (null != entityName && !myLogin.equals(entityName) && !"owner".equals(entityName)) {
                try {
                    if (namespaces == null) {
                        namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
                    }
                    Entity2ACLArgs args = new Entity2ACLArgsImpl(session, accountId, new StringBuilder(36).append(IDNA.toASCII(imapConfig.getServer())).append(':').append(imapConfig.getPort()).toString(), session.getUserId(), fullName, getSeparator(), namespaces, MailAccounts.isSecondaryAccount(accountId, session), imapConfig.getImapCapabilities().hasResolve());
                    List<UserGroupID> res = Entity2ACL.getInstance(imapStore, imapConfig).getEntityIDs(entityName, ctx, args);
                    for (UserGroupID user : res) {
                        int userId = user.getId();
                        if (userId >= 0 && user.isUser() && userId != myUserId) {
                            ListLsubCache.dropFor(userId, ctx.getContextId());
                            MailSessionCache.clearFor(userId, ctx.getContextId());
                        }
                    }
                } catch (OXException e) {
                    LOG.debug("Could not resolve users for entity name {}", entityName, e);
                }
            }
        }
    }

    private static void extractEntityNames(Optional<ACL[]> optAcls, Collection<String> entityNames) {
        if (optAcls.isEmpty()) {
            return;
        }

        ACL[] acls = optAcls.get();
        if (0 == acls.length) {
            return;
        }

        for (ACL acl : acls) {
            if (null != acl) {
                entityNames.add(acl.getName());
            }
        }
    }

    private static char[] stripPOSTRight(String rights) {
        StringBuilder sb = new StringBuilder(rights.length());
        int length = rights.length();
        for (int i = 0; i < length; i++) {
            char c = rights.charAt(i);
            if ('p' != c && 'P' != c) {
                sb.append(c);
            }
        }

        int len = sb.length();
        char[] retval = new char[len];
        sb.getChars(0, len, retval, 0);
        return retval;
    }

    private static boolean equalRights(String rights1, String rights2, boolean ignorePOST) {
        char[] r1 = ignorePOST ? stripPOSTRight(rights1) : rights1.toCharArray();
        char[] r2 = ignorePOST ? stripPOSTRight(rights2) : rights2.toCharArray();
        if (r1.length != r2.length) {
            return false;
        }
        return new TCharHashSet(r1).containsAll(r2);
    }

    private static Map<String, ACL> acl2map(ACL[] acls) {
        final Map<String, ACL> m = new HashMap<>(acls.length);
        for (ACL acl : acls) {
            m.put(acl.getName()/* .toLowerCase(Locale.ENGLISH) */, acl);
        }
        return m;
    }

    private ACL validate(ACL newACL, Map<String, ACL> oldACLs, Optional<JSONObject> optionalMetadata, String fullName) throws OXException {
        ACL oldACL = oldACLs.get(newACL.getName());
        if (null == oldACL) {
            // Either no corresponding old ACL or old ACL's rights is not equal to "p"
            return newACL;
        }

        Rights newRights = newACL.getRights();
        Rights oldRights = oldACL.getRights();
        if (equalRights(newRights.toString(), oldRights.toString(), true)) {
            // Nothing to do...
            return null;
        }

        if (optionalMetadata.isPresent()) {
            if (!"true".equals(LogProperties.get(LogProperties.Name.DEPUTY))) {
                DeputyService deputyService = Services.optService(DeputyService.class);
                if (deputyService != null) {
                    JSONObject jMetadata = optionalMetadata.get();
                    Set<String> aclNames = oldACLs.keySet();
                    for (Map.Entry<String, Object> e : jMetadata.entrySet()) {
                        String deputyId = e.getKey();
                        if (deputyService.existsDeputyPermission(deputyId, session.getContextId())) {
                            JSONObject jDeputyMetadata = (JSONObject) e.getValue();
                            String aclName = jDeputyMetadata.optString("name", null);
                            if (aclName != null && aclNames.contains(aclName)) {
                                throw IMAPException.create(IMAPException.Code.NO_DEPUTY_PERMISSION_CHANGE, imapConfig, session, aclName, fullName);
                            }
                        }
                    }
                }
            }
        }

        // Handle the POST-to-NOT-MAPPABLE problem
        if (oldRights.contains(Rights.Right.POST) && !newRights.contains(Rights.Right.POST)) {
            newRights.add(Rights.Right.POST);
        }

        // Handle the READ-KEEP_SEEN-to-READ problem
        if (oldRights.contains(Rights.Right.READ) && newRights.contains(Rights.Right.READ)) {
            // Both allow READ access
            if (!oldRights.contains(Rights.Right.KEEP_SEEN) && newRights.contains(Rights.Right.KEEP_SEEN)) {
                newRights.remove(Rights.Right.KEEP_SEEN);
            }
        }

        return newACL;
    }

    private static final TIntSet WILDCARDS = new TIntHashSet(new int[] { '%', '*' });

    /**
     * Gets a <code>String</code> containing those characters considered as invalid for a mailbox name.
     *
     * @return A <code>String</code> containing invalid characters
     */
    public static String invalidCharsString(char separator, int userId, int contextId) {
        TIntSet invalidChars = new TIntHashSet(IMAPProperties.getInstance().getInvalidChars(userId, contextId));
        invalidChars.addAll(WILDCARDS);
        invalidChars.add(separator);
        final StringBuilder sb = new StringBuilder(invalidChars.size());
        invalidChars.forEach(invalidChar -> {
            sb.append((char) invalidChar);
            return true;
        });
        return sb.toString();
    }

    /**
     * Checks id specified folder name is allowed to be used on folder creation. The folder name is valid if the separator character does
     * not appear or provided that MBox format is enabled may only appear at name's end.
     *
     * @param name The folder name to check.
     * @param separator The separator character.
     * @param mboxEnabled <code>true</code> If MBox format is enabled; otherwise <code>false</code>
     * @return <code>true</code> if folder name is valid; otherwise <code>false</code>
     */
    private static boolean checkFolderNameValidity(String name, char separator, boolean mboxEnabled, Session session) {
        TIntProcedure procedure = wildcard -> name.indexOf(wildcard) < 0;

        // Check for possibly contained wild-cards
        if (!WILDCARDS.forEach(procedure)) {
            return false;
        }

        // Check for possibly contained invalid characters (as per configuration)
        final TIntSet invalidChars = IMAPProperties.getInstance().getInvalidChars(session.getUserId(), session.getContextId());
        if (null != invalidChars && !invalidChars.isEmpty()) {
            if (!invalidChars.forEach(procedure)) {
                return false;
            }
        }

        // Check for possibly contained separator character (dependent on mbox format)
        final int pos = name.indexOf(separator);
        return mboxEnabled ? (pos < 0) || (pos == name.length() - 1) : (pos < 0);
    }

    private static final String REGEX_TEMPL = "[\\S\\p{Blank}&&[^\\p{Cntrl}#SEP#]]+(?:\\Q#SEP#\\E[\\S\\p{Blank}&&[^\\p{Cntrl}#SEP#]]+)*";

    private static final Pattern PAT_SEP = Pattern.compile("#SEP#");

    private static boolean checkFolderPathValidity(String path, char separator) {
        if ((path != null) && (path.length() > 0)) {
            return Pattern.compile(PAT_SEP.matcher(REGEX_TEMPL).replaceAll(String.valueOf(separator))).matcher(path).matches();
        }
        return false;
    }

    private void removeSessionData(String fullName, char sep, IMAPFolder f) {
        if (!IMAPSessionStorageAccess.isEnabled()) {
            return;
        }
        try {
            for (ListInfo listInfo : IMAPCommandsCollection.listSubfolders(fullName, sep, f)) {
                removeSessionData(listInfo.name, sep, f);
            }
            IMAPSessionStorageAccess.removeDeletedFolder(accountId, session, fullName);
        } catch (MessagingException e) {
            LOG.error("", e);
        }
    }

    private IMAPFolder getIMAPFolder(String fullName) throws MessagingException {
        return ROOT_FOLDER_ID.equals(fullName) ? (IMAPFolder) imapStore.getDefaultFolder() : (IMAPFolder) imapStore.getFolder(fullName);
    }

    private IMAPFolder getIMAPFolderWithRecentListener(String fullName) throws MessagingException {
        return ROOT_FOLDER_ID.equals(fullName) ? (IMAPFolder) imapStore.getDefaultFolder() : (IMAPFolder) imapStore.getFolder(fullName);
    }

    private boolean doesExist(IMAPFolder imapFolder, boolean mayCheckCache) throws OXException, MessagingException {
        String fullName = imapFolder.getFullName();
        if (STR_INBOX.equals(fullName)) {
            return true;
        }

        if (!mayCheckCache) {
            return imapFolder.exists();
        }

        boolean exists = getLISTEntry(fullName, imapFolder).exists();
        if (!exists) {
            exists = imapFolder.exists();
        }
        return exists;
    }

    private static boolean doesExist(ListLsubEntry entry) {
        return STR_INBOX.equals(entry.getFullName()) || (entry.exists());
    }

    private ListLsubEntry getLISTEntry(IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLISTEntry(imapFolder.getFullName(), imapFolder);
    }

    private ListLsubEntry getLISTEntry(String fullName, IMAPFolder imapFolder) throws OXException, MessagingException {
        return ListLsubCache.getCachedLISTEntry(fullName, accountId, imapFolder, session, imapConfig.getIMAPProperties());
    }

    private ListLsubEntry getLSUBEntry(IMAPFolder imapFolder) throws OXException, MessagingException {
        return getLSUBEntry(imapFolder.getFullName(), imapFolder);
    }

    private ListLsubEntry getLSUBEntry(String fullName, IMAPFolder imapFolder) throws OXException, MessagingException {
        return ListLsubCache.getCachedLSUBEntry(fullName, accountId, imapFolder, session, imapConfig.getIMAPProperties());
    }

    /**
     * Gets the separator character.
     *
     * @param imapStore The IMAP store
     * @return The separator character
     * @throws OXException If an error occurs
     */
    public char getSeparator(IMAPStore imapStore) throws OXException {
        try {
            return getLISTEntry(STR_INBOX, (IMAPFolder) imapStore.getDefaultFolder()).getSeparator();
        } catch (MessagingException e) {
            throw handleMessagingException(e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    /**
     * Gets the separator character.
     *
     * @param imapFolder The IMAP folder
     * @return The separator character
     * @throws OXException If an error occurs
     */
    public char getSeparator(IMAPFolder imapFolder) throws OXException {
        try {
            return getLISTEntry(STR_INBOX, imapFolder).getSeparator();
        } catch (MessagingException e) {
            throw handleMessagingException(imapFolder.getFullName(), e);
        } catch (RuntimeException e) {
            throw handleRuntimeException(e);
        }
    }

    private String getNameOf(IMAPFolder imapFolder) throws OXException {
        final String fullName = imapFolder.getFullName();
        return fullName.substring(fullName.lastIndexOf(getSeparator(imapFolder)) + 1);
    }

    private OXException handleRuntimeException(RuntimeException e) {
        if (e instanceof ListLsubRuntimeException) {
            ListLsubCache.clearCache(accountId, session);
            return MailExceptionCode.INTERRUPT_ERROR.create(e, e.getMessage());
        }
        return MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
    }

    private static boolean isSubfolderOf(String fullName, String possibleParentFullName, char separator) {
        if (possibleParentFullName.length() == 0) {
            // Always below root folder...
            return true;
        }
        return fullName.startsWith(possibleParentFullName + separator);
    }

    private boolean isSameNamespace(String fullName1, String fullName2) throws MessagingException {
        if (NamespacesCache.startsWithAnyOfUserNamespaces(fullName1, imapStore, true, session, accountId)) {
            return NamespacesCache.startsWithAnyOfUserNamespaces(fullName2, imapStore, true, session, accountId);
        } else if (NamespacesCache.startsWithAnyOfUserNamespaces(fullName2, imapStore, true, session, accountId)) {
            return false;
        }

        if (NamespacesCache.startsWithAnyOfSharedNamespaces(fullName1, imapStore, true, session, accountId)) {
            return NamespacesCache.startsWithAnyOfSharedNamespaces(fullName2, imapStore, true, session, accountId);
        } else if (NamespacesCache.startsWithAnyOfSharedNamespaces(fullName2, imapStore, true, session, accountId)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if specified full name starts with either a user or a shared namespace path prefix.
     *
     * @param fullName The full name to check
     * @return <code>true</code> if denoted full name is a namespace subfolder; otherwise <code>false</code>
     * @throws MessagingException If operation fails
     */
    public boolean startsWithNamespaceFolder(String fullName) throws MessagingException {
        Namespaces namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
        for (Namespace ns : namespaces.getOtherUsers()) {
            if (fullName.startsWith(ns.getPrefix())) {
                return true;
            }
        }
        for (Namespace ns : namespaces.getShared()) {
            if (fullName.startsWith(ns.getPrefix())) {
                return true;
            }
        }
        return false;
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

    private static class IMAPFolderDeleteTask extends AbstractTask<Void> {

        private final IMAPStore imapStore;
        private final String toDeleteFullName;

        /**
         * Initializes a new {@link IMAPFolderDeleteTask}.
         *
         * @param imapStore The connected IMAP store (closed when task is completed)
         * @param toDeleteFullName The full name of the IMAP folder to delete
         */
        IMAPFolderDeleteTask(IMAPStore imapStore, String toDeleteFullName) {
            super();
            this.imapStore = imapStore;
            this.toDeleteFullName = toDeleteFullName;
        }

        @Override
        public Void call() throws Exception {
            try {
                ((IMAPFolder) imapStore.getFolder(toDeleteFullName)).delete(true);
            } finally {
                Streams.close(imapStore);
            }
            return null;
        }
    }

    private static final class FullDisplayNameComparator implements Comparator<MailFolderInfo> {

        private final Collator collator;

        FullDisplayNameComparator(Locale locale) {
            super();
            collator = Collators.getSecondaryInstance(locale);
        }

        @Override
        public int compare(MailFolderInfo o1, MailFolderInfo o2) {
            /*
             * Compare by full name
             */
            return collator.compare(o1.getFullDisplayName(), o2.getFullDisplayName());
        }

    } // End of FullNameComparator

    private static String urlName(String prefix, String host, int port, String username, String pw) {
        // Start with "protocol:"
        final StringBuilder tempURL = new StringBuilder(128);

        if (null != prefix) {
            tempURL.append(prefix);
        }

        tempURL.append("imap://");

        // Add the user:password@
        if (username != null) {
            tempURL.append(username);
            if (pw != null) {
                tempURL.append(':');
                tempURL.append(pw);
            }
            tempURL.append('@');
        }

        // Add host
        if (host != null) {
            tempURL.append(host);
        }

        // Add port
        if (port > 0) {
            tempURL.append(':');
            tempURL.append(Integer.toString(port));
        }

        return tempURL.toString();
    }

}
