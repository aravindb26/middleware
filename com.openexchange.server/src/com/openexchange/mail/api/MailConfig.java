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

package com.openexchange.mail.api;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.utils.ProviderUtility.toSocketAddrString;
import static com.openexchange.session.Sessions.isGuest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.mail.internet.AddressException;
import javax.mail.internet.idn.IDNA;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailAuthenticator;
import com.openexchange.mail.MailAuthenticatorRegistry;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.config.ConfiguredServer;
import com.openexchange.mail.config.MailConfigException;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.config.MailReloadable;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.oauth.MailOAuthExceptionCodes;
import com.openexchange.mail.oauth.MailOAuthService;
import com.openexchange.mail.oauth.TokenInfo;
import com.openexchange.mail.utils.ImmutableReference;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mail.utils.MailPasswordUtil;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.AccountNature;
import com.openexchange.mailaccount.Credentials;
import com.openexchange.mailaccount.CredentialsProviderRegistry;
import com.openexchange.mailaccount.CredentialsProviderService;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.MailAccounts;
import com.openexchange.mailaccount.Password;
import com.openexchange.mailaccount.TransportAuth;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link MailConfig} - The user-specific mail properties; e.g. containing user's login data.
 * <p>
 * Provides access to global mail properties.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class MailConfig {

    public static final String MISSING_SESSION_PASSWORD = "MISSING_SESSION_PW";

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(MailConfig.class);
    }

    private static final class AuthTypeKey {

        final boolean forMailAccess;
        final AccountNature accountNature;
        final int userId;
        final int contextId;
        private final int hash;

        AuthTypeKey(boolean forMailAccess, AccountNature accountNature, int userId, int contextId) {
            super();
            this.forMailAccess = forMailAccess;
            this.accountNature = accountNature;
            this.userId = userId;
            this.contextId = contextId;

            int prime = 31;
            int result = 1;
            result = prime * result + (forMailAccess ? 1231 : 1237);
            result = prime * result + contextId;
            result = prime * result + userId;
            result = prime * result + (accountNature == null ? 0 : (accountNature.ordinal() + 1));
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
            AuthTypeKey other = (AuthTypeKey) obj;
            if (forMailAccess != other.forMailAccess) {
                return false;
            }
            if (contextId != other.contextId) {
                return false;
            }
            if (userId != other.userId) {
                return false;
            }
            if (accountNature == null) {
                if (other.accountNature != null) {
                    return false;
                }
            } else if (!accountNature.equals(other.accountNature)) {
                return false;
            }
            return true;
        }
    }

    public static enum BoolCapVal {

        /**
         * AUTO
         */
        AUTO("auto"),
        /**
         * FALSE
         */
        FALSE("false"),
        /**
         * TRUE
         */
        TRUE("true");

        private final String str;

        private BoolCapVal(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

        /**
         * Parses given capability value. If given value equals ignore-case to string <code>true</code>, constant {@link #TRUE} will be
         * returned. Else if given value equals ignore-case to string <code>auto</code>, constant {@link #AUTO} will be returned. Otherwise
         * {@link #FALSE} will be returned.
         *
         * @param capVal - the string value to parse
         * @return an instance of <code>BoolCapVal</code>: either {@link #TRUE}, {@link #FALSE}, or {@link #AUTO}
         */
        public static BoolCapVal parseBoolCapVal(String capVal) {
            if (TRUE.str.equalsIgnoreCase(capVal)) {
                return TRUE;
            } else if (AUTO.str.equalsIgnoreCase(capVal)) {
                return AUTO;
            }
            return FALSE;
        }
    }

    public static enum LoginSource {

        /**
         * Login is taken from user.mail kept in storage; e.g. <code>test@foo.bar</code>
         */
        PRIMARY_EMAIL("mail"),
        /**
         * Login is taken from user.imapLogin kept in storage; e.g. <code>test</code>
         */
        USER_IMAPLOGIN("login"),
        /**
         * Login is user's name; e.g. <code>test</code><br>
         * <b>Not applicable for secondary accounts!</b>
         */
        USER_NAME("name");

        private final String str;

        private LoginSource(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

        /**
         * Parses specified string into a login source.
         *
         * @param loginSourceStr The string to parse to a login source
         * @return An appropriate login source or <code>null</code> if string could not be parsed to a login source
         */
        public static LoginSource parse(String loginSourceStr) {
            if (Strings.isEmpty(loginSourceStr)) {
                return null;
            }

            String lookUp = Strings.asciiLowerCase(loginSourceStr.trim());
            for (LoginSource loginSource : LoginSource.values()) {
                if (loginSource.str.equals(lookUp)) {
                    return loginSource;
                }
            }
            return null;
        }
    }

    public static enum PasswordSource {

        /**
         * Password is taken from appropriate property
         */
        GLOBAL("global"),
        /**
         * Password is equal to session password
         */
        SESSION("session"),
        /**
         * Password is taken from account settings<br>
         * <b>Only applicable for secondary accounts</b>.
         */
        USER("user");

        private final String str;

        private PasswordSource(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

        /**
         * Parses specified string into a password source.
         *
         * @param passwordSourceStr The string to parse to a password source
         * @return An appropriate password source or <code>null</code> if string could not be parsed to a password source
         */
        public static PasswordSource parse(String passwordSourceStr) {
            if (Strings.isEmpty(passwordSourceStr)) {
                return null;
            }

            String lookUp = Strings.asciiLowerCase(passwordSourceStr.trim());
            for (PasswordSource passwordSource : PasswordSource.values()) {
                if (passwordSource.str.equals(lookUp)) {
                    return passwordSource;
                }
            }
            return null;
        }
    } // End of enum PasswordSource

    public static enum ServerSource {

        /**
         * Server is taken from appropriate property
         */
        GLOBAL("global"),
        /**
         * Server is taken from user
         */
        USER("user");

        private final String str;

        private ServerSource(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

        /**
         * Parses specified string into a server source.
         *
         * @param serverSourceStr The string to parse to a server source
         * @return An appropriate server source or <code>null</code> if string could not be parsed to a server source
         */
        public static ServerSource parse(String serverSourceStr) {
            if (Strings.isEmpty(serverSourceStr)) {
                return null;
            }

            String lookUp = Strings.asciiLowerCase(serverSourceStr.trim());
            for (ServerSource serverSource : ServerSource.values()) {
                if (serverSource.str.equals(lookUp)) {
                    return serverSource;
                }
            }
            return null;
        }
    } // End of enum ServerSource

    protected static final Class<?>[] CONSTRUCTOR_ARGS = new Class[0];

    protected static final Object[] INIT_ARGS = new Object[0];

    /**
     * Gets the user-specific mail configuration.
     *
     * @param <C> The return value type
     * @param mailConfig A newly created {@link MailConfig mail configuration}
     * @param session The session providing needed user data
     * @param accountId The mail account ID
     * @return The user-specific mail configuration
     * @throws OXException If user-specific mail configuration cannot be determined
     */
    public static final <C extends MailConfig> C getConfig(C mailConfig, Session session, int accountId) throws OXException {
        /*
         * Fetch mail account
         */
        int userId = session.getUserId();
        int contextId = session.getContextId();
        MailAccount mailAccount = ServerServiceRegistry.getServize(MailAccountStorageService.class, true).getMailAccount(accountId, userId, contextId);
        mailConfig.account = mailAccount;
        mailConfig.accountId = accountId;
        mailConfig.session = session;
        mailConfig.applyStandardNames(mailAccount);
        fillLoginAndPassword(mailConfig, session, getUser(session).getLoginInfo(), mailAccount, true);
        UrlInfo urlInfo = MailConfig.getMailServerURL(mailAccount, userId, contextId);
        String serverURL = urlInfo.getServerURL();
        if (serverURL == null) {
            if (mailAccount.isDefaultOrSecondaryAccount()) {
                ServerSource serverSource = mailAccount.isDefaultAccount() ? MailProperties.getInstance().getMailServerSource(userId, contextId, isGuest(session)) : MailProperties.getInstance().getSecondaryMailServerSource(userId, contextId, isGuest(session));
                if (ServerSource.GLOBAL.equals(serverSource)) {
                    String propName = mailAccount.isDefaultAccount() ? "com.openexchange.mail.mailServer" : "com.openexchange.mail.secondary.mailServer";
                    throw MailConfigException.create("Property \"" + propName + "\" not set in mail properties for user " + userId + " in context " + contextId);
                }
                if (mailAccount.isDefaultAccount()) {
                    throw MailConfigException.create(new StringBuilder(64).append("Cannot determine primary mail server URL for user ").append(userId).append(" in context ").append(contextId).toString());
                }
                throw MailConfigException.create(new StringBuilder(64).append("Cannot determine secondary (").append(accountId).append(") mail server URL for user ").append(userId).append(" in context ").append(contextId).toString());
            }
            throw MailConfigException.create(new StringBuilder(64).append("Cannot determine mail server URL of mail account ").append(accountId).append(" for user ").append(userId).append(" in context ").append(contextId).toString());
        }

        mailConfig.parseServerURL(urlInfo);
        mailConfig.doCustomParsing(mailAccount, session);
        return mailConfig;
    }

    /**
     * Gets the user associated with specified session.
     *
     * @param session The session
     * @return The user
     * @throws OXException If user cannot be returned
     */
    protected static User getUser(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getUser();
        }
        return UserStorage.getInstance().getUser(session.getUserId(), session.getContextId());
    }

    /**
     * Gets the mail login with respect to configured login source.
     *
     * @param account The account used to determine the login
     * @param userLoginInfo The login information of the user
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The mail login of specified user
     * @throws OXException If login cannot be determined
     */
    public static final String getMailLogin(Account account, String userLoginInfo, int userId, int contextId) throws OXException {
        return getLogin(account, userLoginInfo, userId, contextId, true);
    }

    /**
     * Gets the login with respect to configured login source.
     *
     * @param account The account used to determine the login
     * @param userLoginInfo The login information of the user
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param forMailAccess <code>true</code> if login is supposed to be returned for mail access; otherwise <code>false</code> for mail transport
     * @return The login of specified user
     * @throws OXException If login cannot be determined
     */
    public static final String getLogin(Account account, String userLoginInfo, int userId, int contextId, boolean forMailAccess) throws OXException {
        return getLogin(account, userLoginInfo, userId, contextId, forMailAccess, Optional.empty());
    }

    /**
     * Gets the login with respect to configured login source.
     *
     * @param account The account used to determine the login
     * @param userLoginInfo The login information of the user
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param forMailAccess <code>true</code> if login is supposed to be returned for mail access; otherwise <code>false</code> for mail transport
     * @param optionalSession The optional session (might only be needed for secondary accounts)
     * @return The mail login of specified user
     * @throws OXException If login cannot be determined
     */
    public static final String getLogin(Account account, String userLoginInfo, int userId, int contextId, boolean forMailAccess, Optional<Session> optionalSession) throws OXException {
        return saneLogin(getLogin0(account, userLoginInfo, userId, contextId, forMailAccess, optionalSession));
    }

    /**
     * Gets the login with respect to configured login source.
     *
     * @param account The account used to determine the login
     * @param userLoginInfo The login information of the user
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param forMailAccess <code>true</code> if login is supposed to be returned for mail access; otherwise <code>false</code> for mail transport
     * @param optionalSession The optional session (might only be needed for secondary accounts)
     * @return The login of specified user
     * @throws OXException If login cannot be determined
     */
    private static final String getLogin0(Account account, String userLoginInfo, int userId, int contextId, boolean forMailAccess, Optional<Session> optionalSession) throws OXException {
        if (account.isNeitherDefaultNorSecondaryAccount()) {
            return forMailAccess || TransportAuth.considerAsMailTransportAuth(account.getTransportAuth()) ? account.getLogin() : account.getTransportLogin();
        }

        // For primary or secondary mail account
        if (optionalSession.isPresent()) {
            Session session = optionalSession.get();
            Optional<MailAuthenticator> optionalAuthenticator = MailAuthenticatorRegistry.getInstance().getMailAuthenticator(session, account, forMailAccess);
            if (optionalAuthenticator.isPresent()) {
                return optionalAuthenticator.get().getLogin(session, account, forMailAccess);
            }
        }

        String login;
        switch (account.isDefaultAccount() ? MailProperties.getInstance().getLoginSource(userId, contextId) : MailProperties.getInstance().getSecondaryLoginSource(userId, contextId)) {
            case USER_IMAPLOGIN:
                login = account.getLogin();
                break;
            case PRIMARY_EMAIL: {
                String primaryAddress = account.getPrimaryAddress();
                try {
                    login = QuotedInternetAddress.toACE(primaryAddress);
                } catch (AddressException e) {
                    org.slf4j.LoggerFactory.getLogger(MailConfig.class).warn("Login source primary email address \"{}\" could not be converted to ASCII. Using unicode representation.", primaryAddress, e);
                    login = primaryAddress;
                }
                break;
            }
            default:
                login = userLoginInfo;
                break;
        }
        if (null == login) {
            if (!MailAccounts.isGuestAccount(account) ) {
                String propName = account.isDefaultAccount() ? "com.openexchange.mail.loginSource" : "com.openexchange.mail.secondary.loginSource";
                throw MailExceptionCode.MISSING_CONNECT_PARAM.create("Login not set. Either an invalid session or property \"" + propName + "\" is set incorrectly.");
            }
        }
        return login;
    }

    /**
     * Gets the mail server URL appropriate to configured mail server source.
     *
     * @param mailAccount The mail account
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The appropriate mail server URL or <code>null</code>
     * @throws OXException If URL information cannot be returned
     */
    public static final UrlInfo getMailServerURL(MailAccount mailAccount, int userId, int contextId) throws OXException {
        if (mailAccount.isNeitherDefaultNorSecondaryAccount()) {
            return new UrlInfo(mailAccount.generateMailServerURL(), mailAccount.isMailStartTls());
        }

        ServerSource serverSource;
        if (mailAccount.isDefaultAccount()) {
            serverSource = MailProperties.getInstance().getMailServerSource(userId, contextId, MailAccounts.isGuestAccount(mailAccount));
        } else {
            serverSource = MailProperties.getInstance().getSecondaryMailServerSource(userId, contextId, MailAccounts.isGuestAccount(mailAccount));
        }
        if (ServerSource.GLOBAL.equals(serverSource)) {
            ConfiguredServer server = mailAccount.isDefaultAccount() ? MailProperties.getInstance().getMailServer(userId, contextId) : MailProperties.getInstance().getSecondaryMailServer(userId, contextId);
            if (server == null) {
                String propName = mailAccount.isDefaultAccount() ? "com.openexchange.mail.mailServer" : "com.openexchange.mail.secondary.mailServer";
                throw MailConfigException.create("Property \"" + propName + "\" not set in mail properties for user " + userId + " in context " + contextId);
            }
            boolean startTls = mailAccount.isDefaultAccount() ? MailProperties.getInstance().isMailStartTls(userId, contextId) : MailProperties.getInstance().isSecondaryMailStartTls(userId, contextId);
            return new UrlInfo(server.getUrlString(true), startTls);
        }
        return new UrlInfo(mailAccount.generateMailServerURL(), mailAccount.isMailStartTls());
    }

    /**
     * Gets the mail server URL appropriate to configured mail server source.
     *
     * @param session The user session
     * @param accountId The account ID
     * @return The appropriate mail server URL or <code>null</code>
     * @throws OXException If mail server URL cannot be returned
     */
    public static final UrlInfo getMailServerURL(Session session, int accountId) throws OXException {
        int userId = session.getUserId();
        int contextId = session.getContextId();

        if (Account.DEFAULT_ID == accountId || MailAccounts.isSecondaryAccount(accountId, session)) {
            ServerSource serverSource;
            if (Account.DEFAULT_ID == accountId) {
                serverSource = MailProperties.getInstance().getMailServerSource(userId, contextId, isGuest(session));
            } else {
                serverSource = MailProperties.getInstance().getSecondaryMailServerSource(userId, contextId, isGuest(session));
            }
            if (ServerSource.GLOBAL == serverSource) {
                ConfiguredServer server = Account.DEFAULT_ID == accountId ? MailProperties.getInstance().getMailServer(userId, contextId) : MailProperties.getInstance().getSecondaryMailServer(userId, contextId);
                if (server == null) {
                    String propName = Account.DEFAULT_ID == accountId ? "com.openexchange.mail.mailServer" : "com.openexchange.mail.secondary.mailServer";
                    throw MailConfigException.create("Property \"" + propName + "\" not set in mail properties for user " + userId + " in context " + contextId);
                }
                boolean startTls = Account.DEFAULT_ID == accountId ? MailProperties.getInstance().isMailStartTls(userId, contextId) : MailProperties.getInstance().isSecondaryMailStartTls(userId, contextId);
                return new UrlInfo(server.getUrlString(true), startTls);
            }
        }

        MailAccount mailAccount = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true).getMailAccount(accountId, userId, contextId);
        return new UrlInfo(mailAccount.generateMailServerURL(), mailAccount.isMailStartTls());
    }

    private static final class UserID {

        final Context context;
        final String pattern;
        final AccountNature accountNature;
        final String serverUrl;
        final int userId;
        private final int hash;

        protected UserID(String pattern, String serverUrl, AccountNature accountNature, int userId, Context context) {
            super();
            this.pattern = pattern;
            this.serverUrl = serverUrl;
            this.accountNature = accountNature;
            this.userId = userId;
            this.context = context;

            int prime = 31;
            int result = prime * 1 + ((context == null) ? 0 : context.getContextId());
            result = prime * result + userId;
            result = prime * result + ((accountNature == null) ? 0 : (accountNature.ordinal() + 1));
            result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
            result = prime * result + ((serverUrl == null) ? 0 : serverUrl.hashCode());
            hash = result;
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
            if (!(obj instanceof UserID)) {
                return false;
            }
            final UserID other = (UserID) obj;
            if (userId != other.userId) {
                return false;
            }
            if (context == null) {
                if (other.context != null) {
                    return false;
                }
            } else if (other.context == null || context.getContextId() != other.context.getContextId()) {
                return false;
            }
            if (accountNature == null) {
                if (other.accountNature != null) {
                    return false;
                }
            } else if (!accountNature.equals(other.accountNature)) {
                return false;
            }
            if (pattern == null) {
                if (other.pattern != null) {
                    return false;
                }
            } else if (!pattern.equals(other.pattern)) {
                return false;
            }
            if (serverUrl == null) {
                if (other.serverUrl != null) {
                    return false;
                }
            } else if (!serverUrl.equals(other.serverUrl)) {
                return false;
            }
            return true;
        }
    }

    private static final LoadingCache<UserID, int[]> USER_ID_CACHE = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(65536 << 1).initialCapacity(8192).expireAfterAccess(30, TimeUnit.MINUTES).build(new CacheLoader<UserID, int[]>() {

        @Override
        public int[] load(UserID userID) throws Exception {
            MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
            return forDefaultAccount(userID.pattern, userID.serverUrl, userID.accountNature, userID.userId, userID.context, storageService);
        }
    });

    /**
     * Resolves the user IDs by specified pattern dependent on configuration's setting for mail login source.
     *
     * @param pattern The pattern
     * @param isDefaultOrInternalAccount Flag indicating whether pattern is supposed to be resolved for default/primary or internal account
     * @param serverUrl The server URL; e.g. <code>"mail.company.org:143"</code>
     * @param userId The user identifier
     * @param ctx The context
     * @return The user IDs from specified pattern dependent on configuration's setting for mail login source
     * @throws OXException If resolving user by specified pattern fails
     */
    public static int[] getUserIDsByMailLogin(String pattern, AccountNature accountNature, String serverUrl, int userId, Context ctx) throws OXException {
        if (accountNature.isDefaultOrSecondaryAccount()) {
            UserID userID = new UserID(pattern, serverUrl, accountNature, userId, ctx);
            boolean remove = true;
            try {
                int[] retval = USER_ID_CACHE.get(userID);
                remove = false;
                return retval;
            } catch (ExecutionException e) {
                ThreadPools.launderThrowable(e, OXException.class);
            } finally {
                if (remove) {
                    USER_ID_CACHE.invalidate(userID);
                }
            }
        }

        // Find user name by user's IMAP login
        MailAccountStorageService storageService = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
        final MailAccount[] accounts = storageService.resolveLogin(pattern, serverUrl, ctx.getContextId());
        final int[] retval = new int[accounts.length];
        for (int i = 0; i < retval.length; i++) {
            retval[i] = accounts[i].getUserId();
        }
        return retval;
    }

    /**
     * Resolves the user IDs by specified pattern dependent on configuration's setting for mail login source for default account
     */
    protected static int[] forDefaultAccount(String pattern, String serverUrl, AccountNature accountNature, int iUserId, Context ctx, MailAccountStorageService storageService) throws OXException {
        if (accountNature == AccountNature.REGULAR) {
            throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create("Invalid account nature: " + accountNature.name());
        }

        boolean isPrimary = accountNature == AccountNature.PRIMARY;
        LoginSource loginSource = isPrimary ? MailProperties.getInstance().getLoginSource(iUserId, ctx.getContextId()) : MailProperties.getInstance().getSecondaryLoginSource(iUserId, ctx.getContextId());
        switch (loginSource) {
            case USER_IMAPLOGIN:
            case PRIMARY_EMAIL:
                final MailAccount[] accounts;
                switch (loginSource) {
                    case USER_IMAPLOGIN:
                        accounts = storageService.resolveLogin(pattern, ctx.getContextId());
                        break;
                    case PRIMARY_EMAIL:
                        accounts = storageService.resolvePrimaryAddr(pattern, ctx.getContextId());
                        break;
                    default:
                        throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create("Unimplemented mail login source.");
                }
                final TIntSet userIds;
                if (accounts.length == 1) {
                    // On ASE some accounts are configured to connect to localhost, some to the full qualified local host name. The socket
                    // would then not match. If we only find one then, use it.
                    userIds = new TIntHashSet(1);
                    userIds.add(accounts[0].getUserId());
                } else {
                    userIds = new TIntHashSet(accounts.length);
                    for (MailAccount candidate : accounts) {
                        final String shouldMatch;
                        switch (isPrimary ? MailProperties.getInstance().getMailServerSource(iUserId, ctx.getContextId(), MailAccounts.isGuestAccount(candidate)) : MailProperties.getInstance().getSecondaryMailServerSource(iUserId, ctx.getContextId(), MailAccounts.isGuestAccount(candidate))) {
                            case USER:
                                shouldMatch = toSocketAddrString(candidate.generateMailServerURL(), 143);
                                break;
                            case GLOBAL:
                                {
                                    ConfiguredServer server = isPrimary ? MailProperties.getInstance().getMailServer(iUserId, ctx.getContextId()) : MailProperties.getInstance().getSecondaryMailServer(iUserId, ctx.getContextId());
                                    if (server == null) {
                                        String propName = isPrimary ? "com.openexchange.mail.mailServer" : "com.openexchange.mail.secondary.mailServer";
                                        throw MailConfigException.create("Property \"" + propName + "\" not set in mail properties for user " + iUserId + " in context " + ctx.getContextId());
                                    }
                                    shouldMatch = toSocketAddrString(server.getHostName(), server.getPort());
                                }
                                break;
                            default:
                                throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create("Unimplemented mail server source.");
                        }
                        if (serverUrl.equals(shouldMatch)) {
                            userIds.add(candidate.getUserId());
                        }
                    }
                }
                // Prefer the default mail account.
                final int size = userIds.size();
                final TIntSet notDefaultAccount = new TIntHashSet(size);
                if (size > 0) {
                    final TIntIterator iter = userIds.iterator();
                    for (int i = size; i-- > 0;) {
                        final int userId = iter.next();
                        for (MailAccount candidate : accounts) {
                            if (candidate.getUserId() == userId && !candidate.isDefaultAccount()) {
                                notDefaultAccount.add(userId);
                            }
                        }
                    }
                }
                if (notDefaultAccount.size() < size) {
                    userIds.removeAll(notDefaultAccount);
                }
                return userIds.toArray();
            case USER_NAME:
                return new int[] { UserStorage.getInstance().getUserId(pattern, ctx) };
            default:
                throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create("Unimplemented mail login source.");
        }
    }

    /**
     * Parses protocol out of specified server string according to URL specification; e.g. <i>mailprotocol://dev.myhost.com:1234</i>
     *
     * @param server The server string
     * @return An array of {@link String} with length <code>2</code>. The first element is the protocol and the second the server. If no
     *         protocol pattern could be found <code>null</code> is returned; meaning no protocol is present in specified server string.
     */
    public static final String[] parseProtocol(String server) {
        final int len = server.length();
        char c;
        for (int i = 0; (i < len) && ((c = server.charAt(i)) != '/'); i++) {
            if (c == ':' && (server.charAt(i + 1) == '/') && (server.charAt(i + 2) == '/')) {
                String s = server.substring(0, i).toLowerCase(Locale.ENGLISH);
                if (isValidProtocol(s)) {
                    int start = i + 1;
                    while (server.charAt(start) == '/') {
                        start++;
                    }
                    return new String[] { s, server.substring(start) };
                }
                break;
            }
        }
        return null; // NOSONARLINT
    }

    static {
        MailReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                doSaneLogin = null;
            }

            @Override
            public Interests getInterests() {
                return Reloadables.interestsForProperties("com.openexchange.mail.saneLogin");
            }
        });
    }

    private static final boolean isValidProtocol(String protocol) {
        final int len = protocol.length();
        if (len < 1) {
            return false;
        }
        char c = protocol.charAt(0);
        if (!Character.isLetter(c)) {
            return false;
        }
        for (int i = 1; i < len; i++) {
            c = protocol.charAt(i);
            if (!Character.isLetterOrDigit(c) && (c != '.') && (c != '+') && (c != '-')) {
                return false;
            }
        }
        return true;
    }

    static volatile Boolean doSaneLogin;
    private static boolean doSaneLogin() {
        Boolean tmp = doSaneLogin;
        if (null == tmp) {
            synchronized (MailConfig.class) {
                tmp = doSaneLogin;
                if (null == tmp) {
                    boolean defaultValue = true;
                    ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
                    if (null == service) {
                        return defaultValue;
                    }
                    tmp = Boolean.valueOf(service.getBoolProperty("com.openexchange.mail.saneLogin", defaultValue));
                    doSaneLogin = tmp;
                }
            }
        }
        return tmp.booleanValue();
    }

    /**
     * Gets the sane (puny-code) representation of passed login in case it appears to be an Internet address.
     *
     * @param login The login
     * @return The sane login
     */
    public static final String saneLogin(String login) {
        if (false == doSaneLogin()) {
            return login;
        }
        try {
            return IDNA.toACE(login);
        } catch (Exception e) {
            return login;
        }
    }

    private static final Cache<AuthTypeKey, ImmutableReference<AuthType>> VOLATILE_CACHE_AUTH_TYPE = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

    /**
     * Invalidates the <i>auth type cache</i>.
     */
    public static void invalidateAuthTypeCache() {
        VOLATILE_CACHE_AUTH_TYPE.invalidateAll();
    }

    /**
     * Gets the configured authentication type for session-associated user's primary or secondary mail account.
     *
     * @param accountNature The account nature (whether look-up is for primary or secondary mail account)
     * @param session The session providing user data
     * @return The authentication type
     * @throws OXException If authentication type cannot be returned
     */
    public static AuthType getConfiguredAuthTypeForMailAccess(AccountNature accountNature, Session session) throws OXException {
        return getConfiguredAuthType(true, accountNature, session);
    }

    /**
     * Gets the configured authentication type for session-associated user's primary or secondary transport account.
     *
     * @param accountNature The account nature (whether look-up is for primary or secondary transport account)
     * @param session The session providing user data
     * @return The authentication type
     * @throws OXException If authentication type cannot be returned
     */
    public static AuthType getConfiguredAuthTypeForMailTransport(AccountNature accountNature, Session session) throws OXException {
        return getConfiguredAuthType(false, accountNature, session);
    }

    /**
     * Gets the configured authentication type for session-associated user's primary or secondary mail/transport account.
     *
     * @param forMailAccess <code>true</code> to check for primary/secondary mail account; otherwise <code>false</code> for primary/secondary transport one
     * @param accountNature The account nature (whether look-up is for primary or secondary mail/transport account)
     * @param session The session providing user data
     * @return The authentication type
     * @throws OXException If authentication type cannot be returned
     */
    private static AuthType getConfiguredAuthType(boolean forMailAccess, AccountNature accountNature, Session session) throws OXException {
        if (accountNature == null || accountNature.isNeitherDefaultNorSecondaryAccount()) {
            return AuthType.LOGIN;
        }

        AuthTypeKey key = new AuthTypeKey(forMailAccess, accountNature, session.getUserId(), session.getContextId());
        ImmutableReference<AuthType> authType = VOLATILE_CACHE_AUTH_TYPE.getIfPresent(key);
        if (null == authType) {
            ConfigViewFactory factory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);
            if (null == factory) {
                return AuthType.LOGIN;
            }

            authType = doGetConfiguredAuthType(forMailAccess, accountNature, session, factory);
            VOLATILE_CACHE_AUTH_TYPE.put(key, authType);
        }
        return authType.getValue();
    }

    private static final String PROPERTY_AUTH_TYPE_MAIL = "com.openexchange.mail.authType";
    private static final String PROPERTY_AUTH_TYPE_SECONDARY_MAIL = "com.openexchange.mail.secondary.authType";
    private static final String PROPERTY_AUTH_TYPE_TRANSPORT = "com.openexchange.mail.transport.authType";
    private static final String PROPERTY_AUTH_TYPE_SECONDARY_TRANSPORT = "com.openexchange.mail.transport.secondary.authType";

    private static ImmutableReference<AuthType> doGetConfiguredAuthType(boolean forMailAccess, AccountNature accountNature, Session session, ConfigViewFactory factory) throws OXException {
        ConfigView view = factory.getView(session.getUserId(), session.getContextId());
        String property;
        if (forMailAccess) {
            property = accountNature.isSecondaryAccount() ? PROPERTY_AUTH_TYPE_SECONDARY_MAIL : PROPERTY_AUTH_TYPE_MAIL;
        } else {
            property = accountNature.isSecondaryAccount() ? PROPERTY_AUTH_TYPE_SECONDARY_TRANSPORT : PROPERTY_AUTH_TYPE_TRANSPORT;
        }
        String authTypeStr = view.opt(property, String.class, AuthType.LOGIN.getName());
        AuthType authType = AuthType.parse(authTypeStr);
        if (null == authType) {
            throw MailConfigException.create("Invalid or unsupported value configured for property \"" + property + "\": " + authTypeStr);
        }
        return new ImmutableReference<>(authType);
    }

    /**
     * Fills login and password in specified instance of {@link MailConfig}.
     *
     * @param mailConfig The mail config whose login and password shall be set
     * @param session The session
     * @param userLoginInfo The user's provisioned login information
     * @param account The account
     * @param forMailAccess <code>true</code> if credentials are supposed to be set for mail access; otherwise <code>false</code> for mail transport
     * @throws OXException If a configuration error occurs
     */
    protected static final void fillLoginAndPassword(MailConfig mailConfig, Session session, String userLoginInfo, Account account, boolean forMailAccess) throws OXException {
        if (account.isDefaultOrSecondaryAccount()) {
            // Primary or secondary account
            // Assign login
            MailProperties mailProperties = MailProperties.getInstance();
            {
                String proxyDelimiter = account.isDefaultAccount() ? mailProperties.getAuthProxyDelimiter() : mailProperties.getSecondaryAuthProxyDelimiter();
                if (proxyDelimiter == null) {
                    mailConfig.login = getLogin(account, userLoginInfo, session.getUserId(), session.getContextId(), forMailAccess, Optional.of(session));
                } else {
                    String slogin = session.getLoginName();
                    if (slogin.contains(proxyDelimiter)) {
                        mailConfig.login = saneLogin(slogin);
                    } else {
                        mailConfig.login = getLogin(account, userLoginInfo, session.getUserId(), session.getContextId(), forMailAccess, Optional.of(session));
                    }
                }
            }
            LoggerHolder.LOG.debug("Determined login for {} account {} ({}): {}", account.getAccountNature(), I(account.getId()), account.getPrimaryAddress(), mailConfig.login);

            // Assign password
            AuthInfo authInfo = determinePasswordAndAuthTypeForPrimaryOrSecondary(mailConfig.login, session, account, forMailAccess);
            mailConfig.password = authInfo.getPassword();
            mailConfig.authType = authInfo.getAuthType();
            mailConfig.oauthAccountId = authInfo.getOauthAccountId();
        } else {
            // External account
            // Assign login
            mailConfig.login = getLogin(account, userLoginInfo, session.getUserId(), session.getContextId(), forMailAccess, Optional.empty()); /* Session not needed for external ones */

            // Assign password
            AuthInfo authInfo = determinePasswordAndAuthTypeFromAccount(mailConfig.login, session, account, forMailAccess);
            mailConfig.password = authInfo.getPassword();
            mailConfig.authType = authInfo.getAuthType();
            mailConfig.oauthAccountId = authInfo.getOauthAccountId();
        }
    }

    /**
     * Determines given account's password and authentication type.
     *
     * @param login The login to assume
     * @param session The session to check by
     * @param account The account
     * @param forMailAccess <code>true</code> to resolve for mail access; otherwise <code>false</code> for mail transport
     * @return The authentication information
     * @throws OXException If authentication information cannot be resolved
     */
    public static AuthInfo determinePasswordAndAuthType(String login, Session session, Account account, boolean forMailAccess) throws OXException {
        if (account.isNeitherDefaultNorSecondaryAccount()) {
            // For all non-primary and non-secondary accounts
            return determinePasswordAndAuthTypeFromAccount(login, session, account, forMailAccess);
        }

        // A primary or secondary account
        return determinePasswordAndAuthTypeForPrimaryOrSecondary(login, session, account, forMailAccess);
    }

    /**
     * Determines given primary/secondary account's password and authentication type.
     *
     * @param login The login to assume
     * @param session The session to check by
     * @param account The account
     * @param forMailAccess <code>true</code> to resolve for mail access; otherwise <code>false</code> for mail transport
     * @return The authentication information
     * @throws OXException If authentication information cannot be resolved
     */
    private static AuthInfo determinePasswordAndAuthTypeForPrimaryOrSecondary(String login, Session session, Account account, boolean forMailAccess) throws OXException {
        // Check for a suitable mail authenticator
        Optional<MailAuthenticator> optionalAuthenticator = MailAuthenticatorRegistry.getInstance().getMailAuthenticator(session, account, forMailAccess);
        if (optionalAuthenticator.isPresent()) {
            // Use mail authenticator...
            AuthInfo authInfo = optionalAuthenticator.get().getAuthInfo(login, session, account, forMailAccess);
            LoggerHolder.LOG.debug("Determined auth info by MailAuthenticator for {} account {} ({}): {}", account.getAccountNature(), I(account.getId()), account.getPrimaryAddress(), authInfo);
            return authInfo;
        }

        // ----------------------------------------- Common routine for primary/secondary account ------------------------------------------

        // First, check the configured authentication type for current user
        AuthType configuredAuthType = getConfiguredAuthType(forMailAccess, account.getAccountNature(), session);
        if (configuredAuthType != null && configuredAuthType.isOAuth()) {
            // Apparently, OAuth is supposed to be used
            LoggerHolder.LOG.debug("Configured auth type for {} account {} ({}): {}", account.getAccountNature(), I(account.getId()), account.getPrimaryAddress(), configuredAuthType);
            Object obj = session.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN);
            if (obj == null) {
                if (isNeitherGuestNorAppPassword(session)) {
                    throw MailExceptionCode.MISSING_CONNECT_PARAM.create("The session contains no OAuth token.");
                }
                obj = "";
            }
            return new AuthInfo(login, obj.toString(), configuredAuthType, -1);
        }

        // Common handling based on configuration
        MailProperties mailProperties = MailProperties.getInstance();
        if (account.isDefaultAccount()) {
            // Primary account
            if (PasswordSource.GLOBAL == mailProperties.getPasswordSource(session.getUserId(), session.getContextId())) {
                // Global password source
                String masterPw = mailProperties.getMasterPassword(session.getUserId(), session.getContextId());
                if (masterPw == null) {
                    if (isNeitherGuestNorAppPassword(session)) {
                        throw MailConfigException.create("Property \"com.openexchange.mail.masterPassword\" not set");
                    }
                    masterPw = "";
                }
                AuthInfo authInfo = new AuthInfo(login, masterPw, AuthType.LOGIN, -1);
                LoggerHolder.LOG.debug("Using master password for primary account {} ({}): {}", I(account.getId()), account.getPrimaryAddress(), authInfo);
                return authInfo;
            }

            // Non-global password source
            if (account.getPassword() == null) {
                // Password source is session
                String sessionPassword = session.getPassword();
                if (null == sessionPassword) {
                    if (isNeitherGuestNorAppPassword(session)) {
                        OXException e = MailExceptionCode.MISSING_CONNECT_PARAM.create("Session password not set. Either an invalid session or master authentication is not enabled (property \"com.openexchange.mail.passwordSource\" is not set to \"global\")");
                        e.setArgument(MISSING_SESSION_PASSWORD, Boolean.TRUE);
                        throw e;
                    }
                    sessionPassword = "";
                }
                AuthInfo authInfo = new AuthInfo(login, sessionPassword, AuthType.LOGIN, -1);
                LoggerHolder.LOG.debug("Using session password for primary account {} ({}): {}", I(account.getId()), account.getPrimaryAddress(), authInfo);
                return authInfo;
            }
        } else {
            // A secondary account
            switch (mailProperties.getSecondaryPasswordSource(session.getUserId(), session.getContextId())) {
                case GLOBAL:
                    // Global password source
                    String masterPw = mailProperties.getSecondaryMasterPassword(session.getUserId(), session.getContextId());
                    if (masterPw == null) {
                        if (isNeitherGuestNorAppPassword(session)) {
                            throw MailConfigException.create("Property \"com.openexchange.mail.secondary.masterPassword\" not set");
                        }
                        masterPw = "";
                    }
                    AuthInfo authInfo = new AuthInfo(login, masterPw, AuthType.LOGIN, -1);
                    LoggerHolder.LOG.debug("Using master password for secondary account {} ({}): {}", I(account.getId()), account.getPrimaryAddress(), authInfo);
                    return authInfo;
                case USER:
                    return determinePasswordAndAuthTypeFromAccount(login, session, account, forMailAccess);
                case SESSION:
                    // Password source is session
                    String sessionPassword = session.getPassword();
                    if (null == sessionPassword) {
                        if (isNeitherGuestNorAppPassword(session)) {
                            OXException e = MailExceptionCode.MISSING_CONNECT_PARAM.create("Session password not set. Either an invalid session or secondary master authentication is not enabled (property \"com.openexchange.mail.secondary.passwordSource\" is not set to \"global\")");
                            e.setArgument(MISSING_SESSION_PASSWORD, Boolean.TRUE);
                            throw e;
                        }
                        sessionPassword = "";
                    }
                    AuthInfo authInfo2 = new AuthInfo(login, sessionPassword, AuthType.LOGIN, -1);
                    LoggerHolder.LOG.debug("Using session password for secondary account {} ({}): {}", I(account.getId()), account.getPrimaryAddress(), authInfo2);
                    return authInfo2;
            }
        }

        CredentialsProviderService credentialsProvider = CredentialsProviderRegistry.getInstance().optCredentialsProviderFor(forMailAccess, account.getId(), session);
        if (null == credentialsProvider) {
            return determinePasswordAndAuthTypeFromAccount(login, session, account, forMailAccess);
        }

        AuthInfo optAuthInfo = getAuthInfoFromCredentials(forMailAccess ? credentialsProvider.getMailCredentials(account.getId(), session) : credentialsProvider.getTransportCredentials(account.getId(), session));
        if (optAuthInfo != null) {
            return optAuthInfo;
        }
        return determinePasswordAndAuthTypeFromAccount(login, session, account, forMailAccess);
    }

    /**
     * Checks if given session neither originates from a guest nor from application-specific authentication.
     *
     * @param session The session to check
     * @return <code>true</code> if session is of regular origina; otherwise <code>false</code> for a guest or from application-specific authentication
     */
    private static boolean isNeitherGuestNorAppPassword(Session session) {
        return !isGuestOrAppPassword(session);
    }

    /**
     * Checks if given session originates from a guest or from application-specific authentication.
     *
     * @param session The session to check
     * @return <code>true</code> if session originates from a guest or from application-specific authentication; otherwise <code>false</code> for a regular session
     */
    private static boolean isGuestOrAppPassword(Session session) {
        return Boolean.TRUE.equals(session.getParameter(Session.PARAM_GUEST)) || session.getParameter(Session.PARAM_RESTRICTED) != null;
    }

    /**
     * Determines given account's password and authentication type.
     *
     * @param login The login to assume
     * @param session The session to check by
     * @param account The account
     * @param forMailAccess <code>true</code> to resolve for mail access; otherwise <code>false</code> for mail transport
     * @return The authentication information
     * @throws OXException If authentication information cannot be resolved
     */
    private static AuthInfo determinePasswordAndAuthTypeFromAccount(String login, Session session, Account account, boolean forMailAccess) throws OXException {
        // Check for OAuth association
        int oAuthAccontId = account.isDefaultOrSecondaryAccount() ? -1 : assumeOauthFor(account, forMailAccess);
        if (oAuthAccontId >= 0) {
            // Do the OAuth dance...
            TokenInfo tokenInfo = getTokenFor(oAuthAccontId, session, login, account, forMailAccess);
            AuthInfo authInfo = new AuthInfo(login, tokenInfo.getToken(), AuthType.parse(tokenInfo.getAuthMechanism()), oAuthAccontId);
            LoggerHolder.LOG.debug("Determined OAuth auth info for {} account {} ({}): {}", account.getAccountNature(), I(account.getId()), account.getPrimaryAddress(), authInfo);
            return authInfo;
        }

        // Account's (encrypted) password
        Optional<String> optEncryptedPassword = getPasswordFrom(account, forMailAccess);
        if (optEncryptedPassword.isEmpty()) {
            // Advertise empty string
            AuthInfo authInfo = new AuthInfo(login, "", AuthType.LOGIN, -1);
            LoggerHolder.LOG.debug("Password unavailable for {} account {} ({}): {}", account.getAccountNature(), I(account.getId()), account.getPrimaryAddress(), authInfo);
            return authInfo;
        }

        // Decrypt password
        String password;
        if (account.isSecondaryAccount()) {
            ObfuscatorService obfuscator = ServerServiceRegistry.getInstance().getService(ObfuscatorService.class);
            if (obfuscator == null) {
                throw ServiceExceptionCode.absentService(ObfuscatorService.class);
            }
            password = obfuscator.unobfuscate(optEncryptedPassword.get());
        } else {
            String server = forMailAccess ? ((MailAccount) account).getMailServer() : account.getTransportServer();
            try {
                password = MailPasswordUtil.decrypt(optEncryptedPassword.get(), session, account.getId(), account.getLogin(), server);
            } catch (OXException e) {
                if (CryptoErrorMessage.BadPassword.equals(e)) {
                    // Account's password could not be decrypted
                    throw MailExceptionCode.CANNOT_DECRYPT_PASSWORD.create(e, server, account.getLogin(), I(session.getUserId()), I(session.getContextId()));
                }
                throw e;
            }
        }
        AuthInfo authInfo = new AuthInfo(login, password, AuthType.LOGIN, -1);
        LoggerHolder.LOG.debug("Determined regular auth info for {} account {} ({}): {}", account.getAccountNature(), I(account.getId()), account.getPrimaryAddress(), authInfo);
        return authInfo;
    }

    /**
     * Gets the optional (encrypted) password from given account.
     *
     * @param account The account to get from
     * @param forMailAccess Whether password is intended for mail access or mail transport
     * @return The encrypted password or empty
     */
    private static Optional<String> getPasswordFrom(Account account, boolean forMailAccess) {
        String encryptedPassword;
        if (forMailAccess) {
            encryptedPassword = account.getPassword();
        } else {
            TransportAuth transportAuth = account.getTransportAuth();
            encryptedPassword = TransportAuth.MAIL == transportAuth ? account.getPassword() : account.getTransportPassword();
        }
        return (null == encryptedPassword || encryptedPassword.length() == 0) ? Optional.empty() : Optional.of(encryptedPassword);
    }

    private static TokenInfo getTokenFor(int oAuthAccontId, Session session, String login, Account account, boolean forMailAccess) throws OXException {
        MailOAuthService mailOAuthService = ServerServiceRegistry.getInstance().getService(MailOAuthService.class);
        if (mailOAuthService == null) {
            throw ServiceExceptionCode.absentService(MailOAuthService.class);
        }
        try {
            return mailOAuthService.getTokenFor(oAuthAccontId, session);
        } catch (OXException e) {
            OXException toThrow = e;
            if (MailOAuthExceptionCodes.NO_SUCH_MAIL_OAUTH_PROVIDER.equals(e)) {
                if (forMailAccess) {
                    MailAccount mailAccount = (MailAccount) account;
                    toThrow = MailExceptionCode.UNSUPPORTED_OAUTH_MAIL_ACCESS.create(mailAccount.getMailServer(), login, I(session.getUserId()), I(session.getContextId()));
                } else {
                    toThrow = MailExceptionCode.UNSUPPORTED_OAUTH_TRANSPORT_ACCESS.create(account.getTransportServer(), login, I(session.getUserId()), I(session.getContextId()));
                }
                LoggerHolder.LOG.warn("{}. Apparently package \"{}\" is not installed.", e.getMessage(), "open-xchange-oauth", toThrow);
            }
            throw toThrow;
        }
    }

    /**
     * Checks whether XOAUTH2/OAUTHBEARER authentication is assumed for specified account.
     *
     * @param account The account to check
     * @param forMailAccess <code>true</code> to resolve for mail access; otherwise <code>false</code> for mail transport
     * @return The verified identifier of the associated OAuth account or <code>-1</code>
     */
    protected static int assumeOauthFor(Account account, boolean forMailAccess) {
        if (forMailAccess) {
            MailAccount mailAccount = (MailAccount) account;
            if (!mailAccount.isMailOAuthAble()) {
                return -1;
            }
            return (mailAccount.getMailOAuthId() >= 0 ? mailAccount.getMailOAuthId() : -1);
        }

        if (!account.isTransportOAuthAble()) {
            return -1;
        }
        return (account.getTransportOAuthId() >= 0 ? account.getTransportOAuthId() : -1);
    }

    private static AuthInfo getAuthInfoFromCredentials(Credentials credentials) {
        if (null == credentials) {
            return null;
        }

        try {
            String login = credentials.getLogin();
            if (Strings.isEmpty(login)) {
                return null;
            }
            Password pw = credentials.getPassword();
            if (null == pw) {
                return null;
            }
            try {
                String password = new String(pw.getPassword());
                AuthType authType = credentials.getAuthType();
                return new AuthInfo(saneLogin(login), password, null == authType ? AuthType.LOGIN : authType, -1);
            } finally {
                Streams.close(pw);
            }
        } finally {
            Streams.close(credentials);
        }
    }

    private static final int LENGTH = 6;

    /*-
     * Member section
     */

    protected AuthType authType;
    protected Map<String, Object> authProps;
    protected int accountId;
    protected int oauthAccountId;
    protected Session session;
    protected String login;
    protected String password;
    protected boolean requireTls;
    protected Account account;
    protected final String[] standardNames;
    protected final String[] standardFullNames;

    /**
     * Initializes a new {@link MailConfig}
     */
    protected MailConfig() {
        super();
        oauthAccountId = -1;
        requireTls = false;
        authProps = null;
        authType = AuthType.LOGIN;
        standardFullNames = new String[LENGTH];
        standardNames = new String[LENGTH];
    }

    /**
     * Gets the account currently associated with this instance
     *
     * @return The account or <code>null</code>
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Gets the authentication type.
     *
     * @return The authentication type
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Gets the optional identifier of the associated OAuth account
     *
     * @return The identifier of the associated OAuth account or <code>-1</code>
     */
    public int getOAuthAccountId() {
        return oauthAccountId;
    }

    /**
     * Gets the authentication properties.
     *
     * @return The authentication properties or <code>null</code>
     */
    public Map<String, Object> getAuthProps() {
        return authProps;
    }

    /**
     * Gets the standard names.
     *
     * @return The standard names
     */
    public String[] getStandardNames() {
        final String[] ret = new String[LENGTH];
        System.arraycopy(standardNames, 0, ret, 0, LENGTH);
        return ret;
    }

    /**
     * Gets the standard full names.
     *
     * @return The standard full names
     */
    public String[] getStandardFullNames() {
        final String[] ret = new String[LENGTH];
        System.arraycopy(standardFullNames, 0, ret, 0, LENGTH);
        return ret;
    }

    /**
     * Applies folder name information from given mail account
     *
     * @param mailAccount The mail account
     */
    public void applyStandardNames(MailAccount mailAccount) {
        applyStandardNames(mailAccount, false);
    }

    /**
     * Applies folder name information from given mail account
     *
     * @param mailAccount The mail account
     * @param force <code>true</code> to enforce setting folder name information from given mail account; otherwise <code>false</code>
     */
    public void applyStandardNames(MailAccount mailAccount, boolean force) {
        if (null == mailAccount) {
            return;
        }
        put(StorageUtility.INDEX_CONFIRMED_HAM, mailAccount.getConfirmedHam(), standardNames, force);
        put(StorageUtility.INDEX_CONFIRMED_SPAM, mailAccount.getConfirmedSpam(), standardNames, force);
        put(StorageUtility.INDEX_DRAFTS, mailAccount.getDrafts(), standardNames, force);
        put(StorageUtility.INDEX_SENT, mailAccount.getSent(), standardNames, force);
        put(StorageUtility.INDEX_SPAM, mailAccount.getSpam(), standardNames, force);
        put(StorageUtility.INDEX_TRASH, mailAccount.getTrash(), standardNames, force);

        put(StorageUtility.INDEX_CONFIRMED_HAM, mailAccount.getConfirmedHamFullname(), standardFullNames, force);
        put(StorageUtility.INDEX_CONFIRMED_SPAM, mailAccount.getConfirmedSpamFullname(), standardFullNames, force);
        put(StorageUtility.INDEX_DRAFTS, mailAccount.getDraftsFullname(), standardFullNames, force);
        put(StorageUtility.INDEX_SENT, mailAccount.getSentFullname(), standardFullNames, force);
        put(StorageUtility.INDEX_SPAM, mailAccount.getSpamFullname(), standardFullNames, force);
        put(StorageUtility.INDEX_TRASH, mailAccount.getTrashFullname(), standardFullNames, force);
    }

    private static void put(int index, String value, String[] arr, boolean force) {
        if (!force && Strings.isEmpty(value)) {
            return;
        }
        if (null == value) {
            arr[index] = null;
        } else {
            arr[index] = MailFolderUtility.prepareMailFolderParamOrElseReturn(value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final MailConfig other = (MailConfig) obj;
        if (login == null) {
            if (other.login != null) {
                return false;
            }
        } else if (!login.equals(other.login)) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (getPort() != other.getPort()) {
            return false;
        }
        if (getServer() == null) {
            if (other.getServer() != null) {
                return false;
            }
        } else if (!getServer().equals(other.getServer())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{ MailConfig [accountId=").append(accountId).append(", ");
        if (login != null) {
            builder.append("login=").append(login).append(", ");
        }
        if (password != null) {
            // builder.append("password=").append(password).append(", ");
        }
        builder.append("getPort()=").append(getPort()).append(", ");
        if (getServer() != null) {
            builder.append("getServer()=").append(getServer()).append(", ");
        }
        builder.append("isSecure()=").append(isSecure()).append("] }");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((login == null) ? 0 : login.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + (getPort());
        final String server = getServer();
        result = prime * result + ((server == null) ? 0 : server.hashCode());
        return result;
    }

    /**
     * Checks if STARTTLS is required in case {@link #isSecure()} returns <code>false</code>
     *
     * @return <code>true</code> if STARTTLS is required; otherwise <code>false</code>
     */
    public boolean isRequireTls() {
        return requireTls;
    }

    /**
     * Sets whether STARTTLS is required in case {@link #isSecure()} returns <code>false</code>
     *
     * @param requireTls <code>true</code> if STARTTLS is required; otherwise <code>false</code>
     */
    public void setRequireTls(boolean requireTls) {
        this.requireTls = requireTls;
    }

    /**
     * Gets the account ID.
     *
     * @return The account ID
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * Gets the session.
     *
     * @return The session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the login.
     *
     * @return the login
     */
    public final String getLogin() {
        return login;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Sets the account ID (externally).
     *
     * @param accountId The account ID
     */
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    /**
     * Sets the session
     *
     * @param session The session
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Sets the login (externally).
     *
     * @param login The login
     */
    public void setLogin(String login) {
        this.login = saneLogin(login);
    }

    /**
     * Sets the password (externally).
     *
     * @param password The password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the authentication type.
     *
     * @param authType The authentication type to set
     */
    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    /**
     * Performs optional custom parsing.
     * <p>
     * Returns <code>false</code> by default.
     *
     * @param account The associated mail account
     * @param session The user's session
     * @return <code>true</code> if custom parsing has been performed; otherwise <code>false</code>
     * @throws OXException If custom parsing fails
     */
    protected boolean doCustomParsing(Account account, Session session) throws OXException {
        return false;
    }

    /**
     * Gets the mail system's capabilities
     *
     * @return The mail system's capabilities
     */
    public abstract MailCapabilities getCapabilities();

    /**
     * Gets the optional port of the server.
     *
     * @return The optional port of the server obtained via {@link #getServer()} or <code>-1</code> if no port needed.
     */
    public abstract int getPort();

    /**
     * Gets the host name or IP address of the server.
     *
     * @return The host name or IP address of the server.
     */
    public abstract String getServer();

    /**
     * Checks if a secure connection shall be established.
     *
     * @return <code>true</code> if a secure connection shall be established; otherwise <code>false</code>
     */
    public abstract boolean isSecure();

    /**
     * Sets the port (externally).
     *
     * @param port The port
     */
    public abstract void setPort(int port);

    /**
     * Sets (externally) whether a secure connection should be established or not.
     *
     * @param secure <code>true</code> if a secure connection should be established; otherwise <code>false</code>
     */
    public abstract void setSecure(boolean secure);

    /**
     * Sets the host name or IP address of the server (externally).
     *
     * @param server The host name or IP address of the server
     */
    public abstract void setServer(String server);

    /**
     * Gets the mail properties for this mail configuration.
     *
     * @return The mail properties for this mail configuration
     */
    public abstract IMailProperties getMailProperties();

    /**
     * Sets the mail properties for this mail configuration.
     *
     * @param mailProperties The mail properties for this mail configuration
     */
    public abstract void setMailProperties(IMailProperties mailProperties);

    /**
     * Parses given server URL which is then accessible through {@link #getServer()} and optional {@link #getPort()}.
     * <p>
     * The implementation is supposed to use {@link #parseProtocol(String)} to determine the protocol.
     * <p>
     * Moreover this method should check if a secure connection shall be established dependent on URL's protocol. The result is then
     * accessible via {@link #isSecure()}.
     *
     * @param serverURL The server URL of the form:<br>
     *            (&lt;protocol&gt;://)?&lt;host&gt;(:&lt;port&gt;)?
     * @throws OXException If server URL cannot be parsed
     */
    protected abstract void parseServerURL(UrlInfo urlInfo) throws OXException;
}
