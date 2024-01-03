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

package com.openexchange.ajax.oauth.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AbstractTestEnvironment;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.chronos.json.oauth.ChronosOAuthScope;
import com.openexchange.contacts.json.ContactActionFactory;
import com.openexchange.java.Strings;
import com.openexchange.oauth.provider.authorizationserver.client.ClientManagementException.Reason;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tasks.json.TaskActionFactory;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.Icon;

/**
 * {@link ClientManagementTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ClientManagementTest extends AbstractTestEnvironment {

    @Test
    public void testUpdatePermutations(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        Client clientData = prepareClient(registeredBy + "_" + System.currentTimeMillis());
        Client client = ConfigAwareProvisioningService.getService().registerOAuthClient(clientData, registeredBy);
        compare(clientData, client);

        // name
        clientData = new Client();
        clientData.setId(client.getId());
        clientData.setName(Strings.reverse(client.getName()));
        assertNotNull(clientData.getName());
        client = ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy);
        assertEquals(clientData.getName(), client.getName());

        // description
        clientData = new Client();
        clientData.setId(client.getId());
        clientData.setDescription(Strings.reverse(client.getDescription()));
        assertNotNull(clientData.getDescription());
        client = ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy);
        assertEquals(clientData.getDescription(), client.getDescription());

        // website
        clientData = new Client();
        clientData.setId(client.getId());
        clientData.setWebsite(Strings.reverse(client.getWebsite()));
        assertNotNull(clientData.getWebsite());
        client = ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy);
        assertEquals(clientData.getWebsite(), client.getWebsite());

        // contact address
        clientData = new Client();
        clientData.setId(client.getId());
        clientData.setContactAddress(Strings.reverse(client.getContactAddress()));
        assertNotNull(clientData.getContactAddress());
        client = ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy);
        assertEquals(clientData.getContactAddress(), client.getContactAddress());

        // default scope
        clientData = new Client();
        clientData.setId(client.getId());
        clientData.setDefaultScope(RestrictedAction.Type.READ.getScope(TaskActionFactory.MODULE) + " " + RestrictedAction.Type.WRITE.getScope(TaskActionFactory.MODULE));
        assertNotNull(clientData.getDefaultScope());
        client = ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy);
        assertEquals(clientData.getDefaultScope(), client.getDefaultScope());

        // redirect URIs
        clientData = new Client();
        clientData.setId(client.getId());
        clientData.setRedirectURIs(Collections.singletonList("http://[::1]/some/where"));
        assertTrue(clientData.getRedirectURIs() != null && clientData.getRedirectURIs().isEmpty() == false);
        client = ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy);
        assertEquals(clientData.getRedirectURIs(), client.getRedirectURIs());
    }

    @Test
    public void testInvalidRedirectURIOnRegister() throws Exception {
        String invalidURI = "http://oauth.example.com/api/callback"; // HTTPS must be enforced for non-localhost URIs
        Client clientData = prepareClient(ClientManagementTest.class.getSimpleName() + "_" + System.currentTimeMillis());
        clientData.setRedirectURIs(Collections.singletonList(invalidURI));
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().registerOAuthClient(clientData, ClientManagementTest.class.getCanonicalName()));
    }

    @Test
    public void testInvalidRedirectURIOnUpdate(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        Client client = ConfigAwareProvisioningService.getService().registerOAuthClient(prepareClient(registeredBy + "_" + System.currentTimeMillis()), registeredBy);
        String invalidURI = "http://oauth.example.com/api/callback"; // HTTPS must be enforced for non-localhost URIs
        Client clientData = new Client();
        clientData.setRedirectURIs(Collections.singletonList(invalidURI));
        clientData.setId(client.getId());
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy));
    }

    @Test
    public void testInvalidIconMimeTypeOnRegister(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        String invalidMimeType = "image/gif"; // Only png and jpg are allowed
        Client clientData = prepareClient(ClientManagementTest.class.getSimpleName() + "_" + System.currentTimeMillis());
        Icon icon = new Icon();
        icon.setData(IconBytes.DATA);
        icon.setMimeType(invalidMimeType);
        clientData.setIcon(icon);
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().registerOAuthClient(clientData, registeredBy));
    }

    @Test
    public void testInvalidIconMimeTypeOnUpdate(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        Client client = ConfigAwareProvisioningService.getService().registerOAuthClient(prepareClient(registeredBy + "_" + System.currentTimeMillis()), registeredBy);
        String invalidMimeType = "image/gif"; // Only png and jpg are allowed
        Client clientData = new Client();
        Icon icon = new Icon();
        icon.setData(IconBytes.DATA);
        icon.setMimeType(invalidMimeType);
        clientData.setIcon(icon);
        clientData.setId(client.getId());
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy));
    }

    @Test
    public void testIconTooLargeOnRegister(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        int maxSize = 0x40000; // Max 256kb
        Client clientData = prepareClient(ClientManagementTest.class.getSimpleName() + "_" + System.currentTimeMillis());
        Icon icon = new Icon();
        byte[] data = new byte[maxSize + 1];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 255);
        }
        icon.setData(data);
        icon.setMimeType("image/png");
        clientData.setIcon(icon);
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().registerOAuthClient(clientData, registeredBy));
    }

    @Test
    public void testIconTooLargeOnUpdate(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        Client client = ConfigAwareProvisioningService.getService().registerOAuthClient(prepareClient(registeredBy + "_" + System.currentTimeMillis()), registeredBy);
        int maxSize = 0x40000; // Max 256kb
        Client clientData = new Client();
        clientData.setId(client.getId());
        Icon icon = new Icon();
        byte[] data = new byte[maxSize + 1];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 255);
        }
        icon.setData(data);
        icon.setMimeType("image/png");
        clientData.setIcon(icon);
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy));
    }

    @Test
    public void testInvalidScopeOnRegister(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "." + testInfo.getDisplayName();
        String invalidScope = "doSomething";
        Client clientData = prepareClient(registeredBy + "_" + System.currentTimeMillis());
        clientData.setDefaultScope(invalidScope);
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().registerOAuthClient(clientData, registeredBy));
    }

    @Test
    public void testInvalidScopeOnUpdate(TestInfo testInfo) throws Exception {
        final String registeredBy = ClientManagementTest.class.getCanonicalName() + "_" + testInfo.getDisplayName();
        Client client = ConfigAwareProvisioningService.getService().registerOAuthClient(prepareClient(registeredBy + "_" + System.currentTimeMillis()), registeredBy);
        String invalidScope = "doSomething";
        Client clientData = new Client();
        clientData.setId(client.getId());
        clientData.setDefaultScope(invalidScope);
        assertThrows( WebServiceException.class, () -> ConfigAwareProvisioningService.getService().updateOAuthClient(clientData, registeredBy));
    }

    private static void compare(Client clientData, Client client) {
        assertNotNull(client.getId());
        assertEquals(clientData.getName(), client.getName());
        assertEquals(clientData.getContactAddress(), client.getContactAddress());
        assertEquals(clientData.getDescription(), client.getDescription());
        assertEquals(clientData.getWebsite(), client.getWebsite());
        assertEquals(clientData.getDefaultScope(), client.getDefaultScope().toString());
        assertEquals(new HashSet<>(clientData.getRedirectURIs()), new HashSet<>(client.getRedirectURIs()));
        assertArrayEquals(clientData.getIcon().getData(), client.getIcon().getData());
        assertTrue(client.getRegistrationDate() > 0);
        assertNotNull(client.getSecret());
        assertTrue(client.isEnabled());
    }

    private static final class CMEMatcher extends TypeSafeMatcher<Exception> {

        private final String invalidValue;

        CMEMatcher(String invalidValue) {
            super();
            this.invalidValue = invalidValue;
        }

        @Override
        protected boolean matchesSafely(Exception e) {
            String message = e.getMessage();
            return message.contains("Invalid client data") && message.contains(invalidValue);
        }

        @Override
        public void describeTo(Description d) {
            d.appendText(new com.openexchange.oauth.provider.authorizationserver.client.ClientManagementException(Reason.INVALID_CLIENT_DATA, invalidValue).getMessage());
        }

    }

    public static Client prepareClient(String name) {
        Icon icon = new Icon();
        icon.setData(IconBytes.DATA);
        icon.setMimeType("image/jpg");

        List<String> redirectURIs = new ArrayList<>(2);
        redirectURIs.add("http://localhost");
        redirectURIs.add("http://localhost:8080");

        Client clientData = new Client();
        clientData.setName(name);
        clientData.setDescription(name);
        clientData.setIcon(icon);
        clientData.setContactAddress("webmaster@example.com");
        clientData.setWebsite("http://www.example.com");
        clientData.setDefaultScope(Scope.newInstance(RestrictedAction.Type.READ.getScope(ContactActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(ContactActionFactory.MODULE), RestrictedAction.Type.READ.getScope(AppointmentActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(AppointmentActionFactory.MODULE), RestrictedAction.Type.READ.getScope(ChronosOAuthScope.MODULE), RestrictedAction.Type.WRITE.getScope(ChronosOAuthScope.MODULE),RestrictedAction.Type.READ.getScope(TaskActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(TaskActionFactory.MODULE)).toString());
        clientData.setRedirectURIs(redirectURIs);
        return clientData;
    }

}
