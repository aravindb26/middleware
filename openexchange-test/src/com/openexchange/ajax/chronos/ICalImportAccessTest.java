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

package com.openexchange.ajax.chronos;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.manager.ICalImportExportManager;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ICalImportAccessTest} This test is a substitution for the test Bug8681forICAL
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10.0
 */
public class ICalImportAccessTest extends AbstractImportExportTest {

    private static final int MAX_RETRY = 3;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        UserModuleAccess usa = new UserModuleAccess();
        usa.enableAll();
        usa.setCalendar(false);
        usa.setTasks(false);
        // @formatter:off
        ConfigAwareProvisioningService.getService()
                                      .changeContext(testContext.getId(), TestContextConfig.builder()
                                                                                           .withUserModuleAccess(usa)
                                                                                           .build(), testContext.getUsedBy());
        // @formatter:on
    }

    @Test
    public void testImportCalendarAccess() throws InterruptedException {
        boolean gotErrorResponse = false;
        int attempts = 0;
        while (MAX_RETRY > attempts && gotErrorResponse == false) {
            try {
                String errorResponse = getImportResponse(ICalImportExportManager.SINGLE_IMPORT_ICS);
                assertNotNull(errorResponse);
                assertTrue(errorResponse.contains("CAL-4045"), "Wrong error:" + errorResponse);

                gotErrorResponse = true;
            } catch (AssertionError | Exception e) {
                if (++attempts == MAX_RETRY) {
                    throw new AssertionError("Unable to retrieve error response after " + MAX_RETRY + " retries.");
                }
                Thread.sleep(5000);
            }
        }
        assertTrue(gotErrorResponse, "Did not receive the error response");
    }

}
