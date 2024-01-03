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

package com.openexchange.ajax.jslob.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.ConfigBody;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.JSlobData;
import com.openexchange.testing.httpclient.models.JSlobsResponse;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link RegionalSettingsTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.3
 */
public class RegionalSettingsTest extends AbstractAPIClientSession {

    private static final String ID = "localeData";
    private static final String CORE = "io.ox/core";
    private JSlobApi jslobApi;
    private ConfigApi configApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        jslobApi = new JSlobApi(getApiClient());
        configApi = new ConfigApi(getApiClient());
    }

    @Test
    public void testCustomRegionalSettingsViaJSLob() throws ApiException {
        // Get jslob and check that user has no custom region format
        Map<String, Object> map = getCoreJSLob();
        assertFalse(map.containsKey(ID));

        // Adjust regional settings
        HashMap<String, Object> custom = new HashMap<>();
        custom.put("number", "1 234,56");
        CommonResponse response = jslobApi.setJSlob(Collections.singletonMap(ID, custom), CORE, null);
        assertNull(response.getError(), response.getErrorDesc());

        // Check again
        map = getCoreJSLob();
        assertTrue(map.containsKey(ID), "Couldn't find region information");
        if (map.get(ID) instanceof Map == false) {
            fail("Unknown format");
        }
        @SuppressWarnings("unchecked") Map<String, Object> region_settings = (Map<String, Object>) map.get(ID);
        assertTrue(region_settings.containsKey("number"));

        // Remove entry by setting region_format to a simple string (NULL doesn't work, because of the api client)
        response = jslobApi.setJSlob(Collections.singletonMap(ID, "NOT_NULL"), CORE, null);
        assertNull(response.getError(), response.getErrorDesc());

        // Get jslob and check that user has no custom region format
        map = getCoreJSLob();
        assertFalse(map.containsKey(ID));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCoreJSLob() throws ApiException {
        JSlobsResponse resp = jslobApi.getJSlobList(Collections.singletonList(CORE), null);
        assertNull(resp.getError(), resp.getErrorDesc());
        assertNotNull(resp.getData(), "");
        List<JSlobData> list = resp.getData();
        assertEquals(1, list.size());
        JSlobData jSlobData = list.get(0);
        if (jSlobData.getTree() instanceof Map) {
            return (Map<String, Object>) jSlobData.getTree();
        }
        fail("Unable to parse result");
        return null;
    }

    @Test
    public void testCustomRegionalSettingsViaConfigTree() throws ApiException, JSONException {
        // Remove settings first
        putRegionSettingToConfigTree(null);
        // Get config tree entry and check that user has no custom region format
        getSettingsFromConfigTree(false);
        // Adjust regional settings
        HashMap<String, Object> custom = new HashMap<>();
        custom.put("number", "1 234,56");
        putRegionSettingToConfigTree(custom);
        // Check again
        Map<String, Object> region_settings = getSettingsFromConfigTree(true);
        assertTrue(region_settings.containsKey("number"));
        // Remove entry again
        putRegionSettingToConfigTree(null);
        // Check again
        getSettingsFromConfigTree(false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSettingsFromConfigTree(boolean exists) throws ApiException {
        ConfigResponse resp = configApi.getConfigNode(ID);
        assertNull(resp.getError(), resp.getErrorDesc());
        String data = resp.getData().toString();
        if (exists == false) {
            assertTrue(JSONObject.NULL.toString().equals(data), "Unexpectedly found custom settings");
            return null;
        }
        assertFalse(JSONObject.NULL.toString().equals(data), "Missing custom settings");
        return (Map<String, Object>) resp.getData();
    }

    private void putRegionSettingToConfigTree(Map<String, Object> settings) throws ApiException, JSONException {
        if (settings == null) {
            // use simple string instead of null, because of bad test client support
            CommonResponse resp = configApi.putConfigNode(ID, new ConfigBody().data("DELETE"));
            assertNull(resp.getError(), resp.getErrorDesc());
            return;
        }
        JSONObject json = new JSONObject();
        for (Entry<String, Object> entry : settings.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }

        CommonResponse resp = configApi.putConfigNode(ID, new ConfigBody().data(json.toString()));
        assertNull(resp.getError(), resp.getErrorDesc());
    }

}
