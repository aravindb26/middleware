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

package com.openexchange.ajax.chronos.bugs;

import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.itip.AbstractITipAnalyzeTest;
import com.openexchange.ajax.chronos.itip.ITipUtil;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.manager.ICalImportExportManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.Lists;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.models.Analysis;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.modules.ExportApi;
import com.openexchange.testing.httpclient.modules.ImportApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.UserApi;
import com.openexchange.time.TimeTools;

/**
 * {@link MWB2296Test}
 * 
 * Working with a particular appointment throws a HTTP500
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MWB2296Test extends AbstractITipAnalyzeTest {

    private ICalImportExportManager importExportManager;
    private TestUser testUser3;
    private EventManager eventManager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        importExportManager = new ICalImportExportManager(new ExportApi(getApiClient()), new ImportApi(getApiClient()));
        testUser3 = testContext.acquireUser();
        eventManager2 = new EventManager(new com.openexchange.ajax.chronos.UserApi(testUser2.getApiClient(), testUser2), getDefaultFolder(testUser2.getApiClient()));
    }

    @Test
    public void testProvokeUIDConflict() throws Exception {
        /*
         * As user X from context 2, create a new event and invite user A from context 1
         */
        String uid = UUID.randomUUID().toString();
        TimeZone timeZone = TimeZone.getTimeZone("America/Vancouver");
        EventData eventData = new EventData();
        eventData.setFolder(folderIdC2);
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        eventData.setLocation(UUIDs.getUnformattedStringFromRandom());
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), TimeTools.D("Next friday at 10 am", timeZone).getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), TimeTools.D("Next friday at 11 am", timeZone).getTime()));
        eventData.setUid(uid);
        eventData.setOrganizer(getCalendarUser(testUserC2));
        eventData.setAttendees(Arrays.asList(getUserAttendee(testUserC2), getExternalAttendee(userResponseC1.getData())));
        EventData createdEvent = eventManagerC2.createEvent(eventData, true);
        createdEvent = eventManagerC2.getEvent(folderIdC2, createdEvent.getId());
        /*
         * As user A from context 1, forward the received invitation mail to user B from context 1
         * As user B from context 1, download the attached iCalendar file, then import it manually into the calendar on App Suite
         * (using a shortcut) 
         */
        MailData iMip = receiveIMip(testUser.getApiClient(), userResponseC2.getData().getEmail1(), createdEvent.getSummary(), 
            createdEvent.getSequence().intValue(), createdEvent.getUid(), null, SchedulingMethod.REQUEST);
        String attachmentId = ITipUtil.extractITipAttachmentId(iMip, SchedulingMethod.REQUEST);
        byte[] icsFile = new MailApi(apiClient).getMailAttachmentBuilder().withFolder(iMip.getFolderId()).withId(iMip.getId()).withAttachment(attachmentId).execute();
        String defaultFolderId2 = getDefaultFolder(testUser2.getApiClient());
        String importResponse = importIcs(testUser2.getApiClient(), defaultFolderId2, icsFile);
        List<EventId> eventIds = importExportManager.parseImportJSONResponseToEventIds(importResponse);
        assertTrue(null != eventIds && 1 == eventIds.size());
        EventData importedEvent = eventManager2.getEvent(eventIds.get(0).getFolder(), eventIds.get(0).getId());
        /*
         * As user X from context 2, edit the appointment created in step 1 and add user C from context 1 as participant
         */
        EventData eventUpdate = new EventData();
        eventUpdate.setFolder(createdEvent.getFolder());
        eventUpdate.setId(createdEvent.getId());
        eventUpdate.setAttendees(Lists.combine(createdEvent.getAttendees(), getExternalAttendee(new UserApi(testUser3.getApiClient()).getUser(String.valueOf(testUser3.getUserId())).getData())));
        eventManagerC2.updateEvent(eventUpdate, false);
        EventData updatedEvent = eventManagerC2.getEvent(folderIdC2, createdEvent.getId());
        /*
         * As user C from context 1, add the received invitation to your calendar (unless it happened automatically)
         */
        iMip = receiveIMip(testUser3.getApiClient(), userResponseC2.getData().getEmail1(), updatedEvent.getSummary(), 
            updatedEvent.getSequence().intValue(), updatedEvent.getUid(), null, SchedulingMethod.REQUEST);
        AnalyzeResponse analyzeResponse = analyze(testUser3.getApiClient(), iMip);
        Analysis analysis = analyzeResponse.getData().get(0);
        assertTrue(null != analysis.getChanges() && 0 < analysis.getChanges().size());
        CalendarResult applyResult = applyCreate(testUser3.getApiClient(), constructBody(iMip));
        assertTrue(null != applyResult.getCreated() && 0 < applyResult.getCreated().size() || null != applyResult.getUpdated() && 0 < applyResult.getUpdated().size());
        /*
         * As user B from context 1, open the appointment imported in step 3, and add user C from context 1 as participant, expecting to fail with a conflict
         */
        eventUpdate = new EventData();
        eventUpdate.setFolder(importedEvent.getFolder());
        eventUpdate.setId(importedEvent.getId());
        eventUpdate.setAttendees(Lists.combine(importedEvent.getAttendees(), getUserAttendee(testUser3)));
        ChronosCalendarResultResponse updateResponse = eventManager2.updateEvent(eventUpdate, false);
        assertNotNull(updateResponse.getError());
        assertEquals("CAL-4093", updateResponse.getCode());
        assertNull(updateResponse.getData());
    }

    @Test
    public void testImportWithAttachmentSchemeCID() throws Exception {
        /*
         * prepare iCal file & import it
         */
        String iCal = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Open-Xchange//8.19.0//EN
            BEGIN:VEVENT
            DTSTAMP:20231002T134253Z
            ATTACH;FILENAME=test_image.jpeg;FMTTYPE=image/jpeg;SIZE=43069:cid:5@592fadc
             f-1a63-4057-8204-4526aeb78caa
            DTEND:20231005T203000Z
            DTSTART:20231005T190000Z
            SUMMARY:MWB2296Test
            END:VEVENT
            END:VCALENDAR
            """;
        String importResponse = importIcs(getApiClient(), defaultFolderId, iCal.getBytes(StandardCharsets.UTF_8));
        List<EventId> eventIds = importExportManager.parseImportJSONResponseToEventIds(importResponse);
        assertTrue(null != eventIds && 1 == eventIds.size());
        assertEquals(defaultFolderId, eventIds.get(0).getFolder());
        /*
         * get imported event & check attachments
         */
        String id = eventIds.get(0).getId();
        EventData eventData = chronosApi.getEventBuilder().withFolder(defaultFolderId).withId(id).execute().getData();
        assertNotNull(eventData);
        List<ChronosAttachment> attachments = eventData.getAttachments();
        assertTrue(null == attachments || attachments.isEmpty());
    }

    @Test
    public void testImportWithAttachmentSchemeHTTPS() throws Exception {
        /*
         * prepare iCal file & import it
         */
        String iCal = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Open-Xchange//8.19.0//EN
            BEGIN:VEVENT
            DTSTAMP:20231002T134253Z
            ATTACH;FILENAME=test_image.jpeg;FMTTYPE=image/jpeg:https://www.open-xchange
             .com/hs-fs/hubfs/ox-logo-darkstay-open.png
            DTEND:20231005T203000Z
            DTSTART:20231005T190000Z
            SUMMARY:MWB2296Test
            END:VEVENT
            END:VCALENDAR
            """;
        String importResponse = importIcs(getApiClient(), defaultFolderId, iCal.getBytes(StandardCharsets.UTF_8));
        List<EventId> eventIds = importExportManager.parseImportJSONResponseToEventIds(importResponse);
        assertTrue(null != eventIds && 1 == eventIds.size());
        assertEquals(defaultFolderId, eventIds.get(0).getFolder());
        /*
         * get imported event & check attachments
         */
        String id = eventIds.get(0).getId();
        EventData eventData = chronosApi.getEventBuilder().withFolder(defaultFolderId).withId(id).execute().getData();
        assertNotNull(eventData);
        List<ChronosAttachment> attachments = eventData.getAttachments();
        assertTrue(null != attachments && 1 == attachments.size());
    }

    private static String importIcs(ApiClient apiClient, String folderId, byte[] data) throws Exception {
        ICalImportExportManager importExportManager = new ICalImportExportManager(new ExportApi(apiClient), new ImportApi(apiClient));
        File tempFile = null;
        try {
            tempFile = File.createTempFile(MWB2296Test.class.getSimpleName(), ".ics");
            tempFile.deleteOnExit();
            FileUtils.writeByteArrayToFile(tempFile, data);
            return importExportManager.importICalFile(folderId, tempFile, Boolean.TRUE, Boolean.FALSE);
        } finally {
            if (null != tempFile) {
                tempFile.delete();
            }
        }
    }

}
