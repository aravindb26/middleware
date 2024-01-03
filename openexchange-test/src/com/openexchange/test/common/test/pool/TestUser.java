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

package com.openexchange.test.common.test.pool;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.test.Host;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.LoginResponse;
import com.openexchange.testing.httpclient.modules.LoginApi;
/**
 * {@link TestUser}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.3
 */
public class TestUser implements Serializable {

    /** The user agent sent for all requests and the client identifier sent during the login request **/
    private static final String HTTP_API_TESTING_AGENT = "HTTP API Testing Agent";

    /** Wrapper object that delays initialization of logger class until needed */
    private static class LoggerHolder {

        static final Logger LOGGER = LoggerFactory.getLogger(TestUser.LoggerHolder.class);
    }

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7616495104780779831L;

    private final String login;

    private final String password;

    private final String user;

    private final String context;

    private final String createdBy;

    private final int userId;

    private final int ctxId;

    private final List<AJAXClient> ajaxClients = new LinkedList<>();

    private final List<SessionAwareClient> apiClients = new LinkedList<>();

    private final Optional<TestUserConfig> config;

    /**
     * Initializes a new {@link TestUser} using <code>-1</code> for context
     * and user identifier.
     *
     * @param user The user name
     * @param context The context of the user
     * @param password The password of the user
     * @param createdBy The creator of the user
     */
    public TestUser(String user, String context, String password, String createdBy) {
        this(user, context, password, I(-1), I(-1), createdBy);
    }

    /**
     * Initializes a new {@link TestUser}.
     *
     * @param user The user name
     * @param context The context of the user
     * @param password The password of the user
     * @param userId The user identifier
     * @param ctxId The context identifier
     * @param createdBy The creator of the user
     */
    public TestUser(String user, String context, String password, Integer userId, Integer ctxId, String createdBy) {
        this(user, context, password, userId, ctxId, Optional.empty(), createdBy);
    }

    /**
     * Initializes a new {@link TestUser}.
     *
     * @param user The user name
     * @param context The context of the user
     * @param password The password of the user
     * @param userId The user identifier
     * @param ctxId The context identifier
     * @param config
     * @param createdBy The creator of the user
     */
    public TestUser(String user, String context, String password, Integer userId, Integer ctxId, Optional<TestUserConfig> config, String createdBy) {
        this.user = user;
        this.context = context;
        this.login = Strings.isNotEmpty(context) ? user + "@" + context : user;
        this.password = password;
        this.userId = null == userId ? -1 : userId.intValue();
        this.ctxId = null == ctxId ? -1 : ctxId.intValue();
        this.config = config;
        this.createdBy = null == createdBy ? "" : createdBy;
    }

    /**
     * Get the login name
     * E.g. <code>anton@context1.ox.test</code>
     *
     * @return The login name
     */
    public String getLogin() {
        return login;
    }

    /**
     * gets the password for the user
     *
     * @return The user password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get the user name
     * E.g. <code>anton</code>
     *
     * @return The user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Get the context identifier the user belongs to
     *
     * @return The users context
     */
    public String getContext() {
        return context;
    }

    /**
     * Gets the userId
     *
     * @return The userId
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the context id
     *
     * @return The context id
     */
    public int getContextId() {
        return ctxId;
    }

    /**
     * Gets the creator
     *
     * @return The creator
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Gets or creates the standard {@link AJAXClient} for this user
     *
     * @return The {@link AJAXClient}
     * @throws OXException If creation fails
     * @throws IOException If creation fails
     * @throws JSONException If creation fails
     */
    public AJAXClient getAjaxClient() throws OXException, IOException, JSONException {
        if (ajaxClients.isEmpty()) {
            synchronized (ajaxClients) {
                if (ajaxClients.isEmpty()) {
                    ajaxClients.add(new AJAXClient(this));
                }
            }
        }
        return ajaxClients.get(0);
    }

    /**
     * Generates a new {@link AJAXClient} in addition to the standard
     * {@link AJAXClient}. If you want to get "just a client" use
     * {@link #getAjaxClient()}
     *
     * @return A new created {@link AJAXClient}
     * @throws OXException If creation fails
     * @throws IOException If creation fails
     * @throws JSONException If creation fails
     */
    public AJAXClient generateAjaxClient() throws OXException, IOException, JSONException {
        synchronized (ajaxClients) {
            AJAXClient ajaxClient = new AJAXClient(this);
            ajaxClients.add(ajaxClient);
            return ajaxClient;
        }
    }

    public void closeAjaxClients() {
        synchronized (ajaxClients) {
            for (AJAXClient c : ajaxClients) {
                try {
                    c.getSession().getHttpClient().close();
                } catch (IOException e) {
                    LoggerHolder.LOGGER.info("Unable to close resource", e);
                }
            }
        }
    }

    /**
     * Gets or creates the standard {@link ApiClient} for this user
     *
     * @param client The identifier of the client to get or create
     * @return The {@link SessionAwareClient}
     * @throws ApiException If creating fails
     */
    public SessionAwareClient getApiClient(String client) throws ApiException {
        if (client == null) {
            return getApiClient();
        }

        if (apiClients.stream().filter(c -> c.hasUserAgent(client)).count() == 0) {
            apiClients.add(configureApiClient(config, Optional.of(client), Host.SERVER));
        }
        return apiClients.stream().filter(c -> c.hasUserAgent(client)).findFirst().get();
    }

