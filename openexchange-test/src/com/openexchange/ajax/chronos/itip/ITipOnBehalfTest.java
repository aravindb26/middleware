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

package com.openexchange.ajax.chronos.itip;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.FOLDER_HUMAN_READABLE;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveNotification;
import static com.openexchange.ajax.chronos.manager.CalendarFolderManager.TREE_ID;
import static com.openexchange.java.Autoboxing.i;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosErrorAwareCalendarResult;
import com.openexchange.testing.httpclient.models.ChronosMultipleCalendarResultResponse;
import com.openexchange.testing.httpclient.models.DeleteEventBody;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ITipOnBehalfTest}
 *
 * Scenario:
 * User A from context 1
 * user C from context 1
 *
 * User B from context 2
 *
 * Share a calendar with another user (user C) of the same context (context 1) to check the "On behalf" functionality
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class ITipOnBehalfTest extends AbstractITipAnalyzeTest {

    private String sharedCalendarFolderId;
    private String sharedMailFolderId;

    /** The user C's client */
    private SessionAwareClient apiClientC1_2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        apiClientC1_2 = testUser2.getApiClient();

        /*
         * Create a new calendar folder to operate on
         */
        sharedCalendarFolderId = folderManager.createCalendarFolder(//
            this.getClass().getSimpleName() + UUID.randomUUID().toString(), //
            i(defaultUserApi.getCalUser()));

        /*
         * Share calendar to another user of context 1
         */
        FolderData data = folderManager.getFolder(sharedCalendarFolderId);
        folderManager.shareFolder(data, testUser2);

        /*
         * Share INBOX to the other user in context 1, too
         */
        // @formatter:off
        FolderResponse folderResponse = foldersApi.getFolderBuilder()
                                                  .withId(FOLDER_HUMAN_READABLE)
                                                  .withTree(TREE_ID)
                                                  .withAllowedModules("mail")
                                                  .execute();
        // @formatter:on
        FolderData inbox = checkResponse(folderResponse.getError(), folderResponse.getErrorDesc(), folderResponse.getData());
        inbox = folderManager.shareFolder(inbox, testUser2);
        FoldersApi foldersApi2 = new FoldersApi(testUser2.getApiClient());
        // @formatter:off
        FoldersVisibilityResponse visibleFolders = foldersApi2.getVisibleFoldersBuilder()
                                                              .withContentType("mail")
                                                              .withColumns("1,300")
                                                              .withTree(TREE_ID)
                                                              .withAll(Boolean.TRUE)
                                                              .execute();
        // @formatter:on
        FoldersVisibilityData visibilityData = checkResponse(visibleFolders.getError(), visibleFolders.getErrorDesc(), visibleFolders.getData());
        sharedMailFolderId = getSharedFolder(visibilityData, testUser.getLogin());
        folderResponse = foldersApi2.getFolder(sharedMailFolderId, TREE_ID, null, null, null);
        checkResponse(folderResponse.getError(), folderResponse.getErrorDesc(), folderResponse.getData());
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }

    @Test
    public void testOnBehalfOfInvitation() throws Exception {
        /*
         * Create event as secretary
         */
        String summary = this.getClass().getSimpleName() + ".testOnBehalfOfInvitation" + UUID.randomUUID();
        EventData data = EventFactory.createSingleTwoHourEvent(getUserId(), summary, defaultFolderId);
        Attendee replyingAttendee = prepareCommonAttendees(data);
        replyingAttendee.setEntity(Integer.valueOf(0));
        // @formatter:off
        ChronosCalendarResultResponse response = new ChronosApi(apiClientC1_2).createEventBuilder()
                                                                              .withFolder(sharedCalendarFolderId)
                                                                              .withEventData(data)
                                                                              .withExtendedEntities(TRUE)
                                                                              .execute();
        // @formatter:on
        assertNotNull(response);
        assertNull(response.getError());
        EventData secretaryEvent = response.getData().getCreated().get(0);

        /*
         * Check event within folder of organizer
         */
        createdEvent = eventManager.getEvent(sharedCalendarFolderId, secretaryEvent.getId());
        assertEquals(secretaryEvent.getUid(), createdEvent.getUid());
        assertAttendeePartStat(createdEvent.getAttendees(), testUser.getLogin(), PartStat.ACCEPTED);
        assertAttendeePartStat(createdEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION);

        /*
         * Check notification mail within organizers INBOX
         *
         */
        receiveNotification(apiClient, testUser.getLogin(), summary);

        /*
         * Receive iMIP as attendee and accept
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyzeResponse).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.ALL);

        EventData attendeeEvent = assertSingleEvent(accept(apiClientC2, constructBody(iMip), null), createdEvent.getUid());
        assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);

        /*
         * Receive accept as organizer and update event
         */
        iMip = receiveIMip(apiClient, testUserC2.getLogin(), summary, 0, SchedulingMethod.REPLY);
        analyzeResponse = analyze(apiClient, iMip);
        newEvent = assertSingleChange(analyzeResponse).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.APPLY);

        EventData eventData = assertSingleEvent(applyResponse(apiClient, constructBody(iMip)));
        assertAttendeePartStat(eventData.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);

        /*
         * Double check in the calendar folder
         */
        createdEvent = eventManager.getEvent(sharedCalendarFolderId, secretaryEvent.getId());
        assertEquals(secretaryEvent.getUid(), createdEvent.getUid());
        assertAttendeePartStat(createdEvent.getAttendees(), testUser.getLogin(), PartStat.ACCEPTED);
        assertAttendeePartStat(createdEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);
    }

    @Test
    public void testOnlyBehalfOf() throws Exception {
        /*
         * Create event as secretary
         */
        String summary = this.getClass().getSimpleName() + ".testOnlyBehalfOf." + UUID.randomUUID();
        EventData data = EventFactory.createSingleTwoHourEvent(getUserId(), summary, defaultFolderId);
        Attendee replyingAttendee = prepareCommonAttendees(data);
        replyingAttendee.setEntity(Integer.valueOf(0));
        // @formatter:off
        ChronosCalendarResultResponse response = new ChronosApi(apiClientC1_2).createEventBuilder()
                                                                              .withEventData(data)
                                                                              .withFolder(sharedCalendarFolderId)
                                                                              .withExtendedEntities(TRUE)
                                                                              .execute();
        // @formatter:on
        assertNotNull(response);
        assertNull(response.getError());
        EventData secretaryEvent = response.getData().getCreated().get(0);

        /*
         * Check event within folder of organizer
         */
        createdEvent = eventManager.getEvent(sharedCalendarFolderId, secretaryEvent.getId());
        assertEquals(secretaryEvent.getUid(), createdEvent.getUid());
        assertAttendeePartStat(createdEvent.getAttendees(), testUser.getLogin(), PartStat.ACCEPTED);
        assertAttendeePartStat(createdEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION);

        /*
         * Check notification mail within organizers INBOX
         *
         */
        receiveNotification(apiClient, testUser.getLogin(), summary);

        /*
         * Receive iMIP as attendee and accept
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyzeResponse).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.ALL);

        EventData attendeeEvent = assertSingleEvent(accept(apiClientC2, constructBody(iMip), null), createdEvent.getUid());
        assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);

        /*
         * Receive accept as secretary and update event
         */
        iMip = receiveIMip(apiClientC1_2, sharedMailFolderId, testUserC2.getLogin(), summary, 0, secretaryEvent.getUid(), null, SchedulingMethod.REPLY, -1);
        analyzeResponse = analyze(apiClientC1_2, iMip);
        newEvent = assertSingleChange(analyzeResponse).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.APPLY);

        EventData eventData = assertSingleEvent(applyResponse(apiClientC1_2, constructBody(iMip)));
        assertAttendeePartStat(eventData.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);

        /*
         * Double check in the calendar folder
         */
        createdEvent = eventManager.getEvent(sharedCalendarFolderId, secretaryEvent.getId());
        assertEquals(secretaryEvent.getUid(), createdEvent.getUid());
        assertAttendeePartStat(createdEvent.getAttendees(), testUser.getLogin(), PartStat.ACCEPTED);
        assertAttendeePartStat(createdEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);

        // @formatter:off
        EventResponse resp = new ChronosApi(apiClientC1_2).getEventBuilder()
                                                          .withFolder(sharedCalendarFolderId)
                                                          .withId(secretaryEvent.getId())
                                                          .execute();
        assertNull(resp.getError(), resp.getErrorDesc());
        // @formatter:on
        /*
         * Delete event as secretary
         */
        DeleteEventBody body = new DeleteEventBody();
        EventId eventId = new EventId();
        eventId.setFolder(sharedCalendarFolderId);
        eventId.setId(secretaryEvent.getId());
        body.setEvents(Collections.singletonList(eventId));
        // @formatter:off
        ChronosMultipleCalendarResultResponse deleteResponse = new ChronosApi(apiClientC1_2).deleteEventBuilder()
                                                                                            .withTimestamp(resp.getTimestamp())
                                                                                            .withDeleteEventBody(body)
                                                                                            .execute();
        // @formatter:on
        List<ChronosErrorAwareCalendarResult> deleted = checkResponse(deleteResponse.getError(), deleteResponse.getErrorDesc(), deleteResponse.getData());
        assertNotNull(deleted);
        assertTrue(deleted.size() == 1);
        assertThat(deleted.get(0).getError(), is(nullValue()));
        assertTrue(deleted.get(0).getDeleted() != null && deleted.get(0).getDeleted().size() == 1);
        assertThat(deleted.get(0).getDeleted().get(0).getId(), is(secretaryEvent.getId()));

        /*
         * Receive delete as attendee and delete, too
         */
        iMip = receiveIMip(apiClientC2, testUser.getLogin(), ITipUtil.deletedSummary(summary), 1, SchedulingMethod.CANCEL);
        analyzeResponse = analyze(apiClientC2, iMip);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.CANCEL);
        cancel(apiClientC2, constructBody(iMip), null, false);
    }

    /*
     * ============================== HELPERS ==============================
     */

    private String getSharedFolder(FoldersVisibilityData visibilityData, String owner) {
        Object privates = visibilityData.getPrivate();
        @SuppressWarnings("unchecked") List<Object> folders = (List<Object>) privates;
        for (Object folder : folders) {
            @SuppressWarnings("unchecked") List<Object> folderColumns = (List<Object>) folder;
            String folderId = folderColumns.get(0).toString();
            if (com.openexchange.java.Strings.isNotEmpty(folderId) && folderId.contains(owner) || folderId.contains("shared")) {
                return folderId;
            }
            String folderTitle = folderColumns.get(1).toString();
            if (com.openexchange.java.Strings.isNotEmpty(folderTitle) && folderTitle.contains(owner) || folderId.contains("shared")) {
                return folderId;
            }
        }
        Assertions.fail("Shared folder not found. Available mail folders : " + visibilityData.toString());
        return null;
    }

}
