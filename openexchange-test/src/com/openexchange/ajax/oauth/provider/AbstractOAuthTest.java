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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.ajax.framework.AbstractSmtpAJAXSession;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.chronos.json.oauth.ChronosOAuthScope;
import com.openexchange.contacts.json.ContactActionFactory;
import com.openexchange.oauth.provider.rmi.client.ClientDataDto;
import com.openexchange.oauth.provider.rmi.client.IconDto;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tasks.json.TaskActionFactory;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractOAuthTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public abstract class AbstractOAuthTest extends AbstractSmtpAJAXSession {

    protected Client clientApp;

    protected OAuthClient oAuthClient;

    protected Scope scope;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        // register client application
        clientApp = registerTestClient(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName());
        if (scope == null) {
            scope = Scope.parseScope(clientApp.getDefaultScope());
        }
        oAuthClient = new OAuthClient(testUser, clientApp.getId(), clientApp.getSecret(), clientApp.getRedirectURIs().get(0), scope);
    }

    public void setOAuthClient(Scope scope){
        oAuthClient = new OAuthClient(testUser, clientApp.getId(), clientApp.getSecret(), clientApp.getRedirectURIs().get(0), scope);
    }

    @AfterEach
    public void tearDown() throws Exception {
        OAuthClient oAuthClient = this.oAuthClient;
        if (null != oAuthClient) {
            oAuthClient.logout();
        }
        unregisterTestClient(clientApp);
    }

    public static Client registerTestClient(String registeredBy) throws Exception {
        return ConfigAwareProvisioningService.getService().registerOAuthClient("Test App " + UUID.randomUUID().toString(), registeredBy);
    }

    public static void unregisterTestClient(Client oAuthClientApp) throws Exception {
        ConfigAwareProvisioningService.getService().unregisterOAuthClient(oAuthClientApp.getId(), oAuthClientApp.getCreatedBy());
    }

    public static ClientDataDto prepareClient(String name) {
        IconDto icon = new IconDto();
        icon.setData(IconBytes.DATA);
        icon.setMimeType("image/jpg");

        List<String> redirectURIs = new ArrayList<>(2);
        redirectURIs.add("http://localhost");
        redirectURIs.add("http://localhost:8080");

        ClientDataDto clientData = new ClientDataDto();
        clientData.setName(name);
        clientData.setDescription(name);
        clientData.setIcon(icon);
        clientData.setContactAddress("webmaster@example.com");
        clientData.setWebsite("http://www.example.com");
        clientData.setDefaultScope(Scope.newInstance(RestrictedAction.Type.READ.getScope(ContactActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(ContactActionFactory.MODULE), RestrictedAction.Type.READ.getScope(AppointmentActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(AppointmentActionFactory.MODULE), RestrictedAction.Type.READ.getScope(ChronosOAuthScope.MODULE), RestrictedAction.Type.WRITE.getScope(ChronosOAuthScope.MODULE), RestrictedAction.Type.READ.getScope(TaskActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(TaskActionFactory.MODULE)).toString());
        clientData.setRedirectURIs(redirectURIs);
        return clientData;
    }

    public static Credentials getMasterAdminCredentials() {
        TestUser oxAdminMaster = TestContextPool.getOxAdminMaster();
        return new Credentials(oxAdminMaster.getUser(), oxAdminMaster.getPassword());
    }

}
