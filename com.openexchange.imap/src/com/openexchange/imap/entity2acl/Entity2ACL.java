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

package com.openexchange.imap.entity2acl;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.mail.MessagingException;
import javax.mail.internet.idn.IDNA;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.i18n.Translator;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.imap.IMAPAccess;
import com.openexchange.imap.cache.NamespacesCache;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.namespace.Namespaces;
import com.openexchange.imap.services.Services;
import com.openexchange.java.Strings;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.mail.login.resolver.ResolverResult;
import com.openexchange.mail.login.resolver.ResolverStatus;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccounts;
import com.openexchange.notification.service.FullNameBuilderService;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.session.UserAndContext;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import com.sun.mail.imap.IMAPStore;

/**
 * {@link Entity2ACL} - Maps numeric entity IDs to corresponding IMAP login name (used in ACLs) and vice versa
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class Entity2ACL {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Entity2ACL.class);
    }

    private static final ConcurrentMap<String, String> GREETING_CACHE = new ConcurrentHashMap<String, String>(16, 0.9f, 1);

    private static String getGreeting(IMAPStore imapStore, IMAPConfig imapConfig) throws MessagingException {
        String greeting = GREETING_CACHE.get(imapConfig.getServer());
        if (null == greeting) {
            final String grt = imapStore.getGreeting();
            greeting = GREETING_CACHE.putIfAbsent(imapConfig.getServer(), grt);
            if (null == greeting) {
                greeting = grt;
            }
        }
        return greeting;
    }

    /**
     * The constant reflecting the found group {@link OCLPermission#ALL_GROUPS_AND_USERS}.
     */
    protected static final UserGroupID ALL_GROUPS_AND_USERS = new UserGroupID(OCLPermission.ALL_GROUPS_AND_USERS, true);

    /**
     * Singleton
     */
    private static volatile Entity2ACL singleton;

    /**
     * Creates a new instance implementing the {@link Entity2ACL} interface.
     *
     * @param imapConfig The user's IMAP config
     * @return an instance implementing the {@link Entity2ACL} interface.
     * @throws OXException if the instance can't be created.
     */
    public static Entity2ACL getInstance(IMAPConfig imapConfig) throws OXException {
        Entity2ACL instance = singleton;
        if (instance != null && Account.DEFAULT_ID == imapConfig.getAccountId()) {
            /*
             * Auto-detection is turned off, return configured implementation
             */
            return instance;
        }
        /*
         * Auto-detect dependent on user's IMAP settings
         */
        try {
            return Entity2ACLAutoDetector.getEntity2ACLImpl(imapConfig);
        } catch (IOException e) {
            throw Entity2ACLExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static final String PARAM_NAME = "Entity2ACL";

    /**
     * Creates a new instance implementing the {@link Entity2ACL} interface.
     *
     * @param imapStore The IMAP store
     * @param imapConfig The user's IMAP configuration
     * @return an instance implementing the {@link Entity2ACL} interface.
     * @throws OXException If a mail error occurs
     */
    public static Entity2ACL getInstance(IMAPStore imapStore, IMAPConfig imapConfig) throws OXException {
        Entity2ACL instance = singleton;
        if (instance != null && Account.DEFAULT_ID == imapConfig.getAccountId()) {
            /*
             * Auto-detection is turned off, return configured implementation
             */
            return instance;
        }
        try {
            Entity2ACL cached = imapConfig.getParameter(PARAM_NAME, Entity2ACL.class);
            if (null == cached) {
                cached = Entity2ACLAutoDetector.implFor(getGreeting(imapStore, imapConfig), imapConfig);
                imapConfig.setParameter(PARAM_NAME, cached);
            }
            return cached;
        } catch (MessagingException e) {
            throw MimeMailException.handleMessagingException(e, imapConfig);
        }
    }

    /**
     * Resets entity2acl
     */
    protected static void resetEntity2ACL() {
        singleton = null;
    }

    /**
     * Only invoked if auto-detection is turned off
     *
     * @param singleton The singleton instance of {@link Entity2ACL}
     */
    protected static void setInstance(Entity2ACL singleton) {
        Entity2ACL.singleton = singleton;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link Entity2ACL}
     */
    protected Entity2ACL() {
        super();
    }

    /**
     * Returns a newly created {@link UserGroupID} instance reflecting a found user.
     *
     * @param userId The user ID
     * @return A newly created {@link UserGroupID} instance reflecting a found user.
     */
    protected final UserGroupID getUserRetval(int userId) {
        if (userId < 0) {
            return UserGroupID.NULL;
        }
        return new UserGroupID(userId, false);
    }

    /**
     * Returns a newly created {@link UserGroupID} instance reflecting a found user.
     *
     * @param userId The user ID
     * @return A newly created {@link UserGroupID} instance reflecting a found user.
     */
    protected final List<UserGroupID> getUsersRetval(int[] userIds) {
        if (userIds == null || userIds.length <= 0) {
            return Collections.singletonList(UserGroupID.NULL);
        }
        List<UserGroupID> l = new ArrayList<UserGroupID>(userIds.length);
        for (int userId : userIds) {
            l.add(getUserRetval(userId));
        }
        return l;
    }

    /**
     * Determines the entity name of the user/group whose ID matches given <code>entity</code> that is used in IMAP server's ACL list.
     *
     * @param entity The user/group ID
     * @param ctx The context
     * @param args The arguments container
     * @return the IMAP login of the user/group whose ID matches given <code>entity</code>
     * @throws OXException IF user/group could not be found
     */
    public abstract String getACLName(int entity, Context ctx, Entity2ACLArgs args) throws OXException;

    /**
     * Determines the user/group ID whose either ACL entity name or user name matches given <code>pattern</code>.
     *
     * @param pattern The pattern for either IMAP login or user name
     * @param ctx The context
     * @param args The arguments container
     * @return An instance of {@link UserGroupID} providing the user/group identifier whose IMAP login matches given <code>pattern</code> or
     *         {@link UserGroupID#NULL} if none found.
     * @throws OXException If user/group search fails
     */
    public abstract UserGroupID getEntityID(String pattern, Context ctx, Entity2ACLArgs args) throws OXException;

    /**
     * Determines the user/group IDs whose either ACL entity name or user name matches given <code>pattern</code>.
     *
     * @param pattern The pattern for either IMAP login or user name
     * @param ctx The context
     * @param args The arguments container
     * @return The user/group identifiers
     * @throws OXException If user/group search fails
     */
    public abstract List<UserGroupID> getEntityIDs(String pattern, Context ctx, Entity2ACLArgs args) throws OXException;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the display name of the user resolved from given ACL entity name.
     *
     * @param entityName The entity name
     * @param fullName The full name
     * @param separator The separator character
     * @param imapAccess The connected IMAP access
     * @param withDisplayName Whether display name should be obtained or not
     * @return The user display name
     */
    public static Optional<UserInfo> getUserInfoFor(String entityName, String fullName, char separator, IMAPAccess imapAccess, boolean withDisplayName) {
        try {
            Session session = imapAccess.getSession();
            int accountId = imapAccess.getAccountId();
            IMAPConfig imapConfig = imapAccess.getIMAPConfig();
            IMAPStore imapStore = imapAccess.getIMAPStore();
            Context ctx = session instanceof ServerSession ? ((ServerSession) session).getContext() : ContextStorage.getStorageContext(session.getContextId());

            Namespaces namespaces = NamespacesCache.getNamespaces(imapStore, true, session, accountId);
            Entity2ACLArgs args = new Entity2ACLArgsImpl(session, accountId, new StringBuilder(36).append(IDNA.toASCII(imapConfig.getServer())).append(':').append(imapConfig.getPort()).toString(), session.getUserId(), fullName, separator, namespaces, MailAccounts.isSecondaryAccount(accountId, session), imapConfig.getImapCapabilities().hasResolve());
            List<UserGroupID> res = Entity2ACL.getInstance(imapStore, imapConfig).getEntityIDs(entityName, ctx, args);
            for (UserGroupID userGroupId : res) {
                int userId = userGroupId.getId();
                if (userId >= 0 && userGroupId.isUser() && userId != session.getUserId()) {
                    if (withDisplayName) {
                        User user = UserStorage.getInstance().getUser(userId, ctx);
                        String displayName = user.getDisplayName();
                        if (Strings.isEmpty(fullName)) {
                            FullNameBuilderService fullNameBuilder = Services.optService(FullNameBuilderService.class);
                            if (fullNameBuilder != null) {
                                Translator translator = Services.getServiceSafe(TranslatorFactory.class).translatorFor(user.getLocale());
                                displayName = fullNameBuilder.buildFullName(userId, ctx.getContextId(), translator);
                            }
                        }
                        return Optional.of(new UserInfo(userId, ctx.getContextId(), displayName));
                    }
                    return Optional.of(new UserInfo(userId, ctx.getContextId(), null));
                }
            }
        } catch (MessagingException e) {
            LoggerHolder.LOG.warn("Could not resolve users for entity name {}", entityName, e);
        } catch (Exception e) {
            LoggerHolder.LOG.debug("Could not resolve users for entity name {}", entityName, e);
        }
        return Optional.empty();
    }

    /**
     * Resolves an userId and contextId pair to a mail login
     *
     * @param sessionContextId The session's contextId
     * @param userId The userId to resolve
     * @param contextId The contextId to resolve
     * @return The user's mail login or <code>null</code> if the userId and contextId pair can not be resolved
     */
    protected static String resolveEntity2MailLogin(int sessionContextId, int userId, int contextId) {
        try {
            MailLoginResolverService resolver = Services.getServiceSafe(MailLoginResolverService.class);
            UserAndContext entity = UserAndContext.newInstance(userId, contextId);
            ResolverResult result = resolver.resolveEntity(sessionContextId, entity);
            if (ResolverStatus.SUCCESS == result.getStatus()) {
                return result.getMailLogin();
            }
        } catch (OXException e) {
            LoggerHolder.LOG.error("Could not resolve mail login for userId {} and contextId {}", I(userId), I(contextId), e);
        }
        return null;
    }

    /**
     * Resolves a mail login to a userId
     *
     * @param sessionContextId The session's contextId
     * @param pattern The mail login
     * @return The userId or <code>null</code> if the mail login can not be resolved
     */
    protected static Integer resolveMailLogin2UserID(int sessionContextId, String pattern) {
        try {
            MailLoginResolverService resolver = Services.getServiceSafe(MailLoginResolverService.class);
            ResolverResult result = resolver.resolveMailLogin(sessionContextId, pattern);
            if (ResolverStatus.SUCCESS == result.getStatus()) {
                return I(result.getUserId());
            }
        } catch (OXException e) {
            LoggerHolder.LOG.error("Could not resolve user for mail login {}", pattern, e);
        }
        return null;
    }

}
