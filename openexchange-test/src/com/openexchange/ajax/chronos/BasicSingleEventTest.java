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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.common.collect.ImmutableList;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.ChronosApiException;
import com.openexchange.ajax.chronos.util.CalendarAssertUtil;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.test.common.data.conversion.ical.Assertions;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.UpdatesResult;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.testing.httpclient.modules.ConfigApi;

/**
 *
 * {@link BasicSingleEventTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public class BasicSingleEventTest extends AbstractChronosTest {

    /**
     * Initializes a new {@link BasicSingleEventTest}.
     */
    public BasicSingleEventTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        eventManager.setIgnoreConflicts(true);
    }

    /**
     * Test the creation of a single event
     */
    @Test
    public void testCreateSingle() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testCreateSingle", folderId), true);
        EventData actualEventData = eventManager.getEvent(folderId, expectedEventData.getId());
        CalendarAssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }

    /**
     * Tests the deletion of a single event
     */
    @Test
    public void testDeleteSingle() throws Exception {
        EventData event = EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testDeleteSingle", folderId);
        event.setFolder(folderId);
        EventData expectedEventData = eventManager.createEvent(event, true);

        EventId eventId = new EventId();
        eventId.setId(expectedEventData.getId());
        eventId.setFolder(folderId);

        eventManager.deleteEvent(eventId);

        try {
            eventManager.getRecurringEvent(folderId, expectedEventData.getId(), null, true);
            fail("No exception was thrown");
        } catch (ChronosApiException e) {
            assertNotNull(e);
            assertEquals("CAL-4040", e.getErrorCode());
        }
    }

    /**
     * Tests the update of a single event
     */
    @Test
    public void testUpdateSingle() throws Exception {
        EventData event = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testUpdateSingle", folderId), true);
        event.setEndDate(DateTimeUtil.incrementDateTimeData(event.getEndDate(), 5000));

        EventData updatedEvent = eventManager.updateEvent(event);

        assertNotEquals(event.getLastModified(), updatedEvent.getLastModified(), "The timestamp matches");
        assertNotEquals(event.getSequence(), updatedEvent.getSequence(), "The sequence matches");

        event.setLastModified(updatedEvent.getLastModified());
        event.setSequence(updatedEvent.getSequence());
        CalendarAssertUtil.assertEventsEqual(event, updatedEvent);
    }

    /**
     * Tests the retrieval of a single event
     */
    @Test
    public void testGetEvent() throws Exception {
        Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        instance.setTimeInMillis(System.currentTimeMillis());
        instance.add(Calendar.HOUR, -1);
        Date from = instance.getTime();
        instance.add(Calendar.DAY_OF_MONTH, 1);
        Date until = instance.getTime();

        // Create a single event
        EventData event = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testGetEvent", folderId), true);
        EventId eventId = new EventId();
        eventId.setId(event.getId());
        eventId.setFolder(folderId);

        // Get event directly
        EventData actualEvent = eventManager.getRecurringEvent(folderId, event.getId(), null, false);
        CalendarAssertUtil.assertEventsEqual(event, actualEvent);

        // Get all events
        List<EventData> events = eventManager.getAllEvents(from, until, false, folderId, null, null, false);
        assertEquals(1, events.size());
        CalendarAssertUtil.assertEventsEqual(event, events.get(0));

        // Get updates
        UpdatesResult updatesResult = eventManager.getUpdates(from, false, folderId);
        assertEquals(1, updatesResult.getNewAndModified().size());
        CalendarAssertUtil.assertEventsEqual(event, updatesResult.getNewAndModified().get(0));

        // List events
        events = eventManager.listEvents(Collections.singletonList(eventId));
        assertEquals(1, events.size());
        CalendarAssertUtil.assertEventsEqual(event, events.get(0));

    }

    /**
     * Tests the creation of a single event with different timezones
     */
    @Test
    public void testCreateSingleWithDifferentTimeZones() throws Exception {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        start.setTimeInMillis(System.currentTimeMillis());

        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(10));

        EventData eventdata = EventFactory.createSingleEvent(getCalendaruser(), "testCreateSingle", null, DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end), folderId);
        EventData expectedEventData = eventManager.createEvent(eventdata, true);
        EventData actualEventData = eventManager.getEvent(folderId, expectedEventData.getId());
        CalendarAssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }

    /**
     * Test extendedEntities parameter
     */
    @Test
    public void testExtendedEntities() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testCreateSingle", folderId), true);
        EventData actualEventData = eventManager.getRecurringEvent(folderId, expectedEventData.getId(), null, false, true);
        List<Attendee> attendees = actualEventData.getAttendees();
        Assertions.assertEquals(1, attendees.size());
        Attendee attendee = attendees.get(0);
        Assertions.assertTrue(attendee.getContact() != null);
        Assertions.assertTrue(attendee.getContact().getFirstName() != null);
        Assertions.assertTrue(attendee.getContact().getLastName() != null);
    }

    ////////////////////////////////// Attachment Tests ///////////////////////////////////

    /**
     * Tests the creation of a single event with attachment from the local file system
     */
    @Test
    public void testCreateSingleWithAttachmentFromLocalFilesystem() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        JSONObject expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(getCalendaruser(), "testCreateSingleWithAttachment", asset, folderId), asset);
        assertEquals(1, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");
    }

    /**
     * Tests the creation of a single event with attachment from the OX drive
     */
    @Test
    public void testCreateSingleWithAttachmentFromOXDrive() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);
        String infostoreFolder = getPrivateInfostoreFolder();
        String fileId = infostoreManager.uploadFile(infostoreFolder, "testCreateSingleWithAttachmentFromOXDrive-" + UUID.randomUUID().toString(), assetManager.readAssetRaw(asset));
        JSONObject expectedEventData = eventManager.createEventWithAttachments(EventFactory.createSingleEventWithAttachment(getCalendaruser(), "testCreateSingleWithAttachmentFromOXDrive", folderId, fileId));
        assertEquals( 1, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");
    }

    /**
     * Tests the creation of a single event with multiple attachments from the OX drive
     */
    @Test
    public void testCreateSingleWithMultipleAttachmentsFromOXDrive() throws Exception {
        Asset asset1 = assetManager.getRandomAsset(AssetType.ods);
        Asset asset2 = assetManager.getRandomAsset(AssetType.odt);

        String infostoreFolder = getPrivateInfostoreFolder();
        String fileId1 = infostoreManager.uploadFile(infostoreFolder, "testCreateSingleWithMultipleAttachmentsFromOXDrive-" + UUID.randomUUID().toString(), assetManager.readAssetRaw(asset1));
        String fileId2 = infostoreManager.uploadFile(infostoreFolder, "testCreateSingleWithMultipleAttachmentsFromOXDrive-" + UUID.randomUUID().toString(), assetManager.readAssetRaw(asset2));

        JSONObject expectedEventData = eventManager.createEventWithAttachments(EventFactory.createSingleEventWithAttachments(getCalendaruser(), "testCreateSingleWithAttachmentFromOXDrive", folderId, ImmutableList.of(fileId1, fileId2)));
        assertEquals( 2, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");
    }

    /**
     * Tests the creation of a single event with attachments both from local filesystem and OX drive
     */
    @Test
    public void testCreateSingleWithAttachmentsFromLocalFilesystemAndOXDrive() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        Asset asset1 = assetManager.getRandomAsset(AssetType.ods);
        Asset asset2 = assetManager.getRandomAsset(AssetType.odt);

        String infostoreFolder = getPrivateInfostoreFolder();
        String fileId1 = infostoreManager.uploadFile(infostoreFolder, "testCreateSingleWithAttachmentsFromLocalFilesystemAndOXDrive-" + UUID.randomUUID().toString(), assetManager.readAssetRaw(asset1));
        String fileId2 = infostoreManager.uploadFile(infostoreFolder, "testCreateSingleWithAttachmentsFromLocalFilesystemAndOXDrive-" + UUID.randomUUID().toString(), assetManager.readAssetRaw(asset2));

        JSONObject expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachments(getCalendaruser(), "testCreateSingleWithAttachment", ImmutableList.of(asset), ImmutableList.of(fileId1, fileId2), folderId), asset);
        assertEquals( 3, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");
    }

    /**
     * Tests the update of a single event with attachment
     */
    @Test
    public void testUpdateSingleWithAttachment() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        JSONObject expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(getCalendaruser(), "testUpdateSingleWithAttachment", asset, folderId), asset);
        assertEquals(1, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");

        EventData actualEventData = eventManager.getEvent(folderId, expectedEventData.getString("id"));

        asset = assetManager.getRandomAsset(AssetType.png);
        actualEventData.getAttachments().get(0).setManagedId(actualEventData.getAttachments().get(0).getManagedId());
        actualEventData.getAttachments().add(EventFactory.createAttachment(asset));

        expectedEventData = eventManager.updateEventWithAttachment(actualEventData, asset);
        assertEquals(2, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");
    }

    /**
     * Tests the retrieval of an attachment from an event
     */
    @Test
    public void testGetAttachment() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.pdf);

        Path path = Paths.get(asset.getAbsolutePath());
        byte[] expectedAttachmentData = Files.readAllBytes(path);

        JSONObject expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(getCalendaruser(), "testUpdateSingleWithAttachment", asset, folderId), asset);
        assertEquals(1, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");

        byte[] actualAttachmentData = eventManager.getAttachment(expectedEventData.getString("id"), expectedEventData.getJSONArray("attachments").getJSONObject(0).getInt("managedId"), folderId);
        assertArrayEquals(expectedAttachmentData, actualAttachmentData, "The attachment binary data does not match");
    }

    /**
     * Tests retrieving the ZIP archive containing multiple attachments of an event
     */
    @Test
    public void testGetZippedAttachments() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        JSONObject expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(getCalendaruser(), "testUpdateSingleWithAttachment", asset, folderId), asset);
        assertEquals(1, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");

        EventData actualEventData = eventManager.getEvent(folderId, expectedEventData.getString("id"));

        asset = assetManager.getRandomAsset(AssetType.png);
        actualEventData.getAttachments().get(0).setManagedId(actualEventData.getAttachments().get(0).getManagedId());
        actualEventData.getAttachments().add(EventFactory.createAttachment(asset));

        expectedEventData = eventManager.updateEventWithAttachment(actualEventData, asset);
        JSONArray jAttachments = expectedEventData.getJSONArray("attachments");
        assertEquals(2, jAttachments.length(), "The amount of attachments is not correct");

        byte[] zipData = eventManager.getZippedAttachments(actualEventData.getId(), folderId, jAttachments.getJSONObject(0).getInt("managedId"), jAttachments.getJSONObject(1).getInt("managedId"));
        assertNotNull(zipData);
    }

    /**
     * Tests the creation of an event with two attachments and the deletion of one of them
     * during an update call
     */
    @Test
    public void testUpdateSingleWithAttachmentsDeleteOne() throws Exception {
        Asset assetA = assetManager.getRandomAsset(AssetType.jpg);
        Asset assetB = assetManager.getRandomAsset(AssetType.png);
        List<Asset> assets = new ArrayList<>(2);
        assets.add(assetA);
        assets.add(assetB);

        JSONObject expectedEventData = eventManager.createEventWithAttachments(EventFactory.createSingleEventWithAttachments(getCalendaruser(), "testUpdateSingleWithAttachmentsDeleteOne", assets, folderId), assets);
        assertEquals(2, expectedEventData.getJSONArray("attachments").length(), "The amount of attachments is not correct");

        EventData actualEventData = eventManager.getEvent(folderId, expectedEventData.getString("id"));

        // Set the managed id for the retained attachment
        assertNotNull(actualEventData.getAttachments());
        assertEquals(2, actualEventData.getAttachments().size());
        ChronosAttachment retainedAttachment = actualEventData.getAttachments().get(0);
        // Create the delta
        EventData updateData = new EventData();
        updateData.setId(actualEventData.getId());
        updateData.setAttachments(Collections.singletonList(retainedAttachment));
        updateData.setLastModified(actualEventData.getLastModified());
        updateData.setFolder(folderId);

        // Update the event
        actualEventData = eventManager.updateEvent(updateData);
        // Get...
        EventData exEventData = eventManager.getEvent(folderId, actualEventData.getId());
        // ...and assert
        CalendarAssertUtil.assertEventsEqual(exEventData, actualEventData);
    }

    private String getPrivateInfostoreFolder() throws ApiException {
        ConfigApi configApi = new ConfigApi(getApiClient());
        ConfigResponse configNode = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath());
        Object data = checkResponse(configNode);
        if (data != null && !data.toString().equalsIgnoreCase("null")) {
            return String.valueOf(data);
        }
        org.junit.Assert.fail("It seems that the user doesn't support drive.");
        return null;
    }

    private Object checkResponse(ConfigResponse resp) {
        assertNull(resp.getErrorDesc(), resp.getError());
        assertNotNull(resp.getData());
        return resp.getData();
    }
}
