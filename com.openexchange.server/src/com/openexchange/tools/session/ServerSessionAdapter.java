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

package com.openexchange.tools.session;

import java.util.Set;
import org.apache.commons.lang.Validate;
import com.drew.lang.annotations.NotNull;
import com.openexchange.annotation.NonNull;
import com.openexchange.annotation.Nullable;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Origin;
import com.openexchange.session.PutIfAbsent;
import com.openexchange.session.Session;
import com.openexchange.sessiond.impl.SessionObject;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserConfigurationService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link ServerSessionAdapter}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ServerSessionAdapter implements ServerSession, PutIfAbsent {

    /**
     * Gets the server session for specified session.
     *
     * @param session The session
     * @return The appropriate server session
     * @throws OXException If context cannot be resolved
     */
    public static ServerSession valueOf(final Session session) throws OXException {
        if (session instanceof ServerSession serverSession) {
            return serverSession;
        }
        return null == session ? null : new ServerSessionAdapter(session);
    }

    /**
     * Gets the server session for specified session.
     *
     * @param session The session
     * @param context The associated context
     * @return The appropriate server session
     */
    public static ServerSession valueOf(final Session session, final Context context) {
        if (session instanceof ServerSession serverSession) {
            return serverSession;
        }
        return null == session ? null : new ServerSessionAdapter(session, context);
    }

    /**
     * Gets the server session for specified session.
     *
     * @param session The session
     * @param context The associated context
     * @param user The user
     * @return The appropriate server session
     */
    public static ServerSession valueOf(final Session session, final Context context, final User user) {
        if (session instanceof ServerSession serverSession) {
            return serverSession;
        }
        return null == session ? null : new ServerSessionAdapter(session, context, user);
    }


    /**
     * Gets the server session for specified session.
     *
     * @param session The session
     * @param context The associated context
     * @param user The user
     * @param userConfiguration The user configuration
     * @return The appropriate server session
     */
    public static ServerSession valueOf(final Session session, final Context context, final User user, final UserConfiguration userConfiguration) {
        if (session instanceof ServerSession serverSession) {
            return serverSession;
        }
        return null == session ? null : new ServerSessionAdapter(session, context, user, userConfiguration);
    }

    /**
     * Creates a synthetic server session for specified user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The synthetic server session
     * @throws OXException If creation of server session fails
     */
    public static @NotNull ServerSession valueOf(int userId, int contextId) throws OXException {
        return new ServerSessionAdapter(userId, contextId);
    }

    // ---------------------------------------------------------------------------------------------------------------------- //

    private final Session session;
    private final ServerSession serverSession;
    private final Context context;
    private final User overwriteUser;
    private final UserConfiguration overwriteUserConfiguration;
    private final UserPermissionBits overwritePermissionBits;

    /**
     * Initializes a new {@link ServerSessionAdapter}.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @throws OXException If initialization fails
     */
    public ServerSessionAdapter(final int userId, final int contextId) throws OXException {
        super();
        if (contextId > 0) {
            context = loadContext(contextId);
        } else {
            context = null;
        }

        session = new SessionObject("synthetic") {
            @Override
            public int getUserId() { return userId; }
            @Override
            public int getContextId() {return contextId; }
        };

        serverSession = null;
        overwriteUserConfiguration = null;
        overwritePermissionBits = null;
        overwriteUser = null;
    }

    /**
     * Initializes a new {@link ServerSessionAdapter}.
     *
     * @param session The delegate session
     * @throws OXException If initialization fails
     */
    public ServerSessionAdapter(final Session session) throws OXException {
        this(session, loadContext(session.getContextId()), null, null, null);
    }

    /**
     * Initializes a new {@link ServerSessionAdapter}.
     *
     * @param session The delegate session
     * @param ctx The session's context object
     * @throws IllegalArgumentException If session argument is <code>null</code>
     */
    public ServerSessionAdapter(@NonNull final Session session, @NonNull final Context ctx) {
        this(session, ctx, null, null, null);
    }

    /**
     * Initializes a new {@link ServerSessionAdapter}.
     *
     * @param session The delegate session
     * @param ctx The session's context object
     * @param user The session's user object
     * @throws IllegalArgumentException If session argument is <code>null</code>
     */
    public ServerSessionAdapter(@NonNull final Session session, @NonNull final Context ctx, @Nullable final User user) {
        this(session, ctx, user, null, null);

    }

    /**
     * Initializes a new {@link ServerSessionAdapter}.
     *
     * @param session The delegate session
     * @param ctx The session's context object
     * @param user The session's user object
     * @throws IllegalArgumentException If session argument is <code>null</code>
     */
    public ServerSessionAdapter(@NonNull final Session session, @NonNull final Context ctx, @Nullable final User user, @Nullable final UserConfiguration userConfiguration) {
        this(session, ctx, user, userConfiguration, null);
    }

    /**
     * Initializes a new {@link ServerSessionAdapter}.
     *
     * @param session The delegate session
     * @param ctx The session's context object
     * @param user The session's user object
     * @throws IllegalArgumentException If session argument is <code>null</code>
     */
    public ServerSessionAdapter(@NonNull final Session session, @NonNull final Context ctx, @Nullable final User user, @Nullable final UserConfiguration userConfiguration, @Nullable final UserPermissionBits permissionBits) {
        super();
        Validate.notNull(session, "Session is null.");
        Validate.notNull(ctx, "Context is null.");

        context = ctx;
        overwriteUser = user;
        overwriteUserConfiguration = userConfiguration;
        overwritePermissionBits = permissionBits;
        if (session instanceof ServerSession srvSes) {
            this.serverSession = srvSes;
            this.session = null;
        } else {
            this.serverSession = null;
            this.session = session;
        }
    }

    @Override
    public int getContextId() {
        return session().getContextId();
    }

    @Override
    public String getLocalIp() {
        return session().getLocalIp();
    }

    @Override
    public void setLocalIp(final String ip) {
        session().setLocalIp(ip);
    }

    @Override
    public String getLoginName() {
        return session().getLoginName();
    }

    @Override
    public boolean containsParameter(final String name) {
        return session().containsParameter(name);
    }

    @Override
    public Object getParameter(final String name) {
        return session().getParameter(name);
    }

    @Override
    public String getPassword() {
        return session().getPassword();
    }

    @Override
    public String getRandomToken() {
        return session().getRandomToken();
    }

    @Override
    public String getSecret() {
        return session().getSecret();
    }

    @Override
    public String getSessionID() {
        return session().getSessionID();
    }

    @Override
    public int getUserId() {
        return session().getUserId();
    }

    @Override
    public String getUserlogin() {
        return session().getUserlogin();
    }

    @Override
    public void setParameter(final String name, final Object value) {
        session().setParameter(name, value);
    }

    @Override
    public Object setParameterIfAbsent(String name, Object value) {
        final Session ses = session();
        if (ses instanceof PutIfAbsent pia) {
            return pia.setParameterIfAbsent(name, value);
        }
        final Object prev = ses.getParameter(name);
        if (null == prev) {
            ses.setParameter(name, value);
            return null;
        }
        return prev;
    }

    @Override
    public String getAuthId() {
        return session().getAuthId();
    }

    @Override
    public String getLogin() {
        return session().getLogin();
    }

    @Override
    public String getHash() {
        return session().getHash();
    }

    @Override
    public String getClient() {
        return session().getClient();
    }

    @Override
    public void setClient(final String client) {
        session().setClient(client);
    }

    @Override
    public Context getContext() {
        if (serverSession != null) {
            return serverSession.getContext();
        }

        return context;
    }

    @Override
    public User getUser() {
        if (serverSession != null) {
            return serverSession.getUser();
        }
        if (null != overwriteUser) {
            return overwriteUser;
        }

        // Do not cache fetched instance
        final int userId = session.getUserId();
        if (userId <= 0) {
            return null;
        }
        try {
            return loadUser();
        } catch (OXException e) {
            throw new IllegalStateException("User object could not be loaded for user " + userId + " in context " + getContextId(), e);
        }
    }

    @Override
    public UserPermissionBits getUserPermissionBits() {
        if (serverSession != null) {
            return serverSession.getUserPermissionBits();
        }
        if (null != overwritePermissionBits) {
            return overwritePermissionBits;
        }

        // Do not cache fetched instance
        final int userId = null == overwriteUser ? session.getUserId() : overwriteUser.getId();
        if (userId <= 0) {
            return null;
        }
        try {
            return loadUserPermissionBits();
        } catch (OXException e) {
            throw new IllegalStateException("User permission bits could not be loaded for user " + userId + " in context " + getContextId(), e);
        }
    }

    @Override
    public UserConfiguration getUserConfiguration() {
        if (serverSession != null) {
            return serverSession.getUserConfiguration();
        }
        if (null != overwriteUserConfiguration) {
            return overwriteUserConfiguration;
        }

        // Do not cache fetched instance
        final int userId = null == overwriteUser ? session.getUserId() : overwriteUser.getId();
        if (userId <= 0) {
            return null;
        }
        try {
            return loadUserConfiguration();
        } catch (OXException e) {
            throw new IllegalStateException("User configuration object could not be loaded for user " + userId + " in context " + getContextId(), e);
        }
    }

    @Override
    public UserSettingMail getUserSettingMail() {
        if (serverSession != null) {
            return serverSession.getUserSettingMail();
        }
        // Do not cache fetched instance
        final int userId = null == overwriteUser ? session.getUserId() : overwriteUser.getId();
        if (userId <= 0) {
            return null;
        }

        if (getUser().isGuest() && !getUserPermissionBits().hasWebMail()) {
            return null;
        }

        return UserSettingMailStorage.getInstance().getUserSettingMail(userId, context);
    }

    private Session session() {
        return serverSession == null ? session : serverSession;
    }

    @Override
    public void setHash(final String hash) {
        session().setHash(hash);
    }

    @Override
    public boolean isAnonymous() {
        return session().getUserId() <= 0 || null == getContext();
    }

    @Override
    public String toString() {
        return session().toString();
    }

    @Override
    public boolean isTransient() {
        return session().isTransient();
    }

    @Override
    public boolean isStaySignedIn() {
        return session().isStaySignedIn();
    }

    @Override
    public Origin getOrigin() {
        return session().getOrigin();
    }

    @Override
    public int hashCode() {
        return session().hashCode();
    }

    @Override
    public Set<String> getParameterNames() {
        return session().getParameterNames();
    }

    private static Context loadContext(final int contextId) throws OXException {
        ContextService service = ServerServiceRegistry.getInstance().getService(ContextService.class);
        if (null == service) {
            throw ServiceExceptionCode.absentService(ContextService.class);
        }

        return service.getContext(contextId);
    }

    private User loadUser() throws OXException {
        UserService service = ServerServiceRegistry.getInstance().getService(UserService.class);
        if (null == service) {
            throw ServiceExceptionCode.absentService(UserService.class);
        }

        return service.getUser(getUserId(), getContextId());
    }

    private UserPermissionBits loadUserPermissionBits() throws OXException {
        UserPermissionService service = ServerServiceRegistry.getInstance().getService(UserPermissionService.class);
        if (null == service) {
            throw ServiceExceptionCode.absentService(UserPermissionService.class);
        }

        return service.getUserPermissionBits(getUserId(), getContext());
    }

    private UserConfiguration loadUserConfiguration() throws OXException {
        UserConfigurationService service = ServerServiceRegistry.getInstance().getService(UserConfigurationService.class);
        if (null == service) {
            throw ServiceExceptionCode.absentService(UserConfigurationService.class);
        }

        return service.getUserConfiguration(session());
    }

}
