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

package com.openexchange.oauth.provider.impl.client;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.google.common.collect.ImmutableSet;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.oauth.provider.authorizationserver.client.Client;
import com.openexchange.oauth.provider.authorizationserver.client.ClientData;
import com.openexchange.oauth.provider.authorizationserver.client.ClientManagement;
import com.openexchange.oauth.provider.authorizationserver.client.ClientManagementException;
import com.openexchange.oauth.provider.authorizationserver.client.ClientManagementException.Reason;
import com.openexchange.oauth.provider.authorizationserver.client.Icon;
import com.openexchange.oauth.provider.impl.ScopeProviderRegistry;
import com.openexchange.oauth.provider.impl.client.storage.OAuthClientStorage;
import com.openexchange.oauth.provider.impl.grant.OAuthGrantStorage;
import com.openexchange.oauth.provider.impl.tools.ClientId;
import com.openexchange.oauth.provider.impl.tools.OAuthClientIdHelper;
import com.openexchange.oauth.provider.tools.URIValidator;
import com.openexchange.session.restricted.Scope;


/**
 * {@link ClientManagementImpl}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ClientManagementImpl implements ClientManagement {

    private static final int MAX_ICON_SIZE = 0x40000;

    private static final Set<String> SUPPORTED_ICON_MIME_TYPES = ImmutableSet.of("image/png", "image/jpg", "image/jpeg");

    private final OAuthClientStorage clientStorage;

    private final OAuthGrantStorage grantStorage;

    /**
     * Initializes a new {@link ClientManagementImpl}.
     * @param clientStorage
     * @param grantStorage
     */
    public ClientManagementImpl(OAuthClientStorage clientStorage, OAuthGrantStorage grantStorage) {
        super();
        this.clientStorage = clientStorage;
        this.grantStorage = grantStorage;
    }

    @Override
    public List<Client> getClients(String contextGroup) throws ClientManagementException {
        return clientStorage.getClients(Strings.isEmpty(contextGroup) ? DEFAULT_GID : contextGroup);
    }

    @Override
    public Client getClientById(String clientId) throws ClientManagementException {
        ClientId clientIdObj = ClientId.parse(clientId);
        if (clientIdObj == null) {
            return null;
        }

        String groupId = clientIdObj.getGroupId();
        return clientStorage.getClientById(groupId, clientId);
    }

    @Override
    public Client registerClient(String contextGroup, ClientData clientData) throws ClientManagementException {
        assertNotNullOrEmpty("A context group ID must be set to which the client will be assigned. In setups without multiple context groups you can pass '" + DEFAULT_GID + "'.", contextGroup);
        assertNotNullOrEmpty("Property 'name' is mandatory and must be set", clientData.getName());
        assertNotNullOrEmpty("Property 'description' is mandatory and must be set", clientData.getDescription());
        assertNotNullOrEmpty("Property 'contact address' is mandatory and must be set", clientData.getContactAddress());
        assertNotNullOrEmpty("Property 'website' is mandatory and must be set", clientData.getWebsite());

        // check redirect URIs
        Set<String> redirectURIs = clientData.getRedirectURIs();
        assertNotNullOrEmpty("Redirect URIs are mandatory and must be set", redirectURIs);
        checkRedirectURIs(redirectURIs);

        // check icon
        Icon icon = clientData.getIcon();
        assertNotNullOrEmpty("Property 'icon' is mandatory and must be set", icon);
        checkIcon(icon);

        // check scope
        String scopeStr = clientData.getDefaultScope();
        assertNotNullOrEmpty("Property 'default scope' is mandatory and must be set", scopeStr);
        checkScope(scopeStr);

        String clientId = OAuthClientIdHelper.getInstance().generateClientId(contextGroup);
        String secret = UUIDs.getUnformattedString(UUID.randomUUID()) + UUIDs.getUnformattedString(UUID.randomUUID());
        return clientStorage.registerClient(contextGroup, clientId, secret, clientData);
    }

    @Override
    public Client updateClient(String clientId, ClientData clientData) throws ClientManagementException {
        ClientId clientIdObj = ClientId.parse(clientId);
        if (clientIdObj == null) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_ID, clientId);
        }

        if (clientData.containsName()) {
            assertNotNullOrEmpty("Property 'name' was set to an empty value", clientData.getName());
        }
        if (clientData.containsDescription()) {
            assertNotNullOrEmpty("Property 'description' was set to an empty value", clientData.getDescription());
        }
        if (clientData.containsContactAddress()) {
            assertNotNullOrEmpty("Property 'contact address' was set to an empty value", clientData.getContactAddress());
        }
        if (clientData.containsWebsite()) {
            assertNotNullOrEmpty("Property 'website' was set to an empty value", clientData.getWebsite());
        }
        if (clientData.containsRedirectURIs()) {
            Set<String> redirectURIs = clientData.getRedirectURIs();
            assertNotNullOrEmpty("At least one of the set redirect URIs is invalid", redirectURIs);
            checkRedirectURIs(redirectURIs);
        }
        if (clientData.containsIcon()) {
            Icon icon = clientData.getIcon();
            assertNotNullOrEmpty("Property 'icon' was set to an empty value", icon);
            checkIcon(icon);
        }
        if (clientData.containsDefaultScope()) {
            String scopeStr = clientData.getDefaultScope();
            assertNotNullOrEmpty("Property 'default scope' was set to an empty value", scopeStr);
            checkScope(scopeStr);
        }

        return clientStorage.updateClient(clientIdObj.getGroupId(), clientId, clientData);
    }

    @Override
    public boolean unregisterClient(String clientId) throws ClientManagementException {
        ClientId clientIdObj = ClientId.parse(clientId);
        if (clientIdObj == null) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_ID, clientId);
        }

        String groupId = clientIdObj.getGroupId();
        try {
            grantStorage.deleteGrantsByClientId(clientId);
            return clientStorage.unregisterClient(groupId, clientId);
        } catch (OXException e) {
            Boolean b = handleOXException(e);
            return b.booleanValue();
        }
    }

    @Override
    public Client revokeClientSecret(String clientId) throws ClientManagementException {
        ClientId clientIdObj = ClientId.parse(clientId);
        if (clientIdObj == null) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_ID, clientId);
        }

        String groupId = clientIdObj.getGroupId();
        try {
            grantStorage.deleteGrantsByClientId(clientId);
        } catch (OXException e) {
            return handleOXException(e);
        }

        String secret = UUIDs.getUnformattedString(UUID.randomUUID()) + UUIDs.getUnformattedString(UUID.randomUUID());
        return clientStorage.revokeClientSecret(groupId, clientId, secret);
    }

    @Override
    public boolean enableClient(String clientId) throws ClientManagementException {
        ClientId clientIdObj = ClientId.parse(clientId);
        if (clientIdObj == null) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_ID, clientId);
        }

        String groupId = clientIdObj.getGroupId();
        return clientStorage.enableClient(groupId, clientId);
    }

    @Override
    public boolean disableClient(String clientId) throws ClientManagementException {
        ClientId clientIdObj = ClientId.parse(clientId);
        if (clientIdObj == null) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_ID, clientId);
        }

        String groupId = clientIdObj.getGroupId();
        try {
            grantStorage.deleteGrantsByClientId(clientId);
        } catch (OXException e) {
            handleOXException(e);
        }

        return clientStorage.disableClient(groupId, clientId);
    }

    private static void assertNotNullOrEmpty(String message, Object obj) throws ClientManagementException {
        boolean assertionFailed = false;
        if (obj == null) {
            assertionFailed = true;
        } else if (obj instanceof String && ((String)obj).trim().isEmpty()) {
            assertionFailed = true;
        } else if (obj instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>)obj;
            if (collection.isEmpty()) {
                assertionFailed = true;
            } else {
                for (Object element : collection) {
                    if (element == null || element instanceof String && ((String)element).trim().isEmpty()) {
                        assertionFailed = true;
                        break;
                    }
                }
            }
        }

        if (assertionFailed) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_DATA, message);
        }
    }

    /**
     * Re-throws the OXException as ClientManagementException with reason 'INTERNAL_ERROR'.
     *
     * @param e The OXException
     * @return Nothing but you may type 'return handleOXException(e)' for your convenience
     * @throws ClientManagementException Will always be thrown
     */
    private <T> T handleOXException(OXException e) throws ClientManagementException {
        ClientManagementException cme = new ClientManagementException(e, Reason.INTERNAL_ERROR, e.getMessage());
        throw cme;
    }

    private void checkRedirectURIs(Set<String> redirectURIs) throws ClientManagementException {
        for (String uri : redirectURIs) {
            if (!URIValidator.isValidRedirectURI(uri)) {
                throw new ClientManagementException(Reason.INVALID_CLIENT_DATA, uri + " is not a valid redirect URI.");
            }
        }
    }

    private void checkIcon(Icon icon) throws ClientManagementException {
        String iconMimeType = icon.getMimeType();
        assertNotNullOrEmpty("The icons mime type is mandatory and must be set", iconMimeType);
        if (!SUPPORTED_ICON_MIME_TYPES.contains(iconMimeType)) {
            StringBuilder msg = new StringBuilder("Icon mime type ").append(iconMimeType).append(" is not supported, allowed types are [");
            Strings.join(SUPPORTED_ICON_MIME_TYPES, ", ", msg);
            msg.append("].");
            throw new ClientManagementException(Reason.INVALID_CLIENT_DATA, msg.toString());
        }

        assertNotNullOrEmpty("The icons data is mandatory and must be set", icon.getData());

        if (icon.getSize() > MAX_ICON_SIZE) {
            throw new ClientManagementException(Reason.INVALID_CLIENT_DATA, "Icon is too large, it must not exceed " + MAX_ICON_SIZE + " bytes.");
        }
    }

    private void checkScope(String scopeStr) throws ClientManagementException {
        if (Scope.isValidScopeString(scopeStr)) {
            Scope scope = Scope.parseScope(scopeStr);
            for (String token : scope.get()) {
                if (!ScopeProviderRegistry.getInstance().hasScopeProvider(token)) {
                    throw new ClientManagementException(Reason.INVALID_CLIENT_DATA, token + " is not a valid scope token.");
                }
            }
        } else {
            throw new ClientManagementException(Reason.INVALID_CLIENT_DATA, scopeStr + " is not a valid scope.");
        }
    }

}
