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

package com.openexchange.authentication;

import java.util.Collections;
import java.util.Map;
import com.google.common.collect.ImmutableMap;

/**
 * {@link AuthenticationRequest} - Containing all information needed to authenticate an user against an {@link AuthenticationService}.
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class AuthenticationRequest {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>AuthenticationRequest</code> */
    public static class Builder {

        private String login;
        private String password;
        private String client;
        private String clientIP;
        private String userAgent;
        private Map<String, Object> parameters;
        private Map<String, Object> properties;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the login.
         *
         * @param login The login to set
         */
        public Builder withLogin(String login) {
            this.login = login;
            return this;
        }

        /**
         * Sets the password.
         *
         * @param password The password to set
         */
        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the client.
         *
         * @param client The client to set
         */
        public Builder withClient(String client) {
            this.client = client;
            return this;
        }

        /**
         * Sets the client IP address.
         *
         * @param clientIP The client IP address to set
         */
        public Builder withClientIP(String clientIP) {
            this.clientIP = clientIP;
            return this;
        }

        /**
         * Sets the user-agent.
         *
         * @param userAgent The user-agent to set
         */
        public Builder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Sets the parameters.
         *
         * @param parameters The parameters to set
         */
        public Builder withParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Sets the properties containing HTTP headers and cookies.
         *
         * @param properties The properties to set
         */
        public Builder withProperties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Builds the resulting instance of <code>AuthenticationRequest</code> from this builder's arguments.
         *
         * @return The <code>AuthenticationRequest</code> instance
         */
        public AuthenticationRequest build() {
            return new AuthenticationRequest(login, password, client, clientIP, userAgent, parameters != null ? parameters : Collections.emptyMap(), properties != null ? properties : Collections.emptyMap());
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String login;
    private final String password;
    private final String client;
    private final String clientIP;
    private final String userAgent;
    private final Map<String, Object> parameters;
    private final Map<String, Object> properties;

    /**
     * Initializes a new {@link AuthenticationRequest}.
     *
     * @param login The user's login name
     * @param pass The user's password
     * @param client The client used to request authentication
     * @param clientIP The IP address this from where the authentication was requested
     * @param userAgent The user-agent header contained in the authentication request
     * @param parameters A map containing additional parameters for this request, e.g. the {@link LoginRequest}
     * @param properties A map containing HTTP headers and cookies
     */
    AuthenticationRequest(String login, String pass, String client, String clientIP, String userAgent, Map<String, Object> parameters, Map<String, Object> properties) {
        super();
        this.login = login;
        this.password = pass;
        this.client = client;
        this.clientIP = clientIP;
        this.userAgent = userAgent;
        this.parameters = parameters;
        this.properties = properties;
    }

    /**
     * Gets the login.
     *
     * @return The login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Gets the password.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the client identifier.
     *
     * @return The client identifier
     */
    public String getClient() {
        return client;
    }

    /**
     * Gets the client IP address.
     *
     * @return The client IP address
     */
    public String getClientIP() {
        return clientIP;
    }

    /**
     * Gets the user-agent.
     *
     * @return The user-agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Gets the containing additional parameters for this request, e.g. the {@link LoginRequest}.
     *
     * @return The parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets the map containing HTTP headers and cookies.
     *
     * @return The properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[login=");
        sb.append(login).append(",password=***,client=").append(client).append(",clientIP=").append(clientIP).append("userAgent=").append(userAgent);
        return sb.toString();
    }

}
