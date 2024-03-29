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

package com.openexchange.ajax.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.GetResponse;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.session.actions.LoginRequest;
import com.openexchange.ajax.session.actions.LoginResponse;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug40821Test}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.0
 */
public class Bug40821Test extends AbstractAJAXSession {

    private String login;
    private String password;
    private AJAXClient clientToLogin;
    private String language;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        login = testUser.getLogin();
        password = testUser.getPassword();
        clientToLogin = new AJAXClient(new AJAXSession(this.getClass().getCanonicalName() + "." + testInfo.getDisplayName()), true);

        GetRequest getRequest = new GetRequest(Tree.Language);
        GetResponse getResponse = getClient().execute(getRequest);
        language = getResponse.getString();
    }

    @Test
    public void testLoginWithStoreLanguage() throws Exception {
        String languageToSet = "de_DE".equalsIgnoreCase(language) ? "en_US" : "de_DE";
        LoginRequest req = new LoginRequest(login, password, LoginTools.generateAuthId(), AJAXClient.class.getName(), AJAXClient.VERSION, languageToSet, true, false);
        LoginResponse resp = clientToLogin.execute(req);
        clientToLogin.logout();
        JSONObject json = (JSONObject) resp.getData();
        String locale = json.getString("locale");
        assertEquals(languageToSet, locale, "Language was not stored.");
    }

    @Test
    public void testLoginWithoutStoreLanguage() throws Exception {
        String languageToSet = "de_DE".equalsIgnoreCase(language) ? "en_US" : "de_DE";
        LoginRequest req = new LoginRequest(login, password, LoginTools.generateAuthId(), AJAXClient.class.getName(), AJAXClient.VERSION, languageToSet, false, false);
        LoginResponse resp = clientToLogin.execute(req);
        clientToLogin.logout();
        JSONObject json = (JSONObject) resp.getData();
        String locale = json.getString("locale");
        assertEquals(language, locale, "Language was stored.");
    }

}
