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

package com.openexchange.cli;

import java.io.InputStream;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONObject;
import org.json.JSONServices;

/**
 * {@link RestAuthenticatorClient}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class RestAuthenticatorClient {

    private static final String PARAM_ENDPOINT_HOST = "api-host";
    private static final String ENDPOINT_HOST_DEFAULT = "http://localhost:8009";

    private final String[] args;

    private Boolean masterAuthenticationDisabled;
    private Boolean contextAuthenticationDisabled;
    private Boolean masterAccountOverride;

    /**
     * Initializes a new {@link RestAuthenticatorClient}.
     *
     * @param args The command arguments
     */
    public RestAuthenticatorClient(String[] args) {
        super();
        this.args = args;
    }

    /**
     * Gets the end-point host to use for accessing the REST API
     *
     * @return The end-point extracted from the given commandLine, or the default end-point
     * @throws ParseException If end-point host cannot be parsed
     */
    private String getEndpointHost() throws ParseException {
        Options options = new ReservedOptions();
        options.addOption(null, PARAM_ENDPOINT_HOST, true, "URL for an alternative REST end-point host. Example: 'https://192.168.0.1:8443'. Default: '" + ENDPOINT_HOST_DEFAULT + "'");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args, true);

        String endPointRoot = commandLine.getOptionValue(PARAM_ENDPOINT_HOST, ENDPOINT_HOST_DEFAULT);
        return endPointRoot.endsWith("/") ? endPointRoot.substring(0, endPointRoot.length() - 1) : endPointRoot;
    }

    /**
     * Authenticates the master administrator
     *
     * @param login The login for master administrator
     * @param password The password for master administrator
     * @throws Exception If credentials are invalid
     */
    public void doAuthentication(String login, String password) throws Exception {
        URI uri = new URI(getEndpointHost() + "/authenticator/v1/authenticate");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget baseTarget = client.target(uri);
        Builder executionContext = baseTarget.request();
        executionContext.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);

        JSONObject requestBody = new JSONObject(4);
        requestBody.put("login", login);
        requestBody.put("password", password);

        InputStream response = null;
        try {
            response = executionContext.put(Entity.json(requestBody.toString()), InputStream.class);
            requestBody = null;

            JSONObject json = JSONServices.parseObject(response);
            boolean authenticated = json.optBoolean("result", false);
            if (authenticated == false) {
                throw new Exception("Invalid credentials");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Authenticates the context administrator
     *
     * @param login The login for context administrator
     * @param password The password for context administrator
     * @param contextId The context identifier
     * @throws Exception If credentials are invalid
     */
    public void doAuthentication(String login, String password, int contextId) throws Exception {
        URI uri = new URI(getEndpointHost() + "/authenticator/v1/authenticate");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget baseTarget = client.target(uri);
        Builder executionContext = baseTarget.request();
        executionContext.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);

        JSONObject requestBody = new JSONObject(4);
        requestBody.put("login", login);
        requestBody.put("password", password);
        requestBody.put("context", contextId);

        InputStream response = null;
        try {
            response = executionContext.put(Entity.json(requestBody.toString()), InputStream.class);
            requestBody = null;

            JSONObject json = JSONServices.parseObject(response);
            boolean authenticated = json.optBoolean("result", false);
            if (authenticated == false) {
                throw new Exception("Invalid credentials");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Checks if master authentication is enabled for associated machine.
     *
     * @return <code>true</code> if master authentication is enabled; otherwise <code>false</code>
     * @throws Exception If operation fails
     */
    public boolean isMasterAuthenticationEnabled() throws Exception {
        return isMasterAuthenticationDisabled() == false;
    }

    /**
     * Checks if master authentication has been disabled for associated machine.
     *
     * @return <code>true</code> if master authentication has been disabled; otherwise <code>false</code>
     * @throws Exception If operation fails
     */
    public boolean isMasterAuthenticationDisabled() throws Exception {
        Boolean masterAuthenticationDisabled = this.masterAuthenticationDisabled;
        if (masterAuthenticationDisabled == null) {
            masterAuthenticationDisabled = Boolean.valueOf(checkForMasterAuthenticationDisabled());
            this.masterAuthenticationDisabled = masterAuthenticationDisabled;
        }
        return masterAuthenticationDisabled.booleanValue();
    }

    private boolean checkForMasterAuthenticationDisabled() throws Exception {
        URI uri = new URI(getEndpointHost() + "/authenticator/v1/checkmaster");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget baseTarget = client.target(uri);
        Builder executionContext = baseTarget.request();
        executionContext.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);
        Response response = executionContext.get();
        String data = response.readEntity(String.class);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return "true".equalsIgnoreCase(data);
        }

        System.err.println("Failed to retrieve if master-authentication is disabled: " + response.getStatusInfo() + ". Assmuning not disabled.");
        return false;
    }

    /**
     * Checks if context authentication is enabled for associated machine.
     *
     * @return <code>true</code> if context authentication is enabled; otherwise <code>false</code>
     * @throws Exception If operation fails
     */
    public boolean isContextAuthenticationEnabled() throws Exception {
        return isContextAuthenticationDisabled() == false;
    }

    /**
     * Checks if context authentication has been disabled for associated machine.
     *
     * @return <code>true</code> if context authentication has been disabled; otherwise <code>false</code>
     * @throws Exception If operation fails
     */
    public boolean isContextAuthenticationDisabled() throws Exception {
        Boolean contextAuthenticationDisabled = this.contextAuthenticationDisabled;
        if (contextAuthenticationDisabled == null) {
            contextAuthenticationDisabled = Boolean.valueOf(checkForContextAuthenticationDisabled());
            this.contextAuthenticationDisabled = contextAuthenticationDisabled;
        }
        return contextAuthenticationDisabled.booleanValue();
    }

    private boolean checkForContextAuthenticationDisabled() throws Exception {
        URI uri = new URI(getEndpointHost() + "/authenticator/v1/checkcontext");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget baseTarget = client.target(uri);
        Builder executionContext = baseTarget.request();
        executionContext.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);
        Response response = executionContext.get();
        String data = response.readEntity(String.class);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return "true".equalsIgnoreCase(data);
        }

        System.err.println("Failed to retrieve if context-authentication is disabled: " + response.getStatusInfo() + ". Assmuning not disabled.");
        return false;
    }

    /**
     * Checks if context authentication has been disabled for associated machine.
     *
     * @return <code>true</code> if context authentication has been disabled; otherwise <code>false</code>
     * @throws Exception If operation fails
     */
    public boolean isMasterAccountOverride() throws Exception {
        Boolean masterAccountOverride = this.masterAccountOverride;
        if (masterAccountOverride == null) {
            masterAccountOverride = Boolean.valueOf(checkForMasterAccountOverride());
            this.masterAccountOverride = masterAccountOverride;
        }
        return masterAccountOverride.booleanValue();
    }

    private boolean checkForMasterAccountOverride() throws Exception {
        URI uri = new URI(getEndpointHost() + "/authenticator/v1/checkoverride");
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);
        WebTarget baseTarget = client.target(uri);
        Builder executionContext = baseTarget.request();
        executionContext.accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);
        Response response = executionContext.get();
        String data = response.readEntity(String.class);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return "true".equalsIgnoreCase(data);
        }

        System.err.println("Failed to retrieve if master-override is enabled: " + response.getStatusInfo() + ". Assmuning not enabled.");
        return false;
    }

}
