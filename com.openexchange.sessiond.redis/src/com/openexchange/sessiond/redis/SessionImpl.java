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

package com.openexchange.sessiond.redis;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import com.openexchange.java.util.UUIDs;
import com.openexchange.session.DefaultSessionAttributes;
import com.openexchange.session.Origin;
import com.openexchange.session.PutIfAbsent;
import com.openexchange.session.Session;
import com.openexchange.session.SessionDescription;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link SessionImpl} - Implements interface {@link Session} (and {@link PutIfAbsent}).
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SessionImpl implements PutIfAbsent {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionImpl.class);

    private final String loginName;
    private volatile String password;
    private final int contextId;
    private final int userId;
    private final String sessionId;
    private final String secret;
    private final String login;
    private volatile String randomToken;
    private volatile String localIp;
    private final String authId;
    private volatile String hash;
    private volatile String client;
    private final boolean staySignedIn;
    private final Origin origin;
    private final ConcurrentMap<String, Object> parameters;
    private final String alternativeId;
    private final AtomicLong lastChecked;

    /**
     * Initializes a new {@link SessionImpl}
     *
     * @param userId The user ID
     * @param loginName The login name
     * @param password The password
     * @param contextId The context ID
     * @param sessionId The session ID
     * @param secret The secret (cookie identifier)
     * @param randomToken The random token
     * @param localIp The local IP
     * @param login The full user's login; e.g. <i>test@foo.bar</i>
     * @param authId The authentication identifier that is used to trace the login request across different systems
     * @param hash The hash identifier
     * @param client The client type
     * @param staySignedIn Whether session is supposed to be annotated with "stay signed in"; otherwise <code>false</code>
     */
    public SessionImpl(int userId, String loginName, String password, int contextId, String sessionId,
        String secret, String randomToken, String localIp, String login, String authId, String hash,
        String client, boolean staySignedIn, Origin origin) {
        this(userId, loginName, password, contextId, sessionId, secret, randomToken, localIp, login, authId, hash, client, staySignedIn,
            UUIDs.getUnformattedStringFromRandom(), origin, null);
    }

    /**
     * Initializes a new {@link SessionImpl}.
     *
     * @param sessionDescription The session description
     */
    public SessionImpl(SessionDescription sessionDescription) {
        this(sessionDescription.getUserID(), sessionDescription.getLoginName(), sessionDescription.getPassword(),
            sessionDescription.getContextId(), sessionDescription.getSessionID(), sessionDescription.getSecret(),
            sessionDescription.getRandomToken(), sessionDescription.getLocalIp(), sessionDescription.getLogin(),
            sessionDescription.getAuthId(), sessionDescription.getHash(), sessionDescription.getClient(), sessionDescription.isStaySignedIn(),
            sessionDescription.getAlternativeId(), sessionDescription.getOrigin(), sessionDescription.getParameters());
    }

    /**
     * Initializes a new {@link SessionImpl} from specified instance.
     * <p>
     * <b>Be careful: This constructor really copies all members!</b>
     *
     * @param other The instance to copy from
     */
    public SessionImpl(SessionImpl other) {
        super();
        this.userId = other.userId;
        this.loginName = other.loginName;
        this.password = other.password;
        this.sessionId = other.sessionId;
        this.secret = other.secret;
        this.randomToken = other.randomToken;
        this.localIp = other.localIp;
        this.contextId = other.contextId;
        this.login = other.login;
        this.authId = other.authId;
        this.hash = other.hash;
        this.client = other.client;
        this.staySignedIn = other.staySignedIn;
        this.origin = other.origin;
        this.alternativeId = other.alternativeId;
        this.parameters = other.parameters;
        this.lastChecked = new AtomicLong();
    }

    /**
     * Initializes a new {@link SessionImpl}
     *
     * @param userId The user ID
     * @param loginName The login name
     * @param password The password
     * @param contextId The context ID
     * @param sessionId The session ID
     * @param secret The secret (cookie identifier)
     * @param randomToken The random token
     * @param localIp The local IP
     * @param login The full user's login; e.g. <i>test@foo.bar</i>
     * @param authId The authentication identifier that is used to trace the login request across different systems
     * @param hash The hash identifier
     * @param client The client type
     * @param staySignedIn Whether session is supposed to be annotated with "stay signed in"; otherwise <code>false</code>
     * @param alternativeId The alternative session identifier
     */
    public SessionImpl(int userId, String loginName, String password, int contextId, String sessionId,
        String secret, String randomToken, String localIp, String login, String authId, String hash,
        String client, boolean staySignedIn, String alternativeId, Origin origin, Map<String, Object> parameters) {
        super();
        this.userId = userId;
        this.loginName = loginName;
        this.password = password;
        this.sessionId = sessionId;
        this.secret = secret;
        this.randomToken = randomToken;
        this.localIp = localIp;
        this.contextId = contextId;
        this.login = login;
        this.authId = authId;
        this.hash = hash;
        this.client = client;
        this.staySignedIn = staySignedIn;
        this.origin = origin;
        this.alternativeId = alternativeId;
        this.parameters = new ConcurrentHashMap<String, Object>(16, 0.9F, 1);
        if (null != parameters) {
            // Copy given parameters
            for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
                this.parameters.put(parameter.getKey(), parameter.getValue());
            }
        }
        this.parameters.put(PARAM_LOCK, new ReentrantLock());
        this.parameters.put(PARAM_COUNTER, new AtomicInteger());
        if (null != alternativeId) {
            this.parameters.put(PARAM_ALTERNATIVE_ID, alternativeId);
        } else if (false == this.parameters.containsKey(PARAM_ALTERNATIVE_ID)) {
            this.parameters.put(PARAM_ALTERNATIVE_ID, UUIDs.getUnformattedStringFromRandom());
        }
        this.lastChecked = new AtomicLong();
    }

    /**
     * Gets this session's last-checked time stamp.
     *
     * @return The last-accessed time stamp
     */
    public long getLastChecked() {
        return lastChecked.get();
    }

    /**
     * Sets this session's last-checked time stamp.
     *
     * @param lastChecked The last-accessed time stamp
     */
    public void setLastChecked(long lastChecked) {
        this.lastChecked.set(lastChecked);
    }

    @Override
    public int getContextId() {
        return contextId;
    }

    @Override
    public boolean containsParameter(String name) {
        return parameters.containsKey(name);
    }

    @Override
    public Object getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public String getRandomToken() {
        return randomToken;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public String getSessionID() {
        return sessionId;
    }

    /**
     * Gets the alternative identifier.
     *
     * @return The alternative identifier
     */
    public String getAlternativeId() {
        return alternativeId;
    }

    @Override
    public void setParameter(String name, Object value) {
        if (PARAM_LOCK.equals(name)) {
            return;
        }
        if (null == value) {
            parameters.remove(name);
        } else {
            parameters.put(name, value);
        }
    }

    @Override
    public Object setParameterIfAbsent(String name, Object value) {
        if (PARAM_LOCK.equals(name)) {
            return parameters.get(PARAM_LOCK);
        }
        return parameters.putIfAbsent(name, value);
    }

    @Override
    public String getLocalIp() {
        return localIp;
    }

    @Override
    public void setLocalIp(String localIp) {
        try {
            setLocalIp(localIp, true);
        } catch (Exception e) {
            LOG.warn("Failed to distribute change of IP address among remote nodes.", e);
        }
    }

    /**
     * Sets the local IP address
     *
     * @param localIp The local IP address
     * @param propagate Whether to propagate that IP change through {@code SessiondService}
     */
    public void setLocalIp(final String localIp, boolean propagate) {
        if (propagate) {
            SessiondService sessiondService = SessiondService.SERVICE_REFERENCE.get();
            if (sessiondService != null) {
                try {
                    sessiondService.setSessionAttributes(sessionId, DefaultSessionAttributes.builder().withLocalIp(localIp).build());
                } catch (Exception e) {
                    LOG.warn("Failed to change IP address for session {}", sessionId, e);
                }
            }
        } else {
            this.localIp = localIp;
        }
    }

    @Override
    public String getLoginName() {
        return loginName;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public String getUserlogin() {
        return loginName;
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password
     *
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getAuthId() {
        return authId;
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public void setHash(String hash) {
        try {
            setHash(hash, true);
        } catch (Exception e) {
            LOG.error("Failed to propagate change of hash identifier.", e);
        }
    }

    /**
     * Sets the hash identifier
     *
     * @param hash The hash identifier
     * @param propagate Whether to propagate that change through {@code SessiondService}
     */
    public void setHash(final String hash, boolean propagate) {
        if (propagate) {
            SessiondService sessiondService = SessiondService.SERVICE_REFERENCE.get();
            if (sessiondService != null) {
                try {
                    sessiondService.setSessionAttributes(sessionId, DefaultSessionAttributes.builder().withHash(hash).build());
                } catch (Exception e) {
                    LOG.warn("Failed to change hash for session {}", sessionId, e);
                }
            }
        } else {
            this.hash = hash;
        }
    }

    @Override
    public String getClient() {
        return client;
    }

    @Override
    public void setClient(String client) {
        try {
            setClient(client, true);
        } catch (Exception e) {
            LOG.error("Failed to propagate change of client identifier.", e);
        }
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public boolean isStaySignedIn() {
        return staySignedIn;
    }

    @Override
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Sets the client identifier
     *
     * @param client The client identifier
     * @param propagate Whether to propagate that change
     */
    public void setClient(final String client, boolean propagate) {
        if (propagate) {
            SessiondService sessiondService = SessiondService.SERVICE_REFERENCE.get();
            if (sessiondService != null) {
                try {
                    sessiondService.setSessionAttributes(sessionId, DefaultSessionAttributes.builder().withClient(client).build());
                } catch (Exception e) {
                    LOG.warn("Failed to change hash for session {}", sessionId, e);
                }
            }
        } else {
            this.client = client;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        String delim = ", ";
        builder.append('{');
        builder.append("contextId=").append(contextId).append(delim);
        builder.append("userId=").append(userId).append(delim);
        if (sessionId != null) {
            builder.append("sessionId=").append(sessionId).append(delim);
        }
        if (login != null) {
            builder.append("login=").append(login).append(delim);
        }
        if (loginName != null) {
            builder.append("loginName=").append(loginName).append(delim);
        }
        if (password != null) {
            builder.append("password=").append("*****").append(delim);
        }
        if (secret != null) {
            builder.append("secret=").append(secret).append(delim);
        }
        if (randomToken != null) {
            builder.append("randomToken=").append(randomToken).append(delim);
        }
        if (localIp != null) {
            builder.append("localIp=").append(localIp).append(delim);
        }
        if (authId != null) {
            builder.append("authId=").append(authId).append(delim);
        }
        if (hash != null) {
            builder.append("hash=").append(hash).append(delim);
        }
        if (client != null) {
            builder.append("client=").append(client).append(delim);
        }
        if (parameters != null) {
            builder.append("parameters=[");
            boolean firstParameter = true;
            for (Map.Entry<String, Object> parameterEntry : parameters.entrySet()) {
                if (firstParameter) {
                    firstParameter = false;
                } else {
                    builder.append(delim);
                }
                builder.append(parameterEntry.getKey()).append('=').append(parameterEntry.getValue());
            }
            builder.append(']');
        }
        builder.append('}');
        return builder.toString();
    }

    @Override
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }
}
