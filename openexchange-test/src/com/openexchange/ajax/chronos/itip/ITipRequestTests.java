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
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.convertToAttendee;
import static com.openexchange.ajax.chronos.itip.ITipUtil.parseICalAttachment;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import org.junit.jupiter.params.ParameterizedTest;
import com.openexchange.testing.httpclient.models.ActionResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;

/**
 * {@link ITipRequestTests}
 * 
 * Creates an event as user B from context 2, sending a invite to user A from context 1.
 * User A will accept, tentative or decline via iTIP.
 * Afterwards the reply message within the organizers inbox will be checked
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.0
 */
public class ITipRequestTests extends AbstractITipTest {

    //@formatter:off
    public static Stream<Arguments> data() {
        return Stream.of(
                // Attendees will be overwritten by setup, so '0' is fine
                Arguments.of("SingleTwoHourEvent", EventFactory.createSingleTwoHourEvent(0, null)),
                Arguments.of("SeriesEventFiveOccurences", EventFactory.createSeriesEvent(0, null, 5, null)),
                Arguments.of("MonthlySeriesEvent", EventFactory.createSeriesEvent(0, null, 5, null, EventFactory.RecurringFrequency.MONTHLY))
        );
    }
    //@formatter:on

    private MailData mailData;

    /** Organizer from context 2, user B */
    private Attendee organizer;

    /** Attendee from context 1, user A */
    private Attendee attendee;

    private String uniqueSummary;

    private EventData createEvent;

    public void prepareTest(TestInfo testInfo, String summary, EventData event) throws Exception {
        super.setUp(testInfo);

        List<Attendee> attendees = new LinkedList<>();

        attendee = convertToAttendee(testUser, Integer.valueOf(0));
        attendee.uri("mailto:" + userResponseC1.getData().getEmail1());
        attendee.email(userResponseC1.getData().getEmail1());
        attendees.add(attendee);
        organizer = convertToAttendee(testUserC2, apiClientC2.getUserId());
        organizer.uri("mailto:" + userResponseC2.getData().getEmail1());
        organizer.email(userResponseC2.getData().getEmail1());
        organizer.setPartStat(PartStat.ACCEPTED.toString());
        attendees.add(organizer);

        event.setAttendees(attendees);
        CalendarUser c = new CalendarUser();
        c.uri("mailto:" + userResponseC2.getData().getEmail1());
        c.cn(userResponseC2.getData().getDisplayName());
        c.email(userResponseC2.getData().getEmail1());
        c.entity(Integer.valueOf(userResponseC2.getData().getId()));
        event.setOrganizer(c);
        event.setCalendarUser(c);

        uniqueSummary = summary + UUID.randomUUID().toString();
        event.setSummary(uniqueSummary);

        /* Create event as user B */
        createEvent = eventManagerC2.createEvent(event, true);

        mailData = receiveIMip(apiClient, organizer.getEmail(), uniqueSummary, 0, SchedulingMethod.REQUEST);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testAccept(String summary, EventData event, TestInfo testInfo) throws Exception {
        prepareTest(testInfo, summary, event);
        validate(accept(constructBody(mailData), null), ParticipationStatus.ACCEPTED);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testTenative(String summary, EventData event, TestInfo testInfo) throws Exception {
        prepareTest(testInfo, summary, event);
        validate(tentative(constructBody(mailData), null), ParticipationStatus.TENTATIVE);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testDecline(String summary, EventData event, TestInfo testInfo) throws Exception {
        prepareTest(testInfo, summary, event);
        validate(decline(constructBody(mailData), null), ParticipationStatus.DECLINED);
    }

    private void validate(ActionResponse response, ParticipationStatus partStat) throws Exception {
        /*
         * Validate event from attendee perspective
         */
        EventData updatedEvent = assertSingleEvent(response);
        assertThat("Should be the same start date", updatedEvent.getStartDate(), is(createEvent.getStartDate()));
        assertThat("Should be the same end date", updatedEvent.getEndDate(), is(createEvent.getEndDate()));

        assertThat("Should contain attendees", updatedEvent.getAttendees(), notNullValue());
        assertThat("Should be same attendees", Integer.valueOf(updatedEvent.getAttendees().size()), is(Integer.valueOf(2)));
        assertThat("Should be the same organizer", updatedEvent.getOrganizer().getEmail(), is(organizer.getEmail()));

        assertAttendeePartStat(updatedEvent.getAttendees(), attendee.getEmail(), partStat.getValue());

        /*
         * Validate event from organizer perspective
         */
        MailData replyMail = receiveIMip(apiClientC2, attendee.getEmail(), uniqueSummary, 0, SchedulingMethod.REPLY);
        assertNotNull(replyMail);

        ImportedCalendar iTipReply = parseICalAttachment(apiClientC2, replyMail);
        assertEquals(SchedulingMethod.REPLY.name(), iTipReply.getMethod());
        assertThat("Only one object should have been handled", Integer.valueOf(iTipReply.getEvents().size()), is(Integer.valueOf(1)));
        Event replyEvent = iTipReply.getEvents().get(0);
        assertAttendeePartStat(replyEvent.getAttendees(), attendee.getEmail(), partStat);
        
        /*
         * Apply changes and validate again
         */
        applyResponse(apiClientC2, constructBody(replyMail));
        EventData organizerEvent = eventManagerC2.getEvent(createEvent.getFolder(), createEvent.getId());
        assertAttendeePartStat(organizerEvent.getAttendees(), attendee.getEmail(), partStat.getValue());
    }
}
