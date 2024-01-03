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

package com.openexchange.ajax.chronos.freebusy.visibility;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.simple.AbstractSimpleClientTest;

/**
 * {@link FreeBusyVisibilityConfigurationNullTest} - checks configuration edge cases for the "freeBusyVisibility" setting
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class FreeBusyVisibilityConfigurationNullTest extends AbstractSimpleClientTest {

    private static final String FREE_BUSY_VISIBILITY = "freeBusyVisibility";

    /**
     * This test does exist in a different class because the generated OpenAPI client
     * does not support to send <code>null</code> (it converts <code>null</code> to <code>{}</code>)
     * so there is the need to use a different framework.
     */
    @Test
    public void testResettingPropertyByProvidingNull_returnDefaultAfterReset() throws Exception {
        asUser(testUser);
        inModule("jslob");

        Map<String, Object> params = createParams("internal-only");
        JSONObject response = currentClient.execute("jslob", "set", params);
        Assertions.assertFalse(response.has("error"));

        Map<String, Object> chronosNull = createParams(null);
        response = currentClient.execute("jslob", "set", chronosNull);
        Assertions.assertFalse(response.has("error"));

        // Now try get the default
        response = currentClient.execute("jslob", "get", chronosNull);
        Assertions.assertFalse(response.has("error"));

        String currentFreeBusyVisibility = response.getJSONObject("data").getJSONObject("tree").getJSONObject("chronos").getString(FREE_BUSY_VISIBILITY);
        Assertions.assertEquals("all", currentFreeBusyVisibility);
    }

    private Map<String, Object> createParams(Object freeBusyVisibilityValue) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", "io.ox/calendar");

        Map<String, Object> freeBusy = new HashMap<String, Object>();
        freeBusy.put(FREE_BUSY_VISIBILITY, freeBusyVisibilityValue);
        Map<String, Object> chronos = new HashMap<String, Object>();
        chronos.put("chronos", freeBusy);

        params.put("body", chronos);
        return params;
    }
}
