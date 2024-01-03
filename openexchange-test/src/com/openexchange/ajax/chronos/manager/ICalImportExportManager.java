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

package com.openexchange.ajax.chronos.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.dmfs.rfc5545.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.InfoItemExport;
import com.openexchange.testing.httpclient.modules.ExportApi;
import com.openexchange.testing.httpclient.modules.ImportApi;

/**
 * {@link ICalImportExportManager}
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10.0
 */
public class ICalImportExportManager {

    /** iCAL file containing a series with a single instance */
    public static final ICalFile RECURRENCE_IMPORT_ICS = new ICalFile("ical_recurrence_import.ics", "Test", "15279a8a-6305-41a5-8de9-029319782b98", "20171010T060000Z");
    /** iCAL file containing a single event */
    public static final ICalFile SINGLE_IMPORT_ICS = new ICalFile("single_test_event.ics", "TestSingle", "e316a2a4-83bf-441f-ad2d-9622e3210772", null);
    /** iCAL file containing a series */
    public static final ICalFile SERIES_IMPORT_ICS = new ICalFile("test_series.ics", "TestSeries", "fbbd81e8-4a81-4092-bc9b-7e3a5cbb5861", null);
    /** iCAL file containing a floating event */
    public static final ICalFile FLOATING_ICS = new ICalFile("MWB-2.ics", "Flight to Berlin-Tegel", "12345abcdef_CGNTXL", null);
    /** iCAL file containing one orphaned instances*/
    public static final ICalFile ORPHANED_INSTANCES_ICS = new ICalFile("MWB-2171.ics", "MWB-2171", "8db02655-e049-42ba-9835-d8597e329e27", null);
    /** iCAL file containing two orphaned instances with the same UID*/
    public static final ICalFile MULTIPLE_ORPHANED_INSTANCES_ICS = new ICalFile("MWB-2171-multiple.ics", "MWB-2171-Multiple", "8db02655-e049-42ba-9835-d8597e329e27", null);

    private final ExportApi exportApi;
    private final ImportApi importApi;

    /**
     * Initializes a new {@link ICalImportExportManager}.
     * 
     * @param exportApi The export API
     * @param importApi The import API
     */
    public ICalImportExportManager(ExportApi exportApi, ImportApi importApi) {
        super();
        this.exportApi = exportApi;
        this.importApi = importApi;
    }

    /**
     * Imports calendar data from iCalendar file.
     *
     *
     * @param folder Object ID of the folder into which the data should be imported. This may be be an appointment or a task folder. (required)
     * @param file The iCal file containing the appointment and task data. (required)
     * @param suppressNotification Can be used to disable the notifications for new appointments that are imported through the given iCal file. This help keeping the Inbox clean if a lot of appointments need to be imported. The value of this
     *            parameter does not matter because only for the existence of the parameter is checked. (optional)
     * @param ignoreUIDs When set to &#x60;true&#x60;, UIDs are partially ignored during import of tasks and appointments from iCal. Internally, each UID is replaced statically by a random one to preserve possibly existing relations between recurring
     *            appointments in the same iCal file, but at the same time to avoid collisions with already existing tasks and appointments. (optional)
     * @return The response. Use {@link #parseImportJSONResponseToEventIds(String)} to decode
     * @throws Exception
     */
    public String importICalFile(String folder, File file, Boolean suppressNotification, Boolean ignoreUIDs) throws Exception {
        return importApi.importICal(folder, file, Boolean.FALSE, suppressNotification, ignoreUIDs, Boolean.TRUE);
    }

    /**
     * Exports appointment and task data to an iCalendar file.
     *
     * @param folder Object ID of the folder whose content shall be exported. This must be a calendar folder. (required)
     * @return The iCAL file
     * @throws ApiException In case of error
     */
    public String exportICalFile(String folder) throws ApiException {
        return exportApi.exportAsICalGetReq(folder);
    }

    /**
     * Exports a batch of appointments and tasks data to a iCalendar file.
     *
     * @param body A list of {@link InfoItemExport} to export
     * @return The iCAL file
     * @throws ApiException In case of error
     */
    public String exportICalBatchFile(List<InfoItemExport> body) throws ApiException {
        return exportApi.exportAsICal(body);
    }

    /**
     * Parses a response into dedicated {@link EventId}s
     *
     * @param response The response to parse
     * @return A {@link List} of events
     * @throws JSONException In case of unknown format
     */
    public List<EventId> parseImportJSONResponseToEventIds(String response) throws JSONException {
        JSONObject object = new JSONObject(response);
        JSONArray data = object.optJSONArray("data");
        if (data == null) {
            return Collections.emptyList();
        }
        int length = data.length();
        if (length <= 0) {
            return Collections.emptyList();
        }
        List<EventId> eventIds = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            JSONObject tuple = data.getJSONObject(i);
            try {
                String folderId = tuple.getString("folder_id");
                String objectId = tuple.getString("id");
                EventId eventId = new EventId();
                eventId.setFolder(folderId);
                eventId.setId(objectId);
                eventIds.add(eventId);
            } catch (@SuppressWarnings("unused") JSONException e) {
                return Collections.emptyList();
            }
        }
        return eventIds;
    }

    /**
     * Asserts that the IDs are equal
     *
     * @param recurrenceId One ID
     * @param recurrenceId2 The other ID
     */
    public static void assertRecurrenceID(String recurrenceId, String recurrenceId2) {
        assertEquals(CalendarUtils.decode(recurrenceId).shiftTimeZone(DateTime.UTC), CalendarUtils.decode(recurrenceId2).shiftTimeZone(DateTime.UTC));
    }

    /**
     * {@link ICalFile}
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * 
     * @param fileName The file name
     * @param summary The summary of the event(s)
     * @param uid The UID of the event(s)
     * @param recurrenceId The optional recurrence ID
     */
    public record ICalFile(String fileName, String summary, String uid, String recurrenceId) {

    }

}
