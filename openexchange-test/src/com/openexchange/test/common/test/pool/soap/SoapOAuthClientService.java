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

package com.openexchange.test.common.test.pool.soap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import com.openexchange.ajax.oauth.provider.IconBytes;
import com.openexchange.oauth.provider.rmi.client.RemoteClientManagement;
import com.openexchange.oauth.provider.soap.Client;
import com.openexchange.oauth.provider.soap.ClientData;
import com.openexchange.oauth.provider.soap.Credentials;
import com.openexchange.oauth.provider.soap.Icon;
import com.openexchange.oauth.provider.soap.OAuthClientServiceException;
import com.openexchange.oauth.provider.soap.OAuthClientServicePortType;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * {@link SoapOAuthClientService}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class SoapOAuthClientService {

    private static SoapOAuthClientService INSTANCE;

    private final OXOAuthClientService oxOAuthClientService;
    private final OAuthClientServicePortType oAuthClientsServicePortType;
    private final Credentials credentials;
    private final String host;

    /**
     * Gets the {@link SoapOAuthClientService}
     *
     * @return The {@link SoapOAuthClientService}
     * @throws MalformedURLException In case service can't be initialized
     */
    public static SoapOAuthClientService getInstance() throws MalformedURLException {
        if (INSTANCE == null) {
            synchronized (SoapOAuthClientService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SoapOAuthClientService();
                }
            }
        }
        return INSTANCE;
    }

    private SoapOAuthClientService() throws MalformedURLException {
        this.oxOAuthClientService = new OXOAuthClientService(new URL(SoapProvisioningService.getSOAPHostUrl(), "/webservices/OAuthClientService?wsdl"));
        this.oAuthClientsServicePortType = oxOAuthClientService.getOAuthClientService();
        TestUser oxAdminMaster = TestContextPool.getOxAdminMaster();
        this.credentials = createCreds(oxAdminMaster.getUser(), oxAdminMaster.getPassword());
        this.host = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
    }
    
    /**
     * Registers a new oauth client
     *
     * @param name The name of the client
     * @return The Client
     * @param registeredBy The class name that registers the resource
     * @throws OAuthClientServiceException
     */
    public Client registerClient(String name, String registeredBy) throws OAuthClientServiceException {
        SoapProvisioningService.setPodHeader(oAuthClientsServicePortType, registeredBy);
        return oAuthClientsServicePortType.registerClient(RemoteClientManagement.DEFAULT_GID, prepareClient(name), credentials);
    }

    /**
     * Registers a new oauth client
     *
     * @param client The client to register
     * @param registeredBy The class name that registers the resource
     * @return The returned Client
     * @throws OAuthClientServiceException
     */
    public Client registerClient(com.openexchange.test.common.test.pool.Client client, String registedBy) throws OAuthClientServiceException {
        SoapProvisioningService.setPodHeader(oAuthClientsServicePortType, registedBy);
        return oAuthClientsServicePortType.registerClient(RemoteClientManagement.DEFAULT_GID, convert(client), credentials);
    }

    /**
     * Updates an existing oauth client
     *
     * @param client The client to update
     * @param updatedBy The class name that updates the resource
     * @return The returned Client
     * @throws OAuthClientServiceException
     */
    public Client updateClient(com.openexchange.test.common.test.pool.Client client, String updatedBy) throws OAuthClientServiceException {
        SoapProvisioningService.setPodHeader(oAuthClientsServicePortType, updatedBy);
        return oAuthClientsServicePortType.updateClient(client.getId(), convert(client), credentials);
    }

    /**
     * Unregisters an existing oauth client
     *
     * @param id The id of the client
     * @param unregisteredBy The class name that unregisters the resource
     * @throws OAuthClientServiceException
     */
    public void unregisterClient(String id, String unregisteredBy) throws OAuthClientServiceException {
        SoapProvisioningService.setPodHeader(oAuthClientsServicePortType, unregisteredBy);
        oAuthClientsServicePortType.unregisterClient(id, credentials);
    }
    
    /**
     * Creates the client data for the given name
     *
     * @param name The client name
     * @return The {@link ClientData}
     */
    private ClientData prepareClient(String name) {
        Icon icon = new Icon();
        icon.setData(Base64.encodeBase64String(IconBytes.DATA));
        icon.setMimeType("image/jpg");

        ClientData clientData = new ClientData();
        List<String> redirectURIs = clientData.getRedirectURIs();
        redirectURIs.add("https://"+host);
        redirectURIs.add("https://"+host+":8080");

        clientData.setName(name);
        clientData.setDescription(name);
        clientData.setIcon(icon);
        clientData.setContactAddress("webmaster@example.com");
        clientData.setWebsite("http://www.example.com");
        clientData.setDefaultScope("read_contacts");
        return clientData;
    }

    /**
     * Converts the client to a soap client
     *
     * @param client The client
     * @return The {@link ClientData}
     */
    private ClientData convert(com.openexchange.test.common.test.pool.Client client) {
        ClientData clientData = new ClientData();
        List<String> redirectURIs = clientData.getRedirectURIs();
        if (client.getRedirectURIs() != null) {
            client.getRedirectURIs().forEach(uri -> redirectURIs.add(uri));
        }

        clientData.setName(client.getName());
        clientData.setDescription(client.getDescription());
        if (client.getIcon() != null) {
            Icon icon = new Icon();
            icon.setData(Base64.encodeBase64String(client.getIcon().getData()));
            icon.setMimeType(client.getIcon().getMimeType());
            clientData.setIcon(icon);
        }
        clientData.setContactAddress(client.getContactAddress());
        clientData.setWebsite(client.getWebsite());
        clientData.setDefaultScope(client.getDefaultScope());
        return clientData;
    }

    /**
     * Creates new credentials based on the given paramters
     *
     * @param login The login name to be used
     * @param password The password to be used
     * @return The credential obj
     */
    private Credentials createCreds(String login, String password) {
        Credentials creds = new Credentials();
        creds.setLogin(login);
        creds.setPassword(password);
        return creds;
    }
}
