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

package com.openexchange.session;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link Session} - Represents an Open-Xchange session.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface Session {

    static final Condition EMPTY_CONDITION = new Condition() {

        @Override
        public void signalAll() {
            // Nothing to do
        }

        @Override
        public void signal() {
            // Nothing to do
        }

        @Override
        public boolean awaitUntil(Date deadline) throws InterruptedException {
            return true;
        }

        @Override
        public void awaitUninterruptibly() {
            // Nothing to do
        }

        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            return 0;
        }

        @Override
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void await() throws InterruptedException {
            // Nothing to do
        }
    };

    /**
     * The empty lock, doing nothing on invocations.
     */
    public static final Lock EMPTY_LOCK = new Lock() {

        @Override
        public void unlock() {
            // ignore
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public Condition newCondition() {
            return EMPTY_CONDITION;
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // ignore
        }

        @Override
        public void lock() {
            // ignore
        }
    };

    /**
     * The parameter name for session lock. The parameter value is an instance of <code>java.uitl.concurrent.locks.Lock</code>.
     * <p>
     * Usage for locking session might look like:
     *
     * <pre>
     * import java.util.concurrent.locks.Lock
     * ...
     * final Lock lock = (Lock) session.getParameter(Session.PARAM_LOCK);
     * if (null == lock) {
     * synchronized (session) {
     * ...
     * }
     * } else {
     * lock.lock();
     * try {
     * ...
     * } finally {
     * lock.unlock();
     * }
     * }
     * </pre>
     */
    public static final String PARAM_LOCK = "session.lock".intern();

    /**
     * The parameter name for request counter currently active on session. The parameter value is an instance of
     * <code>java.util.concurrent.atomic.AtomicInteger</code>.
     */
    public static final String PARAM_COUNTER = "com.openexchange.session.counter".intern();

    /**
     * The parameter for optional alternative identifier.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_ALTERNATIVE_ID = "__session.altId".intern();

    /**
     * The parameter for optional token identifier.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_TOKEN = "__session.token".intern();

    /**
     * The parameter to indicate a guest session.
     *
     * @type <code>java.lang.Boolean</code>
     */
    public static final String PARAM_GUEST = "__session.guest".intern();

    /**
     * The parameter for the cookie refresh time stamp.
     *
     * @type <code>java.lang.Long</code>
     */
    public static final String PARAM_COOKIE_REFRESH_TIMESTAMP = "__session.cookierefresh".intern();

    /**
     * The parameter for optional OAuth access token associated with a session intended for mail access & transport.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_OAUTH_ACCESS_TOKEN = "__session.oauth.access".intern();

    /**
     * The parameter for optional OAuth refresh token associated with a session intended for refreshing the access token
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_OAUTH_REFRESH_TOKEN = "__session.oauth.refresh".intern();

    /**
     * The parameter for optional OAuth ID token.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_OAUTH_ID_TOKEN = "__session.oidc.idToken";

    /**
     * The parameter that holds when the OAuth token expires. Value is string of unix timestamp.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE = "__session.oauth.access.expiry".intern();

    /**
     * The parameter for optional host name associated with a session.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_HOST_NAME = "__session.hostname".intern();

    /**
     * The parameter for optional user agent associated with a session.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_USER_AGENT = "__session.useragent".intern();

    /**
     * The parameter for optional device user agent associated with a session.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_DEVICE_USER_AGENT = "__session.deviceuseragent".intern();

    /**
     * The parameter for optional login time associated with a session
     *
     * @type <code>java.lang.Long</code>
     */
    public static final String PARAM_LOGIN_TIME = "__session.logintime".intern();

    /**
     * The parameter for optional local last active time associated with a session
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: This parameter is not spread through cluster
     * </div>
     *
     * @type <code>java.lang.Long</code>
     */
    public static final String PARAM_LOCAL_LAST_ACTIVE = "__session.locallastactive".intern();

    /**
     * The parameter for optional OAuth flag associated with a session.
     * <p>
     * If present, the session is marked as being created through
     * <a href="https://documentation.open-xchange.com/7.8.4/middleware/components/oauth_provider/developer_guide.html">Open-Xchange Middleware OAuth authentication flow</a>.
     *
     * @type <code>java.lang.Boolean</code>
     */
    public static final String PARAM_IS_OAUTH = "__session.oauth".intern();

    /**
     * The session parameter used to hold the client's push token
     */
    public static final String PARAM_PUSH_TOKEN = "__session.pushtoken".intern();

    /**
     * Parameter if multifactor authentication enabled for the user
     */
    public static final String MULTIFACTOR_PARAMETER = "multifactor".intern();

    /**
     * Parameter that the session has been multifactor authenticated successfully
     */
    public static final String MULTIFACTOR_AUTHENTICATED = "multifactorAuthenticated".intern();

    /**
     * Contains the time that the user last verified using multifactor.
     *
     * This parameter is not stored in session storage, and will be essentially set to 0 during a server change, or
     * autologin. Used to check recent authentication for certain security critical functions.
     *
     */
    public static final String MULTIFACTOR_LAST_VERIFIED = "multifactorSession";

    /**
     * Parameter marking the session as having restricted capabilities, contains a comma-separated string of allowed restricted scopes.
     *
     * @type <code>java.lang.String</code>
     */
    public static final String PARAM_RESTRICTED = "restricted".intern();

    /**
     * The session parameter to hold the client's brand
     */
    public static final String PARAM_BRAND = "com.openexchange.session.brand".intern();

    /**
     * The session parameter to hold session-associated database schema.
     */
    public static final String PARAM_USER_SCHEMA = "__session.userschema".intern();

    /**
     * @return the context identifier.
     */
    int getContextId();

    /**
     * IP address of the session client. Normally every request from the client using this session is verified to come from this IP address.
     *
     * @return The local IP address
     */
    String getLocalIp();

    /**
     * Updates the local IP address.
     *
     * @param ip The new IP address associated with this session
     * @deprecated Use {@link SessiondService#setLocalIp(String, String)} instead
     */
    @Deprecated
    void setLocalIp(String ip);

    /**
     * Gets the login name
     *
     * @return The login name
     */
    String getLoginName();

    /**
     * Checks if there is a parameter bound to specified name.
     *
     * @param name The parameter name
     * @return <code>true</code> if there is a parameter bound to specified name; otherwise <code>false</code>
     */
    boolean containsParameter(String name);

    /**
     * Gets the parameter bound to specified name or <code>null</code> if no such parameter is present
     *
     * @param name The parameter name
     * @return The parameter or <code>null</code>
     */
    Object getParameter(String name);

    /**
     * Gets the password
     *
     * @return The password
     */
    String getPassword();

    /**
     * Gets the random token
     *
     * @return The random token
     */
    String getRandomToken();

    /**
     * Gets the secret
     *
     * @return
     */
    String getSecret();

    /**
     * Gets the session ID
     *
     * @return The session ID
     */
    String getSessionID();

    /**
     * Gets the user ID
     *
     * @return The user ID
     */
    int getUserId();

    /**
     * Gets the user login
     *
     * @return The user login
     */
    String getUserlogin();

    /**
     * Gets the full login information including user and context information; e.g <code>firstname.lastname@domain.tld</code>. The user
     * entered this information into the login field on the login screen.
     * <p>
     * Beware that this highly depends on the behavior of the installed {@link AuthenticationService} what information is available here and
     * how context and user specific information is separated. You can not rely on a separating character like <i>'@'</i>.
     * <p>
     * Beware that in single sign-on (SSO) environments, this information may not be available.E.g. Kerberos uses tickets to authenticate
     * some user and SAML uses cookies. If such a mechanism is used by the {@link AuthenticationService}, no full login information will be
     * available and this method returns <code>null</code>.
     *
     * @return The full login information if it is available, otherwise <code>null</code>.
     */
    String getLogin();

    /**
     * Sets the parameter. Any existing parameters bound to specified name are replaced with given value.
     * <p>
     * A <code>null</code> value removes the parameter.
     * <p>
     * <div style="margin-left: 0.1in; margin-right: 0.5in; margin-bottom: 0.1in; background-color:#FFDDDD;">
     * <b>Note</b>: Parameters will not be considered on remote distribution.
     * </div>
     *
     * @param name The parameter name
     * @param value The parameter value
     */
    void setParameter(String name, Object value);

    /**
     * @return the authentication identifier that is used to trace the login request across different systems.
     */
    String getAuthId();

    /**
     * @return The HashCode distinguishing this session from others in the same store.
     */
    String getHash();

    /**
     * Updates the hash value of this session.
     *
     * @param hash The new hash value
     * @deprecated Use {@link SessiondService#setHash(String, String)} instead
     */
    @Deprecated
    void setHash(String hash);

    /**
     * The client is remembered through the whole session. It should identify what client uses the back-end. Normally this is the web
     * front-end but there may be other clients especially those that synchronize their data with OX. The client is a parameter passed to the
     * back-end during the login request.
     *
     * @return the client identifier of the client using the back-end.
     */
    String getClient();

    /**
     * Should only be used to update the client on a redirect request.
     *
     * @param client The new client identifier.
     * @deprecated Use {@link SessiondService#setClient(String, String)} instead
     */
    @Deprecated
    void setClient(String client);

    /**
     * Gets a value indicating whether the session is transient or not, i.e. the session is distributed to other nodes in the cluster
     * or has been put into another persistent storage. Typically, very short living sessions like those created for CalDAV-/CardDAV-
     * or InfoStore access via WebDAV are marked transient.
     *
     * @return <code>true</code> if the session is transient, <code>false</code>, otherwise
     */
    boolean isTransient();

    /**
     * @return the names of all parameters in this session.
     */
    Set<String> getParameterNames();

    /**
     * Gets this session's origin.
     *
     * @return The origin or <code>null</code>
     * @deprecated This not needed anymore is going to be removed or replaced in the future
     */
    @Deprecated
    Origin getOrigin();

    /**
     * Checks whether session is annotated with "stay signed in".
     *
     * @return <code>true</code> if annotated with "stay signed in"; otherwise <code>false</code>
     */
    boolean isStaySignedIn();

}
