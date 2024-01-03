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

package com.openexchange.passwordchange.common;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.authentication.BasicAuthenticationService;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.configuration.ConfigurationExceptionCodes;
import com.openexchange.event.CommonEvent;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.java.Strings;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mailaccount.Account;
import com.openexchange.passwordchange.PasswordChangeEvent;
import com.openexchange.passwordchange.PasswordChangeService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.user.User;
import com.openexchange.user.UserExceptionCode;
import com.openexchange.user.UserService;

/**
 * {@link AbstractPasswordChangeService} - Performs changing a user's password
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractPasswordChangeService implements PasswordChangeService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractPasswordChangeService.class);

    protected final UserService userService;
    protected final AuthenticationServiceRegistry registry;
    protected final LeanConfigurationService configService;
    protected final SessiondService sessiondService;
    protected final EventAdmin eventAdmin;
    protected final CacheService cacheService;

    /**
     * Initializes a new {@link AbstractPasswordChangeService}
     *
     * @param services The service lookup
     * @throws OXException If services are missing
     */
    public AbstractPasswordChangeService(ServiceLookup services) throws OXException {
        this(services.getServiceSafe(UserService.class), services.getServiceSafe(AuthenticationServiceRegistry.class), services.getServiceSafe(LeanConfigurationService.class), //
            services.getServiceSafe(SessiondService.class), services.getServiceSafe(EventAdmin.class), services.getServiceSafe(CacheService.class));
    }

    /**
     * Initializes a new {@link AbstractPasswordChangeService}.
     *
     * @param userService The {@link UserService}
     * @param registry The {@link AuthenticationServiceRegistry}
     * @param configService The {@link LeanConfigurationService}
     * @param sessiondService The {@link SessiondService}
     * @param eventAdmin The {@link EventAdmin}
     * @param cacheService The {@link CacheService}
     */
    public AbstractPasswordChangeService(UserService userService, AuthenticationServiceRegistry registry, LeanConfigurationService configService,//
        SessiondService sessiondService, EventAdmin eventAdmin, CacheService cacheService) {
        this.userService = userService;
        this.registry = registry;
        this.configService = configService;
        this.sessiondService = sessiondService;
        this.eventAdmin = eventAdmin;
        this.cacheService = cacheService;
    }

    /**
     * Performs the password update.
     *
     * @param event The event containing the session of the user whose password shall be changed, the context, the new password, and the old
     *            password (needed for verification)
     * @throws OXException If password update fails
     */
    @Override
    public boolean perform(PasswordChangeEvent event) throws OXException {
        if (false == allow(event.getContext(), event.getSession().getUserId())) {
            throw UserExceptionCode.PERMISSION.create(Integer.valueOf(event.getContext().getContextId()));
        }
        check(event);
        update(event);
        propagate(event);
        return true;
    }

    /*
     * ============================== Abstract methods ==============================
     */

    /**
     * Actually updates the password in affected resources
     *
     * @param event The event containing the session of the user whose password shall be changed, the context, the new password, and the old
     *            password (needed for verification)
     * @throws OXException If updating the password fails
     */
    protected abstract void update(PasswordChangeEvent event) throws OXException;

    /*
     * ============================== Default implementation ==============================
     */

    protected boolean allow(Context context, int userId) {
        if (null == context || userId < 0) {
            return false;
        }
        if (false == isEnabled(context.getContextId(), userId)) {
            return false;
        }
        try {
            /*
             * At the moment security service is not used for timing reasons but is ought to be used later on
             */
            return UserConfigurationStorage.getInstance().getUserConfiguration(userId, context).isEditPassword();
            /*
             * TODO: Remove statements above and replace with commented call below
             */
            // checkBySecurityService();
        } catch (OXException e) {
            LOG.debug("Error while getting permissions: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Specifies whether the old password is supposed to be checked prior to changing a user's password.
     * <p>
     * Default is true.
     *
     * @param event The event containing the session of the user whose password shall be changed, the context, the new password, and the old password (needed for verification)
     * @return <code>true</code> if the old password is supposed to be checked; otherwise <code>false</code>
     */
    protected boolean checkOldPassword(@SuppressWarnings("unused") PasswordChangeEvent event) {
        return true;
    }

    /**
     * Check old password
     *
     * @param event The event containing the session of the user whose password shall be changed, the context, the new password, and the old
     *            password (needed for verification)
     * @throws OXException If old password is invalid
     */
    protected void check(final PasswordChangeEvent event) throws OXException {
        try {
            /*
             * Check whether to verify old password prior to applying new one
             */
            boolean checkOldPassword = checkOldPassword(event);
            /*
             * Loading user also verifies its existence
             */
            Session session = event.getSession();
            User user = userService.getUser(session.getUserId(), session.getContextId());
            /*
             * Verify mandatory parameters
             */
            if (checkOldPassword && Strings.isEmpty(event.getOldPassword()) && false == user.isGuest()) {
                throw UserExceptionCode.MISSING_CURRENT_PASSWORD.create();
            }
            if (Strings.isEmpty(event.getNewPassword()) && false == user.isGuest()) {
                throw UserExceptionCode.MISSING_NEW_PASSWORD.create();
            }
            /*
             * Check old password
             */
            if (checkOldPassword) {
                Map<String, Object> properties = new LinkedHashMap<String, Object>(2);
                {
                    Map<String, List<String>> headers = event.getHeaders();
                    if (headers != null) {
                        properties.put("headers", headers);
                    }
                    com.openexchange.authentication.Cookie[] cookies = event.getCookies();
                    if (null != cookies) {
                        properties.put("cookies", cookies);
                    }
                }
                AuthenticationResult result;
                if (user.isGuest()) {
                    // @formatter:off
                    result = registry.optBasicAuthenticationService()
                            .orElseThrow(() -> ServiceExceptionCode.SERVICE_UNAVAILABLE.create(BasicAuthenticationService.class.getName()))
                            .handleLoginInfo(user.getId(), session.getContextId(), event.getOldPassword());
                    // @formatter:on
                } else {
                    // @formatter:off
                    AuthenticationRequest authenticationRequest = AuthenticationRequest.builder()
                        .withLogin(event.getSession().getLogin())
                        .withPassword(event.getOldPassword())
                        .withClient(event.getSession().getClient())
                        .withClientIP(event.getSession().getLocalIp())
                        .withUserAgent((String) event.getSession().getParameter(Session.PARAM_USER_AGENT))
                        .withParameters(Collections.emptyMap())
                        .withProperties(properties)
                        .build();
                    // @formatter:on
                    result = registry.doLogin(authenticationRequest, false);
                }
                if (result.getException().isPresent()) {
                    throw result.getException().get();
                }
                if (AuthenticationResult.Status.FAILED.equals(result.getStatus())) {
                    throw UserExceptionCode.INCORRECT_CURRENT_PASSWORD.create();
                }
            }

            if (false == user.isGuest()) {
                checkLength(event);
                checkPattern(event);
            }
        } catch (OXException e) {
            if (e.equalsCode(6, "LGI")) {
                /*
                 * Verification of old password failed
                 */
                throw UserExceptionCode.INCORRECT_CURRENT_PASSWORD.create(e);
            }
            throw e;
        }
    }

    /**
     * Check min/max length restrictions
     *
     * @param event The password change event
     * @throws OXException If restrictions aren't met
     */
    private void checkLength(PasswordChangeEvent event) throws OXException {
        int len = event.getNewPassword().length();
        int min = configService.getIntProperty(event.getSession().getUserId(), event.getSession().getContextId(), PasswordChangeProperties.MIN_LENGTH);
        if (min > 0 && len < min) {
            throw UserExceptionCode.INVALID_MIN_LENGTH.create(I(min));
        }

        int max = configService.getIntProperty(event.getSession().getUserId(), event.getSession().getContextId(), PasswordChangeProperties.MAX_LENGTH);
        if (max > 0 && len > max) {
            throw UserExceptionCode.INVALID_MAX_LENGTH.create(I(max));
        }
    }

    /**
     * Check against "allowed" pattern if defined. However does no
     * validation of new password since admin daemon does no validation, too
     *
     * @param event The password change event
     * @throws OXException If restrictions aren't met
     */
    private void checkPattern(PasswordChangeEvent event) throws OXException {
        String allowedPattern = configService.getProperty(event.getSession().getUserId(), event.getSession().getContextId(), PasswordChangeProperties.ALLOWED_PATTERN).trim();
        if (Strings.isEmpty(allowedPattern)) {
            return;
        }
        try {
            if (false == Pattern.matches(allowedPattern, event.getNewPassword())) {
                String allowedPatternHint = configService.getProperty(event.getSession().getUserId(), event.getSession().getContextId(), PasswordChangeProperties.PATTERN_HINT);
                throw UserExceptionCode.NOT_ALLOWED_PASSWORD.create(allowedPatternHint);
            }
        } catch (PatternSyntaxException e) {
            throw ConfigurationExceptionCodes.INVALID_CONFIGURATION.create(e, PasswordChangeProperties.ALLOWED_PATTERN.getFQPropertyName());
        }
    }

    /**
     * Propagates changed password throughout system: invalidate caches, propagate to sub-systems like mail, etc.
     *
     * @param event The event containing the session of the user whose password shall be changed, the context, the new password, and the old
     *            password (needed for verification)
     * @throws OXException If propagating the password change fails
     */
    protected void propagate(final PasswordChangeEvent event) throws OXException {
        /*
         * Remove possible session-bound cached default mail access
         */
        final Session session = event.getSession();
        MailAccess.getMailAccessCache().removeMailAccess(session, Account.DEFAULT_ID);
        /*
         * Invalidate user cache
         */
        int userId = session.getUserId();
        userService.invalidateUser(event.getContext(), userId);
        /*
         * Update password in session
         */
        try {
            sessiondService.changeSessionPassword(session.getSessionID(), event.getNewPassword());
        } catch (OXException e) {
            LOG.error("Updating password in user session failed", e);
            throw e;
        }
        /*
         * post event
         */
        int contextId = session.getContextId();
        final Map<String, Object> properties = new HashMap<String, Object>(8);
        properties.put(PasswordChangeProperties.PREFIX + "contextId", Integer.valueOf(contextId));
        properties.put(PasswordChangeProperties.PREFIX + "userId", Integer.valueOf(userId));
        properties.put(PasswordChangeProperties.PREFIX + "session", session);
        properties.put(PasswordChangeProperties.PREFIX + "sessionId", session.getSessionID());
        properties.put(PasswordChangeProperties.PREFIX + "clientId", session.getClient());
        properties.put(PasswordChangeProperties.PREFIX + "loginName", session.getLoginName());
        properties.put(PasswordChangeProperties.PREFIX + "oldPassword", event.getOldPassword());
        properties.put(PasswordChangeProperties.PREFIX + "newPassword", event.getNewPassword());
        properties.put(PasswordChangeProperties.PREFIX + "ipAddress", event.getIpAddress());
        properties.put(CommonEvent.PUBLISH_MARKER, Boolean.TRUE);
        eventAdmin.postEvent(new Event("com/openexchange/passwordchange", properties));
        /*
         * Invalidate caches manually
         */
        try {
            final CacheKey key = cacheService.newCacheKey(contextId, userId);
            Cache jcs = cacheService.getCache("User");
            jcs.remove(key);

            jcs = cacheService.getCache("UserPermissionBits");
            jcs.remove(key);

            jcs = cacheService.getCache("UserConfiguration");
            jcs.remove(key);

            jcs = cacheService.getCache("UserSettingMail");
            jcs.remove(key);

            jcs = cacheService.getCache("MailAccount");
            jcs.remove(cacheService.newCacheKey(contextId, String.valueOf(0), String.valueOf(userId)));
            jcs.remove(cacheService.newCacheKey(contextId, Integer.toString(userId)));
            jcs.invalidateGroup(Integer.toString(contextId));
        } catch (OXException e) {
            LOG.trace(e.getMessage(), e);
        }
    }
}
