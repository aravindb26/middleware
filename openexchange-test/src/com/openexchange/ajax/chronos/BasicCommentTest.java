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

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosMultipleCalendarResultResponse;
import com.openexchange.testing.httpclient.models.DeleteEventBody;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.MailAttachment;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailResponse;
import com.openexchange.testing.httpclient.models.MailsResponse;
import com.openexchange.testing.httpclient.models.UpdateEventBody;
import com.openexchange.testing.httpclient.models.UserData;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.UserApi;

/**
 * {@link BasicCommentTest} - Tests for <a href="https://jira.open-xchange.com/browse/MW-989">MW-989</a>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.1
 */
public class BasicCommentTest extends AbstractChronosTest {

    /**
     * Columns: received_date, id, folder_id, subject
     */
    private static final String COLUMNS = "610,600,601,607";

    private static final String INBOX = "default0/INBOX";

    private final static String UPDATE = "Important update message!!";

    private final static String DELETE = "It's cloudy outside. Lets shift the event";

    private EventData eventData;

    @Test
    public void testUpdate() throws Exception {
        String summary = "Test comment function on update";
        eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);

        setAttendees();

        eventData = eventManager.createEvent(eventData, true);
        UpdateEventBody body = new UpdateEventBody();
        body.setEvent(eventData);
        body.setComment(UPDATE);
        eventData.setDescription("Description got updated.");
        ChronosCalendarResultResponse response = chronosApi.updateEvent(getFolderId(), eventData.getId(), Long.valueOf(System.currentTimeMillis()), body, null, null, Boolean.FALSE, null, Boolean.FALSE, null, null, null, null, Boolean.FALSE, null);
        assertThat(response.getErrorDesc(), response.getError(), nullValue());

        validateMailInSecondUsersInbox("Appointment changed: " + summary, UPDATE);
    }

    @Test
    public void testUpdateWithAttachment() throws Exception {
        String summary = "Test comment function on update with attachments";
        eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);

        setAttendees();

        eventData = eventManager.createEvent(eventData, true);

        // Update with attachment
        eventManager.updateEventWithAttachmentAndNotification(eventData, assetManager.getRandomAsset(AssetType.pdf), UPDATE);

        validateMailInSecondUsersInbox("Appointment changed: " + summary, UPDATE);
    }

    @Test
    public void testDelete() throws Exception {
        String summary = "Test comment function on delete";
        eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);

        setAttendees();

        eventData = eventManager.createEvent(eventData, true);

        DeleteEventBody body = new DeleteEventBody();
        body.setComment(DELETE);
        body.setEvents(Collections.singletonList(getEventId()));

        ChronosMultipleCalendarResultResponse response = chronosApi.deleteEvent(Long.valueOf(System.currentTimeMillis()), body, null, null, Boolean.FALSE, Boolean.FALSE, null, null, null);
        assertThat(response.getErrorDesc(), response.getError(), nullValue());
        eventData = null;

        validateMailInSecondUsersInbox("Appointment canceled: " + summary, DELETE);
    }

    private void validateMailInSecondUsersInbox(String mailSubject, String comment) throws OXException, ApiException, Exception {
		SessionAwareClient apiClient2 = testUser2.getApiClient();
        MailApi mailApi = new MailApi(apiClient2);

        MailResponse response = mailApi.getMail(INBOX, getMailId(mailApi, mailSubject), null, null, null, null, null, null, null, null, null, null, null, null);
        MailData mailData = response.getData();
        assertThat("No mail data", mailData, notNullValue());
        MailAttachment mailAttachment = mailData.getAttachments().get(0);
        Assertions.assertTrue(mailAttachment.getContent().contains("<i>" + comment + "</i>"), "Should contain comment");
    }

    private String getMailId(MailApi mailApi, String summary) throws Exception {
        int max = 5;
        String mailId = null;
        while (max > 0 && mailId == null) {
            max--;
            Thread.sleep(10000);
            MailsResponse mailsResponse = mailApi.getAllMails(INBOX, COLUMNS, null, Boolean.FALSE, Boolean.FALSE, "600", "desc", null, null, I(100), null);
            List<List<Object>> data = mailsResponse.getData();
            assertNull(mailsResponse.getError(), mailsResponse.getErrorDesc());
            assertNotNull(mailsResponse.getData());
            if (data.size() == 0) {
                continue;
            }
            for (List<Object> singleMailData : data) {
                // Indices based on COLUMNS
                if (summary.equals(singleMailData.get(3))) {
                    mailId = singleMailData.get(1).toString();
                    break;
                }
            }
        }
        assertThat("No update/cancel mail found", mailId, notNullValue());
        return mailId;
    }

    private EventId getEventId() {
        EventId id = new EventId();
        id.setId(eventData.getId());
        id.setFolder(getFolderId());
        return id;
    }

    private String getFolderId() {
        return null != eventData.getFolder() ? eventData.getFolder() : defaultFolderId;
    }

    private void setAttendees() throws ApiException {
        Attendee organizer = createAttendee(I(testUser.getUserId()));
        Attendee attendee = createAttendee(I(testUser2.getUserId()));
        LinkedList<Attendee> attendees = new LinkedList<>();
        attendees.add(organizer);
        attendees.add(attendee);
        eventData.setAttendees(attendees);
        setOrganizer(organizer);
    }

    protected Attendee createAttendee(Integer userId) throws ApiException {
        Attendee attendee = AttendeeFactory.createAttendee(userId, CuTypeEnum.INDIVIDUAL);

        UserData userData = getUserInformation(userId);

        attendee.cn(userData.getDisplayName());
        attendee.comment("Comment for user " + userData.getDisplayName());
        attendee.email(userData.getEmail1());
        attendee.setUri("mailto:" + userData.getEmail1());
        return attendee;
    }

    protected void setOrganizer(Attendee organizer) {
        CalendarUser c = new CalendarUser();
        c.cn(organizer.getCn());
        c.email(organizer.getEmail());
        c.entity(organizer.getEntity());
        eventData.setOrganizer(c);
        eventData.setCalendarUser(c);
    }

    private UserData getUserInformation(Integer userId) throws ApiException {
        UserApi api = new UserApi(getApiClient());
        UserResponse userResponse = api.getUser(String.valueOf(userId));
        return userResponse.getData();
    }

}
