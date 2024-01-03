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

package com.openexchange.rest.advertisement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import com.openexchange.advertisement.json.AdConfigRestService;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractConfigAwareAjaxSession;
import com.openexchange.rest.advertisement.actions.GetConfigRequest;
import com.openexchange.rest.advertisement.actions.GetConfigResponse;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.restclient.invoker.ApiResponse;
import com.openexchange.testing.restclient.modules.AdvertisementApi;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link AdvertisementTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.3
 */
@Execution(SAME_THREAD)
public class AdvertisementTest extends AbstractConfigAwareAjaxSession {

    private static final String TAXONOMY_TYPES = "TaxonomyTypes";
    private static final String ACCESS_COMBINATIONS = "AccessCombinations";
    private static final String GLOBAL = "Global";
    private static final String MODULE_ACCESS_DEFINITION = "groupware_premium";
    private static final String reloadables = "AdvertisementPackageServiceImpl, TaxonomyTypesAdvertisementConfigService";
    private static final String DEFAULT = "default";

    protected AdvertisementApi advertisementApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        advertisementApi = new AdvertisementApi(getRestClient());
        random = new Random();
    }

    @Override
    public TestClassConfig getTestConfig() {
        // Set webmail access
        UserModuleAccess access = new UserModuleAccess();
        access.enableAll();

        TestContextConfig ctxConfig = TestContextConfig.builder().withUserModuleAccess(access).withTaxonomyType(MODULE_ACCESS_DEFINITION).build();
        return TestClassConfig.builder().withContextConfig(ctxConfig).build();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(AdConfigRestService.class);
    }

    public String packageScheme;
    private static Random random;

    public static Iterable<? extends Object[]> params() {
        ArrayList<String[]> result = new ArrayList<>();
        result.add(new String[] { GLOBAL });
        result.add(new String[] { ACCESS_COMBINATIONS });
        result.add(new String[] { TAXONOMY_TYPES });
        return result;
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        if (packageScheme == null) {
            fail("No package scheme defined!");
        }
        Map<String, String> result = new HashMap<>();
        result.put("com.openexchange.advertisement.default.packageScheme", packageScheme);
        if (packageScheme == TAXONOMY_TYPES) {
            result.put("com.openexchange.advertisement.default.taxonomy.types", MODULE_ACCESS_DEFINITION);
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("params")
    public void configByUserIdTest(String param) throws Exception {
        Long contextId = Long.valueOf(getAjaxClient().getValues().getContextId());
        Long userId = Long.valueOf(getAjaxClient().getValues().getUserId());

        packageScheme = param;

        setUpConfiguration();

        JSONValue adConfig = generateAdConfig();
        try {
            ApiResponse<Void> resp = advertisementApi.putAdvertisementByUserIdWithHttpInfo(contextId, userId, adConfig.toString());
            assertTrue(Arrays.contains(new int[] { 200, 201 }, resp.getStatusCode()), "Unexpected status: " + resp.getStatusCode());

            GetConfigRequest req = new GetConfigRequest();
            GetConfigResponse response = getAjaxClient().execute(req);
            assertTrue(!response.hasError(), "Response has errors: " + getErrorMessage(response));
            assertEquals(adConfig, response.getData(), "The server returned the wrong configuration.");

            AJAXClient client2 = testUser2.getAjaxClient();
            GetConfigResponse response2 = client2.execute(req);
            assertTrue(response2.hasError(), "Expecting a response with an error.");
        } catch (Exception e) {
            LoggerFactory.getLogger(AdvertisementTest.class).error("Test failed with error: {}", e.getMessage(), e);
        } finally {
            ApiResponse<Void> resp = advertisementApi.deleteAdvertisementByUserIdWithHttpInfo(contextId, userId);
            assertEquals(204, resp.getStatusCode(), "Deletion of ad config failed: " + resp.getStatusCode());
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    public void configByResellerAndPackageTest(String param) throws Exception {

        packageScheme = param;

        setUpConfiguration();

        String pack = null;
        switch (packageScheme) {
            case GLOBAL:
                pack = DEFAULT;
                break;
            case ACCESS_COMBINATIONS:
            case TAXONOMY_TYPES:
                pack = MODULE_ACCESS_DEFINITION;
                break;
        }

        JSONValue adConfig = generateAdConfig();
        try {
            ApiResponse<Void> resp = advertisementApi.putAdvertisementByResellerAndPackageWithHttpInfo(DEFAULT, pack, adConfig.toString());
            assertTrue(Arrays.contains(new int[] { 200, 201 }, resp.getStatusCode()), "Unexpected status: " + resp.getStatusCode());

            GetConfigRequest req = new GetConfigRequest();
            GetConfigResponse response = getAjaxClient().execute(req);
            assertFalse(response.hasError(), "Response has errors: " + getErrorMessage(response));
            assertEquals(adConfig, response.getData(), "The server returned the wrong configuration.");
        } catch (Exception e) {
            LoggerFactory.getLogger(AdvertisementTest.class).error("Test failed with error: {}", e.getMessage(), e);
        } finally {
            ApiResponse<Void> resp = advertisementApi.deleteAdvertisementByResellerAndPackageWithHttpInfo(DEFAULT, pack);
            assertEquals(204, resp.getStatusCode(), "Deletion of ad config failed: " + resp.getStatusCode());
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    public void removeConfigTest(String params) throws Exception {

        packageScheme = params;

        setUpConfiguration();

        String pack = null;
        switch (packageScheme) {
            case GLOBAL:
                pack = DEFAULT;
                break;
            case ACCESS_COMBINATIONS:
            case TAXONOMY_TYPES:
                pack = MODULE_ACCESS_DEFINITION;
                break;
        }

        JSONValue adConfig = generateAdConfig();

        // create configuration
        ApiResponse<Void> resp = advertisementApi.putAdvertisementByResellerAndPackageWithHttpInfo(DEFAULT, pack, adConfig.toString());
        assertTrue(Arrays.contains(new int[] { 200, 201 }, resp.getStatusCode()), "Unexpected status: " + resp.getStatusCode());
        // Check if configuration is available
        GetConfigRequest req = new GetConfigRequest();
        GetConfigResponse response = getAjaxClient().execute(req);
        assertTrue(!response.hasError(), "Response has errors: " + getErrorMessage(response));
        assertEquals(adConfig, response.getData(), "The server returned the wrong configuration.");
        // Remove configuration again
        resp = advertisementApi.deleteAdvertisementByResellerAndPackageWithHttpInfo(DEFAULT, pack);

        assertEquals(204, resp.getStatusCode(), "Deletion of ad config failed: " + resp.getStatusCode());
        // Check if configuration is gone
        GetConfigResponse response2 = getAjaxClient().execute(req);
        assertTrue(response2.hasError(), "Expecting a response with an error.");
    }

    @ParameterizedTest
    @MethodSource("params")
    public void removePreviewTest(String params) throws Exception {

        packageScheme = params;

        setUpConfiguration();
        String pack = null;
        switch (packageScheme) {
            case GLOBAL:
                pack = DEFAULT;
                break;
            case ACCESS_COMBINATIONS:
            case TAXONOMY_TYPES:
                pack = MODULE_ACCESS_DEFINITION;
                break;
        }

        Long ctxId = Long.valueOf(getAjaxClient().getValues().getContextId());
        Long userId = Long.valueOf(getAjaxClient().getValues().getUserId());
        JSONValue adConfig = generateAdConfig();
        try {
            ApiResponse<Void> resp = advertisementApi.putAdvertisementByResellerAndPackageWithHttpInfo(DEFAULT, pack, adConfig.toString());
            int statusLine = resp.getStatusCode();
            assertTrue(Arrays.contains(new int[] { 200, 201 }, statusLine), "Unexpected status: " + statusLine);
            GetConfigRequest req = new GetConfigRequest();
            GetConfigResponse response = getAjaxClient().execute(req);
            assertTrue(!response.hasError(), "Response has errors: " + getErrorMessage(response));
            assertEquals(adConfig, response.getData(), "The server returned the wrong configuration.");

            // Create Preview
            JSONValue previewConfig = generateAdConfig();
            resp = advertisementApi.putAdvertisementByUserIdWithHttpInfo(ctxId, userId, previewConfig.toString());
            statusLine = resp.getStatusCode();
            assertTrue(Arrays.contains(new int[] { 200, 201 }, statusLine), "Unexpected status: " + statusLine);

            // Check if preview configuration is available
            req = new GetConfigRequest();
            response = getAjaxClient().execute(req);
            assertTrue(!response.hasError(), "Response has errors: " + getErrorMessage(response));
            assertEquals(previewConfig, response.getData(), "The server returned the wrong configuration.");

            // Remove preview configuration
            resp = advertisementApi.deleteAdvertisementByUserIdWithHttpInfo(ctxId, userId);
            assertEquals(204, resp.getStatusCode(), "Deletion of ad config failed: " + resp.getStatusCode());

            // Check if configuration is back to the old one again
            response = getAjaxClient().execute(req);
            assertTrue(!response.hasError(), "Response has errors: " + getErrorMessage(response));
            assertEquals(adConfig, response.getData(), "The server returned the wrong configuration.");
        } catch (Exception e) {
            LoggerFactory.getLogger(AdvertisementTest.class).error("Test failed with error: {}", e.getMessage(), e);
            fail(e.getMessage());
        } finally {
            advertisementApi.deleteAdvertisementByResellerAndPackage(DEFAULT, pack);
            advertisementApi.deleteAdvertisementByUserId(ctxId, userId);
        }
    }

    private String getErrorMessage(GetConfigResponse response) {
        return response.getException() != null ? response.getException().getMessage() : null;
    }

    private static JSONValue generateAdConfig() throws JSONException {
        JSONArray config = new JSONArray();
        int num = 0;
        while (num == 0) {
            num = random.nextInt(5);
        }

        /*
         * {
         * "cooldown": 5000,
         * "gpt": {
         * "adUnitPath": "%adunit%"
         * },
         * "reloadAfter": 30000,
         * "space": "io.ox/ads/leaderboard"
         * }
         */
        long customerId = random.nextLong();
        for (int i = 0; i < num; i++) {
            JSONObject adSpace = new JSONObject();
            adSpace.put("cooldown", 5000);
            adSpace.put("reloadAfter", 30000);
            adSpace.put("space", "io.ox/ads/random_" + Integer.toString(random.nextInt()));
            JSONObject gpt = new JSONObject();
            gpt.put("adUnitPath", "/" + Long.toString(customerId) + "/RandomUnit_" + Integer.toString(random.nextInt()));
            adSpace.put("gpt", gpt);
            config.put(adSpace);
        }

        return config;
    }

    @Override
    protected String getReloadables() {
        return reloadables;
    }
}
