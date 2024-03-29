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

import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.userfeedback.actions.StoreRequest;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.testing.restclient.invoker.ApiException;
import com.openexchange.testing.restclient.invoker.ApiResponse;
import com.openexchange.userfeedback.rest.services.ExportUserFeedbackService;

/**
 * {@link ExportTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.8.4
 */
public class ExportTest extends AbstractUserFeedbackTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(ExportUserFeedbackService.class);
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        removeFeedbacks();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            removeFeedbacks();
        } finally {
            super.tearDown();
        }
    }

    private void storeFeedbacks(int numberOfFeedbacks) throws OXException, IOException, JSONException {
        StoreRequest feedback = new StoreRequest(type, validFeedback);
        for (int feedbacks = 0; feedbacks < numberOfFeedbacks; feedbacks++) {
            getAjaxClient().execute(feedback);
        }
    }

    private void removeFeedbacks() {
        try {
            userfeedbackApi.delete("default", type, Long.valueOf(0), null);
        } catch (ApiException e) {
            fail("Unable to cleanup: " + e.getMessage());
        }
    }

    @Test
    public void testExportRAW_everythingFine_returnMessage() {
        try {
            ApiResponse<String> export = userfeedbackApi.exportRAWWithHttpInfo("default", type, Long.valueOf(0), Long.valueOf(0));
            assertEquals(200, export.getStatusCode());
            assertNotNull(export.getData());
        } catch (ApiException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testExportRAW_unknownContextGroup_return404() {
        try {
            userfeedbackApi.exportRAW("unknown", type, Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (@SuppressWarnings("unused") ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testExportRAW_unknownFeefdbackType_return404() {
        try {
            userfeedbackApi.exportRAW("default", "schalke-rating", Long.valueOf(0), Long.valueOf(0));
            fail();
        } catch (@SuppressWarnings("unused") ApiException e) {
            assertEquals(404, e.getCode());
        }
    }

    @Test
    public void testRawExport() throws Exception {
        storeFeedbacks(3);

        String export = userfeedbackApi.exportRAW("default", type, Long.valueOf(0), Long.valueOf(0));
        JSONArray jsonExport = new JSONArray(export);

        assertEquals(3, jsonExport.length());

        for (int i = 0; i < jsonExport.length(); ++i) {
            JSONObject exportedFeedback = jsonExport.getJSONObject(i);
            String resolution = exportedFeedback.getString("screen_resolution");
            assertEquals(feedback.getString("Screen_Resolution"), resolution);

            String date = exportedFeedback.getString("date");
            assertTrue(Strings.isNotEmpty(date));

            String score = exportedFeedback.getString("score");
            assertEquals("3", score);

            String browser = exportedFeedback.getString("browser");
            assertEquals("Chrome", browser);

            assertFalse(exportedFeedback.has("Browser"));
            assertFalse(exportedFeedback.has("Language"));
        }
    }

    @Test
    public void testRawExport_onlyOlderFeedbacks_emptyExport() throws Exception {
        storeFeedbacks(3);

        Thread.sleep(2000);
        try {
            DateTime now = DateTime.now(DateTimeZone.UTC);

            String export = userfeedbackApi.exportRAW("default", type, L(now.getMillis()), L(0));
            JSONArray jsonExport = new JSONArray(export);

            assertEquals(0, jsonExport.length());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testRawExport_onlyNewerFeedbacks_emptyExport() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        Thread.sleep(2000);

        storeFeedbacks(3);

        String export = userfeedbackApi.exportRAW("default", type, Long.valueOf(0), L(now.getMillis()));
        JSONArray jsonExport = new JSONArray(export);

        assertEquals(0, jsonExport.length());
    }

    @Test
    public void testRawExport_between_exportThree() throws Exception {
        storeFeedbacks(3);
        Thread.sleep(2000);
        DateTime second = DateTime.now(DateTimeZone.UTC);
        Thread.sleep(2000);
        storeFeedbacks(3);
        Thread.sleep(2000);
        DateTime third = DateTime.now(DateTimeZone.UTC);
        Thread.sleep(2000);
        storeFeedbacks(3);

        String export = userfeedbackApi.exportRAW("default", type, L(second.getMillis()), L(third.getMillis()));
        JSONArray jsonExport = new JSONArray(export);
        assertEquals(3, jsonExport.length());

        String export2 = userfeedbackApi.exportRAW("default", type, L(0), L(0));
        JSONArray jsonExport2 = new JSONArray(export2);
        assertEquals(9, jsonExport2.length());
    }

    @Test
    public void testExportRAW_negativeStart_return404() {
        try {
            userfeedbackApi.exportRAW("default", type, Long.valueOf(-11111), Long.valueOf(0));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testExportRAW_negativeEnd_return404() {
        try {
            userfeedbackApi.exportRAW("default", type, Long.valueOf(0), Long.valueOf(-11111));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testExportRAW_endBeforeStart_return404() {
        try {
            userfeedbackApi.exportRAW("default", type, Long.valueOf(222222222), Long.valueOf(11111));
            fail();
        } catch (ApiException e) {
            assertEquals(400, e.getCode());
        }
    }

    @Test
    public void testExportCSV_everythingFine_returnFile() throws IOException, OXException, JSONException {
        storeFeedbacks(3);

        LineNumberReader lnr = null;
        try {
            ApiResponse<File> export = userfeedbackApi.exportCSVWithHttpInfo("default", type, Long.valueOf(0), Long.valueOf(0), ";");
            assertEquals(200, export.getStatusCode());
            assertNotNull(export.getData());

            lnr = new LineNumberReader(new FileReader(export.getData()));
            lnr.skip(Long.MAX_VALUE);
            assertEquals(4, lnr.getLineNumber());
        } catch (ApiException e) {
            fail(e.getMessage());
        } finally {
            if (lnr != null) {
                lnr.close();
            }
        }
    }

}
