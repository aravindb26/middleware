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

package com.openexchange.admin.rmi.impl;

import java.util.List;
import java.util.Optional;
import org.osgi.framework.BundleContext;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.plugins.BasicAuthenticatorPluginInterface;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.rmi.dataobjects.AlwaysAccept;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.services.PluginInterfaces;
import com.openexchange.admin.storage.interfaces.OXAuthStorageInterface;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.tools.AdminCache;

/**
 *
 * @author cutmasta
 */
public class BasicAuthenticator extends OXCommonImpl {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BasicAuthenticator.class);

    /**
     * Creates an authenticator that <b>ignores</b> possibly registered {@link BasicAuthenticatorPluginInterface} instances.
     *
     * @return The authenticator instance
     * @throws StorageException If instantiation fails
     */
    public static BasicAuthenticator createNonPluginAwareAuthenticator() throws StorageException {
        return new BasicAuthenticator(false);
    }

    /**
     * Creates an authenticator that <b>respects</b> possibly registered {@link BasicAuthenticatorPluginInterface} instances.
     *
     * @return The authenticator instance
     * @throws StorageException If instantiation fails
     */
    public static BasicAuthenticator createPluginAwareAuthenticator() throws StorageException {
        return new BasicAuthenticator(true);
    }

    // --------------------------------------------------------------------------------------------------------------------------------

    private final OXAuthStorageInterface sqlAuth;
    private final OXAuthStorageInterface fileAuth;
    private final OXToolStorageInterface oxtool;
    private final AdminCache cache;
    private final boolean plugInAware;

    /**
     * Use this constructor in case additional bundles should be able to override login
     *
     * @param context The bundle context; needed to mark the authenticator as plugIn-aware
     * @throws StorageException If instantiation fails
     * @deprecated Use {@link #createPluginAwareAuthenticator()}
     */
    @Deprecated
    public BasicAuthenticator(BundleContext context) throws StorageException {
        this(context != null);
    }

    /**
     * Use this constructor in case no additional bundles should be able to override login!
     *
     * @throws StorageException If instantiation fails
     * @deprecated Use {@link #createNonPluginAwareAuthenticator()}
     */
    @Deprecated
    public BasicAuthenticator() throws StorageException {
        this(false);
    }

    /**
     * Initializes a new {@link BasicAuthenticator}.
     *
     * @param plugInAware Whether the authenticator should respect possibly registered {@link BasicAuthenticatorPluginInterface} instances.
     */
    private BasicAuthenticator(boolean plugInAware) throws StorageException {
        super();
        this.plugInAware = plugInAware;
        sqlAuth  = OXAuthStorageInterface.getInstanceSQL();
        fileAuth = OXAuthStorageInterface.getInstanceFile();
        oxtool = OXToolStorageInterface.getInstance();
        cache = ClientAdminThread.cache;
        if (null == cache) {
            // Obviously not properly initialized.
            StorageException e = new StorageException("Open-Xchange Admin not properly initialized.");
            LOG.error("Probably start-up of bundle \"com.openexchange.admin\" failed. Please check log files and/or bundle status via \"/opt/open-xchange/sbin/listbundles\" command-line tool.", e);
            throw e;
        }
    }

    /**
     * Authenticates the master admin. Other bundles can register an OSGi
     * service here and take care about authentication themself
     *
     * @param authdata
     * @throws InvalidCredentialsException
     */
    public void doAuthentication(final Credentials authdata) throws InvalidCredentialsException {
        boolean autoLowerCase = cache.getProperties().getUserProp(AdminProperties.User.AUTO_LOWERCASE, false);
        if (autoLowerCase && null != authdata.getLogin()) {
            authdata.setLogin(authdata.getLogin().toLowerCase());
        }

        final Credentials master = cache.getMasterCredentials();
        if (autoLowerCase && null != master.getLogin()) {
            master.setLogin(master.getLogin().toLowerCase());
        }

        boolean doPluginAuth = false;
        if (cache.masterAuthenticationDisabled() || (authdata != null && master != null && !master.getLogin().equals(authdata.getLogin()))) {
            doPluginAuth = true;
        }
        // only let other plugins authenticate, when we have the BundleContext
        // AND when
        if ( plugInAware && doPluginAuth) {
            // Trigger plugin extensions
            final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
            if (null != pluginInterfaces) {
                for (final BasicAuthenticatorPluginInterface authplug : pluginInterfaces.getBasicAuthenticatorPlugins().getServiceList()) {
                    final String bundlename = authplug.getClass().getName();
                    LOG.debug("Calling doAuthentication for plugin: {}", bundlename);
                    authplug.doAuthentication(authdata);
                    // leave
                    return;
                }
            }
        }
        // first check if whole authentication mech is disabled
        if (!cache.masterAuthenticationDisabled() && !(authdata instanceof AlwaysAccept)){
            if ( authdata == null ) {
                throw new InvalidCredentialsException("credential object is null");
            }
            if (!fileAuth.authenticate(authdata)){
                final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
                LOG.error("Master authentication for user: {}", authdata.getLogin(), invalidCredentialsException);
                throw invalidCredentialsException;
            }
        }
    }

    /**
     * Authenticates the masters of the given context.
     *
     * @param authdata The auth data
     * @param ctx The context
     * @throws InvalidCredentialsException
     * @throws StorageException
     */
    public void doAuthenticationContextMasters(final Credentials authdata, Context ctx) throws InvalidCredentialsException, StorageException {
        doAuthenticationContextMasters(authdata, ctx, false);
    }

    /**
     * Authenticates the masters of the given context.
     *
     * @param authdata The auth data
     * @param ctx The context
     * @param forExistenceCheck Whether this call is done for existence check or not
     * @throws InvalidCredentialsException
     * @throws StorageException
     */
    public void doAuthenticationContextMasters(final Credentials authdata, Context ctx, boolean forExistenceCheck) throws InvalidCredentialsException, StorageException {
        if (authdata == null) {
            throw new InvalidCredentialsException("credential object is null");
        }
        contextcheck(ctx);

        if (cache.masterAuthenticationDisabled() || (authdata instanceof AlwaysAccept)) {
            // auth disabled
            return;
        }

        // Auto lowercase authdata if configured
        boolean autoLowerCase = cache.getProperties().getUserProp(AdminProperties.User.AUTO_LOWERCASE, false);
        if (autoLowerCase && null != authdata.getLogin()) {
            authdata.setLogin(authdata.getLogin().toLowerCase());
        }

        // Get master credentials and auto lowercase
        final Credentials master = cache.getMasterCredentials();
        if (autoLowerCase && null != master.getLogin()) {
            master.setLogin(master.getLogin().toLowerCase());
        }

        // Check for master auth first
        if (master != null && master.getLogin().equals(authdata.getLogin())) {
            // User tries to authenticate with master credentials
            if (fileAuth.authenticate(authdata)) {
                // is master
                return;
            }
            final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
            LOG.error("Master authentication for user: {}", authdata.getLogin(), invalidCredentialsException);
            throw invalidCredentialsException;
        }

        // Check if context exists
        if (cache.getAdminCredentials(ctx) == null) {
            if (!OXToolStorageInterface.getInstance().existsContext(ctx)) {
                if (forExistenceCheck) {
                    return;
                }
                final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
                LOG.error("Requested context {} does not exist!", ctx.getId(), invalidCredentialsException);
                throw invalidCredentialsException;
            }
        }

        // If plugInAware and if the user is a master of the context try to authenticate via plugins
        if (plugInAware && (isMasterOfContext(authdata, ctx) || isOwnerOfContext(authdata, ctx))) {
            // Trigger plugin extensions
            final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
            // @formatter:off
            Optional<BasicAuthenticatorPluginInterface> optAuthPlugin = pluginInterfaces == null ? Optional.empty() : pluginInterfaces.getBasicAuthenticatorPlugins()
                                                                                                                                      .getServiceList()
                                                                                                                                      .stream()
                                                                                                                                      .findFirst();
            // @formatter:on
            if (optAuthPlugin.isPresent()) {
                final String bundlename = optAuthPlugin.get().getClass().getName();
                LOG.debug("Calling doAuthentication for plugin: {}", bundlename);
                optAuthPlugin.get().doAuthentication(authdata);
                // Plugin authenticates the user
                return;
            }
        }

        // User is not a master of the given context
        final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
        LOG.error("Master authentication for user: {}", authdata.getLogin(), invalidCredentialsException);
        throw invalidCredentialsException;
    }

    /**
     * Remove cached admin authdata from auth cache
     *
     * @param ctx
     */
    public void removeFromAuthCache(final Context ctx) {
       cache.removeAdminCredentials(ctx);
    }

    public boolean isMasterOfContext(final Credentials creds, final Context ctx) throws InvalidCredentialsException {
        if (!cache.isAllowMasterOverride() ) {
            return false;
        }
        if (cache.isMasterAdmin(creds) ) {
            return true;
        }
        if ( plugInAware ) {
            // Trigger plugin extensions
            final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
            if (null != pluginInterfaces) {
                for (final BasicAuthenticatorPluginInterface authplug : pluginInterfaces.getBasicAuthenticatorPlugins().getServiceList()) {
                    final String bundlename = authplug.getClass().getName();
                    LOG.debug("Calling isMasterOfContext for plugin: {}", bundlename);
                    return authplug.isMasterOfContext(creds, ctx);
                }
            }
        }
        return false;
    }

    /**
     * Checks if an admin owns the context
     *
     * @param creds The credentials of the admin
     * @param ctx The context to check
     * @return <code>true</code> if the admin owns the context, <code>false</code> otherwise
     * @throws InvalidCredentialsException
     */
    public boolean isOwnerOfContext(final Credentials creds, final Context ctx) throws InvalidCredentialsException {
        if (plugInAware) {
            final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
            if (null != pluginInterfaces) {
                for (final BasicAuthenticatorPluginInterface authplug : pluginInterfaces.getBasicAuthenticatorPlugins().getServiceList()) {
                    final String bundlename = authplug.getClass().getName();
                    LOG.debug("Calling isOwnerOfContext for plugin: {}", bundlename);
                    return authplug.isOwnerOfContext(creds, ctx);
                }
            }
        }
        return false;
    }

    public boolean isMasterOfContext(final Credentials creds, final List<Context> ctxs) throws InvalidCredentialsException {
        if (!cache.isAllowMasterOverride()) {
            return false;
        }
        if (cache.isMasterAdmin(creds)) {
            return true;
        }
        if (plugInAware) {
            // Trigger plugin extensions
            final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
            if (null != pluginInterfaces) {
                for (final BasicAuthenticatorPluginInterface authplug : pluginInterfaces.getBasicAuthenticatorPlugins().getServiceList()) {
                    final String bundlename = authplug.getClass().getName();
                    LOG.debug("Calling isMasterOfContext for plugin: {}", bundlename);
                    return authplug.isMasterOfContext(creds, ctxs);
                }
            }
        }
        return false;
    }

    /**
     * Authenticates ONLY the context admin!
     * This method also validates the Context object data!
     *
     * @param authdata
     * @param ctx
     * @throws InvalidCredentialsException
     * @throws StorageException
     * @throws InvalidDataException
     */
    public void doAuthentication(final Credentials authdata, final Context ctx) throws InvalidCredentialsException, StorageException {
        contextcheck(ctx);

        boolean autoLowerCase = cache.getProperties().getUserProp(AdminProperties.User.AUTO_LOWERCASE, false);
        if (autoLowerCase) {
            authdata.setLogin(authdata.getLogin().toLowerCase());
        }

        // only do context check, if we have not already admin creds in our cache for given context
        // ATTENTION: It is correct that we don't throw a now such context exception here because we won't
        // give an opportunity to indirectly check for contexts here
        if (cache.getAdminCredentials(ctx) == null) {
            if (!OXToolStorageInterface.getInstance().existsContext(ctx)) {
                final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
                LOG.error("Requested context {} does not exist!", ctx.getId(), invalidCredentialsException);
                throw invalidCredentialsException;
            }
        }

        // first check if whole authentication mechanism is disabled
        if (!cache.contextAuthenticationDisabled() && !(authdata instanceof AlwaysAccept)) {
            if (isMasterOfContext(authdata, ctx)) {
                doAuthentication(authdata);
            } else if (!oxtool.existsUserName(ctx, authdata.getLogin()) || !sqlAuth.authenticate(authdata, ctx)) {
                final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
                LOG.error("Admin authentication for user {}", authdata.getLogin(), invalidCredentialsException);
                throw invalidCredentialsException;
            }
        }
    }

    /**
     * Authenticates ONLY the context admin!
     * This method also validates the Context object data!
     *
     * @param authdata
     * @param ctxs
     * @throws InvalidCredentialsException
     * @throws StorageException
     * @throws InvalidDataException
     */
    public void doAuthentication(final Credentials authdata, final List<Context> ctxs) throws InvalidCredentialsException, StorageException {
        if (cache.masterAuthenticationDisabled() || (authdata instanceof AlwaysAccept)) {
            // auth disabled
            return;
        }
        boolean autoLowerCase = cache.getProperties().getUserProp(AdminProperties.User.AUTO_LOWERCASE, false);
        if (autoLowerCase) {
            authdata.setLogin(authdata.getLogin().toLowerCase());
        }

        // Get master credentials and auto lowercase
        final Credentials master = cache.getMasterCredentials();
        if (autoLowerCase && null != master.getLogin()) {
            master.setLogin(master.getLogin().toLowerCase());
        }

        // Check for master auth first
        if (master != null && master.getLogin().equals(authdata.getLogin())) {
            // User tries to authenticate with master credentials
            if (fileAuth.authenticate(authdata)) {
                // is master
                return;
            }
            final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
            LOG.error("Master authentication for user: {}", authdata.getLogin(), invalidCredentialsException);
            throw invalidCredentialsException;
        }

        for (Context c : ctxs) {
            contextcheck(c);
            // only do context check, if we have not already admin creds in our cache for given context
            // ATTENTION: It is correct that we don't throw a now such context exception here because we won't
            // give an opportunity to indirectly check for contexts here
            if (cache.getAdminCredentials(c) == null) {
                if (!OXToolStorageInterface.getInstance().existsContext(c)) {
                    final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
                    LOG.error("Requested context {} does not exist!", c.getId(), invalidCredentialsException);
                    throw invalidCredentialsException;
                }
            }
        }
        // first check if whole authentication mechanism is disabled
        if (!cache.contextAuthenticationDisabled() && !(authdata instanceof AlwaysAccept)) {
            if (isMasterOfContext(authdata, ctxs)) {
                doAuthentication(authdata);
            } else {
                // A admin can never be the context admin of multiple ctxs
                if (ctxs.size() != 1 || !oxtool.existsUserName(ctxs.get(0), authdata.getLogin()) || !sqlAuth.authenticate(authdata, ctxs.get(0))) {
                    final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Authentication failed");
                    LOG.error("Admin authentication for user {}", authdata.getLogin(), invalidCredentialsException);
                    throw invalidCredentialsException;
                }
            }
        }
    }

    /**
     * Authenticates all users within a context!
     * This method also validates the Context object data!
     * @param authdata
     * @param ctx
     * @throws InvalidCredentialsException
     * @throws StorageException
     * @throws InvalidDataException
     */
    public void doUserAuthentication(final Credentials authdata,final Context ctx) throws InvalidCredentialsException, StorageException {
        contextcheck(ctx);

        boolean autoLowerCase = cache.getProperties().getUserProp(AdminProperties.User.AUTO_LOWERCASE, false);
        if (autoLowerCase) {
            authdata.setLogin(authdata.getLogin().toLowerCase());
        }

        if (!OXToolStorageInterface.getInstance().existsContext(ctx)) {
            final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException(
                    "Authentication failed for user " + authdata.getLogin());
            LOG.error("Requested context {} does not exist!", ctx.getId(), invalidCredentialsException);
            throw invalidCredentialsException;
        }

        // first check if whole authentication mech is disabled
        if (!cache.contextAuthenticationDisabled() && !(authdata instanceof AlwaysAccept)) {
            if (!sqlAuth.authenticateUser(authdata, ctx)) {
                final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException(
                        "Authentication failed for user " + authdata.getLogin());
                LOG.error("User authentication: ", invalidCredentialsException);
                throw invalidCredentialsException;
            }
        }
    }
}
