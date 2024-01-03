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
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.imap.services.Services;
import com.openexchange.logging.LogUtility;
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
 * {@link AbstractAnyoneCapableEntity2ACL}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public abstract class AbstractAnyoneCapableEntity2ACL extends Entity2ACL {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractAnyoneCapableEntity2ACL.class);
    }

    /** The identifier of special "anyone" ACL */
    protected static final String ALIAS_ANYONE = "anyone";

    /**
     * Initializes a new {@link AbstractAnyoneCapableEntity2ACL}.
     */
    protected AbstractAnyoneCapableEntity2ACL() {
        super();
    }

    /**
     * Gets the IMAP server.
     *
     * @return The IMAP server
     */
    protected abstract IMAPServer getIMAPServer();

    @Override
    public String getACLName(int userId, Context ctx, Entity2ACLArgs entity2AclArgs) throws OXException {
        if (OCLPermission.ALL_GROUPS_AND_USERS == userId) {
            return ALIAS_ANYONE;
        }
        final Object[] args = entity2AclArgs.getArguments(getIMAPServer());
        if (args == null || args.length == 0) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create();
        }
        final boolean resolveCapability;
        try {
            resolveCapability = ((Boolean) args[4]).booleanValue();
        } catch (ClassCastException e) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create(e);
        }
        int sessionContextId = entity2AclArgs.getSession().getContextId();
        MailLoginResolverService mailLoginResolver = Services.getServiceSafe(MailLoginResolverService.class);
        if (resolveCapability || mailLoginResolver.isEnabled(sessionContextId)) {
            return resolveEntity2MailLogin(sessionContextId, userId, ctx.getContextId());
        }
        final MailAccountStorageService storageService = Services.getService(MailAccountStorageService.class);
        final String userLoginInfo;
        {
            final UserService userService = Services.getService(UserService.class);
            if (null == userService) {
                throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(UserService.class.getName());
            }
            userLoginInfo = userService.getUser(userId, ctx).getLoginInfo();
        }
        final int accountId;
        try {
            accountId = ((Integer) args[0]).intValue();
        } catch (ClassCastException e) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create(e);
        }
        try {
            return MailConfig.getLogin(storageService.getMailAccount(accountId, userId, ctx.getContextId()), userLoginInfo, userId, ctx.getContextId(), true, Optional.of(entity2AclArgs.getSession()));
        } catch (OXException e) {
            throw Entity2ACLExceptionCode.UNKNOWN_USER.create(e, Integer.valueOf(userId), Integer.valueOf(ctx.getContextId()), args[1].toString());
        }
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
        try {
            final int accountId = ((Integer) args[0]).intValue();
            final String serverUrl = args[1].toString();
            final boolean secondaryAccount = ((Boolean) args[3]).booleanValue();
            boolean resolveCapability = ((Boolean) args[4]).booleanValue();
            return getUserRetval(getUserIDInternal(pattern, ctx, accountId, serverUrl, entity2AclArgs.getSession(), secondaryAccount, resolveCapability));
        } catch (ClassCastException e) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create(e);
        }
    }

    @Override
    public List<UserGroupID> getEntityIDs(String pattern, Context ctx, Entity2ACLArgs entity2AclArgs) throws OXException {
        if (ALIAS_ANYONE.equalsIgnoreCase(pattern)) {
            return Collections.singletonList(ALL_GROUPS_AND_USERS);
        }
        Object[] args = entity2AclArgs.getArguments(getIMAPServer());
        if (args == null || args.length == 0) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create();
        }
        try {
            int accountId = ((Integer) args[0]).intValue();
            String serverUrl = args[1].toString();
            boolean secondaryAccount = ((Boolean) args[3]).booleanValue();
            boolean resolveCapability = ((Boolean) args[4]).booleanValue();
            return getUsersRetval(getUserIDsInternal(pattern, ctx, accountId, serverUrl, entity2AclArgs.getSession(), secondaryAccount, resolveCapability));
        } catch (ClassCastException e) {
            throw Entity2ACLExceptionCode.MISSING_ARG.create(e);
        }
    }

    private static int getUserIDInternal(String pattern, Context ctx, int accountId, String serverUrl, Session session, boolean secondaryAccount, boolean resolveCapability) throws OXException {
        MailLoginResolverService mailLoginResolver = Services.getServiceSafe(MailLoginResolverService.class);
        int sessionContextId = session.getContextId();
        if (resolveCapability || mailLoginResolver.isEnabled(sessionContextId)) {
            Integer userId = resolveMailLogin2UserID(sessionContextId, pattern);
            if (userId == null) {
                throw Entity2ACLExceptionCode.RESOLVE_USER_FAILED.create(pattern);
            }
            return userId.intValue();
        }
        int sessionUser = session.getUserId();
        int[] ids = MailConfig.getUserIDsByMailLogin(pattern, secondaryAccount ? AccountNature.SECONDARY : (Account.DEFAULT_ID == accountId ? AccountNature.PRIMARY : AccountNature.REGULAR), serverUrl, sessionUser, ctx);
        if (0 == ids.length) {
            throw Entity2ACLExceptionCode.RESOLVE_USER_FAILED.create(pattern);
        }
        if (1 == ids.length) {
            return ids[0];
        }
        // Prefer session user
        Arrays.sort(ids);
        if (Arrays.binarySearch(ids, sessionUser) >= 0) {
            LoggerHolder.LOG.warn("Found multiple users with login \"{}\" subscribed to IMAP server \"{}\": {}\nThe session user's ID is returned.", pattern, serverUrl, LogUtility.toStringObjectFor(ids));
            return sessionUser;
        }
        LoggerHolder.LOG.warn("Found multiple users with login \"{}\" subscribed to IMAP server \"{}\": {}\nThe first found user is returned.", pattern, serverUrl, LogUtility.toStringObjectFor(ids));
        return ids[0];
    }

    private static int[] getUserIDsInternal(String pattern, Context ctx, int accountId, String serverUrl, Session session, boolean secondaryAccount, boolean resolveCapability) throws OXException {
        MailLoginResolverService mailLoginResolver = Services.getServiceSafe(MailLoginResolverService.class);
        int sessionContextId = session.getContextId();
        if (resolveCapability || mailLoginResolver.isEnabled(sessionContextId)) {
            Integer userID = resolveMailLogin2UserID(sessionContextId, pattern);
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
