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

import static com.openexchange.ajax.chronos.manager.ICalImportExportManager.assertRecurrenceID;
import static com.openexchange.ajax.chronos.util.ChronosUtils.find;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.ICalImportExportManager;
import com.openexchange.ajax.chronos.manager.ICalImportExportManager.ICalFile;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.InfoItemExport;

/**
 * {@link ICalEventImportExportTest}
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10.0
 */
public class ICalEventImportExportTest extends AbstractImportExportTest {

    @Test
    public void testFolderEventExport() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser().intValue(), "testCreateSingle"), true);
        String iCalEvent = importExportManager.exportICalFile(expectedEventData.getFolder());
        assertNotNull(iCalEvent);
        assertTrue(iCalEvent.contains("testCreateSingle"));
    }

    @Test
    public void testBatchEventExport() throws Exception {
        List<EventData> eventData = new ArrayList<>();
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser().intValue(), "testBatchEvent1"), true);
        List<InfoItemExport> itemList = new ArrayList<>();

        addInfoItemExport(itemList, expectedEventData.getFolder(), expectedEventData.getId());
        eventData.add(expectedEventData);

        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        start.setTimeInMillis(System.currentTimeMillis());
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10));

        expectedEventData = eventManager.createEvent(EventFactory.createSingleEvent(defaultUserApi.getCalUser().intValue(), "testCreateSingle", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end)), true);
        addInfoItemExport(itemList, expectedEventData.getFolder(), expectedEventData.getId());
        eventData.add(expectedEventData);

        String iCalExport = importExportManager.exportICalBatchFile(itemList);
        assertNotNull(iCalExport);
        assertEventData(eventData, iCalExport);
    }

    @Test
    public void testSingleEventImport() throws Exception {
        ICalFile iCalFile = ICalImportExportManager.SINGLE_IMPORT_ICS;
        List<EventData> eventData = parseEventData(getImportResponse(iCalFile));
        assertEquals(1, eventData.size());
        assertEquals(iCalFile.summary(), eventData.get(0).getSummary());
        assertEquals(iCalFile.uid(), eventData.get(0).getUid());
        hasModifiedLineup(eventData);
    }

    @Test
    public void testSeriesEventImport() throws Exception {
        ICalFile iCalFile = ICalImportExportManager.SERIES_IMPORT_ICS;
        List<EventData> eventData = parseEventData(getImportResponse(iCalFile));
        assertEquals(1, eventData.size());
        assertEquals(iCalFile.summary(), eventData.get(0).getSummary());
        assertEquals(iCalFile.uid(), eventData.get(0).getUid());
        hasModifiedLineup(eventData);
    }

    @Test
    public void testICalEventRecurrenceImport() throws Exception {
        ICalFile iCalFile = ICalImportExportManager.RECURRENCE_IMPORT_ICS;
        List<EventData> eventData = parseEventData(getImportResponse(iCalFile));
        assertFalse(eventData.isEmpty());
        for (EventData event : eventData) {
            assertEquals(iCalFile.summary(), event.getSummary());
            assertEquals(iCalFile.uid(), event.getUid());
            if (Strings.isNotEmpty(event.getRecurrenceId())) {
                assertRecurrenceID(iCalFile.recurrenceId(), event.getRecurrenceId());
            }
        }
        hasModifiedLineup(eventData);
    }

    @Test
    public void testICalOrphanedEventRecurrenceImport() throws Exception {
        ICalFile iCalFile = ICalImportExportManager.ORPHANED_INSTANCES_ICS;
        List<EventData> eventData = parseEventData(getImportResponse(iCalFile));
        assertFalse(eventData.isEmpty());
        assertTrue(eventData.size() == 1);
        EventData event = eventData.get(0);
        assertEquals(iCalFile.summary(), event.getSummary());
        assertEquals(iCalFile.uid(), event.getUid());
        hasModifiedLineup(event);
    }

    @Test
    public void testICalMultipleOrphanedEventRecurrenceImport() throws Exception {
        ICalFile iCalFile = ICalImportExportManager.MULTIPLE_ORPHANED_INSTANCES_ICS;
        List<EventData> eventData = parseEventData(getImportResponse(iCalFile));
        assertFalse(eventData.isEmpty());
        assertTrue(eventData.size() == 2);
        assertFalse(eventData.get(0).getUid().equals(eventData.get(1).getUid()));
        assertTrue(iCalFile.uid().equals(eventData.get(0).getUid()) || iCalFile.uid().equals(eventData.get(1).getUid()));
        hasModifiedLineup(eventData);
    }

    @Test
    public void testICalEventImportUIDHandling() throws Exception {
        parseEventData(getImportResponse(ICalImportExportManager.RECURRENCE_IMPORT_ICS));
        String response = importICalFile(ICalImportExportManager.RECURRENCE_IMPORT_ICS.fileName());
        assertTrue(response.contains("The appointment could not be created due to another conflicting appointment with the same unique identifier"));
    }

    @Test
    public void testICalImportExportRoundTrip() throws Exception {
        ICalFile iCalFile = ICalImportExportManager.SERIES_IMPORT_ICS;
        List<EventData> eventData = parseEventData(getImportResponse(iCalFile));
        assertEquals(1, eventData.size());
        assertEquals(iCalFile.summary(), eventData.get(0).getSummary());
        assertEquals(iCalFile.uid(), eventData.get(0).getUid());
        hasModifiedLineup(eventData);

        List<InfoItemExport> itemList = new ArrayList<>();
        addInfoItemExport(itemList, eventData.get(0).getFolder(), eventData.get(0).getId());

        String iCalExport = importExportManager.exportICalBatchFile(itemList);
        assertNotNull(iCalExport);
        assertEventData(eventData, iCalExport);
    }

    /**
     * Checks whether the given events have the acting user set
     * as the only attendee and as organizer
     *
     * @param events The events to check
     */
    private void hasModifiedLineup(Collection<EventData> events) {
        for (EventData event : events) {
            hasModifiedLineup(event);
        }
    }

    /**
     * Checks whether the given event has the acting user set
     * as the only attendee and as organizer
     *
     * @param event The event to check
     */
    private void hasModifiedLineup(EventData event) {
        assertTrue(event.getAttendees().size() == 1);
        assertNotNull(find(event.getAttendees(), testUser.getUserId()));
        assertNotNull(event.getOrganizer().getEntity());
        assertTrue(testUser.getUserId() == event.getOrganizer().getEntity().intValue());
    }
}
