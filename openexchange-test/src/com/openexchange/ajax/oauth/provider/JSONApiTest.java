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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.oauth.provider.actions.AllRequest;
import com.openexchange.ajax.oauth.provider.actions.AllResponse;
import com.openexchange.ajax.oauth.provider.actions.RevokeRequest;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.oauth.provider.authorizationserver.grant.GrantView;
import com.openexchange.session.restricted.Scope;

/**
 * {@link JSONApiTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class JSONApiTest extends AbstractOAuthTest {

    /**
     * Initializes a new {@link JSONApiTest}.
     */

    @SuppressWarnings({ "unused" })
    @Test
    public void testAllAndRevoke() throws Exception {
        new OAuthClient(testUser, clientApp.getId(), clientApp.getSecret(), clientApp.getRedirectURIs().get(0), Scope.newInstance(RestrictedAction.Type.READ.getScope(AppointmentActionFactory.MODULE), RestrictedAction.Type.WRITE.getScope(AppointmentActionFactory.MODULE)));
        AllResponse allResponse = getClient().execute(new AllRequest());
        List<GrantView> grantViews = allResponse.getGrantViews();
        GrantView expected = null;
        for (GrantView grant : grantViews) {
            if (grant.getClient().getId().equals(clientApp.getId())) {
                expected = grant;
                break;
            }
        }
        Assertions.assertNotNull(expected);
        Assertions.assertEquals(3, expected.getScope().size());

        // revoke access for application
        getClient().execute(new RevokeRequest(expected.getClient().getId()));

        // assert it does not appear anymore in all response
        allResponse = getClient().execute(new AllRequest());
        grantViews = allResponse.getGrantViews();
        for (GrantView grant : grantViews) {
            Assertions.assertFalse(grant.getClient().getId().equals(clientApp.getId()));
        }
    }

}
