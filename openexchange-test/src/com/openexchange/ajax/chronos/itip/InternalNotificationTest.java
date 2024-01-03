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
import static com.openexchange.ajax.chronos.itip.ITipUtil.changeCalendarSettings;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.convertToAttendee;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.UUID;
import org.jdom2.IllegalDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailAttachment;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import com.openexchange.testing.httpclient.modules.UserApi;

/**
 * {@link InternalNotificationTest} - Checks that a notification is sent to another internal user when the organizer
 * updates participant status of external attendees
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
@Disabled // No such notifications yet
public class InternalNotificationTest extends AbstractITipAnalyzeTest {

    private String summary;

    private EventData attendeeEvent = null;

    private Attendee replyingAttendee;

    private Attendee internalAttendee;

    private UserResponse userResponse2C1;

    private SessionAwareClient apiClient2C1;

    private JSlobApi jslobApi;

    /**
     * Creates an event as user A in context 1 with external attendee user B from context 2.
     * User B accepts the event and user A takes over the changes.
     */
    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        summary = this.getClass().getName() + " " + UUID.randomUUID().toString();

        /*
         * Prepare other internal attendee
         */
        apiClient2C1 = testUser2.getApiClient();
        UserApi userApi = new UserApi(apiClient2C1);
        userResponse2C1 = userApi.getUser(String.valueOf(apiClient2C1.getUserId()));
        if (null == userResponse2C1) {
            throw new IllegalDataException("Need user info for test!");
        }
        internalAttendee = convertToAttendee(testUser2, apiClient2C1.getUserId());
        internalAttendee.setPartStat(PartStat.NEEDS_ACTION.getStatus());

        jslobApi = new JSlobApi(apiClient2C1);

        changeCalendarSettings(jslobApi, true, true, true, false);

        /*
         * Create event
         */
        EventData eventToCreate = EventFactory.createSingleTwoHourEvent(0, summary);
        replyingAttendee = prepareCommonAttendees(eventToCreate);
        eventToCreate.getAttendees().add(internalAttendee);
        createdEvent = eventManager.createEvent(eventToCreate, true);

        /*
         * Receive mail as attendee
         */
        MailData iMip = receiveIMip(apiClientC2, userResponseC1.getData().getEmail1(), summary, 0, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClientC2, iMip)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());

        /*
         * reply with "accepted"
         */
        attendeeEvent = assertSingleEvent(accept(apiClientC2, constructBody(iMip), null), createdEvent.getUid());
        assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED.getStatus());

        /*
         * Receive mail as organizer and check actions
         */
        MailData reply = receiveIMip(testUser.getApiClient(), replyingAttendee.getEmail(), summary, 0, SchedulingMethod.REPLY);
        analyze(reply.getId());

        /*
         * Take over accept and check in calendar
         */
        assertSingleEvent(applyResponse(testUser.getApiClient(), constructBody(reply)), createdEvent.getUid());
        EventResponse eventResponse = chronosApi.getEvent(createdEvent.getId(), createdEvent.getFolder(), createdEvent.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        createdEvent = eventResponse.getData();
        for (Attendee attendee : createdEvent.getAttendees()) {
            if (attendee.getEmail().equalsIgnoreCase(testUserC2.getLogin())) {
                assertThat("Participant status is not correct.", PartStat.ACCEPTED.getStatus(), is(attendee.getPartStat()));
            }
        }
    }

    @Test
    public void testInternalNotificationAfterReply() throws Exception {
        MailData notification;
        try {
            notification = ITipUtil.receiveNotification(apiClient2C1, userResponseC1.getData().getEmail1(), summary);
        } catch (@SuppressWarnings("unused") AssertionError ignoree) {
            /*
             * No internal notifications at the moment
             */
            return;
        }
        fail("Mail should not be received!!");

        assertTrue(null != notification.getAttachment() && notification.getAttachment().booleanValue());
        assertThat(I(notification.getAttachments().size()), is(I(1)));

        MailAttachment mailAttachment = notification.getAttachments().get(0);
        assertThat(mailAttachment, is(not(nullValue())));
        assertThat(mailAttachment.getContent(), is(not(nullValue())));
        assertThat(mailAttachment.getContent(), containsStringIgnoringCase("<span class=\\\"person\\\">" + userResponseC1.getData().getDisplayName() + "</span> has changed an appointment"));
        assertThat(mailAttachment.getContent(), containsStringIgnoringCase("<span class=\\\"person\\\">" + userResponseC2.getData().getLastName() + "</span> has <span class=\\\"status declined\\\">"));
    }

}
