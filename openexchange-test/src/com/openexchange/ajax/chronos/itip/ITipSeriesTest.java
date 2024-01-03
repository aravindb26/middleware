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

import static com.openexchange.ajax.chronos.factory.EventFactory.prepareDeltaEvent;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertChanges;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertEvents;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.acceptSummary;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.java.Autoboxing.L;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.models.AnalysisChange;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosMultipleCalendarResultResponse;
import com.openexchange.testing.httpclient.models.DeleteEventBody;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.MailsCleanUpResponse;
import com.openexchange.testing.httpclient.models.UpdateEventBody;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link ITipSeriesTest}
 *
 * User A from context 1 will create a series with 10 occurrences with user B from context 2 as attendee.
 * User B will accept the event in the setup and the change will be accepted by the organizer.
 *
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class ITipSeriesTest extends AbstractITipAnalyzeTest {

    private String summary;

    /** User B from context 2 */
    private Attendee replyingAttendee;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        summary = this.getClass().getName() + " " + UUID.randomUUID().toString();
        EventData event = EventFactory.createSeriesEvent(getUserId(), summary, 10, defaultFolderId);
        replyingAttendee = prepareCommonAttendees(event);

        createdEvent = eventManager.createEvent(event);
        assertEquals(event.getRrule(), createdEvent.getRrule(), "RRule not equals");

        /*
         * Receive mail as attendee
         */
        MailData inviteMail = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        MailListElement element = new MailListElement();
        element.setFolder(inviteMail.getFolderId());
        element.setId(inviteMail.getId());
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClientC2, inviteMail)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(createdEvent.getUid(), newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION);

        /*
         * reply with "accepted"
         */
        EventData attendeeEvent = assertSingleEvent(accept(apiClientC2, constructBody(inviteMail), null), createdEvent.getUid());
        // remove invite mail
        MailsCleanUpResponse delInviteResp = new MailApi(apiClientC2).deleteMails(Collections.singletonList(element), null, Boolean.TRUE, null);
        assertNull(delInviteResp.getError(), delInviteResp.getErrorDesc());
        assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);

        MailData reply = receiveIMip(apiClient, replyingAttendee.getEmail(), summary, 0, SchedulingMethod.REPLY);
        element = new MailListElement();
        element.setFolder(reply.getFolderId());
        element.setId(reply.getId());
        analyze(reply.getId());

        /*
         * Take over accept and check in calendar
         */
        assertSingleEvent(applyResponse(apiClient, constructBody(reply)), createdEvent.getUid());
        // remove reply mail
        MailsCleanUpResponse delReplyResp = new MailApi(apiClient).deleteMails(Collections.singletonList(element), null, Boolean.TRUE, null);
        assertNull(delReplyResp.getError(), delReplyResp.getErrorDesc());
        EventResponse eventResponse = chronosApi.getEvent(createdEvent.getId(), createdEvent.getFolder(), createdEvent.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        createdEvent = eventResponse.getData();
        for (Attendee attendee : createdEvent.getAttendees()) {
            PartStat.ACCEPTED.assertStatus(attendee);
        }
    }

    @Test
    public void testDeleteSingleOccurrence() throws Exception {
        /*
         * Delete a single occurrence as organizer
         */
        List<EventData> allEvents = getAllEventsOfCreatedEvent();
        String recurrenceId = allEvents.get(2).getRecurrenceId();

        DeleteEventBody body = new DeleteEventBody();
        EventId id = new EventId();
        id.setFolder(defaultFolderId);
        id.setId(createdEvent.getId());
        id.setRecurrenceId(recurrenceId);
        body.setEvents(Collections.singletonList(id));
        // @formatter:off
        ChronosMultipleCalendarResultResponse result = chronosApi.deleteEventBuilder()
                                                                 .withTimestamp(L(eventManager.getLastTimeStamp()))
                                                                 .withDeleteEventBody(body)
                                                                 .execute();
        // @formatter:on

        /*
         * Check result
         */
        assertNotNull(result);
        assertNull(result.getError());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() == 1, "Only one element should be deleted");
        assertNull(result.getData().get(0).getError());
        assertNotNull(result.getData().get(0).getUpdated());
        assertTrue(result.getData().get(0).getUpdated().size() == 1, "Only one element should be deleted");
        assertTrue(result.getData().get(0).getUpdated().get(0).getDeleteExceptionDates().size() == 1, "Only one element should be deleted");
        assertThat(result.getData().get(0).getUpdated().get(0).getDeleteExceptionDates().get(0), is(recurrenceId));

        /*
         * Receive deletion as attendee and delete
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), "Appointment canceled: " + summary, 1, SchedulingMethod.CANCEL);
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        assertAnalyzeActions(analyzeResponse, ITipActionSet.CANCEL);
        cancel(apiClientC2, constructBody(iMip), null, true);
    }

    @Test
    public void testChangeSingleOccurrenceAndSplit() throws Exception {
        /*
         * Update a single occurrence as organizer
         */
        List<EventData> allEvents = getAllEventsOfCreatedEvent();
        String recurrenceId = allEvents.get(2).getRecurrenceId();

        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setDescription("Totally new description for " + this.getClass().getName());

        UpdateEventBody body = getUpdateBody(deltaEvent);
        // @formatter:off
        ChronosCalendarResultResponse result = chronosApi.updateEventBuilder()
                                                         .withFolder(defaultFolderId)
                                                         .withId(createdEvent.getId())
                                                         .withTimestamp(L(eventManager.getLastTimeStamp()))
                                                         .withUpdateEventBody(body)
                                                         .withRecurrenceId(recurrenceId)
                                                         .execute();
        // @formatter:on
        /*
         * Check result
         */
        assertNotNull(result);
        CalendarResult calendarResult = checkResponse(result.getError(), result.getErrorDesc(), result.getData());
        assertNotNull(calendarResult.getCreated());
        assertTrue(calendarResult.getCreated().size() == 1);
        assertTrue(calendarResult.getUpdated().size() == 1);
        assertFalse(createdEvent.getId().equals(calendarResult.getCreated().get(0).getId()));
        assertTrue(createdEvent.getId().equals(calendarResult.getCreated().get(0).getSeriesId()));
        String exceptionId = calendarResult.getCreated().get(0).getId();
        createdEvent = calendarResult.getUpdated().get(0);

        /*
         * Receive update as attendee, no rescheduling
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        AnalyzeResponse analyzeResponse = analyze(apiClientC2, iMip);
        AnalysisChange change = assertSingleChange(analyzeResponse);
        AnalysisChangeNewEvent newEvent = change.getNewEvent();
        assertNotNull(newEvent);
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED.getStatus());
        assertAnalyzeActions(analyzeResponse, ITipActionSet.ALL);
        /*
         * Tentative accept for attendee and check in calendar
         */
        tentative(apiClientC2, constructBody(iMip), null);
        EventData attendeeMaster = eventManagerC2.getEvent(null, change.getCurrentEvent().getSeriesId());
        assertTrue(attendeeMaster.getChangeExceptionDates() != null && attendeeMaster.getChangeExceptionDates().size() == 1, "No exception");
        assertAttendeePartStat(attendeeMaster.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED.getStatus());
        EventData attendeeException = eventManagerC2.getRecurringEvent(null, attendeeMaster.getId(), attendeeMaster.getChangeExceptionDates().get(0), false);
        assertAttendeePartStat(attendeeException.getAttendees(), replyingAttendee.getEmail(), PartStat.TENTATIVE.getStatus());
        /*
         * Update calendar object with the tentative accepted event as organizer
         */
        MailData reply = ITipUtil.receiveIMip(apiClient, replyingAttendee.getEmail(), summary, 0, createdEvent.getUid(), new DefaultRecurrenceId(CalendarUtils.decode(recurrenceId)), SchedulingMethod.REPLY);
        assertEvents(applyResponse(apiClient, constructBody(reply)), createdEvent.getUid(), 2);
        EventResponse eventResponse = chronosApi.getEvent(exceptionId, createdEvent.getFolder(), null, null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        assertAttendeePartStat(eventResponse.getData().getAttendees(), replyingAttendee.getEmail(), PartStat.TENTATIVE.getStatus());
        if (null != eventResponse.getTimestamp()) {
            eventManager.setLastTimeStamp(eventResponse.getTimestamp());
        }
        /*
         * Split series by changing location and summary at fifth occurrence
         */
        recurrenceId = allEvents.get(5).getRecurrenceId();
        String location = "Olpe";
        String updatedSummary = this.getClass().getName() + " " + UUID.randomUUID().toString();
        deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setLocation(location);
        deltaEvent.setSummary(updatedSummary);
        body = getUpdateBody(deltaEvent);
        // @formatter:off
        result = chronosApi.updateEventBuilder()
                           .withFolder(defaultFolderId)
                           .withId(createdEvent.getId())
                           .withTimestamp(Long.valueOf(eventManager.getLastTimeStamp()))
                           .withUpdateEventBody(body)
                           .withRecurrenceId(recurrenceId)
                           .withRecurrenceRange(THIS_AND_FUTURE)
                           .execute();
        // @formatter:on
        /*
         * Check result
         */
        assertNotNull(result);
        calendarResult = checkResponse(result.getError(), result.getErrorDesc(), result.getData());
        assertNotNull(calendarResult.getCreated());
        assertTrue(calendarResult.getCreated().size() == 1);
        assertFalse(createdEvent.getUid().equals(calendarResult.getCreated().get(0).getUid()));
        assertTrue(calendarResult.getUpdated().size() == 2);
        assertTrue(createdEvent.getUid().equals(calendarResult.getUpdated().get(0).getUid()) || createdEvent.getUid().equals(calendarResult.getUpdated().get(1).getUid()));
        createdEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getSeriesId());
        assertNull(createdEvent.getChangeExceptionDates(), "Should have no exception");
        /*
         * Get mails as attendee
         * a) Update with existing UID of event, updated summary
         * b) Invitation to detached series with the existing change exception, old summary
         */
        /*--------- a) --------*/
        /*
         * Get update to existing event as attendee and decline
         */
        MailData updatedSeriersIMip = receiveIMip(apiClientC2, testUser.getLogin(), updatedSummary, 1, SchedulingMethod.REQUEST);
        analyzeResponse = analyze(apiClientC2, updatedSeriersIMip);
        change = assertSingleChange(analyzeResponse);
        AnalysisChangeNewEvent updatedEvent = change.getNewEvent();
        assertNotNull(updatedEvent);
        assertAttendeePartStat(updatedEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED.getStatus());
        assertEquals(createdEvent.getUid(), updatedEvent.getUid(), "Updated series must have the same UID as before");
        assertAnalyzeActions(analyzeResponse, ITipActionSet.ALL);
        String updatedEventSeriesId = assertSingleEvent(decline(apiClientC2, constructBody(updatedSeriersIMip), null)).getSeriesId();
        assertTrue(Strings.isNotEmpty(updatedEventSeriesId));
        assertTrue(attendeeMaster.getSeriesId().equals(updatedEventSeriesId));
        /*
         * Check event in attendees calendar
         */
        EventData attendeeSeriesMaster = eventManagerC2.getEvent(null, updatedEventSeriesId);
        assertAttendeePartStat(attendeeSeriesMaster.getAttendees(), replyingAttendee.getEmail(), PartStat.DECLINED.getStatus());
        assertAttendeePartStat(attendeeSeriesMaster.getAttendees(), testUser.getLogin(), PartStat.ACCEPTED.getStatus());
        assertThat(attendeeSeriesMaster.getChangeExceptionDates(), is(empty())); // TODO BUG?!
        /*
         * Check reply in organizers inbox
         */
        reply = receiveIMip(apiClient, replyingAttendee.getEmail(), updatedSummary, 1, SchedulingMethod.REPLY);
        analyze(reply.getId());

        /*--------- b) --------*/
        /*
         * Get invitation to new series, wit new UID and a existing change exception. Expect unchanged part stat
         */
        MailData newSeriersIMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        newEvent = assertChanges(analyze(apiClientC2, newSeriersIMip), 2, 0).getNewEvent();
        assertNotNull(newEvent);
        assertNotEquals(createdEvent.getUid(), newEvent.getUid(), "New series must NOT have the same UID as before");
        assertAttendeePartStat(newEvent.getAttendees(), replyingAttendee.getEmail(), null == newEvent.getRrule() ? PartStat.TENTATIVE.getStatus() : PartStat.ACCEPTED.getStatus());

        /*
         * Accept and check that master and exception are accepted
         */
        for (EventData attendeeEvent : ITipAssertion.assertEvents(accept(apiClientC2, constructBody(newSeriersIMip), null), newEvent.getUid(), 2)) {
            assertAttendeePartStat(attendeeEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.ACCEPTED);
        }
        
        /*
         * Check reply in organizers inbox
         */
        reply = receiveIMip(apiClient, replyingAttendee.getEmail(), acceptSummary(this.userResponseC2.getData().getDisplayName(), summary), 0, SchedulingMethod.REPLY);
        analyze(reply.getId());
    }
}
