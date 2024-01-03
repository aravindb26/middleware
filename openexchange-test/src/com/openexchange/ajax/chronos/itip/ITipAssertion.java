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

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.util.ChronosUtils;
import com.openexchange.testing.httpclient.models.ActionResponse;
import com.openexchange.testing.httpclient.models.Analysis;
import com.openexchange.testing.httpclient.models.AnalysisChange;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Annotations;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link ITipAssertion}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class ITipAssertion {

    /**
     * identifier of the internal calendar access provider
     */
    public static final String INTERNAL_CALENDAR = "Internal Calendar";

    private ITipAssertion() {}

    /**
     * Asserts that only one analyze with exact one change was provided in the response
     *
     * @param analyzeResponse The response to check
     * @return The {@link AnalysisChange} provided by the server
     */
    public static AnalysisChange assertSingleChange(AnalyzeResponse analyzeResponse) {
        assertNull(analyzeResponse.getCode(), "error during analysis: " + analyzeResponse.getError());
        assertEquals(1, analyzeResponse.getData().size(), "unexpected analysis number in response");
        Analysis analysis = analyzeResponse.getData().get(0);
        assertEquals(1, analysis.getChanges().size(), "unexpected number of changes in analysis. Changes: " + analysis.getChanges().toString());
        return analysis.getChanges().get(0);
    }

    /**
     * Asserts that only one analyze with exact one change was provided in the response
     *
     * @param analyzeResponse The response to check
     * @param size The expected size of events
     * @param index The index of events to return
     * @return The {@link AnalysisChange} provided by the server
     */
    public static AnalysisChange assertChanges(AnalyzeResponse analyzeResponse, int size, int index) {
        assertNull(analyzeResponse.getCode(), "error during analysis: " + analyzeResponse.getError());
        assertEquals(1, analyzeResponse.getData().size(), "unexpected analysis number in response");
        Analysis analysis = analyzeResponse.getData().get(0);
        assertEquals(size, analysis.getChanges().size(), "unexpected number of changes in analysis");
        return analysis.getChanges().get(index);
    }

    /**
     * Asserts that only one analyze with exact one change was provided in the response
     *
     * @param analyzeResponse The response to check
     * @return The {@link AnalysisChange} provided by the server
     */
    public static Annotations assertSingleAnnotations(AnalyzeResponse analyzeResponse) {
        assertNull(analyzeResponse.getCode(), "error during analysis: " + analyzeResponse.getError());
        assertEquals(1, analyzeResponse.getData().size(), "unexpected analysis number in response");
        Analysis analysis = analyzeResponse.getData().get(0);
        assertEquals(1, analysis.getAnnotations().size(), "unexpected number of annotations in analysis");
        return analysis.getAnnotations().get(0);
    }

    /**
     * Asserts that the given attendee represented by its mail has the desired participant status
     *
     * @param attendees The attendees of the event
     * @param email The attendee to check represented by its mail
     * @param partStat The participant status of the attendee
     * @return The attendee to check as {@link Attendee} object
     */
    public static Attendee assertAttendeePartStat(List<Attendee> attendees, String email, PartStat partStat) {
        Attendee attendee = ChronosUtils.find(attendees, email);
        partStat.assertStatus(attendee);
        return attendee;
    }

    /**
     * Asserts that the given attendee represented by its mail has the desired participant status
     *
     * @param attendees The attendees of the event
     * @param email The attendee to check represented by its mail
     * @param expectedPartStat The participant status of the attendee
     * @return The attendee to check as {@link Attendee} object
     */
    public static Attendee assertAttendeePartStat(List<Attendee> attendees, String email, String expectedPartStat) {
        Attendee attendee = ChronosUtils.find(attendees, email);
        assertNotNull(attendee);
        assertEquals(expectedPartStat, attendee.getPartStat());
        return attendee;
    }

    /**
     * Asserts that the given attendee represented by its entity ID has the desired participant status
     *
     * @param attendees The attendees of the event
     * @param entity The attendee to check represented by its entity ID
     * @param expectedPartStat The participant status of the attendee
     * @return The attendee to check as {@link Attendee} object
     */
    public static Attendee assertPartStat(List<Attendee> attendees, Integer entity, PartStat expectedPartStat) {
        Attendee matchingAttendee = ChronosUtils.find(attendees, entity);
        expectedPartStat.assertStatus(matchingAttendee);
        return matchingAttendee;
    }

    /**
     * Asserts that the given attendee represented by its mail has the desired participant status
     *
     * @param attendees The attendees of the event
     * @param email The attendee to check represented by its mail
     * @param expectedPartStat The participant status of the attendee as {@link com.openexchange.chronos.ParticipationStatus}
     * @return The attendee to check as {@link com.openexchange.chronos.Attendee} object
     */
    public static com.openexchange.chronos.Attendee assertAttendeePartStat(List<com.openexchange.chronos.Attendee> attendees, String email, com.openexchange.chronos.ParticipationStatus expectedPartStat) {
        com.openexchange.chronos.Attendee matchingAttendee = null;
        if (null != attendees) {
            for (com.openexchange.chronos.Attendee attendee : attendees) {
                String uri = attendee.getUri();
                if (null != uri && uri.toLowerCase().contains(email.toLowerCase())) {
                    matchingAttendee = attendee;
                    break;
                }
            }
        }
        assertNotNull(matchingAttendee);
        assertEquals(expectedPartStat, matchingAttendee.getPartStat());
        return matchingAttendee;
    }

    /**
     * Asserts that exactly one event was handled by the server
     *
     * @param actionResponse The {@link ActionResponse} from the server
     * @return The {@link EventData} of the handled event
     */
    public static EventData assertSingleEvent(ActionResponse actionResponse) {
        return assertSingleEvent(actionResponse, null);
    }

    /**
     * Asserts that exactly one event was handled by the server
     *
     * @param actionResponse The {@link ActionResponse} from the server
     * @param uid The uid the event should have or <code>null</code>
     * @return The {@link EventData} of the handled event
     */
    public static EventData assertSingleEvent(ActionResponse actionResponse, String uid) {
        return assertEvents(actionResponse, uid, 1).get(0);
    }

    /**
     * Asserts that the given count on events was handled by the server
     *
     * @param actionResponse The {@link ActionResponse} from the server
     * @param uid The uid the event should have or <code>null</code>
     * @param size The expected size of the returned events
     * @return The {@link EventData} of the handled event
     */
    public static List<EventData> assertEvents(ActionResponse actionResponse, String uid, int size) {
        assertNotNull(actionResponse.getData());
        assertThat("Only one object should have been handled", Integer.valueOf(actionResponse.getData().size()), is(I(size)));
        List<EventData> events = new LinkedList<>();
        for (EventData eventData : actionResponse.getData()) {
            if (null != uid) {
                assertEquals(uid, eventData.getUid());
            }
            events.add(eventData);
        }
        return events;
    }

    /**
     * Asserts that exactly one event was handled by the server
     *
     * @param actionResponse The {@link ActionResponse} from the server
     * @return The {@link EventData} of the handled event
     */
    public static EventData assertSingleEvent(CalendarResult actionResponse) {
        return assertSingleEvent(actionResponse, null);
    }

    /**
     * Asserts that exactly one event was handled by the server
     *
     * @param actionResponse The {@link ActionResponse} from the server
     * @param uid The uid the event should have or <code>null</code>
     * @return The {@link EventData} of the handled event
     */
    public static EventData assertSingleEvent(CalendarResult actionResponse, String uid) {
        return assertEvents(actionResponse, uid, 1).get(0);
    }

    /**
     * Asserts that the given count on events was handled by the server
     *
     * @param actionResponse The {@link ActionResponse} from the server
     * @param uid The uid the event should have or <code>null</code>
     * @param size The expected size of the returned events
     * @return The {@link EventData} of the handled event
     */
    public static List<EventData> assertEvents(CalendarResult actionResponse, String uid, int size) {
        assertNotNull(actionResponse);
        assertThat(I(actionResponse.getCreated().size() + actionResponse.getUpdated().size()), is(I(size)));
        List<EventData> events = new LinkedList<>();
        gatherEventGroup(actionResponse.getCreated(), uid, events);
        gatherEventGroup(actionResponse.getUpdated(), uid, events);
        Assertions.assertTrue(events.size() == size);
        return events;
    }

    private static void gatherEventGroup(List<EventData> response, String uid, List<EventData> events) {
        for (EventData eventData : response) {
            if (null != uid) {
                assertEquals(uid, eventData.getUid());
            }
            events.add(eventData);
        }
    }

    /**
     * Assert that the action failed due missing calendar access
     *
     * @param response The response to check
     */
    public static void assertNoCalendarAccess(AnalyzeResponse response) {
        assertNoCalendarAccess(response.getError(), response.getErrorDesc());
    }

    /**
     * Assert that the action failed due missing calendar access
     *
     * @param error The error to check
     * @param errorDesc The error description to log
     */
    public static void assertNoCalendarAccess(String error, String errorDesc) {
        assertNoCalendarAccess(error, errorDesc, INTERNAL_CALENDAR);
    }

    /**
     * Assert that the action failed due missing calendar access
     *
     * @param error The error to check
     * @param errorDesc The error description to log
     * @param providerId The provider ID that shall be checked
     */
    public static void assertNoCalendarAccess(String error, String errorDesc, String providerId) {
        // com.openexchange.chronos.exception.CalendarExceptionMessages.UNSUPPORTED_OPERATION_FOR_PROVIDER_MSG
        assertThat("User has no access, so no operation should have been executed. " + errorDesc, error, is("The requested operation is not supported for calendar provider \"" + providerId + "\"."));
    }

}
