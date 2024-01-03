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

package com.openexchange.ajax.framework;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import com.openexchange.ajax.framework.config.util.TestConfigurationChangeUtil;
import com.openexchange.ajax.oauth.provider.protocol.OAuthParams;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.Client;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * {@link AbstractConfigAwareAPIClientSession} extends the AbstractAPIClientSession with methods to preconfigure reloadable configurations before executing the tests.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.1
 */
public abstract class AbstractConfigAwareAPIClientSession extends AbstractAPIClientSession {

    /**
     * Initializes a new {@link AbstractConfigAwareAPIClientSession}.
     */
    protected AbstractConfigAwareAPIClientSession() {}

    Map<AJAXClient, JSONObject> oldData = new HashMap<>(3, 0.9f);

    /**
     * Changes the configurations given by {@link #getNeededConfigurations()}.
     *
     * @throws Exception if changing the configuration fails
     */
    protected void setUpConfiguration() throws Exception {
        setUpConfigWithOwnClient();
    }

    /**
     *
     * Retrieves all needed configurations.
     *
     * Should be overwritten by child implementations to define necessary configurations.
     *
     * @return Needed configurations.
     */
    protected Map<String, String> getNeededConfigurations() {
        return Collections.emptyMap();
    }

    /**
     * Retrieves the scope to use for the configurations.
     *
     * Can be overwritten by child implementations to change the scope of the configurations. Defaults to "user".
     * Values for the scope can be:
     * <li>user</li>
     * <li>context</li>
     * <li>server</li>
     *
     * @return The scope for the configuration.
     */
    protected String getScope() {
        return "user";
    }

    /**
     * Retrieves the the names of the reloadable classes which should be reloaded.
     *
     * Can be overwritten by child implementations. Defaults to null.
     *
     * @return A comma separated list of reloadable class names or null.
     */
    protected String getReloadables() {
        return null;
    }

    /**
     * Gets the default user id
     *
     * @return The user id
     */
    protected int getUserId() {
        return testUser.getUserId();
    }

    private void setUpConfigWithOwnClient() throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        if (getNeededConfigurations() != null && getNeededConfigurations().isEmpty() == false) {
            changeConfigWithOwnClient(testUser, getNeededConfigurations());
        }
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    protected void changeConfigWithOwnClient(TestUser user) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        changeConfigWithOwnClient(user, getNeededConfigurations());
    }

    /**
     * Changes the configuration of the given user
     *
     * @param user The user
     * @param config The new config
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    protected void changeConfigWithOwnClient(TestUser user, Map<String, String> config) throws ClientProtocolException, IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        TestConfigurationChangeUtil.changeConfigWithOwnClient(user, config, getScope(), getReloadables());
    }

    protected OAuthParams getOAuthParams(Client oAuthClientApp, String state){
        OAuthParams oAuthParams = new OAuthParams()
                .setScheme(AJAXConfig.getProperty(AJAXConfig.Property.OAUTH_PROTOCOL))
                .setHostname(AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME))
                .setPort(AJAXConfig.getProperty(AJAXConfig.Property.OAUTH_PROTOCOL).equals("https") ? 443 : 80)
                .setClientId(oAuthClientApp.getId())
                .setClientSecret(oAuthClientApp.getSecret())
                .setRedirectURI(oAuthClientApp.getRedirectURIs().get(0))
                .setScope("carddav caldav")
                .setState(state);

        return oAuthParams;
    }
}
