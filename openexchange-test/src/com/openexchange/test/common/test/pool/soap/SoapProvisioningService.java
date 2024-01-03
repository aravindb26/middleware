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

import static com.openexchange.java.Autoboxing.I;
import static java.util.Optional.empty;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import com.openexchange.admin.soap.context.dataobjects.Context;
import com.openexchange.admin.soap.context.dataobjects.Entry;
import com.openexchange.admin.soap.context.dataobjects.SOAPMapEntry;
import com.openexchange.admin.soap.context.dataobjects.SOAPStringMap;
import com.openexchange.admin.soap.context.dataobjects.SOAPStringMapMap;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.exception.OXException;
import com.openexchange.oauth.provider.soap.OAuthClientServiceException;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.test.common.test.pool.Icon;
import com.openexchange.test.common.test.pool.ProvisioningService;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccess;

/**
 * {@link SoapProvisioningService}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public class SoapProvisioningService implements ProvisioningService {

    private static SoapProvisioningService INSTANCE = new SoapProvisioningService();

    /**
     * Gets the {@link SoapProvisioningService}
     *
     * @return The {@link SoapProvisioningService}
     */
    public static SoapProvisioningService getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link SoapProvisioningService}.
     *
     * @throws MalformedURLException
     */
    private SoapProvisioningService() {
        super();
    }

    private SoapContextService getContextService() throws OXException {
        try {
            return SoapContextService.getInstance();
        } catch (MalformedURLException e) {
            throw new OXException(e);
        }
    }

    private SoapUserService getUserService() throws OXException {
        try {
            return SoapUserService.getInstance();
        } catch (MalformedURLException e) {
            throw new OXException(e);
        }
    }

    private SoapGroupService getGroupService() throws OXException {
        try {
            return SoapGroupService.getInstance();
        } catch (MalformedURLException e) {
            throw new OXException(e);
        }
    }

    private SoapResourceService getResourceService() throws OXException {
        try {
            return SoapResourceService.getInstance();
        } catch (MalformedURLException e) {
            throw new OXException(e);
        }
    }

    private SoapOAuthClientService getOAuthClientService() throws OXException {
        try {
            return SoapOAuthClientService.getInstance();
        } catch (MalformedURLException e) {
            throw new OXException(e);
        }
    }

    @Override
    public TestContext createContext(String createdBy) throws OXException {
        return createContext(TestContextConfig.EMPTY_CONFIG, createdBy);
    }

    @Override
    public TestContext createContext(TestContextConfig config, String createdBy) throws OXException {
        return getContextService().createTestContext(config, createdBy);
    }

    /**
     * Changes a context
     *
     * @param cid The context identifier
     * @param configs The configuration to pass for context
     * @throws OXException If context can't be created
     */
    public void changeContexConfig(int cid, Map<String, String> configs, String changedBy) throws OXException {
        Context ctx = new Context();
        ctx.setId(I(cid));

        SOAPStringMapMap soapStringMapMap = new SOAPStringMapMap();
        SOAPMapEntry soapMapEntry = new SOAPMapEntry();
        SOAPStringMap soapStringMap = new SOAPStringMap();
        ArrayList<Entry> entries = new ArrayList<>();

        configs.forEach((key, value) -> {
            Entry entry = new Entry();
            entry.setKey(key);
            entry.setValue(value);
            entries.add(entry);
        });

        soapStringMap.setEntries(entries);
        soapMapEntry.setValue(soapStringMap);
        soapMapEntry.setKey("config");

        soapStringMapMap.setEntries(Collections.singletonList(soapMapEntry));

        ctx.setUserAttributes(soapStringMapMap);

        getContextService().change(ctx, changedBy);
    }

    @Override
    public void deleteContext(int cid, String deletedBy) throws OXException {
        getContextService().delete(cid, deletedBy);
    }

    @Override
    public TestUser createUser(int cid, String createdBy) throws OXException {
        return createUser(cid, null, empty(), createdBy);
    }

    @Override
    public TestUser createUser(int cid, String userLogin, Optional<TestUserConfig> userConfig, String createdBy) throws OXException {
        return getUserService().createTestUser(cid, userConfig, userLogin, createdBy);
    }

    @Override
    public void changeUser(int cid, int userID, Optional<Map<String, String>> config, String changedBy) throws OXException {
        getUserService().changeUser(cid, userID, config, changedBy);
    }

    @Override
    public void deleteUser(int cid, int userID, String changedBy) throws OXException {
        getUserService().delete(cid, userID, changedBy);
    }

    @Override
    public UserModuleAccess getModuleAccess(int contextId, int userId) throws OXException {
        return getUserService().getModuleAccess(contextId, userId);
    }

    @Override
    public void changeCapability(int cid, int userID, Set<String> capsToAdd, Set<String> capsToRemove, Set<String> capsToDrop, String changedBy) throws OXException {
        getUserService().changeCapabilities(cid, userID, capsToAdd, capsToRemove, capsToDrop, changedBy);
    }

    @Override
    public void changeModuleAccess(int cid, int userID, UserModuleAccess userModuleAccess, String changedBy) throws OXException {
        getUserService().changeModuleAccess(cid, userID, userModuleAccess, changedBy);
    }

    @Override
    public Integer createGroup(int cid, Optional<List<Integer>> optUserIds, String createdBy) throws OXException {
        return getGroupService().create(cid, optUserIds, createdBy).getId();
    }

    @Override
    public Integer createResource(int cid, String createdBy) throws OXException {
        return getResourceService().create(cid, createdBy).getId();
    }

    @Override
    public TestUser getResource(int cid, int resrourceId) throws OXException {
        return getResourceService().get(cid, resrourceId);
    }

    /**
     * Gets the soap url
     *
     * @return The soap url
     * @throws MalformedURLException
     */
    public static URL getSOAPHostUrl() throws MalformedURLException {
        String soapProtocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
        String soapHost = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);

        String scheme = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
        int soapPort = scheme.equals("https") ? 443 : Integer.parseInt(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_PORT));
        return new URL(soapProtocol, soapHost, soapPort, "/");
    }

    @Override
    public void changeContext(int cid, TestContextConfig config, String changedBy) throws OXException {
        Optional<Map<String, String>> optConfig = config.optConfig();
        Optional<String> optTaxonomyType = config.optTaxonomyType();
        if (optConfig.isPresent() || optTaxonomyType.isPresent()) {
            getContextService().change(getContextService().createContext(ProvisioningUtils.DEFAULT_MAX_QUOTA, optConfig, optTaxonomyType), changedBy);
        }
        Optional<UserModuleAccess> optAccess = config.optAccess();
        if (optAccess.isPresent()) {
            getContextService().changeModuleAccess(cid, optAccess.get(), changedBy);
        }

        return;
    }

    @Override
    public Client registerOAuthClient(String name, String registeredBy) throws OXException {
        try {
            return convert(getOAuthClientService().registerClient(name, registeredBy), registeredBy);
        } catch (OAuthClientServiceException e) {
            throw new OXException(e);
        }
    }

    @Override
    public Client registerOAuthClient(Client client, String registeredBy) throws OXException {
        try {
            return convert(getOAuthClientService().registerClient(client, registeredBy), registeredBy);
        } catch (OAuthClientServiceException e) {
            throw new OXException(e);
        }
    }

    @Override
    public Client updateOAuthClient(Client client, String updatedBy) throws OXException {
        try {
            return convert(getOAuthClientService().updateClient(client, updatedBy), updatedBy);
        } catch (OAuthClientServiceException e) {
            throw new OXException(e);
        }
    }

    @Override
    public void unregisterOAuthClient(String clientId, String unregisteredBy) throws OXException {
        try {
            getOAuthClientService().unregisterClient(clientId, unregisteredBy);
        } catch (OAuthClientServiceException e) {
            throw new OXException(e);
        }
    }

    /**
     * Converts a soap client to a {@link Client}
     *
     * @param soap the soap client
     * @return the {@link Client}
     */
    private Client convert(com.openexchange.oauth.provider.soap.Client soap, String createdBy) {
        Client result = new Client();
        result.setContactAddress(soap.getContactAddress());
        result.setDefaultScope(soap.getDefaultScope());
        result.setDescription(soap.getDescription());
        result.setEnabled(soap.isEnabled());
        result.setIcon(convert(soap.getIcon()));
        result.setId(soap.getId());
        result.setName(soap.getName());
        result.setRedirectURIs(soap.getRedirectURIs());
        result.setRegistrationDate(soap.getRegistrationDate());
        result.setSecret(soap.getSecret());
        result.setWebsite(soap.getWebsite());
        result.setCreatedBy(createdBy);
        return result;
    }

    /**
     * Converts a soap icon to a {@link Icon}
     *
     * @param soap a soap icon
     * @return a {@link Icon}
     */
    private Icon convert(com.openexchange.oauth.provider.soap.Icon soap) {
        Icon result = new Icon();
        result.setData(Base64.decodeBase64(soap.getData().getBytes()));
        result.setMimeType(soap.getMimeType());
        return result;
    }

    /**
     * Adds the 'X-OX-HTTP-Test-Pod' header to soap client
     *
     * @param portType The port type
     * @param value The header value
     */
    protected static void setPodHeader(Object portType, String value) {
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put(ClientCommons.X_OX_HTTP_TEST_HEADER_NAME, Arrays.asList(value));
        ClientProxy.getClient(portType).getRequestContext().put(Message.PROTOCOL_HEADERS, headers);
    }

}
