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

package com.openexchange.ajax.chronos.scheduling;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.parseDateTime;
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.itip.IMipReceiver;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.testing.httpclient.models.AnalysisChange;
import com.openexchange.testing.httpclient.models.AnalysisChangeCurrentEvent;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.MailData;

/**
 * 
 * {@link AutoProcessTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class AutoProcessTest extends AbstractSchedulingTest {

    @Test
    public void testAutoProcess() throws Exception {
        /*
         * Create event
         */
        String summary = "testAutoProcess" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        Attendee replyingAttendee = prepareCommonAttendees(eventData);
        createdEvent = eventManager.createEvent(eventData);
        /*
         * Check that mail is within attendees INBOX, should already be processed
         * even tough the organizer is unknown to the attendee
         */
        MailData iMip = receiveIMip(apiClientC2, testUser.getLogin(), summary, 0, SchedulingMethod.REQUEST);
        AnalyzeResponse response = analyze(apiClientC2, iMip);
        assertAnalyzeActions(response, ITipActionSet.ACTIONS);
        AnalysisChange change = assertSingleChange(response);
        /*
         * Check that participant status isn't set
         */
        AnalysisChangeCurrentEvent currentEvent = change.getCurrentEvent();
        assertThat("No event found", currentEvent, notNullValue());
        assertAttendeePartStat(currentEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());
        assertThat("Event ID not set", currentEvent.getId(), notNullValue());
    }

    @Test
    public void testParallelAutoProcess() throws Exception {
        /*
         * Create event
         */
        String summary = "testParallelAutoProcess" + UUID.randomUUID().toString();
        EventData eventData = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        Attendee replyingAttendee = prepareCommonAttendees(eventData);
        createdEvent = eventManager.createEvent(eventData);

        /*
         * Load INBOX in multiple parallel request to provoke that the listener
         * is triggered multiple times for the same mail
         */

        IMipReceiver receiver = new IMipReceiver(apiClientC2).from(testUser.getLogin()).subject(summary).sequence(I(0)).method(SchedulingMethod.REQUEST).isParallelExecution(true);
        MailData iMip = receiver.receive();
        /*
         * Check that mail is within attendees INBOX, should already be processed
         * even tough the organizer is unknown to the attendee
         */
        AnalyzeResponse response = analyze(apiClientC2, iMip);
        assertAnalyzeActions(response, ITipActionSet.ACTIONS);
        AnalysisChange change = assertSingleChange(response);
        /*
         * Check that participant status isn't set
         */
        AnalysisChangeCurrentEvent currentEvent = change.getCurrentEvent();
        assertThat("No event found", currentEvent, notNullValue());
        assertAttendeePartStat(currentEvent.getAttendees(), replyingAttendee.getEmail(), PartStat.NEEDS_ACTION.getStatus());
        assertThat("Event ID not set", currentEvent.getId(), notNullValue());
        /*
         * Ensure that there are no duplicates
         */
        List<EventData> allEvents = eventManagerC2.getAllEvents(parseDateTime(createdEvent.getStartDate()), parseDateTime(createdEvent.getEndDate()));
        allEvents = allEvents.stream().filter(e -> summary.equals(e.getSummary())).collect(Collectors.toList());
        assertThat(I(allEvents.size()), is(I(1)));
    }
}
