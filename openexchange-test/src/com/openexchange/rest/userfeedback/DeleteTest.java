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

package com.openexchange.rest.userfeedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import com.openexchange.testing.restclient.invoker.ApiException;
import com.openexchange.testing.restclient.invoker.ApiResponse;
import com.openexchange.userfeedback.rest.services.DeleteUserFeedbackService;

/**
 * {@link DeleteTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.4
 */
public class DeleteTest extends AbstractUserFeedbackTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(DeleteUserFeedbackService.class);
    }

    @Test
    public void testDelete_everythingFine_returnMessage() {
        try {
            ApiResponse<String> deleteMsg = userfeedbackApi.deleteWithHttpInfo("default", type, Long.valueOf(0), Long.valueOf(0));
            assertEquals(200, deleteMsg.getStatusCode());
            assertNotNull(deleteMsg.getData());
        } catch (ApiException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testDelete_unknownContextGroup_return404() {
        try {
            userfeedbackApi.deleteWithHttpInfo("unknown", type, Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testDelete_unknownFeefdbackType_return404() {
        try {
            userfeedbackApi.delete("default", "schalke-rating", Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testDelete_negativeStart_return404() {
        try {
            userfeedbackApi.delete("default", type, Long.valueOf(-11111), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testDelete_negativeEnd_return404() {
        try {
            userfeedbackApi.delete("default", type, Long.valueOf(0), Long.valueOf(-11111));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testDelete_endBeforeStart_return404() {
        try {
            userfeedbackApi.delete("default", type, Long.valueOf(222222222), Long.valueOf(11111));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }
}
