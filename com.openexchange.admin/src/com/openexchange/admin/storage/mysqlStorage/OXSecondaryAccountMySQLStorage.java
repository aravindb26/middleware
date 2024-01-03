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

package com.openexchange.admin.storage.mysqlStorage;

import static com.openexchange.admin.storage.mysqlStorage.AdminMySQLStorageUtil.leaseConnectionForContext;
import static com.openexchange.admin.storage.mysqlStorage.AdminMySQLStorageUtil.releaseWriteContextConnection;
import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Strings.isEmpty;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.mail.internet.idn.IDNA;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.rmi.dataobjects.Account;
import com.openexchange.admin.rmi.dataobjects.AccountData;
import com.openexchange.admin.rmi.dataobjects.AccountDataOnCreate;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.services.AdminServiceRegistry;
import com.openexchange.admin.storage.interfaces.OXSecondaryAccountStorageInterface;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheService;
import com.openexchange.context.ContextService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.contexts.UpdateBehavior;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.java.InetAddresses;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.MailAccounts;
import com.openexchange.mailaccount.TransportAuth;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.spamhandler.SpamHandler;
import com.openexchange.tools.net.URITools;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;


/**
 * {@link OXSecondaryAccountMySQLStorage}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class OXSecondaryAccountMySQLStorage extends OXSecondaryAccountStorageInterface {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OXSecondaryAccountMySQLStorage.class);
    }

    private final AdminCache cache;
    private final PropertyHandler prop;

    /**
     * Initializes a new {@link OXSecondaryAccountMySQLStorage}.
     */
    public OXSecondaryAccountMySQLStorage() {
        super();
        cache = ClientAdminThread.cache;
        prop = cache.getProperties();
    }

    @Override
    public void create(AccountDataOnCreate accountData, Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        if ((null == users || 0 == users.length) && (null == groups || 0 == groups.length)) {
            throw new StorageException("Missing users/groups in account data");
        }
        int rollback = 0;
        Connection con = leaseConnectionForContext(context.getId().intValue(), cache);
        try {
            con.setAutoCommit(false);
            rollback = 1;

            create(accountData, context, users, groups, con);

            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            releaseWriteContextConnection(con, context, cache);
        }
    }

    private void create(AccountDataOnCreate accountData, Context context, User[] users, Group[] groups, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int contextId = context.getId().intValue();

            User[] applicableUsers = resolveUsers(context, users, groups, false, con).users;
            if (applicableUsers == null || 0 == applicableUsers.length) {
                // No users in context
                return;
            }

            MailAccountStorageService mass = AdminServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (mass == null) {
                throw new StorageException("Missing " + MailAccountStorageService.class.getSimpleName());
            }

            ObfuscatorService obfuscator = AdminServiceRegistry.getInstance().getService(ObfuscatorService.class);
            if (obfuscator == null) {
                throw new StorageException("Missing " + ObfuscatorService.class.getSimpleName());
            }

            // TODO: How to store/encrypt secret?
            for (User user : applicableUsers) {
                int userId = user.getId().intValue();
                {
                    int accountId = getByPrimaryAddress(accountData.getPrimaryAddress(), userId, contextId, con);
                    if (accountId >= 0) {
                        // Such an account already exists for that user
                        throw MailAccountExceptionCodes.DUPLICATE_MAIL_ACCOUNT.create(I(userId), I(contextId));
                    }
                    checkDuplicateMailAccountOnInsert(accountData, userId, contextId, con, mass);
                }
                int id = IDGenerator.getId(contextId, com.openexchange.groupware.Types.MAIL_SERVICE, con);
                MailAccount defaultMailAccount = null;
                {
                    String encryptedPassword;
                    if (accountData.getPassword() == null) {
                        encryptedPassword = null;
                    } else {
                        encryptedPassword = obfuscator.obfuscate(accountData.getPassword());
                    }

                    String mailServerURL;
                    boolean mailStartTls;
                    switch (accountData.getMailEndpointSource()) {
                        case PRIMARY:
                            defaultMailAccount = mass.getDefaultMailAccount(userId, contextId);
                            mailServerURL = generateMailServerURL(defaultMailAccount);
                            mailStartTls = defaultMailAccount.isMailStartTls();
                            break;
                        case LOCALHOST:
                            mailServerURL = "imap://localhost:143";
                            mailStartTls = true;
                            break;
                        default:
                            mailServerURL = generateMailServerURL(accountData);
                            mailStartTls = accountData.isMailStartTls();
                            break;
                    }
                    if (mailServerURL == null) {
                        throw new StorageException("Missing mail end-point data. Set end-point source to either \"primary\" or \"localhost\"; or specify appropriate fields in account data");
                    }

                    stmt = con.prepareStatement("INSERT INTO user_mail_account (cid, id, user, name, url, login, password, primary_addr, default_flag, trash, sent, drafts, spam, confirmed_spam, confirmed_ham, spam_handler, unified_inbox, trash_fullname, sent_fullname, drafts_fullname, spam_fullname, confirmed_spam_fullname, confirmed_ham_fullname, personal, replyTo, archive, archive_fullname, starttls, oauth, secondary, deactivated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    int pos = 1;
                    stmt.setLong(pos++, contextId);
                    stmt.setLong(pos++, id);
                    stmt.setLong(pos++, userId);
                    stmt.setString(pos++, accountData.getName());
                    stmt.setString(pos++, mailServerURL);
                    stmt.setString(pos++, accountData.getLogin());
                    if (encryptedPassword == null) {
                        stmt.setNull(pos++, Types.VARCHAR);
                    } else {
                        setOptionalString(stmt, pos++, encryptedPassword);
                    }
                    stmt.setString(pos++, accountData.getPrimaryAddress());
                    stmt.setInt(pos++, 0);
                    // Default folder names: trash, sent, drafts, spam, confirmed_spam, confirmed_ham
                    {
                        stmt.setString(pos++, "");
                        stmt.setString(pos++, "");
                        stmt.setString(pos++, "");
                        stmt.setString(pos++, "");
                        stmt.setString(pos++, "");
                        stmt.setString(pos++, "");
                    }
                    // Spam handler
                    stmt.setString(pos++, SpamHandler.SPAM_HANDLER_FALLBACK);
                    stmt.setInt(pos++, 0);
                    // Default folder full names
                    {
                        stmt.setString(pos++, extractFullname(accountData.getTrashFullname()));
                        stmt.setString(pos++, extractFullname(accountData.getSentFullname()));
                        stmt.setString(pos++, extractFullname(accountData.getDraftsFullname()));
                        stmt.setString(pos++, extractFullname(accountData.getSpamFullname()));
                        stmt.setString(pos++, extractFullname(accountData.getConfirmedSpamFullname()));
                        stmt.setString(pos++, extractFullname(accountData.getConfirmedHamFullname()));
                    }
                    // Personal
                    final String personal = accountData.getPersonal();
                    if (isEmpty(personal)) {
                        stmt.setNull(pos++, Types.VARCHAR);
                    } else {
                        stmt.setString(pos++, personal);
                    }
                    final String replyTo = accountData.getReplyTo();
                    if (isEmpty(replyTo)) {
                        stmt.setNull(pos++, Types.VARCHAR);
                    } else {
                        stmt.setString(pos++, replyTo);
                    }
                    // Archive
                    setOptionalString(stmt, pos++, null);
                    setOptionalString(stmt, pos++, accountData.getArchiveFullname());
                    // starttls flag
                    stmt.setInt(pos++, mailStartTls ? 1 : 0);
                    // OAuth account identifier
                    stmt.setNull(pos++, Types.INTEGER);
                    // Secondary flag
                    stmt.setInt(pos++, 1);
                    // Deactivated flag
                    stmt.setInt(pos++, 0);
                    stmt.executeUpdate();
                    Databases.closeSQLStuff(stmt);
                    stmt = null;
                }

                // Transport data
                String transportURL;
                boolean transportStartTls;
                switch (accountData.getMailEndpointSource()) {
                    case PRIMARY:
                        if (defaultMailAccount == null) {
                            defaultMailAccount = mass.getDefaultMailAccount(userId, contextId);
                        }
                        transportURL = generateTransportServerURL(defaultMailAccount);
                        transportStartTls = defaultMailAccount.isTransportStartTls();
                        break;
                    case LOCALHOST:
                        transportURL = "smtp://localhost:25";
                        transportStartTls = true;
                        break;
                    default:
                        transportURL = generateTransportServerURL(accountData);
                        transportStartTls = accountData.isTransportStartTls();
                        break;
                }

                if (null != transportURL) {
                    String encryptedTransportPassword;
                    if (accountData.getTransportPassword() == null) {
                        encryptedTransportPassword = null;
                    } else {
                        encryptedTransportPassword = obfuscator.obfuscate(accountData.getTransportPassword());
                    }

                    stmt = con.prepareStatement("INSERT INTO user_transport_account (cid, id, user, name, url, login, password, send_addr, default_flag, personal, replyTo, starttls, oauth, secondary, disabled) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                    // cid, id, user, name, url, login, password, send_addr, default_flag
                    int pos = 1;
                    stmt.setLong(pos++, contextId);
                    stmt.setLong(pos++, id);
                    stmt.setLong(pos++, userId);
                    stmt.setString(pos++, accountData.getName());
                    stmt.setString(pos++, transportURL);
                    if (null == accountData.getTransportLogin()) {
                        stmt.setString(pos++, "");
                    } else {
                        stmt.setString(pos++, accountData.getTransportLogin());
                    }
                    if (encryptedTransportPassword == null) {
                        stmt.setNull(pos++, Types.VARCHAR);
                    } else {
                        setOptionalString(stmt, pos++, encryptedTransportPassword);
                    }
                    stmt.setString(pos++, accountData.getPrimaryAddress());
                    stmt.setInt(pos++, 0);
                    final String personal = accountData.getPersonal();
                    if (isEmpty(personal)) {
                        stmt.setNull(pos++, Types.VARCHAR);
                    } else {
                        stmt.setString(pos++, personal);
                    }
                    final String replyTo = accountData.getReplyTo();
                    if (isEmpty(replyTo)) {
                        stmt.setNull(pos++, Types.VARCHAR);
                    } else {
                        stmt.setString(pos++, replyTo);
                    }

                    // STARTTLS flag
                    stmt.setInt(pos++, transportStartTls ? 1 : 0);

                    // OAuth account identifier
                    stmt.setNull(pos++, Types.INTEGER);

                    // Secondary flag
                    stmt.setInt(pos++, 1);

                    // Deactivated flag
                    stmt.setInt(pos++, 0);

                    // Execute update
                    stmt.executeUpdate();
                    Databases.closeSQLStuff(stmt);
                    stmt = null;

                    updateProperty(contextId, userId, id, "transport.auth", TransportAuth.MAIL.getId(), true, con);
                }

                // Invalidate cache
                try {
                    CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
                    if (null != cacheService) {
                        Cache cache = cacheService.getCache("MailAccount");
                        cache.remove(cacheService.newCacheKey(contextId, Integer.toString(0), Integer.toString(userId)));
                        cache.remove(cacheService.newCacheKey(contextId, Integer.toString(userId)));
                        cache.invalidateGroup(context.getId().toString());
                    }
                } catch (OXException e) {
                    LoggerHolder.LOG.error("", e);
                }
            }
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } catch (OXException e) {
            LoggerHolder.LOG.error("Open-Xchange Error", e);
            Throwable optCause = getNonOXExceptionCauseFrom(e);
            throw new StorageException(optCause != null ? optCause : e);
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public boolean update(String primaryAddress, AccountData accountData, Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        if ((null == users || 0 == users.length) && (null == groups || 0 == groups.length)) {
            throw new StorageException("Missing users/groups in account data");
        }
        int rollback = 0;
        Connection con = leaseConnectionForContext(context.getId().intValue(), cache);
        try {
            con.setAutoCommit(false);
            rollback = 1;

            boolean updated = update(primaryAddress, accountData, context, users, groups, con);

            con.commit();
            rollback = 2;
            return updated;
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            releaseWriteContextConnection(con, context, cache);
        }
    }

    private boolean update(String primaryAddress, AccountData accountData, Context context, User[] users, Group[] groups, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int contextId = context.getId().intValue();

            User[] usersToIterate = resolveUsers(context, users, groups, false, con).users;
            if (usersToIterate == null) {
                // No users in context
                return false;
            }

            MailAccountStorageService mass = AdminServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (mass == null) {
                throw new StorageException("Missing " + MailAccountStorageService.class.getSimpleName());
            }

            Set<Attribute> attributes = EnumSet.noneOf(Attribute.class);
            MailAccountDescription mailAccountDesc = toMailAccountDescription(accountData, attributes);

            boolean updated = false;
            for (User user : usersToIterate) {
                int userId = user.getId().intValue();
                int accountId = getByPrimaryAddress(primaryAddress, userId, contextId, con);
                if (accountId >= 0) {
                    mailAccountDesc.setId(accountId);
                    mass.updateMailAccount(mailAccountDesc, attributes, userId, contextId, null, con, true);
                    updated = true;
                }
            }
            return updated;
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } catch (OXException e) {
            LoggerHolder.LOG.error("Open-Xchange Error", e);
            Throwable optCause = getNonOXExceptionCauseFrom(e);
            throw new StorageException(optCause != null ? optCause : e);
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private MailAccountDescription toMailAccountDescription(AccountData accountData, Set<Attribute> attributes) {
        MailAccountDescription desc = new MailAccountDescription();

        if (accountData.isLoginset()) {
            desc.setLogin(accountData.getLogin());
            attributes.add(Attribute.LOGIN_LITERAL);
        }

        if (accountData.isPasswordset()) {
            desc.setPassword(accountData.getPassword());
            attributes.add(Attribute.PASSWORD_LITERAL);
        }

        if (accountData.isNameset()) {
            desc.setName(accountData.getName());
            attributes.add(Attribute.NAME_LITERAL);
        }

        if (accountData.isPersonalset()) {
            desc.setPersonal(accountData.getPersonal());
            attributes.add(Attribute.PERSONAL_LITERAL);
        }

        if (accountData.isReplyToset()) {
            desc.setReplyTo(accountData.getReplyTo());
            attributes.add(Attribute.REPLY_TO_LITERAL);
        }

        //--------------------------------------------------------------------------------------

        if (accountData.isMailServerset()) {
            desc.setMailServer(accountData.getMailServer());
            attributes.add(Attribute.MAIL_SERVER_LITERAL);
        }

        if (accountData.isMailPortset()) {
            desc.setMailPort(accountData.getMailPort());
            attributes.add(Attribute.MAIL_PORT_LITERAL);
        }

        if (accountData.isMailProtocolset()) {
            desc.setMailProtocol(accountData.getMailProtocol());
            attributes.add(Attribute.MAIL_PROTOCOL_LITERAL);
        }

        if (accountData.isMailSecureset()) {
            desc.setMailSecure(accountData.isMailSecure());
            attributes.add(Attribute.MAIL_SECURE_LITERAL);
        }

        if (accountData.isMailStartTlsset()) {
            desc.setMailStartTls(accountData.isMailStartTls());
            attributes.add(Attribute.MAIL_STARTTLS_LITERAL);
        }

        // ------------------------------------------------------------------------------------

        if (accountData.isTransportLoginset()) {
            desc.setTransportLogin(accountData.getTransportLogin());
            attributes.add(Attribute.TRANSPORT_LOGIN_LITERAL);
        }

        if (accountData.isTransportPasswordset()) {
            desc.setTransportPassword(accountData.getTransportPassword());
            attributes.add(Attribute.TRANSPORT_PASSWORD_LITERAL);
        }

        if (accountData.isTransportServerset()) {
            desc.setTransportServer(accountData.getTransportServer());
            attributes.add(Attribute.TRANSPORT_SERVER_LITERAL);
        }

        if (accountData.isTransportPortset()) {
            desc.setTransportPort(accountData.getTransportPort());
            attributes.add(Attribute.TRANSPORT_PORT_LITERAL);
        }

        if (accountData.isTransportProtocolset()) {
            desc.setTransportProtocol(accountData.getTransportProtocol());
            attributes.add(Attribute.TRANSPORT_PROTOCOL_LITERAL);
        }

        if (accountData.isTransportSecureset()) {
            desc.setTransportSecure(accountData.isTransportSecure());
            attributes.add(Attribute.TRANSPORT_SECURE_LITERAL);
        }

        if (accountData.isTransportStartTlsset()) {
            desc.setTransportStartTls(accountData.isTransportStartTls());
            attributes.add(Attribute.TRANSPORT_STARTTLS_LITERAL);
        }

        // ------------------------------------------------------------------------------------

        if (accountData.isArchiveFullnameset()) {
            desc.setArchiveFullname(accountData.getArchiveFullname());
            attributes.add(Attribute.ARCHIVE_FULLNAME_LITERAL);
        }

        if (accountData.isDraftsFullnameset()) {
            desc.setDraftsFullname(accountData.getDraftsFullname());
            attributes.add(Attribute.DRAFTS_FULLNAME_LITERAL);
        }

        if (accountData.isSentFullnameset()) {
            desc.setSentFullname(accountData.getSentFullname());
            attributes.add(Attribute.SENT_FULLNAME_LITERAL);
        }

        if (accountData.isSpamFullnameset()) {
            desc.setSpamFullname(accountData.getSpamFullname());
            attributes.add(Attribute.SPAM_FULLNAME_LITERAL);
        }

        if (accountData.isTrashFullnameset()) {
            desc.setTrashFullname(accountData.getTrashFullname());
            attributes.add(Attribute.TRASH_FULLNAME_LITERAL);
        }

        if (accountData.isConfirmedSpamFullnameset()) {
            desc.setConfirmedSpamFullname(accountData.getConfirmedSpamFullname());
            attributes.add(Attribute.CONFIRMED_SPAM_FULLNAME_LITERAL);
        }

        if (accountData.isConfirmedHamFullnameset()) {
            desc.setConfirmedHamFullname(accountData.getConfirmedHamFullname());
            attributes.add(Attribute.CONFIRMED_HAM_FULLNAME_LITERAL);
        }

        return desc;
    }

    @Override
    public boolean delete(String primaryAddress, Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        if ((null == users || 0 == users.length) && (null == groups || 0 == groups.length)) {
            throw new StorageException("Missing users/groups in account data");
        }
        int rollback = 0;
        Connection con = leaseConnectionForContext(context.getId().intValue(), cache);
        try {
            con.setAutoCommit(false);
            rollback = 1;

            boolean deleted = delete(primaryAddress, context, users, groups, con);

            con.commit();
            rollback = 2;
            return deleted;
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            releaseWriteContextConnection(con, context, cache);
        }
    }

    private boolean delete(String primaryAddress, Context context, User[] users, Group[] groups, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int contextId = context.getId().intValue();

            User[] usersToIterate = resolveUsers(context, users, groups, false, con).users;
            if (usersToIterate == null) {
                // No users in context
                return false;
            }

            MailAccountStorageService mass = AdminServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (mass == null) {
                throw new StorageException("Missing " + MailAccountStorageService.class.getSimpleName());
            }

            boolean deleted = false;
            for (User user : usersToIterate) {
                int userId = user.getId().intValue();
                int[] accountIds = getAllByPrimaryAddress(primaryAddress, userId, contextId, con);
                if (accountIds != null && accountIds.length > 0) {
                    for (int accountId : accountIds) {
                        deleted |= mass.deleteMailAccount(accountId, null, userId, contextId, true, con);
                    }
                }
            }
            return deleted;
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } catch (OXException e) {
            LoggerHolder.LOG.error("Open-Xchange Error", e);
            Throwable optCause = getNonOXExceptionCauseFrom(e);
            throw new StorageException(optCause != null ? optCause : e);
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public Account[] list(Context context, User[] users, Group[] groups) throws RemoteException, StorageException, NoSuchContextException, NoSuchUserException, InvalidDataException, DatabaseUpdateException {
        Connection con = leaseConnectionForContext(context.getId().intValue(), cache);
        try {
            return list(context, users, groups, con);
        } finally {
            releaseWriteContextConnection(con, context, cache);
        }
    }

    private Account[] list(Context context, User[] users, Group[] groups, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            int contextId = context.getId().intValue();

            MailAccountStorageService mass = AdminServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (mass == null) {
                throw new StorageException("Missing " + MailAccountStorageService.class.getSimpleName());
            }

            ObfuscatorService obfuscator = AdminServiceRegistry.getInstance().getService(ObfuscatorService.class);
            if (obfuscator == null) {
                throw new StorageException("Missing " + ObfuscatorService.class.getSimpleName());
            }

            ResolveResult resolveResult = resolveUsers(context, users, groups, false, con);
            if (resolveResult.users == null && resolveResult.all == false) {
                // No users in context
                return new Account[0];
            }

            List<IdAndUserId> ids = null;
            if (resolveResult.all) {
                stmt = con.prepareStatement("SELECT id, user FROM user_mail_account WHERE cid = ? AND secondary = 1");
                stmt.setLong(1, contextId);
                result = stmt.executeQuery();
                if (!result.next()) {
                    // No secondary accounts in context
                    return new Account[0];
                }
                ids = new ArrayList<>();
                do {
                    ids.add(new IdAndUserId(result.getInt(1), result.getInt(2)));
                } while (result.next());
                closeSQLStuff(result, stmt);
                result = null;
                stmt = null;
            } else {
                User[] resolvedUsers = resolveResult.users;
                int limit = Databases.IN_LIMIT;
                int length = resolvedUsers.length;
                for (int off = 0; off < length; off += limit) {
                    int clen = off + limit > length ? length - off : limit;
                    stmt = con.prepareStatement(Databases.getIN("SELECT id, user FROM user_mail_account WHERE cid = ? AND secondary = 1 AND user IN (", clen));
                    int pos = 1;
                    stmt.setLong(pos++, contextId);
                    for (int j = 0; j < clen; j++) {
                        stmt.setInt(pos++, resolvedUsers[off+j].getId().intValue());
                    }
                    result = stmt.executeQuery();
                    if (result.next()) {
                        if (ids == null) {
                            ids = new ArrayList<>();
                        }
                        do {
                            ids.add(new IdAndUserId(result.getInt(1), result.getInt(2)));
                        } while (result.next());
                    }
                    closeSQLStuff(result, stmt);
                    result = null;
                    stmt = null;
                }
                if (ids == null) {
                    // No secondary accounts in context
                    return new Account[0];
                }
            }

            List<Account> accounts = new ArrayList<>(ids.size());
            for (IdAndUserId id : ids) {
                MailAccount mailAccount = mass.getMailAccount(id.accountId, id.userId, contextId);
                Account account = new Account();
                account.setContextId(Integer.valueOf(contextId));
                account.setUserId(Integer.valueOf(id.userId));
                account.setArchiveFullname(mailAccount.getArchiveFullname());
                account.setConfirmedHamFullname(mailAccount.getConfirmedHamFullname());
                account.setConfirmedSpamFullname(mailAccount.getConfirmedSpamFullname());
                account.setDraftsFullname(mailAccount.getDraftsFullname());
                account.setId(Integer.valueOf(mailAccount.getId()));
                account.setLogin(mailAccount.getLogin());
                account.setMailPort(mailAccount.getMailPort());
                account.setMailProtocol(mailAccount.getMailProtocol());
                account.setMailSecure(mailAccount.isMailSecure());
                account.setMailServer(mailAccount.getMailServer());
                account.setMailStartTls(mailAccount.isMailStartTls());
                account.setName(mailAccount.getName());
                account.setPassword(mailAccount.getPassword() == null ? null : obfuscator.unobfuscate(mailAccount.getPassword()));
                account.setPersonal(mailAccount.getPersonal());
                account.setPrimaryAddress(mailAccount.getPrimaryAddress());
                account.setReplyTo(mailAccount.getReplyTo());
                account.setSentFullname(mailAccount.getSentFullname());
                account.setSpamFullname(mailAccount.getSpamFullname());
                account.setTransportLogin(mailAccount.getTransportLogin());
                account.setTransportPassword(mailAccount.getTransportPassword() == null ? null : obfuscator.unobfuscate(mailAccount.getTransportPassword()));
                account.setTransportPort(mailAccount.getTransportPort());
                account.setTransportProtocol(mailAccount.getTransportProtocol());
                account.setTransportSecure(mailAccount.isTransportSecure());
                account.setTransportServer(mailAccount.getTransportServer());
                account.setTransportStartTls(mailAccount.isTransportStartTls());
                account.setTrashFullname(mailAccount.getTrashFullname());
                accounts.add(account);
            }
            return accounts.toArray(new Account[accounts.size()]);
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } catch (OXException e) {
            LoggerHolder.LOG.error("Open-Xchange Error", e);
            Throwable optCause = getNonOXExceptionCauseFrom(e);
            throw new StorageException(optCause != null ? optCause : e);
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private void checkDuplicateMailAccountOnInsert(AccountData accountData, int userId, int contextId, Connection con, MailAccountStorageService mass) throws OXException {
        final String server = accountData.getMailServer();
        if (isEmpty(server)) {
            /*
             * No mail server specified
             */
            return;
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT id, url, login FROM user_mail_account WHERE cid = ? AND user = ?");
            stmt.setLong(1, contextId);
            stmt.setLong(2, userId);
            result = stmt.executeQuery();
            if (!result.next()) {
                return;
            }
            InetAddress addr;
            try {
                addr = InetAddresses.forString(IDNA.toASCII(server));
            } catch (UnknownHostException e) {
                LoggerHolder.LOG.warn("Unable to resolve host name '{}' to an IP address", server, e);
                addr = null;
            }
            int port = accountData.getMailPort();
            String login = accountData.getLogin();
            do {
                final int id = (int) result.getLong(1);
                MailAccount current = mass.getMailAccount(id, userId, contextId);
                if (MailAccounts.checkMailServer(server, addr, current.getMailServer()) && MailAccounts.checkProtocol(accountData.getMailProtocol(), current.getMailProtocol()) && current.getMailPort() == port && (null != login && login.equals(result.getString(3)))) {
                    throw MailAccountExceptionCodes.DUPLICATE_MAIL_ACCOUNT.create(I(userId), I(contextId));
                }
            } while (result.next());
        } catch (SQLException e) {
            throw MailAccountExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    private int getByPrimaryAddress(String primaryAddress, int userId, int contextId, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT id FROM user_mail_account WHERE cid = ? AND primary_addr = ? AND user = ?");
            stmt.setLong(1, contextId);
            stmt.setString(2, primaryAddress);
            stmt.setLong(3, userId);
            result = stmt.executeQuery();
            if (!result.next()) {
                return -1;
            }
            int id = result.getInt(1);
            if (result.next()) {
                throw new StorageException("Found two mail accounts with same E-Mail address " + primaryAddress + " for user " + userId + " in context " + contextId);
            }
            return id;
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    private int[] getAllByPrimaryAddress(String primaryAddress, int userId, int contextId, Connection con) throws StorageException {
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT id FROM user_mail_account WHERE cid = ? AND primary_addr = ? AND user = ?");
            stmt.setLong(1, contextId);
            stmt.setString(2, primaryAddress);
            stmt.setLong(3, userId);
            result = stmt.executeQuery();
            if (!result.next()) {
                return new int[0];
            }
            TIntList ids = new TIntArrayList(2);
            do {
                ids.add(result.getInt(1));
            } while (result.next());
            return ids.toArray();
        } catch (SQLException e) {
            LoggerHolder.LOG.error("SQL Error", e);
            throw new StorageException(e);
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    private void updateProperty(final int contextId, final int userId, final int accountId, final String name, final String newValue, final boolean transportProps, final Connection con) throws SQLException {
        if (null == newValue || newValue.length() <= 0) {
            deleteProperty(contextId, userId, accountId, name, transportProps, con);
            return;
        }

        PreparedStatement stmt = null;
        try {
            if (transportProps) {
                stmt = con.prepareStatement("INSERT INTO user_transport_account_properties (cid, user, id, name, value) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE value=?");
            } else {
                stmt = con.prepareStatement("INSERT INTO user_mail_account_properties (cid, user, id, name, value) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE value=?");
            }
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setInt(pos++, accountId);
            stmt.setString(pos++, name);
            stmt.setString(pos++, newValue);
            stmt.setString(pos++, newValue);
            stmt.executeUpdate();
            closeSQLStuff(stmt);
            stmt = null;
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private void deleteProperty(int contextId, int userId, int accountId, String name, boolean transportProps, Connection con) throws SQLException{
        PreparedStatement stmt = null;
        try {
            if (transportProps) {
                stmt = con.prepareStatement("DELETE FROM user_transport_account_properties WHERE cid = ? AND user = ? AND id = ? AND name = ?");
            } else {
                stmt = con.prepareStatement("DELETE FROM user_mail_account_properties WHERE cid = ? AND user = ? AND id = ? AND name = ?");
            }
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            stmt.setInt(pos++, accountId);
            stmt.setString(pos++, name);
            stmt.executeUpdate();
        } finally {
            closeSQLStuff(stmt);
        }
    }

    private String generateMailServerURL(AccountData accountData) throws StorageException {
        if (com.openexchange.java.Strings.isEmpty(accountData.getMailServer())) {
            return null;
        }
        try {
            return URITools.generateURI(accountData.isMailSecure() ? accountData.getMailProtocol() + 's' : accountData.getMailProtocol(), IDNA.toASCII(accountData.getMailServer()), accountData.getMailPort()).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(accountData.getMailProtocol());
            if (accountData.isMailSecure()) {
                sb.append('s');
            }
            throw new StorageException("Invalid host name: " + sb.append("://").append(accountData.getMailServer()).append(':').append(accountData.getMailPort()).toString());
        }
    }

    private String generateMailServerURL(MailAccount mailAccount) throws StorageException {
        if (com.openexchange.java.Strings.isEmpty(mailAccount.getMailServer())) {
            return null;
        }
        try {
            return URITools.generateURI(mailAccount.isMailSecure() ? mailAccount.getMailProtocol() + 's' : mailAccount.getMailProtocol(), IDNA.toASCII(mailAccount.getMailServer()), mailAccount.getMailPort()).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(mailAccount.getMailProtocol());
            if (mailAccount.isMailSecure()) {
                sb.append('s');
            }
            throw new StorageException("Invalid host name: " + sb.append("://").append(mailAccount.getMailServer()).append(':').append(mailAccount.getMailPort()).toString());
        }
    }

    private String generateTransportServerURL(AccountData accountData) throws StorageException {
        if (com.openexchange.java.Strings.isEmpty(accountData.getTransportServer())) {
            return null;
        }
        try {
            return URITools.generateURI(accountData.isTransportSecure() ? accountData.getTransportProtocol() + 's' : accountData.getTransportProtocol(), IDNA.toASCII(accountData.getTransportServer()), accountData.getTransportPort()).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(accountData.getTransportProtocol());
            if (accountData.isTransportSecure()) {
                sb.append('s');
            }
            throw new StorageException("Invalid host name: " + sb.append("://").append(accountData.getTransportServer()).append(':').append(accountData.getTransportPort()).toString());
        }
    }

    private String generateTransportServerURL(com.openexchange.mailaccount.Account account) throws StorageException {
        if (com.openexchange.java.Strings.isEmpty(account.getTransportServer())) {
            return null;
        }
        try {
            return URITools.generateURI(account.isTransportSecure() ? account.getTransportProtocol() + 's' : account.getTransportProtocol(), IDNA.toASCII(account.getTransportServer()), account.getTransportPort()).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(account.getTransportProtocol());
            if (account.isTransportSecure()) {
                sb.append('s');
            }
            throw new StorageException("Invalid host name: " + sb.append("://").append(account.getTransportServer()).append(':').append(account.getTransportPort()).toString());
        }
    }

   private static String extractFullname(String fullnameParameter) {
       return null == fullnameParameter ? null : MailFolderUtility.prepareMailFolderParamOrElseReturn(fullnameParameter);
   }

    private ResolveResult resolveUsers(Context context, User[] users, Group[] groups, boolean resolveInCaseOfAll, Connection con) throws SQLException, StorageException, OXException {
        Set<Integer> resolvedIds = new HashSet<Integer>();
        List<User> resolvedOnes = new ArrayList<User>();
        if (users != null && users.length > 0) {
            for (User user : users) {
                if (user != null && resolvedIds.add(user.getId())) {
                    resolvedOnes.add(user);
                }
            }
        }

        if (groups != null && groups.length > 0) {
            GroupService groupService = AdminServiceRegistry.getInstance().getService(GroupService.class);
            if (groupService == null) {
                throw new StorageException("Missing " + GroupService.class.getSimpleName());
            }

            ContextService contextService = AdminServiceRegistry.getInstance().getService(ContextService.class);
            if (contextService == null) {
                throw new StorageException("Missing " + ContextService.class.getSimpleName());
            }

            for (Group group : groups) {
                if (group != null) {
                    int groupId = group.getId().intValue();
                    int[] members = groupService.getGroup(contextService.getContext(context.getId().intValue(), UpdateBehavior.DENY_UPDATE), groupId, true).getMember();
                    for (int userId : members) {
                        if (resolvedIds.add(I(userId))) {
                            resolvedOnes.add(new User(userId));
                        }
                    }
                }
            }
        }

        if (null != users && 0 < users.length || null != groups && 0 < groups.length) {
            return new ResolveResult(resolvedOnes.toArray(new User[resolvedOnes.size()]), false);
        }

        // Consider all users
        if (resolveInCaseOfAll == false) {
            // No need to load them
            return new ResolveResult(null, true);
        }

        // Load all users from context
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            int contextId = context.getId().intValue();
            stmt = con.prepareStatement("SELECT id FROM user WHERE cid=? AND guestCreatedBy=0");
            stmt.setInt(1, contextId);
            rs = stmt.executeQuery();
            if (rs.next() == false) {
                // No users in context
                return null;
            }
            TIntList userIds = new TIntArrayList();
            do {
                userIds.add(rs.getInt(1));
            } while (rs.next());
            Databases.closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;

            User[] allUsers = new User[userIds.size()];
            int i = 0;
            for (TIntIterator it = userIds.iterator(); it.hasNext();) {
                int userId = it.next();
                allUsers[i++] = new User(userId);
            }
            return new ResolveResult(allUsers, true);
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private static void setOptionalString(final PreparedStatement stmt, final int pos, final String string) throws SQLException {
        stmt.setString(pos, null == string ? "" : string);
    }

    private static Throwable getNonOXExceptionCauseFrom(OXException e) {
        Throwable cause = e.getCause();
        return cause instanceof OXException ? getNonOXExceptionCauseFrom((OXException) cause) : cause;
    }

    private static class IdAndUserId {

        final int accountId;
        final int userId;

        IdAndUserId(int accountId, int userId) {
            super();
            this.accountId = accountId;
            this.userId = userId;
        }
    }

    private static class ResolveResult {

        final User[] users;
        final boolean all;

        ResolveResult(User[] users, boolean all) {
            super();
            this.users = users;
            this.all = all;
        }
    }

}
