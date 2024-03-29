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

package com.openexchange.authentication.basic;

import static com.openexchange.authentication.LoginExceptionCodes.COMMUNICATION;
import static com.openexchange.authentication.LoginExceptionCodes.INVALID_CREDENTIALS;
import org.slf4j.Logger;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.BasicAuthenticationService;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.user.User;
import com.openexchange.user.UserExceptionCode;
import com.openexchange.user.UserService;

/**
 * This implementation authenticates the user against the database.
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */
public class DefaultBasicAuthentication implements BasicAuthenticationService {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultBasicAuthentication.class);
    }

    private static final class AuthenticatedImpl implements Authenticated {

        private final String userInfo;
        private final String contextInfo;

        AuthenticatedImpl(String userInfo, String contextInfo) {
            super();
            this.userInfo = userInfo;
            this.contextInfo = contextInfo;
        }

        @Override
        public String getContextInfo() {
            return contextInfo;
        }

        @Override
        public String getUserInfo() {
            return userInfo;
        }

    } // End of class AuthenticatedImpl

    // ------------------------------------------------------------------------------------------------------------------------------- //

    private final ContextService contextService;
    private final UserService userService;

    /**
     * Default constructor.
     */
    public DefaultBasicAuthentication(ContextService contextService, UserService userService) {
        super();
        this.contextService = contextService;
        this.userService = userService;
    }

    @Override
    public AuthenticationResult handleLoginInfo(int userId, int contextId) throws OXException {
        try {
            Context ctx = contextService.getContext(contextId);
            User user = userService.getUser(userId, ctx);
            return AuthenticationResult.success(new AuthenticatedImpl(user.getLoginInfo(), ctx.getLoginInfo()[0]));
        } catch (OXException e) {
            return AuthenticationResult.failed(e);
        }
    }

    @Override
    public AuthenticationResult handleLoginInfo(int userId, int contextId, String password) throws OXException {
        try {
            Context ctx = contextService.getContext(contextId);
            User user = userService.getUser(userId, ctx);
            if (!userService.authenticate(user, password)) {
                return AuthenticationResult.failed(INVALID_CREDENTIALS.create());
            }
            return AuthenticationResult.success(new AuthenticatedImpl(user.getLoginInfo(), ctx.getLoginInfo()[0]));
        } catch (OXException e) {
            return AuthenticationResult.failed(e);
        }
    }

    @Override
    public AuthenticationResult doLogin(AuthenticationRequest authenticationRequest, boolean autologin) throws OXException {
        if (autologin) {
            return AuthenticationResult.failed(LoginExceptionCodes.NOT_SUPPORTED.create(DefaultBasicAuthentication.class.getName()));
        }
        String password = authenticationRequest.getPassword();
        if (null == password || 0 == password.length()) {
            return AuthenticationResult.failed(INVALID_CREDENTIALS.create());
        }

        Authenticated splitted = split(authenticationRequest.getLogin());
        {
            int ctxId;
            try {
                ctxId = contextService.getContextId(splitted.getContextInfo());
            } catch (OXException e) {
                throw COMMUNICATION.create(e);
            }
            if (ContextStorage.NOT_FOUND == ctxId) {
                LoggerHolder.LOG.debug("Missing context mapping for context \"{}\". Login failed.", splitted.getContextInfo());
                return AuthenticationResult.failed(INVALID_CREDENTIALS.create());
            }
            Context ctx = contextService.getContext(ctxId);

            int userId;
            try {
                userId = userService.getUserId(splitted.getUserInfo(), ctx);
            } catch (OXException e) {
                if (e.equalsCode(LdapExceptionCode.USER_NOT_FOUND.getNumber(), UserExceptionCode.PROPERTY_MISSING.getPrefix())) {
                    LoggerHolder.LOG.debug("Missing user mapping for user \"{}\". Login failed.", splitted.getUserInfo());
                    return AuthenticationResult.failed(INVALID_CREDENTIALS.create());
                }
                throw e;
            }
            User user = userService.getUser(userId, ctx);
            if (!userService.authenticate(user, password)) {
                return AuthenticationResult.failed(INVALID_CREDENTIALS.create());
            }
        }
        return AuthenticationResult.success(splitted);
    }

    /**
     * Splits user name and context.
     *
     * @param loginInfo the composite login information separated by an <code>'@'</code> sign
     * @return An {@code Authenticated} instance providing context and user name
     * @throws OXException If no separator is found
     */
    private static Authenticated split(final String loginInfo) {
        int pos = loginInfo.lastIndexOf('@');
        return pos < 0 ? new AuthenticatedImpl(loginInfo, "defaultcontext") : new AuthenticatedImpl(loginInfo.substring(0, pos), loginInfo.substring(pos + 1));
    }

}
