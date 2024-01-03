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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.EventData;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link LinkedAttachmentsTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.5
 */
public class LinkedAttachmentsTest extends AbstractChronosTest {

    /**
     * Initializes a new {@link LinkedAttachmentsTest}.
     */
    public LinkedAttachmentsTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        eventManager.setIgnoreConflicts(true);
    }

    private static EventData prepareEvent(List<ChronosAttachment> attachments) {
        TimeZone timeZone = TimeZone.getTimeZone("Asis/Shanghai");
        Date start = TimeTools.D("Tomorrow noon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1, timeZone);
        EventData eventData = new EventData();
        eventData.setSummary(LinkedAttachmentsTest.class.getSimpleName());
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventData.setAttachments(attachments);
        return eventData;
    }

    public static ChronosAttachment prepareRandomLinkedAttachment() {
        String name = UUIDs.getUnformattedStringFromRandom();
        String filename = name + ".jpg";
        String uri = "ftp://ftp.example.org/data/test/" + filename;
        String fmtType = "image/jpeg";
        Long size = Long.valueOf(2345235);
        return prepareLinkedAttachment(uri, filename, fmtType, size);
    }
    
    public static ChronosAttachment prepareInlineAttachment(Asset asset, int index) {
        ChronosAttachment chronosAttachment = new ChronosAttachment();
        chronosAttachment.setFilename(asset.getFilename());
        chronosAttachment.setFmtType(MimeTypes.checkedMimeType(asset.getAssetType().name(), asset.getFilename()));
        chronosAttachment.setUri("cid:file_" + index);
        return chronosAttachment;
    }

    private static ChronosAttachment prepareLinkedAttachment(String uri, String filename, String fmtType, Long size) {
        ChronosAttachment chronosAttachment = new ChronosAttachment();
        chronosAttachment.setFilename(filename);
        chronosAttachment.setUri(uri);
        chronosAttachment.setFmtType(fmtType);
        chronosAttachment.setSize(size);
        return chronosAttachment;
    }
    
    public static void assertAttachments(List<ChronosAttachment> expectedAttachments, EventData eventData) {
        List<ChronosAttachment> actualAttachments = eventData.getAttachments();
        if (null == expectedAttachments || expectedAttachments.isEmpty()) {
            assertTrue(null == actualAttachments || actualAttachments.isEmpty());
            return;
        }
        assertNotNull(actualAttachments);
        assertEquals(expectedAttachments.size(), actualAttachments.size());
        for (ChronosAttachment expectedAttachment : expectedAttachments) {
            ChronosAttachment actualAttachment = lookupByFilename(actualAttachments, expectedAttachment.getFilename());
            assertNotNull(actualAttachment);
            if (null == actualAttachment.getUri()) {
                assertTrue(null == expectedAttachment.getUri() || expectedAttachment.getUri().startsWith("cid:file_"));
                assertTrue(null != actualAttachment.getManagedId() && 0 < actualAttachment.getManagedId().intValue());
            } else {
                assertEquals(expectedAttachment.getUri(), actualAttachment.getUri());
            }
            assertEquals(expectedAttachment.getFmtType(), actualAttachment.getFmtType());
        }
    }
    
    private static ChronosAttachment lookupByFilename(List<ChronosAttachment> attachments, String filename) {
        if (null != attachments) {
            for (ChronosAttachment attachment : attachments) {
                if (Objects.equals(attachment.getFilename(), filename)) {
                    return attachment;
                }
            }
        }
        return null;
    }

    @Test
    public void testCreateWithLinkedAttachment() throws Exception {
        /*
         * prepare event with linked attachment
         */
        ChronosAttachment chronosAttachment = prepareRandomLinkedAttachment();
        EventData eventData = prepareEvent(Collections.singletonList(chronosAttachment));
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(Collections.singletonList(chronosAttachment), createdEvent);
    }

    @Test
    public void testCreateWithMultipleLinkedAttachments() throws Exception {
        /*
         * prepare event with linked attachments
         */
        List<ChronosAttachment> chronosAttachments = new ArrayList<ChronosAttachment>();
        int count = 12;
        for (int i = 0; i < count; i++) {
            chronosAttachments.add(prepareRandomLinkedAttachment());
        }
        EventData eventData = prepareEvent(chronosAttachments);
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(chronosAttachments, createdEvent);
    }

    @Test
    public void testCreateWithMixedModeAttachments() throws Exception {
        /*
         * prepare event with linked attachments and file
         */
        List<ChronosAttachment> chronosAttachments = new ArrayList<ChronosAttachment>();
        chronosAttachments.add(prepareRandomLinkedAttachment());
        Asset assetA = assetManager.getRandomAsset(AssetType.jpg);
        chronosAttachments.add(prepareInlineAttachment(assetA, 0));
        chronosAttachments.add(prepareRandomLinkedAttachment());
        Asset assetB = assetManager.getRandomAsset(AssetType.png);
        chronosAttachments.add(prepareInlineAttachment(assetB, 1));
        EventData eventData = prepareEvent(chronosAttachments);
        /*
         * create event & check stored attachment metadata
         */
        JSONObject response = eventManager.createEventWithAttachments(eventData, Arrays.asList(assetA, assetB));
        EventData createdEvent = eventManager.getEvent(defaultFolderId, response.getString("id"));
        assertAttachments(chronosAttachments, createdEvent);
    }

    @Test
    public void testAttemptDownloadLinkedAttachment() throws Exception {
        /*
         * prepare event with linked attachment
         */
        ChronosAttachment chronosAttachment = prepareRandomLinkedAttachment();
        EventData eventData = prepareEvent(Collections.singletonList(chronosAttachment));
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(Collections.singletonList(chronosAttachment), createdEvent);
        /*
         * try to download the attachment (which should not work)
         */
        Exception expectedException = null;
        try {
            chronosApi.getEventAttachment(createdEvent.getId(), createdEvent.getFolder(), createdEvent.getAttachments().get(0).getManagedId());
        } catch (Exception e) {
            expectedException = e;
        }
        assertNotNull(expectedException);
    }

    @Test
    public void testAddLinkedAttachment() throws Exception {
        /*
         * prepare event w/o attachments
         */
        EventData eventData = prepareEvent(null);
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(null, createdEvent);
        /*
         * update event & add a linked attachment
         */
        EventData updatedEventData = new EventData();
        updatedEventData.setId(createdEvent.getId());
        updatedEventData.setFolder(createdEvent.getFolder());
        ChronosAttachment chronosAttachment = prepareRandomLinkedAttachment();
        updatedEventData.setAttachments(Collections.singletonList(chronosAttachment));
        EventData updatedEvent = eventManager.updateEvent(updatedEventData, false, false);
        assertAttachments(Collections.singletonList(chronosAttachment), updatedEvent);
    }
    
    @Test
    public void testRemoveLinkedAttachment() throws Exception {
        /*
         * prepare event with linked attachment
         */
        ChronosAttachment chronosAttachment = prepareRandomLinkedAttachment();
        EventData eventData = prepareEvent(Collections.singletonList(chronosAttachment));
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(Collections.singletonList(chronosAttachment), createdEvent);
        /*
         * update event & remove linked attachment
         */
        EventData updatedEventData = new EventData();
        updatedEventData.setId(createdEvent.getId());
        updatedEventData.setFolder(createdEvent.getFolder());
        updatedEventData.setAttachments(Collections.emptyList());
        EventData updatedEvent = eventManager.updateEvent(updatedEventData, false, false);
        assertAttachments(null, updatedEvent);
    }

    @Test
    public void testAddAnotherLinkedAttachment() throws Exception {
        /*
         * prepare event with linked attachment
         */
        ChronosAttachment chronosAttachment = prepareRandomLinkedAttachment();
        EventData eventData = prepareEvent(Collections.singletonList(chronosAttachment));
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(Collections.singletonList(chronosAttachment), createdEvent);
        /*
         * update event & add another attachment
         */
        EventData updatedEventData = new EventData();
        updatedEventData.setId(createdEvent.getId());
        updatedEventData.setFolder(createdEvent.getFolder());
        ChronosAttachment otherChronosAttachment = prepareRandomLinkedAttachment();
        updatedEventData.setAttachments(Arrays.asList(createdEvent.getAttachments().get(0), otherChronosAttachment));
        EventData updatedEvent = eventManager.updateEvent(updatedEventData, false, false);
        assertAttachments(Arrays.asList(chronosAttachment, otherChronosAttachment), updatedEvent);
    }

    @Test
    public void testRemoveAnotherLinkedAttachment() throws Exception {
        /*
         * prepare event with linked attachments
         */
        ChronosAttachment chronosAttachment1 = prepareRandomLinkedAttachment();
        ChronosAttachment chronosAttachment2 = prepareRandomLinkedAttachment();
        EventData eventData = prepareEvent(Arrays.asList(chronosAttachment1, chronosAttachment2));
        /*
         * create event & check stored attachment metadata
         */
        EventData createdEvent = eventManager.createEvent(eventData, true);
        assertAttachments(Arrays.asList(chronosAttachment1, chronosAttachment2), createdEvent);
        /*
         * update event & remove second attachment
         */
        EventData updatedEventData = new EventData();
        updatedEventData.setId(createdEvent.getId());
        updatedEventData.setFolder(createdEvent.getFolder());
        updatedEventData.setAttachments(Arrays.asList(createdEvent.getAttachments().get(0)));
        EventData updatedEvent = eventManager.updateEvent(updatedEventData, false, false);
        assertAttachments(Arrays.asList(chronosAttachment1), updatedEvent);
    }

}
