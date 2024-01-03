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

package com.openexchange.ajax.onboarding.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractSmtpAJAXSession;
import com.openexchange.ajax.onboarding.actions.ExecuteRequest;
import com.openexchange.ajax.onboarding.actions.OnboardingTestResponse;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * {@link EASSyncProfileTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.1
 */
@Execution(ExecutionMode.SAME_THREAD)
public class EASSyncProfileTest extends AbstractSmtpAJAXSession {

    @Test
    public void testEASSyncProfileViaEmail() throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", getClient().getValues().getDefaultAddress());
        ExecuteRequest req = new ExecuteRequest("apple.iphone/eassync", "email", body, false);
        OnboardingTestResponse resp = getClient().execute(req);
        assertFalse(resp.hasError());

        assertEquals(1, mailManager.getMailCount());
    }

    @Test
    public void testEASSyncProfileViaDisplay() throws Exception {
        ExecuteRequest req = new ExecuteRequest("apple.iphone/easmanual", "display", null, false);
        OnboardingTestResponse resp = getClient().execute(req);
        assertFalse(resp.hasError());
        JSONObject json = (JSONObject) resp.getData();
        assertTrue(json.hasAndNotNull("eas_url"));
        assertTrue(json.hasAndNotNull("eas_login"));
    }

}
