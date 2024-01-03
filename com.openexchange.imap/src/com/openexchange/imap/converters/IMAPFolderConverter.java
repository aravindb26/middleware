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

package com.openexchange.imap.converters;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Strings.isNotEmpty;
import static com.openexchange.mailaccount.MailAccounts.isSecondaryAccount;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;
import javax.mail.StoreClosedException;
import javax.mail.internet.idn.IDNA;
import com.openexchange.deputy.DeputyService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.imap.ACLPermission;
import com.openexchange.imap.IMAPAccess;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.imap.IMAPException;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.cache.ListLsubEntry;
import com.openexchange.imap.cache.NamespacesCache;
import com.openexchange.imap.cache.RightsCache;
import com.openexchange.imap.cache.RootSubfoldersEnabledCache;
import com.openexchange.imap.cache.UserFlagsCache;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.dataobjects.IMAPMailFolder;
import com.openexchange.imap.entity2acl.Entity2ACL;
import com.openexchange.imap.entity2acl.Entity2ACLArgs;
import com.openexchange.imap.entity2acl.Entity2ACLArgsImpl;
import com.openexchange.imap.entity2acl.Entity2ACLExceptionCode;
import com.openexchange.imap.entity2acl.UserInfo;
import com.openexchange.imap.namespace.Namespace;
import com.openexchange.imap.namespace.Namespaces;
import com.openexchange.imap.services.Services;
import com.openexchange.imap.util.IMAPDeputyMetadataUtility;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.OwnerInfo;
import com.openexchange.mail.dataobjects.MailFolder.DefaultFolderType;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.DefaultFolder;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;
import gnu.trove.set.TIntSet;

