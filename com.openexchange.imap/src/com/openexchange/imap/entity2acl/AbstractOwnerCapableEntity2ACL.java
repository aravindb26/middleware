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

import static com.openexchange.java.Autoboxing.i;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.imap.services.Services;
import com.openexchange.java.Strings;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.AccountNature;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.user.UserService;


/**
 * {@link AbstractOwnerCapableEntity2ACL}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public abstract class AbstractOwnerCapableEntity2ACL extends Entity2ACL {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractOwnerCapableEntity2ACL.class);
    }

    /** The identifier of special "owner" ACL */
    protected static final String ALIAS_OWNER = "owner";

    /** The identifier of special "anyone" ACL */
    protected static final String ALIAS_ANYONE = "anyone";

    private final Cache<String, CachedString> cacheSharedOwners;

    /**
     * Initializes a new {@link AbstractOwnerCapableEntity2ACL}.
     */
    protected AbstractOwnerCapableEntity2ACL() {
        super();
        cacheSharedOwners = CacheBuilder.newBuilder().maximumSize(50000).expireAfterAccess(30, TimeUnit.MINUTES).build();
    }

    /**
     * Gets the IMAP server.
     *
     * @return The IMAP server
     */
    protected abstract IMAPServer getIMAPServer();

    @Override
    public String getACLName(int userId, Context ctx, Entity2ACLArgs entity2AclArgs) throws OXException {
        if (userId == OCLPermission.ALL_GROUPS_AND_USERS) {
            return ALIAS_ANYONE;
        }

        Object[] args = entity2AclArgs.getArguments(getIMAPServer());
        if (args == null || args.length == 0) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create();
        }

        int accountId = ((Integer) args[0]).intValue();
        String serverurl = args[1].toString();
        int sessionUser = ((Integer) args[2]).intValue();
        String fullName = (String) args[3];
        char separator = ((Character) args[4]).charValue();
        String sharedOwner = getSharedFolderOwner(fullName, separator, (String[]) args[5]);
        boolean secondaryAccount = ((Boolean) args[7]).booleanValue();
        boolean resolveCapability = ((Boolean) args[8]).booleanValue();
        if (null == sharedOwner) {
            /*
             * A non-shared folder
             */
            if ((sessionUser == userId) && !equalsOrStartsWith(fullName, (String[]) args[6], separator)) {
                /*
                 * Logged-in user is equal to given user
                 */
                return ALIAS_OWNER;
            }
            return getACLNameInternal(userId, ctx, accountId, serverurl, entity2AclArgs.getSession(), resolveCapability);
        }
        /*
         * A shared folder
         */
        final int sharedOwnerID = getUserIDInternal(sharedOwner, ctx, accountId, secondaryAccount, serverurl, entity2AclArgs.getSession(), resolveCapability);
        if (sharedOwnerID == userId) {
            /*
             * Owner is equal to given user
             */
            return ALIAS_OWNER;
        }
        return getACLNameInternal(userId, ctx, accountId, serverurl, entity2AclArgs.getSession(), resolveCapability);
    }

    @Override
    public UserGroupID getEntityID(String pattern, Context ctx, Entity2ACLArgs entity2AclArgs) throws OXException {
        if (ALIAS_ANYONE.equalsIgnoreCase(pattern)) {
            return ALL_GROUPS_AND_USERS;
        }
        final Object[] args = entity2AclArgs.getArguments(getIMAPServer());
        if (args == null || args.length == 0) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create();
        }
        final int accountId = ((Integer) args[0]).intValue();
        final String serverUrl = args[1].toString();
        final int sessionUser = ((Integer) args[2]).intValue();
        final String sharedOwner = getSharedFolderOwner((String) args[3], ((Character) args[4]).charValue(), (String[]) args[5]);
        final boolean secondaryAccount = ((Boolean) args[7]).booleanValue();
        final boolean resolveCapability = ((Boolean) args[8]).booleanValue();
        if (null == sharedOwner) {
            /*
             * A non-shared folder
             */
            if (ALIAS_OWNER.equalsIgnoreCase(pattern)) {
                /*
                 * Map alias "owner" to logged-in user
                 */
                return getUserRetval(sessionUser);
            }
            return getUserRetval(getUserIDInternal(pattern, ctx, accountId, secondaryAccount, serverUrl, entity2AclArgs.getSession(), resolveCapability));
        }
        /*
         * A shared folder
         */
        if (ALIAS_OWNER.equalsIgnoreCase(pattern)) {
            /*
             * Map alias "owner" to shared folder owner
             */
            return getUserRetval(getUserIDInternal(sharedOwner, ctx, accountId, secondaryAccount, serverUrl, entity2AclArgs.getSession(), resolveCapability));
        }
        return getUserRetval(getUserIDInternal(pattern, ctx, accountId, secondaryAccount, serverUrl, entity2AclArgs.getSession(), resolveCapability));
    }

    @Override
    public List<UserGroupID> getEntityIDs(String pattern, Context ctx, Entity2ACLArgs entity2AclArgs) throws OXException {
        if (ALIAS_ANYONE.equalsIgnoreCase(pattern)) {
            return Collections.singletonList(ALL_GROUPS_AND_USERS);
        }
        final Object[] args = entity2AclArgs.getArguments(getIMAPServer());
        if (args == null || args.length == 0) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create();
        }
        final int accountId = ((Integer) args[0]).intValue();
        final String serverUrl = args[1].toString();
        final int sessionUser = ((Integer) args[2]).intValue();
        final String sharedOwner = getSharedFolderOwner((String) args[3], ((Character) args[4]).charValue(), (String[]) args[5]);
        final boolean secondaryAccount = ((Boolean) args[7]).booleanValue();
        boolean resolveCapability = ((Boolean) args[8]).booleanValue();
        if (null == sharedOwner) {
            /*
             * A non-shared folder
             */
            if (ALIAS_OWNER.equalsIgnoreCase(pattern)) {
                /*
                 * Map alias "owner" to logged-in user
                 */
                return Collections.singletonList(getUserRetval(sessionUser));
            }
            return getUsersRetval(getUserIDsInternal(pattern, ctx, accountId, secondaryAccount, serverUrl, entity2AclArgs.getSession(), resolveCapability));
        }
        /*
         * A shared folder
         */
        if (ALIAS_OWNER.equalsIgnoreCase(pattern)) {
            /*
             * Map alias "owner" to shared folder owner
             */
            return getUsersRetval(getUserIDsInternal(sharedOwner, ctx, accountId, secondaryAccount, serverUrl, entity2AclArgs.getSession(), resolveCapability));
        }
        return getUsersRetval(getUserIDsInternal(pattern, ctx, accountId, secondaryAccount, serverUrl, entity2AclArgs.getSession(), resolveCapability));
    }

    /**
     * Gets the identifier of the owner for specified full name of a possibly shared folder.
     *
     * @param sharedFolderName The full name of the folder that might be shared
     * @param delim The delimiting character
     * @param otherUserNamespaces The paths of known shared folders
     * @return The identifier or <code>null</code>
     */
    protected final String getSharedFolderOwner(String sharedFolderName, char delim, String[] otherUserNamespaces) {
        if (null == otherUserNamespaces) {
            return null;
        }

        for (String otherUserNamespace : otherUserNamespaces) {
            if (sharedFolderName.startsWith(otherUserNamespace, 0)) {
                CachedString wrapper = cacheSharedOwners.getIfPresent(sharedFolderName);
                if (null == wrapper) {
                    String quotedDelim = Pattern.quote(String.valueOf(delim));
                    // E.g. "Shared\\Q/\\E((?:\\\\\\Q/\\E|[^\\Q/\\E])+)\\Q/\\E\.+"
                    StringBuilder abstractPattern = new StringBuilder().append(otherUserNamespace).append(quotedDelim);
                    abstractPattern.append("((?:\\\\").append(quotedDelim).append("|[^").append(quotedDelim).append("])+)");
                    abstractPattern.append(quotedDelim).append(".+");
                    Pattern pattern = Pattern.compile(abstractPattern.toString(), Pattern.CASE_INSENSITIVE);
                    Matcher m = pattern.matcher(sharedFolderName);
                    if (m.matches()) {
                        wrapper = CachedString.wrapperFor(m.group(1).replaceAll("\\s+", String.valueOf(delim)));
                    } else {
                        wrapper = CachedString.NIL;
                    }
                    cacheSharedOwners.put(sharedFolderName, wrapper);
                }

                return wrapper.string;
            }
        }

        return null;
    }

    /**
     * Checks if given full name is equal to or starts with any of given public folder paths
     *
     * @param fullName The full name to check
     * @param publicNamespaces The paths of known public folders
     * @param separator The separator character
     * @return
     */
    protected boolean equalsOrStartsWith(String fullName, String[] publicNamespaces, char separator) {
        if (publicNamespaces == null) {
            return false;
        }

        for (String publicNamespace : publicNamespaces) {
            if (Strings.isNotEmpty(publicNamespace) && (fullName.equals(publicNamespace) || fullName.startsWith(new StringBuilder(32).append(publicNamespace).append(separator).toString()))) {
                return true;
            }
        }

        return false;
    }

    protected static final String getACLNameInternal(int userId, Context ctx, int accountId, String serverUrl, Session session, boolean resolveCapability) throws OXException {
        MailLoginResolverService service = Services.getServiceSafe(MailLoginResolverService.class);
        int sessionContextId = session.getContextId();
        if (resolveCapability || service.isEnabled(sessionContextId)) {
            String mailLogin = resolveEntity2MailLogin(sessionContextId, userId, ctx.getContextId());
            if (mailLogin != null) {
                return mailLogin;
            }
        }
        final MailAccountStorageService storageService = Services.getService(MailAccountStorageService.class);
        if (null == storageService) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create( MailAccountStorageService.class.getName());
        }
        final String userLoginInfo;
        {
            final UserService userService = Services.getService(UserService.class);
            if (null == userService) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create( UserService.class.getName());
            }
            userLoginInfo = userService.getUser(userId, ctx).getLoginInfo();
        }
        try {
            return MailConfig.getLogin(storageService.getMailAccount(accountId, userId, ctx.getContextId()), userLoginInfo, userId, ctx.getContextId(), true, Optional.of(session));
        } catch (OXException e) {
            throw Entity2ACLExceptionCode.UNKNOWN_USER.create(e, Integer.valueOf(userId), Integer.valueOf(ctx.getContextId()), serverUrl);
        }
    }

    protected static int getUserIDInternal(String pattern, Context ctx, int accountId, boolean secondaryAccount, String serverUrl, Session session, boolean resolveCapability) throws OXException {
        MailLoginResolverService service = Services.getServiceSafe(MailLoginResolverService.class);
        int sessionContextId = session.getContextId();
        if (resolveCapability || service.isEnabled(sessionContextId)) {
            Integer userID = resolveMailLogin2UserID(sessionContextId, pattern);
            if (userID != null) {
                return userID.intValue();
            }
        }
        int sessionUser = session.getUserId();
        int[] ids = MailConfig.getUserIDsByMailLogin(pattern, secondaryAccount ? AccountNature.SECONDARY : (Account.DEFAULT_ID == accountId ? AccountNature.PRIMARY : AccountNature.REGULAR), serverUrl, sessionUser, ctx);
        if (0 == ids.length) {
            throw Entity2ACLExceptionCode.RESOLVE_USER_FAILED.create(pattern);
        }
        if (1 == ids.length) {
            return ids[0];
        }
        Arrays.sort(ids);
        if (Arrays.binarySearch(ids, sessionUser) >= 0) {
            // Prefer session user
            if (secondaryAccount) {
                LoggerHolder.LOG.debug("Found multiple users with login \"{}\" subscribed to IMAP server \"{}\": {}{}The session user's ID is returned.", pattern, serverUrl, Arrays.toString(ids), Strings.getLineSeparator());
            } else {
                LoggerHolder.LOG.warn("Found multiple users with login \"{}\" subscribed to IMAP server \"{}\": {}{}The session user's ID is returned.", pattern, serverUrl, Arrays.toString(ids), Strings.getLineSeparator());
            }
            return sessionUser;
        }
        LoggerHolder.LOG.warn("Found multiple users with login \"{}\" subscribed to IMAP server \"{}\": {}{}The first found user is returned.", pattern, serverUrl, Arrays.toString(ids), Strings.getLineSeparator());
        return ids[0];
    }

    protected static int[] getUserIDsInternal(String pattern, Context ctx, int accountId, boolean secondaryAccount, String serverUrl, Session session, boolean resolveCapability) throws OXException {
        MailLoginResolverService service = Services.getServiceSafe(MailLoginResolverService.class);
        if (resolveCapability || service.isEnabled(session.getContextId())) {
            Integer userID = resolveMailLogin2UserID(session.getContextId(), pattern);
            if (userID != null) {
                return new int[] { i(userID) };
            }
        }
        int[] ids = MailConfig.getUserIDsByMailLogin(pattern, secondaryAccount ? AccountNature.SECONDARY : (Account.DEFAULT_ID == accountId ? AccountNature.PRIMARY : AccountNature.REGULAR), serverUrl, session.getUserId(), ctx);
        if (0 == ids.length) {
            throw Entity2ACLExceptionCode.RESOLVE_USER_FAILED.create(pattern);
        }
        return ids;
    }

}
