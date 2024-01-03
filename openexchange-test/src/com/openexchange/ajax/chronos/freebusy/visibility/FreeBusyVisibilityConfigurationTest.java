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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.JSlobData;
import com.openexchange.testing.httpclient.models.JSlobsResponse;
import com.openexchange.testing.httpclient.modules.JSlobApi;

/**
 * {@link FreeBusyVisibilityConfigurationTest} - checks configuration for the "freeBusyVisibility" setting
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class FreeBusyVisibilityConfigurationTest extends AbstractFreeBusyVisibilityTest {

    private JSlobApi jSlobApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
        folderId = createAndRememberNewFolder(defaultUserApi, getDefaultFolder(), getCalendaruser());

        jSlobApi = new JSlobApi(testUser.getApiClient());
    }

    @Override
    protected String getScope() {
        return "context";
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        HashMap<String, String> map = new HashMap<>(4, 0.9f);
        map.put("com.openexchange.calendar.enableCrossContextFreeBusy", Boolean.TRUE.toString());
        return map;
    }

    @Test
    public void testFreeBusy_getDefaultFreeBusyVisibility() throws Exception {
        JSlobsResponse jSlobsResponse = jSlobApi.getJSlobList(Collections.singletonList("io.ox/calendar"), null);

        assertNotNull(jSlobsResponse, "Response missing!");
        assertNull(jSlobsResponse.getError());
        for (JSlobData jSlobData : jSlobsResponse.getData()) {
            Assertions.assertTrue(jSlobData.getTree().toString().contains(FREE_BUSY_VISIBILITY + "=all"));
        }
    }

    @Test
    public void testFreeBusy_SetNotExistingValue_returnError() throws Exception {
        CommonResponse response = jSlobApi.setJSlob(Collections.singletonMap("chronos", Collections.singletonMap(FREE_BUSY_VISIBILITY, "bla")), "io.ox/calendar", null);

        assertNotNull(response, "Response missing!");
        assertNotNull(response.getError());
        assertEquals("CAL-4004", response.getCode(), "Unexpected error message");
    }

    @ParameterizedTest
    @ValueSource(strings =
    { "all", "internal-only", "none" })
    public void testFreeBusy_userFromSecondContextDisabledFreeBusyVisibility(String freeBusyVisibility) throws Exception {
        setAndCheckFreeBusyVisibility(jSlobApi, freeBusyVisibility);
    }

}