/**
 * {@link IMAPFolderConverter} - Converts an instance of {@link IMAPFolder} to an instance of {@link MailFolder}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPFolderConverter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IMAPFolderConverter.class);

    /**
     * New mailbox attribute added by the "LIST-EXTENDED" extension.
     */
    private static final String ATTRIBUTE_NON_EXISTENT = "\\nonexistent";

    private static final String ATTRIBUTE_HAS_CHILDREN = "\\haschildren";

    private static final String ATTRIBUTE_NO_INFERIORS = "\\noinferiors";

    /**
     * Prevent instantiation
     */
    private IMAPFolderConverter() {
        super();
    }

    /**
     * Creates an appropriate implementation of {@link Entity2ACLArgs}.
     *
     * @param session The session
     * @param imapFolder The IMAP folder
     * @param imapConfig The IMAP configuration
     * @return An appropriate implementation of {@link Entity2ACLArgs}
     * @throws OXException If IMAP folder's attributes cannot be accessed
     */
    public static Entity2ACLArgs getEntity2AclArgs(Session session, IMAPFolder imapFolder, IMAPConfig imapConfig) throws OXException {
        return getEntity2AclArgs(session.getUserId(), session, imapFolder, imapConfig);
    }

    /**
     * Creates an appropriate implementation of {@link Entity2ACLArgs}.
     *
     * @param userId The user identifier
     * @param session The session
     * @param imapFolder The IMAP folder
     * @param imapConfig The IMAP configuration
     * @return An appropriate implementation of {@link Entity2ACLArgs}
     * @throws OXException If IMAP folder's attributes cannot be accessed
     */
    public static Entity2ACLArgs getEntity2AclArgs(int userId, Session session, IMAPFolder imapFolder, IMAPConfig imapConfig) throws OXException {
        try {
            Namespaces namespaces = NamespacesCache.getNamespaces((IMAPStore) imapFolder.getStore(), true, session, imapConfig.getAccountId());
            return new Entity2ACLArgsImpl(session, imapConfig.getAccountId(), new StringBuilder(36).append(IDNA.toASCII(imapConfig.getServer())).append(':').append(imapConfig.getPort()).toString(), userId, imapFolder.getFullName(), ListLsubCache.getSeparator(imapConfig.getAccountId(), imapFolder, session, imapConfig.getIMAPProperties()), namespaces, isSecondaryAccount(imapConfig.getAccountId(), session), imapConfig.getImapCapabilities().hasResolve());
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    private static final DefaultFolderType[] TYPES = {
        DefaultFolderType.DRAFTS, DefaultFolderType.SENT, DefaultFolderType.SPAM, DefaultFolderType.TRASH,
        DefaultFolderType.CONFIRMED_SPAM, DefaultFolderType.CONFIRMED_HAM, DefaultFolderType.INBOX };

    private static final boolean DO_STATUS = false;

    /**
     * Creates a folder data object from given IMAP folder.
     *
     * @param imapFolder The IMAP folder
     * @param session The session
     * @param ctx The context
     * @return An instance of <code>{@link IMAPMailFolder}</code> containing the attributes from given IMAP folder
     * @throws OXException If conversion fails
     */
    public static IMAPMailFolder convertFolder(IMAPFolder imapFolder, Session session, IMAPAccess imapAccess, Context ctx) throws OXException {
        try {
            synchronized (imapFolder) {
                final String imapFullName = imapFolder.getFullName();
                if (imapFolder instanceof DefaultFolder df) {
                    return convertRootFolder(df, session, imapAccess.getIMAPConfig());
                }
                final IMAPConfig imapConfig = imapAccess.getIMAPConfig();
                final long st = System.currentTimeMillis();
                // Convert non-root folder
                final IMAPMailFolder mailFolder = new IMAPMailFolder();
                mailFolder.setRootFolder(false);
                // Get appropriate entries
                final int accountId = imapConfig.getAccountId();
                final ListLsubEntry listEntry = ListLsubCache.getCachedLISTEntry(imapFullName, accountId, imapFolder, session, imapConfig.getIMAPProperties());
                /*
                 * Check existence
                 */
                final boolean exists = "INBOX".equals(imapFullName) || listEntry.exists();
                mailFolder.setExists(exists);
                mailFolder.setSeparator(listEntry.getSeparator());
                String name = listEntry.getName();
                // Shared or public?
                Namespaces namespaces = NamespacesCache.getNamespaces((IMAPStore) imapFolder.getStore(), true, session, accountId);
                {
                    List<Namespace> userNamespaces = namespaces.getOtherUsers();
                    char sep = mailFolder.getSeparator();
                    Optional<User> optUser = getUserFrom(session);
                    boolean shared = false;
                    String owner = null;
                    OwnerInfo ownerInfo = null;
                    for (int i = 0; !shared && i < userNamespaces.size(); i++) {
                        final Namespace userNamespace = userNamespaces.get(i);
                        if (isNotEmpty(userNamespace.getFullName())) {
                            if (imapFullName.equals(userNamespace.getFullName())) {
                                if (userNamespaces.size() == 1 && optUser.isPresent()) {
                                    // Only set i18n name if there is only one user namespace. Otherwise duplicate names are displayed.
                                    name = StringHelper.valueOf(optUser.get().getLocale()).getString(FolderStrings.SYSTEM_SHARED_FOLDER_NAME);
                                }
                                shared = true;
                            } else {
                                String prefix = userNamespace.getPrefix();
                                if (imapFullName.startsWith(prefix)) {
                                    shared = true;
                                    /*-
                                     * "Other Users/user1"
                                     *  vs.
                                     * "Other Users/user1/My shared folder"
                                     */
                                    int pLen = prefix.length();
                                    int pos = imapFullName.indexOf(sep, pLen);
                                    if (pos < 0) {
                                        // "Other Users/user1"
                                        owner = imapFullName.substring(pLen);
                                        Optional<UserInfo> optUserInfo = Entity2ACL.getUserInfoFor(owner, imapFullName, sep, imapAccess, true);
                                        if (optUserInfo.isPresent()) {
                                            UserInfo userInfo = optUserInfo.get();
                                            ownerInfo = new OwnerInfo(userInfo.getUserId(), userInfo.getContextId(), owner);
                                            name = userInfo.getOptionalDisplayName().orElse(name);
                                        }
                                    } else {
                                        // "Other Users/user1/My shared folder"
                                        owner = imapFullName.substring(pLen, pos);
                                        Optional<UserInfo> optUserInfo = Entity2ACL.getUserInfoFor(owner, imapFullName, sep, imapAccess, false);
                                        if (optUserInfo.isPresent()) {
                                            UserInfo userInfo = optUserInfo.get();
                                            ownerInfo = new OwnerInfo(userInfo.getUserId(), userInfo.getContextId(), owner);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    mailFolder.setShared(shared);
                    if (null != owner) {
                        mailFolder.setOwner(owner);
                    }
                    if (null != ownerInfo) {
                        mailFolder.setOwnerInfo(ownerInfo);
                    }
                    boolean isPublic = false;
                    if (!shared) {
                        List<Namespace> sharedNamespaces = namespaces.getShared();
                        List<Namespace> personalNamespaces = namespaces.getPersonal();
                        for (int i = 0; !isPublic && i < sharedNamespaces.size(); i++) {
                            Namespace sharedNamespace = sharedNamespaces.get(i);
                            if (isNotEmpty(sharedNamespace.getFullName())) {
                                if (imapFullName.equals(sharedNamespace.getFullName())) {
                                    if (sharedNamespaces.size() == 1 && optUser.isPresent()) {
                                        // Only set i18n name if there is only one user namespace. Otherwise duplicate names are displayed.
                                        name = StringHelper.valueOf(optUser.get().getLocale()).getString(FolderStrings.SYSTEM_PUBLIC_FOLDER_NAME);
                                    }
                                    isPublic = true;
                                } else {
                                    if (imapFullName.startsWith(sharedNamespace.getPrefix())) {
                                        isPublic = true;
                                    }
                                }
                            } else if (!startsWithOneOf(imapFullName, personalNamespaces, userNamespaces)) {
                                isPublic = true;
                            }
                        }
                    }
                    mailFolder.setPublic(isPublic);
                }
                /*-
                 * -------------------------------------------------------------------
                 * -------------------------##################------------------------
                 * -------------------------------------------------------------------
                 */
                if (exists) {
                    final Set<String> attrs = listEntry.getAttributes();
                    if (null != attrs && !attrs.isEmpty()) {
                        if (attrs.contains(ATTRIBUTE_NON_EXISTENT)) {
                            mailFolder.setNonExistent(true);
                        }
                        if (imapConfig.getImapCapabilities().hasChildren() && attrs.contains(ATTRIBUTE_HAS_CHILDREN)) {
                            mailFolder.setSubfolders(true);
                        }
                        if (attrs.contains(ATTRIBUTE_NO_INFERIORS)) {
                            mailFolder.setSubfolders(true);
                            mailFolder.setSubscribedSubfolders(false);
                        }
                    }
                    if (!mailFolder.containsSubfolders()) {
                        /*
                         * No \HasChildren attribute found; check for subfolders through a LIST command
                         */
                        final List<ListLsubEntry> children = listEntry.getChildren();
                        mailFolder.setSubfolders(null != children && !children.isEmpty());
                    }
                    if (!mailFolder.containsNonExistent()) {
                        mailFolder.setNonExistent(false);
                    }
                    /*
                     * Check reliably for subscribed subfolders through LSUB command since folder attributes need not to to be present as
                     * per RFC 3501
                     */
                    mailFolder.setSubscribedSubfolders(mailFolder.hasSubfolders() && ListLsubCache.hasAnySubscribedSubfolder(imapFullName, accountId, imapFolder, session, imapConfig.getIMAPProperties()));
                }
                /*
                 * Set full name, name, and parent full name
                 */
                mailFolder.setFullname(imapFullName);
                mailFolder.setName(name);
                {
                    final ListLsubEntry parentListEntry = listEntry.getParent();
                    if (null == parentListEntry) {
                        mailFolder.setParentFullname(null);
                    } else {
                        final String pfn = parentListEntry.getFullName();
                        mailFolder.setParentFullname(pfn.length() == 0 ? MailFolder.ROOT_FOLDER_ID : pfn);
                    }
                }
                if (!mailFolder.containsDefaultFolder()) {
                    /*
                     * Default folder
                     */
                    if (mailFolder.isShared() || mailFolder.isPublic()) {
                        mailFolder.setDefaultFolder(false);
                        mailFolder.setDefaultFolderType(DefaultFolderType.NONE);
                    } else if ("INBOX".equals(imapFullName)) {
                        mailFolder.setDefaultFolder(true);
                        mailFolder.setDefaultFolderType(DefaultFolderType.INBOX);
                    } else if (isDefaultFoldersChecked(session, accountId)) {
                        final String[] defaultMailFolders = getDefaultMailFolders(session, accountId);
                        if (defaultMailFolders != null) {
                            for (int i = 0; i < defaultMailFolders.length && !mailFolder.isDefaultFolder(); i++) {
                                if (imapFullName.equals(defaultMailFolders[i])) {
                                    mailFolder.setDefaultFolder(true);
                                    mailFolder.setDefaultFolderType(TYPES[i]);
                                }
                            }
                        }
                        if (!mailFolder.containsDefaultFolder()) {
                            mailFolder.setDefaultFolder(false);
                            mailFolder.setDefaultFolderType(DefaultFolderType.NONE);
                        }
                    } else {
                        mailFolder.setDefaultFolder(false);
                        mailFolder.setDefaultFolderType(DefaultFolderType.NONE);
                    }
                }
                /*
                 * Set type
                 */
                if (exists) {
                    final int type = listEntry.getType();
                    mailFolder.setHoldsFolders(((type & javax.mail.Folder.HOLDS_FOLDERS) > 0));
                    mailFolder.setHoldsMessages(((type & javax.mail.Folder.HOLDS_MESSAGES) > 0));
                    if (!mailFolder.isHoldsFolders()) {
                        mailFolder.setSubfolders(false);
                        mailFolder.setSubscribedSubfolders(false);
                    }
                } else {
                    mailFolder.setHoldsFolders(false);
                    mailFolder.setHoldsMessages(false);
                    mailFolder.setSubfolders(false);
                    mailFolder.setSubscribedSubfolders(false);
                }
                final boolean selectable = mailFolder.isHoldsMessages();
                Rights ownRights;
                /*
                 * Add own rights
                 */
                {
                    final ACLPermission ownPermission = new ACLPermission();
                    ownPermission.setEntity(session.getUserId());
                    if (!exists || mailFolder.isNonExistent()) {
                        ownPermission.parseRights((ownRights = new Rights()), imapConfig);
                    } else if (!selectable) {
                        ownRights = ownRightsFromProblematic(session, imapAccess, imapFullName, imapConfig, mailFolder, accountId, ownPermission, listEntry);
                    } else {
                        ownRights = getOwnRights(imapFolder, session, imapConfig);
                        if (null == ownRights) {
                            ownRights = ownRightsFromProblematic(session, imapAccess, imapFullName, imapConfig, mailFolder, accountId, ownPermission, listEntry);
                        } else {
                            ownPermission.parseRights(ownRights, imapConfig);
                        }
                    }
                    /*
                     * Check own permission against folder type
                     */
                    if (!mailFolder.isHoldsFolders() && ownPermission.canCreateSubfolders()) {
                        ownPermission.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
                    }
                    if (!selectable) {
                        ownPermission.setReadObjectPermission(OCLPermission.NO_PERMISSIONS);
                    }
                    mailFolder.setOwnPermission(ownPermission);
                }
                /*
                 * Set message counts for total, new, unread, and deleted
                 */
                if (selectable && imapConfig.getACLExtension().canRead(ownRights)) {
                    if (DO_STATUS) {
                        final int messageCount = listEntry.getMessageCount();
                        if (messageCount < 0) {
                            final int[] status = IMAPCommandsCollection.getStatus(imapFolder);
                            mailFolder.setMessageCount(status[0]);
                            mailFolder.setNewMessageCount(status[1]);
                            mailFolder.setUnreadMessageCount(status[2]);
                            listEntry.rememberCounts(status[0], status[1], status[2]);
                        } else {
                            mailFolder.setMessageCount(messageCount);
                            mailFolder.setNewMessageCount(listEntry.getNewMessageCount());
                            mailFolder.setUnreadMessageCount(listEntry.getUnreadMessageCount());
                        }
                    } else {
                        mailFolder.setMessageCount(-1);
                        mailFolder.setNewMessageCount(-1);
                        mailFolder.setUnreadMessageCount(-1);
                    }
                    mailFolder.setDeletedMessageCount(-1/* imapFolder.getDeletedMessageCount() */);
                } else {
                    mailFolder.setMessageCount(-1);
                    mailFolder.setNewMessageCount(-1);
                    mailFolder.setUnreadMessageCount(-1);
                    mailFolder.setDeletedMessageCount(-1);
                }
                mailFolder.setSubscribed(imapConfig.getIMAPProperties().isSupportSubscription() ? ("INBOX".equals(mailFolder.getFullname()) ? true : listEntry.isSubscribed()) : true);
                /*
                 * Parse ACLs to user/group permissions for primary account only.
                 */
                if (imapConfig.isSupportsACLs()) {
                    if (Account.DEFAULT_ID == accountId) {
                        // Check if ACLs can be read; meaning GETACL is allowed
                        handleACLs(imapFolder, session, ctx, imapConfig, mailFolder, listEntry, exists, selectable, ownRights, false);
                    } else if (isSecondaryAccount(accountId, session)) {
                        // Check if ACLs can be read; meaning GETACL is allowed
                        handleACLs(imapFolder, session, ctx, imapConfig, mailFolder, listEntry, exists, selectable, ownRights, true);
                    } else {
                        // External non-internal account. No ACL support then...
                        addOwnACL(mailFolder, imapFolder, session);
                    }
                } else {
                    // No ACL support enabled
                    addOwnACL(mailFolder, imapFolder, session);
                }
                if (imapConfig.getIMAPProperties().isUserFlagsEnabled() && exists && (false == mailFolder.isNonExistent()) && selectable && imapConfig.getACLExtension().canRead(
                    ownRights) && UserFlagsCache.supportsUserFlags(imapFolder, true, session, accountId)) {
                    mailFolder.setSupportsUserFlags(true);
                } else {
                    mailFolder.setSupportsUserFlags(false);
                }
                LOG.debug("IMAP folder \"{}\" converted in {}msec.", imapFullName, L(System.currentTimeMillis() - st));
                return mailFolder;
            }
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the user associated with given session.
     *
     * @param session The session providing user information
     * @return The user or empty
     */
    public static Optional<User> getUserFrom(Session session) {
        if (session instanceof ServerSession) {
            return Optional.of(((ServerSession) session).getUser());
        }

        UserService userService = Services.optService(UserService.class);
        if (userService == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(userService.getUser(session.getUserId(), session.getContextId()));
        } catch (Exception e) {
            LOG.warn("Failed to get user {} from context {}", I(session.getUserId()), I(session.getContextId()), e);
            return Optional.empty();
        }
    }

    private static void handleACLs(IMAPFolder imapFolder, Session session, Context ctx, IMAPConfig imapConfig, IMAPMailFolder mailFolder, ListLsubEntry listEntry, boolean exists, boolean selectable, Rights ownRights, boolean secondaryAccount) throws MessagingException {
        if (selectable && exists && imapConfig.getACLExtension().canGetACL(ownRights)) {
            try {
                applyACL2Permissions(imapFolder, listEntry, session, imapConfig, mailFolder, ownRights, ctx, secondaryAccount);
            } catch (OXException e) {
                LOG.warn("ACLs could not be parsed for folder {} of account {}", imapFolder.getFullName(), imapFolder.getStore(), e);
                mailFolder.removePermissions();
                addOwnACL(mailFolder, imapFolder, session);
            }
        } else {
            addOwnACL(mailFolder, imapFolder, session);
        }
    }

    /**
     * Checks if given IMAP full name starts with any of given personal/user namespaces.
     *
     * @param imapFullName The IMAP full name
     * @param sep The separator character
     * @param personalNamespaces The personal namespaces
     * @param userNamespaces The user namespaces
     * @param tmp Helper instance of <code>StringBuilder</code>
     * @return <code>true</code> if given IMAP full name starts with any of given personal/user namespaces; otherwise <code>false</code>
     */
    public static boolean startsWithOneOf(String imapFullName, Iterable<Namespace> personalNamespaces, Iterable<Namespace> userNamespaces) {
        for (Namespace ns : userNamespaces) {
            if (imapFullName.equals(ns.getFullName()) || imapFullName.startsWith(ns.getPrefix())) {
                return true;
            }
        }
        for (Namespace ns : personalNamespaces) {
            if (imapFullName.equals(ns.getFullName()) || imapFullName.startsWith(ns.getPrefix())) {
                return true;
            }
        }
        return false;
    }

    private static Rights ownRightsFromProblematic(Session session, IMAPAccess imapAccess, String imapFullName, IMAPConfig imapConfig, IMAPMailFolder mailFolder, int accountId, ACLPermission ownPermission, ListLsubEntry listEntry) throws MessagingException, OXException, IMAPException {
        Rights ownRights;
        /*
         * Distinguish between holds folders and none
         */
        if (mailFolder.isHoldsFolders()) {
            /*
             * This is the tricky case: Allow subfolder creation for a common IMAP folder but deny it for namespace folders
             */
            if (checkForNamespaceFolder(imapFullName, imapAccess.getIMAPStore(), session, accountId, imapConfig.getIMAPProperties())) {
                ownRights = new Rights();
                ownPermission.parseRights(ownRights, imapConfig);
            } else {
                ownPermission.setAllPermission(OCLPermission.CREATE_SUB_FOLDERS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
                // Allow folder administration if there are no subfolders...
                if (listEntry.hasChildren()) {
                    ownPermission.setFolderAdmin(false);
                } else {
                    // In case no children exist, the non-selectable should be manageable by user. Otherwise no chance to get rid off it...
                    ownPermission.setFolderAdmin(true);
                }
                ownRights = ACLPermission.permission2Rights(ownPermission, imapConfig);
            }
        } else {
            ownRights = new Rights();
            ownPermission.parseRights(ownRights, imapConfig);
        }
        return ownRights;
    }

    private static IMAPMailFolder convertRootFolder(DefaultFolder rootFolder, Session session, IMAPConfig imapConfig) throws OXException {
        try {
            final IMAPMailFolder mailFolder = new IMAPMailFolder();
            mailFolder.setRootFolder(true);
            mailFolder.setExists(true);
            mailFolder.setShared(false);
            mailFolder.setPublic(true);
            mailFolder.setSeparator(ListLsubCache.getSeparator(imapConfig.getAccountId(), rootFolder, session, imapConfig.getIMAPProperties()));
            final String imapFullname = "";
            final ListLsubEntry listEntry = ListLsubCache.getCachedLISTEntry(imapFullname, imapConfig.getAccountId(), rootFolder, session, imapConfig.getIMAPProperties());
            final Set<String> attrs = listEntry.getAttributes();
            if (null != attrs && !attrs.isEmpty()) {
                if (attrs.contains(ATTRIBUTE_NON_EXISTENT)) {
                    mailFolder.setNonExistent(true);
                }
                if (imapConfig.getImapCapabilities().hasChildren() && attrs.contains(ATTRIBUTE_HAS_CHILDREN)) {
                    mailFolder.setSubfolders(true);
                }
            }
            mailFolder.setSubfolders(true);
            mailFolder.setSubscribedSubfolders(true); // At least INBOX
            if (!mailFolder.containsNonExistent()) {
                mailFolder.setNonExistent(false);
            }
            /*
             * Set full name, name, and parent full name
             */
            mailFolder.setFullname(MailFolder.ROOT_FOLDER_ID);
            mailFolder.setName(MailFolder.ROOT_FOLDER_NAME);
            mailFolder.setParentFullname(null);
            mailFolder.setDefaultFolder(false);
            mailFolder.setDefaultFolderType(DefaultFolderType.NONE);
            /*
             * Root folder only holds folders but no messages
             */
            mailFolder.setHoldsFolders(true);
            mailFolder.setHoldsMessages(false);
            /*
             * Check if subfolder creation is allowed
             */
            final ACLPermission ownPermission = new ACLPermission();
            final int fp = RootSubfoldersEnabledCache.isRootSubfoldersEnabled(imapConfig, rootFolder, session) ? OCLPermission.CREATE_SUB_FOLDERS : OCLPermission.READ_FOLDER;
            ownPermission.setEntity(session.getUserId());
            ownPermission.setAllPermission(fp, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
            ownPermission.setFolderAdmin(false);
            mailFolder.setOwnPermission(ownPermission);
            mailFolder.addPermission(ownPermission);
            /*
             * Set message counts
             */
            mailFolder.setMessageCount(-1);
            mailFolder.setNewMessageCount(-1);
            mailFolder.setUnreadMessageCount(-1);
            mailFolder.setDeletedMessageCount(-1);
            /*
             * Root folder is always subscribed
             */
            mailFolder.setSubscribed(true);
            /*
             * No user flag support
             */
            mailFolder.setSupportsUserFlags(false);
            return mailFolder;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e, imapConfig, session);
        }
    }

    private static boolean isDefaultFoldersChecked(Session session, int accountId) {
        final Boolean b = MailSessionCache.getInstance(session).getParameter(
            accountId,
            MailSessionParameterNames.getParamDefaultFolderChecked());
        return (b != null) && b.booleanValue();
    }

    private static String[] getDefaultMailFolders(Session session, int accountId) {
        return MailSessionCache.getInstance(session).getParameter(accountId, MailSessionParameterNames.getParamDefaultFolderArray());
    }

    /**
     * Parses IMAP folder's ACLs to instances of {@link ACLPermission} and applies them to specified mail folder.
     *
     * @param imapFolder The IMAP folder
     * @param listEntry The LIST entry
     * @param session The session providing needed user data
     * @param imapConfig The user's IMAP configuration
     * @param mailFolder The mail folder
     * @param ownRights The rights granted to IMAP folder for session user
     * @param ctx The context
     * @param secondaryAccount Whether account is a secondary one
     * @throws OXException If ACLs cannot be mapped
     * @throws MessagingException If a messaging error occurs
     */
    private static void applyACL2Permissions(IMAPFolder imapFolder, ListLsubEntry listEntry, Session session, IMAPConfig imapConfig, MailFolder mailFolder, Rights ownRights, Context ctx, boolean secondaryAccount) throws OXException, MessagingException {
        final ACL[] acls;
        try {
            final List<ACL> list = listEntry.getACLs();
            if (null == list) {
                acls = imapFolder.getACL();
                listEntry.rememberACLs(Arrays.asList(acls));
            } else {
                acls = list.toArray(new ACL[list.size()]);
            }
        } catch (MessagingException e) {
            if (!ownRights.contains(Rights.Right.ADMINISTER)) {
                LOG.warn("ACLs could not be requested for folder {}. A newer ACL extension (RFC 4314) seems to be supported by IMAP server {}, which denies GETACL command if no ADMINISTER right is granted.", imapFolder.getFullName(), imapConfig.getServer(), e);
                addOwnACL(mailFolder, imapFolder, session);
                return;
            }
            throw MimeMailException.handleMessagingException(e);
        }
        if (acls.length <= 0) {
            return;
        }
        Namespaces namespaces = NamespacesCache.getNamespaces((IMAPStore) imapFolder.getStore(), true, session, imapConfig.getAccountId());
        Entity2ACLArgs args = new Entity2ACLArgsImpl(session, imapConfig.getAccountId(), new StringBuilder(36).append(IDNA.toASCII(imapConfig.getServer())).append(':').append(imapConfig.getPort()).toString(), session.getUserId(), imapFolder.getFullName(), listEntry.getSeparator(), namespaces, secondaryAccount, imapConfig.getImapCapabilities().hasResolve());
        TIntSet deputyEntities = isDeputyServiceAvailable(session) ? IMAPDeputyMetadataUtility.getDeputyEntities(imapFolder, Optional.of(acls)) : null;
        boolean userPermAdded = false;
        for (int j = 0; j < acls.length; j++) {
            ACL acl = acls[j];
            try {
                ACLPermission aclPerm = new ACLPermission();
                aclPerm.parseACL(acl, args, (IMAPStore) imapFolder.getStore(), imapConfig, ctx);
                aclPerm.setDeputyPermission(deputyEntities != null && deputyEntities.contains(aclPerm.getEntity()));
                if (session.getUserId() == aclPerm.getEntity()) {
                    userPermAdded = true;
                    if (imapConfig.getAccountId() == Account.DEFAULT_ID) {
                        Rights aclRights = acl.getRights();
                        if (!ownRights.equals(aclRights)) {
                            LOG.debug("Detected different rights for MYRIGHTS ({}) and GETACL ({}) for user {} in context {}. Preferring GETACL rights as user's own-rights.", ownRights, aclRights, I(session.getUserId()), I(session.getContextId()));
                            MailPermission ownPermission = mailFolder.getOwnPermission();
                            if (ownPermission instanceof ACLPermission) {
                                ((ACLPermission) ownPermission).parseRights(aclRights, imapConfig);
                            } else {
                                ownPermission.setAllPermission(
                                    aclPerm.getFolderPermission(),
                                    aclPerm.getReadPermission(),
                                    aclPerm.getWritePermission(),
                                    aclPerm.getDeletePermission());
                                ownPermission.setFolderAdmin(aclPerm.isFolderAdmin());
                            }
                        }
                    }
                }
                mailFolder.addPermission(aclPerm);
            } catch (OXException e) {
                if (!isUnknownEntityError(e)) {
                    throw e;
                }

                LOG.debug("Cannot map ACL entity named \"{}\" to a system user", acl.getName());
                if (secondaryAccount) {
                    ACLPermission aclPerm = new ACLPermission();
                    aclPerm.parseRightsFrom(acl, imapConfig);
                    aclPerm.setIdentifier(acl.getName());
                    aclPerm.setDeputyPermission(deputyEntities != null && deputyEntities.contains(aclPerm.getEntity()));
                    mailFolder.addPermission(aclPerm);
                }
            }
        }
        /*
         * Check if permission for user was added
         */
        if (!userPermAdded) {
            addOwnACL(mailFolder, secondaryAccount, imapFolder, session);
        }
    }

    /**
     * Adds current user's rights granted to IMAP folder as an ACL permission.
     *
     * @param mailFolder The mail folder containing own permission
     * @param optImapFolder The optional IMAP folder to check for deputy permission
     * @param session The session
     */
    private static void addOwnACL(MailFolder mailFolder, IMAPFolder optImapFolder, Session session) {
        addOwnACL(mailFolder, false, optImapFolder, session);
    }

    /**
     * Adds current user's rights granted to IMAP folder as an ACL permission.
     *
     * @param mailFolder The mail folder containing own permission
     * @param system Whether to add as system permission
     * @param optImapFolder The optional IMAP folder to check for deputy permission
     * @param session The session
     */
    private static void addOwnACL(MailFolder mailFolder, boolean system, IMAPFolder optImapFolder, Session session) {
        ACLPermission ownPermission = (ACLPermission) mailFolder.getOwnPermission();

        boolean isDeputyPermission = false;
        if (optImapFolder != null && mailFolder.isShared() && isDeputyServiceAvailable(session)) {
            try {
                TIntSet deputyEntities = IMAPDeputyMetadataUtility.getDeputyEntities(optImapFolder, Optional.empty());
                isDeputyPermission = deputyEntities.contains(ownPermission.getEntity());
            } catch (Exception e) {
                LOG.debug("Failed to retrieve deputy permission metadata from IMAP folder {}", optImapFolder.getFullName(), e);
            }
        }

        if (system || isDeputyPermission) {
            try {
                ACLPermission mp = (ACLPermission) ownPermission.clone();
                if (system) {
                    mp.setSystem(1);
                }
                if (isDeputyPermission) {
                    mp.setDeputyPermission(true);
                }
                mailFolder.addPermission(mp);
            } catch (Exception e) {
                throw new InternalError("Method clone() not supported although java.lang.Cloneable is implemented", e);
            }
        } else {
            mailFolder.addPermission(ownPermission);
        }
    }

    private static boolean isDeputyServiceAvailable(Session session) {
        try {
            DeputyService deputyService = Services.optService(DeputyService.class);
            return deputyService != null && deputyService.isAvailable(session);
        } catch (Exception e) {
            LOG.warn("Failed to check availability of deputy service. Assuming as absent.", e);
            return false;
        }
    }

    private static boolean isUnknownEntityError(OXException e) {
        final int code = e.getCode();
        return (e.isPrefix("ACL") && (Entity2ACLExceptionCode.RESOLVE_USER_FAILED.getNumber() == code)) || (e.isPrefix("USR") && (LdapExceptionCode.USER_NOT_FOUND.getNumber() == code));
    }

    private static boolean checkForNamespaceFolder(String fullName, IMAPStore imapStore, Session session, int accountId, IIMAPProperties imapProperties) throws MessagingException, OXException {
        /*
         * Check for namespace folder
         */
        {
            final List<Namespace> personalFolders = NamespacesCache.getPersonalNamespaces(imapStore, true, session, accountId);
            for (Namespace ns : personalFolders) {
                if (ns.getFullName().equals(fullName) || fullName.startsWith(ns.getPrefix())) {
                    return true;
                }
            }
        }
        {
            final List<Namespace> userFolders = NamespacesCache.getUserNamespaces(imapStore, true, session, accountId);
            for (Namespace ns : userFolders) {
                if (ns.getFullName().equals(fullName) || fullName.startsWith(ns.getPrefix())) {
                    return true;
                }
            }
        }
        {
            final List<Namespace> sharedFolders = NamespacesCache.getSharedNamespaces(imapStore, true, session, accountId);
            for (Namespace ns : sharedFolders) {
                if (ns.getFullName().equals(fullName) || fullName.startsWith(ns.getPrefix())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the unread count from given IMAP folder.
     *
     * @param imapFolder The IMAP folder
     * @return The unread count
     * @throws OXException If returning unread count fails
     */
    public static int getUnreadCount(IMAPFolder imapFolder) throws OXException {
        try {
            final int[] status = IMAPCommandsCollection.getStatus(imapFolder);
            return status[2];
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the session user's own rights.
     * <p>
     * <b>Note</b>: This method assumes all preconditions were met (exists, selectable, etc.) to perform MYRIGHTS command on specified IMAP
     * folder.
     *
     * @param folder The IMAP folder
     * @param session The session
     * @param imapConfig The IMAP configuration
     * @return The own rights
     * @throws MessagingException In case an unrecoverable exception occurs
     */
    public static Rights getOwnRights(IMAPFolder folder, Session session, IMAPConfig imapConfig) throws MessagingException {
        if (folder instanceof DefaultFolder) {
            return null;
        }
        final Rights retval;
        if (imapConfig.isSupportsACLs()) {
            try {
                retval = RightsCache.getCachedRights(folder, true, session, imapConfig.getAccountId());
            } catch (FolderClosedException | StoreClosedException e) {
                // Unable to recover from...
                throw e;
            } catch (MessagingException e) {
                Exception nextException = e.getNextException();
                if ((nextException instanceof com.sun.mail.iap.CommandFailedException)) {
                    /*
                     * Handle command failed exception
                     */
                    handleCommandFailedException(((com.sun.mail.iap.CommandFailedException) nextException), folder.getFullName());
                    return null;
                }
                LOG.error("", e);
                /*
                 * Write empty string as rights. Nevertheless user may see folder!
                 */
                return new Rights();
            } catch (Exception t) {
                LOG.error("", t);
                /*
                 * Write empty string as rights. Nevertheless user may see folder!
                 */
                return new Rights();
            }
        } else {
            /*
             * No ACLs enabled. User has full access.
             */
            retval = imapConfig.getACLExtension().getFullRights();
        }
        return retval;
    }

    private static void handleCommandFailedException(com.sun.mail.iap.CommandFailedException e, String fullName) {
        final String msg = e.getMessage().toLowerCase(Locale.ENGLISH);
        if (msg.indexOf("Mailbox doesn't exist") >= 0 || msg.indexOf("Mailbox does not exist") >= 0) {
            LOG.warn(IMAPException.getFormattedMessage(IMAPException.Code.FOLDER_NOT_FOUND, fullName), e);
        } else {
            LOG.debug("Failed MYRIGHTS for: {}", fullName, e);
        }
    }
}
