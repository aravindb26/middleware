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

package com.openexchange.cluster.serialization.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.session.Origin;
import com.openexchange.session.PutIfAbsent;
import com.openexchange.session.Session;

/**
 * {@link ClusterSession} - A session representation held in cluster collections.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ClusterSession implements PutIfAbsent {

    private String loginName;
    private String password;
    private int contextId;
    private int userId;
    private String sessionId;
    private String secret;
    private String login;
    private String randomToken;
    private String localIp;
    private String authId;
    private String hash;
    private String client;
    private String userLogin;
    private Origin origin;
    private boolean staySignedIn;
    private final ConcurrentMap<String, Object> parameters;

    /**
     * Initializes a new {@link ClusterSession}.
     */
    public ClusterSession() {
        super();
        this.parameters = new ConcurrentHashMap<String, Object>(10, 0.9f, 1);
    }

    /**
     * Initializes a new {@link StoredSession}.
     */
    public ClusterSession(String sessionId, String loginName, String password, int contextId, int userId, String secret, String login,
        String randomToken, String localIP, String authId, String hash, String client, String userLogin, boolean staySignedIn, Origin origin, Map<String, Object> parameters) {
        this();
        this.sessionId = sessionId;
        this.loginName = loginName;
        this.password = password;
        this.contextId = contextId;
        this.userId = userId;
        this.secret = secret;
        this.login = login;
        this.randomToken = randomToken;
        this.localIp = localIP;
        this.authId = authId;
        this.hash = hash;
        this.client = client;
        this.userLogin = userLogin;
        this.origin = origin;
        this.staySignedIn = staySignedIn;
        // Take over parameters (if not null)
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }

    /**
     * Initializes a new {@link StoredSession}.
     */
    public ClusterSession(final Session session) {
        this();
        this.authId = session.getAuthId();
        this.client = session.getClient();
        this.contextId = session.getContextId();
        this.hash = session.getHash();
        this.localIp = session.getLocalIp();
        this.login = session.getLogin();
        this.loginName = session.getLoginName();
        // Assign parameters (if any)
        for (String name : session.getParameterNames()) {
            Object value = session.getParameter(name);
            if (null != value) {
                this.parameters.put(name, value);
            }
        }
        this.password = session.getPassword();
        this.randomToken = session.getRandomToken();
        this.secret = session.getSecret();
        this.sessionId = session.getSessionID();
        this.userId = session.getUserId();
        this.userLogin = session.getUserlogin();
        this.staySignedIn = session.isStaySignedIn();
        this.origin = session.getOrigin();
    }

    @Override
    public String getLoginName() {
        return loginName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password The password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int getContextId() {
        return contextId;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getRandomToken() {
        return randomToken;
    }

    @Override
    public String getLocalIp() {
        return localIp;
    }

    @Override
    public void setLocalIp(final String localIp) {
        this.localIp = localIp;
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
    public void setHash(final String hash) {
        this.hash = hash;
    }

    @Override
    public String getClient() {
        return client;
    }

    @Override
    public void setClient(final String client) {
        this.client = client;
    }

    @Override
    public Object setParameterIfAbsent(String name, Object value) {
        if (PARAM_LOCK.equals(name)) {
            return parameters.get(PARAM_LOCK);
        }
        return parameters.putIfAbsent(name, value);
    }

    @Override
    public boolean containsParameter(final String name) {
        return parameters.containsKey(name);
    }

    @Override
    public Object getParameter(final String name) {
        return parameters.get(name);
    }

    @Override
    public String getSessionID() {
        return sessionId;
    }

    @Override
    public String getUserlogin() {
        return userLogin;
    }

    @Override
    public void setParameter(final String name, final Object value) {
        if (null == value) {
            parameters.remove(name);
        } else {
            parameters.put(name, value);
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
    public String toString() {
        StringBuilder builder = new StringBuilder(512);
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
        if (userLogin != null) {
            builder.append("userLogin=").append(userLogin).append(delim);
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

    @Override
    public Origin getOrigin() {
        return origin;
    }

}