    /**
     * Gets or creates the standard {@link ApiClient} for this user
     *
     * @return The {@link SessionAwareClient}
     * @throws ApiException If creating fails
     */
    public SessionAwareClient getApiClient() throws ApiException {
        return getApiClient(Host.SERVER);
    }

    public SessionAwareClient getApiClient(Host host) throws ApiException {
        if (apiClients.isEmpty()) {
            synchronized (apiClients) {
                if (apiClients.isEmpty()) {
                    apiClients.add(configureApiClient(config, host));
                }
            }
        }
        return apiClients.get(0);
    }

    /**
     * Generates a new {@link ApiClient} in addition to the standard
     * {@link ApiClient}. If you want to get "just a client" use
     * {@link #getApiClient()}
     *
     * @return A new created {@link ApiClient}
     * @throws ApiException If generation fails
     */
    public SessionAwareClient generateApiClient() throws ApiException {
        synchronized (apiClients) {
            SessionAwareClient apiClient = configureApiClient(config, Host.SERVER);
            apiClients.add(apiClient);
            return apiClient;
        }
    }

    /**
     * Generates a new {@link ApiClient} in addition to the standard
     * {@link ApiClient}. If you want to get "just a client" use
     * {@link #getApiClient()}
     *
     * @param clientId The client identifier
     * @return A new created {@link ApiClient}
     * @throws ApiException If generation fails
     */
    public SessionAwareClient generateApiClient(String clientId) throws ApiException {
        synchronized (apiClients) {
            SessionAwareClient apiClient = configureApiClient(config, Host.SERVER, clientId);
            apiClients.add(apiClient);
            return apiClient;
        }
    }


    private SessionAwareClient configureApiClient(Optional<TestUserConfig> config, Host host) throws ApiException {
        return configureApiClient(config, Optional.empty(), host);
    }

    private SessionAwareClient configureApiClient(Optional<TestUserConfig> config, Host host, String clientId) throws ApiException {
        return configureApiClient(config, Optional.empty(), host, clientId);
    }

    private SessionAwareClient configureApiClient(Optional<TestUserConfig> config, Optional<String> userAgent, Host host) throws ApiException {
        return configureApiClient(config, userAgent, host, null);
    }

    private SessionAwareClient configureApiClient(Optional<TestUserConfig> config, Optional<String> userAgent, Host host, String clientId) throws ApiException {
        SessionAwareClient apiClient = new SessionAwareClient(this, config, userAgent);
        setBasePath(apiClient, host);
        apiClient.setUserAgent(userAgent.orElse(HTTP_API_TESTING_AGENT));

        if (clientId != null) {
            performLogin(apiClient, clientId);
        } else {
            performLogin(apiClient);
        }

        return apiClient;
    }

    public void performLogin() throws ApiException {
        if (apiClients.isEmpty() == false) {
            performLogin(apiClients.get(0));
        }
    }

    public void performLogin(String clientId) throws ApiException {
        if (apiClients.isEmpty() == false) {
            performLogin(apiClients.get(0), clientId);
        }
    }

    public void performLogout() throws ApiException {
        if (apiClients.isEmpty() == false) {
            performLogout(apiClients.get(0));
        }
    }

    /**
     * Perform a login
     *
     * @param client The {@link SessionAwareClient} to use
     * @return The given client
     * @throws ApiException
     */
    private SessionAwareClient performLogin(SessionAwareClient client) throws ApiException {
        return performLogin(client, null);
    }

    /**
     * Perform a login
     *
     * @param client The {@link SessionAwareClient} to use
     * @param clientId The client identifier
     * @return The given client
     * @throws ApiException
     */
    private SessionAwareClient performLogin(SessionAwareClient client, String clientId) throws ApiException {
        LoginApi api = new LoginApi(client);
        try {
            LoginResponse resp = api.doLogin(client.getUser().getLogin(), client.getUser().getPassword(), null, null, clientId, null, null, HTTP_API_TESTING_AGENT, Boolean.TRUE);
            assertNull(resp.getError(), resp.getErrorDesc());
            client.setSession(resp.getSession());
            client.setUserId(resp.getUserId());
            client.setClient(HTTP_API_TESTING_AGENT);
            return client;
        } catch (ApiException e) {
            LoggerHolder.LOGGER.error("Unable to peform login. Received status '{}' and message '{}'.", I(e.getCode()), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Performs a logout operation
     *
     * @param client The {@link SessionAwareClient} to logout
     * @throws ApiException
     */
    private void performLogout(SessionAwareClient client) throws ApiException {
        new LoginApi(client).doLogout(client.getSession());
    }

    private void setBasePath(ApiClient newClient, Host host) {
        String hostname = getHostname(host);
        String protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
        if (protocol == null) {
            protocol = "http";
        }
        String basePath = AJAXConfig.getProperty(AJAXConfig.Property.BASE_PATH);
        if (basePath.endsWith("/")) {
            basePath = Strings.trimEnd(basePath, '/');
        }
        newClient.setBasePath(protocol + "://" + hostname + basePath);
    }

    private String getHostname(Host host) {
        switch (host) {
            case SERVER:
                return AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
            case SINGLENODE:
                return AJAXConfig.getProperty(AJAXConfig.Property.SINGLENODE_HOSTNAME);
            default:
                return AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append("TestUser [");
        if (Strings.isNotEmpty(login)) {
            builder.append("login=").append(login);
        }
        builder.append(']');
        return builder.toString();
    }

}
